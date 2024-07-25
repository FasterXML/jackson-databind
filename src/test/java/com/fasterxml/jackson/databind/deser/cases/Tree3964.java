package com.fasterxml.jackson.databind.deser.cases;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;

import java.util.List;

/**
 * Original test
 */
@JsonIdentityInfo(
    generator = ObjectIdGenerators.PropertyGenerator.class,
    property = "id",
    scope = Tree3964.class
)
public class Tree3964 {
  protected final int id;
  public List<Fruit3964> fruits;

  @JsonCreator
  public Tree3964(@JsonProperty("id") int id, @JsonProperty("fruits") List<Fruit3964> fruits) {
    this.id = id;
    this.fruits = fruits;
  }

  public int getId() {
    return id;
  }

  public List<Fruit3964> getFruits() {
    return fruits;
  }

  public void setFruits(List<Fruit3964> fruits) {
    this.fruits = fruits;
  }
}
