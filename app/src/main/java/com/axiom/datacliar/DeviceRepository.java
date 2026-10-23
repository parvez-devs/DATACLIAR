package com.axiom.datacliar;

import android.app.AppOpsManager;
import android.app.usage.StorageStats;
import android.app.usage.StorageStatsManager;
import android.content.Context;
import android.content.pm.*;
import java.util.*;

/** Read-only inventory for the current Android profile. */
public final class DeviceRepository {
    public static final class App {
        public ApplicationInfo info;
        public String label, pkg;
        public long code = -1, data = -1, cache = -1;
        public boolean disabled, system;
        public long total() { return code < 0 || data < 0 ? -1 : code + data; }
    }
    private final Context context;
    public DeviceRepository(Context context) { this.context = context.getApplicationContext(); }
    public boolean usageAllowed() {
        AppOpsManager ops = context.getSystemService(AppOpsManager.class);
        return ops != null && ops.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS,
            android.os.Process.myUid(), context.getPackageName()) == AppOpsManager.MODE_ALLOWED;
    }
    public List<App> apps() throws InterruptedException {
        PackageManager pm = context.getPackageManager();
        StorageStatsManager storage = context.getSystemService(StorageStatsManager.class);
        boolean allowed = usageAllowed();
        List<App> result = new ArrayList<>();
        for (ApplicationInfo info : pm.getInstalledApplications(PackageManager.MATCH_DISABLED_COMPONENTS)) {
            if (Thread.currentThread().isInterrupted()) throw new InterruptedException();
            App app = new App(); app.info = info; app.pkg = info.packageName;
            app.label = info.loadLabel(pm).toString(); app.disabled = !info.enabled;
            app.system = (info.flags & (ApplicationInfo.FLAG_SYSTEM | ApplicationInfo.FLAG_UPDATED_SYSTEM_APP)) != 0;
            if (allowed && storage != null) try {
                StorageStats stats = storage.queryStatsForPackage(info.storageUuid, app.pkg, android.os.Process.myUserHandle());
                app.code = stats.getAppBytes(); app.data = stats.getDataBytes(); app.cache = stats.getCacheBytes();
            } catch (Exception ignored) { /* Denied and unavailable are not zero. */ }
            result.add(app);
        }
        result.sort(Comparator.comparingLong(App::total).reversed().thenComparing(a -> a.label.toLowerCase(Locale.ROOT)));
        return result;
    }
}
