package com.fasterxml.jackson.failing;

import com.fasterxml.jackson.annotation.*;

import com.fasterxml.jackson.databind.*;

/**
 * Unit tests for checking handling of unknown properties
 */
public class TestUnknownProperty426 extends BaseMapTest
{
    // For [databind#426]
    @JsonIgnoreProperties({ "userId" })
    static class User {
        public String firstName;
        Integer userId; 

        public void setUserId(CharSequence id) {
            // 21-Dec-2015, tatu: With a fix in 2.7, use of String would not
            //   trigger the problem, so use CharSequence...
            setUserId(Integer.valueOf(id.toString()));
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

