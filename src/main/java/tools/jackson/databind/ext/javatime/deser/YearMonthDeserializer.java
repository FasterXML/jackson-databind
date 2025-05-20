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

package tools.jackson.databind.ext.javatime.deser;

import java.time.DateTimeException;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;

import com.fasterxml.jackson.annotation.JsonFormat;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.core.JsonToken;

/**
 * Deserializer for Java 8 temporal {@link YearMonth}s.
 *
 * @author Nick Williams
 * @since 2.2.0
 */
public class YearMonthDeserializer extends JSR310DateTimeDeserializerBase<YearMonth>
{
    public static final YearMonthDeserializer INSTANCE = new YearMonthDeserializer();

    /**
     * NOTE: only {@code public} so that use via annotations (see [modules-java8#202])
     * is possible
     */
    public YearMonthDeserializer() // public since 2.12
    {
        this(DateTimeFormatter.ofPattern("u-MM"));
    }
    
    public YearMonthDeserializer(DateTimeFormatter formatter)
    {
        super(YearMonth.class, formatter);
    }

    /**
     * Since 2.11
     */
    protected YearMonthDeserializer(YearMonthDeserializer base, Boolean leniency) {
        super(base, leniency);
    }

    /**
     * Since 2.16
     */
    public YearMonthDeserializer(YearMonthDeserializer base,
            Boolean leniency,
            DateTimeFormatter formatter,
            JsonFormat.Shape shape) {
        super(base, leniency, formatter, shape);
    }

    @Override
    protected YearMonthDeserializer withDateFormat(DateTimeFormatter dtf)  {
        return new YearMonthDeserializer(this, _isLenient, dtf, _shape);
    }

    @Override
    protected YearMonthDeserializer withLeniency(Boolean leniency) {
        return new YearMonthDeserializer(this, leniency);
    }

    @Override
    public YearMonth deserialize(JsonParser p, DeserializationContext ctxt) throws JacksonException
    {
        if (p.hasToken(JsonToken.VALUE_STRING)) {
            return _fromString(p, ctxt, p.getString());
        }
        // 30-Sep-2020, tatu: New! "Scalar from Object" (mostly for XML)
        if (p.isExpectedStartObjectToken()) {
            final String str = ctxt.extractScalarFromObject(p, this, handledType());
            // 17-May-2025, tatu: [databind#4656] need to check for `null`
            if (str != null) {
                return _fromString(p, ctxt, str);
            }
            // fall through
        } else if (p.isExpectedStartArrayToken()) {
            JsonToken t = p.nextToken();
            if (t == JsonToken.END_ARRAY) {
                return null;
            }
            if ((t == JsonToken.VALUE_STRING || t == JsonToken.VALUE_EMBEDDED_OBJECT)
                    && ctxt.isEnabled(DeserializationFeature.UNWRAP_SINGLE_VALUE_ARRAYS)) {
                final YearMonth parsed = deserialize(p, ctxt);
                if (p.nextToken() != JsonToken.END_ARRAY) {
                    handleMissingEndArrayForSingle(p, ctxt);
                }
                return parsed;            
            }
            if (t != JsonToken.VALUE_NUMBER_INT) {
                _reportWrongToken(ctxt, JsonToken.VALUE_NUMBER_INT, "years");
            }
            int year = p.getIntValue();
            int month = p.nextIntValue(-1);
            if (month == -1) {
                if (!p.hasToken(JsonToken.VALUE_NUMBER_INT)) {
                    _reportWrongToken(ctxt, JsonToken.VALUE_NUMBER_INT, "months");
                }
                month = p.getIntValue();
            }
            if (p.nextToken() != JsonToken.END_ARRAY) {
                throw ctxt.wrongTokenException(p, handledType(), JsonToken.END_ARRAY,
                        "Expected array to end");
            }
            return YearMonth.of(year, month);
        } else if (p.hasToken(JsonToken.VALUE_EMBEDDED_OBJECT)) {
            return (YearMonth) p.getEmbeddedObject();
        }
        return _handleUnexpectedToken(ctxt, p,
                JsonToken.VALUE_STRING, JsonToken.START_ARRAY);
    }

    protected YearMonth _fromString(JsonParser p, DeserializationContext ctxt,
            String string0)
        throws JacksonException
    {
        String string = string0.trim();
        if (string.length() == 0) {
            // 22-Oct-2020, tatu: not sure if we should pass original (to distinguish
            //   b/w empty and blank); for now don't which will allow blanks to be
            //   handled like "regular" empty (same as pre-2.12)
            return _fromEmptyString(p, ctxt, string);
        }
        try {
            return YearMonth.parse(string, _formatter);
        } catch (DateTimeException e) {
            return _handleDateTimeFormatException(ctxt, e, _formatter, string);
        }
    }
}
