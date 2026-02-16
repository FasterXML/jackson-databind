package tools.jackson.databind.struct;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.*;

import tools.jackson.databind.*;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for verifying that basic {@link JsonUnwrapped} annotation
 * handling works as expected; some more advanced tests are separated out
 * to more specific test classes (like prefix/suffix handling).
 */
public class UnwrappedBasicTest extends DatabindTestUtil
{
    static class Unwrapping {
        public String name;
        @JsonUnwrapped
        public Location location;

        public Unwrapping() { }
        protected Unwrapping(String str, int x, int y) {
            name = str;
            location = new Location(x, y);
        }
    }

    final static class Location {
        public int x;
        public int y;

        public Location() { }
        public Location(int x, int y) {
            this.x = x;
            this.y = y;
        }
    }

    static class DeepUnwrapping
    {
        @JsonUnwrapped
        public Unwrapping unwrapped;

        public DeepUnwrapping() { }
        protected DeepUnwrapping(String str, int x, int y) {
            unwrapped = new Unwrapping(str, x, y);
        }
    }

    static class UnwrappingWithCreator {
        public String name;

        @JsonUnwrapped
        public Location location;

        @JsonCreator
        public UnwrappingWithCreator(@JsonProperty("name") String n) {
            name = n;
        }
    }

    // Class with two unwrapped properties
    static class TwoUnwrappedProperties {
        @JsonUnwrapped
        public Location location;
        @JsonUnwrapped
        public Name name;

        public TwoUnwrappedProperties() { }
    }

    static class Name {
        public String first, last;
    }

    // [databind#615]
    static class Parent {
        @JsonUnwrapped
        public Child c1;

        public Parent() { }
        public Parent(String str) { c1 = new Child(str); }
    }

    static class Child {
        public String field;

        public Child() { }
        public Child(String f) { field = f; }
    }

    static class Inner {
        public String animal;
    }

    static class Outer {
        @JsonUnwrapped
        Inner inner;
    }

    // [databind#1493]: case-insensitive handling
    static class Person {
        @JsonUnwrapped(prefix = "businessAddress.")
        public Address businessAddress;
    }

    static class Address {
        public String street;
        public String addon;
        public String zip;
        public String town;
        public String country;
    }

    // [databind#383]
    static class RecursivePerson {
        public String name;
        public int age;
        @JsonUnwrapped(prefix="child.") public RecursivePerson child;
    }

    // [databind#647]
    static class UnwrappedWithSamePropertyName {
        public MailHolder mail;
    }

    static class MailHolder {
        @JsonUnwrapped
        public Mail mail;
    }

    static class Mail {
        public String mail;
    }

    // [databind#81]
    @JsonTypeInfo(use=JsonTypeInfo.Id.NAME, property="@type")
    @JsonTypeName("OuterType")
    static class Outer81 {
        private @JsonProperty String p1;
        public String getP1() { return p1; }
        public void setP1(String p1) { this.p1 = p1; }

        private Inner81 inner;
        public void setInner(Inner81 inner) { this.inner = inner; }

        @JsonUnwrapped
        public Inner81 getInner() {
            return inner;
        }
    }

    @JsonTypeInfo(use=JsonTypeInfo.Id.NAME, property="@type")
    @JsonTypeName("InnerType")
    static class Inner81 {
        private @JsonProperty String p2;
        public String getP2() { return p2; }
        public void setP2(String p2) { this.p2 = p2; }
    }

    // [databind#1559]
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    static final class Health {
        @JsonUnwrapped(prefix="xxx.")
        public Status status;
    }

    // NOTE: `final` is required to trigger [databind#1559]
    static final class Status {
        public String code;
    }

    // [databind#2461]
    static class Base {
        public String id;

        Base(String id) {
            this.id = id;
        }
    }

    static class InnerContainer {
        @JsonUnwrapped(prefix = "base.")
        public Base base;

        InnerContainer(Base base) {
            this.base = base;
        }
    }

    static class OuterContainer {
        @JsonUnwrapped(prefix = "container.")
        public InnerContainer container;

        OuterContainer(InnerContainer container) {
            this.container = container;
        }
    }

    // [databind#3277]
    static class Holder {
        Object value1;

        @JsonUnwrapped
        Holder2 holder2;

        public Object getValue1() {
            return value1;
        }

        public void setValue1(Object value1) {
            this.value1 = value1;
        }
    }

    static class Holder2 {
        Map<String, Object> data = new HashMap<>();

        @JsonAnyGetter
        public Map<String, Object> getData() {
            return data;
        }

        @JsonAnySetter
        public void setAny(String key, Object value) {
            data.put(key, value);
        }
    }

