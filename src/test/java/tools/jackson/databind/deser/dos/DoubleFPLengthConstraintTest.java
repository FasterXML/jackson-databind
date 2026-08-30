package tools.jackson.databind.deser.dos;

import org.junit.jupiter.api.Test;

import tools.jackson.core.StreamReadConstraints;
import tools.jackson.core.exc.StreamConstraintsException;
import tools.jackson.core.json.JsonFactory;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

// Number-length constraint (StreamReadConstraints.maxNumberLength) must be enforced
// when coercing a String to `double` the same way it already is for `float`.
public class DoubleFPLengthConstraintTest
{
    private final static int MAX_NUMBER_LEN = 1000;

    private final static String OVER_LONG_NUMBER;
    static {
        StringBuilder sb = new StringBuilder(MAX_NUMBER_LEN + 100);
        for (int i = 0; i < MAX_NUMBER_LEN + 100; ++i) {
            sb.append('9');
        }
        OVER_LONG_NUMBER = sb.toString();
    }

    private ObjectMapper mapperWithNumberLen(int maxLen) {
        JsonFactory f = JsonFactory.builder()
                .streamReadConstraints(StreamReadConstraints.builder().maxNumberLength(maxLen).build())
                .build();
        return JsonMapper.builder(f).build();
    }

    @Test
    public void doubleArrayFromStringRespectsNumberLength() throws Exception
    {
        ObjectMapper mapper = mapperWithNumberLen(MAX_NUMBER_LEN);
        String json = "[\"" + OVER_LONG_NUMBER + "\"]";
        try {
            mapper.readValue(json, double[].class);
            fail("Should not pass: number length exceeds configured maximum");
        } catch (StreamConstraintsException e) {
            String msg = e.getMessage();
            assertNotNull(msg);
        }
    }

    // Parity check: `float[]` already enforces the same limit; both should behave alike.
    @Test
    public void floatArrayFromStringRespectsNumberLength() throws Exception
    {
        ObjectMapper mapper = mapperWithNumberLen(MAX_NUMBER_LEN);
        String json = "[\"" + OVER_LONG_NUMBER + "\"]";
        try {
            mapper.readValue(json, float[].class);
            fail("Should not pass: number length exceeds configured maximum");
        } catch (StreamConstraintsException e) {
            String msg = e.getMessage();
            assertNotNull(msg);
        }
    }
}
