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
@SuppressWarnings("serial")
public class DateSerializer
    extends DateTimeSerializerBase<Date>
{
    /**
     * Default instance that is used when no contextual configuration
     * is needed.
     */
    public static final DateSerializer instance = new DateSerializer();

    public DateSerializer() {
        this(null, null);
    }

    public DateSerializer(Boolean useTimestamp, DateFormat customFormat) {
        super(Date.class, useTimestamp, customFormat);
    }

    @Override
    public DateSerializer withFormat(Boolean timestamp, DateFormat customFormat) {
        return new DateSerializer(timestamp, customFormat);
    }

    @Override
    protected long _timestamp(Date value) {
        return (value == null) ? 0L : value.getTime();
    }

    @Override
    public void serialize(Date value, JsonGenerator g, SerializerProvider provider) throws IOException
    {
        if (_asTimestamp(provider)) {
            g.writeNumber(_timestamp(value));
            return;
        }
        _serializeAsString(value, g, provider);
    }
}
