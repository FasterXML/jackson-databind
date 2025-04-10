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
    USE_TIME_ZONE_FOR_LENIENT_DATE_PARSING(false),

    /**
     * Feature that determines whether Date (and date/time) values
     * (and Date-based things like {@link java.util.Calendar}s) are to be
     * serialized as numeric time stamps (true; the default),
     * or as something else (usually textual representation).
     * If textual representation is used, the actual format depends on configuration
     * settings including possible per-property use of {@code @JsonFormat} annotation,
     * globally configured {@link java.text.DateFormat}.
     *<p>
     * For "classic" JDK date types ({@link java.util.Date}, {@link java.util.Calendar})
     * the default formatting is provided by {@link tools.jackson.databind.util.StdDateFormat},
     * and corresponds to format String of "yyyy-MM-dd'T'HH:mm:ss.SSSX"
     * (see {@link java.text.DateFormat} for details of format Strings).
     * Whether this feature affects handling of other date-related
     * types depend on handlers of those types, although ideally they
     * should use this feature
     *<p>
     * Note: whether {@link java.util.Map} keys are serialized as Strings
     * or not is controlled using {@link #WRITE_DATE_KEYS_AS_TIMESTAMPS} instead of
     * this feature.
     *<p>
     * Feature used to be one of {@link tools.jackson.databind.SerializationFeature}s
     * in Jackson 2.x but was moved here in 3.0.
     *<p>
     * Feature is disabled by default as of Jackson 3.0 (in 2.x it was enabled),
     * so that date/time are by default serialized as textual values NOT timestamps.
     */
    WRITE_DATES_AS_TIMESTAMPS(false),

    /**
     * Feature that determines whether {@link java.util.Date}s
     * (and sub-types) used as {@link java.util.Map} keys are serialized
     * as time stamps or not (if not, will be serialized as textual values).
     *<p>
     * Feature used to be one of {@link tools.jackson.databind.SerializationFeature}s
     * in Jackson 2.x but was moved here in 3.0.
     *<p>
     * Feature is disabled by default, meaning that Date-valued Map keys are serialized
     * as textual (ISO-8601) values.
     */
    WRITE_DATE_KEYS_AS_TIMESTAMPS(false),

    /**
     * Feature that determines whether time values that represents time periods
     * (durations, periods, ranges) are to be serialized by default using
     * a numeric (true) or textual (false) representations. Note that numeric
     * representation may mean either simple number, or an array of numbers,
     * depending on type.
     *<p>
     * Feature used to be one of {@link tools.jackson.databind.SerializationFeature}s
     * in Jackson 2.x but was moved here in 3.0.
     *<p>
     * Feature is disabled by default as of Jackson 3.0 (in 2.x it was enabled),
     * so that period/duration are by default serialized as textual values,
     * NOT timestamps.
     */
    WRITE_DURATIONS_AS_TIMESTAMPS(false),
    
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
