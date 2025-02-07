package com.fasterxml.jackson.databind.deser.creators;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.*;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.testutil.DatabindTestUtil;
import com.fasterxml.jackson.databind.testutil.FiveMinuteUser;
import com.fasterxml.jackson.databind.util.ByteBufferBackedInputStream;

import static org.junit.jupiter.api.Assertions.*;

// [databind#4938] Allow JsonCreator factory method to return `null`
public class JsonCreatorReturningNull4938Test
    extends DatabindTestUtil
{
    static class Localized3 {
        public final String en;
        public final String de;
        public final String fr;

        @JsonCreator
        public static Localized3 of(@JsonProperty("en") String en,
            @JsonProperty("de") String de, @JsonProperty("fr") String fr) {
            if (en == null && de == null && fr == null) {
                return null; // Explicitly return null when all arguments are null
            }
            return new Localized3(en, de, fr);
        }

        // This is how users would normally create instances, I think...?
        private Localized3(String en, String de, String fr) {
            this.en = en;
            this.de = de;
            this.fr = fr;
        }
    }

    static class Localized4 {
        public final String en;
        public final String de;
        public final String fr;

        @JsonCreator
        public static Localized4 of(@JsonProperty("en") String en,
                                    @JsonProperty("de") String de, @JsonProperty("fr") String fr) {
            if (en == null && de == null && fr == null) {
                return null; // Explicitly return null when all arguments are null
            }
            throw new IllegalStateException("Should not be called");
        }

        // This is how users would normally create instances, I think...?
        private Localized4(String en, String de, String fr) {
            this.en = en;
            this.de = de;
            this.fr = fr;
        }
    }

    private final ObjectMapper MAPPER = newJsonMapper();

    @Test
    void testDeserializeToNullWhenAllPropertiesAreNull()
            throws Exception
    {
        Localized3 result = MAPPER.readValue(
                "{ \"en\": null, \"de\": null, \"fr\": null }",
                Localized3.class);

        assertNull(result);
    }

    @Test
    void testDeserializeToNonNullWhenAnyPropertyIsNonNull()
            throws Exception
    {
        Localized3 result = MAPPER.readValue(
                "{ \"en\": \"Hello\", \"de\": null, \"fr\": null }",
                Localized3.class);

        assertNotNull(result);
        assertEquals("Hello", result.en);
    }

    @Test
    void testDeserializeToNonNullWhenAnyPropertyIsNonNullWithUnknown()
            throws Exception
    {
        // Should all fail...
        ObjectReader enabled = MAPPER.readerFor(Localized4.class)
                .with(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        // ...with unknown properties in front
        _testDeserialize(enabled, "{ \"en\": null, \"de\": null, \"fr\": null, \"unknown\": null, \"unknown2\": \"hello\" }");
        // ...with unknown properties in back
        _testDeserialize(enabled, "{ \"unknown\": null, \"unknown2\": \"hello\", \"en\": null, \"de\": null, \"fr\": null }");
        // ...with unknown properties mixed
        _testDeserialize(enabled, "{ \"unknown\": null, \"en\": null, \"unknown2\": \"hello\", \"de\": null, \"fr\": null }");

        // Should all pass...
        ObjectReader disabled = MAPPER.readerFor(Localized4.class)
                .without(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        // ...with unknown properties in front
        _testDeserialize(disabled, "{ \"en\": null, \"de\": null, \"fr\": null, \"unknown\": null, \"unknown2\": \"hello\" }");
        // ...with unknown properties in back
        _testDeserialize(disabled, "{ \"unknown\": null, \"unknown2\": \"hello\", \"en\": null, \"de\": null, \"fr\": null }");
        // ...with unknown properties mixed
        _testDeserialize(disabled, "{ \"unknown\": null, \"en\": null, \"unknown2\": \"hello\", \"de\": null, \"fr\": null }");
    }

    @Test
    void testDeserializeToNullWithStream()
            throws Exception
    {
        // Should all fail...
        TreeMap<String, String> map = new TreeMap<>();

        map.put("aa", "aa");
        map.put("cc", "cc");
        map.put("de", null);
        map.put("en", null);
        map.put("fr", null);
        map.put("za", "zz");
        map.put("zb", "zz");
        map.put("zc", "zz");

        byte[] bytes = MAPPER.writeValueAsBytes(map);
        try (InputStream in = new ByteArrayInputStream(bytes)) {
            Localized3 result = MAPPER.readerFor(Localized3.class)
                    .without(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                    .readValue(in);
            // Check if read all the bytes
            assertEquals(-1, in.read());
        }
    }

    private void _testDeserialize(ObjectReader reader, String JSON)
        throws Exception
    {
        Localized4 bean = reader.readValue(JSON);
        assertNull(bean);
    }

}
