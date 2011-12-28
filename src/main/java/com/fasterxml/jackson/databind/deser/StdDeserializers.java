package com.fasterxml.jackson.databind.deser;

import java.util.*;

import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.deser.std.*;
import com.fasterxml.jackson.databind.type.*;

/**
 * Helper class used to contain simple/well-known deserializers for core JDK types.
 */
class StdDeserializers
{
    final HashMap<ClassKey, JsonDeserializer<Object>> _deserializers
        = new HashMap<ClassKey, JsonDeserializer<Object>>();

    private StdDeserializers()
    {
        // First, add the fall-back "untyped" deserializer:
        add(new UntypedObjectDeserializer());

        // Then String and String-like converters:
        StdDeserializer<?> strDeser = new StringDeserializer();
        add(strDeser, String.class);
        add(strDeser, CharSequence.class);

        // Primitives/wrappers, other Numbers:
        add(NumberDeserializers.all());
        // Date/time types
        add(DateDeserializers.all());
        // other JDK types
        add(JdkDeserializers.all());

        add(JacksonDeserializers.all());
    }

    /**
     * Public accessor to deserializers for core types.
     */
    public static HashMap<ClassKey, JsonDeserializer<Object>> constructAll()
    {
        return new StdDeserializers()._deserializers;
    }

    private void add(StdDeserializer<?>[] serializers) {
        for (StdDeserializer<?> ser : serializers) {
            add(ser, ser.getValueClass());
        }
    }

    private void add(StdDeserializer<?> stdDeser) {
        add(stdDeser, stdDeser.getValueClass());
    }

    private void add(StdDeserializer<?> stdDeser, Class<?> valueClass)
    {
        // must do some unfortunate casting here...
        @SuppressWarnings("unchecked")
        JsonDeserializer<Object> deser = (JsonDeserializer<Object>) stdDeser;
        // Not super clean, but default TypeFactory does work here:
        _deserializers.put(new ClassKey(valueClass), deser);
    }
}
