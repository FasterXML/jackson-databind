package com.fasterxml.jackson.databind.ext;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdScalarDeserializer;

public class PathDeserializer extends StdScalarDeserializer<Path>
{
    private static final long serialVersionUID = 1;

    public PathDeserializer() { super(Path.class); }
    
    @Override
    public Path deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        JsonToken t= p.getCurrentToken();
        if (t != null) {
            if (t.isScalarValue()) {
                return Paths.get(p.getValueAsString());
            }
            // 16-Oct-2015: should we perhaps allow JSON Arrays (of Strings) as well?
        }
        throw ctxt.mappingException(Path.class, t);
    }
}
