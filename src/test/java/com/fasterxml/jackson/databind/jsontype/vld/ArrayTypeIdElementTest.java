package com.fasterxml.jackson.databind.jsontype.vld;

import java.io.Serializable;
import java.util.Date;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectMapper.DefaultTyping;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.exc.InvalidDefinitionException;
import com.fasterxml.jackson.databind.exc.InvalidTypeIdException;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.databind.jsontype.PolymorphicTypeValidator;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * The element type of an array type id ({@code [Lcom.foo.Bean;}) is named by
 * the type id like a type argument is, and validated the same way: approving
 * the array, by its base type or by a rule every array matches, does not
 * approve its element type.
 */
public class ArrayTypeIdElementTest extends DatabindTestUtil
{
    /** Stand-in for a gadget class: never allow-listed, not {@code Serializable} */
    public static class Gadget {
        static int constructions;

        public Gadget() {
            ++constructions;
        }

        public void setCmd(String cmd) { }
    }

    /** Abstract, and still deserializable */
    @JsonDeserialize(as = AbstractGadgetImpl.class)
    public static abstract class AbstractGadget {
        public void setCmd(String cmd) {
            ++Gadget.constructions;
        }
    }

    public static class AbstractGadgetImpl extends AbstractGadget { }

    public static class SafeBean {
        public String text;
    }

    public static class SubBean extends SafeBean { }

    enum Color { RED, GREEN }

    // Type ids on the property only: the elements get none of their own
    static final class SerializableHolder {
        @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.WRAPPER_ARRAY)
        public Serializable value;
    }

    // Declared array types get their type ids from default typing: an
    // annotation on the property would apply to the elements instead
    static final class ObjectArrayHolder {
        public Object[] value;
    }

    static final class SafeBeanArrayHolder {
        public SafeBean[] value;
    }

    static final String GADGET = Gadget.class.getName();
    static final String ABSTRACT_GADGET = AbstractGadget.class.getName();
    static final String SAFE_BEAN = SafeBean.class.getName();
    static final String SUB_BEAN = SubBean.class.getName();

    private static ObjectMapper mapperWith(PolymorphicTypeValidator ptv) {
        return jsonMapperBuilder()
                .polymorphicTypeValidator(ptv)
                .build();
    }

    private static ObjectMapper defaultTypingWith(PolymorphicTypeValidator ptv) {
        return jsonMapperBuilder()
                .activateDefaultTyping(ptv, DefaultTyping.NON_FINAL, JsonTypeInfo.As.WRAPPER_ARRAY)
                .build();
    }

    private static String arrayJson(String elementType, String elements) {
        return "{\"value\":[\"[L" + elementType + ";\",[" + elements + "]]}";
    }

    private static InvalidTypeIdException assertRejected(ObjectMapper mapper, String json,
            Class<?> target) {
        Gadget.constructions = 0;
        InvalidTypeIdException e = assertThrows(InvalidTypeIdException.class,
                () -> mapper.readValue(json, target));
        assertEquals(0, Gadget.constructions);
        return e;
    }

    // Every array is `Serializable`: approving that base type approved any
    // element type with it
    @Test
    public void approvedBaseTypeDoesNotApproveElementType() throws Exception
    {
        ObjectMapper mapper = mapperWith(BasicPolymorphicTypeValidator.builder()
                .allowIfBaseType(Serializable.class)
                .build());

        InvalidTypeIdException e = assertRejected(mapper,
                arrayJson(GADGET, "{\"cmd\":\"x\"}"), SerializableHolder.class);
        assertEquals("[L" + GADGET + ";", e.getTypeId());
        verifyException(e, "denied resolution of type parameter `" + GADGET
                + "` (declared as `java.lang.Object`)");

        e = assertRejected(mapper,
                "{\"value\":[\"[[L" + GADGET + ";\",[[{\"cmd\":\"x\"}]]]}", SerializableHolder.class);
        verifyException(e, "denied resolution of type parameter `" + GADGET + "`");

        ObjectMapper arrays = defaultTypingWith(BasicPolymorphicTypeValidator.builder()
                .allowIfBaseType(Object[].class)
                .build());
        e = assertRejected(arrays, arrayJson(GADGET, "{\"cmd\":\"x\"}"), ObjectArrayHolder.class);
        verifyException(e, "denied resolution of type parameter `" + GADGET + "`");
    }

    // Likewise for a subtype rule every array matches
    @Test
    public void approvedArrayTypeDoesNotApproveElementType() throws Exception
    {
        ObjectMapper mapper = mapperWith(BasicPolymorphicTypeValidator.builder()
                .allowIfSubType(Serializable.class)
                .build());

        InvalidTypeIdException e = assertRejected(mapper,
                arrayJson(GADGET, "{\"cmd\":\"x\"}"), SerializableHolder.class);
        verifyException(e, "denied resolution of type parameter `" + GADGET + "`");
    }

    @Test
    public void elementTypesThatCannotNameAClassNeedNoRule() throws Exception
    {
        ObjectMapper mapper = mapperWith(BasicPolymorphicTypeValidator.builder()
                .allowIfBaseType(Serializable.class)
                .build());

        assertArrayEquals(new String[] { "a" }, (String[]) mapper.readValue(
                arrayJson("java.lang.String", "\"a\""), SerializableHolder.class).value);
        assertArrayEquals(new int[] { 1 }, (int[]) mapper.readValue(
                "{\"value\":[\"[I\",[1]]}", SerializableHolder.class).value);
        assertArrayEquals(new Color[] { Color.GREEN }, (Color[]) mapper.readValue(
                arrayJson(Color.class.getName(), "\"GREEN\""), SerializableHolder.class).value);
    }

    @Test
    public void allowListedElementTypeAccepted() throws Exception
    {
        ObjectMapper mapper = mapperWith(BasicPolymorphicTypeValidator.builder()
                .allowIfBaseType(Serializable.class)
                .allowIfSubType(SafeBean.class)
                .build());

        SafeBean[] beans = (SafeBean[]) mapper.readValue(
                arrayJson(SAFE_BEAN, "{\"text\":\"hi\"}"), SerializableHolder.class).value;
        assertEquals("hi", beans[0].text);
    }

    // The declared array type vouches for its element type and for subtypes of
    // it, as a declared type parameter does
    @Test
    public void declaredElementTypeVouchesForItsSubtypes() throws Exception
    {
        // (the elements carry type ids of their own under default typing)
        String declared = arrayJson(SAFE_BEAN, "[\"" + SAFE_BEAN + "\",{\"text\":\"hi\"}]");
        String subtype = arrayJson(SUB_BEAN, "[\"" + SUB_BEAN + "\",{\"text\":\"hi\"}]");

        ObjectMapper mapper = defaultTypingWith(LaissezFaireSubTypeValidator.instance);
        assertEquals("hi", mapper.readValue(declared, SafeBeanArrayHolder.class).value[0].text);
        SafeBean[] beans = mapper.readValue(subtype, SafeBeanArrayHolder.class).value;
        assertEquals(SubBean.class, beans[0].getClass());

        mapper = defaultTypingWith(BasicPolymorphicTypeValidator.builder()
                .allowIfBaseType(SafeBean[].class)
                .allowIfBaseType(SafeBean.class)
                .build());
        assertEquals("hi", mapper.readValue(declared, SafeBeanArrayHolder.class).value[0].text);
        beans = mapper.readValue(subtype, SafeBeanArrayHolder.class).value;
        assertEquals(SubBean.class, beans[0].getClass());
    }

    // Without a validator (the 2.x default), Jackson's own type ids for
    // `Object[]` values still read back: an `Object[]` declares `Object`
    // elements, which any element type fits, nested arrays included
    @Test
    public void noValidatorRoundTripsDeclaredObjectArray() throws Exception
    {
        ObjectMapper mapper = defaultTypingWith(LaissezFaireSubTypeValidator.instance);

        ObjectArrayHolder holder = new ObjectArrayHolder();
        SafeBean bean = new SafeBean();
        bean.text = "hi";
        holder.value = new SafeBean[] { bean };
        ObjectArrayHolder result = mapper.readValue(mapper.writeValueAsString(holder),
                ObjectArrayHolder.class);
        assertEquals("hi", ((SafeBean[]) result.value)[0].text);

        holder.value = new Date[][] { { new Date(1L) } };
        result = mapper.readValue(mapper.writeValueAsString(holder), ObjectArrayHolder.class);
        assertEquals(new Date(1L), ((Date[][]) result.value)[0][0]);

        // The deny list still applies to the element type
        Exception e = assertThrows(InvalidDefinitionException.class,
                () -> mapper.readValue(arrayJson("javax.swing.JEditorPane", "{}"),
                        ObjectArrayHolder.class));
        verifyException(e, "Illegal type");
        verifyException(e, "javax.swing.JEditorPane");
    }

    // The element type of an array for a non-array property is declared as
    // `Object`: a validator that denies `Object` as a base type denies it, as
    // it denies a direct type id for an `Object` property
    @Test
    public void deniedObjectBaseDeniesElementTypeDeclaredAsObject() throws Exception
    {
        ObjectMapper mapper = mapperWith(BasicPolymorphicTypeValidator.builder()
                .allowIfBaseType(Serializable.class)
                .denyForExactBaseType(Object.class)
                .allowIfSubType(SafeBean.class)
                .build());

        InvalidTypeIdException e = assertRejected(mapper,
                arrayJson(SAFE_BEAN, "{\"text\":\"hi\"}"), SerializableHolder.class);
        verifyException(e, "denied resolution of type parameter `" + SAFE_BEAN
                + "` (declared as `java.lang.Object`)");

        assertArrayEquals(new String[] { "a" }, (String[]) mapper.readValue(
                arrayJson("java.lang.String", "\"a\""), SerializableHolder.class).value);
    }

    // An abstract element type is deserializable all the same, through
    // `@JsonDeserialize(as=...)`, so the array-support rule does not exempt it
    @Test
    public void abstractElementTypeNeedsRuleWithArraySupport() throws Exception
    {
        ObjectMapper mapper = mapperWith(BasicPolymorphicTypeValidator.builder()
                .allowIfSubTypeIsArray()
                .build());

        InvalidTypeIdException e = assertRejected(mapper,
                arrayJson(ABSTRACT_GADGET, "{\"cmd\":\"x\"}"), SerializableHolder.class);
        verifyException(e, "denied resolution of type parameter `" + ABSTRACT_GADGET + "`");

        ObjectMapper allowing = mapperWith(BasicPolymorphicTypeValidator.builder()
                .allowIfSubTypeIsArray()
                .allowIfSubType(Pattern.compile(Pattern.quote(ABSTRACT_GADGET)))
                .build());
        AbstractGadget[] values = (AbstractGadget[]) allowing.readValue(
                arrayJson(ABSTRACT_GADGET, "{\"cmd\":\"x\"}"), SerializableHolder.class).value;
        assertEquals(AbstractGadgetImpl.class, values[0].getClass());
    }
}
