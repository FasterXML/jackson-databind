package com.fasterxml.jackson.databind.ser.std;

import java.io.IOException;
import java.text.DateFormat;
import java.util.*;

import com.fasterxml.jackson.core.*;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.annotation.JacksonStdImpl;

/**
 * For efficiency, we will serialize Dates as longs, instead of
 * potentially more readable Strings.
 */
@JacksonStdImpl
public class DateSerializer
    extends DateTimeSerializerBase<Date>
{
    /**
     * Default instance that is used when no contextual configuration
     * is needed.
     */
    public static final DateSerializer instance = new DateSerializer();
    
    public DateSerializer() {
        this(false, null);
    }
        
    public DateSerializer(boolean useTimestamp, DateFormat customFormat) {
        super(Date.class, useTimestamp, customFormat);
    }

    @Override
    public DateSerializer withFormat(boolean timestamp, DateFormat customFormat)
    {
        if (timestamp) {
            return new DateSerializer(true, null);
        }
        return new DateSerializer(false, customFormat);
    }

    @Override
    protected long _timestamp(Date value) {
        return (value == null) ? 0L : value.getTime();
    }

    @Override
    public void serialize(Date value, JsonGenerator jgen, SerializerProvider provider)
        throws IOException, JsonGenerationException
    {
        if (_useTimestamp) {
            jgen.writeNumber(_timestamp(value));
        } else if (_customFormat != null) {
            // 21-Feb-2011, tatu: not optimal, but better than alternatives:
            synchronized (_customFormat) {
                jgen.writeString(_customFormat.format(value));
            }
        } else {
            provider.defaultSerializeDateValue(value, jgen);
        }
    }
}
