package com.danubetech.cborld.codecs;

import com.danubetech.cborld.Transformer;
import com.danubetech.cborld.util.TermInfo;
import com.upokecenter.cbor.CBORObject;

import java.util.Map;

public class VocabTermEncoder extends AbstractCborLdEncoder<String> {

    public VocabTermEncoder(String value, Transformer transformer, TermInfo termInfo) {
        super(value, transformer, termInfo);
    }

    public VocabTermEncoder(String value, Transformer transformer) {
        this(value, transformer, null);
    }

    private EncodedBytes encodeInternal(int termId) {
        return new EncodedBytes(CBORObject.FromObject(termId).EncodeToBytes());
    }

    @Override
    public EncodedBytes encode() {
        Map<String, Integer> termToId = this.transformer.termToId;
        Integer termId = termToId.get(this.value);
        if (termId != null) {
            return this.encodeInternal(termId);
        }
        return new UriEncoder(this.value).encode();
    }
}
