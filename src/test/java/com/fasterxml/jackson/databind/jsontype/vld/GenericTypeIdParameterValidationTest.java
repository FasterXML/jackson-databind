package com.fasterxml.jackson.databind.jsontype.vld;

import java.util.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectMapper.DefaultTyping;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.exc.InvalidDefinitionException;
import com.fasterxml.jackson.databind.exc.InvalidTypeIdException;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.databind.jsontype.PolymorphicTypeValidator;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for [CVE-2026-110879]: classes named as type parameters of a generic
 * type id must pass the same checks as classes named directly as a type id
 * (for base type {@code Object}), regardless of which declared type parameter
 * they bind to and regardless of the outer type having been approved.
 */
public class GenericTypeIdParameterValidationTest extends DatabindTestUtil
{
    static final List<String> INSTANTIATIONS = new ArrayList<>();

    @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS)
    public interface Msg { }

    public static class Env<T> implements Msg {
        public T payload;
    }

    public static class PlainMsg implements Msg {
        public String text;
    }

    /** Stand-in "gadget" type: never allow-listed */
    public static class Gadget {
        public Gadget() {
            INSTANTIATIONS.add("Gadget()");
        }

        // Single-String constructor: usable as Map key via key deserializer
        public Gadget(String key) {
            INSTANTIATIONS.add("Gadget(" + key + ")");
        }

        public void setCmd(String cmd) {
            INSTANTIATIONS.add("setCmd(" + cmd + ")");
        }
    }

    /** Abstract, but still deserializable "gadget" type */
    @JsonDeserialize(as = AbstractGadgetImpl.class)
    public static abstract class AbstractGadget {
        public void setCmd(String cmd) {
            INSTANTIATIONS.add("setCmd(" + cmd + ")");
        }
    }

    public static class AbstractGadgetImpl extends AbstractGadget {
        public AbstractGadgetImpl() {
            INSTANTIATIONS.add("AbstractGadgetImpl()");
        }
    }

    /** Stand-in "gadget" type that is also `Serializable` (like most real ones) */
    public static class SerializableGadget extends Gadget implements java.io.Serializable {
        private static final long serialVersionUID = 1L;
    }

    /** Allow-listed (by some tests) bean type */
    public static class SafeBean {
        public String data;
    }

    public enum Color { RED, GREEN }

    static class MsgWrapper {
        public Msg msg;
    }

    static class MinimalClassWrapper {
        @JsonTypeInfo(use = JsonTypeInfo.Id.MINIMAL_CLASS)
        public Iterable<String> values;
    }

    static final class StringMapWrapper {
        public Map<String, String> map;
    }

    static final class ArrayMapWrapper {
        public Map<String, Object> map;
    }

    static class SerializableWrapper {
        @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS)
        public java.io.Serializable value;
    }

    public static class SubBean extends SafeBean {
        public int extra;
    }

    public static final class FinalSubBean extends SafeBean {
        public int extra;
    }

    static final class BeanArrayWrapper {
        public SafeBean[] beans;
    }

    static class ObjectValueWrapper {
        @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS)
        public Object value;
    }

    static final class ObjectArrayWrapper {
        public Object[] values;
    }

    static final class EnumContainersWrapper {
        public EnumMap<Color, String> enumMap;
        public EnumSet<Color> enumSet;
    }

    private final String ENV = Env.class.getName();
    private final String GADGET = Gadget.class.getName();
    private final String SERIALIZABLE_GADGET = SerializableGadget.class.getName();
    private final String ABSTRACT_GADGET = AbstractGadget.class.getName();

    @BeforeEach
    public void clearInstantiations() {
        INSTANTIATIONS.clear();
    }

    /*
    /**********************************************************************
    /* Default (base-type limiting) validator, annotation-based typing
    /**********************************************************************
     */

    private ObjectMapper blockingMapper() {
        return jsonMapperBuilder()
                .enable(MapperFeature.BLOCK_UNSAFE_POLYMORPHIC_BASE_TYPES)
                .build();
    }

    @Test
    public void directGadgetIsNotASubtype() throws Exception
    {
        InvalidTypeIdException e = assertThrows(InvalidTypeIdException.class,
                () -> blockingMapper().readValue(msgJson(GADGET, "\"cmd\":\"x\""),
                        MsgWrapper.class));
        verifyException(e, "Not a subtype");
        assertEquals(Collections.emptyList(), INSTANTIATIONS);
    }

    @Test
    public void gadgetAsTypeParameterRejected() throws Exception
    {
        InvalidTypeIdException e = assertThrows(InvalidTypeIdException.class,
                () -> blockingMapper().readValue(msgJson(ENV + "<" + GADGET + ">",
                        "\"payload\":{\"cmd\":\"x\"}"), MsgWrapper.class));
        verifyException(e, "denied resolution of contained type");
        verifyException(e, GADGET);
        assertEquals(Collections.emptyList(), INSTANTIATIONS);
    }

    @Test
    public void nestedGadgetAsTypeParameterRejected() throws Exception
    {
        InvalidTypeIdException e = assertThrows(InvalidTypeIdException.class,
                () -> blockingMapper().readValue(
                        msgJson(ENV + "<java.util.ArrayList<" + GADGET + ">>",
                                "\"payload\":[{},{},{}]"), MsgWrapper.class));
        verifyException(e, "denied resolution of contained type");
        assertEquals(Collections.emptyList(), INSTANTIATIONS);
    }

    @Test
    public void safeTypeParametersAccepted() throws Exception
    {
        ObjectMapper mapper = blockingMapper();
        MsgWrapper w = mapper.readValue(msgJson(ENV, "\"payload\":\"a\""), MsgWrapper.class);
        assertEquals("a", ((Env<?>) w.msg).payload);

        w = mapper.readValue(msgJson(ENV + "<java.lang.String>", "\"payload\":\"b\""),
                MsgWrapper.class);
        assertEquals("b", ((Env<?>) w.msg).payload);

        w = mapper.readValue(msgJson(ENV + "<java.lang.Integer>", "\"payload\":3"),
                MsgWrapper.class);
        assertEquals(Integer.valueOf(3), ((Env<?>) w.msg).payload);

        w = mapper.readValue(msgJson(ENV + "<" + Color.class.getName() + ">",
                "\"payload\":\"GREEN\""), MsgWrapper.class);
        assertEquals(Color.GREEN, ((Env<?>) w.msg).payload);

        w = mapper.readValue(msgJson(ENV + "<java.lang.Object>", "\"payload\":\"c\""),
                MsgWrapper.class);
        assertEquals("c", ((Env<?>) w.msg).payload);

        w = mapper.readValue(msgJson(ENV + "<java.math.BigDecimal>", "\"payload\":1.25"),
                MsgWrapper.class);
        assertEquals(new java.math.BigDecimal("1.25"), ((Env<?>) w.msg).payload);

        w = mapper.readValue(msgJson(ENV + "<java.math.BigInteger>", "\"payload\":7"),
                MsgWrapper.class);
        assertEquals(java.math.BigInteger.valueOf(7), ((Env<?>) w.msg).payload);

        final UUID uuid = UUID.randomUUID();
        w = mapper.readValue(msgJson(ENV + "<java.util.UUID>", "\"payload\":\"" + uuid + "\""),
                MsgWrapper.class);
        assertEquals(uuid, ((Env<?>) w.msg).payload);
    }

    /*
    /**********************************************************************
    /* Approval of outer base type must not approve type parameters
    /**********************************************************************
     */

    @Test
    public void allowedBaseTypeDoesNotApproveTypeParameters() throws Exception
    {
        ObjectMapper mapper = jsonMapperBuilder()
                .polymorphicTypeValidator(BasicPolymorphicTypeValidator.builder()
                        .allowIfBaseType(Msg.class)
                        .build())
                .build();
        // Approved base type: direct subtypes and safe type parameters fine
        MsgWrapper w = mapper.readValue(msgJson(PlainMsg.class.getName(), "\"text\":\"x\""),
                MsgWrapper.class);
        assertEquals(PlainMsg.class, w.msg.getClass());
        w = mapper.readValue(msgJson(ENV + "<java.lang.String>", "\"payload\":\"b\""),
                MsgWrapper.class);
        assertEquals("b", ((Env<?>) w.msg).payload);

        InvalidTypeIdException e = assertThrows(InvalidTypeIdException.class,
                () -> mapper.readValue(msgJson(ENV + "<" + GADGET + ">",
                        "\"payload\":{\"cmd\":\"x\"}"), MsgWrapper.class));
        verifyException(e, "denied resolution of contained type");
        // Should name the configured validator, not the internal wrapper
        verifyException(e, BasicPolymorphicTypeValidator.class.getName());
        assertEquals(Collections.emptyList(), INSTANTIATIONS);

        e = assertThrows(InvalidTypeIdException.class,
                () -> mapper.readValue(msgJson(ENV + "<" + SafeBean.class.getName() + ">",
                        "\"payload\":{\"data\":\"x\"}"), MsgWrapper.class));
        verifyException(e, SafeBean.class.getName());
    }

    // Type parameter that is subtype of polymorphic base type: same checks as direct type id
    @Test
    public void baseSubtypeAsTypeParameterAccepted() throws Exception
    {
        final String plainMsgId = ENV + "<" + PlainMsg.class.getName() + ">";
        // (payload is a `Msg` so has type id of its own)
        final String json = msgJson(plainMsgId, "\"payload\":{\"@class\":\""
                + PlainMsg.class.getName() + "\",\"text\":\"x\"}");
        ObjectMapper mapper = jsonMapperBuilder()
                .polymorphicTypeValidator(BasicPolymorphicTypeValidator.builder()
                        .allowIfBaseType(Msg.class)
                        .build())
                .build();
        MsgWrapper w = mapper.readValue(json, MsgWrapper.class);
        assertEquals("x", ((PlainMsg) ((Env<?>) w.msg).payload).text);

        w = blockingMapper().readValue(json, MsgWrapper.class);
        assertEquals("x", ((PlainMsg) ((Env<?>) w.msg).payload).text);
    }

    @Test
    public void allowListedTypeParameterAccepted() throws Exception
    {
        ObjectMapper mapper = jsonMapperBuilder()
                .polymorphicTypeValidator(BasicPolymorphicTypeValidator.builder()
                        .allowIfBaseType(Msg.class)
                        .allowIfSubType(SafeBean.class)
                        .build())
                .build();
        MsgWrapper w = mapper.readValue(msgJson(ENV + "<" + SafeBean.class.getName() + ">",
                "\"payload\":{\"data\":\"x\"}"), MsgWrapper.class);
        assertEquals("x", ((SafeBean) ((Env<?>) w.msg).payload).data);
    }

    @Test
    public void minimalClassTypeParameterRejected() throws Exception
    {
        ObjectMapper mapper = jsonMapperBuilder()
                .polymorphicTypeValidator(BasicPolymorphicTypeValidator.builder()
                        .allowIfBaseType(Iterable.class)
                        .build())
                .build();
        // (JSON Arrays use wrapper-array type id inclusion)
        MinimalClassWrapper w = mapper.readValue(a2q(
                "{'values':['java.util.ArrayList<java.lang.String>',['a']]}"),
                MinimalClassWrapper.class);
        assertEquals(Arrays.asList("a"), w.values);
        InvalidTypeIdException e = assertThrows(InvalidTypeIdException.class,
                () -> mapper.readValue(a2q(
                        "{'values':['java.util.ArrayList<" + GADGET + ">',[{}]]}"),
                        MinimalClassWrapper.class));
        verifyException(e, "denied resolution of contained type");
        assertEquals(Collections.emptyList(), INSTANTIATIONS);
    }

    /*
    /**********************************************************************
    /* Default typing with outer-base rule, Map key
    /**********************************************************************
     */

    private ObjectMapper mapDefaultTypingMapper() {
        PolymorphicTypeValidator ptv = BasicPolymorphicTypeValidator.builder()
                .allowIfBaseType(Map.class)
                .build();
        return jsonMapperBuilder()
                .activateDefaultTyping(ptv, DefaultTyping.NON_FINAL)
                .build();
    }

    @Test
    public void defaultTypingMapKeyGadgetRejected() throws Exception
    {
        ObjectMapper mapper = mapDefaultTypingMapper();
        InvalidTypeIdException e = assertThrows(InvalidTypeIdException.class,
                () -> mapper.readValue(a2q("{'map':['java.util.HashMap<" + GADGET
                        + ",java.lang.String>',{'attacker-chosen':'x'}]}"),
                        StringMapWrapper.class));
        verifyException(e, "denied resolution of contained type");
        verifyException(e, GADGET);
        assertEquals(Collections.emptyList(), INSTANTIATIONS);
    }

    @Test
    public void defaultTypingCompatibleMapAccepted() throws Exception
    {
        ObjectMapper mapper = mapDefaultTypingMapper();
        StringMapWrapper input = new StringMapWrapper();
        input.map = new HashMap<>();
        input.map.put("a", "b");
        String json = mapper.writeValueAsString(input);
        assertEquals(input.map, mapper.readValue(json, StringMapWrapper.class).map);

        StringMapWrapper w = mapper.readValue(a2q(
                "{'map':['java.util.HashMap<java.lang.String,java.lang.String>',{'c':'d'}]}"),
                StringMapWrapper.class);
        assertEquals(Collections.singletonMap("c", "d"), w.map);
    }

    // Arrays are not gadgets by themselves: only their element type is validated
    @Test
    public void defaultTypingArrayTypeParameters() throws Exception
    {
        ObjectMapper mapper = mapDefaultTypingMapper();
        ArrayMapWrapper w = mapper.readValue(a2q(
                "{'map':['java.util.HashMap<java.lang.String,[Ljava.lang.String;>',{'k':['a','b']}]}"),
                ArrayMapWrapper.class);
        assertArrayEquals(new String[] { "a", "b" }, (String[]) w.map.get("k"));

        w = mapper.readValue(a2q("{'map':['java.util.HashMap<java.lang.String,[I>',{'k':[1,2]}]}"),
                ArrayMapWrapper.class);
        assertArrayEquals(new int[] { 1, 2 }, (int[]) w.map.get("k"));

        w = mapper.readValue(a2q("{'map':['java.util.HashMap<java.lang.String,[L"
                + Color.class.getName() + ";>',{'k':['RED']}]}"),
                ArrayMapWrapper.class);
        assertArrayEquals(new Color[] { Color.RED }, (Color[]) w.map.get("k"));

        InvalidTypeIdException e = assertThrows(InvalidTypeIdException.class,
                () -> mapper.readValue(a2q("{'map':['java.util.HashMap<java.lang.String,[L"
                        + GADGET + ";>',{'k':[{}]}]}"),
                        ArrayMapWrapper.class));
        verifyException(e, "denied resolution of contained type");
        verifyException(e, GADGET);
        assertEquals(Collections.emptyList(), INSTANTIATIONS);
    }

    /*
    /**********************************************************************
    /* Array type ids: element type must be validated too
    /**********************************************************************
     */

    private ObjectMapper serializableMapper(Class<?>... allowedSubTypes) {
        BasicPolymorphicTypeValidator.Builder b = BasicPolymorphicTypeValidator.builder()
                .allowIfBaseType(java.io.Serializable.class);
        for (Class<?> cls : allowedSubTypes) {
            b = b.allowIfSubType(cls);
        }
        return jsonMapperBuilder()
                .polymorphicTypeValidator(b.build())
                .build();
    }

    @Test
    public void arrayTypeIdGadgetElementRejected() throws Exception
    {
        // Every array type is `Serializable` (even if element type is not)
        ObjectMapper mapper = serializableMapper();
        InvalidTypeIdException e = assertThrows(InvalidTypeIdException.class,
                () -> mapper.readValue(a2q("{'value':['[L" + GADGET + ";',[{'cmd':'x'}]]}"),
                        SerializableWrapper.class));
        verifyException(e, "denied resolution of contained type");
        verifyException(e, GADGET);

        e = assertThrows(InvalidTypeIdException.class,
                () -> mapper.readValue(a2q("{'value':['[[L" + GADGET + ";',[[{'cmd':'x'}]]]}"),
                        SerializableWrapper.class));
        verifyException(e, "denied resolution of contained type");
        assertEquals(Collections.emptyList(), INSTANTIATIONS);

        e = assertThrows(InvalidTypeIdException.class,
                () -> mapper.readValue(a2q("{'value':['[L" + SafeBean.class.getName()
                        + ";',[{'data':'x'}]]}"), SerializableWrapper.class));
        verifyException(e, SafeBean.class.getName());
    }

    @Test
    public void arrayTypeIdSafeElementAccepted() throws Exception
    {
        SerializableWrapper w = serializableMapper().readValue(
                a2q("{'value':['[Ljava.lang.String;',['a']]}"), SerializableWrapper.class);
        assertArrayEquals(new String[] { "a" }, (String[]) w.value);

        w = serializableMapper().readValue(a2q("{'value':['[I',[1]]}"), SerializableWrapper.class);
        assertArrayEquals(new int[] { 1 }, (int[]) w.value);

        w = serializableMapper(SafeBean.class).readValue(a2q("{'value':['[L"
                + SafeBean.class.getName() + ";',[{'data':'x'}]]}"), SerializableWrapper.class);
        assertEquals("x", ((SafeBean[]) w.value)[0].data);
    }

    // Array type id for exactly the declared type: element type not selected by attacker
    @Test
    public void defaultTypingDeclaredArrayTypeAccepted() throws Exception
    {
        ObjectMapper mapper = jsonMapperBuilder()
                .activateDefaultTyping(BasicPolymorphicTypeValidator.builder()
                        // approves array base type (but not `SafeBean` for base `Object`)
                        .allowIfBaseType(SafeBean[].class)
                        .allowIfBaseType(SafeBean.class)
                        .build(), DefaultTyping.NON_FINAL)
                .build();
        BeanArrayWrapper input = new BeanArrayWrapper();
        SafeBean bean = new SafeBean();
        bean.data = "x";
        input.beans = new SafeBean[] { bean };
        String json = mapper.writeValueAsString(input);
        assertTrue(json.contains("[L" + SafeBean.class.getName() + ";"), json);
        BeanArrayWrapper result = mapper.readValue(json, BeanArrayWrapper.class);
        assertEquals("x", result.beans[0].data);
    }

    // Approval of declared array type covers arrays of subtypes of its element type
    @Test
    public void defaultTypingApprovedArrayBaseCoversElementSubtypes() throws Exception
    {
        ObjectMapper mapper = jsonMapperBuilder()
                .activateDefaultTyping(BasicPolymorphicTypeValidator.builder()
                        .allowIfBaseType(SafeBean[].class)
                        .build(), DefaultTyping.NON_FINAL)
                .build();
        BeanArrayWrapper result = mapper.readValue(a2q("{'beans':['[L"
                + FinalSubBean.class.getName() + ";',[{'extra':4}]]}"), BeanArrayWrapper.class);
        assertEquals(FinalSubBean[].class, result.beans.getClass());
        assertEquals(4, ((FinalSubBean) result.beans[0]).extra);
    }

    // Element type of array type id that is subtype of declared element type
    @Test
    public void defaultTypingDeclaredArraySubtypeAccepted() throws Exception
    {
        ObjectMapper mapper = jsonMapperBuilder()
                .activateDefaultTyping(BasicPolymorphicTypeValidator.builder()
                        .allowIfBaseType(SafeBean[].class)
                        .allowIfBaseType(SafeBean.class)
                        .build(), DefaultTyping.NON_FINAL)
                .build();
        BeanArrayWrapper input = new BeanArrayWrapper();
        SubBean bean = new SubBean();
        bean.data = "x";
        bean.extra = 3;
        input.beans = new SubBean[] { bean };
        String json = mapper.writeValueAsString(input);
        assertTrue(json.contains("[L" + SubBean.class.getName() + ";"), json);
        BeanArrayWrapper result = mapper.readValue(json, BeanArrayWrapper.class);
        assertEquals(SubBean[].class, result.beans.getClass());
        assertEquals(3, ((SubBean) result.beans[0]).extra);
    }

    // ... but approval of declared array type does not cover any element type
    @Test
    public void defaultTypingDeclaredObjectArrayGadgetRejected() throws Exception
    {
        ObjectMapper mapper = jsonMapperBuilder()
                .activateDefaultTyping(BasicPolymorphicTypeValidator.builder()
                        .allowIfBaseType(Object[].class)
                        .build(), DefaultTyping.NON_FINAL)
                .build();
        InvalidTypeIdException e = assertThrows(InvalidTypeIdException.class,
                () -> mapper.readValue(a2q("{'values':['[L" + GADGET + ";',[]]}"),
                        ObjectArrayWrapper.class));
        verifyException(e, "denied resolution of contained type");
        verifyException(e, GADGET);
    }

    @Test
    public void defaultTypingEnumContainersRoundTrip() throws Exception
    {
        ObjectMapper mapper = jsonMapperBuilder()
                .activateDefaultTyping(BasicPolymorphicTypeValidator.builder()
                        .allowIfBaseType(Map.class)
                        .allowIfBaseType(Collection.class)
                        .build(), DefaultTyping.NON_FINAL)
                .build();
        EnumContainersWrapper input = new EnumContainersWrapper();
        input.enumMap = new EnumMap<>(Color.class);
        input.enumMap.put(Color.RED, "r");
        input.enumSet = EnumSet.of(Color.GREEN);
        String json = mapper.writeValueAsString(input);
        // Jackson itself writes generic type ids for these:
        assertTrue(json.contains("java.util.EnumMap<"), json);
        assertTrue(json.contains("java.util.EnumSet<"), json);
        EnumContainersWrapper result = mapper.readValue(json, EnumContainersWrapper.class);
        assertEquals(input.enumMap, result.enumMap);
        assertEquals(input.enumSet, result.enumSet);
    }

    /*
    /**********************************************************************
    /* Deny list applies to type parameters even if validator allows all
    /**********************************************************************
     */

    @Test
    public void denyListedTypeParameterRejected() throws Exception
    {
        // Default (laissez-faire) validator for annotation-based typing, base type
        // `Object`: contained types are allowed by validator, but not by deny list
        ObjectMapper mapper = newJsonMapper();
        InvalidDefinitionException e = assertThrows(InvalidDefinitionException.class,
                () -> mapper.readValue(a2q("{'value':{'@class':'" + ENV
                        + "<java.util.logging.FileHandler>','payload':{}}}"),
                        ObjectValueWrapper.class));
        verifyException(e, "Illegal type");
        verifyException(e, "prevented for security reasons");
        verifyException(e, "java.util.logging.FileHandler");
    }

    /*
    /**********************************************************************
    /* Default (laissez-faire) validator
    /**********************************************************************
     */

    // Laissez-faire validator allows all subtypes of base type, but contained types
    // need to be subtypes of base type as well (not just of `Object`)
    @Test
    public void laissezFaireNonSubtypeTypeParameterRejected() throws Exception
    {
        ObjectMapper mapper = newJsonMapper();
        InvalidTypeIdException e = assertThrows(InvalidTypeIdException.class,
                () -> mapper.readValue(msgJson(ENV + "<" + GADGET + ">",
                        "\"payload\":{\"cmd\":\"x\"}"), MsgWrapper.class));
        verifyException(e, "not a subtype of base type");
        verifyException(e, GADGET);
        assertEquals(Collections.emptyList(), INSTANTIATIONS);

        // also as element type of (nested) arrays
        e = assertThrows(InvalidTypeIdException.class,
                () -> mapper.readValue(msgJson(ENV + "<[[L" + GADGET + ";>",
                        "\"payload\":[[{\"cmd\":\"x\"}]]"), MsgWrapper.class));
        verifyException(e, "not a subtype of base type");
        verifyException(e, GADGET);
        assertEquals(Collections.emptyList(), INSTANTIATIONS);

        MsgWrapper w = mapper.readValue(msgJson(ENV + "<java.lang.String>", "\"payload\":\"b\""),
                MsgWrapper.class);
        assertEquals("b", ((Env<?>) w.msg).payload);
        w = mapper.readValue(msgJson(ENV + "<" + PlainMsg.class.getName() + ">",
                "\"payload\":{\"@class\":\"" + PlainMsg.class.getName() + "\",\"text\":\"x\"}"),
                MsgWrapper.class);
        assertEquals("x", ((PlainMsg) ((Env<?>) w.msg).payload).text);
    }

    @Test
    public void laissezFaireDefaultTyping() throws Exception
    {
        ObjectMapper mapper = jsonMapperBuilder()
                .activateDefaultTyping(LaissezFaireSubTypeValidator.instance, DefaultTyping.NON_FINAL)
                .build();
        // Map key not subtype of `Map`: rejected
        InvalidTypeIdException e = assertThrows(InvalidTypeIdException.class,
                () -> mapper.readValue(a2q("{'map':['java.util.HashMap<" + GADGET
                        + ",java.lang.String>',{'attacker-chosen':'x'}]}"),
                        StringMapWrapper.class));
        verifyException(e, "not a subtype of base type");
        assertEquals(Collections.emptyList(), INSTANTIATIONS);

        // Array of subtype of declared element type: fine
        BeanArrayWrapper input = new BeanArrayWrapper();
        SubBean bean = new SubBean();
        bean.extra = 5;
        input.beans = new SubBean[] { bean };
        BeanArrayWrapper result = mapper.readValue(mapper.writeValueAsString(input),
                BeanArrayWrapper.class);
        assertEquals(5, ((SubBean) result.beans[0]).extra);
    }

    /*
    /**********************************************************************
    /* Array type ids approved by sub-type matcher
    /**********************************************************************
     */

    @Test
    public void arrayTypeIdApprovedBySubTypeMatcher() throws Exception
    {
        // Every array type is `Serializable`, even if element type is not
        ObjectMapper mapper = jsonMapperBuilder()
                .polymorphicTypeValidator(BasicPolymorphicTypeValidator.builder()
                        .allowIfSubType(java.io.Serializable.class)
                        .build())
                .build();
        InvalidTypeIdException e = assertThrows(InvalidTypeIdException.class,
                () -> mapper.readValue(a2q("{'value':['[L" + GADGET + ";',[{'cmd':'x'}]]}"),
                        ObjectValueWrapper.class));
        verifyException(e, "denied resolution of contained type");
        verifyException(e, GADGET);
        assertEquals(Collections.emptyList(), INSTANTIATIONS);

        ObjectValueWrapper w = mapper.readValue(a2q("{'value':['[Ljava.lang.String;',['a']]}"),
                ObjectValueWrapper.class);
        assertArrayEquals(new String[] { "a" }, (String[]) w.value);
    }

    // Abstract element types are not exempt: may still be deserializable
    @Test
    public void abstractArrayElementTypeValidated() throws Exception
    {
        // Name-only allow-list for the generic type
        ObjectMapper mapper = jsonMapperBuilder()
                .polymorphicTypeValidator(BasicPolymorphicTypeValidator.builder()
                        .allowIfSubType(ENV)
                        .build())
                .build();
        InvalidTypeIdException e = assertThrows(InvalidTypeIdException.class,
                () -> mapper.readValue(msgJson(ENV + "<[L" + ABSTRACT_GADGET + ";>",
                        "\"payload\":[{\"cmd\":\"x\"}]"), MsgWrapper.class));
        verifyException(e, "denied resolution of contained type");
        verifyException(e, ABSTRACT_GADGET);
        assertEquals(Collections.emptyList(), INSTANTIATIONS);

        // Array type id approved by sub-type matcher (every array is `Serializable`)
        ObjectMapper mapper2 = jsonMapperBuilder()
                .polymorphicTypeValidator(BasicPolymorphicTypeValidator.builder()
                        .allowIfSubType(java.io.Serializable.class)
                        .build())
                .build();
        e = assertThrows(InvalidTypeIdException.class,
                () -> mapper2.readValue(a2q("{'value':['[L" + ABSTRACT_GADGET + ";',[{'cmd':'x'}]]}"),
                        ObjectValueWrapper.class));
        verifyException(e, "denied resolution of contained type");
        verifyException(e, ABSTRACT_GADGET);
        assertEquals(Collections.emptyList(), INSTANTIATIONS);
    }

    // ... even with `allowIfSubTypeIsArray()`
    @Test
    public void abstractArrayElementTypeWithArraySupport() throws Exception
    {
        ObjectMapper mapper = jsonMapperBuilder()
                .polymorphicTypeValidator(BasicPolymorphicTypeValidator.builder()
                        .allowIfSubType(ENV)
                        .allowIfSubTypeIsArray()
                        .build())
                .build();
        InvalidTypeIdException e = assertThrows(InvalidTypeIdException.class,
                () -> mapper.readValue(a2q("{'value':['[L" + ABSTRACT_GADGET + ";',[{'cmd':'x'}]]}"),
                        ObjectValueWrapper.class));
        verifyException(e, ABSTRACT_GADGET);
        e = assertThrows(InvalidTypeIdException.class,
                () -> mapper.readValue(msgJson(ENV + "<[L" + ABSTRACT_GADGET + ";>",
                        "\"payload\":[{\"cmd\":\"x\"}]"), MsgWrapper.class));
        verifyException(e, ABSTRACT_GADGET);
        assertEquals(Collections.emptyList(), INSTANTIATIONS);

        // but fine if allow-listed
        ObjectMapper mapper2 = jsonMapperBuilder()
                .polymorphicTypeValidator(BasicPolymorphicTypeValidator.builder()
                        .allowIfSubType(Msg.class)
                        .allowIfSubTypeIsArray()
                        .build())
                .build();
        MsgWrapper w = mapper2.readValue(msgJson(ENV + "<[L" + Msg.class.getName() + ";>",
                "\"payload\":[{\"@class\":\"" + ENV + "\"}]"), MsgWrapper.class);
        Object[] payload = (Object[]) ((Env<?>) w.msg).payload;
        assertEquals(Msg[].class, payload.getClass());
        assertEquals(Env.class, payload[0].getClass());
    }

    // `Object[]` (with legacy default typing) accepts any element type
    @Test
    public void laissezFaireDeclaredObjectArrayRoundTrip() throws Exception
    {
        ObjectMapper mapper = jsonMapperBuilder()
                .activateDefaultTyping(LaissezFaireSubTypeValidator.instance, DefaultTyping.NON_FINAL)
                .build();
        ObjectArrayWrapper input = new ObjectArrayWrapper();
        SubBean bean = new SubBean();
        bean.extra = 7;
        input.values = new SubBean[] { bean };
        ObjectArrayWrapper result = mapper.readValue(mapper.writeValueAsString(input),
                ObjectArrayWrapper.class);
        assertEquals(SubBean[].class, result.values.getClass());
        assertEquals(7, ((SubBean) result.values[0]).extra);

        input.values = new Date[] { new Date(123L) };
        result = mapper.readValue(mapper.writeValueAsString(input), ObjectArrayWrapper.class);
        assertArrayEquals(new Date[] { new Date(123L) }, result.values);

        // nested arrays too
        input.values = new Date[][] { { new Date(456L) } };
        result = mapper.readValue(mapper.writeValueAsString(input), ObjectArrayWrapper.class);
        assertEquals(Date[][].class, result.values.getClass());
        assertArrayEquals(new Date[] { new Date(456L) }, (Date[]) result.values[0]);
    }

    // `Serializable` (like `Object`) accepts arrays of any element type
    @Test
    public void laissezFaireSerializableArrayRoundTrip() throws Exception
    {
        ObjectMapper mapper = newJsonMapper();
        SerializableWrapper input = new SerializableWrapper();
        SafeBean bean = new SafeBean();
        bean.data = "x";
        // `SafeBean` itself is not `Serializable`, but its arrays are
        input.value = new SafeBean[] { bean };
        SerializableWrapper result = mapper.readValue(mapper.writeValueAsString(input),
                SerializableWrapper.class);
        assertEquals(SafeBean[].class, result.value.getClass());
        assertEquals("x", ((SafeBean[]) result.value)[0].data);

        input.value = new SafeBean[][] { { bean } };
        result = mapper.readValue(mapper.writeValueAsString(input), SerializableWrapper.class);
        assertEquals(SafeBean[][].class, result.value.getClass());
        assertEquals("x", ((SafeBean[][]) result.value)[0][0].data);
    }

    // Specific approval of array type covers its element type...
    @Test
    public void specificallyApprovedArrayTypeAccepted() throws Exception
    {
        final String json = a2q("{'value':['[L" + SafeBean.class.getName() + ";',[{'data':'x'}]]}");
        for (PolymorphicTypeValidator ptv : new PolymorphicTypeValidator[] {
                BasicPolymorphicTypeValidator.builder()
                    .allowIfSubType(Env.class)
                    .allowIfSubType(SafeBean[].class)
                    .build(),
                BasicPolymorphicTypeValidator.builder()
                    .allowIfSubType(Env.class)
                    .allowIfSubType(java.util.regex.Pattern.compile(
                            java.util.regex.Pattern.quote("[L" + SafeBean.class.getName() + ";")))
                    .build() }) {
            ObjectMapper mapper = jsonMapperBuilder()
                    .polymorphicTypeValidator(ptv)
                    .build();
            ObjectValueWrapper w = mapper.readValue(json, ObjectValueWrapper.class);
            assertEquals("x", ((SafeBean[]) w.value)[0].data);

            // also as type parameter
            w = mapper.readValue(a2q("{'value':['" + ENV + "<[L" + SafeBean.class.getName()
                    + ";>',{'payload':[{'data':'y'}]}]}"), ObjectValueWrapper.class);
            assertEquals("y", ((SafeBean[]) ((Env<?>) w.value).payload)[0].data);
            // but not other array types
            InvalidTypeIdException e = assertThrows(InvalidTypeIdException.class,
                    () -> mapper.readValue(a2q("{'value':['[L" + GADGET + ";',[{'cmd':'x'}]]}"),
                            ObjectValueWrapper.class));
            verifyException(e, GADGET);
            assertEquals(Collections.emptyList(), INSTANTIATIONS);
        }
    }

    // ... but not approval that covers all array types (like `Object[]`)
    @Test
    public void nonSpecificallyApprovedArrayTypeElementValidated() throws Exception
    {
        ObjectMapper mapper = jsonMapperBuilder()
                .polymorphicTypeValidator(BasicPolymorphicTypeValidator.builder()
                        .allowIfSubType("[L")
                        .build())
                .build();
        InvalidTypeIdException e = assertThrows(InvalidTypeIdException.class,
                () -> mapper.readValue(a2q("{'value':['[L" + GADGET + ";',[{'cmd':'x'}]]}"),
                        ObjectValueWrapper.class));
        verifyException(e, "denied resolution of contained type");
        verifyException(e, GADGET);
        assertEquals(Collections.emptyList(), INSTANTIATIONS);
    }

    // ... nor approval that covers all array types of the same depth (like
    // `Object[][]` for all 2-dimensional reference arrays)
    @Test
    public void sameDepthArrayApprovalElementValidated() throws Exception
    {
        for (Class<?> arrayType : new Class<?>[] {
                Object[][].class, Object[][][].class,
                java.io.Serializable[].class, java.io.Serializable[][].class }) {
            ObjectMapper mapper = jsonMapperBuilder()
                    .polymorphicTypeValidator(BasicPolymorphicTypeValidator.builder()
                            .allowIfSubType(arrayType)
                            .build())
                    .build();
            int depth = 0;
            for (Class<?> c = arrayType; c.isArray(); c = c.getComponentType()) {
                ++depth;
            }
            StringBuilder id = new StringBuilder();
            StringBuilder value = new StringBuilder("{'cmd':'x'}");
            for (int i = 0; i < depth; ++i) {
                id.append('[');
                value.insert(0, '[').append(']');
            }
            id.append('L').append(SERIALIZABLE_GADGET).append(';');
            final String json = a2q("{'value':['" + id + "'," + value + "]}");
            InvalidTypeIdException e = assertThrows(InvalidTypeIdException.class,
                    () -> mapper.readValue(json, ObjectValueWrapper.class),
                    "for " + arrayType.getSimpleName());
            verifyException(e, "denied resolution of contained type");
            verifyException(e, SERIALIZABLE_GADGET);
            assertEquals(Collections.emptyList(), INSTANTIATIONS);
        }

        // but specific approval of 2-dimensional array type still covers elements
        ObjectMapper mapper = jsonMapperBuilder()
                .polymorphicTypeValidator(BasicPolymorphicTypeValidator.builder()
                        .allowIfSubType(SafeBean[][].class)
                        .build())
                .build();
        ObjectValueWrapper w = mapper.readValue(a2q("{'value':['[[L" + SafeBean.class.getName()
                + ";',[[{'data':'x'}]]]}"), ObjectValueWrapper.class);
        assertEquals("x", ((SafeBean[][]) w.value)[0][0].data);
        // (except for deeply nested arrays, for which element type is always validated)
        ObjectMapper mapper4 = jsonMapperBuilder()
                .polymorphicTypeValidator(BasicPolymorphicTypeValidator.builder()
                        .allowIfSubType(SafeBean[][][][].class)
                        .build())
                .build();
        InvalidTypeIdException e = assertThrows(InvalidTypeIdException.class,
                () -> mapper4.readValue(a2q("{'value':['[[[[L" + SafeBean.class.getName()
                        + ";',[[[[{'data':'x'}]]]]]}"), ObjectValueWrapper.class));
        verifyException(e, "denied resolution of contained type");
    }

    /*
    /**********************************************************************
    /* JDK containers as type parameters
    /**********************************************************************
     */

    @Test
    public void jdkContainerTypeParametersAccepted() throws Exception
    {
        ObjectMapper mapper = jsonMapperBuilder()
                .polymorphicTypeValidator(BasicPolymorphicTypeValidator.builder()
                        .allowIfBaseType(Msg.class)
                        .build())
                .build();
        MsgWrapper w = mapper.readValue(msgJson(ENV + "<java.util.List<java.lang.String>>",
                "\"payload\":[\"a\"]"), MsgWrapper.class);
        assertEquals(Arrays.asList("a"), ((Env<?>) w.msg).payload);
        w = mapper.readValue(msgJson(ENV + "<java.util.ArrayList<java.lang.String>>",
                "\"payload\":[\"b\"]"), MsgWrapper.class);
        assertEquals(Arrays.asList("b"), ((Env<?>) w.msg).payload);
        w = mapper.readValue(msgJson(ENV + "<java.util.HashMap<java.lang.String,java.lang.Integer>>",
                "\"payload\":{\"c\":1}"), MsgWrapper.class);
        assertEquals(Collections.singletonMap("c", 1), ((Env<?>) w.msg).payload);
        w = mapper.readValue(msgJson(ENV
                + "<java.util.concurrent.atomic.AtomicReference<java.lang.String>>",
                "\"payload\":\"d\""), MsgWrapper.class);
        assertEquals("d", ((java.util.concurrent.atomic.AtomicReference<?>) ((Env<?>) w.msg).payload).get());

        // ... but their contents are still validated
        InvalidTypeIdException e = assertThrows(InvalidTypeIdException.class,
                () -> mapper.readValue(msgJson(ENV + "<java.util.ArrayList<" + GADGET + ">>",
                        "\"payload\":[{}]"), MsgWrapper.class));
        verifyException(e, "denied resolution of contained type");
        verifyException(e, GADGET);
        assertEquals(Collections.emptyList(), INSTANTIATIONS);
    }

    /*
    /**********************************************************************
    /* Validators that deny `Object` as base type
    /**********************************************************************
     */

    // Intentional: contained types that are not subtypes of base type are validated
    // as type ids for base type `Object`, which such validators deny
    @Test
    public void objectBaseDenyingValidatorRejectsAllowListedTypeParameter() throws Exception
    {
        ObjectMapper mapper = jsonMapperBuilder()
                .polymorphicTypeValidator(BasicPolymorphicTypeValidator.builder()
                        .allowIfSubType(Msg.class)
                        .allowIfSubType(SafeBean.class)
                        .denyForExactBaseType(Object.class)
                        .build())
                .build();
        InvalidTypeIdException e = assertThrows(InvalidTypeIdException.class,
                () -> mapper.readValue(msgJson(ENV + "<" + SafeBean.class.getName() + ">",
                        "\"payload\":{\"data\":\"x\"}"), MsgWrapper.class));
        verifyException(e, "denied resolution of contained type");
        verifyException(e, SafeBean.class.getName());
    }

    private String msgJson(String typeId, String props) {
        return "{\"msg\":{\"@class\":\"" + typeId + "\"," + props + "}}";
    }
}
