package com.fasterxml.jackson.failing;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.BaseMapTest;
import com.fasterxml.jackson.databind.ObjectMapper;

public class TestSetterlessProperty
    extends BaseMapTest
{
    static class ImmutableId {
        private final int id;

        public ImmutableId(int id) { this.id = id; }

        public int getId() {
            return id;
        }
    }

    static class ImmutableIdWithEmptyConstuctor {
        private final int id;

        public ImmutableIdWithEmptyConstuctor() { this(-1); }

        public ImmutableIdWithEmptyConstuctor(int id) { this.id = id; }

        public int getId() {
            return id;
        }
    }

    static class ImmutableIdWithJsonCreatorAnnotation {
        private final int id;

        @JsonCreator
        public ImmutableIdWithJsonCreatorAnnotation(int id) { this.id = id; }

        public int getId() {
            return id;
        }
    }

    static class ImmutableIdWithJsonPropertyFieldAnnotation {
        @JsonProperty("id") private final int id;

        public ImmutableIdWithJsonPropertyFieldAnnotation(int id) { this.id = id; }

        public int getId() {
            return id;
        }
    }

    static class ImmutableIdWithJsonPropertyConstructorAnnotation {
        private final int id;

        public ImmutableIdWithJsonPropertyConstructorAnnotation(@JsonProperty("id") int id) { this.id = id; }

        public int getId() {
            return id;
        }
    }

    /*
    /**********************************************************
    /* Unit tests
    /**********************************************************
     */

    public void testSetterlessProperty() throws Exception
    {
        ImmutableId input = new ImmutableId(13);
        ObjectMapper m = new ObjectMapper();
        String json = m.writerWithDefaultPrettyPrinter().writeValueAsString(input);

        ImmutableId output = m.readValue(json, ImmutableId.class);
        assertNotNull(output);

        assertEquals(input.id, output.id);
    }

    // this passes - but with an extra (messy) constructor
    public void testSetterlessPropertyWithEmptyConstructor() throws Exception
    {
        ImmutableIdWithEmptyConstuctor input = new ImmutableIdWithEmptyConstuctor(13);
        ObjectMapper m = new ObjectMapper();
        String json = m.writerWithDefaultPrettyPrinter().writeValueAsString(input);

        ImmutableIdWithEmptyConstuctor output = m.readValue(json, ImmutableIdWithEmptyConstuctor.class);
        assertNotNull(output);

        assertEquals(input.id, output.id);
    }

    // this still fails - despite the JsonCreator annotation
    public void testSetterlessPropertyWithJsonCreator() throws Exception
    {
        ImmutableIdWithJsonCreatorAnnotation input = new ImmutableIdWithJsonCreatorAnnotation(13);
        ObjectMapper m = new ObjectMapper();
        String json = m.writerWithDefaultPrettyPrinter().writeValueAsString(input);

        ImmutableIdWithJsonCreatorAnnotation output =
                m.readValue(json, ImmutableIdWithJsonCreatorAnnotation.class);
        assertNotNull(output);

        assertEquals(input.id, output.id);
    }

    // this passes - but needs an untidy JsonProperty annotation
    public void testSetterlessPropertyWithJsonPropertyField() throws Exception
    {
        ImmutableIdWithJsonPropertyConstructorAnnotation input = new ImmutableIdWithJsonPropertyConstructorAnnotation(13);
        ObjectMapper m = new ObjectMapper();
        String json = m.writerWithDefaultPrettyPrinter().writeValueAsString(input);

        ImmutableIdWithJsonPropertyConstructorAnnotation output =
                m.readValue(json, ImmutableIdWithJsonPropertyConstructorAnnotation.class);
        assertNotNull(output);

        assertEquals(input.id, output.id);
    }

    // this still fails - despite the JsonProperty annotation
    public void testSetterlessPropertyWithJsonPropertyConstructor() throws Exception
    {
        ImmutableIdWithJsonPropertyFieldAnnotation input = new ImmutableIdWithJsonPropertyFieldAnnotation(13);
        ObjectMapper m = new ObjectMapper();
        String json = m.writerWithDefaultPrettyPrinter().writeValueAsString(input);

        ImmutableIdWithJsonPropertyFieldAnnotation output = m.readValue(json, ImmutableIdWithJsonPropertyFieldAnnotation.class);
        assertNotNull(output);

        assertEquals(input.id, output.id);
    }
}
