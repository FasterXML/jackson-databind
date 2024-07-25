package com.fasterxml.jackson.databind.deser.cases;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;

@JsonIdentityInfo(
    generator = ObjectIdGenerators.PropertyGenerator.class,
    property = "id",
    scope = Calories3964.class
)
public class Calories3964 {
  protected final int id;
  protected Fruit3964 fruit;

  @JsonCreator
  public Calories3964(@JsonProperty("id") int id) {
    this.id = id;
  }

  public int getId() {
    return id;
  }

  public Fruit3964 getFruit() {
    return fruit;
  }

  public void setFruit(Fruit3964 fruit) {
    this.fruit = fruit;
  }
}

