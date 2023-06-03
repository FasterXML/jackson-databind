package com.fasterxml.jackson.databind.convert;

import com.fasterxml.jackson.databind.BaseMapTest;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.cfg.CoercionAction;
import com.fasterxml.jackson.databind.cfg.CoercionInputShape;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.fasterxml.jackson.databind.type.TypeFactory;
import java.util.List;

public class DisableCoercions3690Test extends BaseMapTest
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
    public void testFailMessage3924() throws Exception {
        // Arrange : Building a strict ObjectMapper.
        ObjectMapper objectMapper = jsonMapperBuilder()
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
        TypeFactory typeFactory = objectMapper.getTypeFactory();
        JavaType arrayType = typeFactory.constructParametricType(List.class, String.class);
        JavaType inputType = typeFactory.constructParametricType(Input3924.class, arrayType);
        
        // Arrange : Input data
        String[][] inputs = new String[][]{
                new String[]{"{ \"field\": [ 1 ] }", "Cannot coerce Integer value (1)", "to `java.lang.String` value"},
                new String[]{"{ \"field\": [ [ 1 ] ] }", "Cannot coerce Array value ([)", "to `java.lang.String` value"},
                new String[]{"{ \"field\": [ { \"field\": 1 } ] }", "Cannot coerce Object value ({)", "to `java.lang.String` value"}
        };

        // Act & Assert
        for (String[] input : inputs) {
            _verifyFailedCoercionWithInvalidFormat(objectMapper, inputType, input);
        }
    }

    private void _verifyFailedCoercionWithInvalidFormat(ObjectMapper objectMapper, JavaType inputType, String[] input) 
        throws Exception
    {
        String jsonStr = input[0];
        String inputMessage = input[1];
        String targetTypeMessage = input[2];
        try {
            objectMapper.readValue(jsonStr, inputType);
            fail("Should not pass");
        } catch (InvalidFormatException e) {
            assertEquals(String.class, e.getTargetType());
            verifyException(e, inputMessage, targetTypeMessage);
        }
    }
}
