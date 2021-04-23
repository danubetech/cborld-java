package com.danubetech.cborld;

import java.util.HashMap;
import java.util.Map;

public class Keywords {

    public static final Map<String, Integer> KEYWORDS;
    public static final int FIRST_CUSTOM_TERM_ID = 100;

    static {

        KEYWORDS = new HashMap<>();
        // ordered is important, do not change
        KEYWORDS.put("@context", 0);
        KEYWORDS.put("@type", 2);
        KEYWORDS.put("@id", 4);
        KEYWORDS.put("@value", 6);
        // alphabetized after `@context`, `@type`, `@id`, `@value`
        // IDs <= 24 represented with 1 byte, IDs > 24 use 2+ bytes
        KEYWORDS.put("@direction", 8);
        KEYWORDS.put("@graph", 10);
        KEYWORDS.put("@included", 12);
        KEYWORDS.put("@index", 14);
        KEYWORDS.put("@json", 16);
        KEYWORDS.put("@language", 18);
        KEYWORDS.put("@list", 20);
        KEYWORDS.put("@nest", 22);
        KEYWORDS.put("@reverse", 24);
        // TODO: remove these? these only appear in frames and contexts
        KEYWORDS.put("@base", 26);
        KEYWORDS.put("@container", 28);
        KEYWORDS.put("@default", 30);
        KEYWORDS.put("@embed", 32);
        KEYWORDS.put("@explicit", 34);
        KEYWORDS.put("@none", 36);
        KEYWORDS.put("@omitDefault", 38);
        KEYWORDS.put("@prefix", 40);
        KEYWORDS.put("@preserve", 42);
        KEYWORDS.put("@protected", 44);
        KEYWORDS.put("@requireAll", 46);
        KEYWORDS.put("@set", 48);
        KEYWORDS.put("@version", 50);
        KEYWORDS.put("@vocab", 52);
    }
}
