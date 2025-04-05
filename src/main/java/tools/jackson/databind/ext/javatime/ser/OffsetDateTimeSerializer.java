package tools.jackson.databind.ext.javatime.ser;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

public class OffsetDateTimeSerializer extends InstantSerializerBase<OffsetDateTime>
{
    public static final OffsetDateTimeSerializer INSTANCE = new OffsetDateTimeSerializer();

    protected OffsetDateTimeSerializer() {
        super(OffsetDateTime.class, dt -> dt.toInstant().toEpochMilli(),
                OffsetDateTime::toEpochSecond, OffsetDateTime::getNano,
                DateTimeFormatter.ISO_OFFSET_DATE_TIME);
    }

    protected OffsetDateTimeSerializer(OffsetDateTimeSerializer base,
            Boolean useTimestamp, DateTimeFormatter formatter,
            JsonFormat.Shape shape) {
        this(base, formatter, useTimestamp, base._useNanoseconds, shape);
    }

    protected OffsetDateTimeSerializer(OffsetDateTimeSerializer base,
            DateTimeFormatter formatter,
            Boolean useTimestamp, Boolean useNanoseconds,
            JsonFormat.Shape shape) {
        super(base, formatter, useTimestamp, useNanoseconds, shape);
    }

    @Override
    protected JSR310FormattedSerializerBase<?> withFormat(DateTimeFormatter formatter,
            Boolean useTimestamp,
            JsonFormat.Shape shape)
    {
        return new OffsetDateTimeSerializer(this, useTimestamp, formatter, shape);
    }

    @Override
    protected JSR310FormattedSerializerBase<?> withFeatures(Boolean writeZoneId, Boolean writeNanoseconds) {
        return new OffsetDateTimeSerializer(this, _formatter,
                _useTimestamp, writeNanoseconds, _shape);
    }
}
