package com.fasterxml.jackson.databind.records;


import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.testutil.DatabindTestUtil;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test for issue where @JsonIgnore on a getter method was causing
 * inconsistent behavior between serialization and deserialization for Records.
 *
 * Before fix (2.18.4+):
 * - Serialization: {"bar":"foo"}
 * - Deserialization: {"bar":null}
 *
 * After fix:
 * - Both should be: {"bar":"foo"}
 */
public class RecordJsonIgnoreRoundTrip5184Test
    extends DatabindTestUtil
{
    // From original issue - record with @JsonProperty on parameter and @JsonIgnore on getter
    record Foo(@JsonProperty("bar") String bar) {
        @JsonIgnore
        public Object getBar() {
            return 123; // Returns different type/value
        }
    }

    private final ObjectMapper MAPPER = newJsonMapper();

    /**
     * Test round-trip serialization/deserialization consistency.
     * The @JsonIgnore on getBar() should not affect deserialization of the "bar" property.
     */
    @Test
    public void testRoundTripConsistency() throws Exception {
        final Foo obj = new Foo("foo");

        // Serialize
        final String json1 = MAPPER.writeValueAsString(obj);
        assertThat(json1).isEqualTo("{\"bar\":\"foo\"}");

        // Deserialize
        final Foo deserialized = MAPPER.readValue(json1, Foo.class);
        assertThat(deserialized.bar()).isEqualTo("foo");

        // Serialize again - should be same as first serialization
        final String json2 = MAPPER.writeValueAsString(deserialized);
        assertThat(json2).isEqualTo("{\"bar\":\"foo\"}");

        // Round-trip should preserve the value
        assertThat(json1).isEqualTo(json2);
    }

    /**
     * Test that deserialization correctly populates the field
     * even though there's a @JsonIgnore on the getter.
     */
    @Test
    public void testDeserializationPopulatesField() throws Exception {
        final String json = "{\"bar\":\"test-value\"}";
        final Foo result = MAPPER.readValue(json, Foo.class);

        assertThat(result.bar()).isEqualTo("test-value");
    }

    /**
     * Test that serialization uses the field value, not the getter.
     */
    @Test
    public void testSerializationUsesFieldNotGetter() throws Exception {
        final Foo obj = new Foo("field-value");
        final String json = MAPPER.writeValueAsString(obj);

        // Should serialize the field value "field-value", not what getBar() returns (123)
        assertThat(json).isEqualTo("{\"bar\":\"field-value\"}");
    }
}