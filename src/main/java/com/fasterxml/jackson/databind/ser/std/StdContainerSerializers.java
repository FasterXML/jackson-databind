package com.fasterxml.jackson.databind.ser.std;

import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import com.fasterxml.jackson.databind.ser.ContainerSerializer;
import com.fasterxml.jackson.databind.ser.impl.IndexedListSerializer;
import com.fasterxml.jackson.databind.ser.impl.IteratorSerializer;

/**
 * Dummy container class to group standard container serializers: serializers
 * that can serialize things like {@link java.util.List}s,
 * {@link java.util.Map}s and such.
 */
public class StdContainerSerializers
{
    protected StdContainerSerializers() { }
    
    public static ContainerSerializer<?> indexedListSerializer(JavaType elemType,
            boolean staticTyping, TypeSerializer vts, BeanProperty property,
            JsonSerializer<Object> valueSerializer)
    {
        return new IndexedListSerializer(elemType, staticTyping, vts, null, valueSerializer);
    }

    public static ContainerSerializer<?> collectionSerializer(JavaType elemType,
            boolean staticTyping, TypeSerializer vts, BeanProperty property,
            JsonSerializer<Object> valueSerializer)
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
}
