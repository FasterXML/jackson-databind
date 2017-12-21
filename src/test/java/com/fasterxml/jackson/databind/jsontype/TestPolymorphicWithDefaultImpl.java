package com.fasterxml.jackson.databind.jsontype;

import java.util.*;

import com.fasterxml.jackson.annotation.*;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.*;
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

    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type", visible = true, defaultImpl = BaseWithDefaultOthers.class)
    @JsonSubTypes(value = {@JsonSubTypes.Type(name = "impl", value = BaseWithDefaultImpl.class)})
    public static abstract class BaseWithDefault {
        public String base;
    }

    public static class BaseWithDefaultImpl extends BaseWithDefault {
        public String impl;
    }

    public static class BaseWithDefaultOthers extends BaseWithDefault {
        public String type;
        public Map<String, Object> fields = new HashMap<>();
        @JsonAnySetter
        public void set(String name, Object value) {
            fields.put(name, value);
        }
    }

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

    public void testBaseWithDefaultAsBase() throws Exception {
        BaseWithDefault value = MAPPER.readerFor(BaseWithDefault.class).readValue("{\"type\": \"impl\", \"base\": \"b\", \"impl\": \"i\"}");
        assertTrue(value instanceof BaseWithDefaultImpl);
        assertEquals("b", ((BaseWithDefaultImpl) value).base);
        assertEquals("i", ((BaseWithDefaultImpl) value).impl);
    }

    public void testBaseWithDefaultAsImpl() throws Exception {
        // Same test as previous but exact class is specified.
        BaseWithDefaultImpl value = MAPPER.readerFor(BaseWithDefaultImpl.class).readValue("{\"type\": \"impl\", \"base\": \"b\", \"impl\": \"i\"}");
        assertTrue(value instanceof BaseWithDefaultImpl);
        assertEquals("b", ((BaseWithDefaultImpl) value).base);
        assertEquals("i", ((BaseWithDefaultImpl) value).impl);
    }

    public void testBaseWithDefaultOtherAsBase() throws Exception {
        BaseWithDefault value = MAPPER.readerFor(BaseWithDefault.class).readValue("{\"type\": \"other\", \"base\": \"b\", \"impl\": \"i\"}");
        assertTrue(value instanceof BaseWithDefaultOthers);
        assertEquals("b", ((BaseWithDefaultOthers) value).base);
        assertEquals("other", ((BaseWithDefaultOthers) value).type);
        assertEquals(1, ((BaseWithDefaultOthers) value).fields.size());
        assertEquals("i", ((BaseWithDefaultOthers) value).fields.get("impl"));
    }

    public void testBaseWithDefaultOtherAsOthers() throws Exception {
        BaseWithDefaultOthers value = MAPPER.readerFor(BaseWithDefaultOthers.class).readValue("{\"type\": \"other\", \"base\": \"b\", \"impl\": \"i\"}");
        assertTrue(value instanceof BaseWithDefaultOthers);
        assertEquals("b", ((BaseWithDefaultOthers) value).base);
        assertEquals("other", ((BaseWithDefaultOthers) value).type);
        assertEquals(1, ((BaseWithDefaultOthers) value).fields.size());
        assertEquals("i", ((BaseWithDefaultOthers) value).fields.get("impl"));
    }

    public void testBaseWithDefaultAsImplIncorrect() throws Exception {
        try {
            MAPPER.readerFor(BaseWithDefaultImpl.class).readValue("{\"type\": \"other\", \"base\": \"b\", \"impl\": \"i\"}");
            fail("Should not parse class with incorrect type");
        } catch (InvalidTypeIdException e) {
            verifyException(e, "Could not resolve type id 'other' as a subtype");
        }
    }

    public void testDeserializationListOfBase() throws Exception {
        List<BaseWithDefault> value = MAPPER.readerFor(new TypeReference<List<BaseWithDefault>>() {})
                .readValue("[{\"type\": \"impl\", \"base\": \"b\", \"impl\": \"i\"}, {\"type\": \"other\", \"base\": \"b\", \"impl\": \"i\"}]");
        assertEquals(2, value.size());
        assertTrue(value.get(0) instanceof BaseWithDefaultImpl);
        assertEquals("b", ((BaseWithDefaultImpl) value.get(0)).base);
        assertEquals("i", ((BaseWithDefaultImpl) value.get(0)).impl);
        assertTrue(value.get(1) instanceof BaseWithDefaultOthers);
        assertEquals("b", ((BaseWithDefaultOthers) value.get(1)).base);
        assertEquals("other", ((BaseWithDefaultOthers) value.get(1)).type);
        assertEquals(1, ((BaseWithDefaultOthers) value.get(1)).fields.size());
        assertEquals("i", ((BaseWithDefaultOthers) value.get(1)).fields.get("impl"));
    }

    public void testDeserializationListOfImpl() throws Exception {
        // Test similar to testBaseWithDefaultAsImpl, but for list.
        List<BaseWithDefaultImpl> value = MAPPER.readerFor(new TypeReference<List<BaseWithDefaultImpl>>() {})
                .readValue("[{\"type\": \"impl\", \"base\": \"b\", \"impl\": \"i\"}, {\"type\": \"impl\", \"base\": \"bb\", \"impl\": \"ii\"}]");
        assertEquals(2, value.size());
        assertTrue(value.get(0) instanceof BaseWithDefaultImpl);
        assertEquals("b", value.get(0).base);
        assertEquals("i", value.get(0).impl);
        assertTrue(value.get(1) instanceof BaseWithDefaultImpl);
        assertEquals("bb", value.get(1).base);
        assertEquals("ii", value.get(1).impl);
    }

    public void testDeserializationCachingOtherFirst() throws Exception {
        ObjectMapper mapper = new ObjectMapper();

        assertParsingDefaultAsBase(mapper);
        assertParsingImplAsBase(mapper);
        assertParsingImplAsImpl(mapper);
        assertErrorParsingDefaultAsImpl(mapper);
    }

    public void testDeserializationCachingBaseFirst() throws Exception {
        ObjectMapper mapper = new ObjectMapper();

        assertParsingImplAsBase(mapper);
        assertParsingDefaultAsBase(mapper);
        assertParsingImplAsImpl(mapper);
        assertErrorParsingDefaultAsImpl(mapper);
    }

    public void testDeserializationCachingImplFirst() throws Exception {
        ObjectMapper mapper = new ObjectMapper();

        assertParsingImplAsImpl(mapper);
        assertParsingImplAsBase(mapper);
        assertParsingDefaultAsBase(mapper);
        assertErrorParsingDefaultAsImpl(mapper);
    }

    private void assertParsingImplAsBase(ObjectMapper mapper) throws java.io.IOException {
        BaseWithDefault valueBaseImpl = mapper.readerFor(BaseWithDefault.class).readValue("{\"type\": \"impl\", \"base\": \"b\", \"impl\": \"i\"}");
        assertTrue(valueBaseImpl instanceof BaseWithDefaultImpl);
        assertEquals("b", ((BaseWithDefaultImpl) valueBaseImpl).base);
        assertEquals("i", ((BaseWithDefaultImpl) valueBaseImpl).impl);
    }

    private void assertParsingImplAsImpl(ObjectMapper mapper) throws java.io.IOException {
        BaseWithDefaultImpl valueImpl = mapper.readerFor(BaseWithDefaultImpl.class).readValue("{\"type\": \"impl\", \"base\": \"b\", \"impl\": \"i\"}");
        assertTrue(valueImpl instanceof BaseWithDefaultImpl);
        assertEquals("b", ((BaseWithDefaultImpl) valueImpl).base);
        assertEquals("i", ((BaseWithDefaultImpl) valueImpl).impl);
    }

    private void assertParsingDefaultAsBase(ObjectMapper mapper) throws java.io.IOException {
        BaseWithDefault valueBase = mapper.readerFor(BaseWithDefault.class).readValue("{\"type\": \"other\", \"base\": \"b\", \"impl\": \"i\"}");
        assertTrue(valueBase instanceof BaseWithDefaultOthers);
        assertEquals("b", ((BaseWithDefaultOthers) valueBase).base);
        assertEquals("other", ((BaseWithDefaultOthers) valueBase).type);
        assertEquals(1, ((BaseWithDefaultOthers) valueBase).fields.size());
        assertEquals("i", ((BaseWithDefaultOthers) valueBase).fields.get("impl"));
    }

    private void assertErrorParsingDefaultAsImpl(ObjectMapper mapper) throws java.io.IOException {
        try {
            mapper.readerFor(BaseWithDefaultImpl.class).readValue("{\"type\": \"other\", \"base\": \"b\", \"impl\": \"i\"}");
            fail("Should not parse class with incorrect type");
        } catch (InvalidTypeIdException e) {
            verifyException(e, "Could not resolve type id 'other' as a subtype");
        }
    }

