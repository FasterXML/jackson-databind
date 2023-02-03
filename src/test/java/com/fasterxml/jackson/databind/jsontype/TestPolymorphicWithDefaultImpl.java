package com.fasterxml.jackson.databind.jsontype;

import java.util.*;

import com.fasterxml.jackson.annotation.*;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.annotation.NoClass;
import com.fasterxml.jackson.databind.exc.InvalidTypeIdException;

/**
 * Unit tests related to specialized handling of "default implementation"
 * ({@link JsonTypeInfo#defaultImpl}), as well as related
 * cases that allow non-default settings (such as missing type id).
 */
public class TestPolymorphicWithDefaultImpl extends BaseMapTest
{
    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type", defaultImpl = LegacyInter.class)
    @JsonSubTypes(value = {@JsonSubTypes.Type(name = "mine", value = MyInter.class)})
    public static interface Inter { }

    public static class MyInter implements Inter {
        @JsonProperty("blah") public List<String> blah;
    }

    public static class LegacyInter extends MyInter
    {
        @JsonCreator
        LegacyInter(Object obj)
        {
            if (obj instanceof List) {
                blah = new ArrayList<String>();
                for (Object o : (List<?>) obj) {
                    blah.add(o.toString());
                }
            }
            else if (obj instanceof String) {
                blah = Arrays.asList(((String) obj).split(","));
            }
            else {
                throw new IllegalArgumentException("Unknown type: " + obj.getClass());
            }
        }
    }

