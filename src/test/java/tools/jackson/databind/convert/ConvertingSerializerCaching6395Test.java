package tools.jackson.databind.convert;

import java.util.*;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonInclude;

import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.*;
import tools.jackson.databind.annotation.JsonSerialize;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.module.SimpleModule;
import tools.jackson.databind.ser.std.StdConvertingSerializer;
import tools.jackson.databind.ser.std.StdScalarSerializer;
import tools.jackson.databind.testutil.DatabindTestUtil;
import tools.jackson.databind.util.StdConverter;

import static org.junit.jupiter.api.Assertions.assertEquals;

// [databind#6395]: StdConvertingSerializer must reuse dynamically resolved
// delegate serializers (without sharing them between mappers)
public class ConvertingSerializerCaching6395Test extends DatabindTestUtil
{
    @JsonSerialize(converter = ToObject.class)
    static class Input {
        final Object value;
        Input(Object value) { this.value = value; }
    }

    // Same as `Input` but without annotation, for explicitly registered serializer
    static class PlainInput {
        final Object value;
        PlainInput(Object value) { this.value = value; }
    }

    static class ToObject extends StdConverter<Input, Object> {
        @Override public Object convert(Input in) { return in.value; }
    }

    static class PlainToObject extends StdConverter<Object, Object> {
        @Override public Object convert(Object in) { return ((PlainInput) in).value; }
    }

    static class A { }
    static class B { }

    static class NonEmptyWrapper {
        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        public Input value;
        NonEmptyWrapper(Input v) { value = v; }
    }

    static class TwoProps {
        public Input first;
        public Input second;
        TwoProps(Input a, Input b) { first = a; second = b; }
    }

    static class Counting<T> extends StdScalarSerializer<T> {
        final Map<String, Integer> counts;
        final String id;

        Counting(Class<T> type, String id, Map<String, Integer> counts) {
            super(type);
            this.id = id;
            this.counts = counts;
        }

        @Override
        public ValueSerializer<?> createContextual(SerializationContext ctxt, BeanProperty p) {
            counts.merge(id, 1, Integer::sum);
            return this;
        }

        @Override
        public void serialize(T v, JsonGenerator g, SerializationContext ctxt) {
            g.writeString(id);
        }
    }

    private final Map<String, Integer> counts = new TreeMap<>();

    private final ObjectMapper MAPPER = JsonMapper.builder()
            .addModule(new SimpleModule()
                    .addSerializer(A.class, new Counting<>(A.class, "A", counts))
                    .addSerializer(B.class, new Counting<>(B.class, "B", counts)))
            .build();

    @Test
    public void delegateResolvedOncePerType() throws Exception
    {
        List<Input> values = new ArrayList<>();
        for (int i = 0; i < 20; ++i) {
            values.add(new Input(new A()));
        }
        for (int i = 0; i < 5; ++i) {
            values.add(new Input(new B()));
        }
        values.add(new Input(new A()));
        assertEquals("""
                ["A","A","A","A","A","A","A","A","A","A","A","A","A","A","A","A","A","A","A","A","B","B","B","B","B","A"]""",
                MAPPER.writeValueAsString(values));
        assertEquals(Map.of("A", 1, "B", 1), counts);
    }

    // isEmpty() and serialize() should share resolved delegate
    @Test
    public void delegateSharedWithEmptyCheck() throws Exception
    {
        assertEquals("""
                [{"value":"A"},{"value":"A"},{"value":"A"}]""",
                MAPPER.writeValueAsString(List.of(new NonEmptyWrapper(new Input(new A())),
                        new NonEmptyWrapper(new Input(new A())),
                        new NonEmptyWrapper(new Input(new A())))));
        assertEquals(Map.of("A", 1), counts);
    }

    // Different properties must still get delegates contextualized separately
    @Test
    public void delegateResolvedPerProperty() throws Exception
    {
        assertEquals("""
                [{"first":"A","second":"A"},{"first":"A","second":"A"}]""",
                MAPPER.writeValueAsString(List.of(
                        new TwoProps(new Input(new A()), new Input(new A())),
                        new TwoProps(new Input(new A()), new Input(new A())))));
        assertEquals(Map.of("A", 2), counts);
    }

    @Test
    public void nullConversionResult() throws Exception
    {
        assertEquals("""
                [null,"A",null]""",
                MAPPER.writeValueAsString(List.of(new Input(null),
                        new Input(new A()), new Input(null))));
        assertEquals(Map.of("A", 1), counts);
    }

    // A single, already resolved, `StdConvertingSerializer` instance registered
    // with multiple mappers must not share dynamically resolved delegates
    @Test
    public void delegatesNotSharedBetweenMappers() throws Exception
    {
        final ObjectMapper base = newJsonMapper();
        final StdConvertingSerializer shared = new StdConvertingSerializer(
                new PlainToObject(), base.constructType(Object.class), null, null);
        final Map<String, Integer> counts2 = new TreeMap<>();
        ObjectMapper mapper1 = JsonMapper.builder()
                .addModule(new SimpleModule()
                        .addSerializer(PlainInput.class, shared)
                        .addSerializer(A.class, new Counting<>(A.class, "first", counts2)))
                .build();
        ObjectMapper mapper2 = JsonMapper.builder()
                .addModule(new SimpleModule()
                        .addSerializer(PlainInput.class, shared)
                        .addSerializer(A.class, new Counting<>(A.class, "second", counts2)))
                .build();

        assertEquals(q("first"), mapper1.writeValueAsString(new PlainInput(new A())));
        assertEquals(q("second"), mapper2.writeValueAsString(new PlainInput(new A())));
        assertEquals(q("first"), mapper1.writeValueAsString(new PlainInput(new A())));
        assertEquals(q("second"), mapper2.writeValueAsString(new PlainInput(new A())));
    }
}
