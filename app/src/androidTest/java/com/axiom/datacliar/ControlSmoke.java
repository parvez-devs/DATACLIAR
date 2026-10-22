package com.axiom.datacliar;

import android.content.Context;
import android.os.Looper;

/** Shell-only emulator harness, packaged only in the test APK, never the shipped APK. */
public final class ControlSmoke {
    public static void main(String[] args) throws Exception {
        Looper.prepareMainLooper();
        Class<?> thread=Class.forName("android.app.ActivityThread");
        Object instance=thread.getMethod("systemMain").invoke(null);
        Context system=(Context)thread.getMethod("getSystemContext").invoke(instance);
        Context context=system.createPackageContext("com.axiom.datacliar",Context.CONTEXT_INCLUDE_CODE|Context.CONTEXT_IGNORE_SECURITY);
        ControlService service=new ControlService(context);
        String fixture="com.axiom.datacliar.test";
        try {
            expectError(service.execute("freeze","com.axiom.datacliar",0));
            expectError(service.execute("data","com.android.settings",0));
            expectError(service.execute("stop",fixture,10));
            expectOk(service.execute("processes","",0));
            expectOk(service.execute("freeze",fixture,0));
            expectOk(service.execute("unfreeze",fixture,0));
            expectOk(service.execute("sleep",fixture,0));
            expectOk(service.execute("wake",fixture,0));
            expectOk(service.execute("stop",fixture,0));
            expectOk(service.execute("cache",fixture,0));
            expectOk(service.execute("data",fixture,0));
            System.out.println("CONTROL_SMOKE_OK: freeze/unfreeze, sleep/wake, stop, cache, data, protections");
        } finally { service.execute("unfreeze",fixture,0); }
        System.exit(0);
    }
    private static void expectOk(String result) {
        System.out.println(result);
        if(result.startsWith("ERROR:"))throw new AssertionError(result);
    }
    private static void expectError(String result) {
        if(!result.startsWith("ERROR:"))throw new AssertionError("Expected protection refusal, got: "+result);
    }
}
