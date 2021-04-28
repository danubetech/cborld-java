package com.danubetech.cborld.codecs;

import com.danubetech.cborld.Transformer;
import com.danubetech.cborld.util.TermInfo;

import java.util.List;

public class VocabTermDecoder extends AbstractCborLdDecoder<String> {

    public VocabTermDecoder(Object value, Transformer transformer, TermInfo termInfo) {
        super(value, transformer, termInfo);
    }

    public VocabTermDecoder(Object value, Transformer transformer) {
        this(value, transformer, null);
    }

    private String decodeInternal(String term) {
        return term;
    }

    @Override
    public String decode() {
        if (this.value instanceof List<?>) {
            return new UriDecoder(this.value, this.transformer, this.termInfo).decode();
        }
        String term = this.transformer.idToTerm.get(((Number) this.value).intValue());
        if (term != null) {
            return this.decodeInternal(term);
        }
        return null;
    }
}
