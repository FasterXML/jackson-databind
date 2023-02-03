package com.fasterxml.jackson.databind.struct;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat.Shape;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import com.fasterxml.jackson.databind.introspect.Annotated;
import com.fasterxml.jackson.databind.introspect.JacksonAnnotationIntrospector;

public class TestPOJOAsArray extends BaseMapTest
{
    static class PojoAsArrayWrapper
    {
        @JsonFormat(shape=JsonFormat.Shape.ARRAY)
        public PojoAsArray value;

        public PojoAsArrayWrapper() { }
        public PojoAsArrayWrapper(String name, int x, int y, boolean c) {
            value = new PojoAsArray(name, x, y, c);
        }
    }

    @JsonPropertyOrder(alphabetic=true)
    static class NonAnnotatedXY {
        public int x, y;

        public NonAnnotatedXY() { }
        public NonAnnotatedXY(int x0, int y0) {
            x = x0;
            y = y0;
        }
    }

    // note: must be serialized/deserialized alphabetically; fields NOT declared in that order
    @JsonPropertyOrder(alphabetic=true)
    static class PojoAsArray
    {
        public int x, y;
        public String name;
        public boolean complete;

        public PojoAsArray() { }
        public PojoAsArray(String name, int x, int y, boolean c) {
            this.name = name;
            this.x = x;
            this.y = y;
            this.complete = c;
        }
    }

    @JsonPropertyOrder(alphabetic=true)
    @JsonFormat(shape=JsonFormat.Shape.ARRAY)
    static class FlatPojo
    {
        public int x, y;
        public String name;
        public boolean complete;

        public FlatPojo() { }
        public FlatPojo(String name, int x, int y, boolean c) {
            this.name = name;
            this.x = x;
            this.y = y;
            this.complete = c;
        }
    }

    static class ForceArraysIntrospector extends JacksonAnnotationIntrospector
    {
        private static final long serialVersionUID = 1L;

        @Override
        public JsonFormat.Value findFormat(Annotated a) {
            return new JsonFormat.Value().withShape(JsonFormat.Shape.ARRAY);
        }
    }

    static class A {
        public B value = new B();
    }

    @JsonPropertyOrder(alphabetic=true)
    static class B {
        public int x = 1;
        public int y = 2;
    }

    @JsonFormat(shape=Shape.ARRAY)
    static class SingleBean {
        public String name = "foo";
    }

    @JsonPropertyOrder(alphabetic=true)
    @JsonFormat(shape=Shape.ARRAY)
    static class TwoStringsBean {
        public String bar = null;
        public String foo = "bar";
    }

    @JsonFormat(shape=JsonFormat.Shape.ARRAY)
    @JsonPropertyOrder(alphabetic=true)
    static class AsArrayWithMap
    {
        public Map<Integer,Integer> attrs;

        public AsArrayWithMap() { }
        public AsArrayWithMap(int x, int y) {
            attrs = new HashMap<Integer,Integer>();
            attrs.put(x, y);
        }
    }

    @JsonFormat(shape=JsonFormat.Shape.ARRAY)
    static class CreatorWithIndex {
        protected int _a, _b;

        @JsonCreator
        public CreatorWithIndex(@JsonProperty(index=0, value="a") int a,
                @JsonProperty(index=1, value="b") int b) {
            this._a = a;
            this._b = b;
        }
    }

    /*
    /*****************************************************
    /* Basic tests
    /*****************************************************
     */

    private final static ObjectMapper MAPPER = new ObjectMapper();

    /**
     * Test that verifies that property annotation works
     */
    public void testReadSimplePropertyValue() throws Exception
    {
        String json = "{\"value\":[true,\"Foobar\",42,13]}";
        PojoAsArrayWrapper p = MAPPER.readValue(json, PojoAsArrayWrapper.class);
        assertNotNull(p.value);
        assertTrue(p.value.complete);
        assertEquals("Foobar", p.value.name);
        assertEquals(42, p.value.x);
        assertEquals(13, p.value.y);
    }

    /**
     * Test that verifies that Class annotation works
     */
    public void testReadSimpleRootValue() throws Exception
    {
        String json = "[false,\"Bubba\",1,2]";
        FlatPojo p = MAPPER.readValue(json, FlatPojo.class);
        assertFalse(p.complete);
        assertEquals("Bubba", p.name);
        assertEquals(1, p.x);
        assertEquals(2, p.y);
    }

