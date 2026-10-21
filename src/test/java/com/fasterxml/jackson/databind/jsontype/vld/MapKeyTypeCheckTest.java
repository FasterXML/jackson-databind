package com.fasterxml.jackson.databind.jsontype.vld;

import java.util.*;
import java.util.logging.FileHandler;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.InvalidDefinitionException;
import com.fasterxml.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Map key types must get the same type checks as bean types, also when
 * keys are deserialized via single-String constructor.
 */
public class MapKeyTypeCheckTest extends DatabindTestUtil
{
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
