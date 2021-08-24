package com.fasterxml.jackson.databind.deser;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.BaseMapTest;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.junit.Assert;

public class TestObjectOrArrayDeserialization extends BaseMapTest
{
    public static class SomeObject {
        public String someField;
    }

    public static class ArrayOrObject {
        final List<SomeObject> objects;
        final SomeObject object;

        @JsonCreator
        public ArrayOrObject(List<SomeObject> objects) {
            this.objects = objects;
            this.object = null;
        }

        @JsonCreator
        public ArrayOrObject(SomeObject object) {
            this.objects = null;
            this.object = object;
        }
    }


    public void testCoercionOfBlankStrings() {
        String s =  " ";

        final String json = "{ \"J\" : \"" + s + "\" }";
        try {
            final Jack jack = new ObjectMapper().readValue(json, Jack.class);
            System.out.println("j=" + jack.getJ());
            Assert.assertEquals(0, jack.getJ());
        } catch (Throwable t) {
            t.printStackTrace();
            fail();
        }
    }

    private final ObjectMapper MAPPER = newJsonMapper();

    public void testObjectCase() throws Exception {
        ArrayOrObject arrayOrObject = MAPPER.readValue("{}", ArrayOrObject.class);
        assertNull("expected objects field to be null", arrayOrObject.objects);
        assertNotNull("expected object field not to be null", arrayOrObject.object);
    }

    public void testEmptyArrayCase() throws Exception {
        ArrayOrObject arrayOrObject = MAPPER.readValue("[]", ArrayOrObject.class);
        assertNotNull("expected objects field not to be null", arrayOrObject.objects);
        assertTrue("expected objects field to be an empty list", arrayOrObject.objects.isEmpty());
        assertNull("expected object field to be null", arrayOrObject.object);
    }

    public void testNotEmptyArrayCase() throws Exception {
        ArrayOrObject arrayOrObject = MAPPER.readValue("[{}, {}]", ArrayOrObject.class);
        assertNotNull("expected objects field not to be null", arrayOrObject.objects);
        assertEquals("expected objects field to have size 2", 2, arrayOrObject.objects.size());
        assertNull("expected object field to be null", arrayOrObject.object);
    }
}
