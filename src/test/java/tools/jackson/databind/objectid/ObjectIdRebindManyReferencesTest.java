package tools.jackson.databind.objectid;

import java.util.*;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.*;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.annotation.JsonDeserialize;
import tools.jackson.databind.annotation.JsonPOJOBuilder;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests that Object Ids bound to Builders (or delegates) are rebound to built values
 * for many values and references: each rebind locates the Object Id entry, and each
 * reference replaces just its own slot, without scanning all Object Ids or the whole
 * container (which would take O(N^2) time).
 */
public class ObjectIdRebindManyReferencesTest extends DatabindTestUtil
{
    @JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
    @JsonDeserialize(builder = EntityBuilder.class)
    static class Entity
    {
        final int id;
        final Entity ref;
        final List<Entity> list;
        final Set<Entity> set;
        final LinkedHashSet<Entity> orderedSet;
        final Map<String, Entity> map;
        final Entity[] array;

        Entity(EntityBuilder b) {
            id = b.id;
            ref = b.ref;
            list = b.list;
            set = b.set;
            orderedSet = b.orderedSet;
            map = b.map;
            array = b.array;
        }

        public int getId() { return id; }
    }

    @JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
    @JsonPOJOBuilder(withPrefix = "")
    static class EntityBuilder
    {
        int id;
        Entity ref;
        List<Entity> list;
        Set<Entity> set;
        LinkedHashSet<Entity> orderedSet;
        Map<String, Entity> map;
        Entity[] array;

        public EntityBuilder id(int v) { id = v; return this; }
        public EntityBuilder ref(Entity v) { ref = v; return this; }
        public EntityBuilder list(List<Entity> v) { list = v; return this; }
        public EntityBuilder set(Set<Entity> v) { set = v; return this; }
        public EntityBuilder orderedSet(LinkedHashSet<Entity> v) { orderedSet = v; return this; }
        public EntityBuilder map(Map<String, Entity> v) { map = v; return this; }
        public EntityBuilder array(Entity[] v) { array = v; return this; }

        public Entity build() {
            return new Entity(this);
        }
    }

    static class Container {
        public List<Entity> entities;
    }

    // Delegating-creator variant: bound id'd value is a transient delegate
    @JsonIdentityInfo(generator = ObjectIdGenerators.IntSequenceGenerator.class, property = "@id")
    @JsonDeserialize(as = ImmutableItem.class)
    interface Item {
        long getId();
        List<Item> getRefs();
    }

    static class ImmutableItem implements Item {
        private final long id;
        private final List<Item> refs;

        ImmutableItem(long id, List<Item> refs) {
            this.id = id;
            this.refs = refs;
        }

        @Override public long getId() { return id; }
        @Override public List<Item> getRefs() { return refs; }

        @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
        static ImmutableItem fromJson(MutableItem m) {
            return new ImmutableItem(m.id, m.refs);
        }
    }

    // `@JsonDeserialize` (no `as`) cancels the one inherited from `Item`
    @JsonDeserialize
    @JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
    static class MutableItem implements Item {
        public long id;
        public List<Item> refs;

        @Override public long getId() { throw new UnsupportedOperationException(); }
        @Override public List<Item> getRefs() { throw new UnsupportedOperationException(); }
    }

    static class ItemContainer {
        public List<Item> items;
    }

    // Builder with hash code that changes as properties get set (after Object Id is bound)
    @JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
    @JsonDeserialize(builder = NamedBuilder.class)
    static class Named
    {
        final int id;
        final String name;
        final Set<Named> set;

        Named(NamedBuilder b) {
            id = b.id;
            name = b.name;
            set = b.set;
        }

        public int getId() { return id; }
    }

    @JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
    @JsonPOJOBuilder(withPrefix = "")
    static class NamedBuilder
    {
        int id;
        String name;
        Set<Named> set;

        public NamedBuilder id(int v) { id = v; return this; }
        public NamedBuilder name(String v) { name = v; return this; }
        public NamedBuilder set(Set<Named> v) { set = v; return this; }

        public Named build() {
            return new Named(this);
        }

        @Override
        public boolean equals(Object o) {
            return (o instanceof NamedBuilder b) && (b.id == id) && Objects.equals(b.name, name);
        }

        @Override
        public int hashCode() {
            return Objects.hash(id, name);
        }
    }

    static class NamedContainer {
        public List<Named> entities;
    }

    private static final int COUNT = 1000;

    private final ObjectMapper MAPPER = newJsonMapper();

    // Builder added to Set (by reference) before its hash code changes, so
    // it cannot be found by hash code any more when rebound
    @Test
    public void forwardReferencesInSetWithChangingBuilderHashCode()
    {
        NamedContainer c = MAPPER.readValue(a2q("{'entities':[{'id':0,'set':[1,2,1]},"
                + "{'id':1,'name':'a'},{'id':2,'name':'b'}]}"), NamedContainer.class);
        Set<Named> set = c.entities.get(0).set;

        assertEquals(HashSet.class, set.getClass());
        assertEquals(2, set.size());
        assertTrue(set.contains(c.entities.get(1)));
        assertTrue(set.contains(c.entities.get(2)));
    }

    // Values following rebound ones refer to them: must get the built values
    @Test
    public void backReferencesToManyBuiltValues()
    {
        StringBuilder sb = new StringBuilder("{\"entities\":[{\"id\":1}");
        for (int i = 2; i <= COUNT; ++i) {
            sb.append(",{\"id\":").append(i).append(",\"ref\":").append(i - 1).append('}');
        }
        Container c = MAPPER.readValue(sb.append("]}").toString(), Container.class);

        assertEquals(COUNT, c.entities.size());
        for (int i = 1; i < COUNT; ++i) {
            assertSame(c.entities.get(i - 1), c.entities.get(i).ref, "Wrong ref for #"+i);
        }
    }

