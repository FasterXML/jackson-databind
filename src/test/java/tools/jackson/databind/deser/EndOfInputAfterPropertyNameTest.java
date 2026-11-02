package tools.jackson.databind.deser;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import tools.jackson.core.JsonParser;
import tools.jackson.core.ObjectReadContext;
import tools.jackson.core.exc.UnexpectedEndOfInputException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.annotation.JsonDeserialize;
import tools.jackson.databind.annotation.JsonPOJOBuilder;
import tools.jackson.databind.exc.MismatchedInputException;
import tools.jackson.databind.testutil.DatabindTestUtil;
import tools.jackson.databind.util.TokenBuffer;

import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Token stream that ends right after a property name (possible with
 * {@link TokenBuffer}; parsers of textual formats fail on their own) leaves
 * value deserializers without a current token: must be reported as
 * a problem with input.
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

    enum DelegatingFactoryEnum {
        A;

        @JsonCreator
        static DelegatingFactoryEnum of(String id) { return A; }
    }

    enum PropsFactoryEnum {
        A;

        @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
        static PropsFactoryEnum of(@JsonProperty("id") String id) { return A; }
    }

    @JsonSubTypes(@JsonSubTypes.Type(value = PayloadImpl.class, name = "impl"))
    interface Payload { }
    static class PayloadImpl implements Payload { }

    // Regular property of types with External Type Id:

    static class ExternalIdBean {
        @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXTERNAL_PROPERTY,
                property = "type")
        public Payload payload;

        public int value;
    }

    static class ExternalIdCreatorBean {
        public int value;

        @JsonCreator
        ExternalIdCreatorBean(@JsonProperty("payload")
                @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXTERNAL_PROPERTY,
                        property = "type")
                Payload payload) { }
    }

    @JsonDeserialize(builder = ExternalIdViaBuilder.Builder.class)
    static class ExternalIdViaBuilder {
        @JsonPOJOBuilder(withPrefix = "")
        static class Builder {
            @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXTERNAL_PROPERTY,
                    property = "type")
            public Payload payload;

            public int value;

            public ExternalIdViaBuilder build() { return new ExternalIdViaBuilder(); }
        }
    }

    @JsonTypeInfo(use = JsonTypeInfo.Id.DEDUCTION)
    @JsonSubTypes(@JsonSubTypes.Type(DeducedImpl.class))
    interface Deduced { }
    static class DeducedImpl implements Deduced { public int value; }

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
                new TypeReference<Wrapper<ByteBuffer>>() { },
                new TypeReference<Wrapper<DelegatingFactoryEnum>>() { },
                new TypeReference<Wrapper<PropsFactoryEnum>>() { },
                new TypeReference<ExternalIdBean>() { },
                new TypeReference<ExternalIdCreatorBean>() { },
                new TypeReference<ExternalIdViaBuilder>() { },
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
        _verifyFailure(MismatchedInputException.class, type, enumKey ? "A" : "value");
    }

    // Unlike above, content gets buffered via `TokenBuffer.copyCurrentStructure()`
    // which has its own exception type for the problem
    @Test
    void endOfInputAfterNameWithDeduction() throws Exception
    {
        _verifyFailure(UnexpectedEndOfInputException.class, new TypeReference<Deduced>() { }, "value");
    }

    private void _verifyFailure(Class<? extends Exception> expFailure, TypeReference<?> type,
            String name) throws Exception
    {
        try (TokenBuffer buf = TokenBuffer.forGeneration()) {
            buf.writeStartObject();
            buf.writeName(name);
            try (JsonParser p = buf.asParser(ObjectReadContext.empty())) {
                assertThrows(expFailure,
                        () -> MAPPER.readerFor(MAPPER.constructType(type)).readValue(p));
            }
        }
    }
}
