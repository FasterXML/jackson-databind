package tools.jackson.databind.ext.sql;

import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.*;
import tools.jackson.databind.annotation.JacksonStdImpl;
import tools.jackson.databind.jsonFormatVisitors.JsonFormatVisitorWrapper;
import tools.jackson.databind.jsonFormatVisitors.JsonValueFormat;
import tools.jackson.databind.ser.std.StdScalarSerializer;

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
