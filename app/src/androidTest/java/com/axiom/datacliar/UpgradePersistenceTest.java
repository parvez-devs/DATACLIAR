package com.axiom.datacliar;
import android.content.Context;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

/** CI invokes seedData on a signed version-3 fixture, upgrades with adb install -r, then invokes verifyData. */
@RunWith(AndroidJUnit4.class)
public class UpgradePersistenceTest {
    @Test public void seedData(){
        Context c=InstrumentationRegistry.getInstrumentation().getTargetContext();AppStore store=AppStore.get(c);
        if(!store.favourite("com.upgrade.saved"))store.toggleFavourite("com.upgrade.saved");
        String id=store.begin("freeze","com.upgrade.saved","Upgrade sentinel");store.finish(id,"completed","UPGRADE_SENTINEL");
        store.begin("stop","com.upgrade.interrupted","Interrupted sentinel");
        c.getSharedPreferences("updates",Context.MODE_PRIVATE).edit().putBoolean("checkOnLaunch",false).commit();
        assertTrue(store.favourite("com.upgrade.saved"));
    }
    @Test public void verifyData()throws Exception{
        Context c=InstrumentationRegistry.getInstrumentation().getTargetContext();AppStore store=AppStore.get(c);
        assertEquals(Integer.parseInt(InstrumentationRegistry.getArguments().getString("expectedVersionCode","4")),c.getPackageManager().getPackageInfo(c.getPackageName(),0).versionCode);
        assertTrue(store.favourite("com.upgrade.saved"));
        boolean preserved=false,recovered=false;
        for(org.json.JSONObject event:store.history()){
            if("UPGRADE_SENTINEL".equals(event.optString("result")))preserved=true;
            if("com.upgrade.interrupted".equals(event.optString("package"))&&"interrupted".equals(event.optString("status")))recovered=true;
        }
        assertTrue("History preserved",preserved);assertTrue("Interrupted operation was not retried",recovered);
    }
}
