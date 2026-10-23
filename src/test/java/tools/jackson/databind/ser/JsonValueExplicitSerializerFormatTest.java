package tools.jackson.databind.ser;

import java.util.Date;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonValue;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.annotation.JsonSerialize;
import tools.jackson.databind.ser.jdk.JavaUtilDateSerializer;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.assertEquals;

class JsonValueExplicitSerializerFormatTest extends DatabindTestUtil
{
    static class FieldValue {
        @JsonValue
        @JsonSerialize(using = JavaUtilDateSerializer.class)
        @JsonFormat(pattern = "yyyy-MM", timezone = "UTC")
        public Date value = new Date(0L);
    }

    static class MethodValue {
        @JsonValue
        @JsonSerialize(using = JavaUtilDateSerializer.class)
        @JsonFormat(pattern = "yyyy-MM", timezone = "UTC")
        public Date value() {
            return new Date(0L);
        }
    }

    static class Wrapper {
        public FieldValue value = new FieldValue();
    }

    static class FormatOverride {
        @JsonFormat(pattern = "yyyy", timezone = "UTC")
        public FieldValue value = new FieldValue();
    }

    private final ObjectMapper MAPPER = newJsonMapper();

    @Test
    void fieldFormatReachesExplicitSerializer() {
        assertEquals("\"1970-01\"", MAPPER.writeValueAsString(new FieldValue()));
    }

    @Test
    void methodFormatReachesExplicitSerializer() {
        assertEquals("\"1970-01\"", MAPPER.writeValueAsString(new MethodValue()));
    }

    @Test
    void nestedValueRetainsAccessorFormat() {
        assertEquals("{\"value\":\"1970-01\"}", MAPPER.writeValueAsString(new Wrapper()));
    }

    @Test
    void enclosingFormatTakesPrecedence() {
        assertEquals("{\"value\":\"1970\"}", MAPPER.writeValueAsString(new FormatOverride()));
    }
}
