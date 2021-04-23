package com.danubetech.cborld.codecs;

import com.danubetech.cborld.Transformer;
import com.danubetech.cborld.util.TermInfo;
import com.upokecenter.cbor.CBORObject;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.*;

public class UuidUrnEncoder extends AbstractCborLdEncoder<String> {

    public UuidUrnEncoder(String value, Transformer transformer, TermInfo termInfo) {
        super(value, transformer, termInfo);
    }

    public EncodedBytes encodeInternal() {
        String rest = this.value.substring("urn:uuid:".length());
        CBORObject entries = CBORObject.NewArray();
        entries.Add(CBORObject.FromObject(Integer.valueOf(3)));
        if (rest.toLowerCase().equals(rest)) {
            byte[] bytes = uuidToBytes(UUID.fromString(rest));
            entries.Add(CBORObject.FromObject(bytes));
        } else {
            // cannot compress
            entries.Add(CBORObject.FromObject(rest));
        }
        return new EncodedBytes(entries.EncodeToBytes());
    }

    @Override
    public EncodedBytes encode() {
        if (this.value.startsWith("urn:uuid:")) {
            return this.encodeInternal();
        }
        return null;
    }

    private static byte[] uuidToBytes(UUID uuid) {
        ByteBuffer byteBuffer = ByteBuffer.wrap(new byte[16]);
        byteBuffer.putLong(uuid.getMostSignificantBits());
        byteBuffer.putLong(uuid.getLeastSignificantBits());
        return byteBuffer.array();
    }
}