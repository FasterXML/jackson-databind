package com.fasterxml.jackson.databind.ser.std;

import java.io.IOException;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.core.type.WritableTypeId;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JacksonStdImpl;
import com.fasterxml.jackson.databind.jsonFormatVisitors.JsonFormatVisitorWrapper;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;

/**
 * Simple general purpose serializer, useful for any
 * type for which {@link Object#toString} returns the desired JSON
 * value.
 */
@JacksonStdImpl
@SuppressWarnings("serial")
public class ToStringSerializer
    extends StdSerializer<Object>
{
    /**
     * Singleton instance to use.
     */
    public final static ToStringSerializer instance = new ToStringSerializer();

    /**
     *<p>
     * Note: usually you should NOT create new instances, but instead use
     * {@link #instance} which is stateless and fully thread-safe. However,
     * there are cases where constructor is needed; for example,
     * when using explicit serializer annotations like
     * {@link com.fasterxml.jackson.databind.annotation.JsonSerialize#using}.
     */
    public ToStringSerializer() { super(Object.class); }

    public ToStringSerializer(Class<?> handledType) {
        super(handledType);
    }

    @Override
    public boolean isEmpty(SerializerProvider prov, Object value) {
        return value.toString().isEmpty();
    }

    @Override
    public void serialize(Object value, JsonGenerator gen, SerializerProvider provider)
        throws IOException
    {
        gen.writeString(value.toString());
    }

    /* 01-Mar-2011, tatu: We were serializing as "raw" String; but generally that
     *   is not what we want, since lack of type information would imply real
     *   String type.
     */
    /**
     * Default implementation will write type prefix, call regular serialization
     * method (since assumption is that value itself does not need JSON
     * Array or Object start/end markers), and then write type suffix.
     * This should work for most cases; some sub-classes may want to
     * change this behavior.
     */
    @Override
    public void serializeWithType(Object value, JsonGenerator g, SerializerProvider provider,
            TypeSerializer typeSer)
        throws IOException
    {
        WritableTypeId typeIdDef = typeSer.writeTypePrefix(g,
                typeSer.typeId(value, JsonToken.VALUE_STRING));
        serialize(value, g, provider);
        typeSer.writeTypeSuffix(g, typeIdDef);
    }

    @Override
    public void acceptJsonFormatVisitor(JsonFormatVisitorWrapper visitor, JavaType typeHint) throws JsonMappingException
    {
        visitStringFormat(visitor, typeHint);
    }
}
