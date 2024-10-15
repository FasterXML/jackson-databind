package com.fasterxml.jackson.databind.deser.filter;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.deser.DeserializationProblemHandler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import static com.fasterxml.jackson.databind.testutil.DatabindTestUtil.a2q;

public class ProblemHandler1767Test
{
    static class IntHandler
            extends DeserializationProblemHandler
    {
        @Override
        public Object handleWeirdStringValue(DeserializationContext ctxt,
                Class<?> targetType,
                String valueToConvert,
                String failureMsg)
        {
            if (targetType != Integer.TYPE) {
                return NOT_HANDLED;
            }
            return 1;
        }
    }

    static class TestBean {
        int a;

        public int getA() {
            return a;
        }

        public void setA(int a) {
            this.a = a;
        }

    }

    @Test
    public void testPrimitivePropertyWithHandler() throws Exception {
        final ObjectMapper mapper = new ObjectMapper();
        mapper.addHandler(new IntHandler());
        TestBean result = mapper.readValue(a2q("{'a': 'not-a-number'}"), TestBean.class);
        assertNotNull(result);
        assertEquals(1, result.a);
    }
}
