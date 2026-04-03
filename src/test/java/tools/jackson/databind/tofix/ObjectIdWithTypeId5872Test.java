package tools.jackson.databind.tofix;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.*;

import tools.jackson.databind.*;
import tools.jackson.databind.testutil.DatabindTestUtil;
import tools.jackson.databind.testutil.failure.JacksonTestFailureExpected;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test for [databind#5872]: Object identity does not work when
 * combining builder-based deserialization + polymorphism + type info.
 *<p>
 * NOTE: this test class contains still-failing variants; there is a
 * separate class, {@link tools.jackson.databind.objectid.ObjectIdWithTypeId5872Test},
 * for the passing variants (no builder).
 */
class ObjectIdWithTypeId5872Test extends DatabindTestUtil
{
    // Interface base type + builder (fails)
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

    // Class base type + builder (also fails)
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

    // [databind#5872]: interface + builder
    @JacksonTestFailureExpected
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

    // [databind#5872]: class + builder
    @JacksonTestFailureExpected
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
