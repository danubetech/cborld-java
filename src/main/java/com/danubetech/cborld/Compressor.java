package com.danubetech.cborld;

import com.apicatalog.jsonld.loader.DocumentLoader;
import com.danubetech.cborld.codecs.CborLdEncoder;
import com.danubetech.cborld.codecs.*;
import com.danubetech.cborld.util.TermInfo;
import com.danubetech.cborld.util.TermInfoAndValue;
import com.upokecenter.cbor.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.stream.Collectors;

public class Compressor extends Transformer {

    public static final Map<String, Class<? extends CborLdEncoder<String>>> TYPE_ENCODERS;

    static {
        TYPE_ENCODERS = new HashMap<>();
        TYPE_ENCODERS.put("@id", UriEncoder.class);
        TYPE_ENCODERS.put("@vocab", VocabTermEncoder.class);
        TYPE_ENCODERS.put("https://w3id.org/security#multibase", MultibaseEncoder.class);
        TYPE_ENCODERS.put("http://www.w3.org/2001/XMLSchema#date", XsdDateEncoder.class);
        TYPE_ENCODERS.put("http://www.w3.org/2001/XMLSchema#dateTime", XsdDateTimeEncoder.class);
    }

    public static final Integer CONTEXT_TERM_ID = Keywords.KEYWORDS.get("@context");
    public static final Integer CONTEXT_TERM_ID_PLURAL = CONTEXT_TERM_ID + 1;

    public static final CBORTypeMapper CBOR_TYPE_MAPPER;

    static {

        CBOR_TYPE_MAPPER = new CBORTypeMapper();
        CBOR_TYPE_MAPPER.AddConverter(CborLdEncoder.EncodedBytes.class, new ICBORConverter<CborLdEncoder.EncodedBytes>() {
            @Override
            public CBORObject ToCBORObject(CborLdEncoder.EncodedBytes encodedBytes) {
                return CBORObject.DecodeFromBytes(encodedBytes.bytes);
            }
        });
    }

    public Compressor(DocumentLoader documentLoader, Map<String, Integer> appContextMap) {
        super(appContextMap, documentLoader);
    }

    public void compress(Map<String, Object> jsonldDocument, OutputStream out) throws IOException {
        Object transformMaps = this.createTransformMaps(jsonldDocument);
        PODOptions podOptions = PODOptions.Default;
        CBORObject cborObject = CBORObject.FromObject(transformMaps, CBOR_TYPE_MAPPER, podOptions);
        CBOREncodeOptions cborEncodeOptions = CBOREncodeOptions.Default;
        cborObject.WriteTo(out, cborEncodeOptions);
    }

    private Object createTransformMaps(Object jsonldDocument) {
        // initialize state
        this.contextMap = new HashMap<>();
        this.termToId = new HashMap<>(Keywords.KEYWORDS);
        this.nextTermId = Keywords.FIRST_CUSTOM_TERM_ID;

        // handle single or multiple JSON-LD docs
        List<Map> transformMaps = new ArrayList<>();
        boolean isArray = jsonldDocument instanceof List;
        List<Map<String, Object>> docs = isArray ? (List<Map<String, Object>>) jsonldDocument : Collections.singletonList((Map<String, Object>) jsonldDocument);
        for (Map<String, Object> obj : docs) {
            Map<Integer, Object> transformMap = new HashMap<>();
            this.transform(obj, transformMap);
            transformMaps.add(transformMap);
        }

        return isArray ? transformMaps : transformMaps.get(0);
    }

    @Override
    protected void afterObjectContexts(Map<String, Object> obj, Map<Integer, Object> transformMap) {
        // if `@context` is present in the object, encode it
        Object context = obj.get("@context");
        if (context == null) {
            return;
        }

        List entries = new ArrayList();
        boolean isArray = context instanceof List;
        List contexts = isArray ? (List) context : Collections.singletonList(context);
        for (Object value : contexts) {
            ContextEncoder encoder = new ContextEncoder((String) value, this);
            CborLdEncoder.EncodedBytes encoded = encoder.encode();
            entries.add(encoded != null ? encoded : value);
        }
        Integer id = isArray ? CONTEXT_TERM_ID_PLURAL : CONTEXT_TERM_ID;
        transformMap.put(id, isArray ? entries : entries.get(0));
    }

