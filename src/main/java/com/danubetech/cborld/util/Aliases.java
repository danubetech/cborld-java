package com.danubetech.cborld.util;

import java.util.*;

public class Aliases {
    public Set<String> id = new HashSet<>();
    public Set<String> type = new HashSet<>();

    public Aliases(Set<String> id, Set<String> type) {
        this.id = id;
        this.type = type;
    }

    public Aliases() {
    }

    public static Aliases fromMap(Map<String, Object> map) {
        if (map == null) return null;
        Aliases aliases = new Aliases();
        aliases.id = (Set<String>) map.get("id");
        aliases.type = (Set<String>) map.get("type");
        return aliases;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("id", this.id != null ? new ArrayList(this.id) : null);
        map.put("type", this.type != null ? new ArrayList(this.type) : null);
        return map;
    }

    public Set<String> get(String key) {
        if ("id".equals(key)) return id;
        if ("type".equals(key)) return type;
        return null;
    }
}
