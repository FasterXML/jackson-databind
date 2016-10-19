package com.fasterxml.jackson.databind.struct;

import java.net.URI;
import java.util.*;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

/**
 * Tests for {@link JsonFormat} and specifically <code>JsonFormat.Feature</code>s.
 */
public class FormatFeaturesTest extends BaseMapTest
{
    @JsonPropertyOrder( { "strings", "ints", "bools" })
    static class WrapWriteWithArrays
    {
        @JsonProperty("strings")
        @JsonFormat(with={ JsonFormat.Feature.WRITE_SINGLE_ELEM_ARRAYS_UNWRAPPED })
        public String[] _strings = new String[] {
            "a"
        };

        @JsonFormat(without={ JsonFormat.Feature.WRITE_SINGLE_ELEM_ARRAYS_UNWRAPPED })
        public int[] ints = new int[] { 1 };

        public boolean[] bools = new boolean[] { true };
    }

    static class UnwrapShortArray {
        @JsonFormat(with={ JsonFormat.Feature.WRITE_SINGLE_ELEM_ARRAYS_UNWRAPPED })
        public short[] v = { (short) 7 };
    }

    static class UnwrapIntArray {
        @JsonFormat(with={ JsonFormat.Feature.WRITE_SINGLE_ELEM_ARRAYS_UNWRAPPED })
        public int[] v = { 3 };
    }

    static class UnwrapLongArray {
        @JsonFormat(with={ JsonFormat.Feature.WRITE_SINGLE_ELEM_ARRAYS_UNWRAPPED })
        public long[] v = { 1L };
    }

    static class UnwrapBooleanArray {
        @JsonFormat(with={ JsonFormat.Feature.WRITE_SINGLE_ELEM_ARRAYS_UNWRAPPED })
        public boolean[] v = { true };
    }

    static class UnwrapFloatArray {
        @JsonFormat(with={ JsonFormat.Feature.WRITE_SINGLE_ELEM_ARRAYS_UNWRAPPED })
        public float[] v = { 0.5f };
    }

    static class UnwrapDoubleArray {
        @JsonFormat(with={ JsonFormat.Feature.WRITE_SINGLE_ELEM_ARRAYS_UNWRAPPED })
        public double[] v = { 0.25 };
    }

    static class UnwrapIterable {
        @JsonFormat(with={ JsonFormat.Feature.WRITE_SINGLE_ELEM_ARRAYS_UNWRAPPED })
        @JsonSerialize(as=Iterable.class)
        public Iterable<String> v;

        public UnwrapIterable() {
            v = Collections.singletonList("foo");
        }

        public UnwrapIterable(String... values) {
            v = Arrays.asList(values);
        }
    }

    static class UnwrapCollection {
        @JsonFormat(with={ JsonFormat.Feature.WRITE_SINGLE_ELEM_ARRAYS_UNWRAPPED })
        @JsonSerialize(as=Collection.class)
        public Collection<String> v;

        public UnwrapCollection() {
            v = Collections.singletonList("foo");
        }

        public UnwrapCollection(String... values) {
            v = new LinkedHashSet<String>(Arrays.asList(values));
        }
    }
    
    static class UnwrapStringLike {
        @JsonFormat(with={ JsonFormat.Feature.WRITE_SINGLE_ELEM_ARRAYS_UNWRAPPED })
        public URI[] v = { URI.create("http://foo") };
    }
    
    @JsonPropertyOrder( { "strings", "ints", "bools", "enums" })
    static class WrapWriteWithCollections
    {
        @JsonFormat(with={ JsonFormat.Feature.WRITE_SINGLE_ELEM_ARRAYS_UNWRAPPED })
        public List<String> strings = Arrays.asList("a");

        @JsonFormat(without={ JsonFormat.Feature.WRITE_SINGLE_ELEM_ARRAYS_UNWRAPPED })
        public Collection<Integer> ints = Arrays.asList(Integer.valueOf(1));

        public Set<Boolean> bools = Collections.singleton(true);

        @JsonFormat(with={ JsonFormat.Feature.WRITE_SINGLE_ELEM_ARRAYS_UNWRAPPED })
        public EnumSet<ABC> enums = EnumSet.of(ABC.B);
    }

    static class StringArrayWrapper {
        @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
        public String[] values;
    }

    static class StringArrayNotAnnoted {
        public String[] values;

        protected StringArrayNotAnnoted() { }
        public StringArrayNotAnnoted(String ... v) { values = v; }
    }
    
    static class IntArrayWrapper {
        @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
        public int[] values;
    }

    static class LongArrayWrapper {
        @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
        public long[] values;
    }

    static class BooleanArrayWrapper {
        @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
        public boolean[] values;
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

    static class CaseInsensitiveRoleWrapper
    {
        @JsonFormat(with={ JsonFormat.Feature.ACCEPT_CASE_INSENSITIVE_PROPERTIES })
        public Role role;
    }

    static class SortedKeysMap {
        @JsonFormat(with = JsonFormat.Feature.WRITE_SORTED_MAP_ENTRIES)
        public Map<String,Integer> values = new LinkedHashMap<>();

        protected SortedKeysMap() { }

        public SortedKeysMap put(String key, int value) {
            values.put(key, value);
            return this;
        }
    }

