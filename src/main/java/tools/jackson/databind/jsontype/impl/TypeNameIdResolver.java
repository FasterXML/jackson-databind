package tools.jackson.databind.jsontype.impl;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

import tools.jackson.databind.DatabindContext;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.MapperFeature;
import tools.jackson.databind.cfg.MapperConfig;
import tools.jackson.databind.jsontype.NamedType;

public class TypeNameIdResolver extends TypeIdResolverBase
{
    /**
     * Mappings from class name to type id, used for serialization.
     *<p>
     * Since lazily constructed will require synchronization (either internal
     * by type, or external)
     */
    protected final ConcurrentHashMap<String, String> _typeToId;

    /**
     * Mappings from type id to JavaType, used for deserialization.
     *<p>
     * Eagerly constructed, not modified, can use regular unsynchronized {@link Map}.
     */
    protected final Map<String, JavaType> _idToType;

    protected final boolean _caseInsensitive;

    protected TypeNameIdResolver(JavaType baseType,
            ConcurrentHashMap<String, String> typeToId,
            HashMap<String, JavaType> idToType,
            boolean caseInsensitive)
    {
        super(baseType);
        _typeToId = typeToId;
        _idToType = idToType;
        _caseInsensitive = caseInsensitive;
    }

    public static TypeNameIdResolver construct(MapperConfig<?> config, JavaType baseType,
            Collection<NamedType> subtypes, boolean forSer, boolean forDeser)
    {
        // sanity check
        if (forSer == forDeser) throw new IllegalArgumentException();

        final ConcurrentHashMap<String, String> typeToId;
        final HashMap<String, JavaType> idToType;

        if (forSer) {
            // Only need Class-to-id for serialization; but synchronized since may be
            // lazily built (if adding type-id-mappings dynamically)
            typeToId = new ConcurrentHashMap<>();
            idToType = null;
        } else {
            idToType = new HashMap<>();
            // 14-Apr-2016, tatu: Apparently needed for special case of `defaultImpl`;
            //    see [databind#1198] for details: but essentially we only need room
            //    for a single value.
            typeToId = new ConcurrentHashMap<>(4);
        }
        final boolean caseInsensitive = config.isEnabled(MapperFeature.ACCEPT_CASE_INSENSITIVE_VALUES);

        if (subtypes != null) {
            for (NamedType t : subtypes) {
                // no name? Need to figure out default; for now, let's just
                // use non-qualified class name
                Class<?> cls = t.getType();
                String id = t.hasName() ? t.getName() : _defaultTypeId(cls);
                if (forSer) {
                    typeToId.put(cls.getName(), id);
                }
                if (forDeser) {
                    // [databind#1983]: for case-insensitive lookups must canonicalize:
                    if (caseInsensitive) {
                        id = id.toLowerCase();
                    }
                    // One more problem; sometimes we have same name for multiple types;
                    // if so, use most specific
                    JavaType prev = idToType.get(id); // lgtm [java/dereferenced-value-may-be-null]
                    if (prev != null) { // Can only override if more specific
                        if (cls.isAssignableFrom(prev.getRawClass())) { // nope, more generic (or same)
                            continue;
                        }
                    }
                    idToType.put(id, config.constructType(cls));
                }
            }
        }
        return new TypeNameIdResolver(baseType, typeToId, idToType, caseInsensitive);
    }

    @Override
    public JsonTypeInfo.Id getMechanism() { return JsonTypeInfo.Id.NAME; }

    @Override
    public String idFromValue(DatabindContext ctxt, Object value) {
        return idFromClass(ctxt, value.getClass());
    }

    protected String idFromClass(DatabindContext ctxt, Class<?> cls)
    {
        if (cls == null) {
            return null;
        }
        // 12-Oct-2019, tatu: This looked weird; was done in 2.x to force application
        //   of `TypeModifier`. But that just... does not seem right, at least not in
        //   the sense that raw class would be changed (intent for modifier is to change
        //   `JavaType` being resolved, not underlying class. Hence commented out in
        //   3.x. There should be better way to support whatever the use case is.

        // 29-Nov-2019, tatu: Looking at 2.x, test in `TestTypeModifierNameResolution` suggested
        //   that use of `TypeModifier` was used for demoting some types (from impl class to
        //   interface. For what that's worth. Still not supported for 3.x until proven necessary

//        cls = _typeFactory.constructType(cls).getRawClass();

        final String key = cls.getName();
        String name = _typeToId.get(key);

        if (name == null) {
            // 24-Feb-2011, tatu: As per [JACKSON-498], may need to dynamically look up name
            // can either throw an exception, or use default name...
            if (ctxt.isAnnotationProcessingEnabled()) {
                name = ctxt.getAnnotationIntrospector().findTypeName(ctxt.getConfig(),
                        ctxt.introspectClassAnnotations(cls));
            }
            if (name == null) {
                // And if still not found, let's choose default?
                name = _defaultTypeId(cls);
            }
            _typeToId.put(key, name);
        }
        return name;
    }

    @Override
    public String idFromValueAndType(DatabindContext ctxt, Object value, Class<?> type) {
        // 18-Jan-2013, tatu: We may be called with null value occasionally
        //   it seems; nothing much we can figure out that way.
        if (value == null) {
            return idFromClass(ctxt, type);
        }
        return idFromValue(ctxt, value);
    }

    @Override
    public JavaType typeFromId(DatabindContext context, String id) {
        return _typeFromId(id);
    }
    
    protected JavaType _typeFromId(String id) {
        // [databind#1983]: for case-insensitive lookups must canonicalize:
        if (_caseInsensitive) {
            id = id.toLowerCase();
        }
        // Now: if no type is found, should we try to locate it by
        // some other means? (specifically, if in same package as base type,
        // could just try Class.forName)
        // For now let's not add any such workarounds; can add if need be
        return _idToType.get(id);
    }    

    @Override
    public String getDescForKnownTypeIds() {
        // 05-May-2020, tatu: As per [databind#1919], only include ids for
        //    non-abstract types
        final TreeSet<String> ids = new TreeSet<>();
        for (Map.Entry<String, JavaType> entry : _idToType.entrySet()) {
            if (entry.getValue().isConcrete()) {
                ids.add(entry.getKey());
            }
        }
        return ids.toString();
    }

    /*
    @Override
    public String toString() {
        return String.format("[%s; id-to-type=%s]", getClass().getName(), _idToType);
    }
    */

    /*
    /**********************************************************************
    /* Helper methods
    /**********************************************************************
     */
    
    /**
     * If no name was explicitly given for a class, we will just
     * use non-qualified class name
     */
    protected static String _defaultTypeId(Class<?> cls)
    {
        String n = cls.getName();
        int ix = n.lastIndexOf('.');
        return (ix < 0) ? n : n.substring(ix+1);
    }
}
