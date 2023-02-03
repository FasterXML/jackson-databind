package com.fasterxml.jackson.databind.ser;

import java.io.IOException;
import java.util.*;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.ser.impl.*;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.StdScalarSerializer;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

public class AnyGetterTest extends BaseMapTest
{
    static class Bean
    {
        final static Map<String,Boolean> extra = new HashMap<String,Boolean>();
        static {
            extra.put("a", Boolean.TRUE);
        }

        public int getX() { return 3; }

        @JsonAnyGetter
        public Map<String,Boolean> getExtra() { return extra; }
    }

    static class AnyOnlyBean
    {
        @JsonAnyGetter
        public Map<String,Integer> any() {
            HashMap<String,Integer> map = new HashMap<String,Integer>();
            map.put("a", 3);
            return map;
        }
    }

    // For [databind#1376]: allow disabling any-getter
    static class NotEvenAnyBean extends AnyOnlyBean
    {
        @JsonAnyGetter(enabled=false)
        @Override
        public Map<String,Integer> any() {
            throw new RuntimeException("Should not get called!)");
        }

        public int getValue() { return 42; }
    }

    static class MapAsAny
    {
        protected Map<String,Object> stuff = new LinkedHashMap<String,Object>();

        @JsonAnyGetter
        public Map<String,Object> any() {
            return stuff;
        }

        public void add(String key, Object value) {
            stuff.put(key, value);
        }
    }

    static class Issue705Bean
    {
        protected Map<String,String> stuff;

        public Issue705Bean(String key, String value) {
            stuff = new LinkedHashMap<String,String>();
            stuff.put(key, value);
        }

        @JsonSerialize(using = Issue705Serializer.class)
//    @JsonSerialize(converter = MyConverter.class)
        @JsonAnyGetter
        public Map<String, String> getParameters(){
            return stuff;
        }
    }

    @SuppressWarnings("serial")
    static class Issue705Serializer extends StdSerializer<Object>
    {
        public Issue705Serializer() {
            super(Map.class, false);
        }

        @Override
        public void serialize(Object value, JsonGenerator jgen,
                SerializerProvider provider) throws IOException
        {
            StringBuilder sb = new StringBuilder();
            for (Map.Entry<?,?> entry : ((Map<?,?>) value).entrySet()) {
                sb.append('[').append(entry.getKey()).append('/').append(entry.getValue()).append(']');
            }
            jgen.writeStringField("stuff", sb.toString());
        }
    }

    // [databind#1124]
    static class Bean1124
    {
        protected Map<String,String> additionalProperties;

        public void addAdditionalProperty(String key, String value) {
            if (additionalProperties == null) {
                additionalProperties = new HashMap<String,String>();
            }
            additionalProperties.put(key,value);
        }

        public void setAdditionalProperties(Map<String, String> additionalProperties) {
            this.additionalProperties = additionalProperties;
        }

        @JsonAnyGetter
        @JsonSerialize(contentUsing=MyUCSerializer.class)
        public Map<String,String> getAdditionalProperties() { return additionalProperties; }
    }

    // [databind#1124]
    @SuppressWarnings("serial")
    static class MyUCSerializer extends StdScalarSerializer<String>
    {
        public MyUCSerializer() { super(String.class); }

        @Override
        public void serialize(String value, JsonGenerator gen,
                SerializerProvider provider) throws IOException {
            gen.writeString(value.toUpperCase());
        }
    }

    static class Bean2592NoAnnotations
    {
        protected Map<String, String> properties = new LinkedHashMap<>();

        @JsonAnyGetter
        public Map<String, String> getProperties() {
            return properties;
        }

        public void setProperties(Map<String, String> properties) {
            this.properties = properties;
        }

        public void add(String key, String value) {
            properties.put(key, value);
        }
    }

    static class Bean2592PropertyIncludeNonEmpty extends Bean2592NoAnnotations
    {
        @JsonInclude(content = JsonInclude.Include.NON_EMPTY)
        @JsonAnyGetter
        @Override
        public Map<String, String> getProperties() {
            return properties;
        }
    }

    @JsonFilter("Bean2592")
    static class Bean2592WithFilter extends Bean2592NoAnnotations {}

    // [databind#1458]: Allow `@JsonAnyGetter` on fields too
    static class DynaFieldBean {
        public int id;

        @JsonAnyGetter
        @JsonAnySetter
        protected HashMap<String,String> other = new HashMap<String,String>();

        public Map<String,String> any() {
            return other;
        }

        public void set(String name, String value) {
            other.put(name, value);
        }
    }

