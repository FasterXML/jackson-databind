package tools.jackson.databind.struct;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.*;

import tools.jackson.core.JsonGenerator;

import tools.jackson.databind.*;
import tools.jackson.databind.annotation.JsonDeserialize;
import tools.jackson.databind.annotation.JsonPOJOBuilder;
import tools.jackson.databind.annotation.JsonSerialize;
import tools.jackson.databind.node.ObjectNode;
import tools.jackson.databind.ser.std.StdSerializer;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Coverage for combining {@code @JsonUnwrapped(prefix/suffix)} with
 * {@code @JsonAnyGetter} / {@code @JsonAnySetter} on the unwrapped bean:
 * the name transformation must be applied to any-getter keys on write,
 * and reversed for any-setter keys on read.
 *<p>
 * Known gaps are covered by
 * {@code tools.jackson.databind.tofix.UnwrappedAnyGetterPrefixTest}.
 */
public class UnwrappedWithAnyGetterPrefixTest extends DatabindTestUtil
{
    static class AnyBean {
        protected Map<String, Object> extra = new LinkedHashMap<>();

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

    @JsonPropertyOrder({ "name" })
    static class SuffixOuter {
        public String name = "aaa";

        @JsonUnwrapped(suffix = "-z")
        public AnyBean inner = new AnyBean();
    }

    @JsonPropertyOrder({ "name" })
    static class PrefixAndSuffixOuter {
        public String name = "aaa";

        @JsonUnwrapped(prefix = "a-", suffix = "-z")
        public AnyBean inner = new AnyBean();
    }

    @JsonPropertyOrder({ "name" })
    static class NoPrefixOuter {
        public String name = "aaa";

        @JsonUnwrapped
        public AnyBean inner = new AnyBean();
    }

    private final ObjectMapper MAPPER = newJsonMapper();

    /*
    /**********************************************************************
    /* Test methods: prefix/suffix round-tripping
    /**********************************************************************
     */

    @Test
    public void prefixRoundTrip() throws Exception
    {
        PrefixOuter input = new PrefixOuter();
        input.inner.setExtra("age", 64);

        String json = MAPPER.writeValueAsString(input);
        assertEquals("""
                {"name":"aaa","a-age":64}""", json);

        PrefixOuter result = MAPPER.readValue(json, PrefixOuter.class);
        assertEquals("aaa", result.name);
        assertEquals(Map.of("age", 64), result.inner.extra);
    }

    @Test
    public void suffixRoundTrip() throws Exception
    {
        SuffixOuter input = new SuffixOuter();
        input.inner.setExtra("age", 64);

        String json = MAPPER.writeValueAsString(input);
        assertEquals("""
                {"name":"aaa","age-z":64}""", json);

        SuffixOuter result = MAPPER.readValue(json, SuffixOuter.class);
        assertEquals(Map.of("age", 64), result.inner.extra);
    }

    @Test
    public void prefixAndSuffixRoundTrip() throws Exception
    {
        PrefixAndSuffixOuter input = new PrefixAndSuffixOuter();
        input.inner.setExtra("age", 64);

        String json = MAPPER.writeValueAsString(input);
        assertEquals("""
                {"name":"aaa","a-age-z":64}""", json);

        PrefixAndSuffixOuter result = MAPPER.readValue(json, PrefixAndSuffixOuter.class);
        assertEquals(Map.of("age", 64), result.inner.extra);
    }

    // Plain `@JsonUnwrapped` (NOP transformer) must be left exactly as before
    @Test
    public void noPrefixIsUnchanged() throws Exception
    {
        NoPrefixOuter input = new NoPrefixOuter();
        input.inner.setExtra("age", 64);

        String json = MAPPER.writeValueAsString(input);
        assertEquals("""
                {"name":"aaa","age":64}""", json);

        NoPrefixOuter result = MAPPER.readValue(json, NoPrefixOuter.class);
        assertEquals(Map.of("age", 64), result.inner.extra);
    }

    // Key that already starts with the prefix: transform is applied blindly on write,
    // and (symmetrically) reversed exactly once on read
    @Test
    public void keyAlreadyContainingPrefixRoundTrips() throws Exception
    {
        PrefixOuter input = new PrefixOuter();
        input.inner.setExtra("a-age", 64);

        String json = MAPPER.writeValueAsString(input);
        assertEquals("""
                {"name":"aaa","a-a-age":64}""", json);

        PrefixOuter result = MAPPER.readValue(json, PrefixOuter.class);
        assertEquals(Map.of("a-age", 64), result.inner.extra);
    }

