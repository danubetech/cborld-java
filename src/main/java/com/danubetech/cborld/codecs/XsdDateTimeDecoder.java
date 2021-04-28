package com.danubetech.cborld.codecs;

import com.danubetech.cborld.Transformer;
import com.danubetech.cborld.util.TermInfo;
import com.google.api.client.util.DateTime;

import java.util.List;

public class XsdDateTimeDecoder extends AbstractCborLdDecoder<String> {

    public XsdDateTimeDecoder(Object value, Transformer transformer, TermInfo termInfo) {
        super(value, transformer, termInfo);
    }

    private String decodeInternal() {
        if (this.value instanceof Number) {
            return new DateTime(((Number) this.value).longValue() * 1000).toStringRfc3339().replace(".000Z", "Z");
        }
        return new DateTime(((Number) ((List<Object>) this.value).get(0)).longValue() * 1000 + ((Number) ((List<Object>) this.value).get(1)).longValue()).toStringRfc3339();
    }

    @Override
    public String decode() {
        if (this.value instanceof Number) {
            return this.decodeInternal();
        }
        if (this.value instanceof List &&
                ((List<Object>) this.value).size() == 2 &&
                (((List<Object>) this.value).get(0) instanceof Number) ||
                (((List<Object>) this.value).get(1) instanceof Number)) {
            return this.decodeInternal();
        }
        return null;
    }
}
