package com.fasterxml.jackson.databind.deser.cases;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;

import java.util.List;

@JsonIdentityInfo(
    generator = ObjectIdGenerators.PropertyGenerator.class,
    property = "id",
    scope = Fruit3964.class
)
public class Fruit3964 {
  protected final int id;
  protected List<Calories3964> calories;

  @JsonBackReference("id")
  protected Tree3964 tree;

  @JsonCreator
  public Fruit3964(@JsonProperty("id") int id, @JsonProperty("calories") List<Calories3964> calories) {
    this.id = id;
    this.calories = calories;
  }

  public int getId() {
    return id;
  }

  public Tree3964 getTree() {
    return tree;
  }

  public void setTree(Tree3964 tree) {
    this.tree = tree;
  }

  public List<Calories3964> getCalories() {
    return calories;
  }

  public void setCalories(List<Calories3964> calories) {
    this.calories = calories;
  }
}
