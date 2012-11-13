package com.fasterxml.jackson.databind.ser.std;

import java.io.IOException;
import java.text.DateFormat;
import java.util.Calendar;

import com.fasterxml.jackson.core.*;

import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JacksonStdImpl;

/**
 * Standard serializer for {@link java.util.Calendar}.
 * As with other time/date types, is configurable to produce timestamps
 * (standard Java 64-bit timestamp) or textual formats (usually ISO-8601).
 */
@JacksonStdImpl
public class CalendarSerializer
    extends DateTimeSerializerBase<Calendar>
{
    public static final CalendarSerializer instance = new CalendarSerializer();

    public CalendarSerializer() { this(false, null); }

    public CalendarSerializer(boolean useTimestamp, DateFormat customFormat) {
        super(Calendar.class, useTimestamp, customFormat);
    }

    @Override
    public CalendarSerializer withFormat(boolean timestamp, DateFormat customFormat)
    {
        if (timestamp) {
            return new CalendarSerializer(true, null);
        }
        return new CalendarSerializer(false, customFormat);
    }

    @Override
    protected long _timestamp(Calendar value) {
        return (value == null) ? 0L : value.getTimeInMillis();
    }

    @Override
    public void serialize(Calendar value, JsonGenerator jgen, SerializerProvider provider)
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
            provider.defaultSerializeDateValue(value.getTime(), jgen);
        }
    }

}
