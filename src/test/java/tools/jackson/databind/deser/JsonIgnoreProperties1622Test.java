package tools.jackson.databind.deser;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import tools.jackson.databind.*;
import tools.jackson.databind.annotation.JsonDeserialize;
import tools.jackson.databind.testutil.DatabindTestUtil;

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

    // property-based creator using @JsonCreator
    static class ParentWithCreator {
        private String name;

        @JsonIgnoreProperties("parent")
        private List<ChildWithCreator> children = new ArrayList<>();

        public ParentWithCreator() {}

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public List<ChildWithCreator> getChildren() { return children; }
        public void setChildren(List<ChildWithCreator> children) { this.children = children; }
    }

    static class ChildWithCreator {
        private final String name;
        private final ParentWithCreator parent;

        @JsonCreator
        public ChildWithCreator(
                @JsonProperty("name") String name,
                @JsonProperty("parent") ParentWithCreator parent) {
            this.name = name;
            this.parent = parent;
        }

        public String getName() { return name; }
        public ParentWithCreator getParent() { return parent; }
    }

    // Using Builder
    static class ParentWithBuilder {
        private String name;

        @JsonIgnoreProperties("parent")
        private List<ChildWithBuilder> children = new ArrayList<>();

        public ParentWithBuilder() {}

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public List<ChildWithBuilder> getChildren() { return children; }
        public void setChildren(List<ChildWithBuilder> children) { this.children = children; }
    }

    @JsonDeserialize(builder = ChildWithBuilder.Builder.class)
    static class ChildWithBuilder {
        private final String name;
        private final ParentWithBuilder parent;

        private ChildWithBuilder(String name, ParentWithBuilder parent) {
            this.name = name;
            this.parent = parent;
        }

        public String getName() { return name; }
        public ParentWithBuilder getParent() { return parent; }

        static class Builder {
            private String name;
            private ParentWithBuilder parent;

            @JsonProperty("name")
            public Builder withName(String name) {
                this.name = name;
                return this;
            }

            @JsonProperty("parent")
            public Builder withParent(ParentWithBuilder parent) {
                this.parent = parent;
                return this;
            }

            public ChildWithBuilder build() {
                return new ChildWithBuilder(name, parent);
            }
        }
    }


    static class GrandParent {
        private String name;

        @JsonIgnoreProperties("grandParent")
        private List<ParentNested> parents = new ArrayList<>();

        public GrandParent() {}

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public List<ParentNested> getParents() { return parents; }
        public void setParents(List<ParentNested> parents) { this.parents = parents; }
    }

    static class ParentNested {
        private String name;
        private GrandParent grandParent;

        @JsonIgnoreProperties("parent")
        private List<ChildNested> children = new ArrayList<>();

        @JsonCreator
        public ParentNested(
                @JsonProperty("name") String name,
                @JsonProperty("grandParent") GrandParent grandParent,
                @JsonProperty("children") List<ChildNested> children) {
            this.name = name;
            this.grandParent = grandParent;
            this.children = children != null ? children : new ArrayList<>();
        }

        public String getName() { return name; }
        public GrandParent getGrandParent() { return grandParent; }
        public List<ChildNested> getChildren() { return children; }
    }

    static class ChildNested {
        private String name;
        private ParentNested parent;

        @JsonCreator
        public ChildNested(
                @JsonProperty("name") String name,
                @JsonProperty("parent") ParentNested parent) {
            this.name = name;
            this.parent = parent;
        }

        public String getName() { return name; }
        public ParentNested getParent() { return parent; }
    }

    record ParentRecord(
        String name,
        @JsonIgnoreProperties("parent") List<ChildRecord> children
    ) {}

    record ChildRecord(
        String name,
        ParentRecord parent
    ) {}


    /*
    /**********************************************************************
    /* Test methods
    /**********************************************************************
    */

    /**
     * This test demonstrates the race condition: deserializing a child first
     * causes the parent deserialization to fail with "No _valueDeserializer assigned"
     */
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

    @Test
    public void testJsonCreatorWithIgnoreProperties() throws Exception {
        String json = a2q("{'name':'Child1','parent':{'name':'Parent1','children':[{'name':'Child1'}]}}");

        ChildWithCreator child = MAPPER.readValue(json, ChildWithCreator.class);

        assertNotNull(child);
        assertEquals("Child1", child.getName());
        assertNotNull(child.getParent());
        assertEquals("Parent1", child.getParent().getName());

        List<ChildWithCreator> children = child.getParent().getChildren();
        assertEquals(1, children.size());
        assertNull(children.get(0).getParent());
    }

    @Test
    public void testBuilderWithIgnoreProperties() throws Exception {
        String json = a2q("{'name':'Child1','parent':{'name':'Parent1','children':[{'name':'Child1'}]}}");

        ChildWithBuilder child = MAPPER.readValue(json, ChildWithBuilder.class);

        assertNotNull(child);
        assertEquals("Child1", child.getName());
        assertNotNull(child.getParent());

        List<ChildWithBuilder> children = child.getParent().getChildren();
        assertEquals(1, children.size());
        assertNull(children.get(0).getParent());
    }

    @Test
    public void testMultiLevelNestedWithIgnoreProperties() throws Exception {
        String json = a2q("{'name':'Parent1','grandParent':{'name':'GrandParent1','parents':[{'name':'Parent1','children':[]}]},'children':[{'name':'Child1'}]}");

        ParentNested parent = MAPPER.readValue(json, ParentNested.class);

        assertNotNull(parent);
        assertEquals("Parent1", parent.getName());

        assertNotNull(parent.getGrandParent());
        List<ParentNested> nestedParents = parent.getGrandParent().getParents();
        assertEquals(1, nestedParents.size());
        assertNull(nestedParents.get(0).getGrandParent());

        List<ChildNested> children = parent.getChildren();
        assertEquals(1, children.size());
        assertNull(children.get(0).getParent());
    }

    @Test
    public void testRecordWithIgnoreProperties() throws Exception {
        String json = a2q("{'name':'Child1','parent':{'name':'Parent1','children':[{'name':'Child1'}]}}");

        ChildRecord child = MAPPER.readValue(json, ChildRecord.class);

        assertNotNull(child);
        assertEquals("Child1", child.name());
        assertNotNull(child.parent());

        List<ChildRecord> children = child.parent().children();
        assertEquals(1, children.size());
        assertNull(children.get(0).parent());
    }
}
