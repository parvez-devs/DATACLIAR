package com.axiom.datacliar;

import android.app.Instrumentation;
import android.content.Intent;
import android.graphics.Bitmap;
import android.view.*;
import android.widget.*;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.*;
import org.junit.runner.RunWith;
import java.io.*;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class NavigationTest {
    @Test public void launchesWithoutPrivilegesAndNavigatesAllTabs() throws Exception {
        Instrumentation inst=InstrumentationRegistry.getInstrumentation();
        MainActivity activity=(MainActivity)inst.startActivitySync(new Intent(inst.getTargetContext(),MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        inst.waitForIdleSync();
        try {
            assertScreen(inst,activity,"Your phone. In focus.");
            screenshot(inst,"home");
            tap(inst,activity,"Apps");assertScreen(inst,activity,"Your app collection");screenshot(inst,"apps");
            tap(inst,activity,"Disabled");assertScreen(inst,activity,"Disabled apps");screenshot(inst,"disabled");
            tap(inst,activity,"Setup");assertScreen(inst,activity,"Your permissions");assertScreen(inst,activity,"Shizuku setup");assertScreen(inst,activity,"Connect / authorize");screenshot(inst,"setup");
            tap(inst,activity,"Connect / authorize");assertScreen(inst,activity,"Shizuku setup");
            tap(inst,activity,"Home");assertScreen(inst,activity,"Your phone. In focus.");
        } finally {inst.runOnMainSync(activity::finish);}
    }
    @Test public void recreationPreservesSearchAndAppHasNoInternetPermission() throws Exception {
        Instrumentation inst=InstrumentationRegistry.getInstrumentation();
        String pkg=inst.getTargetContext().getPackageName();
        android.content.pm.PackageInfo info=inst.getTargetContext().getPackageManager().getPackageInfo(pkg,android.content.pm.PackageManager.GET_PERMISSIONS);
        if(info.requestedPermissions!=null)for(String permission:info.requestedPermissions){assertNotEquals("android.permission.INTERNET",permission);}
        assertNotNull(Class.forName("rikka.shizuku.Shizuku"));
        MainActivity activity=(MainActivity)inst.startActivitySync(new Intent(inst.getTargetContext(),MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        inst.waitForIdleSync();tap(inst,activity,"Apps");
        inst.runOnMainSync(()->{EditText search=findSearch(activity.getWindow().getDecorView());assertNotNull(search);search.setText("saved-query");});
        Instrumentation.ActivityMonitor monitor=inst.addMonitor(MainActivity.class.getName(),null,false);
        MainActivity restored=null;
        try{
            inst.runOnMainSync(activity::recreate);
            restored=(MainActivity)monitor.waitForActivityWithTimeout(10000);assertNotNull("Recreated Activity",restored);inst.waitForIdleSync();
            MainActivity current=restored;
            inst.runOnMainSync(()->{assertNotNull(find(current.getWindow().getDecorView(),"Your app collection",false));assertEquals("saved-query",findSearch(current.getWindow().getDecorView()).getText().toString());});
        }finally{inst.removeMonitor(monitor);MainActivity current=restored==null?activity:restored;inst.runOnMainSync(current::finish);}
    }
    private EditText findSearch(View view){
        if(view instanceof EditText)return (EditText)view;
        if(view instanceof ViewGroup){ViewGroup group=(ViewGroup)view;for(int i=0;i<group.getChildCount();i++){EditText found=findSearch(group.getChildAt(i));if(found!=null)return found;}}
        return null;
    }
    private void tap(Instrumentation inst,MainActivity activity,String label) {
        inst.runOnMainSync(()->{TextView v=find(activity.getWindow().getDecorView(),label,true);assertNotNull(label,v);assertTrue(v.performClick());});inst.waitForIdleSync();
    }
    private void assertScreen(Instrumentation inst,MainActivity activity,String label) {
        inst.runOnMainSync(()->assertNotNull(label,find(activity.getWindow().getDecorView(),label,false)));
    }
    private TextView find(View view,String label,boolean button) {
        if(view instanceof TextView && (!button||view instanceof Button) && ((TextView)view).getText().toString().equals(label))return (TextView)view;
        if(view instanceof ViewGroup){ViewGroup g=(ViewGroup)view;for(int i=0;i<g.getChildCount();i++){TextView found=find(g.getChildAt(i),label,button);if(found!=null)return found;}}
        return null;
    }
    private void screenshot(Instrumentation inst,String name)throws Exception {
        Bitmap bitmap=inst.getUiAutomation().takeScreenshot();assertNotNull(bitmap);
        File dir=new File(inst.getTargetContext().getExternalFilesDir(null),"screenshots");assertTrue(dir.isDirectory()||dir.mkdirs());
        try(FileOutputStream out=new FileOutputStream(new File(dir,name+".png"))){assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG,100,out));}
        bitmap.recycle();
    }
}
