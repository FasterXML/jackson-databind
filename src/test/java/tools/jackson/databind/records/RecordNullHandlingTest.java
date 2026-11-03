package tools.jackson.databind.records;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.Nulls;

import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.cfg.CoercionAction;
import tools.jackson.databind.cfg.CoercionInputShape;
import tools.jackson.databind.exc.InvalidNullException;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.*;

public class RecordNullHandlingTest extends DatabindTestUtil
{
    // [databind#3847]
    static class Pojo3847 {
        public String fieldName;
    }

    public record PlainRecord(String fieldName) {}
    public record IntRecord(String description, int value) {}
    public record FixedRecord(@JsonProperty("field_name") String fieldName) {}

    // [databind#3084]
    record Bar(String name, String value) {}
    record Foo(List<Bar> list) {}

    private final ObjectMapper NULL_MAPPER = JsonMapper.builder()
            .changeDefaultNullHandling(n -> n.withValueNulls(Nulls.FAIL)
                    .withContentNulls(Nulls.FAIL))
            .withCoercionConfigDefaults(config -> config.setCoercion(CoercionInputShape.String, CoercionAction.Fail))
            .build();

    private final ObjectMapper DEFAULT_MAPPER = jsonMapperBuilder()
            .disable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES)
            .build();

    /*
    /**********************************************************************
    /* Test methods, Nulls.FAIL configuration [databind#3847]
    /**********************************************************************
     */

    // [databind#3847]
    @Test
    public void testPojoNullHandlingValid() throws Exception {
        Pojo3847 pojo = NULL_MAPPER.readValue(a2q("{'fieldName': 'value'}"), Pojo3847.class);
        assertEquals("value", pojo.fieldName);
    }

    @Test
    public void testPojoNullHandlingNullValue() throws Exception {
        try {
            NULL_MAPPER.readValue(a2q("{'fieldName': null}"), Pojo3847.class);
            fail("should expect InvalidNullException");
        } catch (InvalidNullException e) {
            verifyException(e, "Invalid `null` value encountered for property \"fieldName\"");
        }
    }

    @Test
    public void testPojoNullHandlingEmptyJson() throws Exception {
        assertNotNull(NULL_MAPPER.readValue("{}", Pojo3847.class));
    }

    @Test
    public void testRecordNullHandlingValid() throws Exception {
        PlainRecord plainRecord = NULL_MAPPER.readValue(a2q("{'fieldName': 'value'}"), PlainRecord.class);
        assertEquals("value", plainRecord.fieldName);
    }

    @Test
    public void testRecordNullHandlingNullValue() throws Exception {
        try {
            NULL_MAPPER.readValue(a2q("{'fieldName': null}"), PlainRecord.class);
            fail("should expect InvalidNullException");
        } catch (InvalidNullException e) {
            verifyException(e, "Invalid `null` value encountered for property \"fieldName\"");
        }
    }

    @Test
    public void testRecordNullHandlingEmptyJson() throws Exception {
        try {
            NULL_MAPPER.readValue("{}", PlainRecord.class);
            fail("should expect InvalidNullException");
        } catch (InvalidNullException e) {
            verifyException(e, "Invalid `null` value encountered for property \"fieldName\"");
        }
    }

    @Test
    public void testRecordFixedNullHandlingValid() throws Exception {
        FixedRecord fixedRecord = NULL_MAPPER.readValue(a2q("{ 'field_name': 'value' }"), FixedRecord.class);
        assertEquals("value", fixedRecord.fieldName);
    }

    @Test
    public void testRecordFixedNullHandlingNullValue() throws Exception {
        try {
            NULL_MAPPER.readValue(a2q("{ 'field_name': null }"), FixedRecord.class);
            fail("should expect InvalidNullException");
        } catch (InvalidNullException e) {
            verifyException(e, "Invalid `null` value encountered for property \"field_name\"");
        }
    }

    @Test
    public void testRecordFixedNullHandlingEmptyJson() throws Exception {
        try {
            NULL_MAPPER.readValue("{}", FixedRecord.class);
            fail("should expect InvalidNullException");
        } catch (InvalidNullException e) {
            verifyException(e, "Invalid `null` value encountered for property \"field_name\"");
        }
    }

    @Test
    public void testRecordDefaultNullDeserialization() throws Exception {
        PlainRecord pr = DEFAULT_MAPPER.readValue("{}", PlainRecord.class);
        assertNull(pr.fieldName);
    }

    @Test
    public void testIntRecordDefaultNullDeserialization() throws Exception {
        IntRecord ir = DEFAULT_MAPPER.readerFor(IntRecord.class)
                .readValue("{}");
        assertNull(ir.description);
        assertEquals(0, ir.value);
    }

    /*
    /**********************************************************************
    /* Test methods, Nulls.AS_EMPTY with List<Record> [databind#3084]
    /**********************************************************************
     */

    // [databind#3084]
    @Test
    void testEmptyObjectIntoRecordWithListOfRecord() throws Exception {
        ObjectMapper mapper = jsonMapperBuilder()
                .changeDefaultNullHandling(h -> h.withContentNulls(Nulls.AS_EMPTY))
                .build();
        Foo foo = mapper.readValue("{}", Foo.class);
        assertNull(foo.list());
    }

    @Test
    void testNonEmptyListIntoRecordWithListOfRecord() throws Exception {
        ObjectMapper mapper = jsonMapperBuilder()
                .changeDefaultNullHandling(h -> h.withContentNulls(Nulls.AS_EMPTY))
                .build();
        Foo foo = mapper.readValue("{\"list\":[{\"name\":\"a\",\"value\":\"b\"}]}", Foo.class);
        assertNotNull(foo.list());
        assertEquals(1, foo.list().size());
        assertEquals("a", foo.list().get(0).name());
        assertEquals("b", foo.list().get(0).value());
    }
}
