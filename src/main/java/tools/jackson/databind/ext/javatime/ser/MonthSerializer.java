package tools.jackson.databind.ext.javatime.ser;

import java.time.Month;
import java.time.format.DateTimeFormatter;

import com.fasterxml.jackson.annotation.JsonFormat;

import tools.jackson.core.*;
import tools.jackson.core.type.WritableTypeId;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.cfg.DateTimeFeature;
import tools.jackson.databind.jsontype.TypeSerializer;

/**
 * Serializer for Java 8 temporal {@link java.time.Month}s.
 */
public class MonthSerializer
        extends JSR310FormattedSerializerBase<Month>
{
    public static final MonthSerializer INSTANCE = new MonthSerializer();

    protected MonthSerializer() { this(null); }

    public MonthSerializer(DateTimeFormatter formatter) {
        super(Month.class, formatter);
    }

    private MonthSerializer(MonthSerializer base, DateTimeFormatter dtf, Boolean useTimestamp) {
        super(base, dtf, useTimestamp, null, null);
    }

    @Override
    protected MonthSerializer withFormat(DateTimeFormatter dtf,
                                            Boolean useTimestamp, JsonFormat.Shape shape) {
        return new MonthSerializer(this, dtf, useTimestamp);
    }

    @Override
    public void serialize(Month value, JsonGenerator g, SerializationContext ctxt)
            throws JacksonException
    {
        if (_useTimestampExplicitOnly(ctxt)) {
            g.writeStartArray();
            _serialize(g, value, ctxt);
            g.writeEndArray();
        } else {
            _serialize(g, value, ctxt);
        }
    }

    @Override
    public void serializeWithType(Month value, JsonGenerator g,
                                  SerializationContext ctxt, TypeSerializer typeSer)
            throws JacksonException
    {
        WritableTypeId typeIdDef = typeSer.writeTypePrefix(g, ctxt,
                typeSer.typeId(value, serializationShape(ctxt)));
        if ((typeIdDef != null)
                && typeIdDef.valueShape == JsonToken.START_ARRAY) {
            _serialize(g, value, ctxt);
        } else {
            _serialize(g, value, ctxt);
        }
        typeSer.writeTypeSuffix(g, ctxt, typeIdDef);
    }

    @Override
    protected JsonToken serializationShape(SerializationContext ctxt) {
        return _useTimestampExplicitOnly(ctxt) ? JsonToken.START_ARRAY : JsonToken.VALUE_STRING;
    }

    private void _serialize(JsonGenerator g, Month value, SerializationContext ctxt)
            throws JacksonException
    {
        if (_formatter != null) {
            g.writeString(_formatter.format(value));
            return;
        }
        if (ctxt.isEnabled(DateTimeFeature.ONE_BASED_MONTHS)) {
            g.writeNumber(value.getValue());
        } else {
            g.writeNumber(value.getValue() - 1);
        }
    }

}
