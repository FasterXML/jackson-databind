package com.fasterxml.jackson.databind.records;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.cfg.CoercionAction;
import com.fasterxml.jackson.databind.cfg.CoercionInputShape;
import com.fasterxml.jackson.databind.exc.InvalidNullException;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.testutil.DatabindTestUtil;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

// [databind#3874]
public class RecordNullHandling3847Test extends DatabindTestUtil {
    /*
    /**********************************************************
    /* Set up
    /**********************************************************
     */

    static class Pojo3847 {
        public String fieldName;
    }

    public record PlainRecord(String fieldName) {}
    public record IntRecord(String description, int value) {}

    public record FixedRecord(@JsonProperty("field_name") String fieldName) {}

    /*
    /**********************************************************
    /* Tests
    /**********************************************************
     */

    private final ObjectMapper NULL_MAPPER = JsonMapper.builder()
        .defaultSetterInfo(JsonSetter.Value.construct(Nulls.FAIL, Nulls.FAIL))
        .withCoercionConfigDefaults(config -> config.setCoercion(CoercionInputShape.String,
                CoercionAction.Fail))
        .build();

    @Test
    public void testPojoNullHandlingValid() throws Exception {
        Pojo3847 pojo = NULL_MAPPER.readValue(a2q("{'fieldName': 'value'}"), Pojo3847.class); // expected
        assertEquals("value", pojo.fieldName);
    }

    @Test
    public void testPojoNullHandlingNullValue() throws Exception {
        try {
            NULL_MAPPER.readValue(a2q("{'fieldName': null}"), Pojo3847.class); // expected
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
    public void testRecordFixerNullHandlingValid() throws Exception {
        FixedRecord fixedRecord = NULL_MAPPER.readValue(a2q("{ 'field_name': 'value' }"), FixedRecord.class);
        assertEquals("value", fixedRecord.fieldName);
    }

    @Test
    public void testRecordFixerNullHandlingNullValue() throws Exception {
        try {
            NULL_MAPPER.readValue(a2q("{ 'field_name': null }"), FixedRecord.class);
            fail("should expect InvalidNullException");
        } catch (InvalidNullException e) {
            verifyException(e, "Invalid `null` value encountered for property \"field_name\"");
        }
    }

    @Test
    public void testRecordFixerNullHandlingEmptyJson() throws Exception {
        try {
            NULL_MAPPER.readValue("{}", FixedRecord.class);
            fail("should expect InvalidNullException");
        } catch (InvalidNullException e) {
            verifyException(e, "Invalid `null` value encountered for property \"field_name\"");
        }
    }

    @Test
    public void testRecordDefaultNullDeserialization() throws Exception {
        PlainRecord pr = new ObjectMapper().readValue("{}", PlainRecord.class);
        assertNull(pr.fieldName);
    }

    @Test
    public void testIntRecordDefaultNullDeserialization() throws Exception {
        IntRecord ir = new ObjectMapper().readValue("{}", IntRecord.class);
        assertNull(ir.description);
        assertEquals(0, ir.value);
    }
}
