package com.danubetech.cborld.codecs;

import com.danubetech.cborld.Transformer;
import com.danubetech.cborld.util.TermInfo;
import com.upokecenter.cbor.CBORObject;

import java.io.IOException;

public class HttpUrlEncoder extends AbstractCborLdEncoder<String> {

    public HttpUrlEncoder(String value, Transformer transformer, TermInfo termInfo) {
        super(value, transformer, termInfo);
    }

    private EncodedBytes encodeInternal(boolean secure) {
        int length = secure ? "https://".length() : "http://".length();
        CBORObject entries = CBORObject.NewArray();
        entries.Add(secure ? CBORObject.FromObject(Integer.valueOf(2)) : CBORObject.FromObject(Integer.valueOf(1)));
        entries.Add(CBORObject.FromObject(this.value.substring(length)));
        return new EncodedBytes(entries.EncodeToBytes());
    }

    @Override
    public EncodedBytes encode() {
        // presume HTTPS is more common, check for it first
        if (this.value.startsWith("https://")) {
            return this.encodeInternal(true);
        }
        if (this.value.startsWith("http://")) {
            return this.encodeInternal(false);
        }
        return null;
    }
}
