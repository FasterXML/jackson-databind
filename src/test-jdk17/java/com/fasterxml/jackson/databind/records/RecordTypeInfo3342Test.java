package com.fasterxml.jackson.databind.records;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.testutil.DatabindTestUtil;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

// [databind#3102]
public class RecordTypeInfo3342Test extends DatabindTestUtil
{
    public enum SpiceLevel {
        LOW,
        HIGH
    }

    public interface SpiceTolerance {
    }

    public record LowSpiceTolerance(String food) implements SpiceTolerance {
    }

    public record HighSpiceTolerance(String food) implements SpiceTolerance {
    }

    public record Example(
            SpiceLevel level,
            @JsonTypeInfo(
                    use = JsonTypeInfo.Id.NAME,
                    include = JsonTypeInfo.As.EXTERNAL_PROPERTY,
                    property = "level")
            @JsonSubTypes({
                    @JsonSubTypes.Type(value = LowSpiceTolerance.class, name = "LOW"),
                    @JsonSubTypes.Type(value = HighSpiceTolerance.class, name = "HIGH")
            })
            SpiceTolerance tolerance) { }

    private final ObjectMapper MAPPER = newJsonMapper();

    @Test
    public void testSerializeDeserializeJsonSubType_LOW() throws Exception {
        Example record = new Example(SpiceLevel.LOW, new LowSpiceTolerance("Tomato"));

        String json = MAPPER.writeValueAsString(record);
        assertEquals("{\"level\":\"LOW\",\"tolerance\":{\"food\":\"Tomato\"}}", json);

        Example value = MAPPER.readValue(json, Example.class);
        assertEquals(record, value);
    }

    @Test
    public void testSerializeDeserializeJsonSubType_HIGH() throws Exception {
        Example record = new Example(SpiceLevel.HIGH, new HighSpiceTolerance("Chilli"));

        String json = MAPPER.writeValueAsString(record);
        assertEquals("{\"level\":\"HIGH\",\"tolerance\":{\"food\":\"Chilli\"}}", json);

        Example value = MAPPER.readValue(json, Example.class);
        assertEquals(record, value);
    }
}
