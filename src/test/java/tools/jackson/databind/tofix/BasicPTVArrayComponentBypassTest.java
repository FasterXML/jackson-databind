package tools.jackson.databind.tofix;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import tools.jackson.databind.*;
import tools.jackson.databind.exc.InvalidTypeIdException;
import tools.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import tools.jackson.databind.jsontype.PolymorphicTypeValidator;
import tools.jackson.databind.testutil.DatabindTestUtil;
import tools.jackson.databind.testutil.failure.JacksonTestFailureExpected;

import static org.junit.jupiter.api.Assertions.*;

/**
 * [databind#5981] {@code BasicPolymorphicTypeValidator.allowIfSubTypeIsArray()} must not
 * approve arrays whose component type would itself be denied by the configured
 * sub-class allow-list.
 *<p>
 * The pre-fix matcher returns {@code clazz.isArray()} unconditionally. With default
 * typing on an {@code Object}-typed field, an attacker can ship a wrapper-array
 * type id naming {@code FakeGadget[]}: the validator approves the array type, and
 * because the array's component type is concrete and final there is no per-element
 * type id (and therefore no further PTV invocation) -- {@code ObjectArrayDeserializer}
 * runs the plain bean deserializer for each element and {@code FakeGadget} instances
 * are materialized despite not being in the allow-list.
 */
public class BasicPTVArrayComponentBypassTest extends DatabindTestUtil
{
    /** Records every constructor invocation; the test uses this to prove that an
     *  un-allow-listed type was actually instantiated when the bypass succeeds. */
    static final List<String> INSTANTIATIONS = new ArrayList<>();

    /**
     * Stand-in "unsafe" type. Completely benign in itself -- no side effects beyond
     * recording its own instantiation. {@code final} so default typing emits no
     * per-element type id, which is exactly the configuration the bypass exploits.
     */
    static final class FakeGadget {
        public String cmd;
        public FakeGadget() {
            INSTANTIATIONS.add(FakeGadget.class.getName());
        }
    }

    /**
     * A type that IS in the allow-list, present so the PTV configuration is
     * realistic (an explicit allow-list plus arrays).
     */
    static final class SafePayload {
        public String data;
        public SafePayload() { }
    }

    static final class ObjectWrapper {
        public Object value;
        protected ObjectWrapper() { }
    }

    // For [databind#5981]
    @JacksonTestFailureExpected
    @Test
    public void allowIfSubTypeIsArrayMustValidateComponentType() throws Exception
    {
        // Intent: "allow SafePayload, and allow arrays". Bug: arrays of *any*
        // component type pass, including types that are not in the allow-list.
        PolymorphicTypeValidator ptv = BasicPolymorphicTypeValidator.builder()
                .allowIfBaseType(Object.class)
                .allowIfSubType(SafePayload.class)
                .allowIfSubTypeIsArray()
                .build();
        ObjectMapper mapper = jsonMapperBuilder()
                .activateDefaultTyping(ptv, DefaultTyping.NON_FINAL)
                .build();

        // Hand-crafted attacker payload: WRAPPER_ARRAY type id naming FakeGadget[].
        // Equivalent to what default typing emits for an Object-typed field whose
        // runtime value is a FakeGadget[]; written by hand so the test does not
        // depend on the serializer to produce the exact shape.
        final String arrayId = "[L" + FakeGadget.class.getName() + ";";
        final String json = "{\"value\":[\"" + arrayId + "\",[{\"cmd\":\"x\"}]]}";

        INSTANTIATIONS.clear();

        // Expected (post-fix): PTV.validateSubType(Object, FakeGadget[]) recurses
        // into the array's component type, finds FakeGadget is not allow-listed,
        // and returns DENIED -- surfacing as InvalidTypeIdException.
        // Pre-fix: validator returns ALLOWED, ObjectArrayDeserializer constructs a
        // FakeGadget per element via plain bean deser, and readValue succeeds.
        Object materialized = null;
        Throwable thrown = null;
        try {
            materialized = mapper.readValue(json, ObjectWrapper.class);
        } catch (InvalidTypeIdException e) {
            thrown = e;
        }

        if (thrown == null) {
            // Bug path: no exception. Surface the evidence in the failure message.
            Object value = ((ObjectWrapper) materialized).value;
            fail("PTV approved " + arrayId
                    + " whose component type FakeGadget is NOT in the allow-list."
                    + " readValue returned value of type "
                    + (value == null ? "null" : value.getClass().getName())
                    + "; FakeGadget instantiations during deser=" + INSTANTIATIONS);
        }

        // Patched path: ensure the rejection happened on the right type id, and
        // that no FakeGadget slipped through during the (failed) deserialization.
        verifyException((Exception) thrown, arrayId);
        assertEquals(0, INSTANTIATIONS.size(),
                "FakeGadget must not be instantiated when its array form is denied;"
                        + " observed=" + INSTANTIATIONS);
    }

