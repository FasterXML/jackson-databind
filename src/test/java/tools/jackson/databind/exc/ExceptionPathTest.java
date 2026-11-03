package tools.jackson.databind.exc;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.*;

import tools.jackson.core.exc.StreamReadException;
import tools.jackson.databind.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import static tools.jackson.databind.testutil.DatabindTestUtil.newJsonMapper;

public class ExceptionPathTest
{
    static class Outer {
        public Inner inner = new Inner();
    }

    static class Inner {
        public int x;

        @JsonCreator public static Inner create(@JsonProperty("x") int x) {
            throw new RuntimeException("test-exception");
        }
    }

    // Default-constructor-based beans for path reference test
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

    // @JsonCreator-based beans for path reference test
    static class CreatorFoo {
        private CreatorBar bar;

        @JsonCreator
        public CreatorFoo(@JsonProperty("bar") CreatorBar bar) {
            this.bar = bar;
        }

        public CreatorBar getBar() {
            return bar;
        }
    }

    static class CreatorBar {
        private CreatorBaz baz;

        @JsonCreator
        public CreatorBar(@JsonProperty("baz") CreatorBaz baz) {
            this.baz = baz;
        }

        public CreatorBaz getBaz() {
            return baz;
        }
    }

    static class CreatorBaz {
        private String qux;

        @JsonCreator
        public CreatorBaz(@JsonProperty("qux") String qux) {
            this.qux = qux;
        }

        public String getQux() {
            return qux;
        }
    }

    /*
    /**********************************************************
    /* Test methods
    /**********************************************************
     */

    private final ObjectMapper MAPPER = newJsonMapper();

    @Test
    public void testReferenceChainForInnerClass() throws Exception
    {
        String json = MAPPER.writeValueAsString(new Outer());
        try {
            MAPPER.readValue(json, Outer.class);
            fail("Should not pass");
        } catch (ValueInstantiationException e) {
            String referenceStr = e.getPath().get(0).toString();
            assertEquals(getClass().getName()+"$Outer[\"inner\"]", referenceStr);
        }
    }

    @Test
    public void testPathReferenceWithDefaultDeser() throws Exception {
        String input = "{\"bar\":{\"baz\":{qux:\"quxValue\"))}";
        final String THIS = getClass().getName();

        try {
            MAPPER.readValue(input, Foo.class);
            fail("Upsss! Exception has not been thrown.");
        } catch (StreamReadException ex) {
            assertEquals(THIS+"$Foo[\"bar\"]->"+THIS+"$Bar[\"baz\"]",
                    ex.getPathReference());
        }
    }

    @Test
    public void testPathReferenceWithJsonCreatorDeser() throws Exception {
        String input = "{\"bar\":{\"baz\":{qux:\"quxValue\"))}";
        final String THIS = getClass().getName();

        try {
            MAPPER.readValue(input, CreatorFoo.class);
            fail("Upsss! Exception has not been thrown.");
        } catch (StreamReadException ex) {
            assertEquals(THIS+"$CreatorFoo[\"bar\"]->"+THIS+"$CreatorBar[\"baz\"]",
                    ex.getPathReference());
        }
    }
}
