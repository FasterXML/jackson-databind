package com.fasterxml.jackson.databind.deser.std;

import java.io.IOException;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.jsontype.TypeDeserializer;

/**
 * Convenience deserializer that may be used to deserialize values given an
 * intermediate tree representation ({@link JsonNode}).
 * Note that this is a slightly simplified alternative to {@link StdConvertingDeserializer}).
 *
 * @param <T> Target type of this deserializer; that is, type of values that
 *   input data is deserialized into.
 */
public abstract class StdNodeBasedDeserializer<T>
    extends StdDeserializer<T>
{
    private static final long serialVersionUID = 1L;

    protected JsonDeserializer<Object> _treeDeserializer;

    /*
    /**********************************************************************
    /* Life-cycle
    /**********************************************************************
     */
    
    protected StdNodeBasedDeserializer(JavaType targetType) {
        super(targetType);
    }

    protected StdNodeBasedDeserializer(Class<T> targetType) {
        super(targetType);
    }

    /**
     * "Copy-constructor" used when creating a modified copies, most often
     * if sub-class overrides {@link com.fasterxml.jackson.databind.JsonDeserializer#createContextual}.
     */
    protected StdNodeBasedDeserializer(StdNodeBasedDeserializer<?> src) {
        super(src);
        _treeDeserializer = src._treeDeserializer;
    }

    @Override
    public void resolve(DeserializationContext ctxt) throws JsonMappingException {
        _treeDeserializer = ctxt.findRootValueDeserializer(ctxt.constructType(JsonNode.class));
    }

    /*
    /**********************************************************************
    /* Abstract methods for sub-classes
    /**********************************************************************
     */

    public abstract T convert(JsonNode root, DeserializationContext ctxt) throws IOException;

    /*
    /**********************************************************************
    /* JsonDeserializer impl
    /**********************************************************************
     */
    
    @Override
    public T deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        JsonNode n = (JsonNode) _treeDeserializer.deserialize(p, ctxt);
        return convert(n, ctxt);
    }

    @Override
    public Object deserializeWithType(JsonParser p, DeserializationContext ctxt,
            TypeDeserializer td)
        throws IOException
    {
        // 19-Nov-2014, tatu: Quite likely we'd have some issues but... let's
        //   try, just in case.
        JsonNode n = (JsonNode) _treeDeserializer.deserializeWithType(p, ctxt, td);
        return convert(n, ctxt);
    }
}
