package com.fasterxml.jackson.databind.jsontype;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.BaseMapTest;
import com.fasterxml.jackson.databind.ObjectMapper;

public class PolymorphicHandlingOverrideTest extends BaseMapTest {

    @JsonTypeInfo(use = JsonTypeInfo.Id.MINIMAL_CLASS, include = JsonTypeInfo.As.PROPERTY, property = "_class")
    @JsonSubTypes({@JsonSubTypes.Type(Dog.class)})
    static abstract class Animal {
        public String id;
    }

    static class Dog extends Animal {
        public Dog() {
            this.id = "I am a dog";
        }
    }

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "_type")
    @JsonSubTypes({@JsonSubTypes.Type(Squid.class)})
    static abstract class Fish {
        public String id;
    }

    static class Squid extends Fish {
        public Squid() {
            this.id = "sqqq";
        }
    }

    final ObjectMapper MAPPER = newJsonMapper();

    final String classNameDog = a2q("{'_class':'.") + getClass().getSimpleName() + a2q("$Dog','id':'I am a dog'}");

    public void testDefaultBehavioer() throws Exception {
        String dogStr = MAPPER.writeValueAsString(new Dog());
        assertEquals(classNameDog, dogStr);

        Animal animal = MAPPER.readValue(classNameDog, Animal.class);
        assertType(animal, Dog.class);
        assertEquals("I am a dog", animal.id);
    }

    public void testPolymorphicTypeHandlingViaConfigOverride() throws Exception {
        // Override property-name
        final JsonTypeInfo.Value typeInfo = JsonTypeInfo.Value.construct(JsonTypeInfo.Id.NAME, JsonTypeInfo.As.PROPERTY,
                "_some_type", null, false, true);
        ObjectMapper m = jsonMapperBuilder()
                .withConfigOverride(Squid.class, cfg -> cfg.setPolymorphicTypeHandling(typeInfo)).build();

        // Assert
        assertEquals(a2q("{'_some_type':'PolymorphicHandlingOverrideTest$Squid','id':'sqqq'}"),
                m.writeValueAsString(new Squid()));
    }
}
