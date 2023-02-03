package com.fasterxml.jackson.databind.deser.filter;

import java.util.*;

import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.exc.InvalidNullException;

// For [databind#1402]; configurable null handling, for contents of
// Collections, Maps, arrays
public class NullConversionsForContentTest extends BaseMapTest
{
    static class NullContentFail<T> {
        public T nullsOk;

        @JsonSetter(contentNulls=Nulls.FAIL)
        public T noNulls;
    }

    static class NullContentAsEmpty<T> {
        @JsonSetter(contentNulls=Nulls.AS_EMPTY)
        public T values;
    }

    static class NullContentSkip<T> {
        @JsonSetter(contentNulls=Nulls.SKIP)
        public T values;
    }

    static class NullContentUndefined<T> {
        @JsonSetter // leave with defaults
        public T values;
    }

    /*
    /**********************************************************
    /* Test methods, fail-on-null
    /**********************************************************
     */

    private final ObjectMapper MAPPER = newJsonMapper();

    // Tests to verify that we can set default settings for failure
    public void testFailOnNullFromDefaults() throws Exception
    {
        final String JSON = a2q("{'values':[null]}");
        TypeReference<NullContentUndefined<List<String>>> listType = new TypeReference<NullContentUndefined<List<String>>>() { };

        // by default fine to get nulls
        NullContentUndefined<List<String>> result = MAPPER.readValue(JSON, listType);
        assertNotNull(result.values);
        assertEquals(1, result.values.size());
        assertNull(result.values.get(0));

        // but not when overridden globally:
        ObjectMapper mapper = newJsonMapper();
        mapper.setDefaultSetterInfo(JsonSetter.Value.forContentNulls(Nulls.FAIL));
        try {
            mapper.readValue(JSON, listType);
            fail("Should not pass");
        } catch (InvalidNullException e) {
            verifyException(e, "property \"values\"");
            assertEquals(String.class, e.getTargetType());
        }

        // or configured for type:
        mapper = newJsonMapper();
        mapper.configOverride(List.class)
                .setSetterInfo(JsonSetter.Value.forContentNulls(Nulls.FAIL));
        try {
            mapper.readValue(JSON, listType);
            fail("Should not pass");
        } catch (InvalidNullException e) {
            verifyException(e, "property \"values\"");
            assertEquals(String.class, e.getTargetType());
        }
    }

    public void testFailOnNullWithCollections() throws Exception
    {
        TypeReference<NullContentFail<List<Integer>>> typeRef = new TypeReference<NullContentFail<List<Integer>>>() { };

        // first, ok if assigning non-null to not-nullable, null for nullable
        NullContentFail<List<Integer>> result = MAPPER.readValue(a2q("{'nullsOk':[null]}"),
                typeRef);
        assertNotNull(result.nullsOk);
        assertEquals(1, result.nullsOk.size());
        assertNull(result.nullsOk.get(0));

        // and then see that nulls are not ok for non-nullable.

        // List<Integer>
        final String JSON = a2q("{'noNulls':[null]}");
        try {
            MAPPER.readValue(JSON, typeRef);
            fail("Should not pass");
        } catch (InvalidNullException e) {
            verifyException(e, "property \"noNulls\"");
            assertEquals(Integer.class, e.getTargetType());
        }

        // List<String>
        try {
            MAPPER.readValue(JSON, new TypeReference<NullContentFail<List<String>>>() { });
            fail("Should not pass");
        } catch (InvalidNullException e) {
            verifyException(e, "property \"noNulls\"");
            assertEquals(String.class, e.getTargetType());
        }
    }

    public void testFailOnNullWithArrays() throws Exception
    {
        final String JSON = a2q("{'noNulls':[null]}");
        // Object[]
        try {
            MAPPER.readValue(JSON, new TypeReference<NullContentFail<Object[]>>() { });
            fail("Should not pass");
        } catch (InvalidNullException e) {
            verifyException(e, "property \"noNulls\"");
            assertEquals(Object.class, e.getTargetType());
        }

        // String[]
        try {
            MAPPER.readValue(JSON, new TypeReference<NullContentFail<String[]>>() { });
            fail("Should not pass");
        } catch (InvalidNullException e) {
            verifyException(e, "property \"noNulls\"");
            assertEquals(String.class, e.getTargetType());
        }
    }

