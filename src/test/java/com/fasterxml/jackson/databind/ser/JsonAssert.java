package com.fasterxml.jackson.databind.ser;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.io.InputStream;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.json.JsonMapper;

public class JsonAssert {

    private JsonMapper _mapper; // TODO If we don't want to allow people to configure this, then everything in here can be static which would make it look a lot like JUnit Assertions.

    public JsonAssert() {
        // TODO Does it make sense to use compact canonical JSON in unit tests? The diff of the comparison will be hard to use.
        _mapper = CanonicalJsonMapper.builder().prettyPrint().build();
    }
    
    public void assertJson(JsonTestResource expected, Object actual) {
        assertJson(loadResource(expected), actual);
    }
    
    public void assertJson(JsonNode expected, Object actual) {
        assertEquals(serialize(expected), serialize(actual));
    }
    
    public String serialize(JsonNode input) {
        try {
            // TODO Is there a better way to sort the keys than deserializing the whole tree?
            Object obj = _mapper.treeToValue(input, Object.class);
            return _mapper.writeValueAsString(obj);
        } catch(JacksonException e) {
            throw new AssertionError("Serializing failed: " + input, e);
        }
    }
    
    public String serialize(Object data) {
        if (data instanceof JsonNode) {
            return serialize((JsonNode)data);
        }
        
        // TODO Sorting mostly works except for properties defined by @JsonTypeInfo
        try {
            return _mapper.writeValueAsString(data);
        } catch(JacksonException e) {
            throw new AssertionError("Serializing failed: " + data, e);
        }
    }
    
    public JsonNode loadResource(JsonTestResource resource) {
        try (InputStream stream = resource.getInputStream()) {
            // TODO Formatting ok? JUnit 4 or 5 here?
            assertNotNull("Missing resource " + resource, stream);

            return _mapper.readTree(stream);
        } catch (IOException e) {
            throw new AssertionError("Error loading " + resource, e);
        }
    }
}
