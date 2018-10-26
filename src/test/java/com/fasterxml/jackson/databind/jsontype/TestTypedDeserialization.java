package com.fasterxml.jackson.databind.jsontype;

import java.io.IOException;
import java.util.*;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo.As;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.type.TypeFactory;

public class TestTypedDeserialization
    extends BaseMapTest
{
    /*
    /**********************************************************
    /* Helper types
    /**********************************************************
     */

    /**
     * Polymorphic base class
     */
    @JsonTypeInfo(use=Id.CLASS, include=As.PROPERTY, property="@classy")
    static abstract class Animal {
        public String name;
        
        protected Animal(String n)  { name = n; }
    }

    @JsonTypeName("doggie")
    static class Dog extends Animal
    {
        public int boneCount;
        
        @JsonCreator
        public Dog(@JsonProperty("name") String name) {
            super(name);
        }

        public void setBoneCount(int i) { boneCount = i; }
    }
    
    @JsonTypeName("kitty")
    static class Cat extends Animal
    {
        public String furColor;

        @JsonCreator
        public Cat(@JsonProperty("furColor") String c) {
            super(null);
            furColor = c;
        }

        public void setName(String n) { name = n; }
    }

    // for [JACKSON-319] -- allow "empty" beans
    @JsonTypeName("fishy")
    static class Fish extends Animal
    {
        @JsonCreator
        public Fish()
        {
            super(null);
        }
    }

    static class AnimalContainer {
        public Animal animal;
    }

    // base class with no useful info
    @JsonTypeInfo(use=Id.CLASS, include=As.WRAPPER_ARRAY)
    static abstract class DummyBase {
        protected DummyBase(boolean foo) { }
    }

    static class DummyImpl extends DummyBase {
        public int x;

        public DummyImpl() { super(true); }
    }
    
    @JsonTypeInfo(use=Id.MINIMAL_CLASS, include=As.WRAPPER_OBJECT)
    interface TypeWithWrapper { }

    @JsonTypeInfo(use=Id.CLASS, include=As.WRAPPER_ARRAY)
    interface TypeWithArray { }

    static class Issue506DateBean {
        @JsonTypeInfo(use = Id.NAME, include = As.PROPERTY, property = "type2")
        public Date date;
    }
        
    static class Issue506NumberBean
    {
        @JsonTypeInfo(use = Id.NAME, include = As.PROPERTY, property = "type3")
        @JsonSubTypes({ @Type(Long.class),
            @Type(Integer.class) })
        public Number number;
    }
    
    /*
    /**********************************************************
    /* Unit tests
    /**********************************************************
     */

    private final ObjectMapper MAPPER = newJsonMapper();

    /**
     * First things first, let's ensure we can serialize using
     * class name, written as main-level property name
     */
    public void testSimpleClassAsProperty() throws Exception
    {
        Animal a = MAPPER.readValue(asJSONObjectValueString(MAPPER,
                "@classy", Cat.class.getName(),
                "furColor", "tabby", "name", "Garfield"), Animal.class);
        assertNotNull(a);
        assertEquals(Cat.class, a.getClass());
        Cat c = (Cat) a;
        assertEquals("Garfield", c.name);
        assertEquals("tabby", c.furColor);
    }

    // Test inclusion using wrapper style
    public void testTypeAsWrapper() throws Exception
    {
        ObjectMapper m = jsonMapperBuilder()
                .addMixIn(Animal.class, TypeWithWrapper.class)
                .build();
        String JSON = "{\".TestTypedDeserialization$Dog\" : "
            +asJSONObjectValueString(m, "name", "Scooby", "boneCount", "6")+" }";
        Animal a = m.readValue(JSON, Animal.class);
        assertTrue(a instanceof Animal);
        assertEquals(Dog.class, a.getClass());
        Dog d = (Dog) a;
        assertEquals("Scooby", d.name);
        assertEquals(6, d.boneCount);
    }

    // Test inclusion using 2-element array
    public void testTypeAsArray() throws Exception
    {
        ObjectMapper m = jsonMapperBuilder()
                .addMixIn(Animal.class, TypeWithArray.class)
                .build();
        // hmmh. Not good idea to rely on exact output, order may change. But...
        String JSON = "[\""+Dog.class.getName()+"\", "
            +asJSONObjectValueString(m, "name", "Martti", "boneCount", "11")+" ]";
        Animal a = m.readValue(JSON, Animal.class);
        assertEquals(Dog.class, a.getClass());
        Dog d = (Dog) a;
        assertEquals("Martti", d.name);
        assertEquals(11, d.boneCount);
    }

    // Use basic Animal as contents of a regular List
    public void testListAsArray() throws Exception
    {
        // This time using PROPERTY style (default) again
        String JSON = "[\n"
                +asJSONObjectValueString(MAPPER,
                        "@classy", Cat.class.getName(), "name", "Hello", "furColor", "white")
                +",\n"
                // let's shuffle doggy's fields a bit for testing
                +asJSONObjectValueString(MAPPER,
                        "boneCount", Integer.valueOf(1),
                        "@classy", Dog.class.getName(),
                        "name", "Bob"
                        )
                +",\n"
                +asJSONObjectValueString(MAPPER,
                        "@classy", Fish.class.getName())
                +", null\n]";
        
        JavaType expType = TypeFactory.defaultInstance().constructCollectionType(ArrayList.class, Animal.class);
        List<Animal> animals = MAPPER.readValue(JSON, expType);
        assertNotNull(animals);
        assertEquals(4, animals.size());
        Cat c = (Cat) animals.get(0);
        assertEquals("Hello", c.name);
        assertEquals("white", c.furColor);
        Dog d = (Dog) animals.get(1);
        assertEquals("Bob", d.name);
        assertEquals(1, d.boneCount);
        Fish f = (Fish) animals.get(2);
        assertNotNull(f);
        assertNull(animals.get(3));
    }

    public void testCagedAnimal() throws Exception
    {
        String jsonCat = asJSONObjectValueString(MAPPER,
                "@classy", Cat.class.getName(), "name", "Nilson", "furColor", "black");
        String JSON = "{\"animal\":"+jsonCat+"}";

        AnimalContainer cont = MAPPER.readValue(JSON, AnimalContainer.class);
        assertNotNull(cont);
        Animal a = cont.animal;
        assertNotNull(a);
        Cat c = (Cat) a;
        assertEquals("Nilson", c.name);
        assertEquals("black", c.furColor);
    }

    /**
     * Test that verifies that there are few limitations on polymorphic
     * base class.
     */
    public void testAbstractEmptyBaseClass() throws Exception
    {
        DummyBase result = new ObjectMapper().readValue(
                "[\""+DummyImpl.class.getName()+"\",{\"x\":3}]", DummyBase.class);
        assertNotNull(result);
        assertEquals(DummyImpl.class, result.getClass());
        assertEquals(3, ((DummyImpl) result).x);
    }

    // [JACKSON-506], wrt Date
    public void testIssue506WithDate() throws Exception
    {
        Issue506DateBean input = new Issue506DateBean();
        input.date = new Date(1234L);

        String json = MAPPER.writeValueAsString(input);

        Issue506DateBean output = MAPPER.readValue(json, Issue506DateBean.class);
        assertEquals(input.date, output.date);
    }
    
    // [JACKSON-506], wrt Number
    public void testIssue506WithNumber() throws Exception
    {
        Issue506NumberBean input = new Issue506NumberBean();
        input.number = Long.valueOf(4567L);

        String json = MAPPER.writeValueAsString(input);

        Issue506NumberBean output = MAPPER.readValue(json, Issue506NumberBean.class);
        assertEquals(input.number, output.number);
    }

    private String asJSONObjectValueString(ObjectMapper mapper, Object... args) throws IOException
    {
        LinkedHashMap<Object,Object> map = new LinkedHashMap<Object,Object>();
        for (int i = 0, len = args.length; i < len; i += 2) {
            map.put(args[i], args[i+1]);
        }
        return mapper.writeValueAsString(map);
    }
}


