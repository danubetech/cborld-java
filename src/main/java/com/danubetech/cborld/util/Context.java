package com.danubetech.cborld.util;

import java.util.HashMap;
import java.util.Map;

public class Context {
    public static Aliases aliases(Map map) {
        Aliases aliases = (Aliases) map.get("aliases");
        if (aliases == null) {
            aliases = new Aliases();
            map.put("aliases", aliases);
        }
        return aliases;
    }

    public static String context(Map map) {
        return (String) map.get("context");
    }

    public static Map<String, Object> scopedContextMap(Map map) {
        Map<String, Object> scopedContextMap = (Map<String, Object>) map.get("scopedContextMap");
        if (scopedContextMap == null) {
            scopedContextMap = new HashMap<>();
            map.put("scopedContextMap", scopedContextMap);
        }
        return scopedContextMap;
    }

    public static Map<String, Object> termMap(Map map) {
        Map<String, Object> termMap = (Map<String, Object>) map.get("termMap");
        if (termMap == null) {
            termMap = new HashMap<>();
            map.put("termMap", termMap);
        }
        return termMap;
    }
}
