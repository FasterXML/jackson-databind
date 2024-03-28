package com.fasterxml.jackson.databind.deser;

import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.junit.jupiter.api.Test;

import java.beans.ConstructorProperties;
import java.util.ArrayList;
import java.util.List;

import static com.fasterxml.jackson.databind.BaseTest.a2q;
import static org.junit.jupiter.api.Assertions.assertNotNull;

// [databind#4441] @JsonSetter(nulls = Nulls.SKIP) doesn't work in some situations
public class SkipNulls4441Test {

    static class Outer {
        @JsonSetter(nulls = Nulls.SKIP)
        private final List<Middle> listMiddle = new ArrayList<>();

        public Outer() { }

        public List<Middle> getListMiddle() {
            return listMiddle;
        }
    }

    static class Middle {
        @JsonSetter(nulls = Nulls.SKIP)
        private final List<Inner> listInner = new ArrayList<>();
        private final String field1;

        @ConstructorProperties({"field1"})
        public Middle(String field1) {
            this.field1 = field1;
        }

        public List<Inner> getListInner() {
            return listInner;
        }

        public String getField1() {
            return field1;
        }
    }

    static class Inner {
        private final String field1;

        @ConstructorProperties({"field1"})
        public Inner(String field1) {
            this.field1 = field1;
        }

        public String getField1() {
            return field1;
        }
    }

    private final ObjectMapper objectMapper = JsonMapper.builder().build();

    @Test
    public void testJSON() throws Exception {
        // Passes
        // For some reason, if most-inner "list1" field is null in the end, it works
        testSkipNullsWith(
                        a2q("{" +
                                "    'listMiddle': [" +
                                "        {" +
                                "            'field1': 'data',     " +
                                "            'listInner': null  " +
                                "        }" +
                                "    ]" +
                                "}")
        );

        // Fails
        // But if it's null in the beginning, it doesn't work
        testSkipNullsWith(
                        a2q("{" +
                                "    'listMiddle': [" +
                                "        {" +
                                "            'listInner': null,  " +
                                "            'field1': 'data'     " +
                                "        }" +
                                "    ]" +
                                "}")
        );
    }

    private void testSkipNullsWith(String JSON) throws Exception {
        Outer outer = objectMapper.readValue(JSON, Outer.class);

        validateNotNull(outer);
        validateNotNull(outer.getListMiddle());
        for (Middle middle : outer.getListMiddle()) {
            validateNotNull(middle);
            validateNotNull(middle.getField1());
            validateNotNull(middle.getListInner());
        }
    }

    private static void validateNotNull(Object o) {
        assertNotNull(o);
    }
}
