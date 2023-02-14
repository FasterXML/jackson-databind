package com.fasterxml.jackson.databind.jsontype.ext;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo.As;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;

import org.junit.Test;

// for [databind#1341]
public class TestSubtypesExternalPropertyMissingProperty extends BaseMapTest
{
    /**
     * Base class - external property for Fruit subclasses.
     */
    static class Box {
        public String type;
        @JsonTypeInfo(use = Id.NAME, include = As.EXTERNAL_PROPERTY, property = "type")
        public Fruit fruit;

        public Box() {
        }

        public Box(String type, Fruit fruit) {
            this.type = type;
            this.fruit = fruit;
        }
    }

    /**
     * Base class that requires the property to be present.
     */
    static class ReqBox {
        public String type;
        @JsonTypeInfo(use = Id.NAME, include = As.EXTERNAL_PROPERTY, property = "type")
        @JsonProperty(required = true)
        public Fruit fruit;

        public ReqBox() {
        }

        public ReqBox(String type, Fruit fruit) {
            this.type = type;
            this.fruit = fruit;
        }
    }

    @JsonSubTypes({
            @Type(value = Apple.class, name = "apple"),
            @Type(value = Orange.class, name = "orange")
    })
    static abstract class Fruit {
        public String name;

        public Fruit() {
        }

        protected Fruit(String n) {
            name = n;
        }
    }

    static class Apple extends Fruit {
        public int seedCount;

        public Apple() {
        }

        public Apple(String name, int b) {
            super(name);
            seedCount = b;
        }
    }

    static class Orange extends Fruit {
        public String color;

        public Orange() {
        }

        public Orange(String name, String c) {
            super(name);
            color = c;
        }
    }

    private final ObjectReader READER = newJsonMapper().reader();

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
    private static final Box appleBox = new Box("apple", apple);
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
        ObjectReader r = READER.without(DeserializationFeature.FAIL_ON_MISSING_EXTERNAL_TYPE_ID_PROPERTY);
        checkOrangeBox(r);
        checkAppleBox(r);

