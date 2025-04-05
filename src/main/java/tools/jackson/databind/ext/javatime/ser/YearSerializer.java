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

import java.time.Year;
import java.time.format.DateTimeFormatter;

import com.fasterxml.jackson.annotation.JsonFormat;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonGenerator;
import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;

import tools.jackson.databind.JavaType;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.jsonFormatVisitors.JsonFormatVisitorWrapper;
import tools.jackson.databind.jsonFormatVisitors.JsonIntegerFormatVisitor;

/**
 * Serializer for Java 8 temporal {@link Year}s.
 *
 * @author Nick Williams
 */
public class YearSerializer extends JSR310FormattedSerializerBase<Year>
{
    public static final YearSerializer INSTANCE = new YearSerializer();

    protected YearSerializer() {
        this(null);
    }

    public YearSerializer(DateTimeFormatter formatter) {
        super(Year.class, formatter);
    }

    protected YearSerializer(YearSerializer base, DateTimeFormatter dtf,
            Boolean useTimestamp) {
        super(base, dtf, useTimestamp, null, null);
    }

    @Override
    protected YearSerializer withFormat(DateTimeFormatter dtf,
            Boolean useTimestamp, JsonFormat.Shape shape) {
        return new YearSerializer(this, dtf, useTimestamp);
    }

    // Need to ensure Year still defaults to numeric ("timestamp") regardless
    // of general global setting (since that defaults to textual in Jackson 3.x)
    @Override
    protected boolean useTimestampFromGlobalDefaults(SerializationContext ctxt) {
        return true;
    }

    @Override
    public void serialize(Year year, JsonGenerator generator, SerializationContext ctxt)
        throws JacksonException
    {
        if (useTimestamp(ctxt)) {
            generator.writeNumber(year.getValue());
        } else {
            String str = (_formatter == null) ? year.toString() : year.format(_formatter);
            generator.writeString(str);
        }
    }

    // Override because we have String/Int, NOT String/Array
    @Override
    protected void _acceptTimestampVisitor(JsonFormatVisitorWrapper visitor, JavaType typeHint)
    {
        JsonIntegerFormatVisitor v2 = visitor.expectIntegerFormat(typeHint);
        if (v2 != null) {
            v2.numberType(JsonParser.NumberType.LONG);
        }
    }

    @Override // since 2.9
    protected JsonToken serializationShape(SerializationContext ctxt) {
        return useTimestamp(ctxt) ? JsonToken.VALUE_NUMBER_INT : JsonToken.VALUE_STRING;
    }
}
