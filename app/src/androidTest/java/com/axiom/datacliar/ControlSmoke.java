package com.axiom.datacliar;

/** Shell identity harness; only the disposable test APK is mutated. Not shipped in the main APK. */
public final class ControlSmoke {
    public static void main(String[] args)throws Exception{
        ControlService service=new ControlService(0);String fixture="com.axiom.datacliar.test";
        try{
            ok(service.execute("ping","",0));
            error(service.execute("freeze","com.axiom.datacliar",0));
            error(service.execute("data","com.android.settings",0));
            error(service.execute("freeze","moe.shizuku.privileged.api",0));
            error(service.execute("stop",fixture,10));
            error(service.execute("stop","com.app; reboot",0));
            ok(service.execute("freeze",fixture,0));ok(service.execute("unfreeze",fixture,0));
            ok(service.execute("sleep",fixture,0));ok(service.execute("wake",fixture,0));
            ok(service.execute("stop",fixture,0));
            String cache=service.execute("cache",fixture,0);
            if(cache.startsWith("ERROR:")&&!cache.contains("Cache-only is unavailable")&&!cache.contains("Safe cache-only cleanup needs"))throw new AssertionError(cache);
            System.out.println("CACHE_CHECK: "+cache);
            ok(service.execute("data",fixture,0));
            System.out.println("CONTROL_SMOKE_OK: protections, freeze/unfreeze, sleep/wake, stop, cache capability, data");
        }finally{service.execute("unfreeze",fixture,0);}
        System.exit(0);
    }
    private static void ok(String result){System.out.println(result);if(result.startsWith("ERROR:"))throw new AssertionError(result);}
    private static void error(String result){System.out.println(result);if(!result.startsWith("ERROR:"))throw new AssertionError("Expected refusal: "+result);}
}
