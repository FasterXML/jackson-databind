package tools.jackson.databind.records;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import tools.jackson.databind.MapperFeature;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class RecordSerializationOrderTest extends DatabindTestUtil
{
    record NestedRecordOne(String id, String email, NestedRecordTwo nestedRecordTwo) {}
    record NestedRecordOneWithJsonProperty(String id, String email,
            @JsonProperty("nestedProperty") NestedRecordTwo nestedRecordTwo) {}
    record NestedRecordOneWithJsonPropertyIndex(@JsonProperty(index = 2) String id,
            @JsonProperty(index = 0) String email,
            @JsonProperty(value = "nestedProperty", index = 1) NestedRecordTwo nestedRecordTwo) {}

    @JsonPropertyOrder({"email", "nestedProperty", "id"})
    record NestedRecordOneWithJsonPropertyOrder(String id,
            String email,
            @JsonProperty(value = "nestedProperty") NestedRecordTwo nestedRecordTwo) {}

    record NestedRecordTwo(String id, String passport) {}

    record CABRecord(String c, String a, String b) {}

    private final ObjectMapper MAPPER = newJsonMapper();

    /*
    /**********************************************************************
    /* Test methods, alternate constructors
    /**********************************************************************
     */

    @Test
    public void testSerializationOrder() throws Exception {
        NestedRecordTwo nestedRecordTwo = new NestedRecordTwo("2", "111110");
        NestedRecordOne nestedRecordOne = new NestedRecordOne("1", "test@records.com", nestedRecordTwo);
        final String output = MAPPER.writeValueAsString(nestedRecordOne);
        final String expected = "{\"email\":\"test@records.com\",\"id\":\"1\",\"nestedRecordTwo\":{\"id\":\"2\",\"passport\":\"111110\"}}";
        assertEquals(expected, output);
    }

    @Test
    public void testSerializationOrderWithJsonProperty() throws Exception {
        NestedRecordTwo nestedRecordTwo = new NestedRecordTwo("2", "111110");
        NestedRecordOneWithJsonProperty nestedRecordOne =
                new NestedRecordOneWithJsonProperty("1", "test@records.com", nestedRecordTwo);
        final String output = MAPPER.writeValueAsString(nestedRecordOne);
        final String expected = "{\"email\":\"test@records.com\",\"id\":\"1\",\"nestedProperty\":{\"id\":\"2\",\"passport\":\"111110\"}}";
        assertEquals(expected, output);
    }

    @Test
    public void testSerializationOrderWithJsonPropertyIndexes() throws Exception {
        NestedRecordTwo nestedRecordTwo = new NestedRecordTwo("2", "111110");
        NestedRecordOneWithJsonPropertyIndex nestedRecordOne =
                new NestedRecordOneWithJsonPropertyIndex("1", "test@records.com", nestedRecordTwo);
        final String output = MAPPER.writeValueAsString(nestedRecordOne);
        final String expected = "{\"email\":\"test@records.com\",\"nestedProperty\":{\"id\":\"2\",\"passport\":\"111110\"},\"id\":\"1\"}";
        assertEquals(expected, output);
    }

    @Test
    public void testSerializationOrderWithJsonPropertyOrder() throws Exception {
        NestedRecordTwo nestedRecordTwo = new NestedRecordTwo("2", "111110");
        NestedRecordOneWithJsonPropertyOrder nestedRecordOne =
                new NestedRecordOneWithJsonPropertyOrder("1", "test@records.com", nestedRecordTwo);
        final String output = MAPPER.writeValueAsString(nestedRecordOne);
        final String expected = "{\"email\":\"test@records.com\",\"nestedProperty\":{\"id\":\"2\",\"passport\":\"111110\"},\"id\":\"1\"}";
        assertEquals(expected, output);
    }

    // [databind#4580]
    @Test
    public void testSerializationOrderWrtCreatorAlphabetic() throws Exception {
        // In 3.0, sorting by Alphabetic enabled by default so
        assertEquals(a2q("{'a':'a','b':'b','c':'c'}"),
                MAPPER.writeValueAsString(new CABRecord("c", "a", "b")));
        // But can disable
        assertEquals(a2q("{'c':'c','a':'a','b':'b'}"),
                jsonMapperBuilder()
                    .disable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY)
                    .build()
                    .writeValueAsString(new CABRecord("c", "a", "b")));
        // Except if we tell it not to:
        assertEquals(a2q("{'c':'c','a':'a','b':'b'}"),
                jsonMapperBuilder()
                .enable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY)
                .enable(MapperFeature.SORT_CREATOR_PROPERTIES_BY_DECLARATION_ORDER)
                .build()
                .writeValueAsString(new CABRecord("c", "a", "b")));
    }
}
