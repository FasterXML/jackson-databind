package com.fasterxml.jackson.databind.deser.creators;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.databind.BaseMapTest;
import com.fasterxml.jackson.databind.ObjectMapper;

public class DelegatingArrayCreator1804Test extends BaseMapTest
{
    public static class MyTypeImpl extends MyType {
        private final List<Integer> values;

        MyTypeImpl(List<Integer> values) {
            this.values = values;
        }

        @Override
        public List<Integer> getValues() {
            return values;
        }
    }

    static abstract class MyType {
        @JsonValue
        public abstract List<Integer> getValues();

        @JsonCreator(mode=JsonCreator.Mode.DELEGATING)
        public static MyType of(List<Integer> values) {
            return new MyTypeImpl(values);
        }
    }


    public void testDelegatingArray1804() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        MyType thing = mapper.readValue("[]", MyType.class);
        assertNotNull(thing);
    }
}
