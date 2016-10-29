package com.fasterxml.jackson.databind.deser;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Assert;

import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.OptBoolean;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.exc.InvalidDefinitionException;

/**
 * Tests to make sure that the new "merging" property of
 * <code>JsonSetter</code> annotation works as expected.
 * 
 * @since 2.9
 */
@SuppressWarnings("serial")
public class PropertyMergeTest extends BaseMapTest
{
    static class Config {
        @JsonSetter(merge=OptBoolean.TRUE)
        public AB loc = new AB(1, 2);
    }

    static class NonMergeConfig {
        public AB loc = new AB(1, 2);
    }

    // another variant where all we got is a getter
    static class NoSetterConfig {
        AB _value = new AB(1, 2);
 
        @JsonSetter(merge=OptBoolean.TRUE)
        public AB getValue() { return _value; }
    }

    static class AB {
        public int a;
        public int b;

        protected AB() { }
        public AB(int a0, int b0) {
            a = a0;
            b = b0;
        }
    }

    static class CollectionWrapper {
        @JsonSetter(merge=OptBoolean.TRUE)
        public Collection<String> bag = new TreeSet<String>();
        {
            bag.add("a");
        }
    }

    // Custom type that would be deserializable by default
    static class StringReference extends AtomicReference<String> {
        public StringReference(String str) {
            set(str);
        }
    }

    static class MergedMap
    {
        @JsonSetter(merge=OptBoolean.TRUE)
        public Map<String,String> values = new LinkedHashMap<>();
        {
            values.put("a", "x");
        }
    }

    static class MergedList
    {
        @JsonSetter(merge=OptBoolean.TRUE)
        public List<String> values = new ArrayList<>();
        {
            values.add("a");
        }
    }

    static class MergedEnumSet
    {
        @JsonSetter(merge=OptBoolean.TRUE)
        public EnumSet<ABC> abc = EnumSet.of(ABC.B);
    }

    static class MergedReference
    {
        @JsonSetter(merge=OptBoolean.TRUE)
        public StringReference value = new StringReference("default");
    }

    static class MergedX<T>
    {
        @JsonSetter(merge=OptBoolean.TRUE)
        public T value;

        public MergedX(T v) { value = v; }
        protected MergedX() { }
    }
    
    // // // Classes with invalid merge definition(s)

    static class CantMergeInts {
        @JsonSetter(merge=OptBoolean.TRUE)
        public int value;
    }

    /*
    /********************************************************
    /* Test methods, POJO merging
    /********************************************************
     */

    private final ObjectMapper MAPPER = new ObjectMapper()
            // 26-Oct-2016, tatu: Make sure we'll report merge problems by default
            .disable(MapperFeature.IGNORE_MERGE_FOR_UNMERGEABLE)
    ;

    public void testBeanMergingViaProp() throws Exception
    {
        Config config = MAPPER.readValue(aposToQuotes("{'loc':{'b':3}}"), Config.class);
        assertEquals(1, config.loc.a);
        assertEquals(3, config.loc.b);
    }

    public void testBeanMergingViaType() throws Exception
    {
        // by default, no merging
        NonMergeConfig config = MAPPER.readValue(aposToQuotes("{'loc':{'a':3}}"), NonMergeConfig.class);
        assertEquals(3, config.loc.a);
        assertEquals(0, config.loc.b); // not passed, nor merge from original

        // but with type-overrides
        ObjectMapper mapper = new ObjectMapper();
        mapper.configOverride(AB.class).setSetterInfo(
                JsonSetter.Value.forMerging());
        config = mapper.readValue(aposToQuotes("{'loc':{'a':3}}"), NonMergeConfig.class);
        assertEquals(3, config.loc.a);
        assertEquals(2, config.loc.b); // original, merged
    }

    public void testBeanMergingViaGlobal() throws Exception
    {
        // but with type-overrides
        ObjectMapper mapper = new ObjectMapper()
                .setDefaultSetterInfo(JsonSetter.Value.forMerging());
        NonMergeConfig config = mapper.readValue(aposToQuotes("{'loc':{'a':3}}"), NonMergeConfig.class);
        assertEquals(3, config.loc.a);
        assertEquals(2, config.loc.b); // original, merged

        // also, test with bigger POJO type; just as smoke test
        FiveMinuteUser user0 = new FiveMinuteUser("Bob", "Bush", true, FiveMinuteUser.Gender.MALE,
                new byte[] { 1, 2, 3, 4, 5 });
        FiveMinuteUser user = mapper.readerFor(FiveMinuteUser.class)
                .withValueToUpdate(user0)
                .readValue(aposToQuotes("{'name':{'last':'Brown'}}"));
        assertEquals("Bob", user.getName().getFirst());
        assertEquals("Brown", user.getName().getLast());
    }

