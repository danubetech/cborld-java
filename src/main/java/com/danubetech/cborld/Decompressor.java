package com.danubetech.cborld;

import com.apicatalog.jsonld.loader.DocumentLoader;
import com.danubetech.cborld.codecs.*;
import com.danubetech.cborld.util.Context;
import com.danubetech.cborld.util.TermInfo;
import com.danubetech.cborld.util.TermInfoAndValue;
import com.upokecenter.cbor.*;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.stream.Collectors;

public class Decompressor extends Transformer {

    public static final Map<String, Class<? extends CborLdDecoder<String>>> TYPE_DECODERS;

    static {
        TYPE_DECODERS = new HashMap<>();
        TYPE_DECODERS.put("@id", UriDecoder.class);
        TYPE_DECODERS.put("@vocab", VocabTermDecoder.class);
        TYPE_DECODERS.put("https://w3id.org/security#multibase", MultibaseDecoder.class);
        TYPE_DECODERS.put("http://www.w3.org/2001/XMLSchema#date", XsdDateDecoder.class);
        TYPE_DECODERS.put("http://www.w3.org/2001/XMLSchema#dateTime", XsdDateTimeDecoder.class);
    }

    public static final Integer CONTEXT_TERM_ID = Keywords.KEYWORDS.get("@context");
    public static final Integer CONTEXT_TERM_ID_PLURAL = CONTEXT_TERM_ID + 1;

    public static final CBORTypeMapper CBOR_TYPE_MAPPER;

    static {

        CBOR_TYPE_MAPPER = new CBORTypeMapper();
        CBOR_TYPE_MAPPER.AddConverter(Object.class, new ICBORToFromConverter<Object>() {
            @Override
            public Object FromCBORObject(CBORObject cborObject) {
                if (CBORType.Map == cborObject.getType()) {
                    Map<Integer, Object> object = new HashMap<>();
                    for (CBORObject cborKey : cborObject.getKeys()) {
                        CBORObject cborValue = cborObject.get(cborKey);
                        Integer key = cborKey.ToObject(Integer.class);
                        Object value = cborValue.ToObject(Object.class, CBOR_TYPE_MAPPER);
                        object.put(key, value);
                    }
                    return object;
                } else if (CBORType.Array == cborObject.getType()) {
                    List<Object> object = new ArrayList<>();
                    for (CBORObject entry : cborObject.getValues()) {
                        object.add(entry.ToObject(Object.class, CBOR_TYPE_MAPPER));
                    }
                    return object;
                } else if (CBORType.TextString == cborObject.getType()) {
                    return cborObject.ToObject(String.class);
                } else if (CBORType.ByteString == cborObject.getType()) {
                    return cborObject.ToObject(byte[].class);
                } else if (CBORType.Integer == cborObject.getType()) {
                    return cborObject.ToObject(Integer.class);
                } else {
                    throw new CborLdException(null, "Unexpected CBOR object type: " + cborObject.getType());
                }
            }

            @Override
            public CBORObject ToCBORObject(Object object) {
                throw new RuntimeException("Not implemented");
            }
        });
    }

    public Decompressor(DocumentLoader documentLoader, Map<String, Integer> appContextMap) {
        super(appContextMap, documentLoader);
        this.reverseAppContextMap = new HashMap<>();
        // build reverse contxt map
        if (appContextMap != null) {
            for (Map.Entry<String, Integer> entry : appContextMap.entrySet()) {
                this.reverseAppContextMap.put(entry.getValue(), entry.getKey());
            }
        }
    }

    public Object decompress(byte[] compressedBytes) {
        this.contextMap = new HashMap<>();
        this.termToId = new HashMap<>(Keywords.KEYWORDS);
        this.nextTermId = Keywords.FIRST_CUSTOM_TERM_ID;
        this.idToTerm = new HashMap<>();
        for (Map.Entry<String, Integer> entry : this.termToId.entrySet()) {
            this.idToTerm.put(entry.getValue(), entry.getKey());
        }

        // decoded output could be one or more transform maps
        Object transformMap;
        CBOREncodeOptions cborEncodeOptions = CBOREncodeOptions.Default;
        CBORObject cborObject = CBORObject.DecodeFromBytes(compressedBytes, cborEncodeOptions /* , TODO: useMaps: true */);
        PODOptions podOptions = PODOptions.Default;
/*        if (CBORType.Array == cborObject.getType()) {
            transformMap = cborObject.ToObject(List.class, podOptions);
        } else if (CBORType.Map == cborObject.getType()) {
            transformMap = cborObject.ToObject(Map.class, podOptions);
        } else {
            throw new CborLdException(null, "Unsupported CBOR object type: " + cborObject.getType());
        }*/
        transformMap = cborObject.ToObject(Object.class, CBOR_TYPE_MAPPER, podOptions);

        // handle single or multiple JSON-LD docs
        List results = new ArrayList<>();
        boolean isArray = transformMap instanceof List;
        List<Map<Integer, Object>> transformMaps = isArray ? (List<Map<Integer, Object>>) transformMap : Collections.singletonList((Map<Integer, Object>) transformMap);
        for (Map<Integer, Object> itransformMap : transformMaps) {
            Map<String, Object> obj = new HashMap<>();
            this.transform(obj, itransformMap);
            results.add(obj);
        }
        return isArray ? results : results.get(0);
    }

