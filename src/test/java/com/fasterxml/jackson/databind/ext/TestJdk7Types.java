package com.fasterxml.jackson.databind.ext;

import java.nio.file.Path;
import java.nio.file.Paths;

import com.fasterxml.jackson.databind.*;

/**
 * @since 2.7
 */
public class TestJdk7Types extends BaseMapTest
{
    public void testPathRoundtrip() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
    
        // Start with serialization, actually
        Path input = Paths.get("tmp", "foo.txt");

        String json = mapper.writeValueAsString(input);
        assertNotNull(json);
        
        Path p = mapper.readValue(json, Path.class);
        assertNotNull(p);
        
        assertEquals(input.toUri(), p.toUri());
    }
}
