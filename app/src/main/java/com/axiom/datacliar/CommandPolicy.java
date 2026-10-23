package com.axiom.datacliar;

/** Closed command vocabulary. Arguments are never interpreted by a shell. */
public final class CommandPolicy {
    private CommandPolicy() {}
    public static void validate(String action,String pkg,int user) {
        if(user<0)throw new IllegalArgumentException("Invalid Android profile");
        if(pkg==null||!pkg.matches("[a-zA-Z][a-zA-Z0-9_]*(\\.[a-zA-Z0-9_]+)+"))throw new IllegalArgumentException("Invalid package");
        if(!java.util.Arrays.asList("stop","freeze","unfreeze","sleep","wake","cache","data").contains(action))throw new IllegalArgumentException("Unknown action");
    }
    public static String[] command(String action,String pkg,int user,boolean cacheSupported) {
        validate(action,pkg,user);String u=Integer.toString(user);
        switch(action){
            case "stop":return new String[]{"am","force-stop","--user",u,pkg};
            case "freeze":return new String[]{"pm","disable-user","--user",u,pkg};
            case "unfreeze":return new String[]{"pm","enable","--user",u,pkg};
            case "sleep":return new String[]{"am","set-inactive","--user",u,pkg,"true"};
            case "wake":return new String[]{"am","set-inactive","--user",u,pkg,"false"};
            case "data":return new String[]{"pm","clear","--user",u,pkg};
            case "cache":if(!cacheSupported)throw new UnsupportedOperationException("Cache-only is unavailable on this Android build. Use App info > Storage > Clear cache. App data was not cleared.");return new String[]{"pm","clear","--user",u,"--cache-only",pkg};
            default:throw new IllegalArgumentException("Unknown action");
        }
    }
    public static boolean containsPackage(String output,String pkg){
        for(String line:output.split("\\n"))if(line.trim().equals("package:"+pkg))return true;
        return false;
    }
    public static boolean containsComponent(String output,String pkg){
        return java.util.regex.Pattern.compile("(?m)(?:^|\\s)"+java.util.regex.Pattern.quote(pkg)+"/").matcher(output).find();
    }
}
