
package com.fasterxml.jackson.databind.ext;

import java.io.IOException;
import java.nio.file.Path;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdScalarSerializer;

/**
 * @since 2.8
 */
public class NioPathSerializer extends StdScalarSerializer<Path>
{
    private static final long serialVersionUID = 1;

    public NioPathSerializer() { super(Path.class); }

    @Override
    public void serialize(Path value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        // write the Path as a URI, always.
        gen.writeString(value.toUri().toString());
    }
}