    // Each id referenced twice
    @Test
    public void forwardReferencesInList()
    {
        Container c = _readWithRoot("\"list\":[" + _ids(false, 2) + "]");
        List<Entity> list = c.entities.get(0).list;

        assertEquals(2 * COUNT, list.size());
        for (int i = 0; i < list.size(); ++i) {
            assertSame(c.entities.get(1 + i / 2), list.get(i), "Wrong entry #"+i);
        }
    }

    // Referenced in reverse order, so that values get rebound while still pending
    @Test
    public void reverseForwardReferencesInList()
    {
        Container c = _readWithRoot("\"list\":[" + _ids(true, 1) + "]");
        List<Entity> list = c.entities.get(0).list;

        assertEquals(COUNT, list.size());
        for (int i = 0; i < COUNT; ++i) {
            assertSame(c.entities.get(COUNT - i), list.get(i), "Wrong entry #"+i);
        }
    }

    @Test
    public void forwardReferencesInSet()
    {
        Container c = _readWithRoot("\"set\":[" + _ids(false, 2) + "]");
        Set<Entity> set = c.entities.get(0).set;

        assertEquals(HashSet.class, set.getClass());
        assertEquals(COUNT, set.size());
        for (int i = 1; i <= COUNT; ++i) {
            assertTrue(set.contains(c.entities.get(i)), "Missing entry for id "+i);
        }
    }

    // Ordered Set must retain the order of references
    @Test
    public void forwardReferencesInOrderedSet()
    {
        Container c = _readWithRoot("\"orderedSet\":[" + _ids(false, 1) + "]");
        LinkedHashSet<Entity> set = c.entities.get(0).orderedSet;

        assertEquals(COUNT, set.size());
        Iterator<Entity> it = set.iterator();
        for (int i = 1; i <= COUNT; ++i) {
            assertSame(c.entities.get(i), it.next(), "Wrong entry for id "+i);
        }
    }

    @Test
    public void forwardReferencesInMap()
    {
        StringBuilder sb = new StringBuilder("\"map\":{");
        for (int i = 1; i <= COUNT; ++i) {
            sb.append("\"k").append(i).append("\":").append(i).append(',');
        }
        // Duplicate key: later entry overrides earlier one (but retains position)
        sb.append("\"k1\":2}");
        Container c = _readWithRoot(sb.toString());
        Map<String, Entity> map = c.entities.get(0).map;

        assertEquals(COUNT, map.size());
        assertEquals("k1", map.keySet().iterator().next());
        assertSame(c.entities.get(2), map.get("k1"));
        for (int i = 2; i <= COUNT; ++i) {
            assertSame(c.entities.get(i), map.get("k"+i), "Wrong entry for key k"+i);
        }
    }

    // Duplicate key, with overridden reference rebound only after being overridden
    @Test
    public void forwardReferencesInMapWithDuplicateKey()
    {
        Container c = _readWithRoot("\"map\":{\"a\":2,\"a\":1}");
        Map<String, Entity> map = c.entities.get(0).map;

        assertEquals(1, map.size());
        assertSame(c.entities.get(1), map.get("a"));
    }

    // Each id referenced twice
    @Test
    public void forwardReferencesInArray()
    {
        Container c = _readWithRoot("\"array\":[" + _ids(false, 2) + "]");
        Entity[] array = c.entities.get(0).array;

        assertEquals(2 * COUNT, array.length);
        for (int i = 0; i < array.length; ++i) {
            assertSame(c.entities.get(1 + i / 2), array[i], "Wrong entry #"+i);
        }
    }

    @Test
    public void forwardReferencesWithDelegatingCreator()
    {
        StringBuilder sb = new StringBuilder("{\"items\":[{\"@id\":0,\"id\":0,\"refs\":[")
                .append(_ids(false, 1)).append("]}");
        for (int i = 1; i <= COUNT; ++i) {
            sb.append(",{\"@id\":").append(i).append(",\"id\":").append(i).append(",\"refs\":[]}");
        }
        ItemContainer c = MAPPER.readValue(sb.append("]}").toString(), ItemContainer.class);

        assertEquals(COUNT + 1, c.items.size());
        List<Item> refs = c.items.get(0).getRefs();
        assertEquals(COUNT, refs.size());
        for (int i = 0; i < COUNT; ++i) {
            assertEquals(ImmutableItem.class, c.items.get(i + 1).getClass());
            assertSame(c.items.get(i + 1), refs.get(i), "Wrong entry #"+i);
        }
    }

    // Root entity (id 0) with given property, followed by entities with ids 1 to COUNT
    private Container _readWithRoot(String rootProperty)
    {
        StringBuilder sb = new StringBuilder("{\"entities\":[{\"id\":0,")
                .append(rootProperty).append('}');
        for (int i = 1; i <= COUNT; ++i) {
            sb.append(",{\"id\":").append(i).append('}');
        }
        Container c = MAPPER.readValue(sb.append("]}").toString(), Container.class);
        assertEquals(COUNT + 1, c.entities.size());
        return c;
    }

    // Comma-separated ids from 1 to COUNT, in given order, each repeated `times` times
    private static String _ids(boolean reverse, int times)
    {
        StringBuilder sb = new StringBuilder();
        for (int i = 1; i <= COUNT; ++i) {
            final int id = reverse ? (COUNT + 1 - i) : i;
            for (int t = 0; t < times; ++t) {
                if (sb.length() > 0) {
                    sb.append(',');
                }
                sb.append(id);
            }
        }
        return sb.toString();
    }
}
