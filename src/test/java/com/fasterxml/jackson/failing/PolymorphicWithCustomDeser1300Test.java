package com.fasterxml.jackson.failing;

import java.io.IOException;

import com.fasterxml.jackson.annotation.*;

import com.fasterxml.jackson.core.*;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.module.SimpleModule;

public class PolymorphicWithCustomDeser1300Test extends BaseMapTest
{
    // [databind#1300]
    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME,
            include = JsonTypeInfo.As.PROPERTY, property = "type")
    @JsonSubTypes({
            @JsonSubTypes.Type(value = T1.class, name = "T1")
    })
    interface Type1300 {}

    @JsonTypeName("T1")
    static class T1 implements Type1300 {
        public int v;
    }

    /*
    /**********************************************************
    /* Test methods
    /**********************************************************
     */

    // [databind#1300]
    @SuppressWarnings("serial")
    public void testDeserForPolymorphicBaseType() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        SimpleModule mod = new SimpleModule("test", Version.unknownVersion());
        mod.addDeserializer(Type1300.class,
                new StdDeserializer<Type1300>(Type1300.class) {
                    @Override
                    public Type1300 deserialize(JsonParser p,
                            DeserializationContext ctxt) throws IOException
                    {
                        T1 result = new T1();
                        JsonNode n = ctxt.readValue(p, JsonNode.class);
                        result.v = n.path("value").asInt();
                        return result;
                    }
        });
        Type1300 result = mapper.readValue("{\"value\":3, \"type\":\"bogus\"}", Type1300.class);
        assertEquals(T1.class, result.getClass());
        assertEquals(3, ((T1) result).v);
    }

}
