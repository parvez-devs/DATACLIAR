package com.axiom.datacliar;

import android.app.*;
import android.content.*;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.*;
import android.provider.Settings;
import android.text.*;
import android.view.*;
import android.widget.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;

/** Native dashboard with optional Shizuku controls and an embedded setup guide. */
public class MainActivity extends Activity {
    private static final int BG=0xff071321, CARD=0xff0f2437, CYAN=0xff55dfff, WHITE=0xffebf6ff, MUTED=0xff9db5ca;
    private final ExecutorService worker=Executors.newSingleThreadExecutor();
    private final Handler handler=new Handler(Looper.getMainLooper());
    private DeviceRepository repo;
    private ShizukuBridge bridge;
    private final Runnable connectionChanged=()->deliver(()->{render();if(!bridge.busy())scan();});
    private String page="Home", query="", filter="All", status="Dashboard ready. Optional controls are in Setup.";
    private boolean scanning, cleaning, resumed;
    private List<DeviceRepository.App> apps=new ArrayList<>();
    private LinearLayout body;
    private TextView ramText, storageText, countText;
    private ProgressBar ramBar, storageBar;
    private AppAdapter adapter;
    private final Runnable ticker=new Runnable(){public void run(){if(!resumed)return;updateMetrics();handler.postDelayed(this,2000);}};
    @Override public void onCreate(Bundle saved){
        super.onCreate(saved);repo=new DeviceRepository(this);bridge=ShizukuBridge.get(this);bridge.observe(connectionChanged);
        if(saved!=null){page=saved.getString("page","Home");query=saved.getString("query","");filter=saved.getString("filter","All");status=saved.getString("status",status);}
        if(page.equals("Access"))page="Setup";render();
    }
    @Override protected void onResume(){super.onResume();resumed=true;handler.removeCallbacks(ticker);handler.post(ticker);bridge.connect(false);render();scan();}
    @Override protected void onPause(){resumed=false;handler.removeCallbacks(ticker);super.onPause();}
    @Override protected void onSaveInstanceState(Bundle out){out.putString("page",page);out.putString("query",query);out.putString("filter",filter);out.putString("status",status);super.onSaveInstanceState(out);}
    @Override protected void onDestroy(){bridge.removeObserver(connectionChanged);worker.shutdownNow();handler.removeCallbacksAndMessages(null);super.onDestroy();}
    private void deliver(Runnable action){handler.post(()->{if(!isDestroyed()&&!isFinishing())action.run();});}
    private int dp(int n){return Math.round(n*getResources().getDisplayMetrics().density);}
    private LinearLayout column(){LinearLayout v=new LinearLayout(this);v.setOrientation(LinearLayout.VERTICAL);return v;}
    private GradientDrawable background(int color){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(16));return d;}
    private TextView text(String s,int size,int color){TextView v=new TextView(this);v.setText(s);v.setTextSize(size);v.setTextColor(color);v.setPadding(dp(4),dp(5),dp(4),dp(5));return v;}
    private Button button(String label,Runnable action){Button b=new Button(this);b.setText(label);b.setAllCaps(false);b.setTextColor(CYAN);b.setMinHeight(dp(48));b.setOnClickListener(v->action.run());return b;}
    private LinearLayout card(){LinearLayout c=column();c.setBackground(background(CARD));c.setPadding(dp(12),dp(12),dp(12),dp(12));LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.bottomMargin=dp(12);body.addView(c,p);return c;}
    private void navigate(String target){page=target;render();}
    private void render(){
        if(isDestroyed()||isFinishing())return;
        adapter=null;countText=null;ramText=null;storageText=null;
        LinearLayout root=column();root.setBackgroundColor(BG);
        root.setOnApplyWindowInsetsListener((v,i)->{v.setPadding(i.getSystemWindowInsetLeft(),i.getSystemWindowInsetTop(),i.getSystemWindowInsetRight(),i.getSystemWindowInsetBottom());return i;});
        TextView brand=text("◈  DATACLIAR",23,WHITE);brand.setTypeface(null,Typeface.BOLD);root.addView(brand);
        TextView connection=text(bridge.status(),12,bridge.ready()?CYAN:MUTED);connection.setOnClickListener(v->navigate("Setup"));root.addView(connection);
        body=column();body.setPadding(dp(12),dp(8),dp(12),0);
        if(page.equals("Apps")||page.equals("Disabled"))root.addView(body,new LinearLayout.LayoutParams(-1,0,1));
        else{ScrollView scroll=new ScrollView(this);scroll.addView(body);root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));}
        LinearLayout nav=new LinearLayout(this);
        for(String tab:new String[]{"Home","Apps","Disabled","Setup"}){Button b=button(tab,()->navigate(tab));b.setTextSize(11);b.setTextColor(page.equals(tab)?CYAN:MUTED);nav.addView(b,new LinearLayout.LayoutParams(0,-2,1));}
        root.addView(nav);setContentView(root);root.requestApplyInsets();
        if(page.equals("Home"))home();else if(page.equals("Setup"))access();else appPage();
    }
    private ProgressBar meter(LinearLayout parent){ProgressBar b=new ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal);b.setMax(1000);b.setProgressTintList(android.content.res.ColorStateList.valueOf(CYAN));parent.addView(b,new LinearLayout.LayoutParams(-1,dp(12)));return b;}
    private void home(){
        body.addView(text("Your phone. In focus.",27,WHITE));body.addView(text(Build.MANUFACTURER+" "+Build.MODEL+" • Android "+Build.VERSION.RELEASE,13,MUTED));
        LinearLayout ram=card();ram.addView(text("MEMORY / RAM",12,CYAN));ramText=text("Reading…",21,WHITE);ram.addView(ramText);ramBar=meter(ram);ram.addView(text("Available memory includes reclaimable memory.",12,MUTED));
        LinearLayout disk=card();disk.addView(text("INTERNAL STORAGE",12,CYAN));storageText=text("Reading…",21,WHITE);disk.addView(storageText);storageBar=meter(disk);disk.addView(text("Usable data partition, not advertised capacity.",12,MUTED));
        LinearLayout cpu=card();cpu.addView(text("PROCESSOR",12,CYAN));cpu.addView(text(Runtime.getRuntime().availableProcessors()+" available cores • "+Build.SUPPORTED_ABIS[0],17,WHITE));cpu.addView(text("System-wide CPU load and other apps' live processes are restricted by Android.",12,MUTED));
        LinearLayout actions=card();actions.addView(text("Storage tools",21,WHITE));actions.addView(button("Manage app storage",()->navigate("Apps")));actions.addView(button("Open device storage",()->open(new Intent(Settings.ACTION_INTERNAL_STORAGE_SETTINGS))));actions.addView(button("Clear DATACLIAR's own cache",this::confirmOwnCache));actions.addView(button("Shizuku setup / connect",()->navigate("Setup")));actions.addView(text("Connect Shizuku once to freeze/unfreeze, stop, sleep/wake and clear eligible app data inside DATACLIAR. Cache-only support depends on Android. Dashboard works without Shizuku.",13,MUTED));
        LinearLayout result=card();result.addView(text("STATUS",12,CYAN));result.addView(text(status+"\n\n"+bridge.lastResult(),13,MUTED));updateMetrics();
    }
    private void updateMetrics(){
        if(!page.equals("Home")||ramText==null)return;
        ActivityManager.MemoryInfo m=new ActivityManager.MemoryInfo();getSystemService(ActivityManager.class).getMemoryInfo(m);
        ramText.setText(size(m.availMem)+" free / "+size(m.totalMem));ramBar.setProgress(m.totalMem>0?(int)(1000*(1.0-(double)m.availMem/m.totalMem)):0);
        try{StatFs fs=new StatFs(Environment.getDataDirectory().getAbsolutePath());storageText.setText(size(fs.getAvailableBytes())+" free / "+size(fs.getTotalBytes()));storageBar.setProgress(fs.getTotalBytes()>0?(int)(1000*(1.0-(double)fs.getAvailableBytes()/fs.getTotalBytes())):0);}catch(Exception e){storageText.setText("Storage unavailable");}
    }
    private void appPage(){
        body.addView(text(page.equals("Disabled")?"Disabled apps":"Your app collection",25,WHITE));
        body.addView(text("Current profile • tap an app for controls",12,MUTED));
        if(!repo.usageAllowed())body.addView(button("Allow Usage Access for storage sizes",()->open(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))));
        if(!page.equals("Disabled")){
            Spinner spinner=new Spinner(this);String[] filters={"All","User apps","System apps"};spinner.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,filters));spinner.setSelection(Math.max(0,Arrays.asList(filters).indexOf(filter)));
            spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){public void onNothingSelected(AdapterView<?> p){}public void onItemSelected(AdapterView<?> p,View v,int pos,long id){filter=filters[pos];showApps();}});body.addView(spinner);
        }
        EditText search=new EditText(this);search.setSingleLine(true);search.setTextColor(WHITE);search.setHintTextColor(MUTED);search.setHint("Search app or package");search.setContentDescription("Search installed apps");search.setText(query);body.addView(search);
        search.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence s,int a,int c,int f){}public void afterTextChanged(Editable e){}public void onTextChanged(CharSequence s,int a,int b,int c){query=s.toString();showApps();}});
        countText=text("",12,CYAN);body.addView(countText);body.addView(button("Refresh apps & storage",this::scan));
        ListView list=new ListView(this);list.setDividerHeight(dp(6));list.setCacheColorHint(BG);adapter=new AppAdapter();list.setAdapter(adapter);list.setOnItemClickListener((p,v,pos,id)->details(adapter.visible.get(pos)));body.addView(list,new LinearLayout.LayoutParams(-1,0,1));showApps();
    }
    private void showApps(){if(adapter==null)return;adapter.visible.clear();String selected=page.equals("Disabled")?"Disabled":filter;for(DeviceRepository.App a:apps)if(AppQuery.matches(a.label,a.pkg,query,selected,a.system,a.disabled))adapter.visible.add(a);adapter.notifyDataSetChanged();countText.setText(scanning?"Scanning…":adapter.visible.size()+" apps shown • "+status);}
    private void scan(){
        if(scanning||isDestroyed())return;scanning=true;showApps();
        worker.execute(()->{try{List<DeviceRepository.App> found=repo.apps();deliver(()->{apps=found;scanning=false;showApps();});}catch(InterruptedException e){Thread.currentThread().interrupt();}catch(Exception e){deliver(()->{scanning=false;status="Scan failed. Try refresh.";showApps();});}});
    }
    private final class AppAdapter extends BaseAdapter {
        final List<DeviceRepository.App> visible=new ArrayList<>();
        public int getCount(){return visible.size();}public Object getItem(int p){return visible.get(p);}public long getItemId(int p){return p;}
        public View getView(int pos,View recycled,ViewGroup parent){
            LinearLayout row;TextView title,sub;
            if(recycled==null){row=column();row.setBackground(background(CARD));row.setPadding(dp(12),dp(8),dp(12),dp(8));title=text("",17,WHITE);sub=text("",12,MUTED);row.addView(title);row.addView(sub);row.setTag(new TextView[]{title,sub});}
            else{row=(LinearLayout)recycled;TextView[] views=(TextView[])row.getTag();title=views[0];sub=views[1];}
            DeviceRepository.App a=visible.get(pos);title.setText(a.label);sub.setText(a.pkg+"\n"+(a.total()<0?"Storage unavailable":size(a.total())+" total • "+size(a.cache)+" cache")+"\n"+(a.system?"System app":"User app")+(a.disabled?" • Disabled":""));return row;
        }
    }
    private void details(DeviceRepository.App a){
        String info=a.pkg+"\n\n"+(a.total()<0?"Storage sizes need optional Usage Access.":"App: "+size(a.code)+"\nData (includes cache): "+size(a.data)+"\nCache: "+size(a.cache));
        String[] options={"Storage details","Android App info","Clear cache only","Clear ALL app data","Force stop",a.disabled?"Unfreeze / enable":"Freeze / disable","Sleep (inactive)","Wake (active)"};
        new AlertDialog.Builder(this).setTitle(a.label).setItems(options,(d,index)->{
            if(index==0){message(a.label,info);return;}
            if(index==1){open(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,Uri.parse("package:"+a.pkg)));return;}
            if(bridge.busy()){message("Working","Wait for the current operation to finish.");return;}
            if(!bridge.ready()){navigate("Setup");message("Connect Shizuku","Complete the steps below, then tap Connect / authorize.");return;}
            String action=new String[]{"","","cache","data","stop",a.disabled?"unfreeze":"freeze","sleep","wake"}[index];
            String protectedReason=protectedReason(a,action);
            if(protectedReason!=null){message("Protected app",protectedReason);return;}
            String warning=action.equals("data")?"Permanently erase ALL local data, accounts, settings and files for "+a.label+"? This cannot be undone.":action.equals("freeze")?"Disable "+a.label+"? Its icon disappears and notifications stop. Restore it in Disabled. Unfreeze apps before uninstalling DATACLIAR.":action.equals("cache")?"Clear only cache for "+a.label+"? Android 13+ and OS command support are required. This never falls back to wiping app data.":action.equals("sleep")?"Mark "+a.label+" inactive? Android may wake it later. Notifications can be delayed.":options[index]+" for "+a.label+"? Stopping apps may delay notifications.";
            new AlertDialog.Builder(this).setTitle(options[index]).setMessage(warning).setNegativeButton("Cancel",null).setPositiveButton("Continue",(x,y)->{if(bridge.ready()&&!bridge.busy())bridge.perform(action,a.pkg,a.label,android.os.Process.myUid()/100000);else message("Not ready","Reconnect Shizuku or wait for the current operation.");}).show();
        }).setNegativeButton("Close",null).show();
    }
    private String protectedReason(DeviceRepository.App a,String action){
        if(a.pkg.equals(getPackageName())||a.pkg.equals(ShizukuBridge.MANAGER)||a.system)return "System apps, DATACLIAR and Shizuku are protected.";
        if(action.equals("unfreeze"))return null;
        try{
            android.content.pm.PackageManager pm=getPackageManager();
            android.content.pm.ResolveInfo home=pm.resolveActivity(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME),android.content.pm.PackageManager.MATCH_DEFAULT_ONLY);
            if(home!=null&&a.pkg.equals(home.activityInfo.packageName))return "Default launcher is protected.";
            String keyboard=Settings.Secure.getString(getContentResolver(),Settings.Secure.DEFAULT_INPUT_METHOD);
            if(keyboard!=null&&keyboard.startsWith(a.pkg+"/"))return "Active keyboard is protected.";
            java.util.List<ComponentName> admins=getSystemService(android.app.admin.DevicePolicyManager.class).getActiveAdmins();
            if(admins!=null)for(ComponentName admin:admins)if(a.pkg.equals(admin.getPackageName()))return "Device administrator is protected.";
            if(!pm.queryIntentServices(new Intent("android.net.VpnService").setPackage(a.pkg),android.content.pm.PackageManager.MATCH_DISABLED_COMPONENTS).isEmpty())return "VPN apps are protected.";
        }catch(Exception e){return "Could not verify essential-app protections. Use Android App info.";}
        return null;
    }
    private void access(){
        body.addView(text("Your permissions",27,WHITE));
        LinearLayout setup=card();setup.addView(text("Shizuku setup",23,CYAN));setup.addView(text(bridge.status(),15,WHITE));
        setup.addView(text("১. Official Shizuku একবার install করুন।\n২. Wi-Fi বা অন্য ফোনের hotspot-এ যুক্ত হন।\n৩. Developer options → USB debugging ও Wireless debugging চালু করুন।\n৪. Shizuku খুলে Pairing চাপুন। Settings-এর Pair device with pairing code থেকে code নিয়ে Shizuku notification-এ দিন, তারপর Start চাপুন।\n৫. এখানে ফিরে Connect / authorize চাপুন এবং Allow দিন।",15,WHITE));
        setup.addView(text("Shizuku আলাদা helper app; এই APK তার setup guide ও connection দেয়। Pairing ও Start official Shizuku-তেই হবে। Android 11+ এ wireless setup চলে; Android 8–10 এ PC/USB বা root প্রয়োজন।",13,MUTED));
        setup.addView(button("Install official Shizuku",()->open(new Intent(Intent.ACTION_VIEW,Uri.parse("https://shizuku.rikka.app/download/")))));
        setup.addView(button("Open Wi-Fi settings",()->open(new Intent(Settings.ACTION_WIFI_SETTINGS))));
        setup.addView(button("Open Developer options",this::developerOptions));
        setup.addView(button("Open Shizuku / pairing",this::openShizuku));
        setup.addView(button("Connect / authorize",()->bridge.connect(true)));
        setup.addView(button("Refresh connection status",()->bridge.connect(false)));
        LinearLayout keep=card();keep.addView(text("Use mobile data after setup",21,WHITE));
        keep.addView(text("Shizuku চলার পর Wi-Fi বন্ধ করে mobile data ব্যবহার করতে পারেন। DATACLIAR-এর controls ইন্টারনেট বা Wi-Fi পরীক্ষা করে না।\n\nDeveloper options ও USB debugging চালু রাখুন। Shizuku-এর background/battery setting Unrestricted করুন। অপশন থাকলে Disable adb authorization timeout চালু করুন।\n\nফোন restart, service বন্ধ, permission revoke বা OEM restriction হলে আবার Start/connect লাগতে পারে। Power off পর্যন্ত চলার নিশ্চয়তা নেই। DATACLIAR বন্ধ বা rotate করলে Shizuku বন্ধ করা হয় না।",14,MUTED));
        keep.addView(button("Shizuku app / battery settings",()->open(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,Uri.parse("package:"+ShizukuBridge.MANAGER)))));
        LinearLayout usage=card();usage.addView(text("Optional: Usage Access",21,WHITE));usage.addView(text(repo.usageAllowed()?"Granted — storage sizes available where Android permits.":"For per-app storage sizes only; separate from Shizuku authorization.",14,MUTED));usage.addView(button("Open Usage Access",()->open(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))));
        LinearLayout about=card();about.addView(text("On-device controls",21,WHITE));about.addView(text("Freeze/unfreeze, force stop, sleep/wake and explicit data clearing use Shizuku. Cache-only requires Android 13+ and actual OS support; no data-wipe fallback. System and essential apps are protected. Unfreeze apps before uninstalling DATACLIAR.\n\nNo Internet permission, ads, analytics or uploads. Setup/download links open externally. Version 1.2.0 preview.",14,MUTED));about.addView(button("Shizuku API license",this::license));
    }
    private void developerOptions(){
        Intent settings=new Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS);
        if(settings.resolveActivity(getPackageManager())!=null)open(settings);else open(new Intent(Settings.ACTION_DEVICE_INFO_SETTINGS));
    }
    private void openShizuku(){
        Intent launch=getPackageManager().getLaunchIntentForPackage(ShizukuBridge.MANAGER);
        if(launch==null){message("Install Shizuku first","Use Install official Shizuku above, then return here.");return;}open(launch);
    }
    private void license(){
        try(InputStream in=getAssets().open("shizuku-license.txt")){ByteArrayOutputStream bytes=new ByteArrayOutputStream();byte[] buffer=new byte[2048];int n;while((n=in.read(buffer))!=-1)bytes.write(buffer,0,n);message("Shizuku API · MIT",bytes.toString("UTF-8"));}catch(Exception e){message("Shizuku API","RikkaW · MIT License · github.com/RikkaApps/Shizuku-API");}
    }
    private void confirmOwnCache(){
        if(cleaning){message("Working","Cache cleanup is already running.");return;}
        new AlertDialog.Builder(this).setTitle("Clear DATACLIAR cache?").setMessage("Only this app's temporary cache files will be removed. Other apps and their data are untouched.").setNegativeButton("Cancel",null).setPositiveButton("Clear cache",(d,w)->{
            cleaning=true;status="Clearing own cache…";
            worker.execute(()->{final int count=CacheCleaner.clear(getCacheDir(),getExternalCacheDir());deliver(()->{cleaning=false;status=count==0?"DATACLIAR's cache cleared.":"Some cache files could not be removed. Try again.";render();message("Cache cleanup",status);});});
        }).show();
    }
    private void open(Intent intent){try{startActivity(intent);}catch(ActivityNotFoundException|SecurityException e){message("Settings unavailable","This device does not expose that settings screen. Open Android Settings → Apps or Storage manually.");}}
    private void message(String title,String value){if(!isDestroyed()&&!isFinishing())new AlertDialog.Builder(this).setTitle(title).setMessage(value).setPositiveButton("OK",null).show();}
    private String size(long bytes){return android.text.format.Formatter.formatShortFileSize(this,Math.max(0,bytes));}
}
