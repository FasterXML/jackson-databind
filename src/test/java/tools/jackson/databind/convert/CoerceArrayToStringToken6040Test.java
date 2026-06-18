package tools.jackson.databind.convert;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import tools.jackson.databind.JavaType;
import tools.jackson.databind.cfg.CoercionAction;
import tools.jackson.databind.cfg.CoercionInputShape;
import tools.jackson.databind.exc.MismatchedInputException;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.type.TypeFactory;

import static org.junit.jupiter.api.Assertions.fail;

import static tools.jackson.databind.testutil.DatabindTestUtil.*;

/**
 * Tests for [databind#6040]: when an (empty) JSON Array is encountered where a
 * String is expected, and coercion is disallowed, the failure should be reported
 * against the {@code START_ARRAY} token (as with other Array-to-String coercions),
 * not the {@code END_ARRAY} token.
 */
public class CoerceArrayToStringToken6040Test
{
    static class Wrapper6040<T> {
        public T field;
    }

    private final JsonMapper MAPPER = JsonMapper.builder()
            .withCoercionConfigDefaults(cfg ->
                    cfg.setCoercion(CoercionInputShape.String, CoercionAction.Fail))
            .build();

    private final TypeFactory TF = TypeFactory.createDefaultInstance();

    // Plain String target: was already correct
    @Test
    public void arrayForString() throws Exception {
        JavaType t = TF.constructParametricType(Wrapper6040.class, String.class);
        _verifyStartArrayFailure("{ \"field\": [] }", t);
    }

    // [databind#6040]: String as Collection element used to report END_ARRAY
    @Test
    public void arrayForStringCollectionElement() throws Exception {
        JavaType inner = TF.constructParametricType(List.class, String.class);
        JavaType t = TF.constructParametricType(Wrapper6040.class, inner);
        _verifyStartArrayFailure("{ \"field\": [ [] ] }", t);
    }

    // String as Map value: was already correct
    @Test
    public void arrayForStringMapValue() throws Exception {
        JavaType inner = TF.constructParametricType(Map.class, String.class, String.class);
        JavaType t = TF.constructParametricType(Wrapper6040.class, inner);
        _verifyStartArrayFailure("{ \"field\": { \"field\": [] } }", t);
    }

    private void _verifyStartArrayFailure(String json, JavaType targetType) throws Exception {
        try {
            MAPPER.readValue(json, targetType);
            fail("Should not pass");
        } catch (MismatchedInputException e) {
            verifyException(e, "from Array value (token `JsonToken.START_ARRAY`)");
        }
    }
}
