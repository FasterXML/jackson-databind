package com.fasterxml.jackson.databind.jsontype.ext;

import java.math.BigDecimal;
import java.util.*;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.annotation.JsonTypeInfo.As;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import com.fasterxml.jackson.databind.BaseMapTest;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.json.JsonMapper;

// Tests for External type id, one that exists at same level as typed Object,
// that is, property is not within typed object but a member of its parent.
public class ExternalTypeIdTest extends BaseMapTest
{
    static class ExternalBean
    {
        @JsonTypeInfo(use=Id.NAME, include=As.EXTERNAL_PROPERTY, property="extType")
        public Object bean;

        public ExternalBean() { }
        public ExternalBean(int v) {
            bean = new ValueBean(v);
        }
    }

    // for [databind#96]
    static class ExternalBeanWithDefault
    {
        @JsonTypeInfo(use=Id.CLASS, include=As.EXTERNAL_PROPERTY, property="extType",
                defaultImpl=ValueBean.class)
        public Object bean;

        public ExternalBeanWithDefault() { }
        public ExternalBeanWithDefault(int v) {
            bean = new ValueBean(v);
        }
    }

    static class ExternalBean3
    {
        @JsonTypeInfo(use=Id.NAME, include=As.EXTERNAL_PROPERTY, property="extType1")
        public Object value1;

        @JsonTypeInfo(use=Id.NAME, include=As.EXTERNAL_PROPERTY, property="extType2")
        public Object value2;

        public int foo;

        @JsonTypeInfo(use=Id.NAME, include=As.EXTERNAL_PROPERTY, property="extType3")
        public Object value3;

        public ExternalBean3() { }
        public ExternalBean3(int v) {
            value1 = new ValueBean(v);
            value2 = new ValueBean(v+1);
            value3 = new ValueBean(v+2);
            foo = v;
        }
    }

    static class ExternalBeanWithCreator
    {
        @JsonTypeInfo(use=Id.NAME, include=As.EXTERNAL_PROPERTY, property="extType")
        public Object value;

        public int foo;

        @JsonCreator
        public ExternalBeanWithCreator(@JsonProperty("foo") int f)
        {
            foo = f;
            value = new ValueBean(f);
        }
    }

    @JsonTypeName("vbean")
    static class ValueBean {
        public int value;

        public ValueBean() { }
        public ValueBean(int v) { value = v; }
    }

    @JsonTypeName("funk")
    @JsonTypeInfo(use=Id.NAME, include=As.EXTERNAL_PROPERTY, property="extType")
    static class FunkyExternalBean {
        public int i = 3;
    }

    // [JACKSON-798]: problems with polymorphic types, external prop

    @JsonSubTypes(value= { @JsonSubTypes.Type(value=Derived1.class, name="d1"),
            @JsonSubTypes.Type(value=Derived2.class, name="d2") })
    interface Base {
        String getBaseProperty();
    }

    static class Derived1 implements Base {
        private String derived1Property;
        private String baseProperty;
        protected  Derived1() { throw new IllegalStateException("wrong constructor called"); }

        @JsonCreator
        public Derived1(@JsonProperty("derived1Property") String d1p,
                        @JsonProperty("baseProperty") String bp) {
            derived1Property = d1p;
            baseProperty = bp;
        }

        @Override
        @JsonProperty public String getBaseProperty() {
            return baseProperty;
        }

        @JsonProperty public String getDerived1Property() {
            return derived1Property;
        }
    }

    static class Derived2 implements Base {
        private String derived2Property;
        private String baseProperty;
        protected  Derived2() { throw new IllegalStateException("wrong constructor called"); }

        @JsonCreator
        public Derived2(@JsonProperty("derived2Property") String d2p,
                        @JsonProperty("baseProperty") String bp) {
            derived2Property = d2p;
            baseProperty = bp;
        }

        @Override
        @JsonProperty public String getBaseProperty() {
            return baseProperty;
        }

