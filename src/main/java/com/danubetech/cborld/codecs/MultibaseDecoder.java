package com.danubetech.cborld.codecs;

import com.danubetech.cborld.Transformer;
import com.danubetech.cborld.util.TermInfo;
import io.ipfs.multibase.Multibase;

public class MultibaseDecoder extends AbstractCborLdDecoder<String> {

    public MultibaseDecoder(Object value, Transformer transformer, TermInfo termInfo) {
        super(value, transformer, termInfo);
    }

    private String decodeInternal() {
        byte prefix = ((byte[]) this.value)[0];
        byte[] suffix = new byte[((byte[]) this.value).length - 1];
        System.arraycopy((byte[]) this.value, 1, suffix, 0, suffix.length);
        return Multibase.encode(Multibase.Base.lookup((char) prefix), suffix);
    }

    @Override
    public String decode() {
        if (!(this.value instanceof byte[])) {
            return null;
        }
        return this.decodeInternal();
    }
}
