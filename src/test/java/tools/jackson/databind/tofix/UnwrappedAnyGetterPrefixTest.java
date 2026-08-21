package tools.jackson.databind.tofix;

import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.*;

import tools.jackson.databind.*;
import tools.jackson.databind.testutil.DatabindTestUtil;
import tools.jackson.databind.testutil.failure.JacksonTestFailureExpected;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Known gaps in combining {@code @JsonUnwrapped(prefix/suffix)} with
 * {@code @JsonAnyGetter} / {@code @JsonAnySetter}; working cases are covered by
 * {@code tools.jackson.databind.struct.UnwrappedWithAnyGetterPrefixTest}.
 */
public class UnwrappedAnyGetterPrefixTest extends DatabindTestUtil
{
    static class AnyBean {
        public Map<String, Object> extra = new LinkedHashMap<>();

        @JsonAnyGetter
        public Map<String, Object> getExtra() { return extra; }

        @JsonAnySetter
        public void setExtra(String key, Object value) { extra.put(key, value); }
    }

    private final ObjectMapper MAPPER = newJsonMapper();

    static class AnyA {
        public Map<String, Object> ea = new LinkedHashMap<>();

        @JsonAnyGetter
        public Map<String, Object> getEa() { return ea; }

        @JsonAnySetter
        public void setEa(String key, Object value) { ea.put(key, value); }
    }

    static class AnyB {
        public Map<String, Object> eb = new LinkedHashMap<>();

        @JsonAnyGetter
        public Map<String, Object> getEb() { return eb; }

        @JsonAnySetter
        public void setEb(String key, Object value) { eb.put(key, value); }
    }

    @JsonPropertyOrder({ "name" })
    static class TwoUnwrappedOuter {
        public String name = "x";

        @JsonUnwrapped(prefix = "a-")
        public AnyA a = new AnyA();

        @JsonUnwrapped(prefix = "b-")
        public AnyB b = new AnyB();
    }

    // Names that do not match a bean's prefix are still handed to its any-setter
    // (unchanged), so with 2 prefixed unwrapped beans each one collects the other's
    // properties. Serialization of the same value is correct, so this does not round-trip.
    @JacksonTestFailureExpected
    @Test
    public void nonMatchingNamesMustNotReachPrefixedAnySetter() throws Exception
    {
        TwoUnwrappedOuter result = MAPPER.readValue("""
                {"name":"x","a-p":1,"b-q":2}""", TwoUnwrappedOuter.class);
        assertEquals(Map.of("p", 1), result.a.ea);
        assertEquals(Map.of("q", 2), result.b.eb);
    }

    @JsonPropertyOrder({ "mid" })
    static class NestedMid {
        public String mid = "m";

        @JsonUnwrapped(prefix = "b-")
        public AnyBean inner = new AnyBean();
    }

    @JsonPropertyOrder({ "name" })
    static class NestedOuter {
        public String name = "aaa";

        @JsonUnwrapped(prefix = "a-")
        public NestedMid mid = new NestedMid();
    }

    // Nested `@JsonUnwrapped` prefixes chain correctly on write ("a-b-age"), but on
    // read the entry never reaches the innermost any-setter: it is dropped silently
    @JacksonTestFailureExpected
    @Test
    public void nestedPrefixesMustChainOnDeserialization() throws Exception
    {
        NestedOuter result = MAPPER.readValue("""
                {"name":"aaa","a-mid":"m","a-b-age":64}""", NestedOuter.class);
        assertEquals("m", result.mid.mid);
        assertEquals(Map.of("age", 64), result.mid.inner.extra);
    }
}