        @JsonProperty public String getDerived2Property() {
            return derived2Property;
        }
    }

    static class BaseContainer {
        protected final Base base;
        protected final String baseContainerProperty;
        protected BaseContainer() { throw new IllegalStateException("wrong constructor called"); }

        @JsonCreator
        public BaseContainer(@JsonProperty("baseContainerProperty") String bcp, @JsonProperty("base") Base b) {
            baseContainerProperty = bcp;
            base = b;
        }

        @JsonProperty
        public String getBaseContainerProperty() { return baseContainerProperty; }

        @JsonTypeInfo(use=JsonTypeInfo.Id.NAME, include=JsonTypeInfo.As.EXTERNAL_PROPERTY, property="type")
        @JsonProperty
        public Base getBase() { return base; }
    }

    interface Pet {}

    static class Dog implements Pet {
        public String name;
    }

    static class House831 {
        protected String petType;

        @JsonTypeInfo(use = Id.NAME, include = As.EXTERNAL_PROPERTY, property = "petType")
        @JsonSubTypes({@JsonSubTypes.Type(name = "dog", value = Dog.class)})
        public Pet pet;

        public String getPetType() {
            return petType;
        }

        public void setPetType(String petType) {
            this.petType = petType;
        }
    }

    // for [databind#118]
    static class ExternalTypeWithNonPOJO {
        @JsonTypeInfo(use = JsonTypeInfo.Id.NAME,
                property = "type",
                visible = true,
                include = JsonTypeInfo.As.EXTERNAL_PROPERTY,
                defaultImpl = String.class)
        @JsonSubTypes({
            @JsonSubTypes.Type(value = Date.class, name = "date"),
            @JsonSubTypes.Type(value = AsValueThingy.class, name = "thingy")
        })
        public Object value;

        public ExternalTypeWithNonPOJO() { }
        public ExternalTypeWithNonPOJO(Object o) { value = o; }
    }

    // for [databind#119]
    static class AsValueThingy {
        public long rawDate;

        public AsValueThingy(long l) { rawDate = l; }
        public AsValueThingy() { }

        @JsonValue public Date serialization() {
            return new Date(rawDate);
        }
    }

    // [databind#222]
    static class Issue222Bean
    {
        @JsonTypeInfo(use = JsonTypeInfo.Id.NAME,
                property = "type",
                include = JsonTypeInfo.As.EXTERNAL_PROPERTY)
        public Issue222BeanB value;

        public String type = "foo";

        public Issue222Bean() { }
        public Issue222Bean(int v) {
            value = new Issue222BeanB(v);
        }
    }

    @JsonTypeName("222b") // shouldn't actually matter
    static class Issue222BeanB
    {
        public int x;

        public Issue222BeanB() { }
        public Issue222BeanB(int value) { x = value; }
    }

    // [databind#928]
    static class Envelope928 {
        Object _payload;

        public Envelope928(@JsonProperty("payload")
        @JsonTypeInfo(use=JsonTypeInfo.Id.CLASS, include=JsonTypeInfo.As.EXTERNAL_PROPERTY, property="class")
        Object payload) {
            _payload = payload;
        }
    }

    static class Payload928 {
        public String something;
    }

    enum Type965 { BIG_DECIMAL }

    static class Wrapper965 {
        protected Type965 typeEnum;

        protected Object value;

        @JsonGetter("type")
        String getTypeString() {
            return typeEnum.name();
        }

        @JsonSetter("type")
        void setTypeString(String type) {
            this.typeEnum = Type965.valueOf(type);
        }

        @JsonGetter(value = "objectValue")
        Object getValue() {
            return value;
        }

