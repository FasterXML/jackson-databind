package com.fasterxml.jackson.failing;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.BaseMapTest;
import com.fasterxml.jackson.databind.ObjectMapper;

// Test case for https://github.com/FasterXML/jackson-databind/issues/1298
public class TestObjectIdWithUnwrapping1298 extends BaseMapTest
{
    private static Long nextId = 1L;

    public static final class ListOfParents{
        public List<Parent> parents = new ArrayList<>();

        public void addParent( Parent parent) { parents.add(parent);}
    }

    @JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id", scope = Parent.class)
    public static final class Parent {
        public Long id;

        @JsonUnwrapped
        public Child child;
        public Parent() { this.id = nextId++;}
    }

    @JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id", scope = Child.class)
    public static final class Child
    {
        public Long id;

        public final String name;

        public Child(@JsonProperty("name") String name) {
            this.name = name;
            this.id = TestObjectIdWithUnwrapping1298.nextId++;
        }
    }

    public void testObjectIdWithRepeatedChild() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        mapper.disable(JsonGenerator.Feature.AUTO_CLOSE_JSON_CONTENT);

        // Equivalent to Spring _embedded for Bean w/ List property
        ListOfParents parents = new ListOfParents();

        //Bean with Relationship
        Parent parent1 = new Parent();
        Child child1 = new Child("Child1");
        parent1.child = child1;
        parents.addParent(parent1);

        // serialize parent1 and parent2
        String json = mapper
//                .writerWithDefaultPrettyPrinter()
                .writeValueAsString(parents);
        System.out.println("This works: " + json);

        // Add parent3 to create ObjectId reference
        // Bean w/ repeated relationship from parent1, should generate ObjectId
        Parent parent3 = new Parent();
        parent3.child = child1;
        parents.addParent(parent3);
        StringWriter sw = new StringWriter();

        try {
            mapper
//                .writerWithDefaultPrettyPrinter()
                .writeValue(sw, parents);
        } catch (Exception e) {
            System.out.println("Failed output so far: " + sw);
            throw e;
        }

        System.out.println("Also works: " + sw);
    }
}
