package com.fasterxml.jackson.databind.jsontype.impl;

import java.util.*;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.DatabindContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.cfg.MapperConfig;
import com.fasterxml.jackson.databind.jsontype.NamedType;

public class TypeNameIdResolver extends TypeIdResolverBase
{
    protected final MapperConfig<?> _config;

    /**
     * Mappings from class name to type id, used for serialization
     */
    protected final Map<String, String> _typeToId;

    /**
     * Mappings from type id to JavaType, used for deserialization
     */
    protected final Map<String, JavaType> _idToType;

    protected TypeNameIdResolver(MapperConfig<?> config, JavaType baseType,
            Map<String, String> typeToId, Map<String, JavaType> idToType)
    {
        super(baseType, config.getTypeFactory());
        _config = config;
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
        return new TypeNameIdResolver(config, baseType, typeToId, idToType);
    }

    @Override
    public JsonTypeInfo.Id getMechanism() { return JsonTypeInfo.Id.NAME; }

    @Override
    public String idFromValue(Object value) {
        return idFromClass(value.getClass());
    }

    protected String idFromClass(Class<?> clazz)
    {
        if (clazz == null) {
            return null;
        }
        Class<?> cls = _typeFactory.constructType(clazz).getRawClass();
        final String key = cls.getName();
        String name;

        synchronized (_typeToId) {
            name = _typeToId.get(key);
            if (name == null) {
                // 24-Feb-2011, tatu: As per [JACKSON-498], may need to dynamically look up name
                // can either throw an exception, or use default name...
                if (_config.isAnnotationProcessingEnabled()) {
                    BeanDescription beanDesc = _config.introspectClassAnnotations(cls);
                    name = _config.getAnnotationIntrospector().findTypeName(_config, beanDesc.getClassInfo());
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
    public String idFromValueAndType(Object value, Class<?> type) {
        /* 18-Jan-2013, tatu: We may be called with null value occasionally
         *   it seems; nothing much we can figure out that way.
         */
        if (value == null) {
            return idFromClass(type);
        }
        return idFromValue(value);
    }

    @Override
    public JavaType typeFromId(DatabindContext context, String id) {
        return _typeFromId(id);
    }
    
    protected JavaType _typeFromId(String id) {
        /* Now: if no type is found, should we try to locate it by
         * some other means? (specifically, if in same package as base type,
         * could just try Class.forName)
         * For now let's not add any such workarounds; can add if need be
         */
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
