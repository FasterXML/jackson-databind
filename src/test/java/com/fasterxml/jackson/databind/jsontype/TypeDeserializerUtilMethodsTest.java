package com.fasterxml.jackson.databind.jsontype;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.*;

import com.fasterxml.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class TypeDeserializerUtilMethodsTest extends DatabindTestUtil
{
    @Test
    public void testUtilMethods() throws Exception
    {
        final JsonFactory f = new JsonFactory();

        JsonParser p = f.createParser("true");
        assertNull(TypeDeserializer.deserializeIfNatural(p, null, Object.class));
        p.nextToken();
        assertEquals(Boolean.TRUE, TypeDeserializer.deserializeIfNatural(p, null, Object.class));
        p.close();

        p = f.createParser("false ");
        p.nextToken();
        assertEquals(Boolean.FALSE, TypeDeserializer.deserializeIfNatural(p, null, Object.class));
        p.close();

        p = f.createParser("1");
        p.nextToken();
        assertEquals(Integer.valueOf(1), TypeDeserializer.deserializeIfNatural(p, null, Object.class));
        p.close();

        p = f.createParser("0.5 ");
        p.nextToken();
        assertEquals(Double.valueOf(0.5), TypeDeserializer.deserializeIfNatural(p, null, Object.class));
        p.close();

        p = f.createParser("\"foo\" [ ] ");
        p.nextToken();
        assertEquals("foo", TypeDeserializer.deserializeIfNatural(p, null, Object.class));

        p.nextToken();
        assertNull(TypeDeserializer.deserializeIfNatural(p, null, Object.class));

        p.close();
    }
}
