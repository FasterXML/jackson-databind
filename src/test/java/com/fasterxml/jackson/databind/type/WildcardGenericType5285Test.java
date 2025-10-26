package com.fasterxml.jackson.databind.type;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.testutil.DatabindTestUtil;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test for issue #5285: Wildcard generic type resolution should respect type bounds
 */
public class WildcardGenericType5285Test extends DatabindTestUtil
{
    // Generic wrapper class with bounded type parameter
    public static class MessageWrapper<T extends Settings> {
        public T settings;
        public String message;

        public MessageWrapper() { }

        public MessageWrapper(T settings, String message) {
            this.settings = settings;
            this.message = message;
        }

        public T getSettings() { return settings; }
        public void setSettings(T settings) { this.settings = settings; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
    }

    // Settings interface with polymorphic type info
    @JsonTypeInfo(use = Id.NAME, property = "type")
    @JsonSubTypes({
        @JsonSubTypes.Type(value = EmailSettings.class, name = "EMAIL"),
        @JsonSubTypes.Type(value = PhoneSettings.class, name = "PHONE")
    })
    public interface Settings { }

    public static class EmailSettings implements Settings {
        public String email;

        public EmailSettings() { }

        public EmailSettings(String email) {
            this.email = email;
        }

        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
    }

    public static class PhoneSettings implements Settings {
        public String phoneNumber;

        public PhoneSettings() { }

        public PhoneSettings(String phoneNumber) {
            this.phoneNumber = phoneNumber;
        }

        public String getPhoneNumber() { return phoneNumber; }
        public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }
    }

    // Test wrapper classes
    static class WildcardWrapper {
        public MessageWrapper<?> wildcardWrapper;
    }

    static class SpecificWrapper {
        public MessageWrapper<EmailSettings> specificWrapper;
    }

    @Test
    public void testWildcardTypeResolution() throws Exception
    {
        ObjectMapper mapper = newJsonMapper();

        // Create identical instances
        EmailSettings emailSettings = new EmailSettings("me@me.com");
        MessageWrapper<EmailSettings> wrapper = new MessageWrapper<>(emailSettings, "Sample Message");

        WildcardWrapper wildcardObj = new WildcardWrapper();
        wildcardObj.wildcardWrapper = wrapper;

        SpecificWrapper specificObj = new SpecificWrapper();
        specificObj.specificWrapper = wrapper;

        // Get the JavaType for both field declarations
        JavaType wildcardFieldType = mapper.getTypeFactory().constructType(
            WildcardWrapper.class.getDeclaredField("wildcardWrapper").getGenericType());
        JavaType specificFieldType = mapper.getTypeFactory().constructType(
            SpecificWrapper.class.getDeclaredField("specificWrapper").getGenericType());

        System.out.println("Wildcard field type: " + wildcardFieldType);
        System.out.println("Specific field type: " + specificFieldType);

        // Serialize both
        String wildcardJson = mapper.writeValueAsString(wildcardObj);
        String specificJson = mapper.writeValueAsString(specificObj);

        System.out.println("Wildcard JSON: " + wildcardJson);
        System.out.println("Specific JSON: " + specificJson);

        // The wildcard version should also include the "type" field for polymorphic serialization
        assertTrue(wildcardJson.contains("\"type\":\"EMAIL\""),
            "Wildcard wrapper should include type field for polymorphic serialization");

        // Both should produce equivalent JSON (modulo field names)
        assertTrue(specificJson.contains("\"type\":\"EMAIL\""),
            "Specific wrapper should include type field");

        // The type parameter should resolve to Settings, not Object
        JavaType contentType = wildcardFieldType.containedType(0);
        System.out.println("Wildcard content type: " + contentType);

        // Should be Settings or a subtype, not Object
        assertTrue(Settings.class.isAssignableFrom(contentType.getRawClass()) ||
                   contentType.getRawClass().equals(Settings.class),
            "Wildcard type parameter should resolve to Settings bound, not Object. Got: " + contentType);
    }
}
