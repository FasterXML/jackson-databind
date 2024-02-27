package com.fasterxml.jackson.databind.jsontype;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.InvalidDefinitionException;
import com.fasterxml.jackson.databind.exc.InvalidTypeIdException;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class JsonTypeInfoIgnored2968Test extends DatabindTestUtil {
    /*
    /**********************************************************
    /* Set up
    /**********************************************************
     */

    private static final ObjectMapper MAPPER = JsonMapper.builder().disable(MapperFeature.REQUIRE_TYPE_ID_FOR_SUBTYPES).build();

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

    @Test
    public void testDeserializeParentPositiveWithTypeId() throws Exception {
        String json = a2q("{'_class': '_cat', 'name': 'Cat-in-the-hat'} ");

        Animal cat = MAPPER.readValue(json, Animal.class);

        assertEquals("Cat-in-the-hat", cat.name);
    }

    @Test
    public void testDeserializeParentNegativeWithOutTypeId() throws Exception {
        String json = a2q("{'name': 'cat'} ");

        try {
            MAPPER.readValue(json, Animal.class);
        } catch (InvalidTypeIdException e) {
            assertTrue(e.getMessage().contains("missing type id property '_class'"));
        }
    }

    @Test
    public void testDeserializedAsConcreteTypeSuccessfulWithOutPropertySet() throws Exception {
        String json = a2q("{'name': 'cat'} ");

        Cat cat = MAPPER.readValue(json, Cat.class);

        assertEquals("cat", cat.name);
    }

    @Test
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

    @Test
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
