package tools.jackson.databind.objectid;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.*;

import tools.jackson.databind.*;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Passing tests related to [databind#5872]: Object identity
 * with type info and polymorphism. These variants work; the failing
 * variants (with builder) are in {@code tofix/ObjectIdWithTypeId5872Test}.
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

}
