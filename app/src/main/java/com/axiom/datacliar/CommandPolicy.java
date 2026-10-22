package com.axiom.datacliar;

import java.util.*;

/** Pure, allow-listed command construction. No shell interpolation. */
public final class CommandPolicy {
    private CommandPolicy() {}
    public static String[] command(String action, String pkg, int user, boolean cacheSupported) {
        if (user < 0) throw new IllegalArgumentException("Invalid Android user");
        if (action.equals("processes")) return new String[]{"ps", "-A", "-w", "-o", "UID,NAME"};
        if (action.equals("cacheHelp")) return new String[]{"pm", "help"};
        if (action.equals("cpu")) return new String[]{"cat", "/proc/stat"};
        if (pkg == null || !pkg.matches("[a-zA-Z][a-zA-Z0-9_]*(\\.[a-zA-Z0-9_]+)+"))
            throw new IllegalArgumentException("Invalid package name");
        String u = Integer.toString(user);
        switch (action) {
            case "stop": return new String[]{"am", "force-stop", "--user", u, pkg};
            case "freeze": return new String[]{"pm", "disable-user", "--user", u, pkg};
            case "unfreeze": return new String[]{"pm", "enable", "--user", u, pkg};
            case "sleep": return new String[]{"am", "set-inactive", "--user", u, pkg, "true"};
            case "wake": return new String[]{"am", "set-inactive", "--user", u, pkg, "false"};
            case "inactive": return new String[]{"am", "get-inactive", "--user", u, pkg};
            case "data": return new String[]{"pm", "clear", "--user", u, pkg};
            case "cache":
                if (!cacheSupported) throw new UnsupportedOperationException("Cache-only command unavailable. Use Android App info > Storage > Clear cache.");
                return new String[]{"pm", "clear", "--user", u, "--cache-only", pkg};
            default: throw new IllegalArgumentException("Unknown operation");
        }
    }
    public static Set<String> processNames(String output) { return processNames(output, 0); }
    public static Set<String> processNames(String output, int user) {
        Set<String> result = new HashSet<>();
        for (String line : output.split("\\n")) {
            String[] fields = line.trim().split("\\s+");
            if (fields.length < 2) continue;
            String uid = fields[0];
            boolean matches;
            if (uid.matches("[0-9]+")) matches = Long.parseLong(uid) / 100000 == user;
            else matches = uid.startsWith("u" + user + "_");
            if (!matches) continue;
            String name = fields[fields.length - 1].split(":", 2)[0];
            if (name.contains(".")) result.add(name);
        }
        return result;
    }
    public static boolean backgroundState(String raw){
        // AOSP am prints a numeric process state before the human-readable name.
        java.util.regex.Matcher m=java.util.regex.Pattern.compile("^\\s*(\\d+)\\b").matcher(raw);
        if(!m.find())return false;int state=Integer.parseInt(m.group(1));return state>=7&&state<20;
    }
}
