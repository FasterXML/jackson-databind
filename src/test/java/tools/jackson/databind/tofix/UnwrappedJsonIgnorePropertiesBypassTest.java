package tools.jackson.databind.tofix;

import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonUnwrapped;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.testutil.DatabindTestUtil;
import tools.jackson.databind.testutil.failure.JacksonTestFailureExpected;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Reproduction for an outer-class {@code @JsonIgnoreProperties} bypass that
 * appears when the outer bean has a {@link JsonUnwrapped} field whose inner
 * type contains the filtered property name.
 * <p>
 * Regression introduced by commit {@code 5b9a2fc5} ("Fix #1075:
 * {@code @JsonUnwrapped} fields, {@code @JsonIgnore}"), which moved the
 * {@code _unwrappedPropertyHandler.hasUnwrappedProperty()} check ahead of the
 * {@code IgnorePropertiesUtil.shouldIgnore()} check in
 * {@code BeanDeserializer}: an incoming property whose name is on the outer
 * ignore list is now routed to the inner unwrapped deserializer before the
 * ignore filter has a chance to drop it.
 * <p>
 * The any-setter variant is the same shape but with
 * {@code hasUnwrappedProperty()} returning true for every name, so any outer
 * {@code @JsonIgnoreProperties} entry can be smuggled in as long as the inner
 * unwrapped type exposes a {@link JsonAnySetter}.
 */
public class UnwrappedJsonIgnorePropertiesBypassTest extends DatabindTestUtil
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
    @JacksonTestFailureExpected
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
    }

    // Any-setter variant: hasUnwrappedProperty() returns true for every name
    // when the inner type has a @JsonAnySetter, so every outer
    // @JsonIgnoreProperties entry is exploitable.
    @JacksonTestFailureExpected
    @Test
    public void testIgnorePropertiesAnySetterBypass() throws Exception
    {
        String maliciousJson = a2q("{'secret':'INJECTED'}");

        OuterAnySetter result = MAPPER.readValue(maliciousJson, OuterAnySetter.class);

        assertFalse(result.inner.extra.containsKey("secret"),
                "@JsonIgnoreProperties({'secret'}) bypassed via @JsonUnwrapped + "
                + "@JsonAnySetter; inner.extra=" + result.inner.extra);
    }
}
