package com.fasterxml.jackson.failing;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.annotation.*;

import com.fasterxml.jackson.databind.*;

public class ObjectWithCreator1261Test
    extends BaseMapTest
{
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

}
