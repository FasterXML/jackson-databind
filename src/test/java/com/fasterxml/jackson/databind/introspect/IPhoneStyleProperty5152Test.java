package com.fasterxml.jackson.databind.introspect;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.testutil.DatabindTestUtil;

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

    @JsonPropertyOrder({ "4Roses", "$dollar", "_underscore" })
    static class NonLetterFirstCharBean {
        private String _4Roses;
        private String $dollar;
        private String _underscore;

        public String get4Roses() {
            return _4Roses;
        }

        public void set4Roses(String value) {
            _4Roses = value;
        }

        public String get$dollar() {
            return $dollar;
        }

        public void set$dollar(String value) {
            $dollar = value;
        }

        public String get_underscore() {
            return _underscore;
        }

        public void set_underscore(String value) {
            _underscore = value;
        }
    }

    private final ObjectMapper ENABLED = jsonMapperBuilder()
            .enable(MapperFeature.MIXED_CAPS_PROPERTY_NAMING)
            .build();

    private final ObjectMapper ENABLED_WITH_VALIDATION = jsonMapperBuilder()
            .enable(MapperFeature.MIXED_CAPS_PROPERTY_NAMING)
            .accessorNaming(new DefaultAccessorNamingStrategy.Provider()
                    .withFirstCharAcceptance(false, false)) // Don't allow lowercase or non-letter first chars
            .build();

    @Test
    public void testIPhoneStyleProperty() throws Exception {
        // Test with iPhone style property
        String json = "{\"iPhone\":\"iPhone 15\"}";
        IPhoneBean result = ENABLED.readValue(json, IPhoneBean.class);
        assertNotNull(result);
        assertEquals("iPhone 15", result.getIPhone());

        // Test serialization
        String serialized = ENABLED.writeValueAsString(result);
        assertEquals("{\"iPhone\":\"iPhone 15\"}", serialized);
    }

    @Test
    public void testRegularPojoProperty() throws Exception {
        // Test with regular POJO property
        String json = "{\"phoneNumber\":\"123-456-7890\"}";
        RegularBean result = ENABLED.readValue(json, RegularBean.class);
        assertNotNull(result);
        assertEquals("123-456-7890", result.getPhoneNumber());

        // Test serialization
        String serialized = ENABLED.writeValueAsString(result);
        assertEquals("{\"phoneNumber\":\"123-456-7890\"}", serialized);
    }


    @Test
    public void testDLogHeaderStyleProperty() throws Exception {
        // Test with DLogHeader style property
        String json = "{\"dLogHeader\":\"Debug Log Header\"}";
        DLogHeaderBean result = ENABLED.readValue(json, DLogHeaderBean.class);
        assertNotNull(result);
        assertEquals("Debug Log Header", result.getDLogHeader());

        // Test serialization
        String serialized = ENABLED.writeValueAsString(result);
        assertEquals("{\"dLogHeader\":\"Debug Log Header\"}", serialized);
    }

    @Test
    public void testKBSBroadCastingStyleProperty() throws Exception {
        // Test with KBSBroadCasting style property
        String json = "{\"KBSBroadCasting\":\"Korean Broadcasting System\"}";
        KBSBroadCastingBean result = ENABLED.readValue(json, KBSBroadCastingBean.class);
        assertNotNull(result);
        assertEquals("Korean Broadcasting System", result.getKBSBroadCasting());

        // Test serialization
        String serialized = ENABLED.writeValueAsString(result);
        assertEquals("{\"KBSBroadCasting\":\"Korean Broadcasting System\"}", serialized);
    }

    @Test
    public void testNonLetterFirstCharWithValidation() throws Exception {
        // Test with validation enabled - should ignore properties starting with non-letters
        NonLetterFirstCharBean result = ENABLED_WITH_VALIDATION.reader()
                .without(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .readValue("{\"4Roses\":\"Four Roses\",\"$dollar\":\"Dollar\",\"_underscore\":\"Underscore\"}",
                NonLetterFirstCharBean.class);
        assertNotNull(result);
        assertNull(result.get4Roses());
        assertNull(result.get$dollar());
        assertNull(result.get_underscore());

        // Test serialization - should not include properties starting with non-letters
        String serialized = ENABLED_WITH_VALIDATION.writeValueAsString(result);
        assertEquals("{}", serialized);
    }

    @Test
    public void testNonLetterFirstCharWithoutValidation() throws Exception {
        // Test without validation - should accept properties starting with non-letters
        NonLetterFirstCharBean result = ENABLED.readValue(
                "{\"4Roses\":\"Four Roses\",\"$dollar\":\"Dollar\",\"_underscore\":\"Underscore\"}",
                NonLetterFirstCharBean.class);
        assertNotNull(result);
        assertEquals("Four Roses", result.get4Roses());
        assertEquals("Dollar", result.get$dollar());
        assertEquals("Underscore", result.get_underscore());

        // Test serialization
        String serialized = ENABLED.writeValueAsString(result);
        assertEquals("{\"4Roses\":\"Four Roses\",\"$dollar\":\"Dollar\",\"_underscore\":\"Underscore\"}", serialized);
    }

    @Test
    public void testPhoneStyleProperty() throws Exception {
        // Test with Phone style property
        String json = "{\"Phone\":\"iPhone 15\"}";
        PhoneBean result = ENABLED.readValue(json, PhoneBean.class);
        assertNotNull(result);
        assertEquals("iPhone 15", result.getPhone());

        // Test serialization
        String serialized = ENABLED.writeValueAsString(result);
        assertEquals("{\"Phone\":\"iPhone 15\"}", serialized);
    }

}