package tools.jackson.databind.tofix;

import com.fasterxml.jackson.annotation.JsonInclude;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.testutil.DatabindTestUtil;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.testutil.failure.JacksonTestFailureExpected;

import static org.junit.jupiter.api.Assertions.assertEquals;

// [databind#5369] Need to support JsonInclude for Arrays as well
public class JsonIncludeForArray5369Test
        extends DatabindTestUtil
{
    /*
    /**********************************************************
    /* Filters
    /**********************************************************
     */

    static class FooFilter {
        @Override
        public boolean equals(Object other) {
            if (other == null) {
                return false;
            }
            return "foo".equals(other);
        }
    }

    /*
    /**********************************************************
    /* POJOs — one per array type
    /**********************************************************
     */

    static class ObjectArrayPojo {
        @JsonInclude(content = JsonInclude.Include.CUSTOM,
                contentFilter = FooFilter.class)
        public Object[] values;

        ObjectArrayPojo(Object... v) {
            values = v;
        }
    }

    static class StringArrayPojo {
        @JsonInclude(content = JsonInclude.Include.CUSTOM,
                contentFilter = FooFilter.class)
        public String[] values;

        StringArrayPojo(String... v) {
            values = v;
        }
    }

    static class BooleanArrayPojo {
        @JsonInclude(content = JsonInclude.Include.NON_DEFAULT)
        public boolean[] values;

        BooleanArrayPojo(boolean... v) {
            values = v;
        }
    }

    static class IntArrayPojo {
        @JsonInclude(content = JsonInclude.Include.NON_DEFAULT)
        public int[] values;

        IntArrayPojo(int... v) {
            values = v;
        }
    }

    static class LongArrayPojo {
        @JsonInclude(content = JsonInclude.Include.NON_DEFAULT)
        public long[] values;

        LongArrayPojo(long... v) {
            values = v;
        }
    }

    static class DoubleArrayPojo {
        @JsonInclude(content = JsonInclude.Include.NON_DEFAULT)
        public double[] values;

        DoubleArrayPojo(double... v) {
            values = v;
        }
    }

    /*
    /**********************************************************
    /* Mapper
    /**********************************************************
     */

    private final ObjectMapper MAPPER = jsonMapperBuilder()
            .enable(SerializationFeature.APPLY_JSON_INCLUDE_FOR_COLLECTIONS)
            .build();

    /*
    /**********************************************************
    /* Tests — reference arrays (expect filtering, FAIL today)
    /**********************************************************
     */

    @JacksonTestFailureExpected
    @Test
    public void testCustomFilterWithObjectArray() throws Exception {
        ObjectArrayPojo input = new ObjectArrayPojo(
                "1", "foo", "2"
        );

        // EXPECT foo to be filtered out — FAILS today
        assertEquals(
                a2q("{'values':['1','2']}"),
                MAPPER.writeValueAsString(input)
        );
    }

    @JacksonTestFailureExpected
    @Test
    public void testCustomFilterWithStringArray() throws Exception {
        StringArrayPojo input = new StringArrayPojo(
                "1", "foo", "2"
        );

        // EXPECT foo to be filtered out — FAILS today
        assertEquals(
                a2q("{'values':['1','2']}"),
                MAPPER.writeValueAsString(input)
        );
    }

    /*
    /**********************************************************
    /* Tests — primitive arrays (expect NON_DEFAULT filtering, FAIL today)
    /**********************************************************
     */

    @JacksonTestFailureExpected
    @Test
    public void testNonDefaultWithBooleanArray() throws Exception {
        BooleanArrayPojo input = new BooleanArrayPojo(
                true, false, true
        );

        // EXPECT default 'false' to be filtered out — FAILS today
        assertEquals(
                a2q("{'values':[true,true]}"),
                MAPPER.writeValueAsString(input)
        );
    }

    @JacksonTestFailureExpected
    @Test
    public void testNonDefaultWithIntArray() throws Exception {
        IntArrayPojo input = new IntArrayPojo(
                0, 1, 0, 2
        );

        // EXPECT default '0' to be filtered out — FAILS today
        assertEquals(
                a2q("{'values':[1,2]}"),
                MAPPER.writeValueAsString(input)
        );
    }

    @JacksonTestFailureExpected
    @Test
    public void testNonDefaultWithLongArray() throws Exception {
        LongArrayPojo input = new LongArrayPojo(
                0L, 1L, 0L, 2L
        );

        // EXPECT default '0L' to be filtered out — FAILS today
        assertEquals(
                a2q("{'values':[1,2]}"),
                MAPPER.writeValueAsString(input)
        );
    }

    @JacksonTestFailureExpected
    @Test
    public void testNonDefaultWithDoubleArray() throws Exception {
        DoubleArrayPojo input = new DoubleArrayPojo(
                0.0, 1.5, 0.0
        );

        // EXPECT default '0.0' to be filtered out — FAILS today
        assertEquals(
                a2q("{'values':[1.5]}"),
                MAPPER.writeValueAsString(input)
        );
    }
}
