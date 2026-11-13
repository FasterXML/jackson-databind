package tools.jackson.databind.deser.creators;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.*;

import tools.jackson.databind.InjectableValues;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

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

    static class CtorObjects {
        final Object[] values;

        @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
        CtorObjects(Object... values) { this.values = values; }
    }

    // Creators invoked through `call(Object[])` (properties-based, or delegating with an
    // injected parameter) already worked
    static class PropsStrings {
        final String[] values;

        @JsonCreator
        PropsStrings(@JsonProperty("values") String... values) { this.values = values; }
    }

    static class PropsCtorTrailingStrings {
        final String name;
        final String[] tags;

        @JsonCreator
        PropsCtorTrailingStrings(@JsonProperty("name") String name,
                @JsonProperty("tags") String... tags) {
            this.name = name;
            this.tags = tags;
        }
    }

    static class PropsFactoryTrailingInts {
        final int n;
        final int[] values;

        private PropsFactoryTrailingInts(int n, int[] values) {
            this.n = n;
            this.values = values;
        }

        @JsonCreator
        static PropsFactoryTrailingInts of(@JsonProperty("n") int n,
                @JsonProperty("values") int... values) {
            return new PropsFactoryTrailingInts(n, values);
        }
    }

    static class InjectedDelegatingStrings {
        final String prefix;
        final String[] values;

        @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
        InjectedDelegatingStrings(@JacksonInject("prefix") String prefix, String... values) {
            this.prefix = prefix;
            this.values = values;
        }
    }

    private final ObjectMapper MAPPER = newJsonMapper();

    @Test
    void varargsDelegatingConstructor() throws Exception
    {
        assertArrayEquals(new String[] { "a", "b" },
                MAPPER.readValue("""
                        ["a","b"]
                        """, CtorStrings.class).values);
        assertArrayEquals(new String[0],
                MAPPER.readValue("[]", CtorStrings.class).values);
    }

    @Test
    void varargsDelegatingFactory() throws Exception
    {
        assertArrayEquals(new String[] { "a", "b" },
                MAPPER.readValue("""
                        ["a","b"]
                        """, FactoryStrings.class).values);
    }

    @Test
    void primitiveVarargsDelegatingConstructor() throws Exception
    {
        assertArrayEquals(new int[] { 1, 2 },
                MAPPER.readValue("[1,2]", CtorInts.class).values);
    }

    // Wrapped the delegate instead of failing, yielding `[[a, 1]]`
    @Test
    void objectVarargsDelegatingConstructor() throws Exception
    {
        CtorObjects v = MAPPER.readValue("""
                ["a",1]
                """, CtorObjects.class);
        assertEquals("[a, 1]", Arrays.deepToString(v.values));
    }

    @Test
    void varargsPropertiesConstructor() throws Exception
    {
        assertArrayEquals(new String[] { "a" },
                MAPPER.readValue("""
                        {"values":["a"]}
                        """, PropsStrings.class).values);
    }

    @Test
    void varargsAfterOtherPropertiesConstructor() throws Exception
    {
        PropsCtorTrailingStrings v = MAPPER.readValue("""
                {"name":"x","tags":["a","b"]}
                """, PropsCtorTrailingStrings.class);
        assertEquals("x", v.name);
        assertArrayEquals(new String[] { "a", "b" }, v.tags);
    }

    @Test
    void primitiveVarargsAfterOtherPropertiesFactory() throws Exception
    {
        PropsFactoryTrailingInts v = MAPPER.readValue("""
                {"n":1,"values":[2,3]}
                """, PropsFactoryTrailingInts.class);
        assertEquals(1, v.n);
        assertArrayEquals(new int[] { 2, 3 }, v.values);
    }

    @Test
    void varargsDelegateAfterInjectedParameter() throws Exception
    {
        ObjectMapper mapper = jsonMapperBuilder()
                .injectableValues(new InjectableValues.Std().addValue("prefix", "p"))
                .build();
        InjectedDelegatingStrings v = mapper.readValue("""
                ["a","b"]
                """, InjectedDelegatingStrings.class);
        assertEquals("p", v.prefix);
        assertArrayEquals(new String[] { "a", "b" }, v.values);
    }
}
