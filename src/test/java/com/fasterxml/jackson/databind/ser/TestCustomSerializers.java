package com.fasterxml.jackson.databind.ser;

import java.io.IOException;
import java.io.StringWriter;
import java.util.*;

import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Element;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.CollectionSerializer;

/**
 * Test for verifying [JACKSON-238]
 *
 * @author Pablo Lalloni <plalloni@gmail.com>
 * @author tatu
 */
public class TestCustomSerializers
    extends com.fasterxml.jackson.databind.BaseMapTest
{
    /*
    /**********************************************************
    /* Helper beans
    /**********************************************************
     */

    static class ElementSerializer extends JsonSerializer<Element>
    {
        @Override
        public void serialize(Element value, JsonGenerator jgen, SerializerProvider provider) throws IOException, JsonProcessingException {
            jgen.writeString("element");
        }
    }
    
    @JsonSerialize(using = ElementSerializer.class)
    public static class ElementMixin {}

    /*
    /**********************************************************
    /* Unit tests
    /**********************************************************
    */
    
    public void testCustomization() throws Exception
    {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.getSerializationConfig().addMixInAnnotations(Element.class, ElementMixin.class);
        Element element = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument().createElement("el");
        StringWriter sw = new StringWriter();
        objectMapper.writeValue(sw, element);
        assertEquals(sw.toString(), "\"element\"");
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public void testCustomLists() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        SimpleModule module = new SimpleModule("test", Version.unknownVersion());
        JsonSerializer<?> ser = new CollectionSerializer(null, false, null, null, null);
        final JsonSerializer<Object> collectionSerializer = (JsonSerializer<Object>) ser;

        module.addSerializer(Collection.class, new JsonSerializer<Collection>() {
            @Override
            public void serialize(Collection value, JsonGenerator jgen, SerializerProvider provider)
                    throws IOException, JsonProcessingException {
                if (value.size() != 0) {
                    collectionSerializer.serialize(value, jgen, provider);
                } else {
                    jgen.writeNull();
                }
            }
        });
        mapper.registerModule(module);
        assertEquals("null", mapper.writeValueAsString(new ArrayList<Object>()));
    }
}
