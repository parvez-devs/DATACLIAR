package com.axiom.datacliar;

/** Shell identity harness; only the separate disposable fixture APK is mutated. Not shipped in the main APK. */
public final class ControlSmoke {
    public static void main(String[] args)throws Exception{
        ControlService service=new ControlService(0);String fixture="com.axiom.datacliar.fixture";
        try{
            ok(service.execute("ping","",0));
            error(service.execute("freeze","com.axiom.datacliar",0));
            error(service.execute("data","com.android.settings",0));
            error(service.execute("freeze","moe.shizuku.privileged.api",0));
            error(service.execute("stop",fixture,10));
            error(service.execute("stop","com.app; reboot",0));
            ok(service.execute("freeze",fixture,0));ok(service.execute("unfreeze",fixture,0));
            Thread.sleep(1000); // Let package-enabled broadcasts settle before testing standby.
            ok(service.execute("sleep",fixture,0));ok(service.execute("wake",fixture,0));
            ok(service.execute("stop",fixture,0));
            String cache=service.execute("cache",fixture,0);
            if(cache.startsWith("ERROR:")&&!cache.contains("Cache-only is unavailable")&&!cache.contains("Safe cache-only cleanup needs"))throw new AssertionError(cache);
            System.out.println("CACHE_CHECK: "+cache);
            if(!readFixture("files/keep.txt").contains("DATACLIAR_FIXTURE"))throw new AssertionError("Cache operation erased app data");
            ok(service.execute("data",fixture,0));
            if(readFixture("files/keep.txt").contains("DATACLIAR_FIXTURE"))throw new AssertionError("Data clear did not remove fixture data");
            System.out.println("CONTROL_SMOKE_OK: protections, freeze/unfreeze, sleep/wake, stop, cache capability, data");
        }finally{service.execute("unfreeze",fixture,0);}
        System.exit(0);
    }
    private static String readFixture(String path)throws Exception{
        Process process=new ProcessBuilder("run-as","com.axiom.datacliar.fixture","cat",path).redirectErrorStream(true).start();
        java.io.ByteArrayOutputStream out=new java.io.ByteArrayOutputStream();byte[] buffer=new byte[1024];int count;
        try(java.io.InputStream in=process.getInputStream()){while((count=in.read(buffer))!=-1)out.write(buffer,0,count);}
        process.waitFor();return out.toString("UTF-8");
    }
    private static void ok(String result){System.out.println(result);if(result.startsWith("ERROR:"))throw new AssertionError(result);}
    private static void error(String result){System.out.println(result);if(!result.startsWith("ERROR:"))throw new AssertionError("Expected refusal: "+result);}
}