    public void testFailOnNullWithPrimitiveArrays() throws Exception
    {
        final String JSON = a2q("{'noNulls':[null]}");

        // boolean[]
        try {
            MAPPER.readValue(JSON, new TypeReference<NullContentFail<boolean[]>>() { });
            fail("Should not pass");
        } catch (InvalidNullException e) {
            verifyException(e, "property \"noNulls\"");
            assertEquals(Boolean.TYPE, e.getTargetType());
        }
        // int[]
        try {
            MAPPER.readValue(JSON, new TypeReference<NullContentFail<int[]>>() { });
            fail("Should not pass");
        } catch (InvalidNullException e) {
            verifyException(e, "property \"noNulls\"");
            assertEquals(Integer.TYPE, e.getTargetType());
        }
        // double[]
        try {
            MAPPER.readValue(JSON, new TypeReference<NullContentFail<double[]>>() { });
            fail("Should not pass");
        } catch (InvalidNullException e) {
            verifyException(e, "property \"noNulls\"");
            assertEquals(Double.TYPE, e.getTargetType());
        }
    }

    public void testFailOnNullWithMaps() throws Exception
    {
        // Then: Map<String,String>
        try {
            final String MAP_JSON = a2q("{'noNulls':{'a':null}}");
            MAPPER.readValue(MAP_JSON, new TypeReference<NullContentFail<Map<String,String>>>() { });
            fail("Should not pass");
        } catch (InvalidNullException e) {
            verifyException(e, "property \"noNulls\"");
            assertEquals(String.class, e.getTargetType());
        }

        // Then: EnumMap<Enum,String>
        try {
            final String MAP_JSON = a2q("{'noNulls':{'A':null}}");
            MAPPER.readValue(MAP_JSON, new TypeReference<NullContentFail<EnumMap<ABC,String>>>() { });
            fail("Should not pass");
        } catch (InvalidNullException e) {
            verifyException(e, "property \"noNulls\"");
            assertEquals(String.class, e.getTargetType());
        }
    }

    /*
    /**********************************************************
    /* Test methods, null-as-empty
    /**********************************************************
     */

    public void testNullsAsEmptyWithCollections() throws Exception
    {
        final String JSON = a2q("{'values':[null]}");

        // List<Integer>
        {
            NullContentAsEmpty<List<Integer>> result = MAPPER.readValue(JSON,
                    new TypeReference<NullContentAsEmpty<List<Integer>>>() { });
            assertEquals(1, result.values.size());
            assertEquals(Integer.valueOf(0), result.values.get(0));
        }

        // List<String>
        {
            NullContentAsEmpty<List<String>> result = MAPPER.readValue(JSON,
                    new TypeReference<NullContentAsEmpty<List<String>>>() { });
            assertEquals(1, result.values.size());
            assertEquals("", result.values.get(0));
        }
    }

    public void testNullsAsEmptyUsingDefaults() throws Exception
    {
        final String JSON = a2q("{'values':[null]}");
        TypeReference<NullContentUndefined<List<Integer>>> listType = new TypeReference<NullContentUndefined<List<Integer>>>() { };

        // Let's see defaulting in action
        ObjectMapper mapper = newJsonMapper();
        mapper.setDefaultSetterInfo(JsonSetter.Value.forContentNulls(Nulls.AS_EMPTY));
        NullContentUndefined<List<Integer>> result = mapper.readValue(JSON, listType);
        assertEquals(1, result.values.size());
        assertEquals(Integer.valueOf(0), result.values.get(0));

        // or configured for type:
        mapper = newJsonMapper();
        mapper.configOverride(List.class)
                .setSetterInfo(JsonSetter.Value.forContentNulls(Nulls.AS_EMPTY));
        result = mapper.readValue(JSON, listType);
        assertEquals(1, result.values.size());
        assertEquals(Integer.valueOf(0), result.values.get(0));
    }

