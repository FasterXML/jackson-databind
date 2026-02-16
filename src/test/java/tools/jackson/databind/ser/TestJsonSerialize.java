package tools.jackson.databind.ser;

import java.io.IOException;
import java.util.*;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;

import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.*;
import tools.jackson.databind.annotation.JsonSerialize;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.ser.std.NullSerializer;
import tools.jackson.databind.ser.std.StdSerializer;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.*;

/**
 * This unit test suite tests use of {@link JsonSerialize} annotation
 * with bean serialization.
 */
@SuppressWarnings("serial")
public class TestJsonSerialize
    extends DatabindTestUtil
{
    /*
    /**********************************************************
    /* Helper types for basic @JsonSerialize tests
    /**********************************************************
     */

    interface ValueInterface {
        public int getX();
    }

    static class ValueClass
        implements ValueInterface
    {
        @Override
        public int getX() { return 3; }
        public int getY() { return 5; }
    }

    /**
     * Test class to verify that <code>JsonSerialize.as</code>
     * works as expected
     */
    static class WrapperClassForAs
    {
        @JsonSerialize(as=ValueInterface.class)
        public ValueClass getValue() {
            return new ValueClass();
        }
    }

    // This should indicate that static type be used for all fields
    @JsonSerialize(typing=JsonSerialize.Typing.STATIC)
    static class WrapperClassForStaticTyping
    {
        public ValueInterface getValue() {
            return new ValueClass();
        }
    }

    static class WrapperClassForStaticTyping2
    {
        @JsonSerialize(typing=JsonSerialize.Typing.STATIC)
        public ValueInterface getStaticValue() {
            return new ValueClass();
        }

        @JsonSerialize(typing=JsonSerialize.Typing.DYNAMIC)
        public ValueInterface getDynamicValue() {
            return new ValueClass();
        }
    }

    /**
     * Test bean that has an invalid {@link JsonSerialize} annotation.
     */
    static class BrokenClass
    {
        // invalid annotation: String not a supertype of Long
        @JsonSerialize(as=String.class)
        public Long getValue() {
            return Long.valueOf(4L);
        }
    }

    static class ValueMap extends HashMap<String,ValueInterface> { }
    static class ValueList extends ArrayList<ValueInterface> { }
    static class ValueLinkedList extends LinkedList<ValueInterface> { }

    static class Foo294
    {
        @JsonProperty private String id;
        @JsonSerialize(using = Bar294Serializer.class)
        private Bar294 bar;

        public Foo294() { }
        protected Foo294(String id, String id2) {
            this.id = id;
            bar = new Bar294(id2);
        }
    }

    static class Bar294{
        @JsonProperty protected String id;
        @JsonProperty protected String name;

        public Bar294() { }
        public Bar294(String id) {
            this.id = id;
        }

        public String getId() { return id; }
        public String getName() { return name; }
    }

    static class Bar294Serializer extends StdSerializer<Bar294>
    {
        public Bar294Serializer() { super(Bar294.class); }
        @Override
        public void serialize(Bar294 bar, JsonGenerator g,
            SerializationContext provider)
        {
            g.writeString(bar.id);
        }
    }

    @JsonPropertyOrder({ "a", "something" })
    static class Response {
        public String a = "x";

        @JsonProperty   //does not show up
        public boolean isSomething() { return true; }
    }

    /*
    /**********************************************************
    /* Helper types for contentAs/contentUsing/keyUsing tests
    /**********************************************************
     */

    static class SimpleKey {
        protected final String key;

        public SimpleKey(String str) { key = str; }

        @Override public String toString() { return "toString:"+key; }
    }

    static class SimpleValue {
        public final String value;

        public SimpleValue(String str) { value = str; }
    }

    @JsonPropertyOrder({"value", "value2"})
    static class ActualValue extends SimpleValue
    {
        public final String other = "123";

        public ActualValue(String str) { super(str); }
    }

    static class SimpleKeySerializer extends ValueSerializer<SimpleKey> {
        @Override
        public void serialize(SimpleKey key, JsonGenerator g, SerializationContext provider)
        {
            g.writeName("key "+key.key);
        }
    }

    static class SimpleValueSerializer extends ValueSerializer<SimpleValue> {
        @Override
        public void serialize(SimpleValue value, JsonGenerator g, SerializationContext provider)
        {
            g.writeString("value "+value.value);
        }
    }

    @JsonSerialize(contentAs=SimpleValue.class)
    static class SimpleValueList extends ArrayList<ActualValue> { }

    @JsonSerialize(contentAs=SimpleValue.class)
    static class SimpleValueMap extends HashMap<SimpleKey, ActualValue> { }

    @JsonSerialize(contentUsing=SimpleValueSerializer.class)
    static class SimpleValueListWithSerializer extends ArrayList<ActualValue> { }

    @JsonSerialize(keyUsing=SimpleKeySerializer.class, contentUsing=SimpleValueSerializer.class)
    static class SimpleValueMapWithSerializer extends HashMap<SimpleKey, ActualValue> { }

    static class ListWrapperSimple
    {
        @JsonSerialize(contentAs=SimpleValue.class)
        public final ArrayList<ActualValue> values = new ArrayList<ActualValue>();

        public ListWrapperSimple(String value) {
            values.add(new ActualValue(value));
        }
    }

    static class ListWrapperWithSerializer
    {
        @JsonSerialize(contentUsing=SimpleValueSerializer.class)
        public final ArrayList<ActualValue> values = new ArrayList<ActualValue>();

        public ListWrapperWithSerializer(String value) {
            values.add(new ActualValue(value));
        }
    }

    static class MapWrapperSimple
    {
        @JsonSerialize(contentAs=SimpleValue.class)
        public final HashMap<SimpleKey, ActualValue> values = new HashMap<SimpleKey, ActualValue>();

        public MapWrapperSimple(String key, String value) {
            values.put(new SimpleKey(key), new ActualValue(value));
        }
    }

    static class MapWrapperWithSerializer
    {
        @JsonSerialize(keyUsing=SimpleKeySerializer.class, contentUsing=SimpleValueSerializer.class)
        public final HashMap<SimpleKey, ActualValue> values = new HashMap<SimpleKey, ActualValue>();

        public MapWrapperWithSerializer(String key, String value) {
            values.put(new SimpleKey(key), new ActualValue(value));
        }
    }

    static class NullBean
    {
        @JsonSerialize(using=NullSerializer.class)
        public String value = "abc";
    }

    /*
    /**********************************************************
    /* Helper types for contentUsing on list elements
    /**********************************************************
     */

    // [JACKSON-829]
    static class FooToBarSerializer extends ValueSerializer<String> {
        @Override
        public void serialize(String value, JsonGenerator g, SerializationContext provider)
        {
            if ("foo".equals(value)) {
                g.writeString("bar");
            } else {
                g.writeString(value);
            }
        }
    }

    static class MyObject {
        @JsonSerialize(contentUsing = FooToBarSerializer.class)
        List<String> list;
    }

    /*
    /**********************************************************
    /* Test methods, basic @JsonSerialize
    /**********************************************************
     */

    final ObjectMapper MAPPER = newJsonMapper();

    private final ObjectMapper STATIC_MAPPER = jsonMapperBuilder()
            .enable(MapperFeature.USE_STATIC_TYPING)
            .build();

    @SuppressWarnings("unchecked")
    @Test
    public void testSimpleValueDefinition() throws Exception
    {
        Map<String,Object> result = writeAndMap(MAPPER, new WrapperClassForAs());
        assertEquals(1, result.size());
        Object ob = result.get("value");
        // Should see only "x", not "y"
        result = (Map<String,Object>) ob;
        assertEquals(1, result.size());
        assertEquals(Integer.valueOf(3), result.get("x"));
    }

    @Test
    public void testBrokenAnnotation() throws Exception
    {
        try {
            MAPPER.writeValueAsString(new BrokenClass());
            fail("Should not succeed");
        } catch (Exception e) {
            verifyException(e, "types not related");
        }
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testStaticTypingForClass() throws Exception
    {
        Map<String,Object> result = writeAndMap(MAPPER, new WrapperClassForStaticTyping());
        assertEquals(1, result.size());
        Object ob = result.get("value");
        // Should see only "x", not "y"
        result = (Map<String,Object>) ob;
        assertEquals(1, result.size());
        assertEquals(Integer.valueOf(3), result.get("x"));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testMixedTypingForClass() throws Exception
    {
        Map<String,Object> result = writeAndMap(MAPPER, new WrapperClassForStaticTyping2());
        assertEquals(2, result.size());

        Object obStatic = result.get("staticValue");
        // Should see only "x", not "y"
        Map<String,Object> stat = (Map<String,Object>) obStatic;
        assertEquals(1, stat.size());
        assertEquals(Integer.valueOf(3), stat.get("x"));

        Object obDynamic = result.get("dynamicValue");
        // Should see both
        Map<String,Object> dyn = (Map<String,Object>) obDynamic;
        assertEquals(2, dyn.size());
        assertEquals(Integer.valueOf(3), dyn.get("x"));
        assertEquals(Integer.valueOf(5), dyn.get("y"));
    }

    @Test
    public void testStaticTypingWithMap() throws Exception
    {
        ValueMap map = new ValueMap();
        map.put("a", new ValueClass());
        assertEquals("{\"a\":{\"x\":3}}", STATIC_MAPPER.writeValueAsString(map));
    }

    @Test
    public void testStaticTypingWithArrayList() throws Exception
    {
        ValueList list = new ValueList();
        list.add(new ValueClass());
        assertEquals("[{\"x\":3}]", STATIC_MAPPER.writeValueAsString(list));
    }

    @Test
    public void testStaticTypingWithLinkedList() throws Exception
    {
        ValueLinkedList list = new ValueLinkedList();
        list.add(new ValueClass());
        assertEquals("[{\"x\":3}]", STATIC_MAPPER.writeValueAsString(list));
    }

    @Test
    public void testStaticTypingWithArray() throws Exception
    {
        ValueInterface[] array = new ValueInterface[] { new ValueClass() };
        assertEquals("[{\"x\":3}]", STATIC_MAPPER.writeValueAsString(array));
    }

    @Test
    public void testIssue294() throws Exception
    {
        JsonMapper mapper = JsonMapper.builder().enable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY).build();
        assertEquals("{\"bar\":\"barId\",\"id\":\"fooId\"}",
                mapper.writeValueAsString(new Foo294("fooId", "barId")));
    }

    @Test
    public void testWithIsGetter() throws Exception
    {
        ObjectMapper mapper = jsonMapperBuilder()
                .changeDefaultVisibility(vc -> vc
                        .withVisibility(PropertyAccessor.GETTER, Visibility.NONE)
                        .withVisibility(PropertyAccessor.FIELD, Visibility.ANY)
                        .withVisibility(PropertyAccessor.CREATOR, Visibility.NONE)
                        .withVisibility(PropertyAccessor.IS_GETTER, Visibility.NONE)
                        .withVisibility(PropertyAccessor.SETTER, Visibility.NONE))
                .build();
        assertEquals(a2q("{'a':'x','something':true}"),
                mapper.writeValueAsString(new Response()));
    }

    /*
    /**********************************************************
    /* Test methods, contentAs/contentUsing/keyUsing
    /**********************************************************
     */

    // test value annotation applied to List value class
    @Test
    public void testSerializedAsListWithClassAnnotations() throws IOException
    {
        SimpleValueList list = new SimpleValueList();
        list.add(new ActualValue("foo"));
        assertEquals("[{\"value\":\"foo\"}]", MAPPER.writeValueAsString(list));
    }

    // test value annotation applied to Map value class
    @Test
    public void testSerializedAsMapWithClassAnnotations() throws IOException
    {
        SimpleValueMap map = new SimpleValueMap();
        map.put(new SimpleKey("x"), new ActualValue("y"));
        assertEquals("{\"toString:x\":{\"value\":\"y\"}}", MAPPER.writeValueAsString(map));
    }

    // test Serialization annotation with List
    @Test
    public void testSerializedAsListWithClassSerializer() throws IOException
    {
        SimpleValueListWithSerializer list = new SimpleValueListWithSerializer();
        list.add(new ActualValue("foo"));
        assertEquals("[\"value foo\"]", MAPPER.writeValueAsString(list));
    }

    @Test
    public void testSerializedAsListWithPropertyAnnotations() throws IOException
    {
        ListWrapperSimple input = new ListWrapperSimple("bar");
        assertEquals("{\"values\":[{\"value\":\"bar\"}]}", MAPPER.writeValueAsString(input));
    }

    @Test
    public void testSerializedAsMapWithClassSerializer() throws IOException
    {
        SimpleValueMapWithSerializer map = new SimpleValueMapWithSerializer();
        map.put(new SimpleKey("abc"), new ActualValue("123"));
        assertEquals("{\"key abc\":\"value 123\"}", MAPPER.writeValueAsString(map));
    }

    @Test
    public void testSerializedAsMapWithPropertyAnnotations() throws IOException
    {
        MapWrapperSimple input = new MapWrapperSimple("a", "b");
        assertEquals("{\"values\":{\"toString:a\":{\"value\":\"b\"}}}",
                MAPPER.writeValueAsString(input));
    }

    @Test
    public void testSerializedAsListWithPropertyAnnotations2() throws IOException
    {
        ListWrapperWithSerializer input = new ListWrapperWithSerializer("abc");
        assertEquals("{\"values\":[\"value abc\"]}", MAPPER.writeValueAsString(input));
    }

    @Test
    public void testSerializedAsMapWithPropertyAnnotations2() throws IOException
    {
        MapWrapperWithSerializer input = new MapWrapperWithSerializer("foo", "b");
        assertEquals("{\"values\":{\"key foo\":\"value b\"}}", MAPPER.writeValueAsString(input));
    }

    @Test
    public void testEmptyInclusionContainers() throws IOException
    {
        ObjectMapper defMapper = MAPPER;
        ObjectMapper inclMapper = jsonMapperBuilder()
                .changeDefaultPropertyInclusion(incl -> incl.withValueInclusion(JsonInclude.Include.NON_EMPTY))
                .build();

        ListWrapper<String> list = new ListWrapper<String>();
        assertEquals("{\"list\":[]}", defMapper.writeValueAsString(list));
        assertEquals("{}", inclMapper.writeValueAsString(list));
        assertEquals("{}", inclMapper.writeValueAsString(new ListWrapper<String>()));

        MapWrapper<String,Integer> map = new MapWrapper<String,Integer>(new HashMap<String,Integer>());
        assertEquals("{\"map\":{}}", defMapper.writeValueAsString(map));
        assertEquals("{}", inclMapper.writeValueAsString(map));
        assertEquals("{}", inclMapper.writeValueAsString(new MapWrapper<String,Integer>(null)));

        ArrayWrapper<Integer> array = new ArrayWrapper<Integer>(new Integer[0]);
        assertEquals("{\"array\":[]}", defMapper.writeValueAsString(array));
        assertEquals("{}", inclMapper.writeValueAsString(array));
        assertEquals("{}", inclMapper.writeValueAsString(new ArrayWrapper<Integer>(null)));
    }

    @Test
    public void testNullSerializer() throws Exception
    {
        assertEquals("{\"value\":null}", MAPPER.writeValueAsString(new NullBean()));
    }

    /*
    /**********************************************************
    /* Test methods, contentUsing on list elements
    /**********************************************************
     */

    @Test
    public void testCustomContentSerializer() throws Exception
    {
        MyObject object = new MyObject();
        object.list = Arrays.asList("foo");
        String json = MAPPER.writeValueAsString(object);
        assertEquals("{\"list\":[\"bar\"]}", json);
    }
}
