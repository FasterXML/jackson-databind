package com.fasterxml.jackson.databind.deser.creators;

import com.fasterxml.jackson.annotation.JsonCreator;

import java.util.Map;

import com.fasterxml.jackson.annotation.JacksonInject;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.util.TokenBuffer;

public class TestCreatorsDelegating extends BaseMapTest
{
    static class BooleanBean
    {
        protected Boolean value;

        public BooleanBean(Boolean v) { value = v; }
        
        @JsonCreator
        protected static BooleanBean create(Boolean value) {
            return new BooleanBean(value);
        }
    }

    // for [JACKSON-711]; should allow delegate-based one(s) too
    static class CtorBean711
    {
        protected String name;
        protected int age;

        @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
        public CtorBean711(@JacksonInject String n, int a)
        {
            name = n;
            age = a;
        }
    }

    // for [JACKSON-711]; should allow delegate-based one(s) too
    static class FactoryBean711
    {
        protected String name1;
        protected String name2;
        protected int age;

        private FactoryBean711(int a, String n1, String n2) {
            age = a;
            name1 = n1;
            name2 = n2;
        }

        @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
        public static FactoryBean711 create(@JacksonInject String n1, int a, @JacksonInject String n2) {
            return new FactoryBean711(a, n1, n2);
        }
    }

    static class Value592
    {
        protected Object stuff;

        protected Value592(Object ob, boolean bogus) {
            stuff = ob;
        }

        @JsonCreator
        public static Value592 from(TokenBuffer buffer) {
            return new Value592(buffer, false);
        }
    }

    static class MapBean
    {
        protected Map<String,Long> map;
        
        @JsonCreator
        public MapBean(Map<String, Long> map) {
            this.map = map;
        }
    }
    
    /*
    /**********************************************************
    /* Unit tests
    /**********************************************************
     */

    private final ObjectMapper MAPPER = new ObjectMapper();
    
    public void testBooleanDelegate() throws Exception
    {
        // should obviously work with booleans...
        BooleanBean bb = MAPPER.readValue("true", BooleanBean.class);
        assertEquals(Boolean.TRUE, bb.value);

        // but also with value conversion from String
        bb = MAPPER.readValue(quote("true"), BooleanBean.class);
        assertEquals(Boolean.TRUE, bb.value);
    }
    
    // As per [JACKSON-711]: should also work with delegate model (single non-annotated arg)
    public void testWithCtorAndDelegate() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        mapper.setInjectableValues(new InjectableValues.Std()
            .addValue(String.class, "Pooka")
            );
        CtorBean711 bean = null;
        try {
            bean = mapper.readValue("38", CtorBean711.class);
        } catch (JsonMappingException e) {
            fail("Did not expect problems, got: "+e.getMessage());
        }
        assertEquals(38, bean.age);
        assertEquals("Pooka", bean.name);
    }

    public void testWithFactoryAndDelegate() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        mapper.setInjectableValues(new InjectableValues.Std()
            .addValue(String.class, "Fygar")
            );
        FactoryBean711 bean = null;
        try {
            bean = mapper.readValue("38", FactoryBean711.class);
        } catch (JsonMappingException e) {
            fail("Did not expect problems, got: "+e.getMessage());
        }
        assertEquals(38, bean.age);
        assertEquals("Fygar", bean.name1);
        assertEquals("Fygar", bean.name2);
    }

    // [databind#592]
    public void testDelegateWithTokenBuffer() throws Exception
    {
        Value592 value = MAPPER.readValue("{\"a\":1,\"b\":2}", Value592.class);
        assertNotNull(value);
        Object ob = value.stuff;
        assertEquals(TokenBuffer.class, ob.getClass());
        JsonParser jp = ((TokenBuffer) ob).asParser();
        assertToken(JsonToken.START_OBJECT, jp.nextToken());
        assertToken(JsonToken.FIELD_NAME, jp.nextToken());
        assertEquals("a", jp.currentName());
        assertToken(JsonToken.VALUE_NUMBER_INT, jp.nextToken());
        assertEquals(1, jp.getIntValue());
        assertToken(JsonToken.FIELD_NAME, jp.nextToken());
        assertEquals("b", jp.currentName());
        assertToken(JsonToken.VALUE_NUMBER_INT, jp.nextToken());
        assertEquals(2, jp.getIntValue());
        assertToken(JsonToken.END_OBJECT, jp.nextToken());
        jp.close();
    }

    @SuppressWarnings("unchecked")
    public void testIssue465() throws Exception
    {
        final String JSON = "{\"A\":12}";

        // first, test with regular Map, non empty
        Map<String,Long> map = MAPPER.readValue(JSON, Map.class);
        assertEquals(1, map.size());
        assertEquals(Integer.valueOf(12), map.get("A"));
        
        MapBean bean = MAPPER.readValue(JSON, MapBean.class);
        assertEquals(1, bean.map.size());
        assertEquals(Long.valueOf(12L), bean.map.get("A"));

        // and then empty ones
        final String EMPTY_JSON = "{}";

        map = MAPPER.readValue(EMPTY_JSON, Map.class);
        assertEquals(0, map.size());
        
        bean = MAPPER.readValue(EMPTY_JSON, MapBean.class);
        assertEquals(0, bean.map.size());
    }
}
