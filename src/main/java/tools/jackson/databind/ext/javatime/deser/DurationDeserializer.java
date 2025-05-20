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

import java.math.BigDecimal;
import java.time.DateTimeException;
import java.time.Duration;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonFormat;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.core.JsonTokenId;
import tools.jackson.core.StreamReadCapability;
import tools.jackson.core.io.NumberInput;
import tools.jackson.databind.*;
import tools.jackson.databind.cfg.DateTimeFeature;
import tools.jackson.databind.ext.javatime.util.DecimalUtils;
import tools.jackson.databind.ext.javatime.util.DurationUnitConverter;

/**
 * Deserializer for Java 8 temporal {@link Duration}s.
 */
public class DurationDeserializer extends JSR310DeserializerBase<Duration>
{
    public static final DurationDeserializer INSTANCE = new DurationDeserializer();

    /**
     * When defined (not {@code null}) integer values will be converted into duration
     * unit configured for the converter.
     * Using this converter will typically override the value specified in
     * {@link DateTimeFeature#READ_DATE_TIMESTAMPS_AS_NANOSECONDS} as it is
     * considered that the unit set in {@link JsonFormat#pattern()} has precedence
     * since it is more specific.
     *<p>
     * See [jackson-modules-java8#184] for more info.
     */
    protected final DurationUnitConverter _durationUnitConverter;

    /**
     * Flag for <code>JsonFormat.Feature.READ_DATE_TIMESTAMPS_AS_NANOSECONDS</code>
     *
     * @since 2.16
     */
    protected final Boolean _readTimestampsAsNanosOverride;

    public DurationDeserializer() {
        super(Duration.class);
        _durationUnitConverter = null;
        _readTimestampsAsNanosOverride = null;
    }

    /**
     * @since 2.11
     */
    protected DurationDeserializer(DurationDeserializer base, Boolean leniency) {
        super(base, leniency);
        _durationUnitConverter = base._durationUnitConverter;
        _readTimestampsAsNanosOverride = base._readTimestampsAsNanosOverride;
    }

    /**
     * @since 2.12
     */
    protected DurationDeserializer(DurationDeserializer base, DurationUnitConverter converter) {
        super(base, base._isLenient);
        _durationUnitConverter = converter;
        _readTimestampsAsNanosOverride = base._readTimestampsAsNanosOverride;
    }

    /**
     * @since 2.16
     */
    protected DurationDeserializer(DurationDeserializer base,
        Boolean leniency,
        DurationUnitConverter converter,
        Boolean readTimestampsAsNanosOverride) {
        super(base, leniency);
        _durationUnitConverter = converter;
        _readTimestampsAsNanosOverride = readTimestampsAsNanosOverride;
    }

    @Override
    protected DurationDeserializer withLeniency(Boolean leniency) {
        return new DurationDeserializer(this, leniency);
    }

    protected DurationDeserializer withConverter(DurationUnitConverter converter) {
        return new DurationDeserializer(this, converter);
    }

    @Override
    public ValueDeserializer<?> createContextual(DeserializationContext ctxt,
            BeanProperty property)
    {
        JsonFormat.Value format = findFormatOverrides(ctxt, property, handledType());
        DurationDeserializer deser = this;
        boolean leniency = _isLenient;
        DurationUnitConverter unitConverter = _durationUnitConverter;
        Boolean timestampsAsNanosOverride = _readTimestampsAsNanosOverride;
        if (format != null) {
            if (format.hasLenient()) {
                leniency = format.getLenient();
            }
            if (format.hasPattern()) {
                final String pattern = format.getPattern();
                unitConverter = DurationUnitConverter.from(pattern);
                if (unitConverter == null) {
                    ctxt.reportBadDefinition(getValueType(ctxt),
                            String.format(
                                    "Bad 'pattern' definition (\"%s\") for `Duration`: expected one of [%s]",
                                    pattern, DurationUnitConverter.descForAllowed()));
                }
            }
            timestampsAsNanosOverride =
                format.getFeature(JsonFormat.Feature.READ_DATE_TIMESTAMPS_AS_NANOSECONDS);
        }
        if (leniency != _isLenient
            || !Objects.equals(unitConverter, _durationUnitConverter)
            || !Objects.equals(timestampsAsNanosOverride, _readTimestampsAsNanosOverride)) {
            return new DurationDeserializer(
                this, leniency, unitConverter, timestampsAsNanosOverride);
        }
        return deser;
    }

