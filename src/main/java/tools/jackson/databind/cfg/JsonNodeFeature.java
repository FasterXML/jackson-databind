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
    STRIP_TRAILING_BIGDECIMAL_ZEROES(false)
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
