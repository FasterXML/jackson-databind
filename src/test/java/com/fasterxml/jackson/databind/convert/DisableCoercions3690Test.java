package com.fasterxml.jackson.databind.convert;

import java.util.List;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.cfg.CoercionAction;
import com.fasterxml.jackson.databind.cfg.CoercionInputShape;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;

public class DisableCoercions3690Test extends BaseMapTest
{
    static class Input3690 {
        public List<String> field;
    }

    // [databind#3690]
    public void testFailMessage3690() throws Exception
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
            verifyException(e, "Cannot coerce Integer value (1)");
            verifyException(e, "to `java.lang.String` value");
        }
    }
}
