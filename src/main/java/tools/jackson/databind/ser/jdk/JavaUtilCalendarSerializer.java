package tools.jackson.databind.ser.jdk;

import java.text.DateFormat;
import java.util.Calendar;

import tools.jackson.core.*;
import tools.jackson.databind.SerializerProvider;
import tools.jackson.databind.annotation.JacksonStdImpl;

/**
 * Standard serializer for {@link java.util.Calendar}.
 * As with other time/date types, is configurable to produce timestamps
 * (standard Java 64-bit timestamp) or textual formats (usually ISO-8601).
 *<p>
 * NOTE: name was {@code CalendarSerializer} in Jackson 2.x
 */
@JacksonStdImpl
public class JavaUtilCalendarSerializer
    extends DateTimeSerializerBase<Calendar>
{
    public static final JavaUtilCalendarSerializer instance = new JavaUtilCalendarSerializer();

    public JavaUtilCalendarSerializer() { this(null, null); }

    public JavaUtilCalendarSerializer(Boolean useTimestamp, DateFormat customFormat) {
        super(Calendar.class, useTimestamp, customFormat);
    }

    @Override
    public JavaUtilCalendarSerializer withFormat(Boolean timestamp, DateFormat customFormat) {
        return new JavaUtilCalendarSerializer(timestamp, customFormat);
    }

    @Override
    protected long _timestamp(Calendar value) {
        return (value == null) ? 0L : value.getTimeInMillis();
    }

    @Override
    public void serialize(Calendar value, JsonGenerator g, SerializerProvider provider)
        throws JacksonException
    {
        if (_asTimestamp(provider)) {
            g.writeNumber(_timestamp(value));
            return;
        }
        _serializeAsString(value.getTime(), g, provider);
    }
}
