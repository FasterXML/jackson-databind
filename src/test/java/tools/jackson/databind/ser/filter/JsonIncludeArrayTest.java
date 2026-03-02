package tools.jackson.databind.ser.filter;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonInclude;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.*;

public class JsonIncludeArrayTest extends DatabindTestUtil
{
    // NON_EMPTY on array field itself
    static class NonEmptyByteArray {
        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        public byte[] value;

        public NonEmptyByteArray(byte... v) { value = v; }
    }

    static class NonEmptyShortArray {
        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        public short[] value;

        public NonEmptyShortArray(short... v) { value = v; }
    }

    static class NonEmptyCharArray {
        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        public char[] value;

        public NonEmptyCharArray(char... v) { value = v; }
    }

    static class NonEmptyIntArray {
        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        public int[] value;

        public NonEmptyIntArray(int... v) { value = v; }
    }

    static class NonEmptyLongArray {
        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        public long[] value;

        public NonEmptyLongArray(long... v) { value = v; }
    }

    static class NonEmptyBooleanArray {
        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        public boolean[] value;

        public NonEmptyBooleanArray(boolean... v) { value = v; }
    }

    static class NonEmptyDoubleArray {
        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        public double[] value;

        public NonEmptyDoubleArray(double... v) { value = v; }
    }

    static class NonEmptyFloatArray {
        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        public float[] value;

        public NonEmptyFloatArray(float... v) { value = v; }
    }

    // [databind#5515] Content filtering on arrays (CUSTOM / NON_EMPTY / NON_DEFAULT)
    static class Foo5515Filter {
        @Override
        public boolean equals(Object other) {
            if (other == null) {
                return false;
            }
            return "foo".equals(other);
        }
    }

    static class ObjectArray5515Pojo {
        @JsonInclude(content = JsonInclude.Include.CUSTOM,
                contentFilter = Foo5515Filter.class)
        public Object[] values;

        ObjectArray5515Pojo(Object... v) { values = v; }
    }

    static class StringArray5515PojoCustom {
        @JsonInclude(content = JsonInclude.Include.CUSTOM,
                contentFilter = Foo5515Filter.class)
        public String[] values;

        StringArray5515PojoCustom(String... v) { values = v; }
    }

    static class StringArray5515PojoNonEmpty {
        @JsonInclude(content = JsonInclude.Include.NON_EMPTY)
        public String[] values;

        StringArray5515PojoNonEmpty(String... v) { values = v; }
    }

    static class BooleanArray5515Pojo {
        @JsonInclude(content = JsonInclude.Include.NON_DEFAULT)
        public boolean[] values;

        BooleanArray5515Pojo(boolean... v) { values = v; }
    }

    static class IntArray5515Pojo {
        @JsonInclude(content = JsonInclude.Include.NON_DEFAULT)
        public int[] values;

        IntArray5515Pojo(int... v) { values = v; }
    }

    static class LongArray5515Pojo {
        @JsonInclude(content = JsonInclude.Include.NON_DEFAULT)
        public long[] values;

        LongArray5515Pojo(long... v) { values = v; }
    }

    static class DoubleArray5515Pojo {
        @JsonInclude(content = JsonInclude.Include.NON_DEFAULT)
        public double[] values;

        DoubleArray5515Pojo(double... v) { values = v; }
    }

    private final ObjectMapper MAPPER = newJsonMapper();

    // [databind#5515]
    private final ObjectMapper MAPPER_CONTAINERS = jsonMapperBuilder()
            .enable(SerializationFeature.APPLY_JSON_INCLUDE_FOR_CONTAINERS)
            .build();

    /*
    /**********************************************************************
    /* Test methods, @JsonInclude(NON_EMPTY) on array field
    /**********************************************************************
     */

    @Test
    public void testByteArray() throws Exception
    {
        assertEquals("{}", MAPPER.writeValueAsString(new NonEmptyByteArray()));
    }

    @Test
    public void testShortArray() throws Exception
    {
        assertEquals("{}", MAPPER.writeValueAsString(new NonEmptyShortArray()));
        assertEquals("{\"value\":[1]}", MAPPER.writeValueAsString(new NonEmptyShortArray((short) 1)));
    }

