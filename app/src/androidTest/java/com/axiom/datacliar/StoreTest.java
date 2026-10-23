package com.axiom.datacliar;
import android.content.*;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.*;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;
import java.io.*;

@RunWith(AndroidJUnit4.class)
public class StoreTest {
    @Test public void favouritesAndOperationResultsPersistLocally(){
        Context context=InstrumentationRegistry.getInstrumentation().getTargetContext();AppStore store=AppStore.get(context);
        String pkg="com.test.favourite";boolean before=store.favourite(pkg);
        try{
            store.toggleFavourite(pkg);assertEquals(!before,store.favourite(pkg));
            String id=store.begin("freeze",pkg,"Test app");store.finish(id,"failed","ERROR: permission denied");
            assertEquals("failed",store.history().get(0).optString("status"));assertEquals("ERROR: permission denied",store.history().get(0).optString("result"));
            assertTrue(context.getSharedPreferences("workspace",Context.MODE_PRIVATE).getString("history","").contains(id));
        }finally{if(store.favourite(pkg)!=before)store.toggleFavourite(pkg);store.clearHistory();}
    }
    @Test public void updateProviderExposesOnlyOneReadOnlyFile()throws Exception{
        Context context=InstrumentationRegistry.getInstrumentation().getTargetContext();File dir=new File(context.getCacheDir(),"updates");assertTrue(dir.isDirectory()||dir.mkdirs());File apk=new File(dir,"update.apk");
        try{
            try(FileOutputStream out=new FileOutputStream(apk)){out.write(new byte[]{1,2,3});}
            Uri allowed=Uri.parse("content://"+context.getPackageName()+".updates/update.apk");
            try(ParcelFileDescriptor fd=context.getContentResolver().openFileDescriptor(allowed,"r")){assertNotNull(fd);assertEquals(3,fd.getStatSize());}
            assertThrows(FileNotFoundException.class,()->context.getContentResolver().openFileDescriptor(allowed,"w"));
            assertThrows(FileNotFoundException.class,()->context.getContentResolver().openFileDescriptor(Uri.parse("content://"+context.getPackageName()+".updates/private.txt"),"r"));
        }finally{apk.delete();}
    }
}
