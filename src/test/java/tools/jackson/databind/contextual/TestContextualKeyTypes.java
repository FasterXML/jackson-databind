package tools.jackson.databind.contextual;

import java.util.*;

import org.junit.jupiter.api.Test;

import tools.jackson.core.JsonGenerator;
import tools.jackson.core.Version;
import tools.jackson.databind.*;
import tools.jackson.databind.deser.ContextualKeyDeserializer;
import tools.jackson.databind.module.SimpleModule;
import tools.jackson.databind.type.TypeFactory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import static tools.jackson.databind.testutil.DatabindTestUtil.jsonMapperBuilder;

/**
 * Tests to ensure that we can do contextual key serializers and
 * deserializers as well as value ser/deser.
 */
public class TestContextualKeyTypes
{
    /*
    /**********************************************************
    /* Helper classes
    /**********************************************************
     */

    static class ContextualKeySerializer
        extends ValueSerializer<String>
    {
        protected final String _prefix;

        public ContextualKeySerializer() { this(""); }
        public ContextualKeySerializer(String p) {
            _prefix = p;
        }

        @Override
        public void serialize(String value, JsonGenerator g, SerializerProvider provider)
        {
            if (_prefix != null) {
                value = _prefix + value;
            }
            g.writeName(value);
        }

        @Override
        public ValueSerializer<?> createContextual(SerializerProvider prov, BeanProperty property)
        {
            return new ContextualKeySerializer(_prefix+":");
        }
    }

    static class ContextualDeser
        extends KeyDeserializer
        implements ContextualKeyDeserializer
    {
        protected final String _prefix;

        protected ContextualDeser(String p) {
            _prefix = p;
        }

        @Override
        public Object deserializeKey(String key, DeserializationContext ctxt)
        {
            return _prefix + ":" + key;
        }

        @Override
        public KeyDeserializer createContextual(DeserializationContext ctxt,
                BeanProperty property)
        {
            return new ContextualDeser((property == null) ? "ROOT" : property.getName());
        }
    }

    static class MapBean {
        public Map<String, Integer> map;
    }

    /*
    /**********************************************************
    /* Unit tests, serialization
    /**********************************************************
     */

    @Test
    public void testSimpleKeySer() throws Exception
    {
        SimpleModule module = new SimpleModule("test", Version.unknownVersion());
        module.addKeySerializer(String.class, new ContextualKeySerializer("prefix"));
        ObjectMapper mapper = jsonMapperBuilder()
                .addModule(module)
                .build();
        Map<String,Object> input = new HashMap<String,Object>();
        input.put("a", Integer.valueOf(3));
        String json = mapper.writerFor(TypeFactory.defaultInstance().constructMapType(HashMap.class, String.class, Object.class))
            .writeValueAsString(input);
        assertEquals("{\"prefix:a\":3}", json);
    }

    /*
    /**********************************************************
    /* Unit tests, deserialization
    /**********************************************************
     */

    @Test
    public void testSimpleKeyDeser() throws Exception
    {
        SimpleModule module = new SimpleModule("test", Version.unknownVersion());
        module.addKeyDeserializer(String.class, new ContextualDeser("???"));
        ObjectMapper mapper = jsonMapperBuilder()
                .addModule(module)
                .build();
        MapBean result = mapper.readValue("{\"map\":{\"a\":3}}", MapBean.class);
        Map<String,Integer> map = result.map;
        assertNotNull(map);
        assertEquals(1, map.size());
        Map.Entry<String,Integer> entry = map.entrySet().iterator().next();
        assertEquals(Integer.valueOf(3), entry.getValue());
        assertEquals("map:a", entry.getKey());
    }
}