    @Test
    public void testCharArray() throws Exception
    {
        assertEquals("{}", MAPPER.writeValueAsString(new NonEmptyCharArray()));
        // by default considered to be serialized as String
        assertEquals("{\"value\":\"ab\"}", MAPPER.writeValueAsString(new NonEmptyCharArray('a', 'b')));
        // but can force as sparse (real) array too
        assertEquals("{\"value\":[\"a\",\"b\"]}", MAPPER
                .writer().with(SerializationFeature.WRITE_CHAR_ARRAYS_AS_JSON_ARRAYS)
                .writeValueAsString(new NonEmptyCharArray('a', 'b')));
    }

    @Test
    public void testIntArray() throws Exception
    {
        assertEquals("{}", MAPPER.writeValueAsString(new NonEmptyIntArray()));
        assertEquals("{\"value\":[2]}", MAPPER.writeValueAsString(new NonEmptyIntArray(2)));
    }

    @Test
    public void testLongArray() throws Exception
    {
        assertEquals("{}", MAPPER.writeValueAsString(new NonEmptyLongArray()));
        assertEquals("{\"value\":[3,4]}", MAPPER.writeValueAsString(new NonEmptyLongArray(3, 4)));
    }

    @Test
    public void testBooleanArray() throws Exception
    {
        assertEquals("{}", MAPPER.writeValueAsString(new NonEmptyBooleanArray()));
        assertEquals("{\"value\":[true,false]}", MAPPER.writeValueAsString(new NonEmptyBooleanArray(true,false)));
    }

    @Test
    public void testDoubleArray() throws Exception
    {
        assertEquals("{}", MAPPER.writeValueAsString(new NonEmptyDoubleArray()));
        assertEquals("{\"value\":[0.25,-1.0]}", MAPPER.writeValueAsString(new NonEmptyDoubleArray(0.25,-1.0)));
    }

    @Test
    public void testFloatArray() throws Exception
    {
        assertEquals("{}", MAPPER.writeValueAsString(new NonEmptyFloatArray()));
        assertEquals("{\"value\":[0.5]}", MAPPER.writeValueAsString(new NonEmptyFloatArray(0.5f)));
    }

    /*
    /**********************************************************************
    /* Test methods, content filtering on arrays [databind#5515]
    /**********************************************************************
     */

    // [databind#5515]
    @Test
    public void testCustomFilterWithObjectArray() throws Exception {
        assertEquals(a2q("{'values':['1','2']}"),
                MAPPER_CONTAINERS.writeValueAsString(new ObjectArray5515Pojo("1", "foo", "2")));
    }

    @Test
    public void testCustomFilterWithStringArray() throws Exception {
        assertEquals(a2q("{'values':['1','2']}"),
                MAPPER_CONTAINERS.writeValueAsString(new StringArray5515PojoCustom("1", "foo", "2")));
    }

    @Test
    public void testNonEmptyFilterWithStringArray() throws Exception {
        assertEquals(a2q("{'values':['1','foo']}"),
                MAPPER_CONTAINERS.writeValueAsString(new StringArray5515PojoNonEmpty("1", "foo", "")));
    }

    @Test
    public void testNonDefaultWithBooleanArray() throws Exception {
        assertEquals(a2q("{'values':[true,true]}"),
                MAPPER_CONTAINERS.writeValueAsString(new BooleanArray5515Pojo(true, false, true)));
    }

    @Test
    public void testNonDefaultWithIntArray() throws Exception {
        assertEquals(a2q("{'values':[1,2]}"),
                MAPPER_CONTAINERS.writeValueAsString(new IntArray5515Pojo(0, 1, 0, 2)));
    }

    @Test
    public void testNonDefaultWithLongArray() throws Exception {
        assertEquals(a2q("{'values':[1,2]}"),
                MAPPER_CONTAINERS.writeValueAsString(new LongArray5515Pojo(0L, 1L, 0L, 2L)));
    }

    @Test
    public void testNonDefaultWithDoubleArray() throws Exception {
        assertEquals(a2q("{'values':[1.5]}"),
                MAPPER_CONTAINERS.writeValueAsString(new DoubleArray5515Pojo(0.0, 1.5, 0.0)));
    }
}
