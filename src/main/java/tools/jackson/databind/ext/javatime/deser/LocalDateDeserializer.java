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
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import com.fasterxml.jackson.annotation.JsonFormat;

import tools.jackson.core.*;

import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.cfg.CoercionAction;
import tools.jackson.databind.cfg.CoercionInputShape;
import tools.jackson.databind.cfg.DateTimeFeature;

/**
 * Deserializer for Java 8 temporal {@link LocalDate}s.
 *
 * @author Nick Williams
 */
public class LocalDateDeserializer extends JSR310DateTimeDeserializerBase<LocalDate>
{
    private static final DateTimeFormatter DEFAULT_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;
    
    public static final LocalDateDeserializer INSTANCE = new LocalDateDeserializer();

    protected LocalDateDeserializer() {
        this(DEFAULT_FORMATTER);
    }

    public LocalDateDeserializer(DateTimeFormatter dtf) {
        super(LocalDate.class, dtf);
    }

    public LocalDateDeserializer(LocalDateDeserializer base, DateTimeFormatter dtf) {
        super(base, dtf);
    }

    protected LocalDateDeserializer(LocalDateDeserializer base, Boolean leniency) {
        super(base, leniency);
    }

    protected LocalDateDeserializer(LocalDateDeserializer base, JsonFormat.Shape shape) {
        super(base, shape);
    }

    @Override
    protected LocalDateDeserializer withDateFormat(DateTimeFormatter dtf) {
        return new LocalDateDeserializer(this, dtf);
    }

    @Override
    protected LocalDateDeserializer withLeniency(Boolean leniency) {
        return new LocalDateDeserializer(this, leniency);
    }

    @Override
    protected LocalDateDeserializer withShape(JsonFormat.Shape shape) {
        return new LocalDateDeserializer(this, shape);
    }

    @Override
    public LocalDate deserialize(JsonParser p, DeserializationContext ctxt)
        throws JacksonException
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
            if (ctxt.isEnabled(DeserializationFeature.UNWRAP_SINGLE_VALUE_ARRAYS)
                    && (t == JsonToken.VALUE_STRING || t==JsonToken.VALUE_EMBEDDED_OBJECT)) {
                final LocalDate parsed = deserialize(p, ctxt);
                if (p.nextToken() != JsonToken.END_ARRAY) {
                    handleMissingEndArrayForSingle(p, ctxt);
                }
                return parsed;            
            }
            if (t == JsonToken.VALUE_NUMBER_INT) {
                int year = p.getIntValue();
                int month = p.nextIntValue(-1);
                int day = p.nextIntValue(-1);
                
                if (p.nextToken() != JsonToken.END_ARRAY) {
                    throw ctxt.wrongTokenException(p, handledType(), JsonToken.END_ARRAY,
                            "Expected array to end");
                }
                return LocalDate.of(year, month, day);
            }
            ctxt.reportInputMismatch(handledType(),
                    "Unexpected token (%s) within Array, expected VALUE_NUMBER_INT",
                    t);
        } else if (p.hasToken(JsonToken.VALUE_EMBEDDED_OBJECT)) {
            return (LocalDate) p.getEmbeddedObject();
        }
        // 06-Jan-2018, tatu: Is this actually safe? Do users expect such coercion?
        else if (p.hasToken(JsonToken.VALUE_NUMBER_INT)) {
            CoercionAction act = ctxt.findCoercionAction(logicalType(), _valueClass,
                    CoercionInputShape.Integer);
            _checkCoercionFail(ctxt, act, handledType(), p.getLongValue(),
                    "Integer value (" + p.getLongValue() + ")");

            // issue 58 - also check for NUMBER_INT, which needs to be specified when serializing.
            if (_shape == JsonFormat.Shape.NUMBER_INT || isLenient()) {
                return LocalDate.ofEpochDay(p.getLongValue());
            }
            return _failForNotLenient(p, ctxt, JsonToken.VALUE_STRING);
        }
        return _handleUnexpectedToken(ctxt, p, "Expected array or string.");
    }

    protected LocalDate _fromString(JsonParser p, DeserializationContext ctxt,
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
        // as per [datatype-jsr310#37], only check for optional (and, incorrect...) time marker 'T'
        // if we are using default formatter
        final DateTimeFormatter format = _formatter;
        try {
            if (format == DEFAULT_FORMATTER) {
                // JavaScript by default includes time in JSON serialized Dates (UTC/ISO instant format).
                if (string.length() > 10 && string.charAt(10) == 'T') {
                    if (isLenient()) {
                        if (string.endsWith("Z")) {
                            if (ctxt.isEnabled(DateTimeFeature.USE_TIME_ZONE_FOR_LENIENT_DATE_PARSING)) {
                                return Instant.parse(string).atZone(ctxt.getTimeZone().toZoneId()).toLocalDate();
                            }
                            return LocalDate.parse(string.substring(0, string.length() - 1),
                                    DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                        }
                        return LocalDate.parse(string, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                    }
                    JavaType t = getValueType(ctxt);
                    return (LocalDate) ctxt.handleWeirdStringValue(t.getRawClass(),
                            string,
"Should not contain time component when 'strict' mode set for property or type (enable 'lenient' handling to allow)"
                            );
                }
            }
            return LocalDate.parse(string, format);
        } catch (DateTimeException e) {
            return _handleDateTimeFormatException(ctxt, e, format, string);
        }
    }
}
