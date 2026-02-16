package tools.jackson.databind.records;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonValue;

import tools.jackson.core.*;

import tools.jackson.databind.*;
import tools.jackson.databind.annotation.JsonDeserialize;
import tools.jackson.databind.deser.std.StdDeserializer;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class RecordViaParser5683Test extends DatabindTestUtil
{
    @JsonDeserialize(using = Inner.Deser.class)
    public record Inner(@JsonValue long value) {
        static class Deser extends StdDeserializer<Inner> {
            protected Deser() { super(Inner.class); }

            @Override
            public Inner deserialize(JsonParser p,  DeserializationContext ctxt) {
                return new Inner(p.readValueAs(Long.class));
            }
        }
    }

    public record Outer(Inner inner) { }

    /*
    /**********************************************************************
    /* Test methods
    /**********************************************************************
     */

    private final ObjectMapper MAPPER = newJsonMapper();
    
    @Test
    public void testIssue5683()
    {
        final String json = "{\"inner\":\"123\"}";
        final JsonNode tree = MAPPER.readTree(json);

        Outer value;

        value = MAPPER.readValue(json, Outer.class);
        assertEquals(123L, value.inner.value());

        value = MAPPER.treeToValue(tree, Outer.class);
        assertEquals(123L, value.inner.value());

        value = MAPPER.reader().treeToValue(tree, Outer.class);
        assertEquals(123L, value.inner.value());
    }
}
