package com.fasterxml.jackson.databind.jsontype;

import java.util.*;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo.As;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
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

    // Allow "empty" beans
    @JsonTypeName("fishy")
    static class Fish extends Animal
    {
        @JsonCreator
        public Fish()
        {
            super(null);
        }
    }

    // [databind#2467]: Allow missing "content" for as-array deserialization
    @JsonDeserialize(using = NullAnimalDeserializer.class)
    static class NullAnimal extends Animal
    {
        public static final NullAnimal NULL_INSTANCE = new NullAnimal();

        public NullAnimal() {
            super(null);
        }
    }

    static class NullAnimalDeserializer extends JsonDeserializer<NullAnimal> {
        @Override
        public NullAnimal getNullValue(final DeserializationContext context) {
            return NullAnimal.NULL_INSTANCE;
        }

        @Override
        public NullAnimal deserialize(final JsonParser parser, final DeserializationContext context) {
            throw new UnsupportedOperationException();
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

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.WRAPPER_ARRAY)
    @JsonSubTypes({ @Type(value = Issue1751ArrImpl.class, name = "0") })
    static interface Issue1751ArrBase { }

    static class Issue1751ArrImpl implements Issue1751ArrBase { }

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY,
            property = "type")
    @JsonSubTypes({ @Type(value = Issue1751PropImpl.class, name = "1") })
    static interface Issue1751PropBase { }

    static class Issue1751PropImpl implements Issue1751PropBase { }

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
        Animal a = MAPPER.readValue(asJSONObjectValueString("@classy", Cat.class.getName(),
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
        ObjectMapper m = new ObjectMapper();
        m.addMixIn(Animal.class, TypeWithWrapper.class);
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
        ObjectMapper m = new ObjectMapper();
        m.addMixIn(Animal.class, TypeWithArray.class);
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
        ObjectMapper m = MAPPER;
        // This time using PROPERTY style (default) again
        String JSON = "[\n"
            +asJSONObjectValueString(m, "@classy", Cat.class.getName(), "name", "Hello", "furColor", "white")
            +",\n"
            // let's shuffle doggy's fields a bit for testing
            +asJSONObjectValueString(m,
                                     "boneCount", Integer.valueOf(1),
                                     "@classy", Dog.class.getName(),
                                     "name", "Bob"
                                     )
            +",\n"
            +asJSONObjectValueString(m, "@classy", Fish.class.getName())
            +", null\n]";

        JavaType expType = TypeFactory.defaultInstance().constructCollectionType(ArrayList.class, Animal.class);
        List<Animal> animals = m.readValue(JSON, expType);
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
        String jsonCat = asJSONObjectValueString(MAPPER, "@classy", Cat.class.getName(), "name", "Nilson", "furColor", "black");
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
        DummyBase result = MAPPER.readValue(
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

    // [databind#1751]: allow ints as ids too
    public void testIntAsTypeId1751Array() throws Exception
    {
        Issue1751ArrBase value;

        // Should allow both String and Int:
        value = MAPPER.readValue("[0, { }]", Issue1751ArrBase.class);
        assertNotNull(value);
        assertEquals(Issue1751ArrImpl.class, value.getClass());

        value = MAPPER.readValue("[\"0\", { }]", Issue1751ArrBase.class);
        assertNotNull(value);
        assertEquals(Issue1751ArrImpl.class, value.getClass());
    }

    // [databind#1751]: allow ints as ids too
    public void testIntAsTypeId1751Prop() throws Exception
    {
        Issue1751PropBase value;

        // Should allow both String and Int:
        value = MAPPER.readValue("{\"type\" : \"1\"}", Issue1751PropBase.class);
        assertNotNull(value);
        assertEquals(Issue1751PropImpl.class, value.getClass());

        value = MAPPER.readValue("{\"type\" : 1}", Issue1751PropBase.class);
        assertNotNull(value);
        assertEquals(Issue1751PropImpl.class, value.getClass());
    }

    // [databind#2467]: Allow missing "content" for as-array deserialization
    public void testTypeAsArrayWithNullableType() throws Exception
    {
        ObjectMapper m = new ObjectMapper();
        m.addMixIn(Animal.class, TypeWithArray.class);
        Animal a = m.readValue(
                "[\""+Fish.class.getName()+"\"]", Animal.class);
        assertNull(a);
    }

    // [databind#2467]
    public void testTypeAsArrayWithCustomDeserializer() throws Exception
    {
        ObjectMapper m = new ObjectMapper();
        m.addMixIn(Animal.class, TypeWithArray.class);
        Animal a = m.readValue(
                "[\""+NullAnimal.class.getName()+"\"]", Animal.class);
        assertNotNull(a);
        assertEquals(NullAnimal.class, a.getClass());
        NullAnimal c = (NullAnimal) a;
        assertNull(c.name);
    }
}


