package com.fasterxml.jackson.failing;

import java.io.StringReader;
import java.util.*;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.databind.*;

public class ObjectWithCreator1261Test
    extends BaseMapTest
{
    static class Answer
    {
       public SortedMap<String, Parent> parents;

       public Answer() {
           parents = new TreeMap<String, Parent>();
       }
    }

    @JsonIdentityInfo(generator = ObjectIdGenerators.IntSequenceGenerator.class, property = "@id")
    static class Parent
    {
       public Map<String, Child> children;

       @JsonIdentityReference(alwaysAsId = true)
       public Child favoriteChild;

       public String name;

       protected Parent() { }
       
       public Parent(String name, boolean ignored) {
           children = new TreeMap<String, Child>();
           this.name = name;
       }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonIdentityInfo(generator = ObjectIdGenerators.IntSequenceGenerator.class, property = "@id")
    static class Child
    {
        public String name;

        @JsonIdentityReference(alwaysAsId = true)
        public Parent parent;

        @JsonIdentityReference(alwaysAsId = true)
        public List<Parent> parentAsList;

        public String someNullProperty;

        protected Child() { }
        
        @JsonCreator
        public Child(@JsonProperty("name") String name,
              @JsonProperty("someNullProperty") String someNullProperty) {
            this.name = name;
            this.someNullProperty = someNullProperty;
        }

        public Child(String n) {
            name = n;
        }
    }

    public void testObjectIds1261() throws Exception
    {
         ObjectMapper mapper = new ObjectMapper();
         mapper.enable(SerializationFeature.INDENT_OUTPUT);
         mapper.enable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY);

         Answer initialAnswer = createInitialAnswer();
         String initialAnswerString = mapper.writeValueAsString(initialAnswer);
// System.out.println("Initial answer:\n"+initialAnswerString);
         JsonNode tree = mapper.readTree(initialAnswerString);
         Answer deserializedAnswer = mapper.readValue(initialAnswerString,
               Answer.class);
         String reserializedAnswerString = mapper
               .writeValueAsString(deserializedAnswer);
         JsonNode newTree = mapper.readTree(reserializedAnswerString);
         if (!tree.equals(newTree)) {
                  fail("Original and recovered Json are different. Recovered = \n"
                        + reserializedAnswerString + "\n");
         }
   }

   private Answer createInitialAnswer() {
      Answer answer = new Answer();
      String child1Name = "child1";
      String child2Name = "child2";
      String parent1Name = "parent1";
      String parent2Name = "parent2";
      Parent parent1 = new Parent(parent1Name, false);
      answer.parents.put(parent1Name, parent1);
      Child child1 = new Child(child1Name);
      child1.parent = parent1;
      child1.parentAsList = Collections.singletonList(parent1);
      Child child2 = new Child(child2Name);
      Parent parent2 = new Parent(parent2Name, false);
      child2.parent = parent2;
      child2.parentAsList = Collections.singletonList(parent2);
      parent1.children.put(child1Name, child1);
      parent1.children.put(child2Name, child2);
      answer.parents.put(parent2Name, parent2);
      return answer;
   }    
    // 14-Jun-2016, tatu: Original test, has a cycle which may render it impossible
    //    to support (although better error message would be nice).
    //   Left here in case we'll fix the other problem above.

    /*
    static class Answer {
        private List<Parent> _parents;

        public Answer() {
           _parents = new ArrayList<Parent>();
        }

        @JsonCreator
        public Answer(@JsonProperty("parents") List<Parent> parents) {
           _parents = parents;
        }

        @JsonProperty("parents")
        public List<Parent> getParents() {
           return _parents;
        }

     }

    @JsonIdentityInfo(generator = ObjectIdGenerators.IntSequenceGenerator.class, property = "@id")
    static class Parent
    {
        public List<Child> children;

        @JsonIdentityReference(alwaysAsId = true)
        public Child aFavoriteChild;

        public Parent() {
            children = new ArrayList<Child>();        
        }

        @JsonCreator
        public Parent(@JsonProperty("children") List<Child> children
                ,@JsonProperty("aFavoriteChild") Child favoriteChild
                )
        {
           this.children = children;
           this.aFavoriteChild = favoriteChild;
        }
        
        public List<Child> getChildren() { return children; }

        @JsonIgnore
        public void setFavoriteChild(Child c) { aFavoriteChild = c; }
    }

    @JsonIdentityInfo(generator = ObjectIdGenerators.IntSequenceGenerator.class, property = "@id")
    static class Child
    {
        @JsonIdentityReference(alwaysAsId = true)
        public Parent parent;

        public List<Parent> parentAsList;

        public Child() { }
        public Child(Parent p) {
            parent = p;
            parentAsList = Collections.singletonList(parent);
        }
    }

    public void testWithCreators() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        mapper.enable(SerializationFeature.INDENT_OUTPUT);

        Answer initialAnswer = createInitialAnswer();
        String initialAnswerString = mapper.writeValueAsString(initialAnswer);
        Answer deserializedAnswer = mapper.readValue(initialAnswerString,
                Answer.class);
        String reserializedAnswerString = mapper
                .writeValueAsString(deserializedAnswer);
        JsonNode tree = mapper.readTree(initialAnswerString);
        JsonNode newTree = mapper.readTree(reserializedAnswerString);
        if (!tree.equals(newTree)) {
            System.err
                  .print("\nOriginal and recovered Json are different. Recovered = \n"
                        + reserializedAnswerString + "\n");
         }
   }

   private static Answer createInitialAnswer() {
      Answer answer = new Answer();
      Parent parent1 = new Parent();
      answer.getParents().add(parent1);
      Child child1 = new Child(parent1);
      Child child2 = new Child(parent1);
      Child child3 = new Child(parent1);
      Child child4 = new Child(parent1);
      parent1.getChildren().add(child1);
      parent1.getChildren().add(child2);
      parent1.getChildren().add(child3);
      parent1.getChildren().add(child4);
      Parent parent2 = new Parent();
      answer.getParents().add(parent2);
      Child child5 = new Child(parent2);
      parent2.getChildren().add(child5);
      parent1.setFavoriteChild(child5);
      return answer;
   }
*/
}
