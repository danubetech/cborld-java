package com.danubetech.cborld;

import com.apicatalog.jsonld.JsonLdError;
import com.apicatalog.jsonld.document.JsonDocument;
import com.apicatalog.jsonld.loader.DocumentLoader;
import com.danubetech.cborld.util.Aliases;
import com.danubetech.cborld.util.Context;
import com.danubetech.cborld.util.TermInfo;
import com.danubetech.cborld.util.TermInfoAndValue;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.json.Json;
import jakarta.json.JsonObject;

import java.io.StringWriter;
import java.net.URI;
import java.util.*;
import java.util.stream.Collectors;

public abstract class Transformer {

    public Map<Integer, String> idToTerm = new HashMap<>();
    public Map<String, Integer> termToId = new HashMap<>();
    public Map<String, Integer> appContextMap = new HashMap<>();
    public Map<Integer, String> reverseAppContextMap = new HashMap<>();
    public Map<Object, Map<String, Object>> contextMap = new HashMap<>();
    private DocumentLoader documentLoader;
    public int nextTermId = 0;

    public Transformer(Map<String, Integer> appContextMap, DocumentLoader documentLoader) {
        this.appContextMap = appContextMap;
        this.documentLoader = documentLoader;
    }

    // default no-op hook functions
    protected void beforeObjectContexts(Map<String, Object> obj, Map<Integer, Object> transformMap) {
    }

    protected void afterObjectContexts(Map<String, Object> obj, Map<Integer, Object> transformMap) {
    }

    protected void beforeTypeScopedContexts(Map<String, Object> activeCtx, Map<String, Object> obj, Map<Integer, Object> transformMap) {
    }

    protected void transform(Map<String, Object> obj, Map<Integer, Object> transformMap, LinkedList<Map<String, Object>> contextStack) {
        // hook before object contexts are applied
        this.beforeObjectContexts(obj, transformMap);

        // apply embedded contexts in the object
        Map<String, Object> activeCtx = this.applyEmbeddedContexts(obj, contextStack);

        // hook after object contexts are applied
        this.afterObjectContexts(obj, transformMap);

        // TODO: support `@propagate: true` on type-scoped contexts; until then
        // throw an error if it is set

        // preserve context stack before applying type-scoped contexts
        LinkedList childContextStack = new LinkedList(contextStack);

        // hook before type-scoped contexts are applied
        this.beforeTypeScopedContexts(activeCtx, obj, transformMap);

        // apply type-scoped contexts
        activeCtx = this.applyTypeScopedContexts(obj, contextStack);

        // walk term entries to transform
        Aliases aliases = (Aliases) activeCtx.get("aliases");
        Map<String, Object> scopedContextMap = Context.scopedContextMap(activeCtx);
        Map<String, Object> termMap = Context.termMap(activeCtx);
        List<TermInfoAndValue> termEntries = this.getEntries(obj, transformMap, this, termMap);

        for (TermInfoAndValue termEntry : termEntries) {
            TermInfo termInfo = termEntry.termInfo;
            Object value = termEntry != null ? termEntry.value : null;
            String term = termInfo != null ? termInfo.term : null;

            // transform `@id`
            if ("@id".equals(term) || aliases.id.contains(term)) {
                this.transformObjectId(obj, transformMap, termInfo, value);
                continue;
            }

            // transform `@type`
            if ("@type".equals(term) || aliases.type.contains(term)) {
                this.transformObjectType(obj, transformMap, termInfo, value);
                continue;
            }

            // use `childContextStack` when processing properties as it will remove
            // type-scoped contexts unless a property-scoped context is applied
            LinkedList propertyContextStack = childContextStack;

            // apply any property-scoped context
            Map<String, Object> newActiveCtx = null;
            Map<String, Object> propertyScopedContext = (Map<String, Object>) scopedContextMap.get(term);
            if (propertyScopedContext != null) {
                // TODO: support `@propagate: false` on property-scoped contexts; until
                // then throw an error if it is set
                newActiveCtx = this.applyEmbeddedContexts(
                        new HashMap<String, Object>() {
                            {
                                put("@context", propertyScopedContext);
                            }
                        },
                        contextStack);
                propertyContextStack = new LinkedList(contextStack);
            }

            // iterate through all values for the current transform entry
            Boolean plural = termInfo != null ? termInfo.plural : null;
            Object def = termInfo != null ? termInfo.def : null;
            String termType = this.getTermType(newActiveCtx != null ? newActiveCtx : activeCtx, def);
            List<Object> values = Boolean.TRUE.equals(plural) ? (List<Object>) value : Collections.singletonList(value);
            LinkedList entries = new LinkedList<>();
            for (Object ivalue : values) {
                // `null` is never transformed
                if (ivalue == null) {
                    entries.push(null);
                    continue;
                }

                // try to transform typed value
                if (this.transformTypedValue(entries, termType, ivalue, termInfo)) {
                    continue;
                }

                if (!(ivalue instanceof Map)) {
                    // value not transformed and cannot recurse, so do not transform
                    entries.push(value);
                    continue;
                }

                // transform array
                if (ivalue instanceof List) {
                    this.transformArray(entries, propertyContextStack, (List<Map<String, Object>>) ivalue);
                    continue;
                }

                // transform object
                this.transformObject(entries, propertyContextStack, (Map<String, Object>) ivalue);
            }

            // revert property-scoped active context if one was created
            if (newActiveCtx != null) {
                ((Runnable) newActiveCtx.get("revert")).run();
            }

            this.assignEntries(entries, obj, transformMap, termInfo);
        }

        // revert active context for this object
        ((Runnable) activeCtx.get("revert")).run();
    }

