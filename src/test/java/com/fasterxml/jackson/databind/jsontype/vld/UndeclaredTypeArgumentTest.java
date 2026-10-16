package com.fasterxml.jackson.databind.jsontype.vld;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.InvalidTypeIdException;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.databind.jsontype.PolymorphicTypeValidator;
import com.fasterxml.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Type arguments of a generic type id that the base type declares nothing
 * about, or that it declares as {@code Object}: what the configured
 * {@link PolymorphicTypeValidator} (or its absence) has to say about them.
 */
public class UndeclaredTypeArgumentTest extends DatabindTestUtil
{
    @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.WRAPPER_ARRAY)
    abstract static class ClassIdMixin { }

    static class ObjectListHolder {
        public List<Object> value;
    }

    @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.WRAPPER_ARRAY)
    public interface Msg { }

    /** Generic subtype of the base: nothing declares what {@code T} is */
    public static class Env<T> implements Msg {
        public T payload;
    }

    public static class PlainMsg implements Msg {
        public String text;
    }

    static class MsgHolder {
        public Msg value;
    }

    /** Allow-listed bean, not a {@code Msg} */
    public static class SafeBean {
        public String text;
    }

    enum Color { RED, GREEN }

    /** Stand-in for a gadget class: never allow-listed */
    public static class Gadget {
        static int constructions;

        public Gadget() {
            ++constructions;
        }

        public void setCmd(String cmd) { }
    }

    static final String GADGET = Gadget.class.getName();
    static final String ENV = Env.class.getName();
    static final String PLAIN_MSG = PlainMsg.class.getName();
    static final String SAFE_BEAN = SafeBean.class.getName();

    private static ObjectMapper listMapper(PolymorphicTypeValidator ptv) {
        return jsonMapperBuilder()
                .addMixIn(List.class, ClassIdMixin.class)
                .polymorphicTypeValidator(ptv)
                .build();
    }

    private static String listJson(String typeId, String elements) {
        return "{\"value\":[\"java.util.ArrayList<" + typeId + ">\",[" + elements + "]]}";
    }

    private static String envJson(String typeArgument, String payload) {
        return "{\"value\":[\"" + ENV + "<" + typeArgument + ">\",{\"payload\":" + payload + "}]}";
    }

    private static InvalidTypeIdException assertRejected(ObjectMapper mapper, String json) {
        return assertRejected(mapper, json, ObjectListHolder.class);
    }

    private static InvalidTypeIdException assertRejected(ObjectMapper mapper, String json,
            Class<?> target) {
        Gadget.constructions = 0;
        InvalidTypeIdException e = assertThrows(InvalidTypeIdException.class,
                () -> mapper.readValue(json, target));
        assertEquals(0, Gadget.constructions);
        return e;
    }

    // Under a validator that denies `Object` as a base type, an argument that
    // cannot name a class to construct needs no rule: scalars, enums, arrays
    // and `java.util` containers pass, their contents are checked on their own
    @Test
    public void safeArgumentsNeedNoRuleUnderDeniedBaseType() throws Exception
    {
        ObjectMapper mapper = listMapper(BasicPolymorphicTypeValidator.builder()
                .denyForExactBaseType(Object.class)
                .allowIfSubType(ArrayList.class)
                .build());

        assertEquals("a", mapper.readValue(listJson("java.lang.String", "\"a\""),
                ObjectListHolder.class).value.get(0));
        assertEquals(Integer.valueOf(1), mapper.readValue(listJson("java.lang.Integer", "1"),
                ObjectListHolder.class).value.get(0));
        assertEquals(new BigDecimal("1.5"), mapper.readValue(listJson("java.math.BigDecimal", "1.5"),
                ObjectListHolder.class).value.get(0));
        UUID uuid = UUID.randomUUID();
        assertEquals(uuid, mapper.readValue(listJson("java.util.UUID", "\"" + uuid + "\""),
                ObjectListHolder.class).value.get(0));
        assertEquals(Color.GREEN, mapper.readValue(listJson(Color.class.getName(), "\"GREEN\""),
                ObjectListHolder.class).value.get(0));
        assertEquals("a", ((String[]) mapper.readValue(listJson("[Ljava.lang.String;", "[\"a\"]"),
                ObjectListHolder.class).value.get(0))[0]);
        assertEquals("a", ((List<?>) mapper.readValue(
                // The inner list is polymorphic too and carries its own type id
                listJson("java.util.ArrayList<java.lang.String>",
                        "[\"java.util.ArrayList<java.lang.String>\",[\"a\"]]"),
                ObjectListHolder.class).value.get(0)).get(0));
        assertEquals("a", ((AtomicReference<?>) mapper.readValue(
                listJson("java.util.concurrent.atomic.AtomicReference<java.lang.String>", "\"a\""),
                ObjectListHolder.class).value.get(0)).get());
    }

    // The exemption covers the container, not what it contains
    @Test
    public void safeArgumentDoesNotExemptItsContents() throws Exception
    {
        ObjectMapper mapper = listMapper(BasicPolymorphicTypeValidator.builder()
                .denyForExactBaseType(Object.class)
                .allowIfSubType(ArrayList.class)
                .build());

        InvalidTypeIdException e = assertRejected(mapper,
                listJson("java.util.ArrayList<" + GADGET + ">", "[{\"cmd\":\"x\"}]"));
        verifyException(e, "denied resolution of type parameter `" + GADGET + "`");

        e = assertRejected(mapper, listJson("[L" + GADGET + ";", "[{\"cmd\":\"x\"}]"));
        verifyException(e, "denied resolution of type parameter `" + GADGET + "`");
    }

    // The advisory's default-validator case: a direct `Gadget` id is not a
    // subtype of `Msg`, and `Env<Gadget>` must not get around that
    @Test
    public void blockUnsafeRejectsUndeclaredBeanArgument() throws Exception
    {
        ObjectMapper mapper = jsonMapperBuilder()
                .enable(MapperFeature.BLOCK_UNSAFE_POLYMORPHIC_BASE_TYPES)
                .build();

        InvalidTypeIdException e = assertRejected(mapper,
                envJson(GADGET, "{\"cmd\":\"x\"}"), MsgHolder.class);
        verifyException(e, "denied resolution of type parameter `" + GADGET + "`");

        e = assertRejected(mapper,
                envJson("java.util.ArrayList<" + GADGET + ">", "[{\"cmd\":\"x\"}]"), MsgHolder.class);
        verifyException(e, "denied resolution of type parameter `" + GADGET + "`");
    }

    // A subtype of the polymorphic base type is accepted where a direct type id
    // naming it would be; scalars need no rule either way
    @Test
    public void blockUnsafeAcceptsBaseSubtypeAndScalarArguments() throws Exception
    {
        ObjectMapper mapper = jsonMapperBuilder()
                .enable(MapperFeature.BLOCK_UNSAFE_POLYMORPHIC_BASE_TYPES)
                .build();

        // The payload is a `Msg` too and carries its own type id
        Msg msg = mapper.readValue(envJson(PLAIN_MSG, "[\"" + PLAIN_MSG + "\",{\"text\":\"hi\"}]"),
                MsgHolder.class).value;
        assertEquals("hi", ((PlainMsg) ((Env<?>) msg).payload).text);

        msg = mapper.readValue(envJson("java.lang.String", "\"hi\""), MsgHolder.class).value;
        assertEquals("hi", ((Env<?>) msg).payload);
    }

    // A denied base type approves nothing, an allow-list entry included: a
    // direct `SafeBean` id for an `Object` property would be rejected the same way
    @Test
    public void deniedObjectBaseRejectsAllowListedBeanArgument() throws Exception
    {
        ObjectMapper mapper = listMapper(BasicPolymorphicTypeValidator.builder()
                .denyForExactBaseType(Object.class)
                .allowIfSubType(ArrayList.class)
                .allowIfSubType(SafeBean.class)
                .build());

        InvalidTypeIdException e = assertThrows(InvalidTypeIdException.class,
                () -> mapper.readValue(listJson(SAFE_BEAN, "{\"text\":\"hi\"}"), ObjectListHolder.class));
        verifyException(e, "denied resolution of type parameter `" + SAFE_BEAN + "`");
    }
}
