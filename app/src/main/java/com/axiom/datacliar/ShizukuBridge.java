package com.axiom.datacliar;

import android.content.*;
import android.content.pm.PackageManager;
import android.os.*;
import java.util.*;
import rikka.shizuku.Shizuku;

/** Application-scoped connection: rotation/backgrounding never stops Shizuku. No network dependency. */
public final class ShizukuBridge {
    public static final String MANAGER="moe.shizuku.privileged.api";
    private static ShizukuBridge instance;
    public static synchronized ShizukuBridge get(Context c){if(instance==null)instance=new ShizukuBridge(c.getApplicationContext());return instance;}
    private final Handler main=new Handler(Looper.getMainLooper());
    private final Set<Runnable> observers=new HashSet<>();
    private final Shizuku.UserServiceArgs args;
    private final java.util.concurrent.ExecutorService operations=java.util.concurrent.Executors.newSingleThreadExecutor();
    private final android.content.SharedPreferences preferences;
    private boolean busy;
    private volatile boolean cancelled;
    private final AppStore store;
    private String lastResult;
    private volatile IControlService service;
    private boolean binding;
    private int generation;
    private String state="Shizuku is not running";
    private final Shizuku.OnBinderReceivedListener received=()->main.post(()->connect(false));
    private final Shizuku.OnBinderDeadListener dead=()->main.post(()->{generation++;service=null;binding=false;setState("Shizuku stopped — start it again in Setup");});
    private final Shizuku.OnRequestPermissionResultListener permission=(request,result)->main.post(()->{if(result==PackageManager.PERMISSION_GRANTED)connect(false);else setState("Permission denied — allow DATACLIAR in Shizuku, then reconnect");});
    private ShizukuBridge(Context c){
        store=AppStore.get(c);
        preferences=c.getSharedPreferences("controls",Context.MODE_PRIVATE);
        lastResult=preferences.getString("lastResult","No privileged operations yet.");
        args=new Shizuku.UserServiceArgs(new ComponentName(c,ControlService.class)).daemon(true).processNameSuffix("control").version(4);
        Shizuku.addBinderReceivedListenerSticky(received);Shizuku.addBinderDeadListener(dead);Shizuku.addRequestPermissionResultListener(permission);
    }
    public void observe(Runnable r){observers.add(r);}
    public void removeObserver(Runnable r){observers.remove(r);}
    private void setState(String value){if(value.equals(state))return;state=value;notifyObservers();}
    private void notifyObservers(){for(Runnable r:new ArrayList<>(observers))r.run();}
    public boolean busy(){return busy;}
    public String lastResult(){return lastResult;}
    public void cancelBatch(){if(busy){cancelled=true;lastResult="Stopping after the current app finishes…";notifyObservers();}}
    public void perform(String action,String pkg,String label,int user){run(action,Collections.singletonList(new BatchTarget(pkg,label)),user);}
    public void performBatch(String action,List<BatchTarget> targets,int user){
        if(!BatchTarget.batchAction(action))throw new IllegalArgumentException("Only freeze, unfreeze and stop are allowed in batches");
        run(action,BatchTarget.unique(targets),user);
    }
    private void run(String action,List<BatchTarget> targets,int user){
        if(busy||!ready()||targets.isEmpty())return;
        List<BatchTarget> queue=new ArrayList<>(targets);busy=true;cancelled=false;lastResult="Starting "+queue.size()+" operation(s)…";notifyObservers();
        operations.execute(()->{
            int ok=0,failed=0,skipped=0;
            for(int i=0;i<queue.size();i++){
                BatchTarget target=queue.get(i);String id=store.begin(action,target.pkg,target.label);
                String result;String outcome;
                if(cancelled){result="Skipped: batch cancelled; no command executed";outcome="skipped";skipped++;}
                else if(!ready()){result="ERROR: Shizuku disconnected; no command executed";outcome="failed";failed++;cancelled=true;}
                else{result=execute(action,target.pkg,user);outcome=result.startsWith("ERROR:")?"failed":"completed";if(outcome.equals("completed"))ok++;else failed++;}
                store.finish(id,outcome,result);
                String progress=(i+1)+" / "+queue.size()+" • "+target.label+"\n"+result;
                main.post(()->{lastResult=progress;notifyObservers();});
            }
            String summary=ok+" completed • "+failed+" failed • "+skipped+" skipped. See History for each app.";
            main.post(()->{busy=false;lastResult=summary;preferences.edit().putString("lastResult",lastResult).apply();notifyObservers();});
        });
    }
    public String status(){return state;}
    public boolean ready(){try{return service!=null&&service.asBinder().pingBinder()&&Shizuku.pingBinder()&&Shizuku.checkSelfPermission()==PackageManager.PERMISSION_GRANTED;}catch(Exception e){return false;}}
    public void connect(boolean ask){
        try{
            if(!Shizuku.pingBinder()){service=null;binding=false;setState("Shizuku is not running — open Setup to start");return;}
            if(Shizuku.getVersion()<13){setState("Update Shizuku to version 13 or newer");return;}
            if(Shizuku.checkSelfPermission()!=PackageManager.PERMISSION_GRANTED){service=null;setState("Shizuku running — DATACLIAR authorization needed");if(ask){if(Shizuku.shouldShowRequestPermissionRationale())setState("Permission denied — open Shizuku > Authorized applications and allow DATACLIAR");else Shizuku.requestPermission(42);}return;}
            if(ready()){setState("Connected • controls ready • Wi-Fi not required by DATACLIAR");return;}
            if(binding)return;
            binding=true;int attempt=++generation;setState("Connecting to privileged controls…");
            ServiceConnection connection=new ServiceConnection(){
                public void onServiceConnected(ComponentName name,IBinder binder){
                    if(attempt!=generation)return;
                    service=IControlService.Stub.asInterface(binder);binding=false;setState("Connected • controls ready • Wi-Fi not required by DATACLIAR");
                }
                public void onServiceDisconnected(ComponentName name){if(attempt!=generation)return;service=null;binding=false;setState("Controls disconnected — tap Connect again");}
            };
            Shizuku.bindUserService(args,connection);
            main.postDelayed(()->{if(attempt==generation&&binding){generation++;binding=false;try{Shizuku.unbindUserService(args,connection,false);}catch(Exception ignored){}setState("Connection timed out — reopen Shizuku and reconnect");}},15000);
        }catch(Exception e){service=null;binding=false;setState("Connection error: "+e.getMessage());}
    }
    public String execute(String action,String pkg,int user){
        try{IControlService current=service;if(current==null||!ready())return "ERROR: Shizuku disconnected. Reopen Setup and connect.";return current.execute(action,pkg,user);}catch(Exception e){main.post(()->{service=null;binding=false;setState("Shizuku connection lost — reconnect in Setup");});return "ERROR: "+e.getMessage();}
    }
}
