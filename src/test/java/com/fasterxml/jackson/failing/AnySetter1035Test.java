package com.fasterxml.jackson.failing;

import java.util.*;

import com.fasterxml.jackson.annotation.*;

import com.fasterxml.jackson.databind.*;

/**
 * Test for [databind#1035], wherein key type of a `Map` not used with `@JsonAnySetter`
 * (value type is).
 */
public class AnySetter1035Test extends BaseMapTest
{
    static class MyGeneric<T>
    {
        private String staticallyMappedProperty;
        private Map<T, Integer> dynamicallyMappedProperties = new HashMap<T, Integer>();

        public String getStaticallyMappedProperty() {
            return staticallyMappedProperty;
        }

        @JsonAnySetter
        public void addDynamicallyMappedProperty(T key, int value) {
            dynamicallyMappedProperties.put(key, value);
        }

        public void setStaticallyMappedProperty(String staticallyMappedProperty) {
            this.staticallyMappedProperty = staticallyMappedProperty;
        }

        @JsonAnyGetter
        public Map<T, Integer> getDynamicallyMappedProperties() {
            return dynamicallyMappedProperties;
        }
    }

    static class MyWrapper
    {
        private MyGeneric<String> myStringGeneric;
        private MyGeneric<Integer> myIntegerGeneric;

        public MyGeneric<String> getMyStringGeneric() {
            return myStringGeneric;
        }

        public void setMyStringGeneric(MyGeneric<String> myStringGeneric) {
            this.myStringGeneric = myStringGeneric;
        }

        public MyGeneric<Integer> getMyIntegerGeneric() {
            return myIntegerGeneric;
        }

        public void setMyIntegerGeneric(MyGeneric<Integer> myIntegerGeneric) {
            this.myIntegerGeneric = myIntegerGeneric;
        }
    }

    public void testGenericAnySetter() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();

        Map<String, Integer> stringGenericMap = new HashMap<String, Integer>();
        stringGenericMap.put("testStringKey", 5);
        Map<Integer, Integer> integerGenericMap = new HashMap<Integer, Integer>();
        integerGenericMap.put(111, 6);

        MyWrapper deserialized = mapper.readValue(aposToQuotes(
                "{'myStringGeneric':{'staticallyMappedProperty':'Test','testStringKey':5},'myIntegerGeneric':{'staticallyMappedProperty':'Test2','111':6}}"
                ), MyWrapper.class);
        MyGeneric<String> stringGeneric = deserialized.getMyStringGeneric();
        MyGeneric<Integer> integerGeneric = deserialized.getMyIntegerGeneric();

        assertNotNull(stringGeneric);
        assertEquals(stringGeneric.getStaticallyMappedProperty(), "Test");
        for(Map.Entry<String, Integer> entry : stringGeneric.getDynamicallyMappedProperties().entrySet()) {
            assertTrue("A key in MyGeneric<String> is not an String.", entry.getKey() instanceof String);
            assertTrue("A value in MyGeneric<Integer> is not an Integer.", entry.getValue() instanceof Integer);
        }
        assertEquals(stringGeneric.getDynamicallyMappedProperties(), stringGenericMap);

        assertNotNull(integerGeneric);
        assertEquals(integerGeneric.getStaticallyMappedProperty(), "Test2");
        for(Map.Entry<Integer, Integer> entry : integerGeneric.getDynamicallyMappedProperties().entrySet()) {
            Object key = entry.getKey();
            assertEquals("A key in MyGeneric<Integer> is not an Integer.", Integer.class, key.getClass());
            Object value = entry.getValue();
            assertEquals("A value in MyGeneric<Integer> is not an Integer.", Integer.class, value.getClass());
        }
        assertEquals(integerGeneric.getDynamicallyMappedProperties(), integerGenericMap);
    }
}
