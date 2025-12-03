package tools.jackson.databind.ser.enums;

import java.util.*;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;

import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.*;
import tools.jackson.databind.annotation.JsonSerialize;
import tools.jackson.databind.cfg.EnumFeature;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.MethodName.class)
public class EnumAsMapKeySerializationTest extends DatabindTestUtil
{
    static class MapBean {
        public Map<ABCEnum,Integer> map = new HashMap<>();

        public void add(ABCEnum key, int value) {
            map.put(key, Integer.valueOf(value));
        }
    }

    protected enum ABCEnum {
        A, B, C;
        private ABCEnum() { }

        @Override public String toString() { return name().toLowerCase(); }
    }

    // [databind#594]
    static enum MyEnum594 {
        VALUE_WITH_A_REALLY_LONG_NAME_HERE("longValue");

        private final String key;
        private MyEnum594(String k) { key = k; }

        @JsonValue
        public String getKey() { return key; }
    }

    static class MyStuff594 {
        public Map<MyEnum594,String> stuff = new EnumMap<MyEnum594,String>(MyEnum594.class);

        protected MyStuff594() { }
        public MyStuff594(String value) {
            stuff.put(MyEnum594.VALUE_WITH_A_REALLY_LONG_NAME_HERE, value);
        }
    }

    // [databind#661]
    static class MyBean661 {
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

    public enum Foo661 {
        FOO;
        public static class Serializer extends ValueSerializer<Foo661> {
            @Override
            public void serialize(Foo661 value, JsonGenerator g, SerializationContext provider)
            {
                g.writeName("X-"+value.name());
            }
        }
    }

    // [databind#2129]
    public enum Type {
        FIRST,
        SECOND;
    }

    static class TypeContainer {
        public Map<Type, Integer> values;

        public TypeContainer(Type type, int value) {
            values = Collections.singletonMap(type, value);
        }
    }

    // [databind#2457]
    enum MyEnum2457 {
        A,
        B() {
            // just to ensure subclass construction
            @Override
            public void foo() { }
        };

        // needed to force subclassing
        public void foo() { }

        @Override
        public String toString() { return name() + " as string"; }
    }

    // [databind#2457]
    enum MyEnum2457Base {
        @JsonProperty("a_base")
        A,
        @JsonProperty("b_base")
        B() {
            // just to ensure subclass construction
            @Override
            public void foo() { }
        },
        C;
        
        // needed to force sub-classing
        public void foo() { }
        
        @Override
        public String toString() { return name() + " as string"; }
    }

    // [databind#2457]
    enum MyEnum2457Mixin {
        @JsonProperty("a_mixin")
        A,
        @JsonProperty("b_mixin")
        B() {
            // just to ensure subclass construction
            @Override
            public void foo() { }
        };

        // needed to force sub=classing
        public void foo() { }

        @Override
        public String toString() { return name() + " as string"; }
    }

    // [databind#5432]
    enum Color5432 {
        @JsonProperty("red")
        RED
    }

    /*
    /**********************************************************************
    /* Test methods
    /**********************************************************************
     */

    private final ObjectMapper MAPPER = jsonMapperBuilder()
            .disable(EnumFeature.WRITE_ENUMS_USING_TO_STRING)
            .build();

    @Test
    public void testMapWithEnumKeys() throws Exception
    {
        MapBean bean = new MapBean();
        bean.add(ABCEnum.B, 3);

        // By default Enums serialized using `name()`
        String json = MAPPER.writeValueAsString(bean);
        assertEquals("{\"map\":{\"B\":3}}", json);

        // but can change
        json = MAPPER.writer()
                .with(EnumFeature.WRITE_ENUMS_USING_TO_STRING)
                .writeValueAsString(bean);
        assertEquals("{\"map\":{\"b\":3}}", json);

        // [databind#1570]

        // 14-Sep-2019, tatu: as per [databind#2129], must NOT use this feature but
        //    instead new `WRITE_ENUM_KEYS_USING_INDEX` added in 2.10
        json = MAPPER.writer()
                .with(EnumFeature.WRITE_ENUMS_USING_INDEX)
                .writeValueAsString(bean);
//        assertEquals(a2q("{'map':{'"+TestEnum.B.ordinal()+"':3}}"), json);
        assertEquals(a2q("{'map':{'B':3}}"), json);
    }

    @Test
    public void testCustomEnumMapKeySerializer() throws Exception {
        String json = MAPPER.writeValueAsString(new MyBean661("abc"));
        assertEquals(a2q("{'X-FOO':'abc'}"), json);
    }

    // [databind#594]
    @Test
    public void testJsonValueForEnumMapKeySer() throws Exception {
        assertEquals(a2q("{'stuff':{'longValue':'foo'}}"),
                MAPPER.writeValueAsString(new MyStuff594("foo")));
    }

    @Test
    public void testJsonValueForEnumMapKeyDeser() throws Exception {
        final String json = a2q("{'stuff':{'longValue':'foo'}}");
        ObjectReader r = MAPPER.readerFor(MyStuff594.class);
        MyStuff594 result = r.with(EnumFeature.READ_ENUMS_USING_TO_STRING).readValue(json);
        assertEquals("foo", result.stuff.get(MyEnum594.VALUE_WITH_A_REALLY_LONG_NAME_HERE));

        result = r.without(EnumFeature.READ_ENUMS_USING_TO_STRING).readValue(json);
        assertEquals("foo", result.stuff.get(MyEnum594.VALUE_WITH_A_REALLY_LONG_NAME_HERE));
    }

