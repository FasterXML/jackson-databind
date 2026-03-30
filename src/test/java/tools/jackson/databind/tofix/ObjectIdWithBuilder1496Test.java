package tools.jackson.databind.tofix;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;

import tools.jackson.databind.*;
import tools.jackson.databind.annotation.JsonDeserialize;
import tools.jackson.databind.annotation.JsonPOJOBuilder;
import tools.jackson.databind.testutil.DatabindTestUtil;
import tools.jackson.databind.testutil.failure.JacksonTestFailureExpected;

import static org.junit.jupiter.api.Assertions.*;

/**
 * [databind#1496]: Forward references with builder-based deserialization
 * still need work because the Object Id gets bound to the builder (not the built
 * object) before finishBuild, so forward reference resolution injects the builder
 *  instead of the final object.
 * NOTE: this test class contains still failing tests; there is separate class,
 * {@link tools.jackson.databind.objectid.ObjectIdWithBuilder1496Test}, for
 * now-passing cases.
 */
class ObjectIdWithBuilder1496Test extends DatabindTestUtil
{
    @JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
    @JsonDeserialize(builder = EntityBuilder.class)
    static class Entity
    {
        private long id;
        private Entity ref;
        private List<Entity> refs;

        Entity(long id, Entity ref, List<Entity> refs) {
            this.id = id;
            this.ref = ref;
            this.refs = refs;
        }

        public long getId() { return id; }
        public Entity getRef() { return ref; }
        public List<Entity> getRefs() { return refs; }
    }

    @JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
    @JsonPOJOBuilder(withPrefix = "")
    static class EntityBuilder
    {
        private long id;
        private Entity ref;
        private List<Entity> refs;

        public EntityBuilder id(long id) { this.id = id; return this; }
        public EntityBuilder ref(Entity ref) { this.ref = ref; return this; }
        public EntityBuilder refs(List<Entity> refs) { this.refs = refs; return this; }

        public Entity build() {
            return new Entity(id, ref, refs);
        }
    }

    static class EntityContainer
    {
        public List<Entity> entities;
    }

    private final ObjectMapper MAPPER = newJsonMapper();

    // Forward reference: first entity references second entity (which comes later)
    @JacksonTestFailureExpected
    @Test
    public void testForwardReference() throws Exception
    {
        String json = a2q("{'entities':["
                + "{'id':1,'ref':2,'refs':[]},"
                + "{'id':2,'refs':[1]}"
                + "]}");

        EntityContainer container = MAPPER.readValue(json, EntityContainer.class);
        assertNotNull(container);
        assertEquals(2, container.entities.size());

        Entity first = container.entities.get(0);
        Entity second = container.entities.get(1);

        assertEquals(1, first.getId());
        assertEquals(2, second.getId());

        // first.ref -> second (forward reference)
        assertSame(second, first.getRef());
        // second.refs[0] -> first (back reference in collection)
        assertEquals(1, second.getRefs().size());
        assertSame(first, second.getRefs().get(0));
    }

    // Back reference: second entity references first entity (which came earlier)
    // but first entity has forward reference in collection
    @JacksonTestFailureExpected
    @Test
    public void testBackReference() throws Exception
    {
        String json = a2q("{'entities':["
                + "{'id':1,'refs':[2]},"
                + "{'id':2,'ref':1,'refs':[]}"
                + "]}");

        EntityContainer container = MAPPER.readValue(json, EntityContainer.class);
        assertNotNull(container);
        assertEquals(2, container.entities.size());

        Entity first = container.entities.get(0);
        Entity second = container.entities.get(1);

        assertEquals(1, first.getId());
        assertEquals(2, second.getId());

        // second.ref -> first (back reference)
        assertSame(first, second.getRef());
        // first.refs[0] -> second (forward reference in collection)
        assertEquals(1, first.getRefs().size());
        assertSame(second, first.getRefs().get(0));
    }
}
