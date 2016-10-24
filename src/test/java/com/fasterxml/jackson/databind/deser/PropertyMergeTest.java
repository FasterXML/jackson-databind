package com.fasterxml.jackson.databind.deser;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.OptBoolean;
import com.fasterxml.jackson.databind.*;

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
        public int a, b;

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

    /*
    /********************************************************
    /* Test methods, POJO merging
    /********************************************************
     */

    private final ObjectMapper MAPPER = new ObjectMapper();

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
                // 23-Oct-2016, tatu: should work either way, but leave disabled
                //   to ensure handling by scalar types
                .disable(MapperFeature.IGNORE_MERGE_FOR_UNMERGEABLE)
                .setDefaultSetterInfo(JsonSetter.Value.forMerging());
            NonMergeConfig config = mapper.readValue(aposToQuotes("{'loc':{'a':3}}"), NonMergeConfig.class);
        assertEquals(3, config.loc.a);
        assertEquals(2, config.loc.b); // original, merged
    }

    // should even work with no setter
    /*
    public void testBeanMergingWithoutSetter() throws Exception
    {
        NoSetterConfig config = MAPPER.readValue(aposToQuotes("{'value':{'b':99}}"),
                NoSetterConfig.class);
        assertEquals(99, config._value.b);
        assertEquals(1, config._value.a);
    }
    */

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
    /* Test methods, reference types
    /********************************************************
     */
    
    public void testReferenceMerging() throws Exception
    {
        MergedReference result = MAPPER.readValue(aposToQuotes("{'value':'override'}"),
                MergedReference.class);
        assertEquals("override", result.value.get());
    }
}
