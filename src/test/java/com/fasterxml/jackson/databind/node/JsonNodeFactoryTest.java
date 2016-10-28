package com.fasterxml.jackson.databind.node;

import java.math.BigDecimal;
import java.math.BigInteger;

import com.fasterxml.jackson.databind.*;

public class JsonNodeFactoryTest extends NodeTestBase
{
    private final ObjectMapper MAPPER = objectMapper();

    public void testSimpleCreation()
    {
        JsonNodeFactory f = MAPPER.getNodeFactory();
        JsonNode n;

        n = f.numberNode((byte) 4);
        assertTrue(n.isInt());
        assertEquals(4, n.intValue());

        assertTrue(f.numberNode((Byte) null).isNull());

        assertTrue(f.numberNode((Short) null).isNull());

        assertTrue(f.numberNode((Integer) null).isNull());

        assertTrue(f.numberNode((Long) null).isNull());

        assertTrue(f.numberNode((Float) null).isNull());

        assertTrue(f.numberNode((Double) null).isNull());

        assertTrue(f.numberNode((BigDecimal) null).isNull());

        assertTrue(f.numberNode((BigInteger) null).isNull());
    }
}
