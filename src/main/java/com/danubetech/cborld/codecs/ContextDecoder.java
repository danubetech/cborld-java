package com.danubetech.cborld.codecs;

import com.danubetech.cborld.CborLdException;
import com.danubetech.cborld.Transformer;
import com.danubetech.cborld.util.TermInfo;

import java.util.Map;

public class ContextDecoder extends AbstractCborLdDecoder<Object> {

    private Transformer transformer;

    public ContextDecoder(Object value, Transformer transformer, TermInfo termInfo) {
        super(value, transformer, termInfo);
    }

    public ContextDecoder(Object value, Transformer transformer) {
        this(value, transformer, null);
    }

    public ContextDecoder(Object value) {
        this(value, null, null);
    }

    private Object decodeInternal(Map<Integer, String> reverseAppContextMap) {
        // handle uncompressed context
        if (!(this.value instanceof Number)) {
            return mapToObject(this.value);
        }

        // handle compressed context
        String url = RegisteredContexts.ID_TO_URL.get(((Number) this.value).intValue());
        if (url == null) url = reverseAppContextMap.get(((Number) this.value).intValue());
        if (url == null) {
            throw new CborLdException(CborLdException.CborLdError.ERR_UNDEFINED_COMPRESSED_CONTEXT, "Undefined compressed context " + this.value);
        }
        return url;
    }

    @Override
    public Object decode() {
        Map<Integer, String> reverseAppContextMap = this.transformer != null ? this.transformer.reverseAppContextMap : null;
        return this.decodeInternal(reverseAppContextMap);
    }

    private static Object mapToObject(Object map) {
        return map;
    }
}
