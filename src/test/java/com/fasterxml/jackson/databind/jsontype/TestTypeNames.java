package com.fasterxml.jackson.databind.jsontype;

import java.util.*;


import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.annotation.JsonTypeInfo.As;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.jsontype.impl.StdSubtypeResolver;
import com.fasterxml.jackson.databind.type.TypeFactory;

/**
 * Separate tests for verifying that "type name" type id mechanism
 * works.
 */
public class TestTypeNames extends BaseMapTest
{
    @SuppressWarnings("serial")
    static class AnimalMap extends LinkedHashMap<String,Animal> { }

    @JsonTypeInfo(property = "type", include = JsonTypeInfo.As.PROPERTY, use = JsonTypeInfo.Id.NAME)
    @JsonSubTypes({
              @JsonSubTypes.Type(value = A1616.class,name = "A"),
              @JsonSubTypes.Type(value = B1616.class)
    })
    static abstract class Base1616 { }

    static class A1616 extends Base1616 { }

    @JsonTypeName("B")
    static class B1616 extends Base1616 { }

    /*
    /**********************************************************
    /* Unit tests
    /**********************************************************
     */

    private final ObjectMapper MAPPER = objectMapper();

    public void testBaseTypeId1616() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        Collection<NamedType> subtypes = new StdSubtypeResolver().collectAndResolveSubtypesByTypeId(
                mapper.getDeserializationConfig(),
                // note: `null` is fine here as `AnnotatedMember`:
                null,
                mapper.constructType(Base1616.class));
        assertEquals(2, subtypes.size());
        Set<String> ok = new HashSet<>(Arrays.asList("A", "B"));
        for (NamedType type : subtypes) {
            String id = type.getName();
            if (!ok.contains(id)) {
                fail("Unexpected id '"+id+"' (mapping to: "+type.getType()+"), should be one of: "+ok);
            }
        }
    }

    public void testSerialization() throws Exception
    {
        // Note: need to use wrapper array just so that we can define
        // static type on serialization. If we had root static types,
        // could use those; but at the moment root type is dynamic

        assertEquals("[{\"doggy\":{\"name\":\"Spot\",\"ageInYears\":3}}]",
                MAPPER.writeValueAsString(new Animal[] { new Dog("Spot", 3) }));
        assertEquals("[{\"MaineCoon\":{\"name\":\"Belzebub\",\"purrs\":true}}]",
                MAPPER.writeValueAsString(new Animal[] { new MaineCoon("Belzebub", true)}));
    }

    public void testRoundTrip() throws Exception
    {
        Animal[] input = new Animal[] {
                new Dog("Odie", 7),
                null,
                new MaineCoon("Piru", false),
                new Persian("Khomeini", true)
        };
        String json = MAPPER.writeValueAsString(input);
        List<Animal> output = MAPPER.readValue(json,
                TypeFactory.defaultInstance().constructCollectionType(ArrayList.class, Animal.class));
        assertEquals(input.length, output.size());
        for (int i = 0, len = input.length; i < len; ++i) {
            assertEquals("Entry #"+i+" differs, input = '"+json+"'",
                input[i], output.get(i));
        }
    }

    public void testRoundTripMap() throws Exception
    {
        AnimalMap input = new AnimalMap();
        input.put("venla", new MaineCoon("Venla", true));
        input.put("ama", new Dog("Amadeus", 13));
        String json = MAPPER.writeValueAsString(input);
        AnimalMap output = MAPPER.readValue(json, AnimalMap.class);
        assertNotNull(output);
        assertEquals(AnimalMap.class, output.getClass());
        assertEquals(input.size(), output.size());

        // for some reason, straight comparison won't work...
        for (String name : input.keySet()) {
            Animal in = input.get(name);
            Animal out = output.get(name);
            if (!in.equals(out)) {
                fail("Animal in input was ["+in+"]; output not matching: ["+out+"]");
            }
        }
    }
}

/*
/**********************************************************
/* Helper types
/**********************************************************
 */

@JsonTypeInfo(use=Id.NAME, include=As.WRAPPER_OBJECT)
@JsonSubTypes({
    @Type(value=Dog.class, name="doggy"),
    @Type(Cat.class) /* defaults to "TestTypedNames$Cat" then */
})
class Animal
{
    public String name;


    @Override
    public boolean equals(Object o) {
        if (o == this) return true;
        if (o == null) return false;
        if (o.getClass() != getClass()) return false;
        return name.equals(((Animal) o).name);
    }

    @Override
    public String toString() {
        return getClass().toString() + "('"+name+"')";
    }
}

class Dog extends Animal
{
    public int ageInYears;

    public Dog() { }
    public Dog(String n, int y) {
        name = n;
        ageInYears = y;
    }

    @Override
    public boolean equals(Object o) {
        return super.equals(o)
            && ((Dog) o).ageInYears == ageInYears;
    }
}

@JsonSubTypes({
    @Type(MaineCoon.class),
    @Type(Persian.class)
})
abstract class Cat extends Animal {
    public boolean purrs;
    public Cat() { }
    public Cat(String n, boolean p) {
        name = n;
        purrs = p;
    }

    @Override
    public boolean equals(Object o) {
        return super.equals(o) && ((Cat) o).purrs == purrs;
    }

    @Override
    public String toString() {
        return super.toString()+"(purrs: "+purrs+")";
    }
}

/* uses default name ("MaineCoon") since there's no @JsonTypeName,
 * nor did supertype specify name
 */
class MaineCoon extends Cat {
    public MaineCoon() { super(); }
    public MaineCoon(String n, boolean p) {
        super(n, p);
    }
}

@JsonTypeName("persialaisKissa")
class Persian extends Cat {
    public Persian() { super(); }
    public Persian(String n, boolean p) {
        super(n, p);
    }
}

