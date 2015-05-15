package com.fasterxml.jackson.failing;

import com.fasterxml.jackson.annotation.*;

import com.fasterxml.jackson.databind.*;

/**
 * Unit tests for checking handling of unknown properties
 */
public class TestUnknownProperty426 extends BaseMapTest
{
    // For [Issue#426]
    @JsonIgnoreProperties({ "userId" })
    static class User {
        public String firstName;
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
        final String JSON = aposToQuotes("{'userId': 9, 'firstName': 'Mike' }");
        User result = MAPPER.readerFor(User.class).readValue(JSON);
        assertNotNull(result);
        assertEquals("Mike", result.firstName);
    }
}

