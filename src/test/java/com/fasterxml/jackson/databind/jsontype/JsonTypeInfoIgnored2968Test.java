package com.fasterxml.jackson.databind.jsontype;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.BaseMapTest;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.InvalidTypeIdException;

public class JsonTypeInfoIgnored2968Test extends BaseMapTest
{
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @JsonTypeInfo(
            use = JsonTypeInfo.Id.NAME,
            include = JsonTypeInfo.As.PROPERTY,
            property = "_class"
    )
    @JsonSubTypes({
            @JsonSubTypes.Type(value = Dog.class, name = "_dog"),
            @JsonSubTypes.Type(value = Cat.class, name = "_cat")
    })
    static abstract class Animal
    {
        public String name;
    }

    static class Cat extends Animal
    {
    }

    static class Dog extends Animal
    {
    }

    static class Bird
    {
        public String name;
    }

    public void testDeserializeParentPositiveWithTypeId() throws Exception {
        String json = a2q("{'_class': '_cat', 'name': 'Cat-in-the-hat'} ");

        Animal cat = MAPPER.readValue(json, Animal.class);

        assertEquals("Cat-in-the-hat", cat.name);
    }


    public void testDeserializeParentNegativeWithOutTypeId() throws Exception {
        String json = a2q("{'name': 'cat'} ");

        try {
            MAPPER.readValue(json, Animal.class);
        } catch (InvalidTypeIdException e) {
            assertTrue(e.getMessage().contains("missing type id property '_class'"));
        }
    }

    public void testDeserializedAsConcreteTypeSuccessfulWithOutPropertySet() throws Exception {
        String json = a2q("{'name': 'cat'} ");

        Cat cat = MAPPER.readValue(json, Cat.class);

        assertEquals("cat", cat.name);
    }


//    public void testDeserializeParentPositiveWithTypeId() throws Exception {
//        String json = a2q("{'_class': '_cat', 'name': 'Cat-in-the-hat'} ");
//
//        Animal cat = MAPPER.reader().readValue(json, Cat.class);
//
//        assertEquals("Cat-in-the-hat", cat.name);
//    }

    // TODO: ShouldJsonIgnore be there?
    // DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES

}
