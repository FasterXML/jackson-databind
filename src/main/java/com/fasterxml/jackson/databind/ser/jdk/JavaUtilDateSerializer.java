package com.fasterxml.jackson.databind.ser.jdk;

import java.text.DateFormat;
import java.util.*;

import tools.jackson.core.*;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.annotation.JacksonStdImpl;

/**
 * For efficiency, we will serialize Dates as longs, instead of
 * potentially more readable Strings.
 *<p>
 * NOTE: name was {@code DateSerializer} in Jackson 2.x
 */
@JacksonStdImpl
public class JavaUtilDateSerializer
    extends DateTimeSerializerBase<Date>
{
    /**
     * Default instance that is used when no contextual configuration
     * is needed.
     */
    public static final JavaUtilDateSerializer instance = new JavaUtilDateSerializer();
    
    public JavaUtilDateSerializer() {
        this(null, null);
    }
        
    public JavaUtilDateSerializer(Boolean useTimestamp, DateFormat customFormat) {
        super(Date.class, useTimestamp, customFormat);
    }

    @Override
    public JavaUtilDateSerializer withFormat(Boolean timestamp, DateFormat customFormat) {
        return new JavaUtilDateSerializer(timestamp, customFormat);
    }

    @Override
    protected long _timestamp(Date value) {
        return (value == null) ? 0L : value.getTime();
    }

    @Override
    public void serialize(Date value, JsonGenerator g, SerializerProvider provider)
        throws JacksonException
    {
        if (_asTimestamp(provider)) {
            g.writeNumber(_timestamp(value));
            return;
        }
        _serializeAsString(value, g, provider);
    }
}