        r = READER.with(DeserializationFeature.FAIL_ON_MISSING_EXTERNAL_TYPE_ID_PROPERTY);
        checkOrangeBox(r);
        checkAppleBox(r);
    }

    /**
     * Deserialization tests for external type id property null
     */
    @Test
    public void testDeserializationNull() throws Exception {
        ObjectReader r = READER.without(DeserializationFeature.FAIL_ON_MISSING_EXTERNAL_TYPE_ID_PROPERTY);
        checkOrangeBoxNull(r, orangeBoxNullJson);
        checkAppleBoxNull(r, appleBoxNullJson);

        r = READER.with(DeserializationFeature.FAIL_ON_MISSING_EXTERNAL_TYPE_ID_PROPERTY);
        checkOrangeBoxNull(r, orangeBoxNullJson);
        checkAppleBoxNull(r, appleBoxNullJson);
    }

    /**
     * Deserialization tests for external type id property empty
     */
    @Test
    public void testDeserializationEmpty() throws Exception {
        ObjectReader r = READER.without(DeserializationFeature.FAIL_ON_MISSING_EXTERNAL_TYPE_ID_PROPERTY);
        checkOrangeBoxEmpty(r, orangeBoxEmptyJson);
        checkAppleBoxEmpty(r, appleBoxEmptyJson);

        r = READER.with(DeserializationFeature.FAIL_ON_MISSING_EXTERNAL_TYPE_ID_PROPERTY);
        checkOrangeBoxEmpty(r, orangeBoxEmptyJson);
        checkAppleBoxEmpty(r, appleBoxEmptyJson);
    }

    /**
     * Deserialization tests for external type id property missing
     */
    @Test
    public void testDeserializationMissing() throws Exception {
        ObjectReader r = READER.without(DeserializationFeature.FAIL_ON_MISSING_EXTERNAL_TYPE_ID_PROPERTY);
        checkOrangeBoxNull(r, orangeBoxMissingJson);
        checkAppleBoxNull(r, appleBoxMissingJson);

        r = READER.with(DeserializationFeature.FAIL_ON_MISSING_EXTERNAL_TYPE_ID_PROPERTY);
        checkBoxDatabindException(r, orangeBoxMissingJson);
        checkBoxDatabindException(r, appleBoxMissingJson);
    }

    /**
     * Deserialization tests for external type id required property missing
     */
    @Test
    public void testDeserializationMissingRequired() throws Exception {
        ObjectReader r = READER.without(DeserializationFeature.FAIL_ON_MISSING_EXTERNAL_TYPE_ID_PROPERTY);
        checkReqBoxDatabindException(r, orangeBoxMissingJson);
        checkReqBoxDatabindException(r, appleBoxMissingJson);

        r = READER.with(DeserializationFeature.FAIL_ON_MISSING_EXTERNAL_TYPE_ID_PROPERTY);
        checkReqBoxDatabindException(r, orangeBoxMissingJson);
        checkReqBoxDatabindException(r, appleBoxMissingJson);
    }

    private void checkOrangeBox(ObjectReader r) throws Exception {
        Box deserOrangeBox = r.forType(Box.class).readValue(orangeBoxJson);
        assertEquals(orangeBox.type, deserOrangeBox.type);

        Fruit deserOrange = deserOrangeBox.fruit;
        assertSame(Orange.class, deserOrange.getClass());
        assertEquals(orange.name, deserOrange.name);
        assertEquals(orange.color, ((Orange) deserOrange).color);
    }

    private void checkAppleBox(ObjectReader r) throws Exception {
        Box deserAppleBox = r.forType(Box.class).readValue(appleBoxJson);
        assertEquals(appleBox.type, deserAppleBox.type);

        Fruit deserApple = deserAppleBox.fruit;
        assertSame(Apple.class, deserApple.getClass());
        assertEquals(apple.name, deserApple.name);
        assertEquals(apple.seedCount, ((Apple) deserApple).seedCount);
    }

    private void checkOrangeBoxEmpty(ObjectReader r, String json) throws Exception {
        Box deserOrangeBox = r.forType(Box.class).readValue(json);
        assertEquals(orangeBox.type, deserOrangeBox.type);

        Fruit deserOrange = deserOrangeBox.fruit;
        assertSame(Orange.class, deserOrange.getClass());
        assertNull(deserOrange.name);
        assertNull(((Orange) deserOrange).color);
    }

    private void checkAppleBoxEmpty(ObjectReader r, String json) throws Exception {
        Box deserAppleBox = r.forType(Box.class).readValue(json);
        assertEquals(appleBox.type, deserAppleBox.type);

        Fruit deserApple = deserAppleBox.fruit;
        assertSame(Apple.class, deserApple.getClass());
        assertNull(deserApple.name);
        assertEquals(0, ((Apple) deserApple).seedCount);
    }

    private void checkOrangeBoxNull(ObjectReader r, String json) throws Exception {
        Box deserOrangeBox = r.forType(Box.class).readValue(json);
        assertEquals(orangeBox.type, deserOrangeBox.type);
        assertNull(deserOrangeBox.fruit);
    }

    private void checkAppleBoxNull(ObjectReader r, String json) throws Exception {
        Box deserAppleBox = r.forType(Box.class).readValue(json);
        assertEquals(appleBox.type, deserAppleBox.type);
        assertNull(deserAppleBox.fruit);
    }

    private void checkBoxDatabindException(ObjectReader r, String json) throws Exception {
        try {
            r.forType(Box.class).readValue(json);
            fail("Should not pass");
        } catch (MismatchedInputException e) {
            BaseMapTest.verifyException(e, "Missing property 'fruit' for external type id 'type'");
        }
    }

    private void checkReqBoxDatabindException(ObjectReader r, String json) throws Exception {
        try {
            r.forType(ReqBox.class).readValue(json, ReqBox.class);
            fail("Should not pass");
        } catch (MismatchedInputException e) {
            BaseMapTest.verifyException(e, "Missing property 'fruit' for external type id 'type'");
        }
    }
}
