package com.fasterxml.jackson.databind.ser.std;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonFormat.Shape;

import com.fasterxml.jackson.core.*;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.annotation.JacksonStdImpl;
import com.fasterxml.jackson.databind.jsonFormatVisitors.JsonFormatVisitorWrapper;
import com.fasterxml.jackson.databind.jsonFormatVisitors.JsonStringFormatVisitor;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.ser.ContextualSerializer;
import com.fasterxml.jackson.databind.util.EnumValues;

/**
 * Standard serializer used for {@link java.lang.Enum} types.
 *<p>
 * Based on {@link StdScalarSerializer} since the JSON value is
 * scalar (String).
 */
@JacksonStdImpl
public class EnumSerializer
    extends StdScalarSerializer<Enum<?>>
    implements ContextualSerializer
{
    private static final long serialVersionUID = 1L;

    /**
     * This map contains pre-resolved values (since there are ways
     * to customize actual String constants to use) to use as
     * serializations.
     */
    protected final EnumValues _values;

    /**
     * Flag that is set if we statically know serialization choice
     * between index and textual format (null if it needs to be dynamically
     * checked).
     *
     * @since 2.1
     */
    protected final Boolean _serializeAsIndex;

    /*
    /**********************************************************
    /* Construction, initialization
    /**********************************************************
     */

    public EnumSerializer(EnumValues v, Boolean serializeAsIndex)
    {
        super(v.getEnumClass(), false);
        _values = v;
        _serializeAsIndex = serializeAsIndex;
    }

    /**
     * Factory method used by {@link com.fasterxml.jackson.databind.ser.BasicSerializerFactory}
     * for constructing serializer instance of Enum types.
     *
     * @since 2.1
     */
    @SuppressWarnings("unchecked")
    public static EnumSerializer construct(Class<?> enumClass, SerializationConfig config,
            BeanDescription beanDesc, JsonFormat.Value format)
    {
        /* 08-Apr-2015, tatu: As per [databind#749], we cannot statically determine
         *   between name() and toString(), need to construct `EnumValues` with names,
         *   handle toString() case dynamically (for example)
         */
        EnumValues v = EnumValues.constructFromName(config, (Class<Enum<?>>) enumClass);
        Boolean serializeAsIndex = _isShapeWrittenUsingIndex(enumClass, format, true, null);
        return new EnumSerializer(v, serializeAsIndex);
    }

    /**
     * To support some level of per-property configuration, we will need
     * to make things contextual. We are limited to "textual vs index"
     * choice here, however.
     */
    @Override
    public JsonSerializer<?> createContextual(SerializerProvider serializers,
            BeanProperty property) throws JsonMappingException
    {
        JsonFormat.Value format = findFormatOverrides(serializers,
                property, handledType());
        if (format != null) {
            Class<?> type = handledType();
            Boolean serializeAsIndex = _isShapeWrittenUsingIndex(type,
                    format, false, _serializeAsIndex);
            if (!Objects.equals(serializeAsIndex, _serializeAsIndex)) {
                return new EnumSerializer(_values, serializeAsIndex);
            }
        }
        return this;
    }

    /*
    /**********************************************************
    /* Extended API for Jackson databind core
    /**********************************************************
     */

    public EnumValues getEnumValues() { return _values; }

    /*
    /**********************************************************
    /* Actual serialization
    /**********************************************************
     */

    @Override
    public final void serialize(Enum<?> en, JsonGenerator gen, SerializerProvider serializers)
        throws IOException
    {
        if (_serializeAsIndex(serializers)) {
            gen.writeNumber(en.ordinal());
            return;
        }
        // [databind#749]: or via toString()?
        if (serializers.isEnabled(SerializationFeature.WRITE_ENUMS_USING_TO_STRING)) {
            gen.writeString(en.toString());
            return;
        }
        gen.writeString(_values.serializedValueFor(en));
    }

    /*
    /**********************************************************
    /* Schema support
    /**********************************************************
     */

    /**
     * @deprecated Since 2.15
     */
    @Deprecated
    @Override
    public JsonNode getSchema(SerializerProvider provider, Type typeHint)
    {
        if (_serializeAsIndex(provider)) {
            return createSchemaNode("integer", true);
        }
        ObjectNode objectNode = createSchemaNode("string", true);
        if (typeHint != null) {
            JavaType type = provider.constructType(typeHint);
            if (type.isEnumType()) {
                ArrayNode enumNode = objectNode.putArray("enum");
                for (SerializableString value : _values.values()) {
                    enumNode.add(value.getValue());
                }
            }
        }
        return objectNode;
    }

    @Override
    public void acceptJsonFormatVisitor(JsonFormatVisitorWrapper visitor, JavaType typeHint)
        throws JsonMappingException
    {
        SerializerProvider serializers = visitor.getProvider();
        if (_serializeAsIndex(serializers)) {
            visitIntFormat(visitor, typeHint, JsonParser.NumberType.INT);
            return;
        }
        JsonStringFormatVisitor stringVisitor = visitor.expectStringFormat(typeHint);
        if (stringVisitor != null) {
            Set<String> enums = new LinkedHashSet<String>();

            // Use toString()?
            if ((serializers != null) &&
                    serializers.isEnabled(SerializationFeature.WRITE_ENUMS_USING_TO_STRING)) {
                for (Enum<?> e : _values.enums()) {
                    enums.add(e.toString());
                }
            } else {
                // No, serialize using name() or explicit overrides
                for (SerializableString value : _values.values()) {
                    enums.add(value.getValue());
                }
            }
            stringVisitor.enumTypes(enums);
        }
    }

    /*
    /**********************************************************
    /* Helper methods
    /**********************************************************
     */

    protected final boolean _serializeAsIndex(SerializerProvider serializers)
    {
        if (_serializeAsIndex != null) {
            return _serializeAsIndex.booleanValue();
        }
        return serializers.isEnabled(SerializationFeature.WRITE_ENUMS_USING_INDEX);
    }

    /**
     * Helper method called to check whether serialization should be done using
     * index (number) or not.
     */
    protected static Boolean _isShapeWrittenUsingIndex(Class<?> enumClass,
            JsonFormat.Value format, boolean fromClass,
            Boolean defaultValue)
    {
        JsonFormat.Shape shape = (format == null) ? null : format.getShape();
        if (shape == null) {
            return defaultValue;
        }
        // i.e. "default", check dynamically
        if (shape == Shape.ANY || shape == Shape.SCALAR) {
            return defaultValue;
        }
        // 19-May-2016, tatu: also consider "natural" shape
        if (shape == Shape.STRING || shape == Shape.NATURAL) {
            return Boolean.FALSE;
        }
        // 01-Oct-2014, tatu: For convenience, consider "as-array" to also mean 'yes, use index')
        if (shape.isNumeric() || (shape == Shape.ARRAY)) {
            return Boolean.TRUE;
        }
        // 07-Mar-2017, tatu: Also means `OBJECT` not available as property annotation...
        throw new IllegalArgumentException(String.format(
                "Unsupported serialization shape (%s) for Enum %s, not supported as %s annotation",
                    shape, enumClass.getName(), (fromClass? "class" : "property")));
    }
}
