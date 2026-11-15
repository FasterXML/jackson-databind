package tools.jackson.databind.ser;

import java.util.*;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.*;

import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.*;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.module.SimpleModule;
import tools.jackson.databind.ser.std.StdScalarSerializer;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.assertEquals;

// [databind#6392]: JsonValueSerializer must reuse dynamically resolved serializers
public class JsonValueDynamicCaching6392Test extends DatabindTestUtil
{
    static class V {
        final Object o;
        V(Object o) { this.o = o; }
        @JsonValue public Object val() { return o; }
    }

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME)
    @JsonTypeName("TV")
    static class TypedV {
        final Object o;
        TypedV(Object o) { this.o = o; }
        @JsonValue public Object val() { return o; }
    }

    static class Wrapper {
        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        public V value;
        Wrapper(V v) { value = v; }
    }

    static class A { }
    static class B { }

    static class Foo {
        public String p1 = "hello";
        public String p2 = "world";
    }

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME)
    @JsonTypeName("TI")
    static class TypedIgnoring {
        @JsonValue
        @JsonIgnoreProperties("p1")
        public Object foo() { return new Foo(); }
    }

    static class Counting<T> extends StdScalarSerializer<T> {
        final Map<String, Integer> counts;
        final Class<T> type;

        Counting(Class<T> type, Map<String, Integer> counts) {
            super(type);
            this.type = type;
            this.counts = counts;
        }

        @Override
        public ValueSerializer<?> createContextual(SerializationContext ctxt, BeanProperty p) {
            counts.merge(type.getSimpleName(), 1, Integer::sum);
            return this;
        }

        @Override
        public void serialize(T v, JsonGenerator g, SerializationContext ctxt) {
            g.writeString(type.getSimpleName());
        }
    }

    private final Map<String, Integer> counts = new TreeMap<>();

    private final ObjectMapper MAPPER = JsonMapper.builder()
            .addModule(new SimpleModule()
                    .addSerializer(A.class, new Counting<>(A.class, counts))
                    .addSerializer(B.class, new Counting<>(B.class, counts)))
            .build();

    @Test
    public void dynamicSerializerResolvedOncePerType() throws Exception
    {
        List<V> values = new ArrayList<>();
        for (int i = 0; i < 20; ++i) {
            values.add(new V(new A()));
        }
        MAPPER.writeValueAsString(values);
        assertEquals(Map.of("A", 1), counts);

        // Type seen only after many values of another type must be cached too
        values.clear();
        for (int i = 0; i < 5; ++i) {
            values.add(new V(new B()));
        }
        values.add(new V(new A()));
        assertEquals("""
                ["B","B","B","B","B","A"]""",
                MAPPER.writeValueAsString(values));
        assertEquals(Map.of("A", 1, "B", 1), counts);
    }

    @Test
    public void dynamicSerializerResolvedOnceWithTypeInfo() throws Exception
    {
        List<TypedV> values = new ArrayList<>();
        for (int i = 0; i < 5; ++i) {
            values.add(new TypedV(new A()));
        }
        values.add(new TypedV(new B()));
        values.add(new TypedV(new B()));
        MAPPER.writeValueAsString(values);
        assertEquals(Map.of("A", 1, "B", 1), counts);
    }

    // isEmpty() and serialize() should share resolved serializer
    @Test
    public void dynamicSerializerSharedWithEmptyCheck() throws Exception
    {
        assertEquals("""
                [{"value":"A"},{"value":"A"},{"value":"A"}]""",
                MAPPER.writeValueAsString(List.of(new Wrapper(new V(new A())),
                        new Wrapper(new V(new A())), new Wrapper(new V(new A())))));
        assertEquals(Map.of("A", 1), counts);
    }

    // Typed and untyped access share the cache: verify `@JsonIgnoreProperties`
    // of accessor still applies for typed access
    @Test
    public void ignoredPropertiesAppliedWithTypeInfo() throws Exception
    {
        assertEquals("""
                ["TI",{"p2":"world"}]""",
                MAPPER.writeValueAsString(new TypedIgnoring()));
        assertEquals("""
                ["TI",{"p2":"world"}]""",
                MAPPER.writeValueAsString(new TypedIgnoring()));
    }
}
