package tools.jackson.databind.ext.sql;

import java.text.DateFormat;

import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.*;
import tools.jackson.databind.annotation.JacksonStdImpl;
import tools.jackson.databind.ser.jdk.DateTimeSerializerBase;

/**
 * Compared to regular {@link java.util.Date} serialization, we do use String
 * representation here. Why? Basically to truncate of time part, since
 * that should not be used by plain SQL date.
 *<p>
 * NOTE: name was {@code SqlDateSerializer} in Jackson 2.x
 */
@JacksonStdImpl
public class JavaSqlDateSerializer
    extends DateTimeSerializerBase<java.sql.Date>
{
    public JavaSqlDateSerializer() {
        // 11-Oct-2016, tatu: As per [databind#219] fixed for 2.9; was passing `false` prior
        this(null, null);
    }

    protected JavaSqlDateSerializer(Boolean useTimestamp, DateFormat customFormat) {
        super(java.sql.Date.class, useTimestamp, customFormat);
    }

    @Override
    public JavaSqlDateSerializer withFormat(Boolean timestamp, DateFormat customFormat) {
    	return new JavaSqlDateSerializer(timestamp, customFormat);
    }

    @Override
    protected long _timestamp(java.sql.Date value) {
        return (value == null) ? 0L : value.getTime();
    }

    @Override
    public void serialize(java.sql.Date value, JsonGenerator g, SerializerProvider provider)
        throws JacksonException
    {
        if (_asTimestamp(provider)) {
            g.writeNumber(_timestamp(value));
            return;
        }
        // 03-Feb-2021, tatu: Jackson 3.x will simply treat same as `java.util.Date`,
        //    instead of calling "value.toString()" by default

//        if (_customFormat == null) {
//            // 11-Oct-2016, tatu: For backwards-compatibility purposes, we shall just use
//            //    the awful standard JDK serialization via `sqlDate.toString()`... this
//            //    is problematic in multiple ways (including using arbitrary timezone...)
//            g.writeString(value.toString());
//            return;
//        }
        _serializeAsString(value, g, provider);
    }
}
