package com.fasterxml.jackson.databind.jsontype.impl;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.DatabindContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.cfg.MapperConfig;
import com.fasterxml.jackson.databind.jsontype.NamedType;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;

/**
 * {@link com.fasterxml.jackson.databind.jsontype.TypeIdResolver} implementation
 * that converts between (JSON) Strings and simple Java class names via {@link Class#getSimpleName()}.
 * <p>
 * Note that this implementation is identical to {@link TypeNameIdResolver} except that instead of 
 * {@link TypeNameIdResolver#_defaultTypeId(Class)}, this implementation uses {@link Class#getSimpleName()}.
 * 
 * @since 2.16
 */
public class SimpleClassNameIdResolver
    extends TypeIdResolverBase
{
    protected final MapperConfig<?> _config;

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

    /**
     * @since 2.11
     */
    protected final boolean _caseInsensitive;

    protected SimpleClassNameIdResolver(MapperConfig<?> config, JavaType baseType,
            ConcurrentHashMap<String, String> typeToId,
            HashMap<String, JavaType> idToType)
    {
        super(baseType, config.getTypeFactory());
        _config = config;
        _typeToId = typeToId;
        _idToType = idToType;
        _caseInsensitive = config.isEnabled(MapperFeature.ACCEPT_CASE_INSENSITIVE_VALUES);
    }

    public static SimpleClassNameIdResolver construct(MapperConfig<?> config, JavaType baseType,
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
                String id = t.hasName() ? t.getName() : cls.getSimpleName(); // not {@code _defaultTypeId(cls);}
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
        return new SimpleClassNameIdResolver(config, baseType, typeToId, idToType);
    }

    @Override
    public JsonTypeInfo.Id getMechanism() { return JsonTypeInfo.Id.SIMPLE_CLASS_NAME; }

    @Override
    public String idFromValue(Object value) {
        return idFromClass(value.getClass());
    }

    protected String idFromClass(Class<?> clazz)
    {
        if (clazz == null) {
            return null;
        }
        // NOTE: although we may need to let `TypeModifier` change actual type to use
        // for id, we can use original type as key for more efficient lookup:
        final String key = clazz.getName();
        String name = _typeToId.get(key);

        if (name == null) {
            // 29-Nov-2019, tatu: As per test in `TestTypeModifierNameResolution` somehow
            //    we need to do this odd piece here which seems unnecessary but isn't.
            Class<?> cls = _typeFactory.constructType(clazz).getRawClass();
            // 24-Feb-2011, tatu: As per [JACKSON-498], may need to dynamically look up name
            // can either throw an exception, or use default name...
            if (_config.isAnnotationProcessingEnabled()) {
                BeanDescription beanDesc = _config.introspectClassAnnotations(cls);
                name = _config.getAnnotationIntrospector().findTypeName(beanDesc.getClassInfo());
            }
            if (name == null) {
                // And if still not found, let's choose default?
                name = cls.getSimpleName();
            }
            _typeToId.put(key, name);
        }
        return name;
    }

    @Override
    public String idFromValueAndType(Object value, Class<?> type) {
        // 18-Jan-2013, tatu: We may be called with null value occasionally
        //   it seems; nothing much we can figure out that way.
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

    @Override
    public String toString() {
        return String.format("[%s; id-to-type=%s]", getClass().getName(), _idToType);
    }
}
