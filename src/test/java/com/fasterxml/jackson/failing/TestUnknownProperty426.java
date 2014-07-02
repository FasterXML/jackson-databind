package com.fasterxml.jackson.failing;

import com.fasterxml.jackson.annotation.*;

import com.fasterxml.jackson.databind.*;

/**
 * Unit tests for checking handling of unknown properties
 */
public class TestUnknownProperty426 extends BaseMapTest
{
    // For [Issue#426]
    @JsonIgnoreProperties({ "userId"})
    public class User {
        Integer userId; 

        void setUserId(String id) {
            setUserId(new Integer(id));
        }
    
        public Integer getUserId() {
            return userId;
        }
    
        public void setUserId(Integer v) {
            this.userId = v;
        }
    }

    /*
    /**********************************************************
    /* Test methods
    /**********************************************************
     */

    private final ObjectMapper MAPPER = new ObjectMapper();

    public void testIssue426() throws Exception
    {
        String jsonString = "{id: 9, firstName: \"Mike\" }";
        User result = MAPPER.reader( User.class ).readValue(jsonString);
        assertNotNull(result);
    }
}