    public void testNullsAsEmptyWithArrays() throws Exception
    {
        // Note: skip `Object[]`, no default empty value at this point
        final String JSON = a2q("{'values':[null]}");

        // Then: String[]
        {
            NullContentAsEmpty<String[]> result = MAPPER.readValue(JSON,
                    new TypeReference<NullContentAsEmpty<String[]>>() { });
            assertEquals(1, result.values.length);
            assertEquals("", result.values[0]);
        }
    }

    public void testNullsAsEmptyWithPrimitiveArrays() throws Exception
    {
        final String JSON = a2q("{'values':[null]}");

        // int[]
        {
            NullContentAsEmpty<int[]> result = MAPPER.readValue(JSON,
                    new TypeReference<NullContentAsEmpty<int[]>>() { });
            assertEquals(1, result.values.length);
            assertEquals(0, result.values[0]);
        }

        // long[]
        {
            NullContentAsEmpty<long[]> result = MAPPER.readValue(JSON,
                    new TypeReference<NullContentAsEmpty<long[]>>() { });
            assertEquals(1, result.values.length);
            assertEquals(0L, result.values[0]);
        }

        // boolean[]
        {
            NullContentAsEmpty<boolean[]> result = MAPPER.readValue(JSON,
                    new TypeReference<NullContentAsEmpty<boolean[]>>() { });
            assertEquals(1, result.values.length);
            assertEquals(false, result.values[0]);
        }
}

    public void testNullsAsEmptyWithMaps() throws Exception
    {
        // Then: Map<String,String>
        final String MAP_JSON = a2q("{'values':{'A':null}}");
        {
            NullContentAsEmpty<Map<String,String>> result
                = MAPPER.readValue(MAP_JSON, new TypeReference<NullContentAsEmpty<Map<String,String>>>() { });
            assertEquals(1, result.values.size());
            assertEquals("A", result.values.entrySet().iterator().next().getKey());
            assertEquals("", result.values.entrySet().iterator().next().getValue());
        }

        // Then: EnumMap<Enum,String>
        {
            NullContentAsEmpty<EnumMap<ABC,String>> result
                = MAPPER.readValue(MAP_JSON, new TypeReference<NullContentAsEmpty<EnumMap<ABC,String>>>() { });
            assertEquals(1, result.values.size());
            assertEquals(ABC.A, result.values.entrySet().iterator().next().getKey());
            assertEquals("", result.values.entrySet().iterator().next().getValue());
        }
    }

    /*
    /**********************************************************
    /* Test methods, skip-nulls
    /**********************************************************
     */

    public void testNullsSkipUsingDefaults() throws Exception
    {
        final String JSON = a2q("{'values':[null]}");
        TypeReference<NullContentUndefined<List<Long>>> listType = new TypeReference<NullContentUndefined<List<Long>>>() { };

        // Let's see defaulting in action
        ObjectMapper mapper = newJsonMapper();
        mapper.setDefaultSetterInfo(JsonSetter.Value.forContentNulls(Nulls.SKIP));
        NullContentUndefined<List<Long>> result = mapper.readValue(JSON, listType);
        assertEquals(0, result.values.size());

        // or configured for type:
        mapper = newJsonMapper();
        mapper.configOverride(List.class)
                .setSetterInfo(JsonSetter.Value.forContentNulls(Nulls.SKIP));
        result = mapper.readValue(JSON, listType);
        assertEquals(0, result.values.size());
    }

    // Test to verify that per-property setting overrides defaults:
    public void testNullsSkipWithOverrides() throws Exception
    {
        final String JSON = a2q("{'values':[null]}");
        TypeReference<NullContentSkip<List<Long>>> listType = new TypeReference<NullContentSkip<List<Long>>>() { };

        ObjectMapper mapper = newJsonMapper();
        // defaults call for fail; but POJO specifies "skip"; latter should win
        mapper.setDefaultSetterInfo(JsonSetter.Value.forContentNulls(Nulls.FAIL));
        NullContentSkip<List<Long>> result = mapper.readValue(JSON, listType);
        assertEquals(0, result.values.size());

        // ditto for per-type defaults
        mapper = newJsonMapper();
        mapper.configOverride(List.class)
                .setSetterInfo(JsonSetter.Value.forContentNulls(Nulls.FAIL));
        result = mapper.readValue(JSON, listType);
        assertEquals(0, result.values.size());
    }

