package com.danubetech.cborld.codecs;

import com.danubetech.cborld.Transformer;
import com.danubetech.cborld.util.TermInfo;
import com.upokecenter.cbor.CBORObject;

import java.util.Map;

public class ContextEncoder extends AbstractCborLdEncoder<String> {

    public ContextEncoder(String value, Transformer transformer, TermInfo termInfo) {
        super(value, transformer, termInfo);
    }

    public ContextEncoder(String value, Transformer transformer) {
        this(value, transformer, null);
    }

    public ContextEncoder(String value) {
        this(value, null, null);
    }

    private EncodedBytes encodeInternal(String context, Map<String, Integer> appContextMap) {
        Integer id = RegisteredContexts.URL_TO_ID.get(context);
        if (id == null) id = appContextMap.get(context);
        if (id == null) {
            return new EncodedBytes(CBORObject.FromObject(context).EncodeToBytes());
        }
        return new EncodedBytes(CBORObject.FromObject(id).EncodeToBytes());
    }

    @Override
    public EncodedBytes encode() {
        if (!(this.value instanceof String)) {
            return null;
        }
        Map<String, Integer> appContextMap = this.transformer.appContextMap;
        return this.encodeInternal(this.value, appContextMap);
    }
}
