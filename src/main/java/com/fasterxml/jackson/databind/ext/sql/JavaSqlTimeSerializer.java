package com.fasterxml.jackson.databind.ext.sql;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonGenerator;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.annotation.JacksonStdImpl;
import com.fasterxml.jackson.databind.jsonFormatVisitors.JsonFormatVisitorWrapper;
import com.fasterxml.jackson.databind.jsonFormatVisitors.JsonValueFormat;
import com.fasterxml.jackson.databind.ser.std.StdScalarSerializer;

/**
 *<p>
 * NOTE: name was {@code SqlTimeSerializer} in Jackson 2.x
 */
@JacksonStdImpl
public class JavaSqlTimeSerializer
    extends StdScalarSerializer<java.sql.Time>
{
    public JavaSqlTimeSerializer() { super(java.sql.Time.class); }

    @Override
    public void serialize(java.sql.Time value, JsonGenerator g, SerializerProvider provider)
        throws JacksonException
    {
        g.writeString(value.toString());
    }

    @Override
    public void acceptJsonFormatVisitor(JsonFormatVisitorWrapper visitor, JavaType typeHint)
    {
        visitStringFormat(visitor, typeHint, JsonValueFormat.DATE_TIME);
    }
}