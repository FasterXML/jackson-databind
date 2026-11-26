package tools.jackson.databind.jsontype.vld;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

import tools.jackson.databind.*;
import tools.jackson.databind.deser.std.StdValueInstantiator;
import tools.jackson.databind.exc.InvalidTypeIdException;
import tools.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for [databind#6496]: key and content types of Collection, Map and
 * reference types named by type id that come from their class declarations
 * (not visible as type parameters of the type id) must be validated.
 */
@SuppressWarnings("serial")
public class ResolvedContentTypeValidation6496Test extends DatabindTestUtil
{
    /** Stand-in "gadget" type: never allow-listed */
    public static class Gadget { }

    public static class Animal { }
    public static class Dog extends Animal { }
    public static class Item { }

    public static class MyGadgetList extends ArrayList<Gadget> { }
    // Content type not visible as type parameter even with generic type id
    public static class MyHiddenGadgetList<T> extends ArrayList<Gadget> { }
    // Part of content type from class declaration, part from type parameter
    public static class MyHiddenGadgetMapList<T> extends ArrayList<Map<Gadget, T>> { }
    public static class MyNestedGadgetList extends ArrayList<List<Gadget>> { }
    public static class MyGadgetValueMap extends HashMap<String, Gadget> { }
    public static class MyGadgetKeyMap extends HashMap<Gadget, String> { }
    public static class MyGadgetRef extends AtomicReference<Gadget> { }
    public static class MyGadgetListList extends ArrayList<MyGadgetList> { }
    public static class MyGadgetMapList extends ArrayList<HashMap<String, Gadget>> { }
    public static class MyGadgetArrayList extends ArrayList<Gadget[]> { }
    public static class MyGadgetArrayListList extends ArrayList<ArrayList<Gadget>> { }

    public static class MyAnimalList extends ArrayList<Animal> { }
    public static class MyDogList extends ArrayList<Dog> { }
    public static class MyDogListSub extends MyDogList { }
    public static class MyDogListList extends ArrayList<List<Dog>> { }
    public static class MyDogArrayListList extends ArrayList<ArrayList<Dog>> { }
    // Content from type variable bound to declared type
    public static class MyBoundedListList<T extends Animal> extends ArrayList<List<T>> { }
    // Generic container type, given type argument by enclosing class declaration
    public static class MyGenList<T> extends ArrayList<List<T>> { }
    public static class MyGenGadgetHolder extends ArrayList<MyGenList<Gadget>> { }

    // Safe (and well-known value) content types
    public static class MyStringList extends ArrayList<String> { }
    public static class MyDateList extends ArrayList<Date> { }
    public static class MyLocaleMap extends HashMap<Locale, Currency> { }
    public static class MyNodeList extends ArrayList<JsonNode> { }
    public static class MyUriList extends ArrayList<java.net.URI> { }
    public static class MyFileList extends ArrayList<java.io.File> { }
    public static class MyPathList extends ArrayList<java.nio.file.Path> { }
    public static class MyTimestampList extends ArrayList<java.sql.Timestamp> { }
    public static class MyCharSequenceList extends ArrayList<CharSequence> { }
    public static class MyLocalDateList extends ArrayList<java.time.LocalDate> { }
    // ... but not types in sub-packages of `java.time`, nor `Serializable`
    public static class MyFormatterList extends ArrayList<java.time.format.DateTimeFormatter> { }
    public static class MySerializableList extends ArrayList<java.io.Serializable> { }

    // Recursive types: must not cause infinite recursion
    public static class MyNode extends ArrayList<MyNode> { }
    public static class MyPing extends ArrayList<MyPong> { }
    public static class MyPong extends ArrayList<MyPing> { }

    // Content type wraps type parameter (like Spring `LinkedMultiValueMap`)
    public static class MyMultiMap<K, V> extends HashMap<K, List<V>> { }

    // Polymorphic base type other than `Object`
    @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS)
    public interface Payload { }
    public static class Batch extends ArrayList<List<Item>> implements Payload { }
    public static class DogPayload extends ArrayList<Dog> implements Payload { }
    public static class PayloadEnv<T> implements Payload {
        public T payload;
    }

    // With Default Typing (`OBJECT_AND_NON_CONCRETE`), values of these properties are
    // typed (but not elements of concrete types like `Animal`)
    static class Wrapper<T> { public T value; }
    static class ObjectWrapper extends Wrapper<Object> { }
    static class AnimalsWrapper extends Wrapper<List<Animal>> { }
    static class DogsWrapper extends Wrapper<List<Dog>> { }
    static class AnimalIterableWrapper extends Wrapper<Iterable<Animal>> { }
    static class AnimalListsWrapper extends Wrapper<Collection<List<Animal>>> { }
    static class DogListsWrapper extends Wrapper<List<List<Dog>>> { }
    static class PayloadWrapper extends Wrapper<Payload> { }

    /*
    /**********************************************************************
    /* Test methods
    /**********************************************************************
     */

    @Test
    public void contentTypesRejected() throws Exception
    {
        ObjectMapper mapper = containerMapper();
        for (Object typeId : Arrays.asList(MyGadgetList.class,
                MyHiddenGadgetList.class.getName() + "<java.lang.String>",
                MyHiddenGadgetMapList.class.getName() + "<java.lang.String>",
                MyNestedGadgetList.class, MyGadgetValueMap.class, MyGadgetKeyMap.class,
                MyGadgetRef.class, MyGadgetListList.class, MyGadgetMapList.class,
                MyGadgetArrayList.class, MyGadgetArrayListList.class,
                MyFormatterList.class, MySerializableList.class,
                // as type parameter
                "java.util.ArrayList<" + MyGadgetList.class.getName() + ">",
                // as element type of explicitly approved array type
                MyGadgetList[].class)) {
            _rejected(mapper, ObjectWrapper.class, typeId);
        }
        // Subtype of declared content type not approved as such
        _rejected(mapper, AnimalsWrapper.class, MyDogList.class);
        // Declared non-container type (`Iterable<Animal>`) does not cover anything
        _rejected(mapper, AnimalIterableWrapper.class, MyGadgetList.class);
    }

    @Test
    public void contentTypesAccepted() throws Exception
    {
        ObjectMapper mapper = containerMapper();
        for (Object typeId : Arrays.asList(MyStringList.class, ArrayList.class,
                MyDateList.class, MyNodeList.class, MyUriList.class, MyFileList.class,
                MyPathList.class, MyTimestampList.class, MyCharSequenceList.class,
                MyLocalDateList.class, MyNode.class, MyPing.class, MyStringList[].class,
                "java.util.ArrayList<java.lang.String>")) {
            _accepted(mapper, ObjectWrapper.class, typeId, "[]");
        }
        _accepted(mapper, ObjectWrapper.class, HashMap.class, "{}");
        _accepted(mapper, ObjectWrapper.class, MyLocaleMap.class, "{}");
        // Same as declared content type
        _accepted(mapper, AnimalsWrapper.class, MyAnimalList.class, "[]");
        _accepted(mapper, AnimalsWrapper.class, ArrayList.class, "[]");
        _accepted(mapper, DogsWrapper.class, MyDogList.class, "[]");
        // From type variables (bound by declared type), not class declaration
        _accepted(mapper, DogListsWrapper.class, MyBoundedListList.class, "[]");
        _accepted(mapper, AnimalIterableWrapper.class, ArrayList.class, "[]");
    }

    // Approval of base type covers subtypes of declared content types (also nested),
    // nothing else
    @Test
    public void approvedBaseType() throws Exception
    {
        ObjectMapper mapper = mapper(BasicPolymorphicTypeValidator.builder()
                .allowIfBaseType(Collection.class));
        _accepted(mapper, AnimalsWrapper.class, MyDogList.class, "[]");
        _accepted(mapper, AnimalListsWrapper.class, MyDogListList.class, "[]");
        _accepted(mapper, AnimalListsWrapper.class, MyDogArrayListList.class, "[]");
        _rejected(mapper, AnimalsWrapper.class, MyGadgetList.class);
    }

    @Test
    public void approvedContentType() throws Exception
    {
        // As subtype of declared content type, approved as base type
        ObjectMapper mapper = mapper(BasicPolymorphicTypeValidator.builder()
                .allowIfSubType(Collection.class)
                .allowIfBaseType(Animal.class));
        _accepted(mapper, AnimalsWrapper.class, MyDogList.class, "[]");
        // ... which does not cover other types (type erasure)
        _rejected(mapper, AnimalsWrapper.class, MyGadgetList.class);

        // As subtype
        mapper = mapper(BasicPolymorphicTypeValidator.builder()
                .allowIfSubType(Collection.class)
                .allowIfSubType(Animal.class));
        _accepted(mapper, ObjectWrapper.class, MyDogList.class, "[]");
    }

    // Explicit approval of exact container class covers content types it declares,
    // but not for subclasses, nor type arguments given to it
    @Test
    public void explicitlyApprovedContainerType() throws Exception
    {
        ObjectMapper mapper = mapper(BasicPolymorphicTypeValidator.builder()
                .allowIfSubType(Collection.class)
                .allowIfSubType(MyDogList.class)
                .allowIfSubType(MyGenList.class));
        _accepted(mapper, ObjectWrapper.class, MyDogList.class, "[]");
        _accepted(mapper, ObjectWrapper.class,
                "java.util.ArrayList<" + MyDogList.class.getName() + ">", "[]");
        _rejected(mapper, ObjectWrapper.class, MyDogListSub.class);
        _rejected(mapper, ObjectWrapper.class, MyGenGadgetHolder.class);
    }

    // Validators that deny `Object` as base type (like default
    // `DefaultBaseTypeLimitingValidator`) do not deny all content
    @Test
    public void objectDeniedAsBaseType() throws Exception
    {
        ObjectMapper mapper = newJsonMapper();
        _accepted(mapper, PayloadWrapper.class, Batch.class, "[]");
        _accepted(mapper, PayloadWrapper.class,
                PayloadEnv.class.getName() + "<" + DogPayload.class.getName() + ">", "{}");

        mapper = jsonMapperBuilder()
                .polymorphicTypeValidator(BasicPolymorphicTypeValidator.builder()
                        .denyForExactBaseType(Object.class)
                        .allowIfSubType(Collection.class)
                        .allowIfSubType(Dog.class)
                        .build())
                .build();
        _accepted(mapper, PayloadWrapper.class, DogPayload.class, "[]");
    }

    // Declared element type of declared array type is declared type of array elements
    @Test
    public void declaredContentOfArrayElements() throws Exception
    {
        ObjectMapper mapper = newJsonMapper();
        DeserializationContext ctxt = mapper._deserializationContext();
        JavaType arrayType = mapper.constructType(MyDogList[].class);
        for (BasicPolymorphicTypeValidator.Builder b : Arrays.asList(
                // array type explicitly approved
                BasicPolymorphicTypeValidator.builder().allowIfSubType(MyDogList[].class),
                // or not
                BasicPolymorphicTypeValidator.builder().allowIfSubTypeIsArray()
                        .allowIfSubType(Collection.class))) {
            assertEquals(arrayType, ctxt.resolveAndValidateSubType(arrayType,
                    MyDogList[].class.getName(), b.build()));
        }
    }

    // Validation must take linear (not exponential) time wrt nesting depth
    @Test
    public void deeplyNestedTypeIdValidatedFast() throws Exception
    {
        ObjectMapper mapper = containerMapper();
        for (Class<?> mapType : Arrays.asList(HashMap.class, MyMultiMap.class)) {
            StringBuilder sb = new StringBuilder();
            final int levels = 40;
            for (int i = 0; i < levels; ++i) {
                sb.append(mapType.getName()).append("<java.lang.String,");
            }
            sb.append("java.lang.String");
            for (int i = 0; i < levels; ++i) {
                sb.append('>');
            }
            assertTimeoutPreemptively(java.time.Duration.ofSeconds(5),
                    () -> _accepted(mapper, ObjectWrapper.class, sb.toString(), "{}"),
                    mapType.getName());
        }
    }

    // Description of value type (for error messages) is constructed when needed,
    // since constructing it for every level of nested types takes quadratic time
    @Test
    public void valueInstantiatorTypeDesc() throws Exception
    {
        ObjectMapper mapper = newJsonMapper();
        JavaType type = mapper.getTypeFactory().constructFromCanonical(
                MyMultiMap.class.getName() + "<java.lang.String,java.lang.Integer>");
        StdValueInstantiator inst = new StdValueInstantiator(mapper.deserializationConfig(), type);
        assertEquals(type.toString(), inst.getValueTypeDesc());
        assertEquals(type.toString(), new StdValueInstantiator(inst) { }.getValueTypeDesc());
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

    // Type id as Class or String
    private static String json(Object typeId, String value) {
        String id = (typeId instanceof Class<?>) ? ((Class<?>) typeId).getName() : (String) typeId;
        return a2q("{'value':['" + id + "'," + value + "]}");
    }

    private void _accepted(ObjectMapper mapper, Class<? extends Wrapper<?>> wrapperType,
            Object typeId, String value) throws Exception
    {
        Object result = mapper.readValue(json(typeId, value), wrapperType).value;
        assertNotNull(result, String.valueOf(typeId));
        if (typeId instanceof Class<?>) {
            assertEquals(typeId, result.getClass());
        }
    }

    // (type id is validated before value is read)
    private void _rejected(ObjectMapper mapper, Class<? extends Wrapper<?>> wrapperType,
            Object typeId)
    {
        InvalidTypeIdException e = assertThrows(InvalidTypeIdException.class,
                () -> mapper.readValue(json(typeId, "null"), wrapperType),
                String.valueOf(typeId));
        verifyException(e, "denied resolution of contained type");
    }
}