    @Test
    public void emptyAnyGetterMapWritesNothing() throws Exception
    {
        assertEquals("""
                {"name":"aaa"}""", MAPPER.writeValueAsString(new PrefixOuter()));
    }

    /*
    /**********************************************************************
    /* Test methods: transform must not leak into nested values
    /**********************************************************************
     */

    // The prefix applies to the any-getter's own keys only; keys of nested Objects
    // (here: a `Map`-valued entry, and one inside an Array) must be left alone
    @Test
    public void mapValuedEntryDoesNotPrefixNestedKeys() throws Exception
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
    @Test
    public void pojoValuedEntryDoesNotPrefixNestedProperties() throws Exception
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
    @Test
    public void polymorphicValueTypeIdIsNotPrefixed() throws Exception
    {
        PolyValueOuter input = new PolyValueOuter();
        input.inner.extra.put("pet", new Dog());

        assertEquals("""
                {"name":"aaa","a-pet":{"@type":"dog","name":"rex"}}""",
                MAPPER.writeValueAsString(input));
    }

    /*
    /**********************************************************************
    /* Test methods: alternate any-getter/any-setter shapes
    /**********************************************************************
     */

    @JsonPropertyOrder({ "name" })
    static class FieldAnyOuter {
        public String name = "aaa";

        @JsonUnwrapped(prefix = "a-")
        public FieldAnyBean inner = new FieldAnyBean();
    }

    static class FieldAnyBean {
        @JsonAnyGetter
        @JsonAnySetter
        public Map<String, Object> extra = new LinkedHashMap<>();
    }

    @Test
    public void anyGetterAndSetterOnField() throws Exception
    {
        FieldAnyOuter input = new FieldAnyOuter();
        input.inner.extra.put("age", 64);

        String json = MAPPER.writeValueAsString(input);
        assertEquals("""
                {"name":"aaa","a-age":64}""", json);

        FieldAnyOuter result = MAPPER.readValue(json, FieldAnyOuter.class);
        assertEquals(Map.of("age", 64), result.inner.extra);
    }

    @JsonPropertyOrder({ "name" })
    static class NodeAnyOuter {
        public String name = "aaa";

        @JsonUnwrapped(prefix = "a-")
        public NodeAnyBean inner = new NodeAnyBean();
    }

    static class NodeAnyBean {
        public ObjectNode node;

        @JsonAnyGetter
        public ObjectNode getNode() { return node; }
    }

    // [databind#3604] `ObjectNode`-valued any-getter: top-level keys get the prefix,
    // but keys _inside_ the values must not
    @Test
    public void objectNodeAnyGetterWithPrefix() throws Exception
    {
        NodeAnyOuter input = new NodeAnyOuter();
        input.inner.node = MAPPER.createObjectNode();
        input.inner.node.put("age", 64);
        input.inner.node.putObject("sub").put("x", 1);

        assertEquals("""
                {"name":"aaa","a-age":64,"a-sub":{"x":1}}""",
                MAPPER.writeValueAsString(input));
    }

    enum Color { RED, BLUE }

    @JsonPropertyOrder({ "name" })
    static class EnumKeyOuter {
        public String name = "aaa";

        @JsonUnwrapped(prefix = "a-")
        public EnumKeyBean inner = new EnumKeyBean();
    }

    static class EnumKeyBean {
        public Map<Color, Object> extra = new LinkedHashMap<>();

        @JsonAnyGetter
        public Map<Color, Object> getExtra() { return extra; }
    }

    @Test
    public void enumKeyedAnyGetterWithPrefix() throws Exception
    {
        EnumKeyOuter input = new EnumKeyOuter();
        input.inner.extra.put(Color.RED, 1);

        assertEquals("""
                {"name":"aaa","a-RED":1}""", MAPPER.writeValueAsString(input));
    }

    static class CustomAnyMapSerializer extends StdSerializer<Map<String, Object>>
    {
        public CustomAnyMapSerializer() { super(Map.class, false); }

        @Override
        public void serialize(Map<String, Object> value, JsonGenerator g, SerializationContext ctxt) {
            for (Map.Entry<String, Object> entry : value.entrySet()) {
                g.writeName(entry.getKey());
                g.writeString(String.valueOf(entry.getValue()));
            }
        }
    }

    @JsonPropertyOrder({ "name" })
    static class CustomSerOuter {
        public String name = "aaa";

        @JsonUnwrapped(prefix = "a-")
        public CustomSerBean inner = new CustomSerBean();
    }

    static class CustomSerBean {
        public Map<String, Object> extra = new LinkedHashMap<>();

