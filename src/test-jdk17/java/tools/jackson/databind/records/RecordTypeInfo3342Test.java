package tools.jackson.databind.records;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import org.junit.jupiter.api.Test;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

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

    // Test from https://github.com/FasterXML/jackson-modules-base/pull/249

    static record RootRecord249(AbstractMember249 member) {
    }

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "@class")
    @JsonSubTypes({
        @JsonSubTypes.Type(value = StringMember.class, name = "string"),
        @JsonSubTypes.Type(value = IntMember.class, name = "int")
    })
    static abstract class AbstractMember249 { }

    static final class StringMember extends AbstractMember249 {
        final String val;

        @JsonCreator
        public StringMember(@JsonProperty("val") String val) {
          this.val = val;
        }
    }

    static final class IntMember extends AbstractMember249 {
        final int val;

        @JsonCreator
        public IntMember(@JsonProperty("val") int val) {
            this.val = val;
        }
    }

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

    // Test from https://github.com/FasterXML/jackson-modules-base/pull/249
    @Test
    public void testDeserializeRecordWithAbstractMember() throws Exception {
        RootRecord249 value = MAPPER.readValue(
                "{\"member\":{\"@class\":\"string\",\"val\":\"Hello, abstract member!\"}}",
                RootRecord249.class);
        assertNotNull(value.member());
        assertEquals(StringMember.class, value.member().getClass());
        assertEquals("Hello, abstract member!", ((StringMember)value.member()).val);
      }
}
