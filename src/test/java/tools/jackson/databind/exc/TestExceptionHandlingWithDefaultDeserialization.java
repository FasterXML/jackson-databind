package tools.jackson.databind.exc;

import org.junit.jupiter.api.Test;

import tools.jackson.core.exc.StreamReadException;
import tools.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import static tools.jackson.databind.testutil.DatabindTestUtil.newJsonMapper;

public class TestExceptionHandlingWithDefaultDeserialization
{
    static class Foo {
        private Bar bar;

        public Foo() { }

        public Bar getBar() {
            return bar;
        }
    }

    static class Bar {
        private Baz baz;

        public Bar() { }

        public Baz getBaz() {
            return baz;
        }
    }

    static class Baz {
        private String qux;

        public Baz() { }

        public String getQux() {
            return qux;
        }
    }

    @Test
    public void testShouldThrowExceptionWithPathReference() throws Exception {
        // given
        ObjectMapper mapper = newJsonMapper();
        String input = "{\"bar\":{\"baz\":{qux:\"quxValue\"))}";
        final String THIS = getClass().getName();

        // when
        try {
            mapper.readValue(input, Foo.class);
            fail("Upsss! Exception has not been thrown.");
        } catch (StreamReadException ex) {
            // then
            assertEquals(THIS+"$Foo[\"bar\"]->"+THIS+"$Bar[\"baz\"]",
                    ex.getPathReference());
        }
    }
}
