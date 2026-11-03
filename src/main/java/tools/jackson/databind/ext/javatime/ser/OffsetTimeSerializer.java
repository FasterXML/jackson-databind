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

import java.time.OffsetTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;

import com.fasterxml.jackson.annotation.JsonFormat;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonGenerator;
import tools.jackson.core.JsonToken;
import tools.jackson.core.type.WritableTypeId;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.cfg.DateTimeFeature;
import tools.jackson.databind.jsontype.TypeSerializer;

/**
 * Serializer for Java 8 temporal {@link OffsetTime}s.
 *
 * @author Nick Williams
 */
public class OffsetTimeSerializer extends JSR310FormattedSerializerBase<OffsetTime>
{
    public static final OffsetTimeSerializer INSTANCE = new OffsetTimeSerializer();

    protected OffsetTimeSerializer() {
        super(OffsetTime.class);
    }

    protected OffsetTimeSerializer(OffsetTimeSerializer base, DateTimeFormatter dtf,
            Boolean useTimestamp) {
        this(base, dtf, useTimestamp, null);
    }

    protected OffsetTimeSerializer(OffsetTimeSerializer base, DateTimeFormatter dtf,
            Boolean useTimestamp, Boolean useNanoseconds) {
        super(base, dtf, useTimestamp, useNanoseconds, null);
    }

    @Override
    protected OffsetTimeSerializer withFormat(DateTimeFormatter dtf,
            Boolean useTimestamp, JsonFormat.Shape shape) {
        return new OffsetTimeSerializer(this, dtf, useTimestamp);
    }

    @Override
    public void serialize(OffsetTime time, JsonGenerator g, SerializationContext ctxt)
        throws JacksonException
    {
        // Apply millisecond truncation if enabled
        if (ctxt.isEnabled(DateTimeFeature.TRUNCATE_TO_MSECS_ON_WRITE)) {
            time = time.truncatedTo(ChronoUnit.MILLIS);
        }

        if (useTimestamp(ctxt)) {
            g.writeStartArray();
            _serializeAsArrayContents(time, g, ctxt);
            g.writeEndArray();
        } else {
            String str = (_formatter == null) ? time.toString() : time.format(_formatter);
            g.writeString(str);
        }
    }

    @Override
    public void serializeWithType(OffsetTime value, JsonGenerator g, SerializationContext ctxt,
            TypeSerializer typeSer)
        throws JacksonException
    {
        // Apply millisecond truncation if enabled
        if (ctxt.isEnabled(DateTimeFeature.TRUNCATE_TO_MSECS_ON_WRITE)) {
            value = value.truncatedTo(ChronoUnit.MILLIS);
        }

        WritableTypeId typeIdDef = typeSer.writeTypePrefix(g, ctxt,
                typeSer.typeId(value, serializationShape(ctxt)));
        // need to write out to avoid double-writing array markers
        if ((typeIdDef != null)
                && typeIdDef.valueShape == JsonToken.START_ARRAY) {
            _serializeAsArrayContents(value, g, ctxt);
        } else {
            String str = (_formatter == null) ? value.toString() : value.format(_formatter);
            g.writeString(str);
        }
        typeSer.writeTypeSuffix(g, ctxt, typeIdDef);
    }

    protected void _serializeAsArrayContents(OffsetTime value, JsonGenerator g,
            SerializationContext ctxt)
        throws JacksonException
    {
        g.writeNumber(value.getHour());
        g.writeNumber(value.getMinute());
        final int secs = value.getSecond();
        final int nanos = value.getNano();
        if ((secs > 0) || (nanos > 0)) {
            g.writeNumber(secs);
            if (nanos > 0) {
                if(useNanoseconds(ctxt)) {
                    g.writeNumber(nanos);
                } else {
                    g.writeNumber(value.get(ChronoField.MILLI_OF_SECOND));
                }
            }
        }
        g.writeString(value.getOffset().toString());
    }

    @Override
    protected JsonToken serializationShape(SerializationContext ctxt) {
        return useTimestamp(ctxt) ? JsonToken.START_ARRAY : JsonToken.VALUE_STRING;
    }

    @Override
    protected JSR310FormattedSerializerBase<?> withFeatures(Boolean writeZoneId, Boolean writeNanoseconds) {
        return new OffsetTimeSerializer(this, _formatter, _useTimestamp, writeNanoseconds);
    }
}
