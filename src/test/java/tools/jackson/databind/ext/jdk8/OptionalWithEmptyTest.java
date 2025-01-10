package tools.jackson.databind.ext.jdk8;

import java.util.Optional;

import org.junit.jupiter.api.Test;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.*;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.*;

public class OptionalWithEmptyTest
    extends DatabindTestUtil
{
    private final ObjectMapper MAPPER = newJsonMapper();

    static class BooleanBean {
        public Optional<Boolean> value;

        public BooleanBean() { }
        public BooleanBean(Boolean b) {
            value = Optional.ofNullable(b);
        }
    }

    @Test
    public void testOptionalFromEmpty() throws Exception {
        Optional<?> value = MAPPER.readValue(q(""), new TypeReference<Optional<Integer>>() {});
        assertEquals(false, value.isPresent());
    }

    // for [datatype-jdk8#23]
    @Test
    public void testBooleanWithEmpty() throws Exception
    {
        // and looks like a special, somewhat non-conforming case is what a user had
        // issues with
        BooleanBean b = MAPPER.readValue(a2q("{'value':''}"), BooleanBean.class);
        assertNotNull(b.value);

        assertEquals(false, b.value.isPresent());
    }

}
