package com.fasterxml.jackson.databind.deser.std;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

import com.fasterxml.jackson.databind.DeserializationConfig;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.deser.SettableBeanProperty;
import com.fasterxml.jackson.databind.deser.ValueInstantiator;
import com.fasterxml.jackson.databind.util.ClassUtil;

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

    @Override // since 2.9
    public Boolean supportsUpdate(DeserializationConfig config) {
        return Boolean.TRUE;
    }
    
    @Override
    public SettableBeanProperty findBackReference(String refName) {
        JsonDeserializer<Object> valueDeser = getContentDeserializer();
        if (valueDeser == null) {
            throw new IllegalArgumentException(String.format(
                    "Can not handle managed/back reference '%s': type: container deserializer of type %s returned null for 'getContentDeserializer()'",
                    refName, getClass().getName()));
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

    /**
     * @since 2.9
     */
    public ValueInstantiator getValueInstantiator() {
        return null;
    }

    /*
    /**********************************************************
    /* Shared methods for sub-classes
    /**********************************************************
     */

    /**
     * Helper method called by various Map(-like) deserializers.
     */
    protected <BOGUS> BOGUS wrapAndThrow(Throwable t, Object ref, String key) throws IOException
    {
        // to handle StackOverflow:
        while (t instanceof InvocationTargetException && t.getCause() != null) {
            t = t.getCause();
        }
        // Errors and "plain" IOExceptions to be passed as is
        ClassUtil.throwIfError(t);
        // ... except for mapping exceptions
        if (t instanceof IOException && !(t instanceof JsonMappingException)) {
            throw (IOException) t;
        }
        // for [databind#1141]
        throw JsonMappingException.wrapWithPath(t, ref,
                ClassUtil.nonNull(key, "N/A"));
    }
}
