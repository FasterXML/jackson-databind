package com.fasterxml.jackson.databind.deser;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.*;

import static com.fasterxml.jackson.databind.testutil.DatabindTestUtil.newJsonMapper;

public class ObjectOrArrayDeserTest
{
    public static class SomeObject {
        public String someField;
    }

    public static class ArrayOrObject {
        final List<SomeObject> objects;
        final SomeObject object;

        @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
        public ArrayOrObject(List<SomeObject> objects) {
            this.objects = objects;
            this.object = null;
        }

        @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
        public ArrayOrObject(SomeObject object) {
            this.objects = null;
            this.object = object;
        }
    }

    private final ObjectMapper MAPPER = newJsonMapper();

    @Test
    public void testObjectCase() throws Exception {
        ArrayOrObject arrayOrObject = MAPPER.readValue("{}", ArrayOrObject.class);
        assertNull(arrayOrObject.objects, "expected objects field to be null");
        assertNotNull(arrayOrObject.object, "expected object field not to be null");
    }

    @Test
    public void testEmptyArrayCase() throws Exception {
        ArrayOrObject arrayOrObject = MAPPER.readValue("[]", ArrayOrObject.class);
        assertNotNull(arrayOrObject.objects, "expected objects field not to be null");
        assertTrue(arrayOrObject.objects.isEmpty(), "expected objects field to be an empty list");
        assertNull(arrayOrObject.object, "expected object field to be null");
    }

    @Test
    public void testNotEmptyArrayCase() throws Exception {
        ArrayOrObject arrayOrObject = MAPPER.readValue("[{}, {}]", ArrayOrObject.class);
        assertNotNull(arrayOrObject.objects, "expected objects field not to be null");
        assertEquals(2, arrayOrObject.objects.size(), "expected objects field to have size 2");
        assertNull(arrayOrObject.object, "expected object field to be null");
    }
}
