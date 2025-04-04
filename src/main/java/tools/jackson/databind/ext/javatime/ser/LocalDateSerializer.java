/*
 * Copyright 2013 FasterXML.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License. You may obtain
 * a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the license for the specific language governing permissions and
 * limitations under the license.
 */

package tools.jackson.databind.ext.javatime.ser;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import com.fasterxml.jackson.annotation.JsonFormat;

import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonGenerator;
import tools.jackson.core.JsonToken;
import tools.jackson.core.type.WritableTypeId;
import tools.jackson.databind.*;
import tools.jackson.databind.jsonFormatVisitors.JsonFormatVisitorWrapper;
import tools.jackson.databind.jsonFormatVisitors.JsonStringFormatVisitor;
import tools.jackson.databind.jsonFormatVisitors.JsonValueFormat;
import tools.jackson.databind.jsontype.TypeSerializer;

/**
 * Serializer for Java 8 temporal {@link LocalDate}s.
 *
 * @author Nick Williams
 */
public class LocalDateSerializer extends JSR310FormattedSerializerBase<LocalDate>
{
    public static final LocalDateSerializer INSTANCE = new LocalDateSerializer();

    protected LocalDateSerializer() {
        super(LocalDate.class);
    }

    protected LocalDateSerializer(LocalDateSerializer base, DateTimeFormatter dtf,
            Boolean useTimestamp, JsonFormat.Shape shape) {
        super(base, dtf, useTimestamp, null, shape);
    }

    public LocalDateSerializer(DateTimeFormatter formatter) {
        super(LocalDate.class, formatter);
    }

    @Override
    protected LocalDateSerializer withFormat(DateTimeFormatter dtf,
            Boolean useTimestamp, JsonFormat.Shape shape) {
        return new LocalDateSerializer(this, dtf, useTimestamp, shape);
    }

    @Override
    public void serialize(LocalDate date, JsonGenerator g, SerializationContext ctxt)
        throws JacksonException
    {
        if (useTimestamp(ctxt)) {
            if (_shape == JsonFormat.Shape.NUMBER_INT) {
                g.writeNumber(date.toEpochDay());
            } else {
                g.writeStartArray();
                _serializeAsArrayContents(date, g, ctxt);
                g.writeEndArray();
            }
        } else {
            g.writeString((_formatter == null) ? date.toString() : date.format(_formatter));
        }
    }

    @Override
    public void serializeWithType(LocalDate value, JsonGenerator g,
            SerializationContext ctxt, TypeSerializer typeSer)
        throws JacksonException
    {
        WritableTypeId typeIdDef = typeSer.writeTypePrefix(g, ctxt,
                typeSer.typeId(value, serializationShape(ctxt)));
        // need to write out to avoid double-writing array markers
        JsonToken shape = (typeIdDef == null) ? null : typeIdDef.valueShape;
        if (shape == JsonToken.START_ARRAY) {
            _serializeAsArrayContents(value, g, ctxt);
        } else if (shape == JsonToken.VALUE_NUMBER_INT) {
            g.writeNumber(value.toEpochDay());
        } else {
            g.writeString((_formatter == null) ? value.toString() : value.format(_formatter));
        }
        typeSer.writeTypeSuffix(g, ctxt, typeIdDef);
    }

    protected void _serializeAsArrayContents(LocalDate value, JsonGenerator g,
            SerializationContext ctxt)
        throws JacksonException
    {
        g.writeNumber(value.getYear());
        g.writeNumber(value.getMonthValue());
        g.writeNumber(value.getDayOfMonth());
    }

    @Override
    public void acceptJsonFormatVisitor(JsonFormatVisitorWrapper visitor, JavaType typeHint)
    {
        SerializationContext ctxt = visitor.getContext();
        boolean useTimestamp = (ctxt != null) && useTimestamp(ctxt);
        if (useTimestamp) {
            _acceptTimestampVisitor(visitor, typeHint);
        } else {
            JsonStringFormatVisitor v2 = visitor.expectStringFormat(typeHint);
            if (v2 != null) {
                v2.format(JsonValueFormat.DATE);
            }
        }
    }

    @Override // since 2.9
    protected JsonToken serializationShape(SerializationContext ctxt) {
        if (useTimestamp(ctxt)) {
            if (_shape == JsonFormat.Shape.NUMBER_INT) {
                return JsonToken.VALUE_NUMBER_INT;
            }
            return JsonToken.START_ARRAY;
        }
        return JsonToken.VALUE_STRING;
    }
}