        @JsonAnyGetter
        @JsonSerialize(using = CustomAnyMapSerializer.class)
        public Map<String, Object> getExtra() { return extra; }
    }

    // Any-getter with an explicit (non-`MapSerializer`) serializer must still get keys transformed
    @Test
    public void customAnyGetterSerializerWithPrefix() throws Exception
    {
        CustomSerOuter input = new CustomSerOuter();
        input.inner.extra.put("age", 64);

        assertEquals("""
                {"name":"aaa","a-age":"64"}""", MAPPER.writeValueAsString(input));
    }

    /*
    /**********************************************************************
    /* Test methods: interaction with other databind features
    /**********************************************************************
     */

    static class CreatorAnyBean {
        public final String id;
        public Map<String, Object> extra = new LinkedHashMap<>();

        @JsonCreator
        public CreatorAnyBean(@JsonProperty("id") String id) { this.id = id; }

        @JsonAnyGetter
        public Map<String, Object> getExtra() { return extra; }

        @JsonAnySetter
        public void setExtra(String key, Object value) { extra.put(key, value); }
    }

    static class CreatorOuter {
        public String name;

        @JsonUnwrapped(prefix = "a-")
        public CreatorAnyBean inner;
    }

    // Property-based creator on the unwrapped bean: any-setter keys go through
    // a different code path (`_deserializeUsingPropertyBased`)
    @Test
    public void creatorBackedInnerWithAnySetter() throws Exception
    {
        CreatorOuter result = MAPPER.readValue("""
                {"name":"x","a-id":"i1","a-age":64}""", CreatorOuter.class);
        assertEquals("i1", result.inner.id);
        assertEquals(Map.of("age", 64), result.inner.extra);
    }

    @JsonIdentityInfo(generator = ObjectIdGenerators.IntSequenceGenerator.class, property = "@id")
    static class ObjectIdOuter {
        public String name;

        @JsonUnwrapped(prefix = "a-")
        public AnyBean inner;
    }

    @Test
    public void objectIdOuterWithPrefixedAnySetter() throws Exception
    {
        ObjectIdOuter result = MAPPER.readValue("""
                {"@id":1,"name":"n","a-age":64}""", ObjectIdOuter.class);
        assertEquals(Map.of("age", 64), result.inner.extra);
    }

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
    @JsonSubTypes({ @JsonSubTypes.Type(value = PolySubOuter.class, name = "sub") })
    @JsonPropertyOrder({ "name", "v" })
    static abstract class PolyBaseOuter {
        public String name;

        @JsonUnwrapped(prefix = "a-")
        public AnyBean inner;
    }

    static class PolySubOuter extends PolyBaseOuter {
        public int v;
    }

