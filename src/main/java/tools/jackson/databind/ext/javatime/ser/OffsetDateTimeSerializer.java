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

    /**
     * Method for constructing a new {@code OffsetDateTimeSerializer} with settings
     * of this serializer but with custom {@link DateTimeFormatter} overrides.
     * Commonly used on {@code INSTANCE} like so:
     *<pre>
     *  DateTimeFormatter dtf = new DateTimeFormatterBuilder()
     *          .append(DateTimeFormatter.ISO_LOCAL_DATE)
     *          .appendLiteral('T')
     *          // and so on
     *          .toFormatter();
     *  OffsetDateTimeSerializer ser = OffsetDateTimeSerializer.INSTANCE
     *          .withFormatter(dtf);
     *  // register via Module
     *</pre>
     *
     * @since 2.21 / 3.1
     */
    public OffsetDateTimeSerializer withFormatter(DateTimeFormatter formatter)     {
        return new OffsetDateTimeSerializer(this, _useTimestamp, formatter, _shape);
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
