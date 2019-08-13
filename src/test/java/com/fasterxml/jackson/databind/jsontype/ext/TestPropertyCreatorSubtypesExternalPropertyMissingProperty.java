package com.fasterxml.jackson.databind.jsontype.ext;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.As;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

// for [databind#2404]
public class TestPropertyCreatorSubtypesExternalPropertyMissingProperty
{
    @Rule
    public ExpectedException thrown = ExpectedException.none();

    /**
     * Base class - external property for Fruit subclasses.
     */
    static class Box {
        private String type;
        @JsonTypeInfo(use = Id.NAME, include = As.EXTERNAL_PROPERTY, property = "type")
        @JsonSubTypes({
                @Type(value = Apple.class, name = "apple"),
                @Type(value = Orange.class, name = "orange")
        })
        private Fruit fruit;

        private Box(String type, Fruit fruit) {
            this.type = type;
            this.fruit = fruit;
        }

        @JsonCreator
        public static Box getBox(@JsonProperty("type") String type, @JsonProperty("fruit") Fruit fruit) {
            return new Box(type, fruit);
        }

        public String getType() {
            return type;
        }

        public Fruit getFruit() {
            return fruit;
        }
    }

    static abstract class Fruit {
        private String name;

        protected Fruit(String n) {
            name = n;
        }

        public String getName() {
            return name;
        }
    }

    static class Apple extends Fruit {
        private int seedCount;

        private Apple(String name, int b) {
            super(name);
            seedCount = b;
        }

        public int getSeedCount() {
            return seedCount;
        }

        @JsonCreator
        public static Apple getApple(@JsonProperty("name") String name, @JsonProperty("seedCount") int seedCount) {
            return new Apple(name, seedCount);
        }
    }

    static class Orange extends Fruit {
        private String color;
        private Orange(String name, String c) {
            super(name);
            color = c;
        }

        public String getColor() {
            return color;
        }

        @JsonCreator
        public static Orange getOrange(@JsonProperty("name") String name, @JsonProperty("color") String color) {
            return new Orange(name, color);
        }
    }

    private final ObjectMapper MAPPER = new ObjectMapper();

    /*
    /**********************************************************
    /* Mock data
    /**********************************************************
     */

    private static final Orange orange = new Orange("Orange", "orange");
    private static final Box orangeBox = new Box("orange", orange);
    private static final String orangeBoxJson = "{\"type\":\"orange\",\"fruit\":{\"name\":\"Orange\",\"color\":\"orange\"}}";
    private static final String orangeBoxNullJson = "{\"type\":\"orange\",\"fruit\":null}}";
    private static final String orangeBoxEmptyJson = "{\"type\":\"orange\",\"fruit\":{}}}";
    private static final String orangeBoxMissingJson = "{\"type\":\"orange\"}}";

    private static final Apple apple = new Apple("Apple", 16);
    private static Box appleBox = new Box("apple", apple);
    private static final String appleBoxJson = "{\"type\":\"apple\",\"fruit\":{\"name\":\"Apple\",\"seedCount\":16}}";
    private static final String appleBoxNullJson = "{\"type\":\"apple\",\"fruit\":null}";
    private static final String appleBoxEmptyJson = "{\"type\":\"apple\",\"fruit\":{}}";
    private static final String appleBoxMissingJson = "{\"type\":\"apple\"}";

    /*
    /**********************************************************
    /* Unit tests
    /**********************************************************
     */

    /**
     * Deserialization tests for external type id property present
     */
    @Test
    public void testDeserializationPresent() throws Exception {
        MAPPER.disable(DeserializationFeature.FAIL_ON_MISSING_EXTERNAL_TYPE_ID_PROPERTY);
        checkOrangeBox();
        checkAppleBox();

        MAPPER.enable(DeserializationFeature.FAIL_ON_MISSING_EXTERNAL_TYPE_ID_PROPERTY);
        checkOrangeBox();
        checkAppleBox();
    }

    /**
     * Deserialization tests for external type id property null
     */
    @Test
    public void testDeserializationNull() throws Exception {
        MAPPER.disable(DeserializationFeature.FAIL_ON_MISSING_EXTERNAL_TYPE_ID_PROPERTY);
        checkOrangeBoxNull(orangeBoxNullJson);
        checkAppleBoxNull(appleBoxNullJson);

        MAPPER.enable(DeserializationFeature.FAIL_ON_MISSING_EXTERNAL_TYPE_ID_PROPERTY);
        checkOrangeBoxNull(orangeBoxNullJson);
        checkAppleBoxNull(appleBoxNullJson);
    }

