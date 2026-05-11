package com.fasterxml.jackson.databind.tofix;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.As;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.InvalidTypeIdException;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.databind.testutil.DatabindTestUtil;
import com.fasterxml.jackson.databind.testutil.failure.JacksonTestFailureExpected;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * JDB-011: Generic type IDs bypass polymorphic subtype validation.
 *
 * DatabindContext._resolveAndValidateGeneric() validates the raw class name (before '<')
 * against the PTV. If the raw class name is ALLOWED, the code skips validateSubType
 * entirely and constructs the full parameterized type including attacker-controlled type
 * parameters. The type parameters are never individually validated.
 *
 * A PTV that allows a safe container class (e.g. "java.util.ArrayList") but not
 * a gadget class (e.g. EvilGadget) is bypassed: an attacker submits the type ID
 * "java.util.ArrayList&lt;EvilGadget&gt;" and EvilGadget is loaded and deserialized as the
 * element type without any PTV check.
 *
 * Patch: in _resolveAndValidateGeneric(), recursively validate all non-trivial type
 * parameters via _typeParamsAllowed() before returning the constructed type.
 */
public class SecurityPocJdb011Test extends DatabindTestUtil
{
    // Safe type: explicitly in the allowlist
    public static class SafePayload {
        public String data;
        public SafePayload() {}
        public SafePayload(String d) { this.data = d; }
    }

    // "Gadget" type: NOT in the allowlist; should never be instantiated via PTV
    public static class EvilGadget {
        public String secret;
    }

    // Container using class-name type info on the Object field
    static class Container {
        @JsonTypeInfo(use = Id.CLASS, include = As.WRAPPER_ARRAY)
        public Object value;

        public Container() {}
    }

    // NOTE: polymorphicTypeValidator() is used (not activateDefaultTyping) so that
    // the custom PTV governs both base-type and sub-type validation.
    //
    // IMPORTANT: we intentionally do NOT call allowIfBaseType(Object.class).
    // When validateBaseType returns ALLOWED, StdTypeResolverBuilder.verifyBaseTypeValidity()
    // replaces the PTV with LaissezFaireSubTypeValidator ("all subtypes OK"), which would
    // bypass our per-subtype checks. Returning INDETERMINATE keeps the original PTV in use.
    private ObjectMapper buildMapper() {
        BasicPolymorphicTypeValidator ptv = BasicPolymorphicTypeValidator.builder()
                // Allow the container class (ArrayList) by name prefix
                .allowIfSubType("java.util.ArrayList")
                // Allow the explicitly-safe payload class
                .allowIfSubType(SafePayload.class)
                .build();
        return jsonMapperBuilder()
                .polymorphicTypeValidator(ptv)
                .build();
    }

    /**
     * Sanity check: ArrayList&lt;SafePayload&gt; — both the container and its element type
     * are explicitly allowed by the PTV. Deserialization must succeed.
     */
    @Test
    public void testJdb011_negativeControl_allowedGenericType() throws Exception {
        ObjectMapper mapper = buildMapper();

        // Craft JSON with ArrayList<SafePayload> as the type ID
        String safeClass = SafePayload.class.getName();
        String json = "{\"value\":[\"java.util.ArrayList<" + safeClass + ">\",[{\"data\":\"hello\"}]]}";

        Container result = mapper.readValue(json, Container.class);
        assertNotNull(result.value, "ArrayList<SafePayload> should deserialize successfully");
        assertInstanceOf(java.util.ArrayList.class, result.value,
                "value should be an ArrayList");
    }

    /**
     * Issue reproduction: ArrayList&lt;EvilGadget&gt; — ArrayList is allowed by the PTV but
     * EvilGadget is NOT. Without the fix, the raw class name "java.util.ArrayList" is
     * ALLOWED by validateSubClassName, validateSubType is skipped entirely, and the full
     * type ArrayList&lt;EvilGadget&gt; is returned without validating the element type.
     * EvilGadget is then deserialized as a list element, bypassing the PTV allow-list.
     *
     * With the fix, _typeParamsAllowed() recursively validates EvilGadget and finds it
     * is not ALLOWED by any sub-class matcher, causing InvalidTypeIdException.
     */
    @JacksonTestFailureExpected
    @Test
    public void testJdb011_genericTypeIdBypassesAllowlist() throws Exception {
        ObjectMapper mapper = buildMapper();

        // Craft the exploit type ID: ArrayList wrapping the forbidden EvilGadget class
        String evilClass = EvilGadget.class.getName();
        String json = "{\"value\":[\"java.util.ArrayList<" + evilClass + ">\",[{\"secret\":\"hacked\"}]]}";

        try {
            Container result = mapper.readValue(json, Container.class);
            // If deserialization succeeds, EvilGadget must NOT have been instantiated
            if (result.value instanceof java.util.List<?>) {
                java.util.List<?> list = (java.util.List<?>) result.value;
                if (!list.isEmpty()) {
                    assertFalse(list.get(0) instanceof EvilGadget,
                            "JDB-011 VULNERABLE: generic type-id 'java.util.ArrayList<EvilGadget>' " +
                            "bypassed PTV allow-list. EvilGadget was instantiated as list element: " +
                            list.get(0));
                }
            }
        } catch (InvalidTypeIdException e) {
            // Expected after the fix: type parameter EvilGadget was rejected by PTV
            assertTrue(e.getMessage() != null && !e.getMessage().isEmpty());
        }
    }
}
