package tools.jackson.databind.records;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonValue;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.exc.MismatchedInputException;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.testutil.DatabindTestUtil;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

// [databind#3580] Enum (de)serialization in conjunction with JsonFormat.Shape.NUMBER_INT
public class EnumNumberFormatShapeRecord3580Test
        extends DatabindTestUtil
{
    public record RecordNumber3580(@JsonFormat(shape = JsonFormat.Shape.NUMBER) RecordState3580 state) {}

    public record RecordNumberInt3580(@JsonFormat(shape = JsonFormat.Shape.NUMBER_INT) RecordState3580 state) {}

    public enum RecordState3580 {
        OFF(17),
        ON(31),
        UNKNOWN(99);

        private int value;

        RecordState3580(int value) { this.value = value; }

        @JsonValue
        public int value() {return this.value;}
    }

    @Test
    public void testEnumNumberFormatShapeRecord3580()
            throws Exception
    {
        ObjectMapper mapper = JsonMapper.builder().build();

        // Serialize
        assertEquals("{\"state\":17}", mapper.writeValueAsString(new RecordNumber3580(RecordState3580.OFF))); //
        assertEquals("{\"state\":31}", mapper.writeValueAsString(new RecordNumber3580(RecordState3580.ON))); //
        assertEquals("{\"state\":99}", mapper.writeValueAsString(new RecordNumber3580(RecordState3580.UNKNOWN))); //

        // Pass Deserialize
        assertEquals(RecordState3580.OFF, mapper.readValue("{\"state\":17}", RecordNumber3580.class).state); // Pojo[state=OFF]
        assertEquals(RecordState3580.ON, mapper.readValue("{\"state\":31}", RecordNumber3580.class).state); // Pojo[state=OFF]
        assertEquals(RecordState3580.UNKNOWN, mapper.readValue("{\"state\":99}", RecordNumber3580.class).state); // Pojo[state=OFF]

        // Fail : Try to use ordinal number
        assertThrows(MismatchedInputException.class, () -> mapper.readValue("{\"state\":0}", RecordNumber3580.class));
    }

    @Test
    public void testEnumNumberIntFormatShapeRecord3580()
            throws Exception
    {
        ObjectMapper mapper = JsonMapper.builder().build();

        // Serialize
        assertEquals("{\"state\":17}", mapper.writeValueAsString(new RecordNumberInt3580(RecordState3580.OFF))); //
        assertEquals("{\"state\":31}", mapper.writeValueAsString(new RecordNumberInt3580(RecordState3580.ON))); //
        assertEquals("{\"state\":99}", mapper.writeValueAsString(new RecordNumberInt3580(RecordState3580.UNKNOWN))); //

        // Pass Deserialize
        assertEquals(RecordState3580.OFF, mapper.readValue("{\"state\":17}", RecordNumberInt3580.class).state); // Pojo[state=OFF]
        assertEquals(RecordState3580.ON, mapper.readValue("{\"state\":31}", RecordNumberInt3580.class).state); // Pojo[state=OFF]
        assertEquals(RecordState3580.UNKNOWN, mapper.readValue("{\"state\":99}", RecordNumberInt3580.class).state); // Pojo[state=OFF]

        // Fail : Try to use ordinal number
        assertThrows(MismatchedInputException.class, () -> mapper.readValue("{\"state\":0}", RecordNumberInt3580.class));
    }
}