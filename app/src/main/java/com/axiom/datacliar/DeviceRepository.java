package com.axiom.datacliar;

import android.app.*;
import android.app.usage.*;
import android.content.*;
import android.content.pm.*;
import android.os.*;
import java.io.*;
import java.util.*;

public final class DeviceRepository {
    public static class App {
        public ApplicationInfo info;
        public String label, pkg;
        public long code = -1, data = -1, cache = -1;
        public boolean frozen, system, running, sleeping, background;
        public long total() { return code < 0 ? -1 : code + data; } // data already includes cache
    }
    private final Context context;
    public DeviceRepository(Context context) { this.context = context; }
    public boolean usageAllowed() {
        AppOpsManager ops = context.getSystemService(AppOpsManager.class);
        return ops.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, android.os.Process.myUid(), context.getPackageName()) == AppOpsManager.MODE_ALLOWED;
    }
    public List<App> apps(Set<String> running) {
        PackageManager pm = context.getPackageManager();
        StorageStatsManager storage = context.getSystemService(StorageStatsManager.class);
        UsageStatsManager usage = context.getSystemService(UsageStatsManager.class);
        boolean allowed = usageAllowed();
        List<App> result = new ArrayList<>();
        for (ApplicationInfo info : pm.getInstalledApplications(PackageManager.MATCH_DISABLED_COMPONENTS)) {
            App a = new App(); a.info = info; a.pkg = info.packageName;
            a.label = info.loadLabel(pm).toString();
            a.system = (info.flags & (ApplicationInfo.FLAG_SYSTEM | ApplicationInfo.FLAG_UPDATED_SYSTEM_APP)) != 0;
            a.frozen = !info.enabled; a.running = running.contains(a.pkg);
            if (allowed) try {
                StorageStats stats = storage.queryStatsForPackage(info.storageUuid, a.pkg, android.os.Process.myUserHandle());
                a.code = stats.getAppBytes(); a.data = stats.getDataBytes(); a.cache = stats.getCacheBytes();
                a.sleeping = usage.isAppInactive(a.pkg);
            } catch (Exception ignored) { /* Unknown remains unknown, never zero. */ }
            result.add(a);
        }
        result.sort(Comparator.comparingLong(App::total).reversed().thenComparing(a -> a.label.toLowerCase(Locale.ROOT)));
        return result;
    }
    public static long[] cpuTicks(String text) {
        for (String line : text.split("\\n")) if (line.startsWith("cpu ")) {
            String[] fields = line.trim().split("\\s+");
            long total = 0;
            // Guest counters are already included in user/nice; do not double count.
            for (int i = 1; i <= 8 && i < fields.length; i++) total += Long.parseLong(fields[i]);
            long idle = Long.parseLong(fields[4]) + (fields.length > 5 ? Long.parseLong(fields[5]) : 0);
            return new long[]{total, idle};
        }
        return null;
    }
    public static String localCpu() {
        try (BufferedReader r = new BufferedReader(new FileReader("/proc/stat"))) { return r.readLine(); }
        catch (Exception e) { return ""; }
    }
}
