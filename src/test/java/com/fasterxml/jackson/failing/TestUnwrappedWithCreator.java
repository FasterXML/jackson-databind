package com.fasterxml.jackson.failing;

import com.fasterxml.jackson.annotation.*;

import com.fasterxml.jackson.databind.*;

public class TestUnwrappedWithCreator extends BaseMapTest
{
    class JAddress {
        private String address;
        private String city;
        private String state;
         
        @JsonCreator
        public JAddress(
                @JsonProperty("address1") String address,
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

    class JPerson {
        private String name;
        private JAddress address;
        private String alias;
         
        @JsonCreator
        public JPerson(
        @JsonProperty("name") String name,
        @JsonUnwrapped JAddress address,
        @JsonProperty("alias") String alias
        ) {
            this.name = name;
            this.address = address;
            this.alias = alias;
        }
         
        public String getName() {
            return name;
        }
         
        @JsonUnwrapped public JAddress getAddress() {
            return address;
        }
         
        public String getAlias() {
            return alias;
        }
    }
         
    public void testReadWriteJson() throws Exception
    {
        JPerson person = new JPerson("MyName", new JAddress("main street", "springfield", null), null);
        ObjectMapper mapper = new ObjectMapper();
        String json = mapper.writeValueAsString(person);
        System.out.println(json);
        JPerson obj = mapper.readValue(json, JPerson.class);
        assertNotNull(obj);
    }
}
