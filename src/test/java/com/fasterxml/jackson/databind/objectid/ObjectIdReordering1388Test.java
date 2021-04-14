package com.fasterxml.jackson.databind.objectid;

import java.util.*;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.*;

public class ObjectIdReordering1388Test extends BaseMapTest
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


    public void testDeserializationFinalClassJSOG() throws Exception
    {
        final ObjectMapper mapper = new ObjectMapper();
        final UUID id = UUID.fromString("a59aa02c-fe3c-43f8-9b5a-5fe01878a818");
        final NamedThing thing = new NamedThing(id, "Hello");

        final TypeReference<List<NamedThing>> namedThingListType = new TypeReference<List<NamedThing>>() { };

        {
            final String jsog = mapper.writeValueAsString(Arrays.asList(thing, thing, thing));
            final List<NamedThing> list = mapper.readValue(jsog, namedThingListType);
            _assertAllSame(list);
            // this is the jsog representation of the list of 3 of the same item
            assertTrue(jsog.equals("[{\"@id\":1,\"id\":\"a59aa02c-fe3c-43f8-9b5a-5fe01878a818\",\"name\":\"Hello\"},1,1]"));
        }

        // now move it around it have forward references
        // this works
        {
            final String json = "[1,1,{\"@id\":1,\"id\":\"a59aa02c-fe3c-43f8-9b5a-5fe01878a818\",\"name\":\"Hello\"}]";
            final List<NamedThing> forward = mapper.readValue(json, namedThingListType);
            _assertAllSame(forward);
        }

        // next, move @id to between properties
        {
            final String json = a2q("[{'id':'a59aa02c-fe3c-43f8-9b5a-5fe01878a818','@id':1,'name':'Hello'}, 1, 1]");
            final List<NamedThing> forward = mapper.readValue(json, namedThingListType);
            _assertAllSame(forward);
        }

        // and last, move @id to be not the first key in the object
        {
            final String json = a2q("[{'id':'a59aa02c-fe3c-43f8-9b5a-5fe01878a818','name':'Hello','@id':1}, 1, 1]");
            final List<NamedThing> forward = mapper.readValue(json, namedThingListType);
            _assertAllSame(forward);
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
