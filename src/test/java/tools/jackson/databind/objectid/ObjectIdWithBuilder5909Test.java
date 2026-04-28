package tools.jackson.databind.objectid;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;

import tools.jackson.databind.*;
import tools.jackson.databind.annotation.JsonDeserialize;
import tools.jackson.databind.annotation.JsonPOJOBuilder;
import tools.jackson.databind.annotation.JsonSerialize;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for [databind#5909] (follow-up to [databind#1496]): Forward Object Id
 * references that appear inside a {@link java.util.Collection}, or
 * {@link java.util.Map}-typed property of a Builder-based type must be
 * correctly resolved to the built target object, not the Builder instance.
 * Also covers builder-with-property-creator ({@code @JsonCreator}),                                    
 * delegating-creator ({@code [databind#1706]}), and documents the   
 * typed-array limitation. 
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

    // ---- Property-based-creator builder variant: exercises the
    // PropertyValueBuffer.handleIdValue rebind path, distinct from
    // setter-based builders which go via ObjectIdValueProperty.

    @JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
    @JsonDeserialize(builder = EntityCreatorBuilder.class)
    static class EntityCreator
    {
        public final long id;
        public final List<EntityCreator> refs;

        EntityCreator(long id, List<EntityCreator> refs) {
            this.id = id;
            this.refs = refs;
        }
    }

    @JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
    @JsonPOJOBuilder(withPrefix = "")
    static class EntityCreatorBuilder
    {
        private final long id;
        private final List<EntityCreator> refs;

        @JsonCreator
        EntityCreatorBuilder(@JsonProperty("id") long id,
                @JsonProperty("refs") List<EntityCreator> refs) {
            this.id = id;
            this.refs = refs;
        }

        public EntityCreator build() {
            return new EntityCreator(id, refs);
        }
    }

    static class EntityCreatorContainer
    {
        public List<EntityCreator> entities;
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

    // Same forward id appearing multiple times in one List: every slot must be
    // rebound, not just the first.
    @Test
    public void duplicateForwardReferenceInList() throws Exception
    {
        String json = a2q("{'entities':["
                + "{'id':1,'refs':[2,2,2]},"
                + "{'id':2,'refs':[]}"
                + "]}");

        EntityContainer container = MAPPER.readValue(json, EntityContainer.class);
        Entity first = container.entities.get(0);
        Entity second = container.entities.get(1);

        assertEquals(3, first.getRefs().size());
        assertSame(second, first.getRefs().get(0));
        assertSame(second, first.getRefs().get(1));
        assertSame(second, first.getRefs().get(2));
    }

    // Multiple distinct forward ids in one List, all needing rebind.
    @Test
    public void multipleForwardReferencesInList() throws Exception
    {
        String json = a2q("{'entities':["
                + "{'id':1,'refs':[2,3]},"
                + "{'id':2,'refs':[]},"
                + "{'id':3,'refs':[]}"
                + "]}");

        EntityContainer container = MAPPER.readValue(json, EntityContainer.class);
        assertEquals(3, container.entities.size());

        Entity first = container.entities.get(0);
        Entity second = container.entities.get(1);
        Entity third = container.entities.get(2);

        assertEquals(2, first.getRefs().size());
        assertSame(second, first.getRefs().get(0));
        assertSame(third, first.getRefs().get(1));
    }

    // Multi-element Set with two forward refs to distinct ids: every entry
    // must end up rebound to its built object (none left as Builder), and the
    // snapshot/clear/replay path must not drop or duplicate entries.
    @Test
    public void multipleForwardReferencesInSet() throws Exception
    {
        String json = a2q("{'entities':["
                + "{'id':1,'refs':[2,3]},"
                + "{'id':2,'refs':[]},"
                + "{'id':3,'refs':[]}"
                + "]}");

        EntitySetContainer container = MAPPER.readValue(json, EntitySetContainer.class);
        EntitySet first = container.entities.get(0);
        EntitySet second = container.entities.get(1);
        EntitySet third = container.entities.get(2);

        Set<EntitySet> refs = first.getRefs();
        assertEquals(2, refs.size());
        // Every entry must be a built EntitySet, not a leftover Builder.
        for (EntitySet item : refs) {
            assertEquals(EntitySet.class, item.getClass(),
                    "entry should have been rebound from Builder to built object");
        }
        assertTrue(refs.contains(second));
        assertTrue(refs.contains(third));
    }

    // Multi-entry Map: forward and resolved refs in the same Map, distinct keys.
    @Test
    public void multipleForwardReferencesInMap() throws Exception
    {
        String json = a2q("{'entities':["
                + "{'id':1,'refs':{'a':2,'b':3,'c':2}},"
                + "{'id':2,'refs':{}},"
                + "{'id':3,'refs':{}}"
                + "]}");

        EntityMapContainer container = MAPPER.readValue(json, EntityMapContainer.class);
        EntityMap first = container.entities.get(0);
        EntityMap second = container.entities.get(1);
        EntityMap third = container.entities.get(2);

        assertEquals(3, first.getRefs().size());
        assertSame(second, first.getRefs().get("a"));
        assertSame(third, first.getRefs().get("b"));
        assertSame(second, first.getRefs().get("c"));
    }

    @Test
    public void forwardReferenceInCollectionWithPropertyCreatorBuilder() throws Exception
    {
        String json = a2q("{'entities':["
                + "{'id':1,'refs':[2]},"
                + "{'id':2,'refs':[]}"
                + "]}");

        EntityCreatorContainer container = MAPPER.readValue(json, EntityCreatorContainer.class);
        EntityCreator first = container.entities.get(0);
        EntityCreator second = container.entities.get(1);

        assertEquals(1, first.refs.size());
        assertSame(second, first.refs.get(0),
                "forward ref must be rebound from Builder to built object via PropertyValueBuffer path");
    }

    // ---- Array variant: documents the current limitation for typed arrays.
    // A forward Object Id reference inside a typed `Entity[]` property of a
    // Builder-based type cannot resolve to the Builder (different runtime type
    // than the array's component) — the JVM rejects the store with
    // `ArrayStoreException` during `resolveForwardReference`, well before the
    // builder→built rebind path added by [databind#5909] could run.
    //
    // This is a regression test for the documented behavior referenced by
    // {@link tools.jackson.databind.deser.jdk.ObjectArrayDeserializer.ObjectArrayReferringAccumulator#replaceResolvedItem(Object, Object)}.
    // Untyped `Object[]` arrays don't reach the same path because the default
    // element deserializer (UntypedObjectDeserializer) has no ObjectIdReader,
    // so the accumulator/forward-ref path isn't taken at all.

    @JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
    @JsonDeserialize(builder = EntityArrayBuilder.class)
    static class EntityArray
    {
        public final long id;
        public final EntityArray[] refs;

        EntityArray(long id, EntityArray[] refs) {
            this.id = id;
            this.refs = refs;
        }
    }

    @JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
    @JsonPOJOBuilder(withPrefix = "")
    static class EntityArrayBuilder
    {
        private long id;
        private EntityArray[] refs;

        public EntityArrayBuilder id(long id) { this.id = id; return this; }
        public EntityArrayBuilder refs(EntityArray[] refs) { this.refs = refs; return this; }

        public EntityArray build() { return new EntityArray(id, refs); }
    }

    static class EntityArrayContainer
    {
        public List<EntityArray> entities;
    }

    @Test
    public void forwardReferenceInTypedArrayFailsArrayStoreException() throws Exception
    {
        String json = a2q("{'entities':["
                + "{'id':1,'refs':[2]},"
                + "{'id':2,'refs':[]}"
                + "]}");

        // Typed `EntityArray[]` cannot hold an `EntityArrayBuilder` instance.
        // The forward-ref resolution writes the builder into the array slot
        // (in resolveForwardReference) and the JVM throws ArrayStoreException.
        Throwable thrown = assertThrows(Throwable.class,
                () -> MAPPER.readValue(json, EntityArrayContainer.class));
        Throwable root = thrown;
        while (root.getCause() != null && root != root.getCause()) {
            root = root.getCause();
        }
        assertEquals(ArrayStoreException.class, root.getClass(),
                "expected ArrayStoreException, got: " + root);
    }

    // ---- Delegating-creator variant ([databind#1706] + collection forward
    // refs): the bound id'd instance is a transient delegate (Mutable*) which
    // is replaced via updateObjectId after createUsingDelegate. Forward refs
    // inside a Collection-typed property must be rebound to the final bean,
    // not left pointing at the discarded delegate.

    @JsonIdentityInfo(generator = ObjectIdGenerators.IntSequenceGenerator.class, property = "@id")
    @JsonSerialize(as = ImmutableEntity1706.class)
    @JsonDeserialize(as = ImmutableEntity1706.class)
    interface Entity1706 {
        long getId();
        List<Entity1706> getRefs();
    }

    static class ImmutableEntity1706 implements Entity1706 {
        private final long id;
        private final List<Entity1706> refs;

        ImmutableEntity1706(long id, List<Entity1706> refs) {
            this.id = id;
            this.refs = refs;
        }

        @Override public long getId() { return id; }
        @Override public List<Entity1706> getRefs() { return refs; }

        @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
        static ImmutableEntity1706 fromJson(MutableEntity1706 m) {
            return new ImmutableEntity1706(m.id, m.refs);
        }
    }

    // @JsonDeserialize (no `as`) override on MutableEntity1706 cancels the
    // `as=ImmutableEntity1706` inherited from Entity1706 — needed so the
    // delegate type isn't (mis)narrowed during delegate-deserializer lookup.
    @JsonDeserialize
    @JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
    static class MutableEntity1706 implements Entity1706 {
        public long id;
        public List<Entity1706> refs;

        @Override public long getId() { throw new UnsupportedOperationException(); }
        @Override public List<Entity1706> getRefs() { throw new UnsupportedOperationException(); }
    }

    static class Entity1706Container {
        public List<Entity1706> entities;
    }

    @Test
    public void forwardReferenceInCollectionWithDelegatingCreator() throws Exception
    {
        // First entity refs the second by @id forward; second is materialized later.
        String json = a2q("{'entities':["
                + "{'@id':1,'id':10,'refs':[2]},"
                + "{'@id':2,'id':20,'refs':[]}"
                + "]}");

        Entity1706Container container = MAPPER.readValue(json, Entity1706Container.class);
        Entity1706 first = container.entities.get(0);
        Entity1706 second = container.entities.get(1);

        // Both must be the *built* immutable bean, not the leftover Mutable delegate
        assertEquals(ImmutableEntity1706.class, first.getClass());
        assertEquals(ImmutableEntity1706.class, second.getClass());

        assertEquals(1, first.getRefs().size());
        assertSame(second, first.getRefs().get(0),
                "forward ref must be rebound from delegate to built bean");
    }

}
