package com.fasterxml.jackson.databind.deser;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.BaseMapTest;
import com.fasterxml.jackson.databind.ObjectMapper;
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


    public void testCoercionOfBlankString() {
        assertCoercion(" ");//, "0", "0 ", " 0 ", "", "    ", "\\n"});
    }

    public void testCoercionOfBlankStringWith0() {
        assertCoercion("0");
    }

    public void testCoercionOfBlankStringWith0AndSpace() {
        assertCoercion( "0 ");
    }

    public void testCoercion0withSpaces() {
        assertCoercion(" 0 ");
    }

    public void testCoercionOfEmptyString() {
        assertCoercion("");
    }

    public void testCoercionOfLongerBlankString() {
        assertCoercion("    ");
    }

    public void testCoercionOfEndlineCharacter() {
        assertCoercion("\\n");
    }

    private void assertCoercion (String s){
        final String json = "{ \"J\" : \"" + s + "\" }";
        try {
            final Jack jack = new ObjectMapper().readValue(json, Jack.class);
            System.out.println("j=" + jack.getJ());
            Assert.assertEquals("Coercion failed for " + s, 0, jack.getJ());
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
