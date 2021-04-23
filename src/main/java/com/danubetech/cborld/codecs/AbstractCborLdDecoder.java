package com.danubetech.cborld.codecs;

import com.danubetech.cborld.Transformer;
import com.danubetech.cborld.util.TermInfo;

public abstract class AbstractCborLdDecoder<T> implements CborLdDecoder<T> {

    protected Object value;
    protected Transformer transformer;
    protected TermInfo termInfo;

    protected AbstractCborLdDecoder(Object value, Transformer transformer, TermInfo termInfo) {
        this.value = value;
        this.transformer = transformer;
        this.termInfo = termInfo;
    }

    @Override
    public final Object getValue() {
        return this.value;
    }
}
