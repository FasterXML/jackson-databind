package tools.jackson.databind.deser.creators;

import java.math.BigDecimal;
import java.util.*;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.*;

import tools.jackson.core.*;
import tools.jackson.databind.*;
import tools.jackson.databind.testutil.DatabindTestUtil.Point;
import tools.jackson.databind.util.TokenBuffer;

import static org.junit.jupiter.api.Assertions.*;

import static tools.jackson.databind.testutil.DatabindTestUtil.*;

public class DelegatingCreatorsTest
{
    static class BooleanBean
    {
        protected Boolean value;

        public BooleanBean(Boolean v) { value = v; }

        @JsonCreator
        protected static BooleanBean create(Boolean value) {
            return new BooleanBean(value);
        }
    }

    static class IntegerBean
    {
        protected Integer value;

        public IntegerBean(Integer v) { value = v; }

        @JsonCreator
        protected static IntegerBean create(Integer value) {
            return new IntegerBean(value);
        }
    }

    static class LongBean
    {
        protected Long value;

        public LongBean(Long v) { value = v; }

        @JsonCreator
        protected static LongBean create(Long value) {
            return new LongBean(value);
        }
    }

    static class CtorBean711
    {
        protected String name;
        protected int age;

        @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
        public CtorBean711(@JacksonInject String n, int a)
        {
            name = n;
            age = a;
        }
    }

    static class FactoryBean711
    {
        protected String name1;
        protected String name2;
        protected int age;

        private FactoryBean711(int a, String n1, String n2) {
            age = a;
            name1 = n1;
            name2 = n2;
        }

        @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
        public static FactoryBean711 create(@JacksonInject String n1, int a, @JacksonInject String n2) {
            return new FactoryBean711(a, n1, n2);
        }
    }

    static class Value592
    {
        protected Object stuff;

        protected Value592(Object ob, boolean bogus) {
            stuff = ob;
        }

        @JsonCreator
        public static Value592 from(TokenBuffer buffer) {
            return new Value592(buffer, false);
        }
    }

    static class MapBean
    {
        protected Map<String,Long> map;

        @JsonCreator
        public MapBean(Map<String, Long> map) {
            this.map = map;
        }
    }

    // [databind#4688]
    static final class NoFieldSingletonWithDelegatingCreator {
        static final NoFieldSingletonWithDelegatingCreator INSTANCE = new NoFieldSingletonWithDelegatingCreator();

        private NoFieldSingletonWithDelegatingCreator() {}

        @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
        static NoFieldSingletonWithDelegatingCreator of() {
            return INSTANCE;
        }
    }

    // [databind#4688]
    static final class NoFieldSingletonWithPropertiesCreator {
        static final NoFieldSingletonWithPropertiesCreator INSTANCE = new NoFieldSingletonWithPropertiesCreator();

        private NoFieldSingletonWithPropertiesCreator() {}

        @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
        static NoFieldSingletonWithPropertiesCreator of() {
            return INSTANCE;
        }
    }

    // [databind#4688]
    static final class NoFieldSingletonWithDefaultCreator {
        static final NoFieldSingletonWithDefaultCreator INSTANCE = new NoFieldSingletonWithDefaultCreator();

        private NoFieldSingletonWithDefaultCreator() {}

        @JsonCreator
        static NoFieldSingletonWithDefaultCreator of() {
            return INSTANCE;
        }
    }

    // [databind#1003]
    public interface Hero1003 { }

    // [databind#1003]
    static class HeroBattle1003 {

        private final Hero1003 hero;

        HeroBattle1003(Hero1003 hero) {
            if (hero == null) throw new Error();
            this.hero = hero;
        }

        @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.EXTERNAL_PROPERTY, property = "heroType")
        public Hero1003 getHero() {
            return hero;
        }

