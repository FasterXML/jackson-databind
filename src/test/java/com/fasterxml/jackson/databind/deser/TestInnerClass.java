package com.fasterxml.jackson.databind.deser;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.*;

public class TestInnerClass extends BaseMapTest
{
    // [JACKSON-594]
    static class Dog
    {
      public String name;
      public Brain brain;

      public Dog() { }
      public Dog(String n, boolean thinking) {
          name = n;
          brain = new Brain();
          brain.isThinking = thinking;
      }
      
      // note: non-static
      public class Brain {
          public boolean isThinking;

          public String parentName() { return name; }
      }
    }

    /*
    /**********************************************************
    /* Tests
    /**********************************************************
     */

    public void testSimpleNonStaticInner() throws Exception
    {
        // Let's actually verify by first serializing, then deserializing back
        ObjectMapper mapper = new ObjectMapper();
        Dog input = new Dog("Smurf", true);
        String json = mapper.writeValueAsString(input);
        Dog output = mapper.readValue(json, Dog.class);
        assertEquals("Smurf", output.name);
        assertNotNull(output.brain);
        assertTrue(output.brain.isThinking);
        // and verify correct binding...
        assertEquals("Smurf", output.brain.parentName());
        output.name = "Foo";
        assertEquals("Foo", output.brain.parentName());
    }

    // core/[Issue#32]
    public void testInnerList() throws Exception
    {
        Dog2 dog = new Dog2();
        dog.name = "Spike";
        dog.legs = new ArrayList<Dog2.Leg>();
        dog.legs.add(dog.new Leg());
        dog.legs.add(dog.new Leg());
        dog.legs.get(0).length = 5;
        dog.legs.get(1).length = 4;

        ObjectMapper mapper = new ObjectMapper();

        String dogJson = mapper.writeValueAsString(dog);
        System.out.println(dogJson);
      // output: {"name":"Spike","legs":[{length: 5}, {length: 4}]}

        // currently throws JsonMappingException
        Dog2 dogCopy = mapper.readValue(dogJson, Dog2.class);
        assertEquals(dogCopy.legs.get(1).length, 4);
        // prefer fully populated Dog instance
    }

    public static class Dog2
    {
      public String name;
      public List<Leg> legs;

      public class Leg {
        public int length;
      }
    }
}
