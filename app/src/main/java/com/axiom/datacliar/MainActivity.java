package com.axiom.datacliar;

import android.app.*;
import android.content.*;
import android.graphics.*;
import android.graphics.drawable.*;
import android.net.Uri;
import android.os.*;
import android.provider.Settings;
import android.text.*;
import android.view.*;
import android.widget.*;
import java.util.*;
import java.util.concurrent.*;
import rikka.shizuku.Shizuku;

public class MainActivity extends Activity {
    private static final int BG = Color.rgb(7,19,33), CARD = Color.rgb(15,36,55), CYAN = Color.rgb(85,223,255), MUTED = Color.rgb(157,181,202), WHITE = Color.rgb(235,246,255);
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private final Handler handler = new Handler(Looper.getMainLooper());
    private volatile IControlService service;
    private DeviceRepository repo;
    private LinearLayout root, body, nav, list;
    private TextView ramText, storageText, cpuText, statusText, appCount;
    private Meter ramMeter, storageMeter;
    private String page = "Home", query = "", filter = "All", lastResult = "No operations yet.";
    private List<DeviceRepository.App> apps = new ArrayList<>();
    private boolean scanning, busy, resumed, binding, metricsPending;
    private volatile boolean processKnown;
    private long[] previousCpu;
    private Shizuku.UserServiceArgs serviceArgs;
    private final Shizuku.OnBinderReceivedListener received = () -> runOnUiThread(() -> connect(false));
    private final Shizuku.OnBinderDeadListener dead = () -> runOnUiThread(() -> { service = null; binding = false; processKnown = false; render(); });
    private final Shizuku.OnRequestPermissionResultListener permission = (request, grant) -> runOnUiThread(() -> {
        if (grant == android.content.pm.PackageManager.PERMISSION_GRANTED) connect(false);
        else message("Permission not granted", "Dashboard still works. Allow DATACLIAR in Shizuku to enable app controls.");
    });
    private final ServiceConnection connection = new ServiceConnection() {
        public void onServiceConnected(ComponentName name, IBinder binder) {
            service = IControlService.Stub.asInterface(binder); binding = false; render(); scan();
        }
        public void onServiceDisconnected(ComponentName name) { service = null; binding = false; processKnown = false; render(); }
    };
    private final Runnable ticker = new Runnable() {
        public void run() { if (!resumed) return; if (page.equals("Home")) updateMetrics(); handler.postDelayed(this, 2000); }
    };
    @Override public void onCreate(Bundle saved) {
        super.onCreate(saved);
        repo = new DeviceRepository(this);
        if (saved != null) { page = saved.getString("page", "Home"); filter = saved.getString("filter", "All"); }
        serviceArgs = new Shizuku.UserServiceArgs(new ComponentName(this, ControlService.class)).daemon(false).processNameSuffix("control").version(1);
        Shizuku.addBinderReceivedListenerSticky(received);
        Shizuku.addBinderDeadListener(dead);
        Shizuku.addRequestPermissionResultListener(permission);
        render(); scan();
    }
    @Override protected void onResume() { super.onResume(); resumed = true; handler.post(ticker); if (repo != null) { render(); scan(); } }
    @Override protected void onPause() { resumed = false; handler.removeCallbacks(ticker); super.onPause(); }
    @Override protected void onSaveInstanceState(Bundle out) { out.putString("page", page); out.putString("filter", filter); super.onSaveInstanceState(out); }
    @Override protected void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        Shizuku.removeBinderReceivedListener(received); Shizuku.removeBinderDeadListener(dead); Shizuku.removeRequestPermissionResultListener(permission);
        try { if (service != null) Shizuku.unbindUserService(serviceArgs, connection, false); } catch (Exception ignored) { }
        worker.shutdownNow(); super.onDestroy();
    }
    private int dp(float n) { return (int)(n * getResources().getDisplayMetrics().density + .5f); }
    private GradientDrawable bg(int color, int radius) { GradientDrawable d = new GradientDrawable(); d.setColor(color); d.setCornerRadius(dp(radius)); d.setStroke(dp(1), Color.rgb(31,65,86)); return d; }
    private TextView text(String value, int size, int color) { TextView v = new TextView(this); v.setText(value); v.setTextSize(size); v.setTextColor(color); v.setPadding(0,dp(4),0,dp(4)); return v; }
    private LinearLayout column() { LinearLayout l = new LinearLayout(this); l.setOrientation(LinearLayout.VERTICAL); return l; }
    private LinearLayout card(LinearLayout parent) { LinearLayout c = column(); c.setPadding(dp(18),dp(14),dp(18),dp(14)); c.setBackground(bg(CARD,24)); LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1,-2); p.bottomMargin = dp(14); parent.addView(c,p); return c; }
    private Button button(String label, Runnable click) { Button b = new Button(this); b.setText(label); b.setAllCaps(false); b.setTextSize(14); b.setTextColor(CYAN); b.setMinHeight(dp(48)); b.setBackground(bg(Color.rgb(18,48,67),14)); LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1,-2); p.topMargin=dp(8); b.setLayoutParams(p); b.setOnClickListener(v -> click.run()); return b; }
    private void render() {
        if (isDestroyed() || isFinishing()) return;
        root = column(); root.setBackgroundColor(BG);
        root.setOnApplyWindowInsetsListener((v, insets) -> { v.setPadding(insets.getSystemWindowInsetLeft(), insets.getSystemWindowInsetTop(), insets.getSystemWindowInsetRight(), insets.getSystemWindowInsetBottom()); return insets; });
        LinearLayout header = column(); header.setPadding(dp(22),dp(12),dp(22),dp(8));
        TextView brand = text("◈  DATACLIAR",22,WHITE); brand.setTypeface(null,Typeface.BOLD); header.addView(brand);
        statusText = text(service != null ? "●  Shizuku connected  ·  Device controls ready" : "○  Monitor mode  ·  Connect Shizuku for controls",12,service != null ? CYAN : MUTED); header.addView(statusText); root.addView(header);
        ScrollView scroll = new ScrollView(this); scroll.setFillViewport(true); body=column(); body.setPadding(dp(18),dp(8),dp(18),dp(10)); scroll.addView(body); root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        nav = new LinearLayout(this); nav.setPadding(dp(8),dp(6),dp(8),dp(6));
        for (String tab : new String[]{"Home","Apps","Frozen","Access"}) {
            Button b=button(tab,()->{page=tab; query=""; render();}); b.setTextSize(12); b.setTextColor(page.equals(tab)?CYAN:MUTED); nav.addView(b,new LinearLayout.LayoutParams(0,dp(52),1));
        }
        root.addView(nav); setContentView(root); root.requestApplyInsets();
        if (page.equals("Home")) home(); else if (page.equals("Access")) access(); else appPage();
    }
    private void home() {
        body.addView(text("Your phone. In focus.",28,WHITE)); body.addView(text(Build.MANUFACTURER+" "+Build.MODEL+"  •  Android "+Build.VERSION.RELEASE,13,MUTED));
        LinearLayout memory=card(body); memory.addView(text("MEMORY / RAM",12,CYAN)); ramText=text("Reading memory…",24,WHITE); memory.addView(ramText); ramMeter=new Meter(); memory.addView(ramMeter,new LinearLayout.LayoutParams(-1,dp(10))); memory.addView(text("Available RAM includes reclaimable memory.",12,MUTED));
        LinearLayout storage=card(body); storage.addView(text("INTERNAL STORAGE / ROM",12,CYAN)); storageText=text("Reading storage…",22,WHITE); storage.addView(storageText); storageMeter=new Meter(); storage.addView(storageMeter,new LinearLayout.LayoutParams(-1,dp(10))); storage.addView(text("Usable data partition, not advertised chip capacity.",12,MUTED));
        LinearLayout cpu=card(body); cpu.addView(text("PROCESSOR",12,CYAN)); cpuText=text("Sampling CPU…",19,WHITE); cpu.addView(cpuText); cpu.addView(text(Runtime.getRuntime().availableProcessors()+" cores  •  "+Build.SUPPORTED_ABIS[0],13,MUTED));
        LinearLayout actions=card(body); actions.addView(text("A little breathing room",21,WHITE)); actions.addView(text("Clear eligible app caches or stop selected background apps. No invented speed scores.",13,MUTED));
        actions.addView(button("✦  Clean app caches",()->batch(false))); actions.addView(button("ϟ  Boost / stop background apps",()->batch(true)));
        actions.addView(button("View running apps",()->{page="Apps";filter="Running";render();scan();}));
        LinearLayout result=card(body); result.addView(text("LAST ACTIVITY",12,CYAN)); result.addView(text(busy?"Operation in progress…":lastResult,13,MUTED));
        updateMetrics();
    }
    private void updateMetrics() {
        if (ramText == null || !page.equals("Home")) return;
        ActivityManager.MemoryInfo m=new ActivityManager.MemoryInfo(); getSystemService(ActivityManager.class).getMemoryInfo(m);
        ramText.setText(size(m.availMem)+" free / "+size(m.totalMem)); ramMeter.value=1f-(float)m.availMem/m.totalMem; ramMeter.invalidate();
        StatFs fs=new StatFs(Environment.getDataDirectory().getAbsolutePath()); storageText.setText(size(fs.getAvailableBytes())+" free / "+size(fs.getTotalBytes())); storageMeter.value=1f-(float)fs.getAvailableBytes()/fs.getTotalBytes(); storageMeter.invalidate();
        if (busy || scanning || metricsPending) return;
        metricsPending=true;
        worker.execute(()->{
            String raw=DeviceRepository.localCpu(); if (raw.isEmpty() && service!=null) raw=call("cpu","");
            String label="CPU load unavailable · OS restricted";
            try {
                long[] now=DeviceRepository.cpuTicks(raw);
                if (now!=null) { if (previousCpu!=null && now[0]>previousCpu[0]) label=Math.round(100.0*(1.0-(double)(now[1]-previousCpu[1])/(now[0]-previousCpu[0])))+"% CPU in use"; else label="Sampling CPU…"; previousCpu=now; }
            } catch (Exception ignored) { }
            String value=label; runOnUiThread(()->{metricsPending=false;if (!isDestroyed() && page.equals("Home") && cpuText!=null) cpuText.setText(value);});
        });
    }
    private void appPage() {
        body.addView(text(page.equals("Frozen")?"Frozen, not forgotten.":"Your app collection",26,WHITE));
        body.addView(text(page.equals("Frozen")?"Disabled apps remain here. Unfreeze restores their launcher entry.":"Sorted by storage • current Android user",13,MUTED));
        if (!repo.usageAllowed()) body.addView(button("Allow Usage Access for storage sizes",()->open(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))));
        if (!page.equals("Frozen")) {
            Spinner spinner=new Spinner(this); ArrayAdapter<String> adapter=new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,new String[]{"All","User apps","System apps","Running","Sleeping"}); spinner.setAdapter(adapter);
            spinner.setSelection(Arrays.asList("All","User apps","System apps","Running","Sleeping").indexOf(filter));
            spinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener(){public void onNothingSelected(android.widget.AdapterView<?> p){} public void onItemSelected(android.widget.AdapterView<?> p,View v,int pos,long id){filter=(String)p.getItemAtPosition(pos);showApps();}}); body.addView(spinner);
        }
        EditText search=new EditText(this); search.setSingleLine(true); search.setTextColor(WHITE); search.setHintTextColor(MUTED); search.setHint("Search app or package"); search.setContentDescription("Search installed apps"); search.setText(query); body.addView(search);
        search.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence s,int a,int c,int f){} public void onTextChanged(CharSequence s,int a,int b,int c){query=s.toString();showApps();} public void afterTextChanged(Editable e){}});
        appCount=text("",12,CYAN);body.addView(appCount);body.addView(button("Refresh apps & storage",this::scan));list=column();body.addView(list);showApps();
    }
    private void showApps() {
        if (list==null || !(page.equals("Apps")||page.equals("Frozen"))) return;
        list.removeAllViews(); int count=0;
        if (filter.equals("Running") && !processKnown && !page.equals("Frozen")) list.addView(text("Running count unavailable. Connect Shizuku and refresh; Android hides other apps' processes in monitor mode.",15,MUTED));
        for (DeviceRepository.App a : apps) {
            if (page.equals("Frozen") && !a.frozen) continue;
            if (!page.equals("Frozen") && ((filter.equals("Running")&&!a.running)||(filter.equals("Sleeping")&&!a.sleeping)||(filter.equals("User apps")&&a.system)||(filter.equals("System apps")&&!a.system))) continue;
            if (!(a.label+" "+a.pkg).toLowerCase(Locale.ROOT).contains(query.toLowerCase(Locale.ROOT))) continue;
            count++; LinearLayout row=card(list); LinearLayout top=new LinearLayout(this);top.setGravity(Gravity.CENTER_VERTICAL);
            ImageView icon=new ImageView(this);icon.setImageDrawable(a.info.loadIcon(getPackageManager()));icon.setContentDescription(a.label);top.addView(icon,new LinearLayout.LayoutParams(dp(40),dp(40)));
            TextView title=text("  "+a.label,17,WHITE);title.setTypeface(null,Typeface.BOLD);top.addView(title,new LinearLayout.LayoutParams(0,-2,1));row.addView(top);
            row.addView(text(a.pkg,11,MUTED));row.addView(text(a.total()<0?"Storage unavailable · enable Usage Access":size(a.total())+" total  ·  "+size(a.cache)+" cache",14,CYAN));
            row.addView(text((a.system?"System • read-only":"User app")+(a.frozen?"  •  Frozen":"")+(a.running?"  •  Running process":"")+(a.sleeping?"  •  Inactive":""),11,MUTED));row.setOnClickListener(v->details(a));
        }
        appCount.setText(scanning?"Scanning…":count+" apps shown"+(processKnown?"  •  "+apps.stream().filter(a->a.running).count()+" running apps":"  •  Running count unknown"));
        if(count==0&&!scanning)list.addView(text("No matching apps.",16,MUTED));
    }
    private void scan() {
        if(scanning||busy||isDestroyed())return;scanning=true;showApps();
        worker.execute(()->{
            try {
                String processes=service==null?"ERROR: No access":call("processes",""); processKnown=!processes.startsWith("ERROR:");
                List<DeviceRepository.App> found=repo.apps(processKnown?CommandPolicy.processNames(processes,android.os.Process.myUid()/100000):Collections.emptySet());
                runOnUiThread(()->{apps=found;scanning=false;showApps();});
            } catch(Exception e){runOnUiThread(()->{scanning=false;lastResult="Scan failed: "+e.getMessage();showApps();});}
        });
    }
    private void details(DeviceRepository.App a) {
        String info=a.pkg+"\n\n"+(a.total()<0?"Storage access unavailable":"App: "+size(a.code)+"\nData (includes cache): "+size(a.data)+"\nCache: "+size(a.cache))+"\n\nSystem and essential apps are protected. Stop/sleep can delay notifications; freeze hides the launcher icon.";
        String[] options={"Android App info","Clear cache only","Clear ALL app data","Force stop",a.frozen?"Unfreeze / show app":"Freeze / hide app","Sleep (inactive)","Wake (active)"};
        new AlertDialog.Builder(this).setTitle(a.label).setItems(options,(d,i)->{
            if(i==0){appInfo(a.pkg);return;}
            String action=new String[]{"","cache","data","stop",a.frozen?"unfreeze":"freeze","sleep","wake"}[i];
            if(!requireControl())return;
            if(a.system){message("Protected app","System applications are read-only here. Use Android App info if needed.");return;}
            String warning=action.equals("data")?"Permanently delete ALL local data, accounts, settings and files for "+a.label+"? This cannot be undone.":options[i]+" for "+a.label+"?\n\n"+(action.equals("freeze")?"The icon will disappear. Return to Frozen to restore it. Unfreeze before uninstalling DATACLIAR.":action.equals("sleep")?"Android standby hint; not a permanent freeze. Android may wake the app.":"Android may restrict this action. Results will be shown.");
            new AlertDialog.Builder(this).setTitle(options[i]).setMessage(warning).setNegativeButton("Cancel",null).setPositiveButton("Continue",(x,y)->perform(action,a)).show();
        }).setNeutralButton("Storage details",(d,w)->message(a.label,info)).setNegativeButton("Close",null).show();
    }
    private boolean requireControl(){if(busy){message("Working","Wait for the current operation to finish.");return false;}if(service!=null)return true;page="Access";render();message("Connect Shizuku","Android requires elevated access to manage other apps. Connect Shizuku below, or use Android App info for manual controls.");return false;}
    private String call(String action,String pkg){try{IControlService s=service;if(s==null)return "ERROR: Shizuku disconnected";return s.execute(action,pkg,android.os.Process.myUid()/100000);}catch(Exception e){return "ERROR: "+e.getMessage();}}
    private void perform(String action,DeviceRepository.App a){busy=true;worker.execute(()->{String result=call(action,a.pkg);runOnUiThread(()->{busy=false;lastResult=a.label+" · "+action+"\n"+result;render();scan();message(result.startsWith("ERROR:")?"Could not complete":"Operation finished",lastResult);});});}
    private void batch(boolean boost){
        if(!requireControl())return;
        if(scanning){message("Scanning apps","Let the scan finish, then try again.");return;}
        if(!boost&&Build.VERSION.SDK_INT<33){message("Cache-only is unavailable","Android 13+ is required for safe batch cache clearing. Open an app's Android App info > Storage to clear cache manually. App data will not be erased as a fallback.");return;}
        List<DeviceRepository.App> candidates=new ArrayList<>();
        for(DeviceRepository.App a:apps)if(!a.system&&!a.frozen&&!a.pkg.equals(getPackageName())&&!a.pkg.equals("moe.shizuku.privileged.api")&&(!boost||a.running))candidates.add(a);
        if(candidates.isEmpty()){message("Nothing to process",boost?"Connect Shizuku and refresh running apps first.":"No eligible user apps found.");return;}
        String[] names=candidates.stream().map(a->a.label).toArray(String[]::new);boolean[] selected=new boolean[names.length];Arrays.fill(selected,true);
        new AlertDialog.Builder(this).setTitle(boost?"Boost: choose apps to stop":"Clean caches: choose apps").setMultiChoiceItems(names,selected,(d,i,on)->selected[i]=on).setNegativeButton("Cancel",null).setPositiveButton("Run",(d,w)->{
            busy=true;lastResult="Working…";render();worker.execute(()->{
                int ok=0,failed=0,skipped=0;StringBuilder errors=new StringBuilder();
                for(int i=0;i<candidates.size();i++)if(selected[i]){
                    if(Thread.currentThread().isInterrupted())break;
                    DeviceRepository.App a=candidates.get(i);
                    if(boost){String state=call("state",a.pkg);if(!CommandPolicy.backgroundState(state)){skipped++;continue;}}
                    String r=call(boost?"stop":"cache",a.pkg);
                    if(r.startsWith("ERROR:")){failed++;errors.append("\n").append(a.label).append(": ").append(r);}else ok++;
                }
                String result=ok+" completed · "+failed+" failed · "+skipped+" skipped\n"+(boost?"Foreground/unknown state apps were skipped. Stopping apps may delay notifications; speed gains are not guaranteed.":"Eligible user-app caches only. Protected/system caches were not touched.")+errors;
                runOnUiThread(()->{busy=false;lastResult=result;render();scan();message("Batch finished",result);});
            });
        }).show();
    }
    private void access(){
        body.addView(text("Unlock device controls",27,WHITE));body.addView(text("Your permissions. Your choice.",14,MUTED));
        LinearLayout u=card(body);u.addView(text("01  Usage Access",21,WHITE));u.addView(text(repo.usageAllowed()?"Granted · per-app storage is available":"Required to read app, data and cache sizes. No usage information leaves your phone.",14,MUTED));u.addView(button("Open Usage Access",()->open(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))));
        LinearLayout s=card(body);s.addView(text("02  Shizuku",21,WHITE));s.addView(text("Install Shizuku, start it using wireless debugging (Android 11+) or root, then allow DATACLIAR. After a reboot you may need to start Shizuku again.",14,MUTED));s.addView(button(service!=null?"Connected · reconnect":"Connect / authorize Shizuku",()->connect(true)));s.addView(button("Get Shizuku",()->open(new Intent(Intent.ACTION_VIEW,Uri.parse("https://shizuku.rikka.app/download/")))));
        LinearLayout about=card(body);about.addView(text("Made for clarity",21,WHITE));about.addView(text("Native Android • no ads • no analytics • no network permission.\n\nFreeze = disable for this user, hide launcher entry. Sleep = Android inactive hint, not a permanent block.\n\nCache-only cleanup needs Android 13+ and OEM support. System, launcher, keyboard, VPN and device-admin apps are protected. Work profiles are not managed.\n\nUnfreeze apps before removing DATACLIAR. Recovery via ADB: pm enable --user 0 PACKAGE.\n\nVersion 1.0.0 preview",14,MUTED));
    }
    private void connect(boolean ask){
        try{
            if(!Shizuku.pingBinder()){if(ask)message("Start Shizuku first","Open Shizuku and start its service, then return here.");return;}
            if(Shizuku.getVersion()<13){if(ask)message("Update Shizuku","Shizuku v13 or newer is required.");return;}
            if(Shizuku.checkSelfPermission()!=android.content.pm.PackageManager.PERMISSION_GRANTED){if(ask)Shizuku.requestPermission(42);return;}
            if(!binding&&service==null){binding=true;Shizuku.bindUserService(serviceArgs,connection);handler.postDelayed(()->{if(binding&&service==null){binding=false;message("Connection not ready","Shizuku did not connect. Restart Shizuku and try again.");}},10000);}
        }catch(Exception e){binding=false;if(ask)message("Shizuku unavailable",String.valueOf(e.getMessage()));}
    }
    private void appInfo(String pkg){open(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,Uri.parse("package:"+pkg)));}
    private void open(Intent intent){try{startActivity(intent);}catch(Exception e){message("Cannot open",String.valueOf(e.getMessage()));}}
    private void message(String title,String value){if(!isFinishing()&&!isDestroyed())new AlertDialog.Builder(this).setTitle(title).setMessage(value).setPositiveButton("OK",null).show();}
    private String size(long bytes){return android.text.format.Formatter.formatShortFileSize(this,Math.max(0,bytes));}
    private class Meter extends View{
        float value;final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);Meter(){super(MainActivity.this);setContentDescription("Used capacity meter");}
        @Override protected void onDraw(Canvas c){super.onDraw(c);paint.setColor(Color.rgb(28,57,77));c.drawRoundRect(0,0,getWidth(),getHeight(),dp(6),dp(6),paint);paint.setColor(CYAN);c.drawRoundRect(0,0,getWidth()*Math.max(0,Math.min(1,value)),getHeight(),dp(6),dp(6),paint);}
    }
}