//    // Seems not to be a bug, however it might be a useful feature to strictly specify deserializing type.
//    public void testBaseWithDefaultAsImplNoTypeInJson() throws Exception {
//        // Class is specified, there is only one implementation.
//        BaseWithDefaultImpl value = MAPPER.readerFor(BaseWithDefaultImpl.class).readValue("{\"base\": \"b\", \"impl\": \"i\"}");
//        assertEquals("b", value.base);
//        assertEquals("i", value.impl);
//    }

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
        ObjectMapper mapper = new ObjectMapper();
        mapper.disable(DeserializationFeature.FAIL_ON_INVALID_SUBTYPE);
        Object ob = mapper.readValue("{}", MysteryPolymorphic.class);
        assertNull(ob);
        ob = mapper.readValue("{ \"whatever\":13}", MysteryPolymorphic.class);
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
        BaseFor656 value = MAPPER.readValue(aposToQuotes("{'foobar':{'a':3}}"), BaseFor656.class);
        assertNotNull(value);
        assertEquals(ImplFor656.class, value.getClass());
        assertEquals(3, ((ImplFor656) value).a);
    }

    public void testUnknownTypeIDRecovery() throws Exception
    {
        ObjectReader reader = MAPPER.readerFor(CallRecord.class).without(
                DeserializationFeature.FAIL_ON_INVALID_SUBTYPE);
        String json = aposToQuotes("{'version':0.0,'application':'123',"
                +"'item':{'type':'xevent','location':'location1'},"
                +"'item2':{'type':'event','location':'location1'}}");
        // can't read item2 - which is valid
        CallRecord r = reader.readValue(json);
        assertNull(r.item);
        assertNotNull(r.item2);

        json = aposToQuotes("{'item':{'type':'xevent','location':'location1'}, 'version':0.0,'application':'123'}");
        CallRecord r3 = reader.readValue(json);
        assertNull(r3.item);
        assertEquals("123", r3.application);
    }

    public void testUnknownClassAsSubtype() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_INVALID_SUBTYPE, false);
        BaseWrapper w = mapper.readValue(aposToQuotes
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
            fail("Expected " + JsonMappingException.class);
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