    @Override
    public Duration deserialize(JsonParser parser, DeserializationContext context)
        throws JacksonException
    {
        switch (parser.currentTokenId())
        {
            case JsonTokenId.ID_NUMBER_FLOAT:
                BigDecimal value = parser.getDecimalValue();
                // [modules-java8#337] since 2.19, Duration does not need negative adjustment
                return DecimalUtils.extractSecondsAndNanos(value, Duration::ofSeconds, false);
            case JsonTokenId.ID_NUMBER_INT:
                return _fromTimestamp(context, parser.getLongValue());
            case JsonTokenId.ID_STRING:
                return _fromString(parser, context, parser.getString());
            case JsonTokenId.ID_EMBEDDED_OBJECT:
                // 20-Apr-2016, tatu: Related to [databind#1208], can try supporting embedded
                //    values quite easily
                return (Duration) parser.getEmbeddedObject();
            case JsonTokenId.ID_START_ARRAY:
                return _deserializeFromArray(parser, context);
            // 30-Sep-2020, tatu: New! "Scalar from Object" (mostly for XML)
            case JsonTokenId.ID_START_OBJECT:
                String str = context.extractScalarFromObject(parser, this, handledType());
                // 17-May-2025, tatu: [databind#4656] need to check for `null`
                if (str != null) {
                    return _fromString(parser, context, str);
                }
                // fall through
        }
        return _handleUnexpectedToken(context, parser, JsonToken.VALUE_STRING,
                JsonToken.VALUE_NUMBER_INT, JsonToken.VALUE_NUMBER_FLOAT);
    }

    protected Duration _fromString(JsonParser parser, DeserializationContext ctxt,
            String value0)
        throws JacksonException
    {
        String value = value0.trim();
        if (value.length() == 0) {
            // 22-Oct-2020, tatu: not sure if we should pass original (to distinguish
            //   b/w empty and blank); for now don't which will allow blanks to be
            //   handled like "regular" empty (same as pre-2.12)
            return _fromEmptyString(parser, ctxt, value);
        }
        // 30-Sep-2020: Should allow use of "Timestamp as String" for
        //     some textual formats
        if (ctxt.isEnabled(StreamReadCapability.UNTYPED_SCALARS)
                && _isValidTimestampString(value)) {
            return _fromTimestamp(ctxt, NumberInput.parseLong(value));
        }

        try {
            return Duration.parse(value);
        } catch (DateTimeException e) {
            // null format -> "default formatter"
            return _handleDateTimeFormatException(ctxt, e, null, value);
        }
    }

    protected Duration _fromTimestamp(DeserializationContext ctxt, long ts)
    {
        if (_durationUnitConverter != null) {
            return _durationUnitConverter.convert(ts);
        }
        // 20-Oct-2020, tatu: This makes absolutely no sense but... somehow
        //   became the default handling.
        if (shouldReadTimestampsAsNanoseconds(ctxt)) {
            return Duration.ofSeconds(ts);
        }
        return Duration.ofMillis(ts);
    }

    protected boolean shouldReadTimestampsAsNanoseconds(DeserializationContext context) {
        return (_readTimestampsAsNanosOverride != null) ? _readTimestampsAsNanosOverride :
            context.isEnabled(DateTimeFeature.READ_DATE_TIMESTAMPS_AS_NANOSECONDS);
    }
}
