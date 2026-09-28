package com.fasterxml.jackson.databind.deser.std;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class NumericDeserializationReviewMain {

    public static void main(String[] args) throws JsonMappingException, JsonProcessingException {
        String byteJson = String.valueOf(Byte.MAX_VALUE);
        String smallDecimalFormInteger = "100.0";
        String smallExponentialFormInteger = "1e2";
        String shortJson = String.valueOf(Short.MAX_VALUE);
        String intJson = String.valueOf(Integer.MAX_VALUE);
        String longJson = String.valueOf(Long.MAX_VALUE);
        String bigIntegerJson = BigInteger.valueOf(Long.MAX_VALUE).add(BigInteger.ONE).toString();
        String bigIntegerExponentialFormJson = "1.23e56";
        String floatJson = String.valueOf(Float.MAX_VALUE);
        String doubleJson = String.valueOf(Double.MAX_VALUE);
        String bigDecimalJson = BigDecimal.valueOf(Double.MAX_VALUE).multiply(BigDecimal.TEN).add(new BigDecimal("1.2"))
                .toString();

        List<String> names = Arrays.asList("maxByte", "smallDecimalFormInteger", "smallExponentialFormInteger",
                "maxShort", "maxInt", "maxLong", "bigInteger", "bigIntegerExponentialForm", "maxFloat", "maxDouble",
                "bigDecimal");
        List<String> values = Arrays.asList(byteJson, smallDecimalFormInteger, smallExponentialFormInteger, shortJson,
                intJson, longJson, bigIntegerJson, bigIntegerExponentialFormJson, floatJson, doubleJson,
                bigDecimalJson);
        List<Class<?>> classes = Arrays.asList(ByteProperty.class, ShortProperty.class, IntProperty.class,
                LongProperty.class, FloatProperty.class, DoubleProperty.class, BigIntegerProperty.class,
                BigDecimalProperty.class);
        List<Class<?>> singleArgClasses = Arrays.asList(OfByte.class, OfShort.class, OfInt.class, OfLong.class,
                OfFloat.class, OfDouble.class, OfBigInteger.class, OfBigDecimal.class);

        for (int i = 0; i < values.size(); i++) {
            String value = values.get(i);
            String name = names.get(i);
            System.out.println();
            System.out.println("------ Property " + name + "-----------");
            System.out.println("  value " + value);
            System.out.println();
            for (Class<?> cls : classes) {
                check(value, propertyJson(value), cls);
            }
        }

        for (int i = 0; i < values.size(); i++) {
            String value = values.get(i);
            String name = names.get(i);
            System.out.println();
            System.out.println("------ SingleArg " + name + "-----------");
            System.out.println("  value " + value);
            System.out.println();
            for (Class<?> cls : singleArgClasses) {
                check(value, value, cls);
            }
        }

    }

    private static void check(String value, String json, Class<?> cls) {
        String result;
        String v;
        String err = null;
        try {
            v = new ObjectMapper().readValue(json, cls).toString();
            if (new BigDecimal(v).compareTo(new BigDecimal(value)) == 0) {
                result = "Ok";
            } else {
                BigDecimal ratio = new BigDecimal(v).divide(new BigDecimal(value));
                if (ratio.compareTo(BigDecimal.valueOf(1.01)) < 0 && ratio.compareTo(BigDecimal.valueOf(0.99)) > 0) {
                    result = "Prec"; // precision
                } else {
                    result = "?";
                }
            }
        } catch (Throwable e) {
            if (e.getMessage().toLowerCase().contains("no creators")) {
                v = "no creators";
            } else if (e.getMessage().toLowerCase().contains("infinity")) {
                v = "infinity";
            } else {
                v = limit(e.getMessage().replaceAll("\n.*", ""), 1000);
            }

            result = "Err";
            err = e.getMessage();
        }
        System.out.println(result + " " + cls.getSimpleName() + " " + v);
        if (err != null) {
//            System.out.println("  " + err);
        }
    }

    private static String limit(String s, int limit) {
        if (s.length() <= limit) {
            return s;
        } else {
            return s.substring(0, limit);
        }
    }

    // Conclusions for property deserialization
    //
    // integer types (byte, short, int, long) give error on overflow (good)
    // integer in exponential form 1e2 not parsed by any integer type (fail)
    // integer with decimal point (100.0) not parsed by any integer type (fail)
    // cannot parse Integer.MAX_VALUE or Long.MAX_VALUE to float (fail, should accept precision loss)
    // decimal types go to Infinity on overflow (acceptable)
    // decimal types lose precision rather than throw (good)

    // Conclusions for single-arg deserialization
    //
    // no support for byte, short, float (fail)
    // integer in exponential form 1e2 not parsed by any integer type (fail)
    // 1e2 not parsed by BigDecimal (fail)
    // 9223372036854775807 (Long.MAX_VALUE) not parsed by BigDecimal (fail)
    // 100 not parsed by BigDecimal (fail)
    // large decimal (not exponential form) not parsed by BigDecimal (fail)
    // large decimal (not exponential form) when parsed to Double should be Infinity
    // but throws (fail)

    private static String propertyJson(String value) {
        return "{\"name\":\"saturn\",\"value\":\"" + value + "\"}";
    }

    public static final class ByteProperty {
        final String name;
        final byte value;

        public ByteProperty(@JsonProperty("name") String name, @JsonProperty("value") byte value) {
            this.name = name;
            this.value = value;
        }

        @Override
        public String toString() {
            return String.valueOf(value);
        }
    }

    public static final class ShortProperty {
        final String name;
        final short value;

        public ShortProperty(@JsonProperty("name") String name, @JsonProperty("value") short value) {
            this.name = name;
            this.value = value;
        }

        @Override
        public String toString() {
            return String.valueOf(value);
        }
    }

    public static final class IntProperty {
        final String name;
        final int value;

        public IntProperty(@JsonProperty("name") String name, @JsonProperty("value") int value) {
            this.name = name;
            this.value = value;
        }

        @Override
        public String toString() {
            return String.valueOf(value);
        }
    }

    public static final class LongProperty {
        final String name;
        final long value;

        public LongProperty(@JsonProperty("name") String name, @JsonProperty("value") long value) {
            this.name = name;
            this.value = value;
        }

        @Override
        public String toString() {
            return String.valueOf(value);
        }
    }

    public static final class FloatProperty {
        final String name;
        final float value;

        public FloatProperty(@JsonProperty("name") String name, @JsonProperty("value") float value) {
            this.name = name;
            this.value = value;
        }

        @Override
        public String toString() {
            return String.valueOf(value);
        }
    }

    public static final class DoubleProperty {
        final String name;
        final double value;

        public DoubleProperty(@JsonProperty("name") String name, @JsonProperty("value") double value) {
            this.name = name;
            this.value = value;
        }

        @Override
        public String toString() {
            return String.valueOf(value);
        }
    }

    public static final class BigIntegerProperty {
        final String name;
        final BigInteger value;

        public BigIntegerProperty(@JsonProperty("name") String name, @JsonProperty("value") BigInteger value) {
            this.name = name;
            this.value = value;
        }

        @Override
        public String toString() {
            return String.valueOf(value);
        }
    }

    public static final class BigDecimalProperty {
        final String name;
        final BigDecimal value;

        public BigDecimalProperty(@JsonProperty("name") String name, @JsonProperty("value") BigDecimal value) {
            this.name = name;
            this.value = value;
        }

        @Override
        public String toString() {
            return String.valueOf(value);
        }
    }

    public static final class OfByte {
        final byte value;

        public OfByte(byte value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return String.valueOf(value);
        }
    }

    public static final class OfShort {
        final short value;

        public OfShort(short value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return String.valueOf(value);
        }
    }

    public static final class OfInt {
        final int value;

        public OfInt(int value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return String.valueOf(value);
        }
    }

    public static final class OfLong {
        final long value;

        public OfLong(long value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return String.valueOf(value);
        }
    }

    public static final class OfFloat {
        final float value;

        public OfFloat(float value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return String.valueOf(value);
        }
    }

    public static final class OfDouble {
        final double value;

        public OfDouble(double value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return String.valueOf(value);
        }
    }

    public static final class OfBigInteger {
        final BigInteger value;

        public OfBigInteger(BigInteger value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return String.valueOf(value);
        }
    }

    public static final class OfBigDecimal {
        final BigDecimal value;

        public OfBigDecimal(BigDecimal value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return String.valueOf(value);
        }
    }
}
