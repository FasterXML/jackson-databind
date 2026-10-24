package com.fasterxml.jackson.databind.jsontype.vld;

import java.util.*;
import java.util.logging.FileHandler;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.cfg.DeserializerFactoryConfig;
import com.fasterxml.jackson.databind.deser.BeanDeserializerFactory;
import com.fasterxml.jackson.databind.deser.DefaultDeserializationContext;
import com.fasterxml.jackson.databind.exc.InvalidDefinitionException;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Creating a {@link KeyDeserializer} for a type must get the same type checks
 * as creating a bean deserializer, when keys are deserialized via
 * single-String constructor or factory method.
 */
public class MapKeyTypeCheckTest extends DatabindTestUtil
{
    // Custom factory that denies one more type
    static class StricterDeserializerFactory extends BeanDeserializerFactory
    {
        private static final long serialVersionUID = 1L;

        public StricterDeserializerFactory() {
            super(new DeserializerFactoryConfig());
        }

        @Override
        protected void _validateSubType(DeserializationContext ctxt, JavaType type,
                BeanDescription beanDesc)
            throws JsonMappingException
        {
            if (type.hasRawClass(StringBuilder.class)) {
                ctxt.reportBadTypeDefinition(beanDesc, "Custom-denied type");
            }
            super._validateSubType(ctxt, type, beanDesc);
        }
    }

    // Custom factory that allows a type otherwise denied
    static class LenientDeserializerFactory extends BeanDeserializerFactory
    {
        private static final long serialVersionUID = 1L;

        public LenientDeserializerFactory() {
            super(new DeserializerFactoryConfig());
        }

        @Override
        protected void _validateSubType(DeserializationContext ctxt, JavaType type,
                BeanDescription beanDesc)
            throws JsonMappingException
        {
            if (!type.hasRawClass(FileHandler.class)) {
                super._validateSubType(ctxt, type, beanDesc);
            }
        }
    }

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

    // Overridden `_validateSubType()` must apply to key deserializers too,
    // whether it denies more types...
    @Test
    public void stricterValidationAppliedToKeyDeserializers() throws Exception
    {
        ObjectMapper mapper = _mapper(new StricterDeserializerFactory());
        InvalidDefinitionException e = assertThrows(InvalidDefinitionException.class,
                () -> _findKeyDeserializer(mapper, StringBuilder.class));
        verifyException(e, "Custom-denied type");
    }

    // ... or fewer
    @Test
    public void lenientValidationAppliedToKeyDeserializers() throws Exception
    {
        ObjectMapper mapper = _mapper(new LenientDeserializerFactory());
        assertNotNull(_findKeyDeserializer(mapper, FileHandler.class));
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

    private ObjectMapper _mapper(BeanDeserializerFactory factory) {
        return new ObjectMapper(null, null, new DefaultDeserializationContext.Impl(factory));
    }
}
