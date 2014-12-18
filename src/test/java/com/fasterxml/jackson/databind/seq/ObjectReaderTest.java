package com.fasterxml.jackson.databind.seq;

import com.fasterxml.jackson.core.*;

import com.fasterxml.jackson.databind.*;

public class ObjectReaderTest extends BaseMapTest
{
    final ObjectMapper MAPPER = new ObjectMapper();

    public void testParserFeatures() throws Exception
    {
        final String JSON = "[ /* foo */ 7 ]";
        // default won't accept comments, let's change that:
        ObjectReader reader = MAPPER.reader(int[].class)
                .with(JsonParser.Feature.ALLOW_COMMENTS);

        int[] value = reader.readValue(JSON);
        assertNotNull(value);
        assertEquals(1, value.length);
        assertEquals(7, value[0]);

        // but also can go back
        try {
            reader.without(JsonParser.Feature.ALLOW_COMMENTS).readValue(JSON);
            fail("Should not have passed");
        } catch (JsonProcessingException e) {
            verifyException(e, "foo");
        }
    }
}
