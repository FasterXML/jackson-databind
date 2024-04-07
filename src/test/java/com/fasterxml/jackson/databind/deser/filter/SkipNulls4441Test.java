package com.fasterxml.jackson.databind.deser.filter;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.assertNotNull;

// [databind#4441] @JsonSetter(nulls = Nulls.SKIP) doesn't work in some situations
public class SkipNulls4441Test
    extends DatabindTestUtil
{
    static class Middle {
        @JsonSetter(nulls = Nulls.SKIP)
        private final List<Inner> listInner = new ArrayList<>();
        private final String field1;

        @JsonCreator
        public Middle(@JsonProperty("field1") String field1) {
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

        @JsonCreator
        public Inner(@JsonProperty("field1") String field1) {
            this.field1 = field1;
        }

        public String getField1() {
            return field1;
        }
    }

    static class MiddleSetter {
        private List<InnerSetter> listInner = new ArrayList<>();
        private final String field1;

        @JsonCreator
        public MiddleSetter(@JsonProperty("field1") String field1) {
            this.field1 = field1;
        }

        @JsonSetter(nulls = Nulls.SKIP)
        public void setListInner(List<InnerSetter> listInner) {
            // null passed here
            Objects.requireNonNull(listInner);
            this.listInner = listInner;
        }

        public List<InnerSetter> getListInner() {
            return listInner;
        }

        public String getField1() {
            return field1;
        }
    }

    static class InnerSetter {
        private final String field1;

        @JsonCreator
        public InnerSetter(@JsonProperty("field1") String field1) {
            this.field1 = field1;
        }

        public String getField1() {
            return field1;
        }
    }

    private final ObjectMapper objectMapper = JsonMapper.builder().build();

    private final String NULL_ENDING_JSON = a2q("{" +
            "    'field1': 'data',     " +
            "    'listInner': null  " +
            "}");

    private final String NULL_BEGINNING_JSON = a2q("{" +
            "    'listInner': null,  " +
            "    'field1': 'data'     " +
            "}");

    @Test
    public void testFields() throws Exception {
        // Passes
        // For some reason, if most-inner "list1" field is null in the end, it works
        _testFieldNullSkip(NULL_ENDING_JSON);
        // Fails
        // But if it's null in the beginning, it doesn't work
        _testFieldNullSkip(NULL_BEGINNING_JSON);
    }

    @Test
    public void testMethods() throws Exception {
        // Passes
        // For some reason, if most-inner "list1" field is null in the end, it works
        _testMethodNullSkip(NULL_ENDING_JSON);
        // Fails
        // But if it's null in the beginning, it doesn't work
        _testMethodNullSkip(NULL_BEGINNING_JSON);
    }

    private void _testMethodNullSkip(String s) throws Exception {
        MiddleSetter middle = objectMapper.readValue(s, MiddleSetter.class);

        testMiddleSetter(middle);
    }

    private void _testFieldNullSkip(String s) throws Exception {
        Middle middle = objectMapper.readValue(s, Middle.class);

        testMiddle(middle);
    }

    private void testMiddle(Middle middle) {
        validateNotNull(middle);
        validateNotNull(middle.getField1());
        validateNotNull(middle.getListInner());
    }

    private void testMiddleSetter(MiddleSetter middle) {
        validateNotNull(middle);
        validateNotNull(middle.getField1());
        validateNotNull(middle.getListInner());
    }

    private static void validateNotNull(Object o) {
        assertNotNull(o);
    }
}
