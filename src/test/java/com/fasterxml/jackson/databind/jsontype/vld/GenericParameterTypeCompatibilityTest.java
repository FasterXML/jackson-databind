package com.fasterxml.jackson.databind.jsontype.vld;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.InvalidTypeIdException;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.databind.jsontype.PolymorphicTypeValidator;
import com.fasterxml.jackson.databind.testutil.DatabindTestUtil;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.fasterxml.jackson.databind.util.LookupCache;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class GenericParameterTypeCompatibilityTest extends DatabindTestUtil
{
    @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS)
    abstract static class ClassIdMixin { }

    static class StringMapHolder {
        public Map<String, String> value;
    }

    static class ObjectMapHolder {
        public Map<Object, Object> value;
    }

    static class IntegerMapHolder {
        public Map<String, Integer> value;
    }

    static class NestedMapHolder {
        public List<Map<String, Integer>> value;
    }

    static class NestedListHolder {
        public List<List<String>> value;
    }

    static class ArrayBindingHolder {
        public List<String[]> value;
    }

    static class RecursiveListHolder {
        public List<RecursiveList<String>> value;
    }

    static class SupplierHolder {
        public Supplier<Object> value;
    }

    static class AnimalListHolder {
        public List<Animal> value;
    }

    public static class Animal {
        public String name;
    }

    public static class Dog extends Animal { }

    static class ObjectHolder {
        @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS,
                include = JsonTypeInfo.As.WRAPPER_ARRAY)
        public Object value;
    }

    public static class SwappedMap<A, B> extends HashMap<B, A> {
        private static final long serialVersionUID = 1L;
    }

    public static class FixedValueMap<T> extends HashMap<String, T> {
        private static final long serialVersionUID = 1L;
    }

    public static class ExtraParameterMap<K, V, X> extends HashMap<K, V> {
        private static final long serialVersionUID = 1L;
    }

    public static class NestedBindingList<T> extends ArrayList<List<T>> {
        private static final long serialVersionUID = 1L;
    }

    public static class LongList extends ArrayList<Long> {
        private static final long serialVersionUID = 1L;
    }

    public static class ArrayBindingList<T> extends ArrayList<T[]> {
        private static final long serialVersionUID = 1L;
    }

    public static class ArrayBindingWithExtra<T, X> extends ArrayList<T[]> {
        private static final long serialVersionUID = 1L;
    }

    public static class RecursiveList<T> extends ArrayList<RecursiveList<T>> {
        private static final long serialVersionUID = 1L;
    }

    public static class SelfSupplier<T> implements Supplier<SelfSupplier<T>> {
        public T payload;

        @Override
        public SelfSupplier<T> get() {
            return this;
        }
    }

    // Names its own class with a fixed argument: not a self-reference, but
    // `TypeFactory` resolves it as one unless `FixedStringSupplier<String>`
    // is already in its cache.
    public static class FixedStringSupplier<T> implements Supplier<FixedStringSupplier<String>> {
        public T payload;

        @Override
        public FixedStringSupplier<String> get() {
            return null;
        }
    }

    public static class InternalKey { }

    public static class KeyedList<T> extends ArrayList<InternalKey> {
        private static final long serialVersionUID = 1L;
    }

    static class FixedStringSupplierHolder {
        public Supplier<FixedStringSupplier<String>> value;
    }

    static class ObjectListHolder {
        public List<Object> value;
    }

    // Type cache that lets a test evict one entry, which `LRUMap` cannot
    static class EvictableTypeCache implements LookupCache<Object, JavaType> {
        private final Map<Object, JavaType> _map = new HashMap<>();

        @Override
        public int size() { return _map.size(); }

        @Override
        public JavaType get(Object key) { return _map.get(key); }

        @Override
        public JavaType put(Object key, JavaType value) { return _map.put(key, value); }

        @Override
        public JavaType putIfAbsent(Object key, JavaType value) { return _map.putIfAbsent(key, value); }

        @Override
        public void clear() { _map.clear(); }

        @Override
        public void contents(BiConsumer<Object, JavaType> consumer) { _map.forEach(consumer); }

        @Override
        public LookupCache<Object, JavaType> emptyCopy() { return new EvictableTypeCache(); }

        void evict(String canonical) {
            _map.values().removeIf(type -> canonical.equals(type.toCanonical()));
        }
    }

    public static class IncompatibleKey {
        static int constructions;

        public IncompatibleKey(String value) {
            ++constructions;
        }
    }

    public static class GenericGadget {
        static int constructions;

        public GenericGadget() {
            ++constructions;
        }

        public String value;
    }

    private ObjectMapper mapperWithClassIdMixin(Class<?> target) {
        return jsonMapperBuilder()
                .addMixIn(target, ClassIdMixin.class)
                .build();
    }

    private ObjectMapper mapperAllowingObjectBase() {
        BasicPolymorphicTypeValidator ptv = BasicPolymorphicTypeValidator.builder()
                .allowIfBaseType(Object.class)
                .build();
        return jsonMapperBuilder()
                .polymorphicTypeValidator(ptv)
                .build();
    }

    @Test
    public void canonicalTypeArgumentMustMatchDeclaredMapKey() throws Exception
    {
        ObjectMapper mapper = mapperWithClassIdMixin(Map.class);
        String typeId = "java.util.HashMap<" + IncompatibleKey.class.getName()
                + ",java.lang.String>";
        String json = "{\"value\":[\"" + typeId + "\",{\"key\":\"value\"}]}";

        IncompatibleKey.constructions = 0;
        assertThrows(InvalidTypeIdException.class,
                () -> mapper.readValue(json, StringMapHolder.class));
        assertEquals(0, IncompatibleKey.constructions);
    }

    @Test
    public void allowedOuterBaseDoesNotApproveGenericArguments() throws Exception
    {
        BasicPolymorphicTypeValidator ptv = BasicPolymorphicTypeValidator.builder()
                .allowIfBaseType(Map.class)
                .build();
        ObjectMapper mapper = jsonMapperBuilder()
                .addMixIn(Map.class, ClassIdMixin.class)
                .polymorphicTypeValidator(ptv)
                .build();
        String typeId = "java.util.HashMap<java.lang.Object,"
                + GenericGadget.class.getName() + ">";
        String json = "{\"value\":[\"" + typeId
                + "\",{\"key\":{\"value\":\"attacker-controlled\"}}]}";

        GenericGadget.constructions = 0;
        InvalidTypeIdException e = assertThrows(InvalidTypeIdException.class,
                () -> mapper.readValue(json, ObjectMapHolder.class));
        assertEquals(0, GenericGadget.constructions);
        assertEquals(typeId, e.getTypeId());
        verifyException(e, "as a subtype of `java.util.Map<java.lang.Object,java.lang.Object>`");
        verifyException(e, "denied resolution of type parameter `"
                + GenericGadget.class.getName() + "` (declared as `java.lang.Object`)");
    }

    @Test
    public void incompatibleTypeArgumentMessageNamesPolymorphicBaseAndTypeId() throws Exception
    {
        ObjectMapper mapper = mapperWithClassIdMixin(Map.class);
        String typeId = "java.util.HashMap<java.lang.String,java.lang.Long>";
        String json = "{\"value\":[\"" + typeId + "\",{\"key\":3}]}";

        InvalidTypeIdException e = assertThrows(InvalidTypeIdException.class,
                () -> mapper.readValue(json, IntegerMapHolder.class));
        assertEquals(typeId, e.getTypeId());
        assertEquals(Map.class, e.getBaseType().getRawClass());
        verifyException(e, "Could not resolve type id '" + typeId
                + "' as a subtype of `java.util.Map<java.lang.String,java.lang.Integer>`");
        verifyException(e, "type parameter `java.lang.Long` (declared as `java.lang.Integer`)"
                + " is not a subtype of its declared type");
    }

    @Test
    public void reorderedSubtypeArgumentsUseProjectedBaseBindings() throws Exception
    {
        ObjectMapper mapper = mapperWithClassIdMixin(Map.class);
        String typeId = SwappedMap.class.getName()
                + "<java.lang.Integer,java.lang.String>";
        String json = "{\"value\":[\"" + typeId + "\",{\"key\":3}]}";

        IntegerMapHolder result = mapper.readValue(json, IntegerMapHolder.class);
        assertEquals(Integer.valueOf(3), result.value.get("key"));
    }

    @Test
    public void nestedSubtypeArgumentsUseProjectedBaseBindings() throws Exception
    {
        ObjectMapper mapper = mapperWithClassIdMixin(List.class);
        String nestedType = SwappedMap.class.getName()
                + "<java.lang.Integer,java.lang.String>";
        String typeId = ArrayList.class.getName() + "<" + nestedType + ">";
        String json = "{\"value\":[\"" + typeId + "\",[{\"key\":3}]]}";

        NestedMapHolder result = mapper.readValue(json, NestedMapHolder.class);
        assertEquals(Integer.valueOf(3), result.value.get(0).get("key"));
    }

    @Test
    public void nestedInheritedBindingIsNotValidatedTwice() throws Exception
    {
        ObjectMapper mapper = mapperWithClassIdMixin(List.class);
        String typeId = NestedBindingList.class.getName() + "<java.lang.String>";
        String json = "{\"value\":[\"" + typeId + "\",[]]}";

        NestedListHolder result = mapper.readValue(json, NestedListHolder.class);
        assertEquals(NestedBindingList.class, result.value.getClass());
    }

    @Test
    public void nestedInheritedBindingMismatchRejected() throws Exception
    {
        ObjectMapper mapper = mapperWithClassIdMixin(List.class);
        String typeId = NestedBindingList.class.getName() + "<java.lang.Integer>";
        String json = "{\"value\":[\"" + typeId + "\",[]]}";

        assertThrows(InvalidTypeIdException.class,
                () -> mapper.readValue(json, NestedListHolder.class));
    }

    // A non-generic argument type still has to match the declared parameter
    // through the type arguments its supertype fixes: `LongList` is a `List<Long>`.
    @Test
    public void nonGenericArgumentWithFixedBindingMismatchRejected() throws Exception
    {
        ObjectMapper mapper = mapperWithClassIdMixin(List.class);
        String typeId = "java.util.ArrayList<" + LongList.class.getName() + ">";
        String json = "{\"value\":[\"" + typeId + "\",[]]}";

        assertThrows(InvalidTypeIdException.class,
                () -> mapper.readValue(json, NestedListHolder.class));
    }

    // The declared parameter type is the base type the validator sees, so
    // `allowIfBaseType(Animal.class)` covers `Dog` as an argument even though
    // no rule names `Dog` itself.
    @Test
    public void allowedDeclaredParameterBaseAcceptsSubtypeArgument() throws Exception
    {
        BasicPolymorphicTypeValidator ptv = BasicPolymorphicTypeValidator.builder()
                .allowIfSubType(ArrayList.class)
                .allowIfBaseType(Animal.class)
                .build();
        ObjectMapper mapper = jsonMapperBuilder()
                .addMixIn(List.class, ClassIdMixin.class)
                .polymorphicTypeValidator(ptv)
                .build();
        String typeId = ArrayList.class.getName() + "<" + Dog.class.getName() + ">";
        String json = "{\"value\":[\"" + typeId + "\",[{\"name\":\"Rex\"}]]}";

        AnimalListHolder result = mapper.readValue(json, AnimalListHolder.class);
        assertEquals(Dog.class, result.value.get(0).getClass());
        assertEquals("Rex", result.value.get(0).name);
    }

    @Test
    public void canonicalInterfaceTypeWithObjectBaseAccepted() throws Exception
    {
        ObjectMapper mapper = mapperAllowingObjectBase();
        String json = "{\"value\":[\"java.util.List<java.lang.String>\",[\"value\"]]}";

        ObjectHolder result = mapper.readValue(json, ObjectHolder.class);
        assertEquals("value", ((List<?>) result.value).get(0));
    }

    @Test
    public void rawGenericTypeArgumentAccepted() throws Exception
    {
        ObjectMapper mapper = mapperAllowingObjectBase();
        String json = "{\"value\":[\"java.util.ArrayList<java.util.List>\",[]]}";

        ObjectHolder result = mapper.readValue(json, ObjectHolder.class);
        assertEquals(ArrayList.class, result.value.getClass());
    }

    @Test
    public void inheritedGenericArrayBindingAccepted() throws Exception
    {
        BasicPolymorphicTypeValidator ptv = BasicPolymorphicTypeValidator.builder()
                .allowIfSubType(Object.class)
                .build();
        ObjectMapper mapper = jsonMapperBuilder()
                .addMixIn(List.class, ClassIdMixin.class)
                .polymorphicTypeValidator(ptv)
                .build();
        String typeId = ArrayBindingList.class.getName() + "<java.lang.String>";
        String json = "{\"value\":[\"" + typeId + "\",[[\"value\"]]]}";

        ArrayBindingHolder result = mapper.readValue(json, ArrayBindingHolder.class);
        assertEquals("value", result.value.get(0)[0]);
    }

    @Test
    public void inheritedGenericArrayMismatchRejected() throws Exception
    {
        ObjectMapper mapper = mapperWithClassIdMixin(List.class);
        String typeId = ArrayBindingList.class.getName() + "<java.lang.Integer>";
        String json = "{\"value\":[\"" + typeId + "\",[]]}";

        assertThrows(InvalidTypeIdException.class,
                () -> mapper.readValue(json, ArrayBindingHolder.class));
    }

    @Test
    public void recursiveGenericBindingAccepted() throws Exception
    {
        BasicPolymorphicTypeValidator ptv = BasicPolymorphicTypeValidator.builder()
                .allowIfBaseType(List.class)
                .allowIfBaseType(String.class)
                .build();
        ObjectMapper mapper = jsonMapperBuilder()
                .addMixIn(List.class, ClassIdMixin.class)
                .polymorphicTypeValidator(ptv)
                .build();
        String typeId = RecursiveList.class.getName() + "<java.lang.String>";
        String json = "{\"value\":[\"" + typeId + "\",[]]}";

        RecursiveListHolder result = mapper.readValue(json, RecursiveListHolder.class);
        assertEquals(RecursiveList.class, result.value.getClass());
    }

    @Test
    public void recursiveGenericBindingMismatchRejected() throws Exception
    {
        ObjectMapper mapper = mapperWithClassIdMixin(List.class);
        String typeId = RecursiveList.class.getName() + "<java.lang.Integer>";
        String json = "{\"value\":[\"" + typeId + "\",[]]}";

        assertThrows(InvalidTypeIdException.class,
                () -> mapper.readValue(json, RecursiveListHolder.class));
    }

    // The projection of `SelfSupplier<X>` onto `Supplier` is `Supplier<SelfSupplier<X>>`,
    // where the argument is a self-reference: `X` must still be validated through it.
    @Test
    public void selfReferencingBindingDoesNotHideSubtypeArgument() throws Exception
    {
        BasicPolymorphicTypeValidator ptv = BasicPolymorphicTypeValidator.builder()
                .allowIfSubType(SelfSupplier.class)
                .build();
        ObjectMapper mapper = jsonMapperBuilder()
                .addMixIn(Supplier.class, ClassIdMixin.class)
                .polymorphicTypeValidator(ptv)
                .build();
        String typeId = SelfSupplier.class.getName() + "<" + GenericGadget.class.getName() + ">";
        String json = "{\"value\":[\"" + typeId + "\",{\"payload\":{\"value\":\"x\"}}]}";

        GenericGadget.constructions = 0;
        assertThrows(InvalidTypeIdException.class,
                () -> mapper.readValue(json, SupplierHolder.class));
        assertEquals(0, GenericGadget.constructions);
    }

    @Test
    public void arrayBindingDoesNotHideSubtypeOnlyArgument() throws Exception
    {
        BasicPolymorphicTypeValidator ptv = BasicPolymorphicTypeValidator.builder()
                .allowIfBaseType(String[].class)
                .allowIfBaseType(String.class)
                .allowIfSubType(ArrayBindingWithExtra.class)
                .build();
        ObjectMapper mapper = jsonMapperBuilder()
                .addMixIn(List.class, ClassIdMixin.class)
                .polymorphicTypeValidator(ptv)
                .build();
        String typeId = ArrayBindingWithExtra.class.getName()
                + "<java.lang.String," + GenericGadget.class.getName() + ">";
        String json = "{\"value\":[\"" + typeId + "\",[]]}";

        GenericGadget.constructions = 0;
        assertThrows(InvalidTypeIdException.class,
                () -> mapper.readValue(json, ArrayBindingHolder.class));
        assertEquals(0, GenericGadget.constructions);
    }

    @Test
    public void primitiveArrayTypeParameterAccepted() throws Exception
    {
        ObjectMapper mapper = mapperAllowingObjectBase();
        String json = "{\"value\":[\"java.util.ArrayList<[I>\",[[1,2]]]}";

        ObjectHolder result = mapper.readValue(json, ObjectHolder.class);
        int[] values = (int[]) ((List<?>) result.value).get(0);
        assertEquals(1, values[0]);
        assertEquals(2, values[1]);
    }

    @Test
    public void fixedSubtypeBindingMustMatchDeclaredBase() throws Exception
    {
        ObjectMapper mapper = mapperWithClassIdMixin(Map.class);
        String validTypeId = FixedValueMap.class.getName() + "<java.lang.String>";
        String validJson = "{\"value\":[\"" + validTypeId
                + "\",{\"key\":\"value\"}]}";
        assertEquals("value", mapper.readValue(validJson,
                StringMapHolder.class).value.get("key"));

        String invalidTypeId = FixedValueMap.class.getName() + "<"
                + GenericGadget.class.getName() + ">";
        String invalidJson = "{\"value\":[\"" + invalidTypeId + "\",{}]}";
        assertThrows(InvalidTypeIdException.class,
                () -> mapper.readValue(invalidJson, StringMapHolder.class));
    }

    @Test
    public void subtypeOnlyGenericArgumentIsStillValidated() throws Exception
    {
        BasicPolymorphicTypeValidator ptv = BasicPolymorphicTypeValidator.builder()
                .allowIfBaseType(String.class)
                .allowIfSubType(ExtraParameterMap.class)
                .build();
        ObjectMapper mapper = jsonMapperBuilder()
                .addMixIn(Map.class, ClassIdMixin.class)
                .polymorphicTypeValidator(ptv)
                .build();
        String typeId = ExtraParameterMap.class.getName()
                + "<java.lang.String,java.lang.String,"
                + GenericGadget.class.getName() + ">";
        String json = "{\"value\":[\"" + typeId + "\",{}]}";

        assertThrows(InvalidTypeIdException.class,
                () -> mapper.readValue(json, StringMapHolder.class));
    }

    // `FixedStringSupplier<Gadget>` resolves to `Supplier<FixedStringSupplier<String>>`
    // while `FixedStringSupplier<String>` is cached, and to a self-reference
    // otherwise. The check must find `Gadget` either way.
    @Test
    public void typeCacheStateDoesNotHideSubtypeArgument() throws Exception
    {
        EvictableTypeCache cache = new EvictableTypeCache();
        TypeFactory typeFactory = TypeFactory.defaultInstance().withCache(cache);
        BasicPolymorphicTypeValidator ptv = BasicPolymorphicTypeValidator.builder()
                .allowIfSubType(FixedStringSupplier.class)
                .allowIfSubType(String.class)
                .build();
        ObjectMapper mapper = jsonMapperBuilder()
                .typeFactory(typeFactory)
                .addMixIn(Supplier.class, ClassIdMixin.class)
                .polymorphicTypeValidator(ptv)
                .build();
        String fixedTypeId = FixedStringSupplier.class.getName() + "<java.lang.String>";
        String typeId = FixedStringSupplier.class.getName()
                + "<" + GenericGadget.class.getName() + ">";
        String json = "{\"value\":[\"" + typeId + "\",{\"payload\":{\"value\":\"x\"}}]}";

        // Resolve the type id while the fixed argument type is cached, then evict it
        typeFactory.constructFromCanonical(fixedTypeId);
        typeFactory.constructFromCanonical(typeId);
        cache.evict(fixedTypeId);

        GenericGadget.constructions = 0;
        assertThrows(InvalidTypeIdException.class,
                () -> mapper.readValue(json, SupplierHolder.class));
        assertEquals(0, GenericGadget.constructions);
    }

    // `InternalKey` comes from the declaration of `KeyedList`, not from the
    // type id, so it needs no entry in the allow-list: the plain id
    // `KeyedList` does not need one either.
    @Test
    public void argumentFixedBySubtypeDeclarationNeedsNoAllowListEntry() throws Exception
    {
        BasicPolymorphicTypeValidator ptv = BasicPolymorphicTypeValidator.builder()
                .allowIfSubType(KeyedList.class)
                .allowIfSubType(String.class)
                .build();
        ObjectMapper mapper = jsonMapperBuilder()
                .addMixIn(List.class, ClassIdMixin.class)
                .polymorphicTypeValidator(ptv)
                .build();
        String typeId = KeyedList.class.getName() + "<java.lang.String>";
        String json = "{\"value\":[\"" + typeId + "\",[]]}";

        ObjectListHolder result = mapper.readValue(json, ObjectListHolder.class);
        assertEquals(KeyedList.class, result.value.getClass());
    }
}
