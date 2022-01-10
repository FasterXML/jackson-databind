package com.fasterxml.jackson.databind.ser.jdk;

import com.fasterxml.jackson.annotation.JsonFormat;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser.NumberType;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.annotation.JacksonStdImpl;
import com.fasterxml.jackson.databind.jsonFormatVisitors.JsonFormatVisitorWrapper;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import com.fasterxml.jackson.databind.ser.std.StdScalarSerializer;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;

/**
 * Serializer used for primitive boolean, as well as java.util.Boolean
 * wrapper type.
 *<p>
 * Since this is one of "natural" (aka "native") types, no type information is ever
 * included on serialization (unlike for most other scalar types)
 */
@JacksonStdImpl
public final class BooleanSerializer
    extends StdScalarSerializer<Object>
{
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
    public ValueSerializer<?> createContextual(SerializerProvider serializers,
            BeanProperty property)
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
    public void serialize(Object value, JsonGenerator g, SerializerProvider provider)
            throws JacksonException {
        g.writeBoolean(Boolean.TRUE.equals(value));
    }

    @Override
    public final void serializeWithType(Object value, JsonGenerator g, SerializerProvider provider,
            TypeSerializer typeSer) throws JacksonException
    {
        g.writeBoolean(Boolean.TRUE.equals(value));
    }

    @Override
    public void acceptJsonFormatVisitor(JsonFormatVisitorWrapper visitor, JavaType typeHint) {
        visitor.expectBooleanFormat(typeHint);
    }

    /**
     * Alternate implementation that is used when values are to be serialized
     * as numbers <code>0</code> (false) or <code>1</code> (true).
     */
    final static class AsNumber
        extends StdScalarSerializer<Object>
    {
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
        public ValueSerializer<?> createContextual(SerializerProvider serializers,
                BeanProperty property)
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

        @Override
        public void serialize(Object value, JsonGenerator g, SerializerProvider provider)
                throws JacksonException
        {
            g.writeNumber((Boolean.FALSE.equals(value)) ? 0 : 1);
        }

        @Override
        public final void serializeWithType(Object value, JsonGenerator g, SerializerProvider provider,
                TypeSerializer typeSer)
            throws JacksonException
        {
            // 27-Mar-2017, tatu: Actually here we CAN NOT serialize as number without type,
            //    since with natural types that would map to number, not boolean. So choice
            //    comes to between either add type id, or serialize as boolean. Choose
            //    latter at this point
            g.writeBoolean(Boolean.TRUE.equals(value));
        }

        @Override
        public void acceptJsonFormatVisitor(JsonFormatVisitorWrapper visitor, JavaType typeHint) {
            // 27-Mar-2017, tatu: As usual, bit tricky but... seems like we should call
            //    visitor for actual representation
            visitIntFormat(visitor, typeHint, NumberType.INT);
        }
    }
}
