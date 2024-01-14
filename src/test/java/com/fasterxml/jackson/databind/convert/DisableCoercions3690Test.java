package com.fasterxml.jackson.databind.convert;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.cfg.CoercionAction;
import com.fasterxml.jackson.databind.cfg.CoercionInputShape;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import com.fasterxml.jackson.databind.type.TypeFactory;

import static org.junit.jupiter.api.Assertions.*;

import static com.fasterxml.jackson.databind.testutil.DatabindTestUtil.*;

public class DisableCoercions3690Test
{
    // [databind#3690]
    static class Input3690 {
        public List<String> field;
    }

    static class Input3924<T> {
        private T field;

        public T getField() {
            return field;
        }

        public void setField(T field) {
            this.field = field;
        }
    }

    // [databind#3690]
    @Test
    public void testCoercionFail3690() throws Exception
    {
        ObjectMapper mapper = jsonMapperBuilder()
                .withCoercionConfigDefaults(config -> {
                    config.setCoercion(CoercionInputShape.Boolean, CoercionAction.Fail)
                        .setCoercion(CoercionInputShape.Integer, CoercionAction.Fail)
                        .setCoercion(CoercionInputShape.Float, CoercionAction.Fail)
                        .setCoercion(CoercionInputShape.String, CoercionAction.Fail)
                        .setCoercion(CoercionInputShape.Array, CoercionAction.Fail)
                        .setCoercion(CoercionInputShape.Object, CoercionAction.Fail);
                })
                .build();
        String json = "{ \"field\": [ 1 ] }";
        try {
            mapper.readValue(json, Input3690.class);
            fail("Should not pass");
        } catch (InvalidFormatException e) {
            assertEquals(String.class, e.getTargetType());
            assertEquals(Integer.valueOf(1), e.getValue());
            verifyException(e, "Cannot coerce Integer value (1)");
            verifyException(e, "to `java.lang.String` value");
        }
    }

    // [databind#3924]
    @Test
    public void testFailMessage3924() throws Exception {
        // Arrange : Building a strict ObjectMapper.
        ObjectMapper mapper = jsonMapperBuilder()
                .withCoercionConfigDefaults(config -> {
                    config.setCoercion(CoercionInputShape.Boolean, CoercionAction.Fail)
                        .setCoercion(CoercionInputShape.Integer, CoercionAction.Fail)
                        .setCoercion(CoercionInputShape.Float, CoercionAction.Fail)
                        .setCoercion(CoercionInputShape.String, CoercionAction.Fail)
                        .setCoercion(CoercionInputShape.Array, CoercionAction.Fail)
                        .setCoercion(CoercionInputShape.Object, CoercionAction.Fail);
                })
                .build();

        // Arrange : Type configuration 
        TypeFactory typeFactory = mapper.getTypeFactory();
        JavaType arrayType = typeFactory.constructParametricType(List.class, String.class);
        JavaType inputType = typeFactory.constructParametricType(Input3924.class, arrayType);

        // Act & Assert
        _verifyFailedCoercionWithInvalidFormat("{ \"field\": [ 1 ] }", 
                "Cannot coerce Integer value (1) to `java.lang.String` value",
                mapper, inputType);
        
        _verifyFailedCoercionWithInvalidFormat("{ \"field\": [ [ 1 ] ] }", 
                "Cannot deserialize value of type `java.lang.String` from Array value", 
                mapper, inputType);
        
        _verifyFailedCoercionWithInvalidFormat("{ \"field\": [ { \"field\": 1 } ] }",
                "Cannot deserialize value of type `java.lang.String` from Object value",
                mapper, inputType);
        
    }

    private void _verifyFailedCoercionWithInvalidFormat(String jsonStr, String expectedMsg, ObjectMapper mapper, 
                                                        JavaType inputType) throws Exception 
    {
        try {
            mapper.readValue(jsonStr, inputType);
            fail("Should not pass");
        } catch (MismatchedInputException e) {
            assertEquals(String.class, e.getTargetType());
            verifyException(e, expectedMsg);
        }
    }
}