        @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXTERNAL_PROPERTY, property = "type")
        @JsonSubTypes({ @JsonSubTypes.Type(name = "BIG_DECIMAL", value = BigDecimal.class) })
        @JsonSetter(value = "objectValue")
        private void setValue(Object value) {
            this.value = value;
        }
    }

    /*
    /**********************************************************
    /* Unit tests, serialization
    /**********************************************************
     */

    private final ObjectMapper MAPPER = new ObjectMapper();

    public void testSimpleSerialization() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerSubtypes(ValueBean.class);
        // This may look odd, but one implementation nastiness is the fact
        // that we cannot properly serialize type id before the object,
        // because call is made after property name (for object) has already
        // been written out. So we'll write it after...
        // Deserializer will work either way as it cannot rely on ordering
        // anyway.
        assertEquals("{\"bean\":{\"value\":11},\"extType\":\"vbean\"}",
                mapper.writeValueAsString(new ExternalBean(11)));
    }

    // If trying to use with Class, should just become "PROPERTY" instead:
    public void testImproperExternalIdSerialization() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        assertEquals("{\"extType\":\"funk\",\"i\":3}",
                mapper.writeValueAsString(new FunkyExternalBean()));
    }

    // for [databind#942]
    public void testExternalTypeIdWithNull() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerSubtypes(ValueBean.class);
        ExternalBean b;
        b = mapper.readValue(a2q("{'bean':null,'extType':'vbean'}"),
                ExternalBean.class);
        assertNotNull(b);
        b = mapper.readValue(a2q("{'extType':'vbean','bean':null}"),
                ExternalBean.class);
        assertNotNull(b);
    }

    /*
    /**********************************************************
    /* Unit tests, deserialization
    /**********************************************************
     */

    public void testSimpleDeserialization() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerSubtypes(ValueBean.class);
        ExternalBean result = mapper.readValue("{\"bean\":{\"value\":11},\"extType\":\"vbean\"}", ExternalBean.class);
        assertNotNull(result);
        assertNotNull(result.bean);
        ValueBean vb = (ValueBean) result.bean;
        assertEquals(11, vb.value);

        // let's also test with order switched:
        result = mapper.readValue("{\"extType\":\"vbean\", \"bean\":{\"value\":13}}", ExternalBean.class);
        assertNotNull(result);
        assertNotNull(result.bean);
        vb = (ValueBean) result.bean;
        assertEquals(13, vb.value);
    }

    // Test for verifying that it's ok to have multiple (say, 3)
    // externally typed things, mixed with other stuff...
    public void testMultipleTypeIdsDeserialization() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerSubtypes(ValueBean.class);
        String json = mapper.writeValueAsString(new ExternalBean3(3));
        ExternalBean3 result = mapper.readValue(json, ExternalBean3.class);
        assertNotNull(result);
        assertNotNull(result.value1);
        assertNotNull(result.value2);
        assertNotNull(result.value3);
        assertEquals(3, ((ValueBean)result.value1).value);
        assertEquals(4, ((ValueBean)result.value2).value);
        assertEquals(5, ((ValueBean)result.value3).value);
        assertEquals(3, result.foo);
    }

    // Also, it should be ok to use @JsonCreator as well...
    public void testExternalTypeWithCreator() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerSubtypes(ValueBean.class);
        String json = mapper.writeValueAsString(new ExternalBeanWithCreator(7));
        ExternalBeanWithCreator result = mapper.readValue(json, ExternalBeanWithCreator.class);
        assertNotNull(result);
        assertNotNull(result.value);
        assertEquals(7, ((ValueBean)result.value).value);
        assertEquals(7, result.foo);
    }

    // If trying to use with Class, should just become "PROPERTY" instead:
    public void testImproperExternalIdDeserialization() throws Exception
    {
        FunkyExternalBean result = MAPPER.readValue("{\"extType\":\"funk\",\"i\":3}",
                FunkyExternalBean.class);
        assertNotNull(result);
        assertEquals(3, result.i);

        result = MAPPER.readValue("{\"i\":4,\"extType\":\"funk\"}",
                FunkyExternalBean.class);
        assertNotNull(result);
        assertEquals(4, result.i);
    }

    public void testIssue798() throws Exception
    {
        Base base = new Derived1("derived1 prop val", "base prop val");
        BaseContainer baseContainer = new BaseContainer("bc prop val", base);
        String generatedJson = MAPPER.writeValueAsString(baseContainer);
        BaseContainer baseContainer2 = MAPPER.readValue(generatedJson,BaseContainer.class);
        assertEquals("bc prop val", baseContainer.getBaseContainerProperty());

        Base b = baseContainer2.getBase();
        assertNotNull(b);
        if (b.getClass() != Derived1.class) {
            fail("Should have type Derived1, was "+b.getClass().getName());
        }

        Derived1 derived1 = (Derived1) b;
        assertEquals("base prop val", derived1.getBaseProperty());
        assertEquals("derived1 prop val", derived1.getDerived1Property());
    }

    // There seems to be some problems if type is also visible...
    public void testIssue831() throws Exception
    {
        final String JSON = "{ \"petType\": \"dog\",\n"
                +"\"pet\": { \"name\": \"Pluto\" }\n}";
        House831 result = MAPPER.readValue(JSON, House831.class);
        assertNotNull(result);
        assertNotNull(result.pet);
        assertSame(Dog.class, result.pet.getClass());
        assertEquals("dog", result.petType);
    }

    // For [databind#118]
    // Note: String works fine, since no type id will used; other scalar types have issues
    public void testWithScalar118() throws Exception
    {
        ExternalTypeWithNonPOJO input = new ExternalTypeWithNonPOJO(new java.util.Date(123L));
        String json = MAPPER.writeValueAsString(input);
        assertNotNull(json);

        // and back just to be sure:
        ExternalTypeWithNonPOJO result = MAPPER.readValue(json, ExternalTypeWithNonPOJO.class);
        assertNotNull(result.value);
        assertTrue(result.value instanceof java.util.Date);
    }

    // For [databind#118] using "natural" type(s)
    public void testWithNaturalScalar118() throws Exception
    {
        ExternalTypeWithNonPOJO input = new ExternalTypeWithNonPOJO(Integer.valueOf(13));
        String json = MAPPER.writeValueAsString(input);
        assertNotNull(json);
        // and back just to be sure:
        ExternalTypeWithNonPOJO result = MAPPER.readValue(json, ExternalTypeWithNonPOJO.class);
        assertNotNull(result.value);
        assertTrue(result.value instanceof Integer);

        // ditto with others types
        input = new ExternalTypeWithNonPOJO(Boolean.TRUE);
        json = MAPPER.writeValueAsString(input);
        assertNotNull(json);
        result = MAPPER.readValue(json, ExternalTypeWithNonPOJO.class);
        assertNotNull(result.value);
        assertTrue(result.value instanceof Boolean);

        input = new ExternalTypeWithNonPOJO("foobar");
        json = MAPPER.writeValueAsString(input);
        assertNotNull(json);
        result = MAPPER.readValue(json, ExternalTypeWithNonPOJO.class);
        assertNotNull(result.value);
        assertTrue(result.value instanceof String);
        assertEquals("foobar", result.value);
    }

    // For [databind#119]... and bit of [#167] as well
    public void testWithAsValue() throws Exception
    {
        ExternalTypeWithNonPOJO input = new ExternalTypeWithNonPOJO(new AsValueThingy(12345L));
        String json = MAPPER.writeValueAsString(input);
        assertNotNull(json);
        assertEquals("{\"value\":12345,\"type\":\"thingy\"}", json);

        // and get it back too:
        ExternalTypeWithNonPOJO result = MAPPER.readValue(json, ExternalTypeWithNonPOJO.class);
        assertNotNull(result);
        assertNotNull(result.value);
        assertEquals(AsValueThingy.class, result.value.getClass());
        assertEquals(12345L, ((AsValueThingy) result.value).rawDate);
    }

    // for [databind#222]
    public void testExternalTypeWithProp222() throws Exception
    {
        JsonMapper mapper = JsonMapper.builder().enable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY).build();
        Issue222Bean input = new Issue222Bean(13);
        String json = mapper.writeValueAsString(input);
        assertEquals("{\"type\":\"foo\",\"value\":{\"x\":13}}", json);
    }

    // [databind#928]
    public void testInverseExternalId928() throws Exception
    {
        final String CLASS = Payload928.class.getName();

        ObjectMapper mapper = new ObjectMapper();

        final String successCase = "{\"payload\":{\"something\":\"test\"},\"class\":\""+CLASS+"\"}";
        Envelope928 envelope1 = mapper.readValue(successCase, Envelope928.class);
        assertNotNull(envelope1);
        assertEquals(Payload928.class, envelope1._payload.getClass());

        // and then re-ordered case that was problematic
        final String failCase = "{\"class\":\""+CLASS+"\",\"payload\":{\"something\":\"test\"}}";
        Envelope928 envelope2 = mapper.readValue(failCase, Envelope928.class);
        assertNotNull(envelope2);
        assertEquals(Payload928.class, envelope2._payload.getClass());
    }

    // for [databind#965]
    public void testBigDecimal965() throws Exception
    {
        Wrapper965 w = new Wrapper965();
        w.typeEnum = Type965.BIG_DECIMAL;
        final String NUM_STR = "-10000000000.0000000001";
        w.value = new BigDecimal(NUM_STR);

        String json = MAPPER.writeValueAsString(w);

        // simple sanity check so serialization is faithful
        if (!json.contains(NUM_STR)) {
            fail("JSON content should contain value '"+NUM_STR+"', does not appear to: "+json);
        }

        Wrapper965 w2 = MAPPER.readerFor(Wrapper965.class)
                .with(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS)
                .readValue(json);

        assertEquals(w.typeEnum, w2.typeEnum);
        assertTrue(String.format("Expected %s = %s; got back %s = %s",
            w.value.getClass().getSimpleName(), w.value.toString(), w2.value.getClass().getSimpleName(), w2.value.toString()),
            w.value.equals(w2.value));
    }

    public void testBigDecimal965StringBased() throws Exception
    {
        Wrapper965 w = new Wrapper965();
        w.typeEnum = Type965.BIG_DECIMAL;
        final String NUM_STR = "-10000000000.0000000001";
        w.value = new BigDecimal(NUM_STR);

        String json = "{\"objectValue\":\"-10000000000.0000000001\",\"type\":\"BIG_DECIMAL\"}";

        Wrapper965 w2 = MAPPER.readerFor(Wrapper965.class)
                .with(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS)
                .readValue(json);

        assertEquals(w.typeEnum, w2.typeEnum);
        assertTrue(String.format("Expected %s = %s; got back %s = %s",
                        w.value.getClass().getSimpleName(), w.value.toString(), w2.value.getClass().getSimpleName(), w2.value.toString()),
                w.value.equals(w2.value));
    }

    static class Box3008 {
        public String type;
        public Fruit3008 fruit;

        public Box3008(@JsonProperty("type") String type,
                @JsonTypeInfo(use = Id.NAME, include = As.EXTERNAL_PROPERTY, property = "type")
                @JsonSubTypes({@JsonSubTypes.Type(value = Orange.class, name = "orange")})
                @JsonProperty("fruit")
                Fruit3008 fruit) {
            this.type = type;
            this.fruit = fruit;
        }
    }

    // [databind#3008]: allow
    interface Fruit3008 {}

    static class Orange implements Fruit3008 {
        public String name;
        public String color;

        public Orange(@JsonProperty("name") String name, @JsonProperty("name") String color) {
            this.name = name;
            this.color = color;
        }
    }

    // for [databind#3008]
    public void testIssue3008() throws Exception
    {
        ObjectReader r = MAPPER.readerFor(Box3008.class);
        Box3008 deserOrangeBox = r.readValue("{\"type\":null,\"fruit\":null}}");
        assertNull(deserOrangeBox.fruit);
        assertNull(deserOrangeBox.type); // error: "expected null, but was:<null>"
    }
}
