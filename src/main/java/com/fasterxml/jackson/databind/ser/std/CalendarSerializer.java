package com.fasterxml.jackson.databind.ser.std;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Calendar;

import com.fasterxml.jackson.core.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JacksonStdImpl;

/**
 * Standard serializer for {@link java.util.Calendar}.
 * As with other time/date types, is configurable to produce timestamps
 * (standard Java 64-bit timestamp) or textual formats (usually ISO-8601).
 */
@JacksonStdImpl
public class CalendarSerializer
    extends StdScalarSerializer<Calendar>
{
    public static CalendarSerializer instance = new CalendarSerializer();

    public CalendarSerializer() { super(Calendar.class); }
    
    @Override
    public boolean isEmpty(Calendar value) {
        // let's assume "null date" (timestamp 0) qualifies for empty
        return (value == null) || (value.getTimeInMillis() == 0L);
    }

    @Override
    public void serialize(Calendar value, JsonGenerator jgen, SerializerProvider provider)
        throws IOException, JsonGenerationException
    {
        provider.defaultSerializeDateValue(value.getTimeInMillis(), jgen);
    }

    @Override
    public JsonNode getSchema(SerializerProvider provider, Type typeHint)
    {
        //TODO: (ryan) add a format for the date in the schema?
        return createSchemaNode(provider.isEnabled(SerializationConfig.Feature.WRITE_DATES_AS_TIMESTAMPS)
                ? "number" : "string", true);
    }
}
