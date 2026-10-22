package com.axiom.datacliar;
import org.junit.Test;
import static org.junit.Assert.*;
public class CommandPolicyTest {
    @Test public void cacheNeverFallsBackToDataWipe() {
        assertThrows(UnsupportedOperationException.class, () -> CommandPolicy.command("cache","com.example.app",0,false));
        assertArrayEquals(new String[]{"pm","clear","--user","0","--cache-only","com.example.app"}, CommandPolicy.command("cache","com.example.app",0,true));
    }
    @Test public void rejectsShellInjectionAndUnknownActions() {
        assertThrows(IllegalArgumentException.class, () -> CommandPolicy.command("stop","com.app; reboot",0,true));
        assertThrows(IllegalArgumentException.class, () -> CommandPolicy.command("reboot","com.app",0,true));
        assertThrows(IllegalArgumentException.class, () -> CommandPolicy.command("stop","com.app",-1,true));
    }
    @Test public void freezeIsReversibleAndUserScoped() {
        assertArrayEquals(new String[]{"pm","disable-user","--user","10","com.example.app"},CommandPolicy.command("freeze","com.example.app",10,true));
        assertArrayEquals(new String[]{"pm","enable","--user","10","com.example.app"},CommandPolicy.command("unfreeze","com.example.app",10,true));
    }
    @Test public void processesDeduplicateChildProcesses() {
        assertEquals(2,CommandPolicy.processNames("UID NAME\nu0_a1 com.example.app\nu0_a1 com.example.app:worker\nu0_a2 com.example.other\n0 init").size());
    }
    @Test public void cpuDoesNotDoubleCountGuestTime() {
        assertArrayEquals(new long[]{36,9},DeviceRepository.cpuTicks("cpu  1 2 3 4 5 6 7 8 9 10"));
    }
    @Test public void boostSkipsForegroundAndUnknownStates() {
        assertFalse(MainActivity.backgroundState("2 (TOP)"));
        assertFalse(MainActivity.backgroundState("ERROR: denied"));
        assertFalse(MainActivity.backgroundState("20 (NONEXISTENT)"));
        assertTrue(MainActivity.backgroundState("19 (CACHED_EMPTY)"));
    }
}
