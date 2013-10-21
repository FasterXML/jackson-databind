package com.fasterxml.jackson.failing;

import com.fasterxml.jackson.annotation.*;

import com.fasterxml.jackson.databind.*;

public class TestUnwrappedWithCreator extends BaseMapTest
{
    static class JAddress {
        private String address;
        private String city;
        private String state;
         
        @JsonCreator
        public JAddress(
                @JsonProperty("address") String address,
                @JsonProperty("city") String city,
                @JsonProperty("state") String state
        ){
            this.address = address;
            this.city = city;
            this.state = state;
        }
         
        public String getAddress1() { return address; }
        public String getCity() { return city; }
        public String getState() { return state; }
    }

    static class JPerson {
        private String _name;
        private JAddress _address;
        private String _alias;
         
        @JsonCreator
        public JPerson(
        @JsonProperty("name") String name,
        @JsonUnwrapped JAddress address,
        @JsonProperty("alias") String alias
        ) {
            _name = name;
            _address = address;
            _alias = alias;
        }
         
        public String getName() {
            return _name;
        }
         
        @JsonUnwrapped public JAddress getAddress() {
            return _address;
        }
         
        public String getAlias() { return _alias; }
    }

    /*
    /**********************************************************
    /* Test methods
    /**********************************************************
     */

    // For [Issue#265] / [Scala#90]
    public void testUnwrappedWithCreator() throws Exception
    {
        JPerson person = new JPerson("MyName", new JAddress("main street", "springfield", "WA"), "bubba");
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
