package tools.jackson.databind.struct;

import java.io.IOException;
import java.io.StringReader;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URI;
import java.util.*;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import tools.jackson.core.json.JsonWriteFeature;
import tools.jackson.core.type.TypeReference;

import tools.jackson.databind.*;
import tools.jackson.databind.annotation.JsonSerialize;
import tools.jackson.databind.exc.MismatchedInputException;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.*;

public class UnwrapSingleArrayTest extends DatabindTestUtil
{
    // // // Inner types for UNWRAP_SINGLE_VALUE_ARRAYS feature tests

    static class BooleanBean {
        boolean _v;
        void setV(boolean v) { _v = v; }
    }

    static class StringWrapper {
        public String value;
    }

    // // // Inner types for per-property WRITE_SINGLE_ELEM_ARRAYS_UNWRAPPED tests

    static class StringArrayNotAnnoted {
        public String[] values;

        protected StringArrayNotAnnoted() { }
        public StringArrayNotAnnoted(String ... v) { values = v; }
    }

    @JsonPropertyOrder( { "strings", "ints", "bools" })
    static class WrapWriteWithArrays
    {
        @JsonProperty("strings")
        @JsonFormat(with={ JsonFormat.Feature.WRITE_SINGLE_ELEM_ARRAYS_UNWRAPPED })
        public String[] _strings = new String[] {
            "a"
        };

        @JsonFormat(without={ JsonFormat.Feature.WRITE_SINGLE_ELEM_ARRAYS_UNWRAPPED })
        public int[] ints = new int[] { 1 };

        public boolean[] bools = new boolean[] { true };
    }

    static class UnwrapShortArray {
        @JsonFormat(with={ JsonFormat.Feature.WRITE_SINGLE_ELEM_ARRAYS_UNWRAPPED })
        public short[] v = { (short) 7 };
    }

    static class UnwrapIntArray {
        @JsonFormat(with={ JsonFormat.Feature.WRITE_SINGLE_ELEM_ARRAYS_UNWRAPPED })
        public int[] v = { 3 };
    }

    static class UnwrapLongArray {
        @JsonFormat(with={ JsonFormat.Feature.WRITE_SINGLE_ELEM_ARRAYS_UNWRAPPED })
        public long[] v = { 1L };
    }

    static class UnwrapBooleanArray {
        @JsonFormat(with={ JsonFormat.Feature.WRITE_SINGLE_ELEM_ARRAYS_UNWRAPPED })
        public boolean[] v = { true };
    }

    static class UnwrapFloatArray {
        @JsonFormat(with={ JsonFormat.Feature.WRITE_SINGLE_ELEM_ARRAYS_UNWRAPPED })
        public float[] v = { 0.5f };
    }

    static class UnwrapDoubleArray {
        @JsonFormat(with={ JsonFormat.Feature.WRITE_SINGLE_ELEM_ARRAYS_UNWRAPPED })
        public double[] v = { 0.25 };
    }

    static class UnwrapIterable {
        @JsonFormat(with={ JsonFormat.Feature.WRITE_SINGLE_ELEM_ARRAYS_UNWRAPPED })
        @JsonSerialize(as=Iterable.class)
        public Iterable<String> v;

        public UnwrapIterable() {
            v = Collections.singletonList("foo");
        }

        public UnwrapIterable(String... values) {
            v = Arrays.asList(values);
        }
    }

    static class UnwrapCollection {
        @JsonFormat(with={ JsonFormat.Feature.WRITE_SINGLE_ELEM_ARRAYS_UNWRAPPED })
        @JsonSerialize(as=Collection.class)
        public Collection<String> v;

        public UnwrapCollection() {
            v = Collections.singletonList("foo");
        }

        public UnwrapCollection(String... values) {
            v = new LinkedHashSet<String>(Arrays.asList(values));
        }
    }

