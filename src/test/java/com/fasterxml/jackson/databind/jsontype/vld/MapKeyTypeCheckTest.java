package com.fasterxml.jackson.databind.jsontype.vld;

import java.util.*;
import java.util.logging.FileHandler;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.deser.std.StdKeyDeserializers;
import com.fasterxml.jackson.databind.exc.InvalidDefinitionException;
import com.fasterxml.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Map key types deserialized via single-String constructor or factory method
 * must be checked against the same deny list as bean types.
 */
public class MapKeyTypeCheckTest extends DatabindTestUtil
{
    static class KeyedBean {
        public Map<FileHandler, String> values;
    }

    private final ObjectMapper MAPPER = newJsonMapper();

    @Test
    public void illegalMapKeyTypeRejected() throws Exception
    {
        InvalidDefinitionException e = assertThrows(InvalidDefinitionException.class,
                () -> MAPPER.readValue(a2q("{'values':{'key':'x'}}"), KeyedBean.class));
        verifyException(e, "Illegal type");
        verifyException(e, FileHandler.class.getName());
    }

    // Check must also apply when key deserializer is requested directly
    @Test
    public void illegalTypeRejectedByStdKeyDeserializers() throws Exception
    {
        InvalidDefinitionException e = assertThrows(InvalidDefinitionException.class,
                () -> StdKeyDeserializers.findStringBasedKeyDeserializer(
                        MAPPER.getDeserializationConfig(), MAPPER.constructType(FileHandler.class)));
        verifyException(e, "Illegal type");
    }

    @Test
    public void stringCtorMapKeyTypeStillWorks() throws Exception
    {
        Map<StringBuilder, String> result = MAPPER.readValue(a2q("{'key':'x'}"),
                new TypeReference<Map<StringBuilder, String>>() { });
        assertEquals(1, result.size());
        assertEquals("key", result.keySet().iterator().next().toString());
    }
}
