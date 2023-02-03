package com.fasterxml.jackson.databind;

import com.fasterxml.jackson.annotation.*;

import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import com.fasterxml.jackson.databind.json.JsonMapper;

/**
 * Unit tests dealing with handling of "root element wrapping",
 * including configuration of root name to use.
 */
public class TestRootName extends BaseMapTest
{
    @JsonRootName("rudy")
    static class Bean {
        public int a = 3;
    }

    @JsonRootName("")
    static class RootBeanWithEmpty {
        public int a = 2;
    }

    /*
    /**********************************************************
    /* Unit tests
    /**********************************************************
     */

    public void testRootViaMapper() throws Exception
    {
        ObjectMapper mapper = rootMapper();
        String json = mapper.writeValueAsString(new Bean());
        assertEquals("{\"rudy\":{\"a\":3}}", json);
        Bean bean = mapper.readValue(json, Bean.class);
        assertNotNull(bean);

        // also same with explicitly "not defined"...
        json = mapper.writeValueAsString(new RootBeanWithEmpty());
        assertEquals("{\"RootBeanWithEmpty\":{\"a\":2}}", json);
        RootBeanWithEmpty bean2 = mapper.readValue(json, RootBeanWithEmpty.class);
        assertNotNull(bean2);
        assertEquals(2, bean2.a);
    }

    public void testRootViaMapperFails() throws Exception
    {
        final ObjectMapper mapper = rootMapper();
        // First kind of fail, wrong name
        try {
            mapper.readValue(a2q("{'notRudy':{'a':3}}"), Bean.class);
            fail("Should not pass");
        } catch (MismatchedInputException e) {
            verifyException(e, "Root name ('notRudy') does not match expected ('rudy')");
        }

        // second: non-Object
        try {
            mapper.readValue(a2q("[{'rudy':{'a':3}}]"), Bean.class);
            fail("Should not pass");
        } catch (MismatchedInputException e) {
            verifyException(e, "Unexpected token (START_ARRAY");
        }

        // Third: empty Object
        try {
            mapper.readValue(a2q("{}]"), Bean.class);
            fail("Should not pass");
        } catch (MismatchedInputException e) {
            verifyException(e, "Current token not FIELD_NAME");
        }

        // Fourth, stuff after wrapped
        try {
            mapper.readValue(a2q("{'rudy':{'a':3}, 'extra':3}"), Bean.class);
            fail("Should not pass");
        } catch (MismatchedInputException e) {
            verifyException(e, "Unexpected token");
            verifyException(e, "Current token not END_OBJECT (to match wrapper");
        }
    }

    public void testRootViaReaderFails() throws Exception
    {
        final ObjectReader reader = rootMapper().readerFor(Bean.class);
        // First kind of fail, wrong name
        try {
            reader.readValue(a2q("{'notRudy':{'a':3}}"));
            fail("Should not pass");
        } catch (MismatchedInputException e) {
            verifyException(e, "Root name ('notRudy') does not match expected ('rudy')");
        }

        // second: non-Object
        try {
            reader.readValue(a2q("[{'rudy':{'a':3}}]"));
            fail("Should not pass");
        } catch (MismatchedInputException e) {
            verifyException(e, "Unexpected token (START_ARRAY");
        }

        // Third: empty Object
        try {
            reader.readValue(a2q("{}]"));
            fail("Should not pass");
        } catch (MismatchedInputException e) {
            verifyException(e, "Current token not FIELD_NAME");
        }

        // Fourth, stuff after wrapped
        try {
            reader.readValue(a2q("{'rudy':{'a':3}, 'extra':3}"));
            fail("Should not pass");
        } catch (MismatchedInputException e) {
            verifyException(e, "Unexpected token");
            verifyException(e, "Current token not END_OBJECT (to match wrapper");
        }
    }

    public void testRootViaWriterAndReader() throws Exception
    {
        ObjectMapper mapper = rootMapper();
        String json = mapper.writer().writeValueAsString(new Bean());
        assertEquals("{\"rudy\":{\"a\":3}}", json);
        Bean bean = mapper.readerFor(Bean.class).readValue(json);
        assertNotNull(bean);
    }

    public void testReconfiguringOfWrapping() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        // default: no wrapping
        final Bean input = new Bean();
        String jsonUnwrapped = mapper.writeValueAsString(input);
        assertEquals("{\"a\":3}", jsonUnwrapped);
        // secondary: wrapping
        String jsonWrapped = mapper.writer(SerializationFeature.WRAP_ROOT_VALUE)
            .writeValueAsString(input);
        assertEquals("{\"rudy\":{\"a\":3}}", jsonWrapped);

        // and then similarly for readers:
        Bean result = mapper.readValue(jsonUnwrapped, Bean.class);
        assertNotNull(result);
        try { // must not have extra wrapping
            result = mapper.readerFor(Bean.class).with(DeserializationFeature.UNWRAP_ROOT_VALUE)
                .readValue(jsonUnwrapped);
            fail("Should have failed");
        } catch (MismatchedInputException e) {
            verifyException(e, "Root name ('a')");
        }
        // except wrapping may be expected:
        result = mapper.readerFor(Bean.class).with(DeserializationFeature.UNWRAP_ROOT_VALUE)
            .readValue(jsonWrapped);
        assertNotNull(result);
    }

    public void testRootUsingExplicitConfig() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        ObjectWriter writer = mapper.writer().withRootName("wrapper");
        String json = writer.writeValueAsString(new Bean());
        assertEquals("{\"wrapper\":{\"a\":3}}", json);

        ObjectReader reader = mapper.readerFor(Bean.class).withRootName("wrapper");
        Bean bean = reader.readValue(json);
        assertNotNull(bean);

        // also: verify that we can override SerializationFeature as well:
        ObjectMapper wrapping = rootMapper();
        json = wrapping.writer().withRootName("something").writeValueAsString(new Bean());
        assertEquals("{\"something\":{\"a\":3}}", json);
        json = wrapping.writer().withRootName("").writeValueAsString(new Bean());
        assertEquals("{\"a\":3}", json);

        // 21-Apr-2015, tatu: Alternative available with 2.6 as well:
        json = wrapping.writer().withoutRootName().writeValueAsString(new Bean());
        assertEquals("{\"a\":3}", json);

        bean = wrapping.readerFor(Bean.class).withRootName("").readValue(json);
        assertNotNull(bean);
        assertEquals(3, bean.a);

        bean = wrapping.readerFor(Bean.class).withoutRootName().readValue("{\"a\":4}");
        assertNotNull(bean);
        assertEquals(4, bean.a);

        // and back to defaults
        bean = wrapping.readerFor(Bean.class).readValue("{\"rudy\":{\"a\":7}}");
        assertNotNull(bean);
        assertEquals(7, bean.a);
    }

    /*
    /**********************************************************
    /* Helper methods
    /**********************************************************
     */

    private final ObjectMapper ROOT_MAPPER = JsonMapper.builder()
            .enable(SerializationFeature.WRAP_ROOT_VALUE)
            .enable(DeserializationFeature.UNWRAP_ROOT_VALUE)
            .build();

    private ObjectMapper rootMapper() {
        return ROOT_MAPPER;
    }
}