    @Override
    protected void beforeObjectContexts(Map<String, Object> obj, Map<Integer, Object> transformMap) {
        // decode `@context` for `transformMap`, if any
        Object encodedContext = transformMap.get(CONTEXT_TERM_ID);
        if (encodedContext != null) {
            Object decoded = new ContextDecoder(encodedContext, this).decode();
            obj.put("@context", decoded != null ? decoded : encodedContext);
        }
        Object encodedContexts = transformMap.get(CONTEXT_TERM_ID_PLURAL);
        if (encodedContexts != null) {
            if (encodedContext != null) {
                // can't use *both* the singular and plural context term ID
                throw new CborLdException(CborLdException.CborLdError.ERR_INVALID_ENCODED_CONTEXT, "Both singular and plural context IDs were found in the CBOR-LD input.");
            }
            if (!(encodedContexts instanceof List)) {
                // `encodedContexts` must be an array
                throw new CborLdException(CborLdException.CborLdError.ERR_INVALID_ENCODED_CONTEXT, "Encoded plural context value must be an array.");
            }
            List entries = new ArrayList<>();
            for (Object value : (List) encodedContexts) {
                Object decoded = new ContextDecoder(value, this).decode();
                entries.add(decoded != null ? decoded : value);
            }
            obj.put("@context", entries);
        }
    }

    @Override
    protected void beforeTypeScopedContexts(Map<String, Object> activeCtx, Map<String, Object> obj, Map<Integer, Object> transformMap) {
        // decode object types
        Map<String, Integer> termToId = this.termToId;
        List<String> typeTerms = new ArrayList<>(Collections.singletonList("@type"));
        typeTerms.addAll(Context.aliases(activeCtx).type);
        for (String term : typeTerms) {
            // check both singular and plural term IDs
            Integer termId = termToId.get(term);
            Object value = transformMap.get(termId);
            if (value == null) {
                value = transformMap.get(termId + 1);
            }
            if (value != null) {
                if (value instanceof List) {
                    obj.put(term, ((List) value).stream().map((ivalue) -> {
                        String decoded = new VocabTermDecoder(ivalue, Decompressor.this).decode();
                        return decoded != null ? decoded : ivalue;
                    }).collect(Collectors.toCollection(ArrayList::new)));
                } else {
                    String decoded = new VocabTermDecoder(value, Decompressor.this).decode();
                    obj.put(term, decoded != null ? decoded : value);
                }
            }
        }
    }

    @Override
    protected List<TermInfoAndValue> getEntries(Map<String, Object> obj, Map<Integer, Object> transformMap, Transformer transformer, Map<String, Object> termMap) {
        // get term entries to be transformed and sort by *term* to ensure term
        // IDs will be assigned in the same order that the compressor assigned them
        List<TermInfoAndValue> entries = new ArrayList<>();
        for (Map.Entry<Integer, Object> entry : transformMap.entrySet()) {
            Integer key = (Integer) entry.getKey();
            Object value = entry.getValue();
            // skip `@context`; not a term entry
            if (CONTEXT_TERM_ID.equals(key) || CONTEXT_TERM_ID_PLURAL.equals(key)) {
                continue;
            }

            // check for undefined term IDs
            TermInfo termInfo = this.getTermForId(key);
            String term = termInfo != null ? termInfo.term : null;
            Boolean plural = termInfo != null ? termInfo.plural : null;
            if (term == null) {
                throw new CborLdException(CborLdException.CborLdError.ERR_UNKNOWN_CBORLD_TERM_ID, "Unknown term ID '" + key + "' was detected in the CBOR-LD input.");
            }

            // check for undefined term
            Object def = termMap.get(term);
            if (def == null && ! (term.startsWith("@") && Keywords.KEYWORDS.containsKey(term))) {
                throw new CborLdException(CborLdException.CborLdError.ERR_UNKNOWN_CBORLD_TERM, "Unknown term \"" + term + "\" was detected in the CBOR-LD input.");
            }

            entries.add(new TermInfoAndValue(new TermInfo(term, key, plural, def), value));
        }
        return sortEntriesByTerm(entries);
    }

