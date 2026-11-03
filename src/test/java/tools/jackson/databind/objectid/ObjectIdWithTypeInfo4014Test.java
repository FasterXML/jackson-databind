package tools.jackson.databind.objectid;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.*;

import tools.jackson.databind.*;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test for [databind#4014]: combining {@code @JsonTypeInfo(include = As.PROPERTY)}
 * with {@code @JsonIdentityInfo(generator = PropertyGenerator.class)} on an interface
 * should work for deserialization.
 */
public class ObjectIdWithTypeInfo4014Test extends DatabindTestUtil
{
    @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "@c")
    @JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "@id")
    public interface BaseEntity {
        @JsonProperty("@id")
        Integer getId();
    }

    // Immutable classes: constructor-only @id, no setter
    static class Foo implements BaseEntity {
        private final Integer id;
        private Bar bar;

        @JsonCreator
        public Foo(@JsonProperty("@id") Integer id) {
            this.id = id;
        }

        @Override @JsonProperty("@id") public Integer getId() { return id; }
        public Bar getBar() { return bar; }
        public void setBar(Bar bar) { this.bar = bar; }
    }

    static class Bar implements BaseEntity {
        private final Integer id;
        private Foo foo;

        @JsonCreator
        public Bar(@JsonProperty("@id") Integer id) {
            this.id = id;
        }

        @Override @JsonProperty("@id") public Integer getId() { return id; }
        public Foo getFoo() { return foo; }
        public void setFoo(Foo foo) { this.foo = foo; }
    }

    // Mutable classes: default constructor + setters
    static class FooWithSetter implements BaseEntity {
        private Integer id;
        private BarWithSetter bar;

        public FooWithSetter() { }

        @Override @JsonProperty("@id") public Integer getId() { return id; }
        @JsonProperty("@id") public void setId(Integer id) { this.id = id; }
        public BarWithSetter getBar() { return bar; }
        public void setBar(BarWithSetter bar) { this.bar = bar; }
    }

    static class BarWithSetter implements BaseEntity {
        private Integer id;
        private FooWithSetter foo;

        public BarWithSetter() { }

        @Override @JsonProperty("@id") public Integer getId() { return id; }
        @JsonProperty("@id") public void setId(Integer id) { this.id = id; }
        public FooWithSetter getFoo() { return foo; }
        public void setFoo(FooWithSetter foo) { this.foo = foo; }
    }

    // Wrapper-array variants: exercises AsArrayTypeDeserializer fix
    @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.WRAPPER_ARRAY, property = "@c")
    @JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "@id")
    public interface WrapperArrayEntity {
        @JsonProperty("@id")
        Integer getId();
    }

    static class WrapFoo implements WrapperArrayEntity {
        private final Integer id;
        private WrapBar bar;

        @JsonCreator
        public WrapFoo(@JsonProperty("@id") Integer id) { this.id = id; }

        @Override @JsonProperty("@id") public Integer getId() { return id; }
        public WrapBar getBar() { return bar; }
        public void setBar(WrapBar bar) { this.bar = bar; }
    }

    static class WrapBar implements WrapperArrayEntity {
        private final Integer id;
        private WrapFoo foo;

        @JsonCreator
        public WrapBar(@JsonProperty("@id") Integer id) { this.id = id; }

        @Override @JsonProperty("@id") public Integer getId() { return id; }
        public WrapFoo getFoo() { return foo; }
        public void setFoo(WrapFoo foo) { this.foo = foo; }
    }

    // // // [databind#5872]: ObjectId with type info, interface/class base type, +/- builder

    static class Container5872 {
        @JsonProperty
        public List<Base5872> list;
    }

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME)
    @JsonSubTypes({@JsonSubTypes.Type(name = "Derived", value = Derived5872.class)})
    @JsonIdentityInfo(generator = ObjectIdGenerators.StringIdGenerator.class)
    interface Base5872 {
    }

    static class Derived5872 implements Base5872 {
        @JsonProperty
        public String a;
    }

    static class ContainerWithClass5872 {
        @JsonProperty
        public List<BaseClass5872> list;
    }

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME)
    @JsonSubTypes({@JsonSubTypes.Type(name = "DerivedFromClass", value = DerivedFromClass5872.class)})
    @JsonIdentityInfo(generator = ObjectIdGenerators.StringIdGenerator.class)
    static class BaseClass5872 {
    }

    static class DerivedFromClass5872 extends BaseClass5872 {
        @JsonProperty
        public String a;
    }

    static class ContainerInterfaceWithBuilder5872 {
        @JsonProperty
        public List<BaseWithBuilder5872> list;
    }

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME)
    @JsonSubTypes({@JsonSubTypes.Type(name = "DerivedWithBuilder", value = DerivedWithBuilder5872.class)})
    @JsonIdentityInfo(generator = ObjectIdGenerators.StringIdGenerator.class,
            resolver = SimpleObjectIdResolver.class)
    interface BaseWithBuilder5872 {
    }

    @tools.jackson.databind.annotation.JsonDeserialize(builder = DerivedWithBuilder5872.DerivedBuilder.class)
    static class DerivedWithBuilder5872 implements BaseWithBuilder5872 {
        @JsonProperty
        public String a;

        DerivedWithBuilder5872(String a) {
            this.a = a;
        }

        @tools.jackson.databind.annotation.JsonPOJOBuilder(withPrefix = "", buildMethodName = "build")
        public static class DerivedBuilder {
            private String a;

            @JsonProperty
            public DerivedBuilder a(String a) {
                this.a = a;
                return this;
            }

            public DerivedWithBuilder5872 build() {
                return new DerivedWithBuilder5872(this.a);
            }
        }
    }

    static class ContainerClassWithBuilder5872 {
        @JsonProperty
        public List<BaseClassWithBuilder5872> list;
    }

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME)
    @JsonSubTypes({@JsonSubTypes.Type(name = "DerivedClassBuilder", value = DerivedClassWithBuilder5872.class)})
    @JsonIdentityInfo(generator = ObjectIdGenerators.StringIdGenerator.class,
            resolver = SimpleObjectIdResolver.class)
    static class BaseClassWithBuilder5872 {
    }

    @tools.jackson.databind.annotation.JsonDeserialize(builder = DerivedClassWithBuilder5872.DerivedBuilder.class)
    static class DerivedClassWithBuilder5872 extends BaseClassWithBuilder5872 {
        @JsonProperty
        public String a;

        DerivedClassWithBuilder5872(String a) {
            this.a = a;
        }

        @tools.jackson.databind.annotation.JsonPOJOBuilder(withPrefix = "", buildMethodName = "build")
        public static class DerivedBuilder {
            private String a;

            @JsonProperty
            public DerivedBuilder a(String a) {
                this.a = a;
                return this;
            }

            public DerivedClassWithBuilder5872 build() {
                return new DerivedClassWithBuilder5872(this.a);
            }
        }
    }

    private final ObjectMapper MAPPER = newJsonMapper();

    // [databind#4014]: simple round-trip via interface (setter-based)
    @Test
    public void testSimpleDeserializationViaInterface() throws Exception
    {
        FooWithSetter foo = new FooWithSetter();
        foo.setId(1);
        String json = MAPPER.writeValueAsString(foo);
        BaseEntity result = MAPPER.readValue(json, BaseEntity.class);
        assertNotNull(result);
        assertInstanceOf(FooWithSetter.class, result);
        assertEquals(1, ((FooWithSetter) result).getId());
    }

    // [databind#4014]: simple round-trip via interface (constructor-based)
    @Test
    public void testSimpleDeserializationWithCreator() throws Exception
    {
        Foo foo = new Foo(1);
        String json = MAPPER.writeValueAsString(foo);
        BaseEntity result = MAPPER.readValue(json, BaseEntity.class);
        assertNotNull(result);
        assertInstanceOf(Foo.class, result);
        assertEquals(1, ((Foo) result).getId());
    }

    // [databind#4014]: circular references with setter-based classes
    @Test
    public void testCircularReferenceWithSetters() throws Exception
    {
        FooWithSetter foo = new FooWithSetter();
        foo.setId(1);
        BarWithSetter bar = new BarWithSetter();
        bar.setId(2);
        foo.setBar(bar);
        bar.setFoo(foo);

        String json = MAPPER.writeValueAsString(foo);

        BaseEntity result = MAPPER.readValue(json, BaseEntity.class);
        assertNotNull(result);
        assertInstanceOf(FooWithSetter.class, result);

        FooWithSetter resultFoo = (FooWithSetter) result;
        assertEquals(1, resultFoo.getId());
        assertNotNull(resultFoo.getBar());
        assertEquals(2, resultFoo.getBar().getId());
        assertSame(resultFoo, resultFoo.getBar().getFoo());
    }

    // [databind#4014]: circular references with constructor-based classes
    @Test
    public void testCircularReferenceWithCreator() throws Exception
    {
        Foo foo = new Foo(1);
        Bar bar = new Bar(2);
        foo.setBar(bar);
        bar.setFoo(foo);

        String json = MAPPER.writeValueAsString(foo);

        BaseEntity result = MAPPER.readValue(json, BaseEntity.class);
        assertNotNull(result);
        assertInstanceOf(Foo.class, result);

        Foo resultFoo = (Foo) result;
        assertEquals(1, resultFoo.getId());
        assertNotNull(resultFoo.getBar());
        assertEquals(2, resultFoo.getBar().getId());
        assertSame(resultFoo, resultFoo.getBar().getFoo());
    }

    // [databind#4014]: simple round-trip with WRAPPER_ARRAY
    @Test
    public void testSimpleDeserializationWrapperArray() throws Exception
    {
        WrapFoo foo = new WrapFoo(1);
        String json = MAPPER.writeValueAsString(foo);
        WrapperArrayEntity result = MAPPER.readValue(json, WrapperArrayEntity.class);
        assertNotNull(result);
        assertInstanceOf(WrapFoo.class, result);
        assertEquals(1, ((WrapFoo) result).getId());
    }

    // [databind#4014]: circular references with WRAPPER_ARRAY
    @Test
    public void testCircularReferenceWrapperArray() throws Exception
    {
        WrapFoo foo = new WrapFoo(1);
        WrapBar bar = new WrapBar(2);
        foo.setBar(bar);
        bar.setFoo(foo);

        String json = MAPPER.writeValueAsString(foo);

        WrapperArrayEntity result = MAPPER.readValue(json, WrapperArrayEntity.class);
        assertNotNull(result);
        assertInstanceOf(WrapFoo.class, result);

        WrapFoo resultFoo = (WrapFoo) result;
        assertEquals(1, resultFoo.getId());
        assertNotNull(resultFoo.getBar());
        assertEquals(2, resultFoo.getBar().getId());
        assertSame(resultFoo, resultFoo.getBar().getFoo());
    }

    /*
    /**********************************************************
    /* Unit tests, ObjectId + type info + interface/class base [databind#5872]
    /**********************************************************
     */

    // [databind#5872]: ObjectId with interface base type (no builder)
    @Test
    public void testObjectIdWithInterfaceBaseType5872() throws Exception
    {
        String json = a2q("{'list':["
                + "{'@type':'Derived','@id':'id1','a':'foo'},"
                + "'id1'"
                + "]}");
        Container5872 container = MAPPER.readValue(json, Container5872.class);
        assertNotNull(container);
        assertNotNull(container.list);
        assertEquals(2, container.list.size());

        Base5872 first = container.list.get(0);
        assertEquals(Derived5872.class, first.getClass());
        assertEquals("foo", ((Derived5872) first).a);

        Base5872 second = container.list.get(1);
        assertSame(first, second);
    }

    // [databind#5872]: ObjectId with class base type (no builder)
    @Test
    public void testObjectIdWithClassBaseType5872() throws Exception
    {
        String json = a2q("{'list':["
                + "{'@type':'DerivedFromClass','@id':'id1','a':'foo'},"
                + "'id1'"
                + "]}");
        ContainerWithClass5872 container = MAPPER.readValue(json, ContainerWithClass5872.class);
        assertNotNull(container);
        assertNotNull(container.list);
        assertEquals(2, container.list.size());

        BaseClass5872 first = container.list.get(0);
        assertEquals(DerivedFromClass5872.class, first.getClass());
        assertEquals("foo", ((DerivedFromClass5872) first).a);

        BaseClass5872 second = container.list.get(1);
        assertSame(first, second);
    }

    // [databind#5872]: ObjectId with interface base type + builder
    @Test
    public void testObjectIdWithInterfaceAndBuilder5872() throws Exception
    {
        String json = a2q("{'list':["
                + "{'@type':'DerivedWithBuilder','@id':'id1','a':'foo'},"
                + "'id1'"
                + "]}");
        ContainerInterfaceWithBuilder5872 container = MAPPER.readValue(json, ContainerInterfaceWithBuilder5872.class);
        assertNotNull(container);
        assertNotNull(container.list);
        assertEquals(2, container.list.size());

        BaseWithBuilder5872 first = container.list.get(0);
        assertEquals(DerivedWithBuilder5872.class, first.getClass());
        assertEquals("foo", ((DerivedWithBuilder5872) first).a);

        BaseWithBuilder5872 second = container.list.get(1);
        assertSame(first, second);
    }

    // [databind#5872]: ObjectId with class base type + builder
    @Test
    public void testObjectIdWithClassAndBuilder5872() throws Exception
    {
        String json = a2q("{'list':["
                + "{'@type':'DerivedClassBuilder','@id':'id1','a':'foo'},"
                + "'id1'"
                + "]}");
        ContainerClassWithBuilder5872 container = MAPPER.readValue(json, ContainerClassWithBuilder5872.class);
        assertNotNull(container);
        assertNotNull(container.list);
        assertEquals(2, container.list.size());

        BaseClassWithBuilder5872 first = container.list.get(0);
        assertEquals(DerivedClassWithBuilder5872.class, first.getClass());
        assertEquals("foo", ((DerivedClassWithBuilder5872) first).a);

        BaseClassWithBuilder5872 second = container.list.get(1);
        assertSame(first, second);
    }
}
