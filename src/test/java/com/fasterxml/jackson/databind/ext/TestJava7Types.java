package com.fasterxml.jackson.databind.ext;

import java.nio.file.FileSystem;
import java.nio.file.Path;

import com.fasterxml.jackson.databind.*;

import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;

/**
 * @since 2.7
 */
public class TestJava7Types extends BaseMapTest
{
    public void testPathRoundtrip() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();

        FileSystem fs = Jimfs.newFileSystem(Configuration.unix());
        Path input = fs.getPath("/tmp", "foo.txt");

        String json = mapper.writeValueAsString(input);
        assertNotNull(json);
        
        Path p = mapper.readValue(json, Path.class);
        assertNotNull(p);
        
        assertEquals(input.toUri(), p.toUri());
        assertEquals(input, p);
        fs.close();
    }
}
