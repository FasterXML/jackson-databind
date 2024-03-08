package com.fasterxml.jackson.databind.module;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.exc.InvalidDefinitionException;
import com.fasterxml.jackson.databind.testutil.DatabindTestUtil;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Behavioral test to prove that by design decision, (de)serializers from class-level annotations will always
 * have preference over default (de)serializers.
 * 
 */
@SuppressWarnings("serial")
public class SimpleModuleAddMethodsTest extends DatabindTestUtil
{
    @JsonDeserialize(using = ClassDogDeserializer.class)
    @JsonSerialize(using = ClassDogSerializer.class)
    static class Dog {
        public String name;

        public Dog(String name) {
            this.name = name;
        }
    }

    @JsonDeserialize(keyUsing = ClassDogKeyDeserializer.class, contentUsing = ClassDogDeserializer.class)
    @JsonSerialize(keyUsing = ClassDogKeySerializer.class, contentUsing = ClassDogSerializer.class)
    static class DogMap extends HashMap<Dog, Dog> {}

    @JsonDeserialize(contentUsing = ClassDogDeserializer.class)
    @JsonSerialize(contentUsing = ClassDogSerializer.class)
    static class DogList extends ArrayList<Dog> {}

    static class ClassDogSerializer extends JsonSerializer<Dog> {
        @Override
        public void serialize(Dog value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
            gen.writeString("class-dog");
        }
    }

    static class ClassDogDeserializer extends JsonDeserializer<Dog> {
        @Override
        public Dog deserialize(JsonParser p, DeserializationContext ctxt) {
            return new Dog("class-dog");
        }
    }

    static class ClassDogKeySerializer extends JsonSerializer<Dog> {
        @Override
        public void serialize(Dog value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
            gen.writeFieldName("class-dog");
        }
    }

    static class ClassDogKeyDeserializer extends KeyDeserializer {
        @Override
        public Object deserializeKey(String key, DeserializationContext ctxt) {
            return new Dog("class-dog");
        }
    }

    static class ModuleDogDeserializer extends JsonDeserializer<Dog> {
        @Override
        public Dog deserialize(JsonParser p, DeserializationContext ctxt) {
            return new Dog("module-dog");
        }
    }

    static class ModuleDogSerializer extends JsonSerializer<Dog> {
        @Override
        public void serialize(Dog value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
            gen.writeString("module-dog");
        }
    }

    static class ModuleDogKeyDeserializer extends KeyDeserializer {
        @Override
        public Object deserializeKey(String key, DeserializationContext ctxt) {
            return new Dog("module-dog");
        }
    }

    static class ModuleDogKeySerializer extends JsonSerializer<Dog> {
        @Override
        public void serialize(Dog value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
            gen.writeFieldName("module-dog");
        }
    }

    @JsonDeserialize(builder = SimpleBuilderXY.class)
    static class BuildFailBean {
        final int _x, _y;

        protected BuildFailBean(int x, int y) {
            _x = x;
            _y = y;
        }
    }

    @JsonDeserialize(builder = SimpleBuilderXY.class)
    static class BuildSuccessBean {
        final int _x, _y;

        protected BuildSuccessBean(int x, int y) {
            _x = x;
            _y = y;
        }
    }

    @JsonDeserialize(builder = java.lang.Void.class)
    public static abstract class BuildBeanMixin {}

    static class SimpleBuilderXY {
        public int x, y;

        public SimpleBuilderXY withX(int x0) {
            this.x = x0;
            return this;
        }

        public SimpleBuilderXY withY(int y0) {
            this.y = y0;
            return this;
        }

        public BuildFailBean build() {
            return new BuildFailBean(x, y);
        }
    }

    static class BuildSuccessBeanDeserializer extends JsonDeserializer<BuildSuccessBean> {
        @Override
        public BuildSuccessBean deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            return new BuildSuccessBean(7, 8);
        }
    }
     
    /*
    /**********************************************************
    /* Test methods
    /**********************************************************
     */

    private final ObjectMapper MAPPER = _buildMapper();

    private ObjectMapper _buildMapper() {
        ObjectMapper mapper = newJsonMapper();

        // Simple tests
        SimpleModule simpleModule = new SimpleModule()
            .addSerializer(Dog.class, new ModuleDogSerializer())
            .addDeserializer(Dog.class, new ModuleDogDeserializer())
            .addKeyDeserializer(Dog.class, new ModuleDogKeyDeserializer())
            .addKeySerializer(Dog.class, new ModuleDogKeySerializer());

        // "remove" builder annotation using mix-in
        mapper.addMixIn(BuildFailBean.class, BuildBeanMixin.class);

        // "remove" builder annotation using mix-in, then register deserializer using module
        mapper.addMixIn(BuildSuccessBean.class, BuildBeanMixin.class);
        simpleModule.addDeserializer(BuildSuccessBean.class, new BuildSuccessBeanDeserializer());

        mapper.registerModule(simpleModule);
        return mapper;
    }

    @Test
    public void testPojoDeserialization() throws Exception {
        Dog dog = MAPPER.readValue(a2q("{'name': 'my-dog'}"), Dog.class);
        assertEquals("class-dog", dog.name);
    }

    @Test
    public void testPojoSerialization() throws Exception {
        assertEquals(
            a2q("'class-dog'"),
            MAPPER.writeValueAsString(new Dog("my-dog")));
    }

    @Test
    public void testRemoveAnnotationUsingMixIn() throws Exception {
        try {
            MAPPER.readValue(
                a2q("{'x':1, 'y':2}"), BuildFailBean.class);
            fail("Should not pass");
        } catch (InvalidDefinitionException e) {
            verifyException(e, "cannot deserialize from Object value (no delegate- or property-based Creator)");
        }
    }

    @Test
    public void testRemoveAnnotationUsingMixInAndOverrideByModule() throws Exception {
        BuildSuccessBean bean = MAPPER.readValue(
            a2q("{'x':1, 'y':2}"), BuildSuccessBean.class);
        assertEquals(7, bean._x);
        assertEquals(8, bean._y);
    }

    @Test
    public void testDogMapDeserialization() throws Exception {
        DogMap map = MAPPER.readValue(a2q("{'simple-dog': 'simple-dog'}"), DogMap.class);

        assertEquals(1, map.size());
        for (Map.Entry<Dog, Dog> entry : map.entrySet()) {
            assertEquals("class-dog", entry.getKey().name);
            assertEquals("class-dog", entry.getValue().name);
        }
    }

    @Test
    public void testDogMapSerialization() throws Exception {
        DogMap map = new DogMap();
        map.put(new Dog("my-dog"), new Dog("my-dog"));

        assertEquals(
            a2q("{'class-dog':'class-dog'}"),
            MAPPER.writeValueAsString(map));
    }

    @Test
    public void testDogListDeserialization() throws Exception {
        DogList list = MAPPER.readValue(a2q("['simple-dog']"), DogList.class);

        assertEquals(1, list.size());
        for (Dog dog : list) {
            assertEquals("class-dog", dog.name);
        }
    }

    @Test
    public void testDogListSerialization() throws Exception {
        DogList list = new DogList();
        list.add(new Dog("my-dog"));

        assertEquals(
            a2q("['class-dog']"),
            MAPPER.writeValueAsString(list));
    }
}

