package com.fasterxml.jackson.databind.module;

import java.util.*;

import com.fasterxml.jackson.annotation.JsonFormat;
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
    implements java.io.Serializable // since included by SimpleModule
{
    private static final long serialVersionUID = 3L;

    /**
     * Class-based mappings that are used both for exact and
     * sub-class matches.
     */
    protected HashMap<ClassKey,ValueSerializer<?>> _classMappings = null;

    /**
     * Interface-based matches.
     */
    protected HashMap<ClassKey,ValueSerializer<?>> _interfaceMappings = null;

    /**
     * Flag to help find "generic" enum serializer, if one has been registered.
     */
    protected boolean _hasEnumSerializer = false;
    
    /*
    /**********************************************************
    /* Life-cycle, construction and configuring
    /**********************************************************
     */
    
    public SimpleSerializers() { }

    public SimpleSerializers(List<ValueSerializer<?>> sers) {
        addSerializers(sers);
    }
    
    /**
     * Method for adding given serializer for type that {@link ValueSerializer#handledType}
     * specifies (which MUST return a non-null class; and can NOT be {@link Object}, as a
     * sanity check).
     * For serializers that do not declare handled type, use the variant that takes
     * two arguments.
     * 
     * @param ser
     */
    public SimpleSerializers addSerializer(ValueSerializer<?> ser)
    {
        // Interface to match?
        Class<?> cls = ser.handledType();
        if (cls == null || cls == Object.class) {
            throw new IllegalArgumentException("`ValueSerializer` of type `"+ser.getClass().getName()
                    +"` does not define valid handledType() -- must either register with method that takes type argument "
                    +" or make serializer extend 'com.fasterxml.jackson.databind.ser.std.StdSerializer'"); 
        }
        _addSerializer(cls, ser);
        return this;
    }

    public <T> SimpleSerializers addSerializer(Class<? extends T> type, ValueSerializer<T> ser)
    {
        _addSerializer(type, ser);
        return this;
    }

    public SimpleSerializers addSerializers(List<ValueSerializer<?>> sers) {
        for (ValueSerializer<?> ser : sers) {
            addSerializer(ser);
        }
        return this;
    }

    /*
    /**********************************************************
    /* Serializers implementation
    /**********************************************************
     */
    
    @Override
    public ValueSerializer<?> findSerializer(SerializationConfig config,
            JavaType type, BeanDescription beanDesc, JsonFormat.Value formatOverrides)
    {
        Class<?> cls = type.getRawClass();
        ClassKey key = new ClassKey(cls);
        ValueSerializer<?> ser = null;
        
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
    public ValueSerializer<?> findArraySerializer(SerializationConfig config,
            ArrayType type, BeanDescription beanDesc, JsonFormat.Value formatOverrides,
            TypeSerializer elementTypeSerializer, ValueSerializer<Object> elementValueSerializer) {
        return findSerializer(config, type, beanDesc, formatOverrides);
    }

    @Override
    public ValueSerializer<?> findCollectionSerializer(SerializationConfig config,
            CollectionType type, BeanDescription beanDesc, JsonFormat.Value formatOverrides,
            TypeSerializer elementTypeSerializer, ValueSerializer<Object> elementValueSerializer) {
        return findSerializer(config, type, beanDesc, formatOverrides);
    }

    @Override
    public ValueSerializer<?> findCollectionLikeSerializer(SerializationConfig config,
            CollectionLikeType type, BeanDescription beanDesc, JsonFormat.Value formatOverrides,
            TypeSerializer elementTypeSerializer, ValueSerializer<Object> elementValueSerializer) {
        return findSerializer(config, type, beanDesc, formatOverrides);
    }
        
    @Override
    public ValueSerializer<?> findMapSerializer(SerializationConfig config,
            MapType type, BeanDescription beanDesc, JsonFormat.Value formatOverrides,
            ValueSerializer<Object> keySerializer,
            TypeSerializer elementTypeSerializer, ValueSerializer<Object> elementValueSerializer) {
        return findSerializer(config, type, beanDesc, formatOverrides);
    }

    @Override
    public ValueSerializer<?> findMapLikeSerializer(SerializationConfig config,
            MapLikeType type, BeanDescription beanDesc, JsonFormat.Value formatOverrides,
            ValueSerializer<Object> keySerializer,
            TypeSerializer elementTypeSerializer, ValueSerializer<Object> elementValueSerializer) {
        return findSerializer(config, type, beanDesc, formatOverrides);
    }
    
    /*
    /**********************************************************************
    /* Internal methods
    /**********************************************************************
     */
    
    protected ValueSerializer<?> _findInterfaceMapping(Class<?> cls, ClassKey key)
    {
        for (Class<?> iface : cls.getInterfaces()) {
            key.reset(iface);
            ValueSerializer<?> ser = _interfaceMappings.get(key);
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

    protected void _addSerializer(Class<?> cls, ValueSerializer<?> ser)
    {
        ClassKey key = new ClassKey(cls);
        // Interface or class type?
        if (cls.isInterface()) {
            if (_interfaceMappings == null) {
                _interfaceMappings = new HashMap<ClassKey,ValueSerializer<?>>();
            }
            _interfaceMappings.put(key, ser);
        } else { // nope, class:
            if (_classMappings == null) {
                _classMappings = new HashMap<ClassKey,ValueSerializer<?>>();
            }
            _classMappings.put(key, ser);
            if (cls == Enum.class) {
                _hasEnumSerializer = true;
            }
        }
    }
}
