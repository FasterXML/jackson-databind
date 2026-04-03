package tools.jackson.databind.objectid;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.*;

import tools.jackson.databind.*;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for [databind#5872]: Object identity with type info, polymorphism,
 * and builder-based deserialization.
 */
class ObjectIdWithTypeId5872Test extends DatabindTestUtil
{
    // Interface base type (no builder)
    static class Container {
        @JsonProperty
        public List<Base> list;
    }

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME)
    @JsonSubTypes({@JsonSubTypes.Type(name = "Derived", value = Derived.class)})
    @JsonIdentityInfo(generator = ObjectIdGenerators.StringIdGenerator.class)
    interface Base {
    }

    static class Derived implements Base {
        @JsonProperty
        public String a;
    }

    // Class base type (no builder)
    static class ContainerWithClass {
        @JsonProperty
        public List<BaseClass> list;
    }

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME)
    @JsonSubTypes({@JsonSubTypes.Type(name = "DerivedFromClass", value = DerivedFromClass.class)})
    @JsonIdentityInfo(generator = ObjectIdGenerators.StringIdGenerator.class)
    static class BaseClass {
    }

    static class DerivedFromClass extends BaseClass {
        @JsonProperty
        public String a;
    }

    // Interface base type + builder
    static class ContainerInterfaceWithBuilder {
        @JsonProperty
        public List<BaseWithBuilder> list;
    }

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME)
    @JsonSubTypes({@JsonSubTypes.Type(name = "DerivedWithBuilder", value = DerivedWithBuilder.class)})
    @JsonIdentityInfo(generator = ObjectIdGenerators.StringIdGenerator.class,
            resolver = SimpleObjectIdResolver.class)
    interface BaseWithBuilder {
    }

    @tools.jackson.databind.annotation.JsonDeserialize(builder = DerivedWithBuilder.DerivedBuilder.class)
    static class DerivedWithBuilder implements BaseWithBuilder {
        @JsonProperty
        public String a;

        DerivedWithBuilder(String a) {
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

            public DerivedWithBuilder build() {
                return new DerivedWithBuilder(this.a);
            }
        }
    }

    // Class base type + builder
    static class ContainerClassWithBuilder {
        @JsonProperty
        public List<BaseClassWithBuilder> list;
    }

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME)
    @JsonSubTypes({@JsonSubTypes.Type(name = "DerivedClassBuilder", value = DerivedClassWithBuilder.class)})
    @JsonIdentityInfo(generator = ObjectIdGenerators.StringIdGenerator.class,
            resolver = SimpleObjectIdResolver.class)
    static class BaseClassWithBuilder {
    }

    @tools.jackson.databind.annotation.JsonDeserialize(builder = DerivedClassWithBuilder.DerivedBuilder.class)
    static class DerivedClassWithBuilder extends BaseClassWithBuilder {
        @JsonProperty
        public String a;

        DerivedClassWithBuilder(String a) {
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

            public DerivedClassWithBuilder build() {
                return new DerivedClassWithBuilder(this.a);
            }
        }
    }

    private final ObjectMapper MAPPER = newJsonMapper();

    // [databind#5872]: ObjectId with interface base type (no builder)
    @Test
    public void testObjectIdWithInterfaceBaseType() throws Exception
    {
        String json = a2q("{'list':["
                + "{'@type':'Derived','@id':'id1','a':'foo'},"
                + "'id1'"
                + "]}");
        Container container = MAPPER.readValue(json, Container.class);
        assertNotNull(container);
        assertNotNull(container.list);
        assertEquals(2, container.list.size());

        Base first = container.list.get(0);
        assertEquals(Derived.class, first.getClass());
        assertEquals("foo", ((Derived) first).a);

        Base second = container.list.get(1);
        assertSame(first, second);
    }

    // [databind#5872]: ObjectId with class base type (no builder)
    @Test
    public void testObjectIdWithClassBaseType() throws Exception
    {
        String json = a2q("{'list':["
                + "{'@type':'DerivedFromClass','@id':'id1','a':'foo'},"
                + "'id1'"
                + "]}");
        ContainerWithClass container = MAPPER.readValue(json, ContainerWithClass.class);
        assertNotNull(container);
        assertNotNull(container.list);
        assertEquals(2, container.list.size());

        BaseClass first = container.list.get(0);
        assertEquals(DerivedFromClass.class, first.getClass());
        assertEquals("foo", ((DerivedFromClass) first).a);

        BaseClass second = container.list.get(1);
        assertSame(first, second);
    }

    // [databind#5872]: ObjectId with interface base type + builder
    @Test
    public void testObjectIdWithInterfaceAndBuilder() throws Exception
    {
        String json = a2q("{'list':["
                + "{'@type':'DerivedWithBuilder','@id':'id1','a':'foo'},"
                + "'id1'"
                + "]}");
        ContainerInterfaceWithBuilder container = MAPPER.readValue(json, ContainerInterfaceWithBuilder.class);
        assertNotNull(container);
        assertNotNull(container.list);
        assertEquals(2, container.list.size());

        BaseWithBuilder first = container.list.get(0);
        assertEquals(DerivedWithBuilder.class, first.getClass());
        assertEquals("foo", ((DerivedWithBuilder) first).a);

        BaseWithBuilder second = container.list.get(1);
        assertSame(first, second);
    }

    // [databind#5872]: ObjectId with class base type + builder
    @Test
    public void testObjectIdWithClassAndBuilder() throws Exception
    {
        String json = a2q("{'list':["
                + "{'@type':'DerivedClassBuilder','@id':'id1','a':'foo'},"
                + "'id1'"
                + "]}");
        ContainerClassWithBuilder container = MAPPER.readValue(json, ContainerClassWithBuilder.class);
        assertNotNull(container);
        assertNotNull(container.list);
        assertEquals(2, container.list.size());

        BaseClassWithBuilder first = container.list.get(0);
        assertEquals(DerivedClassWithBuilder.class, first.getClass());
        assertEquals("foo", ((DerivedClassWithBuilder) first).a);

        BaseClassWithBuilder second = container.list.get(1);
        assertSame(first, second);
    }
}
