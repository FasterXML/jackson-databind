package com.fasterxml.jackson.databind.ser.jdk;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.assertNotNull;

public class CustomExceptionSer5194Test
    extends DatabindTestUtil
{
    static class MyIllegalArgumentException extends RuntimeException {
        private static final long serialVersionUID = 1L;

        public MyIllegalArgumentException() {
            super();
        }

        public MyIllegalArgumentException(String s) {
            super(s);
        }

        public MyIllegalArgumentException(String message, Throwable cause) {
            super(message, cause);
        }

        public MyIllegalArgumentException(Throwable cause) {
            super(cause);
        }
    }

    // [databind#5194]: failed to serialize custom exception
    @Test
    public void test5194() throws Exception {
        ObjectMapper mapper = JsonMapper.builder()
                .visibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.NONE)
                .visibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY)
                .build();

        String json = mapper.writeValueAsString(new MyIllegalArgumentException());
        assertNotNull(json);
    }
}
