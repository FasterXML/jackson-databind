package com.fasterxml.jackson.databind.deser.std;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.deser.SettableBeanProperty;

/**
 * Intermediate base deserializer class that adds more shared accessor
 * so that other classes can access information about contained (value)
 * types
 */
@SuppressWarnings("serial")
public abstract class ContainerDeserializerBase<T>
    extends StdDeserializer<T>
{
    protected ContainerDeserializerBase(JavaType selfType) {
        super(selfType);
    }

    /**
     * @deprecated Since 2.3 use one that takes {@link JavaType}
     */
    @Deprecated
    protected ContainerDeserializerBase(Class<?> selfType) {
        super(selfType);
    }

    /*
    /**********************************************************
    /* Overrides
    /**********************************************************
     */

    @Override
    public SettableBeanProperty findBackReference(String refName) {
        JsonDeserializer<Object> valueDeser = getContentDeserializer();
        if (valueDeser == null) {
            throw new IllegalArgumentException("Can not handle managed/back reference '"+refName
                    +"': type: container deserializer of type "+getClass().getName()+" returned null for 'getContentDeserializer()'");
        }
        return valueDeser.findBackReference(refName);
    }
    
    /*
    /**********************************************************
    /* Extended API
    /**********************************************************
     */

    /**
     * Accessor for declared type of contained value elements; either exact
     * type, or one of its supertypes.
     */
    public abstract JavaType getContentType();

    /**
     * Accesor for deserializer use for deserializing content values.
     */
    public abstract JsonDeserializer<Object> getContentDeserializer();
}
