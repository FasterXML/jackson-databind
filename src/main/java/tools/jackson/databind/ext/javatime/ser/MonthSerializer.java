package tools.jackson.databind.ext.javatime.ser;

import java.time.Month;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashSet;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonFormat;

import tools.jackson.core.*;
import tools.jackson.core.type.WritableTypeId;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.cfg.DateTimeFeature;
import tools.jackson.databind.jsonFormatVisitors.JsonFormatVisitorWrapper;
import tools.jackson.databind.jsonFormatVisitors.JsonStringFormatVisitor;
import tools.jackson.databind.jsontype.TypeSerializer;

/**
 * Serializer for Java 8 temporal {@link java.time.Month}s.
 *<p>
 * By default values are serialized as JSON Numbers: one- or zero-based
 * index depending on {@link DateTimeFeature#ONE_BASED_MONTHS}.
 * With {@code @JsonFormat(pattern = ...)} values are serialized as
 * JSON Strings formatted using the pattern (and locale, if any); and with
 * {@code @JsonFormat(shape = JsonFormat.Shape.STRING)} (without pattern)
 * as JSON Strings containing Enum name (like {@code "JANUARY"}).
 */
public class MonthSerializer
        extends JSR310FormattedSerializerBase<Month>
{
    public static final MonthSerializer INSTANCE = new MonthSerializer();

    protected MonthSerializer() { this(null); }

    public MonthSerializer(DateTimeFormatter formatter) {
        super(Month.class, formatter);
    }

    private MonthSerializer(MonthSerializer base, DateTimeFormatter dtf, Boolean useTimestamp,
            JsonFormat.Shape shape) {
        super(base, dtf, useTimestamp, null, shape);
    }

    @Override
    protected MonthSerializer withFormat(DateTimeFormatter dtf,
                                            Boolean useTimestamp, JsonFormat.Shape shape) {
        return new MonthSerializer(this, dtf, useTimestamp, shape);
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
        _serialize(g, value, ctxt);
        typeSer.writeTypeSuffix(g, ctxt, typeIdDef);
    }

    @Override
    protected JsonToken serializationShape(SerializationContext ctxt) {
        return _useTimestampExplicitOnly(ctxt) ? JsonToken.START_ARRAY : JsonToken.VALUE_STRING;
    }

    @Override
    public void acceptJsonFormatVisitor(JsonFormatVisitorWrapper visitor, JavaType typeHint)
    {
        // [databind#6233]: must match what `_serialize()` writes
        if (_useTimestampExplicitOnly(visitor.getContext())) {
            _acceptTimestampVisitor(visitor, typeHint);
        } else if (_formatter != null) {
            visitStringFormat(visitor, typeHint);
        } else if (Boolean.FALSE.equals(_useTimestamp)) {
            JsonStringFormatVisitor v2 = visitor.expectStringFormat(typeHint);
            if (v2 != null) {
                Set<String> names = new LinkedHashSet<>();
                for (Month m : Month.values()) {
                    names.add(m.name());
                }
                v2.enumTypes(names);
            }
        } else {
            visitIntFormat(visitor, typeHint, JsonParser.NumberType.INT);
        }
    }

    private void _serialize(JsonGenerator g, Month value, SerializationContext ctxt)
            throws JacksonException
    {
        if (_formatter != null) {
            g.writeString(_formatter.format(value));
            return;
        }
        // [databind#6233]: explicit `Shape.STRING` (without pattern) means Enum name,
        // which is locale-independent and readable by `MonthDeserializer`
        if (Boolean.FALSE.equals(_useTimestamp)) {
            g.writeString(value.name());
            return;
        }
        if (ctxt.isEnabled(DateTimeFeature.ONE_BASED_MONTHS)) {
            g.writeNumber(value.getValue());
        } else {
            g.writeNumber(value.getValue() - 1);
        }
    }

}
