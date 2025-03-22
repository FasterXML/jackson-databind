package com.fasterxml.jackson.databind.introspect;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

// [databind#4603] Keep stacktrace when re-throwing exception with JsonMappingException
public class RetainStacktrace4603Test
    extends DatabindTestUtil
{

    static class CustomException extends RuntimeException {
        public CustomException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    static class Feature1347DeserBean {

        int value;

        public void setValue(int x) {
            try {
                // Throws ArithmeticException
                int a = x / 0;
            } catch (Exception e) {
                // should NOT get called, but just to
                throw new CustomException("Should NOT get called", e);
            }
        }
    }

    final String JSON = a2q("{'value':3}");

    final ObjectMapper enabledMapper = jsonMapperBuilder()
            .configure(MapperFeature.UNWRAP_ROOT_CAUSE, true)
            .build();

    final ObjectMapper disabledMapper = jsonMapperBuilder()
            .configure(MapperFeature.UNWRAP_ROOT_CAUSE, false)
            .build();

    final ObjectMapper defaultMapper = newJsonMapper();


    // Whether disabled or enabled, should get ArithmeticException
    @Test
    public void testVisibility()
            throws Exception
    {
        DatabindException enabledResult = _tryDeserializeWith(enabledMapper);
        assertInstanceOf(ArithmeticException.class, enabledResult.getCause());

        DatabindException defaultResult = _tryDeserializeWith(defaultMapper);
        assertInstanceOf(ArithmeticException.class, defaultResult.getCause());

        DatabindException disabledResult = _tryDeserializeWith(disabledMapper);
        assertInstanceOf(CustomException.class, disabledResult.getCause());
        assertInstanceOf(ArithmeticException.class, disabledResult.getCause().getCause());

    }

    private DatabindException _tryDeserializeWith(ObjectMapper mapper) {
        return assertThrows(DatabindException.class,
                () -> mapper.readValue(JSON, Feature1347DeserBean.class)
        );
    }

}
