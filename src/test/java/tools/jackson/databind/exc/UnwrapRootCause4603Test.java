package tools.jackson.databind.exc;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import tools.jackson.core.JacksonException;
import tools.jackson.core.TokenStreamLocation;
import tools.jackson.databind.*;
import tools.jackson.databind.testutil.DatabindTestUtil;

import com.fasterxml.jackson.annotation.*;

import static org.junit.jupiter.api.Assertions.*;

// [databind#4603]: Stop unwrapping root cause
public class UnwrapRootCause4603Test
    extends DatabindTestUtil
{
    @SuppressWarnings("serial")
    static class CustomException extends RuntimeException {
        public CustomException(String message) {
            super(message);
        }
    }

    static class Feature1347DeserBean
    {
        int value;

        public void setValue(int x) {
            throw new CustomException("setValue, fail on purpose");
        }
    }

    static class AnySetterBean
    {
        protected Map<String, Integer> props = new HashMap<String, Integer>();

        @JsonAnySetter
        public void prop(String name, Integer value) {
            throw new CustomException("@JsonAnySetter, fail on purpose");
        }
    }

    static class Baz {
        private String qux;

        @JsonCreator
        public Baz(@JsonProperty("qux") String qux) {
            this.qux = qux;
        }

        public String getQux() {
            return qux;
        }
    }

    private final ObjectMapper MAPPER = newJsonMapper();

    // Whether disabled or enabled, should get ArithmeticException
    @Test
    public void testExceptionWrappingConfiguration()
        throws Exception
    {
        JacksonException result = _tryDeserializeWith(MAPPER);
        assertInstanceOf(DatabindException.class, result);
        // We are throwing exception inside a setter, but InvocationTargetException
        // will still be unwrapped
        assertInstanceOf(CustomException.class, result.getCause());
        TokenStreamLocation loc = result.getLocation();
        assertNotSame(TokenStreamLocation.NA, loc);
        // happens to point to location after `3`
        assertEquals(1, loc.getLineNr());
        assertEquals(11, loc.getColumnNr());
    }

    private JacksonException _tryDeserializeWith(ObjectMapper mapper) {
        return assertThrows(JacksonException.class,
                () -> mapper.readValue(a2q("{'value':3}"), Feature1347DeserBean.class)
        );
    }

    @Test
    public void testWithAnySetter()
            throws Exception
    {
        DatabindException result = assertThrows(DatabindException.class,
                () -> MAPPER.readValue(a2q("{'a':72}"), AnySetterBean.class));
        assertInstanceOf(CustomException.class, result.getCause());
        TokenStreamLocation loc = result.getLocation();
        assertNotSame(TokenStreamLocation.NA, loc);
        // happens to point to location after `72`
        assertEquals(1, loc.getLineNr());
        assertEquals(8, loc.getColumnNr());
    }
}
