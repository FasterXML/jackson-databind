package com.fasterxml.jackson.databind.struct;

import com.fasterxml.jackson.annotation.*;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.exc.InvalidDefinitionException;

public class UnwrappedCreatorParam265Test extends BaseMapTest
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

    static class JPersonWithoutName
    {
        public String name;

        protected JAddress _address;

        @JsonCreator
        public JPersonWithoutName(@JsonProperty("name") String name,
                @JsonUnwrapped JAddress address)
        {
            this.name = name;
            _address = address;
        }

        @JsonUnwrapped
        public JAddress getAddress() { return _address; }
    }

    static class JPersonWithName
    {
        public String name;

        protected JAddress _address;

        @JsonCreator
        public JPersonWithName(@JsonProperty("name") String name,
                @JsonUnwrapped
                @JsonProperty("address")
                JAddress address)
        {
            this.name = name;
            _address = address;
        }

        @JsonUnwrapped
        public JAddress getAddress() { return _address; }
    }

    /*
    /**********************************************************
    /* Test methods
    /**********************************************************
     */

    // For [databind#265]: handle problem by throwing exception
    public void testUnwrappedWithUnnamedCreatorParam() throws Exception
    {
        JPersonWithoutName person = new JPersonWithoutName("MyName", new JAddress("main street", "springfield", "WA"));
        ObjectMapper mapper = new ObjectMapper();
        // serialization should be fine as far as that goes
        String json = mapper.writeValueAsString(person);

        // but not deserialization:
        try {
            /*JPersonWithoutName result =*/ mapper.readValue(json, JPersonWithoutName.class);
            fail("Should not pass");
        } catch (InvalidDefinitionException e) {
            verifyException(e, "Cannot define Creator parameter");
            verifyException(e, "@JsonUnwrapped");
        }
    }

    // For [databind#265]: handle problem by throwing exception
    public void testUnwrappedWithNamedCreatorParam() throws Exception
    {
        JPersonWithName person = new JPersonWithName("MyName", new JAddress("main street", "springfield", "WA"));
        ObjectMapper mapper = new ObjectMapper();
        // serialization should be fine as far as that goes
        String json = mapper.writeValueAsString(person);
        try {
            /*JPersonWithName result =*/ mapper.readValue(json, JPersonWithName.class);
            fail("Should not pass");
        } catch (InvalidDefinitionException e) {
            verifyException(e, "Cannot define Creator property \"address\"");
            verifyException(e, "@JsonUnwrapped");
        }
    }
}
