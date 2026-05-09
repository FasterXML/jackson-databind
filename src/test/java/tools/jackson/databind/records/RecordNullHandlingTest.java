package tools.jackson.databind.records;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;

import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.ObjectReader;
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

    // [databind#5418]
    record TestRecord5418(String subject, String body) {}

    // [databind#2974]
    record RecordWithNonNullDefs2974(
            @JsonSetter(nulls=Nulls.AS_EMPTY) List<String> names,
            @JsonSetter(nulls=Nulls.FAIL) Map<String, Integer> agesByNames)
    { }

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

    /*
    /**********************************************************************
    /* Test methods, NON_ABSENT/NON_NULL via changeDefaultPropertyInclusion [databind#5418]
    /**********************************************************************
     */

    // [databind#5418]: NON_ABSENT/NON_NULL inclusion not working with Records
    // when configured via changeDefaultPropertyInclusion()
    @Test
    public void testNonAbsentInclusionViaDefaultConfig5418() throws Exception
    {
        ObjectMapper mapper = JsonMapper.builder()
            .changeDefaultPropertyInclusion(
                incl -> JsonInclude.Value.construct(
                    JsonInclude.Include.NON_ABSENT,
                    JsonInclude.Include.NON_ABSENT))
            .build();

        // Should exclude null fields
        String json = mapper.writeValueAsString(new TestRecord5418("test subject", null));
        assertEquals(a2q("{'subject':'test subject'}"), json);

        // Both null
        json = mapper.writeValueAsString(new TestRecord5418(null, null));
        assertEquals("{}", json);

        // Both present
        json = mapper.writeValueAsString(new TestRecord5418("test subject", "test body"));
        assertEquals(a2q("{'subject':'test subject','body':'test body'}"), json);
    }

    // [databind#5418]
    @Test
    public void testNonNullInclusionViaDefaultConfig5418() throws Exception
    {
        ObjectMapper mapper = JsonMapper.builder()
            .changeDefaultPropertyInclusion(
                incl -> JsonInclude.Value.construct(
                    JsonInclude.Include.NON_NULL,
                    JsonInclude.Include.NON_NULL))
            .build();

        // Should exclude null fields
        String json = mapper.writeValueAsString(new TestRecord5418("test subject", null));
        assertEquals(a2q("{'subject':'test subject'}"), json);

        // Both null
        json = mapper.writeValueAsString(new TestRecord5418(null, null));
        assertEquals("{}", json);

        // Both present
        json = mapper.writeValueAsString(new TestRecord5418("test subject", "test body"));
        assertEquals(a2q("{'subject':'test subject','body':'test body'}"), json);
    }

    /*
    /**********************************************************************
    /* Test methods, @JsonSetter(nulls=...) on record fields [databind#2974]
    /**********************************************************************
     */

    // [databind#2974]
    @Test
    public void testDeserializeWithNullAsEmpty() throws Exception
    {
        final ObjectReader r = DEFAULT_MAPPER.readerFor(RecordWithNonNullDefs2974.class);
        // First, regular case
        RecordWithNonNullDefs2974 value = r.readValue(a2q(
"{'names':['bob'],'agesByNames':{'bob':39}}"));
        assertEquals(1, value.names().size());
        assertEquals("bob", value.names().get(0));
        assertEquals(1, value.agesByNames().size());
        assertEquals(Integer.valueOf(39), value.agesByNames().get("bob"));

        // Then leave out list
        value = r.readValue(a2q("{'agesByNames':{'bob':42}}"));
        assertNotNull(value.names());
        assertEquals(0, value.names().size());
        assertNotNull(value.agesByNames());
        assertEquals(1, value.agesByNames().size());
        assertEquals(Integer.valueOf(42), value.agesByNames().get("bob"));
    }

    // [databind#2974]
    @Test
    public void testDeserializeWithFailForNull() throws Exception
    {
        final ObjectReader r = DEFAULT_MAPPER.readerFor(RecordWithNonNullDefs2974.class);
        // attempting to leave out Map ought to fail
        try {
            r.readValue(a2q("{'names':['bob']}"));
            fail("Should not pass with missing/null 'agesByNames'");
        } catch (InvalidNullException e) {
            verifyException(e, "property \"agesByNames\"");
        }
    }
}
