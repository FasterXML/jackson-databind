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
import java.time.Year;
import java.time.format.DateTimeFormatter;

import com.fasterxml.jackson.annotation.JsonFormat;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.core.StreamReadCapability;
import tools.jackson.core.io.NumberInput;
import tools.jackson.databind.DeserializationContext;

/**
 * Deserializer for Java 8 temporal {@link Year}s.
 *
 * @author Nick Williams
 */
public class YearDeserializer extends JSR310DateTimeDeserializerBase<Year>
{
    public static final YearDeserializer INSTANCE = new YearDeserializer();

    /**
     * NOTE: only {@code public} so that use via annotations (see [modules-java8#202])
     * is possible
     */
    public YearDeserializer() { // public since 2.12
        this(null);
    }

    public YearDeserializer(DateTimeFormatter formatter) {
        super(Year.class, formatter);
    }

    /**
     * Since 2.12
     */
    protected YearDeserializer(YearDeserializer base, Boolean leniency) {
        super(base, leniency);
    }

    /**
     * Since 2.16
     */
    public YearDeserializer(YearDeserializer base,
            Boolean leniency,
            DateTimeFormatter formatter,
            JsonFormat.Shape shape) {
        super(base, leniency, formatter, shape);
    }

    @Override
    protected YearDeserializer withDateFormat(DateTimeFormatter dtf) {
        return new YearDeserializer(this, _isLenient, dtf, _shape);
    }

    @Override
    protected YearDeserializer withLeniency(Boolean leniency) {
        return new YearDeserializer(this, leniency);
    }

    @Override
    public Year deserialize(JsonParser p, DeserializationContext ctxt) throws JacksonException
    {
        JsonToken t = p.currentToken();
        if (t == JsonToken.VALUE_STRING) {
            return _fromString(p, ctxt, p.getString());
        }
        if (t == JsonToken.VALUE_NUMBER_INT) {
            return _fromNumber(ctxt, p.getIntValue());
        }
        if (t == JsonToken.VALUE_EMBEDDED_OBJECT) {
            return (Year) p.getEmbeddedObject();
        }
        // 30-Sep-2020, tatu: New! "Scalar from Object" (mostly for XML)
        if (t == JsonToken.START_OBJECT) {
            final String str = ctxt.extractScalarFromObject(p, this, handledType());
            // 17-May-2025, tatu: [databind#4656] need to check for `null`
            if (str != null) {
                return _fromString(p, ctxt, str);
            }
            // fall through
        } else if (p.hasToken(JsonToken.START_ARRAY)){
            return _deserializeFromArray(p, ctxt);
        }
        return _handleUnexpectedToken(ctxt, p, JsonToken.VALUE_STRING, JsonToken.VALUE_NUMBER_INT);
    }

    protected Year _fromString(JsonParser p, DeserializationContext ctxt,
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
        // 30-Sep-2020: Should allow use of "Timestamp as String" for XML/CSV
        if (ctxt.isEnabled(StreamReadCapability.UNTYPED_SCALARS)
                && _isValidTimestampString(string)) {
            return _fromNumber(ctxt, NumberInput.parseInt(string));
        }
        try {
            if (_formatter == null) {
                return Year.parse(string);
            }
            return Year.parse(string, _formatter);
        } catch (DateTimeException e) {
            return _handleDateTimeFormatException(ctxt, e, _formatter, string);
        }
    }

    protected Year _fromNumber(DeserializationContext ctxt, int value) {
        return Year.of(value);
    }
}
