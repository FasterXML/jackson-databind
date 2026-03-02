package tools.jackson.databind.struct;

import java.util.*;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat.Shape;

import tools.jackson.databind.*;
import tools.jackson.databind.annotation.JsonDeserialize;
import tools.jackson.databind.cfg.MapperConfig;
import tools.jackson.databind.exc.InvalidDefinitionException;
import tools.jackson.databind.exc.MismatchedInputException;
import tools.jackson.databind.introspect.Annotated;
import tools.jackson.databind.introspect.JacksonAnnotationIntrospector;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.*;

public class POJOAsArrayTest extends DatabindTestUtil
{
    // // // Inner types for basic POJO-as-Array tests

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

    // // // Inner types for advanced POJO-as-Array tests (views, creators)

    @JsonFormat(shape=JsonFormat.Shape.ARRAY)
    @JsonPropertyOrder(alphabetic=true)
    static class CreatorAsArray
    {
        protected int x, y;
        public int a, b;

        @JsonCreator
        public CreatorAsArray(@JsonProperty("x") int x, @JsonProperty("y") int y)
        {
            this.x = x;
            this.y = y;
        }

        public int getX() { return x; }
        public int getY() { return y; }
    }

    @JsonFormat(shape=JsonFormat.Shape.ARRAY)
    @JsonPropertyOrder({"a","b","x","y"})
    static class CreatorAsArrayShuffled
    {
        protected int x, y;
        public int a, b;

        @JsonCreator
        public CreatorAsArrayShuffled(@JsonProperty("x") int x, @JsonProperty("y") int y)
        {
            this.x = x;
            this.y = y;
        }

        public int getX() { return x; }
        public int getY() { return y; }
    }

    static class ViewA { }
    static class ViewB { }

    @JsonFormat(shape=JsonFormat.Shape.ARRAY)
    @JsonPropertyOrder(alphabetic=true)
    static class AsArrayWithView
    {
        @JsonView(ViewA.class)
        public int a;
        @JsonView(ViewB.class)
        public int b;
        public int c;
    }

    @JsonFormat(shape=JsonFormat.Shape.ARRAY)
    @JsonPropertyOrder(alphabetic=true)
    static class AsArrayWithViewAndCreator
    {
        @JsonView(ViewA.class)
        public int a;
        @JsonView(ViewB.class)
        public int b;
        public int c;

        @JsonCreator
        public AsArrayWithViewAndCreator(@JsonProperty("a") int a,
                @JsonProperty("b") int b,
                @JsonProperty("c") int c)
        {
            this.a = a;
            this.b = b;
            this.c = c;
        }
    }

    // // // Inner types for POJO-as-Array with Builder tests

    @JsonDeserialize(builder=SimpleBuilderXY.class)
    @JsonFormat(shape=JsonFormat.Shape.ARRAY)
    @JsonPropertyOrder(alphabetic=true)
    static class ValueClassXY
    {
        final int _x, _y;

        protected ValueClassXY(int x, int y) {
            _x = x+1;
            _y = y+1;
        }
    }

    @JsonFormat(shape=JsonFormat.Shape.ARRAY)
    @JsonPropertyOrder(alphabetic=true)
    static class SimpleBuilderXY
    {
        public int x, y;

        protected SimpleBuilderXY() { }
        protected SimpleBuilderXY(int x0, int y0) {
            x = x0;
            y = y0;
        }

        public SimpleBuilderXY withX(int x0) {
            this.x = x0;
            return this;
        }

        public SimpleBuilderXY withY(int y0) {
            this.y = y0;
            return this;
        }

        public ValueClassXY build() {
            return new ValueClassXY(x, y);
        }
    }

    @JsonDeserialize(builder=CreatorBuilder.class)
    @JsonFormat(shape=JsonFormat.Shape.ARRAY)
    @JsonPropertyOrder(alphabetic=true)
    static class CreatorValue
    {
        final int a, b, c;

