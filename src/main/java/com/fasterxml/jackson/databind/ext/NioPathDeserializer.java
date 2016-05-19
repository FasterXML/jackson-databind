package com.fasterxml.jackson.databind.ext;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 *
 */
public class NioPathDeserializer extends JsonDeserializer<Path> {
    @Override
    public Path deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        if (p.getCurrentToken() == JsonToken.VALUE_NULL) {
            return null;
        } else if (p.getCurrentToken() != JsonToken.VALUE_STRING) {
            throw ctxt.wrongTokenException(p, JsonToken.VALUE_STRING, "The value of a java.nio.file.Path must be a string");
        }
        URI uri;
        try {
            uri = new URI(p.readValueAs(String.class));
        } catch (URISyntaxException e) {
            throw ctxt.instantiationException(Path.class, e);
        }
        return Paths.get(uri);
    }
}
