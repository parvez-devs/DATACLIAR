package com.axiom.datacliar;

import android.content.*;
import org.json.*;
import java.util.*;

/** Local-only user preferences and bounded durable operation journal. */
public final class AppStore {
    private static AppStore instance;
    public static synchronized AppStore get(Context c){if(instance==null)instance=new AppStore(c.getApplicationContext());return instance;}
    private final SharedPreferences preferences;
    private AppStore(Context context){
        preferences=context.getSharedPreferences("workspace",Context.MODE_PRIVATE);
        try{JSONArray all=records();boolean changed=false;for(int i=0;i<all.length();i++){JSONObject item=all.getJSONObject(i);if("running".equals(item.optString("status"))){item.put("status","interrupted");item.put("result","App process ended before a result was saved. Refresh the app state; this operation was not retried.");changed=true;}}if(changed)save(all);}catch(JSONException ignored){}
    }
    public synchronized boolean favourite(String pkg){return favourites().contains(pkg);}
    public synchronized Set<String> favourites(){return new HashSet<>(preferences.getStringSet("favourites",Collections.emptySet()));}
    public synchronized void toggleFavourite(String pkg){Set<String> values=favourites();if(!values.remove(pkg))values.add(pkg);preferences.edit().putStringSet("favourites",values).apply();}
    public synchronized String begin(String action,String pkg,String label){
        String id=UUID.randomUUID().toString();
        try{JSONArray all=records();JSONObject item=new JSONObject().put("id",id).put("time",System.currentTimeMillis()).put("action",action).put("package",pkg).put("label",label).put("status","running").put("result","Operation started");all.put(item);save(all);}catch(JSONException ignored){}
        return id;
    }
    public synchronized void finish(String id,String status,String result){
        try{JSONArray all=records();for(int i=0;i<all.length();i++){JSONObject item=all.getJSONObject(i);if(id.equals(item.optString("id"))){item.put("status",status).put("result",result);break;}}save(all);}catch(JSONException ignored){}
    }
    public synchronized List<JSONObject> history(){List<JSONObject> out=new ArrayList<>();JSONArray all=records();for(int i=all.length()-1;i>=0;i--)if(all.optJSONObject(i)!=null)out.add(all.optJSONObject(i));return out;}
    public synchronized void clearHistory(){preferences.edit().remove("history").apply();}
    private JSONArray records(){try{return new JSONArray(preferences.getString("history","[]"));}catch(JSONException e){return new JSONArray();}}
    private void save(JSONArray records){JSONArray limited=new JSONArray();for(int i=Math.max(0,records.length()-200);i<records.length();i++)limited.put(records.opt(i));preferences.edit().putString("history",limited.toString()).commit();}
}