    // For [databind#5981]: negative control isolating the array matcher.
    //
    // The test above includes allowIfBaseType(Object.class), which causes
    // verifyBaseTypeValidity() to swap the PTV out for LaissezFaireSubTypeValidator
    // when the base type is Object -- after which *any* subtype is approved, not
    // only arrays. To prove the array matcher specifically (not LaissezFaire) is
    // the weakness, this control omits allowIfBaseType so the validator stays
    // engaged, and asserts:
    //   (a) a direct FakeGadget value IS denied, and
    //   (b) FakeGadget[] still slips through via allowIfSubTypeIsArray().
    @JacksonTestFailureExpected
    @Test
    public void arrayMatcherBypassesValidatorEvenWhenDirectClassIsDenied() throws Exception
    {
        PolymorphicTypeValidator ptv = BasicPolymorphicTypeValidator.builder()
                .allowIfSubType(SafePayload.class)
                .allowIfSubTypeIsArray()
                .build();
        ObjectMapper mapper = jsonMapperBuilder()
                .activateDefaultTyping(ptv, DefaultTyping.NON_FINAL)
                .build();

        // (a) Direct FakeGadget: must be denied. If this fails, the validator is
        // not actually engaged and the rest of the test proves nothing.
        final String classId = FakeGadget.class.getName();
        final String directJson = "{\"value\":[\"" + classId + "\",{\"cmd\":\"x\"}]}";
        INSTANTIATIONS.clear();
        InvalidTypeIdException directDenied = assertThrows(InvalidTypeIdException.class,
                () -> mapper.readValue(directJson, ObjectWrapper.class),
                "Sanity: direct FakeGadget must be denied with this PTV (no array, no SafePayload match)");
        verifyException(directDenied, classId);
        assertEquals(0, INSTANTIATIONS.size(),
                "FakeGadget must not be instantiated when its bare class is denied;"
                        + " observed=" + INSTANTIATIONS);

        // (b) Same FakeGadget, but wrapped as FakeGadget[]: pre-fix this passes
        // purely because of allowIfSubTypeIsArray() ignoring the component type.
        final String arrayId = "[L" + classId + ";";
        final String arrayJson = "{\"value\":[\"" + arrayId + "\",[{\"cmd\":\"x\"}]]}";
        INSTANTIATIONS.clear();
        Object materialized = null;
        Throwable thrown = null;
        try {
            materialized = mapper.readValue(arrayJson, ObjectWrapper.class);
        } catch (InvalidTypeIdException e) {
            thrown = e;
        }
        if (thrown == null) {
            Object value = ((ObjectWrapper) materialized).value;
            fail("PTV approved " + arrayId + " though the bare class " + classId
                    + " is denied by the SAME validator -- the array matcher does not"
                    + " validate component types. readValue returned value of type "
                    + (value == null ? "null" : value.getClass().getName())
                    + "; FakeGadget instantiations during deser=" + INSTANTIATIONS);
        }
        verifyException((Exception) thrown, arrayId);
        assertEquals(0, INSTANTIATIONS.size(),
                "FakeGadget must not be instantiated when its array form is denied;"
                        + " observed=" + INSTANTIATIONS);
    }
}
