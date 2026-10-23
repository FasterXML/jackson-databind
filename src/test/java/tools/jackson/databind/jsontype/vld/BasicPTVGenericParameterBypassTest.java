package tools.jackson.databind.jsontype.vld;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.As;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.exc.InvalidTypeIdException;
import tools.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * [databind#5988]: generic type IDs must not bypass {@link tools.jackson.databind.jsontype.PolymorphicTypeValidator}.
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
    // Fully-qualified name of this test class -- works as a name-prefix matcher
    // for every nested helper (SafePayload, EvilGadget, Container) via their
    // "outer.Name$Nested" form. Used in namePrefixAllowsBothContainerAndParameter.
    private static final String OWN_CLASS_NAME_PREFIX =
            BasicPTVGenericParameterBypassTest.class.getName();

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
        // String-arg constructor: doubles as Jackson's automatic Map-key
        // deserializer when SafePayload appears as a Map key type
        // (used by mapWithAllowedKeyAndValueAccepted).
        public SafePayload(String d) { this.data = d; }
    }

    /** Plain enum used to verify the enum-exemption from type-parameter validation. */
    public enum NonAllowListedEnum {
        VALUE_A, VALUE_B
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
        // The PTV walks containedType(i) in order: key (String) is checked first and
        // denied because String is not on the allow-list; this is still a correct
        // denial of the overall type id. Either class name in the exception message is
        // acceptable -- the iteration order is the only thing that picks one over the
        // other.
        final String msg = e.getMessage();
        assertTrue(msg != null
                        && (msg.contains("java.lang.String") || msg.contains(evilClass)),
                "Denial message should reference the rejected parameter type;"
                        + " was: " + msg);
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

    // (8) Array as a generic parameter: ArrayList<EvilGadget[]> must be denied because
    // the array's element type (EvilGadget) is not allow-listed. Exercises the
    // isArrayType() recursion branch in _validateTypeParameter.
    @Test
    public void gadgetArrayAsGenericParameterDenied() throws Exception
    {
        ObjectMapper mapper = mapperWithArrayListAndSafePayload();

        final String evilClass = EvilGadget.class.getName();
        final String arrayId = "[L" + evilClass + ";";
        // ArrayList<EvilGadget[]> type id, with an array containing one inner element.
        String json = "{\"value\":[\"java.util.ArrayList<" + arrayId + ">\","
                + "[[{\"secret\":\"hacked\"}]]]}";

        INSTANTIATIONS.clear();
        InvalidTypeIdException e = assertThrows(InvalidTypeIdException.class,
                () -> mapper.readValue(json, Container.class),
                "ArrayList<EvilGadget[]> must be denied: array element EvilGadget is not allow-listed");
        verifyException(e, evilClass);
        assertEquals(0, INSTANTIATIONS.size());
    }

    // (9) Name-prefix PTV: allowIfSubType(String) registers a name matcher, not a class
    // matcher. Type parameters should be approved by the same name-prefix rule used
    // for the container -- otherwise a configuration intended to allow everything
    // under "com.example." would reject "com.example.Foo" as a type parameter.
    @Test
    public void namePrefixAllowsBothContainerAndParameter() throws Exception
    {
        BasicPolymorphicTypeValidator ptv = BasicPolymorphicTypeValidator.builder()
                .allowIfSubType("java.util.ArrayList")
                // Allow SafePayload by its enclosing-class name prefix (name matcher
                // only -- no class matcher is registered for SafePayload).
                .allowIfSubType(OWN_CLASS_NAME_PREFIX)
                .build();
        ObjectMapper mapper = jsonMapperBuilder()
                .polymorphicTypeValidator(ptv)
                .build();

        String json = "{\"value\":[\"java.util.ArrayList<" + SafePayload.class.getName() + ">\","
                + "[{\"data\":\"hello\"}]]}";

        Container result = mapper.readValue(json, Container.class);
        assertNotNull(result.value);
        assertEquals(ArrayList.class, result.value.getClass());
    }

    // (10) Enum exemption: EnumSet<NonAllowListedEnum> -- container "java.util.EnumSet" is
    // approved by the "java" name prefix, but the enum element class is not on the
    // allow-list. Enum classes are JVM-managed singletons resolved by name lookup
    // (no attacker-controlled instantiation), so the type-parameter validation
    // exempts them -- matching the exemption that already applies to Object. Without
    // this exemption, a reasonable name-prefix PTV would break legitimate
    // EnumSet/EnumMap deserialization (see [databind#4849]-style regression).
    @Test
    public void enumTypeParameterAccepted() throws Exception
    {
        BasicPolymorphicTypeValidator ptv = BasicPolymorphicTypeValidator.builder()
                .allowIfSubType("java.")
                .build();
        ObjectMapper mapper = jsonMapperBuilder()
                .polymorphicTypeValidator(ptv)
                .build();

        String json = "{\"value\":[\"java.util.EnumSet<" + NonAllowListedEnum.class.getName() + ">\","
                + "[\"VALUE_A\"]]}";

        Container result = mapper.readValue(json, Container.class);
        assertNotNull(result.value);
        assertEquals(EnumSet.of(NonAllowListedEnum.VALUE_A), result.value);
    }
}
