package com.fasterxml.jackson.databind.jsontype.vld;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectMapper.DefaultTyping;
import com.fasterxml.jackson.databind.deser.std.StdValueInstantiator;
import com.fasterxml.jackson.databind.exc.InvalidTypeIdException;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for [databind#6496]: resolved content (and key) types of Collection,
 * Map and reference types named by type id must be validated, even if not
 * visible as type parameters of the type id.
 */
@SuppressWarnings("serial")
public class ResolvedContentTypeValidation6496Test extends DatabindTestUtil
{
    static boolean gadgetCreated;

    /** Stand-in "gadget" type: never allow-listed */
    public static class Gadget {
        public Gadget() { gadgetCreated = true; }

        // usable as Map key
        public Gadget(String key) { gadgetCreated = true; }
    }

    public static class Animal {
        public String name;
    }

    public static class Dog extends Animal { }

    public static class Item {
        public int x;
    }

    public static class MyGadgetList extends ArrayList<Gadget> { }
    // Content type not visible as type parameter even with generic type id
    public static class MyHiddenGadgetList<T> extends ArrayList<Gadget> { }
    public static class MyNestedGadgetList extends ArrayList<List<Gadget>> { }
    public static class MyGadgetValueMap extends HashMap<String, Gadget> { }
    public static class MyGadgetKeyMap extends HashMap<Gadget, String> { }
    public static class MyGadgetRef extends AtomicReference<Gadget> { }

    public static class MyStringList extends ArrayList<String> { }
    public static class MyAnimalList extends ArrayList<Animal> { }
    public static class MyDogList extends ArrayList<Dog> { }
    // Well-known value types
    public static class MyDateList extends ArrayList<Date> { }
    public static class MyLocaleMap extends HashMap<Locale, Currency> { }
    public static class MyNodeList extends ArrayList<JsonNode> { }
    public static class MyUriList extends ArrayList<java.net.URI> { }
    public static class MyFileList extends ArrayList<java.io.File> { }
    public static class MyPathList extends ArrayList<java.nio.file.Path> { }
    public static class MyTimestampList extends ArrayList<java.sql.Timestamp> { }
    public static class MyCharSequenceList extends ArrayList<CharSequence> { }
    // not considered safe
    public static class MySerializableList extends ArrayList<java.io.Serializable> { }
    public static class MyLocalDateList extends ArrayList<java.time.LocalDate> { }
    // ... but not types in sub-packages of `java.time`
    public static class MyFormatterList extends ArrayList<java.time.format.DateTimeFormatter> { }

    public static class MyDogListList extends ArrayList<List<Dog>> { }

    // Content type wraps type parameter (like Spring `LinkedMultiValueMap`)
    public static class MyMultiMap<K, V> extends HashMap<K, List<V>> { }
    // Part of content type from class declaration, part from type parameter
    public static class MyHiddenGadgetMapList<T> extends ArrayList<Map<Gadget, T>> { }
    // Mutually recursive content types
    public static class MyPing extends ArrayList<MyPong> { }
    public static class MyPong extends ArrayList<MyPing> { }

    // Polymorphic (via property annotation) base type, not `Object`
    public interface Plain { }
    public static class PlainEnv<T> implements Plain {
        public T payload;
    }
    public static class PlainDogList extends ArrayList<Dog> implements Plain { }

    // Recursive type: must not cause infinite recursion
    public static class MyNode extends ArrayList<MyNode> { }

    @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS)
    public interface Payload { }

    public static class Batch extends ArrayList<List<Item>> implements Payload { }

    // With Default Typing (`OBJECT_AND_NON_CONCRETE`), values of these properties
    // are typed (but not elements of concrete types like `Animal`)
    static class ObjectWrapper { public Object value; }
    static class AnimalsWrapper { public List<Animal> value; }
    static class DogsWrapper { public List<Dog> value; }
    static class AnimalIterableWrapper { public Iterable<Animal> value; }
    static class AnimalListsWrapper { public Collection<List<Animal>> value; }
    // ... and with annotation, with default validator
    static class PayloadWrapper { public Payload value; }
    static class AnnotatedObjectWrapper {
        @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS)
        public Object value;
    }
    static class PlainWrapper {
        @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS)
        public Plain value;
    }

    @BeforeEach
    public void reset() {
        gadgetCreated = false;
    }

    /*
    /**********************************************************************
    /* Test methods
    /**********************************************************************
     */

    @Test
    public void contentTypesRejected() throws Exception
    {
        ObjectMapper mapper = containerMapper();
        _verifyRejected(mapper, json(MyGadgetList.class, "[{}]"));
        _verifyRejected(mapper, json(MyHiddenGadgetList.class.getName() + "<java.lang.String>", "[{}]"));
        _verifyRejected(mapper, json(MyNestedGadgetList.class, "[[{}]]"));
        _verifyRejected(mapper, json(MyGadgetValueMap.class, "{'a':{}}"));
        _verifyRejected(mapper, json(MyGadgetKeyMap.class, "{'a':'b'}"));
        _verifyRejected(mapper, json(MyGadgetRef.class, "{}"));
        _verifyRejected(mapper, json(MyHiddenGadgetMapList.class.getName() + "<java.lang.String>",
                "[{}]"));
        // as type parameter
        _verifyRejected(mapper, json("java.util.ArrayList<" + MyGadgetList.class.getName() + ">",
                "[[{}]]"));
        // as element type of explicitly approved array type
        _verifyRejected(mapper, json("[L" + MyGadgetList.class.getName() + ";", "[[{}]]"));
        // not a well-known value type
        _verifyRejected(mapper, json(MyFormatterList.class, "[]"));
        _verifyRejected(mapper, json(MySerializableList.class, "[]"));
    }

    // Content type need not be subtype of declared content type (type erasure):
    // approval of declared content type as base type must not cover it
    @Test
    public void contentTypeNotSubtypeOfDeclared() throws Exception
    {
        ObjectMapper mapper = mapper(BasicPolymorphicTypeValidator.builder()
                .allowIfSubType(Collection.class)
                .allowIfBaseType(Animal.class));
        _verifyRejected(mapper, json(MyGadgetList.class, "[{}]"), AnimalsWrapper.class);
    }

    // ... nor allowed by default validator (`LaissezFaireSubTypeValidator`), which only
    // allows subtypes of declared type
    @Test
    public void contentTypeNotSubtypeOfDeclaredWithDefaultValidator() throws Exception
    {
        ObjectMapper mapper = jsonMapperBuilder()
                .activateDefaultTyping(LaissezFaireSubTypeValidator.instance,
                        DefaultTyping.OBJECT_AND_NON_CONCRETE)
                .build();
        InvalidTypeIdException e = assertThrows(InvalidTypeIdException.class,
                () -> mapper.readValue(json(MyGadgetList.class, "[{}]"), AnimalsWrapper.class));
        verifyException(e, "not a subtype of declared type");
        assertFalse(gadgetCreated);
        // but subtypes are fine
        assertEquals(MyDogList.class, mapper.readValue(json(MyDogList.class, "[{}]"),
                AnimalsWrapper.class).value.getClass());
    }

    // Approval of base type does not cover content type either
    @Test
    public void contentTypeRejectedWithApprovedBaseType() throws Exception
    {
        ObjectMapper mapper = mapper(BasicPolymorphicTypeValidator.builder()
                .allowIfBaseType(Collection.class));
        _verifyRejected(mapper, json(MyGadgetList.class, "[{}]"), AnimalsWrapper.class);
    }

    @Test
    public void safeContentTypesAccepted() throws Exception
    {
        ObjectMapper mapper = containerMapper();
        assertEquals(Arrays.asList("a"), _read(mapper, MyStringList.class, "['a']"));
        assertEquals(Arrays.asList("a"), _read(mapper, ArrayList.class, "['a']"));
        assertEquals(Collections.singletonMap("a", 1), _read(mapper, HashMap.class, "{'a':1}"));
        assertEquals(Arrays.asList("a"), mapper.readValue(
                json("java.util.ArrayList<java.lang.String>", "['a']"), ObjectWrapper.class).value);
        assertEquals(MyStringList[].class, mapper.readValue(
                json("[L" + MyStringList.class.getName() + ";", "[['a']]"), ObjectWrapper.class)
                .value.getClass());
        assertEquals(MyNode.class, _read(mapper, MyNode.class, "[]").getClass());
        assertEquals(MyPing.class, _read(mapper, MyPing.class, "[[]]").getClass());
    }

    @Test
    public void valueContentTypesAccepted() throws Exception
    {
        ObjectMapper mapper = containerMapper();
        assertEquals(Arrays.asList(new Date(0L)), _read(mapper, MyDateList.class, "[0]"));
        assertEquals(Collections.singletonMap(Locale.CANADA, Currency.getInstance("CAD")),
                _read(mapper, MyLocaleMap.class, "{'en_CA':'CAD'}"));
        assertEquals(Arrays.asList(mapper.readTree(a2q("{'a':1}"))),
                _read(mapper, MyNodeList.class, "[{'a':1}]"));
        assertEquals(Arrays.asList(java.net.URI.create("urn:x")),
                _read(mapper, MyUriList.class, "['urn:x']"));
        assertEquals(Arrays.asList(new java.io.File("x")), _read(mapper, MyFileList.class, "['x']"));
        // (`Path` elements would need type ids of their own here, being abstract)
        assertEquals(Collections.emptyList(), _read(mapper, MyPathList.class, "[]"));
        assertEquals(Arrays.asList(new java.sql.Timestamp(0L)),
                _read(mapper, MyTimestampList.class, "[0]"));
        assertEquals(Arrays.asList("x"), _read(mapper, MyCharSequenceList.class, "['x']"));
        // (no Java 8 date/time module here: just verify type id is accepted)
        assertEquals(Collections.emptyList(), _read(mapper, MyLocalDateList.class, "[]"));
    }

    @Test
    public void allowedContentTypeAccepted() throws Exception
    {
        ObjectMapper mapper = mapper(BasicPolymorphicTypeValidator.builder()
                .allowIfSubType(Collection.class)
                .allowIfSubType(Animal.class));
        List<?> dogs = (List<?>) _read(mapper, MyDogList.class, "[{'name':'Rex'}]");
        assertEquals(Dog.class, dogs.get(0).getClass());
    }

    // Content type same as declared one comes from declaration: not validated
    @Test
    public void declaredContentTypeAccepted() throws Exception
    {
        ObjectMapper mapper = containerMapper();
        assertEquals(MyAnimalList.class, mapper.readValue(json(MyAnimalList.class, "[{}]"),
                AnimalsWrapper.class).value.getClass());
        assertEquals(ArrayList.class, mapper.readValue(json(ArrayList.class, "[{}]"),
                AnimalsWrapper.class).value.getClass());
        assertEquals(MyDogList.class, mapper.readValue(json(MyDogList.class, "[{}]"),
                DogsWrapper.class).value.getClass());
    }

    // Subtype of declared content type: allowed only if approved (by base type)
    @Test
    public void subtypeOfDeclaredContentType() throws Exception
    {
        _verifyRejected(containerMapper(), json(MyDogList.class, "[{}]"), AnimalsWrapper.class);

        ObjectMapper mapper = mapper(BasicPolymorphicTypeValidator.builder()
                .allowIfBaseType(List.class));
        List<Animal> animals = mapper.readValue(json(MyDogList.class, "[{}]"),
                AnimalsWrapper.class).value;
        assertEquals(Dog.class, animals.get(0).getClass());
    }

    // Type parameters of declared content types are not named by type id either:
    // must not be rejected by default validator (`LaissezFaireSubTypeValidator`)
    @Test
    public void nestedDeclaredContentWithDefaultValidator() throws Exception
    {
        Payload p = newJsonMapper().readValue(json(Batch.class, "[[{'x':1}]]"),
                PayloadWrapper.class).value;
        assertEquals(1, ((Batch) p).get(0).get(0).x);
    }

    // Validation must take linear (not exponential) time wrt nesting depth
    @Test
    public void deeplyNestedTypeIdValidatedFast() throws Exception
    {
        _verifyNestedFast(HashMap.class.getName());
        _verifyNestedFast(MyMultiMap.class.getName());
    }

    private void _verifyNestedFast(String mapType) throws Exception
    {
        StringBuilder sb = new StringBuilder();
        final int levels = 40;
        for (int i = 0; i < levels; ++i) {
            sb.append(mapType).append("<java.lang.String,");
        }
        sb.append("java.lang.String");
        for (int i = 0; i < levels; ++i) {
            sb.append('>');
        }
        final String json = json(sb.toString(), "{}");
        ObjectMapper mapper = containerMapper();
        assertTimeoutPreemptively(java.time.Duration.ofSeconds(5), () -> {
            assertEquals(Collections.emptyMap(),
                    mapper.readValue(json, ObjectWrapper.class).value);
        }, mapType);
    }

    // Description of value type (for error messages) is constructed lazily, since
    // constructing it for every level of nested types takes quadratic time
    @Test
    public void valueInstantiatorTypeDescLazy() throws Exception
    {
        ObjectMapper mapper = newJsonMapper();
        JavaType type = mapper.getTypeFactory().constructFromCanonical(
                MyMultiMap.class.getName() + "<java.lang.String,java.lang.Integer>");
        StdValueInstantiator inst = new StdValueInstantiator(mapper.getDeserializationConfig(), type);
        assertEquals(type.toString(), inst.getValueTypeDesc());
        assertEquals(type.toString(), new StdValueInstantiator(inst) { }.getValueTypeDesc());
    }

    // Content type that comes from type parameter bound to declared type parameter
    // of base type that is not a container type (`Iterable<Animal>`) is not validated
    @Test
    public void contentTypeFromNonContainerBaseType() throws Exception
    {
        List<?> animals = (List<?>) containerMapper().readValue(json(ArrayList.class, "[{}]"),
                AnimalIterableWrapper.class).value;
        assertEquals(Animal.class, animals.get(0).getClass());
    }

    // Content types of a type parameter come from its class declaration: not to be
    // rejected by default validator any more than for top-level type id
    @Test
    public void contentTypeOfTypeParameterWithDefaultValidator() throws Exception
    {
        Plain p = newJsonMapper().readValue(
                json(PlainEnv.class.getName() + "<" + PlainDogList.class.getName() + ">",
                        "{'payload':[{}]}"),
                PlainWrapper.class).value;
        assertEquals(Dog.class, ((PlainDogList) ((PlainEnv<?>) p).payload).get(0).getClass());
    }

    // Approval of base type covers subtypes of declared type parameters of
    // content type same as for content type itself
    @Test
    public void nestedSubtypeOfDeclaredContentType() throws Exception
    {
        ObjectMapper mapper = mapper(BasicPolymorphicTypeValidator.builder()
                .allowIfBaseType(Collection.class));
        Collection<List<Animal>> lists = mapper.readValue(json(MyDogListList.class, a2q("[['java.util.ArrayList',[{}]]]")),
                AnimalListsWrapper.class).value;
        assertEquals(MyDogListList.class, lists.getClass());
    }

    /*
    /**********************************************************************
    /* Helper methods
    /**********************************************************************
     */

    // Validator that approves all Collections (and arrays of), Maps and references
    private ObjectMapper containerMapper() {
        return mapper(BasicPolymorphicTypeValidator.builder()
                .allowIfSubType(Collection.class)
                .allowIfSubType(Collection[].class)
                .allowIfSubType(Map.class)
                .allowIfSubType(AtomicReference.class));
    }

    private ObjectMapper mapper(BasicPolymorphicTypeValidator.Builder ptv) {
        return jsonMapperBuilder()
                .activateDefaultTyping(ptv.build(), DefaultTyping.OBJECT_AND_NON_CONCRETE)
                .build();
    }

    private static String json(Class<?> type, String value) {
        return json(type.getName(), value);
    }

    private static String json(String typeId, String value) {
        return a2q("{'value':['" + typeId + "'," + value + "]}");
    }

    private Object _read(ObjectMapper mapper, Class<?> type, String value) throws Exception {
        Object result = mapper.readValue(json(type, value), ObjectWrapper.class).value;
        assertEquals(type, result.getClass());
        return result;
    }

    private void _verifyRejected(ObjectMapper mapper, String json) {
        _verifyRejected(mapper, json, ObjectWrapper.class);
    }

    private void _verifyRejected(ObjectMapper mapper, String json, Class<?> wrapperType)
    {
        InvalidTypeIdException e = assertThrows(InvalidTypeIdException.class,
                () -> mapper.readValue(json, wrapperType));
        verifyException(e, "denied resolution of contained type");
        assertFalse(gadgetCreated);
    }
}