    // [databind#2129]
    @Test
    public void testEnumAsIndexForRootMap() throws Exception
    {
        final Map<Type, Integer> input = Collections.singletonMap(Type.FIRST, 3);

        // by default, write using name()
        assertEquals(a2q("{'FIRST':3}"),
                MAPPER.writeValueAsString(input));

        // but change with setting
        assertEquals(a2q("{'0':3}"),
                MAPPER.writer()
                .with(EnumFeature.WRITE_ENUM_KEYS_USING_INDEX)
                .writeValueAsString(input));

        // but NOT with value settings
        assertEquals(a2q("{'FIRST':3}"),
                MAPPER.writer()
                    .with(EnumFeature.WRITE_ENUMS_USING_INDEX)
                    .writeValueAsString(input));
    }

    // [databind#2129]
    @Test
    public void testEnumAsIndexForValueMap() throws Exception
    {
        final TypeContainer input = new TypeContainer(Type.SECOND, 72);

        // by default, write using name()
        assertEquals(a2q("{'values':{'SECOND':72}}"),
                MAPPER.writeValueAsString(input));

        // but change with setting
        assertEquals(a2q("{'values':{'1':72}}"),
                MAPPER.writer()
                .with(EnumFeature.WRITE_ENUM_KEYS_USING_INDEX)
                .writeValueAsString(input));

        // but NOT with value settings
        assertEquals(a2q("{'values':{'SECOND':72}}"),
                MAPPER.writer()
                    .with(EnumFeature.WRITE_ENUMS_USING_INDEX)
                    .writeValueAsString(input));
    }

    // [databind#2457]
    @Test
    public void testCustomEnumAsRootMapKey() throws Exception
    {
        final Map<MyEnum2457, String> map = new LinkedHashMap<>();
        map.put(MyEnum2457.A, "1");
        map.put(MyEnum2457.B, "2");
        assertEquals(a2q("{'A':'1','B':'2'}"),
                MAPPER.writer()
                        .without(EnumFeature.WRITE_ENUMS_USING_TO_STRING)
                        .writeValueAsString(map));

        // But should be able to override
        assertEquals(a2q("{'"+MyEnum2457.A.toString()+"':'1','"+MyEnum2457.B.toString()+"':'2'}"),
                MAPPER.writer()
                    .with(EnumFeature.WRITE_ENUMS_USING_TO_STRING)
                    .writeValueAsString(map));
    }

    /**
     * @see #testCustomEnumAsRootMapKey
     */
    // [databind#2457]
    @Test
    public void testCustomEnumAsRootMapKeyMixin() throws Exception
    {
        ObjectMapper mixinMapper = JsonMapper.builder()
                .addMixIn(MyEnum2457Base.class, MyEnum2457Mixin.class)
                .build();
        final Map<MyEnum2457Base, String> map = new LinkedHashMap<>();
        map.put(MyEnum2457Base.A, "1");
        map.put(MyEnum2457Base.B, "2");
        map.put(MyEnum2457Base.C, "3");
        assertEquals(a2q("{'a_mixin':'1','b_mixin':'2','C':'3'}"),
                mixinMapper.writer()
                        .without(EnumFeature.WRITE_ENUMS_USING_TO_STRING)
                        .writeValueAsString(map));

        // But should be able to override
        assertEquals(a2q("{'a_mixin':'1','b_mixin':'2','"
                +MyEnum2457Base.C.toString()+"':'3'}"
                ),
                mixinMapper.writer()
                        .with(EnumFeature.WRITE_ENUMS_USING_TO_STRING)
                        .writeValueAsString(map));
    }

    // [databind#5432]
    @Test
    void enumKeyShouldSerializeUsingJsonPropertyAndToString() throws Exception
    {
        final ObjectMapper mapper = jsonMapperBuilder()
                .enable(EnumFeature.WRITE_ENUMS_USING_TO_STRING)
                .build();

        // Sanity check first
        // assertEquals(q("red"), mapper.writeValueAsString(Color5432.RED));

        // Then actual test
        Map<Color5432, String> map = Collections.singletonMap(Color5432.RED, "#ff0000");
        String json = mapper.writeValueAsString(map);
        assertEquals("{\"red\":\"#ff0000\"}", json);
    }

    // [databind#5432]
    @Test
    void enumKeyShouldSerializeUsingJsonPropertyAndName() throws Exception
    {
        final ObjectMapper mapper = jsonMapperBuilder()
                .disable(EnumFeature.WRITE_ENUMS_USING_TO_STRING)
                .build();

        // Sanity check first
        assertEquals(q("red"), mapper.writeValueAsString(Color5432.RED));

        // Then actual test
        Map<Color5432, String> map = Collections.singletonMap(Color5432.RED, "#ff0000");
        String json = mapper.writeValueAsString(map);
        assertEquals("{\"red\":\"#ff0000\"}", json);
    }
}
