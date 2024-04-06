package com.fasterxml.jackson.databind.records;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.testutil.DatabindTestUtil;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

public class RecordFailingSetter3938Test extends DatabindTestUtil
{
    private final static String ERROR_3938_PREFIX = "Non-null 'options' not allowed for ";

    // [databind#3938]
    interface NoOptionsCommand {
        @JsonProperty("options")
        default void setOptions(JsonNode value) {
          if (value.isNull()) {
             return;
          }
          throw new IllegalArgumentException(ERROR_3938_PREFIX+getClass().getName());
        }
    }

    public record Command3938(int id, String filter) implements NoOptionsCommand { }

    private final ObjectMapper MAPPER = newJsonMapper();

    // [databind#3938]: Should detect and use setters too
    @Test
    public void testFailingSetter3939() throws Exception
    {
        final ObjectReader R = MAPPER.readerFor(Command3938.class);

        // First, missing value and `null` are fine, as long as we have all fields
        assertNotNull(R.readValue(a2q("{'id':1, 'filter':'abc'}")));
        assertNotNull(R.readValue(a2q("{'id':2, 'filter':'abc', 'options':null}")));

        // But then failure for non-empty Array (f.ex)
        try {
            R.readValue(a2q("{'id':2,'options':[123]}}"));
            fail("Should not pass");
        } catch (DatabindException e) {
            verifyException(e, ERROR_3938_PREFIX);
        }
    }
}
