package com.fasterxml.jackson.databind.ext;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdScalarDeserializer;

/**
 * @since 2.8
 */
public class NioPathDeserializer extends StdScalarDeserializer<Path>
{
    private static final long serialVersionUID = 1;

    public NioPathDeserializer() { super(Path.class); }

    @Override
    public Path deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        if (!p.hasToken(JsonToken.VALUE_STRING)) {
// 19-May-2016, tatu: Need to rework as part of exception handling improvements for 2.8;
//    for now use simpler call only because `wrongTokenException()` will mean something
//    different, and mismatch will need different call
//            throw ctxt.wrongTokenException(p, JsonToken.VALUE_STRING, "The value of a java.nio.file.Path must be a string");
            ctxt.reportMappingException(Path.class, p.getCurrentToken());
        }
        final String value = p.getText();
        // If someone gives us an input with no : at all, treat as local path, instead of failing
        // with invalid URI.
        if (value.indexOf(':') < 0) {
            return Paths.get(value);
        }
        try {
            URI uri = new URI(value);
            return Paths.get(uri);
        } catch (URISyntaxException e) {
            return (Path) ctxt.handleInstantiationProblem(handledType(), value, e);
        }
    }
}
