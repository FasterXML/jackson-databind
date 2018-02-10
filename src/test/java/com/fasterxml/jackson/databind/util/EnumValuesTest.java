package com.fasterxml.jackson.databind.util;

import java.util.List;

import com.fasterxml.jackson.databind.BaseMapTest;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.SerializationFeature;

public class EnumValuesTest extends BaseMapTest
{
    enum ABC {
        A("A"),
        B("b"),
        C("C");

        private final String desc;

        private ABC(String d) { desc = d; }

        @Override
        public String toString() { return desc; }
    }

    final ObjectMapper MAPPER = new ObjectMapper();

    @SuppressWarnings("unchecked")
    public void testConstructFromName() {
        SerializationConfig cfg = MAPPER.serializationConfig()
                .without(SerializationFeature.WRITE_ENUMS_USING_TO_STRING);
        Class<Enum<?>> enumClass = (Class<Enum<?>>)(Class<?>) ABC.class;
        EnumValues values = EnumValues.construct(cfg, enumClass);
        assertEquals("A", values.serializedValueFor(ABC.A).toString());
        assertEquals("B", values.serializedValueFor(ABC.B).toString());
        assertEquals("C", values.serializedValueFor(ABC.C).toString());
        assertEquals(3, values.values().size());
        assertEquals(3, values.internalMap().size());
    }

    @SuppressWarnings("unchecked")
    public void testConstructWithToString() {
        SerializationConfig cfg = MAPPER.serializationConfig()
                .with(SerializationFeature.WRITE_ENUMS_USING_TO_STRING);
        Class<Enum<?>> enumClass = (Class<Enum<?>>)(Class<?>) ABC.class;
        EnumValues values = EnumValues.construct(cfg, enumClass);
        assertEquals("A", values.serializedValueFor(ABC.A).toString());
        assertEquals("b", values.serializedValueFor(ABC.B).toString());
        assertEquals("C", values.serializedValueFor(ABC.C).toString());
        assertEquals(3, values.values().size());
        assertEquals(3, values.internalMap().size());
    }

    public void testEnumResolver()
    {
        EnumResolver enumRes = EnumResolver.constructUnsafeUsingToString(ABC.class, null);
        assertEquals(ABC.B, enumRes.getEnum(1));
        assertNull(enumRes.getEnum(-1));
        assertNull(enumRes.getEnum(3));
        assertEquals(2, enumRes.lastValidIndex());
        List<Enum<?>> enums = enumRes.getEnums();
        assertEquals(3, enums.size());
        assertEquals(ABC.A, enums.get(0));
        assertEquals(ABC.B, enums.get(1));
        assertEquals(ABC.C, enums.get(2));
    }
}
