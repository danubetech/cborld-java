package com.danubetech.cborld.codecs;

public interface CborLdDecoder<T> {

    default T decode() {
        throw new RuntimeException("Not implemented.");
    }

    public Object getValue();
}
