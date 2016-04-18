package com.fasterxml.jackson.databind.jsontype;

import java.util.concurrent.atomic.AtomicReference;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.*;

public class PolymorphicViaRefTypeTest extends BaseMapTest
{
    
    @JsonSubTypes({
        @JsonSubTypes.Type(name = "impl5", value = ImplForAtomic.class)
    })
    static class BaseForAtomic {
    }

    static class ImplForAtomic extends BaseForAtomic {
        public int x;

        protected ImplForAtomic() { }
        public ImplForAtomic(int x) { this.x = x; }
    }

    static class TypeInfoAtomic {
        @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "$type")
        public AtomicReference<BaseForAtomic> value;
    }

    /*
    /**********************************************************************
    /* Test methods
    /**********************************************************************
     */

    private final ObjectMapper MAPPER = objectMapper();

    public void testOptionalWithAtomic() throws Exception
    {
        TypeInfoAtomic data = new TypeInfoAtomic();
        data.value = new AtomicReference<BaseForAtomic>(new ImplForAtomic(42));
        String json = MAPPER.writeValueAsString(data);
        TypeInfoAtomic result = MAPPER.readValue(json, TypeInfoAtomic.class);
        assertNotNull(result);
    }
}
