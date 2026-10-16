package com.fasterxml.jackson.databind.jsontype.vld;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

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

    private static ObjectMapper listMapper(PolymorphicTypeValidator ptv) {
        return jsonMapperBuilder()
                .addMixIn(List.class, ClassIdMixin.class)
                .polymorphicTypeValidator(ptv)
                .build();
    }

    private static String listJson(String typeId, String elements) {
        return "{\"value\":[\"java.util.ArrayList<" + typeId + ">\",[" + elements + "]]}";
    }

    private static InvalidTypeIdException assertRejected(ObjectMapper mapper, String json) {
        Gadget.constructions = 0;
        InvalidTypeIdException e = assertThrows(InvalidTypeIdException.class,
                () -> mapper.readValue(json, ObjectListHolder.class));
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
}