    // should even work with no setter
    public void testBeanMergingWithoutSetter() throws Exception
    {
        NoSetterConfig config = MAPPER.readValue(aposToQuotes("{'value':{'b':99}}"),
                NoSetterConfig.class);
        assertEquals(99, config._value.b);
        assertEquals(1, config._value.a);
    }

    /*
    /********************************************************
    /* Test methods, Collection merging
    /********************************************************
     */

    public void testCollectionMerging() throws Exception
    {
        CollectionWrapper w = MAPPER.readValue(aposToQuotes("{'bag':['b']}"), CollectionWrapper.class);
        assertEquals(2, w.bag.size());
        assertTrue(w.bag.contains("a"));
        assertTrue(w.bag.contains("b"));
    }

    public void testListMerging() throws Exception
    {
        MergedList w = MAPPER.readValue(aposToQuotes("{'values':['x']}"), MergedList.class);
        assertEquals(2, w.values.size());
        assertTrue(w.values.contains("a"));
        assertTrue(w.values.contains("x"));
    }

    // Test that uses generic type
    public void testGenericListMerging() throws Exception
    {
        Collection<String> l = new ArrayList<>();
        l.add("foo");
        MergedX<Collection<String>> input = new MergedX<Collection<String>>(l);

        MergedX<Collection<String>> result = MAPPER
                .readerFor(new TypeReference<MergedX<Collection<String>>>() {})
                .withValueToUpdate(input)
                .readValue(aposToQuotes("{'value':['bar']}"));
        assertSame(input, result);
        assertEquals(2, result.value.size());
        Iterator<String> it = result.value.iterator();
        assertEquals("foo", it.next());
        assertEquals("bar", it.next());
    }

    public void testEnumSetMerging() throws Exception
    {
        MergedEnumSet result = MAPPER.readValue(aposToQuotes("{'abc':['A']}"), MergedEnumSet.class);
        assertEquals(2, result.abc.size());
        assertTrue(result.abc.contains(ABC.B)); // original
        assertTrue(result.abc.contains(ABC.A)); // added
    }

    /*
    /********************************************************
    /* Test methods, Map merging
    /********************************************************
     */

    public void testMapMerging() throws Exception
    {
        MergedMap v = MAPPER.readValue(aposToQuotes("{'values':{'c':'y'}}"), MergedMap.class);
        assertEquals(2, v.values.size());
        assertEquals("y", v.values.get("c"));
        assertEquals("x", v.values.get("a"));
    }

    /*
    /********************************************************
    /* Test methods, array merging
    /********************************************************
     */