    // [databind#1458]: Allow `@JsonAnyGetter` on fields too
    public void testDynaFieldBean() throws Exception
    {
        DynaFieldBean b = new DynaFieldBean();
        b.id = 123;
        b.set("name", "Billy");
        assertEquals("{\"id\":123,\"name\":\"Billy\"}", MAPPER.writeValueAsString(b));

        DynaFieldBean result = MAPPER.readValue("{\"id\":2,\"name\":\"Joe\"}", DynaFieldBean.class);
        assertEquals(2, result.id);
        assertEquals("Joe", result.other.get("name"));
    }

    /*
    /**********************************************************
    /* Test methods
    /**********************************************************
     */

    private final ObjectMapper MAPPER = newJsonMapper();

    public void testSimpleAnyBean() throws Exception
    {
        String json = MAPPER.writeValueAsString(new Bean());
        Map<?,?> map = MAPPER.readValue(json, Map.class);
        assertEquals(2, map.size());
        assertEquals(Integer.valueOf(3), map.get("x"));
        assertEquals(Boolean.TRUE, map.get("a"));
    }

    public void testAnyOnly() throws Exception
    {
        ObjectMapper m;

        // First, with normal fail settings:
        m = new ObjectMapper();
        m.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, true);
        String json = serializeAsString(m, new AnyOnlyBean());
        assertEquals("{\"a\":3}", json);

        // then without fail
        m = new ObjectMapper();
        m.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        json = serializeAsString(m, new AnyOnlyBean());
        assertEquals("{\"a\":3}", json);
    }

    public void testAnyDisabling() throws Exception
    {
        String json = MAPPER.writeValueAsString(new NotEvenAnyBean());
        assertEquals(a2q("{'value':42}"), json);
    }

    // Trying to repro [databind#577]
    public void testAnyWithNull() throws Exception
    {
        MapAsAny input = new MapAsAny();
        input.add("bar", null);
        assertEquals(a2q("{'bar':null}"),
                MAPPER.writeValueAsString(input));
    }

    public void testIssue705() throws Exception
    {
        Issue705Bean input = new Issue705Bean("key", "value");
        String json = MAPPER.writeValueAsString(input);
        assertEquals("{\"stuff\":\"[key/value]\"}", json);
    }

    // [databind#1124]
    public void testAnyGetterWithValueSerializer() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        Bean1124 input = new Bean1124();
        input.addAdditionalProperty("key", "value");
        String json = mapper.writeValueAsString(input);
        assertEquals("{\"key\":\"VALUE\"}", json);
    }

    // [databind#2592]
    public void testAnyGetterWithMapperDefaultIncludeNonEmpty() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper()
                .setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
        Bean2592NoAnnotations input = new Bean2592NoAnnotations();
        input.add("non-empty", "property");
        input.add("empty", "");
        input.add("null", null);
        String json = mapper.writeValueAsString(input);
        assertEquals("{\"non-empty\":\"property\"}", json);
    }

    // [databind#2592]
    public void testAnyGetterWithMapperDefaultIncludeNonEmptyAndFilterOnBean() throws Exception
    {
        FilterProvider filters = new SimpleFilterProvider()
                .addFilter("Bean2592", SimpleBeanPropertyFilter.serializeAllExcept("something"));
        ObjectMapper mapper = new ObjectMapper()
                .setSerializationInclusion(JsonInclude.Include.NON_EMPTY)
                .setFilterProvider(filters);
        Bean2592WithFilter input = new Bean2592WithFilter();
        input.add("non-empty", "property");
        input.add("empty", "");
        input.add("null", null);
        String json = mapper.writeValueAsString(input);
        // Unfortunately path for bean with filter is different. It still skips nulls.
        assertEquals("{\"non-empty\":\"property\",\"empty\":\"\"}", json);
    }

    // [databind#2592]
    public void testAnyGetterWithPropertyIncludeNonEmpty() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        Bean2592PropertyIncludeNonEmpty input = new Bean2592PropertyIncludeNonEmpty();
        input.add("non-empty", "property");
        input.add("empty", "");
        input.add("null", null);
        String json = mapper.writeValueAsString(input);
        assertEquals("{\"non-empty\":\"property\"}", json);
    }

    // [databind#2592]
    public void testAnyGetterConfigIncludeNonEmpty() throws Exception
    {
        final ObjectMapper mapper = jsonMapperBuilder()
                .withConfigOverride(Map.class, incl -> incl.setInclude(
                    JsonInclude.Value.construct(JsonInclude.Include.USE_DEFAULTS,
                    JsonInclude.Include.NON_EMPTY)))
                .build();
        Bean2592NoAnnotations input = new Bean2592NoAnnotations();
        input.add("non-empty", "property");
        input.add("empty", "");
        input.add("null", null);
        String json = mapper.writeValueAsString(input);
        assertEquals("{\"non-empty\":\"property\"}", json);
    }
}
