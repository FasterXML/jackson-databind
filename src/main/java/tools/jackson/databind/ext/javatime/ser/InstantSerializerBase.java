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

import java.time.format.DateTimeFormatter;
import java.time.temporal.Temporal;
import java.util.function.ToIntFunction;
import java.util.function.ToLongFunction;

import com.fasterxml.jackson.annotation.JsonFormat;

import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonGenerator;
import tools.jackson.core.JsonToken;
import tools.jackson.core.JsonParser.NumberType;

import tools.jackson.databind.*;
import tools.jackson.databind.cfg.DateTimeFeature;
import tools.jackson.databind.ext.javatime.util.DecimalUtils;
import tools.jackson.databind.jsonFormatVisitors.JsonFormatVisitorWrapper;
import tools.jackson.databind.jsonFormatVisitors.JsonIntegerFormatVisitor;
import tools.jackson.databind.jsonFormatVisitors.JsonNumberFormatVisitor;
import tools.jackson.databind.jsonFormatVisitors.JsonValueFormat;

/**
 * Base class for serializers used for {@link java.time.Instant} and
 * other {@link Temporal} subtypes.
 */
public abstract class InstantSerializerBase<T extends Temporal>
    extends JSR310FormattedSerializerBase<T>
{
    private final DateTimeFormatter defaultFormat;

    private final ToLongFunction<T> getEpochMillis;

    private final ToLongFunction<T> getEpochSeconds;

    private final ToIntFunction<T> getNanoseconds;

    protected InstantSerializerBase(Class<T> supportedType, ToLongFunction<T> getEpochMillis,
            ToLongFunction<T> getEpochSeconds, ToIntFunction<T> getNanoseconds,
            DateTimeFormatter defaultFormat)
    {
        // Bit complicated, just because we actually want to "hide" default formatter,
        // so that it won't accidentally force use of textual presentation
        super(supportedType, null);
        this.defaultFormat = defaultFormat;
        this.getEpochMillis = getEpochMillis;
        this.getEpochSeconds = getEpochSeconds;
        this.getNanoseconds = getNanoseconds;
    }

    protected InstantSerializerBase(InstantSerializerBase<T> base,
            DateTimeFormatter dtf,
            Boolean useTimestamp,  Boolean useNanoseconds,
            JsonFormat.Shape shape)
    {
        super(base, dtf, useTimestamp, useNanoseconds, shape);
        defaultFormat = base.defaultFormat;
        getEpochMillis = base.getEpochMillis;
        getEpochSeconds = base.getEpochSeconds;
        getNanoseconds = base.getNanoseconds;
    }

    @Override
    protected abstract JSR310FormattedSerializerBase<?> withFormat(DateTimeFormatter dtf,
        Boolean useTimestamp,
        JsonFormat.Shape shape);

    @Override
    public void serialize(T value, JsonGenerator generator, SerializationContext ctxt)
        throws JacksonException
    {
        if (useTimestamp(ctxt)) {
            if (useNanoseconds(ctxt)) {
                generator.writeNumber(DecimalUtils.toBigDecimal(
                        getEpochSeconds.applyAsLong(value), getNanoseconds.applyAsInt(value)
                ));
                return;
            }
            generator.writeNumber(getEpochMillis.applyAsLong(value));
            return;
        }

        generator.writeString(formatValue(value, ctxt));
    }

    // Overridden to ensure that our timestamp handling is as expected
    @Override
    protected void _acceptTimestampVisitor(JsonFormatVisitorWrapper visitor, JavaType typeHint)
    {
        if (useNanoseconds(visitor.getContext())) {
            JsonNumberFormatVisitor v2 = visitor.expectNumberFormat(typeHint);
            if (v2 != null) {
                v2.numberType(NumberType.BIG_DECIMAL);
            }
        } else {
            JsonIntegerFormatVisitor v2 = visitor.expectIntegerFormat(typeHint);
            if (v2 != null) {
                v2.numberType(NumberType.LONG);
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

    protected String formatValue(T value, SerializationContext ctxt)
    {
        DateTimeFormatter formatter = (_formatter == null) ? defaultFormat :_formatter;
        if (formatter != null) {
            if (formatter.getZone() == null) { // timezone set if annotated on property
                // If the user specified to use the context TimeZone explicitly, and the formatter provided doesn't contain a TZ
                // Then we use the TZ specified in the objectMapper
                if (ctxt.getConfig().hasExplicitTimeZone()
                        && ctxt.isEnabled(DateTimeFeature.WRITE_DATES_WITH_CONTEXT_TIME_ZONE)) {
                    formatter = formatter.withZone(ctxt.getTimeZone().toZoneId());
                }
            }
            return formatter.format(value);
        }

        return value.toString();
    }
}
