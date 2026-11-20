package tools.jackson.databind.deser.merge;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonMerge;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.OptBoolean;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

// [databind#6445]: `@JsonMerge` on an `EnumMap` should update existing values,
// same as `MapDeserializer` does
public class EnumMapMerge6445Test
    extends DatabindTestUtil
{
    enum Key { A, B, C }

    // No default constructor: can only be updated, not created, from JSON Object
    static class Value {
        public String data;
        public int count;

        Value(String data, Key owner) { this.data = data; }
    }

    static class EnumMapBean {
        @JsonMerge
        public final EnumMap<Key, Value> map = new EnumMap<>(Key.class);

        EnumMapBean() {
            for (Key k : Key.values()) {
                map.put(k, new Value("init-" + k, k));
            }
        }
    }

    // As in the issue: declared as `Map`, but keyed by Enum so `EnumMapDeserializer` is used
    static class MapTypedBean {
        @JsonMerge
        public final Map<Key, Value> map = new EnumMap<>(Key.class);

        MapTypedBean() {
            for (Key k : Key.values()) {
                map.put(k, new Value("init-" + k, k));
            }
        }
    }

    // Value type that can be created, to verify merge keeps the existing instance
    static class Creatable {
        public String a;
        public String b;
    }

    static class CreatableBean {
        @JsonMerge
        public final EnumMap<Key, Creatable> map = new EnumMap<>(Key.class);
    }

    static class MergeDisabledBean {
        @JsonMerge(OptBoolean.FALSE)
        public EnumMap<Key, Creatable> map = new EnumMap<>(Key.class);
    }

    static class ScalarBean {
        @JsonMerge
        public final EnumMap<Key, String> map = new EnumMap<>(Key.class);
    }

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME)
    @JsonSubTypes({ @JsonSubTypes.Type(value = Shape.class, name = "shape") })
    static class Shape {
        public String color;
        public int size;
    }

    static class PolymorphicBean {
        @JsonMerge
        public final EnumMap<Key, Shape> map = new EnumMap<>(Key.class);
    }

    static class PolymorphicHashMapBean {
        @JsonMerge
        public final Map<String, Shape> map = new HashMap<>();
    }

    private final ObjectMapper MAPPER = newJsonMapper();

    // Polymorphic values go through `deserializeWithType()` with the existing value;
    // result must match what `MapDeserializer` does for the same input
    @Test
    void mergeWithPolymorphicValuesMatchesMap() throws Exception
    {
        PolymorphicBean enumBean = new PolymorphicBean();
        enumBean.map.put(Key.A, shape("red", 1));
        PolymorphicHashMapBean mapBean = new PolymorphicHashMapBean();
        mapBean.map.put("A", shape("red", 1));

        final String json = a2q("{'map':{'A':{'@type':'shape','size':5}}}");
        Shape fromEnumMap = MAPPER.readerForUpdating(enumBean).<PolymorphicBean>readValue(json)
                .map.get(Key.A);
        Shape fromMap = MAPPER.readerForUpdating(mapBean).<PolymorphicHashMapBean>readValue(json)
                .map.get("A");

        assertEquals(fromMap.color, fromEnumMap.color);
        assertEquals(fromMap.size, fromEnumMap.size);
        assertEquals(5, fromEnumMap.size);
    }

    private static Shape shape(String color, int size) {
        Shape s = new Shape();
        s.color = color;
        s.size = size;
        return s;
    }

    // Scalar values can not be updated: should just be replaced, as with `MapDeserializer`
    @Test
    void mergeWithScalarValuesReplaces() throws Exception
    {
        ScalarBean bean = new ScalarBean();
        bean.map.put(Key.A, "a0");
        bean.map.put(Key.B, "b0");

        ScalarBean result = MAPPER.readerForUpdating(bean)
                .readValue(a2q("{'map':{'A':'a1','C':'c1'}}"));

        assertEquals("a1", result.map.get(Key.A));
        assertEquals("b0", result.map.get(Key.B));
        assertEquals("c1", result.map.get(Key.C));
    }

    @Test
    void mergeIntoEnumMapWithNonCreatableValues() throws Exception
    {
        EnumMapBean bean = new EnumMapBean();
        Value oldA = bean.map.get(Key.A);

        EnumMapBean result = MAPPER.readerForUpdating(bean)
                .readValue(a2q("{'map':{'A':{'count':3},'B':{'data':'b'}}}"));

        assertSame(bean, result);
        assertSame(oldA, result.map.get(Key.A));
        assertEquals("init-A", result.map.get(Key.A).data);
        assertEquals(3, result.map.get(Key.A).count);
        assertEquals("b", result.map.get(Key.B).data);
        assertEquals("init-C", result.map.get(Key.C).data);
    }

    @Test
    void mergeIntoMapTypedEnumMap() throws Exception
    {
        MapTypedBean bean = MAPPER.readerForUpdating(new MapTypedBean())
                .readValue(a2q("{'map':{'C':{'count':7}}}"));

        assertEquals("init-C", bean.map.get(Key.C).data);
        assertEquals(7, bean.map.get(Key.C).count);
        assertEquals("init-A", bean.map.get(Key.A).data);
    }

    @Test
    void mergeKeepsExistingValueAndAddsMissingKeys() throws Exception
    {
        CreatableBean bean = new CreatableBean();
        Creatable oldA = new Creatable();
        oldA.a = "a0";
        oldA.b = "b0";
        bean.map.put(Key.A, oldA);

        CreatableBean result = MAPPER.readerForUpdating(bean)
                .readValue(a2q("{'map':{'A':{'b':'b1'},'B':{'a':'new'}}}"));

        assertSame(oldA, result.map.get(Key.A));
        assertEquals("a0", oldA.a);
        assertEquals("b1", oldA.b);
        assertEquals("new", result.map.get(Key.B).a);
        assertNull(result.map.get(Key.B).b);
    }

    @Test
    void mergeDisabledOnPropertyReplacesMap() throws Exception
    {
        MergeDisabledBean bean = new MergeDisabledBean();
        Creatable oldA = new Creatable();
        oldA.a = "a0";
        bean.map.put(Key.A, oldA);
        EnumMap<Key, Creatable> oldMap = bean.map;

        MergeDisabledBean result = MAPPER.readerForUpdating(bean)
                .readValue(a2q("{'map':{'A':{'b':'b1'}}}"));

        assertNotSame(oldMap, result.map);
        assertNull(result.map.get(Key.A).a);
        assertEquals("b1", result.map.get(Key.A).b);
    }

    // [databind#3205]-style: content type with `mergeable = false` must not merge values
    @Test
    void mergeDisabledForContentTypeReplacesValues() throws Exception
    {
        ObjectMapper mapper = JsonMapper.builder()
                .withConfigOverride(Creatable.class, o -> o.setMergeable(Boolean.FALSE))
                .build();
        CreatableBean bean = new CreatableBean();
        Creatable oldA = new Creatable();
        oldA.a = "a0";
        bean.map.put(Key.A, oldA);

        CreatableBean result = mapper.readerForUpdating(bean)
                .readValue(a2q("{'map':{'A':{'b':'b1'}}}"));

        assertNotSame(oldA, result.map.get(Key.A));
        assertNull(result.map.get(Key.A).a);
        assertEquals("b1", result.map.get(Key.A).b);
    }

    @Test
    void plainReadIsUnchanged() throws Exception
    {
        EnumMap<Key, Creatable> map = MAPPER.readValue(a2q("{'A':{'a':'x'}}"),
                MAPPER.getTypeFactory().constructMapType(EnumMap.class, Key.class, Creatable.class));
        assertEquals(1, map.size());
        assertEquals("x", map.get(Key.A).a);
    }
}
