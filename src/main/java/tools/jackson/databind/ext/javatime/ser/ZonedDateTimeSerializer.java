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
            // [modules-java8#333], [databind#6151]: explicitly configured format should
            //   override `DateTimeFeature.WRITE_DATES_WITH_ZONE_ID`
            if (_hasExplicitFormat()) {
                ; // use default handling
            } else if (shouldWriteWithZoneId(ctxt)) {
                // Apply millisecond truncation if enabled
                if (ctxt.isEnabled(DateTimeFeature.TRUNCATE_TO_MSECS_ON_WRITE)) {
                    value = value.truncatedTo(ChronoUnit.MILLIS);
                }
                // write with zone (note: only standard default format gets here, so
                // sub-second variant may be used as-is)
                DateTimeFormatter formatter = ctxt.isEnabled(DateTimeFeature.ALWAYS_WRITE_SUBSECOND_DIGITS)
                        ? SubSecondFormatters.ZONED_DATE_TIME
                        : DateTimeFormatter.ISO_ZONED_DATE_TIME;
                g.writeString(formatter.format(value));
                return;
            }
        }
        super.serialize(value, g, ctxt);
    }

    @Override
    protected String formatValue(ZonedDateTime value, SerializationContext ctxt) {
        String formatted = super.formatValue(value, ctxt);
        // [modules-java8#333], [databind#6151]: when an explicitly configured format is
        //   used, Zone Id is only added if specifically requested (via `@JsonFormat`),
        //   and NOT due to `DateTimeFeature.WRITE_DATES_WITH_ZONE_ID`
        if (_hasExplicitFormat()) {
            // Why not `if (shouldWriteWithZoneId(provider))` ?
            if (Boolean.TRUE.equals(_writeZoneId)) {
                formatted += "[" + value.getZone().getId() + "]";
            }
        }
        return formatted;
    }

    /**
     * Accessor for checking whether this serializer has an explicitly configured
     * format that should be used as-is, taking precedence over
     * {@link DateTimeFeature#WRITE_DATES_WITH_ZONE_ID}: either a {@code @JsonFormat}
     * pattern (with String shape), or a caller-provided default formatter (see
     * {@link #ZonedDateTimeSerializer(DateTimeFormatter)}).
     *
     * @since 3.3
     */
    protected boolean _hasExplicitFormat() {
        if ((_formatter != null) && (_shape == JsonFormat.Shape.STRING)) {
            return true;
        }
        DateTimeFormatter df = _defaultFormat();
        return (df != null) && (df != DateTimeFormatter.ISO_OFFSET_DATE_TIME);
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

    @Override
    protected DateTimeFormatter _alwaysWriteSubSecondDigitsFormatter(ZonedDateTime value,
            DateTimeFormatter defaultFormat) {
        // 10-Aug-2026, tatu: Caller may pass its own default formatter (see
        //    `ZonedDateTimeSerializer(DateTimeFormatter)`); if so, must not override it
        return (defaultFormat == DateTimeFormatter.ISO_OFFSET_DATE_TIME)
                ? SubSecondFormatters.OFFSET_DATE_TIME : null;
    }
}
