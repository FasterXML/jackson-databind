package tools.jackson.databind.ext.javatime.ser;

import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;

/**
 * Container for the ISO-8601 {@link DateTimeFormatter}s used in place of the
 * JDK-provided defaults when
 * {@link tools.jackson.databind.cfg.DateTimeFeature#ALWAYS_WRITE_SUBSECOND_DIGITS}
 * is enabled.
 *<p>
 * These differ from the JDK counterparts only in the sub-second field: instead of
 * omitting it when zero, at least 3 (millisecond) digits are always written, and up
 * to 9 when the value carries higher precision (so nothing is truncated).
 *
 * @since 3.3
 */
final class SubSecondFormatters
{
    private SubSecondFormatters() { }

    /**
     * Date and time down to the seconds field, followed by 3 to 9 sub-second digits:
     * the shared prefix of all formatters here.
     */
    private static DateTimeFormatterBuilder _localDateTimeBuilder() {
        return new DateTimeFormatterBuilder()
                .append(DateTimeFormatter.ISO_LOCAL_DATE)
                .appendLiteral('T')
                .appendValue(ChronoField.HOUR_OF_DAY, 2)
                .appendLiteral(':')
                .appendValue(ChronoField.MINUTE_OF_HOUR, 2)
                .appendLiteral(':')
                .appendValue(ChronoField.SECOND_OF_MINUTE, 2)
                .appendFraction(ChronoField.NANO_OF_SECOND, 3, 9, true);
    }

    /**
     * Counterpart of {@link DateTimeFormatter#ISO_LOCAL_DATE_TIME}.
     */
    static final DateTimeFormatter LOCAL_DATE_TIME = _localDateTimeBuilder()
            .toFormatter();

    /**
     * Counterpart of {@link DateTimeFormatter#ISO_OFFSET_DATE_TIME}.
     */
    static final DateTimeFormatter OFFSET_DATE_TIME = _localDateTimeBuilder()
            .appendOffsetId()
            .toFormatter();

    /**
     * Counterpart of {@link DateTimeFormatter#ISO_ZONED_DATE_TIME}, that is,
     * {@link #OFFSET_DATE_TIME} with the optional {@code [Zone/Id]} suffix.
     */
    static final DateTimeFormatter ZONED_DATE_TIME = new DateTimeFormatterBuilder()
            .append(OFFSET_DATE_TIME)
            .optionalStart()
            .appendLiteral('[')
            .parseCaseSensitive()
            .appendZoneRegionId()
            .appendLiteral(']')
            .toFormatter();

    /**
     * Counterpart of {@link DateTimeFormatter#ISO_INSTANT} (and of
     * {@link java.time.Instant#toString()}, which is what the default
     * {@code Instant} serialization actually uses): UTC-based, so the
     * offset is always rendered as {@code Z}.
     */
    static final DateTimeFormatter INSTANT = OFFSET_DATE_TIME
            .withZone(ZoneOffset.UTC);
}
