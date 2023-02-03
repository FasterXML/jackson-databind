package com.fasterxml.jackson.databind.jsontype;

import java.util.*;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo.As;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;

import com.fasterxml.jackson.core.type.TypeReference;

import com.fasterxml.jackson.databind.BaseMapTest;
import com.fasterxml.jackson.databind.ObjectMapper;

public class ExistingPropertyTest extends BaseMapTest
{
    /**
     * Polymorphic base class - existing property as simple property on subclasses
     */
    @JsonTypeInfo(use = Id.NAME, include = As.EXISTING_PROPERTY, property = "type",
            visible=true)
    @JsonSubTypes({
        @Type(value = Apple.class, name = "apple") ,
        @Type(value = Orange.class, name = "orange")
    })
    static abstract class Fruit {
        public String name;
        protected Fruit(String n)  { name = n; }
    }

    @JsonTypeName("apple")
    @JsonPropertyOrder({ "name", "seedCount", "type" })
    static class Apple extends Fruit
    {
        public int seedCount;
        public String type;

        private Apple() { super(null); }
        public Apple(String name, int b) {
            super(name);
            seedCount = b;
            type = "apple";
        }
    }

    @JsonTypeName("orange")
    @JsonPropertyOrder({ "name", "color", "type" })
    static class Orange extends Fruit
    {
        public String color;
        public String type;

        private Orange() { super(null); }
        public Orange(String name, String c) {
            super(name);
            color = c;
            type = "orange";
        }
    }

    static class FruitWrapper {
        public Fruit fruit;
        public FruitWrapper() {}
        public FruitWrapper(Fruit f) { fruit = f; }
    }

    /**
     * Polymorphic base class - existing property forced by abstract method
     */
	@JsonTypeInfo(use = Id.NAME, include = As.EXISTING_PROPERTY, property = "type")
	@JsonSubTypes({
		@Type(value = Dog.class, name = "doggie") ,
		@Type(value = Cat.class, name = "kitty")
		})
	static abstract class Animal {
        public String name;

        protected Animal(String n)  { name = n; }

        public abstract String getType();
    }

    @JsonTypeName("doggie")
    static class Dog extends Animal
    {
        public int boneCount;

        private Dog() { super(null); }
        public Dog(String name, int b) {
            super(name);
            boneCount = b;
        }

 		@Override
		public String getType() {
        	return "doggie";
        }
    }

    @JsonTypeName("kitty")
    static class Cat extends Animal
    {
        public String furColor;

        private Cat() { super(null); }
        public Cat(String name, String c) {
            super(name);
            furColor = c;
        }

		@Override
		public String getType() {
        	return "kitty";
        }
    }

    static class AnimalWrapper {
        public Animal animal;
        public AnimalWrapper() {}
        public AnimalWrapper(Animal a) { animal = a; }
    }

    /**
     * Polymorphic base class - existing property NOT forced by abstract method on base class
     */
	@JsonTypeInfo(use = Id.NAME, include = As.EXISTING_PROPERTY, property = "type")
	@JsonSubTypes({
		@Type(value = Accord.class, name = "accord") ,
		@Type(value = Camry.class, name = "camry")
		})
	static abstract class Car {
        public String name;
        protected Car(String n)  { name = n; }
    }

    @JsonTypeName("accord")
    static class Accord extends Car
    {
        public int speakerCount;

        private Accord() { super(null); }
        public Accord(String name, int b) {
            super(name);
            speakerCount = b;
        }

		public String getType() {
        	return "accord";
        }
    }

    @JsonTypeName("camry")
    static class Camry extends Car
    {
        public String exteriorColor;

        private Camry() { super(null); }
        public Camry(String name, String c) {
            super(name);
            exteriorColor = c;
        }

		public String getType() {
        	return "camry";
        }
    }

    static class CarWrapper {
        public Car car;
        public CarWrapper() {}
        public CarWrapper(Car c) { car = c; }
    }

