package com.axiom.datacliar;

import java.util.Locale;

public final class AppQuery {
    private AppQuery() {}
    public static boolean matches(String label, String pkg, String query, String filter, boolean system, boolean disabled) {
        if ("User apps".equals(filter) && system) return false;
        if ("System apps".equals(filter) && !system) return false;
        if ("Disabled".equals(filter) && !disabled) return false;
        String needle = query.trim().toLowerCase(Locale.ROOT);
        return label.toLowerCase(Locale.ROOT).contains(needle) || pkg.toLowerCase(Locale.ROOT).contains(needle);
    }
}
