package tools.jackson.databind.ser.filter;

import tools.jackson.core.*;
import tools.jackson.databind.*;
import tools.jackson.databind.annotation.JsonSerialize;
import tools.jackson.databind.cfg.GeneratorSettings;
import tools.jackson.databind.cfg.SerializationContexts;
import tools.jackson.databind.module.SimpleModule;
import tools.jackson.databind.node.ObjectNode;
import tools.jackson.databind.ser.SerializationContextExt;
import tools.jackson.databind.ser.SerializerCache;
import tools.jackson.databind.ser.SerializerFactory;
import tools.jackson.databind.ser.std.StdSerializer;

public class CustomNullSerializationTest
    extends BaseMapTest
{
    static class NullAsFoobarSerializer extends StdSerializer<Object>
    {
        public NullAsFoobarSerializer() { super(Object.class); }
        @Override
        public void serialize(Object value, JsonGenerator gen, SerializerProvider provider)
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
        public MyNullSerializerContexts() { super(); }
        public MyNullSerializerContexts(TokenStreamFactory tsf, SerializerFactory serializerFactory,
                SerializerCache cache) {
            super(tsf, serializerFactory, cache);
        }

        @Override
        public SerializationContexts forMapper(Object mapper,
                TokenStreamFactory tsf, SerializerFactory serializerFactory,
                SerializerCache cache) {
            return new MyNullSerializerContexts(tsf, serializerFactory, cache);
        }

        @Override
        public SerializationContextExt createContext(SerializationConfig config,
                GeneratorSettings genSettings) {
            return new MyNullSerializerProvider(_streamFactory, _cache,
                    config, genSettings, _serializerFactory);
        }
    }

    static class MyNullSerializerProvider extends SerializationContextExt
    {
        public MyNullSerializerProvider(TokenStreamFactory streamFactory,
                SerializerCache cache, SerializationConfig config,
                GeneratorSettings genSettings, SerializerFactory f) {
            super(streamFactory, config, genSettings, f, cache);
        }

        @Override
        public ValueSerializer<Object> findNullValueSerializer(BeanProperty property)
        {
            if ("name".equals(property.getName())) {
                return new NullAsFoobarSerializer();
            }
            return super.findNullValueSerializer(property);
        }
    }

    static class BeanWithNullProps
    {
        @JsonSerialize(nullsUsing=NullAsFoobarSerializer.class)
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

    public void testOverriddenDefaultValueNulls() throws Exception
    {
        ObjectMapper m = jsonMapperBuilder()
                .addModule(new SimpleModule()
                        .setDefaultNullValueSerializer(new NullAsFoobarSerializer()))
                .build();
        assertEquals("\"foobar\"", m.writeValueAsString(null));
    }

    public void testCustomNulls() throws Exception
    {
        ObjectMapper m = jsonMapperBuilder()
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
        ObjectMapper m = jsonMapperBuilder()
                .serializationContexts(new MyNullSerializerContexts())
                .addModule(new SimpleModule()
                        .setDefaultNullValueSerializer(new NullAsFoobarSerializer()))
                .build();
        assertEquals("{\"a\":\"foobar\"}", m.writeValueAsString(root));
    }

    public void testNullSerializerForProperty() throws Exception
    {
        assertEquals("{\"a\":\"foobar\"}", MAPPER.writeValueAsString(new BeanWithNullProps()));
    }
}
