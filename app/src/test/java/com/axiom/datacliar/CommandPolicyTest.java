package com.axiom.datacliar;
import org.junit.Test;
import static org.junit.Assert.*;
public class CommandPolicyTest {
    @Test public void refusesUnsupportedCacheWithoutDataFallback(){
        assertThrows(UnsupportedOperationException.class,()->CommandPolicy.command("cache","com.example.app",0,false));
        assertArrayEquals(new String[]{"pm","clear","--user","0","--cache-only","com.example.app"},CommandPolicy.command("cache","com.example.app",0,true));
    }
    @Test public void rejectsInjectionUnknownActionsAndInvalidProfiles(){
        assertThrows(IllegalArgumentException.class,()->CommandPolicy.command("freeze","com.app; reboot",0,true));
        assertThrows(IllegalArgumentException.class,()->CommandPolicy.command("reboot","com.app",0,true));
        assertThrows(IllegalArgumentException.class,()->CommandPolicy.command("stop","com.app",-1,true));
    }
    @Test public void freezeAndRestoreAreExplicitlyProfileScoped(){
        assertArrayEquals(new String[]{"pm","disable-user","--user","10","com.app"},CommandPolicy.command("freeze","com.app",10,false));
        assertArrayEquals(new String[]{"pm","enable","--user","10","com.app"},CommandPolicy.command("unfreeze","com.app",10,false));
    }
    @Test public void packageProtectionChecksUseExactNames(){
        assertTrue(CommandPolicy.containsPackage("package:com.app\npackage:com.other","com.app"));
        assertFalse(CommandPolicy.containsPackage("package:com.app.ext","com.app"));
        assertTrue(CommandPolicy.containsComponent("priority=0\ncom.launcher/.MainActivity","com.launcher"));
        assertFalse(CommandPolicy.containsComponent("com.launcher.ext/.MainActivity","com.launcher"));
    }
}
