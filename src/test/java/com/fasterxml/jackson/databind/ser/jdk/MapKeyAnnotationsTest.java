package com.fasterxml.jackson.databind.ser.jdk;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonKey;
import com.fasterxml.jackson.annotation.JsonValue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.assertEquals;

// Tests focusing on {@code JsonValue} and newer {@code JsonKey}
// annotation handling
public class MapKeyAnnotationsTest extends DatabindTestUtil
{
    // [databind#47]
    public static class Wat
    {
        private final String wat;

        @JsonCreator
        Wat(String wat) {
            this.wat = wat;
        }

        @JsonValue
        public String getWat() {
            return wat;
        }

        @Override
        public String toString() {
            return "(String)[Wat: " + wat + "]";
        }
    }

    @SuppressWarnings("serial")
    static class WatMap extends HashMap<Wat,Boolean> { }

    // [databind#943]
    static class UCString {
        private String value;

        public UCString(String v) {
            value = v.toUpperCase();
        }

        @JsonValue
        public String asString() {
            return value;
        }
    }

    enum AbcLC {
        A, B, C;

        @JsonValue
        public String toLC() {
            return name().toLowerCase();
        }
    }

    // [databind#2306]
    static class JsonValue2306Key {
        @JsonValue
        private String id;

        public JsonValue2306Key(String id) {
            this.id = id;
        }
    }

    // [databind#2871]
    static class Inner {
        @JsonKey
        String key;

        @JsonValue
        String value;

        Inner(String key, String value) {
            this.key = key;
            this.value = value;
        }

        @Override
        public String toString() {
            return "Inner(" + this.key + "," + this.value + ")";
        }
    }

    static class Outer {
        @JsonKey
        @JsonValue
        Inner inner;

        Outer(Inner inner) {
            this.inner = inner;
        }
    }

    static class NoKeyOuter {
        @JsonValue
        Inner inner;

        NoKeyOuter(Inner inner) {
            this.inner = inner;
        }
    }

    // [databind#6240]: same as `Outer`, but annotations on getter
    static class OuterWithGetter {
        private final Inner inner;

        OuterWithGetter(Inner inner) {
            this.inner = inner;
        }

        @JsonKey
        @JsonValue
        public Inner getInner() {
            return inner;
        }
    }

    // [databind#6240]
    static class KeyOnlyGetter {
        public String name = "value";

        @JsonKey
        @JsonValue(false)
        public String getId() {
            return "a";
        }
    }

    // [databind#6240]
    static class ValueOnlyGetter {
        @JsonKey(false)
        @JsonValue
        public String getId() {
            return "a";
        }
    }

    /*
    /**********************************************************************
    /* Test methods
    /**********************************************************************
     */

    private final ObjectMapper MAPPER = newJsonMapper();

    // [databind#47]
    @Test
    public void testMapJsonValueKey47() throws Exception
    {
        WatMap input = new WatMap();
        input.put(new Wat("3"), true);

        String json = MAPPER.writeValueAsString(input);
        assertEquals(a2q("{'3':true}"), json);
    }

    // [databind#943]
    @Test
    public void testDynamicMapKeys() throws Exception
    {
        Map<Object,Integer> stuff = new LinkedHashMap<Object,Integer>();
        stuff.put(AbcLC.B, Integer.valueOf(3));
        stuff.put(new UCString("foo"), Integer.valueOf(4));
        String json = MAPPER.writeValueAsString(stuff);
        assertEquals(a2q("{'b':3,'FOO':4}"), json);
    }

    // [databind#2306]
    @Test
    public void testMapKeyWithJsonValue() throws Exception
    {
        final Map<JsonValue2306Key, String> map = Collections.singletonMap(
                new JsonValue2306Key("myId"), "value");
        assertEquals(a2q("{'myId':'value'}"),
                MAPPER.writeValueAsString(map));
    }

    // [databind#2871]
    @Test
    public void testClassAsKey() throws Exception {
        Outer outer = new Outer(new Inner("innerKey", "innerValue"));
        Map<Outer, String> map = Collections.singletonMap(outer, "value");
        String actual = MAPPER.writeValueAsString(map);
        assertEquals("{\"innerKey\":\"value\"}", actual);
    }

    // [databind#2871]
    @Test
    public void testClassAsValue() throws Exception {
        Map<String, Outer> mapA = Collections.singletonMap("key", new Outer(new Inner("innerKey", "innerValue")));
        String actual = MAPPER.writeValueAsString(mapA);
        assertEquals("{\"key\":\"innerValue\"}", actual);
    }

    // [databind#2871]
    @Test
    public void testNoKeyOuter() throws Exception {
        Map<String, NoKeyOuter> mapA = Collections.singletonMap("key", new NoKeyOuter(new Inner("innerKey", "innerValue")));
        String actual = MAPPER.writeValueAsString(mapA);
        assertEquals("{\"key\":\"innerValue\"}", actual);
    }

    // [databind#6240]
    @Test
    public void testClassAsKeyWithGetter() throws Exception {
        OuterWithGetter outer = new OuterWithGetter(new Inner("innerKey", "innerValue"));
        assertEquals("{\"innerKey\":\"value\"}",
                MAPPER.writeValueAsString(Collections.singletonMap(outer, "value")));
    }

    // [databind#6240]
    @Test
    public void testClassAsValueWithGetter() throws Exception {
        OuterWithGetter outer = new OuterWithGetter(new Inner("innerKey", "innerValue"));
        assertEquals("\"innerValue\"", MAPPER.writeValueAsString(outer));
        assertEquals("{\"key\":\"innerValue\"}",
                MAPPER.writeValueAsString(Collections.singletonMap("key", outer)));
    }

    // [databind#6240]: disabled `@JsonValue` must not expose key getter as property
    @Test
    public void testKeyOnlyGetter() throws Exception {
        assertEquals("{\"name\":\"value\"}", MAPPER.writeValueAsString(new KeyOnlyGetter()));
        assertEquals("{\"a\":1}",
                MAPPER.writeValueAsString(Collections.singletonMap(new KeyOnlyGetter(), 1)));
    }

    // [databind#6240]: disabled `@JsonKey` must not disable `@JsonValue`
    @Test
    public void testValueOnlyGetter() throws Exception {
        assertEquals("\"a\"", MAPPER.writeValueAsString(new ValueOnlyGetter()));
    }
}