        @JsonCreator
        static HeroBattle1003 fromJson(Delegate1003 json) {
            return new HeroBattle1003(json.hero);
        }
    }

    // [databind#1003]
    static class Delegate1003 {
        @JsonProperty
        @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.EXTERNAL_PROPERTY, property = "heroType")
        public Hero1003 hero;
    }

    // [databind#1003]
    static class Superman1003 implements Hero1003 {
        String name = "superman";

        public String getName() {
            return name;
        }
    }

    // For [databind#580]
    @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS)
    static abstract class Issue580Base {
    }

    static class Issue580Impl extends Issue580Base {
        public int id = 3;

        public Issue580Impl() { }
        public Issue580Impl(int id) { this.id = id; }
    }

    static class Issue580Bean {
        public Issue580Base value;

        @JsonCreator
        public Issue580Bean(Issue580Base v) {
            value = v;
        }

        @JsonValue
        public Issue580Base value() {
            return value;
        }
    }

    // [TestConstructFromMap]

    static class ConstructorFromMap
    {
        int _x;
        String _y;

        @JsonCreator
        ConstructorFromMap(Map<?,?> arg)
        {
            _x = ((Number) arg.get("x")).intValue();
            _y = (String) arg.get("y");
        }
    }

    static class FactoryFromPoint
    {
        int _x, _y;

        private FactoryFromPoint(Point p) {
            _x = p.x;
            _y = p.y;
        }

        @JsonCreator
        static FactoryFromPoint createIt(Point p)
        {
            return new FactoryFromPoint(p);
        }
    }

    // Also: let's test BigDecimal-from-JSON-String factory
    static class FactoryFromDecimalString
    {
        int _value;

        private FactoryFromDecimalString(BigDecimal d) {
	    _value = d.intValue();
        }

        @JsonCreator
        static FactoryFromDecimalString whateverNameWontMatter(BigDecimal d)
        {
            return new FactoryFromDecimalString(d);
        }
    }

    // [databind#2353]: allow delegating and properties-based
    static class SuperToken2353 {
        public long time;
        public String username;

        @JsonCreator(mode=JsonCreator.Mode.DELEGATING) // invoked when a string is passed
        public static SuperToken2353 from(String username) {
            SuperToken2353 token = new SuperToken2353();
            token.username = username;
            token.time = System.currentTimeMillis();
            return token;
        }

        @JsonCreator(mode=JsonCreator.Mode.PROPERTIES) // invoked when an object is passed, pre-validating property existence
        public static SuperToken2353 create(
                @JsonProperty("name") String username,
                @JsonProperty("time") long time)
        {
            SuperToken2353 token = new SuperToken2353();
            token.username = username;
            token.time = time;

            return token;
        }
    }

    /*
    /**********************************************************
    /* Test methods
    /**********************************************************
     */

    private final ObjectMapper MAPPER = newJsonMapper();

    @Test
    public void testBooleanDelegate() throws Exception
    {
        // should obviously work with booleans...
        BooleanBean bb = MAPPER.readValue("true", BooleanBean.class);
        assertEquals(Boolean.TRUE, bb.value);

        // but also with value conversion from String
        bb = MAPPER.readValue(q("true"), BooleanBean.class);
        assertEquals(Boolean.TRUE, bb.value);
    }

    @Test
    public void testIntegerDelegate() throws Exception
    {
        IntegerBean bb = MAPPER.readValue("-13", IntegerBean.class);
        assertEquals(Integer.valueOf(-13), bb.value);

        // but also with value conversion from String (unless blocked)
        bb = MAPPER.readValue(q("127"), IntegerBean.class);
        assertEquals(Integer.valueOf(127), bb.value);
    }

    @Test
    public void testLongDelegate() throws Exception
    {
        LongBean bb = MAPPER.readValue("11", LongBean.class);
        assertEquals(Long.valueOf(11L), bb.value);

        // but also with value conversion from String (unless blocked)
        bb = MAPPER.readValue(q("-99"), LongBean.class);
        assertEquals(Long.valueOf(-99L), bb.value);
    }

    // should also work with delegate model (single non-annotated arg)
    @Test
    public void testWithCtorAndDelegate() throws Exception
    {
        ObjectMapper mapper = jsonMapperBuilder()
                .injectableValues(new InjectableValues.Std()
                        .addValue(String.class, "Pooka"))
                .build();
        CtorBean711 bean = mapper.readValue("38", CtorBean711.class);
        assertEquals(38, bean.age);
        assertEquals("Pooka", bean.name);
    }

    @Test
    public void testWithFactoryAndDelegate() throws Exception
    {
        ObjectMapper mapper = jsonMapperBuilder()
                .injectableValues(new InjectableValues.Std()
                        .addValue(String.class, "Fygar"))
                .build();
        FactoryBean711 bean = mapper.readValue("38", FactoryBean711.class);
        assertEquals(38, bean.age);
        assertEquals("Fygar", bean.name1);
        assertEquals("Fygar", bean.name2);
    }

    // [databind#592]
    @Test
    public void testDelegateWithTokenBuffer() throws Exception
    {
        Value592 value = MAPPER.readValue("{\"a\":1,\"b\":2}", Value592.class);
        assertNotNull(value);
        Object ob = value.stuff;
        assertEquals(TokenBuffer.class, ob.getClass());
        JsonParser p = ((TokenBuffer) ob).asParser(ObjectReadContext.empty());
        assertToken(JsonToken.START_OBJECT, p.nextToken());
        assertToken(JsonToken.PROPERTY_NAME, p.nextToken());
        assertEquals("a", p.currentName());
        assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
        assertEquals(1, p.getIntValue());
        assertToken(JsonToken.PROPERTY_NAME, p.nextToken());
        assertEquals("b", p.currentName());
        assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
        assertEquals(2, p.getIntValue());
        assertToken(JsonToken.END_OBJECT, p.nextToken());
        p.close();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testIssue465() throws Exception
    {
        final String JSON = "{\"A\":12}";

        // first, test with regular Map, non empty
        Map<String,Long> map = MAPPER.readValue(JSON, Map.class);
        assertEquals(1, map.size());
        assertEquals(Integer.valueOf(12), map.get("A"));

        MapBean bean = MAPPER.readValue(JSON, MapBean.class);
        assertEquals(1, bean.map.size());
        assertEquals(Long.valueOf(12L), bean.map.get("A"));

        // and then empty ones
        final String EMPTY_JSON = "{}";

        map = MAPPER.readValue(EMPTY_JSON, Map.class);
        assertEquals(0, map.size());

        bean = MAPPER.readValue(EMPTY_JSON, MapBean.class);
        assertEquals(0, bean.map.size());
    }

    // [databind#2353]: allow delegating and properties-based
    @Test
    public void testMultipleCreators2353() throws Exception
    {
        // first, test delegating
        SuperToken2353 result = MAPPER.readValue(q("Bob"), SuperToken2353.class);
        assertEquals("Bob", result.username);

        // and then properties-based
        result = MAPPER.readValue(a2q("{'name':'Billy', 'time':123}"), SuperToken2353.class);
        assertEquals("Billy", result.username);
        assertEquals(123L, result.time);
    }

    // [databind#4688]
    @Test
    public void testNoFieldSingletonWithDelegatingCreator() throws Exception
    {
        NoFieldSingletonWithDelegatingCreator deserialized = MAPPER.readValue("{}",
                NoFieldSingletonWithDelegatingCreator.class);
        assertSame(NoFieldSingletonWithDelegatingCreator.INSTANCE, deserialized);
    }

    // [databind#4688]
    @Test
    public void testNoFieldSingletonWithPropertiesCreator() throws Exception
    {
        NoFieldSingletonWithPropertiesCreator deserialized = MAPPER.readValue("{}",
                NoFieldSingletonWithPropertiesCreator.class);
        assertSame(NoFieldSingletonWithPropertiesCreator.INSTANCE, deserialized);
    }

    // [databind#4688]
    @Test
    public void testNoFieldSingletonWithDefaultCreator() throws Exception
    {
        NoFieldSingletonWithDefaultCreator deserialized = MAPPER.readValue("{}",
                NoFieldSingletonWithDefaultCreator.class);
        assertSame(NoFieldSingletonWithDefaultCreator.INSTANCE, deserialized);
    }

    // [databind#1003]
    @Test
    public void testExtrnalPropertyDelegatingCreator() throws Exception
    {
        final String json = MAPPER.writeValueAsString(new HeroBattle1003(new Superman1003()));
        final HeroBattle1003 battle = MAPPER.readValue(json, HeroBattle1003.class);

        assertInstanceOf(Superman1003.class, battle.getHero());
    }

    // [databind#580]
    @Test
    public void testAbstractDelegateWithCreator() throws Exception
    {
        Issue580Bean input = new Issue580Bean(new Issue580Impl(13));
        String json = MAPPER.writeValueAsString(input);
        Issue580Bean result = MAPPER.readValue(json, Issue580Bean.class);
        assertNotNull(result);
        assertNotNull(result.value);
        assertEquals(13, ((Issue580Impl) result.value).id);
    }

    // [TestConstructFromMap]

    @Test
    public void testViaConstructor() throws Exception
    {
        ConstructorFromMap result = MAPPER.readValue
            ("{ \"x\":1, \"y\" : \"abc\" }", ConstructorFromMap.class);
        assertEquals(1, result._x);
        assertEquals("abc", result._y);
    }

    @Test
    public void testViaFactory() throws Exception
    {
        FactoryFromPoint result = MAPPER.readValue("{ \"x\" : 3, \"y\" : 4 }", FactoryFromPoint.class);
        assertEquals(3, result._x);
        assertEquals(4, result._y);
    }

    @Test
    public void testViaFactoryUsingString() throws Exception
    {
        FactoryFromDecimalString result = MAPPER.readValue("\"12.57\"", FactoryFromDecimalString.class);
        assertNotNull(result);
        assertEquals(12, result._value);
    }
}
