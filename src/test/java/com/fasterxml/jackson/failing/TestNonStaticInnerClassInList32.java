package com.fasterxml.jackson.failing;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.*;

public class TestNonStaticInnerClassInList32 extends BaseMapTest
{
    public static class Dog2
    {
        public String name;
        public List<Leg> legs;

        // NOTE: non-static on purpose!
        public class Leg {
            public int length;
        }
    }

    /*
    /**********************************************************
    /* Tests
    /**********************************************************
     */

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
//        System.out.println(dogJson);
      // output: {"name":"Spike","legs":[{length: 5}, {length: 4}]}

        Dog2 dogCopy = mapper.readValue(dogJson, Dog2.class);
        assertEquals(dogCopy.legs.get(1).length, 4);
        // prefer fully populated Dog instance
    }
}


