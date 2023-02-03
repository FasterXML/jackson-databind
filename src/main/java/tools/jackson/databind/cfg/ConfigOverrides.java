package tools.jackson.databind.cfg;

import java.util.*;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonSetter;

import tools.jackson.core.util.Snapshottable;
import tools.jackson.databind.introspect.VisibilityChecker;

/**
 * Container for individual {@link ConfigOverride} values.
 */
public class ConfigOverrides
    implements java.io.Serializable,
        Snapshottable<ConfigOverrides>
{
    private static final long serialVersionUID = 3L;

    /**
     * Convenience value used as the default root setting.
     * Note that although in a way it would make sense use "ALWAYS" for both,
     * problems arise in some cases where default is seen as explicit setting,
     * overriding possible per-class annotation; hence use of "USE_DEFAULTS".
     *
     * @since 3.0
     */
    final static JsonInclude.Value INCLUDE_DEFAULT // non-private for test access
        = JsonInclude.Value.construct(JsonInclude.Include.USE_DEFAULTS, JsonInclude.Include.USE_DEFAULTS);

    private final static VisibilityChecker DEFAULT_VISIBILITY_CHECKER
        = VisibilityChecker.defaultInstance();

    private final static VisibilityChecker DEFAULT_RECORD_VISIBILITY_CHECKER
        = VisibilityChecker.allPublicExceptCreatorsInstance();

    /**
     * Per-type override definitions
     */
    protected Map<Class<?>, MutableConfigOverride> _overrides;

    // // // Global defaulting

    protected JsonInclude.Value _defaultInclusion;

    protected JsonSetter.Value _defaultNullHandling;

    protected VisibilityChecker _visibilityChecker;

    protected Boolean _defaultMergeable;

    protected Boolean _defaultLeniency;

    /*
    /**********************************************************************
    /* Life cycle
    /**********************************************************************
     */

    public ConfigOverrides() {
        this(null,
                INCLUDE_DEFAULT,
                JsonSetter.Value.empty(),
                DEFAULT_VISIBILITY_CHECKER,
                null, null
        );
    }

    protected ConfigOverrides(Map<Class<?>, MutableConfigOverride> overrides,
            JsonInclude.Value defIncl, JsonSetter.Value defSetter,
            VisibilityChecker defVisibility,
            Boolean defMergeable, Boolean defLeniency) {
        _overrides = overrides;
        _defaultInclusion = defIncl;
        _defaultNullHandling = defSetter;
        _visibilityChecker = defVisibility;
        _defaultMergeable = defMergeable;
        _defaultLeniency = defLeniency;
    }

    @Override
    public ConfigOverrides snapshot()
    {
        Map<Class<?>, MutableConfigOverride> newOverrides;
        if (_overrides == null) {
            newOverrides = null;
        } else {
            newOverrides = _newMap();
            for (Map.Entry<Class<?>, MutableConfigOverride> entry : _overrides.entrySet()) {
                newOverrides.put(entry.getKey(), entry.getValue().copy());
            }
        }
        return new ConfigOverrides(newOverrides,
                _defaultInclusion, _defaultNullHandling, _visibilityChecker,
                _defaultMergeable, _defaultLeniency);
    }

    /*
    /**********************************************************************
    /* Per-type override access
    /**********************************************************************
     */

    public ConfigOverride findOverride(Class<?> type) {
        if (_overrides == null) {
            return null;
        }
        return _overrides.get(type);
    }

    public MutableConfigOverride findOrCreateOverride(Class<?> type) {
        if (_overrides == null) {
            _overrides = _newMap();
        }
        MutableConfigOverride override = _overrides.get(type);
        if (override == null) {
            override = new MutableConfigOverride();
            _overrides.put(type, override);
        }
        return override;
    }

    /**
     * Specific accessor for finding {code JsonFormat.Value} for given type,
     * considering global default for leniency as well as per-type format
     * override (if any).
     *
     * @return Default format settings for type; never null.
     *
     * @since 2.10
     */
    public JsonFormat.Value findFormatDefaults(Class<?> type) {
        if (_overrides != null) {
            ConfigOverride override = _overrides.get(type);
            if (override != null) {
                JsonFormat.Value format = override.getFormat();
                if (format != null) {
                    if (!format.hasLenient()) {
                        return format.withLenient(_defaultLeniency);
                    }
                    return format;
                }
            }
        }
        if (_defaultLeniency == null) {
            return JsonFormat.Value.empty();
        }
        return JsonFormat.Value.forLeniency(_defaultLeniency);
    }

    /*
    /**********************************************************************
    /* Global defaults accessors
    /**********************************************************************
     */

    public JsonInclude.Value getDefaultInclusion() {
        return _defaultInclusion;
    }

    public JsonSetter.Value getDefaultNullHandling() {
        return _defaultNullHandling;
    }

    public Boolean getDefaultMergeable() {
        return _defaultMergeable;
    }

    public Boolean getDefaultLeniency() {
        return _defaultLeniency;
    }

    public VisibilityChecker getDefaultVisibility() {
        return _visibilityChecker;
    }

    /**
     * Alternate accessor needed due to complexities of Record
     * auto-discovery: needs to obey custom overrides but also
     * give alternate "default default" if no customizations made.
     *
     * @since 3.0
     */
    public VisibilityChecker getDefaultRecordVisibility() {
        // Records only use default if it has been explicitly overridden to
        // settings other than original settings
        return (DEFAULT_VISIBILITY_CHECKER.equals(_visibilityChecker))
                ? DEFAULT_RECORD_VISIBILITY_CHECKER
                : _visibilityChecker;
    }

    /*
    /**********************************************************************
    /* Global defaults mutators
    /**********************************************************************
     */

    public ConfigOverrides setDefaultInclusion(JsonInclude.Value v) {
        _defaultInclusion = v;
        return this;
    }

    public ConfigOverrides setDefaultNullHandling(JsonSetter.Value v) {
        _defaultNullHandling = v;
        return this;
    }

    public ConfigOverrides setDefaultMergeable(Boolean b) {
        _defaultMergeable = b;
        return this;
    }

    public ConfigOverrides setDefaultLeniency(Boolean b) {
        _defaultLeniency = b;
        return this;
    }

    public ConfigOverrides setDefaultVisibility(VisibilityChecker v) {
        _visibilityChecker = v;
        return this;
    }

    public ConfigOverrides setDefaultVisibility(JsonAutoDetect.Value vis) {
        _visibilityChecker = VisibilityChecker.construct(vis);
        return this;
    }

    /*
    /**********************************************************************
    /* Standard methods (for diagnostics)
    /**********************************************************************
     */

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("[ConfigOverrides ")
                .append("incl=").append(_defaultInclusion)
                .append(", nulls=").append(_defaultNullHandling)
                .append(", merge=").append(_defaultMergeable)
                .append(", leniency=").append(_defaultLeniency)
                .append(", visibility=").append(_visibilityChecker)
                .append(", typed=")
                ;
        if (_overrides == null) {
            sb.append("NLLL");
        } else {
            sb.append("(").append(_overrides.size()).append("){");
            TreeMap<String, MutableConfigOverride> sorted = new TreeMap<>();
            _overrides.forEach((k, v) -> sorted.put(k.getName(), v));
            sorted.forEach((k, v) -> {
                sb.append(String.format("'%s'->%s", k, v));
            });
            sb.append("}");
        }
        return sb.append("]").toString();
    }

    /*
    /**********************************************************************
    /* Helper methods
    /**********************************************************************
     */

    protected Map<Class<?>, MutableConfigOverride> _newMap() {
        return new HashMap<Class<?>, MutableConfigOverride>();
    }
}
