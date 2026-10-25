package com.fasterxml.jackson.databind.jsontype.vld;

import java.util.*;
import java.util.logging.FileHandler;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.deser.DefaultDeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdKeyDeserializers;
import com.fasterxml.jackson.databind.exc.InvalidDefinitionException;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Creating a {@link KeyDeserializer} for a type must apply the same deny list
 * ({@code SubTypeValidator}) as creating a bean deserializer, when keys are
 * deserialized via single-String constructor or factory method.
 */
public class MapKeyTypeCheckTest extends DatabindTestUtil
{
    static class KeyedBean {
        public Map<FileHandler, String> values;
    }

    static class FileHandlerKeyDeserializer extends KeyDeserializer {
        @Override
        public Object deserializeKey(String key, DeserializationContext ctxt) {
            return null;
        }
    }

    private final ObjectMapper MAPPER = newJsonMapper();

    // Note: none of the tests deserialize `FileHandler` keys; they only
    // create (or try to create) the `KeyDeserializer`

    @Test
    public void keyDeserializerForIllegalTypeRejected() throws Exception
    {
        InvalidDefinitionException e = assertThrows(InvalidDefinitionException.class,
                () -> _findKeyDeserializer(MAPPER, FileHandler.class));
        verifyException(e, "Illegal type");
        verifyException(e, FileHandler.class.getName());
    }

    @Test
    public void keyDeserializerForStringCtorTypeCreated() throws Exception
    {
        KeyDeserializer kd = _findKeyDeserializer(MAPPER, StringBuilder.class);
        assertNotNull(kd);
        assertEquals("key", kd.deserializeKey("key", _context(MAPPER)).toString());
    }

    // Check must also apply when String-based key deserializer is
    // requested directly, not via deserializer factory
    @Test
    public void stdKeyDeserializersRejectIllegalType() throws Exception
    {
        InvalidDefinitionException e = assertThrows(InvalidDefinitionException.class,
                () -> StdKeyDeserializers.findStringBasedKeyDeserializer(
                        MAPPER.getDeserializationConfig(), MAPPER.constructType(FileHandler.class)));
        verifyException(e, "Illegal type");
        verifyException(e, FileHandler.class.getName());
    }

    // Explicitly registered key deserializers are not checked
    @Test
    public void customKeyDeserializerForIllegalTypeAllowed() throws Exception
    {
        ObjectMapper mapper = jsonMapperBuilder()
                .addModule(new SimpleModule()
                        .addKeyDeserializer(FileHandler.class, new FileHandlerKeyDeserializer()))
                .build();
        assertInstanceOf(FileHandlerKeyDeserializer.class,
                _findKeyDeserializer(mapper, FileHandler.class));
    }

    // And end-to-end: Map-valued property with illegal key type
    @Test
    public void illegalMapKeyTypeInPropertyRejected() throws Exception
    {
        InvalidDefinitionException e = assertThrows(InvalidDefinitionException.class,
                () -> MAPPER.readValue(a2q("{'values':{'key':'x'}}"), KeyedBean.class));
        verifyException(e, "Illegal type");
    }

    private KeyDeserializer _findKeyDeserializer(ObjectMapper mapper, Class<?> keyType)
        throws Exception
    {
        return _context(mapper).findKeyDeserializer(mapper.constructType(keyType), null);
    }

    private DeserializationContext _context(ObjectMapper mapper) {
        return ((DefaultDeserializationContext) mapper.getDeserializationContext())
                .createDummyInstance(mapper.getDeserializationConfig());
    }
}
