package com.fasterxml.jackson.databind.ser.std;

import java.io.IOException;
import java.lang.reflect.Type;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.atomic.AtomicReference;

import com.fasterxml.jackson.annotation.JsonFormat;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.jsonFormatVisitors.*;
import com.fasterxml.jackson.databind.ser.ContextualSerializer;
import com.fasterxml.jackson.databind.util.StdDateFormat;

@SuppressWarnings("serial")
public abstract class DateTimeSerializerBase<T>
    extends StdScalarSerializer<T>
    implements ContextualSerializer
{
    /**
     * Flag that indicates that serialization must be done as the
     * Java timestamp, regardless of other settings.
     */
    protected final Boolean _useTimestamp;

    /**
     * Specific format to use, if not default format: non null value
     * also indicates that serialization is to be done as JSON String,
     * not numeric timestamp, unless {@link #_useTimestamp} is true.
     */
    protected final DateFormat _customFormat;

    /**
     * If {@link #_customFormat} is used, we will try to reuse instances in simplest
     * possible form; thread-safe, but without overhead of <code>ThreadLocal</code>
     * (not from code, but wrt retaining of possibly large number of format instances
     * over all threads, properties with custom formats).
     *
     * @since 2.9
     */
    protected final AtomicReference<DateFormat> _reusedCustomFormat;

    protected DateTimeSerializerBase(Class<T> type,
            Boolean useTimestamp, DateFormat customFormat)
    {
        super(type);
        _useTimestamp = useTimestamp;
        _customFormat = customFormat;
        _reusedCustomFormat = (customFormat == null) ? null : new AtomicReference<DateFormat>();
    }

    public abstract DateTimeSerializerBase<T> withFormat(Boolean timestamp, DateFormat customFormat);

    @Override
    public JsonSerializer<?> createContextual(SerializerProvider serializers,
            BeanProperty property) throws JsonMappingException
    {
        // Note! Should not skip if `property` null since that'd skip check
        // for config overrides, in case of root value
        JsonFormat.Value format = findFormatOverrides(serializers, property, handledType());
        if (format == null) {
            return this;
        }
        // Simple case first: serialize as numeric timestamp?
        JsonFormat.Shape shape = format.getShape();
        if (shape.isNumeric()) {
            return withFormat(Boolean.TRUE, null);
        }

        // 08-Jun-2017, tatu: With [databind#1648], this gets bit tricky..
        // First: custom pattern will override things
        if (format.hasPattern()) {
            final Locale loc = format.hasLocale()
                            ? format.getLocale()
                            : serializers.getLocale();
            SimpleDateFormat df = new SimpleDateFormat(format.getPattern(), loc);
            TimeZone tz = format.hasTimeZone() ? format.getTimeZone()
                    : serializers.getTimeZone();
            df.setTimeZone(tz);
            return withFormat(Boolean.FALSE, df);
        }

        // Otherwise, need one of these changes:
        final boolean hasLocale = format.hasLocale();
        final boolean hasTZ = format.hasTimeZone();
        final boolean asString = (shape == JsonFormat.Shape.STRING);

        if (!hasLocale && !hasTZ && !asString) {
            return this;
        }

        DateFormat df0 = serializers.getConfig().getDateFormat();
        // Jackson's own `StdDateFormat` is quite easy to deal with...
        if (df0 instanceof StdDateFormat) {
            StdDateFormat std = (StdDateFormat) df0;
            if (format.hasLocale()) {
                std = std.withLocale(format.getLocale());
            }
            if (format.hasTimeZone()) {
                std = std.withTimeZone(format.getTimeZone());
            }
            return withFormat(Boolean.FALSE, std);
        }

        // 08-Jun-2017, tatu: Unfortunately there's no generally usable
        //    mechanism for changing `DateFormat` instances (or even clone()ing)
        //    So: require it be `SimpleDateFormat`; can't config other types
        if (!(df0 instanceof SimpleDateFormat)) {
            serializers.reportBadDefinition(handledType(), String.format(
"Configured `DateFormat` (%s) not a `SimpleDateFormat`; cannot configure `Locale` or `TimeZone`",
df0.getClass().getName()));
        }
        SimpleDateFormat df = (SimpleDateFormat) df0;
        if (hasLocale) {
            // Ugh. No way to change `Locale`, create copy; must re-crete completely:
            df = new SimpleDateFormat(df.toPattern(), format.getLocale());
        } else {
            df = (SimpleDateFormat) df.clone();
        }
        TimeZone newTz = format.getTimeZone();
        boolean changeTZ = (newTz != null) && !newTz.equals(df.getTimeZone());
        if (changeTZ) {
            df.setTimeZone(newTz);
        }
        return withFormat(Boolean.FALSE, df);
    }

    /*
    /**********************************************************
    /* Accessors
    /**********************************************************
     */

    @Override
    public boolean isEmpty(SerializerProvider serializers, T value) {
        // 09-Mar-2017, tatu: as per [databind#1550] timestamp 0 is NOT "empty"; but
        //   with versions up to 2.8.x this was the case. Fixed for 2.9.
//        return _timestamp(value) == 0L;
        return false;
    }

    protected abstract long _timestamp(T value);

    /**
     * @deprecated Since 2.15
     */
    @Deprecated
    @Override
    public JsonNode getSchema(SerializerProvider serializers, Type typeHint) {
        //todo: (ryan) add a format for the date in the schema?
        return createSchemaNode(_asTimestamp(serializers) ? "number" : "string", true);
    }

    @Override
    public void acceptJsonFormatVisitor(JsonFormatVisitorWrapper visitor, JavaType typeHint) throws JsonMappingException
    {
        _acceptJsonFormatVisitor(visitor, typeHint, _asTimestamp(visitor.getProvider()));
    }

    /*
    /**********************************************************
    /* Actual serialization
    /**********************************************************
     */

    @Override
    public abstract void serialize(T value, JsonGenerator gen, SerializerProvider serializers)
        throws IOException;

    /*
    /**********************************************************
    /* Helper methods
    /**********************************************************
     */

    protected boolean _asTimestamp(SerializerProvider serializers)
    {
        if (_useTimestamp != null) {
            return _useTimestamp.booleanValue();
        }
        if (_customFormat == null) {
            if (serializers != null) {
                return serializers.isEnabled(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
            }
            // 12-Jun-2014, tatu: Is it legal not to have provider? Was NPE:ing earlier so leave a check
            throw new IllegalArgumentException("Null SerializerProvider passed for "+handledType().getName());
        }
        return false;
    }

    protected void _acceptJsonFormatVisitor(JsonFormatVisitorWrapper visitor, JavaType typeHint,
		boolean asNumber) throws JsonMappingException
    {
        if (asNumber) {
            visitIntFormat(visitor, typeHint,
                    JsonParser.NumberType.LONG, JsonValueFormat.UTC_MILLISEC);
        } else {
            visitStringFormat(visitor, typeHint, JsonValueFormat.DATE_TIME);
        }
    }

    /**
     * @since 2.9
     */
    protected void _serializeAsString(Date value, JsonGenerator g, SerializerProvider provider) throws IOException
    {
        if (_customFormat == null) {
            provider.defaultSerializeDateValue(value, g);
            return;
        }

        // 19-Jul-2017, tatu: Here we will try a simple but (hopefully) effective mechanism for
        //    reusing formatter instance. This is our second attempt, after initially trying simple
        //    synchronization (which turned out to be bottleneck for some users in production...).
        //    While `ThreadLocal` could alternatively be used, it is likely that it would lead to
        //    higher memory footprint, but without much upside -- if we can not reuse, we'll just
        //    clone(), which has some overhead but not drastic one.

        DateFormat f = _reusedCustomFormat.getAndSet(null);
        if (f == null) {
            f = (DateFormat) _customFormat.clone();
        }
        g.writeString(f.format(value));
        _reusedCustomFormat.compareAndSet(null, f);
    }
}
