package com.axiom.datacliar;

import android.content.*;
import android.content.pm.*;
import android.net.Uri;
import android.os.*;
import org.json.*;
import javax.net.ssl.HttpsURLConnection;
import java.io.*;
import java.net.URL;
import java.nio.file.*;
import java.security.*;
import java.util.*;
import java.util.concurrent.*;

/** Opt-in GitHub checks and user-confirmed updates; no telemetry or silent installation. */
public final class UpdateManager {
    private static UpdateManager instance;
    public static synchronized UpdateManager get(Context c){if(instance==null)instance=new UpdateManager(c.getApplicationContext());return instance;}
    private final Context context;
    private final SharedPreferences preferences;
    private final Handler main=new Handler(Looper.getMainLooper());
    private final ExecutorService worker=Executors.newSingleThreadExecutor();
    private final Set<Runnable> observers=new HashSet<>();
    private boolean busy;
    private String status="Check for signed updates from this project's GitHub Releases.";
    private JSONObject available;
    private UpdateManager(Context c){context=c;preferences=c.getSharedPreferences("updates",Context.MODE_PRIVATE);try{available=new JSONObject(preferences.getString("available","{}"));if(!valid(available)||available.optLong("versionCode")<=versionCode())available=null;}catch(Exception e){available=null;}}
    public void observe(Runnable r){observers.add(r);}public void removeObserver(Runnable r){observers.remove(r);}
    private void publish(){for(Runnable r:new ArrayList<>(observers))r.run();}
    public boolean busy(){return busy;}public String status(){return status;}
    public boolean automatic(){return preferences.getBoolean("checkOnLaunch",false);}
    public void automatic(boolean value){preferences.edit().putBoolean("checkOnLaunch",value).apply();if(value)check();}
    public void checkOnLaunch(){if(automatic()&&System.currentTimeMillis()-preferences.getLong("checkedAt",0)>86400000L)check();}
    public long versionCode(){try{return code(context.getPackageManager().getPackageInfo(context.getPackageName(),0));}catch(Exception e){return 0;}}
    public String versionName(){try{return context.getPackageManager().getPackageInfo(context.getPackageName(),0).versionName;}catch(Exception e){return "Unknown";}}
    private long code(PackageInfo info){return Build.VERSION.SDK_INT>=28?info.getLongVersionCode():info.versionCode;}
    public String availableVersion(){return available==null?"No newer signed version found yet.":"Available: "+available.optString("versionName")+" ("+available.optLong("versionCode")+")";}
    public boolean hasUpdate(){return available!=null&&available.optLong("versionCode")>versionCode();}
    public boolean downloaded(){File apk=apk();return hasUpdate()&&apk.isFile()&&preferences.getString("downloadSha","").equals(available.optString("sha256"));}
    private File apk(){return new File(context.getCacheDir(),"updates/update.apk");}
    public void check(){
        if(busy)return;preferences.edit().putLong("checkedAt",System.currentTimeMillis()).apply();busy=true;status="Checking GitHub Releases…";publish();
        worker.execute(()->{
            JSONObject newest=null;String message;
            try{
                JSONArray releases=new JSONArray(text("https://api.github.com/repos/parvez-devs/DATACLIAR/releases?per_page=20"));
                for(int i=0;i<releases.length();i++){
                    JSONObject release=releases.getJSONObject(i);if(release.optBoolean("draft")||release.optBoolean("prerelease"))continue;
                    JSONArray assets=release.optJSONArray("assets");if(assets==null)continue;
                    for(int j=0;j<assets.length();j++){
                        JSONObject asset=assets.getJSONObject(j);if(!"update.json".equals(asset.optString("name")))continue;
                        String url=asset.optString("browser_download_url");if(!UpdatePolicy.assetUrl(url))continue;
                        JSONObject metadata=new JSONObject(text(url));if(!valid(metadata))continue;
                        if(metadata.optLong("versionCode")>versionCode()&&(newest==null||metadata.optLong("versionCode")>newest.optLong("versionCode")))newest=metadata;
                    }
                }
                message=newest==null?"You have the newest signed version available in this release channel.":"New version found. Download verifies SHA-256 and the signing certificate before installation.";
                preferences.edit().putLong("checkedAt",System.currentTimeMillis()).putString("available",newest==null?"{}":newest.toString()).apply();
            }catch(Exception e){message="Update check failed: "+e.getMessage()+". Your installed app and controls still work.";newest=available;}
            JSONObject result=newest;String outcome=message;
            main.post(()->{available=result;busy=false;status=outcome;publish();});
        });
    }
    private boolean valid(JSONObject metadata){
        try{UpdatePolicy.validate(metadata.getLong("versionCode"),metadata.getString("apkUrl"),metadata.getString("sha256"),metadata.getString("certificateSha256"));return "com.axiom.datacliar".equals(metadata.getString("packageName"))&&"stable".equals(metadata.getString("channel"));}catch(Exception e){return false;}
    }
    public void download(){
        if(busy||!hasUpdate())return;
        JSONObject metadata=available;busy=true;status="Downloading and verifying update…";publish();
        worker.execute(()->{
            String message;
            File partial=new File(context.getCacheDir(),"updates/update.part");
            try{
                if(!partial.getParentFile().isDirectory()&&!partial.getParentFile().mkdirs())throw new IOException("Cannot create update cache");
                HttpsURLConnection connection=open(metadata.getString("apkUrl"));
                try(InputStream in=connection.getInputStream();FileOutputStream out=new FileOutputStream(partial)){
                    MessageDigest digest=MessageDigest.getInstance("SHA-256");byte[] buffer=new byte[32768];int count;long total=0;
                    while((count=in.read(buffer))!=-1){total+=count;if(total>50L*1024*1024)throw new IOException("Update exceeds 50 MiB limit");digest.update(buffer,0,count);out.write(buffer,0,count);}
                    if(!hex(digest.digest()).equalsIgnoreCase(metadata.getString("sha256")))throw new SecurityException("Checksum mismatch; download rejected");
                }finally{connection.disconnect();}
                verifyArchive(partial,metadata);
                Files.move(partial.toPath(),apk().toPath(),StandardCopyOption.REPLACE_EXISTING);
                preferences.edit().putString("downloadSha",metadata.getString("sha256")).apply();
                message="Update verified. Tap Install update and confirm in Android's installer.";
            }catch(Exception e){partial.delete();message="Update rejected: "+e.getMessage();}
            String result=message;main.post(()->{busy=false;status=result;publish();});
        });
    }
    @SuppressWarnings("deprecation") private void verifyArchive(File file,JSONObject metadata)throws Exception{
        PackageManager pm=context.getPackageManager();int flags=Build.VERSION.SDK_INT>=28?PackageManager.GET_SIGNING_CERTIFICATES:PackageManager.GET_SIGNATURES;
        PackageInfo archive=pm.getPackageArchiveInfo(file.getAbsolutePath(),flags);
        if(archive==null||!context.getPackageName().equals(archive.packageName)||code(archive)!=metadata.getLong("versionCode")||code(archive)<=versionCode())throw new SecurityException("Package or version mismatch");
        PackageInfo installed=pm.getPackageInfo(context.getPackageName(),flags);
        String candidate=signer(archive),current=signer(installed);
        if(!UpdatePolicy.SIGNER.equals(candidate)||!candidate.equals(current))throw new SecurityException("Signing certificate differs. Old debug previews require a one-time manual migration after unfreezing apps.");
    }
    @SuppressWarnings("deprecation") private String signer(PackageInfo info)throws Exception{
        android.content.pm.Signature[] signatures=Build.VERSION.SDK_INT>=28?(info.signingInfo==null?null:info.signingInfo.getApkContentsSigners()):info.signatures;
        if(signatures==null||signatures.length!=1)throw new SecurityException("Unexpected APK signer");
        return hex(MessageDigest.getInstance("SHA-256").digest(signatures[0].toByteArray()));
    }
    public Intent installIntent()throws Exception{
        if(!downloaded())throw new IOException("Download and verify an update first");
        if(!sha(apk()).equalsIgnoreCase(available.getString("sha256")))throw new SecurityException("Downloaded APK changed; download again");
        verifyArchive(apk(),available);
        return new Intent(Intent.ACTION_VIEW).setDataAndType(Uri.parse("content://"+context.getPackageName()+".updates/update.apk"),"application/vnd.android.package-archive").addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
    }
    private String sha(File file)throws Exception{MessageDigest digest=MessageDigest.getInstance("SHA-256");try(InputStream in=new FileInputStream(file)){byte[] buffer=new byte[32768];int count;while((count=in.read(buffer))!=-1)digest.update(buffer,0,count);}return hex(digest.digest());}
    private String hex(byte[] bytes){StringBuilder out=new StringBuilder();for(byte b:bytes)out.append(String.format(Locale.ROOT,"%02x",b&255));return out.toString();}
    private String text(String url)throws Exception{
        HttpsURLConnection connection=open(url);
        try(InputStream in=connection.getInputStream();ByteArrayOutputStream out=new ByteArrayOutputStream()){
            byte[] buffer=new byte[8192];int count;while((count=in.read(buffer))!=-1){if(out.size()+count>1048576)throw new IOException("Update metadata too large");out.write(buffer,0,count);}return out.toString("UTF-8");
        }finally{connection.disconnect();}
    }
    private HttpsURLConnection open(String initial)throws Exception{
        String current=initial;
        for(int redirects=0;redirects<6;redirects++){
            URL url=new URL(current);String host=url.getHost();
            if(!"https".equals(url.getProtocol())||!(host.equals("github.com")||host.equals("api.github.com")||host.equals("release-assets.githubusercontent.com")||host.equals("objects.githubusercontent.com")))throw new SecurityException("Unexpected update host");
            HttpsURLConnection connection=(HttpsURLConnection)url.openConnection();connection.setConnectTimeout(15000);connection.setReadTimeout(15000);connection.setInstanceFollowRedirects(false);connection.setRequestProperty("User-Agent","DATACLIAR-update-check");connection.setRequestProperty("Accept","application/vnd.github+json");
            int code=connection.getResponseCode();if(code==301||code==302||code==303||code==307||code==308){String next=connection.getHeaderField("Location");connection.disconnect();if(next==null)throw new IOException("Missing redirect");current=new URL(url,next).toString();continue;}
            if(code!=200){connection.disconnect();throw new IOException("GitHub returned HTTP "+code);}
            return connection;
        }
        throw new IOException("Too many update redirects");
    }
}