    @Test
    public void polymorphicOuterWithPrefixedAny() throws Exception
    {
        PolyBaseOuter result = MAPPER.readValue("""
                {"type":"sub","name":"n","v":3,"a-age":64}""", PolyBaseOuter.class);
        assertEquals(Map.of("age", 64), result.inner.extra);

        assertEquals("""
                {"type":"sub","name":"n","v":3,"a-age":64}""",
                MAPPER.writeValueAsString(result));
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

    // Nested `@JsonUnwrapped` prefixes chain on the write side
    // (read side is still broken; see `tofix` counterpart)
    @Test
    public void nestedPrefixesChainOnSerialization() throws Exception
    {
        NestedOuter input = new NestedOuter();
        input.mid.inner.setExtra("age", 64);

        assertEquals("""
                {"name":"aaa","a-mid":"m","a-b-age":64}""",
                MAPPER.writeValueAsString(input));
    }

    /*
    /**********************************************************************
    /* Test methods: handler caching
    /**********************************************************************
     */

    @JsonPropertyOrder({ "name" })
    static class OtherPrefixOuter {
        public String name = "aaa";

        @JsonUnwrapped(prefix = "b-")
        public AnyBean inner = new AnyBean();
    }

    // Same unwrapped type used with different prefixes (and standalone) must not
    // share/poison cached (de)serializers
    @Test
    public void sameInnerTypeWithDifferentPrefixes() throws Exception
    {
        PrefixOuter a = new PrefixOuter();
        a.inner.setExtra("age", 64);
        OtherPrefixOuter b = new OtherPrefixOuter();
        b.inner.setExtra("age", 64);
        NoPrefixOuter plain = new NoPrefixOuter();
        plain.inner.setExtra("age", 64);

        assertEquals("""
                {"name":"aaa","a-age":64}""", MAPPER.writeValueAsString(a));
        assertEquals("""
                {"name":"aaa","b-age":64}""", MAPPER.writeValueAsString(b));
        assertEquals("""
                {"name":"aaa","age":64}""", MAPPER.writeValueAsString(plain));
        // and again, to make sure the first result was not cached with the wrong transformer
        assertEquals("""
                {"name":"aaa","a-age":64}""", MAPPER.writeValueAsString(a));

        assertEquals(Map.of("age", 64),
                MAPPER.readValue("""
                        {"name":"aaa","a-age":64}""", PrefixOuter.class).inner.extra);
        assertEquals(Map.of("age", 64),
                MAPPER.readValue("""
                        {"name":"aaa","b-age":64}""", OtherPrefixOuter.class).inner.extra);
        assertEquals(Map.of("age", 64),
                MAPPER.readValue("""
                        {"name":"aaa","age":64}""", NoPrefixOuter.class).inner.extra);
        // Standalone (non-unwrapped) use of the same type must be untouched
        assertEquals(Map.of("a-age", 64),
                MAPPER.readValue("""
                        {"a-age":64}""", AnyBean.class).extra);
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

    // Two unwrapped beans, each with its own any-getter and prefix
    @Test
    public void twoPrefixedUnwrappedBeansSerialization() throws Exception
    {
        TwoUnwrappedOuter input = new TwoUnwrappedOuter();
        input.a.ea.put("p", 1);
        input.b.eb.put("q", 2);

        assertEquals("""
                {"name":"x","a-p":1,"b-q":2}""", MAPPER.writeValueAsString(input));
    }

    // ... and each any-setter must collect only the names carrying its own prefix:
    // every unwrapped bean is offered all unknown properties, so without reversing
    // the transformation each would also pick up its siblings' properties
    @Test
    public void twoPrefixedUnwrappedBeansRoundTrip() throws Exception
    {
        TwoUnwrappedOuter result = MAPPER.readValue("""
                {"name":"x","a-p":1,"b-q":2}""", TwoUnwrappedOuter.class);
        assertEquals(Map.of("p", 1), result.a.ea);
        assertEquals(Map.of("q", 2), result.b.eb);
    }

    // A name matching no prefix belongs to none of the unwrapped beans
    @Test
    public void nonMatchingNameDoesNotReachPrefixedAnySetter() throws Exception
    {
        PrefixOuter result = MAPPER.readValue("""
                {"name":"aaa","a-age":64,"zz":3}""", PrefixOuter.class);
        assertEquals(Map.of("age", 64), result.inner.extra);
    }

    /*
    /**********************************************************************
    /* Test methods: numeric map keys
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
    // which needs transforming just like `String`/`Enum` keys do
    @Test
    public void intKeyedAnyGetterAppliesPrefix() throws Exception
    {
        IntKeyOuter input = new IntKeyOuter();
        input.inner.extra.put(3, "x");

        assertEquals("""
                {"name":"aaa","a-3":"x"}""", MAPPER.writeValueAsString(input));
    }

    /*
    /**********************************************************************
    /* Test methods: builder-based unwrapped bean
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

    // Builder-based unwrapped bean: `BuilderBasedDeserializer` has to retain the
    // `NameTransformer` too, so that the prefix is stripped off any-setter keys
    @Test
    public void builderBasedInnerStripsPrefix() throws Exception
    {
        BuilderOuter result = MAPPER.readValue("""
                {"name":"x","a-id":"i1","a-age":64}""", BuilderOuter.class);
        assertEquals("i1", result.inner.id);
        assertEquals(Map.of("age", 64), result.inner.extra);
    }

    static class OuterWithOwnAnySetter {
        public String name;

        @JsonUnwrapped(prefix = "a-")
        public AnyBean inner;

        public Map<String, Object> outerExtra = new LinkedHashMap<>();

        @JsonAnyGetter
        public Map<String, Object> getOuterExtra() { return outerExtra; }

        @JsonAnySetter
        public void setOuterExtra(String key, Object value) { outerExtra.put(key, value); }
    }

    // A property matching no unwrapped bean's prefix falls back to the *outer* bean's
    // any-setter. An unwrapped bean with an any-setter otherwise claims every unknown
    // property (`UnwrappedPropertyHandler`), which would starve the outer one.
    @Test
    public void nonMatchingNameFallsBackToOuterAnySetter() throws Exception
    {
        OuterWithOwnAnySetter result = MAPPER.readValue("""
                {"name":"n","a-age":64,"zz":3}""", OuterWithOwnAnySetter.class);
        assertEquals(Map.of("age", 64), result.inner.extra);
        assertEquals(Map.of("zz", 3), result.outerExtra);
    }
}
