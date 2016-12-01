package com.fasterxml.jackson.failing;

import com.fasterxml.jackson.annotation.*;

import com.fasterxml.jackson.databind.*;

public class TestUnwrappedWithCreator265 extends BaseMapTest
{
    static class JAddress {
        public String address;
        public String city;
        public String state;

        protected JAddress() { }

        public JAddress(String address, String city, String state) {
            this.address = address;
            this.city = city;
            this.state = state;
        }
    }

    static class JPerson {
        protected String _name;
        protected JAddress _address;
         
        @JsonCreator
        public JPerson(@JsonProperty("name") String name,
        @JsonUnwrapped JAddress address) {
            _name = name;
            _address = address;
        }
         
        public String getName() {
            return _name;
        }
         
        @JsonUnwrapped public JAddress getAddress() {
            return _address;
        }
    }

    /*
    /**********************************************************
    /* Test methods
    /**********************************************************
     */

    // For [databind#265] / [Scala#90]
    public void testUnwrappedWithCreator() throws Exception
    {
        JPerson person = new JPerson("MyName", new JAddress("main street", "springfield", "WA"));
        ObjectMapper mapper = new ObjectMapper();
        String json = mapper.writeValueAsString(person);
        JPerson result = mapper.readValue(json, JPerson.class);
        assertNotNull(result);
        assertEquals(person._name, result._name);
        assertNotNull(result._address);
        assertEquals(person._address.city, result._address.city);

        // and see that round-tripping works
        assertEquals(json, mapper.writeValueAsString(result));
    }
}
