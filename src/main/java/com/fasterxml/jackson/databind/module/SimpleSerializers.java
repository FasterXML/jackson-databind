package com.fasterxml.jackson.databind.module;

import java.util.*;


import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import com.fasterxml.jackson.databind.ser.Serializers;
import com.fasterxml.jackson.databind.type.ArrayType;
import com.fasterxml.jackson.databind.type.ClassKey;
import com.fasterxml.jackson.databind.type.CollectionLikeType;
import com.fasterxml.jackson.databind.type.CollectionType;
import com.fasterxml.jackson.databind.type.MapLikeType;
import com.fasterxml.jackson.databind.type.MapType;

/**
 * Simple implementation {@link Serializers} which allows registration of
 * serializers based on raw (type erased class).
 * It can work well for basic bean and scalar type serializers, but is not
 * a good fit for handling generic types (like {@link Map}s and {@link Collection}s).
 *<p>
 * Type registrations are assumed to be general; meaning that registration of serializer
 * for a super type will also be used for handling subtypes, unless an exact match
 * is found first. As an example, handler for {@link CharSequence} would also be used
 * serializing {@link StringBuilder} instances, unless a direct mapping was found.
 */
public class SimpleSerializers
    extends Serializers.Base
    implements java.io.Serializable
{
    private static final long serialVersionUID = 3L;

    /**
     * Class-based mappings that are used both for exact and
     * sub-class matches.
     */
    protected HashMap<ClassKey,JsonSerializer<?>> _classMappings = null;

    /**
     * Interface-based matches.
     */
    protected HashMap<ClassKey,JsonSerializer<?>> _interfaceMappings = null;

    /**
     * Flag to help find "generic" enum serializer, if one has been registered.
     *
     * @since 2.3
     */
    protected boolean _hasEnumSerializer = false;

    /*
    /**********************************************************
    /* Life-cycle, construction and configuring
    /**********************************************************
     */

    public SimpleSerializers() { }

    /**
     * @since 2.1
     */
    public SimpleSerializers(List<JsonSerializer<?>> sers) {
        addSerializers(sers);
    }

    /**
     * Method for adding given serializer for type that {@link JsonSerializer#handledType}
     * specifies (which MUST return a non-null class; and can NOT be {@link Object}, as a
     * sanity check).
     * For serializers that do not declare handled type, use the variant that takes
     * two arguments.
     *
     * @param ser
     */
    public void addSerializer(JsonSerializer<?> ser)
    {
        // Interface to match?
        Class<?> cls = ser.handledType();
        if (cls == null || cls == Object.class) {
            throw new IllegalArgumentException("JsonSerializer of type "+ser.getClass().getName()
                    +" does not define valid handledType() -- must either register with method that takes type argument "
                    +" or make serializer extend 'com.fasterxml.jackson.databind.ser.std.StdSerializer'");
        }
        _addSerializer(cls, ser);
    }

    public <T> void addSerializer(Class<? extends T> type, JsonSerializer<T> ser)
    {
        _addSerializer(type, ser);
    }

    /**
     * @since 2.1
     */
    public void addSerializers(List<JsonSerializer<?>> sers) {
        for (JsonSerializer<?> ser : sers) {
            addSerializer(ser);
        }
    }

    /*
    /**********************************************************
    /* Serializers implementation
    /**********************************************************
     */

    @Override
    public JsonSerializer<?> findSerializer(SerializationConfig config,
            JavaType type, BeanDescription beanDesc)
    {
        Class<?> cls = type.getRawClass();
        ClassKey key = new ClassKey(cls);
        JsonSerializer<?> ser = null;

        // First: direct match?
        if (cls.isInterface()) {
            if (_interfaceMappings != null) {
                ser = _interfaceMappings.get(key);
                if (ser != null) {
                    return ser;
                }
            }
        } else {
            if (_classMappings != null) {
                ser = _classMappings.get(key);
                if (ser != null) {
                    return ser;
                }

                // [Issue#227]: Handle registration of plain `Enum` serializer
                if (_hasEnumSerializer && type.isEnumType()) {
                    key.reset(Enum.class);
                    ser = _classMappings.get(key);
                    if (ser != null) {
                        return ser;
                    }
                }

                // If not direct match, maybe super-class match?
                for (Class<?> curr = cls; (curr != null); curr = curr.getSuperclass()) {
                    key.reset(curr);
                    ser = _classMappings.get(key);
                    if (ser != null) {
                        return ser;
                    }
                }
            }
        }
        // No direct match? How about super-interfaces?
        if (_interfaceMappings != null) {
            ser = _findInterfaceMapping(cls, key);
            if (ser != null) {
                return ser;
            }
            // still no matches? Maybe interfaces of super classes
            if (!cls.isInterface()) {
                while ((cls = cls.getSuperclass()) != null) {
                    ser = _findInterfaceMapping(cls, key);
                    if (ser != null) {
                        return ser;
                    }
                }
            }
        }
        return null;
    }

    @Override
    public JsonSerializer<?> findArraySerializer(SerializationConfig config,
            ArrayType type, BeanDescription beanDesc,
            TypeSerializer elementTypeSerializer, JsonSerializer<Object> elementValueSerializer) {
        return findSerializer(config, type, beanDesc);
    }

    @Override
    public JsonSerializer<?> findCollectionSerializer(SerializationConfig config,
            CollectionType type, BeanDescription beanDesc,
            TypeSerializer elementTypeSerializer, JsonSerializer<Object> elementValueSerializer) {
        return findSerializer(config, type, beanDesc);
    }

    @Override
    public JsonSerializer<?> findCollectionLikeSerializer(SerializationConfig config,
            CollectionLikeType type, BeanDescription beanDesc,
            TypeSerializer elementTypeSerializer, JsonSerializer<Object> elementValueSerializer) {
        return findSerializer(config, type, beanDesc);
    }

    @Override
    public JsonSerializer<?> findMapSerializer(SerializationConfig config,
            MapType type, BeanDescription beanDesc,
            JsonSerializer<Object> keySerializer,
            TypeSerializer elementTypeSerializer, JsonSerializer<Object> elementValueSerializer) {
        return findSerializer(config, type, beanDesc);
    }

    @Override
    public JsonSerializer<?> findMapLikeSerializer(SerializationConfig config,
            MapLikeType type, BeanDescription beanDesc,
            JsonSerializer<Object> keySerializer,
            TypeSerializer elementTypeSerializer, JsonSerializer<Object> elementValueSerializer) {
        return findSerializer(config, type, beanDesc);
    }

    /*
    /**********************************************************
    /* Internal methods
    /**********************************************************
     */

    protected JsonSerializer<?> _findInterfaceMapping(Class<?> cls, ClassKey key)
    {
        for (Class<?> iface : cls.getInterfaces()) {
            key.reset(iface);
            JsonSerializer<?> ser = _interfaceMappings.get(key);
            if (ser != null) {
                return ser;
            }
            ser = _findInterfaceMapping(iface, key);
            if (ser != null) {
                return ser;
            }
        }
        return null;
    }

    protected void _addSerializer(Class<?> cls, JsonSerializer<?> ser)
    {
        ClassKey key = new ClassKey(cls);
        // Interface or class type?
        if (cls.isInterface()) {
            if (_interfaceMappings == null) {
                _interfaceMappings = new HashMap<ClassKey,JsonSerializer<?>>();
            }
            _interfaceMappings.put(key, ser);
        } else { // nope, class:
            if (_classMappings == null) {
                _classMappings = new HashMap<ClassKey,JsonSerializer<?>>();
            }
            _classMappings.put(key, ser);
            if (cls == Enum.class) {
                _hasEnumSerializer = true;
            }
        }
    }
}
