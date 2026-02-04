package tools.jackson.databind.type;

import java.lang.reflect.Type;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import tools.jackson.databind.*;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.*;

// Tests for [databind#5285]: unbounded wildcards should resolve to
// the type variable's declared upper bound, not Object.
class WildcardBoundResolve5285Test extends DatabindTestUtil
{
    // -- Test type hierarchy from the original issue report --

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
    @JsonSubTypes({
        @JsonSubTypes.Type(value = EmailSettings.class, name = "EMAIL"),
        @JsonSubTypes.Type(value = PhoneSettings.class, name = "PHONE")
    })
    interface Settings { }

    record EmailSettings(String email) implements Settings { }

    record PhoneSettings(String phoneNumber) implements Settings { }

    record MessageWrapper<T extends Settings>(T settings, String message) { }

    // Container to obtain ParameterizedType via field reflection
    static class Holder {
        MessageWrapper<?> wildcardWrapper;
        MessageWrapper<EmailSettings> specificWrapper;
    }

    // -- Additional test types --

    static class Box<T extends Number> {
        public T value;
    }

    static class BoxHolder {
        Box<?> wildcardBox;
    }

    static class Pair<L, R> {
        public L left;
        public R right;
    }

    @SuppressWarnings("unused")
    static class PairHolder {
        Pair<String, ?> wildcardPair;
    }

    // -- Tests --

    private final ObjectMapper MAPPER = newJsonMapper();

    // Core issue: wildcard type resolution should use declared bound
    @Test
    void wildcardResolvesToDeclaredBound() throws Exception {
        Type genericType = Holder.class.getDeclaredField("wildcardWrapper").getGenericType();
        JavaType jacksonType = MAPPER.constructType(genericType);

        // Should resolve to MessageWrapper<Settings>, NOT MessageWrapper<Object>
        assertEquals(MessageWrapper.class, jacksonType.getRawClass());
        assertEquals(1, jacksonType.containedTypeCount());
        assertEquals(Settings.class, jacksonType.containedType(0).getRawClass());
    }

    // Specific type should still work unchanged
    @Test
    void specificTypeUnchanged() throws Exception {
        Type genericType = Holder.class.getDeclaredField("specificWrapper").getGenericType();
        JavaType jacksonType = MAPPER.constructType(genericType);

        assertEquals(MessageWrapper.class, jacksonType.getRawClass());
        assertEquals(1, jacksonType.containedTypeCount());
        assertEquals(EmailSettings.class, jacksonType.containedType(0).getRawClass());
    }

    // Serialization with wildcard type should include @JsonTypeInfo discriminator
    @Test
    void serializationPreservesTypeInfo() throws Exception {
        MessageWrapper<EmailSettings> wrapper = new MessageWrapper<>(
                new EmailSettings("me@me.com"), "Sample Message");
        Type genericType = Holder.class.getDeclaredField("wildcardWrapper").getGenericType();
        JavaType jacksonType = MAPPER.constructType(genericType);
        String json = MAPPER.writerFor(jacksonType).writeValueAsString(wrapper);

        assertTrue(json.contains("\"type\":\"EMAIL\""),
                "JSON should contain type discriminator, got: " + json);
        assertTrue(json.contains("\"email\":\"me@me.com\""),
                "JSON should contain email field, got: " + json);
    }

    // Non-regression: Box<T extends Number> with Box<?> should resolve to Number
    @Test
    void wildcardResolvesToNumberBound() throws Exception {
        Type genericType = BoxHolder.class.getDeclaredField("wildcardBox").getGenericType();
        JavaType jacksonType = MAPPER.constructType(genericType);

        assertEquals(Box.class, jacksonType.getRawClass());
        assertEquals(1, jacksonType.containedTypeCount());
        assertEquals(Number.class, jacksonType.containedType(0).getRawClass());
    }

    // Non-regression: Pair<L, R> (no explicit bounds) with Pair<String, ?> keeps Object for R
    @Test
    void unboundedTypeParamStaysObject() throws Exception {
        Type genericType = PairHolder.class.getDeclaredField("wildcardPair").getGenericType();
        JavaType jacksonType = MAPPER.constructType(genericType);

        assertEquals(Pair.class, jacksonType.getRawClass());
        assertEquals(2, jacksonType.containedTypeCount());
        assertEquals(String.class, jacksonType.containedType(0).getRawClass());
        // R has no explicit bound, so ? should stay as Object
        assertEquals(Object.class, jacksonType.containedType(1).getRawClass());
    }
}
