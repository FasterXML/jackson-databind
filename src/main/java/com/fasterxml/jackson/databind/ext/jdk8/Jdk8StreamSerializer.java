package com.fasterxml.jackson.databind.ext.jdk8;

import java.util.stream.Stream;

import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

/**
 * Common typed stream serializer
 *
 */
public class Jdk8StreamSerializer extends StdSerializer<Stream<?>>
{
    /**
     * Stream elements type (matching T)
     */
    private final JavaType elemType;
    
    /**
     * element specific serializer, if any
     */
    private transient final ValueSerializer<Object> elemSerializer;

    /**
     * Constructor
     *
     * @param streamType Stream type
     * @param elemType   Stream elements type (matching T)
     */
    public Jdk8StreamSerializer(JavaType streamType, JavaType elemType) {
        this(streamType, elemType, null);
    }

    /**
     * Constructor with custom serializer
     *
     * @param streamType     Stream type
     * @param elemType       Stream elements type (matching T)
     * @param elemSerializer Custom serializer to use for element type
     */
    public Jdk8StreamSerializer(JavaType streamType, JavaType elemType, ValueSerializer<Object> elemSerializer) {
        super(streamType);
        this.elemType = elemType;
        this.elemSerializer = elemSerializer;
    }

    @Override
    public ValueSerializer<?> createContextual(SerializerProvider provider, BeanProperty property)
    {
        if (!elemType.hasRawClass(Object.class)
                && (provider.isEnabled(MapperFeature.USE_STATIC_TYPING) || elemType.isFinal())) {
            return new Jdk8StreamSerializer(
                    provider.getTypeFactory().constructParametricType(Stream.class, elemType),
                    elemType,
                    provider.findContentValueSerializer(elemType, property));
        }
        return this;
    }

    @Override
    public void serialize(Stream<?> stream, JsonGenerator g, SerializerProvider ctxt)
        throws JacksonException
    {
        try (Stream<?> s = stream) {
            g.writeStartArray(s);
            
            s.forEach(elem -> {
                if (elemSerializer == null) {
                    ctxt.writeValue(g, elem);
                } else {
                    elemSerializer.serialize(elem, g, ctxt);
                }
            });
            g.writeEndArray();
        } catch (Exception e) {
            // For most regular serializers we won't both handling but streams are typically
            // root values so 
            wrapAndThrow(ctxt, e, stream, g.streamWriteContext().getCurrentIndex());
        }
    }
}
