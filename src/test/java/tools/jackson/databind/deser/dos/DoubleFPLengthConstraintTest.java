package tools.jackson.databind.deser.dos;

import org.junit.jupiter.api.Test;

import tools.jackson.core.StreamReadConstraints;
import tools.jackson.core.exc.StreamConstraintsException;
import tools.jackson.core.json.JsonFactory;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.fail;

import static tools.jackson.databind.testutil.DatabindTestUtil.verifyException;

// Number-length constraint (StreamReadConstraints.maxNumberLength) must be enforced
// when coercing a String to `double` the same way it already is for `float`.
public class DoubleFPLengthConstraintTest
{
    // Deliberately NOT the default (1000), so that these tests actually depend on
    // the configured constraint being applied
    private final static int MAX_NUMBER_LEN = 100;

    private final static int OVER_LONG_LEN = MAX_NUMBER_LEN + 50;

    private final static String OVER_LONG_NUMBER = "9".repeat(OVER_LONG_LEN);

    private ObjectMapper mapperWithNumberLen(int maxLen) {
        JsonFactory f = JsonFactory.builder()
                .streamReadConstraints(StreamReadConstraints.builder().maxNumberLength(maxLen).build())
                .build();
        return JsonMapper.builder(f).build();
    }

    private String jsonArrayWith(String value) {
        return """
                ["%s"]
                """.formatted(value);
    }

    @Test
    public void doubleArrayFromStringRespectsNumberLength() throws Exception
    {
        ObjectMapper mapper = mapperWithNumberLen(MAX_NUMBER_LEN);
        try {
            mapper.readValue(jsonArrayWith(OVER_LONG_NUMBER), double[].class);
            fail("Should not pass: number length exceeds configured maximum");
        } catch (StreamConstraintsException e) {
            verifyException(e, "Number value length ("+OVER_LONG_LEN+")");
            verifyException(e, "exceeds the maximum allowed ("+MAX_NUMBER_LEN);
        }
    }

    // Parity check: `float[]` already enforces the same limit; both should behave alike.
    @Test
    public void floatArrayFromStringRespectsNumberLength() throws Exception
    {
        ObjectMapper mapper = mapperWithNumberLen(MAX_NUMBER_LEN);
        try {
            mapper.readValue(jsonArrayWith(OVER_LONG_NUMBER), float[].class);
            fail("Should not pass: number length exceeds configured maximum");
        } catch (StreamConstraintsException e) {
            verifyException(e, "Number value length ("+OVER_LONG_LEN+")");
            verifyException(e, "exceeds the maximum allowed ("+MAX_NUMBER_LEN);
        }
    }

    // And verify the input itself is otherwise acceptable: it is only the lowered
    // limit above that rejects it, not the value being unparseable
    @Test
    public void overLongNumberAcceptedWithDefaultConstraints() throws Exception
    {
        ObjectMapper mapper = JsonMapper.builder().build();
        assertArrayEquals(new double[] { Double.parseDouble(OVER_LONG_NUMBER) },
                mapper.readValue(jsonArrayWith(OVER_LONG_NUMBER), double[].class));
    }
}
