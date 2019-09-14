package com.fasterxml.jackson.failing;

import java.util.*;

import com.fasterxml.jackson.databind.*;

public class EnumAsIndexMapKey2129Test extends BaseMapTest
{
    // [databind#2129]
    public enum Type {
        FIRST,
        SECOND;
    }

    static class TypeContainer {
        public Map<Type, Integer> values;

        public TypeContainer(Type type, int value) {
            values = Collections.singletonMap(type, value);
        }
    }

    final ObjectMapper MAPPER = newJsonMapper();
    
    // [databind#2129]
    public void testEnumAsIndexForRootMap() throws Exception
    {
        final Map<Type, Integer> input = Collections.singletonMap(Type.FIRST, 3);

        // by default, write using name()
        assertEquals(aposToQuotes("{'FIRST':3}"),
                MAPPER.writeValueAsString(input));

        // but change with setting
        assertEquals(aposToQuotes("{'0':3}"),
                MAPPER.writer()
                .with(SerializationFeature.WRITE_ENUM_KEYS_USING_INDEX)
                .writeValueAsString(input));

        // but NOT with value settings
        assertEquals(aposToQuotes("{'FIRST':3}"),
                MAPPER.writer()
                    .with(SerializationFeature.WRITE_ENUMS_USING_INDEX)
                    .writeValueAsString(input));
    }
    
    // [databind#2129]
    public void testEnumAsIndexForValueMap() throws Exception
    {
        final TypeContainer input = new TypeContainer(Type.SECOND, 72);

        // by default, write using name()
        assertEquals(aposToQuotes("{'values':{'SECOND':72}}"),
                MAPPER.writeValueAsString(input));

        // but change with setting
        assertEquals(aposToQuotes("{'values':{'1':72}}"),
                MAPPER.writer()
                .with(SerializationFeature.WRITE_ENUM_KEYS_USING_INDEX)
                .writeValueAsString(input));

        // but NOT with value settings
        assertEquals(aposToQuotes("{'values':{'SECOND':72}}"),
                MAPPER.writer()
                    .with(SerializationFeature.WRITE_ENUMS_USING_INDEX)
                    .writeValueAsString(input));
    }
}