    /**
     * Note: <code>NoClass</code> here has special meaning, of mapping invalid
     * types into null instances.
     */
    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type",
            defaultImpl = NoClass.class)
    public static class DefaultWithNoClass { }

    /**
     * Also another variant to verify that from 2.5 on, can use non-deprecated
     * value for the same.
     */
    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type",
            defaultImpl = Void.class)
    public static class DefaultWithVoidAsDefault { }

    // and then one with no defaultImpl nor listed subtypes
    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
    abstract static class MysteryPolymorphic { }

    // [databind#511] types

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME,
            include = JsonTypeInfo.As.WRAPPER_OBJECT)
    @JsonSubTypes(@JsonSubTypes.Type(name="sub1", value = BadSub1.class))
    public static class BadItem {}

    public static class BadSub1 extends BadItem {
        public String a ;
    }

    public static class Good {
        public List<GoodItem> many;
    }

    public static class Bad {
        public List<BadItem> many;
    }

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME,
            include = JsonTypeInfo.As.WRAPPER_OBJECT)
    @JsonSubTypes({@JsonSubTypes.Type(name="sub1", value = GoodSub1.class),
            @JsonSubTypes.Type(name="sub2", value = GoodSub2.class) })
    public static class GoodItem {}

    public static class GoodSub1 extends GoodItem {
        public String a;
    }
    public static class GoodSub2 extends GoodItem {
        public String b;

    }

    // for [databind#656]
    @JsonTypeInfo(use=JsonTypeInfo.Id.NAME, include= JsonTypeInfo.As.WRAPPER_OBJECT, defaultImpl=ImplFor656.class)
    static abstract class BaseFor656 { }

    static class ImplFor656 extends BaseFor656 {
        public int a;
    }

    static class CallRecord {
        public float version;
        public String application;
        public Item item;
        public Item item2;
        public CallRecord() {}
    }

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type", visible = true)
    @JsonSubTypes({@JsonSubTypes.Type(value = Event.class, name = "event")})
    @JsonIgnoreProperties(ignoreUnknown=true)
    public interface Item { }

    static class Event implements Item {
        public String location;
        public Event() {}
    }

    @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY,
            property = "clazz")
    abstract static class BaseClass { }

    static class BaseWrapper {
        public BaseClass value;
    }

    // [databind#1533]
    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY,
            property = "type")
    static class AsProperty {
    }

    static class AsPropertyWrapper {
        public AsProperty value;
    }

    /*
    /**********************************************************
    /* Unit tests, deserialization
    /**********************************************************
     */

    private final ObjectMapper MAPPER = new ObjectMapper();

    public void testDeserializationWithObject() throws Exception
    {
        Inter inter = MAPPER.readerFor(Inter.class).readValue("{\"type\": \"mine\", \"blah\": [\"a\", \"b\", \"c\"]}");
        assertTrue(inter instanceof MyInter);
        assertFalse(inter instanceof LegacyInter);
        assertEquals(Arrays.asList("a", "b", "c"), ((MyInter) inter).blah);
    }

    public void testDeserializationWithString() throws Exception
    {
        Inter inter = MAPPER.readerFor(Inter.class).readValue("\"a,b,c,d\"");
        assertTrue(inter instanceof LegacyInter);
        assertEquals(Arrays.asList("a", "b", "c", "d"), ((MyInter) inter).blah);
    }

    public void testDeserializationWithArray() throws Exception
    {
        Inter inter = MAPPER.readerFor(Inter.class).readValue("[\"a\", \"b\", \"c\", \"d\"]");
        assertTrue(inter instanceof LegacyInter);
        assertEquals(Arrays.asList("a", "b", "c", "d"), ((MyInter) inter).blah);
    }

    public void testDeserializationWithArrayOfSize2() throws Exception
    {
        Inter inter = MAPPER.readerFor(Inter.class).readValue("[\"a\", \"b\"]");
        assertTrue(inter instanceof LegacyInter);
        assertEquals(Arrays.asList("a", "b"), ((MyInter) inter).blah);
    }

    // [databind#148]
    public void testDefaultAsNoClass() throws Exception
    {
        Object ob = MAPPER.readerFor(DefaultWithNoClass.class).readValue("{ }");
        assertNull(ob);
        ob = MAPPER.readerFor(DefaultWithNoClass.class).readValue("{ \"bogus\":3 }");
        assertNull(ob);
    }

    // same, with 2.5 and Void.class
    public void testDefaultAsVoid() throws Exception
    {
        Object ob = MAPPER.readerFor(DefaultWithVoidAsDefault.class).readValue("{ }");
        assertNull(ob);
        ob = MAPPER.readerFor(DefaultWithVoidAsDefault.class).readValue("{ \"bogus\":3 }");
        assertNull(ob);
    }

    // [databind#148]
    public void testBadTypeAsNull() throws Exception
    {
        ObjectReader r = MAPPER.readerFor(MysteryPolymorphic.class)
                .without(DeserializationFeature.FAIL_ON_INVALID_SUBTYPE);
        Object ob = r.readValue("{}");
        assertNull(ob);
        ob = r.readValue("{ \"whatever\":13}");
        assertNull(ob);
    }

    // [databind#511]
    public void testInvalidTypeId511() throws Exception {
        ObjectReader reader = MAPPER.reader().without(
                DeserializationFeature.FAIL_ON_INVALID_SUBTYPE,
                DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES,
                DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES
        );
        String json = "{\"many\":[{\"sub1\":{\"a\":\"foo\"}},{\"sub2\":{\"b\":\"bar\"}}]}" ;
        Good goodResult = reader.forType(Good.class).readValue(json) ;
        assertNotNull(goodResult) ;
        Bad badResult = reader.forType(Bad.class).readValue(json);
        assertNotNull(badResult);
    }

    // [databind#656]
    public void testDefaultImplWithObjectWrapper() throws Exception
    {
        BaseFor656 value = MAPPER.readValue(a2q("{'foobar':{'a':3}}"), BaseFor656.class);
        assertNotNull(value);
        assertEquals(ImplFor656.class, value.getClass());
        assertEquals(3, ((ImplFor656) value).a);
    }

    public void testUnknownTypeIDRecovery() throws Exception
    {
        ObjectReader reader = MAPPER.readerFor(CallRecord.class).without(
                DeserializationFeature.FAIL_ON_INVALID_SUBTYPE);
        String json = a2q("{'version':0.0,'application':'123',"
                +"'item':{'type':'xevent','location':'location1'},"
                +"'item2':{'type':'event','location':'location1'}}");
        // can't read item2 - which is valid
        CallRecord r = reader.readValue(json);
        assertNull(r.item);
        assertNotNull(r.item2);

        json = a2q("{'item':{'type':'xevent','location':'location1'}, 'version':0.0,'application':'123'}");
        CallRecord r3 = reader.readValue(json);
        assertNull(r3.item);
        assertEquals("123", r3.application);
    }

    public void testUnknownClassAsSubtype() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_INVALID_SUBTYPE, false);
        BaseWrapper w = mapper.readValue(a2q
                ("{'value':{'clazz':'com.foobar.Nothing'}}'"),
                BaseWrapper.class);
        assertNotNull(w);
        assertNull(w.value);
    }

    public void testWithoutEmptyStringAsNullObject1533() throws Exception
    {
        ObjectReader r = MAPPER.readerFor(AsPropertyWrapper.class)
                .without(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT);
        try {
            r.readValue("{ \"value\": \"\" }");
            fail("Expected InvalidTypeIdException");
        } catch (InvalidTypeIdException e) {
            verifyException(e, "missing type id property 'type'");
        }
    }

    // [databind#1533]
    public void testWithEmptyStringAsNullObject1533() throws Exception
    {
        ObjectReader r = MAPPER.readerFor(AsPropertyWrapper.class)
                .with(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT);
        AsPropertyWrapper wrapper = r.readValue("{ \"value\": \"\" }");
        assertNull(wrapper.value);
    }

    /*
    /**********************************************************
    /* Unit tests, serialization
    /**********************************************************
     */

    /*
    public void testDontWriteIfDefaultImpl() throws Exception {
        String json = MAPPER.writeValueAsString(new MyInter());
        assertEquals("{\"blah\":null}", json);
    }
    */
}
