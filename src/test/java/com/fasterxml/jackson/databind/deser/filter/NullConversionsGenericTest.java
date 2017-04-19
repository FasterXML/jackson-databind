package com.fasterxml.jackson.databind.deser.filter;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import com.fasterxml.jackson.core.type.TypeReference;

import com.fasterxml.jackson.databind.*;

// for [databind#1402]; configurable null handling, for values themselves,
// using generic types
public class NullConversionsGenericTest extends BaseMapTest
{
    static class GeneralEmpty<T> {
        // 09-Feb-2017, tatu: Should only need annotation either for field OR setter, not both:
//        @JsonSetter(nulls=JsonSetter.Nulls.AS_EMPTY)
        T value;

        @JsonSetter(nulls=Nulls.AS_EMPTY)
        public void setValue(T v) {
            value = v;
        }
    }

    static class NoCtorWrapper {
        @JsonSetter(nulls=Nulls.AS_EMPTY)
        public NoCtorPOJO value;
    }

    static class NoCtorPOJO {
        public NoCtorPOJO(boolean b) { }
    }

    /*
    /**********************************************************
    /* Test methods
    /**********************************************************
     */

    private final ObjectMapper MAPPER = newObjectMapper();

    public void testNullsToEmptyPojo() throws Exception
    {
        GeneralEmpty<Point> result = MAPPER.readValue(aposToQuotes("{'value':null}"),
                new TypeReference<GeneralEmpty<Point>>() { });
        assertNotNull(result.value);
        Point p = result.value;
        assertEquals(0, p.x);
        assertEquals(0, p.y);

        // and then also failing case with no suitable creator:
        try {
            /* NoCtorWrapper nogo =*/ MAPPER.readValue(aposToQuotes("{'value':null}"),
                    NoCtorWrapper.class);
            fail("Should not pass");
        } catch (JsonMappingException e) {
            verifyException(e, "Can not create empty instance");
        }
    }

    public void testNullsToEmptyCollection() throws Exception
    {
        GeneralEmpty<List<String>> result = MAPPER.readValue(aposToQuotes("{'value':null}"),
                new TypeReference<GeneralEmpty<List<String>>>() { });
        assertNotNull(result.value);
        assertEquals(0, result.value.size());

        // but also non-String type, since impls vary
        GeneralEmpty<List<Integer>> result2 = MAPPER.readValue(aposToQuotes("{'value':null}"),
                new TypeReference<GeneralEmpty<List<Integer>>>() { });
        assertNotNull(result2.value);
        assertEquals(0, result2.value.size());
    }

    public void testNullsToEmptyMap() throws Exception
    {
        GeneralEmpty<Map<String,String>> result = MAPPER.readValue(aposToQuotes("{'value':null}"),
                new TypeReference<GeneralEmpty<Map<String,String>>>() { });
        assertNotNull(result.value);
        assertEquals(0, result.value.size());
    }

    public void testNullsToEmptyArrays() throws Exception
    {
        final String json = aposToQuotes("{'value':null}");

        GeneralEmpty<Object[]> result = MAPPER.readValue(json,
                new TypeReference<GeneralEmpty<Object[]>>() { });
        assertNotNull(result.value);
        assertEquals(0, result.value.length);

        GeneralEmpty<String[]> result2 = MAPPER.readValue(json,
                new TypeReference<GeneralEmpty<String[]>>() { });
        assertNotNull(result2.value);
        assertEquals(0, result2.value.length);

        GeneralEmpty<int[]> result3 = MAPPER.readValue(json,
                new TypeReference<GeneralEmpty<int[]>>() { });
        assertNotNull(result3.value);
        assertEquals(0, result3.value.length);

        GeneralEmpty<double[]> result4 = MAPPER.readValue(json,
                new TypeReference<GeneralEmpty<double[]>>() { });
        assertNotNull(result4.value);
        assertEquals(0, result4.value.length);

        GeneralEmpty<boolean[]> result5 = MAPPER.readValue(json,
                new TypeReference<GeneralEmpty<boolean[]>>() { });
        assertNotNull(result5.value);
        assertEquals(0, result5.value.length);
    }
}
