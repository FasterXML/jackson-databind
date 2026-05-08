package tools.jackson.databind.jsontype.vld;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import tools.jackson.databind.*;
import tools.jackson.databind.exc.InvalidTypeIdException;
import tools.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import tools.jackson.databind.jsontype.PolymorphicTypeValidator;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.*;

/**
 * [databind#5981] {@code BasicPolymorphicTypeValidator.allowIfSubTypeIsArray()} must not
 * approve arrays whose component type would itself be denied by the configured
 * sub-class allow-list.
 *<p>
 * Pre-fix the matcher returned {@code clazz.isArray()} unconditionally. With default
 * typing on an {@code Object}-typed field, an attacker could ship a wrapper-array
 * type id naming {@code FakeGadget[]}: the validator approved the array type, and
 * because the array's component type was concrete and final there was no per-element
 * type id (and therefore no further PTV invocation) -- {@code ObjectArrayDeserializer}
 * ran the plain bean deserializer for each element, materializing {@code FakeGadget}
 * instances despite not being in the allow-list.
 *<p>
 * Post-fix the array matcher delegates the component-type check back to the rest of
 * the builder's sub-class matchers, so {@code FakeGadget[]} is denied unless
 * {@code FakeGadget} itself is also allow-listed.
 */
public class BasicPTVArrayComponentBypassTest extends DatabindTestUtil
{
    /** Records every constructor invocation; lets the test prove that an
     *  un-allow-listed type is not actually instantiated. */
    static final List<String> INSTANTIATIONS = new ArrayList<>();

    /** Stand-in "unsafe" type. Completely benign in itself -- only side effect is
     *  recording its own instantiation. {@code final} so default typing emits no
     *  per-element type id, which is the configuration the bypass exploited. */
    static final class FakeGadget {
        public String cmd;
        public FakeGadget() {
            INSTANTIATIONS.add(FakeGadget.class.getName());
        }
    }

    /** A type that IS in the allow-list, present so the PTV configuration is
     *  realistic (an explicit allow-list plus arrays). */
    static final class SafePayload {
        public String data;
        public SafePayload() { }
    }

    static final class ObjectWrapper {
        public Object value;
        protected ObjectWrapper() { }
    }

    // For [databind#5981]: with the validator engaged (no allowIfBaseType escape
    // hatch into LaissezFaire), a direct FakeGadget value must be denied AND the
    // same FakeGadget wrapped as FakeGadget[] must also be denied -- the array
    // matcher must not bypass the component-type allow-list.
    @Test
    public void arrayMatcherMustNotBypassComponentAllowList() throws Exception
    {
        PolymorphicTypeValidator ptv = BasicPolymorphicTypeValidator.builder()
                .allowIfSubType(SafePayload.class)
                .allowIfSubTypeIsArray()
                .build();
        ObjectMapper mapper = jsonMapperBuilder()
                .activateDefaultTyping(ptv, DefaultTyping.NON_FINAL)
                .build();

        // (a) Direct FakeGadget: denied. Sanity check that the validator is actually
        // engaged with this configuration.
        final String classId = FakeGadget.class.getName();
        final String directJson = "{\"value\":[\"" + classId + "\",{\"cmd\":\"x\"}]}";
        INSTANTIATIONS.clear();
        InvalidTypeIdException directDenied = assertThrows(InvalidTypeIdException.class,
                () -> mapper.readValue(directJson, ObjectWrapper.class),
                "Direct FakeGadget must be denied (no array, no SafePayload match)");
        verifyException(directDenied, classId);
        assertEquals(0, INSTANTIATIONS.size(),
                "FakeGadget must not be instantiated when its bare class is denied;"
                        + " observed=" + INSTANTIATIONS);

        // (b) FakeGadget wrapped as FakeGadget[]: pre-fix this slipped through via
        // allowIfSubTypeIsArray() ignoring the component type. Post-fix the array
        // matcher delegates the component check to the peer matchers and denies it.
        final String arrayId = "[L" + classId + ";";
        final String arrayJson = "{\"value\":[\"" + arrayId + "\",[{\"cmd\":\"x\"}]]}";
        INSTANTIATIONS.clear();
        InvalidTypeIdException arrayDenied = assertThrows(InvalidTypeIdException.class,
                () -> mapper.readValue(arrayJson, ObjectWrapper.class),
                "FakeGadget[] must be denied because FakeGadget is not allow-listed");
        verifyException(arrayDenied, arrayId);
        assertEquals(0, INSTANTIATIONS.size(),
                "FakeGadget must not be instantiated when its array form is denied;"
                        + " observed=" + INSTANTIATIONS);
    }
}
