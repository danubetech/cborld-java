package com.danubetech.cborld.codecs;

import com.danubetech.cborld.Transformer;
import com.danubetech.cborld.util.TermInfo;
import com.google.api.client.util.DateTime;
import com.upokecenter.cbor.CBORObject;

public class XsdDateEncoder extends AbstractCborLdEncoder<String> {

    public XsdDateEncoder(String value, Transformer transformer, TermInfo termInfo) {
        super(value, transformer, termInfo);
    }

    private EncodedBytes encodeInternal(DateTime parsedDateTime) {
        long parsed = parsedDateTime.getValue();
        long secondsSinceEpoch = (long) Math.floor(((double) parsed) / 1000);
        String dateString = new DateTime(parsed * 1000, parsedDateTime.getTimeZoneShift()).toStringRfc3339();
        String expectedDate = dateString.substring(0, dateString.indexOf('T'));
        if (!this.value.equals(expectedDate)) {
            // compression would be lossy, do not compress
            return new EncodedBytes(CBORObject.FromObject(this.value).EncodeToBytes());
        }
        return new EncodedBytes(CBORObject.FromObject(secondsSinceEpoch).EncodeToBytes());
    }

    @Override
    public EncodedBytes encode() {
        if (this.value.contains("T")) {
            // time included, cannot compress
            return null;
        }
        DateTime parsedDateTime = DateTime.parseRfc3339(this.value);
        if (parsedDateTime == null) {
            // no date parsed, cannot compress
            return null;
        }
        return this.encodeInternal(parsedDateTime);
    }
}
