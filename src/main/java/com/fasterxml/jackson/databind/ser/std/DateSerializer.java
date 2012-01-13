package com.fasterxml.jackson.databind.ser.std;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Date;

import com.fasterxml.jackson.core.*;

import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JacksonStdImpl;

/**
 * For efficiency, we will serialize Dates as longs, instead of
 * potentially more readable Strings.
 */
@JacksonStdImpl
public class DateSerializer
    extends ScalarSerializerBase<Date>
{
    public static DateSerializer instance = new DateSerializer();
    
    public DateSerializer() { super(Date.class); }

    @Override
    public boolean isEmpty(Date value) {
        // let's assume "null date" (timestamp 0) qualifies for empty
        return (value == null) || (value.getTime() == 0L);
    }
    
    @Override
    public void serialize(Date value, JsonGenerator jgen, SerializerProvider provider)
        throws IOException, JsonGenerationException
    {
        provider.defaultSerializeDateValue(value, jgen);
    }

    @Override
    public JsonNode getSchema(SerializerProvider provider, Type typeHint)
    {
        //todo: (ryan) add a format for the date in the schema?
        return createSchemaNode(provider.isEnabled(SerializationConfig.Feature.WRITE_DATES_AS_TIMESTAMPS)
                ? "number" : "string", true);
    }
}