    protected void transform(Map<String, Object> obj, Map<Integer, Object> transformMap) {
        this.transform(obj, transformMap, new LinkedList<>());
    }

    /**
     * Apply the embedded contexts in the given object to produce an
     * active context.
     *
     * @param {object} options - The options to use.
     * @param {object} options.obj - The object to get the active context for.
     * @param {Array}  [options.contextStack] - The stack of active contexts.
     * @returns {Promise<object>} - The active context instance.
     */
    protected Map<String, Object> applyEmbeddedContexts(Map<String, Object> obj, LinkedList<Map<String, Object>> contextStack) {
        final int stackTop = contextStack.size();

        // push any local embedded contexts onto the context stack
        Object localContexts = obj.get("@context");
        this.updateContextStack(contextStack, localContexts);

        // get `id` and `type` aliases for the active context
        Map<String, Object> active = contextStack.get(contextStack.size() - 1);
        if (active == null) {
            // empty initial context
            active = new HashMap<String, Object>();
        }

        active.put("revert", (Runnable) () -> {
            while (contextStack.size() > stackTop) contextStack.removeLast();
        });
        return active;
    }

    protected Map<String, Object> applyTypeScopedContexts(Map<String, Object> obj, LinkedList<Map<String, Object>> contextStack) {
        final int stackTop = contextStack.size();

        // get `id` and `type` aliases for the active context
        Map<String, Object> active = contextStack.get(contextStack.size() - 1);
        if (active == null) {
            // empty initial context
            active = new HashMap<String, Object>();
        }
        Aliases aliases = Context.aliases(active);

        // get unique object type(s)
        Set<String> totalTypes = new HashSet<>();
        List<String> typeTerms = new ArrayList<>();
        typeTerms.add("@type");
        typeTerms.addAll(aliases.type);
        for (String term : typeTerms) {
            Object types = obj.get(term);
            if (types instanceof List) {
                totalTypes.addAll((List<String>) types);
            } else {
                if (types == null) continue;
                totalTypes.add((String) types);
            }
        }
        // apply types in lexicographically sorted order (per JSON-LD spec)
        totalTypes = totalTypes.stream().sorted().collect(Collectors.toCollection(LinkedHashSet::new));

        // apply any type-scoped contexts
        Map<String, Object> scopedContextMap = Context.scopedContextMap(active);
        for (String type : totalTypes) {
            Map<String, Object> contexts = (Map<String, Object>) scopedContextMap.get(type);
            if (contexts != null) {
                this.updateContextStack(contextStack, contexts);
                active = contextStack.get(contextStack.size() - 1);
                scopedContextMap = Context.scopedContextMap(active);
            }
        }

        active.put("revert", (Runnable) () -> {
            while (contextStack.size() > stackTop) contextStack.removeLast();
        });
        return active;
    }

