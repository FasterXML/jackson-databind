package tools.jackson.databind.convert;

import java.util.List;

import tools.jackson.databind.*;
import tools.jackson.databind.cfg.CoercionAction;
import tools.jackson.databind.cfg.CoercionInputShape;
import tools.jackson.databind.exc.InvalidFormatException;

public class DisableCoercions3690Test extends BaseMapTest
{
    // [databind#3690]
    static class Input3690 {
        public List<String> field;
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
}
