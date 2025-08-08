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
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonFormat;

import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.databind.BeanProperty;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.cfg.DateTimeFeature;

/**
 * Deserializer for Java 8 temporal {@link LocalTime}s.
 *
 * @author Nick Williams
 */
public class LocalTimeDeserializer extends JSR310DateTimeDeserializerBase<LocalTime>
{
    private static final DateTimeFormatter DEFAULT_FORMATTER = DateTimeFormatter.ISO_LOCAL_TIME;

    public static final LocalTimeDeserializer INSTANCE = new LocalTimeDeserializer();

    /**
     * Flag for <code>JsonFormat.Feature.READ_DATE_TIMESTAMPS_AS_NANOSECONDS</code>
     *
     * @since 2.16
     */
    protected final Boolean _readTimestampsAsNanosOverride;

    protected LocalTimeDeserializer() { // was private before 2.12
        this(DEFAULT_FORMATTER);
    }

    public LocalTimeDeserializer(DateTimeFormatter formatter) {
        super(LocalTime.class, formatter);
        _readTimestampsAsNanosOverride = null;
    }

    protected LocalTimeDeserializer(LocalTimeDeserializer base, Boolean leniency) {
        super(base, leniency);
        _readTimestampsAsNanosOverride = base._readTimestampsAsNanosOverride;
    }

    /**
     * Since 2.16
     */
    protected LocalTimeDeserializer(LocalTimeDeserializer base,
        Boolean leniency,
        DateTimeFormatter formatter,
        JsonFormat.Shape shape,
        Boolean readTimestampsAsNanosOverride) {
        super(base, leniency, formatter, shape);
        _readTimestampsAsNanosOverride = readTimestampsAsNanosOverride;
    }

    @Override
    protected LocalTimeDeserializer withDateFormat(DateTimeFormatter dtf) {
        return new LocalTimeDeserializer(this, _isLenient, dtf, _shape, _readTimestampsAsNanosOverride);
    }

    @Override
    protected LocalTimeDeserializer withLeniency(Boolean leniency) {
        return new LocalTimeDeserializer(this, leniency);
    }

    @Override
    protected JSR310DateTimeDeserializerBase<?> _withFormatOverrides(DeserializationContext ctxt,
        BeanProperty property, JsonFormat.Value formatOverrides)
    {
        LocalTimeDeserializer deser = (LocalTimeDeserializer)
            super._withFormatOverrides(ctxt, property, formatOverrides);
        Boolean readTimestampsAsNanosOverride = formatOverrides.getFeature(
            JsonFormat.Feature.READ_DATE_TIMESTAMPS_AS_NANOSECONDS);
        if (!Objects.equals(readTimestampsAsNanosOverride, deser._readTimestampsAsNanosOverride)) {
            return new LocalTimeDeserializer(deser, deser._isLenient, deser._formatter,
                deser._shape, readTimestampsAsNanosOverride);
        }
        return deser;
    }

    @Override
    public LocalTime deserialize(JsonParser p, DeserializationContext ctxt) throws JacksonException
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
                final LocalTime parsed = deserialize(p, ctxt);
                if (p.nextToken() != JsonToken.END_ARRAY) {
                    handleMissingEndArrayForSingle(p, ctxt);
                }
                return parsed;            
            }
            if (t == JsonToken.VALUE_NUMBER_INT) {
                int hour = p.getIntValue();
    
                p.nextToken();
                int minute = p.getIntValue();
                LocalTime result;
    
                t = p.nextToken();
                if (t == JsonToken.END_ARRAY) {
                    result = LocalTime.of(hour, minute);
                } else {
                    int second = p.getIntValue();
                    t = p.nextToken();
                    if (t == JsonToken.END_ARRAY) {
                        result = LocalTime.of(hour, minute, second);
                    } else {
                        int partialSecond = p.getIntValue();
                        if(partialSecond < 1_000 && !shouldReadTimestampsAsNanoseconds(ctxt))
                            partialSecond *= 1_000_000; // value is milliseconds, convert it to nanoseconds
                        t = p.nextToken();
                        if (t != JsonToken.END_ARRAY) {
                            throw ctxt.wrongTokenException(p, handledType(), JsonToken.END_ARRAY,
                                    "Expected array to end");
                        }
                        result = LocalTime.of(hour, minute, second, partialSecond);
                    }
                }
                return result;
            }
            ctxt.reportInputMismatch(handledType(),
                    "Unexpected token (%s) within Array, expected VALUE_NUMBER_INT",
                    t);
        } else if (p.hasToken(JsonToken.VALUE_EMBEDDED_OBJECT)) {
            return (LocalTime) p.getEmbeddedObject();
        } else if (p.hasToken(JsonToken.VALUE_NUMBER_INT)) {
            _throwNoNumericTimestampNeedTimeZone(p, ctxt);
        }
        return _handleUnexpectedToken(ctxt, p, "Expected array or string.");
    }

    protected boolean shouldReadTimestampsAsNanoseconds(DeserializationContext context) {
        return (_readTimestampsAsNanosOverride != null) ? _readTimestampsAsNanosOverride :
            context.isEnabled(DateTimeFeature.READ_DATE_TIMESTAMPS_AS_NANOSECONDS);
    }

    protected LocalTime _fromString(JsonParser p, DeserializationContext ctxt,
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
            if (format == DEFAULT_FORMATTER) {
                if (string.contains("T")) {
                    return LocalTime.parse(string, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                }
            }
            return LocalTime.parse(string, format);
        } catch (DateTimeException e) {
            return _handleDateTimeFormatException(ctxt, e, format, string);
        }
    }
}
