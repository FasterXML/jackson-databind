package com.fasterxml.jackson.databind.ser.std;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.*;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonFormat.Shape;
import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.annotation.JacksonStdImpl;
import com.fasterxml.jackson.databind.introspect.Annotated;
import com.fasterxml.jackson.databind.jsonFormatVisitors.JsonFormatVisitorWrapper;
import com.fasterxml.jackson.databind.jsonFormatVisitors.JsonIntegerFormatVisitor;
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
 * 
 * @author tatu
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
    
    /**
     * @deprecated Since 2.1
     */
    @Deprecated
    public EnumSerializer(EnumValues v) {
        this(v, null);
    }

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
        /* 08-Apr-2015, tatu: As per [databind#749], we can not statically determine
         *   between name() and toString(), need to construct `EnumValues` with names,
         *   handle toString() case dynamically (for example)
         */
        EnumValues v = EnumValues.constructFromName(config, (Class<Enum<?>>) enumClass);
        Boolean serializeAsIndex = _isShapeWrittenUsingIndex(enumClass, format, true);
        return new EnumSerializer(v, serializeAsIndex);
    }

    /**
     * To support some level of per-property configuration, we will need
     * to make things contextual. We are limited to "textual vs index"
     * choice here, however.
     */
    @Override
    public JsonSerializer<?> createContextual(SerializerProvider prov, BeanProperty property) throws JsonMappingException
    {
        if (property != null) {
            JsonFormat.Value format = prov.getAnnotationIntrospector().findFormat((Annotated) property.getMember());
            if (format != null) {
                Boolean serializeAsIndex = _isShapeWrittenUsingIndex(property.getType().getRawClass(), format, false);
                if (serializeAsIndex != _serializeAsIndex) {
                    return new EnumSerializer(_values, serializeAsIndex);
                }
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
        // [JACKSON-684]: serialize as index?
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
    
    @Override
    public JsonNode getSchema(SerializerProvider provider, Type typeHint)
    {
        // [JACKSON-684]: serialize as index?
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
            JsonIntegerFormatVisitor v2 = visitor.expectIntegerFormat(typeHint);
            if (v2 != null) { // typically serialized as a small number (byte or int)
                v2.numberType(JsonParser.NumberType.INT);
            }
            return;
        }
        boolean usingToString = (serializers != null)  && 
                serializers.isEnabled(SerializationFeature.WRITE_ENUMS_USING_TO_STRING);

        JsonStringFormatVisitor stringVisitor = visitor.expectStringFormat(typeHint);
        if (typeHint != null && stringVisitor != null) {
            Set<String> enums = new LinkedHashSet<String>();
            for (SerializableString value : _values.values()) {
                if (usingToString) {
                    enums.add(value.toString());
                } else {
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
     * Helper method called to check whether 
     */
    protected static Boolean _isShapeWrittenUsingIndex(Class<?> enumClass,
            JsonFormat.Value format, boolean fromClass)
    {
        JsonFormat.Shape shape = (format == null) ? null : format.getShape();
        if (shape == null) {
            return null;
        }
        if (shape == Shape.ANY || shape == Shape.SCALAR) { // i.e. "default", check dynamically
            return null;
        }
        if (shape == Shape.STRING) {
            return Boolean.FALSE;
        }
        // 01-Oct-2014, tatu: For convenience, consider "as-array" to also mean 'yes, use index')
        if (shape.isNumeric() || (shape == Shape.ARRAY)) {
            return Boolean.TRUE;
        }
        throw new IllegalArgumentException("Unsupported serialization shape ("+shape+") for Enum "+enumClass.getName()
                    +", not supported as "
                    + (fromClass? "class" : "property")
                    +" annotation");
    }
}
