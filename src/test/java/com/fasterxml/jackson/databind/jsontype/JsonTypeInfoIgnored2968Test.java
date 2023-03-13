package com.fasterxml.jackson.databind.jsontype;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.BaseMapTest;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.InvalidDefinitionException;
import com.fasterxml.jackson.databind.exc.InvalidTypeIdException;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;

public class JsonTypeInfoIgnored2968Test extends BaseMapTest {
    /*
    /**********************************************************
    /* Set up
    /**********************************************************
     */

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
    static abstract class Animal {
        public String name;
    }

    static class Cat extends Animal {}

    static class Dog extends Animal {}

    static abstract class SimpleBall {
        public int size = 3;
    }

    static class BasketBall extends SimpleBall {
        protected BasketBall() {}

        public BasketBall(int size) {
            super();
            this.size = size;
        }
    }

    // make this type `final` to avoid polymorphic handling
    static final class BallValueWrapper {
        public SimpleBall value;
    }

    /*
    /**********************************************************
    /* Tests
    /**********************************************************
     */

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

    public void testDeserializationWrapperWithDefaultTyping() throws Exception {
        final PolymorphicTypeValidator ptv = BasicPolymorphicTypeValidator.builder()
            .allowIfBaseType(SimpleBall.class)
            .build();
        ObjectMapper mapper = jsonMapperBuilder()
            .activateDefaultTyping(ptv, ObjectMapper.DefaultTyping.NON_FINAL)
            .build();

        final String defaultTypedJson = a2q(
            "{'value':" +
                "['"+getClass().getName()+"$BasketBall'," +
                "{'size':42}]}");

        BallValueWrapper wrapper = mapper.readValue(defaultTypedJson, BallValueWrapper.class);
        assertEquals(42, wrapper.value.size);
        assertEquals(BasketBall.class, wrapper.value.getClass());
    }

    public void testDeserializationBaseClassWithDefaultTyping() throws Exception {
        final PolymorphicTypeValidator ptv = BasicPolymorphicTypeValidator.builder()
            .allowIfBaseType(SimpleBall.class)
            .build();
        ObjectMapper mapper = jsonMapperBuilder()
            .activateDefaultTyping(ptv, ObjectMapper.DefaultTyping.NON_FINAL)
            .build();

        final String concreteTypeJson = a2q("{'size': 42}");
        try {
            mapper.readValue(concreteTypeJson, SimpleBall.class);
        } catch (MismatchedInputException | InvalidDefinitionException e) {
            verifyException(e, "Unexpected token (START_OBJECT), expected START_ARRAY: need Array value " +
                "to contain `As.WRAPPER_ARRAY` type information for class");
        }
    }
}
