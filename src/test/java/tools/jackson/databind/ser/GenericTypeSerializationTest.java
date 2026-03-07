package tools.jackson.databind.ser;

import java.io.StringWriter;
import java.util.*;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.annotation.JsonTypeInfo.As;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.*;
import tools.jackson.databind.annotation.JsonDeserialize;
import tools.jackson.databind.annotation.JsonSerialize;
import tools.jackson.databind.exc.InvalidDefinitionException;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.*;

public class GenericTypeSerializationTest extends DatabindTestUtil
{
    static class Account {
        private Long id;
        private String name;

        @JsonCreator
        public Account(
                @JsonProperty("name") String name,
                @JsonProperty("id") Long id) {
            this.id = id;
            this.name = name;
        }

        public String getName() { return name; }
        public Long getId() { return id; }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Account account = (Account) o;
            return Objects.equals(id, account.id) && Objects.equals(name, account.name);
        }

        @Override
        public int hashCode() {
            return Objects.hash(id, name);
        }
    }

    static class Key<T> {
        private final T id;

        public Key(T id) { this.id = id; }

        public T getId() { return id; }

        public <V> Key<V> getParent() { return null; }
    }

    static class Person1 {
        private Long id;
        private String name;
        private Key<Account> account;

        public Person1(String name) { this.name = name; }

        public String getName() {
            return name;
        }

        public Key<Account> getAccount() {
            return account;
        }

        public Long getId() {
            return id;
        }

        public void setAccount(Key<Account> account) {
            this.account = account;
        }
    }

    static class Person2 {
        private Long id;
        private String name;
        private List<Key<Account>> accounts;

        public Person2(String name) {
                this.name = name;
        }

        public String getName() { return name; }
        public List<Key<Account>> getAccounts() { return accounts; }
        public Long getId() { return id; }

        public void setAccounts(List<Key<Account>> accounts) {
            this.accounts = accounts;
        }
    }

    static class GenericBogusWrapper<T> {
        public Element wrapped;

        public GenericBogusWrapper(T v) { wrapped = new Element(v); }

        class Element {
            public T value;

            public Element(T v) { value = v; }
        }
    }

    // For [databind#728]
    static class Base727 {
        public int a;
    }

    @JsonPropertyOrder(alphabetic=true)
    static class Impl727 extends Base727 {
        public int b;

        public Impl727(int a, int b) {
            this.a = a;
            this.b = b;
        }
    }

    // For [databind#2821]
    static final class Wrapper2821 {
        // if Entity<?> -> Entity , the test passes
        final List<Entity2821<?>> entities;

        @JsonCreator
        public Wrapper2821(List<Entity2821<?>> entities) {
            this.entities = entities;
        }

        public List<Entity2821<?>> getEntities() {
            return this.entities;
        }
    }

    static class Entity2821<T> {
        @JsonIgnore
        final Attributes2821 attributes;

        final T data;

        public Entity2821(Attributes2821 attributes, T data) {
            this.attributes = attributes;
            this.data = data;
        }

        @JsonUnwrapped
        public Attributes2821 getAttributes() {
            return attributes;
        }

        public T getData() {
            return data;
        }

        @JsonCreator
        public static <T> Entity2821<T> create(@JsonProperty("attributes") Attributes2821 attributes,
                @JsonProperty("data") T data) {
            return new Entity2821<>(attributes, data);
        }
    }

    public static class Attributes2821 {
        public final String id;

        public Attributes2821(String id) {
            this.id = id;
        }

        @JsonCreator
        public static Attributes2821 create(@JsonProperty("id") String id) {
            return new Attributes2821(id);
        }

        // if this method is removed, the test passes
        @SuppressWarnings("rawtypes")
        public static Attributes2821 dummyMethod(Map attributes) {
            return null;
        }
    }

    @JsonSerialize(as = GenericWrapperImpl.class)
    @JsonDeserialize(as = GenericWrapperImpl.class)
    public interface GenericWrapper<A, AA> {
        A first();
        AA second();
    }

    public static final class GenericWrapperImpl<B, BB> implements GenericWrapper<B, BB> {

        private final B first;
        private final BB second;

        GenericWrapperImpl(B first, BB second) {
            this.first = first;
            this.second = second;
        }

        @Override
        public B first() {
            return first;
        }

        @Override
        public BB second() {
            return second;
        }

        @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
        // Invert the type parameter order to make things exciting!
        public static <C, CC> GenericWrapperImpl<CC, C> fromJson(JsonGenericWrapper<CC, C> val) {
            return new GenericWrapperImpl<>(val.first(), val.second());
        }
    }

    @JsonDeserialize
    @JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.NONE)
    public static final class JsonGenericWrapper<D, DD> implements GenericWrapper<D, DD> {

        @JsonProperty("first")
        private D first;

        @JsonProperty("second")
        private DD second;

        @Override
        @JsonProperty("first")
        public D first() {
            return first;
        }

        @Override
        @JsonProperty("second")
        public DD second() {
            return second;
        }
    }

    public static final class GenericSpecificityWrapper0<E, EE> {

        private final E first;
        private final EE second;

        GenericSpecificityWrapper0(E first, EE second) {
            this.first = first;
            this.second = second;
        }

        public E first() {
            return first;
        }

        public EE second() {
            return second;
        }

        @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
        public static <F> GenericSpecificityWrapper0<?, F> fromJson(JsonGenericWrapper<Long, F> val) {
            return new GenericSpecificityWrapper0<>(val.first(), val.second());
        }
    }

    public static final class GenericSpecificityWrapper1<E, EE> {

        private final E first;
        private final EE second;

        GenericSpecificityWrapper1(E first, EE second) {
            this.first = first;
            this.second = second;
        }

        public E first() {
            return first;
        }

        public EE second() {
            return second;
        }

        @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
        public static <F extends StringStubSubclass, FF> GenericSpecificityWrapper1<F, FF> fromJson(JsonGenericWrapper<F, FF> val) {
            return new GenericSpecificityWrapper1<>(val.first(), val.second());
        }
    }

    public static class StringStub {
        final String value;

        StringStub(String value) {
            this.value = value;
        }

        @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
        public static StringStub valueOf(String value) {
            return new StringStub(value);
        }
    }

    public static class StringStubSubclass extends StringStub {

        private StringStubSubclass(String value) {
            super(value);
        }

        @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
        public static StringStubSubclass valueOf(String value) {
            return new StringStubSubclass(value);
        }
    }

    public static final class GenericSpecificityWrapper2<E, EE> {

        private final E first;
        private final EE second;

        GenericSpecificityWrapper2(E first, EE second) {
            this.first = first;
            this.second = second;
        }

        public E first() {
            return first;
        }

        public EE second() {
            return second;
        }

        @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
        public static <F extends Stub<StringStubSubclass>, FF> GenericSpecificityWrapper2<F, FF> fromJson(JsonGenericWrapper<F, FF> val) {
            return new GenericSpecificityWrapper2<>(val.first(), val.second());
        }
    }

    public static class Stub<T> {
        final T value;

        private Stub(T value) {
            this.value = value;
        }

        @JsonCreator
        public static <T> Stub<T> valueOf(T value) {
            return new Stub<>(value);
        }
    }

    public static final class WildcardWrapperImpl<G, GG> {

        private final G first;
        private final GG second;

        WildcardWrapperImpl(G first, GG second) {
            this.first = first;
            this.second = second;
        }

        public G first() {
            return first;
        }

        public GG second() {
            return second;
        }

        @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
        public static <H, HH> WildcardWrapperImpl<H, ? extends HH> fromJson(JsonGenericWrapper<H, HH> val) {
            return new WildcardWrapperImpl<>(val.first(), val.second());
        }
    }

    public static class SimpleWrapper<T>
    {
        final T value;

        SimpleWrapper(T value) {
            this.value = value;
        }

        @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
        public static <T> SimpleWrapper<T> fromJson(JsonSimpleWrapper<T> value) {
            return new SimpleWrapper<>(value.object);
        }
    }

    @JsonDeserialize
    public static final class JsonSimpleWrapper<T>
    {
        @JsonProperty("object")
        public T object;
    }

    interface Indexed<T> {
        T index();
    }

    public static class TestIndexed implements Indexed<String> {
        final UUID value;

        @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
        TestIndexed(UUID value) {
            this.value = value;
        }

        @Override
        public String index() {
            return value.toString();
        }
    }

    public static final class IndexedList<T extends Indexed<K>, K> extends AbstractList<T> {

        final ArrayList<T> delegate;

        private IndexedList(ArrayList<T> delegate) {
            this.delegate = delegate;
        }

        @Override
        public T get(int index) {
            return delegate.get(index);
        }

        @Override
        public int size() {
            return delegate.size();
        }

        @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
        public static <T extends Indexed<K>, K> IndexedList<T, K> fromJson(Iterable<? extends T> values) {
            ArrayList<T> arrayList = new ArrayList<>();
            for (T value : values) {
                arrayList.add(value);
            }
            return new IndexedList<>(arrayList);
        }
    }

    // From TestRootType

    interface BaseInterface {
        int getB();
    }

    static class BaseType
        implements BaseInterface
    {
        public String a = "a";

        @Override
        public int getB() { return 3; }
    }

    static class SubType extends BaseType {
        public String a2 = "x";

        public boolean getB2() { return true; }
    }

    @JsonTypeInfo(use=Id.NAME, include=As.PROPERTY, property="beanClass")
    public abstract static class BaseClass398 { }

    public static class TestClass398 extends BaseClass398 {
       public String property = "aa";
    }

    @JsonRootName("root")
    static class WithRootName {
        public int a = 3;
    }

    // [databind#412]
    @JsonPropertyOrder({ "uuid", "type" })
    static class TestCommandParent {
        public String uuid;
        public int type;
    }

    static interface Issue822Interface {
        public int getA();
    }

    static class Issue822Impl implements Issue822Interface {
        @Override
        public int getA() { return 3; }
        public int getB() { return 9; }
    }

    static class TestCommandChild extends TestCommandParent { }

    /*
    /**********************************************************
    /* Unit tests
    /**********************************************************
     */

    private final ObjectMapper MAPPER = newJsonMapper();

    private final ObjectMapper VANILLA_MAPPER = sharedMapper();

    private final ObjectMapper WRAP_ROOT_MAPPER = jsonMapperBuilder()
            .enable(SerializationFeature.WRAP_ROOT_VALUE)
            .build();

    @SuppressWarnings("unchecked")
    @Test
    public void testIssue468a() throws Exception
    {
        Person1 p1 = new Person1("John");
        p1.setAccount(new Key<Account>(new Account("something", 42L)));

        // First: ensure we can serialize (pre 1.7 this failed)
        String json = MAPPER.writeValueAsString(p1);

        // and then verify that results make sense
        Map<String,Object> map = MAPPER.readValue(json, Map.class);
        assertEquals("John", map.get("name"));
        Object ob = map.get("account");
        assertNotNull(ob);
        Map<String,Object> acct = (Map<String,Object>) ob;
        Object idOb = acct.get("id");
        assertNotNull(idOb);
        Map<String,Object> key = (Map<String,Object>) idOb;
        assertEquals("something", key.get("name"));
        assertEquals(Integer.valueOf(42), key.get("id"));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testIssue468b() throws Exception
    {
        Person2 p2 = new Person2("John");
        List<Key<Account>> accounts = new ArrayList<Key<Account>>();
        accounts.add(new Key<Account>(new Account("a", 42L)));
        accounts.add(new Key<Account>(new Account("b", 43L)));
        accounts.add(new Key<Account>(new Account("c", 44L)));
        p2.setAccounts(accounts);

        // serialize without error:
        String json = MAPPER.writeValueAsString(p2);

        // then verify output
        Map<String,Object> map = MAPPER.readValue(json, Map.class);
        assertEquals("John", map.get("name"));
        Object ob = map.get("accounts");
        assertNotNull(ob);
        List<?> acctList = (List<?>) ob;
        assertEquals(3, acctList.size());
        // ... might want to verify more, but for now that should suffice
    }

    /**
     * Test related to unbound type variables, usually resulting
     * from inner classes of generic classes (like Sets).
     */
    @Test
    public void testUnboundTypes() throws Exception
    {
        GenericBogusWrapper<Integer> list = new GenericBogusWrapper<Integer>(Integer.valueOf(7));
        String json = MAPPER.writeValueAsString(list);
        assertEquals("{\"wrapped\":{\"value\":7}}", json);
    }

    @Test
    public void testRootTypeForCollections727() throws Exception
    {
        List<Base727> input = new ArrayList<Base727>();
        input.add(new Impl727(1, 2));

        final String EXP = a2q("[{'a':1,'b':2}]");
        // Without type enforcement, produces expected output:
        assertEquals(EXP, MAPPER.writeValueAsString(input));
        assertEquals(EXP, MAPPER.writer().writeValueAsString(input));

        // but enforcing type will hinder:
        TypeReference<?> typeRef = new TypeReference<List<Base727>>() { };
        assertEquals(EXP, MAPPER.writer().forType(typeRef).writeValueAsString(input));
    }

    // For [databind#2821]
    @SuppressWarnings("unchecked")
    @Test
    public void testTypeResolution2821() throws Exception
    {
        Entity2821<String> entity = new Entity2821<>(new Attributes2821("id"), "hello");
        List<Entity2821<?>> list;
        {
            List<Entity2821<String>> foo = new ArrayList<>();
            foo.add(entity);
            list = (List<Entity2821<?>>) (List<?>) foo;
        }
        Wrapper2821 val = new Wrapper2821(list);
        String json = MAPPER.writeValueAsString(val);
        assertNotNull(json);
    }

    @Test
    public void testStaticDelegateDeserialization() throws Exception
    {
        GenericWrapper<Account, String> wrapper = MAPPER.readValue(
                "{\"first\":{\"id\":1,\"name\":\"name\"},\"second\":\"str\"}",
                new TypeReference<GenericWrapper<Account, String>>() {});
        Account account = wrapper.first();
        assertEquals(new Account("name", 1L), account);
        String second = wrapper.second();
        assertEquals("str", second);
    }

    @Test
    public void testStaticDelegateDeserialization_factoryProvidesSpecificity0() throws Exception
    {
        GenericSpecificityWrapper0<Object, Account> wrapper = MAPPER.readValue(
                "{\"first\":\"1\",\"second\":{\"id\":1,\"name\":\"name\"}}",
                new TypeReference<GenericSpecificityWrapper0<Object, Account>>() {});
        Object first = wrapper.first();
        assertEquals(Long.valueOf(1L), first);
        Account second = wrapper.second();
        assertEquals(new Account("name", 1L), second);
    }

    @Test
    public void testStaticDelegateDeserialization_factoryProvidesSpecificity1() throws Exception
    {
        GenericSpecificityWrapper1<StringStub, Account> wrapper = MAPPER.readValue(
                "{\"first\":\"1\",\"second\":{\"id\":1,\"name\":\"name\"}}",
                new TypeReference<GenericSpecificityWrapper1<StringStub, Account>>() {});
        StringStub first = wrapper.first();
        assertEquals("1", first.value);
        Account second = wrapper.second();
        assertEquals(new Account("name", 1L), second);
    }

    @Test
    public void testStaticDelegateDeserialization_factoryProvidesSpecificity2() throws Exception
    {
        GenericSpecificityWrapper2<Stub<Object>, Account> wrapper = MAPPER.readValue(
                "{\"first\":\"1\",\"second\":{\"id\":1,\"name\":\"name\"}}",
                new TypeReference<GenericSpecificityWrapper2<Stub<Object>, Account>>() {});
        Stub<Object> first = wrapper.first();
        StringStub stringStub = (StringStub) first.value;
        assertEquals("1", stringStub.value);
        Account second = wrapper.second();
        assertEquals(new Account("name", 1L), second);
    }

    @Test
    public void testStaticDelegateDeserialization_wildcardInResult() throws Exception
    {
        WildcardWrapperImpl<Account, Account> wrapper = MAPPER.readValue(
                "{\"first\":{\"id\":1,\"name\":\"name1\"},\"second\":{\"id\":2,\"name\":\"name2\"}}",
                new TypeReference<WildcardWrapperImpl<Account, Account>>() {});
        Account account1 = wrapper.first();
        assertEquals(new Account("name1", 1L), account1);
        Account account2 = wrapper.second();
        assertEquals(new Account("name2", 2L), account2);
    }

    @Test
    public void testSimpleStaticJsonCreator() throws Exception
    {
        SimpleWrapper<Account> wrapper = MAPPER.readValue("{\"object\":{\"id\":1,\"name\":\"name1\"}}",
                new TypeReference<SimpleWrapper<Account>>() {});
        Account account = wrapper.value;
        assertEquals(new Account("name1", 1L), account);
    }

    @Test
    public void testIndexedListExample() throws Exception
    {
        UUID uuid = UUID.randomUUID();
        IndexedList<TestIndexed, String> value = MAPPER.readValue(String.format("[\"%s\"]", uuid.toString()),
                new TypeReference<IndexedList<TestIndexed, String>>() {});
        assertEquals(1, value.size());
        assertEquals(uuid, value.delegate.get(0).value);
    }

    // From TestRootType

    @SuppressWarnings("unchecked")
    @Test
    public void testSuperClass() throws Exception
    {
        SubType bean = new SubType();

        // first, test with dynamically detected type
        Map<String,Object> result = writeAndMap(VANILLA_MAPPER, bean);
        assertEquals(4, result.size());
        assertEquals("a", result.get("a"));
        assertEquals(Integer.valueOf(3), result.get("b"));
        assertEquals("x", result.get("a2"));
        assertEquals(Boolean.TRUE, result.get("b2"));

        // and then using specified typed writer
        ObjectWriter w = VANILLA_MAPPER.writerFor(BaseType.class);
        String json = w.writeValueAsString(bean);
        result = (Map<String,Object>)VANILLA_MAPPER.readValue(json, Map.class);
        assertEquals(2, result.size());
        assertEquals("a", result.get("a"));
        assertEquals(Integer.valueOf(3), result.get("b"));
    }

    @Test
    public void testSuperInterface() throws Exception
    {
        SubType bean = new SubType();

        // let's constrain by interface:
        ObjectWriter w = VANILLA_MAPPER.writerFor(BaseInterface.class);
        String json = w.writeValueAsString(bean);
        @SuppressWarnings("unchecked")
        Map<String,Object> result = VANILLA_MAPPER.readValue(json, Map.class);
        assertEquals(1, result.size());
        assertEquals(Integer.valueOf(3), result.get("b"));
    }

    @Test
    public void testInArray() throws Exception
    {
        // must force static typing, otherwise won't matter a lot
        ObjectMapper mapper = jsonMapperBuilder()
                .enable(MapperFeature.USE_STATIC_TYPING)
                .build();
        SubType[] ob = new SubType[] { new SubType() };
        String json = mapper.writerFor(BaseInterface[].class).writeValueAsString(ob);
        // should propagate interface type through due to root declaration; static typing
        assertEquals("[{\"b\":3}]", json);
    }

    /**
     * Unit test to ensure that proper exception is thrown if declared
     * root type is not compatible with given value instance.
     */
    @Test
    public void testIncompatibleRootType() throws Exception
    {
        SubType bean = new SubType();

        // and then let's try using incompatible type
        ObjectWriter w = VANILLA_MAPPER.writerFor(HashMap.class);
        try {
            w.writeValueAsString(bean);
            fail("Should have failed due to incompatible type");
        } catch (InvalidDefinitionException e) {
            verifyException(e, "Incompatible types");
        }

        // and also with alternate output method
        try {
            w.writeValueAsBytes(bean);
            fail("Should have failed due to incompatible type");
        } catch (InvalidDefinitionException e) {
            verifyException(e, "Incompatible types");
        }
    }

    @Test
    public void testJackson398() throws Exception
    {
        JavaType collectionType = defaultTypeFactory().constructCollectionType(ArrayList.class, BaseClass398.class);
        List<TestClass398> typedList = new ArrayList<TestClass398>();
        typedList.add(new TestClass398());

        final String EXP = "[{\"beanClass\":\"GenericTypeSerializationTest$TestClass398\",\"property\":\"aa\"}]";

        // First simplest way:
        String json = VANILLA_MAPPER.writerFor(collectionType).writeValueAsString(typedList);
        assertEquals(EXP, json);

        StringWriter out = new StringWriter();
        VANILLA_MAPPER.writerFor(collectionType)
            .writeValue(VANILLA_MAPPER.createGenerator(out), typedList);

        assertEquals(EXP, out.toString());
    }

    // [JACKSON-163]
    @Test
    public void testRootWrapping() throws Exception
    {
        String json = WRAP_ROOT_MAPPER.writeValueAsString(new StringWrapper("abc"));
        assertEquals("{\"StringWrapper\":{\"str\":\"abc\"}}", json);
    }

    @Test
    public void testIssue456WrapperPart() throws Exception
    {
        assertEquals("123", VANILLA_MAPPER.writerFor(Integer.TYPE).writeValueAsString(Integer.valueOf(123)));
        assertEquals("456", VANILLA_MAPPER.writerFor(Long.TYPE).writeValueAsString(Long.valueOf(456L)));
    }

    @Test
    public void testRootNameAnnotation() throws Exception
    {
        String json = WRAP_ROOT_MAPPER.writeValueAsString(new WithRootName());
        assertEquals("{\"root\":{\"a\":3}}", json);
    }

    // [databind#412]
    @Test
    public void testRootNameWithExplicitType() throws Exception
    {
        TestCommandChild cmd = new TestCommandChild();
        cmd.uuid = "1234";
        cmd.type = 1;

        ObjectWriter writer = WRAP_ROOT_MAPPER.writerFor(TestCommandParent.class);
        String json =  writer.writeValueAsString(cmd);

        assertEquals("{\"TestCommandParent\":{\"uuid\":\"1234\",\"type\":1}}", json);
    }

    // First ensure that basic interface-override works:
    @Test
    public void testTypedSerialization() throws Exception
    {
        String singleJson = VANILLA_MAPPER.writerFor(Issue822Interface.class)
                .writeValueAsString(new Issue822Impl());
        // start with specific value case:
        assertEquals("{\"a\":3}", singleJson);
    }

    @Test
    public void testTypedRootArrays() throws Exception
    {
        assertEquals("[{\"a\":3}]", VANILLA_MAPPER.writerFor(Issue822Interface[].class).writeValueAsString(
                new Issue822Interface[] { new Issue822Impl() }));
    }

    @Test
    public void testTypedRootLists() throws Exception
    {
        List<Issue822Interface> list = new ArrayList<Issue822Interface>();
        list.add(new Issue822Impl());
        String listJson = VANILLA_MAPPER.writerFor(new TypeReference<List<Issue822Interface>>(){})
                .writeValueAsString(list);
        assertEquals("[{\"a\":3}]", listJson);
    }

    @Test
    public void testTypedRootMaps() throws Exception
    {
        Map<String,Issue822Interface> map = new HashMap<String,Issue822Interface>();
        map.put("a", new Issue822Impl());
        String listJson = VANILLA_MAPPER
                .writerFor(new TypeReference<Map<String,Issue822Interface>>(){})
                .writeValueAsString(map);
        assertEquals("{\"a\":{\"a\":3}}", listJson);
    }
}
