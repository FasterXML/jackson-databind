package com.fasterxml.jackson.databind.deser.creators;

import java.util.*;

import com.fasterxml.jackson.annotation.JacksonInject;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JacksonException;
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

    static class IntegerBean
    {
        protected Integer value;

        public IntegerBean(Integer v) { value = v; }

        @JsonCreator
        protected static IntegerBean create(Integer value) {
            return new IntegerBean(value);
        }
    }

    static class LongBean
    {
        protected Long value;

        public LongBean(Long v) { value = v; }

        @JsonCreator
        protected static LongBean create(Long value) {
            return new LongBean(value);
        }
    }

    static class CtorBean711
    {
        protected String name;
        protected int age;

        @JsonCreator
        public CtorBean711(@JacksonInject String n, int a)
        {
            name = n;
            age = a;
        }
    }

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

        @JsonCreator
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

    // [databind#2353]: allow delegating and properties-based
    static class SuperToken2353 {
        public long time;
        public String username;

        @JsonCreator(mode=JsonCreator.Mode.DELEGATING) // invoked when a string is passed
        public static SuperToken2353 from(String username) {
            SuperToken2353 token = new SuperToken2353();
            token.username = username;
            token.time = System.currentTimeMillis();
            return token;
        }

        @JsonCreator(mode=JsonCreator.Mode.PROPERTIES) // invoked when an object is passed, pre-validating property existence
        public static SuperToken2353 create(
                @JsonProperty("name") String username,
                @JsonProperty("time") long time)
        {
            SuperToken2353 token = new SuperToken2353();
            token.username = username;
            token.time = time;

            return token;
        }
    }

    /*
    /**********************************************************
    /* Test methods
    /**********************************************************
     */

    private final ObjectMapper MAPPER = newJsonMapper();

    public void testBooleanDelegate() throws Exception
    {
        // should obviously work with booleans...
        BooleanBean bb = MAPPER.readValue("true", BooleanBean.class);
        assertEquals(Boolean.TRUE, bb.value);

        // but also with value conversion from String
        bb = MAPPER.readValue(q("true"), BooleanBean.class);
        assertEquals(Boolean.TRUE, bb.value);
    }

    public void testIntegerDelegate() throws Exception
    {
        IntegerBean bb = MAPPER.readValue("-13", IntegerBean.class);
        assertEquals(Integer.valueOf(-13), bb.value);

        // but also with value conversion from String (unless blocked)
        bb = MAPPER.readValue(q("127"), IntegerBean.class);
        assertEquals(Integer.valueOf(127), bb.value);
    }

    public void testLongDelegate() throws Exception
    {
        LongBean bb = MAPPER.readValue("11", LongBean.class);
        assertEquals(Long.valueOf(11L), bb.value);

        // but also with value conversion from String (unless blocked)
        bb = MAPPER.readValue(q("-99"), LongBean.class);
        assertEquals(Long.valueOf(-99L), bb.value);
    }

    // should also work with delegate model (single non-annotated arg)
    public void testWithCtorAndDelegate() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        mapper.setInjectableValues(new InjectableValues.Std()
            .addValue(String.class, "Pooka")
            );
        CtorBean711 bean = null;
        try {
            bean = mapper.readValue("38", CtorBean711.class);
        } catch (JacksonException e) {
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
        } catch (JacksonException e) {
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

    // [databind#2353]: allow delegating and properties-based
    public void testMultipleCreators2353() throws Exception
    {
        // first, test delegating
        SuperToken2353 result = MAPPER.readValue(q("Bob"), SuperToken2353.class);
        assertEquals("Bob", result.username);

        // and then properties-based
        result = MAPPER.readValue(a2q("{'name':'Billy', 'time':123}"), SuperToken2353.class);
        assertEquals("Billy", result.username);
        assertEquals(123L, result.time);
    }
}
