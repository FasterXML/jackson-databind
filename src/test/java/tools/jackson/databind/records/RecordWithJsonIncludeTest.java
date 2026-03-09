package tools.jackson.databind.records;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonIncludeProperties;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class RecordWithJsonIncludeTest extends DatabindTestUtil
{
    // Basic @JsonInclude
    public record AnnotatedParamRecordClass(
        @JsonInclude(JsonInclude.Include.NON_NULL)
        String omitFieldIfNull,
        String standardField
    ) { }

    public record AnnotatedGetterRecordClass(
        String omitFieldIfNull,
        String standardField
    ) {
        @JsonInclude(JsonInclude.Include.NON_NULL)
        @Override
        public String omitFieldIfNull() {
            return omitFieldIfNull;
        }
    }

    // [databind#4629], [databind#4630]
    public record Id2Name(int id, String name) { }

    // [databind#4629]
    public record RecordWithInclude4629(
            @JsonIncludeProperties("id") Id2Name child
    ) { }

    public record RecordWithIgnore4629(
            @JsonIgnoreProperties("name") Id2Name child
    ) { }

    // [databind#4630]
    public record RecordWithJsonIncludeProperties(@JsonIncludeProperties("id") Id2Name child) {
        @Override
        public Id2Name child() {
            return child;
        }
    }

    public record RecordWithJsonIgnoreProperties(@JsonIgnoreProperties("name") Id2Name child) {
        @Override
        public Id2Name child() {
            return child;
        }
    }

    private final ObjectMapper MAPPER = newJsonMapper();

    /*
    /**********************************************************************
    /* Test methods, @JsonInclude on record parameters/getters
    /**********************************************************************
     */

    @Test
    public void testJsonIncludeOnRecordParam() throws Exception
    {
        assertEquals(a2q("{'standardField':'def'}"),
            MAPPER.writeValueAsString(new AnnotatedParamRecordClass(null, "def")));
        assertEquals(a2q("{'standardField':'def'}"),
            MAPPER.writeValueAsString(new AnnotatedGetterRecordClass(null, "def")));
    }

    /*
    /**********************************************************************
    /* Test methods, @JsonIncludeProperties/@JsonIgnoreProperties on fields [databind#4629]
    /**********************************************************************
     */

    // [databind#4629]
    @Test
    void testJsonIncludeProperties4629() throws Exception
    {
        RecordWithInclude4629 expected = new RecordWithInclude4629(new Id2Name(123, null));
        String input = "{\"child\":{\"id\":123,\"name\":\"Bob\"}}";

        RecordWithInclude4629 actual = MAPPER.readValue(input, RecordWithInclude4629.class);

        assertEquals(expected, actual);
    }

    @Test
    void testJsonIgnoreProperties4629() throws Exception
    {
        RecordWithIgnore4629 expected = new RecordWithIgnore4629(new Id2Name(123, null));
        String input = "{\"child\":{\"id\":123,\"name\":\"Bob\"}}";

        RecordWithIgnore4629 actual = MAPPER.readValue(input, RecordWithIgnore4629.class);

        assertEquals(expected, actual);
    }

    /*
    /**********************************************************************
    /* Test methods, @JsonIncludeProperties/@JsonIgnoreProperties with overridden accessor [databind#4630]
    /**********************************************************************
     */

    // [databind#4630]
    @Test
    public void testSerializeJsonIncludeProperties4630() throws Exception {
        String json = MAPPER.writeValueAsString(new RecordWithJsonIncludeProperties(new Id2Name(123, "Bob")));
        assertEquals(a2q("{'child':{'id':123}}"), json);
    }

    @Test
    public void testSerializeJsonIgnoreProperties4630() throws Exception {
        String json = MAPPER.writeValueAsString(new RecordWithJsonIgnoreProperties(new Id2Name(123, "Bob")));
        assertEquals(a2q("{'child':{'id':123}}"), json);
    }
}
