package tools.jackson.databind.ext.javatime.deser;

import java.time.Instant;

import org.junit.jupiter.api.Test;

import tools.jackson.databind.ObjectReader;
import tools.jackson.databind.ext.javatime.DateTimeTestBase;

import static org.junit.jupiter.api.Assertions.assertEquals;

// [modules-java8#359] InstantDeserializer deserializes the nanosecond portion of
//   fractional timestamps incorrectly: -1.000000001 deserializes to 1969-12-31T23:59:59.000000001Z
//   instead of 1969-12-31T23:59:58.999999999Z
public class InstantDeserializerNegative359Test
    extends DateTimeTestBase
{
    private final ObjectReader READER = newMapper().readerFor(Instant.class);

    @Test
    public void testDeserializationAsFloat04()
        throws Exception
    {
        Instant actual = READER.readValue("-1.000000001");
        Instant expected = Instant.ofEpochSecond(-1L, -1L);
        assertEquals(expected, actual);
    }

    @Test
    public void testDeserializationAsFloat05()
        throws Exception
    {
        Instant actual = READER.readValue("-0.000000001");
        Instant expected = Instant.ofEpochSecond(0L, -1L);
        assertEquals(expected, actual);
    }
}
