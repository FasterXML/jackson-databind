package com.fasterxml.jackson.databind.ser;

import java.util.*;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import tools.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

/**
 * This unit test suite tests use of @JsonClass Annotation
 * with bean serialization.
 */
@SuppressWarnings("serial")
public class TestJsonSerialize
    extends BaseMapTest
{
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
        public Foo294(String id, String id2) {
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
            SerializerProvider provider)
        {
            g.writeString(bar.id);
        }
    }

    /*
    /**********************************************************
    /* Main tests
    /**********************************************************
     */

    final ObjectMapper MAPPER = objectMapper();

    private final ObjectMapper STATIC_MAPPER = jsonMapperBuilder()
            .enable(MapperFeature.USE_STATIC_TYPING)
            .build();
    
    @SuppressWarnings("unchecked")
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

    public void testStaticTypingWithMap() throws Exception
    {
        ValueMap map = new ValueMap();
        map.put("a", new ValueClass());
        assertEquals("{\"a\":{\"x\":3}}", STATIC_MAPPER.writeValueAsString(map));
    }

    public void testStaticTypingWithArrayList() throws Exception
    {
        ValueList list = new ValueList();
        list.add(new ValueClass());
        assertEquals("[{\"x\":3}]", STATIC_MAPPER.writeValueAsString(list));
    }

    public void testStaticTypingWithLinkedList() throws Exception
    {
        ValueLinkedList list = new ValueLinkedList();
        list.add(new ValueClass());
        assertEquals("[{\"x\":3}]", STATIC_MAPPER.writeValueAsString(list));
    }
    
    public void testStaticTypingWithArray() throws Exception
    {
        ValueInterface[] array = new ValueInterface[] { new ValueClass() };
        assertEquals("[{\"x\":3}]", STATIC_MAPPER.writeValueAsString(array));
    }

    public void testIssue294() throws Exception
    {
        JsonMapper mapper = JsonMapper.builder().enable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY).build();
        assertEquals("{\"bar\":\"barId\",\"id\":\"fooId\"}",
                mapper.writeValueAsString(new Foo294("fooId", "barId")));
    }

    @JsonPropertyOrder({ "a", "something" })
    static class Response {
        public String a = "x";

        @JsonProperty   //does not show up
        public boolean isSomething() { return true; }
    }

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
}
