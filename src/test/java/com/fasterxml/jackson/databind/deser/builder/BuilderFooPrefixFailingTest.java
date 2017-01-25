package com.fasterxml.jackson.databind.deser.builder;

import java.io.IOException;

import com.fasterxml.jackson.databind.BaseMapTest;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

public class BuilderFooPrefixFailingTest extends BaseMapTest {

    public void testFooPrefix() throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        String strFilters = "{\"field\":\"item1\",\"operator\":\"not_equal\",\"value\":\"1132\"}";
        Filter filter = objectMapper.readValue(strFilters, Filter.class);
    }

    @JsonDeserialize(builder = Filter.FilterBuilder.class)
    @JsonPOJOBuilder(withPrefix = "foo")
    static class Filter {
        private String field;
        private String operator;
        private String value;

        private Filter() {
            // private constructor
        }

        public String field() {
            return this.field;
        }

        public String operator() {
            return this.operator;
        }

        public String value() {
            return this.value;
        }

        static FilterBuilder builder() {
            return new FilterBuilder();
        }

        static class FilterBuilder {
            private String field;
            private String operator;
            private String value;

            public FilterBuilder fooField(String field) {
                this.field = field;
                return this;
            }

            public FilterBuilder fooOperator(String operator) {
                this.operator = operator;
                return this;
            }

            public FilterBuilder fooValue(String value) {
                this.value = value;
                return this;
            }

            public Filter build() {
                Filter filter = new Filter();

                filter.field = this.field;
                filter.operator = this.operator;
                filter.value = this.value;

                return filter;
            }
        }
    }
}
