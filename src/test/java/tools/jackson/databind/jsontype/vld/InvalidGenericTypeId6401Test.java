package tools.jackson.databind.jsontype.vld;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

import tools.jackson.databind.DatabindContext;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.annotation.JsonTypeIdResolver;
import tools.jackson.databind.exc.InvalidTypeIdException;
import tools.jackson.databind.jsontype.impl.TypeIdResolverBase;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for [databind#6401]: malformed or conflicting generic type ids
 * must result in {@link InvalidTypeIdException}, not raw
 * {@link IllegalArgumentException}.
 */
public class InvalidGenericTypeId6401Test extends DatabindTestUtil
{
    interface Box<T> { }

    // Generic subtype with type binding conflicting with `Box<Integer>`
    static class LongBox<X> implements Box<Long> { }

    // Custom resolver that uses non-validating `resolveSubType()`
    static class UnvalidatedClassIdResolver extends TypeIdResolverBase
    {
        private JavaType _type;

        @Override
        public void init(JavaType baseType) { _type = baseType; }

        @Override
        public JsonTypeInfo.Id getMechanism() { return JsonTypeInfo.Id.CUSTOM; }

        @Override
        public String idFromValue(DatabindContext ctxt, Object value) { return null; }

        @Override
        public String idFromValueAndType(DatabindContext ctxt, Object value, Class<?> type) { return null; }

        @Override
        public JavaType typeFromId(DatabindContext ctxt, String id) {
            return ctxt.resolveSubType(_type, id);
        }
    }

    static class BoxWrapper {
        // resolved via `resolveAndValidateSubType()`
        @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS)
        public Box<Integer> validated;

        // resolved via `resolveSubType()`
        @JsonTypeInfo(use = JsonTypeInfo.Id.CUSTOM)
        @JsonTypeIdResolver(UnvalidatedClassIdResolver.class)
        public Box<Integer> unvalidated;
    }

    private static final String MALFORMED_ID = "java.util.HashMap<java.lang.String>";

    private final ObjectMapper MAPPER = newJsonMapper();

    @Test
    public void wrongTypeParameterCount() throws Exception {
        _verifyInvalidTypeId("validated", MALFORMED_ID, "expects 2");
        _verifyInvalidTypeId("unvalidated", MALFORMED_ID, "expects 2");
    }

    @Test
    public void conflictingTypeParameters() throws Exception {
        _verifyInvalidTypeId("validated", LongBox.class.getName(), "Type parameter #1/1 differs");
        _verifyInvalidTypeId("unvalidated", LongBox.class.getName(), "Type parameter #1/1 differs");
    }

    private void _verifyInvalidTypeId(String prop, String typeId, String expMsg) {
        InvalidTypeIdException e = assertThrows(InvalidTypeIdException.class,
                () -> MAPPER.readValue("""
                        {"%s": ["%s", {}]}
                        """.formatted(prop, typeId), BoxWrapper.class));
        verifyException(e, "Could not resolve type id '"+typeId+"'");
        verifyException(e, expMsg);
    }
}
