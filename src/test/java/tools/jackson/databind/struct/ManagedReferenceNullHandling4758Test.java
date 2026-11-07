package tools.jackson.databind.struct;

import java.util.*;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.*;

import tools.jackson.databind.*;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.*;

// [databind#4758] ConfigOverride using Nulls.AS_EMPTY does not work with JsonManagedReference
public class ManagedReferenceNullHandling4758Test
    extends DatabindTestUtil
{
    static class Parent {
        public String name;

        @JsonManagedReference
        public List<Item> children = new ArrayList<>();
    }

    static class Item {
        public String name;

        @JsonBackReference
        public Parent parent;
    }

    // Verify that Nulls.AS_EMPTY works with @JsonManagedReference List property
    @Test
    void testNullsAsEmptyWithManagedReference() throws Exception
    {
        ObjectMapper mapper = JsonMapper.builder()
                .withConfigOverride(List.class,
                        o -> o.setNullHandling(JsonSetter.Value.forValueNulls(Nulls.AS_EMPTY)))
                .build();

        String json = "{ \"name\": \"parent\", \"children\": null }";
        Parent result = mapper.readValue(json, Parent.class);

        assertNotNull(result.children,
                "children should be empty list, not null, when Nulls.AS_EMPTY is configured");
        assertTrue(result.children.isEmpty());
    }

    // Verify non-null children still work correctly with back-references
    @Test
    void testNonNullChildrenWithManagedReference() throws Exception
    {
        ObjectMapper mapper = JsonMapper.builder()
                .withConfigOverride(List.class,
                        o -> o.setNullHandling(JsonSetter.Value.forValueNulls(Nulls.AS_EMPTY)))
                .build();

        String json = "{ \"name\": \"parent\", \"children\":[{\"name\":\"child1\"},{\"name\":\"child2\"}]}";
        Parent result = mapper.readValue(json, Parent.class);

        assertNotNull(result.children);
        assertEquals(2, result.children.size());
        assertEquals("child1", result.children.get(0).name);
        assertEquals("child2", result.children.get(1).name);
        // Verify back-references are properly set
        assertSame(result, result.children.get(0).parent);
        assertSame(result, result.children.get(1).parent);
    }
}