    static class UnwrapStringLike {
        @JsonFormat(with={ JsonFormat.Feature.WRITE_SINGLE_ELEM_ARRAYS_UNWRAPPED })
        public URI[] v = { URI.create("http://foo") };
    }

    // [databind#1232]
    static class SortedKeysMap {
        @JsonFormat(with = JsonFormat.Feature.WRITE_SORTED_MAP_ENTRIES)
        public Map<String,Integer> values = new LinkedHashMap<>();

        protected SortedKeysMap() { }

        public SortedKeysMap put(String key, int value) {
            values.put(key, value);
            return this;
        }
    }

    @JsonPropertyOrder( { "strings", "ints", "bools", "enums" })
    static class WrapWriteWithCollections
    {
        @JsonFormat(with={ JsonFormat.Feature.WRITE_SINGLE_ELEM_ARRAYS_UNWRAPPED })
        public List<String> strings = Arrays.asList("a");

        @JsonFormat(without={ JsonFormat.Feature.WRITE_SINGLE_ELEM_ARRAYS_UNWRAPPED })
        public Collection<Integer> ints = Arrays.asList(Integer.valueOf(1));

        public Set<Boolean> bools = Collections.singleton(true);

        @JsonFormat(with={ JsonFormat.Feature.WRITE_SINGLE_ELEM_ARRAYS_UNWRAPPED })
        public EnumSet<ABC> enums = EnumSet.of(ABC.B);
    }

    /*
    /**********************************************************
    /* Mapper and reader instances
    /**********************************************************
     */

    private final ObjectMapper MAPPER = newJsonMapper();

    private final ObjectMapper UNWRAPPING_MAPPER = jsonMapperBuilder()
            .enable(DeserializationFeature.UNWRAP_SINGLE_VALUE_ARRAYS)
            .build();

    private final ObjectReader NO_UNWRAPPING_READER = MAPPER.reader();
    private final ObjectReader UNWRAPPING_READER = MAPPER.reader()
            .with(DeserializationFeature.UNWRAP_SINGLE_VALUE_ARRAYS);

    /*
    /**********************************************************
    /* Tests for boolean
    /**********************************************************
     */

    @Test
    public void testBooleanPrimitiveArrayUnwrap() throws Exception
    {
        // [databind#381]
        ObjectMapper mapper = jsonMapperBuilder()
                .enable(DeserializationFeature.UNWRAP_SINGLE_VALUE_ARRAYS)
                .disable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES)
                .build();
        BooleanBean result = mapper.readValue(new StringReader("{\"v\":[true]}"), BooleanBean.class);
        assertTrue(result._v);

        _verifyMultiValueArrayFail("[{\"v\":[true,true]}]", BooleanBean.class);

        result = mapper.readValue("{\"v\":[null]}", BooleanBean.class);
        assertNotNull(result);
        assertFalse(result._v);

        result = mapper.readValue("[{\"v\":[null]}]", BooleanBean.class);
        assertNotNull(result);
        assertFalse(result._v);

