package com.fasterxml.jackson.databind.tofix;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.testutil.DatabindTestUtil;
import com.fasterxml.jackson.databind.testutil.failure.JacksonTestFailureExpected;

import static org.junit.jupiter.api.Assertions.*;

// [databind#5152] Support "iPhone" style capitalized properties
public class IPhoneStyleProperty5152Test
        extends DatabindTestUtil
{
    static class IPhoneBean {
        private String iPhone;

        public String getIPhone() {
            return iPhone;
        }

        public void setIPhone(String value) {
            iPhone = value;
        }
    }

    static class RegularBean {
        private String phoneNumber;

        public String getPhoneNumber() {
            return phoneNumber;
        }

        public void setPhoneNumber(String value) {
            phoneNumber = value;
        }
    }

    // [databind#2835]: "dLogHeader" property
    static class DLogHeaderBean {
        private String DLogHeader;

        public String getDLogHeader() {
            return DLogHeader;
        }

        public void setDLogHeader(String value) {
            DLogHeader = value;
        }
    }

    static class KBSBroadCastingBean {
        private String KBSBroadCasting;

        public String getKBSBroadCasting() {
            return KBSBroadCasting;
        }

        public void setKBSBroadCasting(String value) {
            KBSBroadCasting = value;
        }
    }

    static class PhoneBean {
        private String Phone;

        public String getPhone() {
            return Phone;
        }
        public void setPhone(String value) {
            Phone = value;
        }
    }

    private final ObjectMapper MAPPER = jsonMapperBuilder()
            .build();

    @JacksonTestFailureExpected
    @Test
    public void testIPhoneStyleProperty() throws Exception {
        // Test with iPhone style property
        String json = "{\"iPhone\":\"iPhone 15\"}";
        IPhoneBean result = MAPPER.readValue(json, IPhoneBean.class);
        assertNotNull(result);
        assertEquals("iPhone 15", result.getIPhone());

        // Test serialization
        String serialized = MAPPER.writeValueAsString(result);
        assertEquals("{\"iPhone\":\"iPhone 15\"}", serialized);
    }

    @Test
    public void testRegularPojoProperty() throws Exception {
        // Test with regular POJO property
        String json = "{\"phoneNumber\":\"123-456-7890\"}";
        RegularBean result = MAPPER.readValue(json, RegularBean.class);
        assertNotNull(result);
        assertEquals("123-456-7890", result.getPhoneNumber());

        // Test serialization
        String serialized = MAPPER.writeValueAsString(result);
        assertEquals("{\"phoneNumber\":\"123-456-7890\"}", serialized);
    }

    // [databind#2835]: "dLogHeader" property
    @JacksonTestFailureExpected
    @Test
    public void testDLogHeaderStyleProperty() throws Exception {
        // Test with DLogHeader style property
        String json = "{\"dLogHeader\":\"Debug Log Header\"}";
        DLogHeaderBean result = MAPPER.readValue(json, DLogHeaderBean.class);
        assertNotNull(result);
        assertEquals("Debug Log Header", result.getDLogHeader());

        // Test serialization
        String serialized = MAPPER.writeValueAsString(result);
        assertEquals("{\"dLogHeader\":\"Debug Log Header\"}", serialized);
    }

    @JacksonTestFailureExpected
    @Test
    public void testKBSBroadCastingStyleProperty() throws Exception {
        // Test with KBSBroadCasting style property
        String json = "{\"KBSBroadCasting\":\"Korean Broadcasting System\"}";
        KBSBroadCastingBean result = MAPPER.readValue(json, KBSBroadCastingBean.class);
        assertNotNull(result);
        assertEquals("Korean Broadcasting System", result.getKBSBroadCasting());

        // Test serialization
        String serialized = MAPPER.writeValueAsString(result);
        assertEquals("{\"KBSBroadCasting\":\"Korean Broadcasting System\"}", serialized);
    }

    @JacksonTestFailureExpected
    @Test
    public void testPhoneStyleProperty() throws Exception {
        // Test with Phone style property
        String json = "{\"Phone\":\"iPhone 15\"}";
        PhoneBean result = MAPPER.readValue(json, PhoneBean.class);
        assertNotNull(result);
        assertEquals("iPhone 15", result.getPhone());

        // Test serialization
        String serialized = MAPPER.writeValueAsString(result);
        assertEquals("{\"Phone\":\"iPhone 15\"}", serialized);
    }

}