    /**
     * Test that verifies that property annotation works
     */
    public void testWriteSimplePropertyValue() throws Exception
    {
        String json = MAPPER.writeValueAsString(new PojoAsArrayWrapper("Foobar", 42, 13, true));
        // will have wrapper POJO, then POJO-as-array..
        assertEquals("{\"value\":[true,\"Foobar\",42,13]}", json);
    }

    /**
     * Test that verifies that Class annotation works
     */
    public void testWriteSimpleRootValue() throws Exception
    {
        String json = MAPPER.writeValueAsString(new FlatPojo("Bubba", 1, 2, false));
        // will have wrapper POJO, then POJO-as-array..
        assertEquals("[false,\"Bubba\",1,2]", json);
    }

    // [Issue#223]
    public void testNullColumn() throws Exception
    {
        assertEquals("[null,\"bar\"]", MAPPER.writeValueAsString(new TwoStringsBean()));
    }

    /*
    /*****************************************************
    /* Compatibility with "single-elem as array" feature
    /*****************************************************
     */

    public void testSerializeAsArrayWithSingleProperty() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        mapper.enable(SerializationFeature.WRITE_SINGLE_ELEM_ARRAYS_UNWRAPPED);
        String json = mapper.writeValueAsString(new SingleBean());
        assertEquals("\"foo\"", json);
    }

    public void testBeanAsArrayUnwrapped() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        mapper.enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY);
        SingleBean result = mapper.readValue("[\"foobar\"]", SingleBean.class);
        assertNotNull(result);
        assertEquals("foobar", result.name);
    }

    /*
    /*****************************************************
    /* Round-trip tests
    /*****************************************************
     */

    public void testAnnotationOverride() throws Exception
    {
        // by default, POJOs become JSON Objects;
        assertEquals("{\"value\":{\"x\":1,\"y\":2}}", MAPPER.writeValueAsString(new A()));

        // but override should change it:
        ObjectMapper mapper2 = new ObjectMapper();
        mapper2.setAnnotationIntrospector(new ForceArraysIntrospector());
        assertEquals("[[1,2]]", mapper2.writeValueAsString(new A()));

        // and allow reading back, too
    }

    public void testWithMaps() throws Exception
    {
        AsArrayWithMap input = new AsArrayWithMap(1, 2);
        String json = MAPPER.writeValueAsString(input);
        AsArrayWithMap output = MAPPER.readValue(json, AsArrayWithMap.class);
        assertNotNull(output);
        assertNotNull(output.attrs);
        assertEquals(1, output.attrs.size());
        assertEquals(Integer.valueOf(2), output.attrs.get(1));
    }

    public void testSimpleWithIndex() throws Exception
    {
        // as POJO:
//        CreatorWithIndex value = MAPPER.readValue(aposToQuotes("{'b':1,'a':2}"),
        CreatorWithIndex value = MAPPER.readValue(a2q("[2,1]"),
                CreatorWithIndex.class);
        assertEquals(2, value._a);
        assertEquals(1, value._b);
    }

    public void testWithConfigOverrides() throws Exception
    {
        ObjectMapper mapper = jsonMapperBuilder()
                .withConfigOverride(NonAnnotatedXY.class,
                        o -> o.setFormat(JsonFormat.Value.forShape(JsonFormat.Shape.ARRAY)))
                .build();
        final String json = mapper.writeValueAsString(new NonAnnotatedXY(2, 3));
        assertEquals("[2,3]", json);

        // also, read it back
        NonAnnotatedXY result = mapper.readValue(json, NonAnnotatedXY.class);
        assertNotNull(result);
        assertEquals(3, result.y);
    }

    /*
    /*****************************************************
    /* Failure tests
    /*****************************************************
     */

    public void testUnknownExtraProp() throws Exception
    {
        String json = "{\"value\":[true,\"Foobar\",42,13, false]}";
        try {
            MAPPER.readValue(json, PojoAsArrayWrapper.class);
            fail("should not pass with extra element");
        } catch (MismatchedInputException e) {
            verifyException(e, "Unexpected JSON values");
        }

        // but actually fine if skip-unknown set
        PojoAsArrayWrapper v = MAPPER.readerFor(PojoAsArrayWrapper.class)
                .without(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .readValue(json);
        assertNotNull(v);
        // note: +1 for both so
        assertEquals(v.value.x, 42);
        assertEquals(v.value.y, 13);
        assertTrue(v.value.complete);
        assertEquals("Foobar", v.value.name);
    }
}
