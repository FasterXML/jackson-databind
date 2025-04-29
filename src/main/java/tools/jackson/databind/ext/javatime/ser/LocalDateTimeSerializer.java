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

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoField;

import com.fasterxml.jackson.annotation.JsonFormat;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonGenerator;
import tools.jackson.core.JsonToken;
import tools.jackson.core.type.WritableTypeId;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.jsontype.TypeSerializer;

/**
 * Serializer for Java 8 temporal {@link LocalDateTime}s.
 *
 * @author Nick Williams
 */
public class LocalDateTimeSerializer extends JSR310FormattedSerializerBase<LocalDateTime>
{
    public static final LocalDateTimeSerializer INSTANCE = new LocalDateTimeSerializer();
    
    protected LocalDateTimeSerializer() {
        this(null);
    }

    public LocalDateTimeSerializer(DateTimeFormatter f) {
        super(LocalDateTime.class, f);
    }

    protected LocalDateTimeSerializer(LocalDateTimeSerializer base, DateTimeFormatter dtf,
            Boolean useTimestamp, Boolean useNanoseconds) {
        super(base, dtf, useTimestamp, useNanoseconds, null);
    }

    @Override
    protected JSR310FormattedSerializerBase<LocalDateTime> withFormat(DateTimeFormatter f,
            Boolean useTimestamp, JsonFormat.Shape shape) {
        return new LocalDateTimeSerializer(this, f, useTimestamp, _useNanoseconds);
    }

    protected DateTimeFormatter _defaultFormatter() {
        return DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    }

    @Override
    public void serialize(LocalDateTime value, JsonGenerator g, SerializationContext ctxt)
        throws JacksonException
    {
        if (useTimestamp(ctxt)) {
            g.writeStartArray();
            _serializeAsArrayContents(value, g, ctxt);
            g.writeEndArray();
        } else {
            DateTimeFormatter dtf = _formatter;
            if (dtf == null) {
                dtf = _defaultFormatter();
            }
            g.writeString(value.format(dtf));
        }
    }

    @Override
    public void serializeWithType(LocalDateTime value, JsonGenerator g, SerializationContext ctxt,
            TypeSerializer typeSer)
        throws JacksonException
    {
        WritableTypeId typeIdDef = typeSer.writeTypePrefix(g, ctxt,
                typeSer.typeId(value, serializationShape(ctxt)));
        // need to write out to avoid double-writing array markers
        if ((typeIdDef != null)
                && typeIdDef.valueShape == JsonToken.START_ARRAY) {
            _serializeAsArrayContents(value, g, ctxt);
        } else {
            DateTimeFormatter dtf = _formatter;
            if (dtf == null) {
                dtf = _defaultFormatter();
            }
            g.writeString(value.format(dtf));
        }
        typeSer.writeTypeSuffix(g, ctxt, typeIdDef);
    }

    private final void _serializeAsArrayContents(LocalDateTime value, JsonGenerator g,
            SerializationContext ctxt)
        throws JacksonException
    {
        g.writeNumber(value.getYear());
        g.writeNumber(value.getMonthValue());
        g.writeNumber(value.getDayOfMonth());
        g.writeNumber(value.getHour());
        g.writeNumber(value.getMinute());
        final int secs = value.getSecond();
        final int nanos = value.getNano();
        if ((secs > 0) || (nanos > 0)) {
            g.writeNumber(secs);
            if (nanos > 0) {
                if (useNanoseconds(ctxt)) {
                    g.writeNumber(nanos);
                } else {
                    g.writeNumber(value.get(ChronoField.MILLI_OF_SECOND));
                }
            }
        }
    }

    @Override
    protected JsonToken serializationShape(SerializationContext ctxt) {
        return useTimestamp(ctxt) ? JsonToken.START_ARRAY : JsonToken.VALUE_STRING;
    }

    @Override
    protected JSR310FormattedSerializerBase<?> withFeatures(Boolean writeZoneId, Boolean writeNanoseconds) {
        return new LocalDateTimeSerializer(this, _formatter, _useTimestamp, writeNanoseconds);
    }
}