    /**
     * Deserialization tests for external type id property empty
     */
    @Test
    public void testDeserializationEmpty() throws Exception {
        MAPPER.disable(DeserializationFeature.FAIL_ON_MISSING_EXTERNAL_TYPE_ID_PROPERTY);
        checkOrangeBoxEmpty(orangeBoxEmptyJson);
        checkAppleBoxEmpty(appleBoxEmptyJson);

        MAPPER.enable(DeserializationFeature.FAIL_ON_MISSING_EXTERNAL_TYPE_ID_PROPERTY);
        checkOrangeBoxEmpty(orangeBoxEmptyJson);
        checkAppleBoxEmpty(appleBoxEmptyJson);
    }

    /**
     * Deserialization tests for external type id property missing
     */
    @Test
    public void testDeserializationMissing() throws Exception {
        MAPPER.disable(DeserializationFeature.FAIL_ON_MISSING_EXTERNAL_TYPE_ID_PROPERTY);
        checkOrangeBoxNull(orangeBoxMissingJson);
        checkAppleBoxNull(appleBoxMissingJson);

        MAPPER.enable(DeserializationFeature.FAIL_ON_MISSING_EXTERNAL_TYPE_ID_PROPERTY);
        checkBoxJsonMappingException(orangeBoxMissingJson);
        checkBoxJsonMappingException(appleBoxMissingJson);
    }

    private void checkOrangeBox() throws Exception {
        Box deserOrangeBox = MAPPER.readValue(orangeBoxJson, Box.class);
        assertEquals(orangeBox.getType(), deserOrangeBox.getType());

        Fruit deserOrange = deserOrangeBox.getFruit();
        assertSame(Orange.class, deserOrange.getClass());
        assertEquals(orange.getName(), deserOrange.getName());
        assertEquals(orange.getColor(), ((Orange) deserOrange).getColor());
    }

    private void checkAppleBox() throws Exception {
        Box deserAppleBox = MAPPER.readValue(appleBoxJson, Box.class);
        assertEquals(appleBox.getType(), deserAppleBox.getType());

        Fruit deserApple = deserAppleBox.fruit;
        assertSame(Apple.class, deserApple.getClass());
        assertEquals(apple.getName(), deserApple.getName());
        assertEquals(apple.getSeedCount(), ((Apple) deserApple).getSeedCount());
    }

    private void checkOrangeBoxEmpty(String json) throws Exception {
        Box deserOrangeBox = MAPPER.readValue(json, Box.class);
        assertEquals(orangeBox.getType(), deserOrangeBox.getType());

        Fruit deserOrange = deserOrangeBox.getFruit();
        assertSame(Orange.class, deserOrange.getClass());
        assertNull(deserOrange.getName());
        assertNull(((Orange) deserOrange).getColor());
    }

    private void checkAppleBoxEmpty(String json) throws Exception {
        Box deserAppleBox = MAPPER.readValue(json, Box.class);
        assertEquals(appleBox.getType(), deserAppleBox.getType());

        Fruit deserApple = deserAppleBox.fruit;
        assertSame(Apple.class, deserApple.getClass());
        assertNull(deserApple.getName());
        assertEquals(0, ((Apple) deserApple).getSeedCount());
    }

    private void checkOrangeBoxNull(String json) throws Exception {
        Box deserOrangeBox = MAPPER.readValue(json, Box.class);
        assertEquals(orangeBox.getType(), deserOrangeBox.getType());
        assertNull(deserOrangeBox.getFruit());
    }

    private void checkAppleBoxNull(String json) throws Exception {
        Box deserAppleBox = MAPPER.readValue(json, Box.class);
        assertEquals(appleBox.getType(), deserAppleBox.getType());
        assertNull(deserAppleBox.getFruit());
    }

    private void checkBoxJsonMappingException(String json) throws Exception {
        thrown.expect(JsonMappingException.class);
        thrown.expectMessage("Missing property 'fruit' for external type id 'type'");
        MAPPER.readValue(json, Box.class);
    }
}    
