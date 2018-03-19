package com.fasterxml.jackson.databind.jsontype;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.jsontype.impl.StdTypeResolverBuilder;

import java.util.*;

// [databind#1395]: prevent attempts at including type info for primitives
public class DefaultTypingWithPrimitivesTest extends BaseMapTest
{
    static class Data {
        public long key;
    }

    public void testDefaultTypingWithLong() throws Exception
    {
        Data data = new Data();
        data.key = 1L;
        Map<String, Object> mapData = new HashMap<String, Object>();
        mapData.put("longInMap", 2L);
        mapData.put("longAsField", data);

        // Configure Jackson to preserve types
        StdTypeResolverBuilder resolver = new StdTypeResolverBuilder(JsonTypeInfo.Id.CLASS,
                JsonTypeInfo.As.PROPERTY, "__t");
        ObjectMapper mapper = ObjectMapper.builder()
                .enable(SerializationFeature.INDENT_OUTPUT)
                .setDefaultTyping(resolver)
                .build();

        // Serialize
        String json = mapper.writeValueAsString(mapData);

        // Deserialize
        Map<?,?> result = mapper.readValue(json, Map.class);
        assertNotNull(result);
        assertEquals(2, result.size());
    }
}
