package com.fasterxml.jackson.databind.jsontype;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.exc.InvalidTypeIdException;

// Tests wrt [databind#1983]
public class JsonTypeInfoCaseInsensitive1983Test extends BaseMapTest
{
    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXTERNAL_PROPERTY,
            property = "Operation")
    @JsonSubTypes({
            @JsonSubTypes.Type(value = Equal.class, name = "eq"),
            @JsonSubTypes.Type(value = NotEqual.class, name = "notEq"),
    })
    static abstract class Filter {
    }

    static class Equal extends Filter { }

    static class NotEqual extends Filter { }

    // verify failures when exact matching required:
    private final ObjectMapper MAPPER = newJsonMapper();

    public void testReadMixedCaseSubclass() throws Exception
    {
        final String serialised = "{\"Operation\":\"NoTeQ\"}";

        // first: mismatch with value unless case-sensitivity disabled:
        try {
            MAPPER.readValue(serialised, Filter.class);
            fail("Should not pass");
        } catch (InvalidTypeIdException e) {
            verifyException(e, "Could not resolve type id 'NoTeQ'");
        }

        ObjectMapper mapper = jsonMapperBuilder()
                .enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_VALUES)
                .build();
        // Type id ("value") mismatch, should work now:
        Filter result = mapper.readValue(serialised, Filter.class);

        assertEquals(NotEqual.class, result.getClass());
    }

    public void testReadMixedCasePropertyName() throws Exception
    {
        final String serialised = "{\"oPeRaTioN\":\"notEq\"}";
        // first: mismatch with property name unless case-sensitivity disabled:
        try {
            MAPPER.readValue(serialised, Filter.class);
            fail("Should not pass");
        } catch (InvalidTypeIdException e) {
            verifyException(e, "missing type id property");
        }

        ObjectMapper mapper = jsonMapperBuilder()
                .enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES)
                .build();
        // Type property name mismatch (but value match); should work:
        Filter result = mapper.readValue(serialised, Filter.class);

        assertEquals(NotEqual.class, result.getClass());
    }
}
