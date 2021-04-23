package com.danubetech.cborld.codecs;

public interface CborLdEncoder<T> {

    default EncodedBytes encode() {
        throw new RuntimeException("Not implemented.");
    }

    public T getValue();

    public static class EncodedBytes {
        public byte[] bytes;
        public EncodedBytes(byte[] bytes) {
            this.bytes = bytes;
        }
    }
}
