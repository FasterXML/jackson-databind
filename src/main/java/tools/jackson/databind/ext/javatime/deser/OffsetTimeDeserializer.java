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
import java.time.OffsetTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonFormat;
import tools.jackson.core.*;
import tools.jackson.databind.*;
import tools.jackson.databind.cfg.DateTimeFeature;

/**
 * Deserializer for Java 8 temporal {@link OffsetTime}s.
 *
 * @author Nick Williams
 */
public class OffsetTimeDeserializer extends JSR310DateTimeDeserializerBase<OffsetTime>
{
    public static final OffsetTimeDeserializer INSTANCE = new OffsetTimeDeserializer();

    /**
     * Flag for <code>JsonFormat.Feature.READ_DATE_TIMESTAMPS_AS_NANOSECONDS</code>
     *
     * @since 2.16
     */
    protected final Boolean _readTimestampsAsNanosOverride;

    protected OffsetTimeDeserializer() { // was private before 2.12
        this(DateTimeFormatter.ISO_OFFSET_TIME);
    }

    protected OffsetTimeDeserializer(DateTimeFormatter dtf) {
        super(OffsetTime.class, dtf);
        _readTimestampsAsNanosOverride = null;
    }

    /**
     * Since 2.11
     */
    protected OffsetTimeDeserializer(OffsetTimeDeserializer base, Boolean leniency) {
        super(base, leniency);
        _readTimestampsAsNanosOverride = base._readTimestampsAsNanosOverride;
    }

    /**
     * Since 2.16
     */
    protected OffsetTimeDeserializer(OffsetTimeDeserializer base,
        Boolean leniency,
        DateTimeFormatter formatter,
        JsonFormat.Shape shape,
        Boolean readTimestampsAsNanosOverride) {
        super(base, leniency, formatter, shape);
        _readTimestampsAsNanosOverride = readTimestampsAsNanosOverride;
    }

    @Override
    protected OffsetTimeDeserializer withDateFormat(DateTimeFormatter dtf) {
        return new OffsetTimeDeserializer(this, _isLenient, dtf, _shape, _readTimestampsAsNanosOverride);
    }

    @Override
    protected OffsetTimeDeserializer withLeniency(Boolean leniency) {
        return new OffsetTimeDeserializer(this, leniency);
    }

    @Override
    protected JSR310DateTimeDeserializerBase<?> _withFormatOverrides(DeserializationContext ctxt,
        BeanProperty property, JsonFormat.Value formatOverrides)
    {
        OffsetTimeDeserializer deser = (OffsetTimeDeserializer)
            super._withFormatOverrides(ctxt, property, formatOverrides);
        Boolean readTimestampsAsNanosOverride = formatOverrides.getFeature(
            JsonFormat.Feature.READ_DATE_TIMESTAMPS_AS_NANOSECONDS);
        if (!Objects.equals(readTimestampsAsNanosOverride, deser._readTimestampsAsNanosOverride)) {
            return new OffsetTimeDeserializer(deser, deser._isLenient, deser._formatter,
                deser._shape, readTimestampsAsNanosOverride);
        }
        return deser;
    }

    @Override
    public OffsetTime deserialize(JsonParser p, DeserializationContext ctxt)
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
        }
        if (!p.isExpectedStartArrayToken()) {
            if (p.hasToken(JsonToken.VALUE_EMBEDDED_OBJECT)) {
                return (OffsetTime) p.getEmbeddedObject();
            }
            if (p.hasToken(JsonToken.VALUE_NUMBER_INT)) {
                _throwNoNumericTimestampNeedTimeZone(p, ctxt);
            }
            throw ctxt.wrongTokenException(p, handledType(), JsonToken.START_ARRAY,
                    "Expected array or string");
        }
        JsonToken t = p.nextToken();
        if (t != JsonToken.VALUE_NUMBER_INT) {
            if (t == JsonToken.END_ARRAY) {
                return null;
            }
            if ((t == JsonToken.VALUE_STRING || t == JsonToken.VALUE_EMBEDDED_OBJECT)
                    && ctxt.isEnabled(DeserializationFeature.UNWRAP_SINGLE_VALUE_ARRAYS)) {
                final OffsetTime parsed = deserialize(p, ctxt);
                if (p.nextToken() != JsonToken.END_ARRAY) {
                    handleMissingEndArrayForSingle(p, ctxt);
                }
                return parsed;            
            }
            ctxt.reportInputMismatch(handledType(),
                    "Unexpected token (%s) within Array, expected VALUE_NUMBER_INT",
                    t);
        }
        int hour = p.getIntValue();
        int minute = p.nextIntValue(-1);
        if (minute == -1) {
            t = p.currentToken();
            if (t == JsonToken.END_ARRAY) {
                return null;
            }
            if (t != JsonToken.VALUE_NUMBER_INT) {
                _reportWrongToken(ctxt, JsonToken.VALUE_NUMBER_INT, "minutes");
            }
            minute = p.getIntValue();
        }
        int partialSecond = 0;
        int second = 0;
        if (p.nextToken() == JsonToken.VALUE_NUMBER_INT) {
            second = p.getIntValue();
            if (p.nextToken() == JsonToken.VALUE_NUMBER_INT) {
                partialSecond = p.getIntValue();
                if (partialSecond < 1_000 && !shouldReadTimestampsAsNanoseconds(ctxt)) {
                    partialSecond *= 1_000_000; // value is milliseconds, convert it to nanoseconds
                }
                p.nextToken();
            }
        }
        if (p.currentToken() == JsonToken.VALUE_STRING) {
            OffsetTime result = OffsetTime.of(hour, minute, second, partialSecond, ZoneOffset.of(p.getString()));
            if (p.nextToken() != JsonToken.END_ARRAY) {
                _reportWrongToken(ctxt, JsonToken.END_ARRAY, "timezone");
            }
            return result;
        }
        throw ctxt.wrongTokenException(p, handledType(), JsonToken.VALUE_STRING,
                "Expected string for TimeZone after numeric values");
    }

    protected boolean shouldReadTimestampsAsNanoseconds(DeserializationContext context) {
        return (_readTimestampsAsNanosOverride != null) ? _readTimestampsAsNanosOverride :
            context.isEnabled(DateTimeFeature.READ_DATE_TIMESTAMPS_AS_NANOSECONDS);
    }

    protected OffsetTime _fromString(JsonParser p, DeserializationContext ctxt,
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
            return OffsetTime.parse(string, _formatter);
        } catch (DateTimeException e) {
            return _handleDateTimeFormatException(ctxt, e, _formatter, string);
        }
    }
}
