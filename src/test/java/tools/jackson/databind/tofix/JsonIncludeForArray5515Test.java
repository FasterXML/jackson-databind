package tools.jackson.databind.tofix;

import com.fasterxml.jackson.annotation.JsonInclude;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.testutil.DatabindTestUtil;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.testutil.failure.JacksonTestFailureExpected;

import static org.junit.jupiter.api.Assertions.assertEquals;

// [databind#5515] Need to support JsonInclude for Arrays as well
public class JsonIncludeForArray5515Test
        extends DatabindTestUtil
{
    /*
    /**********************************************************
    /* Filters
    /**********************************************************
     */

    static class Foo5515Filter {
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

    static class ObjectArray5155Pojo {
        @JsonInclude(content = JsonInclude.Include.CUSTOM,
                contentFilter = Foo5515Filter.class)
        public Object[] values;

        ObjectArray5155Pojo(Object... v) {
            values = v;
        }
    }

    static class StringArray5515Pojo {
        @JsonInclude(content = JsonInclude.Include.CUSTOM,
                contentFilter = Foo5515Filter.class)
        public String[] values;

        StringArray5515Pojo(String... v) {
            values = v;
        }
    }

    static class BooleanArray5515Pojo {
        @JsonInclude(content = JsonInclude.Include.NON_DEFAULT)
        public boolean[] values;

        BooleanArray5515Pojo(boolean... v) {
            values = v;
        }
    }

    static class IntArray5515Pojo {
        @JsonInclude(content = JsonInclude.Include.NON_DEFAULT)
        public int[] values;

        IntArray5515Pojo(int... v) {
            values = v;
        }
    }

    static class LongArray5515Pojo {
        @JsonInclude(content = JsonInclude.Include.NON_DEFAULT)
        public long[] values;

        LongArray5515Pojo(long... v) {
            values = v;
        }
    }

    static class DoubleArray5515Pojo {
        @JsonInclude(content = JsonInclude.Include.NON_DEFAULT)
        public double[] values;

        DoubleArray5515Pojo(double... v) {
            values = v;
        }
    }

    /*
    /**********************************************************
    /* Mapper
    /**********************************************************
     */

    private final ObjectMapper MAPPER = jsonMapperBuilder()
            // We need something like this.
            // .enable(SerializationFeature.APPLY_JSON_INCLUDE_FOR_ARRAYS)
            .build();

    /*
    /**********************************************************
    /* Tests — reference arrays (expect filtering, FAIL today)
    /**********************************************************
     */

    @JacksonTestFailureExpected
    @Test
    public void testCustomFilterWithObjectArray() throws Exception {
        ObjectArray5155Pojo input = new ObjectArray5155Pojo(
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
        StringArray5515Pojo input = new StringArray5515Pojo(
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
        BooleanArray5515Pojo input = new BooleanArray5515Pojo(
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
        IntArray5515Pojo input = new IntArray5515Pojo(
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
        LongArray5515Pojo input = new LongArray5515Pojo(
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
        DoubleArray5515Pojo input = new DoubleArray5515Pojo(
                0.0, 1.5, 0.0
        );

        // EXPECT default '0.0' to be filtered out — FAILS today
        assertEquals(
                a2q("{'values':[1.5]}"),
                MAPPER.writeValueAsString(input)
        );
    }
}
