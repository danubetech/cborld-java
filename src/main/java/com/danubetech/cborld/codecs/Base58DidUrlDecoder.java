package com.danubetech.cborld.codecs;

import com.danubetech.cborld.Transformer;
import com.danubetech.cborld.util.TermInfo;
import io.leonard.Base58;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Base58DidUrlDecoder extends AbstractCborLdDecoder<String> {

    public static final Map<Integer, String> ID_TO_SCHEME;

    static {
        ID_TO_SCHEME = new HashMap<>();
        ID_TO_SCHEME.put(1024, "did:v1:nym");
        ID_TO_SCHEME.put(1025, "did:key");
    }

    public Base58DidUrlDecoder(Object value, Transformer transformer, TermInfo termInfo) {
        super(value, transformer, termInfo);
    }

    private String decodeInternal() {
        List<?> value = (List<?>) this.value;
        String url = ID_TO_SCHEME.get(((Number) value.get(0)).intValue());
        if (value.get(1) instanceof String) {
            url += (String) value.get(1);
        } else {
            url += "z" + Base58.encode((byte[]) value.get(1));
        }
        if (value.size() > 2) {
            if (value.get(2) instanceof String) {
                url += "#" + (String) value.get(2);
            } else {
                url += "z" + Base58.encode((byte[]) value.get(2));
            }
        }
        return url;
    }

    @Override
    public String decode() {
        if (!(this.value instanceof List<?>)) return null;
        List<?> value = (List<?>) this.value;
        if (!(value.size() > 1 && value.size() <= 3)) {
            return null;
        }
        if (!ID_TO_SCHEME.containsKey(((Number) value.get(0)).intValue())) {
            return null;
        }
        return this.decodeInternal();
    }
}
