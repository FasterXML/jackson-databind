package tools.jackson.databind.format;

import java.math.BigInteger;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonFormat;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.exc.InvalidFormatException;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

public class DifferentRadixNumberFormatTest extends DatabindTestUtil {

    private static final int HEX_RADIX = 16;
    private static final int BINARY_RADIX = 2;

    static class IntegerWrapper {
        public Integer value;

        public IntegerWrapper() {}
        public IntegerWrapper(Integer v) { value = v; }
    }

    static class IntWrapper {
        public int value;

        public IntWrapper() {}
        public IntWrapper(int v) { value = v; }
    }

    static class AnnotatedMethodIntWrapper {
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

    static class IncorrectlyAnnotatedMethodIntWrapper {
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

    static class AllIntegralTypeWrapper {
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

    private final ObjectMapper MAPPER = newJsonMapper();

    @Test
    void testIntSerializedAsHexString()
    {
        ObjectMapper mapper = jsonMapperBuilder()
                .withConfigOverride(int.class,
                        o -> o.setFormat(JsonFormat.Value.forShape(JsonFormat.Shape.STRING).withRadix(HEX_RADIX)))
                .build();
        IntWrapper intialIntWrapper = new IntWrapper(10);
        String expectedJson = a2q("{'value':'a'}");

        String json = mapper.writeValueAsString(intialIntWrapper);

        assertEquals(expectedJson, json);

        IntWrapper readBackIntWrapper = mapper.readValue(expectedJson, IntWrapper.class);
        assertEquals(intialIntWrapper.value, readBackIntWrapper.value);

        // And error case too:
        try {
            mapper.readValue(a2q("{'value':'XYZ'}"), IntWrapper.class);
            fail("Should not pass");
        } catch (InvalidFormatException e) {
            verifyException(e, "Cannot deserialize value of type `int` from String \"XYZ\"");
            verifyException(e, "not a valid representation of `int` value with radix 16");
        }
    }

    @Test
    void testIntSerializedAsHexStringWithDefaultRadix()
    {
        ObjectMapper mapper = jsonMapperBuilder()
                .defaultFormat(JsonFormat.Value.forRadix(HEX_RADIX).withShape(JsonFormat.Shape.STRING))
                .build();
        IntWrapper intialIntWrapper = new IntWrapper(10);
        String expectedJson = a2q("{'value':'a'}");

        String json = mapper.writeValueAsString(intialIntWrapper);

        assertEquals(expectedJson, json);

        IntWrapper readBackIntWrapper = mapper.readValue(expectedJson, IntWrapper.class);

        assertNotNull(readBackIntWrapper);
        assertEquals(intialIntWrapper.value, readBackIntWrapper.value);

        // And error case too:
        try {
            mapper.readValue(a2q("{'value':'_x'}"), IntWrapper.class);
            fail("Should not pass");
        } catch (InvalidFormatException e) {
            verifyException(e, "Cannot deserialize value of type `int` from String \"_x\"");
            verifyException(e, "not a valid representation of `int` value with radix 16");
        }
    }

    @Test
    void testAnnotatedAccessorSerializedAsHexString()
    {
        AnnotatedMethodIntWrapper initialIntWrapper = new AnnotatedMethodIntWrapper(10);
        String expectedJson = a2q("{'value':'a'}");

        String json = MAPPER.writeValueAsString(initialIntWrapper);

        assertEquals(expectedJson, json);

        AnnotatedMethodIntWrapper readBackIntWrapper = MAPPER.readValue(expectedJson, AnnotatedMethodIntWrapper.class);

        assertNotNull(readBackIntWrapper);
        assertEquals(initialIntWrapper.value, readBackIntWrapper.value);
    }

    @Test
    void testAnnotatedAccessorWithoutRadixDoesNotThrow()
    {
        IncorrectlyAnnotatedMethodIntWrapper initialIntWrapper = new IncorrectlyAnnotatedMethodIntWrapper(10);
        assertEquals(a2q("{'value':'10'}"), MAPPER.writeValueAsString(initialIntWrapper));
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
        AllIntegralTypeWrapper initialIntegralTypeWrapper = new AllIntegralTypeWrapper((byte) 1,
                (byte) 2, (short) 3, (short) 4, 5, 6, 7L, 8L, new BigInteger("9"));
        String expectedJson = a2q("{'byteValue':'1','ByteValue':'10','shortValue':'11','ShortValue':'100','intValue':'101','IntegerValue':'110','longValue':'111','LongValue':'1000','bigInteger':'1001'}");

        assertEquals(expectedJson, MAPPER.writeValueAsString(initialIntegralTypeWrapper));

        AllIntegralTypeWrapper readbackIntegralTypeWrapper = MAPPER.readValue(expectedJson,
                AllIntegralTypeWrapper.class);

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
