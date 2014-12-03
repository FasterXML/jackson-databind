package com.fasterxml.jackson.databind.ser;

import java.util.*;

import com.fasterxml.jackson.databind.BaseMapTest;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.annotation.JsonAppend;

/**
 * Tests for verifying that one can append virtual properties after regular ones.
 * 
 * @since 2.5
 */
public class TestVirtualProperties extends BaseMapTest
{
    @JsonAppend(attrs={ @JsonAppend.Attr("id"),
        @JsonAppend.Attr(value="internal", propName="extra", required=true)
    })
    static class SimpleBean
    {
        public int value = 13;
    }

    enum ABC {
        A, B, C;
    }
    
    /*
    /**********************************************************
    /* Test methods
    /**********************************************************
     */

    private final ObjectWriter WRITER = objectWriter();

    public void testAttributeProperties() throws Exception
    {
        Map<String,Object> stuff = new LinkedHashMap<String,Object>();
        stuff.put("x", 3);
        stuff.put("y", ABC.B);
        String json = WRITER.withAttribute("id", "abc123")
                .withAttribute("internal", stuff)
                .writeValueAsString(new SimpleBean());
        assertEquals(aposToQuotes("{'value':13,'id':'abc123','extra':{'x':3,'y':'B'}}"), json);
    }
}