        protected CreatorValue(int a, int b, int c) {
            this.a = a;
            this.b = b;
            this.c = c;
        }
    }

    @JsonFormat(shape=JsonFormat.Shape.ARRAY)
    static class CreatorBuilder {
        private final int a, b;

        private int c;

        @JsonCreator
        public CreatorBuilder(@JsonProperty("a") int a,
                @JsonProperty("b") int b)
        {
            this.a = a;
            this.b = b;
        }

        @JsonView(String.class)
        public CreatorBuilder withC(int v) {
            c = v;
            return this;
        }

        public CreatorValue build() {
            return new CreatorValue(a, b, c);
        }
    }

    // // // Inner types for roundtrip tests

    @JsonFormat(shape=JsonFormat.Shape.ARRAY)
    @JsonPropertyOrder({"content", "images"})
    static class MediaItemAsArray
    {
        public enum Player { JAVA, FLASH;  }
        public enum Size { SMALL, LARGE; }

        private List<Photo> _photos;
        private Content _content;

        public MediaItemAsArray() { }

        protected MediaItemAsArray(Content c)
        {
            _content = c;
        }

        public void addPhoto(Photo p) {
            if (_photos == null) {
                _photos = new ArrayList<Photo>();
            }
            _photos.add(p);
        }

        public List<Photo> getImages() { return _photos; }
        public void setImages(List<Photo> p) { _photos = p; }

        public Content getContent() { return _content; }
        public void setContent(Content c) { _content = c; }

        @JsonFormat(shape=JsonFormat.Shape.ARRAY)
        @JsonPropertyOrder({"uri","title","width","height","size"})
        static class Photo
        {
            private String _uri;
            private String _title;
            private int _width;
            private int _height;
            private Size _size;

            public Photo() {}
            protected Photo(String uri, String title, int w, int h, Size s)
            {
              _uri = uri;
              _title = title;
              _width = w;
              _height = h;
              _size = s;
            }

          public String getUri() { return _uri; }
          public String getTitle() { return _title; }
          public int getWidth() { return _width; }
          public int getHeight() { return _height; }
          public Size getSize() { return _size; }

          public void setUri(String u) { _uri = u; }
          public void setTitle(String t) { _title = t; }
          public void setWidth(int w) { _width = w; }
          public void setHeight(int h) { _height = h; }
          public void setSize(Size s) { _size = s; }
        }

        @JsonFormat(shape=JsonFormat.Shape.ARRAY)
        @JsonPropertyOrder({"uri","title","width","height","format","duration","size","bitrate","persons","player","copyright"})
        public static class Content
        {
            private Player _player;
            private String _uri;
            private String _title;
            private int _width;
            private int _height;
            private String _format;
            private long _duration;
            private long _size;
            private int _bitrate;
            private List<String> _persons;
            private String _copyright;

            public Content() { }

            public void addPerson(String p) {
                if (_persons == null) {
                    _persons = new ArrayList<>();
                }
                _persons.add(p);
            }

            public Player getPlayer() { return _player; }
            public String getUri() { return _uri; }
            public String getTitle() { return _title; }
            public int getWidth() { return _width; }
            public int getHeight() { return _height; }
            public String getFormat() { return _format; }
            public long getDuration() { return _duration; }
            public long getSize() { return _size; }
            public int getBitrate() { return _bitrate; }
            public List<String> getPersons() { return _persons; }
            public String getCopyright() { return _copyright; }

            public void setPlayer(Player p) { _player = p; }
            public void setUri(String u) {  _uri = u; }
            public void setTitle(String t) {  _title = t; }
            public void setWidth(int w) {  _width = w; }
            public void setHeight(int h) {  _height = h; }
            public void setFormat(String f) {  _format = f;  }
            public void setDuration(long d) {  _duration = d; }
            public void setSize(long s) {  _size = s; }
            public void setBitrate(int b) {  _bitrate = b; }
            public void setPersons(List<String> p) {  _persons = p; }
            public void setCopyright(String c) {  _copyright = c; }
        }
    }

