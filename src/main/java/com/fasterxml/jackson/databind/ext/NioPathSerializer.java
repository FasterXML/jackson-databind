
package com.fasterxml.jackson.databind.ext;

import java.io.IOException;
import java.nio.file.Path;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.type.WritableTypeId;

import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
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

    // [databind#1688]: Not sure this is 100% ok, considering there are legitimately different
    //  impls... but has to do
    @Override
    public void serializeWithType(Path value, JsonGenerator g,
            SerializerProvider provider, TypeSerializer typeSer) throws IOException
    {
        // Better ensure we don't use specific sub-classes:
        WritableTypeId typeIdDef = typeSer.writeTypePrefix(g,
                typeSer.typeId(value, Path.class, JsonToken.VALUE_STRING));
        serialize(value, g, provider);
        typeSer.writeTypeSuffix(g, typeIdDef);
    }
}
