package tools.jackson.databind.tofix;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;

import tools.jackson.databind.*;
import tools.jackson.databind.annotation.JsonDeserialize;
import tools.jackson.databind.annotation.JsonPOJOBuilder;
import tools.jackson.databind.testutil.DatabindTestUtil;
import tools.jackson.databind.testutil.failure.JacksonTestFailureExpected;

import static org.junit.jupiter.api.Assertions.*;

class ObjectIdWithBuilder1496Test extends DatabindTestUtil {
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

    @JacksonTestFailureExpected
    @Test
    void builderId1496() throws Exception {
        POJO input = new POJOBuilder().id(123L).var(456).build();
        String json = MAPPER.writeValueAsString(input);
        POJO result = MAPPER.readValue(json, POJO.class);
        assertNotNull(result);
    }

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
