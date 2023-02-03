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

    protected HashMap<ClassKey,JsonDeserializer<?>> _classMappings = null;

    /**
     * Flag to help find "generic" enum deserializer, if one has been registered.
     *
     * @since 2.3
     */
    protected boolean _hasEnumDeserializer = false;

    /*
    /**********************************************************
    /* Life-cycle, construction and configuring
    /**********************************************************
     */

    public SimpleDeserializers() { }

    /**
     * @since 2.1
     */
    public SimpleDeserializers(Map<Class<?>,JsonDeserializer<?>> desers) {
        addDeserializers(desers);
    }

    public <T> void addDeserializer(Class<T> forClass, JsonDeserializer<? extends T> deser)
    {
        ClassKey key = new ClassKey(forClass);
        if (_classMappings == null) {
            _classMappings = new HashMap<ClassKey,JsonDeserializer<?>>();
        }
        _classMappings.put(key, deser);
        // [Issue#227]: generic Enum deserializer?
        if (forClass == Enum.class) {
            _hasEnumDeserializer = true;
        }
    }

    /**
     * @since 2.1
     */
    @SuppressWarnings("unchecked")
    public void addDeserializers(Map<Class<?>,JsonDeserializer<?>> desers)
    {
        for (Map.Entry<Class<?>,JsonDeserializer<?>> entry : desers.entrySet()) {
            Class<?> cls = entry.getKey();
            // what a mess... nominal generics safety...
            JsonDeserializer<Object> deser = (JsonDeserializer<Object>) entry.getValue();
            addDeserializer((Class<Object>) cls, deser);
        }
    }

    /*
    /**********************************************************
    /* Serializers implementation
    /**********************************************************
     */

    @Override
    public JsonDeserializer<?> findArrayDeserializer(ArrayType type,
            DeserializationConfig config, BeanDescription beanDesc,
            TypeDeserializer elementTypeDeserializer, JsonDeserializer<?> elementDeserializer)
        throws JsonMappingException
    {
        return _find(type);
    }

    @Override
    public JsonDeserializer<?> findBeanDeserializer(JavaType type,
            DeserializationConfig config, BeanDescription beanDesc)
        throws JsonMappingException
    {
        return _find(type);
    }

    @Override
    public JsonDeserializer<?> findCollectionDeserializer(CollectionType type,
            DeserializationConfig config, BeanDescription beanDesc,
            TypeDeserializer elementTypeDeserializer,
            JsonDeserializer<?> elementDeserializer)
        throws JsonMappingException
    {
        return _find(type);
    }

    @Override
    public JsonDeserializer<?> findCollectionLikeDeserializer(CollectionLikeType type,
            DeserializationConfig config, BeanDescription beanDesc,
            TypeDeserializer elementTypeDeserializer,
            JsonDeserializer<?> elementDeserializer)
        throws JsonMappingException
    {
        return _find(type);
    }

    @Override
    public JsonDeserializer<?> findEnumDeserializer(Class<?> type,
            DeserializationConfig config, BeanDescription beanDesc)
        throws JsonMappingException
    {
        if (_classMappings == null) {
            return null;
        }
        JsonDeserializer<?> deser = _classMappings.get(new ClassKey(type));
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
    public JsonDeserializer<?> findTreeNodeDeserializer(Class<? extends JsonNode> nodeType,
            DeserializationConfig config, BeanDescription beanDesc)
        throws JsonMappingException
    {
        if (_classMappings == null) {
            return null;
        }
        return _classMappings.get(new ClassKey(nodeType));
    }

    @Override
    public JsonDeserializer<?> findReferenceDeserializer(ReferenceType refType,
            DeserializationConfig config, BeanDescription beanDesc,
            TypeDeserializer contentTypeDeserializer, JsonDeserializer<?> contentDeserializer)
        throws JsonMappingException {
        // 21-Oct-2015, tatu: Unlikely this will really get used (reference types need more
        //    work, simple registration probably not sufficient). But whatever.
        return _find(refType);
    }

    @Override
    public JsonDeserializer<?> findMapDeserializer(MapType type,
            DeserializationConfig config, BeanDescription beanDesc,
            KeyDeserializer keyDeserializer,
            TypeDeserializer elementTypeDeserializer,
            JsonDeserializer<?> elementDeserializer)
        throws JsonMappingException
    {
        return _find(type);
    }

    @Override
    public JsonDeserializer<?> findMapLikeDeserializer(MapLikeType type,
            DeserializationConfig config, BeanDescription beanDesc,
            KeyDeserializer keyDeserializer,
            TypeDeserializer elementTypeDeserializer,
            JsonDeserializer<?> elementDeserializer)
        throws JsonMappingException
    {
        return _find(type);
    }

    @Override // since 2.11
    public boolean hasDeserializerFor(DeserializationConfig config,
            Class<?> valueType) {
        return (_classMappings != null)
                && _classMappings.containsKey(new ClassKey(valueType));
    }

    private final JsonDeserializer<?> _find(JavaType type) {
        if (_classMappings == null) {
            return null;
        }
        return _classMappings.get(new ClassKey(type.getRawClass()));
    }
}
