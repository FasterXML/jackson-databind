package com.fasterxml.jackson.failing;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.PropertyAccessor;

import com.fasterxml.jackson.databind.*;

// Should be failing but isn't... keeping here for a bit, until underlying
// problem is resolved (was active 21-Jul-2017)
public class EnumCreator1699Test extends BaseMapTest
{
    enum JacksonTest {
        TEST1(0), TEST2(1);

        private JacksonTest(int i) { }

        @JsonCreator
        public static JacksonTest fromInt(int i) {
            switch (i) {
            case 1: return TEST1;
            case 2: return TEST2;
            default: throw new IllegalArgumentException();
            }
        }
    }

    public void testIssue1699() throws Exception
    {
        final ObjectMapper mapper = new ObjectMapper();

        mapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.NONE);
        mapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);
        mapper.setVisibility(PropertyAccessor.CREATOR, JsonAutoDetect.Visibility.ANY);

        JacksonTest result = mapper.readValue("1", JacksonTest.class);
        assertEquals(JacksonTest.TEST1, result);
    }
}
