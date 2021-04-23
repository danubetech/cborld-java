package com.danubetech.cborld.codecs;

import com.danubetech.cborld.Transformer;
import com.danubetech.cborld.util.TermInfo;

public abstract class AbstractCborLdEncoder <T> implements CborLdEncoder<T> {

    protected T value;
    protected Transformer transformer;
    protected TermInfo termInfo;

    protected AbstractCborLdEncoder(T value, Transformer transformer, TermInfo termInfo) {
        this.value = value;
        this.transformer = transformer;
        this.termInfo = termInfo;
    }

    @Override
    public final T getValue() {
        return this.value;
    }
}
