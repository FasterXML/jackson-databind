package com.fasterxml.jackson.databind.ser.std;

import java.io.IOException;
import java.lang.reflect.Type;

import com.fasterxml.jackson.core.*;

import org.codehaus.jackson.node.ObjectNode;
import org.codehaus.jackson.schema.JsonSerializableSchema;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonSerializable;
import com.fasterxml.jackson.databind.JsonSerializableWithType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.TypeSerializer;
import com.fasterxml.jackson.databind.annotate.JacksonStdImpl;
import com.fasterxml.jackson.databind.type.TypeFactory;

/**
 * Generic handler for types that implement {@link JsonSerializable}.
 *<p>
 * Note: given that this is used for anything that implements
 * interface, can not be checked for direct class equivalence.
 */
@JacksonStdImpl
@SuppressWarnings("deprecation")
public class SerializableSerializer
    extends SerializerBase<JsonSerializable>
{
    public final static SerializableSerializer instance = new SerializableSerializer();

    protected SerializableSerializer() { super(JsonSerializable.class); }

    @Override
    public void serialize(JsonSerializable value, JsonGenerator jgen, SerializerProvider provider)
        throws IOException, JsonGenerationException
    {
        value.serialize(jgen, provider);
    }

    @Override
    public final void serializeWithType(JsonSerializable value, JsonGenerator jgen, SerializerProvider provider,
            TypeSerializer typeSer)
        throws IOException, JsonGenerationException
    {
        /* 24-Jan-2009, tatus: This is not quite optimal (perhaps we should
         *   just create separate serializer...), but works until 2.0 will
         *   deprecate non-typed interface
         */
        if (value instanceof JsonSerializableWithType) {
            ((JsonSerializableWithType) value).serializeWithType(jgen, provider, typeSer);
        } else {
            this.serialize(value, jgen, provider);
        }
    }
    
    @Override
    public JsonNode getSchema(SerializerProvider provider, Type typeHint)
        throws JsonMappingException
    {
        ObjectNode objectNode = createObjectNode();
        String schemaType = "any";
        String objectProperties = null;
        String itemDefinition = null;
        if (typeHint != null) {
            Class<?> rawClass = TypeFactory.type(typeHint).getRawClass();
            if (rawClass.isAnnotationPresent(JsonSerializableSchema.class)) {
                JsonSerializableSchema schemaInfo = rawClass.getAnnotation(JsonSerializableSchema.class);
                schemaType = schemaInfo.schemaType();
                if (!"##irrelevant".equals(schemaInfo.schemaObjectPropertiesDefinition())) {
                    objectProperties = schemaInfo.schemaObjectPropertiesDefinition();
                }
                if (!"##irrelevant".equals(schemaInfo.schemaItemDefinition())) {
                    itemDefinition = schemaInfo.schemaItemDefinition();
                }
            }
        }
        objectNode.put("type", schemaType);
        if (objectProperties != null) {
            try {
                objectNode.put("properties", new ObjectMapper().readValue(objectProperties, JsonNode.class));
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        }
        if (itemDefinition != null) {
            try {
                objectNode.put("items", new ObjectMapper().readValue(itemDefinition, JsonNode.class));
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        }
        // always optional, no need to specify:
        //objectNode.put("required", false);
        return objectNode;
    }
}
