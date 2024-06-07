package tools.jackson.databind.deser.enums;

import org.junit.jupiter.api.Test;

import tools.jackson.databind.*;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class EnumWithNullToString4355Test extends DatabindTestUtil
{
    // [databind#4355]
    enum Enum4355 {
        ALPHA("A"),
        BETA("B"),
        UNDEFINED(null);

        private final String s;

        Enum4355(String s) {
            this.s = s;
        }

        @Override
        public String toString() {
            return s;
        }
    }

    private final ObjectMapper MAPPER = jsonMapperBuilder()
            .disable(SerializationFeature.WRITE_ENUMS_USING_TO_STRING).build();

    // [databind#4355]
    @Test
    public void testWithNullToString() throws Exception
    {
        assertEquals(q("ALPHA"), MAPPER.writeValueAsString(Enum4355.ALPHA));
        assertEquals(q("BETA"), MAPPER.writeValueAsString(Enum4355.BETA));
        assertEquals(q("UNDEFINED"), MAPPER.writeValueAsString(Enum4355.UNDEFINED));
    }
}
