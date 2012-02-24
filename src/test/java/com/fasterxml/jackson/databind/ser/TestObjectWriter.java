package com.fasterxml.jackson.databind.ser;

import java.util.*;


import com.fasterxml.jackson.core.PrettyPrinter;
import com.fasterxml.jackson.databind.*;

/**
 * Unit tests for checking features added to {@link ObjectWriter}, such
 * as adding of explicit pretty printer.
 */
public class TestObjectWriter
    extends BaseMapTest
{
    public void testPrettyPrinter() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        ObjectWriter writer = mapper.writer();
        HashMap<String, Integer> data = new HashMap<String,Integer>();
        data.put("a", 1);
        
        // default: no indentation
        assertEquals("{\"a\":1}", writer.writeValueAsString(data));

        // and then with standard
        writer = writer.withDefaultPrettyPrinter();
        assertEquals("{\n  \"a\" : 1\n}", writer.writeValueAsString(data));

        // and finally, again without indentation
        writer = writer.with((PrettyPrinter) null);
        assertEquals("{\"a\":1}", writer.writeValueAsString(data));
    }
}
