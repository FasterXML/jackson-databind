package com.fasterxml.jackson.databind.ser.std;

import java.io.IOException;
import java.lang.reflect.Type;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.TimeZone;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.introspect.Annotated;
import com.fasterxml.jackson.databind.jsonFormatVisitors.*;
import com.fasterxml.jackson.databind.ser.ContextualSerializer;
import com.fasterxml.jackson.databind.util.StdDateFormat;

public abstract class DateTimeSerializerBase<T>
    extends StdScalarSerializer<T>
    implements ContextualSerializer
{
    /**
     * Flag that indicates that serialization must be done as the
     * Java timetamp, regardless of other settings.
     */
    protected final boolean _useTimestamp;
    
    /**
     * Specific format to use, if not default format: non null value
     * also indicates that serialization is to be done as JSON String,
     * not numeric timestamp, unless {@link #_useTimestamp} is true.
     */
    protected final DateFormat _customFormat;

    protected DateTimeSerializerBase(Class<T> type,
            boolean useTimestamp, DateFormat customFormat)
    {
        super(type);
        _useTimestamp = useTimestamp;
        _customFormat = customFormat;
    }

    public abstract DateTimeSerializerBase<T> withFormat(boolean timestamp, DateFormat customFormat);

    @Override
    public JsonSerializer<?> createContextual(SerializerProvider prov,
            BeanProperty property) throws JsonMappingException
    {
        if (property != null) {
            JsonFormat.Value format = prov.getAnnotationIntrospector().findFormat((Annotated)property.getMember());
            if (format != null) {
                // Simple case first: serialize as numeric timestamp?
                if (format.getShape().isNumeric()) {
                    return withFormat(true, null);
                }
                // If not, do we have a pattern?
                TimeZone tz = format.getTimeZone();
                String pattern = format.getPattern();
                if (pattern.length() > 0){
                    Locale loc = format.getLocale();
                    if (loc == null) {
                        loc = prov.getLocale();
                    }
                    SimpleDateFormat df = new SimpleDateFormat(pattern, loc);
                    if (tz == null) {
                        tz = prov.getTimeZone();
                    }
                    df.setTimeZone(tz);
                    return withFormat(false, df);
                }
                // If not, do we at least have a custom timezone?
                if (tz != null) {
                    DateFormat df = prov.getConfig().getDateFormat();
                    // one shortcut: with our custom format, can simplify handling a bit
                    if (df.getClass() == StdDateFormat.class) {
                        df = StdDateFormat.getISO8601Format(tz);
                    } else {
                        // otherwise need to clone, re-set timezone:
                        df = (DateFormat) df.clone();
                        df.setTimeZone(tz);
                    }
                    return withFormat(false, df);
                }
            }
        }
        return this;
    }

    /*
    /**********************************************************
    /* Accessors
    /**********************************************************
     */

    @Override
    public boolean isEmpty(T value) {
        // let's assume "null date" (timestamp 0) qualifies for empty
        return (value == null) || (_timestamp(value) == 0L);
    }

    protected abstract long _timestamp(T value);
    
    @Override
    public JsonNode getSchema(SerializerProvider provider, Type typeHint)
    {
        //todo: (ryan) add a format for the date in the schema?
        boolean asNumber = _useTimestamp;
        if (!asNumber) {
            if (_customFormat == null) {
                asNumber = provider.isEnabled(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
            }
        }
        return createSchemaNode(asNumber ? "number" : "string", true);
    }
    
    @Override
    public void acceptJsonFormatVisitor(JsonFormatVisitorWrapper visitor, JavaType typeHint)
        throws JsonMappingException
    {
        boolean asNumber = _useTimestamp;
        if (!asNumber) {
            if (_customFormat == null) {
                asNumber = visitor.getProvider().isEnabled(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
            }
        }
        if (asNumber) {
            JsonIntegerFormatVisitor v2 = visitor.expectIntegerFormat(typeHint);
            if (v2 != null) {
                v2.numberType(JsonParser.NumberType.LONG);
                v2.format(JsonValueFormat.UTC_MILLISEC);
            }
        } else {
            JsonStringFormatVisitor v2 = visitor.expectStringFormat(typeHint);
            if (v2 != null) {
                v2.format(JsonValueFormat.DATE_TIME);
            }
        }
    }

    /*
    /**********************************************************
    /* Actual serialization
    /**********************************************************
     */

    @Override
    public abstract void serialize(T value, JsonGenerator jgen, SerializerProvider provider)
        throws IOException, JsonGenerationException;
}
