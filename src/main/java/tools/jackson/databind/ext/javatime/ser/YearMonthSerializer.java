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

import java.time.YearMonth;
import java.time.format.DateTimeFormatter;

import com.fasterxml.jackson.annotation.JsonFormat;

import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonGenerator;
import tools.jackson.core.JsonToken;
import tools.jackson.core.type.WritableTypeId;

import tools.jackson.databind.JavaType;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.jsonFormatVisitors.JsonFormatVisitorWrapper;
import tools.jackson.databind.jsonFormatVisitors.JsonStringFormatVisitor;
import tools.jackson.databind.jsonFormatVisitors.JsonValueFormat;
import tools.jackson.databind.jsontype.TypeSerializer;

/**
 * Serializer for Java 8 temporal {@link YearMonth}s.
 *
 * @author Nick Williams
 * @since 2.2
 */
public class YearMonthSerializer extends JSR310FormattedSerializerBase<YearMonth>
{
    public static final YearMonthSerializer INSTANCE = new YearMonthSerializer();

    protected YearMonthSerializer() { // was private before 2.12
        this(null);
    }

    public YearMonthSerializer(DateTimeFormatter formatter) {
        super(YearMonth.class, formatter);
    }

    private YearMonthSerializer(YearMonthSerializer base, DateTimeFormatter dtf,
            Boolean useTimestamp) {
        super(base, dtf, useTimestamp, null, null);
    }

    @Override
    protected YearMonthSerializer withFormat(DateTimeFormatter dtf,
            Boolean useTimestamp, JsonFormat.Shape shape) {
        return new YearMonthSerializer(this, dtf, useTimestamp);
    }

    @Override
    public void serialize(YearMonth value, JsonGenerator g, SerializationContext ctxt)
        throws JacksonException
    {
        if (useTimestamp(ctxt)) {
            g.writeStartArray();
            _serializeAsArrayContents(value, g, ctxt);
            g.writeEndArray();
            return;
        }
        g.writeString((_formatter == null) ? value.toString() : value.format(_formatter));
    }

    @Override
    public void serializeWithType(YearMonth value, JsonGenerator g,
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

    protected void _serializeAsArrayContents(YearMonth value, JsonGenerator g,
            SerializationContext ctxt)
        throws JacksonException
    {
        g.writeNumber(value.getYear());
        g.writeNumber(value.getMonthValue());
    }

    @Override
    protected void _acceptTimestampVisitor(JsonFormatVisitorWrapper visitor, JavaType typeHint)
    {
        SerializationContext ctxt = visitor.getContext();
        boolean useTimestamp = (ctxt != null) && useTimestamp(ctxt);
        if (useTimestamp) {
            super._acceptTimestampVisitor(visitor, typeHint);
        } else {
            JsonStringFormatVisitor v2 = visitor.expectStringFormat(typeHint);
            if (v2 != null) {
                v2.format(JsonValueFormat.DATE_TIME);
            }
        }
    }

    @Override // since 2.9
    protected JsonToken serializationShape(SerializationContext ctxt) {
        return useTimestamp(ctxt) ? JsonToken.START_ARRAY : JsonToken.VALUE_STRING;
    }
}
