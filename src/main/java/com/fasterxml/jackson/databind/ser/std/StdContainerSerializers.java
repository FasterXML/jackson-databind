package com.fasterxml.jackson.databind.ser.std;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import com.fasterxml.jackson.databind.ser.ContainerSerializer;
import com.fasterxml.jackson.databind.ser.impl.IndexedListSerializer;
import com.fasterxml.jackson.databind.ser.impl.IteratorSerializer;
import com.fasterxml.jackson.databind.ser.std.CollectionSerializer;

/**
 * Dummy container class to group standard container serializers: serializers
 * that can serialize things like {@link java.util.List}s,
 * {@link java.util.Map}s and such.
 */
public class StdContainerSerializers
{
    protected StdContainerSerializers() { }
    
    /**
     * @since 2.1
     */
    public static ContainerSerializer<?> indexedListSerializer(JavaType elemType,
            boolean staticTyping, TypeSerializer vts, JsonSerializer<Object> valueSerializer)
    {
        return new IndexedListSerializer(elemType, staticTyping, vts, null, valueSerializer);
    }
    
    /**
     * @since 2.1
     */
    public static ContainerSerializer<?> collectionSerializer(JavaType elemType,
            boolean staticTyping, TypeSerializer vts, JsonSerializer<Object> valueSerializer)
    {
        return new CollectionSerializer(elemType, staticTyping, vts, null, valueSerializer);
    }

    public static ContainerSerializer<?> iteratorSerializer(JavaType elemType,
            boolean staticTyping, TypeSerializer vts)
    {
        return new IteratorSerializer(elemType, staticTyping, vts, null);
    }

    public static ContainerSerializer<?> iterableSerializer(JavaType elemType,
            boolean staticTyping, TypeSerializer vts)
    {
        return new IterableSerializer(elemType, staticTyping, vts, null);
    }

    public static JsonSerializer<?> enumSetSerializer(JavaType enumType)
    {
        return new EnumSetSerializer(enumType, null);
    }

    /*
    /**********************************************************
    /* Deprecated methods
    /**********************************************************
     */

    /**
     * @deprecated Since 2.1; use variant that does not take 'property' argument
     */
    @Deprecated
    public static ContainerSerializer<?> indexedListSerializer(JavaType elemType,
            boolean staticTyping, TypeSerializer vts, BeanProperty property,
            JsonSerializer<Object> valueSerializer)
    {
        return indexedListSerializer(elemType, staticTyping, vts, valueSerializer);
    }

    /**
     * @deprecated Since 2.1; use variant that does not take 'property' argument
     */
    @Deprecated
    public static ContainerSerializer<?> collectionSerializer(JavaType elemType,
            boolean staticTyping, TypeSerializer vts, BeanProperty property,
            JsonSerializer<Object> valueSerializer)
    {
        return collectionSerializer(elemType, staticTyping, vts, valueSerializer);
    }
}
