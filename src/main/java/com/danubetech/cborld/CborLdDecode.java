package com.danubetech.cborld;

import com.apicatalog.jsonld.loader.DocumentLoader;
import com.upokecenter.cbor.CBOREncodeOptions;
import com.upokecenter.cbor.CBORObject;

import java.util.HashMap;
import java.util.Map;

public class CborLdDecode {

    public static Object decode(byte[] cborLdBytes, DocumentLoader documentLoader, Map<String, Integer> appContextMap) {

        // 0xd9 == 11011001
        // 110 = CBOR major type 6
        // 11001 = 25, 16-bit tag size (65536 possible values)
        int index = 0;
        if (cborLdBytes[index++] != (byte) 0xd9) {
            throw new CborLdException(CborLdException.CborLdError.ERR_NOT_CBORLD, "CBOR-LD must start with a CBOR major type \"Tag\" header of `0xd9`.");
        }

        // ensure `cborldBytes` represent CBOR-LD
        if (cborLdBytes[index++] != (byte) 0x05) {
            throw new CborLdException(CborLdException.CborLdError.ERR_NOT_CBORLD, "CBOR-LD 16-bit tag must start with `0x05`.");
        }

        int compressionMode = (int) cborLdBytes[index];
        if (compressionMode != 0 && compressionMode != 1) {
            throw new CborLdException(CborLdException.CborLdError.ERR_NOT_CBORLD, "Unsupported CBOR-LD compression mode: " + compressionMode);
        }

        index++;
        byte[] suffix = new byte[cborLdBytes.length - index];
        System.arraycopy(cborLdBytes, index, suffix, 0, cborLdBytes.length - index);

        // handle uncompressed CBOR-LD
        if (compressionMode == 0) {
            CBORObject.DecodeFromBytes(suffix);
        }

        // decompress CBOR-LD
        Decompressor decompressor = new Decompressor(documentLoader, appContextMap);
        Object jsonLdDocument = decompressor.decompress(suffix /* , {useMaps: false} */);

        // done
        return jsonLdDocument;
    }

    public static Object decode(byte[] bytes, DocumentLoader documentLoader) {

        return decode(bytes, documentLoader, new HashMap<String, Integer>());
    }
}
