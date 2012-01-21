package com.fasterxml.jackson.databind.ser.std;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Type;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.jsonschema.SchemaAware;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Base class used by all standard serializers, and can also
 * be used for custom serializers (in fact, this is the recommended
 * base class to use).
 * Provides convenience methods for implementing {@link SchemaAware}
 */
public abstract class StdSerializer<T>
    extends JsonSerializer<T>
    implements SchemaAware
{
    /**
     * Nominal type supported, usually declared type of
     * property for which serializer is used.
     */
    protected final Class<T> _handledType;

    /*
    /**********************************************************
    /* Life-cycle
    /**********************************************************
     */
    
    protected StdSerializer(Class<T> t) {
        _handledType = t;
    }

    @SuppressWarnings("unchecked")
    protected StdSerializer(JavaType type) {
        _handledType = (Class<T>) type.getRawClass();
    }
    
    /**
     * Alternate constructor that is (alas!) needed to work
     * around kinks of generic type handling
     */
    @SuppressWarnings("unchecked")
    protected StdSerializer(Class<?> t, boolean dummy) {
        _handledType = (Class<T>) t;
    }
    /*
    /**********************************************************
    /* Accessors
    /**********************************************************
     */

    
    @Override
    public final Class<T> handledType() { return _handledType; }

    /*
    /**********************************************************
    /* Serialization
    /**********************************************************
     */
    
    @Override
    public abstract void serialize(T value, JsonGenerator jgen, SerializerProvider provider)
        throws IOException, JsonGenerationException;

    /*
    /**********************************************************
    /* Helper methods for JSON Schema generation
    /**********************************************************
     */
    
    /**
     * Default implementation simply claims type is "string"; usually
     * overriden by custom serializers.
     */
    @Override
    public JsonNode getSchema(SerializerProvider provider, Type typeHint)
        throws JsonMappingException
    {
        return createSchemaNode("string");
    }
    
    protected ObjectNode createObjectNode() {
        return JsonNodeFactory.instance.objectNode();
    }
    
    protected ObjectNode createSchemaNode(String type)
    {
        ObjectNode schema = createObjectNode();
        schema.put("type", type);
        return schema;
    }
    
    protected ObjectNode createSchemaNode(String type, boolean isOptional)
    {
        ObjectNode schema = createSchemaNode(type);
        // as per [JACKSON-563]. Note that 'required' defaults to false
        if (!isOptional) {
            schema.put("required", !isOptional);
        }
        return schema;
    }
    
    /*
    /**********************************************************
    /* Helper methods for exception handling
    /**********************************************************
     */
    
    /**
     * Method that will modify caught exception (passed in as argument)
     * as necessary to include reference information, and to ensure it
     * is a subtype of {@link IOException}, or an unchecked exception.
     *<p>
     * Rules for wrapping and unwrapping are bit complicated; essentially:
     *<ul>
     * <li>Errors are to be passed as is (if uncovered via unwrapping)
     * <li>"Plain" IOExceptions (ones that are not of type
     *   {@link JsonMappingException} are to be passed as is
     *</ul>
     */
    public void wrapAndThrow(SerializerProvider provider,
            Throwable t, Object bean, String fieldName)
        throws IOException
    {
        /* 05-Mar-2009, tatu: But one nasty edge is when we get
         *   StackOverflow: usually due to infinite loop. But that
         *   usually gets hidden within an InvocationTargetException...
         */
        while (t instanceof InvocationTargetException && t.getCause() != null) {
            t = t.getCause();
        }
        // Errors and "plain" IOExceptions to be passed as is
        if (t instanceof Error) {
            throw (Error) t;
        }
        // Ditto for IOExceptions... except for mapping exceptions!
        boolean wrap = (provider == null) || provider.isEnabled(SerializationConfig.Feature.WRAP_EXCEPTIONS);
        if (t instanceof IOException) {
            if (!wrap || !(t instanceof JsonMappingException)) {
                throw (IOException) t;
            }
        } else if (!wrap) { // [JACKSON-407] -- allow disabling wrapping for unchecked exceptions
            if (t instanceof RuntimeException) {
                throw (RuntimeException) t;
            }
        }
        // [JACKSON-55] Need to add reference information
        throw JsonMappingException.wrapWithPath(t, bean, fieldName);
    }

    public void wrapAndThrow(SerializerProvider provider,
            Throwable t, Object bean, int index)
        throws IOException
    {
        while (t instanceof InvocationTargetException && t.getCause() != null) {
            t = t.getCause();
        }
        // Errors are to be passed as is
        if (t instanceof Error) {
            throw (Error) t;
        }
        // Ditto for IOExceptions... except for mapping exceptions!
        boolean wrap = (provider == null) || provider.isEnabled(SerializationConfig.Feature.WRAP_EXCEPTIONS);
        if (t instanceof IOException) {
            if (!wrap || !(t instanceof JsonMappingException)) {
                throw (IOException) t;
            }
        } else if (!wrap) { // [JACKSON-407] -- allow disabling wrapping for unchecked exceptions
            if (t instanceof RuntimeException) {
                throw (RuntimeException) t;
            }
        }
        // [JACKSON-55] Need to add reference information
        throw JsonMappingException.wrapWithPath(t, bean, index);
    }
}
