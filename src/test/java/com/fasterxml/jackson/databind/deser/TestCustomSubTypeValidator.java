package com.fasterxml.jackson.databind.deser;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.BaseMapTest;
import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.cfg.DeserializerFactoryConfig;
import com.fasterxml.jackson.databind.exc.InvalidDefinitionException;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.jsontype.impl.SubTypeValidator;

public class TestCustomSubTypeValidator 
    extends BaseMapTest
{
    /*
    /**********************************************************
    /* Some sample documents:
    /**********************************************************
     */

    protected static final String STRING_VALUE_JSON = "\"just a plain JSON string\"";


    protected static final String ZOO_JSON = "{"
            + "  \"name\" : \"Jacksonville Zoo\","
            + "  \"animals\": ["
            + "    { "
            + "      \"species\" : \"com.fasterxml.jackson.databind.deser.TestCustomSubTypeValidator$Warthog\","
            + "      \"name\" : \"Pumba\""
            + "    },"
            + "    { "
            + "      \"species\" : \"com.fasterxml.jackson.databind.deser.TestCustomSubTypeValidator$Lion\","
            + "      \"name\" : \"Kimba\""
            + "    },"
            + "    { "
            + "      \"species\" : \"com.fasterxml.jackson.databind.deser.TestCustomSubTypeValidator$Goat\","
            + "      \"name\" : \"Goaty McGoatface\""
            + "    },"
            + "    {"
            + "      \"species\" : \"com.fasterxml.jackson.databind.deser.TestCustomSubTypeValidator$Penguin\","
            + "      \"name\" : \"Tux\""
            + "    }"
            + "  ]"
            + "}";

    protected static final String PETTING_ZOO_JSON = "{"
            + "  \"name\" : \"Jacksonville Petting Zoo\","
            + "  \"animals\": ["
            + "    { "
            + "      \"species\" : \"com.fasterxml.jackson.databind.deser.TestCustomSubTypeValidator$Goat\","
            + "      \"name\" : \"Bill\""
            + "    },"
            + "    { "
            + "      \"species\" : \"com.fasterxml.jackson.databind.deser.TestCustomSubTypeValidator$Sheep\","
            + "      \"name\" : \"Shaun\""
            + "    }"
            + "  ]"
            + "}";

    /*
    /**********************************************************
    /* Helper classes (beans)
    /**********************************************************
     */

    public static class Zoo
    {
        private String _name;

        private List<Animal> _animals;

        public String getName()
        {
            return _name;
        }

        public void setName(String name)
        {
            _name = name;
        }

        public List<Animal> getAnimals()
        {
            return _animals;
        }

        public void setAnimals(List<Animal> animals)
        {
            _animals = animals;
        }
    }

    @JsonTypeInfo(
        use=JsonTypeInfo.Id.CLASS,
        include=JsonTypeInfo.As.PROPERTY,
        property="species"
    )
    public static abstract class Animal
    {
        private String _name;

        public String getName() {
            return _name;
        }

        public void setName(String name) {
            _name = name;
        }
    }

    public static class Lion extends Animal
    {
    }

    public static class Warthog extends Animal
    {
    }

    public static class Goat extends Animal
    {
    }

    public static class Sheep extends Animal
    {
    }

    public static class Penguin extends Animal
    {
    }

    /*
    /**********************************************************
    /* Helper classes (validators)
    /**********************************************************
     */

    static class DenyAllValidator
        extends SubTypeValidator
    {
        @Override
        public void validateSubType(DeserializationContext ctxt, JavaType type, BeanDescription beanDesc)
            throws JsonMappingException 
        {
            ctxt.reportBadTypeDefinition(
                    beanDesc,
                    "Illegal type (%s) to deserialize: this validator does not allow deserialization at all!",
                    type.getRawClass().getName());
        }
    }

    static class FriendlyAnimalValidator
        extends SubTypeValidator
    {
        private static Class<?>[] FRIENDLY_ANIMAL_WHITELIST = new Class<?>[] {
            Animal.class,
            Goat.class,
            Sheep.class,
            Penguin.class
        };

        @Override
        public void validateSubType(DeserializationContext ctxt, JavaType type, BeanDescription beanDesc)
            throws JsonMappingException 
        {
            super.validateSubType(ctxt, type, beanDesc);

            if (type.isTypeOrSubTypeOf(Animal.class)) {
                for (final Class<?> friendlyAnimalSpecies : FRIENDLY_ANIMAL_WHITELIST) {
                    if (type.hasRawClass(friendlyAnimalSpecies)) {
                        return;
                    }
                }

                ctxt.reportBadTypeDefinition(
                        beanDesc,
                        "Illegal type (%s) to deserialize: prevented because this animal species is not known to be friendly",
                        type.getRawClass().getName());
            }
        }
    }

    /*
    /**********************************************************
    /* Factory methods
    /**********************************************************
     */

    protected ObjectMapper objectMapper(SubTypeValidator validator)
    {
        final DeserializerFactoryConfig config =
                new DeserializerFactoryConfig()
                    .withSubTypeValidator(validator);

        final DeserializerFactory factory =
                new BeanDeserializerFactory(config);

        final JsonMapper mapper =
                JsonMapper
                    .builder()
                    .deserializerFactory(factory)
                    .build();

        return mapper;
    }

    /*
    /**********************************************************
    /* Unit tests
    /**********************************************************
     */

    public void testDefaultValidator() throws Exception
    {
        final ObjectMapper mapper = objectMapper();

        mapper.readValue(STRING_VALUE_JSON, String.class);
        mapper.readValue(ZOO_JSON, Zoo.class);
        mapper.readValue(PETTING_ZOO_JSON, Zoo.class);
    }

    public void testDenyAllValidator() throws Exception
    {
        final ObjectMapper mapper = objectMapper(new DenyAllValidator());

        // still deserializes primitive JSON types
        mapper.readValue(STRING_VALUE_JSON, String.class);

        // fails to deserialize, because the JSON contains objects that are mapped to beans
        while(true)
        {
            try {
                mapper.readValue(ZOO_JSON, Zoo.class);
            } catch(InvalidDefinitionException e) {
                break;
            }
            fail("Expected InvalidDefinitionException, but got none.");
        }

        // fails to deserialize, because the JSON contains objects that are mapped to beans
        while(true)
        {
            try {
                mapper.readValue(PETTING_ZOO_JSON, Zoo.class);
            } catch(InvalidDefinitionException e) {
                break;
            }
            fail("Expected InvalidDefinitionException, but got none.");
        }
    }

    public void testFriendlyAnimalValidator() throws Exception
    {
        final ObjectMapper mapper = objectMapper(new FriendlyAnimalValidator());

        // still deserializes primitive JSON types
        mapper.readValue(STRING_VALUE_JSON, String.class);

        // fails to deserialize, because the zoo has some unfriendly animals in it
        while(true)
        {
            try {
                mapper.readValue(ZOO_JSON, Zoo.class);
            } catch(InvalidDefinitionException e) {
                break;
            }
            fail("Expected InvalidDefinitionException, but got none.");
        }

        // deserializes successfully, because the petting zoo has only friendly animals in it
        mapper.readValue(PETTING_ZOO_JSON, Zoo.class);
    }
}
