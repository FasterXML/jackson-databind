package tools.jackson.databind.ser.jdk;

import java.util.*;

import org.junit.jupiter.api.Test;

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

    static class CountingSerializer extends StdScalarSerializer<ABC> {
        int contextualizations;

        CountingSerializer() { super(ABC.class); }

        @Override
        public ValueSerializer<?> createContextual(SerializationContext ctxt, BeanProperty p) {
            ++contextualizations;
            return this;
        }

        @Override
        public void serialize(ABC v, JsonGenerator g, SerializationContext ctxt) {
            g.writeString(v.name());
        }
    }

    private final CountingSerializer COUNTER = new CountingSerializer();

    private final ObjectMapper MAPPER = JsonMapper.builder()
            .addModule(new SimpleModule().addSerializer(ABC.class, COUNTER))
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
}
