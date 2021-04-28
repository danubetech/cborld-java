package com.danubetech.cborld.util;

public class TermInfo {
    public String term;
    public Integer termId;
    public Boolean plural;
    public Object def;

    public TermInfo(String term, Integer termId, Boolean plural, Object def) {
        this.term = term;
        this.termId = termId;
        this.plural = plural;
        this.def = def;
    }

    public TermInfo() {
    }
}
