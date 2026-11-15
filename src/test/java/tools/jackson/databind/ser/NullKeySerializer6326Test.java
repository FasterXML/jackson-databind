package tools.jackson.databind.ser;

import java.util.Collections;

import org.junit.jupiter.api.Test;

import tools.jackson.databind.*;
import tools.jackson.databind.cfg.SerializerFactoryConfig;
import tools.jackson.databind.exc.InvalidDefinitionException;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.assertThrows;

// [databind#6326]: custom `SerializerFactory` returning `null` key serializer
// should fail with `InvalidDefinitionException`, not NPE
class NullKeySerializer6326Test extends DatabindTestUtil
{
    static class NullKeySerializerFactory extends BeanSerializerFactory {
        NullKeySerializerFactory() { super(null); }

        @Override
        public SerializerFactory withConfig(SerializerFactoryConfig config) {
            return this;
        }

        @Override
        public ValueSerializer<Object> createKeySerializer(SerializationContext ctxt,
                JavaType type) {
            return null;
        }
    }

    @Test
    void testNullKeySerializer() throws Exception
    {
        ObjectMapper mapper = JsonMapper.builder()
                .serializerFactory(new NullKeySerializerFactory())
                .build();

        InvalidDefinitionException e = assertThrows(InvalidDefinitionException.class,
                () -> mapper.writeValueAsString(Collections.singletonMap("k", "v")));
        verifyException(e, "returned `null` key serializer");
    }
}
