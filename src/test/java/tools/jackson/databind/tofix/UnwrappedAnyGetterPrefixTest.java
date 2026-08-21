package tools.jackson.databind.tofix;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.*;

import tools.jackson.databind.*;
import tools.jackson.databind.annotation.JsonDeserialize;
import tools.jackson.databind.annotation.JsonPOJOBuilder;
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

    @JsonPropertyOrder({ "name" })
    static class PrefixOuter {
        public String name = "aaa";

        @JsonUnwrapped(prefix = "a-")
        public AnyBean inner = new AnyBean();
    }

    private final ObjectMapper MAPPER = newJsonMapper();

    /*
    /**********************************************************************
    /* Serialization: prefix leaks into nested values
    /**********************************************************************
     */

    // The prefix must only be applied to the any-getter's own keys; keys of nested
    // Objects (here: a `Map`-valued entry, and one inside an Array) must be left alone
    @JacksonTestFailureExpected
    @Test
    public void mapValuedEntryMustNotPrefixNestedKeys() throws Exception
    {
        Map<String, Object> nested = new LinkedHashMap<>();
        nested.put("x", 1);
        nested.put("y", 2);

        PrefixOuter input = new PrefixOuter();
        input.inner.setExtra("obj", nested);
        input.inner.setExtra("arr", List.of(nested));

        assertEquals("""
                {"name":"aaa","a-obj":{"x":1,"y":2},"a-arr":[{"x":1,"y":2}]}""",
                MAPPER.writeValueAsString(input));
    }

    static class Point {
        public int x = 1;
        public int y = 2;
    }

    // Same as above, but with a POJO-valued entry
    @JacksonTestFailureExpected
    @Test
    public void pojoValuedEntryMustNotPrefixNestedProperties() throws Exception
    {
        PrefixOuter input = new PrefixOuter();
        input.inner.setExtra("pt", new Point());

        assertEquals("""
                {"name":"aaa","a-pt":{"x":1,"y":2}}""",
                MAPPER.writeValueAsString(input));
    }

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "@type")
    @JsonSubTypes({ @JsonSubTypes.Type(value = Dog.class, name = "dog") })
    static abstract class Animal {
        public String name = "rex";
    }

    static class Dog extends Animal { }

    @JsonPropertyOrder({ "name" })
    static class PolyValueOuter {
        public String name = "aaa";

        @JsonUnwrapped(prefix = "a-")
        public PolyValueBean inner = new PolyValueBean();
    }

    static class PolyValueBean {
        public Map<String, Animal> extra = new LinkedHashMap<>();

        @JsonAnyGetter
        public Map<String, Animal> getExtra() { return extra; }
    }

    // Type ids written for values are property names too, and must not be prefixed
    @JacksonTestFailureExpected
    @Test
    public void polymorphicValueTypeIdMustNotBePrefixed() throws Exception
    {
        PolyValueOuter input = new PolyValueOuter();
        input.inner.extra.put("pet", new Dog());

        assertEquals("""
                {"name":"aaa","a-pet":{"@type":"dog","name":"rex"}}""",
                MAPPER.writeValueAsString(input));
    }

    /*
    /**********************************************************************
    /* Serialization: prefix skipped for numeric keys
    /**********************************************************************
     */

    @JsonPropertyOrder({ "name" })
    static class IntKeyOuter {
        public String name = "aaa";

        @JsonUnwrapped(prefix = "a-")
        public IntKeyBean inner = new IntKeyBean();
    }

    static class IntKeyBean {
        public Map<Integer, Object> extra = new LinkedHashMap<>();

        @JsonAnyGetter
        public Map<Integer, Object> getExtra() { return extra; }
    }

    // `Integer`/`Long` keys are written via `JsonGenerator.writePropertyId(long)`,
    // which bypasses the name transformation (`String` and `Enum` keys work)
    @JacksonTestFailureExpected
    @Test
    public void intKeyedAnyGetterMustApplyPrefix() throws Exception
    {
        IntKeyOuter input = new IntKeyOuter();
        input.inner.extra.put(3, "x");

        assertEquals("""
                {"name":"aaa","a-3":"x"}""", MAPPER.writeValueAsString(input));
    }

    /*
    /**********************************************************************
    /* Deserialization gaps
    /**********************************************************************
     */

    @JsonDeserialize(builder = BuilderAnyBean.Builder.class)
    static class BuilderAnyBean {
        public final String id;
        public final Map<String, Object> extra;

        BuilderAnyBean(String id, Map<String, Object> extra) {
            this.id = id;
            this.extra = extra;
        }

        @JsonPOJOBuilder(withPrefix = "")
        static class Builder {
            String id;
            Map<String, Object> extra = new LinkedHashMap<>();

            public Builder id(String id) {
                this.id = id;
                return this;
            }

            @JsonAnySetter
            public void any(String key, Object value) { extra.put(key, value); }

            public BuilderAnyBean build() { return new BuilderAnyBean(id, extra); }
        }
    }

    static class BuilderOuter {
        public String name;

        @JsonUnwrapped(prefix = "a-")
        public BuilderAnyBean inner;
    }

    // `BuilderBasedDeserializer.unwrappingDeserializer()` does not retain the
    // `NameTransformer`, so the prefix is not stripped off any-setter keys
    @JacksonTestFailureExpected
    @Test
    public void builderBasedInnerMustStripPrefix() throws Exception
    {
        BuilderOuter result = MAPPER.readValue("""
                {"name":"x","a-id":"i1","a-age":64}""", BuilderOuter.class);
        assertEquals("i1", result.inner.id);
        assertEquals(Map.of("age", 64), result.inner.extra);
    }

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
