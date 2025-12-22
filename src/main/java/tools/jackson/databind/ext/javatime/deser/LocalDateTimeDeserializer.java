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
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonFormat;

import tools.jackson.core.*;
import tools.jackson.databind.*;
import tools.jackson.databind.cfg.DateTimeFeature;

/**
 * Deserializer for Java 8 temporal {@link LocalDateTime}s.
 *
 * @author Nick Williams
 */
public class LocalDateTimeDeserializer
    extends JSR310DateTimeDeserializerBase<LocalDateTime>
{
    private static final DateTimeFormatter DEFAULT_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    public static final LocalDateTimeDeserializer INSTANCE = new LocalDateTimeDeserializer();

    /**
     * Flag for <code>JsonFormat.Feature.READ_DATE_TIMESTAMPS_AS_NANOSECONDS</code>
     */
    protected final Boolean _readTimestampsAsNanosOverride;

    protected LocalDateTimeDeserializer() {
        this(DEFAULT_FORMATTER);
    }

    public LocalDateTimeDeserializer(DateTimeFormatter formatter) {
        super(LocalDateTime.class, formatter);
        _readTimestampsAsNanosOverride = null;
    }

    protected LocalDateTimeDeserializer(LocalDateTimeDeserializer base, Boolean leniency) {
        super(base, leniency);
        _readTimestampsAsNanosOverride = base._readTimestampsAsNanosOverride;
    }

    protected LocalDateTimeDeserializer(LocalDateTimeDeserializer base,
        Boolean leniency,
        DateTimeFormatter formatter,
        JsonFormat.Shape shape,
        Boolean readTimestampsAsNanosOverride) {
        super(base, leniency, formatter, shape);
        _readTimestampsAsNanosOverride = readTimestampsAsNanosOverride;
    }

    @Override
    protected LocalDateTimeDeserializer withDateFormat(DateTimeFormatter dtf) {
        return new LocalDateTimeDeserializer(this, _isLenient, dtf, _shape, _readTimestampsAsNanosOverride);
    }

    @Override
    protected LocalDateTimeDeserializer withLeniency(Boolean leniency) {
        return new LocalDateTimeDeserializer(this, leniency);
    }

    @Override
    protected JSR310DateTimeDeserializerBase<?> _withFormatOverrides(DeserializationContext ctxt,
        BeanProperty property, JsonFormat.Value formatOverrides)
    {
        LocalDateTimeDeserializer deser = (LocalDateTimeDeserializer)
            super._withFormatOverrides(ctxt, property, formatOverrides);
        Boolean readTimestampsAsNanosOverride = formatOverrides.getFeature(
            JsonFormat.Feature.READ_DATE_TIMESTAMPS_AS_NANOSECONDS);
        if (!Objects.equals(readTimestampsAsNanosOverride, deser._readTimestampsAsNanosOverride)) {
            return new LocalDateTimeDeserializer(deser, deser._isLenient, deser._formatter,
                deser._shape, readTimestampsAsNanosOverride);
        }
        return deser;
    }

    @Override
    public LocalDateTime deserialize(JsonParser p, DeserializationContext ctxt)
        throws JacksonException
    {
        LocalDateTime result;

        if (p.hasTokenId(JsonTokenId.ID_STRING)) {
            result = _fromString(p, ctxt, p.getString());
        }
        // 30-Sep-2020, tatu: New! "Scalar from Object" (mostly for XML)
        else if (p.isExpectedStartObjectToken()) {
            final String str = ctxt.extractScalarFromObject(p, this, handledType());
            // 17-May-2025, tatu: [databind#4656] need to check for `null`
            if (str != null) {
                result = _fromString(p, ctxt, str);
            } else {
                result = _handleUnexpectedToken(ctxt, p, "Expected array or string");
            }
        } else if (p.isExpectedStartArrayToken()) {
            JsonToken t = p.nextToken();
            if (t == JsonToken.END_ARRAY) {
                // 22-Dec-2025, tatu: Has been like this but seems inconsistent?
                return null;
            }
            if ((t == JsonToken.VALUE_STRING || t == JsonToken.VALUE_EMBEDDED_OBJECT)
                    && ctxt.isEnabled(DeserializationFeature.UNWRAP_SINGLE_VALUE_ARRAYS)) {
                final LocalDateTime parsed = deserialize(p, ctxt);
                if (p.nextToken() != JsonToken.END_ARRAY) {
                    handleMissingEndArrayForSingle(p, ctxt);
                }
                // Since we got it via `deserialize()`, possible truncation already applied
                return parsed;
            }
            if (t == JsonToken.VALUE_NUMBER_INT) {
                int year = p.getIntValue();
                int month = p.nextIntValue(-1);
                int day = p.nextIntValue(-1);
                int hour = p.nextIntValue(-1);
                int minute = p.nextIntValue(-1);

                t = p.nextToken();
                if (t == JsonToken.END_ARRAY) {
                    result = LocalDateTime.of(year, month, day, hour, minute);
                } else {
                    int second = p.getIntValue();
                    t = p.nextToken();
                    if (t == JsonToken.END_ARRAY) {
                        result = LocalDateTime.of(year, month, day, hour, minute, second);
                    } else {
                        int partialSecond = p.getIntValue();
                        if (partialSecond < 1_000 && !shouldReadTimestampsAsNanoseconds(ctxt))
                            partialSecond *= 1_000_000; // value is milliseconds, convert it to nanoseconds
                        if (p.nextToken() != JsonToken.END_ARRAY) {
                            throw ctxt.wrongTokenException(p, handledType(), JsonToken.END_ARRAY,
                                    "Expected array to end");
                        }
                        result = LocalDateTime.of(year, month, day, hour, minute, second, partialSecond);
                    }
                }
            } else {
                result = ctxt.reportInputMismatch(handledType(),
                        "Unexpected token (%s) within Array, expected VALUE_NUMBER_INT",
                        t);
            }
        } else if (p.hasToken(JsonToken.VALUE_EMBEDDED_OBJECT)) {
            result = (LocalDateTime) p.getEmbeddedObject();
        } else if (p.hasToken(JsonToken.VALUE_NUMBER_INT)) {
            _throwNoNumericTimestampNeedTimeZone(p, ctxt);
            return null; // Unreachable but satisfies compiler
        } else {
            result = _handleUnexpectedToken(ctxt, p, "Expected array or string");
        }

        // Apply millisecond truncation if enabled
        if (result != null && ctxt.isEnabled(DateTimeFeature.TRUNCATE_TO_MSECS_ON_READ)) {
            result = result.truncatedTo(ChronoUnit.MILLIS);
        }

        return result;
    }

    protected boolean shouldReadTimestampsAsNanoseconds(DeserializationContext context) {
        return (_readTimestampsAsNanosOverride != null) ? _readTimestampsAsNanosOverride :
            context.isEnabled(DateTimeFeature.READ_DATE_TIMESTAMPS_AS_NANOSECONDS);
    }

    protected LocalDateTime _fromString(JsonParser p, DeserializationContext ctxt,
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
        final DateTimeFormatter format = _formatter;
        try {
            // 21-Oct-2020, tatu: Changed as per [modules-base#94] for 2.12,
            //    had bad timezone handle change from [modules-base#56]
            if (_formatter == DEFAULT_FORMATTER) {
                // ... only allow iff lenient mode enabled since
                // JavaScript by default includes time and zone in JSON serialized Dates (UTC/ISO instant format).
                if (string.length() > 10 && string.charAt(10) == 'T') {
                   if (string.endsWith("Z")) {
                       if (isLenient()) {
                           if (ctxt.isEnabled(DateTimeFeature.USE_TIME_ZONE_FOR_LENIENT_DATE_PARSING)) {
                               return Instant.parse(string).atZone(ctxt.getTimeZone().toZoneId()).toLocalDateTime();
                           }
                           return LocalDateTime.parse(string.substring(0, string.length()-1),
                                   _formatter);
                       }
                       JavaType t = getValueType(ctxt);
                       return (LocalDateTime) ctxt.handleWeirdStringValue(t.getRawClass(),
                               string,
"Should not contain offset when 'strict' mode set for property or type (enable 'lenient' handling to allow)"
                               );
                   }
                }
            }
           return LocalDateTime.parse(string, _formatter);
        } catch (DateTimeException e) {
            return _handleDateTimeFormatException(ctxt, e, format, string);
        }
    }
}
