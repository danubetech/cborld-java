package com.danubetech.cborld.codecs;

import com.danubetech.cborld.Transformer;
import com.danubetech.cborld.util.TermInfo;
import com.google.api.client.util.DateTime;
import com.upokecenter.cbor.CBORObject;

import java.io.IOException;
import java.util.Map;

public class XsdDateTimeEncoder extends AbstractCborLdEncoder<String> {

    public XsdDateTimeEncoder(String value, Transformer transformer, TermInfo termInfo) {
        super(value, transformer, termInfo);
    }

    private EncodedBytes encodeInternal(long parsed) {
        long secondsSinceEpoch = (long) Math.floor(((double) parsed) / 1000);
        CBORObject secondsCBORObject = CBORObject.FromObject(secondsSinceEpoch);
        int millisecondIndex = this.value.indexOf('.');
        if (millisecondIndex == -1) {
            String expectedDate = new DateTime(secondsSinceEpoch * 1000).toStringRfc3339().replace(".000Z", "Z");
            if (! this.value.equals(expectedDate)) {
                // compression would be lossy, do not compress
                return new EncodedBytes(CBORObject.FromObject(this.value).EncodeToBytes());
            }
            // compress with second precision
            return new EncodedBytes(secondsCBORObject.EncodeToBytes());
        }

        int milliseconds = Integer.parseInt(this.value.substring(millisecondIndex + 1).replaceAll("[^\\d]", ""), 10);
        String expectedDate = new DateTime(secondsSinceEpoch * 1000 + milliseconds).toStringRfc3339();
        if (! this.value.equals(expectedDate)) {
            // compression would be lossy, do not compress
            return new EncodedBytes(CBORObject.FromObject(this.value).EncodeToBytes());
        }

        // compress with subsecond precision
        CBORObject entries = CBORObject.NewArray();
        entries.Add(secondsCBORObject);
        entries.Add(CBORObject.FromObject(milliseconds));
        return new EncodedBytes(entries.EncodeToBytes());
    }

    @Override
    public EncodedBytes encode() {
        if (! this.value.contains("T")) {
            // no time included, cannot compress
            return null;
        }
        DateTime parsedDateTime = DateTime.parseRfc3339(this.value);
        if (parsedDateTime == null) {
            // no date parsed, cannot compress
            return null;
        }
        long parsed = parsedDateTime.getValue();
        return this.encodeInternal(parsed);
    }
}
