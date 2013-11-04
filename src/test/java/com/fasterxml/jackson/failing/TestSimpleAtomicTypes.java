package com.fasterxml.jackson.failing;

import java.util.concurrent.atomic.*;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.ObjectMapper;

/* Looks like AtomicReference has issues with polymorphic
 * types.
 */
public class TestSimpleAtomicTypes
    extends com.fasterxml.jackson.databind.BaseMapTest
{
    private final ObjectMapper MAPPER = objectMapper();

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME)
    @JsonSubTypes({ @JsonSubTypes.Type(Impl.class) })
    static abstract class Base { }

    @JsonTypeName("I")
    static class Impl extends Base {
        public int value;

        public Impl() { }
        public Impl(int v) { value = v; }
    }

    static class RefWrapper
    {
        public AtomicReference<Base> w;

        public RefWrapper() { }
        public RefWrapper(Base b) {
            w = new AtomicReference<Base>(b);
        }
        public RefWrapper(int i) {
            w = new AtomicReference<Base>(new Impl(i));
        }
    }
    
    /*
    /**********************************************************
    /* Test methods
    /**********************************************************
     */

    // [Issue#340]
    public void testPolymorphicAtomicReference() throws Exception
    {
        RefWrapper input = new RefWrapper(13);
        String json = MAPPER.writeValueAsString(input);
        
        RefWrapper result = MAPPER.readValue(json, RefWrapper.class);
        assertNotNull(result.w);
        Object ob = result.w.get();
        assertEquals(Impl.class, ob);
        assertEquals(13, ((Impl) ob).value);
    }
}
