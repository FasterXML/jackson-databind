package tools.jackson.databind.deser;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import tools.jackson.core.JsonParser;
import tools.jackson.core.ObjectReadContext;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.exc.MismatchedInputException;
import tools.jackson.databind.testutil.DatabindTestUtil;
import tools.jackson.databind.util.TokenBuffer;

import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Token stream that ends right after a property name (possible with
 * {@link TokenBuffer}; parsers of textual formats fail on their own) leaves
 * value deserializers without a current token: must be reported as
 * a problem with input, for all types of values.
 */
class EndOfInputAfterPropertyNameTest extends DatabindTestUtil
{
    static class Wrapper<T> { public T value; }

    static class IntBean { public int value; }
    static class LongBean { public long value; }
    static class ShortBean { public short value; }
    static class ByteBean { public byte value; }
    static class DoubleBean { public double value; }
    static class FloatBean { public float value; }
    static class BooleanBean { public boolean value; }
    static class CharBean { public char value; }

    static class Point { public int x; }
    record PointRecord(int x) { }
    enum ABC { A, B, C }

    private final ObjectMapper MAPPER = newJsonMapper();

    static Stream<TypeReference<?>> types() {
        return Stream.of(
                new TypeReference<IntBean>() { }, new TypeReference<LongBean>() { },
                new TypeReference<ShortBean>() { }, new TypeReference<ByteBean>() { },
                new TypeReference<DoubleBean>() { }, new TypeReference<FloatBean>() { },
                new TypeReference<BooleanBean>() { }, new TypeReference<CharBean>() { },
                new TypeReference<Wrapper<String>>() { }, new TypeReference<Wrapper<Integer>>() { },
                new TypeReference<Wrapper<Long>>() { }, new TypeReference<Wrapper<Short>>() { },
                new TypeReference<Wrapper<Byte>>() { }, new TypeReference<Wrapper<Double>>() { },
                new TypeReference<Wrapper<Float>>() { }, new TypeReference<Wrapper<Boolean>>() { },
                new TypeReference<Wrapper<Character>>() { },
                new TypeReference<Wrapper<BigDecimal>>() { }, new TypeReference<Wrapper<BigInteger>>() { },
                new TypeReference<Wrapper<UUID>>() { }, new TypeReference<Wrapper<Date>>() { },
                new TypeReference<Wrapper<Calendar>>() { }, new TypeReference<Wrapper<ABC>>() { },
                new TypeReference<Wrapper<Locale>>() { }, new TypeReference<Wrapper<Class<?>>>() { },
                new TypeReference<Wrapper<StringBuilder>>() { },
                new TypeReference<Wrapper<Object>>() { }, new TypeReference<Wrapper<JsonNode>>() { },
                new TypeReference<Wrapper<Point>>() { }, new TypeReference<Wrapper<PointRecord>>() { },
                new TypeReference<Wrapper<List<String>>>() { }, new TypeReference<Wrapper<List<Integer>>>() { },
                new TypeReference<Wrapper<Set<ABC>>>() { }, new TypeReference<Wrapper<EnumSet<ABC>>>() { },
                new TypeReference<Wrapper<Map<String, Integer>>>() { },
                new TypeReference<Wrapper<EnumMap<ABC, String>>>() { },
                new TypeReference<Wrapper<String[]>>() { }, new TypeReference<Wrapper<Object[]>>() { },
                new TypeReference<Wrapper<int[]>>() { }, new TypeReference<Wrapper<long[]>>() { },
                new TypeReference<Wrapper<boolean[]>>() { }, new TypeReference<Wrapper<double[]>>() { },
                new TypeReference<Wrapper<byte[]>>() { }, new TypeReference<Wrapper<char[]>>() { },
                new TypeReference<Wrapper<Optional<String>>>() { },
                new TypeReference<Wrapper<AtomicReference<String>>>() { },
                // and name directly within Map
                new TypeReference<Map<String, String>>() { },
                new TypeReference<Map<String, Integer>>() { },
                new TypeReference<Map<String, Object>>() { },
                new TypeReference<EnumMap<ABC, String>>() { },
                new TypeReference<Object>() { },
                new TypeReference<JsonNode>() { }
        );
    }

    @ParameterizedTest
    @MethodSource("types")
    void endOfInputAfterName(TypeReference<?> type) throws Exception
    {
        // Name that is valid for all types tested: "value" for beans, and Map
        // keys need to match Enum in case of `EnumMap`
        final boolean enumKey = type.getType().getTypeName().startsWith("java.util.EnumMap");
        try (TokenBuffer buf = TokenBuffer.forGeneration()) {
            buf.writeStartObject();
            buf.writeName(enumKey ? "A" : "value");
            try (JsonParser p = buf.asParser(ObjectReadContext.empty())) {
                assertThrows(MismatchedInputException.class,
                        () -> MAPPER.readerFor(MAPPER.constructType(type)).readValue(p));
            }
        }
    }
}