    /*
    /*****************************************************
    /* Mapper instances
    /*****************************************************
     */

    private final static ObjectMapper MAPPER = newJsonMapper();

    // 06-Jan-2025, tatu: NOTE! need to make sure Default View Inclusion
    //   is enabled for view tests to work as expected
    private final static ObjectMapper MAPPER_WITH_VIEWS = jsonMapperBuilder()
            .enable(MapperFeature.DEFAULT_VIEW_INCLUSION)
            .build();

    /*
    /*****************************************************
    /* Basic tests
    /*****************************************************
     */

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

    @Test
    public void testMedaItemRoundtrip() throws Exception
    {
        MediaItemAsArray.Content c = new MediaItemAsArray.Content();
        c.setBitrate(9600);
        c.setCopyright("none");
        c.setDuration(360000L);
        c.setFormat("lzf");
        c.setHeight(640);
        c.setSize(128000L);
        c.setTitle("Amazing Stuff For Something Or Oth\u00CBr!");
        c.setUri("http://multi.fario.us/index.html");
        c.setWidth(1400);

        c.addPerson("Joe Sixp\u00e2ck");
        c.addPerson("Ezekiel");
        c.addPerson("Sponge-Bob Squarepant\u00DF");

        MediaItemAsArray input = new MediaItemAsArray(c);
        input.addPhoto(new MediaItemAsArray.Photo());
        input.addPhoto(new MediaItemAsArray.Photo());
        input.addPhoto(new MediaItemAsArray.Photo());

        String json = MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(input);

        MediaItemAsArray output = MAPPER.readValue(new java.io.StringReader(json), MediaItemAsArray.class);
        assertNotNull(output);

        assertNotNull(output.getImages());
        assertEquals(input.getImages().size(), output.getImages().size());
        assertNotNull(output.getContent());
        assertEquals(input.getContent().getTitle(), output.getContent().getTitle());
        assertEquals(input.getContent().getUri(), output.getContent().getUri());

        // compare re-serialization as a simple check as well
        assertEquals(json, MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(output));
    }

    /*
    /*****************************************************
    /* Tests with views and creators
    /*****************************************************
     */

    @Test
    public void testWithView() throws Exception
    {
        // Ok, first, ensure that serializer will "black out" filtered properties
        AsArrayWithView input = new AsArrayWithView();
        input.a = 1;
        input.b = 2;
        input.c = 3;
        String json = MAPPER_WITH_VIEWS.writerWithView(ViewA.class).writeValueAsString(input);
        assertEquals("[1,null,3]", json);

        // and then that conversely deserializer does something similar
        AsArrayWithView result = MAPPER_WITH_VIEWS.readerFor(AsArrayWithView.class).withView(ViewB.class)
                .readValue("[1,2,3]");
        // should include 'c' (not view-able) and 'b' (include in ViewB) but not 'a'
        assertEquals(3, result.c);
        assertEquals(2, result.b);
        assertEquals(0, result.a);
    }

    @Test
    public void testWithViewAndCreator() throws Exception
    {
        AsArrayWithViewAndCreator result = MAPPER_WITH_VIEWS.readerFor(AsArrayWithViewAndCreator.class)
                .withView(ViewB.class)
                .without(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES)
                .readValue("[1,2,3]");
        // should include 'c' (not view-able) and 'b' (include in ViewB) but not 'a'
        assertEquals(3, result.c);
        assertEquals(2, result.b);
        assertEquals(0, result.a);
    }

    @Test
    public void testWithCreatorsOrdered() throws Exception
    {
        CreatorAsArray input = new CreatorAsArray(3, 4);
        input.a = 1;
        input.b = 2;

        // note: Creator properties get sorted ahead of others, hence not [1,2,3,4] but:
        String json = MAPPER_WITH_VIEWS.writeValueAsString(input);
        assertEquals("[3,4,1,2]", json);

        // and should get back in proper order, too
        CreatorAsArray output = MAPPER_WITH_VIEWS.readValue(json, CreatorAsArray.class);
        assertEquals(1, output.a);
        assertEquals(2, output.b);
        assertEquals(3, output.x);
        assertEquals(4, output.y);
    }

