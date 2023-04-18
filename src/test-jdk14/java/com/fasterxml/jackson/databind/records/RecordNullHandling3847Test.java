package com.fasterxml.jackson.databind.records;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import com.fasterxml.jackson.databind.BaseMapTest;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.cfg.CoercionAction;
import com.fasterxml.jackson.databind.cfg.CoercionInputShape;
import com.fasterxml.jackson.databind.exc.InvalidNullException;
import com.fasterxml.jackson.databind.json.JsonMapper;

// [databind#3874]
public class RecordNullHandling3847Test extends BaseMapTest {
    /*
    /**********************************************************
    /* Set up
    /**********************************************************
     */

    static class Pojo3847 {
        public String fieldName;
    }

    public record PlainRecord(String fieldName) {}

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

    public void testPojoNullHandlingValid() throws Exception {
        Pojo3847 pojo = NULL_MAPPER.readValue(a2q("{'fieldName': 'value'}"), Pojo3847.class); // expected
        assertEquals("value", pojo.fieldName);
    }

    public void testPojoNullHandlingNullValue() throws Exception {
        try {
            NULL_MAPPER.readValue(a2q("{'fieldName': null}"), Pojo3847.class); // expected
            fail("should expect InvalidNullException");
        } catch (InvalidNullException e) {
            verifyException(e, "Invalid `null` value encountered for property \"fieldName\"");
        }
    }

    public void testPojoNullHandlingEmptyJson() throws Exception {
        assertNotNull(NULL_MAPPER.readValue("{}", Pojo3847.class));
    }

    public void testRecordNullHandlingValid() throws Exception {
        PlainRecord plainRecord = NULL_MAPPER.readValue(a2q("{'fieldName': 'value'}"), PlainRecord.class);
        assertEquals("value", plainRecord.fieldName);
    }

    public void testRecordNullHandlingNullValue() throws Exception {
        try {
            NULL_MAPPER.readValue(a2q("{'fieldName': null}"), PlainRecord.class);
            fail("should expect InvalidNullException");
        } catch (InvalidNullException e) {
            verifyException(e, "Invalid `null` value encountered for property \"fieldName\"");
        }
    }

    public void testRecordNullHandlingEmptyJson() throws Exception {
        try {
            NULL_MAPPER.readValue("{}", PlainRecord.class);
            fail("should expect InvalidNullException");
        } catch (InvalidNullException e) {
            verifyException(e, "Invalid `null` value encountered for property \"fieldName\"");
        }
    }

    public void testRecordFixerNullHandlingValid() throws Exception {
        FixedRecord fixedRecord = NULL_MAPPER.readValue(a2q("{ 'field_name': 'value' }"), FixedRecord.class);
        assertEquals("value", fixedRecord.fieldName);
    }

    public void testRecordFixerNullHandlingNullValue() throws Exception {
        try {
            NULL_MAPPER.readValue(a2q("{ 'field_name': null }"), FixedRecord.class);
            fail("should expect InvalidNullException");
        } catch (InvalidNullException e) {
            verifyException(e, "Invalid `null` value encountered for property \"field_name\"");
        }
    }

    public void testRecordFixerNullHandlingEmptyJson() throws Exception {
        try {
            NULL_MAPPER.readValue("{}", FixedRecord.class);
            fail("should expect InvalidNullException");
        } catch (InvalidNullException e) {
            verifyException(e, "Invalid `null` value encountered for property \"field_name\"");
        }
    }
}
