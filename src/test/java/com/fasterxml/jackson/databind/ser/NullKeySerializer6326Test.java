package com.fasterxml.jackson.databind.ser;

import java.util.Collections;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.exc.InvalidDefinitionException;
import com.fasterxml.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.assertThrows;

// [databind#6326]: custom `SerializerFactory` returning `null` key serializer
// should fail with `InvalidDefinitionException`, not NPE
class NullKeySerializer6326Test extends DatabindTestUtil
{
    @SuppressWarnings("serial")
    static class NullKeySerializerFactory extends BeanSerializerFactory {
        NullKeySerializerFactory() { super(null); }

        @Override
        public JsonSerializer<Object> createKeySerializer(SerializerProvider prov,
                JavaType type, JsonSerializer<Object> defaultImpl) {
            return null;
        }
    }

    @Test
    void testNullKeySerializer() throws Exception
    {
        ObjectMapper mapper = newJsonMapper();
        mapper.setSerializerFactory(new NullKeySerializerFactory());

        InvalidDefinitionException e = assertThrows(InvalidDefinitionException.class,
                () -> mapper.writeValueAsString(Collections.singletonMap("k", "v")));
        verifyException(e, "returned `null` key serializer");
    }
}
