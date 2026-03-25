package tools.jackson.databind.ser;

import java.math.BigDecimal;
import java.util.*;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.*;

import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.*;
import tools.jackson.databind.annotation.JsonSerialize;
import tools.jackson.databind.module.SimpleModule;
import tools.jackson.databind.ser.std.StdScalarSerializer;
import tools.jackson.databind.ser.std.ToStringSerializer;
import tools.jackson.databind.testutil.DatabindTestUtil;
import tools.jackson.databind.util.RawValue;

import static org.junit.jupiter.api.Assertions.*;

/**
 * This unit test suite tests functioning of {@link JsonValue}
 * annotation with bean serialization.
 */
@SuppressWarnings("serial")
public class JsonValueSerializationTest
    extends DatabindTestUtil
{
    public static class ValueClass<T>
    {
        final T _value;

        public ValueClass(T v) { _value = v; }

        @JsonValue T value() { return _value; }
    }

    public static class FieldValueClass<T>
    {
        @JsonValue(true)
        final T _value;

        public FieldValueClass(T v) { _value = v; }
    }

    /**
     * Another test class to check that it is also possible to
     * force specific serializer to use with @JsonValue annotated
     * method. Difference is between Integer serialization, and
     * conversion to a Json String.
     */
    public final static class ToStringValueClass<T>
        extends ValueClass<T>
    {
        public ToStringValueClass(T value) { super(value); }

        // Also, need to use this annotation to help
        @JsonSerialize(using=ToStringSerializer.class)
        @Override
        @JsonValue T value() { return super.value(); }
    }

    public final static class ToStringValueClass2
        extends ValueClass<String>
    {
        public ToStringValueClass2(String value) { super(value); }

        // Simple as well, but let's ensure that other getters won't matter...

        @JsonProperty int getFoobar() { return 4; }

        public String[] getSomethingElse() { return new String[] { "1", "a" }; }
    }

    public static class ValueBase {
        public String a = "a";
    }

    public static class ValueType extends ValueBase {
        public String b = "b";
    }

    // Finally, let's also test static vs dynamic type
    public static class ValueWrapper {
        @JsonValue
        public ValueBase getX() { return new ValueType(); }
    }

    public static class MapBean
    {
        @JsonValue
        public Map<String,String> toMap()
        {
            HashMap<String,String> map = new HashMap<String,String>();
            map.put("a", "1");
            return map;
        }
    }

    public static class MapFieldBean
    {
        @JsonValue
        Map<String,String> stuff = new HashMap<>();
        {
            stuff.put("b", "2");
        }
    }

    public static class MapAsNumber extends HashMap<String,String>
    {
        @JsonValue
        public int value() { return 42; }
    }

    public static class ListAsNumber extends ArrayList<Integer>
    {
        @JsonValue
        public int value() { return 13; }
    }

    // Just to ensure it's possible to disable annotation (usually
    // via mix-ins, but here directly)
    @JsonPropertyOrder({ "x", "y" })
    public static class DisabledJsonValue {
        @JsonValue(false)
        public int x = 1;

        @JsonValue(false)
        public int getY() { return 2; }
    }

    public static class IntExtBean {
        public List<Internal> values = new ArrayList<Internal>();

        public void add(int v) { values.add(new Internal(v)); }
    }

    public static class Internal {
        public int value;

        public Internal(int v) { value = v; }

        @JsonValue
        public External asExternal() { return new External(this); }
    }

    public static class External {
        public int i;

        External(Internal e) { i = e.value; }
    }

    // [databind#167]

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "boingo")
    @JsonSubTypes(value = {@JsonSubTypes.Type(name = "boopsy", value = AdditionInterfaceImpl.class)
    })
    public static interface AdditionInterface
    {
        public int add(int in);
    }

    public static class AdditionInterfaceImpl implements AdditionInterface
    {
	    private final int toAdd;

	    @JsonCreator
	    public AdditionInterfaceImpl(@JsonProperty("toAdd") int toAdd) {
	      this.toAdd = toAdd;
	    }

	    @JsonProperty
	    public int getToAdd() {
	      return toAdd;
	    }

	    @Override
	    public int add(int in) {
	      return in + toAdd;
	    }
    }

    public static class Bean838 {
        @JsonValue
        public String value() {
            return "value";
        }
    }

    public static class Bean838Serializer extends StdScalarSerializer<Bean838>
    {
        public Bean838Serializer() {
            super(Bean838.class);
        }

        @Override
        public void serialize(Bean838 value, JsonGenerator gen,
                SerializationContext provider) {
            gen.writeNumber(42);
        }
    }

    // [databind#1806]
    public static class Bean1806 {
        @JsonValue public List<Elem1806> getThings() {
            return Collections.singletonList(new Elem1806.Impl());
        }
    }

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.WRAPPER_OBJECT)
    @JsonSubTypes(@JsonSubTypes.Type(value = Elem1806.Impl.class, name = "impl"))
    public interface Elem1806 {
        final class Impl implements Elem1806 {
            public int value = 1;
        }
    }

    // [databind#2822]
    @JsonPropertyOrder({ "description", "b" })
    public static class A2822 {
        public final String description;

        @JsonFormat(shape = JsonFormat.Shape.STRING)
        public final B2822 b;

        public A2822(final String description, final B2822 b ) {
            this.description = description;
            this.b = b;
        }
    }

    public static class B2822 {
        @JsonValue
        private final BigDecimal value;

        public B2822(final BigDecimal value ) {
            this.value = value;
        }
    }

    // [databind#348]: @JsonRawValue tests

    /// Class for testing {@link JsonRawValue} annotations with getters returning String
    @JsonPropertyOrder(alphabetic=true)
    public final static class ClassGetter<T>
    {
        protected final T _value;

        protected ClassGetter(T v) { _value = v;}

        public T getNonRaw() { return _value; }

        @JsonProperty("raw") @JsonRawValue public T foobar() { return _value; }

        @JsonProperty @JsonRawValue protected T value() { return _value; }
    }

    // [databind#348]
    public static class RawWrapped
    {
        @JsonRawValue
        private final String json;

        public RawWrapped(String str) {
            json = str;
        }
    }

    /*
    /**********************************************************************
    /* Test cases, @JsonValue
    /**********************************************************************
     */

    private final ObjectMapper MAPPER = newJsonMapper();

    @Test
    public void testSimpleMethodJsonValue() throws Exception
    {
        assertEquals("\"abc\"", MAPPER.writeValueAsString(new ValueClass<String>("abc")));
        assertEquals("null", MAPPER.writeValueAsString(new ValueClass<String>(null)));
    }

    @Test
    public void testSimpleFieldJsonValue() throws Exception
    {
        assertEquals("\"abc\"", MAPPER.writeValueAsString(new FieldValueClass<String>("abc")));
        assertEquals("null", MAPPER.writeValueAsString(new FieldValueClass<String>(null)));
    }

    @Test
    public void testJsonValueWithUseSerializer() throws Exception
    {
        assertEquals("\"123\"",
                MAPPER.writeValueAsString(new ToStringValueClass<Integer>(Integer.valueOf(123))));
    }

    /**
     * Test for verifying that additional getters won't confuse serializer.
     */
    @Test
    public void testMixedJsonValue() throws Exception
    {
        assertEquals("\"xyz\"",
                MAPPER.writeValueAsString(new ToStringValueClass2("xyz")));
    }

    @Test
    public void testDisabling() throws Exception
    {
        assertEquals(a2q("{'x':1,'y':2}"),
                MAPPER.writeValueAsString(new DisabledJsonValue()));
    }

    @Test
    public void testValueWithStaticType() throws Exception
    {
        // Ok; first, with dynamic type:
        assertEquals("{\"a\":\"a\",\"b\":\"b\"}", MAPPER.writeValueAsString(new ValueWrapper()));

        // then static
        ObjectMapper staticMapper = jsonMapperBuilder()
                .configure(MapperFeature.USE_STATIC_TYPING, true)
                .build();
        assertEquals("{\"a\":\"a\"}", staticMapper.writeValueAsString(new ValueWrapper()));
    }

    @Test
    public void testMapWithJsonValue() throws Exception {
        // First via method
        assertEquals("{\"a\":\"1\"}", MAPPER.writeValueAsString(new MapBean()));

        // then field
        assertEquals("{\"b\":\"2\"}", MAPPER.writeValueAsString(new MapFieldBean()));
    }

    @Test
    public void testWithMap() throws Exception {
        assertEquals("42", MAPPER.writeValueAsString(new MapAsNumber()));
    }

    @Test
    public void testWithList() throws Exception {
        assertEquals("13", MAPPER.writeValueAsString(new ListAsNumber()));
    }

    @Test
    public void testInList() throws Exception {
        IntExtBean bean = new IntExtBean();
        bean.add(1);
        bean.add(2);
        String json = MAPPER.writeValueAsString(bean);
        assertEquals(json, "{\"values\":[{\"i\":1},{\"i\":2}]}");
    }

    // [databind#167]
    @Test
    public void testPolymorphicSerdeWithDelegate() throws Exception
    {
	    AdditionInterface adder = new AdditionInterfaceImpl(1);

	    assertEquals(2, adder.add(1));
	    String json = MAPPER.writeValueAsString(adder);
	    assertEquals("{\"boingo\":\"boopsy\",\"toAdd\":1}", json);
	    assertEquals(2, MAPPER.readValue(json, AdditionInterface.class).add(1));
    }

    @Test
    public void testJsonValueWithCustomOverride() throws Exception
    {
        final Bean838 INPUT = new Bean838();

        // by default, @JsonValue should be used
        assertEquals(q("value"), MAPPER.writeValueAsString(INPUT));

        // but custom serializer should override it
        ObjectMapper mapper = jsonMapperBuilder()
                .addModule(new SimpleModule()
                        .addSerializer(Bean838.class, new Bean838Serializer()))
                .build();
        assertEquals("42", mapper.writeValueAsString(INPUT));
    }

    // [databind#1806]
    @Test
    public void testCollectionViaJsonValue() throws Exception
    {
        assertEquals("[{\"impl\":{\"value\":1}}]",
                MAPPER.writeValueAsString(new Bean1806()));
    }

    // [databind#2822]
    @Test
    public void testFormatWithJsonValue() throws Exception
    {
        final String json = MAPPER.writeValueAsString(new A2822("desc",
                new B2822(BigDecimal.ONE)));
        assertEquals(a2q("{'description':'desc','b':'1'}"), json);
    }

    /*
    /**********************************************************************
    /* Test cases, @JsonRawValue
    /**********************************************************************
     */

    @Test
    public void testSimpleStringGetter() throws Exception
    {
        String value = "abc";
        String result = MAPPER.writeValueAsString(new ClassGetter<String>(value));
        String expected = String.format("{\"nonRaw\":\"%s\",\"raw\":%s,\"value\":%s}", value, value, value);
        assertEquals(expected, result);
    }

    @Test
    public void testSimpleNonStringGetter() throws Exception
    {
        int value = 123;
        String result = MAPPER.writeValueAsString(new ClassGetter<Integer>(value));
        String expected = String.format("{\"nonRaw\":%d,\"raw\":%d,\"value\":%d}", value, value, value);
        assertEquals(expected, result);
    }

    @Test
    public void testNullStringGetter() throws Exception
    {
        String result = MAPPER.writeValueAsString(new ClassGetter<String>(null));
        String expected = "{\"nonRaw\":null,\"raw\":null,\"value\":null}";
        assertEquals(expected, result);
    }

    @Test
    public void testWithValueToTree() throws Exception
    {
        JsonNode w = MAPPER.valueToTree(new RawWrapped("{ }"));
        assertNotNull(w);
        assertEquals("{\"json\":{ }}", MAPPER.writeValueAsString(w));
    }

    // for [databind#743]
    @Test
    public void testRawFromMapToTree() throws Exception
    {
        RawValue myType = new RawValue("Jackson");

        Map<String, Object> object = new HashMap<String, Object>();
        object.put("key", myType);
        JsonNode jsonNode = MAPPER.valueToTree(object);
        String json = MAPPER.writeValueAsString(jsonNode);
        assertEquals("{\"key\":Jackson}", json);
    }
}
