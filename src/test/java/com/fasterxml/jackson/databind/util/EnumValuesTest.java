package com.fasterxml.jackson.databind.util;

import java.util.List;

import com.fasterxml.jackson.databind.BaseMapTest;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.cfg.EnumFeature;
import com.fasterxml.jackson.databind.introspect.AnnotatedClass;
import com.fasterxml.jackson.databind.introspect.AnnotatedClassResolver;

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

    private final ObjectMapper MAPPER = new ObjectMapper();

    public void testConstructFromName() {
        SerializationConfig cfg = MAPPER.getSerializationConfig()
                .without(SerializationFeature.WRITE_ENUMS_USING_TO_STRING);
        AnnotatedClass enumClass = resolve(MAPPER, ABC.class);
        EnumValues values = EnumValues.construct(cfg, enumClass);
        assertEquals("A", values.serializedValueFor(ABC.A).toString());
        assertEquals("B", values.serializedValueFor(ABC.B).toString());
        assertEquals("C", values.serializedValueFor(ABC.C).toString());
        assertEquals(3, values.values().size());
        assertEquals(3, values.internalMap().size());
    }

    public void testConstructWithToString() {
        SerializationConfig cfg = MAPPER.getSerializationConfig()
                .with(SerializationFeature.WRITE_ENUMS_USING_TO_STRING);
        AnnotatedClass enumClass = resolve(MAPPER, ABC.class);
        EnumValues values = EnumValues.construct(cfg, enumClass);
        assertEquals("A", values.serializedValueFor(ABC.A).toString());
        assertEquals("b", values.serializedValueFor(ABC.B).toString());
        assertEquals("C", values.serializedValueFor(ABC.C).toString());
        assertEquals(3, values.values().size());
        assertEquals(3, values.internalMap().size());
    }

    public void testEnumResolverNew()
    {
        AnnotatedClass annotatedClass = resolve(MAPPER, ABC.class);
        EnumResolver enumRes = EnumResolver.constructUsingToString(MAPPER.getDeserializationConfig(), annotatedClass);
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

    // [databind#3053]
    public void testConstructFromNameLowerCased() {
        SerializationConfig cfg = MAPPER.getSerializationConfig()
            .with(EnumFeature.WRITE_ENUMS_TO_LOWERCASE);
        AnnotatedClass enumClass = resolve(MAPPER, ABC.class);
        EnumValues values = EnumValues.construct(cfg, enumClass);
        assertEquals("a", values.serializedValueFor(ABC.A).toString());
        assertEquals("b", values.serializedValueFor(ABC.B).toString());
        assertEquals("c", values.serializedValueFor(ABC.C).toString());
        assertEquals(3, values.values().size());
        assertEquals(3, values.internalMap().size());
    }

    private AnnotatedClass resolve(ObjectMapper mapper, Class<?> enumClass) {
        return AnnotatedClassResolver.resolve(mapper.getSerializationConfig(),
                mapper.constructType(enumClass), null);
    }
}
