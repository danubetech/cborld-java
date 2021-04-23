package com.danubetech.cborld.codecs;

import java.util.HashMap;
import java.util.Map;

public class RegisteredContexts {

    public static final Map<Integer, String> ID_TO_URL;
    public static final Map<String, Integer> URL_TO_ID;

    static {
        ID_TO_URL = new HashMap<>();
        ID_TO_URL.put(0x10, "https://www.w3.org/ns/activitystreams");
        ID_TO_URL.put(0x11, "https://www.w3.org/2018/credentials/v1");
        ID_TO_URL.put(0x12, "https://www.w3.org/ns/did/v1");
        ID_TO_URL.put(0x13, "https://w3id.org/security/suites/ed25519-2018/v1");
        ID_TO_URL.put(0x14, "https://w3id.org/security/suites/ed25519-2020/v1");
        ID_TO_URL.put(0x15, "https://w3id.org/cit/v1");
        ID_TO_URL.put(0x16, "https://w3id.org/age/v1");
        ID_TO_URL.put(0x17, "https://w3id.org/security/suites/x25519-2020/v1");
        ID_TO_URL.put(0x18, "https://w3id.org/veres-one/v1");
    }

    static {
        URL_TO_ID = new HashMap<>();
        for (Map.Entry<Integer, String> id_to_url : ID_TO_URL.entrySet()) {
            URL_TO_ID.put(id_to_url.getValue(), id_to_url.getKey());
        }
    }
}
