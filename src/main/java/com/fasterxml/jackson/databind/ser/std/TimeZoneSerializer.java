package com.fasterxml.jackson.databind.ser.std;

import java.io.IOException;
import java.util.TimeZone;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;

@SuppressWarnings("serial")
public class TimeZoneSerializer extends StdScalarSerializer<TimeZone>
{
    public TimeZoneSerializer() { super(TimeZone.class); }

    @Override
    public void serialize(TimeZone value, JsonGenerator jgen, SerializerProvider provider) throws IOException {
        jgen.writeString(value.getID());
    }

    @Override
    public void serializeWithType(TimeZone value, JsonGenerator jgen, SerializerProvider provider, TypeSerializer typeSer) throws IOException {
        // Better ensure we don't use specific sub-classes:
        typeSer.writeTypePrefixForScalar(value, jgen, TimeZone.class);
        serialize(value, jgen, provider);
        typeSer.writeTypeSuffixForScalar(value, jgen);
    }
}
