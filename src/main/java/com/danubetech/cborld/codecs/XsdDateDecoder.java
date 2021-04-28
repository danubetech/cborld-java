package com.danubetech.cborld.codecs;

import com.danubetech.cborld.Transformer;
import com.danubetech.cborld.util.TermInfo;
import com.google.api.client.util.DateTime;

public class XsdDateDecoder extends AbstractCborLdDecoder<String> {

    public XsdDateDecoder(Object value, Transformer transformer, TermInfo termInfo) {
        super(value, transformer, termInfo);
    }

    private String decodeInternal() {
        String dateString = new DateTime(((Number) this.value).longValue() * 1000).toStringRfc3339();
        return dateString.substring(0, dateString.indexOf('T'));
    }

    @Override
    public String decode() {
        if (this.value instanceof Long) {
            return this.decodeInternal();
        }
        return null;
    }
}
