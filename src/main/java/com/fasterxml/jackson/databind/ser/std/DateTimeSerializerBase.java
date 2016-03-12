package com.fasterxml.jackson.databind.ser.std;

import java.io.IOException;
import java.lang.reflect.Type;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.TimeZone;

import com.fasterxml.jackson.annotation.JsonFormat;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.introspect.Annotated;
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

    protected DateTimeSerializerBase(Class<T> type,
            Boolean useTimestamp, DateFormat customFormat)
    {
        super(type);
        _useTimestamp = useTimestamp;
        _customFormat = customFormat;
    }

    public abstract DateTimeSerializerBase<T> withFormat(Boolean timestamp, DateFormat customFormat);

    @Override
    public JsonSerializer<?> createContextual(SerializerProvider serializers,
            BeanProperty property) throws JsonMappingException
    {
        if (property != null) {
            JsonFormat.Value format = serializers.getAnnotationIntrospector().findFormat((Annotated)property.getMember());
            if (format != null) {

            	// Simple case first: serialize as numeric timestamp?
                JsonFormat.Shape shape = format.getShape();
                if (shape.isNumeric()) {
                    return withFormat(Boolean.TRUE, null);
                }

                if ((shape == JsonFormat.Shape.STRING) || format.hasPattern()
                                || format.hasLocale() || format.hasTimeZone()) {
                    TimeZone tz = format.getTimeZone();
                    final String pattern = format.hasPattern()
                                    ? format.getPattern()
                                    : StdDateFormat.DATE_FORMAT_STR_ISO8601;
                    final Locale loc = format.hasLocale()
                                    ? format.getLocale()
                                    : serializers.getLocale();
                    SimpleDateFormat df = new SimpleDateFormat(pattern, loc);
                    if (tz == null) {
                        tz = serializers.getTimeZone();
                    }
                    df.setTimeZone(tz);
                    return withFormat(Boolean.FALSE, df);
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

    @Deprecated
    @Override
    public boolean isEmpty(T value) {
        // let's assume "null date" (timestamp 0) qualifies for empty
        return (value == null) || (_timestamp(value) == 0L);
    }

    @Override
    public boolean isEmpty(SerializerProvider serializers, T value) {
        // let's assume "null date" (timestamp 0) qualifies for empty
        return (value == null) || (_timestamp(value) == 0L);
    }
    
    protected abstract long _timestamp(T value);
    
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
}
