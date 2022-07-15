package tools.jackson.databind.cfg;

/**
 * New Datatype-specific features added in Jackson 2.14.
 *
 * @since 2.14
 */
public enum JsonNodeFeature implements DatatypeFeature
{
    // // // Direct Read/Write configuration settings

    /**
     * When reading {@code JsonNode}s are null valued properties included as explicit
     * {@code NullNode}s in resulting {@code ObjectNode} or skipped?
     *<p>
     * Default value: {@code true}
     */
    READ_NULL_PROPERTIES(true),

    /**
     * When writing {@code JsonNode}s are null valued properties written as explicit
     * JSON {@code null}s ior skipped?
     *<p>
     * Default value: {@code true}
     */
    WRITE_NULL_PROPERTIES(true),

    // // // Merge configuration settings

    /**
     * 
     */
    ALLOW_ARRAY_MERGE(true),

    /**
     * 
     */
    ALLOW_OBJECT_MERGE(true),
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
