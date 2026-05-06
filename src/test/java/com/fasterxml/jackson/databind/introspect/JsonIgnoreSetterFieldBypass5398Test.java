package com.fasterxml.jackson.databind.introspect;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.*;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests covering the [databind#5398] follow-up: when a property's setter is
 * {@code @JsonIgnore}d (and the getter carries {@code @JsonProperty} that
 * keeps the property non-ignored as a whole), the inferred field mutator
 * retained via {@code MapperFeature.INFER_PROPERTY_MUTATORS} must NOT be
 * used to bypass the ignored setter. The property should remain
 * serialization-only (read-only).
 */
public class JsonIgnoreSetterFieldBypass5398Test extends DatabindTestUtil
{
    static class RenamedGetterIgnoredSetter {
        private String prop;

        @JsonProperty("renamedProp")
        public String getProp() {
            return prop;
        }

        @JsonIgnore
        public void setProp(String prop) {
            this.prop = prop;
        }
    }

    static class StandardGetterIgnoredSetter {
        private String prop;

        @JsonProperty
        public String getProp() {
            return prop;
        }

        @JsonIgnore
        public void setProp(String prop) {
            this.prop = prop;
        }
    }

    // Sibling shape: READ_ONLY access on getter rather than @JsonIgnore on setter.
    static class ReadOnlyRenamedGetter {
        private String prop;

        @JsonProperty(value = "renamedProp", access = JsonProperty.Access.READ_ONLY)
        public String getProp() {
            return prop;
        }

        public void setProp(String prop) {
            this.prop = prop;
        }
    }

    // 2.x defaults FAIL_ON_UNKNOWN_PROPERTIES to true; disable so the "not written"
    // outcome (rather than an exception) is what we assert on read paths.
    private final ObjectMapper MAPPER = jsonMapperBuilder()
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .build();

    @Test
    public void renamedGetterStillSerializes() throws Exception
    {
        RenamedGetterIgnoredSetter bean = new RenamedGetterIgnoredSetter();
        bean.setProp("someValue");
        assertEquals("{\"renamedProp\":\"someValue\"}",
                MAPPER.writeValueAsString(bean));
    }

    @Test
    public void standardGetterStillSerializes() throws Exception
    {
        StandardGetterIgnoredSetter bean = new StandardGetterIgnoredSetter();
        bean.setProp("someValue");
        assertEquals("{\"prop\":\"someValue\"}",
                MAPPER.writeValueAsString(bean));
    }

    // The renamed property must be read-only: @JsonIgnore on the setter
    // blocks the write, and the inferred field mutator must not bypass it.
    @Test
    public void renamedIgnoredSetterDoesNotBypassViaField() throws Exception
    {
        RenamedGetterIgnoredSetter result = MAPPER.readValue(
                "{\"renamedProp\":\"someValue\"}",
                RenamedGetterIgnoredSetter.class);
        assertNotNull(result);
        assertNull(result.getProp());
    }

    @Test
    public void standardIgnoredSetterDoesNotBypassViaField() throws Exception
    {
        StandardGetterIgnoredSetter result = MAPPER.readValue(
                "{\"prop\":\"someValue\"}",
                StandardGetterIgnoredSetter.class);
        assertNotNull(result);
        assertNull(result.getProp());
    }

    // Control: feeding the implicit field name (rather than the renamed
    // public name) also does not write the field.
    @Test
    public void implicitFieldNameDoesNotWriteWhenSetterIgnored() throws Exception
    {
        RenamedGetterIgnoredSetter result = MAPPER.readerFor(RenamedGetterIgnoredSetter.class)
                .without(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .readValue("{\"prop\":\"someValue\"}");
        assertNotNull(result);
        assertNull(result.getProp());
    }

    // Bypass must also stay closed when INFER_PROPERTY_MUTATORS is disabled,
    // i.e. the fix is not just an artifact of inference being on.
    @Test
    public void inferMutatorsDisabledStillBlocksWrite() throws Exception
    {
        ObjectMapper mapper = jsonMapperBuilder()
                .disable(MapperFeature.INFER_PROPERTY_MUTATORS)
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .build();
        RenamedGetterIgnoredSetter result = mapper.readValue(
                "{\"renamedProp\":\"someValue\"}",
                RenamedGetterIgnoredSetter.class);
        assertNotNull(result);
        assertNull(result.getProp());
    }

    // Sibling: @JsonProperty(access = READ_ONLY) on the renamed getter must
    // produce the same outcome — serialize under the new name, not write
    // back via the inferred field mutator.
    @Test
    public void readOnlyRenamedGetterIsSerializeOnly() throws Exception
    {
        ReadOnlyRenamedGetter bean = new ReadOnlyRenamedGetter();
        bean.setProp("someValue");
        assertEquals("{\"renamedProp\":\"someValue\"}",
                MAPPER.writeValueAsString(bean));

        ReadOnlyRenamedGetter result = MAPPER.readValue(
                "{\"renamedProp\":\"someValue\"}",
                ReadOnlyRenamedGetter.class);
        assertNotNull(result);
        assertNull(result.getProp());
    }

}
