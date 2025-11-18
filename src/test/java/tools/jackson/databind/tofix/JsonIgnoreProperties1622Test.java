package tools.jackson.databind.tofix;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import tools.jackson.databind.*;
import tools.jackson.databind.testutil.DatabindTestUtil;
import tools.jackson.databind.testutil.failure.JacksonTestFailureExpected;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test to reproduce [databind#1622]: Race condition in deserialization with
 * {@code @JsonIgnoreProperties} when deserializing child objects before parent
 * objects in cyclic references.
 */
public class JsonIgnoreProperties1622Test
    extends DatabindTestUtil
{
    // Classes for reproducing the issue
    static class Parent {
        private String name;

        @JsonIgnoreProperties("parent")
        private List<Child> children = new ArrayList<>();

        public Parent() {}

        public Parent(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public List<Child> getChildren() {
            return children;
        }

        public void setChildren(List<Child> children) {
            this.children = children;
        }

        public void addChild(Child child) {
            children.add(child);
            child.setParent(this);
        }
    }

    static class Child {
        private String name;
        private Parent parent;

        public Child() {}

        public Child(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public Parent getParent() {
            return parent;
        }

        public void setParent(Parent parent) {
            this.parent = parent;
        }
    }

    // Variant with allowSetters workaround
    static class ParentWithWorkaround {
        private String name;

        @JsonIgnoreProperties(value = "parent", allowSetters = true)
        private List<ChildForWorkaround> children = new ArrayList<>();

        public ParentWithWorkaround() {}

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public List<ChildForWorkaround> getChildren() {
            return children;
        }

        public void setChildren(List<ChildForWorkaround> children) {
            this.children = children;
        }
    }

    static class ChildForWorkaround {
        private String name;
        private ParentWithWorkaround parent;

        public ChildForWorkaround() {}

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public ParentWithWorkaround getParent() {
            return parent;
        }

        public void setParent(ParentWithWorkaround parent) {
            this.parent = parent;
        }
    }

    private final ObjectMapper MAPPER = newJsonMapper();

    /**
     * This test demonstrates the race condition: deserializing a child first
     * causes the parent deserialization to fail with "No _valueDeserializer assigned"
     */
    @JacksonTestFailureExpected
    @Test
    public void raceConditionWithChildFirst() throws Exception
    {
        // First create and serialize the objects
        Parent parent = new Parent("Parent1");
        Child child = new Child("Child1");
        parent.addChild(child);

        String parentJson = MAPPER.writeValueAsString(parent);
        String childJson = MAPPER.writeValueAsString(child);

        // Deserialize child first - this triggers the race condition
        Child deserializedChild = MAPPER.readValue(childJson, Child.class);
        assertNotNull(deserializedChild);
        assertEquals("Child1", deserializedChild.getName());

        // Now try to deserialize parent - this fail with the race condition
        // Expected error: "No _valueDeserializer assigned"
        Parent deserializedParent = MAPPER.readValue(parentJson, Parent.class);
        assertNotNull(deserializedParent);
        assertEquals("Parent1", deserializedParent.getName());
        assertEquals(1, deserializedParent.getChildren().size());
        assertEquals("Child1", deserializedParent.getChildren().get(0).getName());
    }

    /**
     * Control test: deserializing parent first works fine
     */
    @Test
    public void noRaceConditionWithParentFirst() throws Exception
    {
        Parent parent = new Parent("Parent1");
        Child child = new Child("Child1");
        parent.addChild(child);

        String parentJson = MAPPER.writeValueAsString(parent);

        // Deserialize parent first - this should work
        Parent deserializedParent = MAPPER.readValue(parentJson, Parent.class);
        assertNotNull(deserializedParent);
        assertEquals("Parent1", deserializedParent.getName());
        assertEquals(1, deserializedParent.getChildren().size());
        assertEquals("Child1", deserializedParent.getChildren().get(0).getName());
    }

    /**
     * Test that the workaround with allowSetters = true resolves the issue
     */
    @Test
    public void workaroundWithAllowSetters() throws Exception
    {
        ParentWithWorkaround parent = new ParentWithWorkaround();
        parent.setName("Parent1");

        ChildForWorkaround child = new ChildForWorkaround();
        child.setName("Child1");
        child.setParent(parent);

        parent.setChildren(Arrays.asList(child));

        String parentJson = MAPPER.writeValueAsString(parent);
        String childJson = MAPPER.writeValueAsString(child);

        // Deserialize child first
        ChildForWorkaround deserializedChild = MAPPER.readValue(childJson, ChildForWorkaround.class);
        assertNotNull(deserializedChild);
        assertEquals("Child1", deserializedChild.getName());

        // Now deserialize parent - should work with allowSetters workaround
        ParentWithWorkaround deserializedParent = MAPPER.readValue(parentJson, ParentWithWorkaround.class);
        assertNotNull(deserializedParent);
        assertEquals("Parent1", deserializedParent.getName());
        assertEquals(1, deserializedParent.getChildren().size());
        assertEquals("Child1", deserializedParent.getChildren().get(0).getName());
    }
}
