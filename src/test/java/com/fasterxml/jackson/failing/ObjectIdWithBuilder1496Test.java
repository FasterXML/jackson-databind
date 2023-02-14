package com.fasterxml.jackson.failing;

import java.util.concurrent.ConcurrentHashMap;

import com.fasterxml.jackson.annotation.*;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

public class ObjectIdWithBuilder1496Test extends BaseMapTest
{
    @JsonIdentityInfo(generator=ObjectIdGenerators.PropertyGenerator.class, property="id")
    @JsonDeserialize(builder=POJOBuilder.class)
    static class POJO
    {
         private long id;
         public long getId() { return id; }
         private int var;
         public int getVar() { return var; }

         POJO (long id, int var) { this.id = id; this.var = var; }

         @Override
         public String toString() { return "id: " + id + ", var: " + var; }
    }

    @JsonIdentityInfo(generator=ObjectIdGenerators.PropertyGenerator.class, property="id")
    @JsonPOJOBuilder(withPrefix = "", buildMethodName="readFromCacheOrBuild")
    static final class POJOBuilder {
        // Standard builder stuff
        private long id;
        private int var;

        public POJOBuilder id(long _id) { id = _id; return this; }
        public POJOBuilder var(int _var) { var = _var; return this; }

        public POJO build() { return new POJO(id, var); }

        // Special build method for jackson deserializer that caches objects already deserialized
        private final static ConcurrentHashMap<Long, POJO> cache = new ConcurrentHashMap<>();
        public POJO readFromCacheOrBuild() {
            POJO pojo = cache.get(id);
            if (pojo == null) {
                POJO newPojo = build();
                pojo = cache.putIfAbsent(id, newPojo);
                if (pojo == null) {
                    pojo = newPojo;
                }
            }
            return pojo;
        }
    }

    /*
    /**********************************************************
    /* Test methods
    /**********************************************************
     */

    private final ObjectMapper MAPPER = newJsonMapper();

    public void testBuilderId1496() throws Exception
    {
        POJO input = new POJOBuilder().id(123L).var(456).build();
        String json = MAPPER.writeValueAsString(input);
        POJO result = MAPPER.readValue(json, POJO.class);
        assertNotNull(result);
    }
}
