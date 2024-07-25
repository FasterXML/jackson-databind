package com.fasterxml.jackson.databind.deser.cases;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;

import java.util.List;

public class Animals3964 {
  /**
   * Lean version that fails and Without getters and setters
   */
  @JsonIdentityInfo(
      generator = ObjectIdGenerators.PropertyGenerator.class,
      property = "id",
      scope = Animal.class
  )
  public static class Animal {
    public final int id;
    public List<Cat> cats;

    @JsonCreator
    public Animal(@JsonProperty("id") int id, @JsonProperty("cats") List<Cat> cats) {
      this.id = id;
      this.cats = cats;
    }
  }

  @JsonIdentityInfo(
      generator = ObjectIdGenerators.PropertyGenerator.class,
      property = "id",
      scope = Cat.class
  )
  public static class Cat {
    public int id;
    public List<Food> foods;
    @JsonBackReference("id")
    public Animal animal;

    @JsonCreator
    public Cat(@JsonProperty("id") int id, @JsonProperty("foods") List<Food> foods) {
      this.id = id;
      this.foods = foods;
    }
  }

  @JsonIdentityInfo(
      generator = ObjectIdGenerators.PropertyGenerator.class,
      property = "id",
      scope = Food.class
  )
  public static class Food {
    public int id;
    public Cat cat;

    @JsonCreator
    public Food(@JsonProperty("id") int id) {
      this.id = id;
    }
  }

  /**
   * Passes : Testing lean without getters and setters
   * and also without {@link JsonCreator}.
   */
  @JsonIdentityInfo(
      generator = ObjectIdGenerators.PropertyGenerator.class,
      property = "id",
      scope = Fish.class
  )
  public static class Fish {
    public int id;
    public List<Squid> squids;
  }

  @JsonIdentityInfo(
      generator = ObjectIdGenerators.PropertyGenerator.class,
      property = "id",
      scope = Squid.class
  )
  public static class Squid {
    public int id;
    public List<Shrimp> shrimps;
    @JsonBackReference("id")
    public Fish fish;
  }

  @JsonIdentityInfo(
      generator = ObjectIdGenerators.PropertyGenerator.class,
      property = "id",
      scope = Shrimp.class
  )
  public static class Shrimp {
    public int id;
    public Squid squid;
  }
}
