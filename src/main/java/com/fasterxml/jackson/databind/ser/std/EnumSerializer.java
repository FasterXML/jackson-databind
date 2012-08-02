package com.fasterxml.jackson.databind.ser.std;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.core.io.SerializedString;


import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.annotation.JacksonStdImpl;
import com.fasterxml.jackson.databind.jsonschema.visitors.JsonFormatVisitor;
import com.fasterxml.jackson.databind.jsonschema.visitors.JsonObjectFormatVisitor;
import com.fasterxml.jackson.databind.jsonschema.visitors.JsonStringFormatVisitor;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
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
{
    /**
     * This map contains pre-resolved values (since there are ways
     * to customize actual String constants to use) to use as
     * serializations.
     */
    protected final EnumValues _values;

    public EnumSerializer(EnumValues v) {
        super(Enum.class, false);
        _values = v;
    }

    public static EnumSerializer construct(Class<Enum<?>> enumClass, SerializationConfig config,
            BeanDescription beanDesc)
    {
        // [JACKSON-212]: If toString() is to be used instead, leave EnumValues null
        AnnotationIntrospector intr = config.getAnnotationIntrospector();
        EnumValues v = config.isEnabled(SerializationFeature.WRITE_ENUMS_USING_TO_STRING)
            ? EnumValues.constructFromToString(enumClass, intr) : EnumValues.constructFromName(enumClass, intr);
        return new EnumSerializer(v);
    }
    
    @Override
    public final void serialize(Enum<?> en, JsonGenerator jgen, SerializerProvider provider)
        throws IOException, JsonGenerationException
    {
        // [JACKSON-684]: serialize as index?
        if (provider.isEnabled(SerializationFeature.WRITE_ENUMS_USING_INDEX)) {
            jgen.writeNumber(en.ordinal());
            return;
        }
        jgen.writeString(_values.serializedValueFor(en));
    }
    
    @Override
    public void acceptJsonFormatVisitor(JsonFormatVisitor visitor, JavaType typeHint)
    {
        // [JACKSON-684]: serialize as index?
    	if (visitor.getProvider().isEnabled(SerializationFeature.WRITE_ENUMS_USING_INDEX)) {
    		visitor.integerFormat();
    	} else {
    		JsonStringFormatVisitor stringVisitor = visitor.stringFormat();
    		if (typeHint != null) {
    			if (typeHint.isEnumType()) {
    				Set<String> enums = new HashSet<String>();
    				for (SerializedString value : _values.values()) {
    					enums.add(value.getValue());
    				}
    				stringVisitor.enumTypes(enums);
    			}
    		}
    	}
    }

    public EnumValues getEnumValues() { return _values; }
}

