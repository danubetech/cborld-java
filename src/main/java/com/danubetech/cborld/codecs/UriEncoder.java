package com.danubetech.cborld.codecs;

import com.danubetech.cborld.Transformer;
import com.danubetech.cborld.util.TermInfo;

import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.util.*;

public class UriEncoder extends AbstractCborLdEncoder<String> {

    public static final Map<String, Class<? extends CborLdEncoder<String>>> SCHEME_TO_ENCODER;

    static {
        SCHEME_TO_ENCODER = new HashMap<>();
        SCHEME_TO_ENCODER.put("http", HttpUrlEncoder.class);
        SCHEME_TO_ENCODER.put("https", HttpUrlEncoder.class);
        SCHEME_TO_ENCODER.put("urn:uuid", UuidUrnEncoder.class);
        SCHEME_TO_ENCODER.put("did:v1:nym", Base58DidUrlEncoder.class);
        SCHEME_TO_ENCODER.put("did:key", Base58DidUrlEncoder.class);
    }

    public UriEncoder(String value, Transformer transformer, TermInfo termInfo) {
        super(value, transformer, termInfo);
    }

    public UriEncoder(String value) {
        this(value, null, null);
    }

    @Override
    public EncodedBytes encode() {
        // get full colon-delimited prefix
        URI uri = URI.create(this.value);
        String scheme = uri.getScheme();
        String pathname = uri.getPath();
        if (pathname != null && pathname.contains(":")) scheme += pathname;
        List<String> split = new ArrayList<String>(Arrays.asList(this.value.split(":")));
        split.remove(split.size() - 1);
        scheme = String.join(":", split);

        Class<? extends CborLdEncoder<String>> cl = SCHEME_TO_ENCODER.get(scheme);
        if (cl == null) return null;
        return createEncoder(cl, this.value, this.transformer, this.termInfo).encode();
    }

    private static <T> CborLdEncoder<T> createEncoder(Class<? extends CborLdEncoder<T>> cl, T value, Transformer transformer, TermInfo termInfo) {
        try {
            return cl.getConstructor(String.class, Transformer.class, TermInfo.class).newInstance(value, transformer, termInfo);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException ex) {
            throw new RuntimeException("Cannot instantiate encoder " + cl.getName() + ": " + ex.getMessage(), ex);
        }
    }
}
