package com.fasterxml.jackson.databind.module;

import java.util.*;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.deser.Deserializers;
import com.fasterxml.jackson.databind.jsontype.TypeDeserializer;
import com.fasterxml.jackson.databind.type.*;

/**
 * Simple implementation {@link Deserializers} which allows registration of
 * deserializers based on raw (type erased class).
 * It can work well for basic bean and scalar type deserializers, but is not
 * a good fit for handling generic types (like {@link Map}s and {@link Collection}s
 * or array types).
 *<p>
 * Unlike {@link SimpleSerializers}, this class does not currently support generic mappings;
 * all mappings must be to exact declared deserialization type.
 */
public class SimpleDeserializers
   extends Deserializers.Base
   implements java.io.Serializable
{
    private static final long serialVersionUID = 1L;

    protected HashMap<ClassKey,ValueDeserializer<?>> _classMappings = null;

    /**
     * Flag to help find "generic" enum deserializer, if one has been registered.
     */
    protected boolean _hasEnumDeserializer = false;
    
    /*
    /**********************************************************
    /* Life-cycle, construction and configuring
    /**********************************************************
     */
    
    public SimpleDeserializers() { }

    public SimpleDeserializers(Map<Class<?>,ValueDeserializer<?>> desers) {
        addDeserializers(desers);
    }
    
    public <T> SimpleDeserializers addDeserializer(Class<T> forClass, ValueDeserializer<? extends T> deser)
    {
        ClassKey key = new ClassKey(forClass);
        if (_classMappings == null) {
            _classMappings = new HashMap<ClassKey,ValueDeserializer<?>>();
        }
        _classMappings.put(key, deser);
        // [Issue#227]: generic Enum deserializer?
        if (forClass == Enum.class) {
            _hasEnumDeserializer = true;
        }
        return this;
    }

    @SuppressWarnings("unchecked")
    public SimpleDeserializers addDeserializers(Map<Class<?>,ValueDeserializer<?>> desers)
    {
        for (Map.Entry<Class<?>,ValueDeserializer<?>> entry : desers.entrySet()) {
            Class<?> cls = entry.getKey();
            // what a mess... nominal generics safety...
            ValueDeserializer<Object> deser = (ValueDeserializer<Object>) entry.getValue();
            addDeserializer((Class<Object>) cls, deser);
        }
        return this;
    }
    
    /*
    /**********************************************************
    /* Serializers implementation
    /**********************************************************
     */

    @Override
    public ValueDeserializer<?> findArrayDeserializer(ArrayType type,
            DeserializationConfig config, BeanDescription beanDesc,
            TypeDeserializer elementTypeDeserializer, ValueDeserializer<?> elementDeserializer)
    {
        return _find(type);
    }

    @Override
    public ValueDeserializer<?> findBeanDeserializer(JavaType type,
            DeserializationConfig config, BeanDescription beanDesc)
    {
        return _find(type);
    }

    @Override
    public ValueDeserializer<?> findCollectionDeserializer(CollectionType type,
            DeserializationConfig config, BeanDescription beanDesc,
            TypeDeserializer elementTypeDeserializer,
            ValueDeserializer<?> elementDeserializer)
    {
        return _find(type);
    }

    @Override
    public ValueDeserializer<?> findCollectionLikeDeserializer(CollectionLikeType type,
            DeserializationConfig config, BeanDescription beanDesc,
            TypeDeserializer elementTypeDeserializer,
            ValueDeserializer<?> elementDeserializer)
    {
        return _find(type);
    }
    
    @Override
    public ValueDeserializer<?> findEnumDeserializer(Class<?> type,
            DeserializationConfig config, BeanDescription beanDesc)
    {
        if (_classMappings == null) {
            return null;
        }
        ValueDeserializer<?> deser = _classMappings.get(new ClassKey(type));
        if (deser == null) {
            // 29-Sep-2019, tatu: Not 100% sure this is workable logic but leaving
            //   as is (wrt [databind#2457]. Probably works ok since this covers direct
            //   sub-classes of `Enum`; but even if custom sub-classes aren't, unlikely
            //   mapping for those ever requested for deserialization
            if (_hasEnumDeserializer && type.isEnum()) {
                deser = _classMappings.get(new ClassKey(Enum.class));
            }
        }
        return deser;
    }

    @Override
    public ValueDeserializer<?> findTreeNodeDeserializer(Class<? extends JsonNode> nodeType,
            DeserializationConfig config, BeanDescription beanDesc)
    {
        if (_classMappings == null) {
            return null;
        }
        return _classMappings.get(new ClassKey(nodeType));
    }

    @Override
    public ValueDeserializer<?> findReferenceDeserializer(ReferenceType refType,
            DeserializationConfig config, BeanDescription beanDesc,
            TypeDeserializer contentTypeDeserializer, ValueDeserializer<?> contentDeserializer)
    {
        // 21-Oct-2015, tatu: Unlikely this will really get used (reference types need more
        //    work, simple registration probably not sufficient). But whatever.
        return _find(refType);
    }

    @Override
    public ValueDeserializer<?> findMapDeserializer(MapType type,
            DeserializationConfig config, BeanDescription beanDesc,
            KeyDeserializer keyDeserializer,
            TypeDeserializer elementTypeDeserializer,
            ValueDeserializer<?> elementDeserializer)
    {
        return _find(type);
    }

    @Override
    public ValueDeserializer<?> findMapLikeDeserializer(MapLikeType type,
            DeserializationConfig config, BeanDescription beanDesc,
            KeyDeserializer keyDeserializer,
            TypeDeserializer elementTypeDeserializer,
            ValueDeserializer<?> elementDeserializer)
    {
        return _find(type);
    }

    @Override
    public boolean hasDeserializerFor(DeserializationConfig config,
            Class<?> valueType)
    {
        return (_classMappings != null)
                && _classMappings.containsKey(new ClassKey(valueType));
    }

    private final ValueDeserializer<?> _find(JavaType type)
    {
        if (_classMappings == null) {
            return null;
        }
        return _classMappings.get(new ClassKey(type.getRawClass()));
    }
}
