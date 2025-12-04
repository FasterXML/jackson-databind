package tools.jackson.databind.format;

import com.fasterxml.jackson.annotation.JsonFormat;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.testutil.DatabindTestUtil;

import java.math.BigInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class DifferentRadixNumberFormatTest extends DatabindTestUtil {

    private static final int HEX_RADIX = 16;
    private static final int BINARY_RADIX = 2;

    private static class IntegerWrapper {
        public Integer value;

        public IntegerWrapper() {}
        public IntegerWrapper(Integer v) { value = v; }
    }

    private static class IntWrapper {
        public int value;

        public IntWrapper() {}
        public IntWrapper(int v) { value = v; }
    }

    private static class AnnotatedMethodIntWrapper {
        private int value;

        public AnnotatedMethodIntWrapper() {
        }
        public AnnotatedMethodIntWrapper(int v) {
            value = v;
        }

        @JsonFormat(shape = JsonFormat.Shape.STRING, radix = HEX_RADIX)
        public int getValue() {
            return value;
        }
    }

    private static class IncorrectlyAnnotatedMethodIntWrapper {
        private int value;

        public IncorrectlyAnnotatedMethodIntWrapper() {
        }
        public IncorrectlyAnnotatedMethodIntWrapper(int v) {
            value = v;
        }

        @JsonFormat(shape = JsonFormat.Shape.STRING)
        public int getValue() {
            return value;
        }
    }

    private static class AllIntegralTypeWrapper {
        @JsonFormat(shape = JsonFormat.Shape.STRING, radix = BINARY_RADIX)
        public byte byteValue;
        @JsonFormat(shape = JsonFormat.Shape.STRING, radix = BINARY_RADIX)
        public Byte ByteValue;

        @JsonFormat(shape = JsonFormat.Shape.STRING, radix = BINARY_RADIX)
        public short shortValue;
        @JsonFormat(shape = JsonFormat.Shape.STRING, radix = BINARY_RADIX)
        public Short ShortValue;

        @JsonFormat(shape = JsonFormat.Shape.STRING, radix = BINARY_RADIX)
        public int intValue;
        @JsonFormat(shape = JsonFormat.Shape.STRING, radix = BINARY_RADIX)
        public Integer IntegerValue;

        @JsonFormat(shape = JsonFormat.Shape.STRING, radix = BINARY_RADIX)
        public long longValue;
        @JsonFormat(shape = JsonFormat.Shape.STRING, radix = BINARY_RADIX)
        public Long LongValue;

        @JsonFormat(shape = JsonFormat.Shape.STRING, radix = BINARY_RADIX)
        public BigInteger bigInteger;

        public AllIntegralTypeWrapper() {
        }

        public AllIntegralTypeWrapper(byte byteValue, Byte ByteValue, short shortValue, Short ShortValue, int intValue,
                                      Integer IntegerValue, long longValue, Long LongValue, BigInteger bigInteger) {
            this.byteValue = byteValue;
            this.ByteValue = ByteValue;
            this.shortValue = shortValue;
            this.ShortValue = ShortValue;
            this.intValue = intValue;
            this.IntegerValue = IntegerValue;
            this.longValue = longValue;
            this.LongValue = LongValue;
            this.bigInteger = bigInteger;
        }
    }

    @Test
    void testIntSerializedAsHexString()
    {
        ObjectMapper mapper = jsonMapperBuilder()
                .withConfigOverride(int.class,
                        o -> o.setFormat(JsonFormat.Value.forShape(JsonFormat.Shape.STRING).withRadix(HEX_RADIX)))
                .build();
        IntWrapper intialIntWrapper = new IntWrapper(10);
        String expectedJson = "{'value':'a'}";

        String json = mapper.writeValueAsString(intialIntWrapper);

        assertEquals(a2q(expectedJson), json);

        IntWrapper readBackIntWrapper = mapper.readValue(a2q(expectedJson), IntWrapper.class);

        assertNotNull(readBackIntWrapper);
        assertEquals(intialIntWrapper.value, readBackIntWrapper.value);

    }

    @Test
    void testIntSerializedAsHexStringWithDefaultRadix()
    {
        ObjectMapper mapper = jsonMapperBuilder()
                .defaultFormat(JsonFormat.Value.forRadix(HEX_RADIX).withShape(JsonFormat.Shape.STRING))
                .build();
        IntWrapper intialIntWrapper = new IntWrapper(10);
        String expectedJson = "{'value':'a'}";

        String json = mapper.writeValueAsString(intialIntWrapper);

        assertEquals(a2q(expectedJson), json);

        IntWrapper readBackIntWrapper = mapper.readValue(a2q(expectedJson), IntWrapper.class);

        assertNotNull(readBackIntWrapper);
        assertEquals(intialIntWrapper.value, readBackIntWrapper.value);

    }

    @Test
    void testAnnotatedAccessorSerializedAsHexString()
    {
        ObjectMapper mapper = newJsonMapper();
        AnnotatedMethodIntWrapper initialIntWrapper = new AnnotatedMethodIntWrapper(10);
        String expectedJson = "{'value':'a'}";

        String json = mapper.writeValueAsString(initialIntWrapper);

        assertEquals(a2q(expectedJson), json);

        AnnotatedMethodIntWrapper readBackIntWrapper = mapper.readValue(a2q(expectedJson), AnnotatedMethodIntWrapper.class);

        assertNotNull(readBackIntWrapper);
        assertEquals(initialIntWrapper.value, readBackIntWrapper.value);
    }

    @Test
    void testAnnotatedAccessorWithoutRadixDoesNotThrow()
    {
        ObjectMapper mapper = newJsonMapper();
        IncorrectlyAnnotatedMethodIntWrapper initialIntWrapper = new IncorrectlyAnnotatedMethodIntWrapper(10);
        String expectedJson = "{'value':'10'}";

        String json = mapper.writeValueAsString(initialIntWrapper);

        assertEquals(a2q(expectedJson), json);
    }

    @Test
    void testUsingDefaultConfigOverrideRadixToSerializeAsHexString()
    {
        ObjectMapper mapper = jsonMapperBuilder()
                .withConfigOverride(Integer.class,
                        o -> o.setFormat(JsonFormat.Value.forShape(JsonFormat.Shape.STRING).withRadix(HEX_RADIX)))
                .build();
        IntegerWrapper intialIntegerWrapper = new IntegerWrapper(10);
        String expectedJson = "{'value':'a'}";

        String json = mapper.writeValueAsString(intialIntegerWrapper);

        assertEquals(a2q(expectedJson), json);

        IntegerWrapper readBackIntegerWrapper = mapper.readValue(a2q(expectedJson), IntegerWrapper.class);

        assertNotNull(readBackIntegerWrapper);
        assertEquals(intialIntegerWrapper.value, readBackIntegerWrapper.value);
    }

    @Test
    void testAllIntegralTypesGetSerializedAsBinary()
    {
        ObjectMapper mapper = newJsonMapper();
        AllIntegralTypeWrapper initialIntegralTypeWrapper = new AllIntegralTypeWrapper((byte) 1,
                (byte) 2, (short) 3, (short) 4, 5, 6, 7L, 8L, new BigInteger("9"));
        String expectedJson = "{'byteValue':'1','ByteValue':'10','shortValue':'11','ShortValue':'100','intValue':'101','IntegerValue':'110','longValue':'111','LongValue':'1000','bigInteger':'1001'}";

        String json = mapper.writeValueAsString(initialIntegralTypeWrapper);

        assertEquals(a2q(expectedJson), json);

        AllIntegralTypeWrapper readbackIntegralTypeWrapper = mapper.readValue(a2q(expectedJson), AllIntegralTypeWrapper.class);

        assertNotNull(readbackIntegralTypeWrapper);
        assertEquals(initialIntegralTypeWrapper.byteValue, readbackIntegralTypeWrapper.byteValue);
        assertEquals(initialIntegralTypeWrapper.ByteValue, readbackIntegralTypeWrapper.ByteValue);
        assertEquals(initialIntegralTypeWrapper.shortValue, readbackIntegralTypeWrapper.shortValue);
        assertEquals(initialIntegralTypeWrapper.ShortValue, readbackIntegralTypeWrapper.ShortValue);
        assertEquals(initialIntegralTypeWrapper.intValue, readbackIntegralTypeWrapper.intValue);
        assertEquals(initialIntegralTypeWrapper.IntegerValue, readbackIntegralTypeWrapper.IntegerValue);
        assertEquals(initialIntegralTypeWrapper.longValue, readbackIntegralTypeWrapper.longValue);
        assertEquals(initialIntegralTypeWrapper.LongValue, readbackIntegralTypeWrapper.LongValue);
        assertEquals(initialIntegralTypeWrapper.bigInteger, readbackIntegralTypeWrapper.bigInteger);
    }
}