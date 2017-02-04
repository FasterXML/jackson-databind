package com.fasterxml.jackson.databind.deser.filter;

import java.util.*;

import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.exc.InvalidNullException;

// For [databind#1402]; configurable null handling, for contents of
// Collections, Maps, arrays
public class NullConversionsForContentTest extends BaseMapTest
{
    static class NullContentFail<T> {
        public T nullsOk;

        @JsonSetter(contentNulls=JsonSetter.Nulls.FAIL)
        public T noNulls;
    }

    static class NullContentAsEmpty<T> {
        @JsonSetter(contentNulls=JsonSetter.Nulls.AS_EMPTY)
        public T values;
    }

    static class NullContentSkip<T> {
        @JsonSetter(contentNulls=JsonSetter.Nulls.SKIP)
        public T values;
    }
    
    private final ObjectMapper MAPPER = new ObjectMapper();
    
    /*
    /**********************************************************
    /* Test methods, fail-on-null
    /**********************************************************
     */

    public void testFailOnNull() throws Exception
    {
        TypeReference<?> typeRef = new TypeReference<NullContentFail<List<Integer>>>() { };

        // first, ok if assigning non-null to not-nullable, null for nullable
        NullContentFail<List<Integer>> result = MAPPER.readValue(aposToQuotes("{'nullsOk':[null]}"),
                typeRef);
        assertNotNull(result.nullsOk);
        assertEquals(1, result.nullsOk.size());
        assertNull(result.nullsOk.get(0));

        // and then see that nulls are not ok for non-nullable.
        
        // List<Integer>
        final String JSON = aposToQuotes("{'noNulls':[null]}");
        try {
            MAPPER.readValue(JSON, typeRef);
            fail("Should not pass");
        } catch (InvalidNullException e) {
            verifyException(e, "property \"noNulls\"");
        }

        // List<String>
        try {
            MAPPER.readValue(JSON, new TypeReference<NullContentFail<List<String>>>() { });
            fail("Should not pass");
        } catch (InvalidNullException e) {
            verifyException(e, "property \"noNulls\"");
        }
    }

    public void testFailOnNullWithArrays() throws Exception
    {
        final String JSON = aposToQuotes("{'noNulls':[null]}");
        // Object[]
        try {
            MAPPER.readValue(JSON, new TypeReference<NullContentFail<Object[]>>() { });
            fail("Should not pass");
        } catch (InvalidNullException e) {
            verifyException(e, "property \"noNulls\"");
        }

        // String[]
        try {
            MAPPER.readValue(JSON, new TypeReference<NullContentFail<String[]>>() { });
            fail("Should not pass");
        } catch (InvalidNullException e) {
            verifyException(e, "property \"noNulls\"");
        }
    }

    /*
    public void testFailOnNullWithPrimitiveArrays() throws Exception
    {
        final String JSON = aposToQuotes("{'noNulls':[null]}");
        // boolean[]
        try {
            MAPPER.readValue(JSON, new TypeReference<NullContentFail<boolean[]>>() { });
            fail("Should not pass");
        } catch (InvalidNullException e) {
            verifyException(e, "property \"noNulls\"");
        }

        // int[]
        try {
            MAPPER.readValue(JSON, new TypeReference<NullContentFail<int[]>>() { });
            fail("Should not pass");
        } catch (InvalidNullException e) {
            verifyException(e, "property \"noNulls\"");
        }
    }
    */

    public void testFailOnNullWithMaps() throws Exception
    {
        // Then: Map<String,String>
        try {
            final String MAP_JSON = aposToQuotes("{'noNulls':{'a':null}}");
            MAPPER.readValue(MAP_JSON, new TypeReference<NullContentFail<Map<String,String>>>() { });
            fail("Should not pass");
        } catch (InvalidNullException e) {
            verifyException(e, "property \"noNulls\"");
        }

        // Then: EnumMap<Enum,String>
        try {
            final String MAP_JSON = aposToQuotes("{'noNulls':{'A':null}}");
            MAPPER.readValue(MAP_JSON, new TypeReference<NullContentFail<EnumMap<ABC,String>>>() { });
            fail("Should not pass");
        } catch (InvalidNullException e) {
            verifyException(e, "property \"noNulls\"");
        }
    }

    /*
    /**********************************************************
    /* Test methods, null-as-empty
    /**********************************************************
     */

    public void testNullsAsEmpty() throws Exception
    {
        final String JSON = aposToQuotes("{'values':[null]}");

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

    public void testNullsAsEmptyWithArrays() throws Exception
    {
        // Note: skip `Object[]`, no default empty value at this point
        final String JSON = aposToQuotes("{'values':[null]}");

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
        final String JSON = aposToQuotes("{'values':[null]}");

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
        final String MAP_JSON = aposToQuotes("{'values':{'A':null}}");
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

    public void testNullsSkip() throws Exception
    {
        // List<Integer>
        {
            final String JSON = aposToQuotes("{'values':[1,null,2]}");
            NullContentSkip<List<Integer>> result = MAPPER.readValue(JSON,
                    new TypeReference<NullContentSkip<List<Integer>>>() { });
            assertEquals(2, result.values.size());
            assertEquals(Integer.valueOf(1), result.values.get(0));
            assertEquals(Integer.valueOf(2), result.values.get(1));
        }

        // List<String>
        {
            final String JSON = aposToQuotes("{'values':['ab',null,'xy']}");
            NullContentSkip<List<String>> result = MAPPER.readValue(JSON,
                    new TypeReference<NullContentSkip<List<String>>>() { });
            assertEquals(2, result.values.size());
            assertEquals("ab", result.values.get(0));
            assertEquals("xy", result.values.get(1));
        }
    }

    public void testNullsSkipWithArrays() throws Exception
    {
        final String JSON = aposToQuotes("{'values':['a',null,'xy']}");
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
            final String JSON = aposToQuotes("{'values':[3,null,7]}");
            NullContentSkip<int[]> result = MAPPER.readValue(JSON,
                    new TypeReference<NullContentSkip<int[]>>() { });
            assertEquals(2, result.values.length);
            assertEquals(3, result.values[0]);
            assertEquals(7, result.values[1]);
        }

        // long[]
        {
            final String JSON = aposToQuotes("{'values':[-13,null,999]}");
            NullContentSkip<long[]> result = MAPPER.readValue(JSON,
                    new TypeReference<NullContentSkip<long[]>>() { });
            assertEquals(2, result.values.length);
            assertEquals(-13L, result.values[0]);
            assertEquals(999L, result.values[1]);
        }

        // boolean[]
        {
            final String JSON = aposToQuotes("{'values':[true,null,true]}");
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
        final String MAP_JSON = aposToQuotes("{'values':{'A':'foo','B':null,'C':'bar'}}");
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
