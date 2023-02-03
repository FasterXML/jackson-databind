package com.fasterxml.jackson.databind.ser.jdk;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.atomic.*;

import com.fasterxml.jackson.annotation.*;

import com.fasterxml.jackson.databind.BaseMapTest;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

/**
 * Unit tests for verifying serialization of {@link java.util.concurrent.atomic.AtomicReference}
 * and other atomic types, via various settings.
 */
public class AtomicTypeSerializationTest
    extends BaseMapTest
{
    static class UCStringWrapper {
        @JsonSerialize(contentUsing=UpperCasingSerializer.class)
        public AtomicReference<String> value;

        public UCStringWrapper(String s) { value = new AtomicReference<String>(s); }
    }

    // [datatypes-java8#17]
    @JsonPropertyOrder({ "date1", "date2", "date" })
    static class ContextualOptionals
    {
        public AtomicReference<Date> date;

        @JsonFormat(shape=JsonFormat.Shape.STRING, pattern="yyyy+MM+dd")
        public AtomicReference<Date> date1;

        @JsonFormat(shape=JsonFormat.Shape.STRING, pattern="yyyy*MM*dd")
        public AtomicReference<Date> date2;
    }

    // [databind#1673]
    static class ContainerA {
        public AtomicReference<Strategy> strategy =
                new AtomicReference<>((Strategy) new Foo(42));
    }

    static class ContainerB {
        public AtomicReference<List<Strategy>> strategy;
        {
            List<Strategy> list = new ArrayList<>();
            list.add(new Foo(42));
            strategy = new AtomicReference<>(list);
        }
    }

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
    @JsonSubTypes({ @JsonSubTypes.Type(name = "Foo", value = Foo.class) })
    interface Strategy { }

    static class Foo implements Strategy {
        public int foo;

        @JsonCreator
        Foo(@JsonProperty("foo") int foo) {
            this.foo = foo;
        }
    }

    // [databind#2565]: problems with JsonUnwrapped, non-unwrappable type
    static class MyBean2565 {
        @JsonUnwrapped
        public AtomicReference<String> maybeText = new AtomicReference<>("value");
    }

    /*
    /**********************************************************
    /* Test methods
    /**********************************************************
     */

    private final ObjectMapper MAPPER = newJsonMapper();

    public void testAtomicBoolean() throws Exception
    {
        assertEquals("true", MAPPER.writeValueAsString(new AtomicBoolean(true)));
        assertEquals("false", MAPPER.writeValueAsString(new AtomicBoolean(false)));
    }

    public void testAtomicInteger() throws Exception
    {
        assertEquals("1", MAPPER.writeValueAsString(new AtomicInteger(1)));
        assertEquals("-9", MAPPER.writeValueAsString(new AtomicInteger(-9)));
    }

    public void testAtomicLong() throws Exception
    {
        assertEquals("0", MAPPER.writeValueAsString(new AtomicLong(0)));
    }

    public void testAtomicReference() throws Exception
    {
        String[] strs = new String[] { "abc" };
        assertEquals("[\"abc\"]", MAPPER.writeValueAsString(new AtomicReference<String[]>(strs)));
    }

    public void testCustomSerializer() throws Exception
    {
        final String VALUE = "fooBAR";
        String json = MAPPER.writeValueAsString(new UCStringWrapper(VALUE));
        assertEquals(json, a2q("{'value':'FOOBAR'}"));
    }

    public void testContextualAtomicReference() throws Exception
    {
        SimpleDateFormat df = new SimpleDateFormat("yyyy/MM/dd");
        df.setTimeZone(TimeZone.getTimeZone("UTC"));
        final ObjectMapper mapper = objectMapper();
        mapper.setDateFormat(df);
        ContextualOptionals input = new ContextualOptionals();
        input.date = new AtomicReference<>(new Date(0L));
        input.date1 = new AtomicReference<>(new Date(0L));
        input.date2 = new AtomicReference<>(new Date(0L));
        final String json = mapper.writeValueAsString(input);
        assertEquals(a2q(
                "{'date1':'1970+01+01','date2':'1970*01*01','date':'1970/01/01'}"),
                json);
    }

    // [databind#1673]
    public void testPolymorphicReferenceSimple() throws Exception
    {
        final String EXPECTED = "{\"type\":\"Foo\",\"foo\":42}";
        String json = MAPPER.writeValueAsString(new ContainerA());
        assertEquals("{\"strategy\":" + EXPECTED + "}", json);
    }

    // [databind#1673]
    public void testPolymorphicReferenceListOf() throws Exception
    {
        final String EXPECTED = "{\"type\":\"Foo\",\"foo\":42}";
        // Reproduction of issue seen with scala.Option and java8 Optional types:
        // https://github.com/FasterXML/jackson-module-scala/issues/346#issuecomment-336483326
        String json = MAPPER.writeValueAsString(new ContainerB());
        assertEquals("{\"strategy\":[" + EXPECTED + "]}", json);
    }

    // [databind#2565]: problems with JsonUnwrapped, non-unwrappable type
    public void testWithUnwrappableUnwrapped() throws Exception
    {
        assertEquals(a2q("{'maybeText':'value'}"),
                MAPPER.writeValueAsString(new MyBean2565()));
    }
}
