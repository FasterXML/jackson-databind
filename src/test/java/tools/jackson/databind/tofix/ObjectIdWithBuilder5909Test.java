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
 * [databind#5909] (follow-up to [databind#1496]: Forward Object Id references
 * that appear inside a
 * {@link java.util.Collection}-typed property of a Builder-based type are
 * silently resolved to the Builder instance instead of the built target.
 * <p>
 * Unlike direct forward references on scalar properties -- which now fail with
 * a clear {@code InvalidDefinitionException} (see
 * {@link tools.jackson.databind.objectid.ObjectIdWithBuilder1496Test}) -- the
 * collection-item path does not go through
 * {@code ObjectIdReferenceProperty.deserializeSetAndReturn}, so
 * {@code addPendingForwardRef} is never called and the guard in
 * {@code BuilderBasedDeserializer.finishBuild} does not trip. Fixing this
 * requires a separate mechanism to re-bind collection-item references from the
 * builder to the built object.
 */
class ObjectIdWithBuilder5909Test extends DatabindTestUtil
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

    // First entity's `refs` list holds a forward reference (id 2) resolved only
    // after entity 2 has been deserialized; the slot currently ends up holding
    // the EntityBuilder instance instead of the built Entity.
    @JacksonTestFailureExpected
    @Test
    public void forwardReferenceInCollection() throws Exception
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

        assertSame(first, second.getRef());
        assertEquals(1, first.getRefs().size());
        assertSame(second, first.getRefs().get(0));
    }
}
