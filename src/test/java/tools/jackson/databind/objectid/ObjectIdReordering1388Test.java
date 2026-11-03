package tools.jackson.databind.objectid;

import java.util.*;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.*;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.*;
import tools.jackson.databind.deser.UnresolvedForwardReference;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.*;

public class ObjectIdReordering1388Test extends DatabindTestUtil
{
    @JsonIdentityInfo(generator = ObjectIdGenerators.IntSequenceGenerator.class)
    public static class NamedThing {
        private final UUID id;
        private final String name;

        @JsonCreator
        public NamedThing(@JsonProperty("id") UUID id, @JsonProperty("name") String name) {
            this.id = id;
            this.name = name;
        }

        public UUID getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            NamedThing that = (NamedThing) o;
            return that.id.equals(id) && that.name.equals(name);
        }

        @Override
        public int hashCode() {
            return name.hashCode();
        }
    }

    private final ObjectMapper MAPPER = newJsonMapper();

    private final TypeReference<List<NamedThing>> namedThingListType = new TypeReference<List<NamedThing>>() { };
    
    // [databind#1388]
    @Test
    public void testOrdering1388() throws Exception
    {
        final UUID id = UUID.fromString("a59aa02c-fe3c-43f8-9b5a-5fe01878a818");
        final NamedThing thing = new NamedThing(id, "Hello");


        {
            final String json = MAPPER.writeValueAsString(Arrays.asList(thing, thing, thing));
            final List<NamedThing> list = MAPPER.readValue(json, namedThingListType);
            _assertAllSame(list);
            // this is the jsog representation of the list of 3 of the same item
            assertTrue(json.equals("[{\"@id\":1,\"id\":\"a59aa02c-fe3c-43f8-9b5a-5fe01878a818\",\"name\":\"Hello\"},1,1]"));
        }

        // now move it around it have forward references
        // this works
        {
            final String json = "[1,1,{\"@id\":1,\"id\":\"a59aa02c-fe3c-43f8-9b5a-5fe01878a818\",\"name\":\"Hello\"}]";
            final List<NamedThing> forward = MAPPER.readValue(json, namedThingListType);
            _assertAllSame(forward);
        }

        // next, move @id to between properties
        {
            final String json = a2q("[{'id':'a59aa02c-fe3c-43f8-9b5a-5fe01878a818','@id':1,'name':'Hello'}, 1, 1]");
            final List<NamedThing> forward = MAPPER.readValue(json, namedThingListType);
            _assertAllSame(forward);
        }

        // and last, move @id to be not the first key in the object
        {
            final String json = a2q("[{'id':'a59aa02c-fe3c-43f8-9b5a-5fe01878a818','name':'Hello','@id':1}, 1, 1]");
            final List<NamedThing> forward = MAPPER.readValue(json, namedThingListType);
            _assertAllSame(forward);
        }
    }

    @Test
    public void testNullsNoObjectId() throws Exception
    {
        final List<NamedThing> l = MAPPER.readValue("[null]", namedThingListType);
        assertEquals(1, l.size());
        assertNull(l.get(0));
    }
    
    @Test
    public void testUnresolvedObjectId() throws Exception
    {
        try { 
            MAPPER.readValue("[123]", namedThingListType);
            fail("Should not pass");
        } catch (UnresolvedForwardReference e) {
            verifyException(e, "Unresolved forward references: [{Object id: 123}]");
        }
    }

    private void _assertAllSame(List<?> entries) {
        Object first = entries.get(0);
        for (int i = 0, end = entries.size(); i < end; ++i) {
            if (first != entries.get(i)) {
                fail("Mismatch: entry #"+i+" not same as #0");
            }
        }
    }
}
