package tools.jackson.databind.ext.javatime.ser;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

import com.fasterxml.jackson.annotation.JsonFormat;

import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonGenerator;
import tools.jackson.core.JsonToken;

import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.cfg.DateTimeFeature;

public class ZonedDateTimeSerializer extends InstantSerializerBase<ZonedDateTime> {
    public static final ZonedDateTimeSerializer INSTANCE = new ZonedDateTimeSerializer();

    /**
     * Flag for <code>JsonFormat.Feature.WRITE_DATES_WITH_ZONE_ID</code>
     */
    protected final Boolean _writeZoneId;
    
    protected ZonedDateTimeSerializer() {
        // ISO_ZONED_DATE_TIME is an extended version of ISO compliant format
        // ISO_OFFSET_DATE_TIME with additional information :Zone Id
        // (This is not part of the ISO-8601 standard)
        this(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
    }

    public ZonedDateTimeSerializer(DateTimeFormatter formatter) {
        super(ZonedDateTime.class, dt -> dt.toInstant().toEpochMilli(),
              ZonedDateTime::toEpochSecond, ZonedDateTime::getNano,
              formatter);
        _writeZoneId = null;
    }

    protected ZonedDateTimeSerializer(ZonedDateTimeSerializer base,
            DateTimeFormatter formatter,
            Boolean useTimestamp, Boolean useNanoseconds,
            Boolean writeZoneId,
            JsonFormat.Shape shape)
    {
        super(base, formatter, useTimestamp, useNanoseconds, shape);
        _writeZoneId = writeZoneId;
    }

    @Override
    protected JSR310FormattedSerializerBase<?> withFormat(DateTimeFormatter formatter, 
            Boolean useTimestamp,
            JsonFormat.Shape shape)
    {
        return new ZonedDateTimeSerializer(this, formatter,
                useTimestamp, _useNanoseconds, _writeZoneId,
                shape);
    }

    @Override
    protected JSR310FormattedSerializerBase<?> withFeatures(Boolean writeZoneId,
            Boolean useNanoseconds)
    {
        return new ZonedDateTimeSerializer(this, _formatter,
                _useTimestamp, useNanoseconds, writeZoneId, _shape);
    }

    @Override
    public void serialize(ZonedDateTime value, JsonGenerator g, SerializationContext ctxt)
        throws JacksonException
    {
        if (!useTimestamp(ctxt)) {
            // [modules-java8#333]: `@JsonFormat` with pattern should override
            //   `SerializationFeature.WRITE_DATES_WITH_ZONE_ID`
            if ((_formatter != null) && (_shape == JsonFormat.Shape.STRING)) {
                ; // use default handling
            } else if (shouldWriteWithZoneId(ctxt)) {
                // Apply millisecond truncation if enabled
                if (ctxt.isEnabled(DateTimeFeature.TRUNCATE_TO_MSECS_ON_WRITE)) {
                    value = value.truncatedTo(ChronoUnit.MILLIS);
                }
                // write with zone
                g.writeString(DateTimeFormatter.ISO_ZONED_DATE_TIME.format(value));
                return;
            }
        }
        super.serialize(value, g, ctxt);
    }

    @Override
    protected String formatValue(ZonedDateTime value, SerializationContext ctxt) {
        String formatted = super.formatValue(value, ctxt);
        // [modules-java8#333]: `@JsonFormat` with pattern should override
        //   `SerializationFeature.WRITE_DATES_WITH_ZONE_ID`
        if (_formatter != null && _shape == JsonFormat.Shape.STRING) {
            // Why not `if (shouldWriteWithZoneId(provider))` ?
            if (Boolean.TRUE.equals(_writeZoneId)) {
                formatted += "[" + value.getZone().getId() + "]";
            }
        }
        return formatted;
    }    
    public boolean shouldWriteWithZoneId(SerializationContext ctxt) {
        return (_writeZoneId != null)
                ? _writeZoneId
                : ctxt.isEnabled(DateTimeFeature.WRITE_DATES_WITH_ZONE_ID);
    }

    @Override
    protected JsonToken serializationShape(SerializationContext ctxt) {
        if (!useTimestamp(ctxt) && shouldWriteWithZoneId(ctxt)) {
            return JsonToken.VALUE_STRING;
        }
        return super.serializationShape(ctxt);
    }
}
