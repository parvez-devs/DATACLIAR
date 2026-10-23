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
    private AppStore store;
    private UpdateManager updates;
    private final Runnable updateChanged=()->deliver(this::render);
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
        super.onCreate(saved);repo=new DeviceRepository(this);bridge=ShizukuBridge.get(this);bridge.observe(connectionChanged);store=AppStore.get(this);updates=UpdateManager.get(this);updates.observe(updateChanged);
        if(saved!=null){page=saved.getString("page","Home");query=saved.getString("query","");filter=saved.getString("filter","All");status=saved.getString("status",status);}
        if(page.equals("Access"))page="Setup";render();
    }
    @Override protected void onResume(){super.onResume();resumed=true;handler.removeCallbacks(ticker);handler.post(ticker);bridge.connect(false);updates.checkOnLaunch();render();scan();}
    @Override protected void onPause(){resumed=false;handler.removeCallbacks(ticker);super.onPause();}
    @Override protected void onSaveInstanceState(Bundle out){out.putString("page",page);out.putString("query",query);out.putString("filter",filter);out.putString("status",status);super.onSaveInstanceState(out);}
    @Override protected void onDestroy(){bridge.removeObserver(connectionChanged);updates.removeObserver(updateChanged);worker.shutdownNow();handler.removeCallbacksAndMessages(null);super.onDestroy();}
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
        if(page.equals("Apps")||page.equals("Disabled")||page.equals("History"))root.addView(body,new LinearLayout.LayoutParams(-1,0,1));
        else{ScrollView scroll=new ScrollView(this);scroll.addView(body);root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));}
        LinearLayout nav=new LinearLayout(this);
        for(String tab:new String[]{"Home","Apps","Disabled","Setup"}){Button b=button(tab,()->navigate(tab));b.setTextSize(11);b.setTextColor(page.equals(tab)?CYAN:MUTED);nav.addView(b,new LinearLayout.LayoutParams(0,-2,1));}
        root.addView(nav);setContentView(root);root.requestApplyInsets();
        if(page.equals("Home"))home();else if(page.equals("Setup"))access();else if(page.equals("History"))historyPage();else if(page.equals("Updates"))updatesPage();else appPage();
    }
    private ProgressBar meter(LinearLayout parent){ProgressBar b=new ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal);b.setMax(1000);b.setProgressTintList(android.content.res.ColorStateList.valueOf(CYAN));parent.addView(b,new LinearLayout.LayoutParams(-1,dp(12)));return b;}
    private void home(){
        body.addView(text("Your phone. In focus.",27,WHITE));if(updates.hasUpdate())body.addView(button("Update available • "+updates.availableVersion(),()->navigate("Updates")));body.addView(text(Build.MANUFACTURER+" "+Build.MODEL+" • Android "+Build.VERSION.RELEASE,13,MUTED));
        LinearLayout ram=card();ram.addView(text("MEMORY / RAM",12,CYAN));ramText=text("Reading…",21,WHITE);ram.addView(ramText);ramBar=meter(ram);ram.addView(text("Available memory includes reclaimable memory.",12,MUTED));
        LinearLayout disk=card();disk.addView(text("INTERNAL STORAGE",12,CYAN));storageText=text("Reading…",21,WHITE);disk.addView(storageText);storageBar=meter(disk);disk.addView(text("Usable data partition, not advertised capacity.",12,MUTED));
        LinearLayout cpu=card();cpu.addView(text("PROCESSOR",12,CYAN));cpu.addView(text(Runtime.getRuntime().availableProcessors()+" available cores • "+Build.SUPPORTED_ABIS[0],17,WHITE));cpu.addView(text("System-wide CPU load and other apps' live processes are restricted by Android.",12,MUTED));
        LinearLayout actions=card();actions.addView(text("Storage tools",21,WHITE));actions.addView(button("Manage app storage",()->navigate("Apps")));actions.addView(button("Open device storage",()->open(new Intent(Settings.ACTION_INTERNAL_STORAGE_SETTINGS))));actions.addView(button("Clear DATACLIAR's own cache",this::confirmOwnCache));actions.addView(button("Shizuku setup / connect",()->navigate("Setup")));actions.addView(button("★ Favourites",()->{filter="Favourites";query="";navigate("Apps");}));actions.addView(button("Operation history",()->navigate("History")));actions.addView(button("App updates",()->navigate("Updates")));actions.addView(text("Connect Shizuku once to freeze/unfreeze, stop, sleep/wake and clear eligible app data inside DATACLIAR. Cache-only support depends on Android. Dashboard works without Shizuku.",13,MUTED));
        LinearLayout result=card();result.addView(text("STATUS",12,CYAN));result.addView(text(status+"\n\n"+bridge.lastResult(),13,MUTED));if(bridge.busy())result.addView(button("Stop after current app",bridge::cancelBatch));updateMetrics();
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
            Spinner spinner=new Spinner(this);String[] filters={"All","Favourites","User apps","System apps"};spinner.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,filters));spinner.setSelection(Math.max(0,Arrays.asList(filters).indexOf(filter)));
            spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){public void onNothingSelected(AdapterView<?> p){}public void onItemSelected(AdapterView<?> p,View v,int pos,long id){filter=filters[pos];showApps();}});body.addView(spinner);
        }
        EditText search=new EditText(this);search.setSingleLine(true);search.setTextColor(WHITE);search.setHintTextColor(MUTED);search.setHint("Search app or package");search.setContentDescription("Search installed apps");search.setText(query);body.addView(search);
        search.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence s,int a,int c,int f){}public void afterTextChanged(Editable e){}public void onTextChanged(CharSequence s,int a,int b,int c){query=s.toString();showApps();}});
        countText=text("",12,CYAN);body.addView(countText);body.addView(button("Batch controls",this::chooseBatch));body.addView(button("Refresh apps & storage",this::scan));
        ListView list=new ListView(this);list.setDividerHeight(dp(6));list.setCacheColorHint(BG);adapter=new AppAdapter();list.setAdapter(adapter);list.setOnItemClickListener((p,v,pos,id)->details(adapter.visible.get(pos)));body.addView(list,new LinearLayout.LayoutParams(-1,0,1));showApps();
    }
    private void showApps(){if(adapter==null)return;adapter.visible.clear();String selected=page.equals("Disabled")?"Disabled":filter;for(DeviceRepository.App a:apps)if(AppQuery.matches(a.label,a.pkg,query,selected,a.system,a.disabled)&&(!selected.equals("Favourites")||store.favourite(a.pkg)))adapter.visible.add(a);adapter.notifyDataSetChanged();countText.setText(scanning?"Scanning…":adapter.visible.size()+" apps shown • "+status);}
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
            DeviceRepository.App a=visible.get(pos);title.setText((store.favourite(a.pkg)?"★ ":"")+a.label);sub.setText(a.pkg+"\n"+(a.total()<0?"Storage unavailable":size(a.total())+" total • "+size(a.cache)+" cache")+"\n"+(a.system?"System app":"User app")+(a.disabled?" • Disabled":""));return row;
        }
    }
    private void details(DeviceRepository.App a){
        String info=a.pkg+"\n\n"+(a.total()<0?"Storage sizes need optional Usage Access.":"App: "+size(a.code)+"\nData (includes cache): "+size(a.data)+"\nCache: "+size(a.cache));
        String[] options={"Storage details","Android App info","Clear cache only","Clear ALL app data","Force stop",a.disabled?"Unfreeze / enable":"Freeze / disable","Sleep (OS-dependent)","Wake (active)",store.favourite(a.pkg)?"Remove favourite":"Add favourite"};
        new AlertDialog.Builder(this).setTitle(a.label).setItems(options,(d,index)->{
            if(index==8){store.toggleFavourite(a.pkg);showApps();return;}
            if(index==0){message(a.label,info);return;}
            if(index==1){open(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,Uri.parse("package:"+a.pkg)));return;}
            if(bridge.busy()){message("Working","Wait for the current operation to finish.");return;}
            if(!bridge.ready()){navigate("Setup");message("Connect Shizuku","Complete the steps below, then tap Connect / authorize.");return;}
            String action=new String[]{"","","cache","data","stop",a.disabled?"unfreeze":"freeze","sleep","wake"}[index];
            String protectedReason=protectedReason(a,action);
            if(protectedReason!=null){message("Protected app",protectedReason);return;}
            String warning=action.equals("data")?"Permanently erase ALL local data, accounts, settings and files for "+a.label+"? This cannot be undone.":action.equals("freeze")?"Disable "+a.label+"? Its icon disappears and notifications stop. Restore it in Disabled. Unfreeze apps before uninstalling DATACLIAR.":action.equals("cache")?"Clear only cache for "+a.label+"? Android 13+ and OS command support are required. This never falls back to wiping app data.":action.equals("sleep")?"Mark "+a.label+" inactive? Android may refuse or override this hint, especially while charging or recently used. This is not permanent freezing. Notifications can be delayed.":options[index]+" for "+a.label+"? Stopping apps may delay notifications.";
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
        body.addView(text("Your permissions",27,WHITE));body.addView(button("History & results",()->navigate("History")));body.addView(button("Updates & version",()->navigate("Updates")));
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
        LinearLayout about=card();about.addView(text("On-device controls",21,WHITE));about.addView(text("Freeze/unfreeze, force stop, sleep/wake and explicit data clearing use Shizuku. Cache-only requires Android 13+ and actual OS support; no data-wipe fallback. System and essential apps are protected. Unfreeze apps before uninstalling DATACLIAR.\n\nNo ads, analytics or device-data uploads. Internet is used only for optional GitHub update checks/downloads. Android asks before installing an update. Version 1.3.0.",14,MUTED));about.addView(button("Shizuku API license",this::license));
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
    private void chooseBatch(){
        if(bridge.busy()){message("Working",bridge.lastResult());return;}
        if(!bridge.ready()){navigate("Setup");message("Connect Shizuku","Authorize DATACLIAR first, then choose apps for a batch.");return;}
        if(scanning){message("Scanning","Wait for the app list to finish refreshing.");return;}
        new AlertDialog.Builder(this).setTitle("Batch controls").setItems(new String[]{"Freeze selected apps","Unfreeze selected apps","Force stop selected apps"},(dialog,choice)->selectBatch(new String[]{"freeze","unfreeze","stop"}[choice])).setNegativeButton("Cancel",null).show();
    }
    private void selectBatch(String action){
        List<DeviceRepository.App> candidates=new ArrayList<>();
        List<DeviceRepository.App> visible=adapter==null?apps:new ArrayList<>(adapter.visible);
        for(DeviceRepository.App app:visible)if(protectedReason(app,action)==null&&(!action.equals("freeze")||!app.disabled)&&(!action.equals("unfreeze")||app.disabled))candidates.add(app);
        if(candidates.isEmpty()){message("No eligible apps","Current search/filter has no eligible apps for this action.");return;}
        String[] names=candidates.stream().map(a->a.label+" • "+a.pkg).toArray(String[]::new);boolean[] selected=new boolean[names.length];
        new AlertDialog.Builder(this).setTitle("Choose apps • "+action).setMultiChoiceItems(names,selected,(d,index,value)->selected[index]=value).setNegativeButton("Cancel",null).setPositiveButton("Review batch",(d,w)->{
            List<BatchTarget> targets=new ArrayList<>();for(int i=0;i<candidates.size();i++)if(selected[i])targets.add(new BatchTarget(candidates.get(i).pkg,candidates.get(i).label));
            if(targets.isEmpty()||targets.size()>200){message("Select apps","Select between 1 and 200 apps.");return;}
            String warning=action.equals("freeze")?"Their icons disappear and notifications stop. Restore them through Disabled. Unfreeze before uninstalling DATACLIAR.":action.equals("stop")?"Notifications may be delayed. This does not guarantee a speed improvement.":"Disabled user apps will be enabled again.";
            new AlertDialog.Builder(this).setTitle(action+" "+targets.size()+" apps?").setMessage(warning+"\n\nEach result is recorded in History. Stop after current app cancels the remaining queue.").setNegativeButton("Cancel",null).setPositiveButton("Run batch",(x,y)->{
                if(!bridge.ready()||bridge.busy()){message("Not ready","Reconnect Shizuku or wait for the current job.");return;}
                navigate("Home");bridge.performBatch(action,targets,android.os.Process.myUid()/100000);
            }).show();
        }).show();
    }
    private void historyPage(){
        body.addView(text("Operation history",25,WHITE));body.addView(text("Last 200 results • stored only on this device",12,MUTED));
        if(bridge.busy()){body.addView(text(bridge.lastResult(),13,CYAN));body.addView(button("Stop after current app",bridge::cancelBatch));}
        List<org.json.JSONObject> events=store.history();
        body.addView(button("Clear history",()->{if(bridge.busy()){message("Working","Wait until the current operation finishes.");return;}new AlertDialog.Builder(this).setTitle("Clear history?").setMessage("Only local operation records will be removed. App states and favourites stay unchanged.").setNegativeButton("Cancel",null).setPositiveButton("Clear",(d,w)->{store.clearHistory();render();}).show();}));
        List<String> rows=new ArrayList<>();java.text.DateFormat date=java.text.DateFormat.getDateTimeInstance(java.text.DateFormat.SHORT,java.text.DateFormat.SHORT);
        for(org.json.JSONObject item:events)rows.add(item.optString("label")+" • "+item.optString("action")+"\n"+item.optString("status")+" • "+date.format(new Date(item.optLong("time"))));
        if(rows.isEmpty())body.addView(text("No operations recorded yet.",16,MUTED));
        ListView list=new ListView(this);list.setAdapter(new ArrayAdapter<String>(this,android.R.layout.simple_list_item_1,rows){@Override public View getView(int p,View reused,ViewGroup parent){TextView view=(TextView)super.getView(p,reused,parent);view.setTextColor(WHITE);view.setTextSize(15);view.setPadding(dp(10),dp(12),dp(10),dp(12));return view;}});
        list.setOnItemClickListener((p,v,index,id)->{org.json.JSONObject event=events.get(index);message(event.optString("label"),event.optString("package")+"\n"+event.optString("action")+" • "+event.optString("status")+"\n\n"+event.optString("result"));});body.addView(list,new LinearLayout.LayoutParams(-1,0,1));
    }
    private void updatesPage(){
        body.addView(text("App updates",27,WHITE));
        LinearLayout current=card();current.addView(text("Installed: "+updates.versionName()+" ("+updates.versionCode()+")",19,WHITE));current.addView(text(updates.status(),14,MUTED));current.addView(text(updates.availableVersion(),14,CYAN));
        current.addView(button("Check for updates",updates::check));
        Switch automatic=new Switch(this);automatic.setText("Check on launch • at most once daily");automatic.setTextColor(WHITE);automatic.setChecked(updates.automatic());automatic.setOnCheckedChangeListener((v,checked)->updates.automatic(checked));current.addView(automatic);
        if(updates.hasUpdate())current.addView(button("Download verified update",updates::download));
        if(updates.downloaded())current.addView(button("Install downloaded update",()->{
            if(!getPackageManager().canRequestPackageInstalls()){open(new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,Uri.parse("package:"+getPackageName())));return;}
            try{open(updates.installIntent());}catch(Exception e){message("Update unavailable",e.getMessage());}
        }));
        LinearLayout details=card();details.addView(text("Safe upgrades",21,WHITE));details.addView(text("Updates use the same private signing key from version 1.3 onward. Favourites, history and settings remain when Android accepts an in-place update. Older debug previews may need a one-time reinstall: unfreeze apps first.\n\nOnly this project's GitHub release assets are accepted. Downloads are checked against SHA-256, package name, version and the installed signing certificate. Android's installer always asks for confirmation.\n\nUpdate checks/downloads contact GitHub; no device inventory or operation history is uploaded. Checks on launch are off until you enable them.",14,MUTED));details.addView(button("View official releases",()->open(new Intent(Intent.ACTION_VIEW,Uri.parse(UpdatePolicy.REPO+"/releases")))));
    }
    private void confirmOwnCache(){
        if(cleaning||updates.busy()){message("Working","Wait for cache cleanup or the current update task to finish.");return;}
        new AlertDialog.Builder(this).setTitle("Clear DATACLIAR cache?").setMessage("Only this app's temporary cache files will be removed. Other apps and their data are untouched.").setNegativeButton("Cancel",null).setPositiveButton("Clear cache",(d,w)->{
            cleaning=true;status="Clearing own cache…";
            worker.execute(()->{final int count=CacheCleaner.clear(getCacheDir(),getExternalCacheDir());deliver(()->{cleaning=false;status=count==0?"DATACLIAR's cache cleared.":"Some cache files could not be removed. Try again.";render();message("Cache cleanup",status);});});
        }).show();
    }
    private void open(Intent intent){try{startActivity(intent);}catch(ActivityNotFoundException|SecurityException e){message("Settings unavailable","This device does not expose that settings screen. Open Android Settings → Apps or Storage manually.");}}
    private void message(String title,String value){if(!isDestroyed()&&!isFinishing())new AlertDialog.Builder(this).setTitle(title).setMessage(value).setPositiveButton("OK",null).show();}
    private String size(long bytes){return android.text.format.Formatter.formatShortFileSize(this,Math.max(0,bytes));}
}
