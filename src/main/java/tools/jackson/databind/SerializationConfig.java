package tools.jackson.databind;

import java.text.DateFormat;

import tools.jackson.core.*;
import tools.jackson.core.util.Instantiatable;
import tools.jackson.databind.cfg.*;
import tools.jackson.databind.introspect.ClassIntrospector;
import tools.jackson.databind.introspect.MixInHandler;
import tools.jackson.databind.jsontype.SubtypeResolver;
import tools.jackson.databind.ser.FilterProvider;
import tools.jackson.databind.ser.SerializerFactory;
import tools.jackson.databind.type.TypeFactory;
import tools.jackson.databind.util.RootNameLookup;

/**
 * Object that contains baseline configuration for serialization
 * process. An instance is owned by {@link ObjectMapper}, which
 * passes an immutable instance for serialization process to
 * {@link SerializerProvider} and {@link SerializerFactory}
 * (either directly, or through {@link ObjectWriter}.
 *<p>
 * Note that instances are considered immutable and as such no copies
 * should need to be created for sharing; all copying is done with
 * "fluent factory" methods.
 */
public final class SerializationConfig
    extends MapperConfigBase<SerializationFeature, SerializationConfig>
    implements java.io.Serializable
{
    private static final long serialVersionUID = 3L;

    /*
    /**********************************************************************
    /* Configured helper objects
    /**********************************************************************
     */

    /**
     * Object used for resolving filter ids to filter instances.
     * Non-null if explicitly defined; null by default.
     */
    protected final FilterProvider _filterProvider;

    /**
     * If "default pretty-printing" is enabled, it will create the instance
     * from this blueprint object.
     */
    protected final PrettyPrinter _defaultPrettyPrinter;

    /*
    /**********************************************************************
    /* Feature flags
    /**********************************************************************
     */

    /**
     * Set of {@link SerializationFeature}s enabled.
     */
    protected final int _serFeatures;

    /**
     * States of {@link tools.jackson.core.StreamWriteFeature}s to enable/disable.
     */
    protected final int _streamWriteFeatures;

    /**
     * States of {@link tools.jackson.core.FormatFeature}s to enable/disable.
     */
    protected final int _formatWriteFeatures;

    /*
    /**********************************************************************
    /* Life-cycle, primary constructors for new instances
    /**********************************************************************
     */

    /**
     * @since 3.0
     */
    public SerializationConfig(MapperBuilder<?,?> b,
            long mapperFeatures, int serFeatures, int streamWriteFeatures, int formatWriteFeatures,
            ConfigOverrides configOverrides,
            TypeFactory tf, ClassIntrospector classIntr, MixInHandler mixins, SubtypeResolver str,
            ContextAttributes defaultAttrs, RootNameLookup rootNames,
            FilterProvider filterProvider)
    {
        super(b, mapperFeatures, tf, classIntr, mixins, str, configOverrides,
                defaultAttrs, rootNames);
        _serFeatures = serFeatures;
        _filterProvider = filterProvider;
        _streamWriteFeatures = streamWriteFeatures;
        _formatWriteFeatures = formatWriteFeatures;
        _defaultPrettyPrinter = b.defaultPrettyPrinter();
    }

    /*
    /**********************************************************************
    /* Life-cycle, secondary constructors to support
    /* "mutant factories", with single property changes
    /**********************************************************************
     */

    private SerializationConfig(SerializationConfig src,
            int serFeatures, int streamWriteFeatures, int formatWriteFeatures)
    {
        super(src);
        _serFeatures = serFeatures;
        _filterProvider = src._filterProvider;
        _defaultPrettyPrinter = src._defaultPrettyPrinter;
        _streamWriteFeatures = streamWriteFeatures;
        _formatWriteFeatures = formatWriteFeatures;
    }

    private SerializationConfig(SerializationConfig src, BaseSettings base)
    {
        super(src, base);
        _serFeatures = src._serFeatures;
        _filterProvider = src._filterProvider;
        _defaultPrettyPrinter = src._defaultPrettyPrinter;
        _streamWriteFeatures = src._streamWriteFeatures;
        _formatWriteFeatures = src._formatWriteFeatures;
    }

    private SerializationConfig(SerializationConfig src, FilterProvider filters)
    {
        super(src);
        _serFeatures = src._serFeatures;
        _filterProvider = filters;
        _defaultPrettyPrinter = src._defaultPrettyPrinter;
        _streamWriteFeatures = src._streamWriteFeatures;
        _formatWriteFeatures = src._formatWriteFeatures;
    }

    private SerializationConfig(SerializationConfig src, Class<?> view)
    {
        super(src, view);
        _serFeatures = src._serFeatures;
        _filterProvider = src._filterProvider;
        _defaultPrettyPrinter = src._defaultPrettyPrinter;
        _streamWriteFeatures = src._streamWriteFeatures;
        _formatWriteFeatures = src._formatWriteFeatures;
    }

    private SerializationConfig(SerializationConfig src, PropertyName rootName)
    {
        super(src, rootName);
        _serFeatures = src._serFeatures;
        _filterProvider = src._filterProvider;
        _defaultPrettyPrinter = src._defaultPrettyPrinter;
        _streamWriteFeatures = src._streamWriteFeatures;
        _formatWriteFeatures = src._formatWriteFeatures;
    }

    protected SerializationConfig(SerializationConfig src, ContextAttributes attrs)
    {
        super(src, attrs);
        _serFeatures = src._serFeatures;
        _filterProvider = src._filterProvider;
        _defaultPrettyPrinter = src._defaultPrettyPrinter;
        _streamWriteFeatures = src._streamWriteFeatures;
        _formatWriteFeatures = src._formatWriteFeatures;
    }

    protected SerializationConfig(SerializationConfig src, PrettyPrinter defaultPP)
    {
        super(src);
        _serFeatures = src._serFeatures;
        _filterProvider = src._filterProvider;
        _defaultPrettyPrinter = defaultPP;
        _streamWriteFeatures = src._streamWriteFeatures;
        _formatWriteFeatures = src._formatWriteFeatures;
    }

    protected SerializationConfig(SerializationConfig src, DatatypeFeatures dtFeatures)
    {
        super(src, dtFeatures);
        _serFeatures = src._serFeatures;
        _filterProvider = src._filterProvider;
        _defaultPrettyPrinter = src._defaultPrettyPrinter;
        _streamWriteFeatures = src._streamWriteFeatures;
        _formatWriteFeatures = src._formatWriteFeatures;
    }

    /*
    /**********************************************************************
    /* Life-cycle, factory methods from MapperConfig(Base)
    /**********************************************************************
     */

    @Override
    protected final SerializationConfig _withBase(BaseSettings newBase) {
        return (_base == newBase) ? this : new SerializationConfig(this, newBase);
    }

    @Override
    protected final SerializationConfig _with(DatatypeFeatures dtFeatures) {
        return new SerializationConfig(this, dtFeatures);
    }

    @Override
    public SerializationConfig withRootName(PropertyName rootName) {
        if (rootName == null) {
            if (_rootName == null) {
                return this;
            }
        } else if (rootName.equals(_rootName)) {
            return this;
        }
        return new SerializationConfig(this, rootName);
    }

    @Override
    public SerializationConfig withView(Class<?> view) {
        return (_view == view) ? this : new SerializationConfig(this, view);
    }

    @Override
    public SerializationConfig with(ContextAttributes attrs) {
        return (attrs == _attributes) ? this : new SerializationConfig(this, attrs);
    }

    /*
    /**********************************************************************
    /* Factory method overrides
    /**********************************************************************
     */

    /**
     * In addition to constructing instance with specified date format,
     * will enable or disable <code>SerializationFeature.WRITE_DATES_AS_TIMESTAMPS</code>
     * (enable if format set as null; disable if non-null)
     */
    @Override
    public SerializationConfig with(DateFormat df) {
        SerializationConfig cfg = super.with(df);
        // Also need to toggle this feature based on existence of date format:
        if (df == null) {
            return cfg.with(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        }
        return cfg.without(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    /*
    /**********************************************************************
    /* Factory methods for SerializationFeature
    /**********************************************************************
     */

    /**
     * Fluent factory method that will construct and return a new configuration
     * object instance with specified feature enabled.
     */
    public SerializationConfig with(SerializationFeature feature)
    {
        int newSerFeatures = _serFeatures | feature.getMask();
        return (newSerFeatures == _serFeatures) ? this
                : new SerializationConfig(this,
                        newSerFeatures, _streamWriteFeatures, _formatWriteFeatures);
    }

    /**
     * Fluent factory method that will construct and return a new configuration
     * object instance with specified features enabled.
     */
    public SerializationConfig with(SerializationFeature first, SerializationFeature... features)
    {
        int newSerFeatures = _serFeatures | first.getMask();
        for (SerializationFeature f : features) {
            newSerFeatures |= f.getMask();
        }
        return (newSerFeatures == _serFeatures) ? this
                : new SerializationConfig(this,
                        newSerFeatures, _streamWriteFeatures, _formatWriteFeatures);
    }

    /**
     * Fluent factory method that will construct and return a new configuration
     * object instance with specified features enabled.
     */
    public SerializationConfig withFeatures(SerializationFeature... features)
    {
        int newSerFeatures = _serFeatures;
        for (SerializationFeature f : features) {
            newSerFeatures |= f.getMask();
        }
        return (newSerFeatures == _serFeatures) ? this
                : new SerializationConfig(this,
                        newSerFeatures, _streamWriteFeatures, _formatWriteFeatures);
    }

    /**
     * Fluent factory method that will construct and return a new configuration
     * object instance with specified feature disabled.
     */
    public SerializationConfig without(SerializationFeature feature)
    {
        int newSerFeatures = _serFeatures & ~feature.getMask();
        return (newSerFeatures == _serFeatures) ? this
                : new SerializationConfig(this,
                        newSerFeatures, _streamWriteFeatures,  _formatWriteFeatures);
    }

    /**
     * Fluent factory method that will construct and return a new configuration
     * object instance with specified features disabled.
     */
    public SerializationConfig without(SerializationFeature first, SerializationFeature... features)
    {
        int newSerFeatures = _serFeatures & ~first.getMask();
        for (SerializationFeature f : features) {
            newSerFeatures &= ~f.getMask();
        }
        return (newSerFeatures == _serFeatures) ? this
                : new SerializationConfig(this, newSerFeatures,
                        _streamWriteFeatures, _formatWriteFeatures);
    }

    /**
     * Fluent factory method that will construct and return a new configuration
     * object instance with specified features disabled.
     */
    public SerializationConfig withoutFeatures(SerializationFeature... features)
    {
        int newSerFeatures = _serFeatures;
        for (SerializationFeature f : features) {
            newSerFeatures &= ~f.getMask();
        }
        return (newSerFeatures == _serFeatures) ? this
                : new SerializationConfig(this, newSerFeatures,
                        _streamWriteFeatures, _formatWriteFeatures);
    }

    /*
    /**********************************************************************
    /* Factory methods for StreamWriteFeature
    /**********************************************************************
     */

    /**
     * Fluent factory method that will construct and return a new configuration
     * object instance with specified feature enabled.
     */
    public SerializationConfig with(StreamWriteFeature feature)
    {
        int newSet = _streamWriteFeatures | feature.getMask();
        return (_streamWriteFeatures == newSet) ? this :
            new SerializationConfig(this, _serFeatures, newSet,
                    _formatWriteFeatures);
    }

    /**
     * Fluent factory method that will construct and return a new configuration
     * object instance with specified features enabled.
     */
    public SerializationConfig withFeatures(StreamWriteFeature... features)
    {
        int newSet = _streamWriteFeatures;
        for (StreamWriteFeature f : features) {
            newSet |= f.getMask();
        }
        return (_streamWriteFeatures == newSet) ? this :
            new SerializationConfig(this, _serFeatures, newSet,
                    _formatWriteFeatures);
    }

    /**
     * Fluent factory method that will construct and return a new configuration
     * object instance with specified feature disabled.
     */
    public SerializationConfig without(StreamWriteFeature feature)
    {
        int newSet = _streamWriteFeatures & ~feature.getMask();
        return (_streamWriteFeatures == newSet) ? this :
            new SerializationConfig(this, _serFeatures, newSet,
                    _formatWriteFeatures);
    }

    /**
     * Fluent factory method that will construct and return a new configuration
     * object instance with specified features disabled.
     */
    public SerializationConfig withoutFeatures(StreamWriteFeature... features)
    {
        int newSet = _streamWriteFeatures;
        for (StreamWriteFeature f : features) {
            newSet &= ~f.getMask();
        }
        return (_streamWriteFeatures == newSet) ? this :
            new SerializationConfig(this, _serFeatures, newSet,
                    _formatWriteFeatures);
    }

    /*
    /**********************************************************************
    /* Factory methods for FormatFeature
    /**********************************************************************
     */

    /**
     * Fluent factory method that will construct and return a new configuration
     * object instance with specified feature enabled.
     */
    public SerializationConfig with(FormatFeature feature)
    {
        int newSet = _formatWriteFeatures | feature.getMask();
        return (_formatWriteFeatures == newSet) ? this :
            new SerializationConfig(this,
                    _serFeatures, _streamWriteFeatures, newSet);
    }

    /**
     * Fluent factory method that will construct and return a new configuration
     * object instance with specified features enabled.
     */
    public SerializationConfig withFeatures(FormatFeature... features)
    {
        int newSet = _formatWriteFeatures;
        for (FormatFeature f : features) {
            newSet |= f.getMask();
        }
        return (_formatWriteFeatures == newSet) ? this :
            new SerializationConfig(this,
                    _serFeatures, _streamWriteFeatures, newSet);
    }

    /**
     * Fluent factory method that will construct and return a new configuration
     * object instance with specified feature disabled.
     */
    public SerializationConfig without(FormatFeature feature)
    {
        int newSet = _formatWriteFeatures & ~feature.getMask();
        return (_formatWriteFeatures == newSet) ? this :
            new SerializationConfig(this,
                    _serFeatures, _streamWriteFeatures, newSet);
    }

    /**
     * Fluent factory method that will construct and return a new configuration
     * object instance with specified features disabled.
     */
    public SerializationConfig withoutFeatures(FormatFeature... features)
    {
        int newSet = _formatWriteFeatures;
        for (FormatFeature f : features) {
            newSet &= ~f.getMask();
        }
        return (_formatWriteFeatures == newSet) ? this :
            new SerializationConfig(this, _serFeatures, _streamWriteFeatures, newSet);
    }

    /*
    /**********************************************************************
    /* Factory methods, other
    /**********************************************************************
     */

    public SerializationConfig withFilters(FilterProvider filterProvider) {
        return (filterProvider == _filterProvider) ? this : new SerializationConfig(this, filterProvider);
    }

    public SerializationConfig withDefaultPrettyPrinter(PrettyPrinter pp) {
        return (_defaultPrettyPrinter == pp) ? this:  new SerializationConfig(this, pp);
    }

    /*
    /**********************************************************************
    /* Factories for objects configured here
    /**********************************************************************
     */

    public PrettyPrinter constructDefaultPrettyPrinter() {
        PrettyPrinter pp = _defaultPrettyPrinter;
        if (pp instanceof Instantiatable<?>) {
            pp = (PrettyPrinter) ((Instantiatable<?>) pp).createInstance();
        }
        return pp;
    }

    /*
    /**********************************************************************
    /* Support for ObjectWriteContext
    /**********************************************************************
     */

    /**
     * @since 3.0
     */
    public int getStreamWriteFeatures() {
        return _streamWriteFeatures;
    }

    /**
     * @since 3.0
     */
    public int getFormatWriteFeatures() {
        return _formatWriteFeatures;
    }

    /*
    /**********************************************************************
    /* Configuration: other
    /**********************************************************************
     */

    @Override
    public boolean useRootWrapping()
    {
        if (_rootName != null) { // empty String disables wrapping; non-empty enables
            return !_rootName.isEmpty();
        }
        return isEnabled(SerializationFeature.WRAP_ROOT_VALUE);
    }

    /**
     * Accessor for checking whether give {@link SerializationFeature}
     * is enabled or not.
     *
     * @param feature Feature to check
     *
     * @return True if feature is enabled; false otherwise
     */
    public final boolean isEnabled(SerializationFeature feature) {
        return (_serFeatures & feature.getMask()) != 0;
    }

    /**
     * Accessor method that first checks if we have any overrides
     * for feature, and only if not, checks state of passed-in
     * factory.
     */
    public final boolean isEnabled(StreamWriteFeature f) {
        return (_streamWriteFeatures & f.getMask()) != 0;
    }

    public final boolean hasFormatFeature(FormatFeature f) {
        return (_formatWriteFeatures & f.getMask()) != 0;
    }

    /**
     * "Bulk" access method for checking that all features specified by
     * mask are enabled.
     */
    public final boolean hasSerializationFeatures(int featureMask) {
        return (_serFeatures & featureMask) == featureMask;
    }

    public final int getSerializationFeatures() {
        return _serFeatures;
    }

    /**
     * Method for getting provider used for locating filters given
     * id (which is usually provided with filter annotations).
     * Will be null if no provided was set for {@link ObjectWriter}
     * (or if serialization directly called from {@link ObjectMapper})
     */
    public FilterProvider getFilterProvider() {
        return _filterProvider;
    }

    /**
     * Accessor for configured blueprint "default" {@link PrettyPrinter} to
     * use, if default pretty-printing is enabled.
     *<p>
     * NOTE: returns the "blueprint" instance, and does NOT construct
     * an instance ready to use; call {@link #constructDefaultPrettyPrinter()} if
     * actually usable instance is desired.
     */
    public PrettyPrinter getDefaultPrettyPrinter() {
        return _defaultPrettyPrinter;
    }
}
