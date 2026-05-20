package tools.jackson.databind.jsontype.ext;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.*;

import tools.jackson.databind.MapperFeature;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.ObjectReader;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.*;

// [databind#5958]: When a POJO uses a property-based @JsonCreator together with
// an EXTERNAL_PROPERTY type id whose `visible=true` settable property is
// restricted by @JsonView, the deserializer must skip that type-id property in
// views where it is not visible (instead of forwarding the type id into the
// external type handler).
public class ExternalTypeIdView5958Test extends DatabindTestUtil
{
    static class Views {
        static class Public { }
        static class Internal { }
    }

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME)
    @JsonSubTypes({
            @JsonSubTypes.Type(value = Dog.class, name = "dog"),
            @JsonSubTypes.Type(value = Cat.class, name = "cat")
    })
    static abstract class Animal {
        public String name;
    }

    @JsonTypeName("dog")
    static class Dog extends Animal { }

    @JsonTypeName("cat")
    static class Cat extends Animal { }

    // Default impl used when the type id is filtered out by view processing.
    @JsonTypeName("unknown")
    static class UnknownAnimal extends Animal { }

    static class Container {
        // External type id, exposed (`visible=true`) as a settable property,
        // restricted to the Internal view.
        @JsonView(Views.Internal.class)
        public String petType;

        @JsonTypeInfo(use = JsonTypeInfo.Id.NAME,
                include = JsonTypeInfo.As.EXTERNAL_PROPERTY,
                property = "petType",
                visible = true,
                defaultImpl = UnknownAnimal.class)
        @JsonSubTypes({
                @JsonSubTypes.Type(value = Dog.class, name = "dog"),
                @JsonSubTypes.Type(value = Cat.class, name = "cat")
        })
        @JsonView(Views.Internal.class)
        public Animal pet;

        public String label;

        // Property-based creator forces the
        // deserializeUsingPropertyBasedWithExternalTypeId code path.
        @JsonCreator
        public Container(@JsonProperty("label") String label) {
            this.label = label;
        }
    }

    private final ObjectMapper MAPPER = jsonMapperBuilder()
            // Properties without a @JsonView are visible in every view; ones
            // with @JsonView are only visible in (a subtype of) that view.
            .enable(MapperFeature.DEFAULT_VIEW_INCLUSION)
            .build();

    private static final String JSON = a2q(
            "{'label':'box','petType':'dog','pet':{'name':'Rex'}}");

    @Test
    public void typeIdHonoredInVisibleView() throws Exception
    {
        ObjectReader reader = MAPPER.readerFor(Container.class)
                .withView(Views.Internal.class);
        Container c = reader.readValue(JSON);

        assertEquals("box", c.label);
        assertEquals("dog", c.petType);
        assertNotNull(c.pet);
        assertInstanceOf(Dog.class, c.pet);
        assertEquals("Rex", c.pet.name);
    }

    // The core [databind#5958] assertion: under a view where the type-id property is
    // not visible, the value in the JSON must NOT be forwarded into the
    // external type handler. Without the patch, `petType:"dog"` would be picked
    // up via `_propsByIndex`/`handleTypePropertyValue` even though the field is
    // restricted to a different view, and `pet` would be deserialized as `Dog`.
    // With the patch, the type-id property is skipped and `defaultImpl` kicks in.
    @Test
    public void typeIdSkippedInRestrictedView() throws Exception
    {
        ObjectReader reader = MAPPER.readerFor(Container.class)
                .withView(Views.Public.class);
        Container c = reader.readValue(JSON);

        assertEquals("box", c.label);
        assertNull(c.petType, "petType is @JsonView-restricted; must be skipped under Public view");
        assertNotNull(c.pet);
        assertInstanceOf(UnknownAnimal.class, c.pet,
                "type id must not leak across views; defaultImpl should be used instead of Dog");
    }
}
