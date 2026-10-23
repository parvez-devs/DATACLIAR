package com.axiom.datacliar;
import java.util.*;

public final class BatchTarget {
    public final String pkg,label;
    public BatchTarget(String pkg,String label){this.pkg=pkg;this.label=label;}
    public static List<BatchTarget> unique(List<BatchTarget> input){
        if(input.size()>200)throw new IllegalArgumentException("Choose at most 200 apps per batch");
        Map<String,BatchTarget> items=new LinkedHashMap<>();
        for(BatchTarget item:input){if(item==null||item.pkg==null)throw new IllegalArgumentException("Invalid target");items.putIfAbsent(item.pkg,item);}
        return new ArrayList<>(items.values());
    }
    public static boolean batchAction(String action){return "freeze".equals(action)||"unfreeze".equals(action)||"stop".equals(action);}
}
