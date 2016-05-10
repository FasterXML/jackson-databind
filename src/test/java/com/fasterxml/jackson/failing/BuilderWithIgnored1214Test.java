package com.fasterxml.jackson.failing;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

public class BuilderWithIgnored1214Test extends BaseMapTest
{
    @JsonDeserialize(builder = TestObject1214.Builder.class)
    @JsonIgnoreProperties(ignoreUnknown = true)
    static class TestObject1214 {
        final String property1;

        private TestObject1214(Builder builder) {
            property1 = builder.property1;
        }

        public static Builder builder() {
            return new Builder();
        }

        public String getProperty1() {
            return property1;
        }

        static class Builder {

            private String property1;

            public Builder withProperty1(String p1) {
                property1 = p1;
                return this;
            }

            public TestObject1214 build() {
                return new TestObject1214(this);
            }
        }
    }

    public void testUnknown1214() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        TestObject1214 value = mapper.readValue(aposToQuotes
                ("{'property1':'a', 'property2':'b'}"),
                TestObject1214.class);
        assertEquals("a", value.property1);
    }
}
