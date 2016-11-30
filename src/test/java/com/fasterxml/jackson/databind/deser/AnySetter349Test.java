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
    
    public void testUnwrappedWithAny() throws Exception
    {
        final ObjectMapper mapper = objectMapper();
        final String json = aposToQuotes(
"{ 'type' : 'IST',\n"
//+" 'spacename' : 'Foo Models',\n"
//+" 'name' : 'BLAH-New',\n"
//+" 'description' : 'namespace.name: X THIN FIR.DR-WD12-New',\n"
+" 'ZoomLinks': [ 'foofoofoofoo', 'barbarbarbar' ] }"
                );
        Bean349 value = mapper.readValue(json,  Bean349.class);
        assertNotNull(value);
    }
}