    @Override
    protected List<TermInfoAndValue> getEntries(Map<String, Object> obj, Map<Integer, Object> transformMap, Transformer transformer, Map<String, Object> termMap) {
        // get term entries to be transformed and sort by *term* to ensure term
        // IDs will be assigned in the same order that the decompressor will
        List<TermInfoAndValue> entries = new ArrayList<>();
        List<String> keys = obj.keySet().stream().sorted().collect(Collectors.toCollection(ArrayList::new));
        for (String key : keys) {
            // skip `@context`; not a term entry
            if ("@context".equals(key)) {
                continue;
            }

            // check for undefined terms
            Object def = termMap.get(key);
            if (def == null && !(key.startsWith("@") && Keywords.KEYWORDS.containsKey(key))) {
                throw new CborLdException(CborLdException.CborLdError.ERR_UNKNOWN_CBORLD_TERM, "Unknown term '" + key + "' was detected in the JSON-LD input.");
            }

            Object value = obj.get(key);
            Boolean plural = value instanceof List;
            Integer termId = this.getIdForTerm(key, plural);
            entries.add(new TermInfoAndValue(new TermInfo(key, termId, plural, def), value));
        }
        return entries;
    }

    @Override
    public void transformObjectId(Map<String, Object> obj, Map<Integer, Object> transformMap, TermInfo termInfo, String value) {
        Integer termId = termInfo != null ? termInfo.termId : null;
        CborLdEncoder.EncodedBytes encoded = new UriEncoder(value, this, termInfo).encode();
        transformMap.put(termId, encoded != null ? encoded : value);
    }

    @Override
    public void transformObjectType(Map<String, Object> obj, Map<Integer, Object> transformMap, TermInfo termInfo, Object value) {
        Integer termId = termInfo != null ? termInfo.termId : null;
        Boolean plural = termInfo != null ? termInfo.plural : null;
        List<String> values = Boolean.TRUE.equals(plural) ? (List<String>) value : Collections.singletonList((String) value);
        List<Object> entries = new ArrayList<>();
        for (String ivalue : values) {
            CborLdEncoder.EncodedBytes encoded = new VocabTermEncoder(ivalue, this, termInfo).encode();
            entries.add(encoded != null ? encoded : ivalue);
        }
        transformMap.put(termId, Boolean.TRUE.equals(plural) ? entries : entries.get(0));
    }

    @Override
    public boolean transformTypedValue(LinkedList entries, String termType, Object value, TermInfo termInfo) {
        Class<? extends CborLdEncoder<String>> encoderClass = termType != null ? TYPE_ENCODERS.get(termType) : null;
        CborLdEncoder<String> encoder = null;
        if (encoderClass != null) {
            try {
                Constructor<? extends CborLdEncoder<String>> constructor = encoderClass.getConstructor(value.getClass(), Transformer.class, TermInfo.class);
                encoder = constructor.newInstance(value, this, termInfo);
            } catch (NoSuchMethodException ex) {
                return false;
            } catch (InstantiationException | IllegalAccessException | InvocationTargetException ex) {
                throw new RuntimeException("Cannot instantiate encoder: " + ex.getMessage(), ex);
            }
        }
        CborLdEncoder.EncodedBytes encoded = encoder != null ? encoder.encode() : null;
        if (encoded != null) {
            entries.add(encoded);
            return true;
        }
        return false;
    }

    @Override
    public void transformArray(LinkedList entries, LinkedList<Map<String, Object>> contextStack, List<? extends Map<? extends Object, Object>> value) {
        // recurse into array
        List<Map<Integer, Object>> children = new ArrayList<>();
        for (Map<String, Object> obj : (List<Map<String, Object>>) value) {
            Map<Integer, Object> childMap = new HashMap<>();
            children.add(childMap);
            this.transform(obj, childMap, contextStack);
        }
        entries.add(children);
    }

    @Override
    public void transformObject(LinkedList entries, LinkedList<Map<String, Object>> contextStack, Map<? extends Object, Object> value) {
        // recurse into object
        Map<Integer, Object> transformMap = new HashMap<>();
        entries.add(transformMap);
        this.transform((Map<String, Object>) value, transformMap, contextStack);
    }

    @Override
    public void assignEntries(LinkedList entries, Map<String, Object> obj, Map<Integer, Object> transformMap, TermInfo termInfo) {
        Integer termId = termInfo != null ? termInfo.termId : null;
        Boolean plural = termInfo != null ? termInfo.plural : null;
        transformMap.put(termId, Boolean.TRUE.equals(plural) ? entries : entries.get(0));
    }
}