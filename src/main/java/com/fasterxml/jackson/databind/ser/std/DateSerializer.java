package com.fasterxml.jackson.databind.ser.std;

import java.io.IOException;
import java.lang.reflect.Type;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.core.*;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.annotation.JacksonStdImpl;
import com.fasterxml.jackson.databind.ser.ContextualSerializer;

/**
 * For efficiency, we will serialize Dates as longs, instead of
 * potentially more readable Strings.
 */
@JacksonStdImpl
public class DateSerializer
    extends StdScalarSerializer<Date>
    implements ContextualSerializer
{
    /**
     * Default instance that is used when no contextual configuration
     * is needed.
     */
    public static DateSerializer instance = new DateSerializer();
    
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
    
    /*
    /**********************************************************
    /* Life-cycle
    /**********************************************************
     */
    
    public DateSerializer() {
        this(false, null);
    }
        
    public DateSerializer(boolean useTimestamp, DateFormat customFormat)
    {
        super(Date.class);
        _useTimestamp = useTimestamp;
        _customFormat = customFormat;
    }

    public DateSerializer withFormat(boolean timestamp, DateFormat customFormat)
    {
        if (timestamp) {
            return new DateSerializer(true, null);
        }
        return new DateSerializer(false, customFormat);
    }

    @Override
    public JsonSerializer<?> createContextual(SerializerProvider prov,
            BeanProperty property) throws JsonMappingException
    {
        if (property != null) {
            JsonFormat.Value format = prov.getAnnotationIntrospector().findFormat(property.getMember());
            if (format != null) {
                if (format.shape.isNumeric()) {
                    return withFormat(true, null);
                }
                String pattern = format.pattern;
                if (pattern.length() > 0){
                    SimpleDateFormat df = new SimpleDateFormat(pattern, prov.getLocale());
                    df.setTimeZone(prov.getTimeZone());
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
    public boolean isEmpty(Date value) {
        // let's assume "null date" (timestamp 0) qualifies for empty
        return (value == null) || (value.getTime() == 0L);
    }

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

    /*
    /**********************************************************
    /* Actual serialization
    /**********************************************************
     */
    
    @Override
    public void serialize(Date value, JsonGenerator jgen, SerializerProvider provider)
        throws IOException, JsonGenerationException
    {
        if (_useTimestamp) {
            jgen.writeNumber(value.getTime());
        } else if (_customFormat != null) {
            // 21-Feb-2011, tatu: not optimal, but better than alternatives:
            synchronized (_customFormat) {
                jgen.writeString(_customFormat.format(value));
            }
        } else {
            provider.defaultSerializeDateValue(value, jgen);
        }
    }
}
