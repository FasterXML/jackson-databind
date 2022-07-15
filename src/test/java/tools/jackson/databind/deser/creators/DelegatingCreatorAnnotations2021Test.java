package tools.jackson.databind.deser.creators;

import com.fasterxml.jackson.annotation.JsonCreator;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.BaseMapTest;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.annotation.JsonDeserialize;
import tools.jackson.databind.deser.std.StdDeserializer;

// Tests for problems uncovered with [databind#2016]; related to
// `@JsonDeserialize` modifications to type, deserializer(s)
public class DelegatingCreatorAnnotations2021Test extends BaseMapTest
{
    // [databind#2021]
    static class DelegatingWithCustomDeser2021 {
        public final static Double DEFAULT = 0.25;

        Number value;

        @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
        public DelegatingWithCustomDeser2021(@JsonDeserialize(using = ValueDeser2021.class) Number v) {
            value = v;
        }
    }

    static class ValueDeser2021 extends StdDeserializer<Number> {
        public ValueDeser2021() { super(Number.class); }

        @Override
        public Number deserialize(JsonParser p, DeserializationContext ctxt)
            throws JacksonException
        {
            p.skipChildren();
            return DelegatingWithCustomDeser2021.DEFAULT;
        }
    }

    /*
    /**********************************************************************
    /* Test methods
    /**********************************************************************
     */

    private final ObjectMapper MAPPER = newJsonMapper();

    // [databind#2021]
    public void testCustomDeserForDelegating() throws Exception
    {
        DelegatingWithCustomDeser2021 actual = MAPPER.readValue(" true ", DelegatingWithCustomDeser2021.class);
        assertEquals(DelegatingWithCustomDeser2021.DEFAULT, actual.value);
    }
}
