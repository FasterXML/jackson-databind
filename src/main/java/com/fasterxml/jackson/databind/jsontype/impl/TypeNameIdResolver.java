package com.fasterxml.jackson.databind.jsontype.impl;

import java.util.*;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

import com.fasterxml.jackson.databind.DatabindContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.cfg.MapperConfig;
import com.fasterxml.jackson.databind.jsontype.NamedType;

public class TypeNameIdResolver extends TypeIdResolverBase
{
    /**
     * Mappings from class name to type id, used for serialization
     */
    protected final Map<String, String> _typeToId;

    /**
     * Mappings from type id to JavaType, used for deserialization
     */
    protected final Map<String, JavaType> _idToType;

    protected TypeNameIdResolver(JavaType baseType,
            Map<String, String> typeToId, Map<String, JavaType> idToType)
    {
        super(baseType);
        _typeToId = typeToId;
        _idToType = idToType;
    }
 
    public static TypeNameIdResolver construct(MapperConfig<?> config, JavaType baseType,
            Collection<NamedType> subtypes, boolean forSer, boolean forDeser)
    {
        // sanity check
        if (forSer == forDeser) throw new IllegalArgumentException();
        Map<String, String> typeToId = null;
        Map<String, JavaType> idToType = null;

        if (forSer) {
            typeToId = new HashMap<String, String>();
        }
        if (forDeser) {
            idToType = new HashMap<String, JavaType>();
            // 14-Apr-2016, tatu: Apparently needed for special case of `defaultImpl`;
            //    see [databind#1198] for details.
            typeToId = new TreeMap<String, String>();
        }
        if (subtypes != null) {
            for (NamedType t : subtypes) {
                /* no name? Need to figure out default; for now, let's just
                 * use non-qualified class name
                 */
                Class<?> cls = t.getType();
                String id = t.hasName() ? t.getName() : _defaultTypeId(cls);
                if (forSer) {
                    typeToId.put(cls.getName(), id);
                }
                if (forDeser) {
                    // One more problem; sometimes we have same name for multiple types;
                    // if so, use most specific
                    JavaType prev = idToType.get(id);
                    if (prev != null) { // Can only override if more specific
                        if (cls.isAssignableFrom(prev.getRawClass())) { // nope, more generic (or same)
                            continue;
                        }
                    }
                    idToType.put(id, config.constructType(cls));
                }
            }
        }
        return new TypeNameIdResolver(baseType, typeToId, idToType);
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

//        cls = _typeFactory.constructType(cls).getRawClass();

        final String key = cls.getName();
        String name;

        synchronized (_typeToId) {
            name = _typeToId.get(key);
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
        // Now: if no type is found, should we try to locate it by
        // some other means? (specifically, if in same package as base type,
        // could just try Class.forName)
        // For now let's not add any such workarounds; can add if need be
        return _idToType.get(id);
    }    

    @Override
    public String getDescForKnownTypeIds() {
        return new TreeSet<String>(_idToType.keySet()).toString();
    }

    @Override
    public String toString() {
        return String.format("[%s; id-to-type=%s]", getClass().getName(), _idToType);
    }

    /*
    /*********************************************************
    /* Helper methods
    /*********************************************************
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
