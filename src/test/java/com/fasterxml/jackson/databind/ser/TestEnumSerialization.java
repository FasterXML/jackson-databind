package com.fasterxml.jackson.databind.ser;

import java.io.*;
import java.util.*;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonFormat.Shape;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;

/**
 * Unit tests for verifying serialization of simple basic non-structured
 * types; primitives (and/or their wrappers), Strings.
 */
public class TestEnumSerialization
    extends BaseMapTest
{
    /*
    /**********************************************************
    /* Helper enums
    /**********************************************************
     */

    /**
     * Test enumeration for verifying Enum serialization functionality.
     */
    protected enum TestEnum {
        A, B, C;
        private TestEnum() { }

        @Override public String toString() { return name().toLowerCase(); }
    }

    /**
     * Alternative version that forces use of "toString-serializer".
     */
    @JsonSerialize(using=ToStringSerializer.class)
    protected enum AnnotatedTestEnum {
        A2, B2, C2;
        private AnnotatedTestEnum() { }

        @Override public String toString() { return name().toLowerCase(); }
    }

    protected enum EnumWithJsonValue {
        A("foo"), B("bar");
        private final String name;
        private EnumWithJsonValue(String n) {
            name = n;
        }
        @JsonValue
        @Override
        public String toString() { return name; }
    }
    
    protected static interface ToStringMixin {
        @Override
        @JsonValue public String toString();
    }

    protected static enum SerializableEnum implements JsonSerializable
    {
        A, B, C;

        private SerializableEnum() { }

        @Override
        public void serializeWithType(JsonGenerator jgen, SerializerProvider provider, TypeSerializer typeSer)
                throws IOException, JsonProcessingException
        {
            serialize(jgen, provider);
        }

        @Override
        public void serialize(JsonGenerator jgen, SerializerProvider provider) throws IOException, JsonProcessingException
        {
            jgen.writeString("foo");
        }
    }

    protected static enum LowerCaseEnum {
        A, B, C;
        private LowerCaseEnum() { }
        @Override
        public String toString() { return name().toLowerCase(); }
    }

    static class MapBean {
        public Map<TestEnum,Integer> map = new HashMap<TestEnum,Integer>();
        
        public void add(TestEnum key, int value) {
            map.put(key, Integer.valueOf(value));
        }
    }

    // [JACKSON-757]
    static enum NOT_OK {
        V1("v1"); 
        protected String key;
        // any runtime-persistent annotation is fine
        NOT_OK(@JsonProperty String key) { this.key = key; }
    }

    static enum OK {
        V1("v1");
        protected String key;
        OK(String key) { this.key = key; }
    }
    
    // Types for [https://github.com/FasterXML/jackson-databind/issues/24]
    // (Enums as JSON Objects)

    @JsonFormat(shape=JsonFormat.Shape.OBJECT)
    static enum PoNUM {
        A("a1"), B("b2");

        @JsonProperty
        protected final String value;
        
        private PoNUM(String v) { value = v; }

        public String getValue() { return value; }
    }

    static class PoNUMContainer {
        @JsonFormat(shape=Shape.NUMBER)
        public OK text = OK.V1;
    }
    
    @JsonFormat(shape=JsonFormat.Shape.ARRAY) // alias for 'number', as of 2.5
    static enum PoAsArray
    {
        A, B;
    }

    @SuppressWarnings({ "rawtypes", "serial" })
    static class LowerCasingEnumSerializer extends StdSerializer<Enum>
    {
        public LowerCasingEnumSerializer() { super(Enum.class); }
        @Override
        public void serialize(Enum value, JsonGenerator jgen,
                SerializerProvider provider) throws IOException {
            jgen.writeString(value.name().toLowerCase());
        }
    }

    // for [databind#572]
    static class PoOverrideAsString
    {
        @JsonFormat(shape=Shape.STRING)
        public PoNUM value = PoNUM.B;
    }

    static class PoOverrideAsNumber
    {
        @JsonFormat(shape=Shape.NUMBER)
        public PoNUM value = PoNUM.B;
    }

    static enum MyEnum594 {
        VALUE_WITH_A_REALLY_LONG_NAME_HERE("longValue");

        private final String key;
        private MyEnum594(String k) { key = k; }

        @JsonValue
        public String getKey() { return key; }
    }

    static class MyStuff594 {
        public Map<MyEnum594,String> stuff = new EnumMap<MyEnum594,String>(MyEnum594.class);
        
        public MyStuff594(String value) {
            stuff.put(MyEnum594.VALUE_WITH_A_REALLY_LONG_NAME_HERE, value);
        }
    }

    public class MyBean661 {
        private Map<Foo661, String> foo = new EnumMap<Foo661, String>(Foo661.class);

        public MyBean661(String value) {
            foo.put(Foo661.FOO, value);
        }

        @JsonAnyGetter
        @JsonSerialize(keyUsing = Foo661.Serializer.class)
        public Map<Foo661, String> getFoo() {
            return foo;
        }
    }

    enum Foo661 {
        FOO;
        public static class Serializer extends JsonSerializer<Foo661> {
            @Override
            public void serialize(Foo661 value, JsonGenerator jgen, SerializerProvider provider) 
                    throws IOException {
                jgen.writeFieldName("X-"+value.name());
            }
        }
    }
    /*
    /**********************************************************
    /* Tests
    /**********************************************************
     */

    private final ObjectMapper mapper = new ObjectMapper();
    
    public void testSimple() throws Exception
    {
        assertEquals("\"B\"", mapper.writeValueAsString(TestEnum.B));
    }

    public void testEnumSet() throws Exception
    {
        StringWriter sw = new StringWriter();
        EnumSet<TestEnum> value = EnumSet.of(TestEnum.B);
        mapper.writeValue(sw, value);
        assertEquals("[\"B\"]", sw.toString());
    }

    /**
     * Whereas regular Enum serializer uses enum names, some users
     * prefer calling toString() instead. So let's verify that
     * this can be done using annotation for enum class.
     */
    public void testEnumUsingToString() throws Exception
    {
        StringWriter sw = new StringWriter();
        mapper.writeValue(sw, AnnotatedTestEnum.C2);
        assertEquals("\"c2\"", sw.toString());
    }

    // Test [JACKSON-214]
    public void testSubclassedEnums() throws Exception
    {
        assertEquals("\"B\"", mapper.writeValueAsString(EnumWithSubClass.B));
    }

    public void testEnumsWithJsonValue() throws Exception
    {
        assertEquals("\"bar\"", mapper.writeValueAsString(EnumWithJsonValue.B));
    }

    public void testEnumsWithJsonValueUsingMixin() throws Exception
    {
        // can't share, as new mix-ins are added
        ObjectMapper m = new ObjectMapper();
        m.addMixIn(TestEnum.class, ToStringMixin.class);
        assertEquals("\"b\"", m.writeValueAsString(TestEnum.B));
    }

    // [databind#601]
    public void testEnumsWithJsonValueInMap() throws Exception
    {
        EnumMap<EnumWithJsonValue,String> input = new EnumMap<EnumWithJsonValue,String>(EnumWithJsonValue.class);
        input.put(EnumWithJsonValue.B, "x");
        assertEquals("{\""+EnumWithJsonValue.B.toString()+"\":\"x\"}", mapper.writeValueAsString(input));
    }
    
    /**
     * Test for ensuring that @JsonSerializable is used with Enum types as well
     * as with any other types.
     */
    public void testSerializableEnum() throws Exception
    {
        assertEquals("\"foo\"", mapper.writeValueAsString(SerializableEnum.A));
    }

    // [JACKSON-212]
    public void testToStringEnum() throws Exception
    {
        ObjectMapper m = new ObjectMapper();
        m.configure(SerializationFeature.WRITE_ENUMS_USING_TO_STRING, true);
        assertEquals("\"b\"", m.writeValueAsString(LowerCaseEnum.B));

        // [databind#749] but should also be able to dynamically disable
        assertEquals("\"B\"",
                m.writer().without(SerializationFeature.WRITE_ENUMS_USING_TO_STRING)
                    .writeValueAsString(LowerCaseEnum.B));
    }

    // [JACKSON-212]
    public void testToStringEnumWithEnumMap() throws Exception
    {
        ObjectMapper m = new ObjectMapper();
        m.configure(SerializationFeature.WRITE_ENUMS_USING_TO_STRING, true);
        EnumMap<LowerCaseEnum,String> enums = new EnumMap<LowerCaseEnum,String>(LowerCaseEnum.class);
        enums.put(LowerCaseEnum.C, "value");
        assertEquals("{\"c\":\"value\"}", m.writeValueAsString(enums));
    }

    // [JACKSON-576]
    public void testMapWithEnumKeys() throws Exception
    {
        MapBean bean = new MapBean();
        bean.add(TestEnum.B, 3);
        String json = mapper.writeValueAsString(bean);
        assertEquals("{\"map\":{\"b\":3}}", json);
    }

    // [JACKSON-684]
    public void testAsIndex() throws Exception
    {
        // By default, serialize using name
        ObjectMapper m = new ObjectMapper();
        assertFalse(m.isEnabled(SerializationFeature.WRITE_ENUMS_USING_INDEX));
        assertEquals(quote("B"), m.writeValueAsString(TestEnum.B));

        // but we can change (dynamically, too!) it to be number-based
        m.enable(SerializationFeature.WRITE_ENUMS_USING_INDEX);
        assertEquals("1", m.writeValueAsString(TestEnum.B));
    }

    // [JACKSON-757]
    public void testAnnotationsOnEnumCtor() throws Exception
    {
        assertEquals(quote("V1"), mapper.writeValueAsString(OK.V1));
        assertEquals(quote("V1"), mapper.writeValueAsString(NOT_OK.V1));
        assertEquals(quote("V2"), mapper.writeValueAsString(NOT_OK2.V2));
    }

    // Tests for [issue#24]

    public void testEnumAsObjectValid() throws Exception {
        assertEquals("{\"value\":\"a1\"}", mapper.writeValueAsString(PoNUM.A));
    }

    public void testEnumAsIndexViaAnnotations() throws Exception {
        assertEquals("{\"text\":0}", mapper.writeValueAsString(new PoNUMContainer()));
    }

    // As of 2.5, use of Shape.ARRAY is legal alias for "write as number"
    public void testEnumAsObjectBroken() throws Exception
    {
        assertEquals("0", mapper.writeValueAsString(PoAsArray.A));
    }
    
    // [Issue#227]
    public void testGenericEnumSerializer() throws Exception
    {
        // By default, serialize using name
        ObjectMapper m = new ObjectMapper();
        SimpleModule module = new SimpleModule("foobar");
        module.addSerializer(Enum.class, new LowerCasingEnumSerializer());
        m.registerModule(module);
        assertEquals(quote("b"), m.writeValueAsString(TestEnum.B));
    }

    // [databind#572]
    public void testOverrideEnumAsString() throws Exception {
        assertEquals("{\"value\":\"B\"}", mapper.writeValueAsString(new PoOverrideAsString()));
    }

    public void testOverrideEnumAsNumber() throws Exception {
        assertEquals("{\"value\":1}", mapper.writeValueAsString(new PoOverrideAsNumber()));
    }

    // [databind#594]
    public void testJsonValueForEnumMapKey() throws Exception {
        assertEquals(aposToQuotes("{'stuff':{'longValue':'foo'}}"),
                mapper.writeValueAsString(new MyStuff594("foo")));
    }

    // [databind#661]
    public void testCustomEnumMapKeySerializer() throws Exception {
        String json = mapper.writeValueAsString(new MyBean661("abc"));
        assertEquals(aposToQuotes("{'X-FOO':'abc'}"), json);
    }
}

// [JACKSON-757], non-inner enum
enum NOT_OK2 {
    V2("v2"); 
    protected String key;
    // any runtime-persistent annotation is fine
    NOT_OK2(@JsonProperty String key) { this.key = key; }
}
