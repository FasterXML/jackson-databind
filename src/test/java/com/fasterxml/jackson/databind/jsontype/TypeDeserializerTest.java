package com.fasterxml.jackson.databind.jsontype;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.core.json.JsonFactory;
import com.fasterxml.jackson.databind.*;

public class TypeDeserializerTest extends BaseMapTest
{
    public void testUtilMethods() throws Exception
    {
        final JsonFactory f = new JsonFactory();

        JsonParser p = f.createParser(ObjectReadContext.empty(), "true");
        assertNull(TypeDeserializer.deserializeIfNatural(p, null, Object.class));
        p.nextToken();
        assertEquals(Boolean.TRUE, TypeDeserializer.deserializeIfNatural(p, null, Object.class));
        p.close();

        p = f.createParser(ObjectReadContext.empty(), "false ");
        p.nextToken();
        assertEquals(Boolean.FALSE, TypeDeserializer.deserializeIfNatural(p, null, Object.class));
        p.close();

        p = f.createParser(ObjectReadContext.empty(), "1");
        p.nextToken();
        assertEquals(Integer.valueOf(1), TypeDeserializer.deserializeIfNatural(p, null, Object.class));
        p.close();

        p = f.createParser(ObjectReadContext.empty(), "0.5 ");
        p.nextToken();
        assertEquals(Double.valueOf(0.5), TypeDeserializer.deserializeIfNatural(p, null, Object.class));
        p.close();

        p = f.createParser(ObjectReadContext.empty(), "\"foo\" [ ] ");
        p.nextToken();
        assertEquals("foo", TypeDeserializer.deserializeIfNatural(p, null, Object.class));

        p.nextToken();
        assertNull(TypeDeserializer.deserializeIfNatural(p, null, Object.class));

        p.close();
    }
}