        boolean[] array = mapper.readValue(new StringReader("[ [ null ] ]"), boolean[].class);
        assertNotNull(array);
        assertEquals(1, array.length);
        assertFalse(array[0]);
    }

    /*
    /**********************************************************
    /* Single-element as array tests, numbers
    /**********************************************************
     */

    // [databind#381]
    @Test
    public void testSingleElementScalarArrays() throws Exception {
        final int intTest = 932832;
        final double doubleTest = 32.3234;
        final long longTest = 2374237428374293423L;
        final short shortTest = (short) intTest;
        final float floatTest = 84.3743f;
        final byte byteTest = (byte) 43;
        final char charTest = 'c';

        ObjectMapper mapper = jsonMapperBuilder()
                .enable(DeserializationFeature.UNWRAP_SINGLE_VALUE_ARRAYS)
                .build();

        final int intValue = mapper.readValue(asArray(intTest), Integer.TYPE);
        assertEquals(intTest, intValue);
        final Integer integerWrapperValue = mapper.readValue(asArray(Integer.valueOf(intTest)), Integer.class);
        assertEquals(Integer.valueOf(intTest), integerWrapperValue);

        final double doubleValue = mapper.readValue(asArray(doubleTest), Double.class);
        assertEquals(doubleTest, doubleValue);
        final Double doubleWrapperValue = mapper.readValue(asArray(Double.valueOf(doubleTest)), Double.class);
        assertEquals(Double.valueOf(doubleTest), doubleWrapperValue);

        final long longValue = mapper.readValue(asArray(longTest), Long.TYPE);
        assertEquals(longTest, longValue);
        final Long longWrapperValue = mapper.readValue(asArray(Long.valueOf(longTest)), Long.class);
        assertEquals(Long.valueOf(longTest), longWrapperValue);

        final short shortValue = mapper.readValue(asArray(shortTest), Short.TYPE);
        assertEquals(shortTest, shortValue);
        final Short shortWrapperValue = mapper.readValue(asArray(Short.valueOf(shortTest)), Short.class);
        assertEquals(Short.valueOf(shortTest), shortWrapperValue);

        final float floatValue = mapper.readValue(asArray(floatTest), Float.TYPE);
        assertEquals(floatTest, floatValue);
        final Float floatWrapperValue = mapper.readValue(asArray(Float.valueOf(floatTest)), Float.class);
        assertEquals(Float.valueOf(floatTest), floatWrapperValue);

        final byte byteValue = mapper.readValue(asArray(byteTest), Byte.TYPE);
        assertEquals(byteTest, byteValue);
        final Byte byteWrapperValue = mapper.readValue(asArray(Byte.valueOf(byteTest)), Byte.class);
        assertEquals(Byte.valueOf(byteTest), byteWrapperValue);

        final char charValue = mapper.readValue(asArray(q(String.valueOf(charTest))), Character.TYPE);
        assertEquals(charTest, charValue);
        final Character charWrapperValue = mapper.readValue(asArray(q(String.valueOf(charTest))), Character.class);
        assertEquals(Character.valueOf(charTest), charWrapperValue);

        final boolean booleanTrueValue = mapper.readValue(asArray(true), Boolean.TYPE);
        assertTrue(booleanTrueValue);

        final boolean booleanFalseValue = mapper.readValue(asArray(false), Boolean.TYPE);
        assertFalse(booleanFalseValue);

        final Boolean booleanWrapperTrueValue = mapper.readValue(asArray(Boolean.valueOf(true)), Boolean.class);
        assertEquals(Boolean.TRUE, booleanWrapperTrueValue);
    }

    @Test
    public void testSingleElementArrayDisabled() throws Exception {
        final ObjectMapper mapper = jsonMapperBuilder()
                .disable(DeserializationFeature.UNWRAP_SINGLE_VALUE_ARRAYS)
                .build();
        try {
            mapper.readValue("[42]", Integer.class);
            fail("Single value array didn't throw an exception when DeserializationFeature.UNWRAP_SINGLE_VALUE_ARRAYS is disabled");
        } catch (MismatchedInputException exp) {
            //Exception was thrown correctly
        }
        try {
            mapper.readValue("[42]", Integer.TYPE);
            fail("Single value array didn't throw an exception when DeserializationFeature.UNWRAP_SINGLE_VALUE_ARRAYS is disabled");
        } catch (MismatchedInputException exp) {
            //Exception was thrown correctly
        }
        try {
            mapper.readValue("[42342342342342]", Long.class);
            fail("Single value array didn't throw an exception when DeserializationFeature.UNWRAP_SINGLE_VALUE_ARRAYS is disabled");
        } catch (MismatchedInputException exp) {
            //Exception was thrown correctly
        }
        try {
            mapper.readValue("[42342342342342342]", Long.TYPE);
            fail("Single value array didn't throw an exception when DeserializationFeature.UNWRAP_SINGLE_VALUE_ARRAYS is disabled");
        } catch (MismatchedInputException exp) {
            //Exception was thrown correctly
        }

        try {
            mapper.readValue("[42]", Short.class);
            fail("Single value array didn't throw an exception when DeserializationFeature.UNWRAP_SINGLE_VALUE_ARRAYS is disabled");
        } catch (MismatchedInputException exp) {
            //Exception was thrown correctly
        }
        try {
            mapper.readValue("[42]", Short.TYPE);
            fail("Single value array didn't throw an exception when DeserializationFeature.UNWRAP_SINGLE_VALUE_ARRAYS is disabled");
        } catch (MismatchedInputException exp) {
            //Exception was thrown correctly
        }

        try {
            mapper.readValue("[327.2323]", Float.class);
            fail("Single value array didn't throw an exception when DeserializationFeature.UNWRAP_SINGLE_VALUE_ARRAYS is disabled");
        } catch (MismatchedInputException exp) {
            //Exception was thrown correctly
        }
        try {
            mapper.readValue("[82.81902]", Float.TYPE);
            fail("Single value array didn't throw an exception when DeserializationFeature.UNWRAP_SINGLE_VALUE_ARRAYS is disabled");
        } catch (MismatchedInputException exp) {
            //Exception was thrown correctly
        }

        try {
            mapper.readValue("[22]", Byte.class);
            fail("Single value array didn't throw an exception when DeserializationFeature.UNWRAP_SINGLE_VALUE_ARRAYS is disabled");
        } catch (MismatchedInputException exp) {
            //Exception was thrown correctly
        }
        try {
            mapper.readValue("[22]", Byte.TYPE);
            fail("Single value array didn't throw an exception when DeserializationFeature.UNWRAP_SINGLE_VALUE_ARRAYS is disabled");
        } catch (MismatchedInputException exp) {
            //Exception was thrown correctly
        }

        try {
            mapper.readValue("['d']", Character.class);
            fail("Single value array didn't throw an exception when DeserializationFeature.UNWRAP_SINGLE_VALUE_ARRAYS is disabled");
        } catch (MismatchedInputException exp) {
            //Exception was thrown correctly
        }
        try {
            mapper.readValue("['d']", Character.TYPE);
            fail("Single value array didn't throw an exception when DeserializationFeature.UNWRAP_SINGLE_VALUE_ARRAYS is disabled");
        } catch (MismatchedInputException exp) {
            //Exception was thrown correctly
        }

        try {
            mapper.readValue("[true]", Boolean.class);
            fail("Single value array didn't throw an exception when DeserializationFeature.UNWRAP_SINGLE_VALUE_ARRAYS is disabled");
        } catch (MismatchedInputException exp) {
            //Exception was thrown correctly
        }
        try {
            mapper.readValue("[true]", Boolean.TYPE);
            fail("Single value array didn't throw an exception when DeserializationFeature.UNWRAP_SINGLE_VALUE_ARRAYS is disabled");
        } catch (MismatchedInputException exp) {
            //Exception was thrown correctly
        }
    }

    @Test
    public void testMultiValueArrayException() throws IOException {
        _verifyMultiValueArrayFail("[42,42]", Integer.class);
        _verifyMultiValueArrayFail("[42,42]", Integer.TYPE);
        _verifyMultiValueArrayFail("[42342342342342,42342342342342]", Long.class);
        _verifyMultiValueArrayFail("[42342342342342342,42342342342342]", Long.TYPE);
        _verifyMultiValueArrayFail("[42,42]", Short.class);
        _verifyMultiValueArrayFail("[42,42]", Short.TYPE);
        _verifyMultiValueArrayFail("[22,23]", Byte.class);
        _verifyMultiValueArrayFail("[22,23]", Byte.TYPE);
        _verifyMultiValueArrayFail("[327.2323,327.2323]", Float.class);
        _verifyMultiValueArrayFail("[82.81902,327.2323]", Float.TYPE);
        _verifyMultiValueArrayFail("[42.273,42.273]", Double.class);
        _verifyMultiValueArrayFail("[42.2723,42.273]", Double.TYPE);
        _verifyMultiValueArrayFail(asArray(q("c") + ","  + q("d")), Character.class);
        _verifyMultiValueArrayFail(asArray(q("c") + ","  + q("d")), Character.TYPE);
        _verifyMultiValueArrayFail("[true,false]", Boolean.class);
        _verifyMultiValueArrayFail("[true,false]", Boolean.TYPE);
    }

    /*
    /**********************************************************
    /* Simple non-primitive types
    /**********************************************************
     */

    @Test
    public void testSingleString() throws Exception
    {
        String value = "FOO!";
        String result = MAPPER.readValue("\""+value+"\"", String.class);
        assertEquals(value, result);
    }

    @Test
    public void testSingleStringWrapped() throws Exception
    {
        ObjectMapper mapper = jsonMapperBuilder()
                .disable(DeserializationFeature.UNWRAP_SINGLE_VALUE_ARRAYS)
                .build();

        String value = "FOO!";
        try {
            mapper.readValue("[\""+value+"\"]", String.class);
            fail("Exception not thrown when attempting to unwrap a single value 'String' array into a simple String");
        } catch (MismatchedInputException exp) {
            _verifyNoDeserFromArray(exp);
        }

        mapper = jsonMapperBuilder()
                .enable(DeserializationFeature.UNWRAP_SINGLE_VALUE_ARRAYS)
                .build();

        try {
            mapper.readValue("[\""+value+"\",\""+value+"\"]", String.class);
            fail("Exception not thrown when attempting to unwrap a single value 'String' array that contained more than one value into a simple String");
        } catch (MismatchedInputException exp) {
            verifyException(exp, "Attempted to unwrap");
        }
        String result = mapper.readValue("[\""+value+"\"]", String.class);
        assertEquals(value, result);
    }

    @Test
    public void testBigDecimal() throws Exception
    {
        ObjectMapper mapper = jsonMapperBuilder()
                .disable(DeserializationFeature.UNWRAP_SINGLE_VALUE_ARRAYS)
                .build();

        BigDecimal value = new BigDecimal("0.001");
        BigDecimal result = mapper.readValue(value.toString(), BigDecimal.class);
        assertEquals(value, result);
        try {
            mapper.readValue("[" + value.toString() + "]", BigDecimal.class);
            fail("Exception was not thrown when attempting to read a single value array of BigDecimal when UNWRAP_SINGLE_VALUE_ARRAYS feature is disabled");
        } catch (MismatchedInputException exp) {
            _verifyNoDeserFromArray(exp);
        }

        mapper = jsonMapperBuilder()
                .enable(DeserializationFeature.UNWRAP_SINGLE_VALUE_ARRAYS)
                .build();
        result = mapper.readValue("[" + value.toString() + "]", BigDecimal.class);
        assertEquals(value, result);

        try {
            mapper.readValue("[" + value.toString() + "," + value.toString() + "]", BigDecimal.class);
            fail("Exception was not thrown when attempting to read a muti value array of BigDecimal when UNWRAP_SINGLE_VALUE_ARRAYS feature is enabled");
        } catch (MismatchedInputException exp) {
            verifyException(exp, "Attempted to unwrap");
        }
    }

    @Test
    public void testBigInteger() throws Exception
    {
        ObjectMapper mapper = jsonMapperBuilder()
                .disable(DeserializationFeature.UNWRAP_SINGLE_VALUE_ARRAYS)
                .build();

        BigInteger value = new BigInteger("-1234567890123456789012345567809");
        BigInteger result = mapper.readValue(value.toString(), BigInteger.class);
        assertEquals(value, result);

        try {
            mapper.readValue("[" + value.toString() + "]", BigInteger.class);
            fail("Exception was not thrown when attempting to read a single value array of BigInteger when UNWRAP_SINGLE_VALUE_ARRAYS feature is disabled");
        } catch (MismatchedInputException exp) {
            _verifyNoDeserFromArray(exp);
        }

        mapper = jsonMapperBuilder()
                .enable(DeserializationFeature.UNWRAP_SINGLE_VALUE_ARRAYS)
                .build();
        result = mapper.readValue("[" + value.toString() + "]", BigInteger.class);
        assertEquals(value, result);

        try {
            mapper.readValue("[" + value.toString() + "," + value.toString() + "]", BigInteger.class);
            fail("Exception was not thrown when attempting to read a multi-value array of BigInteger when UNWRAP_SINGLE_VALUE_ARRAYS feature is enabled");
        } catch (MismatchedInputException exp) {
            verifyException(exp, "Attempted to unwrap");
        }
    }

    @Test
    public void testClassAsArray() throws Exception
    {
        Class<?> result = MAPPER
                    .readerFor(Class.class)
                    .with(DeserializationFeature.UNWRAP_SINGLE_VALUE_ARRAYS)
                    .readValue(q(String.class.getName()));
        assertEquals(String.class, result);

        try {
            MAPPER.readerFor(Class.class)
                .without(DeserializationFeature.UNWRAP_SINGLE_VALUE_ARRAYS)
                .readValue("[" + q(String.class.getName()) + "]");
            fail("Did not throw exception when UNWRAP_SINGLE_VALUE_ARRAYS feature was disabled and attempted to read a Class array containing one element");
        } catch (MismatchedInputException e) {
            _verifyNoDeserFromArray(e);
        }

        _verifyMultiValueArrayFail("[" + q(Object.class.getName()) + "," + q(Object.class.getName()) +"]",
                Class.class);
        result = MAPPER.readerFor(Class.class)
                .with(DeserializationFeature.UNWRAP_SINGLE_VALUE_ARRAYS)
                .readValue("[" + q(String.class.getName()) + "]");
        assertEquals(String.class, result);
    }

    @Test
    public void testURIAsArray() throws Exception
    {
        final ObjectReader reader = MAPPER.readerFor(URI.class);
        final URI value = new URI("http://foo.com");
        try {
            reader.without(DeserializationFeature.UNWRAP_SINGLE_VALUE_ARRAYS)
                .readValue("[\""+value.toString()+"\"]");
            fail("Did not throw exception for single value array when UNWRAP_SINGLE_VALUE_ARRAYS is disabled");
        } catch (MismatchedInputException e) {
            _verifyNoDeserFromArray(e);
        }

        _verifyMultiValueArrayFail("[\""+value.toString()+"\",\""+value.toString()+"\"]", URI.class);
    }

    @Test
    public void testUUIDAsArray() throws Exception
    {
        final ObjectReader reader = MAPPER.readerFor(UUID.class);
        final String uuidStr = "76e6d183-5f68-4afa-b94a-922c1fdb83f8";
        UUID uuid = UUID.fromString(uuidStr);
        try {
            NO_UNWRAPPING_READER.forType(UUID.class)
                .readValue("[" + q(uuidStr) + "]");
            fail("Exception was not thrown when UNWRAP_SINGLE_VALUE_ARRAYS is disabled and attempted to read a single value array as a single element");
        } catch (MismatchedInputException e) {
            _verifyNoDeserFromArray(e);
        }
        assertEquals(uuid,
                reader.with(DeserializationFeature.UNWRAP_SINGLE_VALUE_ARRAYS)
                    .readValue("[" + q(uuidStr) + "]"));
        _verifyMultiValueArrayFail("[" + q(uuidStr) + "," + q(uuidStr) + "]", UUID.class);
    }

    /*
    /**********************************************************
    /* Tests methods, POJOs/Maps
    /**********************************************************
     */

    @Test
    public void testSimplePOJOUnwrapping() throws Exception
    {
        ObjectReader r = UNWRAPPING_MAPPER.readerFor(IntWrapper.class);
        IntWrapper w = r.readValue(a2q("[{'i':42}]"));
        assertEquals(42, w.i);

        try {
            r.readValue(a2q("[{'i':42},{'i':16}]"));
            fail("Did not throw exception while reading a value from a multi value array");
        } catch (MismatchedInputException e) {
            verifyException(e, "more than one value");
        }
    }

    // [databind#2767]: should work for Maps, too
    @Test
    public void testSimpleMapUnwrapping() throws Exception
    {
        ObjectReader r = UNWRAPPING_MAPPER.readerFor(Map.class);
        Map<String,Object> m = r.readValue(a2q("[{'stuff':42}]"));
        assertEquals(Collections.<String,Object>singletonMap("stuff", Integer.valueOf(42)), m);

        try {
            r.readValue(a2q("[{'i':42},{'i':16}]"));
            fail("Did not throw exception while reading a value from a multi value array");
        } catch (MismatchedInputException e) {
            verifyException(e, "more than one value");
        }
    }

    @Test
    public void testEnumMapUnwrapping() throws Exception
    {
        ObjectReader r = UNWRAPPING_MAPPER.readerFor(new TypeReference<EnumMap<ABC,Integer>>() { });
        EnumMap<ABC,Integer> m = r.readValue(a2q("[{'A':42}]"));
        EnumMap<ABC,Integer> exp = new EnumMap<>(ABC.class);
        exp.put(ABC.A, Integer.valueOf(42));
        assertEquals(exp, m);

        try {
            r.readValue(a2q("[{'A':42},{'B':13}]"));
            fail("Did not throw exception while reading a value from a multi value array");
        } catch (MismatchedInputException e) {
            verifyException(e, "more than one value");
        }
    }

    // [databind#4844]: should work for wrapped null values too
    @Test
    public void testDeserializeArrayWithNullElement() throws Exception
    {
        StringWrapper v = UNWRAPPING_MAPPER
            .readerFor(StringWrapper.class)
            .readValue("{\"value\": [null]}");

        assertNotNull(v);
        assertNull(v.value);
    }

    /*
    /**********************************************************
    /* Tests, writing with per-property WRITE_SINGLE_ELEM_ARRAYS_UNWRAPPED
    /**********************************************************
     */

    @Test
    public void testWithArrayTypes() throws Exception
    {
        // default: strings unwrapped, ints wrapped
        assertEquals(a2q("{'strings':'a','ints':[1],'bools':[true]}"),
                MAPPER.writeValueAsString(new WrapWriteWithArrays()));

        // change global default to "yes, unwrap"; changes 'bools' only
        assertEquals(a2q("{'strings':'a','ints':[1],'bools':true}"),
                MAPPER.writer().with(SerializationFeature.WRITE_SINGLE_ELEM_ARRAYS_UNWRAPPED)
                .writeValueAsString(new WrapWriteWithArrays()));

        // change global default to "no, don't, unwrap", same as first case
        assertEquals(a2q("{'strings':'a','ints':[1],'bools':[true]}"),
                MAPPER.writer().without(SerializationFeature.WRITE_SINGLE_ELEM_ARRAYS_UNWRAPPED)
                .writeValueAsString(new WrapWriteWithArrays()));

        // And then without SerializationFeature but with config override:
        ObjectMapper mapper = jsonMapperBuilder()
                .withConfigOverride(String[].class,
                        v -> v.setFormat(JsonFormat.Value.empty()
                                .withFeature(JsonFormat.Feature.WRITE_SINGLE_ELEM_ARRAYS_UNWRAPPED)))
                .build();
        assertEquals(a2q("{'values':'a'}"),
                mapper.writeValueAsString(new StringArrayNotAnnoted("a")));
    }

    @Test
    public void testWithCollectionTypes() throws Exception
    {
        // default: strings unwrapped, ints wrapped
        assertEquals(a2q("{'strings':'a','ints':[1],'bools':[true],'enums':'B'}"),
                MAPPER.writeValueAsString(new WrapWriteWithCollections()));

        // change global default to "yes, unwrap"; changes 'bools' only
        assertEquals(a2q("{'strings':'a','ints':[1],'bools':true,'enums':'B'}"),
                MAPPER.writer().with(SerializationFeature.WRITE_SINGLE_ELEM_ARRAYS_UNWRAPPED)
                .writeValueAsString(new WrapWriteWithCollections()));

        // change global default to "no, don't, unwrap", same as first case
        assertEquals(a2q("{'strings':'a','ints':[1],'bools':[true],'enums':'B'}"),
                MAPPER.writer().without(SerializationFeature.WRITE_SINGLE_ELEM_ARRAYS_UNWRAPPED)
                .writeValueAsString(new WrapWriteWithCollections()));
    }

    // [databind#1232]: allow forcing sorting on Map keys
    @Test
    public void testOrderedMaps() throws Exception {
        SortedKeysMap map = new SortedKeysMap()
            .put("b", 2)
            .put("a", 1);
        assertEquals(a2q("{'values':{'a':1,'b':2}}"),
                MAPPER.writeValueAsString(map));
    }

    @Test
    public void testUnwrapWithPrimitiveArraysEtc() throws Exception {
        assertEquals("{\"v\":7}", MAPPER.writeValueAsString(new UnwrapShortArray()));
        assertEquals("{\"v\":3}", MAPPER.writeValueAsString(new UnwrapIntArray()));
        assertEquals("{\"v\":1}", MAPPER.writeValueAsString(new UnwrapLongArray()));
        assertEquals("{\"v\":true}", MAPPER.writeValueAsString(new UnwrapBooleanArray()));

        assertEquals("{\"v\":0.5}", MAPPER.writeValueAsString(new UnwrapFloatArray()));
        assertEquals("{\"v\":0.25}", MAPPER.writeValueAsString(new UnwrapDoubleArray()));
        assertEquals("0.5",
                MAPPER.writer().with(SerializationFeature.WRITE_SINGLE_ELEM_ARRAYS_UNWRAPPED)
                .writeValueAsString(new double[] { 0.5 }));

        assertEquals("{\"v\":\"foo\"}", MAPPER.writeValueAsString(new UnwrapIterable()));
        assertEquals("{\"v\":\"x\"}", MAPPER.writeValueAsString(new UnwrapIterable("x")));
        assertEquals("{\"v\":[\"x\",null]}", MAPPER.writeValueAsString(new UnwrapIterable("x", null)));

        assertEquals("{\"v\":\"foo\"}", MAPPER.writeValueAsString(new UnwrapCollection()));
        assertEquals("{\"v\":\"x\"}", MAPPER.writeValueAsString(new UnwrapCollection("x")));
        assertEquals("{\"v\":[\"x\",null]}", MAPPER.writeValueAsString(new UnwrapCollection("x", null)));

        assertEquals("{\"v\":\"http://foo\"}",
                MAPPER.writer()
                    .without(JsonWriteFeature.ESCAPE_FORWARD_SLASHES)
                    .writeValueAsString(new UnwrapStringLike()));
    }

    /*
    /**********************************************************
    /* Helper methods
    /**********************************************************
     */

    private void _verifyNoDeserFromArray(Exception e) {
        verifyException(e, "Cannot deserialize");
        verifyException(e, "from Array value");
        verifyException(e, "JsonToken.START_ARRAY");
    }

    private void _verifyMultiValueArrayFail(String input, Class<?> type) throws IOException {
        try {
            UNWRAPPING_READER.forType(type).readValue(input);
            fail("Single value array didn't throw an exception when DeserializationFeature.UNWRAP_SINGLE_VALUE_ARRAYS is disabled");
        } catch (MismatchedInputException e) {
            verifyException(e, "Attempted to unwrap");
        }
    }

    private static String asArray(Object value) {
        return "["+value+"]";
    }
}
