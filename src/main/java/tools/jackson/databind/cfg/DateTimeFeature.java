package tools.jackson.databind.cfg;

/**
 * Configurable on/off features to configure Date/Time handling.
 * Mostly used to configure
 * Java 8 Time ({@code java.time}) type handling (see
 * {@link tools.jackson.databind.ext.javatime.JavaTimeInitializer})
 * but also to "legacy" ({@link java.util.Date}, {@link java.util.Calendar})
 * and Joda Date/Time.
 */
public enum DateTimeFeature implements DatatypeFeature
{
    /**
     * Feature that controls whether stringified numbers (Strings that without
     * quotes would be legal JSON Numbers) may be interpreted as
     * timestamps (enabled) or not (disabled), in case where there is an
     * explicitly defined pattern ({@code DateTimeFormatter}, usually by
     * using {@code @JsonFormat} annotation) for value.
     * <p>
     * Note that when the default pattern is used (no custom pattern defined),
     * stringified numbers are always accepted as timestamps regardless of
     * this feature.
     */
    ALWAYS_ALLOW_STRINGIFIED_DATE_TIMESTAMPS(false),

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
     * Feature that determines whether {@link java.time.Month} is serialized as
     * and deserialized expected a zero- ({@code false}) or one-based index ({@code true}).
     * For example, {@code Month.JANUARY} would be serialized as {@code 1} when enabled
     * but as {@code 0} when disabled.
     * Conversely JSON Number {@code 1} would be deserialized as {@code Month.JANUARY}
     * when enabled, but as {@code Month.FEBRUARY} when disabled.
     *<p>
     * Default setting is {@code true}, meaning that Month is serialized using one-based index
     * and deserialized expecting one-based index.
     *<p>
     * NOTE: default setting changed between Jackson 2.x (was {@code false}) and Jackson
     * 3.0 (changed to {@code true}).
     */
    ONE_BASED_MONTHS(true),

    /**
     * Feature that determines whether the {@link java.util.TimeZone} of the
     * {@link tools.jackson.databind.DeserializationContext} is used
     * when leniently deserializing {@link java.time.LocalDate} or
     * {@link java.time.LocalDateTime} from the UTC/ISO instant format.
     * <p>
     * Default setting is disabled, for backwards-compatibility with
     * Jackson 2.18.
     */
    USE_TIME_ZONE_FOR_LENIENT_DATE_PARSING(false)

    ;

    private final static int FEATURE_INDEX = DatatypeFeatures.FEATURE_INDEX_DATETIME;

    /**
     * Whether feature is enabled or disabled by default.
     */
    private final boolean _enabledByDefault;

    private final int _mask;

    private DateTimeFeature(boolean enabledByDefault) {
        _enabledByDefault = enabledByDefault;
        _mask = (1 << ordinal());
    }

    @Override
    public boolean enabledByDefault() { return _enabledByDefault; }
    @Override
    public boolean enabledIn(int flags) { return (flags & _mask) != 0; }
    @Override
    public int getMask() { return _mask; }

    @Override
    public int featureIndex() {
        return FEATURE_INDEX;
    }
}