    // Same as above, but ordering of properties different...
    @Test
    public void testWithCreatorsShuffled() throws Exception
    {
        CreatorAsArrayShuffled input = new CreatorAsArrayShuffled(3, 4);
        input.a = 1;
        input.b = 2;

        // note: explicit ordering overrides implicit creators-first ordering:
        String json = MAPPER_WITH_VIEWS.writeValueAsString(input);
        assertEquals("[1,2,3,4]", json);

        // and should get back in proper order, too
        CreatorAsArrayShuffled output = MAPPER_WITH_VIEWS.readValue(json, CreatorAsArrayShuffled.class);
        assertEquals(1, output.a);
        assertEquals(2, output.b);
        assertEquals(3, output.x);
        assertEquals(4, output.y);
    }

    /*
    /*****************************************************
    /* Tests with Builder pattern
    /*****************************************************
     */

    @Test
    public void testSimpleBuilder() throws Exception
    {
        ValueClassXY value = MAPPER.readValue("[1,2]", ValueClassXY.class);
        assertEquals(2, value._x);
        assertEquals(3, value._y);
    }

    // Won't work, but verify exception
    @Test
    public void testBuilderWithUpdate() throws Exception
    {
        try {
            /*value =*/ MAPPER.readerFor(ValueClassXY.class)
                    .withValueToUpdate(new ValueClassXY(6, 7))
                    .readValue("[1,2]");
            fail("Should not pass");
        } catch (InvalidDefinitionException e) {
            verifyException(e, "Deserialization of");
            verifyException(e, "by passing existing instance");
            verifyException(e, "ValueClassXY");
        }
    }

    // test to ensure @JsonCreator also works with builder
    @Test
    public void testWithCreator() throws Exception
    {
        ObjectReader r = MAPPER.readerFor(CreatorValue.class)
                .without(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES);

        CreatorValue value = r.readValue("[1,2,3]");
        assertEquals(1, value.a);
        assertEquals(2, value.b);
        assertEquals(3, value.c);

        // and should be ok with partial too?
        value = r.readValue("[1,2]");
        assertEquals(1, value.a);
        assertEquals(2, value.b);
        assertEquals(0, value.c);

        value = r.readValue("[1]");
        assertEquals(1, value.a);
        assertEquals(0, value.b);
        assertEquals(0, value.c);

        value = r.readValue("[]");
        assertEquals(0, value.a);
        assertEquals(0, value.b);
        assertEquals(0, value.c);
    }

    @Test
    public void testWithCreatorAndView() throws Exception
    {
        ObjectReader reader = MAPPER_WITH_VIEWS.readerFor(CreatorValue.class);

        CreatorValue value;

        // First including values in view
        value = reader.withView(String.class).readValue("[1,2,3]");
        assertEquals(1, value.a);
        assertEquals(2, value.b);
        assertEquals(3, value.c);

        // then not including view
        value = reader.withView(Character.class).readValue("[1,2,3]");
        assertEquals(1, value.a);
        assertEquals(2, value.b);
        assertEquals(0, value.c);
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

    @Test
    public void testBuilderUnknownExtraProp() throws Exception
    {
        String json = "[1, 2, 3, 4]";
        try {
            MAPPER.readerFor(ValueClassXY.class)
                    .with(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                    .readValue(json);
            fail("should not pass with extra element");
        } catch (MismatchedInputException e) {
            // Looks like we get either "Unexpected JSON values" or "Unexpected JSON value(s)"
            verifyException(e, "Unexpected JSON value");
        }

        // but actually fine if skip-unknown set
        ValueClassXY v = MAPPER.readerFor(ValueClassXY.class)
                .without(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .readValue(json);
        assertNotNull(v);
        // note: +1 for both so
        assertEquals(v._x, 2);
        assertEquals(v._y, 3);
    }
}