    private void updateContextStack(LinkedList<Map<String, Object>> contextStack, Object contexts, Transformer transformer) {
        // push any localized contexts onto the context stack
        if (contexts == null) {
            return;
        }
        if (!(contexts instanceof List<?>)) {
            contexts = Arrays.asList(contexts);
        }

        contextMap = this.contextMap;
        for (Object context : (List<Object>) contexts) {
            Map<String, Object> entry = (Map<String, Object>) contextMap.get(context);
            if (entry == null) {
                Object ctx = context;
                String contextUrl = null;
                if (context instanceof String) {
                    // fetch context
                    contextUrl = (String) context;
                    Map<String, Object> document = this.getDocument(contextUrl);
                    ctx = document.get("@context");
                }
                // FIXME: validate `ctx` to ensure its a valid JSON-LD context value
                // add context
                entry = this.addContext((Map<String, Object>) ctx, contextUrl, transformer);
            }

            // clone entry to create new active context entry for context stack
            Map<String, Object> newActive = new HashMap<String, Object>();
            Context.aliases(newActive).id = new HashSet(Context.aliases(entry).id);
            Context.aliases(newActive).type = new HashSet(Context.aliases(entry).type);
            newActive.put("context", context);
            newActive.put("scopedContextMap", new HashMap<>(Context.scopedContextMap(entry)));
            newActive.put("termMap", new HashMap<>(Context.termMap(entry)));

            // push new active context and get old one
            Map<String, Object> oldActive = contextStack.size() > 0 ? contextStack.get(contextStack.size() - 1) : null;
            contextStack.add(newActive);
            if (oldActive == null) {
                continue;
            }

            // compute `id` and `type` aliases by including any previous aliases that
            // have not been replaced by the new context
            Aliases aliases = (Aliases) newActive.get("aliases");
            Map<String, Object> termMap = Context.termMap(newActive);
            for (String key : Arrays.asList("id", "type")) {
                for (String alias : ((Aliases) oldActive.get("aliases")).get(key)) {
                    if (!((context instanceof Map && ((Map<String, Object>) context).containsKey(key) && ((Map<String, Object>) context).get(alias) == null) || Context.termMap(newActive).containsKey(alias))) {
                        aliases.get(key).add(alias);
                    }
                }
            }

            // compute scoped context map by including any scoped contexts that have
            // not been replaced by the new context
            Map<String, Object> scopedContextMap = Context.scopedContextMap(newActive);
            for (Map.Entry<String, Object> entry2 : Context.scopedContextMap(oldActive).entrySet()) {
                String key = entry2.getKey();
                Object value = entry2.getValue();
                if (!((context instanceof Map && ((Map<String, Object>) context).containsKey(key) && ((Map<String, Object>) context).get(key) == null) || scopedContextMap.containsKey(key))) {
                    scopedContextMap.put(key, value);
                }
            }

            // compute new terms map
            for (Map.Entry<String, Object> entry2 : Context.termMap(oldActive).entrySet()) {
                String key = entry2.getKey();
                Object value = entry2.getValue();
                if (!((context instanceof Map && ((Map<String, Object>) context).containsKey(key) && ((Map<String, Object>) context).get(key) == null) || termMap.containsKey(key))) {
                    termMap.put(key, value);
                }
            }
        }
    }

    private void updateContextStack(LinkedList<Map<String, Object>> contextStack, Object contexts) {
        updateContextStack(contextStack, contexts, null);
    }

    private Map<String, Object> addContext(Map<String, Object> context, String contextUrl, Transformer transformer) {
        Map<Object, Map<String, Object>> contextMap = this.contextMap;
        Map<String, Integer> termToId = this.termToId;
        Map<Integer, String> idToTerm = this.idToTerm;

        // handle `@import`
        String importUrl = (String) context.get("@import");
        if (importUrl != null) {
            Map<String, Object> importEntry = contextMap.get(importUrl);
            if (importEntry == null) {
                Map<String, Object> document = this.getDocument(importUrl);
                Map<String, Object> importCtx = (Map<String, Object>) document.get("@context");
                importEntry = this.addContext(importCtx, importUrl);
            }
            Map<String, Object> temp = new HashMap<>((Map<String, Object>) importEntry.get("context"));
            temp.putAll((Map<String, Object>) context);
            context = temp;
        }

        // precompute any `@id` and `@type` aliases, scoped contexts, and terms
        Map<String, Object> scopedContextMap = new HashMap<>();
        Map<String, Object> termMap = new HashMap<String, Object>();
        Map<String, Object> entry = new HashMap<>();
        entry.put("context", context);
        entry.put("scopedContextMap", scopedContextMap);
        entry.put("termMap", termMap);

        // process context keys in sorted order to ensure term IDs are assigned
        // consistently
        List<String> keys = context.keySet().stream().sorted().collect(Collectors.toCollection(ArrayList::new));
        for (String key : keys) {
            Object def = ((Map<String, Object>) context).get(key);
            if (def == null) {
                continue;
            }
            if ("@id".equals(def) || (def instanceof Map && "@id".equals(((Map<String, Object>) def).get("id")))) {
                Context.aliases(entry).id.add(key);
            } else if ("@type".equals(def) || (def instanceof Map && "@type".equals(((Map<String, Object>) def).get("id")))) {
                Context.aliases(entry).type.add(key);
            }
            if (Keywords.KEYWORDS.containsKey(key)) {
                // skip keywords
                continue;
            }
            // ensure the term has been assigned an ID
            if (!termToId.containsKey(key)) {
                int id = this.nextTermId;
                this.nextTermId += 2;
                termToId.put(key, id);
                if (idToTerm != null) {
                    idToTerm.put(id, key);
                }
            }
            termMap.put(key, def);
            Map<String, Object> scopedContext = def instanceof Map ? (Map<String, Object>) ((Map<String, Object>) def).get("@context") : null;
            if (scopedContext != null) {
                scopedContextMap.put(key, scopedContext);
            }
        }

        // add entry for context URL or context object
        contextMap.put(contextUrl != null ? contextUrl : context, entry);

        return entry;
    }

