package com.fasterxml.jackson.databind.ser.std;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import com.fasterxml.jackson.databind.ser.ContainerSerializer;
import com.fasterxml.jackson.databind.ser.impl.IteratorSerializer;

/**
 * Dummy container class to group standard container serializers: serializers
 * that can serialize things like {@link java.util.List}s,
 * {@link java.util.Map}s and such.
 */
public class StdContainerSerializers
{
    protected StdContainerSerializers() { }
    
    public static ContainerSerializer<?> iteratorSerializer(JavaType elemType,
            boolean staticTyping, TypeSerializer vts) {
        return new IteratorSerializer(elemType, staticTyping, vts, null);
    }

    public static ContainerSerializer<?> iterableSerializer(JavaType elemType,
            boolean staticTyping, TypeSerializer vts) {
        return new IterableSerializer(elemType, staticTyping, vts, null);
    }
}
