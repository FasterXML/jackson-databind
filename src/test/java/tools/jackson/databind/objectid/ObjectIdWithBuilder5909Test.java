package tools.jackson.databind.objectid;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;

import tools.jackson.databind.*;
import tools.jackson.databind.annotation.JsonDeserialize;
import tools.jackson.databind.annotation.JsonPOJOBuilder;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.*;

/**
 * [databind#5909] (follow-up to [databind#1496]: Forward Object Id references
 * that appear inside a {@link java.util.Collection}-typed property of a
 * Builder-based type must be correctly resolved to the built target object,
 * not the Builder instance.
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

    // ---- Set variant: exercises CollectionReferring non-List branch ----

    @JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
    @JsonDeserialize(builder = EntitySetBuilder.class)
    static class EntitySet
    {
        private final long id;
        private final Set<EntitySet> refs;

        EntitySet(long id, Set<EntitySet> refs) {
            this.id = id;
            this.refs = refs;
        }
        public long getId() { return id; }
        public Set<EntitySet> getRefs() { return refs; }
    }

    @JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
    @JsonPOJOBuilder(withPrefix = "")
    static class EntitySetBuilder
    {
        private long id;
        private Set<EntitySet> refs;

        public EntitySetBuilder id(long id) { this.id = id; return this; }
        public EntitySetBuilder refs(Set<EntitySet> refs) { this.refs = refs; return this; }

        public EntitySet build() {
            return new EntitySet(id, refs == null ? new LinkedHashSet<>() : refs);
        }
    }

    static class EntitySetContainer
    {
        public List<EntitySet> entities;
    }

    // ---- Map variant: exercises MapReferring rebind path ----

    @JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
    @JsonDeserialize(builder = EntityMapBuilder.class)
    static class EntityMap
    {
        private final long id;
        private final Map<String, EntityMap> refs;

        EntityMap(long id, Map<String, EntityMap> refs) {
            this.id = id;
            this.refs = refs;
        }
        public long getId() { return id; }
        public Map<String, EntityMap> getRefs() { return refs; }
    }

    @JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
    @JsonPOJOBuilder(withPrefix = "")
    static class EntityMapBuilder
    {
        private long id;
        private Map<String, EntityMap> refs;

        public EntityMapBuilder id(long id) { this.id = id; return this; }
        public EntityMapBuilder refs(Map<String, EntityMap> refs) { this.refs = refs; return this; }

        public EntityMap build() {
            return new EntityMap(id, refs == null ? new LinkedHashMap<>() : refs);
        }
    }

    static class EntityMapContainer
    {
        public List<EntityMap> entities;
    }

    // NOTE: An equivalent test for `Entity[]`-typed properties is intentionally
    // omitted: typed arrays of the built type fail earlier with ArrayStoreException
    // when the Builder is stored into `_array` during bindItem, well before the
    // rebind path added here can run. That gap is out of scope for #5909.

    private final ObjectMapper MAPPER = newJsonMapper();

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

    @Test
    public void forwardReferenceInSet() throws Exception
    {
        String json = a2q("{'entities':["
                + "{'id':1,'refs':[2]},"
                + "{'id':2,'refs':[]}"
                + "]}");

        EntitySetContainer container = MAPPER.readValue(json, EntitySetContainer.class);
        assertEquals(2, container.entities.size());

        EntitySet first = container.entities.get(0);
        EntitySet second = container.entities.get(1);

        assertEquals(1, first.getRefs().size());
        assertSame(second, first.getRefs().iterator().next());
    }

    @Test
    public void forwardReferenceInMap() throws Exception
    {
        String json = a2q("{'entities':["
                + "{'id':1,'refs':{'k':2}},"
                + "{'id':2,'refs':{}}"
                + "]}");

        EntityMapContainer container = MAPPER.readValue(json, EntityMapContainer.class);
        assertEquals(2, container.entities.size());

        EntityMap first = container.entities.get(0);
        EntityMap second = container.entities.get(1);

        assertEquals(1, first.getRefs().size());
        assertSame(second, first.getRefs().get("k"));
    }

}
