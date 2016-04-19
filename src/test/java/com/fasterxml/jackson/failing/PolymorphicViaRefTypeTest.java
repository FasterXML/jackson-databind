package com.fasterxml.jackson.failing;

import java.util.concurrent.atomic.AtomicReference;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.ObjectMapper.DefaultTyping;

public class PolymorphicViaRefTypeTest extends BaseMapTest
{
    static class AtomicStringWrapper {
        public AtomicReference<StringWrapper> wrapper;

        protected AtomicStringWrapper() { }
        public AtomicStringWrapper(String str) {
            wrapper = new AtomicReference<StringWrapper>(new StringWrapper(str));
        }
    }
    
    /*
    /**********************************************************************
    /* Test methods
    /**********************************************************************
     */

    public void testAtomicRefViaDefaultTyping() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        mapper.enableDefaultTyping(DefaultTyping.NON_FINAL);
        AtomicStringWrapper data = new AtomicStringWrapper("foo");
        String json = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(data);
        AtomicStringWrapper result = mapper.readValue(json, AtomicStringWrapper.class);
        assertNotNull(result);
    }
}
