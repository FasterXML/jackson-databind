package tools.jackson.databind.deser.creators;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.*;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

// [databind#6426]: a varargs creator unreflects to a variable-arity `MethodHandle`,
// whose `asType()` collected the (already array-valued) delegate into another array.
class DelegatingVarargsCreator6426Test extends DatabindTestUtil
{
    static class CtorStrings {
        final String[] values;

        @JsonCreator
        CtorStrings(String... values) { this.values = values; }
    }

    static class FactoryStrings {
        final String[] values;

        private FactoryStrings(String[] values) { this.values = values; }

        @JsonCreator
        static FactoryStrings of(String... values) { return new FactoryStrings(values); }
    }

    static class CtorInts {
        final int[] values;

        @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
        CtorInts(int... values) { this.values = values; }
    }

    // Properties-based creators already worked; guards against regressing them
    static class PropsStrings {
        final String[] values;

        @JsonCreator
        PropsStrings(@JsonProperty("values") String... values) { this.values = values; }
    }

    private final ObjectMapper MAPPER = newJsonMapper();

    @Test
    void varargsDelegatingConstructor() throws Exception
    {
        assertArrayEquals(new String[] { "a", "b" },
                MAPPER.readValue(a2q("['a','b']"), CtorStrings.class).values);
        assertArrayEquals(new String[0],
                MAPPER.readValue("[]", CtorStrings.class).values);
    }

    @Test
    void varargsDelegatingFactory() throws Exception
    {
        assertArrayEquals(new String[] { "a", "b" },
                MAPPER.readValue(a2q("['a','b']"), FactoryStrings.class).values);
    }

    @Test
    void primitiveVarargsDelegatingConstructor() throws Exception
    {
        assertArrayEquals(new int[] { 1, 2 },
                MAPPER.readValue("[1,2]", CtorInts.class).values);
    }

    @Test
    void varargsPropertiesConstructor() throws Exception
    {
        assertArrayEquals(new String[] { "a" },
                MAPPER.readValue(a2q("{'values':['a']}"), PropsStrings.class).values);
    }
}
