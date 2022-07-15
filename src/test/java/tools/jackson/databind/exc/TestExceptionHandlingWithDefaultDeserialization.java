package tools.jackson.databind.exc;

import tools.jackson.databind.BaseMapTest;
import tools.jackson.databind.DatabindException;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;

public class TestExceptionHandlingWithDefaultDeserialization
    extends BaseMapTest
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

    public void testShouldThrowExceptionWithPathReference() throws IOException {
        // given
        ObjectMapper mapper = newJsonMapper();
        String input = "{\"bar\":{\"baz\":{qux:\"quxValue\"))}";
        final String THIS = getClass().getName();

        // when
        try {
            mapper.readValue(input, Foo.class);
            fail("Upsss! Exception has not been thrown.");
        } catch (DatabindException ex) {
            // then
            assertEquals(THIS+"$Foo[\"bar\"]->"+THIS+"$Bar[\"baz\"]",
                    ex.getPathReference());
        }
    }
}
