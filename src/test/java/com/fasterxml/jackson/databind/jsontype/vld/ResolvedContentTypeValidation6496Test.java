package com.fasterxml.jackson.databind.jsontype.vld;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.core.type.TypeReference;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectMapper.DefaultTyping;
import com.fasterxml.jackson.databind.exc.InvalidTypeIdException;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.databind.jsontype.PolymorphicTypeValidator;
import com.fasterxml.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for [databind#6496]: resolved content (and key) types of Collection,
 * Map and reference types named by type id must be validated, even if not
 * visible as type parameters of the type id.
 */
public class ResolvedContentTypeValidation6496Test extends DatabindTestUtil
{
    static final List<String> INSTANTIATIONS = new ArrayList<>();

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

    public static class Animal {
        public String name;
    }

    public static class Dog extends Animal { }

    @SuppressWarnings("serial")
    public static class MyGadgetList extends ArrayList<Gadget> { }

    @SuppressWarnings("serial")
    public static class MyStringList extends ArrayList<String> { }

    // Content type not visible as type parameter even with generic type id
    @SuppressWarnings("serial")
    public static class MyHiddenGadgetList<T> extends ArrayList<Gadget> { }

    @SuppressWarnings("serial")
    public static class MyGadgetValueMap extends HashMap<String, Gadget> { }

    @SuppressWarnings("serial")
    public static class MyGadgetKeyMap extends HashMap<Gadget, String> { }

    @SuppressWarnings("serial")
    public static class MyGadgetRef extends AtomicReference<Gadget> { }

    @SuppressWarnings("serial")
    public static class MyNestedGadgetList extends ArrayList<List<Gadget>> { }

    @SuppressWarnings("serial")
    public static class MyDogList extends ArrayList<Dog> { }

    @SuppressWarnings("serial")
    public static class MyAnimalList extends ArrayList<Animal> { }

    // Recursive type: must not cause infinite recursion
    @SuppressWarnings("serial")
    public static class MyNode extends ArrayList<MyNode> { }

    static class ObjectWrapper {
        @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS)
        public Object value;
    }

    // NOTE: `@JsonTypeInfo` on a Collection-valued property applies to its elements,
    // so polymorphic Collection base types are tested with Default Typing instead
    static final TypeReference<Collection<?>> COLLECTION_TYPE = new TypeReference<Collection<?>>() { };
    static final TypeReference<List<Animal>> ANIMALS_TYPE = new TypeReference<List<Animal>>() { };
    static final TypeReference<List<Dog>> DOGS_TYPE = new TypeReference<List<Dog>>() { };

    @BeforeEach
    public void clearInstantiations() {
        INSTANTIATIONS.clear();
    }

    // Validator that approves all Collections, Maps and references (by sub-type)
    private ObjectMapper containerMapper() {
        return mapper(BasicPolymorphicTypeValidator.builder()
                .allowIfSubType(Collection.class)
                .allowIfSubType(Map.class)
                .allowIfSubType(AtomicReference.class)
                .build());
    }

    private ObjectMapper mapper(PolymorphicTypeValidator ptv) {
        return jsonMapperBuilder()
                .polymorphicTypeValidator(ptv)
                .build();
    }

    // Default Typing for abstract types (`List`, `Collection`) but not for `Animal`, `Dog`
    private ObjectMapper defaultTypingMapper(PolymorphicTypeValidator ptv) {
        return jsonMapperBuilder()
                .activateDefaultTyping(ptv, DefaultTyping.OBJECT_AND_NON_CONCRETE)
                .build();
    }

    private ObjectMapper containerDefaultTypingMapper() {
        return defaultTypingMapper(BasicPolymorphicTypeValidator.builder()
                .allowIfSubType(Collection.class)
                .build());
    }

    private static String typedJson(Class<?> type, String value) {
        return a2q("['" + type.getName() + "'," + value + "]");
    }

    private static String json(Class<?> type, String value) {
        return json(type.getName(), value);
    }

    private static String json(String typeId, String value) {
        return a2q("{'value':['" + typeId + "'," + value + "]}");
    }

    /*
    /**********************************************************************
    /* Content types not allowed
    /**********************************************************************
     */

    @Test
    public void gadgetListRejected() throws Exception
    {
        _verifyRejected(containerMapper(), json(MyGadgetList.class, "[{'cmd':'x'}]"));
    }

    @Test
    public void hiddenGadgetListRejected() throws Exception
    {
        _verifyRejected(containerMapper(),
                json(MyHiddenGadgetList.class.getName() + "<java.lang.String>", "[{'cmd':'x'}]"));
    }

    @Test
    public void gadgetMapValueRejected() throws Exception
    {
        _verifyRejected(containerMapper(), json(MyGadgetValueMap.class, "{'a':{'cmd':'x'}}"));
    }

    @Test
    public void gadgetMapKeyRejected() throws Exception
    {
        _verifyRejected(containerMapper(), json(MyGadgetKeyMap.class, "{'a':'b'}"));
    }

    @Test
    public void gadgetReferenceRejected() throws Exception
    {
        _verifyRejected(containerMapper(), json(MyGadgetRef.class, "{'cmd':'x'}"));
    }

    @Test
    public void nestedGadgetListRejected() throws Exception
    {
        _verifyRejected(containerMapper(), json(MyNestedGadgetList.class, "[[{'cmd':'x'}]]"));
    }

    // Content type of a container type named as type parameter
    @Test
    public void gadgetListAsTypeParameterRejected() throws Exception
    {
        _verifyRejected(containerMapper(),
                json("java.util.ArrayList<" + MyGadgetList.class.getName() + ">",
                        "[[{'cmd':'x'}]]"));
    }

    // Approval of polymorphic base type must not approve content type either
    @Test
    public void gadgetListRejectedWithApprovedBaseType() throws Exception
    {
        ObjectMapper mapper = defaultTypingMapper(BasicPolymorphicTypeValidator.builder()
                .allowIfBaseType(Collection.class)
                .build());
        InvalidTypeIdException e = assertThrows(InvalidTypeIdException.class,
                () -> mapper.readValue(typedJson(MyGadgetList.class, "[{'cmd':'x'}]"),
                        COLLECTION_TYPE));
        verifyException(e, "denied resolution of contained type");
        verifyException(e, Gadget.class.getName());
        assertEquals(Collections.emptyList(), INSTANTIATIONS);
    }

    @Test
    public void gadgetListRejectedWithDefaultTyping() throws Exception
    {
        ObjectMapper mapper = containerDefaultTypingMapper();
        InvalidTypeIdException e = assertThrows(InvalidTypeIdException.class,
                () -> mapper.readValue(typedJson(MyGadgetList.class, "[{'cmd':'x'}]"),
                        Object.class));
        verifyException(e, "denied resolution of contained type");
        verifyException(e, Gadget.class.getName());
        assertEquals(Collections.emptyList(), INSTANTIATIONS);
    }

    /*
    /**********************************************************************
    /* Content types allowed
    /**********************************************************************
     */

    @Test
    public void stringListAccepted() throws Exception
    {
        ObjectWrapper w = containerMapper().readValue(json(MyStringList.class, "['a','b']"),
                ObjectWrapper.class);
        assertEquals(MyStringList.class, w.value.getClass());
        assertEquals(Arrays.asList("a", "b"), w.value);
    }

    @Test
    public void plainJdkContainersAccepted() throws Exception
    {
        ObjectMapper mapper = containerMapper();
        ObjectWrapper w = mapper.readValue(json(ArrayList.class, "['a']"), ObjectWrapper.class);
        assertEquals(Collections.singletonList("a"), w.value);
        w = mapper.readValue(json(HashMap.class, "{'a':1}"), ObjectWrapper.class);
        assertEquals(Collections.singletonMap("a", 1), w.value);
        w = mapper.readValue(json("java.util.ArrayList<java.lang.String>", "['b']"),
                ObjectWrapper.class);
        assertEquals(Collections.singletonList("b"), w.value);
    }

    // Allowed content type is fine
    @Test
    public void allowedContentTypeAccepted() throws Exception
    {
        ObjectMapper mapper = mapper(BasicPolymorphicTypeValidator.builder()
                .allowIfSubType(Collection.class)
                .allowIfSubType(Animal.class)
                .build());
        ObjectWrapper w = mapper.readValue(json(MyDogList.class, "[{'name':'Rex'}]"),
                ObjectWrapper.class);
        assertEquals(MyDogList.class, w.value.getClass());
        assertEquals(Dog.class, ((List<?>) w.value).get(0).getClass());
    }

    // Content type exactly as declared: from declaration, not validated
    @Test
    public void declaredContentTypeAccepted() throws Exception
    {
        ObjectMapper mapper = containerDefaultTypingMapper();
        List<Animal> animals = mapper.readValue(typedJson(MyAnimalList.class, "[{'name':'Rex'}]"),
                ANIMALS_TYPE);
        assertEquals(MyAnimalList.class, animals.getClass());
        assertEquals(Animal.class, animals.get(0).getClass());

        animals = mapper.readValue(typedJson(ArrayList.class, "[{'name':'Rex'}]"), ANIMALS_TYPE);
        assertEquals(ArrayList.class, animals.getClass());
        assertEquals(Animal.class, animals.get(0).getClass());

        List<Dog> dogs = mapper.readValue(typedJson(MyDogList.class, "[{'name':'Rex'}]"),
                DOGS_TYPE);
        assertEquals(MyDogList.class, dogs.getClass());
        assertEquals(Dog.class, dogs.get(0).getClass());
    }

    // Content type that is a subtype of declared one: allowed if approved base type
    // (declared type) covers it
    @Test
    public void subtypeOfDeclaredContentType() throws Exception
    {
        // Not approved: subtype of declared content type, but not allowed
        InvalidTypeIdException e = assertThrows(InvalidTypeIdException.class,
                () -> containerDefaultTypingMapper().readValue(
                        typedJson(MyDogList.class, "[{'name':'Rex'}]"), ANIMALS_TYPE));
        verifyException(e, "denied resolution of contained type");
        verifyException(e, Dog.class.getName());

        // But approval of base type `List<Animal>` covers subtypes of `Animal`
        ObjectMapper mapper = defaultTypingMapper(BasicPolymorphicTypeValidator.builder()
                .allowIfBaseType(List.class)
                .build());
        List<Animal> animals = mapper.readValue(typedJson(MyDogList.class, "[{'name':'Rex'}]"),
                ANIMALS_TYPE);
        assertEquals(MyDogList.class, animals.getClass());
        assertEquals(Dog.class, animals.get(0).getClass());
    }

    @Test
    public void recursiveContentType() throws Exception
    {
        ObjectMapper mapper = mapper(BasicPolymorphicTypeValidator.builder()
                .allowIfSubType(MyNode.class)
                .build());
        ObjectWrapper w = mapper.readValue(json(MyNode.class, "[]"), ObjectWrapper.class);
        assertEquals(MyNode.class, w.value.getClass());
        assertEquals(0, ((List<?>) w.value).size());
    }

    private void _verifyRejected(ObjectMapper mapper, String json) throws Exception
    {
        InvalidTypeIdException e = assertThrows(InvalidTypeIdException.class,
                () -> mapper.readValue(json, ObjectWrapper.class));
        verifyException(e, "denied resolution of contained type");
        assertEquals(Collections.emptyList(), INSTANTIATIONS);
    }
}