    private Map<String, Object> addContext(Map<String, Object> context, String contextUrl) {
        return this.addContext(context, contextUrl, null);
    }

    private static ObjectMapper objectMapper = new ObjectMapper();

    public Map<String, Object> getDocument(String url) {
        JsonDocument document = null;
        try {
            document = (JsonDocument) this.documentLoader.loadDocument(URI.create(url), null);
        } catch (JsonLdError ex) {
            throw new CborLdException(null, "Document loader error: " + ex.getMessage(), ex);
        }
        if (document == null) {
            throw new CborLdException(null, "Cannot load context: " + url);
        }
        JsonObject jsonObject = (JsonObject) document.getJsonContent().get();
        StringWriter writer = new StringWriter();
        Json.createWriter(writer).write(jsonObject);
        try {
            return objectMapper.readValue(writer.toString(), Map.class);
        } catch (JsonProcessingException ex) {
            throw new CborLdException(null, "Cannot process JSON: " + ex.getMessage(), ex);
        }
    }

    public String getTermType(Map<String, Object> activeCtx, Object def) {
        String type = def instanceof Map ? (String) ((Map<String, Object>) def).get("@type") : null;
        if (type == null) {
            // no term type
            return null;
        }

        // check for potential CURIE value
        LinkedList<String> suffix = new LinkedList<>(Arrays.asList(type.split(":")));
        String prefix = suffix.poll();

        Object prefixDef = Context.termMap(activeCtx).get(prefix);
        if (prefixDef == null) {
            // no CURIE
            return type;
        }

        // handle CURIE
        if (prefixDef instanceof String) {
            return prefixDef + String.join(":", suffix);
        }

        // prefix definition must be an object
        if (!(prefixDef instanceof Map &&
                ((Map<String, Object>) prefixDef).get("@id") instanceof String)) {
            throw new CborLdException(CborLdException.CborLdError.ERR_INVALID_TERM_DEFINITION, "JSON-LD term definitions must be strings or objects with \"@id\".");
        }
        return ((Map<String, Object>) prefixDef).get("@id") + String.join(":", suffix);
    }

    public Integer getIdForTerm(String term, Boolean plural) {
        Integer id = this.termToId.get(term);
        if (id == null) {
            throw new CborLdException(CborLdException.CborLdError.ERR_UNDEFINED_TERM, "CBOR-LD compression requires all terms to be defined in a JSON-LD context.");
        }
        return Boolean.TRUE.equals(plural) ? id + 1 : id;
    }

    public TermInfo getTermForId(Integer id) {
        Boolean plural = (id & 1) == 1;
        String term = this.idToTerm.get(Boolean.TRUE.equals(plural) ? id - 1 : id);
        return new TermInfo(term, null, plural, null);
    }

    protected abstract List<TermInfoAndValue> getEntries(Map<String, Object> obj, Map<Integer, Object> transformMap, Transformer
            transformer, Map<String, Object> termMap);

    protected abstract void transformObjectId(Map<String, Object> obj, Map<Integer, Object> transformMap, TermInfo termInfo, Object value);

    protected abstract void transformObjectType(Map<String, Object> obj, Map<Integer, Object> transformMap, TermInfo
            termInfo, Object value);

    protected abstract boolean transformTypedValue(LinkedList entries, String termType, Object value, TermInfo
            termInfo);

    protected abstract void transformArray(LinkedList entries, LinkedList<Map<String, Object>> contextStack, List<? extends Map<? extends Object, Object>> value);

    protected abstract void transformObject(LinkedList entries, LinkedList<Map<String, Object>> contextStack, Map<? extends Object, Object> value);

    protected abstract void assignEntries(LinkedList entries, Map<String, Object> obj, Map<Integer, Object> transformMap, TermInfo
            termInfo);
}
