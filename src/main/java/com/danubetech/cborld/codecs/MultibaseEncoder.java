package com.danubetech.cborld.codecs;

import com.danubetech.cborld.Transformer;
import com.danubetech.cborld.util.TermInfo;
import com.upokecenter.cbor.CBORObject;
import io.ipfs.multibase.Multibase;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class MultibaseEncoder extends AbstractCborLdEncoder<String> {

    public MultibaseEncoder(String value, Transformer transformer, TermInfo termInfo) {
        super(value, transformer, termInfo);
    }

    @Override
    public EncodedBytes encode() {
        byte prefix = (byte) this.value.charAt(0);
        byte[] suffix = Multibase.decode(this.value);
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        bytes.write(prefix);
        try {
            bytes.write(suffix);
        } catch (IOException ex) {
            throw new RuntimeException("Cannot write " + suffix.length + " bytes: " + ex.getMessage(), ex);
        }
        return new EncodedBytes(CBORObject.FromObject(bytes.toByteArray()).EncodeToBytes());
    }
}
