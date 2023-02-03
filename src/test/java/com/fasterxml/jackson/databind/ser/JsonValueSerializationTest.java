package com.fasterxml.jackson.databind.ser;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;

import com.fasterxml.jackson.annotation.*;

import com.fasterxml.jackson.core.JsonGenerator;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.StdScalarSerializer;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;

/**
 * This unit test suite tests functioning of {@link JsonValue}
 * annotation with bean serialization.
 */
@SuppressWarnings("serial")
public class JsonValueSerializationTest
    extends BaseMapTest
{
    static class ValueClass<T>
    {
        final T _value;

        public ValueClass(T v) { _value = v; }

        @JsonValue T value() { return _value; }
    }

    static class FieldValueClass<T>
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
    final static class ToStringValueClass<T>
        extends ValueClass<T>
    {
        public ToStringValueClass(T value) { super(value); }

        // Also, need to use this annotation to help
        @JsonSerialize(using=ToStringSerializer.class)
        @Override
        @JsonValue T value() { return super.value(); }
    }

    final static class ToStringValueClass2
        extends ValueClass<String>
    {
        public ToStringValueClass2(String value) { super(value); }

        // Simple as well, but let's ensure that other getters won't matter...

        @JsonProperty int getFoobar() { return 4; }

        public String[] getSomethingElse() { return new String[] { "1", "a" }; }
    }

    static class ValueBase {
        public String a = "a";
    }

    static class ValueType extends ValueBase {
        public String b = "b";
    }

    // Finally, let's also test static vs dynamic type
    static class ValueWrapper {
        @JsonValue
        public ValueBase getX() { return new ValueType(); }
    }

    static class MapBean
    {
        @JsonValue
        public Map<String,String> toMap()
        {
            HashMap<String,String> map = new HashMap<String,String>();
            map.put("a", "1");
            return map;
        }
    }

    static class MapFieldBean
    {
        @JsonValue
        Map<String,String> stuff = new HashMap<>();
        {
            stuff.put("b", "2");
        }
    }

    static class MapAsNumber extends HashMap<String,String>
    {
        @JsonValue
        public int value() { return 42; }
    }

    static class ListAsNumber extends ArrayList<Integer>
    {
        @JsonValue
        public int value() { return 13; }
    }

    // Just to ensure it's possible to disable annotation (usually
    // via mix-ins, but here directly)
    @JsonPropertyOrder({ "x", "y" })
    static class DisabledJsonValue {
        @JsonValue(false)
        public int x = 1;

        @JsonValue(false)
        public int getY() { return 2; }
    }

    static class IntExtBean {
        public List<Internal> values = new ArrayList<Internal>();

        public void add(int v) { values.add(new Internal(v)); }
    }

    static class Internal {
        public int value;

        public Internal(int v) { value = v; }

        @JsonValue
        public External asExternal() { return new External(this); }
    }

    static class External {
        public int i;

        External(Internal e) { i = e.value; }
    }

    // [databind#167]

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "boingo")
    @JsonSubTypes(value = {@JsonSubTypes.Type(name = "boopsy", value = AdditionInterfaceImpl.class)
    })
    static interface AdditionInterface
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

    static class Bean838 {
        @JsonValue
        public String value() {
            return "value";
        }
    }

    static class Bean838Serializer extends StdScalarSerializer<Bean838>
    {
        public Bean838Serializer() {
            super(Bean838.class);
        }

        @Override
        public void serialize(Bean838 value, JsonGenerator gen,
                SerializerProvider provider) throws IOException {
            gen.writeNumber(42);
        }
    }

    // [databind#1806]
    static class Bean1806 {
        @JsonValue public List<Elem1806> getThings() {
            return Collections.singletonList(new Elem1806.Impl());
        }
    }

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.WRAPPER_OBJECT)
    @JsonSubTypes(@JsonSubTypes.Type(value = Elem1806.Impl.class, name = "impl"))
    interface Elem1806 {
        final class Impl implements Elem1806 {
            public int value = 1;
        }
    }

    // [databind#2822]
    @JsonPropertyOrder({ "description", "b" })
    static class A2822 {
        public final String description;

        @JsonFormat(shape = JsonFormat.Shape.STRING)
        public final B2822 b;

        public A2822(final String description, final B2822 b ) {
            this.description = description;
            this.b = b;
        }
    }

    static class B2822 {
        @JsonValue
        private final BigDecimal value;

        public B2822(final BigDecimal value ) {
            this.value = value;
        }
    }

    /*
    /**********************************************************************
    /* Test cases
    /**********************************************************************
     */

    private final ObjectMapper MAPPER = newJsonMapper();

    public void testSimpleMethodJsonValue() throws Exception
    {
        assertEquals("\"abc\"", MAPPER.writeValueAsString(new ValueClass<String>("abc")));
        assertEquals("null", MAPPER.writeValueAsString(new ValueClass<String>(null)));
    }

    public void testSimpleFieldJsonValue() throws Exception
    {
        assertEquals("\"abc\"", MAPPER.writeValueAsString(new FieldValueClass<String>("abc")));
        assertEquals("null", MAPPER.writeValueAsString(new FieldValueClass<String>(null)));
    }

    public void testJsonValueWithUseSerializer() throws Exception
    {
        String result = serializeAsString(MAPPER, new ToStringValueClass<Integer>(Integer.valueOf(123)));
        assertEquals("\"123\"", result);
    }

    /**
     * Test for verifying that additional getters won't confuse serializer.
     */
    public void testMixedJsonValue() throws Exception
    {
        String result = serializeAsString(MAPPER, new ToStringValueClass2("xyz"));
        assertEquals("\"xyz\"", result);
    }

    public void testDisabling() throws Exception
    {
        assertEquals(a2q("{'x':1,'y':2}"),
                MAPPER.writeValueAsString(new DisabledJsonValue()));
    }

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

    public void testMapWithJsonValue() throws Exception {
        // First via method
        assertEquals("{\"a\":\"1\"}", MAPPER.writeValueAsString(new MapBean()));

        // then field
        assertEquals("{\"b\":\"2\"}", MAPPER.writeValueAsString(new MapFieldBean()));
    }

    public void testWithMap() throws Exception {
        assertEquals("42", MAPPER.writeValueAsString(new MapAsNumber()));
    }

    public void testWithList() throws Exception {
        assertEquals("13", MAPPER.writeValueAsString(new ListAsNumber()));
    }

    public void testInList() throws Exception {
        IntExtBean bean = new IntExtBean();
        bean.add(1);
        bean.add(2);
        String json = MAPPER.writeValueAsString(bean);
        assertEquals(json, "{\"values\":[{\"i\":1},{\"i\":2}]}");
    }

    // [databind#167]
    public void testPolymorphicSerdeWithDelegate() throws Exception
    {
	    AdditionInterface adder = new AdditionInterfaceImpl(1);

	    assertEquals(2, adder.add(1));
	    String json = MAPPER.writeValueAsString(adder);
	    assertEquals("{\"boingo\":\"boopsy\",\"toAdd\":1}", json);
	    assertEquals(2, MAPPER.readValue(json, AdditionInterface.class).add(1));
    }

    public void testJsonValueWithCustomOverride() throws Exception
    {
        final Bean838 INPUT = new Bean838();

        // by default, @JsonValue should be used
        assertEquals(q("value"), MAPPER.writeValueAsString(INPUT));

        // but custom serializer should override it
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new SimpleModule()
            .addSerializer(Bean838.class, new Bean838Serializer())
            );
        assertEquals("42", mapper.writeValueAsString(INPUT));
    }

    // [databind#1806]
    public void testCollectionViaJsonValue() throws Exception
    {
        assertEquals("[{\"impl\":{\"value\":1}}]",
                MAPPER.writeValueAsString(new Bean1806()));
    }

    // [databind#2822]
    public void testFormatWithJsonValue() throws Exception
    {
        final String json = MAPPER.writeValueAsString(new A2822("desc",
                new B2822(BigDecimal.ONE)));
        assertEquals(a2q("{'description':'desc','b':'1'}"), json);
    }
}
