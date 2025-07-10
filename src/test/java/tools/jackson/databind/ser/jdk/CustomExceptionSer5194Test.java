package tools.jackson.databind.ser.jdk;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import tools.jackson.databind.*;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.testutil.DatabindTestUtil;

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
    // 09-Jul-2025, tatu: Works for 2.x, fails for 3.x -- no idea why, disabled for now
    @Disabled
    @Test
    public void test5194() throws Exception {
        ObjectMapper mapper = JsonMapper.builder()
                .changeDefaultVisibility(vc -> vc
                    .withVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.NONE)
                    .withVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY)
                    )
                .build();

        String json = mapper.writerWithDefaultPrettyPrinter()
                .writeValueAsString(new MyIllegalArgumentException());
        //System.err.println("JSON: " + json);
        assertNotNull(json);
    }
}
