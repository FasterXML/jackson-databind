package com.fasterxml.jackson.databind.struct;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;

import com.fasterxml.jackson.databind.*;

public class TestObjectIdWithPolymorphic extends BaseMapTest
{
    @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS)
    @JsonIdentityInfo(generator=ObjectIdGenerators.IntSequenceGenerator.class, property="id")
    static class Base
    {
        public int value;

        public Base next;
        
        public Base() { this(0); }
        public Base(int v) {
            value = v;
        }
    }

    static class Impl extends Base
    {
        public int extra;

        public Impl() { this(0, 0); }
        public Impl(int v, int e) {
            super(v);
            extra = e;
        }
    }
    
    /*
    /*****************************************************
    /* Unit tests for polymorphic type handling
    /*****************************************************
     */

    private final ObjectMapper mapper = new ObjectMapper();

    public void testPolymorphicRoundtrip() throws Exception
    {
        // create simple 2 node loop:
        Impl in1 = new Impl(123, 456);
        in1.next = new Impl(111, 222);
        in1.next.next = in1;
        
        String json = mapper.writeValueAsString(in1);
        
        // then bring back...
        Base result0 = mapper.readValue(json, Base.class);
        assertNotNull(result0);
        assertSame(Impl.class, result0.getClass());
        Impl result = (Impl) result0;
        assertEquals(123, result.value);
        assertEquals(456, result.extra);
        Impl result2 = (Impl) result.next;
        assertEquals(111, result2.value);
        assertEquals(222, result2.extra);
        assertSame(result, result2.next);
    }

}