    public void testObjectArrayMerging() throws Exception
    {
        MergedX<Object[]> input = new MergedX<Object[]>(new Object[] {
                "foo"
        });
        final JavaType type = MAPPER.getTypeFactory().constructType(new TypeReference<MergedX<Object[]>>() {});
        MergedX<Object[]> result = MAPPER.readerFor(type)
                .withValueToUpdate(input)
                .readValue(aposToQuotes("{'value':['bar']}"));
        assertSame(input, result);
        assertEquals(2, result.value.length);
        assertEquals("foo", result.value[0]);
        assertEquals("bar", result.value[1]);

        // and with one trick
        result = MAPPER.readerFor(type)
                .with(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
                .withValueToUpdate(input)
                .readValue(aposToQuotes("{'value':'zap'}"));
        assertSame(input, result);
        assertEquals(3, result.value.length);
        assertEquals("foo", result.value[0]);
        assertEquals("bar", result.value[1]);
        assertEquals("zap", result.value[2]);
    }

    public void testStringArrayMerging() throws Exception
    {
        MergedX<String[]> input = new MergedX<String[]>(new String[] { "foo" });
        MergedX<String[]> result = MAPPER
                .readerFor(new TypeReference<MergedX<String[]>>() {})
                .withValueToUpdate(input)
                .readValue(aposToQuotes("{'value':['bar']}"));
        assertSame(input, result);
        assertEquals(2, result.value.length);
        assertEquals("foo", result.value[0]);
        assertEquals("bar", result.value[1]);
    }

    public void testBooleanArrayMerging() throws Exception
    {
        MergedX<boolean[]> input = new MergedX<boolean[]>(new boolean[] { true, false });
        MergedX<boolean[]> result = MAPPER
                .readerFor(new TypeReference<MergedX<boolean[]>>() {})
                .withValueToUpdate(input)
                .readValue(aposToQuotes("{'value':[true]}"));
        assertSame(input, result);
        assertEquals(3, result.value.length);
        Assert.assertArrayEquals(new boolean[] { true, false, true }, result.value);
    }

    public void tesByteArrayMerging() throws Exception
    {
        MergedX<byte[]> input = new MergedX<byte[]>(new byte[] { 1, 2 });
        MergedX<byte[]> result = MAPPER
                .readerFor(new TypeReference<MergedX<byte[]>>() {})
                .withValueToUpdate(input)
                .readValue(aposToQuotes("{'value':[4, 6]}"));
        assertSame(input, result);
        assertEquals(4, result.value.length);
        Assert.assertArrayEquals(new byte[] { 1, 2, 4, 6 }, result.value);
    }

    public void testShortArrayMerging() throws Exception
    {
        MergedX<short[]> input = new MergedX<short[]>(new short[] { 1, 2 });
        MergedX<short[]> result = MAPPER
                .readerFor(new TypeReference<MergedX<short[]>>() {})
                .withValueToUpdate(input)
                .readValue(aposToQuotes("{'value':[4, 6]}"));
        assertSame(input, result);
        assertEquals(4, result.value.length);
        Assert.assertArrayEquals(new short[] { 1, 2, 4, 6 }, result.value);
    }

    public void testCharArrayMerging() throws Exception
    {
        MergedX<char[]> input = new MergedX<char[]>(new char[] { 'a', 'b' });
        MergedX<char[]> result = MAPPER
                .readerFor(new TypeReference<MergedX<char[]>>() {})
                .withValueToUpdate(input)
                .readValue(aposToQuotes("{'value':['c']}"));
        assertSame(input, result);
        Assert.assertArrayEquals(new char[] { 'a', 'b', 'c' }, result.value);

        // also some variation
        input = new MergedX<char[]>(new char[] { });
        result = MAPPER
                .readerFor(new TypeReference<MergedX<char[]>>() {})
                .withValueToUpdate(input)
                .readValue(aposToQuotes("{'value':['c']}"));
        assertSame(input, result);
        Assert.assertArrayEquals(new char[] { 'c' }, result.value);
    }
    
    public void testIntArrayMerging() throws Exception
    {
        MergedX<int[]> input = new MergedX<int[]>(new int[] { 1, 2 });
        MergedX<int[]> result = MAPPER
                .readerFor(new TypeReference<MergedX<int[]>>() {})
                .withValueToUpdate(input)
                .readValue(aposToQuotes("{'value':[4, 6]}"));
        assertSame(input, result);
        assertEquals(4, result.value.length);
        Assert.assertArrayEquals(new int[] { 1, 2, 4, 6 }, result.value);

        // also some variation
        input = new MergedX<int[]>(new int[] { 3, 4, 6 });
        result = MAPPER
                .readerFor(new TypeReference<MergedX<int[]>>() {})
                .withValueToUpdate(input)
                .readValue(aposToQuotes("{'value':[ ]}"));
        assertSame(input, result);
        Assert.assertArrayEquals(new int[] { 3, 4, 6 }, result.value);
    }

    public void testLongArrayMerging() throws Exception
    {
        MergedX<long[]> input = new MergedX<long[]>(new long[] { 1, 2 });
        MergedX<long[]> result = MAPPER
                .readerFor(new TypeReference<MergedX<long[]>>() {})
                .withValueToUpdate(input)
                .readValue(aposToQuotes("{'value':[4, 6]}"));
        assertSame(input, result);
        assertEquals(4, result.value.length);
        Assert.assertArrayEquals(new long[] { 1, 2, 4, 6 }, result.value);
    }
    
    /*
    /********************************************************
    /* Test methods, reference types
    /********************************************************
     */

    public void testReferenceMerging() throws Exception
    {
        MergedReference result = MAPPER.readValue(aposToQuotes("{'value':'override'}"),
                MergedReference.class);
        assertEquals("override", result.value.get());
    }

    /*
    /********************************************************
    /* Test methods, failure checking
    /********************************************************
     */

    public void testInvalidPropertyMerge() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper()
                .disable(MapperFeature.IGNORE_MERGE_FOR_UNMERGEABLE);
        
        try {
            mapper.readValue("{\"value\":3}", CantMergeInts.class);
            fail("Should not pass");
        } catch (InvalidDefinitionException e) {
            verifyException(e, "can not be merged");
        }
    }
}
