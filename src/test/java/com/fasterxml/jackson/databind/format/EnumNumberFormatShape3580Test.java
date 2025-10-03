package com.fasterxml.jackson.databind.format;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.testutil.DatabindTestUtil;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

// [databind#3580] Enum (de)serialization in conjunction with JsonFormat.Shape.NUMBER_INT
public class EnumNumberFormatShape3580Test
    extends DatabindTestUtil
{
    public static class Pojo3580 {
        public PojoState3580 state;
        public Pojo3580() {}
        public Pojo3580(PojoState3580 state) {this.state = state;}
    }

    @JsonFormat(shape = JsonFormat.Shape.NUMBER_INT)
    public enum PojoState3580 {
        OFF(17),
        ON(31),
        UNKNOWN(99);

        private int value;

        PojoState3580(int value) { this.value = value; }

        @JsonValue
        public int value() {return this.value;}
    }

    @Test
    public void testEnumNumberFormatShape3580()
        throws Exception
    {
        ObjectMapper mapper = JsonMapper.builder().build();

        // Serialize
        assertEquals("{\"state\":17}", mapper.writeValueAsString(new Pojo3580(PojoState3580.OFF))); //
        assertEquals("{\"state\":31}", mapper.writeValueAsString(new Pojo3580(PojoState3580.ON))); //
        assertEquals("{\"state\":99}", mapper.writeValueAsString(new Pojo3580(PojoState3580.UNKNOWN))); //

        // Pass Deserialize
        assertEquals(PojoState3580.OFF, mapper.readValue("{\"state\":17}", Pojo3580.class).state); // Pojo[state=OFF]
        assertEquals(PojoState3580.ON, mapper.readValue("{\"state\":31}", Pojo3580.class).state); // Pojo[state=OFF]
        assertEquals(PojoState3580.UNKNOWN, mapper.readValue("{\"state\":99}", Pojo3580.class).state); // Pojo[state=OFF]

        // Fail : Try to use ordinal number
        assertThrows(InvalidFormatException.class, () -> mapper.readValue("{\"state\":0}", Pojo3580.class));
    }
}