    protected TermInfo getTermInfo(Map<String, Object> termMap, Integer key) {
        // check for undefined term IDs
        TermInfo termInfo = this.getTermForId(key);
        String term = termInfo != null ? termInfo.term : null;
        Boolean plural = termInfo != null ? termInfo.plural : null;
        if (term == null) {
            throw new CborLdException(CborLdException.CborLdError.ERR_UNKNOWN_CBORLD_TERM_ID, "Unknown term ID '" + key + "' was detected in the CBOR-LD input.");
        }

        // check for undefined term
        Object def = termMap.get(term);
        if (def == null && ! (term.startsWith("@") && Keywords.KEYWORDS.containsKey(term))) {
            throw new CborLdException(CborLdException.CborLdError.ERR_UNKNOWN_CBORLD_TERM, "Unknown term \"" + term + "\" was detected in the CBOR-LD input.");
        }

        return new TermInfo(term, key, plural, def);
    }

    @Override
    protected void transformObjectId(Map<String, Object> obj, Map<Integer, Object> transformMap, TermInfo termInfo, String value) {
        Object decoded = new UriDecoder(value).decode();
        obj.put(termInfo.term, decoded != null ? decoded : value);
    }

    @Override
    protected void transformObjectType(Map<String, Object> obj, Map<Integer, Object> transformMap, TermInfo termInfo, Object value) {
        String term = termInfo != null ? termInfo.term : null;
        Boolean plural = termInfo != null ? termInfo.plural : null;
        List values = Boolean.TRUE.equals(plural) ? (List) value : Collections.singletonList(value);
        List entries = new ArrayList<>();
        for (Object ivalue : values) {
            Object decoded = new VocabTermDecoder(value, this).decode();
            entries.add(decoded != null ? decoded : value);
        }
        obj.put(term, Boolean.TRUE.equals(plural) ? entries : entries.get(0));
    }

    @Override
    protected boolean transformTypedValue(LinkedList entries, String termType, Object value, TermInfo termInfo) {
        Class<? extends CborLdDecoder<String>> decoderClass = termType != null ? TYPE_DECODERS.get(termType) : null;
        CborLdDecoder<String> decoder = null;
        if (decoderClass != null) {
            try {
                Constructor<? extends CborLdDecoder<String>> constructor = decoderClass.getConstructor(value.getClass(), Transformer.class, TermInfo.class);
                decoder = constructor.newInstance(value, this, termInfo);
            } catch (NoSuchMethodException ex) {
                return false;
            } catch (InstantiationException | IllegalAccessException | InvocationTargetException ex) {
                throw new RuntimeException("Cannot instantiate decoder: " + ex.getMessage(), ex);
            }
        }
        Object decoded = decoder != null ? decoder.decode() : null;
        if (decoded != null) {
            entries.add(decoded);
            return true;
        }
        return false;
    }

    @Override
    public void transformArray(LinkedList entries, LinkedList<Map<String, Object>> contextStack, List<? extends Map<? extends Object, Object>> value) {
        // recurse into array
        List<Map<String, Object>> children = new ArrayList<>();
        for (Map<Integer, Object> transformMap : (List<Map<Integer, Object>>) value) {
            Map<String, Object> obj = new HashMap<>();
            children.add(obj);
            this.transform(obj, transformMap, contextStack);
        }
        entries.add(children);
    }

    @Override
    public void transformObject(LinkedList entries, LinkedList<Map<String, Object>> contextStack, Map<? extends Object, Object> value) {
        // recurse into object
        Map<String, Object> child = new HashMap<>();
        entries.add(child);
        this.transform(child, (Map<Integer, Object>) value, contextStack);
    }

    @Override
    public void assignEntries(LinkedList entries, Map<String, Object> obj, Map<Integer, Object> transformMap, TermInfo termInfo) {
        String term = termInfo != null ? termInfo.term : null;
        Boolean plural = termInfo != null ? termInfo.plural : null;
        obj.put(term, Boolean.TRUE.equals(plural) ? entries : entries.get(0));
    }

    private static List<TermInfoAndValue> sortEntriesByTerm(List<TermInfoAndValue> entries) {
        Collections.sort(entries, new Comparator<TermInfoAndValue>() {
            @Override
            public int compare(TermInfoAndValue t1, TermInfoAndValue t2) {
                return t1.termInfo.term.compareTo(t2.termInfo.term);
            }
        });
        return entries;
    }
}