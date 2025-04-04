package tools.jackson.databind.ext.datetime;

import tools.jackson.core.util.JacksonFeature;

/**
 * Configurable on/off features for Java 8 Date/Time module ({@link JavaTimeInitializer}).
 */
public enum JavaTimeFeature implements JacksonFeature
{
    /**
     * Feature that determines whether {@link java.time.ZoneId} is normalized
     * (via call to {@code java.time.ZoneId#normalized()}) when deserializing
     * types like {@link java.time.ZonedDateTime}.
     *<p>
     * Default setting is enabled, for backwards-compatibility with
     * Jackson 2.15.
     */
    NORMALIZE_DESERIALIZED_ZONE_ID(true),

    /**
     * Feature that determines whether the {@link java.util.TimeZone} of the
     * {@link tools.jackson.databind.DeserializationContext} is used
     * when leniently deserializing {@link java.time.LocalDate} or
     * {@link java.time.LocalDateTime} from the UTC/ISO instant format.
     * <p>
     * Default setting is disabled, for backwards-compatibility with
     * Jackson 2.18.
     *
     * @since 2.19
     */
    USE_TIME_ZONE_FOR_LENIENT_DATE_PARSING(false),

    /**
     * Feature that controls whether stringified numbers (Strings that without
     * quotes would be legal JSON Numbers) may be interpreted as
     * timestamps (enabled) or not (disabled), in case where there is an
     * explicitly defined pattern ({@code DateTimeFormatter}) for value.
     * <p>
     * Note that when the default pattern is used (no custom pattern defined),
     * stringified numbers are always accepted as timestamps regardless of
     * this feature.
     */
    ALWAYS_ALLOW_STRINGIFIED_DATE_TIMESTAMPS(false),

    /**
     * Feature that determines whether {@link java.time.Month} is serialized
     * and deserialized as using a zero-based index (FALSE) or a one-based index (TRUE).
     * For example, "1" would be serialized/deserialized as Month.JANUARY if TRUE and Month.FEBRUARY if FALSE.
     *<p>
     * Default setting is false, meaning that Month is serialized/deserialized as a zero-based index.
     */
    ONE_BASED_MONTHS(false)
    ;

    /**
     * Whether feature is enabled or disabled by default.
     */
    private final boolean _defaultState;

    private final int _mask;

    JavaTimeFeature(boolean enabledByDefault) {
        _defaultState = enabledByDefault;
        _mask = (1 << ordinal());
    }

    @Override
    public boolean enabledByDefault() { return _defaultState; }

    @Override
    public boolean enabledIn(int flags) { return (flags & _mask) != 0; }

    @Override
    public int getMask() { return _mask; }
}
