package com.axiom.datacliar;

import java.io.File;
import java.io.IOException;

/** Deletes children of explicitly supplied own-cache directories, without following links outside them. */
public final class CacheCleaner {
    private CacheCleaner() {}
    public static int clear(File... roots) {
        int failures=0;
        for(File dir:roots)if(dir!=null)try{
            File[] children=dir.listFiles();
            if(children==null){if(dir.exists())failures++;continue;}
            String root=dir.getCanonicalPath();
            for(File child:children)failures+=delete(child,root);
        }catch(IOException|SecurityException e){failures++;}
        return failures;
    }
    private static int delete(File file,String root) throws IOException {
        if(Thread.currentThread().isInterrupted())return 1;
        if(!file.getCanonicalPath().startsWith(root+File.separator))return 1;
        int failures=0;
        if(file.isDirectory()){
            File[] children=file.listFiles();if(children==null)return 1;
            for(File child:children)failures+=delete(child,root);
        }
        if(!file.delete()&&file.exists())failures++;
        return failures;
    }
}
