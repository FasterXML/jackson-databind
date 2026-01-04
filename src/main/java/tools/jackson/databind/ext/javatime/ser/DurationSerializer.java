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

import java.math.BigDecimal;
import java.time.Duration;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

import com.fasterxml.jackson.annotation.JsonFormat;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonGenerator;
import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;

import tools.jackson.databind.*;
import tools.jackson.databind.cfg.DateTimeFeature;
import tools.jackson.databind.ext.javatime.util.DecimalUtils;
import tools.jackson.databind.ext.javatime.util.DurationUnitConverter;
import tools.jackson.databind.jsonFormatVisitors.JsonFormatVisitorWrapper;
import tools.jackson.databind.jsonFormatVisitors.JsonIntegerFormatVisitor;
import tools.jackson.databind.jsonFormatVisitors.JsonValueFormat;

/**
 * Serializer for Java 8 temporal {@link Duration}s.
 *<p>
 * NOTE: since 2.10, {@link DateTimeFeature#WRITE_DURATIONS_AS_TIMESTAMPS}
 * determines global default used for determining if serialization should use
 * numeric (timestamps) or textual representation. Before this,
 * {@link DateTimeFeature#WRITE_DATES_AS_TIMESTAMPS} was used.
 *
 * @author Nick Williams
 */
public class DurationSerializer extends JSR310FormattedSerializerBase<Duration>
{
    public static final DurationSerializer INSTANCE = new DurationSerializer();

    /**
     * When defined (not {@code null}) duration values will be converted into integers
     * with the unit configured for the converter.
     * Only available when {@link SerializationFeature#WRITE_DURATIONS_AS_TIMESTAMPS} is enabled
     * and {@link SerializationFeature#WRITE_DATE_TIMESTAMPS_AS_NANOSECONDS} is not enabled
     * since the duration converters do not support fractions
     */
    private DurationUnitConverter _durationUnitConverter;

    protected DurationSerializer() {
        super(Duration.class);
    }

    protected DurationSerializer(DurationSerializer base, DateTimeFormatter dtf,
            Boolean useTimestamp) {
        super(base, dtf, useTimestamp, null, null);
    }

    protected DurationSerializer(DurationSerializer base, DateTimeFormatter dtf,
            Boolean useTimestamp, Boolean useNanoseconds) {
        super(base, dtf, useTimestamp, useNanoseconds, null);
    }

    protected DurationSerializer(DurationSerializer base, DurationUnitConverter converter) {
        super(base, base._formatter, base._useTimestamp, base._useNanoseconds, base._shape);
        _durationUnitConverter = converter;
    }

    @Override
    protected DurationSerializer withFormat(DateTimeFormatter dtf,
            Boolean useTimestamp, JsonFormat.Shape shape) {
        return new DurationSerializer(this, dtf, useTimestamp);
    }

    protected DurationSerializer withConverter(DurationUnitConverter converter) {
        return new DurationSerializer(this, converter);
    }

    @Override
    protected DateTimeFeature getTimestampsFeature() {
        return DateTimeFeature.WRITE_DURATIONS_AS_TIMESTAMPS;
    }

    @Override
    public ValueSerializer<?> createContextual(SerializationContext ctxt, BeanProperty property)
    {
        DurationSerializer ser = (DurationSerializer) super.createContextual(ctxt, property);
        JsonFormat.Value format = findFormatOverrides(ctxt, property, handledType());
        if (format != null && format.hasPattern()) {
            final String pattern = format.getPattern();
            DurationUnitConverter conv = DurationUnitConverter.from(pattern);
            if (conv == null) {
                ctxt.reportBadDefinition(handledType(),
                        String.format(
                                "Bad 'pattern' definition (\"%s\") for `Duration`: expected one of [%s]",
                                pattern, DurationUnitConverter.descForAllowed()));
            }
            ser = ser.withConverter(conv);
        }
        return ser;
    }

    @Override
    public void serialize(Duration duration, JsonGenerator generator, SerializationContext ctxt)
        throws JacksonException
    {
        // Apply millisecond truncation if enabled
        if (ctxt.isEnabled(DateTimeFeature.TRUNCATE_TO_MSECS_ON_WRITE)) {
            duration = duration.truncatedTo(ChronoUnit.MILLIS);
        }

        if (useTimestamp(ctxt)) {
            // 03-Aug-2022, tatu: As per [modules-java8#224] need to consider
            //     Pattern first, and only then nano-seconds/millis difference
            if (_durationUnitConverter != null) {
                generator.writeNumber(_durationUnitConverter.convert(duration));
            } else if (useNanoseconds(ctxt)) {
                generator.writeNumber(_toNanos(duration));
            } else {
                generator.writeNumber(duration.toMillis());
            }
        } else {
            generator.writeString(duration.toString());
        }
    }

    // 20-Oct-2020, tatu: [modules-java8#165] Need to take care of
    //    negative values too, and without work-around values
    //    returned are wonky wrt conversions
    private BigDecimal _toNanos(Duration duration) {
        BigDecimal bd;
        if (duration.isNegative()) {
            duration = duration.abs();
            bd = DecimalUtils.toBigDecimal(duration.getSeconds(),
                    duration.getNano())
                .negate();
        } else {
            bd = DecimalUtils.toBigDecimal(duration.getSeconds(),
                    duration.getNano());
        }
        return bd;
    }

    @Override
    protected void _acceptTimestampVisitor(JsonFormatVisitorWrapper visitor, JavaType typeHint)
    {
        JsonIntegerFormatVisitor v2 = visitor.expectIntegerFormat(typeHint);
        if (v2 != null) {
            v2.numberType(JsonParser.NumberType.LONG);
            SerializationContext ctxt = visitor.getContext();
            if ((ctxt != null) && useNanoseconds(ctxt)) {
                // big number, no more specific qualifier to use...
            } else { // otherwise good old Unix timestamp, in milliseconds
                v2.format(JsonValueFormat.UTC_MILLISEC);
            }
        }
    }

    @Override
    protected JsonToken serializationShape(SerializationContext ctxt) {
        if (useTimestamp(ctxt)) {
            if (useNanoseconds(ctxt)) {
                return JsonToken.VALUE_NUMBER_FLOAT;
            }
            return JsonToken.VALUE_NUMBER_INT;
        }
        return JsonToken.VALUE_STRING;
    }

    @Override
    protected JSR310FormattedSerializerBase<?> withFeatures(Boolean writeZoneId, Boolean writeNanoseconds) {
        return new DurationSerializer(this, _formatter, _useTimestamp, writeNanoseconds);
    }

    @Override
    protected DateTimeFormatter _useDateTimeFormatter(SerializationContext ctxt, JsonFormat.Value format) {
        return null;
    }
}