    public void testNullsSkipWithCollections() throws Exception
    {
        // List<Integer>
        {
            final String JSON = a2q("{'values':[1,null,2]}");
            NullContentSkip<List<Integer>> result = MAPPER.readValue(JSON,
                    new TypeReference<NullContentSkip<List<Integer>>>() { });
            assertEquals(2, result.values.size());
            assertEquals(Integer.valueOf(1), result.values.get(0));
            assertEquals(Integer.valueOf(2), result.values.get(1));
        }

        // List<String>
        {
            final String JSON = a2q("{'values':['ab',null,'xy']}");
            NullContentSkip<List<String>> result = MAPPER.readValue(JSON,
                    new TypeReference<NullContentSkip<List<String>>>() { });
            assertEquals(2, result.values.size());
            assertEquals("ab", result.values.get(0));
            assertEquals("xy", result.values.get(1));
        }
    }

    public void testNullsSkipWithArrays() throws Exception
    {
        final String JSON = a2q("{'values':['a',null,'xy']}");
        // Object[]
        {
            NullContentSkip<Object[]> result = MAPPER.readValue(JSON,
                    new TypeReference<NullContentSkip<Object[]>>() { });
            assertEquals(2, result.values.length);
            assertEquals("a", result.values[0]);
            assertEquals("xy", result.values[1]);
        }
        // String[]
        {
            NullContentSkip<String[]> result = MAPPER.readValue(JSON,
                    new TypeReference<NullContentSkip<String[]>>() { });
            assertEquals(2, result.values.length);
            assertEquals("a", result.values[0]);
            assertEquals("xy", result.values[1]);
        }
    }

    public void testNullsSkipWithPrimitiveArrays() throws Exception
    {
        // int[]
        {
            final String JSON = a2q("{'values':[3,null,7]}");
            NullContentSkip<int[]> result = MAPPER.readValue(JSON,
                    new TypeReference<NullContentSkip<int[]>>() { });
            assertEquals(2, result.values.length);
            assertEquals(3, result.values[0]);
            assertEquals(7, result.values[1]);
        }

        // long[]
        {
            final String JSON = a2q("{'values':[-13,null,999]}");
            NullContentSkip<long[]> result = MAPPER.readValue(JSON,
                    new TypeReference<NullContentSkip<long[]>>() { });
            assertEquals(2, result.values.length);
            assertEquals(-13L, result.values[0]);
            assertEquals(999L, result.values[1]);
        }

        // boolean[]
        {
            final String JSON = a2q("{'values':[true,null,true]}");
            NullContentSkip<boolean[]> result = MAPPER.readValue(JSON,
                    new TypeReference<NullContentSkip<boolean[]>>() { });
            assertEquals(2, result.values.length);
            assertEquals(true, result.values[0]);
            assertEquals(true, result.values[1]);
        }
    }

    public void testNullsSkipWithMaps() throws Exception
    {
        // Then: Map<String,String>
        final String MAP_JSON = a2q("{'values':{'A':'foo','B':null,'C':'bar'}}");
        {
            NullContentSkip<Map<String,String>> result
                = MAPPER.readValue(MAP_JSON, new TypeReference<NullContentSkip<Map<String,String>>>() { });
            assertEquals(2, result.values.size());
            assertEquals("foo", result.values.get("A"));
            assertEquals("bar", result.values.get("C"));
        }

        // Then: EnumMap<Enum,String>
        {
            NullContentSkip<EnumMap<ABC,String>> result
                = MAPPER.readValue(MAP_JSON, new TypeReference<NullContentSkip<EnumMap<ABC,String>>>() { });
            assertEquals(2, result.values.size());
            assertEquals("foo", result.values.get(ABC.A));
            assertEquals("bar", result.values.get(ABC.C));
        }
    }
}
