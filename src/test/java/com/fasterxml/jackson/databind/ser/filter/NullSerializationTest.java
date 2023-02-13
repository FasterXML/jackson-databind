package com.fasterxml.jackson.databind.ser.filter;

import java.io.*;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.deser.filter.NullConversionsPojoTest;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.ser.DefaultSerializerProvider;
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
    static class MyNullProvider extends DefaultSerializerProvider
    {
        public MyNullProvider() { super(); }
        public MyNullProvider(MyNullProvider base, SerializationConfig config, SerializerFactory jsf) {
            super(base, config, jsf);
        }

        // not really a proper impl, but has to do
        @Override
        public DefaultSerializerProvider copy() {
            return this;
        }

        @Override
        public DefaultSerializerProvider createInstance(SerializationConfig config, SerializerFactory jsf) {
            return new MyNullProvider(this, config, jsf);
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
    /**********************************************************
    /* Test methods
    /**********************************************************
     */

    private final ObjectMapper MAPPER = objectMapper();

    public void testSimple() throws Exception
    {
        assertEquals("null", MAPPER.writeValueAsString(null));
    }

    public void testOverriddenDefaultNulls() throws Exception
    {
        DefaultSerializerProvider sp = new DefaultSerializerProvider.Impl();
        sp.setNullValueSerializer(new NullSerializer());
        ObjectMapper m = new ObjectMapper();
        m.setSerializerProvider(sp);
        assertEquals("\"foobar\"", m.writeValueAsString(null));
    }

    public void testCustomNulls() throws Exception
    {
        ObjectMapper m = new ObjectMapper();
        m.setSerializerProvider(new MyNullProvider());
        assertEquals("{\"name\":\"foobar\"}", m.writeValueAsString(new Bean1()));
        assertEquals("{\"type\":null}", m.writeValueAsString(new Bean2()));
    }

    // #281
    public void testCustomNullForTrees() throws Exception
    {
        ObjectNode root = MAPPER.createObjectNode();
        root.putNull("a");

        // by default, null is... well, null
        assertEquals("{\"a\":null}", MAPPER.writeValueAsString(root));

        // but then we can customize it:
        DefaultSerializerProvider prov = new MyNullProvider();
        prov.setNullValueSerializer(new NullSerializer());
        ObjectMapper m = new ObjectMapper();
        m.setSerializerProvider(prov);
        assertEquals("{\"a\":\"foobar\"}", m.writeValueAsString(root));
    }

    public void testNullSerializerForProperty() throws Exception
    {
        assertEquals("{\"a\":\"foobar\"}", MAPPER.writeValueAsString(new BeanWithNullProps()));
    }


    static class Issue3645BeanC {
        private String text;
        private Collection<Integer> products;

        public Issue3645BeanC(
            @JsonProperty("text") String text,
            @JsonProperty("products") @JsonSetter(nulls = Nulls.AS_EMPTY) Collection<Integer> products
        ) {
            this.text = text;
            this.products = products;
        }
    }

    public void testDeserializeWithNullAsEmptyWithBuilderDefaults() throws JsonProcessingException {
        String json = "{\"text\": \"Computer\", \"products\" : null}";

        String actual = MAPPER.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false)
            .writeValueAsString(new Issue3645BeanC("hello-world", Collections.emptyList()));

        assertEquals(actual, "hihi");
    }

    public void testDeserializeWithNullAsEmptyWithBuilderDefaults2() throws JsonProcessingException {
        String json = "{\"text\": \"Computer\"}";

        ObjectMapper mapper = JsonMapper.builder()
            .defaultSetterInfo(JsonSetter.Value.construct(Nulls.AS_EMPTY, Nulls.AS_EMPTY))
            .build();

        Issue3645BeanC actual = mapper.readValue(json, Issue3645BeanC.class);

        assertEquals(actual.text, "Computer");
        assertTrue(actual.products.isEmpty());
    }
}
