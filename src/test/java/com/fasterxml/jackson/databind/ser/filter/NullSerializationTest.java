package com.fasterxml.jackson.databind.ser.filter;

import java.io.*;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.core.json.JsonFactory;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.cfg.GeneratorSettings;
import com.fasterxml.jackson.databind.cfg.SerializationContexts;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.ser.DefaultSerializerProvider;
import com.fasterxml.jackson.databind.ser.SerializerCache;
import com.fasterxml.jackson.databind.ser.SerializerFactory;

public class NullSerializationTest
    extends BaseMapTest
{
    static class NullSerializer extends JsonSerializer<Object>
    {
        @Override
        public void serialize(Object value, JsonGenerator gen, SerializerProvider provider)
            throws IOException
        {
            gen.writeString("foobar");
        }
    }

    static class Bean1 {
        public String name = null;
    }

    static class Bean2 {
        public String type = null;
    }
    
    @SuppressWarnings("serial")
    static class MyNullSerializerContexts extends SerializationContexts
    {
        public MyNullSerializerContexts() { super(new JsonFactory()); }

        @Override
        public SerializationContexts snapshot() {
            return this;
        }

        @Override
        public DefaultSerializerProvider createContext(SerializationConfig config,
                GeneratorSettings genSettings,
                SerializerFactory serFactory) {
            return new MyNullSerializerProvider(_streamFactory, _serializerCache,
                    config, genSettings, serFactory);
        }
    }

    @SuppressWarnings("serial")
    static class MyNullSerializerProvider extends DefaultSerializerProvider
    {
        public MyNullSerializerProvider(TokenStreamFactory streamFactory,
                SerializerCache cache, SerializationConfig config,
                GeneratorSettings genSettings, SerializerFactory f) {
            super(streamFactory, cache, config, genSettings, f);
        }

        @Override
        public JsonSerializer<Object> findNullValueSerializer(BeanProperty property)
            throws JsonMappingException
        {
            if ("name".equals(property.getName())) {
                return new NullSerializer();
            }
            return super.findNullValueSerializer(property);
        }
    }

    static class BeanWithNullProps
    {
        @JsonSerialize(nullsUsing=NullSerializer.class)
        public String a = null;
    }

/*
    @JsonSerialize(nullsUsing=NullSerializer.class)
    static class NullValuedType { }
*/
    
    /*
    /**********************************************************************
    /* Test methods
    /**********************************************************************
     */

    private final ObjectMapper MAPPER = objectMapper();
    
    public void testSimple() throws Exception
    {
        assertEquals("null", MAPPER.writeValueAsString(null));
    }

    public void testOverriddenDefaultNulls() throws Exception
    {
        ObjectMapper m = ObjectMapper.builder()
                .addModule(new SimpleModule()
                        .setDefaultNullValueSerializer(new NullSerializer()))
                .build();
        assertEquals("\"foobar\"", m.writeValueAsString(null));
    }

    public void testCustomNulls() throws Exception
    {
        ObjectMapper m = ObjectMapper.builder()
                .serializationContexts(new MyNullSerializerContexts())
                .build();
        assertEquals("{\"name\":\"foobar\"}", m.writeValueAsString(new Bean1()));
        assertEquals("{\"type\":null}", m.writeValueAsString(new Bean2()));
    }

    public void testCustomNullForTrees() throws Exception
    {
        ObjectNode root = MAPPER.createObjectNode();
        root.putNull("a");

        // by default, null is... well, null
        assertEquals("{\"a\":null}", MAPPER.writeValueAsString(root));

        // but then we can customize it:
        ObjectMapper m = ObjectMapper.builder()
                .serializationContexts(new MyNullSerializerContexts())
                .addModule(new SimpleModule()
                        .setDefaultNullValueSerializer(new NullSerializer()))
                .build();
        assertEquals("{\"a\":\"foobar\"}", m.writeValueAsString(root));
    }

    public void testNullSerializerForProperty() throws Exception
    {
        assertEquals("{\"a\":\"foobar\"}", MAPPER.writeValueAsString(new BeanWithNullProps()));
    }
}
