package com.fasterxml.jackson.failing;

import com.fasterxml.jackson.annotation.*;

import com.fasterxml.jackson.databind.*;

public class TestNameConflicts extends BaseMapTest
{
    static class Bean193
    {
        @JsonProperty("val1")
        private int x;
        @JsonIgnore
        private int value2;
        
        public Bean193(@JsonProperty("val1")int value1,
                    @JsonProperty("val2")int value2)
        {
            this.x = value1;
            this.value2 = value2;
        }
        
        @JsonProperty("val2")
        int x()
        {
            return value2;
        }
    }
    
    /*
    /**********************************************************
    /* Unit tests
    /**********************************************************
     */

    public void testIssue193() throws Exception
    {
        String json = objectWriter().writeValueAsString(new Bean193(1, 2));
        assertNotNull(json);
    }
}
