package com.fasterxml.jackson.databind.deser;

import java.util.*;

import com.fasterxml.jackson.annotation.*;

import com.fasterxml.jackson.databind.*;

// test(s) for [databind#349]
public class AnySetter349Test extends BaseMapTest
{
    static class Bean349
    {
        public String type;
        public int x, y;
    
        private Map<String, Object> props = new HashMap<>();
    
        @JsonAnySetter
        public void addProperty(String key, Object value) {
            props.put(key, value);
        }
    
        @JsonAnyGetter
        public Map<String, Object> getProperties() {
            return props;
        }
    
        @JsonUnwrapped
        public IdentityDTO349 identity;
    }

    static class IdentityDTO349 {
        public int x, y;
    }

    final static String UNWRAPPED_JSON_349 = aposToQuotes(
"{ 'type' : 'IST',\n"
+" 'x' : 3,\n"
//+" 'name' : 'BLAH-New',\n"
//+" 'description' : 'namespace.name: X THIN FIR.DR-WD12-New',\n"
+" 'ZoomLinks': [ 'foofoofoofoo', 'barbarbarbar' ],\n"
+" 'y' : 4, 'z' : 8 }"
            );
    
    public void testUnwrappedWithAny() throws Exception
    {
        final ObjectMapper mapper = objectMapper();
        Bean349 value = mapper.readValue(UNWRAPPED_JSON_349,  Bean349.class);
        assertNotNull(value);
        assertEquals(3, value.x);
        assertEquals(4, value.y);
        assertEquals(2, value.props.size());
    }

    public void testUnwrappedWithAnyAsUpdate() throws Exception
    {
        final ObjectMapper mapper = objectMapper();
        Bean349 bean = mapper.readerFor(Bean349.class)
                .withValueToUpdate(new Bean349())
                .readValue(UNWRAPPED_JSON_349);
        assertEquals(3, bean.x);
        assertEquals(4, bean.y);
        assertEquals(2, bean.props.size());
    }
}
