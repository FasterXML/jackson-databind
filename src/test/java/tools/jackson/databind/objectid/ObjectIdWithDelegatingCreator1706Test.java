package tools.jackson.databind.objectid;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.*;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.*;
import tools.jackson.databind.annotation.JsonDeserialize;
import tools.jackson.databind.annotation.JsonSerialize;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test for [databind#1706]: {@code @JsonCreator(mode=DELEGATING)} not working
 * properly with {@code @JsonIdentityInfo} when deserializing a collection
 * containing back-references.
 */
public class ObjectIdWithDelegatingCreator1706Test extends DatabindTestUtil
{
    @JsonSerialize(as = ImmutableItem.class)
    @JsonDeserialize(as = ImmutableItem.class)
    @JsonIdentityInfo(generator = ObjectIdGenerators.IntSequenceGenerator.class)
    interface Item {
        Integer getId();
        String getName();
    }

    static class ImmutableItem implements Item {
        private final Integer id;
        private final String name;

        ImmutableItem(Integer id, String name) {
            this.id = id;
            this.name = name;
        }

        @Override
        public Integer getId() { return id; }

        @Override
        public String getName() { return name; }

        @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
        static ImmutableItem fromJson(MutableItem json) {
            return new ImmutableItem(json.id, json.name);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ImmutableItem that = (ImmutableItem) o;
            return id.equals(that.id) && name.equals(that.name);
        }

        @Override
        public int hashCode() {
            return 31 * id.hashCode() + name.hashCode();
        }
    }

    @JsonDeserialize
    @JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.NONE)
    static class MutableItem implements Item {
        private String name;
        private Integer id;

        public void setId(Integer id) { this.id = id; }
        public void setName(String name) { this.name = name; }

        @Override
        public String getName() { throw new UnsupportedOperationException(); }

        @Override
        public Integer getId() { throw new UnsupportedOperationException(); }
    }

    private final ObjectMapper MAPPER = newJsonMapper();

    // [databind#1706]
    @Test
    public void testObjectIdWithDelegatingCreator() throws Exception
    {
        ImmutableItem item = new ImmutableItem(1, "test");

        // Serialize: list with same item twice produces ObjectId back-reference
        // e.g. [{"@id":1,"id":1,"name":"test"},1]
        String json = MAPPER.writeValueAsString(java.util.Arrays.asList(item, item));

        // Deserialize: back-reference should resolve to ImmutableItem, not MutableItem
        List<Item> result = MAPPER.readValue(json, new TypeReference<List<Item>>() {});

        assertEquals(2, result.size());
        // Both items should be ImmutableItem (not MutableItem for the back-reference)
        assertInstanceOf(ImmutableItem.class, result.get(0),
                "First item should be ImmutableItem");
        assertInstanceOf(ImmutableItem.class, result.get(1),
                "Second item (back-reference) should be ImmutableItem, not intermediate MutableItem");
        assertEquals(result.get(0), result.get(1));
    }
}
