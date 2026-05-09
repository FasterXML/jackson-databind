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
import static org.junit.jupiter.api.Assertions.assertNull;

// [databind#5965]: outer-class @JsonIgnoreProperties / @JsonIncludeProperties
// must still block a name even when the bean has a @JsonUnwrapped field whose
// inner type defines that name (or exposes a @JsonAnySetter that would accept
// any name). Originally a regression from commit 5b9a2fc5 (fix for #1075),
// which moved the unwrapped-property check ahead of the ignore check in
// BeanDeserializer.
public class UnwrappedJsonIgnoreProperties5965Test extends DatabindTestUtil
{
    static class Inner {
        public String admin;
        public String name;
    }

    @JsonIgnoreProperties({"admin"})
    static class OuterWithIgnoreProperties {
        @JsonUnwrapped
        public Inner inner = new Inner();
    }

    // [databind#5965]: outer @JsonIgnoreProperties carries `ignoreUnknown`
    // alongside the explicit `value` list. Both must continue to work when
    // unwrapping is in play.
    @JsonIgnoreProperties(value = {"admin"}, ignoreUnknown = true)
    static class OuterWithIgnoreUnknown {
        @JsonUnwrapped
        public Inner inner = new Inner();
    }

    // The unwrapped wrapper field's *property name* ("inner") must appear in
    // the include set, otherwise filterBeanProps drops the wrapper at
    // construction time and unwrapping never fires (orthogonal pre-existing
    // limitation, not in scope for this fix).
    @JsonIncludeProperties({"inner", "name"})
    static class OuterWithIncludeProperties {
        @JsonUnwrapped
        public Inner inner = new Inner();
    }

    static class InnerWithAnySetter {
        public Map<String, Object> extra = new LinkedHashMap<>();

        @JsonAnySetter
        public void set(String k, Object v) { extra.put(k, v); }
    }

    @JsonIgnoreProperties({"secret"})
    static class OuterAnySetter {
        @JsonUnwrapped
        public InnerWithAnySetter inner = new InnerWithAnySetter();
    }

    private final ObjectMapper MAPPER = newJsonMapper();

    // Outer @JsonIgnoreProperties({"admin"}) must block "admin" even when the
    // unwrapped inner type also defines an "admin" property.
    @Test
    public void testIgnorePropertiesBypassedViaUnwrapped() throws Exception
    {
        String maliciousJson = a2q("{'admin':'INJECTED','name':'alice'}");

        OuterWithIgnoreProperties result = MAPPER.readValue(maliciousJson,
                OuterWithIgnoreProperties.class);

        assertNull(result.inner.admin,
                "@JsonIgnoreProperties({'admin'}) on outer class was bypassed by "
                + "@JsonUnwrapped inner field; 'admin' should be null but was: "
                + result.inner.admin);
        assertEquals("alice", result.inner.name);
    }

    // Any-setter variant: an inner @JsonAnySetter would otherwise accept every
    // outer-ignored name.
    @Test
    public void testIgnorePropertiesAnySetterBypass() throws Exception
    {
        String maliciousJson = a2q("{'secret':'INJECTED'}");

        OuterAnySetter result = MAPPER.readValue(maliciousJson, OuterAnySetter.class);

        assertFalse(result.inner.extra.containsKey("secret"),
                "@JsonIgnoreProperties({'secret'}) bypassed via @JsonUnwrapped + "
                + "@JsonAnySetter; inner.extra=" + result.inner.extra);
    }

    // ignoreUnknown=true on the outer must coexist with the explicit ignore
    // list: "admin" is blocked, "extraneous" is silently dropped (no
    // UnrecognizedPropertyException), "name" still flows to the inner.
    @Test
    public void testIgnorePropertiesIgnoreUnknownInteraction() throws Exception
    {
        String json = a2q("{'admin':'INJECTED','extraneous':'whatever','name':'alice'}");

        OuterWithIgnoreUnknown result = MAPPER.readValue(json, OuterWithIgnoreUnknown.class);

        assertNull(result.inner.admin,
                "@JsonIgnoreProperties value list bypassed; admin=" + result.inner.admin);
        assertEquals("alice", result.inner.name);
    }

    // Outer @JsonIncludeProperties — the dual of @JsonIgnoreProperties — must
    // also forbid a non-included name from sneaking through unwrapping. The
    // include set lists the wrapper field name "inner" (so unwrapping fires)
    // plus the legitimate inner name "name".
    @Test
    public void testIncludePropertiesBypassedViaUnwrapped() throws Exception
    {
        String maliciousJson = a2q("{'admin':'INJECTED','name':'alice'}");

        OuterWithIncludeProperties result = MAPPER.readValue(maliciousJson,
                OuterWithIncludeProperties.class);

        assertNull(result.inner.admin,
                "@JsonIncludeProperties on outer class was bypassed by "
                + "@JsonUnwrapped inner field; 'admin' should be null but was: "
                + result.inner.admin);
        assertEquals("alice", result.inner.name);
    }
}
