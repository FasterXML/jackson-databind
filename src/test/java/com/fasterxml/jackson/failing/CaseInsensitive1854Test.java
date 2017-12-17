package com.fasterxml.jackson.failing;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.*;

public class CaseInsensitive1854Test extends BaseMapTest
{
    static class Obj1854 {

        private final int id;
        private final List<ChildObj> items;

        public Obj1854(int id, List<ChildObj> items) {
            this.id = id;
            this.items = items;
        }

        @JsonCreator
        public static Obj1854 fromJson(@JsonProperty("ID") int id,
                @JsonProperty("Items") List<ChildObj> items) {
            return new Obj1854(id, items);
        }

        public int getId() {
            return id;
        }

        public List<ChildObj> getItems() {
            return items;
        }

    }

    static class ChildObj {
        private final String id;

        public ChildObj(String id) {
            this.id = id;
        }

        @JsonCreator
        public static ChildObj fromJson(@JsonProperty("ID") String id) {
            return new ChildObj(id);
        }

        public String getId() {
            return id;
        }

    }
    
    public void testIssue1854() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        mapper.enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES);
        final String DOC = aposToQuotes("{'ID': 1, 'Items': [ { 'ID': 10 } ]}");
        Obj1854 result = mapper.readValue(DOC, Obj1854.class);
        assertNotNull(result);
        assertEquals(1, result.getId());
        assertNotNull(result.getItems());
        assertEquals(1, result.getItems().size());
    }
}
