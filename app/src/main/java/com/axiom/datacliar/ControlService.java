package com.axiom.datacliar;

import android.content.Context;
import android.os.Build;
import java.io.*;
import java.util.concurrent.TimeUnit;

/** Shizuku UserService: no PackageManager/ContentResolver calls with a mismatched app Context. */
public final class ControlService extends IControlService.Stub {
    private final int profile;
    public ControlService(Context context){this(context.getApplicationInfo().uid/100000);}
    ControlService(int profile){this.profile=profile;}
    @Override public void destroy(){System.exit(0);}
    @Override public synchronized String execute(String action,String pkg,int user){
        try{
            if(user!=profile)return "ERROR: Cross-profile controls are disabled";
            int uid=android.os.Process.myUid();if(uid!=0&&uid!=2000)return "ERROR: Shizuku shell/root access is required";
            if("ping".equals(action))return "OK: privileged control service ready";
            CommandPolicy.validate(action,pkg,user);
            String u=Integer.toString(user);
            if(pkg.equals("com.axiom.datacliar")||pkg.equals("moe.shizuku.privileged.api"))throw new SecurityException("DATACLIAR and Shizuku are protected");
            String users=required(new String[]{"pm","list","packages","-3","--user",u});
            if(!CommandPolicy.containsPackage(users,pkg))throw new SecurityException("Only installed user apps can be controlled; system apps are protected");
            if(!action.equals("unfreeze"))checkEssential(pkg,u);
            boolean cache=false;
            if(action.equals("cache")){
                if(Build.VERSION.SDK_INT<33)throw new UnsupportedOperationException("Safe cache-only cleanup needs Android 13+ and OS support. Use App info for manual cache clearing.");
                cache=required(new String[]{"pm","help"}).contains("--cache-only");
            }
            String result=run(CommandPolicy.command(action,pkg,user,cache),20);
            if(result.startsWith("ERROR:"))return action.equals("cache")?"ERROR: Cache-only cleanup was not confirmed: "+result+" Use Android App info > Storage > Clear cache.":result;
            if((action.equals("data")||action.equals("cache"))&&!result.equals("Success"))return "ERROR: Android did not confirm clearing: "+result;
            if(action.equals("freeze")||action.equals("unfreeze")){
                boolean disabled=CommandPolicy.containsPackage(required(new String[]{"pm","list","packages","-d","--user",u}),pkg);
                if(disabled!=action.equals("freeze"))return "ERROR: Requested enabled/disabled state was not confirmed. Refresh before retrying.";
            }
            if(action.equals("sleep")||action.equals("wake")){
                String expected="Idle="+(action.equals("sleep")?"true":"false");
                String state="";
                // Android applies the inactivity hint asynchronously. Re-read; never re-run the mutation.
                for(int attempt=0;attempt<10;attempt++){
                    state=required(new String[]{"am","get-inactive","--user",u,pkg});
                    if(state.contains(expected))break;
                    Thread.sleep(200);
                }
                if(!state.contains(expected))return "ERROR: Android did not confirm the inactive state (charging or recent use may override it): "+state;
                result=state;
            }
            return result.isEmpty()?"OK: command accepted by Android":result;
        }catch(Exception e){return "ERROR: "+e.getMessage();}
    }
    private void checkEssential(String pkg,String user)throws Exception{
        String home=required(new String[]{"cmd","package","resolve-activity","--brief","--user",user,"-a","android.intent.action.MAIN","-c","android.intent.category.HOME"});
        if(!home.contains("/"))throw new SecurityException("Could not verify default launcher; controls refused");
        if(CommandPolicy.containsComponent(home,pkg))throw new SecurityException("Default launcher is protected");
        String keyboard=required(new String[]{"settings","--user",user,"get","secure","default_input_method"});
        if(keyboard.startsWith(pkg+"/"))throw new SecurityException("Active keyboard is protected");
        String admins=required(new String[]{"dumpsys","device_policy"});
        if(CommandPolicy.containsComponent(admins,pkg))throw new SecurityException("Device-policy app is protected");
        String vpn=required(new String[]{"cmd","package","query-services","--brief","--query-flags","512","--user",user,"-a","android.net.VpnService"});
        if(CommandPolicy.containsComponent(vpn,pkg))throw new SecurityException("VPN app is protected");
    }
    private String required(String[] cmd)throws Exception{
        String result=run(cmd,10);if(result.startsWith("ERROR:"))throw new IOException("Protection/state check failed: "+result);return result;
    }
    private String run(String[] command,int seconds)throws Exception{
        Process process=new ProcessBuilder(command).redirectErrorStream(true).start();
        ByteArrayOutputStream bytes=new ByteArrayOutputStream();final boolean[] truncated={false};
        Thread reader=new Thread(()->{try(InputStream in=process.getInputStream()){byte[] buffer=new byte[4096];int count;while((count=in.read(buffer))!=-1){if(bytes.size()+count<=512000)bytes.write(buffer,0,count);else truncated[0]=true;}}catch(IOException ignored){}});
        reader.start();
        if(!process.waitFor(seconds,TimeUnit.SECONDS)){process.destroyForcibly();reader.join(1000);return "ERROR: Operation timed out. Refresh to check the current state.";}
        reader.join(1000);
        if(reader.isAlive()||truncated[0])return "ERROR: Incomplete command output; result could not be verified";
        String out=bytes.toString("UTF-8").trim();
        if(process.exitValue()!=0||out.contains("Exception:")||out.startsWith("Error:")||out.startsWith("Failure")||out.startsWith("Unknown command"))return "ERROR: "+out;
        return out;
    }
}
