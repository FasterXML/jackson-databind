package com.fasterxml.jackson.databind.struct;

import java.util.EnumSet;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.BaseMapTest;
import com.fasterxml.jackson.databind.ObjectMapper;

public class FormatFeatureAcceptSingleTest extends BaseMapTest
{
    static class StringArrayNotAnnoted {
        public String[] values;

        protected StringArrayNotAnnoted() { }
        public StringArrayNotAnnoted(String ... v) { values = v; }
    }
    
    static class StringArrayWrapper {
        @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
        public String[] values;
    }

    static class BooleanArrayWrapper {
        @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
        public boolean[] values;
    }

    static class IntArrayWrapper {
        @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
        public int[] values;
    }

    static class LongArrayWrapper {
        @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
        public long[] values;
    }

    static class FloatArrayWrapper {
        @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
        public float[] values;
    }
    
    static class DoubleArrayWrapper {
        @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
        public double[] values;
    }

    static class StringListWrapper {
        @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
        public List<String> values;
    }

    static class EnumSetWrapper {
        @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
        public EnumSet<ABC> values;
    }
    
    static class RolesInArray {
        @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
        public Role[] roles;
    }

    static class RolesInList {
        @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
        public List<Role> roles;
    }

    static class Role {
        public String ID;
        public String Name;
    }

    private final ObjectMapper MAPPER = new ObjectMapper();

    /*
    /**********************************************************
    /* Test methods, reading with single-element unwrapping
    /**********************************************************
     */

    public void testSingleStringArrayRead() throws Exception {
        String json = aposToQuotes(
                "{ 'values': 'first' }");
        StringArrayWrapper result = MAPPER.readValue(json, StringArrayWrapper.class);
        assertNotNull(result.values);
        assertEquals(1, result.values.length);
        assertEquals("first", result.values[0]);

        // and then without annotation, but with global override
        ObjectMapper mapper = new ObjectMapper();
        mapper.configOverride(String[].class).setFormat(JsonFormat.Value.empty()
                .withFeature(JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY));
        StringArrayNotAnnoted result2 = mapper.readValue(json, StringArrayNotAnnoted.class);
        assertNotNull(result2.values);
        assertEquals(1, result2.values.length);
        assertEquals("first", result2.values[0]);
    }

    public void testSingleIntArrayRead() throws Exception {
        String json = aposToQuotes(
                "{ 'values': 123 }");
        IntArrayWrapper result = MAPPER.readValue(json, IntArrayWrapper.class);
        assertNotNull(result.values);
        assertEquals(1, result.values.length);
        assertEquals(123, result.values[0]);
    }

    public void testSingleLongArrayRead() throws Exception {
        String json = aposToQuotes(
                "{ 'values': -205 }");
        LongArrayWrapper result = MAPPER.readValue(json, LongArrayWrapper.class);
        assertNotNull(result.values);
        assertEquals(1, result.values.length);
        assertEquals(-205L, result.values[0]);
    }

    public void testSingleBooleanArrayRead() throws Exception {
        String json = aposToQuotes(
                "{ 'values': true }");
        BooleanArrayWrapper result = MAPPER.readValue(json, BooleanArrayWrapper.class);
        assertNotNull(result.values);
        assertEquals(1, result.values.length);
        assertEquals(true, result.values[0]);
    }

    public void testSingleDoubleArrayRead() throws Exception {
        String json = aposToQuotes(
                "{ 'values': -0.5 }");
        DoubleArrayWrapper result = MAPPER.readValue(json, DoubleArrayWrapper.class);
        assertNotNull(result.values);
        assertEquals(1, result.values.length);
        assertEquals(-0.5, result.values[0]);
    }

    public void testSingleFloatArrayRead() throws Exception {
        String json = aposToQuotes(
                "{ 'values': 0.25 }");
        FloatArrayWrapper result = MAPPER.readValue(json, FloatArrayWrapper.class);
        assertNotNull(result.values);
        assertEquals(1, result.values.length);
        assertEquals(0.25f, result.values[0]);
    }
    
    public void testSingleElementArrayRead() throws Exception {
        String json = aposToQuotes(
                "{ 'roles': { 'Name': 'User', 'ID': '333' } }");
        RolesInArray response = MAPPER.readValue(json, RolesInArray.class);
        assertNotNull(response.roles);
        assertEquals(1, response.roles.length);
        assertEquals("333", response.roles[0].ID);
    }
    
    public void testSingleStringListRead() throws Exception {
        String json = aposToQuotes(
                "{ 'values': 'first' }");
        StringListWrapper result = MAPPER.readValue(json, StringListWrapper.class);
        assertNotNull(result.values);
        assertEquals(1, result.values.size());
        assertEquals("first", result.values.get(0));
    }

    public void testSingleElementListRead() throws Exception {
        String json = aposToQuotes(
                "{ 'roles': { 'Name': 'User', 'ID': '333' } }");
        RolesInList response = MAPPER.readValue(json, RolesInList.class);
        assertNotNull(response.roles);
        assertEquals(1, response.roles.size());
        assertEquals("333", response.roles.get(0).ID);
    }

    public void testSingleEnumSetRead() throws Exception {
        EnumSetWrapper result = MAPPER.readValue(aposToQuotes("{ 'values': 'B' }"),
                EnumSetWrapper.class);
        assertNotNull(result.values);
        assertEquals(1, result.values.size());
        assertEquals(ABC.B, result.values.iterator().next());
    }
}
