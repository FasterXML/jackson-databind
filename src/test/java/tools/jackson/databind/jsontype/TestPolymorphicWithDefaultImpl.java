package tools.jackson.databind.jsontype;

import java.util.*;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.*;

import tools.jackson.core.Version;
import tools.jackson.databind.*;
import tools.jackson.databind.exc.InvalidTypeIdException;
import tools.jackson.databind.module.SimpleModule;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests related to specialized handling of "default implementation"
 * ({@link JsonTypeInfo#defaultImpl}), as well as related
 * cases that allow non-default settings (such as missing type id).
 */
public class TestPolymorphicWithDefaultImpl extends DatabindTestUtil
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

    // [databind#1565]
    @JsonTypeInfo(use=JsonTypeInfo.Id.NAME, include=JsonTypeInfo.As.PROPERTY,
        property="typeInfo",  defaultImpl = CBaseClass1565.class)
    @JsonSubTypes({
        @JsonSubTypes.Type(CDerived1565.class)
    })
    public static interface CTestInterface1565
    {
         public String getName();
         public void setName(String name);
         public String getTypeInfo();
    }

    static class CBaseClass1565 implements CTestInterface1565
    {
         private String mName;

         @Override
         public String getName() {
              return(mName);
         }

         @Override
         public void setName(String name) {
              mName = name;
         }

         @Override
         public String getTypeInfo() {
              return "base";
         }
    }

    @JsonTypeName("derived")
    static class CDerived1565 extends CBaseClass1565
    {
         public String description;

         @Override
         public String getTypeInfo() {
              return "derived";
         }
    }

    // [databind#1861]
    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type", defaultImpl = DefaultImpl1861.class)
    @JsonSubTypes({
            @JsonSubTypes.Type(name = "a", value = Impl1861A.class)
    })
    static abstract class Bean1861 {
        public String base;
    }

    static class DefaultImpl1861 extends Bean1861 {
        public int id;
    }

    static class Impl1861A extends Bean1861 {
        public int valueA;
    }

    // for TestSubtypesWithDefaultImpl
    @JsonTypeInfo(use=JsonTypeInfo.Id.NAME, include=JsonTypeInfo.As.PROPERTY,
            property="#type",
            defaultImpl=DefaultImpl.class)
    static abstract class SuperTypeWithDefault { }

    static class DefaultImpl extends SuperTypeWithDefault {
        public int a;
    }

    @JsonTypeInfo(use=JsonTypeInfo.Id.NAME, include=JsonTypeInfo.As.PROPERTY, property="#type")
    static abstract class SuperTypeWithoutDefault { }

    static class DefaultImpl505 extends SuperTypeWithoutDefault {
        public int a;
    }

    /*
    /**********************************************************
    /* Unit tests, deserialization
    /**********************************************************
     */

    private final ObjectMapper MAPPER = newJsonMapper();

    @Test
    public void testDeserializationWithObject() throws Exception
    {
        Inter inter = MAPPER.readerFor(Inter.class).readValue("{\"type\": \"mine\", \"blah\": [\"a\", \"b\", \"c\"]}");
        assertInstanceOf(MyInter.class, inter);
        assertFalse(inter instanceof LegacyInter);
        assertEquals(Arrays.asList("a", "b", "c"), ((MyInter) inter).blah);
    }

    @Test
    public void testDeserializationWithString() throws Exception
    {
        Inter inter = MAPPER.readerFor(Inter.class).readValue("\"a,b,c,d\"");
        assertInstanceOf(LegacyInter.class, inter);
        assertEquals(Arrays.asList("a", "b", "c", "d"), ((MyInter) inter).blah);
    }

    @Test
    public void testDeserializationWithArray() throws Exception
    {
        Inter inter = MAPPER.readerFor(Inter.class).readValue("[\"a\", \"b\", \"c\", \"d\"]");
        assertInstanceOf(LegacyInter.class, inter);
        assertEquals(Arrays.asList("a", "b", "c", "d"), ((MyInter) inter).blah);
    }

    @Test
    public void testDeserializationWithArrayOfSize2() throws Exception
    {
        Inter inter = MAPPER.readerFor(Inter.class).readValue("[\"a\", \"b\"]");
        assertInstanceOf(LegacyInter.class, inter);
        assertEquals(Arrays.asList("a", "b"), ((MyInter) inter).blah);
    }

    // [databind#148]
    @Test
    public void testDefaultAsVoid() throws Exception
    {
        // 07-Mar-2018, tatu: Specifically, use of `Void` should infer that unknown type
        //   values should become `null`s
        Object ob = MAPPER.readerFor(DefaultWithVoidAsDefault.class).readValue("{ }");
        assertNull(ob);
        ob = MAPPER.readerFor(DefaultWithVoidAsDefault.class).readValue("{ \"bogus\":3 }");
        assertNull(ob);
    }

    // [databind#148]
    @Test
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
    @Test
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
    @Test
    public void testDefaultImplWithObjectWrapper() throws Exception
    {
        BaseFor656 value = MAPPER.readValue(a2q("{'foobar':{'a':3}}"), BaseFor656.class);
        assertNotNull(value);
        assertEquals(ImplFor656.class, value.getClass());
        assertEquals(3, ((ImplFor656) value).a);
    }

    @Test
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

    @Test
    public void testUnknownClassAsSubtype() throws Exception
    {
        ObjectMapper mapper = jsonMapperBuilder()
                .configure(DeserializationFeature.FAIL_ON_INVALID_SUBTYPE, false)
                .build();
        BaseWrapper w = mapper.readValue(a2q
                ("{'value':{'clazz':'com.foobar.Nothing'}}"),
                BaseWrapper.class);
        assertNotNull(w);
        assertNull(w.value);
    }

    @Test
    public void testWithoutEmptyStringAsNullObject1533() throws Exception
    {
        ObjectReader r = MAPPER.readerFor(AsPropertyWrapper.class)
                .without(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT);
        InvalidTypeIdException e = assertThrows(InvalidTypeIdException.class,
                () -> r.readValue("{ \"value\": \"\" }"));
        verifyException(e, "missing type id property 'type'");
    }

    // [databind#1533]
    @Test
    public void testWithEmptyStringAsNullObject1533() throws Exception
    {
        ObjectReader r = MAPPER.readerFor(AsPropertyWrapper.class)
                .with(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT);
        AsPropertyWrapper wrapper = r.readValue("{ \"value\": \"\" }");
        assertNull(wrapper.value);
    }

    // [databind#1565]
    @Test
    public void testIncompatibleDefaultImpl1565() throws Exception
    {
        String value = "{\"typeInfo\": \"derived\", \"name\": \"John\", \"description\": \"Owner\"}";
        CDerived1565 result = MAPPER.readValue(value, CDerived1565.class);
        assertNotNull(result);
    }

    // [databind#1861]
    @Test
    public void testWithIncompatibleTargetType1861() throws Exception
    {
        // Should allow deserialization even if `defaultImpl` incompatible
        Impl1861A result = MAPPER.readValue(a2q("{'type':'a','base':'foo','valueA':3}"),
                Impl1861A.class);
        assertNotNull(result);
    }

    @Test
    public void testDefaultImplFromAnnotation() throws Exception
    {
        // first, test with no type information
        SuperTypeWithDefault bean = MAPPER.readValue("{\"a\":13}", SuperTypeWithDefault.class);
        assertEquals(DefaultImpl.class, bean.getClass());
        assertEquals(13, ((DefaultImpl) bean).a);

        // and then with unmapped info
        bean = MAPPER.readValue("{\"a\":14,\"#type\":\"foobar\"}", SuperTypeWithDefault.class);
        assertEquals(DefaultImpl.class, bean.getClass());
        assertEquals(14, ((DefaultImpl) bean).a);

        bean = MAPPER.readValue("{\"#type\":\"foobar\",\"a\":15}", SuperTypeWithDefault.class);
        assertEquals(DefaultImpl.class, bean.getClass());
        assertEquals(15, ((DefaultImpl) bean).a);

        bean = MAPPER.readValue("{\"#type\":\"foobar\"}", SuperTypeWithDefault.class);
        assertEquals(DefaultImpl.class, bean.getClass());
        assertEquals(0, ((DefaultImpl) bean).a);
    }

    @Test
    public void testDefaultImplViaModule() throws Exception
    {
        final String JSON = "{\"a\":123}";

        // first: without registration etc, epic fail:
        InvalidTypeIdException e = assertThrows(InvalidTypeIdException.class,
                () -> MAPPER.readValue(JSON, SuperTypeWithoutDefault.class));
        verifyException(e, "missing type id property '#type'");

        // but then succeed when we register default impl
        SimpleModule module = new SimpleModule("test", Version.unknownVersion());
        module.addAbstractTypeMapping(SuperTypeWithoutDefault.class, DefaultImpl505.class);
        ObjectMapper mapper = jsonMapperBuilder()
                .addModule(module)
                .build();
        SuperTypeWithoutDefault bean = mapper.readValue(JSON, SuperTypeWithoutDefault.class);
        assertNotNull(bean);
        assertEquals(DefaultImpl505.class, bean.getClass());
        assertEquals(123, ((DefaultImpl505) bean).a);

        bean = mapper.readValue("{\"#type\":\"foobar\"}", SuperTypeWithoutDefault.class);
        assertEquals(DefaultImpl505.class, bean.getClass());
        assertEquals(0, ((DefaultImpl505) bean).a);
    }

    /*
    /**********************************************************
    /* Unit tests, serialization
    /**********************************************************
     */

    /*
    @Test
    public void testDontWriteIfDefaultImpl() throws Exception {
        String json = MAPPER.writeValueAsString(new MyInter());
        assertEquals("{\"blah\":null}", json);
    }
    */
}
