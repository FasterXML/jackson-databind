package com.fasterxml.jackson.databind.ext.jdk8;

import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.type.ReferenceType;

public class JDK8TypesTest extends BaseMapTest
{
    private final ObjectMapper MAPPER = newObjectMapper();

    public void testOptionalsAreReferentialTypes() throws Exception
    {
        JavaType t = MAPPER.constructType(Optional.class);
        assertTrue(t.isReferenceType());
        ReferenceType rt = (ReferenceType) t;
        assertEquals(Object.class, rt.getContentType().getRawClass());

        t = MAPPER.constructType(OptionalInt.class);
        assertTrue(t.isReferenceType());
        rt = (ReferenceType) t;
        assertEquals(Integer.TYPE, rt.getContentType().getRawClass());

        t = MAPPER.constructType(OptionalLong.class);
        assertTrue(t.isReferenceType());
        rt = (ReferenceType) t;
        assertEquals(Long.TYPE, rt.getContentType().getRawClass());

        t = MAPPER.constructType(OptionalDouble.class);
        assertTrue(t.isReferenceType());
        rt = (ReferenceType) t;
        assertEquals(Double.TYPE, rt.getContentType().getRawClass());
   }
}
