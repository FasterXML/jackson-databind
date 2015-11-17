package com.fasterxml.jackson.databind.deser;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.BaseMapTest;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;
import java.util.Map;

public class TestObjectOrArrayDeserialization extends BaseMapTest {

    public static class ArrayOrObject {
        private final List<Object> objects;
        private final Object object;

        @JsonCreator public ArrayOrObject(List<Object> objects) {
            this.objects = objects;
            this.object = null;
        }

        @JsonCreator public ArrayOrObject(Object object) {
            this.objects = null;
            this.object = object;
        }
    }

    public void testObjectCase() throws Exception {
        ArrayOrObject arrayOrObject = objectMapper().readValue("{}", ArrayOrObject.class);
        assertNull("expected objects field to be null", arrayOrObject.objects);
        assertNotNull("expected object field not to be null", arrayOrObject.object);
    }

    public void testEmptyArrayCase() throws Exception {
        ArrayOrObject arrayOrObject = objectMapper().readValue("[]", ArrayOrObject.class);
        assertNotNull("expected objects field not to be null", arrayOrObject.objects);
        assertTrue("expected objects field to be an empty list", arrayOrObject.objects.isEmpty());
        assertNull("expected object field to be null", arrayOrObject.object);
    }

    public void testNotEmptyArrayCase() throws Exception {
        ArrayOrObject arrayOrObject = objectMapper().readValue("[{}, {}]", ArrayOrObject.class);
        assertNotNull("expected objects field not to be null", arrayOrObject.objects);
        assertEquals("expected objects field to have size 2", 2, arrayOrObject.objects.size());
        assertNull("expected object field to be null", arrayOrObject.object);
    }

}
