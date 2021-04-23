package com.danubetech.cborld.codecs;

import com.danubetech.cborld.Transformer;
import com.danubetech.cborld.util.TermInfo;

import java.io.IOException;
import java.util.List;

public class UuidUrnDecoder extends AbstractCborLdDecoder<String> {

    public UuidUrnDecoder(Object value, Transformer transformer, TermInfo termInfo) {
        super(value, transformer, termInfo);
    }

    private String decodeInternal() {
        List<?> value = (List<?>) this.value;
        String uuid = value.get(1) instanceof String ? (String) value.get(1) : new String((byte[]) value.get(1));
        return "urn:uuid:" + uuid;
    }

    @Override
    public String decode() {
        if (! (this.value instanceof List<?>)) return null;
        List<?> value = (List<?>) this.value;
        if (value.size() == 2 && (value.get(0) instanceof String || value.get(1) instanceof byte[])) {
            return this.decodeInternal();
        }
        return null;
    }
}
