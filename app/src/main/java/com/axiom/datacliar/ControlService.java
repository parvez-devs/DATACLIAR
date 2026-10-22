package com.axiom.datacliar;

import android.content.*;
import android.content.pm.*;
import android.app.admin.DevicePolicyManager;
import android.os.Build;
import android.provider.Settings;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;

/** Runs only in the Shizuku-granted shell/root process; never exported as an app service. */
public class ControlService extends IControlService.Stub {
    private final Context context;
    private Boolean cacheSupported;
    public ControlService(Context context) { this.context = context; }
    @Override public void destroy() { System.exit(0); }
    @Override public synchronized String execute(String action, String pkg, int user) {
        try {
            if (user != context.getApplicationInfo().uid / 100000)
                return "ERROR: Cross-profile operations are disabled";
            if (!Arrays.asList("processes", "cpu", "cacheHelp", "inactive", "state").contains(action)) checkTarget(pkg);
            if (action.equals("state")) {
                ApplicationInfo info = context.getPackageManager().getApplicationInfo(pkg, PackageManager.MATCH_DISABLED_COMPONENTS);
                return run(new String[]{"am", "get-uid-state", Integer.toString(info.uid)});
            }
            if (action.equals("cache") && cacheSupported == null)
                cacheSupported = Build.VERSION.SDK_INT >= 33 && run(new String[]{"pm", "help"}).contains("--cache-only");
            String result = run(CommandPolicy.command(action, pkg, user, Boolean.TRUE.equals(cacheSupported)));
            if (result.startsWith("ERROR:")) return result;
            if ((action.equals("cache") || action.equals("data")) && !result.contains("Success"))
                return "ERROR: Android did not confirm completion: " + result;
            if (action.equals("freeze") && !result.contains("disabled-user")) return "ERROR: Freeze not confirmed: " + result;
            if (action.equals("unfreeze") && !result.contains("enabled")) return "ERROR: Unfreeze not confirmed: " + result;
            if (action.equals("sleep") || action.equals("wake")) {
                String verified = run(CommandPolicy.command("inactive", pkg, user, false));
                String expected = action.equals("sleep") ? "true" : "false";
                if (!verified.contains("Idle=" + expected)) return "ERROR: Standby state not confirmed: " + verified;
                return verified;
            }
            return result.trim().isEmpty() ? "OK" : result;
        } catch (Exception e) { return "ERROR: " + e.getMessage(); }
    }
    private void checkTarget(String pkg) throws Exception {
        if (pkg == null || pkg.equals(context.getPackageName()) || pkg.equals("moe.shizuku.privileged.api"))
            throw new SecurityException("This control app is protected");
        PackageManager pm = context.getPackageManager();
        ApplicationInfo info = pm.getApplicationInfo(pkg, PackageManager.MATCH_DISABLED_COMPONENTS);
        if ((info.flags & (ApplicationInfo.FLAG_SYSTEM | ApplicationInfo.FLAG_UPDATED_SYSTEM_APP)) != 0 || info.uid % 100000 < 10000)
            throw new SecurityException("System apps are read-only");
        ResolveInfo home = pm.resolveActivity(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME), PackageManager.MATCH_DEFAULT_ONLY);
        if (home != null && pkg.equals(home.activityInfo.packageName)) throw new SecurityException("Default launcher is protected");
        String keyboard = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.DEFAULT_INPUT_METHOD);
        if (keyboard != null && keyboard.startsWith(pkg + "/")) throw new SecurityException("Active keyboard is protected");
        DevicePolicyManager dpm = context.getSystemService(DevicePolicyManager.class);
        List<ComponentName> admins = dpm.getActiveAdmins();
        if (admins != null) for (ComponentName admin : admins)
            if (pkg.equals(admin.getPackageName())) throw new SecurityException("Device administrator is protected");
        Intent vpn = new Intent("android.net.VpnService").setPackage(pkg);
        if (!pm.queryIntentServices(vpn, PackageManager.MATCH_DISABLED_COMPONENTS).isEmpty()) throw new SecurityException("VPN apps are protected");
    }
    private String run(String[] command) throws Exception {
        Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        Thread reader = new Thread(() -> {
            try (InputStream in = process.getInputStream()) {
                byte[] buffer = new byte[4096]; int count;
                while ((count = in.read(buffer)) != -1) if (bytes.size() < 512000) bytes.write(buffer, 0, count);
            } catch (IOException ignored) { }
        });
        reader.start();
        if (!process.waitFor(20, TimeUnit.SECONDS)) { process.destroyForcibly(); reader.join(1000); return "ERROR: Operation timed out; refresh to check its state"; }
        reader.join(1000);
        String out = bytes.toString("UTF-8").trim();
        return process.exitValue() == 0 ? out : "ERROR: " + out;
    }
}
