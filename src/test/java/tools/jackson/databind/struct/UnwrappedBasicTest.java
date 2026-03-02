package tools.jackson.databind.struct;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.*;

import tools.jackson.databind.*;
import tools.jackson.databind.exc.UnrecognizedPropertyException;
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
    // // // Inner types for basic @JsonUnwrapped tests

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

    static class Location {
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

    // // // Inner types for USE_NULL_FOR_EMPTY_UNWRAPPED tests [databind#1709]

    @JsonInclude(JsonInclude.Include.NON_DEFAULT)
    static class Container1709 {
        public String name;
        @JsonUnwrapped
        public Unwrapped1709 u;
    }

    @JsonInclude(JsonInclude.Include.NON_DEFAULT)
    static class Unwrapped1709 {
        public String s;
        public Integer n;
    }

    // // // Inner types for @JsonUnwrapped with @JsonCreator [databind#1467]

    static class ExplicitWithoutName {
        private final String _unrelated;
        private final Inner1467 _inner;

        @JsonCreator
        public ExplicitWithoutName(@JsonProperty("unrelated") String unrelated, @JsonUnwrapped Inner1467 inner) {
            _unrelated = unrelated;
            _inner = inner;
        }

        public String getUnrelated() {
            return _unrelated;
        }

        @JsonUnwrapped
        public Inner1467 getInner() {
            return _inner;
        }
    }

    static class ExplicitWithName {
        private final String _unrelated;
        private final Inner1467 _inner;

        @JsonCreator
        public ExplicitWithName(@JsonProperty("unrelated") String unrelated, @JsonProperty("inner") @JsonUnwrapped Inner1467 inner) {
            _unrelated = unrelated;
            _inner = inner;
        }

        public String getUnrelated() {
            return _unrelated;
        }

        public Inner1467 getInner() {
            return _inner;
        }
    }

    static class ImplicitWithName {
        private final String _unrelated;
        private final Inner1467 _inner;

        public ImplicitWithName(@JsonProperty("unrelated") String unrelated, @JsonProperty("inner") @JsonUnwrapped Inner1467 inner) {
            _unrelated = unrelated;
            _inner = inner;
        }

        public String getUnrelated() {
            return _unrelated;
        }

        public Inner1467 getInner() {
            return _inner;
        }
    }

    static class WithTwoUnwrappedProperties {
        private final String _unrelated;
        private final Inner1467 _inner1;
        private final Inner1467 _inner2;

        public WithTwoUnwrappedProperties(
                @JsonProperty("unrelated") String unrelated,
                @JsonUnwrapped(prefix = "first-") Inner1467 inner1,
                @JsonUnwrapped(prefix = "second-") Inner1467 inner2
        ) {
            _unrelated = unrelated;
            _inner1 = inner1;
            _inner2 = inner2;
        }

        public String getUnrelated() {
            return _unrelated;
        }

        @JsonUnwrapped(prefix = "first-")
        public Inner1467 getInner1() {
            return _inner1;
        }

        @JsonUnwrapped(prefix = "second-")
        public Inner1467 getInner2() {
            return _inner2;
        }
    }

    static class Inner1467 {
        private final String _property1;
        private final String _property2;

        public Inner1467(@JsonProperty("property1") String property1, @JsonProperty("property2") String property2) {
            _property1 = property1;
            _property2 = property2;
        }

        public String getProperty1() {
            return _property1;
        }

        public String getProperty2() {
            return _property2;
        }
    }

    static class PrefixOuter {
        @JsonUnwrapped(prefix = "inner-")
        PrefixInner inner;
    }

    static class PrefixInner {
        private final String _property;

        public PrefixInner(@JsonProperty("property") String property) {
            _property = property;
        }

        public String getProperty() {
            return _property;
        }
    }

    // // // Inner types for @JsonUnwrapped with prefix/suffix tests

    // Class with unwrapping using prefixes
    static class PrefixUnwrap
    {
        public String name;
        @JsonUnwrapped(prefix="_")
        public Location location;

        public PrefixUnwrap() { }
        protected PrefixUnwrap(String str, int x, int y) {
            name = str;
            location = new Location(x, y);
        }
    }

    static class DeepPrefixUnwrap
    {
        @JsonUnwrapped(prefix="u.")
        public PrefixUnwrap unwrapped;

        public DeepPrefixUnwrap() { }
        protected DeepPrefixUnwrap(String str, int x, int y) {
            unwrapped = new PrefixUnwrap(str, x, y);
        }
    }

    // Let's actually test hierarchic names with unwrapping bit more:
    @JsonPropertyOrder({ "general", "misc" })
    static class ConfigRoot
    {
        @JsonUnwrapped(prefix="general.")
        public ConfigGeneral general = new ConfigGeneral();

        @JsonUnwrapped(prefix="misc.")
        public ConfigMisc misc = new ConfigMisc();

        public ConfigRoot() { }
        protected ConfigRoot(String name, int value)
        {
            general = new ConfigGeneral(name);
            misc.value = value;
        }
    }

    static class ConfigAlternate
    {
        @JsonUnwrapped
        public ConfigGeneral general = new ConfigGeneral();

        @JsonUnwrapped(prefix="misc.")
        public ConfigMisc misc = new ConfigMisc();

        public int id;

        public ConfigAlternate() { }
        protected ConfigAlternate(int id, String name, int value)
        {
            this.id = id;
            general = new ConfigGeneral(name);
            misc.value = value;
        }
    }

    static class ConfigGeneral
    {
        @JsonUnwrapped(prefix="names.")
        public ConfigNames names = new ConfigNames();

        public ConfigGeneral() { }
        protected ConfigGeneral(String name) {
            names.name = name;
        }
    }

    static class ConfigNames {
        public String name = "x";
    }

    static class ConfigMisc {
        public int value;
    }

    // For [Issue#226]
    static class Parent226 {
        @JsonUnwrapped(prefix="c1.")
        public Child226 c1;
        @JsonUnwrapped(prefix="c2.")
        public Child226 c2;
    }

    static class Child226 {
        @JsonUnwrapped(prefix="sc2.")
        public SubChild sc1;
    }

    static class SubChild {
        public String value;
    }

    // // // Inner types for @JsonUnwrapped with unknown property handling [databind#650]

    static class A650 {
        @JsonUnwrapped
        public B650 b;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    static class A650WithUnknownsOk {
        @JsonUnwrapped
        public B650 b;
    }

    static class B650 {
        public String field;
    }

    // For prefix/suffix
    static class A650WithPrefix {
        @JsonUnwrapped(prefix = "nested.")
        public B650 b;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    static class A650WithPrefixUnknownsOk {
        @JsonUnwrapped(prefix = "nested.")
        public B650 b;
    }

    // For @JsonCreator + @JsonUnwrapped
    static class A650WithCreator {
        public String name;

        @JsonUnwrapped
        public B650 b;

        @JsonCreator
        public A650WithCreator(@JsonProperty("name") String name) {
            this.name = name;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    static class A650WithCreatorUnknownsOk {
        public String name;

        @JsonUnwrapped
        public B650 b;

        @JsonCreator
        public A650WithCreatorUnknownsOk(@JsonProperty("name") String name) {
            this.name = name;
        }
    }

    // For @JsonCreator + @JsonUnwrapped with prefix
    static class A650WithCreatorAndPrefix {
        public String name;

        @JsonUnwrapped(prefix = "nested.")
        public B650 b;

        @JsonCreator
        public A650WithCreatorAndPrefix(@JsonProperty("name") String name) {
            this.name = name;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    static class A650WithCreatorAndPrefixUnknownsOk {
        public String name;

        @JsonUnwrapped(prefix = "nested.")
        public B650 b;

        @JsonCreator
        public A650WithCreatorAndPrefixUnknownsOk(@JsonProperty("name") String name) {
            this.name = name;
        }
    }

    /*
    /**********************************************************
    /* Mapper instances
    /**********************************************************
     */

    private final ObjectMapper MAPPER = newJsonMapper();

    // [databind#1709]
    private final ObjectMapper MAPPER_ENABLED = jsonMapperBuilder()
            .enable(DeserializationFeature.USE_NULL_FOR_EMPTY_UNWRAPPED)
            .build();

    private final ObjectMapper MAPPER_DISABLED = jsonMapperBuilder()
            .disable(DeserializationFeature.USE_NULL_FOR_EMPTY_UNWRAPPED)
            .build();

    // [databind#650]: with FAIL_ON_UNKNOWN_PROPERTIES enabled
    private final ObjectMapper STRICT_MAPPER = JsonMapper.builder()
            .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .build();

    /*
    /**********************************************************
    /* Tests, serialization
    /**********************************************************
     */

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

    /*
    /**********************************************************
    /* Tests, USE_NULL_FOR_EMPTY_UNWRAPPED [databind#1709]
    /**********************************************************
     */

    @Test
    public void testEmptyUnwrappedAsNull() throws Exception {
        String json = a2q("{'name':'test'}");
        Container1709 result = MAPPER_ENABLED.readValue(json, Container1709.class);
        assertNotNull(result);
        assertEquals("test", result.name);
        assertNull(result.u);
    }

    @Test
    public void testEmptyJsonEmptyUnwrappedAsNull() throws Exception {
        Container1709 result = MAPPER_ENABLED.readValue("{}", Container1709.class);
        assertNotNull(result);
        assertNull(result.name);
        assertNull(result.u);
    }

    @Test
    public void testNonNullUnwrappedPreserved() throws Exception {
        String json = a2q("{'name':'test','s':'value'}");
        Container1709 result = MAPPER_ENABLED.readValue(json, Container1709.class);
        assertNotNull(result);
        assertEquals("test", result.name);
        assertNotNull(result.u);
        assertEquals("value", result.u.s);
    }

    @Test
    public void testPartialNonNullUnwrappedPreserved() throws Exception {
        String json = a2q("{'s':'value'}");
        Container1709 result = MAPPER_ENABLED.readValue(json, Container1709.class);
        assertNotNull(result);
        assertNotNull(result.u);
        assertEquals("value", result.u.s);
        assertNull(result.u.n);
    }

    @Test
    public void testEmptyUnwrappedAsNullWhenDisabled() throws Exception {
        String json = a2q("{'name':'test'}");
        Container1709 result = MAPPER_DISABLED.readValue(json, Container1709.class);
        assertNotNull(result);
        assertEquals("test", result.name);
        assertNotNull(result.u);
        assertNull(result.u.s);
        assertNull(result.u.n);
    }

    @Test
    public void testEmptyJsonEmptyUnwrappedAsNullWhenDisabled() throws Exception {
        Container1709 result = MAPPER_DISABLED.readValue("{}", Container1709.class);
        assertNotNull(result);
        assertNull(result.name);
        assertNotNull(result.u);
        assertNull(result.u.s);
        assertNull(result.u.n);
    }

    @Test
    public void testNonNullUnwrappedPreservedWhenDisabled() throws Exception {
        String json = a2q("{'name':'test','s':'value'}");
        Container1709 result = MAPPER_DISABLED.readValue(json, Container1709.class);
        assertNotNull(result);
        assertEquals("test", result.name);
        assertNotNull(result.u);
        assertEquals("value", result.u.s);
    }

    @Test
    public void testPartialNonNullUnwrappedPreservedWhenDisabled() throws Exception {
        String json = a2q("{'s':'value'}");
        Container1709 result = MAPPER_DISABLED.readValue(json, Container1709.class);
        assertNull(result.u.n);
    }

    /*
    /**********************************************************
    /* Tests, @JsonUnwrapped with @JsonCreator [databind#1467]
    /**********************************************************
     */

    @Test
    public void testUnwrappedWithJsonCreatorWithExplicitWithoutName() throws Exception
    {
        String json = "{\"unrelated\": \"unrelatedValue\", \"property1\": \"value1\", \"property2\": \"value2\"}";
        ExplicitWithoutName outer = MAPPER.readValue(json, ExplicitWithoutName.class);

        assertEquals("unrelatedValue", outer.getUnrelated());
        assertEquals("value1", outer.getInner().getProperty1());
        assertEquals("value2", outer.getInner().getProperty2());
    }

    @Test
    public void testUnwrappedWithJsonCreatorExplicitWithName() throws Exception
    {
        String json = "{\"unrelated\": \"unrelatedValue\", \"property1\": \"value1\", \"property2\": \"value2\"}";
        ExplicitWithName outer = MAPPER.readValue(json, ExplicitWithName.class);

        assertEquals("unrelatedValue", outer.getUnrelated());
        assertEquals("value1", outer.getInner().getProperty1());
        assertEquals("value2", outer.getInner().getProperty2());
    }

    @Test
    public void testUnwrappedWithJsonCreatorImplicitWithName() throws Exception
    {
        String json = "{\"unrelated\": \"unrelatedValue\", \"property1\": \"value1\", \"property2\": \"value2\"}";
        ImplicitWithName outer = MAPPER.readValue(json, ImplicitWithName.class);

        assertEquals("unrelatedValue", outer.getUnrelated());
        assertEquals("value1", outer.getInner().getProperty1());
        assertEquals("value2", outer.getInner().getProperty2());
    }

    @Test
    public void testUnwrappedWithTwoUnwrappedProperties() throws Exception
    {
        String json = "{\"unrelated\": \"unrelatedValue\", " +
                "\"first-property1\": \"first-value1\", \"first-property2\": \"first-value2\", " +
                "\"second-property1\": \"second-value1\", \"second-property2\": \"second-value2\"}";
        WithTwoUnwrappedProperties outer = MAPPER.readValue(json, WithTwoUnwrappedProperties.class);

        assertEquals("unrelatedValue", outer.getUnrelated());
        assertEquals("first-value1", outer.getInner1().getProperty1());
        assertEquals("first-value2", outer.getInner1().getProperty2());
        assertEquals("second-value1", outer.getInner2().getProperty1());
        assertEquals("second-value2", outer.getInner2().getProperty2());
    }

    @Test
    public void testUnwrappedWithPrefixCreator() throws Exception
    {
        String json = "{\"inner-property\": \"value\"}";
        PrefixOuter outer = MAPPER.readValue(json, PrefixOuter.class);

        assertEquals("value", outer.inner.getProperty());
    }

    /*
    /**********************************************************
    /* Tests, @JsonUnwrapped with prefix/suffix
    /**********************************************************
     */

    @Test
    public void testPrefixedUnwrappingSerialize() throws Exception
    {
        JsonMapper mapper = JsonMapper.builder().enable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY).build();
        assertEquals("{\"_x\":1,\"_y\":2,\"name\":\"Tatu\"}",
                mapper.writeValueAsString(new PrefixUnwrap("Tatu", 1, 2)));
    }

    @Test
    public void testDeepPrefixedUnwrappingSerialize() throws Exception
    {
        JsonMapper mapper = JsonMapper.builder().enable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY).build();
        String json = mapper.writeValueAsString(new DeepPrefixUnwrap("Bubba", 1, 1));
        assertEquals("{\"u._x\":1,\"u._y\":1,\"u.name\":\"Bubba\"}", json);
    }

    @Test
    public void testHierarchicConfigSerialize() throws Exception
    {
        String json = MAPPER.writeValueAsString(new ConfigRoot("Fred", 25));
        assertEquals("{\"general.names.name\":\"Fred\",\"misc.value\":25}", json);
    }

    @Test
    public void testPrefixedUnwrapDeserialize() throws Exception
    {
        PrefixUnwrap bean = MAPPER.readValue("{\"name\":\"Axel\",\"_x\":4,\"_y\":7}", PrefixUnwrap.class);
        assertNotNull(bean);
        assertEquals("Axel", bean.name);
        assertNotNull(bean.location);
        assertEquals(4, bean.location.x);
        assertEquals(7, bean.location.y);
    }

    @Test
    public void testDeepPrefixedUnwrapDeserialize() throws Exception
    {
        DeepPrefixUnwrap bean = MAPPER.readValue("{\"u.name\":\"Bubba\",\"u._x\":2,\"u._y\":3}",
                DeepPrefixUnwrap.class);
        assertNotNull(bean.unwrapped);
        assertNotNull(bean.unwrapped.location);
        assertEquals(2, bean.unwrapped.location.x);
        assertEquals(3, bean.unwrapped.location.y);
        assertEquals("Bubba", bean.unwrapped.name);
    }

    @Test
    public void testHierarchicConfigDeserialize() throws Exception
    {
        ConfigRoot root = MAPPER.readValue("{\"general.names.name\":\"Bob\",\"misc.value\":3}",
                ConfigRoot.class);
        assertNotNull(root.general);
        assertNotNull(root.general.names);
        assertNotNull(root.misc);
        assertEquals(3, root.misc.value);
        assertEquals("Bob", root.general.names.name);
    }

    @Test
    public void testHierarchicConfigRoundTrip() throws Exception
    {
        ConfigAlternate input = new ConfigAlternate(123, "Joe", 42);
        String json = MAPPER.writeValueAsString(input);

        ConfigAlternate root = MAPPER.readValue(json, ConfigAlternate.class);
        assertEquals(123, root.id);
        assertNotNull(root.general);
        assertNotNull(root.general.names);
        assertNotNull(root.misc);
        assertEquals("Joe", root.general.names.name);
        assertEquals(42, root.misc.value);
    }

    // [Issue#226]
    @Test
    public void testIssue226() throws Exception
    {
        Parent226 input = new Parent226();
        input.c1 = new Child226();
        input.c1.sc1 = new SubChild();
        input.c1.sc1.value = "a";
        input.c2 = new Child226();
        input.c2.sc1 = new SubChild();
        input.c2.sc1.value = "b";

        String json = MAPPER.writeValueAsString(input);

        Parent226 output = MAPPER.readValue(json, Parent226.class);
        assertNotNull(output.c1);
        assertNotNull(output.c2);

        assertNotNull(output.c1.sc1);
        assertNotNull(output.c2.sc1);

        assertEquals("a", output.c1.sc1.value);
        assertEquals("b", output.c2.sc1.value);
    }

    /*
    /**********************************************************
    /* Tests, @JsonUnwrapped with unknown property handling [databind#650]
    /**********************************************************
     */

    @Test
    public void testFailOnUnknownPropertyUnwrapped() throws Exception {
        final String json = a2q("{'field': 'value', 'bad': 'bad value'}");
        try {
            STRICT_MAPPER.readValue(json, A650.class);
            fail("Exception was not thrown on unknown property");
        } catch (UnrecognizedPropertyException e) {
            verifyException(e, "Unrecognized property");
        }
    }

    @Test
    public void testWorkOnUnknownWithAnnotation() throws Exception {
        final String json = a2q("{'field': 'value', 'bad': 'bad value'}");
        A650WithUnknownsOk a = STRICT_MAPPER.readValue(json, A650WithUnknownsOk.class);
        assertEquals("value", a.b.field);
    }

    // Passing case, regular usage
    @Test
    public void testWorksOnRegularPropertyUnwrapped() throws Exception {
        A650 value = STRICT_MAPPER.readValue(a2q("{'field': 'value'}"), A650.class);
        assertEquals("value", value.b.field);
    }

    // Tests for @JsonUnwrapped with prefix
    @Test
    public void testFailOnUnknownPropertyUnwrappedWithPrefix() throws Exception {
        final String json = a2q("{'nested.field': 'value', 'bad': 'bad value'}");
        try {
            STRICT_MAPPER.readValue(json, A650WithPrefix.class);
            fail("Exception was not thrown on unknown property");
        } catch (UnrecognizedPropertyException e) {
            verifyException(e, "Unrecognized property");
        }
    }

    @Test
    public void testWorkOnUnknownWithPrefixAndAnnotation() throws Exception {
        final String json = a2q("{'nested.field': 'value', 'bad': 'bad value'}");
        A650WithPrefixUnknownsOk a = STRICT_MAPPER.readValue(json, A650WithPrefixUnknownsOk.class);
        assertEquals("value", a.b.field);
    }

    @Test
    public void testWorksOnRegularPropertyUnwrappedWithPrefix() throws Exception {
        A650WithPrefix value = STRICT_MAPPER.readValue(a2q("{'nested.field': 'value'}"), A650WithPrefix.class);
        assertEquals("value", value.b.field);
    }

    // Tests for @JsonCreator + @JsonUnwrapped (deserializeUsingPropertyBasedWithUnwrapped)
    @Test
    public void testFailOnUnknownPropertyWithCreator() throws Exception {
        final String json = a2q("{'name': 'test', 'field': 'value', 'bad': 'bad value'}");
        try {
            STRICT_MAPPER.readValue(json, A650WithCreator.class);
            fail("Exception was not thrown on unknown property");
        } catch (UnrecognizedPropertyException e) {
            verifyException(e, "Unrecognized property");
        }
    }

    @Test
    public void testWorkOnUnknownWithCreatorAndAnnotation() throws Exception {
        final String json = a2q("{'name': 'test', 'field': 'value', 'bad': 'bad value'}");
        A650WithCreatorUnknownsOk a = STRICT_MAPPER.readValue(json, A650WithCreatorUnknownsOk.class);
        assertEquals("test", a.name);
        assertEquals("value", a.b.field);
    }

    @Test
    public void testWorksOnRegularPropertyWithCreator() throws Exception {
        A650WithCreator value = STRICT_MAPPER.readValue(a2q("{'name': 'test', 'field': 'value'}"), A650WithCreator.class);
        assertEquals("test", value.name);
        assertEquals("value", value.b.field);
    }

    // Tests for @JsonCreator + @JsonUnwrapped with prefix
    @Test
    public void testFailOnUnknownPropertyWithCreatorAndPrefix() throws Exception {
        final String json = a2q("{'name': 'test', 'nested.field': 'value', 'bad': 'bad value'}");
        try {
            STRICT_MAPPER.readValue(json, A650WithCreatorAndPrefix.class);
            fail("Exception was not thrown on unknown property");
        } catch (UnrecognizedPropertyException e) {
            verifyException(e, "Unrecognized property");
        }
    }

    @Test
    public void testWorkOnUnknownWithCreatorAndPrefixAndAnnotation() throws Exception {
        final String json = a2q("{'name': 'test', 'nested.field': 'value', 'bad': 'bad value'}");
        A650WithCreatorAndPrefixUnknownsOk a = STRICT_MAPPER.readValue(json, A650WithCreatorAndPrefixUnknownsOk.class);
        assertEquals("test", a.name);
        assertEquals("value", a.b.field);
    }

    @Test
    public void testWorksOnRegularPropertyWithCreatorAndPrefix() throws Exception {
        A650WithCreatorAndPrefix value = STRICT_MAPPER.readValue(
                a2q("{'name': 'test', 'nested.field': 'value'}"), A650WithCreatorAndPrefix.class);
        assertEquals("test", value.name);
        assertEquals("value", value.b.field);
    }
}
