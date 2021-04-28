package com.danubetech.cborld;

import com.apicatalog.jsonld.loader.DocumentLoader;
import com.upokecenter.cbor.CBORObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class CborLdEncode {

    public static byte[] encode(Map<String, Object> jsonLdDocument, DocumentLoader documentLoader, Map<String, Integer> appContextMap, int compressionMode) throws IOException {

        if (compressionMode != 0 && compressionMode != 1) {
            throw new IllegalArgumentException("'compressionMode' must be '0' (no compression) or '1' (for compression mode version 1.'");
        }

        // 0xd9 == 11011001
        // 110 = CBOR major type 6
        // 11001 = 25, 16-bit tag size (65536 possible values)
        // 0x05 = always the first 8-bits of a CBOR-LD tag
        // compressionMode = last 8-bits of a CBOR-LD tag indicating compression type
        byte[] prefix = new byte[]{(byte) 0xd9, (byte) 0x05, (byte) compressionMode};
        ByteArrayOutputStream suffix = new ByteArrayOutputStream();

        if (compressionMode == 0) {
            CBORObject.Write(jsonLdDocument, suffix);
        } else {
            Compressor compressor = new Compressor(documentLoader, appContextMap);
            compressor.compress(jsonLdDocument, suffix);
        }

        // concatenate prefix and suffix
        int length = prefix.length + suffix.size();
        byte[] cborLdBytes = new byte[length];
        System.arraycopy(prefix, 0, cborLdBytes, 0, prefix.length);
        System.arraycopy(suffix.toByteArray(), 0, cborLdBytes, prefix.length, suffix.size());

        // done
        return cborLdBytes;
    }

    public static byte[] encode(Map<String, Object> jsonLdDocument, DocumentLoader documentLoader, Map<String, Integer> appContextMap) throws IOException {

        return encode(jsonLdDocument, documentLoader, appContextMap, 1);
    }

    public static byte[] encode(Map<String, Object> jsonLdDocument, DocumentLoader documentLoader) throws IOException {

        return encode(jsonLdDocument, documentLoader, new HashMap<String, Integer>(), 1);
    }
}
