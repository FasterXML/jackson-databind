package tools.jackson.databind.misc;

import org.junit.jupiter.api.Test;

import tools.jackson.core.*;
import tools.jackson.databind.*;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit test mostly written to cover issue with unintended blocking
 * after data binding.
 */
public class TestBlocking
    extends DatabindTestUtil
{
    /**
     * This is an indirect test that should trigger problems if (and only if)
     * underlying parser is advanced beyond the only element array.
     * Basically, although content is invalid, this should be encountered
     * quite yet.
     */
    @Test
    public void testEagerAdvance() throws Exception
    {
        ObjectMapper mapper = jsonMapperBuilder()
                .disable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
                .build();
        JsonParser p = createParserUsingReader("[ 1  ");
        assertToken(JsonToken.START_ARRAY, p.nextToken());
        assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());

        // And then try to map just a single entry: shouldn't fail:
        Integer I = mapper.readValue(p, Integer.class);
        assertEquals(Integer.valueOf(1), I);

        // and should fail only now:
        try {
            p.nextToken();
            fail("Should not pass");
        } catch (JacksonException ioe) {
            verifyException(ioe, "Unexpected end-of-input: expected close marker for ARRAY");
        }
        p.close();
    }
}
