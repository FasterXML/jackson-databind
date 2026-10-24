package com.fasterxml.jackson.databind.jsontype.vld;

import java.util.*;
import java.util.logging.FileHandler;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.cfg.DeserializerFactoryConfig;
import com.fasterxml.jackson.databind.deser.BeanDeserializerFactory;
import com.fasterxml.jackson.databind.deser.DefaultDeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdKeyDeserializers;
import com.fasterxml.jackson.databind.exc.InvalidDefinitionException;
import com.fasterxml.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Map key types must get the same type checks as bean types, also when
 * keys are deserialized via single-String constructor.
 */
public class MapKeyTypeCheckTest extends DatabindTestUtil
{
    // Custom factory that denies one more type, to verify override hook
    // is used for Map keys as well
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

    private final ObjectMapper MAPPER = newJsonMapper();

    @Test
    public void illegalMapKeyTypeRejected() throws Exception
    {
        JavaType mapType = MAPPER.getTypeFactory().constructMapType(HashMap.class,
                FileHandler.class, String.class);
        InvalidDefinitionException e = assertThrows(InvalidDefinitionException.class,
                () -> MAPPER.readValue(a2q("{'key':'x'}"), mapType));
        verifyException(e, "Illegal type");
    }

    // Check must also apply when String-based key deserializer is
    // requested directly, not via deserializer factory
    @Test
    public void illegalTypeRejectedByStdKeyDeserializers() throws Exception
    {
        InvalidDefinitionException e = assertThrows(InvalidDefinitionException.class,
                () -> StdKeyDeserializers.findStringBasedKeyDeserializer(
                        MAPPER.getDeserializationConfig(), MAPPER.constructType(FileHandler.class)));
        verifyException(e, "Illegal type");
    }

    // Overridden `_validateSubType()` must apply to Map keys too
    @Test
    public void customValidationAppliedToMapKeys() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper(null, null,
                new DefaultDeserializationContext.Impl(new StricterDeserializerFactory()));
        JavaType mapType = mapper.getTypeFactory().constructMapType(HashMap.class,
                StringBuilder.class, String.class);
        InvalidDefinitionException e = assertThrows(InvalidDefinitionException.class,
                () -> mapper.readValue(a2q("{'key':'x'}"), mapType));
        verifyException(e, "Custom-denied type");
    }

    @Test
    public void stringCtorMapKeyTypeStillWorks() throws Exception
    {
        JavaType mapType = MAPPER.getTypeFactory().constructMapType(HashMap.class,
                StringBuilder.class, String.class);
        Map<StringBuilder, String> result = MAPPER.readValue(a2q("{'key':'x'}"), mapType);
        assertEquals(1, result.size());
        assertEquals("key", result.keySet().iterator().next().toString());
    }
}
