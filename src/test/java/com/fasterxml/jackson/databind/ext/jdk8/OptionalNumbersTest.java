package com.fasterxml.jackson.databind.ext.jdk8;

import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.*;

public class OptionalNumbersTest extends BaseMapTest
{
    static class OptionalIntBean {
        public OptionalInt value;

        public OptionalIntBean() { value = OptionalInt.empty(); }
        OptionalIntBean(int v) {
            value = OptionalInt.of(v);
        }
    }
    
    static class OptionalLongBean {
        public OptionalLong value;

        public OptionalLongBean() { value = OptionalLong.empty(); }
        OptionalLongBean(long v) {
            value = OptionalLong.of(v);
        }
    }

    static class OptionalDoubleBean {
        public OptionalDouble value;

        public OptionalDoubleBean() { value = OptionalDouble.empty(); }
        OptionalDoubleBean(double v) {
            value = OptionalDouble.of(v);
        }
    }

    private final ObjectMapper MAPPER = newObjectMapper();

    /*
    /**********************************************************
    /* Test methods, OptionalInt
    /**********************************************************
     */

    public void testOptionalIntAbsent() throws Exception
    {
        String json = MAPPER.writeValueAsString(OptionalInt.empty());
        assertFalse(MAPPER.readValue(json, OptionalInt.class).isPresent());
    }

    public void testOptionalIntInArrayAbsent() throws Exception
    {
        OptionalInt[] ints = MAPPER.readValue("[null]", OptionalInt[].class);
        assertEquals(1, ints.length);
        assertNotNull(ints[0]);
        assertFalse(ints[0].isPresent());
    }

    public void testOptionalIntPresent() throws Exception
    {
        assertEquals(5, MAPPER.readValue(MAPPER.writeValueAsBytes(OptionalInt.of(5)), OptionalInt.class).getAsInt());
    }

    public void testOptionalIntCoerceFromString() throws Exception
    {
        OptionalInt opt = MAPPER.readValue(quote("123"), OptionalInt.class);
        assertEquals(123, opt.getAsInt());
        opt = MAPPER.readValue("\"\"", OptionalInt.class);
        assertNotNull(opt);
        assertFalse(opt.isPresent());

        OptionalIntBean bean = MAPPER.readValue(aposToQuotes("{'value':null}"),
                OptionalIntBean.class);
        assertNotNull(bean.value);
        assertFalse(bean.value.isPresent());

        bean = MAPPER.readValue(aposToQuotes("{'value':'-37'}"), OptionalIntBean.class);
        assertNotNull(bean.value);
        assertEquals(-37L, bean.value.getAsInt());
    }

    /*
    /**********************************************************
    /* Test methods, OptionalLong
    /**********************************************************
     */

    public void testOptionalLongAbsent() throws Exception
    {
        assertFalse(MAPPER.readValue(MAPPER.writeValueAsBytes(OptionalLong.empty()), OptionalLong.class).isPresent());
    }

    public void testOptionalLongInArrayAbsent() throws Exception
    {
        OptionalLong[] arr = MAPPER.readValue("[null]", OptionalLong[].class);
        assertEquals(1, arr.length);
        assertNotNull(arr[0]);
        assertFalse(arr[0].isPresent());
    }
    
    public void testOptionalLongPresent() throws Exception
    {
        assertEquals(Long.MAX_VALUE, MAPPER.readValue(MAPPER.writeValueAsBytes(OptionalLong.of(Long.MAX_VALUE)), OptionalLong.class).getAsLong());
    }

    public void testOptionalLongCoerceFromString() throws Exception
    {
        OptionalLong opt = MAPPER.readValue(quote("123"), OptionalLong.class);
        assertEquals(123L, opt.getAsLong());

        // should coerce from empty String too (by default)
        opt = MAPPER.readValue("\"\"", OptionalLong.class);
        assertNotNull(opt);
        assertFalse(opt.isPresent());
        
        OptionalLongBean bean = MAPPER.readValue(aposToQuotes("{'value':null}"),
                OptionalLongBean.class);
        assertNotNull(bean.value);
        assertFalse(bean.value.isPresent());

        bean = MAPPER.readValue(aposToQuotes("{'value':'19'}"), OptionalLongBean.class);
        assertNotNull(bean.value);
        assertEquals(19L, bean.value.getAsLong());
    }
    
    public void testOptionalLongSerializeFilter() throws Exception
    {
        ObjectMapper mapper = newObjectMapper()
                .setSerializationInclusion(JsonInclude.Include.NON_NULL);
        assertEquals(aposToQuotes("{'value':123}"),
                mapper.writeValueAsString(new OptionalLongBean(123L)));
        // absent is not strictly null so
        assertEquals(aposToQuotes("{'value':null}"),
                mapper.writeValueAsString(new OptionalLongBean()));

        // however:
        mapper = newObjectMapper()
                .setSerializationInclusion(JsonInclude.Include.NON_ABSENT);
        assertEquals(aposToQuotes("{'value':456}"),
                mapper.writeValueAsString(new OptionalLongBean(456L)));
        assertEquals(aposToQuotes("{}"),
                mapper.writeValueAsString(new OptionalLongBean()));
    }

    /*
    /**********************************************************
    /* Test methods, OptionalDouble
    /**********************************************************
     */

    public void testOptionalDoubleAbsent() throws Exception
    {
        assertFalse(MAPPER.readValue(MAPPER.writeValueAsBytes(OptionalInt.empty()), OptionalInt.class).isPresent());
    }

    public void testOptionalDoubleInArrayAbsent() throws Exception
    {
        OptionalDouble[] arr = MAPPER.readValue("[null]", OptionalDouble[].class);
        assertEquals(1, arr.length);
        assertNotNull(arr[0]);
        assertFalse(arr[0].isPresent());
    }

    public void testOptionalDoublePresent() throws Exception
    {
        assertEquals(Double.MIN_VALUE, MAPPER.readValue(MAPPER.writeValueAsBytes(OptionalDouble.of(Double.MIN_VALUE)), OptionalDouble.class).getAsDouble());
    }

    public void testOptionalDoubleCoerceFromString() throws Exception
    {
        OptionalDouble opt = MAPPER.readValue(quote("0.25"), OptionalDouble.class);
        assertEquals(0.25, opt.getAsDouble());

        // should coerce from empty String too (by default)
        opt = MAPPER.readValue("\"\"", OptionalDouble.class);
        assertNotNull(opt);
        assertFalse(opt.isPresent());
        
        OptionalDoubleBean bean = MAPPER.readValue(aposToQuotes("{'value':null}"),
                OptionalDoubleBean.class);
        assertNotNull(bean.value);
        assertFalse(bean.value.isPresent());

        bean = MAPPER.readValue(aposToQuotes("{'value':'0.5'}"), OptionalDoubleBean.class);
        assertNotNull(bean.value);
        assertEquals(0.5, bean.value.getAsDouble());
    }
}
