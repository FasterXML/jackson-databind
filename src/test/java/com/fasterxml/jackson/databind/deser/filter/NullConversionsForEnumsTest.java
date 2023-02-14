package com.fasterxml.jackson.databind.deser.filter;

import java.util.EnumMap;
import java.util.EnumSet;

import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.BaseMapTest;
import com.fasterxml.jackson.databind.ObjectMapper;

public class NullConversionsForEnumsTest extends BaseMapTest
{
    static class NullValueAsEmpty<T> {
        @JsonSetter(nulls=Nulls.AS_EMPTY)
        public T value;
    }

    static class NullContentAsEmpty<T> {
        @JsonSetter(contentNulls=Nulls.AS_EMPTY)
        public T values;
    }

    static class NullContentSkip<T> {
        @JsonSetter(contentNulls=Nulls.SKIP)
        public T values;
    }

    /*
    /**********************************************************
    /* Test methods, for container values as empty
    /**********************************************************
     */

    private final ObjectMapper MAPPER = newJsonMapper();

    public void testEnumSetAsEmpty() throws Exception
    {
        NullValueAsEmpty<EnumSet<ABC>> result = MAPPER.readValue(a2q("{'value': null }"),
                new TypeReference<NullValueAsEmpty<EnumSet<ABC>>>() { });
        assertNotNull(result.value);
        assertEquals(0, result.value.size());
    }

    public void testEnumMapAsEmpty() throws Exception
    {
        NullValueAsEmpty<EnumMap<ABC, String>> result = MAPPER.readValue(a2q("{'value': null }"),
                new TypeReference<NullValueAsEmpty<EnumMap<ABC, String>>>() { });
        assertNotNull(result.value);
        assertEquals(0, result.value.size());
    }

    /*
    /**********************************************************
    /* Test methods, for container contents, null as empty
    /**********************************************************
     */

    // // NOTE: no "empty" value for Enums, so can't use with EnumSet, only EnumMap

    public void testEnumMapNullsAsEmpty() throws Exception
    {
        NullContentAsEmpty<EnumMap<ABC, String>> result = MAPPER.readValue(a2q("{'values': {'B':null} }"),
                new TypeReference<NullContentAsEmpty<EnumMap<ABC, String>>>() { });
        assertNotNull(result.values);
        assertEquals(1, result.values.size());
        assertEquals("", result.values.get(ABC.B));
    }

    /*
    /**********************************************************
    /* Test methods, for container contents, skip nulls
    /**********************************************************
     */


    public void testEnumSetSkipNulls() throws Exception
    {
        NullContentSkip<EnumSet<ABC>> result = MAPPER.readValue(a2q("{'values': [ null ]}"),
                new TypeReference<NullContentSkip<EnumSet<ABC>>>() { });
        assertNotNull(result.values);
        assertEquals(0, result.values.size());
    }

    public void testEnumMapSkipNulls() throws Exception
    {
        NullContentSkip<EnumMap<ABC, String>> result = MAPPER.readValue(a2q("{'values': {'B':null} }"),
                new TypeReference<NullContentSkip<EnumMap<ABC, String>>>() { });
        assertNotNull(result.values);
        assertEquals(0, result.values.size());
    }
}
