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

    record JsonPropertyRecord(@JsonProperty("aa") int a, int b) {}

    record JsonPropertyRecord2(int a, @JsonProperty("bb") int b) {}

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
        final String expected = a2q("{'id':'1','email':'test@records.com','nestedRecordTwo':{'id':'2','passport':'111110'}}");
        assertEquals(expected, output);
    }

    @Test
    public void testBasicSerializationOrderWithJsonProperty() throws Exception {
        JsonPropertyRecord jsonPropertyRecord = new JsonPropertyRecord(1, 2);
        final String output = MAPPER.writeValueAsString(jsonPropertyRecord);
        final String expected = "{\"aa\":1,\"b\":2}";
        assertEquals(expected, output);
    }

    @Test
    public void testBasicSerializationOrderWithJsonProperty2() throws Exception {
        JsonPropertyRecord2 jsonPropertyRecord = new JsonPropertyRecord2(1, 2);
        final String output = MAPPER.writeValueAsString(jsonPropertyRecord);
        final String expected = "{\"a\":1,\"bb\":2}";
        assertEquals(expected, output);
    }

    @Test
    public void testSerializationOrderWithJsonProperty() throws Exception {
        NestedRecordTwo nestedRecordTwo = new NestedRecordTwo("2", "111110");
        NestedRecordOneWithJsonProperty nestedRecordOne =
                new NestedRecordOneWithJsonProperty("1", "test@records.com", nestedRecordTwo);
        final String output = MAPPER.writeValueAsString(nestedRecordOne);
        final String expected = a2q("{'id':'1','email':'test@records.com','nestedProperty':{'id':'2','passport':'111110'}}");
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
        // In 3.0, sorting by Alphabetic enabled by default BUT it won't affect Creator props
        assertEquals(a2q("{'c':'c','a':'a','b':'b'}"),
                MAPPER.writeValueAsString(new CABRecord("c", "a", "b")));
        // Unless we disable Creator-props-first setting:
        assertEquals(a2q("{'a':'a','b':'b','c':'c'}"),
                jsonMapperBuilder()
                    .disable(MapperFeature.SORT_CREATOR_PROPERTIES_FIRST)
                    .build()
                    .writeValueAsString(new CABRecord("c", "a", "b")));
    }
}
