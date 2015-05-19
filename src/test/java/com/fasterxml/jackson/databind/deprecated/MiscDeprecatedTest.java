package com.fasterxml.jackson.databind.deprecated;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.ser.std.EnumMapSerializer;
import com.fasterxml.jackson.databind.util.EnumValues;

@SuppressWarnings("deprecation")
public class MiscDeprecatedTest extends BaseMapTest
{
    private final ObjectMapper MAPPER = objectMapper();

    @SuppressWarnings("unchecked")
    public void testOldEnumMapSerializer() throws Exception
    {
        /*
        public EnumMapSerializer(JavaType valueType, boolean staticTyping, EnumValues keyEnums,
                TypeSerializer vts, JsonSerializer<Object> valueSerializer)
                */
        // to be removed from 2.7 or so:
        Class<?> enumClass = ABC.class;
        EnumMapSerializer ser = new EnumMapSerializer(MAPPER.constructType(String.class), true,
                EnumValues.construct(MAPPER.getSerializationConfig(), (Class<Enum<?>>) enumClass),
                null, /* value serializer */ null);
        
        // ... and?
        assertNotNull(ser);
    }
}
