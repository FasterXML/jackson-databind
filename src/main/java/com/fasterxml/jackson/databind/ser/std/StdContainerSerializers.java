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
    
    public static ContainerSerializer<?> indexedListSerializer(JavaType elemType,
            boolean staticTyping, TypeSerializer vts, BeanProperty property,
            JsonSerializer<Object> valueSerializer)
    {
        return new IndexedListSerializer(elemType, staticTyping, vts, property, valueSerializer);
    }

    public static ContainerSerializer<?> collectionSerializer(JavaType elemType,
            boolean staticTyping, TypeSerializer vts, BeanProperty property,
            JsonSerializer<Object> valueSerializer)
    {
        return new CollectionSerializer(elemType, staticTyping, vts, property, valueSerializer);
    }

    public static ContainerSerializer<?> iteratorSerializer(JavaType elemType,
            boolean staticTyping, TypeSerializer vts, BeanProperty property)
    {
        return new IteratorSerializer(elemType, staticTyping, vts, property);
    }

    public static ContainerSerializer<?> iterableSerializer(JavaType elemType,
            boolean staticTyping, TypeSerializer vts, BeanProperty property)
    {
        return new IterableSerializer(elemType, staticTyping, vts, property);
    }

    public static JsonSerializer<?> enumSetSerializer(JavaType enumType, BeanProperty property)
    {
        return new EnumSetSerializer(enumType, property);
    }
}