    // [databind#2088]
    static class Issue2088Bean {
        int x;
        int y;

        @JsonUnwrapped
        Issue2088UnwrappedBean w;

        public Issue2088Bean(@JsonProperty("x") int x, @JsonProperty("y") int y) {
            this.x = x;
            this.y = y;
        }

        public void setW(Issue2088UnwrappedBean w) {
            this.w = w;
        }
    }

    static class Issue2088UnwrappedBean {
        int a;
        int b;

        public Issue2088UnwrappedBean(@JsonProperty("a") int a, @JsonProperty("b") int b) {
            this.a = a;
            this.b = b;
        }
    }

    /*
    /**********************************************************
    /* Tests, serialization
    /**********************************************************
     */

    private final ObjectMapper MAPPER = newJsonMapper();

    @Test
    public void testSimpleUnwrappingSerialize() throws Exception {
        JsonMapper mapper = JsonMapper.builder().enable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY).build();
        assertEquals("{\"x\":1,\"y\":2,\"name\":\"Tatu\"}",
                mapper.writeValueAsString(new Unwrapping("Tatu", 1, 2)));
    }

    @Test
    public void testDeepUnwrappingSerialize() throws Exception {
        JsonMapper mapper = JsonMapper.builder().enable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY).build();
        assertEquals("{\"x\":1,\"y\":2,\"name\":\"Tatu\"}",
                mapper.writeValueAsString(new DeepUnwrapping("Tatu", 1, 2)));
    }

    /*
    /**********************************************************
    /* Tests, deserialization
    /**********************************************************
     */

    @Test
    public void testSimpleUnwrappedDeserialize() throws Exception
    {
        Unwrapping bean = MAPPER.readValue("{\"name\":\"Tatu\",\"y\":7,\"x\":-13}",
                Unwrapping.class);
        assertEquals("Tatu", bean.name);
        Location loc = bean.location;
        assertNotNull(loc);
        assertEquals(-13, loc.x);
        assertEquals(7, loc.y);
    }

    @Test
    public void testDoubleUnwrapping() throws Exception
    {
        TwoUnwrappedProperties bean = MAPPER.readValue("{\"first\":\"Joe\",\"y\":7,\"last\":\"Smith\",\"x\":-13}",
                TwoUnwrappedProperties.class);
        Location loc = bean.location;
        assertNotNull(loc);
        assertEquals(-13, loc.x);
        assertEquals(7, loc.y);
        Name name = bean.name;
        assertNotNull(name);
        assertEquals("Joe", name.first);
        assertEquals("Smith", name.last);
    }

    @Test
    public void testDeepUnwrapping() throws Exception
    {
        DeepUnwrapping bean = MAPPER.readValue("{\"x\":3,\"name\":\"Bob\",\"y\":27}",
                DeepUnwrapping.class);
        Unwrapping uw = bean.unwrapped;
        assertNotNull(uw);
        assertEquals("Bob", uw.name);
        Location loc = uw.location;
        assertNotNull(loc);
        assertEquals(3, loc.x);
        assertEquals(27, loc.y);
    }

    @Test
    public void testUnwrappedDeserializeWithCreator() throws Exception
    {
        UnwrappingWithCreator bean = MAPPER.readValue("{\"x\":1,\"y\":2,\"name\":\"Tatu\"}",
                UnwrappingWithCreator.class);
        assertEquals("Tatu", bean.name);
        Location loc = bean.location;
        assertNotNull(loc);
        assertEquals(1, loc.x);
        assertEquals(2, loc.y);
    }

    @Test
    public void testIssue615() throws Exception
    {
        Parent input = new Parent("name");
        String json = MAPPER.writeValueAsString(input);
        Parent output = MAPPER.readValue(json, Parent.class);
        assertEquals("name", output.c1.field);
    }

    @Test
    public void testUnwrappedAsPropertyIndicator() throws Exception
    {
        Inner inner = new Inner();
        inner.animal = "Zebra";

        Outer outer = new Outer();
        outer.inner = inner;

        String actual = MAPPER.writeValueAsString(outer);

        assertTrue(actual.contains("animal"));
        assertTrue(actual.contains("Zebra"));
        assertFalse(actual.contains("inner"));
    }

    // [databind#1493]: case-insensitive handling
    @Test
    public void testCaseInsensitiveUnwrap() throws Exception
    {
        ObjectMapper mapper = jsonMapperBuilder()
                .enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES)
                .build();
        Person p = mapper.readValue("{ }", Person.class);
        assertNotNull(p);
    }

    // [databind#2088]: accidental skipping of values
    @Test
    public void testIssue2088UnwrappedFieldsAfterLastCreatorProp() throws Exception
    {
        Issue2088Bean bean = MAPPER.readValue("{\"x\":1,\"a\":2,\"y\":3,\"b\":4}", Issue2088Bean.class);
        assertEquals(1, bean.x);
        assertEquals(2, bean.w.a);
        assertEquals(3, bean.y);
        assertEquals(4, bean.w.b);
    }

    // [databind#383]
    @Test
    public void testRecursiveUsage() throws Exception
    {
        final String JSON = "{ 'name': 'Bob', 'age': 45, 'gender': 0, 'child.name': 'Bob jr', 'child.age': 15 }";
        RecursivePerson p = MAPPER.readValue(a2q(JSON), RecursivePerson.class);
        assertNotNull(p);
        assertEquals("Bob", p.name);
        assertNotNull(p.child);
        assertEquals("Bob jr", p.child.name);
    }

    // [databind#647]
    @Test
    public void testUnwrappedWithSamePropertyName() throws Exception {
        final String JSON = "{'mail': {'mail': 'the mail text'}}";
        UnwrappedWithSamePropertyName result = MAPPER.readValue(a2q(JSON), UnwrappedWithSamePropertyName.class);
        assertNotNull(result.mail);
        assertNotNull(result.mail.mail);
        assertEquals("the mail text", result.mail.mail.mail);
    }

    // [databind#81]
    @Test
    public void testDefaultUnwrappedWithTypeInfo() throws Exception
    {
        Outer81 outer = new Outer81();
        outer.setP1("101");

        Inner81 inner = new Inner81();
        inner.setP2("202");
        outer.setInner(inner);

        try {
            MAPPER.writeValueAsString(outer);
             fail("Expected exception to be thrown.");
        } catch (DatabindException ex) {
            verifyException(ex, "requires use of type information");
        }
    }

    // [databind#81]
    @Test
    public void testUnwrappedWithTypeInfoAndFeatureDisabled() throws Exception
    {
        Outer81 outer = new Outer81();
        outer.setP1("101");

        Inner81 inner = new Inner81();
        inner.setP2("202");
        outer.setInner(inner);

        ObjectMapper mapper = jsonMapperBuilder()
                .disable(SerializationFeature.FAIL_ON_UNWRAPPED_TYPE_IDENTIFIERS)
                .enable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY)
                .build();

        String json = mapper.writeValueAsString(outer);

        assertEquals("{\"@type\":\"OuterType\",\"p2\":\"202\",\"p1\":\"101\"}", json);
    }

    // [databind#1559]
    @Test
    public void testCanSerializeSimpleWithDefaultView() throws Exception
    {
        String json = jsonMapperBuilder()
                .disable(MapperFeature.DEFAULT_VIEW_INCLUSION)
                .build()
                .writeValueAsString(new Health());
        assertEquals(a2q("{}"), json);
        // and just in case this, although won't matter wrt output
        json = jsonMapperBuilder()
                .enable(MapperFeature.DEFAULT_VIEW_INCLUSION)
                .build()
                .writeValueAsString(new Health());
        assertEquals(a2q("{}"), json);
    }

    // [databind#2461]
    @Test
    public void testUnwrappedCaching() throws Exception {
        final InnerContainer inner = new InnerContainer(new Base("12345"));
        final OuterContainer outer = new OuterContainer(inner);

        final String EXP_INNER = "{\"base.id\":\"12345\"}";
        final String EXP_OUTER = "{\"container.base.id\":\"12345\"}";

        final ObjectMapper mapperOrder1 = newJsonMapper();
        assertEquals(EXP_OUTER, mapperOrder1.writeValueAsString(outer));
        assertEquals(EXP_INNER, mapperOrder1.writeValueAsString(inner));
        assertEquals(EXP_OUTER, mapperOrder1.writeValueAsString(outer));

        final ObjectMapper mapperOrder2 = newJsonMapper();
        assertEquals(EXP_INNER, mapperOrder2.writeValueAsString(inner));
        //  Used to fail here
        assertEquals(EXP_OUTER, mapperOrder2.writeValueAsString(outer));
    }

    // [databind#3277]
    @Test
    public void testIsInstanceOfDouble() throws Exception
    {
        Holder holder = MAPPER.readValue("{\"value1\": -60.0, \"value2\": -60.0}", Holder.class);

        // Validate type
        assertEquals(Double.class, holder.value1.getClass());
        assertEquals(Double.class, holder.holder2.data.get("value2").getClass());
        // Validate value
        assertEquals(-60.0, holder.value1);
        assertEquals(-60.0, holder.holder2.data.get("value2"));
    }
}
