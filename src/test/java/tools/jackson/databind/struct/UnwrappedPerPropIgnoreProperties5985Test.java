package tools.jackson.databind.struct;

import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonIncludeProperties;
import com.fasterxml.jackson.annotation.JsonUnwrapped;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

// [databind#5985]: Per-property `@JsonIgnoreProperties` /
// `@JsonIncludeProperties` placed directly on a `@JsonUnwrapped` field should
// filter the unwrapped inner POJO's properties — same as on a regular
// (non-unwrapped) POJO-valued property.
//
// Investigation showed this already works in 3.x without dedicated code:
// the inner deserializer is contextualized via the property's accessor in
// BeanDeserializerBase.createContextual(), which routes through
// _handleByNameInclusion() and produces a filtered inner deserializer; the
// unwrapping clone (BeanDeserializerBase ctor near line 337) preserves
// `_ignorableProps` / `_includableProps`. These tests pin that behavior so
// future refactors don't silently regress it.
public class UnwrappedPerPropIgnoreProperties5985Test extends DatabindTestUtil
{
    static class Inner {
        public String admin;
        public String name;
    }

    static class InnerWithAnySetter {
        public Map<String, Object> extra = new LinkedHashMap<>();
        @JsonAnySetter
        public void set(String k, Object v) { extra.put(k, v); }
    }

    static class OuterIgnore {
        @JsonUnwrapped
        @JsonIgnoreProperties({"admin"})
        public Inner inner = new Inner();
    }

    static class OuterInclude {
        @JsonUnwrapped
        @JsonIncludeProperties({"name"})
        public Inner inner = new Inner();
    }

    static class OuterIgnoreAnySetter {
        @JsonUnwrapped
        @JsonIgnoreProperties({"secret"})
        public InnerWithAnySetter inner = new InnerWithAnySetter();
    }

    private final ObjectMapper MAPPER = newJsonMapper();

    // (1) Per-property @JsonIgnoreProperties on @JsonUnwrapped — deserialization.
    @Test
    public void ignorePropertiesPerPropOnUnwrapped_deser() throws Exception
    {
        OuterIgnore r = MAPPER.readValue(
                a2q("{'admin':'INJECTED','name':'alice'}"), OuterIgnore.class);
        assertEquals("alice", r.inner.name);
        assertNull(r.inner.admin,
                "@JsonIgnoreProperties({'admin'}) on @JsonUnwrapped field"
                + " did not filter inner; admin=" + r.inner.admin);
    }

    // (2) Per-property @JsonIncludeProperties on @JsonUnwrapped — deserialization.
    @Test
    public void includePropertiesPerPropOnUnwrapped_deser() throws Exception
    {
        OuterInclude r = MAPPER.readValue(
                a2q("{'admin':'INJECTED','name':'alice'}"), OuterInclude.class);
        assertEquals("alice", r.inner.name);
        assertNull(r.inner.admin,
                "@JsonIncludeProperties({'name'}) on @JsonUnwrapped field"
                + " did not restrict inner; admin=" + r.inner.admin);
    }

    // (3) Per-property @JsonIgnoreProperties must also block the inner
    //     @JsonAnySetter fallback for a forbidden name — deserialization.
    @Test
    public void ignorePerPropBlocksInnerAnySetter_deser() throws Exception
    {
        OuterIgnoreAnySetter r = MAPPER.readValue(
                a2q("{'secret':'X','ok':'Y'}"), OuterIgnoreAnySetter.class);
        assertNotNull(r.inner.extra);
        assertFalse(r.inner.extra.containsKey("secret"),
                "@JsonIgnoreProperties({'secret'}) bypassed via inner @JsonAnySetter; extra="
                + r.inner.extra);
        assertEquals("Y", r.inner.extra.get("ok"));
    }

    // (4) Per-property @JsonIgnoreProperties on @JsonUnwrapped — serialization.
    @Test
    public void ignorePropertiesPerPropOnUnwrapped_ser() throws Exception
    {
        OuterIgnore o = new OuterIgnore();
        o.inner.admin = "secret";
        o.inner.name = "alice";
        String json = MAPPER.writeValueAsString(o);
        assertEquals(a2q("{'name':'alice'}"), json,
                "@JsonIgnoreProperties({'admin'}) on @JsonUnwrapped field"
                + " did not drop 'admin' from output: " + json);
    }

    // (5) Per-property @JsonIncludeProperties on @JsonUnwrapped — serialization.
    @Test
    public void includePropertiesPerPropOnUnwrapped_ser() throws Exception
    {
        OuterInclude o = new OuterInclude();
        o.inner.admin = "secret";
        o.inner.name = "alice";
        String json = MAPPER.writeValueAsString(o);
        assertEquals(a2q("{'name':'alice'}"), json,
                "@JsonIncludeProperties({'name'}) on @JsonUnwrapped field"
                + " did not restrict output: " + json);
    }
}
