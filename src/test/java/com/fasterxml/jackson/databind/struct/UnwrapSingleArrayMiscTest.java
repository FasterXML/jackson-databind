package com.fasterxml.jackson.databind.struct;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;

public class UnwrapSingleArrayMiscTest extends BaseMapTest
{
    private final ObjectMapper UNWRAPPING_MAPPER = jsonMapperBuilder()
            .enable(DeserializationFeature.UNWRAP_SINGLE_VALUE_ARRAYS)
            .build();

    /*
    /**********************************************************
    /* Tests methods, POJOs
    /**********************************************************
     */

    public void testSimplePOJOUnwrapping() throws Exception
    {
        ObjectReader r = UNWRAPPING_MAPPER.readerFor(IntWrapper.class);
        IntWrapper w = r.readValue(a2q("[{'i':42}]"));
        assertEquals(42, w.i);

        try {
            r.readValue(a2q("[{'i':42},{'i':16}]"));
            fail("Did not throw exception while reading a value from a multi value array");
        } catch (MismatchedInputException e) {
            verifyException(e, "more than one value");
        }
    }

    /*
    /**********************************************************
    /* Tests methods, Maps
    /**********************************************************
     */

    // [databind#2767]: should work for Maps, too
    public void testSimpleMapUnwrapping() throws Exception
    {
        ObjectReader r = UNWRAPPING_MAPPER.readerFor(Map.class);
        Map<String,Object> m = r.readValue(a2q("[{'stuff':42}]"));
        assertEquals(Collections.<String,Object>singletonMap("stuff", Integer.valueOf(42)), m);

        try {
            r.readValue(a2q("[{'i':42},{'i':16}]"));
            fail("Did not throw exception while reading a value from a multi value array");
        } catch (MismatchedInputException e) {
            verifyException(e, "more than one value");
        }
    }

    public void testEnumMapUnwrapping() throws Exception
    {
        ObjectReader r = UNWRAPPING_MAPPER.readerFor(new TypeReference<EnumMap<ABC,Integer>>() { });
        EnumMap<ABC,Integer> m = r.readValue(a2q("[{'A':42}]"));
        EnumMap<ABC,Integer> exp = new EnumMap<>(ABC.class);
        exp.put(ABC.A, Integer.valueOf(42));
        assertEquals(exp, m);

        try {
            r.readValue(a2q("[{'A':42},{'B':13}]"));
            fail("Did not throw exception while reading a value from a multi value array");
        } catch (MismatchedInputException e) {
            verifyException(e, "more than one value");
        }
    }
}
