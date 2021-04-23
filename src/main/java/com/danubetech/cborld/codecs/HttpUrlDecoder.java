package com.danubetech.cborld.codecs;

import com.danubetech.cborld.Transformer;
import com.danubetech.cborld.util.TermInfo;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HttpUrlDecoder extends AbstractCborLdDecoder<String> {

    public HttpUrlDecoder(Object value, Transformer transformer, TermInfo termInfo) {
        super(value, transformer, termInfo);
    }

    private String decodeInternal(boolean secure) {
        String scheme = secure ? "https://" : "http://";
        return scheme + (String) ((List<?>) this.value).get(1);
    }

    @Override
    public String decode() {
        if ((! (this.value instanceof List<?>))) return null;
        List<?> value = (List<?>) this.value;
        if (! (value.size() == 2 && value.get(1) instanceof String)) {
            return null;
        }
        return this.decodeInternal(((Number) ((List<?>) this.value).get(0)).intValue() == 2);
    }
}
