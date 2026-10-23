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

/** All operations work in the ordinary app sandbox. App management stays in Android Settings. */
public class MainActivity extends Activity {
    private static final int BG=0xff071321, CARD=0xff0f2437, CYAN=0xff55dfff, WHITE=0xffebf6ff, MUTED=0xff9db5ca;
    private final ExecutorService worker=Executors.newSingleThreadExecutor();
    private final Handler handler=new Handler(Looper.getMainLooper());
    private DeviceRepository repo;
    private String page="Home", query="", filter="All", status="Ready. No root or Shizuku required.";
    private boolean scanning, cleaning, resumed;
    private List<DeviceRepository.App> apps=new ArrayList<>();
    private LinearLayout body;
    private TextView ramText, storageText, countText;
    private ProgressBar ramBar, storageBar;
    private AppAdapter adapter;
    private final Runnable ticker=new Runnable(){public void run(){if(!resumed)return;updateMetrics();handler.postDelayed(this,2000);}};
    @Override public void onCreate(Bundle saved){
        super.onCreate(saved);repo=new DeviceRepository(this);
        if(saved!=null){page=saved.getString("page","Home");query=saved.getString("query","");filter=saved.getString("filter","All");status=saved.getString("status",status);}
        render();
    }
    @Override protected void onResume(){super.onResume();resumed=true;handler.removeCallbacks(ticker);handler.post(ticker);render();scan();}
    @Override protected void onPause(){resumed=false;handler.removeCallbacks(ticker);super.onPause();}
    @Override protected void onSaveInstanceState(Bundle out){out.putString("page",page);out.putString("query",query);out.putString("filter",filter);out.putString("status",status);super.onSaveInstanceState(out);}
    @Override protected void onDestroy(){worker.shutdownNow();handler.removeCallbacksAndMessages(null);super.onDestroy();}
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
        root.addView(text("No root • No Shizuku • On-device only",12,CYAN));
        body=column();body.setPadding(dp(12),dp(8),dp(12),0);
        if(page.equals("Apps")||page.equals("Disabled"))root.addView(body,new LinearLayout.LayoutParams(-1,0,1));
        else{ScrollView scroll=new ScrollView(this);scroll.addView(body);root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));}
        LinearLayout nav=new LinearLayout(this);
        for(String tab:new String[]{"Home","Apps","Disabled","Access"}){Button b=button(tab,()->navigate(tab));b.setTextSize(11);b.setTextColor(page.equals(tab)?CYAN:MUTED);nav.addView(b,new LinearLayout.LayoutParams(0,-2,1));}
        root.addView(nav);setContentView(root);root.requestApplyInsets();
        if(page.equals("Home"))home();else if(page.equals("Access"))access();else appPage();
    }
    private ProgressBar meter(LinearLayout parent){ProgressBar b=new ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal);b.setMax(1000);b.setProgressTintList(android.content.res.ColorStateList.valueOf(CYAN));parent.addView(b,new LinearLayout.LayoutParams(-1,dp(12)));return b;}
    private void home(){
        body.addView(text("Your phone. In focus.",27,WHITE));body.addView(text(Build.MANUFACTURER+" "+Build.MODEL+" • Android "+Build.VERSION.RELEASE,13,MUTED));
        LinearLayout ram=card();ram.addView(text("MEMORY / RAM",12,CYAN));ramText=text("Reading…",21,WHITE);ram.addView(ramText);ramBar=meter(ram);ram.addView(text("Available memory includes reclaimable memory.",12,MUTED));
        LinearLayout disk=card();disk.addView(text("INTERNAL STORAGE",12,CYAN));storageText=text("Reading…",21,WHITE);disk.addView(storageText);storageBar=meter(disk);disk.addView(text("Usable data partition, not advertised capacity.",12,MUTED));
        LinearLayout cpu=card();cpu.addView(text("PROCESSOR",12,CYAN));cpu.addView(text(Runtime.getRuntime().availableProcessors()+" available cores • "+Build.SUPPORTED_ABIS[0],17,WHITE));cpu.addView(text("System-wide CPU load and other apps' live processes are restricted by Android.",12,MUTED));
        LinearLayout actions=card();actions.addView(text("Storage tools",21,WHITE));actions.addView(button("Manage app storage",()->navigate("Apps")));actions.addView(button("Open device storage",()->open(new Intent(Settings.ACTION_INTERNAL_STORAGE_SETTINGS))));actions.addView(button("Clear DATACLIAR's own cache",this::confirmOwnCache));actions.addView(text("To clear another app's cache/data, force stop or disable it, select the app and continue in Android Settings. Automatic batch controls are unavailable without elevated access.",13,MUTED));
        LinearLayout result=card();result.addView(text("STATUS",12,CYAN));result.addView(text(status,13,MUTED));updateMetrics();
    }
    private void updateMetrics(){
        if(!page.equals("Home")||ramText==null)return;
        ActivityManager.MemoryInfo m=new ActivityManager.MemoryInfo();getSystemService(ActivityManager.class).getMemoryInfo(m);
        ramText.setText(size(m.availMem)+" free / "+size(m.totalMem));ramBar.setProgress(m.totalMem>0?(int)(1000*(1.0-(double)m.availMem/m.totalMem)):0);
        try{StatFs fs=new StatFs(Environment.getDataDirectory().getAbsolutePath());storageText.setText(size(fs.getAvailableBytes())+" free / "+size(fs.getTotalBytes()));storageBar.setProgress(fs.getTotalBytes()>0?(int)(1000*(1.0-(double)fs.getAvailableBytes()/fs.getTotalBytes())):0);}catch(Exception e){storageText.setText("Storage unavailable");}
    }
    private void appPage(){
        body.addView(text(page.equals("Disabled")?"Disabled apps":"Your app collection",25,WHITE));
        body.addView(text("Current Android profile • manage each app in Settings",12,MUTED));
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
        worker.execute(()->{try{List<DeviceRepository.App> found=repo.apps();deliver(()->{apps=found;scanning=false;status="App list refreshed.";showApps();});}catch(InterruptedException e){Thread.currentThread().interrupt();}catch(Exception e){deliver(()->{scanning=false;status="Scan failed. Try refresh.";showApps();});}});
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
        String info=a.pkg+"\n\n"+(a.total()<0?"Storage sizes need Usage Access and OS support.":"App: "+size(a.code)+"\nData (includes cache): "+size(a.data)+"\nCache: "+size(a.cache))+"\n\nIn Android App info:\n• Storage & cache → Clear cache\n• Clear storage/data resets local app data\n• Force stop, Disable or Enable where Android permits\n\nAndroid decides which controls are available. No action has been performed by opening this screen.";
        new AlertDialog.Builder(this).setTitle(a.label).setMessage(info).setPositiveButton("Open App info",(d,w)->open(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,Uri.parse("package:"+a.pkg)))).setNegativeButton("Close",null).show();
    }
    private void access(){
        body.addView(text("Your permissions",27,WHITE));
        LinearLayout basic=card();basic.addView(text("Works immediately",21,WHITE));basic.addView(text("RAM, device storage, processor details, installed apps, search and Android App info shortcuts work without root or Shizuku. No setup service is required.",14,MUTED));
        LinearLayout usage=card();usage.addView(text("Optional: Usage Access",21,WHITE));usage.addView(text(repo.usageAllowed()?"Granted. Per-app storage is available where Android permits.":"Enable only if you want per-app storage sizes. The app remains usable without it.",14,MUTED));usage.addView(button("Open Usage Access",()->open(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))));
        LinearLayout about=card();about.addView(text("Android controls stay yours",21,WHITE));about.addView(text("Other apps' cache/data, force stop and disable/enable are managed manually in Android Settings. No automatic freeze, boost, live process count or batch cache clearing is claimed. Disabled apps can be restored in App info when Android offers Enable.\n\nNo Internet permission, ads, analytics, uploads or background monitoring. All displayed data stays on this device.\n\nVersion 1.1.0 • no-root preview",14,MUTED));
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
