package tools.jackson.databind.struct;

import java.util.*;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat.Shape;

import tools.jackson.databind.*;
import tools.jackson.databind.cfg.MapperConfig;
import tools.jackson.databind.exc.MismatchedInputException;
import tools.jackson.databind.introspect.Annotated;
import tools.jackson.databind.introspect.JacksonAnnotationIntrospector;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.*;

public class POJOAsArrayTest extends DatabindTestUtil
{
    static class PojoAsArrayWrapper
    {
        @JsonFormat(shape=JsonFormat.Shape.ARRAY)
        public PojoAsArray value;

        public PojoAsArrayWrapper() { }
        protected PojoAsArrayWrapper(String name, int x, int y, boolean c) {
            value = new PojoAsArray(name, x, y, c);
        }
    }

    @JsonPropertyOrder(alphabetic=true)
    static class NonAnnotatedXY {
        public int x, y;

        public NonAnnotatedXY() { }
        protected NonAnnotatedXY(int x0, int y0) {
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
        protected PojoAsArray(String name, int x, int y, boolean c) {
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
        protected FlatPojo(String name, int x, int y, boolean c) {
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
        public JsonFormat.Value findFormat(MapperConfig<?> config, Annotated a) {
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
        protected AsArrayWithMap(int x, int y) {
            attrs = new HashMap<Integer,Integer>();
            attrs.put(x, y);
        }
    }

    // [databind#2077]
    @JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.WRAPPER_ARRAY)
    @JsonSubTypes({
        @JsonSubTypes.Type(value = DirectLayout.class, name = "Direct"),
    })
    public interface Layout {
    }

    @JsonFormat(shape=JsonFormat.Shape.ARRAY)
    public static class DirectLayout implements Layout {
    }

    // [databind#4961]
    static class WrapperForAnyGetter {
        public BeanWithAnyGetter value;
    }

    @JsonFormat(shape = JsonFormat.Shape.ARRAY)
    @JsonPropertyOrder({ "firstProperty", "secondProperties", "forthProperty" })
    static class BeanWithAnyGetter {
        public String firstProperty = "first";
        public String secondProperties = "second";
        public String forthProperty = "forth";
        @JsonAnyGetter
        public Map<String, String> getAnyProperty() {
            Map<String, String> map = new TreeMap<>();
            map.put("third_A", "third_A");
            map.put("third_B", "third_B");
            return map;
        }
    }

    // [databind#646]
    @JsonFormat(shape = JsonFormat.Shape.ARRAY)
    @JsonPropertyOrder(alphabetic = true)
    static class Outer646 {
        protected Map<String, TheItem646> attributes;

        public Outer646() {
            attributes = new HashMap<String, TheItem646>();
        }

        @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.WRAPPER_ARRAY)
        public Map<String, TheItem646> getAttributes() {
            return attributes;
        }
    }

    @JsonFormat(shape = JsonFormat.Shape.ARRAY)
    @JsonPropertyOrder(alphabetic = true)
    static class TheItem646 {

        @JsonFormat(shape = JsonFormat.Shape.ARRAY)
        @JsonPropertyOrder(alphabetic = true)
        public static class NestedItem {
            public String nestedStrValue;

            @JsonCreator
            public NestedItem(@JsonProperty("nestedStrValue") String nestedStrValue) {
                this.nestedStrValue = nestedStrValue;
            }
        }

        private String strValue;
        private boolean boolValue;
        private List<NestedItem> nestedItems;

        @JsonCreator
        public TheItem646(@JsonProperty("strValue") String strValue, @JsonProperty("boolValue") boolean boolValue, @JsonProperty("nestedItems") List<NestedItem> nestedItems) {
            this.strValue = strValue;
            this.boolValue = boolValue;
            this.nestedItems = nestedItems;
        }

        public String getStrValue() {
            return strValue;
        }

        public void setStrValue(String strValue) {
            this.strValue = strValue;
        }

        public boolean isBoolValue() {
            return boolValue;
        }

        public void setBoolValue(boolean boolValue) {
            this.boolValue = boolValue;
        }

        public List<NestedItem> getNestedItems() {
            return nestedItems;
        }

        public void setNestedItems(List<NestedItem> nestedItems) {
            this.nestedItems = nestedItems;
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

    private final static ObjectMapper MAPPER = newJsonMapper();

    /**
     * Test that verifies that property annotation works
     */
    @Test
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
    @Test
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
    @Test
    public void testWriteSimplePropertyValue() throws Exception
    {
        String json = MAPPER.writeValueAsString(new PojoAsArrayWrapper("Foobar", 42, 13, true));
        // will have wrapper POJO, then POJO-as-array..
        assertEquals("{\"value\":[true,\"Foobar\",42,13]}", json);
    }

    /**
     * Test that verifies that Class annotation works
     */
    @Test
    public void testWriteSimpleRootValue() throws Exception
    {
        String json = MAPPER.writeValueAsString(new FlatPojo("Bubba", 1, 2, false));
        // will have wrapper POJO, then POJO-as-array..
        assertEquals("[false,\"Bubba\",1,2]", json);
    }

    // [Issue#223]
    @Test
    public void testNullColumn() throws Exception
    {
        assertEquals("[null,\"bar\"]", MAPPER.writeValueAsString(new TwoStringsBean()));
    }

    /*
    /*****************************************************
    /* Compatibility with "single-elem as array" feature
    /*****************************************************
     */

    @Test
    public void testSerializeAsArrayWithSingleProperty() throws Exception {
        ObjectMapper mapper = jsonMapperBuilder()
                .enable(SerializationFeature.WRITE_SINGLE_ELEM_ARRAYS_UNWRAPPED)
                .build();
        String json = mapper.writeValueAsString(new SingleBean());
        assertEquals("\"foo\"", json);
    }

    @Test
    public void testBeanAsArrayUnwrapped() throws Exception
    {
        ObjectMapper mapper = jsonMapperBuilder()
                .enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
                .build();
        SingleBean result = mapper.readValue("[\"foobar\"]", SingleBean.class);
        assertNotNull(result);
        assertEquals("foobar", result.name);
    }

    /*
    /*****************************************************
    /* Round-trip tests
    /*****************************************************
     */

    @Test
    public void testAnnotationOverride() throws Exception
    {
        // by default, POJOs become JSON Objects;
        assertEquals("{\"value\":{\"x\":1,\"y\":2}}", MAPPER.writeValueAsString(new A()));

        // but override should change it:
        ObjectMapper mapper2 = jsonMapperBuilder()
                .annotationIntrospector(new ForceArraysIntrospector())
                .build();
        assertEquals("[[1,2]]", mapper2.writeValueAsString(new A()));

        // and allow reading back, too
    }

    @Test
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

    @Test
    public void testSimpleWithIndex() throws Exception
    {
        // as POJO:
//        CreatorWithIndex value = MAPPER.readValue(aposToQuotes("{'b':1,'a':2}"),
        CreatorWithIndex value = MAPPER.readValue(a2q("[2,1]"),
                CreatorWithIndex.class);
        assertEquals(2, value._a);
        assertEquals(1, value._b);
    }

    @Test
    public void testWithConfigOverrides() throws Exception
    {
        ObjectMapper mapper = jsonMapperBuilder()
                .withConfigOverride(NonAnnotatedXY.class,
                        o -> o.setFormat(JsonFormat.Value.forShape(JsonFormat.Shape.ARRAY)))
                .build();
        String json = mapper.writeValueAsString(new NonAnnotatedXY(2, 3));
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

    // [databind#2077]
    @Test
    public void testPolymorphicAsArray() throws Exception
    {
        String json = MAPPER.writeValueAsString(new DirectLayout());

        Layout instance = MAPPER.readValue(json, Layout.class);
        assertNotNull(instance);
    }

    // [databind#4961]
    @Test
    public void testSerializeArrayWithAnyGetterWithWrapper() throws Exception {
        WrapperForAnyGetter wrapper = new WrapperForAnyGetter();
        wrapper.value = new BeanWithAnyGetter();

        String json = MAPPER.writeValueAsString(wrapper);

        assertEquals(a2q("{\"value\":[\"first\",\"second\",\"forth\",{\"third_A\":\"third_A\",\"third_B\":\"third_B\"}]}"), json);
    }

    // [databind#4961]
    @Test
    public void testSerializeArrayWithAnyGetterAsRoot() throws Exception {
        BeanWithAnyGetter bean = new BeanWithAnyGetter();

        String json = MAPPER.writeValueAsString(bean);

        assertEquals(a2q("[\"first\",\"second\",\"forth\",{\"third_A\":\"third_A\",\"third_B\":\"third_B\"}]"), json);
    }

    // [databind#646]
    @Test
    public void testWithCustomTypeId() throws Exception {

        List<TheItem646.NestedItem> nestedList = new ArrayList<TheItem646.NestedItem>();
        nestedList.add(new TheItem646.NestedItem("foo1"));
        nestedList.add(new TheItem646.NestedItem("foo2"));
        TheItem646 item = new TheItem646("first", false, nestedList);
        Outer646 outer = new Outer646();
        outer.getAttributes().put("entry1", item);

        String json = MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(outer);

        Outer646 result = MAPPER.readValue(json, Outer646.class);
        assertNotNull(result);
        assertNotNull(result.attributes);
        assertEquals(1, result.attributes.size());
    }

    /*
    /*****************************************************
    /* Failure tests
    /*****************************************************
     */

    @Test
    public void testUnknownExtraProp() throws Exception
    {
        String json = "{\"value\":[true,\"Foobar\",42,13, false]}";
        try {
            MAPPER.readerFor(PojoAsArrayWrapper.class)
                    .with(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                    .readValue(json);
            fail("should not pass with extra element");
        } catch (MismatchedInputException e) {
            verifyException(e, "Unexpected JSON values");
        }

        // but actually fine if skip-unknown set
        PojoAsArrayWrapper v = MAPPER.readerFor(PojoAsArrayWrapper.class)
                .without(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .readValue(json);
        assertNotNull(v);
        assertEquals(42, v.value.x);
        assertEquals(13, v.value.y);
        assertTrue(v.value.complete);
        assertEquals("Foobar", v.value.name);
    }
}
