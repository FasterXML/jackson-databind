package com.fasterxml.jackson.databind.ser.std;

import java.io.IOException;
import java.lang.reflect.Type;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser.NumberType;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JacksonStdImpl;
import com.fasterxml.jackson.databind.jsonFormatVisitors.JsonFormatVisitorWrapper;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import com.fasterxml.jackson.databind.ser.ContextualSerializer;

/**
 * Serializer used for primitive boolean, as well as java.util.Boolean
 * wrapper type.
 *<p>
 * Since this is one of "natural" (aka "native") types, no type information is ever
 * included on serialization (unlike for most other scalar types)
 */
@JacksonStdImpl
public final class BooleanSerializer
//In 2.9, removed use of intermediate type `NonTypedScalarSerializerBase`
    extends StdScalarSerializer<Object>
    implements ContextualSerializer
{
    private static final long serialVersionUID = 1L;

    /**
     * Whether type serialized is primitive (boolean) or wrapper
     * (java.lang.Boolean); if true, former, if false, latter.
     */
    protected final boolean _forPrimitive;

    public BooleanSerializer(boolean forPrimitive) {
        super(forPrimitive ? Boolean.TYPE : Boolean.class, false);
        _forPrimitive = forPrimitive;
    }

    @Override
    public JsonSerializer<?> createContextual(SerializerProvider serializers,
            BeanProperty property) throws JsonMappingException
    {
        // 16-Mar-2021, tatu: As per [databind#3080], was passing wrapper type
        //    always; should not have.
        JsonFormat.Value format = findFormatOverrides(serializers, property,
                handledType());
        if (format != null) {
            JsonFormat.Shape shape = format.getShape();
            if (shape.isNumeric()) {
                return new AsNumber(_forPrimitive);
            }
            if (shape == JsonFormat.Shape.STRING) {
                return new ToStringSerializer(_handledType);
            }
        }
        return this;
    }

    @Override
    public void serialize(Object value, JsonGenerator g, SerializerProvider provider) throws IOException {
        g.writeBoolean(Boolean.TRUE.equals(value));
    }

    @Override
    public final void serializeWithType(Object value, JsonGenerator g, SerializerProvider provider,
            TypeSerializer typeSer) throws IOException
    {
        g.writeBoolean(Boolean.TRUE.equals(value));
    }

    /**
     * @deprecated Since 2.15
     */
    @Deprecated
    @Override
    public JsonNode getSchema(SerializerProvider provider, Type typeHint) {
        return createSchemaNode("boolean", !_forPrimitive);
    }

    @Override
    public void acceptJsonFormatVisitor(JsonFormatVisitorWrapper visitor, JavaType typeHint) throws JsonMappingException {
        visitor.expectBooleanFormat(typeHint);
    }

    /**
     * Alternate implementation that is used when values are to be serialized
     * as numbers <code>0</code> (false) or <code>1</code> (true).
     *
     * @since 2.9
     */
    final static class AsNumber
        extends StdScalarSerializer<Object>
        implements ContextualSerializer
    {
        private static final long serialVersionUID = 1L;

        /**
         * Whether type serialized is primitive (boolean) or wrapper
         * (java.lang.Boolean); if true, former, if false, latter.
         */
        protected final boolean _forPrimitive;

        public AsNumber(boolean forPrimitive) {
            super(forPrimitive ? Boolean.TYPE : Boolean.class, false);
            _forPrimitive = forPrimitive;
        }

        @Override
        public void serialize(Object value, JsonGenerator g, SerializerProvider provider) throws IOException {
            g.writeNumber((Boolean.FALSE.equals(value)) ? 0 : 1);
        }

        @Override
        public final void serializeWithType(Object value, JsonGenerator g, SerializerProvider provider,
                TypeSerializer typeSer) throws IOException
        {
            // 27-Mar-2017, tatu: Actually here we CAN NOT serialize as number without type,
            //    since with natural types that would map to number, not boolean. So choice
            //    comes to between either add type id, or serialize as boolean. Choose
            //    latter at this point
            g.writeBoolean(Boolean.TRUE.equals(value));
        }

        @Override
        public void acceptJsonFormatVisitor(JsonFormatVisitorWrapper visitor, JavaType typeHint) throws JsonMappingException {
            // 27-Mar-2017, tatu: As usual, bit tricky but... seems like we should call
            //    visitor for actual representation
            visitIntFormat(visitor, typeHint, NumberType.INT);
        }

        @Override
        public JsonSerializer<?> createContextual(SerializerProvider serializers,
                BeanProperty property) throws JsonMappingException
        {
            JsonFormat.Value format = findFormatOverrides(serializers,
                    property, Boolean.class);
            if (format != null) {
                JsonFormat.Shape shape = format.getShape();
                if (!shape.isNumeric()) {
                    return new BooleanSerializer(_forPrimitive);
                }
            }
            return this;
        }
    }
}