    /*
    /**********************************************************
    /* Test methods, writing with single-element unwrapping
    /**********************************************************
     */
    
    private final ObjectMapper MAPPER = new ObjectMapper();

    public void testWithArrayTypes() throws Exception
    {
        // default: strings unwrapped, ints wrapped
        assertEquals(aposToQuotes("{'strings':'a','ints':[1],'bools':[true]}"),
                MAPPER.writeValueAsString(new WrapWriteWithArrays()));

        // change global default to "yes, unwrap"; changes 'bools' only
        assertEquals(aposToQuotes("{'strings':'a','ints':[1],'bools':true}"),
                MAPPER.writer().with(SerializationFeature.WRITE_SINGLE_ELEM_ARRAYS_UNWRAPPED)
                .writeValueAsString(new WrapWriteWithArrays()));

        // change global default to "no, don't, unwrap", same as first case
        assertEquals(aposToQuotes("{'strings':'a','ints':[1],'bools':[true]}"),
                MAPPER.writer().without(SerializationFeature.WRITE_SINGLE_ELEM_ARRAYS_UNWRAPPED)
                .writeValueAsString(new WrapWriteWithArrays()));

        // And then without SerializationFeature but with config override:
        ObjectMapper mapper = new ObjectMapper();
        mapper.configOverride(String[].class).setFormat(JsonFormat.Value.empty()
                .withFeature(JsonFormat.Feature.WRITE_SINGLE_ELEM_ARRAYS_UNWRAPPED));
        assertEquals(aposToQuotes("{'values':'a'}"),
                mapper.writeValueAsString(new StringArrayNotAnnoted("a")));
    }

    public void testWithCollectionTypes() throws Exception
    {
        // default: strings unwrapped, ints wrapped
        assertEquals(aposToQuotes("{'strings':'a','ints':[1],'bools':[true],'enums':'B'}"),
                MAPPER.writeValueAsString(new WrapWriteWithCollections()));

        // change global default to "yes, unwrap"; changes 'bools' only
        assertEquals(aposToQuotes("{'strings':'a','ints':[1],'bools':true,'enums':'B'}"),
                MAPPER.writer().with(SerializationFeature.WRITE_SINGLE_ELEM_ARRAYS_UNWRAPPED)
                .writeValueAsString(new WrapWriteWithCollections()));

        // change global default to "no, don't, unwrap", same as first case
        assertEquals(aposToQuotes("{'strings':'a','ints':[1],'bools':[true],'enums':'B'}"),
                MAPPER.writer().without(SerializationFeature.WRITE_SINGLE_ELEM_ARRAYS_UNWRAPPED)
                .writeValueAsString(new WrapWriteWithCollections()));
    }

    public void testUnwrapWithPrimitiveArraysEtc() throws Exception {
        assertEquals("{\"v\":7}", MAPPER.writeValueAsString(new UnwrapShortArray()));
        assertEquals("{\"v\":3}", MAPPER.writeValueAsString(new UnwrapIntArray()));
        assertEquals("{\"v\":1}", MAPPER.writeValueAsString(new UnwrapLongArray()));
        assertEquals("{\"v\":true}", MAPPER.writeValueAsString(new UnwrapBooleanArray()));

        assertEquals("{\"v\":0.5}", MAPPER.writeValueAsString(new UnwrapFloatArray()));
        assertEquals("{\"v\":0.25}", MAPPER.writeValueAsString(new UnwrapDoubleArray()));
        assertEquals("0.5",
                MAPPER.writer().with(SerializationFeature.WRITE_SINGLE_ELEM_ARRAYS_UNWRAPPED)
                .writeValueAsString(new double[] { 0.5 }));

        assertEquals("{\"v\":\"foo\"}", MAPPER.writeValueAsString(new UnwrapIterable()));
        assertEquals("{\"v\":\"x\"}", MAPPER.writeValueAsString(new UnwrapIterable("x")));
        assertEquals("{\"v\":[\"x\",null]}", MAPPER.writeValueAsString(new UnwrapIterable("x", null)));

        assertEquals("{\"v\":\"foo\"}", MAPPER.writeValueAsString(new UnwrapCollection()));
        assertEquals("{\"v\":\"x\"}", MAPPER.writeValueAsString(new UnwrapCollection("x")));
        assertEquals("{\"v\":[\"x\",null]}", MAPPER.writeValueAsString(new UnwrapCollection("x", null)));

        assertEquals("{\"v\":\"http://foo\"}", MAPPER.writeValueAsString(new UnwrapStringLike()));
    }

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

    // [databind#1232]: allow per-property case-insensitivity
    public void testCaseInsensitive() throws Exception {
        CaseInsensitiveRoleWrapper w = MAPPER.readValue
                (aposToQuotes("{'role':{'id':'12','name':'Foo'}}"),
                        CaseInsensitiveRoleWrapper.class);
        assertNotNull(w);
        assertEquals("12", w.role.ID);
        assertEquals("Foo", w.role.Name);
    }

    // [databind#1232]: allow forcing sorting on Map keys
    public void testOrderedMaps() throws Exception {
        SortedKeysMap map = new SortedKeysMap()
            .put("b", 2)
            .put("a", 1);
        assertEquals(aposToQuotes("{'values':{'a':1,'b':2}}"),
                MAPPER.writeValueAsString(map));
    }
}
