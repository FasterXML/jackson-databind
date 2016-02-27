package com.fasterxml.jackson.databind.deser.std;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.deser.SettableBeanProperty;

/**
 * Intermediate base deserializer class that adds more shared accessor
 * so that other classes can access information about contained (value) types
 */
@SuppressWarnings("serial")
public abstract class ContainerDeserializerBase<T>
    extends StdDeserializer<T>
{
    protected ContainerDeserializerBase(JavaType selfType) {
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

    /*
    /**********************************************************
    /* Shared methods for sub-classes
    /**********************************************************
     */

    /**
     * Helper method called by various Map(-like) deserializers.
     */
    protected void wrapAndThrow(Throwable t, Object ref, String key) throws IOException
    {
        // to handle StackOverflow:
        while (t instanceof InvocationTargetException && t.getCause() != null) {
            t = t.getCause();
        }
        // Errors and "plain" IOExceptions to be passed as is
        if (t instanceof Error) {
            throw (Error) t;
        }
        // ... except for mapping exceptions
        if (t instanceof IOException && !(t instanceof JsonMappingException)) {
            throw (IOException) t;
        }
        // for [databind#1141]
        if (key == null) {
            key = "N/A";
        }
        throw JsonMappingException.wrapWithPath(t, ref, key);
    }
}
