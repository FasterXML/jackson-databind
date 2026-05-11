package com.fasterxml.jackson.databind.jsontype.vld;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.As;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.InvalidTypeIdException;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * [databind#5988]: generic type IDs must not bypass {@link com.fasterxml.jackson.databind.jsontype.PolymorphicTypeValidator}.
 *<p>
 * {@code DatabindContext._resolveAndValidateGeneric()} historically validated the raw
 * container class name (before {@code '<'}) only; if approved, it constructed the full
 * parameterized type and returned it without validating the type parameters. An attacker
 * could supply a type ID such as {@code "java.util.ArrayList<EvilGadget>"} to smuggle a
 * non-allow-listed element type past the PTV. The fix recursively validates each non-
 * trivial type parameter (and array element types appearing as parameters).
 */
public class BasicPTVGenericParameterBypassTest extends DatabindTestUtil
{
    /**
     * Records every constructor invocation; lets the tests prove that a non-allow-listed
     * type is not actually instantiated when the validator rejects it.
     */
    static final List<String> INSTANTIATIONS = new ArrayList<>();

    /** Stand-in "unsafe" type, never allow-listed. */
    public static class EvilGadget {
        public String secret;
        public EvilGadget() {
            INSTANTIATIONS.add(EvilGadget.class.getName());
        }
    }

    /** Always allow-listed in these tests. */
    public static class SafePayload {
        public String data;
        public SafePayload() {}
        public SafePayload(String d) { this.data = d; }
    }

    static class Container {
        @JsonTypeInfo(use = Id.CLASS, include = As.WRAPPER_ARRAY)
        public Object value;

        public Container() {}
    }

    // NOTE: only polymorphicTypeValidator() is set (no activateDefaultTyping and no
    // allowIfBaseType(Object)). validateBaseType returning INDETERMINATE keeps the
    // original PTV in force; an ALLOWED return value would swap in
    // LaissezFaireSubTypeValidator and would defeat the per-subtype check.
    private ObjectMapper mapperWithArrayListAndSafePayload() {
        BasicPolymorphicTypeValidator ptv = BasicPolymorphicTypeValidator.builder()
                .allowIfSubType("java.util.ArrayList")
                .allowIfSubType("java.util.HashMap")
                .allowIfSubType(SafePayload.class)
                .build();
        return jsonMapperBuilder()
                .polymorphicTypeValidator(ptv)
                .build();
    }

    // (1) Sanity check: ArrayList<SafePayload> -- both container and element are allowed.
    @Test
    public void allowedGenericTypeAccepted() throws Exception
    {
        ObjectMapper mapper = mapperWithArrayListAndSafePayload();

        String json = "{\"value\":[\"java.util.ArrayList<" + SafePayload.class.getName() + ">\","
                + "[{\"data\":\"hello\"}]]}";

        Container result = mapper.readValue(json, Container.class);
        assertNotNull(result.value);
        assertEquals(ArrayList.class, result.value.getClass());
    }

    // (2) Issue reproduction: ArrayList<EvilGadget> -- container is allowed, element is not.
    // Pre-fix this slipped past the PTV. Post-fix the element type is validated and denied.
    @Test
    public void genericTypeIdBypassesAllowlistDenied() throws Exception
    {
        ObjectMapper mapper = mapperWithArrayListAndSafePayload();

        final String evilClass = EvilGadget.class.getName();
        String json = "{\"value\":[\"java.util.ArrayList<" + evilClass + ">\","
                + "[{\"secret\":\"hacked\"}]]}";

        INSTANTIATIONS.clear();
        InvalidTypeIdException e = assertThrows(InvalidTypeIdException.class,
                () -> mapper.readValue(json, Container.class),
                "ArrayList<EvilGadget> must be denied because EvilGadget is not allow-listed");
        verifyException(e, evilClass);
        assertEquals(0, INSTANTIATIONS.size(),
                "EvilGadget must not be instantiated when its element form is denied;"
                        + " observed=" + INSTANTIATIONS);
    }

    // (3) Map value position: HashMap<String, EvilGadget> -- container allowed (HashMap),
    // String key is a benign JDK type but not allow-listed -> denied at the key step.
    @Test
    public void mapValueGadgetDenied() throws Exception
    {
        ObjectMapper mapper = mapperWithArrayListAndSafePayload();

        final String evilClass = EvilGadget.class.getName();
        String json = "{\"value\":[\"java.util.HashMap<java.lang.String," + evilClass + ">\","
                + "{\"k\":{\"secret\":\"hacked\"}}]}";

        INSTANTIATIONS.clear();
        InvalidTypeIdException e = assertThrows(InvalidTypeIdException.class,
                () -> mapper.readValue(json, Container.class),
                "HashMap<String,EvilGadget> must be denied (neither String nor EvilGadget are allow-listed)");
        // The PTV walks containedType(i) in order: key (String) is checked first and fails
        // because String is not on the allow-list; this is still a correct denial of the
        // overall type id.
        verifyException(e, "denied");
        assertEquals(0, INSTANTIATIONS.size());
    }

    // (4) Map key position: HashMap<EvilGadget, String> -- key denied.
    @Test
    public void mapKeyGadgetDenied() throws Exception
    {
        ObjectMapper mapper = mapperWithArrayListAndSafePayload();

        final String evilClass = EvilGadget.class.getName();
        String json = "{\"value\":[\"java.util.HashMap<" + evilClass + ",java.lang.String>\","
                + "{}]}";

        INSTANTIATIONS.clear();
        InvalidTypeIdException e = assertThrows(InvalidTypeIdException.class,
                () -> mapper.readValue(json, Container.class),
                "HashMap<EvilGadget,String> must be denied: key type EvilGadget is not allow-listed");
        verifyException(e, evilClass);
        assertEquals(0, INSTANTIATIONS.size());
    }

    // (5) Nested generics: ArrayList<ArrayList<EvilGadget>> -- inner element denied.
    @Test
    public void nestedGenericGadgetDenied() throws Exception
    {
        ObjectMapper mapper = mapperWithArrayListAndSafePayload();

        final String evilClass = EvilGadget.class.getName();
        String json = "{\"value\":[\"java.util.ArrayList<java.util.ArrayList<" + evilClass + ">>\","
                + "[[{\"secret\":\"hacked\"}]]]}";

        INSTANTIATIONS.clear();
        InvalidTypeIdException e = assertThrows(InvalidTypeIdException.class,
                () -> mapper.readValue(json, Container.class),
                "Nested ArrayList<ArrayList<EvilGadget>> must be denied at the innermost element");
        verifyException(e, evilClass);
        assertEquals(0, INSTANTIATIONS.size());
    }

    // (6) Sanity check for Map: HashMap<SafePayload, SafePayload> -- both key+value allowed.
    @Test
    public void mapWithAllowedKeyAndValueAccepted() throws Exception
    {
        ObjectMapper mapper = mapperWithArrayListAndSafePayload();

        String safe = SafePayload.class.getName();
        String json = "{\"value\":[\"java.util.HashMap<" + safe + "," + safe + ">\",{}]}";

        Container result = mapper.readValue(json, Container.class);
        assertNotNull(result.value);
        assertEquals(HashMap.class, result.value.getClass());
    }

    // (7) Wildcards / Object resolve to Object.class via TypeFactory; that's the
    // intentional escape hatch and must keep working.
    @Test
    public void objectTypeParameterAccepted() throws Exception
    {
        ObjectMapper mapper = mapperWithArrayListAndSafePayload();

        // Use SafePayload as the element so deserialization itself can complete; the
        // point of this test is purely that "java.util.ArrayList<java.lang.Object>" as
        // a *type id* is not rejected by the generic-parameter validation pass.
        String json = "{\"value\":[\"java.util.ArrayList<java.lang.Object>\",[]]}";
        Container result = mapper.readValue(json, Container.class);
        assertNotNull(result.value);
        assertEquals(ArrayList.class, result.value.getClass());
    }

}
