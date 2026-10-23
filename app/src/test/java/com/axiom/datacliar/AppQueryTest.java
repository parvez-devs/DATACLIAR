package com.axiom.datacliar;
import org.junit.Test;
import static org.junit.Assert.*;
public class AppQueryTest {
    @Test public void searchesLabelAndPackageIgnoringCaseAndWhitespace(){
        assertTrue(AppQuery.matches("Camera","com.vendor.camera"," CAMERA ","All",false,false));
        assertTrue(AppQuery.matches("Photos","com.vendor.gallery","VENDOR","All",false,false));
        assertFalse(AppQuery.matches("Camera","com.vendor.camera","clock","All",false,false));
    }
    @Test public void filtersRemainIndependentOfSearch(){
        assertFalse(AppQuery.matches("Camera","com.camera","","User apps",true,false));
        assertTrue(AppQuery.matches("Camera","com.camera","","System apps",true,false));
        assertFalse(AppQuery.matches("Camera","com.camera","","Disabled",false,false));
        assertTrue(AppQuery.matches("Camera","com.camera","camera","Disabled",false,true));
    }
    @Test public void unknownStorageIsNotCountedAsZero(){
        DeviceRepository.App app=new DeviceRepository.App();assertEquals(-1,app.total());
        app.code=100;assertEquals(-1,app.total());app.data=200;app.cache=50;assertEquals(300,app.total());
    }
}
