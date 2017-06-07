package com.fasterxml.jackson.databind.deser.builder;

import java.io.IOException;

import com.fasterxml.jackson.databind.BaseMapTest;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

public class BuilderWithPrefixPassingTest extends BaseMapTest {

    public void testWithPrefix() throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        String strFilters = "{\"field\":\"item1\",\"operator\":\"not_equal\",\"value\":\"1132\"}";
        Filter filter = objectMapper.readValue(strFilters, Filter.class);
    }

    @JsonDeserialize(builder = Filter.FilterBuilder.class)
    @JsonPOJOBuilder(withPrefix = "with")
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

            public FilterBuilder withField(String field) {
                this.field = field;
                return this;
            }

            public FilterBuilder withOperator(String operator) {
                this.operator = operator;
                return this;
            }

            public FilterBuilder withValue(String value) {
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
