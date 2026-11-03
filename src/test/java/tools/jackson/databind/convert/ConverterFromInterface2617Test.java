package tools.jackson.databind.convert;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonProperty;

import tools.jackson.databind.annotation.JsonDeserialize;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.testutil.DatabindTestUtil;
import tools.jackson.databind.util.StdConverter;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Test for https://github.com/FasterXML/jackson-databind/issues/2617
 *
 * When deserializing a class using @JsonDeserialize(converter = ...),
 * if the converter's FROM type is an interface using @JsonDeserialize(as = ...),
 * deserialization fails. Jackson is failing to use the "as" setting when used
 * as a FROM class in a converter.
 */
public class ConverterFromInterface2617Test extends DatabindTestUtil
{
    @JsonDeserialize(converter = FromConverter.class)
    static class Concrete {
        private String field;

        public String getField() {
            return field;
        }

        public void setField(String field) {
            this.field = field;
        }
    }

    @JsonDeserialize(as = FromImpl.class)
    interface From {
        String field();
    }

    static class FromImpl implements From {
        @JsonProperty
        private String field;

        @Override
        public String field() {
            return field;
        }
    }

    static class FromConverter extends StdConverter<From, Concrete> {
        @Override
        public Concrete convert(From value) {
            Concrete test = new Concrete();
            test.setField(value.field());
            return test;
        }
    }

    private final ObjectMapper MAPPER = newJsonMapper();

    @Test
    public void testConverterFromInterface() throws Exception
    {
        Concrete value = MAPPER.readValue("{\"field\": \"foo\"}", Concrete.class);
        assertEquals("foo", value.getField());
    }
}
