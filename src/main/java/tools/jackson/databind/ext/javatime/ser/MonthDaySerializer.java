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

import java.time.MonthDay;
import java.time.format.DateTimeFormatter;

import com.fasterxml.jackson.annotation.JsonFormat;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonGenerator;
import tools.jackson.core.JsonToken;
import tools.jackson.core.type.WritableTypeId;

import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.jsontype.TypeSerializer;

/**
 * Serializer for Java 8 temporal {@link MonthDay}s.
 *<p>
 * NOTE: unlike many other date/time type serializers, this serializer will only
 * use Array notation if explicitly instructed to do so with <code>JsonFormat</code>
 * (either directly or through per-type defaults) and NOT with global defaults.
 *
 * @since 2.7.1
 */
public class MonthDaySerializer extends JSR310FormattedSerializerBase<MonthDay>
{
    public static final MonthDaySerializer INSTANCE = new MonthDaySerializer();

    protected MonthDaySerializer() { // was private before 2.12
        this(null);
    }

    public MonthDaySerializer(DateTimeFormatter formatter) {
        super(MonthDay.class, formatter);
    }

    private MonthDaySerializer(MonthDaySerializer base, DateTimeFormatter dtf, Boolean useTimestamp) {
        super(base, dtf, useTimestamp, null, null);
    }

    @Override
    protected MonthDaySerializer withFormat(DateTimeFormatter dtf,
            Boolean useTimestamp, JsonFormat.Shape shape) {
        return new MonthDaySerializer(this, dtf, useTimestamp);
    }

    @Override
    public void serialize(MonthDay value, JsonGenerator g, SerializationContext ctxt)
        throws JacksonException
    {
        if (_useTimestampExplicitOnly(ctxt)) {
            g.writeStartArray();
            _serializeAsArrayContents(value, g, ctxt);
            g.writeEndArray();
        } else {
            g.writeString((_formatter == null) ? value.toString() : value.format(_formatter));
        }
    }

    @Override
    public void serializeWithType(MonthDay value, JsonGenerator g,
            SerializationContext ctxt, TypeSerializer typeSer)
        throws JacksonException
    {
        WritableTypeId typeIdDef = typeSer.writeTypePrefix(g, ctxt,
                typeSer.typeId(value, serializationShape(ctxt)));
        // need to write out to avoid double-writing array markers
        if ((typeIdDef != null)
                && typeIdDef.valueShape == JsonToken.START_ARRAY) {
            _serializeAsArrayContents(value, g, ctxt);
        } else {
            g.writeString((_formatter == null) ? value.toString() : value.format(_formatter));
        }
        typeSer.writeTypeSuffix(g, ctxt, typeIdDef);
    }
    
    protected void _serializeAsArrayContents(MonthDay value, JsonGenerator g,
            SerializationContext ctxt)
        throws JacksonException
    {
        g.writeNumber(value.getMonthValue());
        g.writeNumber(value.getDayOfMonth());
    }

    @Override
    protected JsonToken serializationShape(SerializationContext ctxt) {
        return _useTimestampExplicitOnly(ctxt) ? JsonToken.START_ARRAY : JsonToken.VALUE_STRING;
    }
}