    // for [databind#1635]

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME,
            include = JsonTypeInfo.As.EXISTING_PROPERTY,
            // IMPORTANT! Must be defined as `visible`
            visible=true,
            property = "type",
            defaultImpl=Bean1635Default.class)
    @JsonSubTypes({ @JsonSubTypes.Type(Bean1635A.class) })
    static class Bean1635 {
        public ABC type;
    }

    @JsonTypeName("A")
    static class Bean1635A extends Bean1635 {
        public int value;
    }

    static class Bean1635Default extends Bean1635 { }

    // [databind#3271]
    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY,
            visible = true, property = "type", defaultImpl = DefaultShape3271.class)
    @JsonSubTypes({@JsonSubTypes.Type(value = Square3271.class, name = "square")})
    static abstract class Shape3271 {
        public String type;

        public String getType() { return this.type; }

        public void setType(String type) { this.type = type; }
    }

    static class Square3271 extends Shape3271 {}

    static class DefaultShape3271 extends Shape3271 {}

    /*
    /**********************************************************************
    /* Mock data
    /**********************************************************************
     */

    private static final Orange mandarin = new Orange("Mandarin Orange", "orange");
    private static final String mandarinJson = "{\"name\":\"Mandarin Orange\",\"color\":\"orange\",\"type\":\"orange\"}";
    private static final Apple pinguo = new Apple("Apple-A-Day", 16);
    private static final String pinguoJson = "{\"name\":\"Apple-A-Day\",\"seedCount\":16,\"type\":\"apple\"}";
    private static final FruitWrapper pinguoWrapper = new FruitWrapper(pinguo);
    private static final String pinguoWrapperJson = "{\"fruit\":" + pinguoJson + "}";
    private static final List<Fruit> fruitList = Arrays.asList(pinguo, mandarin);
    private static final String fruitListJson = "[" + pinguoJson + "," + mandarinJson + "]";

    private static final Cat beelzebub = new Cat("Beelzebub", "tabby");
    private static final String beelzebubJson = "{\"name\":\"Beelzebub\",\"furColor\":\"tabby\",\"type\":\"kitty\"}";
    private static final Dog rover = new Dog("Rover", 42);
    private static final String roverJson = "{\"name\":\"Rover\",\"boneCount\":42,\"type\":\"doggie\"}";
    private static final AnimalWrapper beelzebubWrapper = new AnimalWrapper(beelzebub);
    private static final String beelzebubWrapperJson = "{\"animal\":" + beelzebubJson + "}";
    private static final List<Animal> animalList = Arrays.asList(beelzebub, rover);
    private static final String animalListJson = "[" + beelzebubJson + "," + roverJson + "]";

    private static final Camry camry = new Camry("Sweet Ride", "candy-apple-red");
    private static final String camryJson = "{\"name\":\"Sweet Ride\",\"exteriorColor\":\"candy-apple-red\",\"type\":\"camry\"}";
    private static final Accord accord = new Accord("Road Rage", 6);
    private static final String accordJson = "{\"name\":\"Road Rage\",\"speakerCount\":6,\"type\":\"accord\"}";
    private static final CarWrapper camryWrapper = new CarWrapper(camry);
    private static final String camryWrapperJson = "{\"car\":" + camryJson + "}";
    private static final List<Car> carList = Arrays.asList(camry, accord);
    private static final String carListJson = "[" + camryJson + "," + accordJson + "]";

    /*
    /**********************************************************************
    /* Test methods
    /**********************************************************************
     */

    private final ObjectMapper MAPPER = newJsonMapper();

    /**
     * Fruits - serialization tests for simple property on sub-classes
     */
    public void testExistingPropertySerializationFruits() throws Exception
    {
        Map<String,Object> result = writeAndMap(MAPPER, pinguo);
        assertEquals(3, result.size());
        assertEquals(pinguo.name, result.get("name"));
        assertEquals(pinguo.seedCount, result.get("seedCount"));
        assertEquals(pinguo.type, result.get("type"));

        result = writeAndMap(MAPPER, mandarin);
        assertEquals(3, result.size());
        assertEquals(mandarin.name, result.get("name"));
        assertEquals(mandarin.color, result.get("color"));
        assertEquals(mandarin.type, result.get("type"));

        String pinguoSerialized = MAPPER.writeValueAsString(pinguo);
        assertEquals(pinguoSerialized, pinguoJson);

        String mandarinSerialized = MAPPER.writeValueAsString(mandarin);
        assertEquals(mandarinSerialized, mandarinJson);

        String fruitWrapperSerialized = MAPPER.writeValueAsString(pinguoWrapper);
        assertEquals(fruitWrapperSerialized, pinguoWrapperJson);

        String fruitListSerialized = MAPPER.writeValueAsString(fruitList);
        assertEquals(fruitListSerialized, fruitListJson);
    }

    /**
     * Fruits - deserialization tests for simple property on sub-classes
     */
    public void testSimpleClassAsExistingPropertyDeserializationFruits() throws Exception
    {
        Fruit pinguoDeserialized = MAPPER.readValue(pinguoJson, Fruit.class);
        assertTrue(pinguoDeserialized instanceof Apple);
        assertSame(pinguoDeserialized.getClass(), Apple.class);
        assertEquals(pinguo.name, pinguoDeserialized.name);
        assertEquals(pinguo.seedCount, ((Apple) pinguoDeserialized).seedCount);
        assertEquals(pinguo.type, ((Apple) pinguoDeserialized).type);

        FruitWrapper pinguoWrapperDeserialized = MAPPER.readValue(pinguoWrapperJson, FruitWrapper.class);
        Fruit pinguoExtracted = pinguoWrapperDeserialized.fruit;
        assertTrue(pinguoExtracted instanceof Apple);
        assertSame(pinguoExtracted.getClass(), Apple.class);
        assertEquals(pinguo.name, pinguoExtracted.name);
        assertEquals(pinguo.seedCount, ((Apple) pinguoExtracted).seedCount);
        assertEquals(pinguo.type, ((Apple) pinguoExtracted).type);

        Fruit[] fruits = MAPPER.readValue(fruitListJson, Fruit[].class);
        assertEquals(2, fruits.length);
        assertEquals(Apple.class, fruits[0].getClass());
        assertEquals("apple", ((Apple) fruits[0]).type);
        assertEquals(Orange.class, fruits[1].getClass());
        assertEquals("orange", ((Orange) fruits[1]).type);

        List<Fruit> f2 = MAPPER.readValue(fruitListJson,
                new TypeReference<List<Fruit>>() { });
        assertNotNull(f2);
        assertTrue(f2.size() == 2);
        assertEquals(Apple.class, f2.get(0).getClass());
        assertEquals(Orange.class, f2.get(1).getClass());
    }

    /**
     * Animals - serialization tests for abstract method in base class
     */
    public void testExistingPropertySerializationAnimals() throws Exception
    {
        Map<String,Object> result = writeAndMap(MAPPER, beelzebub);
        assertEquals(3, result.size());
        assertEquals(beelzebub.name, result.get("name"));
        assertEquals(beelzebub.furColor, result.get("furColor"));
        assertEquals(beelzebub.getType(), result.get("type"));

        result = writeAndMap(MAPPER, rover);
        assertEquals(3, result.size());
        assertEquals(rover.name, result.get("name"));
        assertEquals(rover.boneCount, result.get("boneCount"));
        assertEquals(rover.getType(), result.get("type"));

        String beelzebubSerialized = MAPPER.writeValueAsString(beelzebub);
        assertEquals(beelzebubSerialized, beelzebubJson);

        String roverSerialized = MAPPER.writeValueAsString(rover);
        assertEquals(roverSerialized, roverJson);

        String animalWrapperSerialized = MAPPER.writeValueAsString(beelzebubWrapper);
        assertEquals(animalWrapperSerialized, beelzebubWrapperJson);

        String animalListSerialized = MAPPER.writeValueAsString(animalList);
        assertEquals(animalListSerialized, animalListJson);
    }

    /**
     * Animals - deserialization tests for abstract method in base class
     */
    public void testSimpleClassAsExistingPropertyDeserializationAnimals() throws Exception
    {
        Animal beelzebubDeserialized = MAPPER.readValue(beelzebubJson, Animal.class);
        assertTrue(beelzebubDeserialized instanceof Cat);
        assertSame(beelzebubDeserialized.getClass(), Cat.class);
        assertEquals(beelzebub.name, beelzebubDeserialized.name);
        assertEquals(beelzebub.furColor, ((Cat) beelzebubDeserialized).furColor);
        assertEquals(beelzebub.getType(), beelzebubDeserialized.getType());

        AnimalWrapper beelzebubWrapperDeserialized = MAPPER.readValue(beelzebubWrapperJson, AnimalWrapper.class);
        Animal beelzebubExtracted = beelzebubWrapperDeserialized.animal;
        assertTrue(beelzebubExtracted instanceof Cat);
        assertSame(beelzebubExtracted.getClass(), Cat.class);
        assertEquals(beelzebub.name, beelzebubExtracted.name);
        assertEquals(beelzebub.furColor, ((Cat) beelzebubExtracted).furColor);
        assertEquals(beelzebub.getType(), beelzebubExtracted.getType());

        @SuppressWarnings("unchecked")
        List<Animal> animalListDeserialized = MAPPER.readValue(animalListJson, List.class);
        assertNotNull(animalListDeserialized);
        assertTrue(animalListDeserialized.size() == 2);
        Animal cat = MAPPER.convertValue(animalListDeserialized.get(0), Animal.class);
        assertTrue(cat instanceof Cat);
        assertSame(cat.getClass(), Cat.class);
        Animal dog = MAPPER.convertValue(animalListDeserialized.get(1), Animal.class);
        assertTrue(dog instanceof Dog);
        assertSame(dog.getClass(), Dog.class);
    }

    /**
     * Cars - serialization tests for no abstract method or type variable in base class
     */
    public void testExistingPropertySerializationCars() throws Exception
    {
        Map<String,Object> result = writeAndMap(MAPPER, camry);
        assertEquals(3, result.size());
        assertEquals(camry.name, result.get("name"));
        assertEquals(camry.exteriorColor, result.get("exteriorColor"));
        assertEquals(camry.getType(), result.get("type"));

        result = writeAndMap(MAPPER, accord);
        assertEquals(3, result.size());
        assertEquals(accord.name, result.get("name"));
        assertEquals(accord.speakerCount, result.get("speakerCount"));
        assertEquals(accord.getType(), result.get("type"));

        String camrySerialized = MAPPER.writeValueAsString(camry);
        assertEquals(camrySerialized, camryJson);

        String accordSerialized = MAPPER.writeValueAsString(accord);
        assertEquals(accordSerialized, accordJson);

        String carWrapperSerialized = MAPPER.writeValueAsString(camryWrapper);
        assertEquals(carWrapperSerialized, camryWrapperJson);

        String carListSerialized = MAPPER.writeValueAsString(carList);
        assertEquals(carListSerialized, carListJson);
    }

    /**
     * Cars - deserialization tests for no abstract method or type variable in base class
     */
    public void testSimpleClassAsExistingPropertyDeserializationCars() throws Exception
    {
        Car camryDeserialized = MAPPER.readValue(camryJson, Camry.class);
        assertTrue(camryDeserialized instanceof Camry);
        assertSame(camryDeserialized.getClass(), Camry.class);
        assertEquals(camry.name, camryDeserialized.name);
        assertEquals(camry.exteriorColor, ((Camry) camryDeserialized).exteriorColor);
        assertEquals(camry.getType(), ((Camry) camryDeserialized).getType());

        CarWrapper camryWrapperDeserialized = MAPPER.readValue(camryWrapperJson, CarWrapper.class);
        Car camryExtracted = camryWrapperDeserialized.car;
        assertTrue(camryExtracted instanceof Camry);
        assertSame(camryExtracted.getClass(), Camry.class);
        assertEquals(camry.name, camryExtracted.name);
        assertEquals(camry.exteriorColor, ((Camry) camryExtracted).exteriorColor);
        assertEquals(camry.getType(), ((Camry) camryExtracted).getType());

        @SuppressWarnings("unchecked")
        List<Car> carListDeserialized = MAPPER.readValue(carListJson, List.class);
        assertNotNull(carListDeserialized);
        assertTrue(carListDeserialized.size() == 2);
        Car result = MAPPER.convertValue(carListDeserialized.get(0), Car.class);
        assertTrue(result instanceof Camry);
        assertSame(result.getClass(), Camry.class);

        result = MAPPER.convertValue(carListDeserialized.get(1), Car.class);
        assertTrue(result instanceof Accord);
        assertSame(result.getClass(), Accord.class);
    }

    // for [databind#1635]: simple usage
    public void testExistingEnumTypeId() throws Exception
    {
        Bean1635 result = MAPPER.readValue(a2q("{'value':3, 'type':'A'}"),
                Bean1635.class);
        assertEquals(Bean1635A.class, result.getClass());
        Bean1635A bean = (Bean1635A) result;
        assertEquals(3, bean.value);
        assertEquals(ABC.A, bean.type);
    }

    // for [databind#1635]: verify that `defaultImpl` does not block assignment of
    // type id
    public void testExistingEnumTypeIdViaDefault() throws Exception
    {
        Bean1635 result = MAPPER.readValue(a2q("{'type':'C'}"),
                Bean1635.class);
        assertEquals(Bean1635Default.class, result.getClass());
        assertEquals(ABC.C, result.type);
    }

    // [databind#3271]: verify that `null` token does not become "null" String

    public void testDeserializationWithValidType() throws Exception {
        Shape3271 deserShape = MAPPER.readValue("{\"type\":\"square\"}", Shape3271.class);
        assertEquals("square", deserShape.getType());
    }

    public void testDeserializationWithInvalidType() throws Exception {
        Shape3271 deserShape = MAPPER.readValue("{\"type\":\"invalid\"}", Shape3271.class);
        assertEquals("invalid", deserShape.getType());
    }

    public void testDeserializationNull() throws Exception {
        Shape3271 deserShape = MAPPER.readValue("{\"type\":null}", Shape3271.class);
        assertNull(deserShape.getType()); // error: "expected null, but was:<null>"
    }
}
