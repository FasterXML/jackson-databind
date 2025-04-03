package tools.jackson.databind.cfg;

/**
 * New Datatype-specific configuration options related to handling of
 * {@link tools.jackson.databind.JsonNode} types.
 */
public enum JsonNodeFeature implements DatatypeFeature
{
    // // // Direct Read/Write configuration settings

    /**
     * When reading {@link tools.jackson.databind.JsonNode}s are null valued properties included as explicit
     * {@code NullNode}s in resulting {@link tools.jackson.databind.node.ObjectNode}
     * or skipped?
     *<p>
     * Default value: {@code true}
     */
    READ_NULL_PROPERTIES(true),

    /**
     * When writing {@code JsonNode}s are null valued properties written as explicit
     * JSON {@code null}s or skipped?
     *<p>
     * Default value: {@code true}
     */
    WRITE_NULL_PROPERTIES(true),

    /**
     * When writing {@code com.fasterxml.jackson.databind.JsonNode}s are Object properties
     * (for {@code ObjectNode}s) sorted alphabetically (using natural order of
     * {@link java.lang.String}) or not?
     * If not sorted, order is the insertion order; when reading this also means retaining
     * order from the input document.
     *<p>
     * Default value: {@code false}
     */
    WRITE_PROPERTIES_SORTED(false),

    // // // Merge configuration settings

    // // // 03-Aug-2022, tatu: Possible other additions:

//    ALLOW_ARRAY_MERGE(true),

//    ALLOW_OBJECT_MERGE(true),

    // // // Misc other

    /**
     * Feature that determines whether {@link java.math.BigDecimal} values
     * will be "normalized" by stripping trailing zeroes off, when constructing
     * nodes with {@link tools.jackson.databind.node.JsonNodeFactory#numberNode(java.math.BigDecimal)}.
     * If enabled, {@link java.math.BigDecimal#stripTrailingZeros()} will be called
     * prior to node creation; if disabled, numeric value will be used as is.
     *<p>
     * Default value: {@code false} (since 3.0).
     */
    STRIP_TRAILING_BIGDECIMAL_ZEROES(false),

    /**
     * Determines the behavior when coercing `NaN` to {@link java.math.BigDecimal} with
     * {@link tools.jackson.databind.DeserializationFeature#USE_BIG_DECIMAL_FOR_FLOATS} enabled.
     *
     * 1. If set to {@code true}, will throw an {@link tools.jackson.databind.exc.InvalidFormatException} for
     * attempting to coerce {@code NaN} into {@link java.math.BigDecimal}.
     * 2. If set to {@code false}, will simply let coercing {@code NaN} into {@link java.math.BigDecimal} happen,
     * regardless of how such coercion will behave -- as of 2.16, will simply stay as {@code NaN} of original
     * floating-point type node.
     *
     * <p>
     * Default value is {@code false} for backwards-compatibility, but will most likely be changed to
     * {@code true} in 3.0.
     */
    FAIL_ON_NAN_TO_BIG_DECIMAL_COERCION(false),

    /**
     * Determines whether floating-point numbers should be deserialized into
     * {@link java.math.BigDecimal} when reading {@link tools.jackson.databind.JsonNode}s.
     * This feature provides more precise control over number deserialization for {@code JsonNode}
     * and takes precedence over {@link tools.jackson.databind.DeserializationFeature#USE_BIG_DECIMAL_FOR_FLOATS}
     * if explicitly set.
     *
     * <p>
     * Behavior follows these rules:
     * <ul>
     *   <li>If explicitly enabled, floating-point numbers will be read as {@link java.math.BigDecimal}.</li>
     *   <li>If explicitly disabled, floating-point numbers will be read as {@link java.lang.Double}.</li>
     *   <li>If left undefined (default), the behavior follows {@code DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS}.</li>
     * </ul>
     *
     * <p>
     * Default value is {@code false} but unless explicitly set, handling
     * depends on more general {@code DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS}).
     */
    USE_BIG_DECIMAL_FOR_FLOATS(false),
    ;

    private final static int FEATURE_INDEX = DatatypeFeatures.FEATURE_INDEX_JSON_NODE;

    /**
     * Whether feature is enabled or disabled by default.
     */
    private final boolean _enabledByDefault;

    private final int _mask;

    private JsonNodeFeature(boolean enabledByDefault) {
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
