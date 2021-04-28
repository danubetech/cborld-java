package com.danubetech.cborld.codecs;

import com.danubetech.cborld.Transformer;
import com.danubetech.cborld.util.TermInfo;
import com.upokecenter.cbor.CBORObject;
import io.leonard.Base58;

import java.util.HashMap;
import java.util.Map;

public class Base58DidUrlEncoder extends AbstractCborLdEncoder<String> {

    public static final Map<String, Integer> SCHEME_TO_ID;

    static {
        SCHEME_TO_ID = new HashMap<>();
        SCHEME_TO_ID.put("did:v1:nym", 1024);
        SCHEME_TO_ID.put("did:key", 1025);
    }

    public Base58DidUrlEncoder(String value, Transformer transformer, TermInfo termInfo) {
        super(value, transformer, termInfo);
    }

    public EncodedBytes encodeInternal(String scheme) {
        String suffix = this.value.substring(scheme.length());
        String[] split = suffix.split("#");
        String authority = split[0];
        String fragment = split.length > 1 ? split[1] : null;
        CBORObject entries = CBORObject.NewArray();
        entries.Add(CBORObject.FromObject(SCHEME_TO_ID.get(scheme)));
        entries.Add(multibase58ToCBORObject(authority));
        if (fragment != null) entries.Add(multibase58ToCBORObject(fragment));
        return new EncodedBytes(entries.EncodeToBytes());
    }

    @Override
    public EncodedBytes encode() {
        for (String key : SCHEME_TO_ID.keySet()) {
            if (this.value.startsWith(key)) return this.encodeInternal(key);
        }
        return null;
    }

    private static CBORObject multibase58ToCBORObject(String string) {
        if (string.startsWith("z")) {
            byte[] decoded = Base58.decode(string.substring(1));
            return CBORObject.FromObject(decoded);
        }
        // cannot compress
        return CBORObject.FromObject(string);
    }
}