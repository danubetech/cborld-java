package com.danubetech.cborld.codecs;

import com.danubetech.cborld.Transformer;
import com.danubetech.cborld.util.TermInfo;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UriDecoder extends AbstractCborLdDecoder<String> {

    public static final Map<Integer, Class<? extends CborLdDecoder<String>>> SCHEME_ID_TO_ENCODER;

    static {
        SCHEME_ID_TO_ENCODER = new HashMap<>();
        SCHEME_ID_TO_ENCODER.put(1, HttpUrlDecoder.class);
        SCHEME_ID_TO_ENCODER.put(2, HttpUrlDecoder.class);
        SCHEME_ID_TO_ENCODER.put(3, UuidUrnDecoder.class);
        SCHEME_ID_TO_ENCODER.put(1024, Base58DidUrlDecoder.class);
        SCHEME_ID_TO_ENCODER.put(1025, Base58DidUrlDecoder.class);
    }

    public UriDecoder(Object value, Transformer transformer, TermInfo termInfo) {
        super(value, transformer, termInfo);
    }

    public UriDecoder(Object value) {
        this(value, null, null);
    }

    @Override
    public String decode() {
        if (!(this.value instanceof List<?> && ((List<?>) this.value).size() > 1)) {
            return null;
        }

        Class<? extends CborLdDecoder<String>> cl = SCHEME_ID_TO_ENCODER.get(((Number) ((List<?>) this.value).get(0)).intValue());
        if (cl == null) return null;
        return createDecoder(cl, this.value, this.transformer, this.termInfo).decode();
    }

    private static <T> CborLdDecoder<T> createDecoder(Class<? extends CborLdDecoder<T>> cl, Object value, Transformer transformer, TermInfo termInfo) {
        try {
            return cl.getConstructor(Object.class, Transformer.class, TermInfo.class).newInstance(value, transformer, termInfo);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException ex) {
            throw new RuntimeException("Cannot instantiate decoder " + cl.getName() + ": " + ex.getMessage(), ex);
        }
    }
}
