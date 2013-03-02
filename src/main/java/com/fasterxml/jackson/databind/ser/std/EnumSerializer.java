package com.fasterxml.jackson.databind.ser.std;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.*;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonFormat.Shape;
import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.core.io.SerializedString;

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
        super(Enum.class, false);
        _values = v;
        _serializeAsIndex = serializeAsIndex;
    }
    
    /**
     * Factory method used by {@link com.fasterxml.jackson.databind.ser.BasicSerializerFactory}
     * for constructing serializer instance of Enum types.
     * 
     * @since 2.1
     */
    public static EnumSerializer construct(Class<Enum<?>> enumClass, SerializationConfig config,
            BeanDescription beanDesc, JsonFormat.Value format)
    {
        // [JACKSON-212]: If toString() is to be used instead, leave EnumValues null
        AnnotationIntrospector intr = config.getAnnotationIntrospector();
        EnumValues v = config.isEnabled(SerializationFeature.WRITE_ENUMS_USING_TO_STRING)
            ? EnumValues.constructFromToString(enumClass, intr) : EnumValues.constructFromName(enumClass, intr);
        Boolean serializeAsIndex = _isShapeWrittenUsingIndex(enumClass, format, true);
        return new EnumSerializer(v, serializeAsIndex);
    }
    
    /**
     * @deprecated Since 2.1 use the variant that takes in <code>format</code> argument.
     */
    @Deprecated
    public static EnumSerializer construct(Class<Enum<?>> enumClass, SerializationConfig config,
            BeanDescription beanDesc)
    {
        return construct(enumClass, config, beanDesc, beanDesc.findExpectedFormat(null));
    }

    /**
     * To support some level of per-property configuration, we will need
     * to make things contextual. We are limited to "textual vs index"
     * choice here, however.
     */
    @Override
    public JsonSerializer<?> createContextual(SerializerProvider prov,
            BeanProperty property) throws JsonMappingException
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
    public final void serialize(Enum<?> en, JsonGenerator jgen, SerializerProvider provider)
        throws IOException, JsonGenerationException
    {
        // [JACKSON-684]: serialize as index?
        if (_serializeAsIndex(provider)) {
            jgen.writeNumber(en.ordinal());
            return;
        }
        jgen.writeString(_values.serializedValueFor(en));
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
                for (SerializedString value : _values.values()) {
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
        // [JACKSON-684]: serialize as index?
        if (visitor.getProvider().isEnabled(SerializationFeature.WRITE_ENUMS_USING_INDEX)) {
            JsonIntegerFormatVisitor v2 = visitor.expectIntegerFormat(typeHint);
            if (v2 != null) { // typically serialized as a small number (byte or int)
                v2.numberType(JsonParser.NumberType.INT);
            }
        } else {
    		JsonStringFormatVisitor stringVisitor = visitor.expectStringFormat(typeHint);
    		if (typeHint != null && stringVisitor != null) {
    			if (typeHint.isEnumType()) {
    				Set<String> enums = new LinkedHashSet<String>();
    				for (SerializedString value : _values.values()) {
    					enums.add(value.getValue());
    				}
    				stringVisitor.enumTypes(enums);
    			}
    		}
        }
    }

    /*
    /**********************************************************
    /* Helper methods
    /**********************************************************
     */
    
    protected final boolean _serializeAsIndex(SerializerProvider provider)
    {
        if (_serializeAsIndex != null) {
            return _serializeAsIndex.booleanValue();
        }
        return provider.isEnabled(SerializationFeature.WRITE_ENUMS_USING_INDEX);
        
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
        if (shape.isNumeric()) {
            return Boolean.TRUE;
        }
        throw new IllegalArgumentException("Unsupported serialization shape ("+shape+") for Enum "+enumClass.getName()
                    +", not supported as "
                    + (fromClass? "class" : "property")
                    +" annotation");
    }
}

