package tools.jackson.databind.ser.jdk;

import java.util.*;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonInclude;

import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.*;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.module.SimpleModule;
import tools.jackson.databind.ser.std.StdScalarSerializer;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.assertEquals;

// [databind#6447]: EnumSetSerializer must reuse dynamically resolved enum serializer
public class EnumSetSerializerCaching6447Test extends DatabindTestUtil
{
    enum ABC { A, B, C }

    // Enum with constant-specific body: `getClass()` differs from `getDeclaringClass()`
    enum WithBody {
        X { @Override public String toString() { return "x"; } },
        Y
    }

    static class NonEmptyHolder {
        @JsonInclude(value = JsonInclude.Include.NON_EMPTY, content = JsonInclude.Include.NON_EMPTY)
        public Object values;

        NonEmptyHolder(EnumSet<?> v) { values = v; }
    }

    static class NonEmptyWildcardHolder {
        @JsonInclude(value = JsonInclude.Include.NON_EMPTY, content = JsonInclude.Include.NON_EMPTY)
        public EnumSet<?> values = EnumSet.of(ABC.A, ABC.B);
    }

    static class CountingSerializer<E extends Enum<E>> extends StdScalarSerializer<E> {
        int contextualizations;

        CountingSerializer(Class<E> type) { super(type); }

        @Override
        public ValueSerializer<?> createContextual(SerializationContext ctxt, BeanProperty p) {
            ++contextualizations;
            return this;
        }

        @Override
        public void serialize(E v, JsonGenerator g, SerializationContext ctxt) {
            g.writeString(v.name());
        }
    }

    private final CountingSerializer<ABC> COUNTER = new CountingSerializer<>(ABC.class);

    private final CountingSerializer<WithBody> BODY_COUNTER = new CountingSerializer<>(WithBody.class);

    private final ObjectMapper MAPPER = JsonMapper.builder()
            .addModule(new SimpleModule()
                    .addSerializer(ABC.class, COUNTER)
                    .addSerializer(WithBody.class, BODY_COUNTER))
            .enable(SerializationFeature.APPLY_JSON_INCLUDE_FOR_CONTAINERS)
            .build();

    // NOTE: no test for `EnumSet` valued properties: for those `BeanPropertyWriter`
    // does not reuse `EnumSetSerializer` itself (separate problem)

    @Test
    public void enumSerializerResolvedOnceForUntypedElements() throws Exception
    {
        List<Object> sets = new ArrayList<>();
        for (int i = 0; i < 4; ++i) {
            sets.add(EnumSet.of(ABC.C));
        }
        assertEquals("""
                [["C"],["C"],["C"],["C"]]""",
                MAPPER.writeValueAsString(sets));
        assertEquals(1, COUNTER.contextualizations);
    }

    @Test
    public void enumSerializerResolvedOnceForRootValues() throws Exception
    {
        for (int i = 0; i < 4; ++i) {
            assertEquals("""
                    ["A","C"]""",
                    MAPPER.writeValueAsString(EnumSet.of(ABC.A, ABC.C)));
        }
        assertEquals(1, COUNTER.contextualizations);
    }

    // Used to fail with NPE: element type not known for content inclusion check
    @Test
    public void contentInclusionWithUnknownElementType() throws Exception
    {
        assertEquals("""
                {"values":["A","B"]}""",
                MAPPER.writeValueAsString(new NonEmptyWildcardHolder()));
    }

    // `isEmpty()` and `serializeContents()` should share resolved serializer,
    // also for Enums with constant-specific bodies
    @Test
    public void enumSerializerSharedWithEmptyCheck() throws Exception
    {
        assertEquals("""
                [{"values":["X","Y"]},{"values":["X"]}]""",
                MAPPER.writeValueAsString(List.of(
                        new NonEmptyHolder(EnumSet.of(WithBody.X, WithBody.Y)),
                        new NonEmptyHolder(EnumSet.of(WithBody.X)))));
        assertEquals(1, BODY_COUNTER.contextualizations);
    }
}
