package tools.jackson.databind.objectid;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;

import tools.jackson.databind.*;
import tools.jackson.databind.annotation.JsonDeserialize;
import tools.jackson.databind.annotation.JsonPOJOBuilder;
import tools.jackson.databind.exc.InvalidDefinitionException;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.*;

/**
 * [databind#1496]: Object Ids with builder-based deserialization.
 *<p>
 * NOTE: this test class contains passing tests; collection-item forward
 * references are tested in
 * {@link tools.jackson.databind.objectid.ObjectIdWithBuilder5909Test}.
 */
class ObjectIdWithBuilder1496Test extends DatabindTestUtil
{
    @JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
    @JsonDeserialize(builder = POJOBuilder.class)
    static class POJO {
        private long id;

        public long getId() {
            return id;
        }

        private int var;

        public int getVar() {
            return var;
        }

        POJO(long id, int var) {
            this.id = id;
            this.var = var;
        }

        @Override
        public String toString() {
            return "id: " + id + ", var: " + var;
        }
    }

    @JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
    @JsonPOJOBuilder(withPrefix = "", buildMethodName = "readFromCacheOrBuild")
    static final class POJOBuilder {
        // Standard builder stuff
        private long id;
        private int var;

        public POJOBuilder id(long _id) {
            id = _id;
            return this;
        }

        public POJOBuilder var(int _var) {
            var = _var;
            return this;
        }

        public POJO build() {
            return new POJO(id, var);
        }

        // Special build method for jackson deserializer that caches objects already deserialized
        private final static ConcurrentHashMap<Long, POJO> cache = new ConcurrentHashMap<>();

        public POJO readFromCacheOrBuild() {
            POJO pojo = cache.get(id);
            if (pojo == null) {
                POJO newPojo = build();
                pojo = cache.putIfAbsent(id, newPojo);
                if (pojo == null) {
                    pojo = newPojo;
                }
            }
            return pojo;
        }
    }

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

    /*
    /**********************************************************************
    /* Test cases
    /**********************************************************************
     */

    // [databind#1496]: basic round-trip with builder + Object Id
    @Test
    void builderId1496() throws Exception {
        POJO input = new POJOBuilder().id(123L).var(456).build();
        String json = MAPPER.writeValueAsString(input);
        POJO result = MAPPER.readValue(json, POJO.class);
        assertNotNull(result);
        assertEquals(123L, result.getId());
        assertEquals(456, result.getVar());
    }

    // [databind#1496]: back reference (entity 2 references entity 1 which was already built)
    @Test
    void testPureBackReference() throws Exception
    {
        String json = a2q("{'entities':["
                + "{'id':1,'refs':[]},"
                + "{'id':2,'ref':1,'refs':[]}"
                + "]}");

        EntityContainer container = MAPPER.readValue(json, EntityContainer.class);
        assertNotNull(container);
        assertEquals(2, container.entities.size());

        Entity first = container.entities.get(0);
        Entity second = container.entities.get(1);

        assertEquals(1, first.getId());
        assertEquals(2, second.getId());

        // second.ref -> first (back reference, already built)
        assertSame(first, second.getRef());
    }

    // [databind#1496]: forward Object Id references are not (yet) supported with
    // Builder-based deserialization; verify we fail with a clear, actionable error
    // message instead of silently injecting the builder or throwing something generic.
    @Test
    void forwardReferenceReportsClearError() throws Exception
    {
        String json = a2q("{'entities':["
                + "{'id':1,'ref':2,'refs':[]},"
                + "{'id':2,'refs':[]}"
                + "]}");
        try {
            MAPPER.readValue(json, EntityContainer.class);
            fail("Expected InvalidDefinitionException for forward reference with Builder");
        } catch (InvalidDefinitionException e) {
            verifyException(e, "Cannot resolve forward Object Id references");
            verifyException(e, "Builder-based");
        }
    }

}
