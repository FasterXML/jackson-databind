package tools.jackson.databind.introspect;

import java.lang.reflect.Modifier;
import java.util.*;

import com.fasterxml.jackson.annotation.JacksonInject;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import tools.jackson.databind.*;
import tools.jackson.databind.cfg.ConstructorDetector;
import tools.jackson.databind.cfg.HandlerInstantiator;
import tools.jackson.databind.cfg.MapperConfig;
import tools.jackson.databind.deser.impl.UnwrappedPropertyHandler;
import tools.jackson.databind.util.ClassUtil;
import tools.jackson.databind.util.NameTransformer;
import tools.jackson.databind.util.RecordUtil;

/**
 * Helper class used for aggregating information about all possible
 * properties of a POJO.
 */
public class POJOPropertiesCollector
{
    /*
    /**********************************************************************
    /* Configuration
    /**********************************************************************
     */

    /**
     * Configuration settings
     */
    protected final MapperConfig<?> _config;

    /**
     * Handler used for name-mangling of getter, mutator (setter/with) methods
     */
    protected final AccessorNamingStrategy _accessorNaming;

    /**
     * True if introspection is done for serialization (giving
     * precedence for serialization annotations), or not (false, deserialization)
     */
    protected final boolean _forSerialization;

    /**
     * Type of POJO for which properties are being collected.
     */
    protected final JavaType _type;

    /**
     * Low-level introspected class information (methods, fields etc)
     */
    protected final AnnotatedClass _classDef;

    protected final VisibilityChecker _visibilityChecker;

    protected final AnnotationIntrospector _annotationIntrospector;

    protected final boolean _useAnnotations;

    protected final boolean _isRecordType;

    /*
    /**********************************************************************
    /* Collected property information
    /**********************************************************************
     */

    /**
     * State flag we keep to indicate whether actual property information
     * has been collected or not.
     */
    protected boolean _collected;

    /**
     * Set of logical property information collected so far.
     */
    protected LinkedHashMap<String, POJOPropertyBuilder> _properties;

    protected List<POJOPropertyBuilder> _creatorProperties;

    /**
     * @since 2.18
     */
    protected PotentialCreators _potentialCreators;

    /**
     * A set of "field renamings" that have been discovered, indicating
     * intended renaming of other accessors: key is the implicit original
     * name and value intended name to use instead.
     *<p>
     * Note that these renamings are applied earlier than "regular" (explicit)
     * renamings and affect implicit name: their effect may be changed by
     * further renaming based on explicit indicators.
     * The main use case is to effectively relink accessors based on fields
     * discovered, and used to sort of correct otherwise missing linkage between
     * fields and other accessors.
     */
    protected Map<PropertyName, PropertyName> _fieldRenameMappings;

    protected LinkedList<AnnotatedMember> _anyGetters;

    protected LinkedList<AnnotatedMember> _anyGetterField;

    protected LinkedList<AnnotatedMethod> _anySetters;

    protected LinkedList<AnnotatedMember> _anySetterField;

    /**
     * Accessors (field or "getter" method annotated with
     * {@link com.fasterxml.jackson.annotation.JsonKey}
     */
    protected LinkedList<AnnotatedMember> _jsonKeyAccessors;

    /**
     * Accessors (field or "getter" method) annotated with
     * {@link com.fasterxml.jackson.annotation.JsonValue}
     */
    protected LinkedList<AnnotatedMember> _jsonValueAccessors;

    /**
     * Per-property ignorals: names collected from {@code @JsonIgnore} markers and
     * read/write-only access rules via {@link #_collectIgnorals}.
     *<p>
     * Kept as a mutable set because {@link #_renameProperties} may remove entries
     * for {@code [databind#2001]} when a per-property ignoral is overridden by a
     * creator parameter renamed to the same name. The pre-rescue state is
     * captured into {@link #_nonRescuedIgnoredPropertyNames} on the first such
     * removal so {@link #getNonRescuedIgnoredPropertyNames()} can still report
     * it. Class-level ignorals are tracked in {@link #_classLevelIgnoredNames}
     * instead so they remain immune to that rescue.
     */
    protected HashSet<String> _perPropertyIgnoredNames;

    /**
     * Class-level ignorals: direction-specific names derived from
     * {@code @JsonIgnoreProperties} (annotation plus config overrides), copied here
     * from {@link #_propertyIgnorals} during {@link #_collectClassLevelIgnorals()}.
     *<p>
     * Held separately from {@link #_perPropertyIgnoredNames} so that the
     * {@code [databind#2001]} creator-rename rescue does not strip them: class-level
     * ignorals are absolute, per-property ignorals can be overridden by creators.
     *
     * @since 3.2
     */
    protected HashSet<String> _classLevelIgnoredNames;

    /**
     * Snapshot of {@link #_perPropertyIgnoredNames} taken just before the
     * {@code [databind#2001]} creator-rename rescue can fire, used by
     * {@link #getNonRescuedIgnoredPropertyNames()}. Null when no rescue happened
     * (in which case {@link #_perPropertyIgnoredNames} itself is still complete).
     *
     * @since 3.2
     */
    protected HashSet<String> _nonRescuedIgnoredPropertyNames;

    /**
     * Class-level ignorals (annotation plus config overrides), computed once during
     * {@link #_collectClassLevelIgnorals()} and exposed to the factory layer via
     * {@link #getPropertyIgnorals()} so that {@code findPropertyIgnoralByName()} is
     * called exactly once per type.
     *<p>
     * The direction-specific name set carried here is also mirrored into
     * {@link #_classLevelIgnoredNames} for inclusion in
     * {@link #getIgnoredPropertyNames()}; this field additionally retains the
     * {@code ignoreUnknown}, {@code allowGetters}/{@code allowSetters}, and
     * {@code merge} attributes which the simple name set does not.
     */
    protected JsonIgnoreProperties.Value _propertyIgnorals;

    /**
     * Lazily collected list of members that were annotated to
     * indicate that they represent mutators for deserializer
     * value injection.
     */
    protected LinkedHashMap<Object, AnnotatedMember> _injectables;

    /**
     * Lazily accessed information about POJO format overrides
     */
    protected JsonFormat.Value _formatOverrides;

    /*
    /**********************************************************************
    /* Life-cycle
    /**********************************************************************
     */

    protected POJOPropertiesCollector(MapperConfig<?> config, boolean forSerialization,
            JavaType type, AnnotatedClass classDef, AccessorNamingStrategy accessorNaming)
    {
        _config = config;
        _forSerialization = forSerialization;
        _type = type;
        _classDef = classDef;
        _isRecordType = _type.isRecordType();
        if (config.isAnnotationProcessingEnabled()) {
            _useAnnotations = true;
            _annotationIntrospector = _config.getAnnotationIntrospector();
        } else {
            _useAnnotations = false;
            _annotationIntrospector = AnnotationIntrospector.nopInstance();
        }
        _visibilityChecker = _config.getDefaultVisibilityChecker(type.getRawClass(),
                classDef);
        _accessorNaming = accessorNaming;
    }

    /*
    /**********************************************************************
    /* Public API
    /**********************************************************************
     */

    public MapperConfig<?> getConfig() {
        return _config;
    }

    public JavaType getType() {
        return _type;
    }

    public boolean isRecordType() {
        return _isRecordType;
    }

    public AnnotatedClass getClassDef() {
        return _classDef;
    }

    public AnnotationIntrospector getAnnotationIntrospector() {
        return _annotationIntrospector;
    }

    public List<BeanPropertyDefinition> getProperties() {
        // make sure we return a copy, so caller can remove entries if need be:
        Map<String, POJOPropertyBuilder> props = getPropertyMap();
        return new ArrayList<>(props.values());
    }

    // @since 2.18
    public PotentialCreators getPotentialCreators() {
        if (!_collected) {
            collectAll();
        }
        return _potentialCreators;
    }

    public Map<Object, AnnotatedMember> getInjectables() {
        if (!_collected) {
            collectAll();
        }
        return _injectables;
    }

    public AnnotatedMember getJsonKeyAccessor() {
        if (!_collected) {
            collectAll();
        }
        // If @JsonKey defined, must have a single one
        if (_jsonKeyAccessors != null) {
            if (_jsonKeyAccessors.size() > 1) {
                if (!_resolveFieldVsGetter(_jsonKeyAccessors)) {
                    reportProblem("Multiple 'as-key' properties defined (%s vs %s)",
                            _jsonKeyAccessors.get(0),
                            _jsonKeyAccessors.get(1));
                }
            }
            // otherwise we won't greatly care
            return _jsonKeyAccessors.get(0);
        }
        return null;
    }

    public AnnotatedMember getJsonValueAccessor()
    {
        if (!_collected) {
            collectAll();
        }
        // If @JsonValue defined, must have a single one
        // 15-Jan-2023, tatu: Except let's try resolving "getter-over-field" case at least
        if (_jsonValueAccessors != null) {
            if (_jsonValueAccessors.size() > 1) {
                if (!_resolveFieldVsGetter(_jsonValueAccessors)) {
                    reportProblem("Multiple 'as-value' properties defined (%s vs %s)",
                            _jsonValueAccessors.get(0),
                            _jsonValueAccessors.get(1));
                }
            }
            // otherwise we won't greatly care
            return _jsonValueAccessors.get(0);
        }
        return null;
    }

    public AnnotatedMember getAnyGetterField()
    {
        if (!_collected) {
            collectAll();
        }
        if (_anyGetterField != null) {
            if (_anyGetterField.size() > 1) {
                reportProblem("Multiple 'any-getter' fields defined (%s vs %s)",
                        _anyGetterField.get(0), _anyGetterField.get(1));
            }
            return _anyGetterField.getFirst();
        }
        return null;
    }

    public AnnotatedMember getAnyGetterMethod()
    {
        if (!_collected) {
            collectAll();
        }
        if (_anyGetters != null) {
            if (_anyGetters.size() > 1) {
                reportProblem("Multiple 'any-getter' methods defined (%s vs %s)",
                        _anyGetters.get(0), _anyGetters.get(1));
            }
            return _anyGetters.getFirst();
        }
        return null;
    }

    public AnnotatedMember getAnySetterField()
    {
        if (!_collected) {
            collectAll();
        }
        if (_anySetterField != null) {
            if (_anySetterField.size() > 1) {
                reportProblem("Multiple 'any-setter' fields defined (%s vs %s)",
                        _anySetterField.get(0), _anySetterField.get(1));
            }
            return _anySetterField.getFirst();
        }
        return null;
    }

    public AnnotatedMethod getAnySetterMethod()
    {
        if (!_collected) {
            collectAll();
        }
        if (_anySetters != null) {
            if (_anySetters.size() > 1) {
                reportProblem("Multiple 'any-setter' methods defined (%s vs %s)",
                        _anySetters.get(0), _anySetters.get(1));
            }
            return _anySetters.getFirst();
        }
        return null;
    }

    /**
     * Accessor for the full set of property names marked ignored — combining
     * per-property {@code @JsonIgnore} markers, class-level
     * {@code @JsonIgnoreProperties}, and read/write-only access rules. Filtered
     * by direction (ser vs. deser) at collection time.
     *<p>
     * Per-property names that have been "rescued" by the {@code [databind#2001]}
     * creator-rename redirect are excluded from this set; class-level names are
     * not subject to that rescue and always appear here. This is the set factory
     * code should consult to populate a deserializer's runtime ignore-list (see
     * {@code BeanDeserializerFactory#addBeanProps}). For the un-rescued
     * per-property view see {@link #getNonRescuedIgnoredPropertyNames()}.
     *<p>
     * <b>Performance note:</b> when both per-property and class-level ignorals
     * exist, this allocates a fresh {@link HashSet} on every call to merge them.
     * Callers that reference the result repeatedly (or in a loop) should cache
     * the returned set in a local. The intended call-site is
     * {@code BeanDeserializerFactory#addBeanProps}, which runs once per type
     * during deserializer construction — not on the dispatch hot path.
     */
    public Set<String> getIgnoredPropertyNames() {
        if (!_collected) {
            collectAll();
        }
        return _unionWithClassLevel(_perPropertyIgnoredNames);
    }

    /**
     * Accessor for the per-property ignorals view <em>before</em> any
     * {@code [databind#2001]} creator-rename rescue is applied: includes every
     * per-property {@code @JsonIgnore} or read/write-only name that was originally
     * collected, even those later overridden by a creator parameter renamed to
     * the same name. Class-level {@code @JsonIgnoreProperties} names are also
     * included (they are never rescued in the first place).
     *<p>
     * Most callers should use {@link #getIgnoredPropertyNames()} instead — this
     * accessor exists for the rare case where a caller needs to know about
     * names that <em>would</em> have been ignored but for the rescue.
     *
     * @since 3.2
     */
    public Set<String> getNonRescuedIgnoredPropertyNames() {
        if (!_collected) {
            collectAll();
        }
        // Snapshot of pre-rescue per-property names is captured into
        // _nonRescuedIgnoredPropertyNames just before strip-out can fire (see
        // _renameProperties); it is null when no rescue ever happened, in which
        // case _perPropertyIgnoredNames itself is still the full pre-rescue view.
        Set<String> perPropertyView = (_nonRescuedIgnoredPropertyNames != null)
                ? _nonRescuedIgnoredPropertyNames
                : _perPropertyIgnoredNames;
        return _unionWithClassLevel(perPropertyView);
    }

    /**
     * Accessor for class-level property ignorals (annotation plus config overrides),
     * computed once during collection and cached for reuse by the factory layer.
     * Returns {@code null} when no ignorals are defined.
     *<p>
     * Carries only class-level ignorals, in their original (un-stripped) form.
     * Equivalent to the class-level subset of {@link #getIgnoredPropertyNames()},
     * but in {@link JsonIgnoreProperties.Value} form (which also carries
     * {@code ignoreUnknown}, {@code allowGetters}/{@code allowSetters}, etc.).
     */
    public JsonIgnoreProperties.Value getPropertyIgnorals() {
        if (!_collected) {
            collectAll();
        }
        return _propertyIgnorals;
    }

    /**
     * Accessor to find out whether type specified requires inclusion
     * of Object Identifier.
     */
    public ObjectIdInfo getObjectIdInfo()
    {
        ObjectIdInfo info = _annotationIntrospector.findObjectIdInfo(_config, _classDef);
        if (info != null) {
            info = _annotationIntrospector.findObjectReferenceInfo(_config, _classDef, info);
        }
        return info;
    }

    // Method called by main "getProperties()" method; left
    // "protected" for unit tests
    protected Map<String, POJOPropertyBuilder> getPropertyMap() {
        if (!_collected) {
            collectAll();
        }
        return _properties;
    }

    // 14-May-2024, tatu: Not 100% sure this is needed in Jackson 3.x; was added
    //    in 2.x, merged in 2.18 timeframe to 3.0 for easier merges.
    /**
     * @since 2.17
     * @deprecated Since 3.2
     */
    @Deprecated // since 3.2; remove from 3.3 or later
    public JsonFormat.Value getFormatOverrides() {
        if (_formatOverrides == null) {
            // Let's check both per-type defaults and annotations;
            // per-type defaults having higher precedence, so start with annotations
            JsonFormat.Value format = _annotationIntrospector.findFormat(_config, _classDef);
            JsonFormat.Value v = _config.getDefaultPropertyFormat(_type.getRawClass());
            if (v != null) {
                if (format == null) {
                    format = v;
                } else {
                    format = format.withOverrides(v);
                }
            }
            _formatOverrides = (format == null) ? JsonFormat.Value.empty() : format;
        }
        return _formatOverrides;
    }

    /*
    /**********************************************************************
    /* Public API: main-level collection
    /**********************************************************************
     */

    /**
     * Internal method that will collect actual property information.
     */
    protected void collectAll()
    {
//System.out.println(" PojoPropsCollector.collectAll() for  "+_classDef.getRawType().getName()); 
        _potentialCreators = new PotentialCreators();

        // First: gather basic accessors
        LinkedHashMap<String, POJOPropertyBuilder> props = new LinkedHashMap<>();

        // 14-Nov-2024, tatu: Previously skipped checking fields for Records; with 2.18+ won't
        //    (see [databind#3628], [databind#3895], [databind#3992], [databind#4626])
        _addFields(props); // note: populates _fieldRenameMappings
        _addMethods(props);
        // 25-Jan-2016, tatu: Avoid introspecting (constructor-)creators for non-static
        //    inner classes, see [databind#1502]
        // 14-Nov-2024, tatu: Similarly need Creators for Records too (2.18+)
        if (!_classDef.isNonStaticInnerClass()) {
            _addCreators(props);
        }
        // 11-Jun-2025, tatu: [databind#5152] May need to "fix" mis-matching leading case
        //    wrt Fields vs Accessors
        if (_config.isEnabled(MapperFeature.FIX_FIELD_NAME_UPPER_CASE_PREFIX)) {
             _fixLeadingFieldNameCase(props);
        }
        // Remove ignored properties, first; this MUST precede annotation merging
        // since logic relies on knowing exactly which accessor has which annotation
        _removeUnwantedProperties(props);
        // [databind#3591]: Also collect class-level @JsonIgnoreProperties ignorals
        _collectClassLevelIgnorals();
        // and then remove unneeded accessors (wrt read-only, read-write)
        _removeUnwantedAccessors(props);

        // Rename remaining properties
        _renameProperties(props);

        // and now add injectables, but taking care to avoid overlapping ones
        // via creator and regular properties
        _addInjectables(props);

        // then merge annotations, to simplify further processing: has to be done AFTER
        // preceding renaming step to get right propagation
        for (POJOPropertyBuilder property : props.values()) {
            property.mergeAnnotations(_forSerialization);
        }

        // And use custom naming strategy, if applicable...
        // 18-Jan-2021, tatu: To be done before trimming, to resolve
        //   [databind#3368]
        PropertyNamingStrategy naming = _findNamingStrategy();
        if (naming != null) {
            _renameUsing(props, naming);
        }

        // Sort by visibility (explicit over implicit); drop all but first of member
        // type (getter, setter etc) if there is visibility difference
        for (POJOPropertyBuilder property : props.values()) {
            property.trimByVisibility();
        }

        // 22-Jul-2024, tatu: And now drop Record Fields once their effect
        //   (annotations) has been applied. But just for deserialization
        if (_isRecordType && !_forSerialization) {
            for (POJOPropertyBuilder property : props.values()) {
                property.removeFields();
            }
        }

        // and, if required, apply wrapper name: note, MUST be done after
        // annotations are merged.
        if (_config.isEnabled(MapperFeature.USE_WRAPPER_NAME_AS_PROPERTY_NAME)) {
            _renameWithWrappers(props);
        }

        // well, almost last: there's still ordering...
        _sortProperties(props);
        _properties = props;
        _collected = true;
    }

    /*
    /**********************************************************************
    /* Property introspection: Fields
    /**********************************************************************
     */

    /**
     * Method for collecting basic information on all fields found
     */
    protected void _addFields(Map<String, POJOPropertyBuilder> props)
    {
        final AnnotationIntrospector ai = _annotationIntrospector;
        // 28-Mar-2013, tatu: For deserialization we may also want to remove
        //   final fields, as often they won't make very good mutators...
        //  (although, maybe surprisingly, JVM _can_ force setting of such fields!)
        final boolean pruneFinalFields = !_forSerialization && !_config.isEnabled(MapperFeature.ALLOW_FINAL_FIELDS_AS_MUTATORS);
        final boolean transientAsIgnoral = _config.isEnabled(MapperFeature.PROPAGATE_TRANSIENT_MARKER);

        for (AnnotatedField f : _classDef.fields()) {
            // @JsonKey?
            if (Boolean.TRUE.equals(ai.hasAsKey(_config, f))) {
                if (_jsonKeyAccessors == null) {
                    _jsonKeyAccessors = new LinkedList<>();
                }
                _jsonKeyAccessors.add(f);
            }
            // @JsonValue?
            if (Boolean.TRUE.equals(ai.hasAsValue(_config, f))) {
                if (_jsonValueAccessors == null) {
                    _jsonValueAccessors = new LinkedList<>();
                }
                _jsonValueAccessors.add(f);
                continue;
            }
            // 12-October-2020, dominikrebhan: [databind#1458] Support @JsonAnyGetter on
            //   fields and allow @JsonAnySetter to be declared as well.
            boolean anyGetter = Boolean.TRUE.equals(ai.hasAnyGetter(_config, f));
            boolean anySetter = Boolean.TRUE.equals(ai.hasAnySetter(_config, f));
            if (anyGetter || anySetter) {
                // @JsonAnyGetter?
                if (anyGetter) {
                    if (_anyGetterField == null) {
                        _anyGetterField = new LinkedList<>();
                    }
                    _anyGetterField.add(f);
                }
                // @JsonAnySetter?
                if (anySetter) {
                    if (_anySetterField == null) {
                        _anySetterField = new LinkedList<>();
                    }
                    _anySetterField.add(f);
                    // 07-Feb-2025: [databind#4775]: Skip the rest of processing, but only
                    //    for "any-setter', not any-getter
                    continue;
                }
            }
            String implName = ai.findImplicitPropertyName(_config, f);
            if (implName == null) {
                implName = f.getName();
            }
            // 27-Aug-2020, tatu: [databind#2800] apply naming strategy for
            //   fields too, to allow use of naming conventions.
            implName = _accessorNaming.modifyFieldName(f, implName);
            if (implName == null) {
                continue;
            }

            final PropertyName implNameP = _propNameFromSimple(implName);
            // [databind#2527: Field-based renaming can be applied early (here),
            // or at a later point, but probably must be done before pruning
            // final fields. So let's do it early here
            final PropertyName rename = ai.findRenameByField(_config, f, implNameP);
            if ((rename != null) && !rename.equals(implNameP)) {
                if (_fieldRenameMappings == null) {
                    _fieldRenameMappings = new HashMap<>();
                }
                _fieldRenameMappings.put(rename, implNameP);
            }

            PropertyName pn;

            if (_forSerialization) {
                // 18-Aug-2011, tatu: As per existing unit tests, we should only
                //   use serialization annotation (@JsonSerialize) when serializing
                //   fields, and similarly for deserialize-only annotations... so
                //   no fallbacks in this particular case.
                pn = ai.findNameForSerialization(_config, f);
            } else {
                pn = ai.findNameForDeserialization(_config, f);
            }
            boolean hasName = (pn != null);
            boolean nameExplicit = hasName;

            if (nameExplicit && pn.isEmpty()) { // empty String meaning "use default name", here just means "same as field name"
                pn = _propNameFromSimple(implName);
                nameExplicit = false;
            }
            // having explicit name means that field is visible; otherwise need to check the rules
            boolean visible = (pn != null);
            if (!visible) {
                visible = _visibilityChecker.isFieldVisible(f);
            }
            // and finally, may also have explicit ignoral
            boolean ignored = ai.hasIgnoreMarker(_config, f);

            // 13-May-2015, tatu: Moved from earlier place (AnnotatedClass) in 2.6
            if (f.isTransient()) {
                // 20-May-2016, tatu: as per [databind#1184] explicit annotation should override
                //    "default" `transient`
                if (!hasName) {
                    // 25-Nov-2022, tatu: [databind#3682] Drop transient Fields early;
                    //     only retain if also have ignoral annotations (for name or ignoral)
                    if (transientAsIgnoral) {
                        ignored = true;

                    // 18-Jul-2023, tatu: [databind#3948] Need to retain if there was explicit
                    //   ignoral marker
                    } else if (!ignored) {
                        continue;
                    }
                }
            }
            /* [databind#190]: this is the place to prune final fields, if they are not
             *  to be used as mutators. Must verify they are not explicitly included.
             *  Also: if 'ignored' is set, need to include until a later point, to
             *  avoid losing ignoral information.
             */
            if (pruneFinalFields && (pn == null) && !ignored
                    && Modifier.isFinal(f.getModifiers())) {
                continue;
            }
            _property(props, implName).addField(f, pn, nameExplicit, visible, ignored);
        }
    }

    /*
    /**********************************************************************
    /* Property introspection: Creators (constructors, factory methods)
    /**********************************************************************
     */

    // Completely rewritten in 2.18
    protected void _addCreators(Map<String, POJOPropertyBuilder> props)
    {
        final PotentialCreators creators = _potentialCreators;

        // First, resolve explicit annotations for all potential Creators
        // (but do NOT filter out DISABLED ones yet!)
        List<PotentialCreator> constructors = _collectCreators(_classDef.getConstructors());
        List<PotentialCreator> factories = _collectCreators(_classDef.getFactoryMethods());

        // Note! 0-param ("default") constructor is NOT included in 'constructors':
        PotentialCreator zeroParamsConstructor;
        {
            AnnotatedConstructor ac = _classDef.getDefaultConstructor();
            zeroParamsConstructor = (ac == null) ? null : _potentialCreator(ac);
        }

        // Then find what is the Primary Constructor (if one exists for type):
        // for Java Records and potentially other types too ("data classes"):
        // Needs to be done early to get implicit names populated
        final PotentialCreator primaryCreator;
        if (_isRecordType) {
            primaryCreator = RecordUtil.findCanonicalRecordConstructor(_config, _classDef, constructors);
        } else {
            primaryCreator = _annotationIntrospector.findPreferredCreator(_config, _classDef,
                    constructors, factories,
                    Optional.ofNullable(zeroParamsConstructor));
        }
        // Next: remove creators marked as explicitly disabled
        _removeDisabledCreators(constructors);
        _removeDisabledCreators(factories);
        if (zeroParamsConstructor != null && _isDisabledCreator(zeroParamsConstructor)) {
            zeroParamsConstructor = null;
        }

        // And then remove non-annotated static methods that do not look like factories
        _removeNonFactoryStaticMethods(factories, primaryCreator);

        // and use annotations to find explicitly chosen Creators
        if (_useAnnotations) { // can't have explicit ones without Annotation introspection
            // Start with Constructors as they have higher precedence
            _addExplicitlyAnnotatedCreators(creators, constructors, props, false);
            // followed by Factory methods (lower precedence)
            _addExplicitlyAnnotatedCreators(creators, factories, props,
                    creators.hasPropertiesBased());

            // 08-Sep-2025, tatu: [databind#5045] Need to ensure 0-param ("default")
            //   constructor considered if annotated (disabled case handled above).
            // 27-Mar-2026, [databind#5840] But only if no other properties-based
            //   creator was found (multi-arg @JsonCreator takes precedence over 0-arg one)
            if (zeroParamsConstructor != null && zeroParamsConstructor.isAnnotated()
                    && !creators.hasPropertiesBased()) {
                creators.setExplicitPropertiesBased(_config, zeroParamsConstructor);
            }
        }

        // If no Explicitly annotated creators (or Primary one) found, look
        // for ones with explicitly-named ({@code @JsonProperty}) parameters
        if (!creators.hasPropertiesBased()) {
            // only discover constructor Creators?
            _addCreatorsWithAnnotatedNames(creators, constructors, primaryCreator);
        }


        // But if no annotation-based Creators found, find/use Primary Creator
        // detected earlier, if any
        if (primaryCreator != null) {
            // ... but only process if still included as a candidate
            if (constructors.remove(primaryCreator)
                    || factories.remove(primaryCreator)) {
                // and then consider delegating- vs properties-based
                if (_isDelegatingConstructor(primaryCreator)) {
                    // 08-Oct-2024, tatu: [databind#4724] Only add if no explicit
                    //    candidates added
                    if (!creators.hasDelegating() && !creators.hasExplicitPropertiesBased()) {
                        // ... not technically explicit but simpler this way
                        creators.addExplicitDelegating(primaryCreator);
                    }
                } else { // primary creator is properties-based
                    if (!creators.hasPropertiesBased()) {
                        creators.setPropertiesBased(_config, primaryCreator, "primary");
                    }
                }
            }
        }

        // One more thing: if neither explicit (constructor or factory) nor
        // canonical (constructor?), consider implicit Constructor with all named.
        final ConstructorDetector ctorDetector = _config.getConstructorDetector();
        if (!creators.hasPropertiesBasedOrDelegating()
                && !ctorDetector.requireCtorAnnotation()) {
            // But only if no Default (0-params) constructor available OR if we are configured
            // to prefer properties-based Creators
            // 19-Sep-2025, tatu: [databind#5318] Actually let's potentially allow
            //  implicit constructor even if class also has 0-param ("default") constructor
            //  (as long as it is not annotated)
            if ((zeroParamsConstructor == null)
                    || (!zeroParamsConstructor.isAnnotated()
                            && ctorDetector.allowImplicitWithDefaultConstructor())) {
                _addImplicitConstructor(creators, constructors, props);
            }
        }

        // Anything else left, add as possible implicit Creators
        // ... but first, trim non-visible
        _removeNonVisibleCreators(constructors);
        _removeNonVisibleCreators(factories);
        creators.setImplicitDelegating(constructors, factories);

        // And finally add logical properties for the One Properties-based
        // creator selected (if any):
        PotentialCreator propsCtor = creators.propertiesBased;
        if (propsCtor == null) {
            _creatorProperties = Collections.emptyList();
        } else {
            _creatorProperties = new ArrayList<>();
            _addCreatorParams(props, propsCtor, _creatorProperties);
        }
    }

    // Method to determine if given non-explictly-annotated constructor
    // looks like delegating one
    private boolean _isDelegatingConstructor(PotentialCreator ctor)
    {
        // First things first: could be 
        switch (ctor.creatorModeOrDefault()) {
        case DELEGATING:
            return true;
        case DISABLED:
        case PROPERTIES:
            return false;
        default:
        }

        // Only consider single-arg case, for now
        if (ctor.paramCount() == 1) {
            // Main thing: @JsonValue makes it delegating:
            if (_nonNullNonEmpty(_jsonValueAccessors)) {
                return true;
            }
        }
        return false;
    }

    private List<PotentialCreator> _collectCreators(List<? extends AnnotatedWithParams> ctors)
    {
        if (ctors.isEmpty()) {
            return Collections.emptyList();
        }
        List<PotentialCreator> result = new ArrayList<>();
        for (AnnotatedWithParams ctor : ctors) {
            // 06-Jul-2024, tatu: Can't yet drop DISABLED ones; add all (for now)
            result.add(_potentialCreator(ctor));
        }
        return (result == null) ? Collections.emptyList() : result;
    }

    private PotentialCreator _potentialCreator(AnnotatedWithParams ctor) {
        final JsonCreator.Mode creatorMode = _useAnnotations
                ? _annotationIntrospector.findCreatorAnnotation(_config, ctor) : null;
        return new PotentialCreator(ctor, creatorMode);
    }

    private void _removeDisabledCreators(List<PotentialCreator> ctors)
    {
        Iterator<PotentialCreator> it = ctors.iterator();
        while (it.hasNext()) {
            // explicitly disabled? Remove
            if (_isDisabledCreator(it.next())) {
                it.remove();
            }
        }
    }

    private boolean _isDisabledCreator(PotentialCreator ctor) {
         return ctor.creatorMode() == JsonCreator.Mode.DISABLED;
    }

    private void _removeNonVisibleCreators(List<PotentialCreator> ctors)
    {
        Iterator<PotentialCreator> it = ctors.iterator();
        while (it.hasNext()) {
            PotentialCreator ctor = it.next();
            boolean visible = (ctor.paramCount() == 1)
                    ? _visibilityChecker.isScalarConstructorVisible(ctor.creator())
                    : _visibilityChecker.isCreatorVisible(ctor.creator());
            if (!visible) {
                it.remove();
            }
        }
    }

    private void _removeNonFactoryStaticMethods(List<PotentialCreator> ctors,
            PotentialCreator primaryCreator)
    {
        final Class<?> rawType = _type.getRawClass();
        Iterator<PotentialCreator> it = ctors.iterator();
        while (it.hasNext()) {
            // explicit mode? Retain (for now)
            PotentialCreator ctor = it.next();
            if (ctor.isAnnotated()) {
                continue;
            }
            // Do not trim Primary creator either
            if (primaryCreator == ctor) {
                continue;
            }
            // Copied from `BasicBeanDescription.isFactoryMethod()`
            AnnotatedWithParams factory = ctor.creator();
            if (rawType.isAssignableFrom(factory.getRawType())
                    && ctor.paramCount() == 1) {
                String name = factory.getName();

                if ("valueOf".equals(name)) {
                    continue;
                } else if ("fromString".equals(name)) {
                    Class<?> cls = factory.getRawParameterType(0);
                    if (cls == String.class || CharSequence.class.isAssignableFrom(cls)) {
                        continue;
                    }
                }
            }
            it.remove();
        }
    }

    private void _addExplicitlyAnnotatedCreators(PotentialCreators collector,
            List<PotentialCreator> ctors,
            Map<String, POJOPropertyBuilder> props,
            boolean skipPropsBased)
    {
        final ConstructorDetector ctorDetector = _config.getConstructorDetector();
        Iterator<PotentialCreator> it = ctors.iterator();
        while (it.hasNext()) {
            PotentialCreator ctor = it.next();

            // If no explicit annotation, skip for now (may be discovered
            // at a later point)
            if (!ctor.isAnnotated()) {
                continue;
            }

            it.remove();

            boolean isPropsBased;

            switch (ctor.creatorMode()) {
            case DELEGATING:
                isPropsBased = false;
                break;
            case PROPERTIES:
                isPropsBased = true;
                break;
            case DEFAULT:
            default:
                isPropsBased = _isExplicitlyAnnotatedCreatorPropsBased(ctor,
                        props, ctorDetector);
            }

            if (isPropsBased) {
                // Skipping done if we already got higher-precedence Creator
                if (!skipPropsBased) {
                    collector.setExplicitPropertiesBased(_config, ctor);
                }
            } else {
                collector.addExplicitDelegating(ctor);
            }
        }
    }

    private boolean _isExplicitlyAnnotatedCreatorPropsBased(PotentialCreator ctor,
            Map<String, POJOPropertyBuilder> props, ConstructorDetector ctorDetector)
    {
        if (ctor.paramCount() == 1) {
            // Is ambiguity/heuristics allowed?
            switch (ctorDetector.singleArgMode()) {
            case DELEGATING:
                return false;
            case PROPERTIES:
                return true;
            case REQUIRE_MODE:
                throw new IllegalArgumentException(String.format(
"Single-argument constructor (%s) is annotated but no 'mode' defined; `ConstructorDetector`"
+ "configured with `SingleArgConstructor.REQUIRE_MODE`",
ctor.creator()));
            case HEURISTIC:
            default:
            }
        }

        // First: if explicit names found, is Properties-based
        ctor.introspectParamNames(_config);
        if (ctor.hasExplicitNames()) {
            return true;
        }
        // Second: [databind#3180] @JsonValue indicates delegating
        if (_nonNullNonEmpty(_jsonValueAccessors)) {
            return false;
        }
        if (ctor.paramCount() == 1) {
            // One more possibility: implicit name that maps to implied
            // property with at least one visible accessor
            PropertyName paramName = ctor.implicitName(0);
            if (paramName != null) {
                POJOPropertyBuilder prop = props.get(paramName.getSimpleName());
                if (prop != null) {
                    if (prop.anyVisible() && !prop.anyIgnorals()) {
                        return true;
                    }
                } else {
                    // 26-Nov-2024, tatu: [databind#4810] Implicit name not always
                    //   enough; may need to link to explicit name override
                    for (POJOPropertyBuilder pb : props.values()) {
                        if (pb.anyVisible()
                                && !pb.anyIgnorals()
                                && pb.hasExplicitName(paramName)) {
                            return true;
                        }
                    }
                }
            }
            // Second: injectable also suffices
            if (_annotationIntrospector.findInjectableValue(_config, ctor.param(0)) != null) {
                return true;
            }
            return false;
        }
        // Trickiest case: rely on existence of implicit names and/or injectables
        return ctor.hasNameOrInjectForAllParams(_config);
    }

    private void _addCreatorsWithAnnotatedNames(PotentialCreators collector,
            List<PotentialCreator> ctors, PotentialCreator primaryCtor)
    {
        final List<PotentialCreator> found = _findCreatorsWithAnnotatedNames(ctors);
        // 16-Jul-2024, tatu: [databind#4620] If Primary Creator found, it
        //    will be used to resolve candidate to use, if any
        if (primaryCtor != null) {
            if (found.contains(primaryCtor)) {
                collector.setPropertiesBased(_config, primaryCtor, "implicit");
                return;
            }
        }
        for (PotentialCreator ctor : found) {
            collector.setPropertiesBased(_config, ctor, "implicit");
        }
    }

    private List<PotentialCreator> _findCreatorsWithAnnotatedNames(List<PotentialCreator> ctors)
    {
        List<PotentialCreator> found = null;
        Iterator<PotentialCreator> it = ctors.iterator();
        while (it.hasNext()) {
            PotentialCreator ctor = it.next();
            // Ok: existence of explicit (annotated) names infers properties-based:
            ctor.introspectParamNames(_config);
            if (!ctor.hasExplicitNames()) {
                continue;
            }
            it.remove();
            if (found == null) {
                found = new ArrayList<>(4);
            }
            found.add(ctor);
        }
        return (found == null) ? Collections.emptyList() : found;
    }

    private boolean _addImplicitConstructor(PotentialCreators collector,
            List<PotentialCreator> ctors, Map<String, POJOPropertyBuilder> props)
    {
        // Must have one and only one candidate
        if (ctors.size() != 1) {
            return false;
        }
        final PotentialCreator ctor = ctors.get(0);
        // which needs to be visible
        final boolean visible = (ctor.paramCount() == 1)
                ? _visibilityChecker.isScalarConstructorVisible(ctor.creator())
                : _visibilityChecker.isCreatorVisible(ctor.creator());
        if (!visible) {
            return false;
        }
        ctor.introspectParamNames(_config);

        // As usual, 1-param case is distinct
        if (ctor.paramCount() != 1) {
            if (!ctor.hasNameOrInjectForAllParams(_config)) {
                return false;
            }
        } else {
            // First things first: if only param has Injectable, must be Props-based
            if (_annotationIntrospector.findInjectableValue(_config, ctor.param(0)) != null) {
                // props-based, continue
            } else {
                // may have explicit preference
                final ConstructorDetector ctorDetector = _config.getConstructorDetector();
                if (ctorDetector.singleArgCreatorDefaultsToDelegating()) {
                    return false;
                }
                // 20-Dec-2024, tatu: [databind#4860] Cannot detect as properties-based
                //   without implicit name (Injectable was checked earlier)
                String implicitParamName = ctor.implicitNameSimple(0);
                if (implicitParamName == null) {
                    return false;
                }

                // if not, prefer Properties-based if explicit preference OR
                // property with same name with at least one visible accessor
                if (!ctorDetector.singleArgCreatorDefaultsToProperties()) {
                    POJOPropertyBuilder prop = props.get(implicitParamName);
                    if ((prop == null) || !prop.anyVisible() || prop.anyIgnorals()) {
                        return false;
                    }
                }
            }
        }

        ctors.remove(0);
        collector.setPropertiesBased(_config, ctor, "implicit");
        return true;
    }

    private void _addCreatorParams(Map<String, POJOPropertyBuilder> props,
            PotentialCreator ctor, List<POJOPropertyBuilder> creatorProps)
    {
        final int paramCount = ctor.paramCount();
        for (int i = 0; i < paramCount; ++i) {
            final AnnotatedParameter param = ctor.param(i);
            final PropertyName explName = ctor.explicitName(i);
            PropertyName implName = ctor.implicitName(i);
            final boolean hasExplicit = (explName != null);
            final boolean hasImplicit = (implName != null);

            // First: check "Unwrapped" unless explicit name
            if (!hasExplicit) {
                NameTransformer unwrapper = _annotationIntrospector.findUnwrappingNameTransformer(_config, param);
                if (unwrapper != null) {
                    // If unwrapping, use a placeholder name to avoid name conflicts during
                    // deserialization. Store the implicit name as
                    // the internal name so _sortProperties() can place this property at its
                    // declaration position without re-invoking the annotation introspector.
                    // (see [databind#5716])
                    final PropertyName placeholder = UnwrappedPropertyHandler.creatorParamName(param.getIndex());
                    final PropertyName internalName = hasImplicit ? implName : placeholder;
                    final POJOPropertyBuilder prop = new POJOPropertyBuilder(_config,
                            _annotationIntrospector, _forSerialization, internalName, placeholder);
                    prop.markAsUnwrapped();
                    prop.addCtor(param, placeholder, false, true, false);
                    props.put(placeholder.getSimpleName(), prop);
                    creatorProps.add(prop);
                    continue;
                }
                if (!hasImplicit) {
                    // Without name, cannot make use of this creator parameter -- may or may not
                    // be a problem, verified at a later point.
                    creatorProps.add(null);
                    continue;
                }
            }

            // 27-Dec-2019, tatu: [databind#2527] may need to rename according to field
            final POJOPropertyBuilder prop;
            if (hasImplicit) {
                String n = _checkRenameByField(implName.getSimpleName());
                implName = PropertyName.construct(n);
                prop = _property(props, implName);
            } else {
                prop = _property(props, explName);
            }
            prop.addCtor(param, hasExplicit ? explName : implName, hasExplicit, true, false);
            creatorProps.add(prop);
        }
        ctor.assignPropertyDefs(creatorProps);
    }

    /*
    /**********************************************************************
    /* Property introspection: Methods (getters, setters etc)
    /**********************************************************************
     */

    /**
     * Method for collecting basic information on all accessor methods found
     */
    protected void _addMethods(Map<String, POJOPropertyBuilder> props)
    {
        for (AnnotatedMethod m : _classDef.memberMethods()) {
            // For methods, handling differs between getters and setters; and
            // we will also only consider entries that either follow the bean
            // naming convention or are explicitly marked: just being visible
            // is not enough (unlike with fields)

            int argCount = m.getParameterCount();
            if (argCount == 0) { // getters (including 'any getter')
                _addGetterMethod(props, m);
            } else if (argCount == 1) { // setters
                _addSetterMethod(props, m);
            } else if (argCount == 2) { // any setter?
                if (Boolean.TRUE.equals(_annotationIntrospector.hasAnySetter(_config, m))) {
                    if (_anySetters == null) {
                        _anySetters = new LinkedList<>();
                    }
                    _anySetters.add(m);
                }
            }
        }
    }

    protected void _addGetterMethod(Map<String, POJOPropertyBuilder> props,
            AnnotatedMethod m)
    {
        // Very first thing: skip if not returning any value
        // 06-May-2020, tatu: [databind#2675] changes handling slightly...
        {
            final Class<?> rt = m.getRawReturnType();
            if ((rt == Void.TYPE) ||
                    ((rt == Void.class) && !_config.isEnabled(MapperFeature.ALLOW_VOID_VALUED_PROPERTIES))) {
                return;
            }
        }

        // any getter?
        // @JsonAnyGetter?
        if (Boolean.TRUE.equals(_annotationIntrospector.hasAnyGetter(_config, m))) {
            if (_anyGetters == null) {
                _anyGetters = new LinkedList<>();
            }
            _anyGetters.add(m);
            // 07-Feb-2025: [databind#4775] Do not stop processing here
            //   (used to return)
        }
        // @JsonKey?
        else if (Boolean.TRUE.equals(_annotationIntrospector.hasAsKey(_config, m))) {
            if (_jsonKeyAccessors == null) {
                _jsonKeyAccessors = new LinkedList<>();
            }
            _jsonKeyAccessors.add(m);
            return;
        }
        // @JsonValue?
        else if (Boolean.TRUE.equals(_annotationIntrospector.hasAsValue(_config, m))) {
            if (_jsonValueAccessors == null) {
                _jsonValueAccessors = new LinkedList<>();
            }
            _jsonValueAccessors.add(m);
            return;
        }
        String implName; // from naming convention
        boolean visible;

        PropertyName pn = _annotationIntrospector.findNameForSerialization(_config, m);
        boolean nameExplicit = (pn != null);

        if (!nameExplicit) { // no explicit name; must consider implicit
            implName = _annotationIntrospector.findImplicitPropertyName(_config, m);
            if (implName == null) {
                implName = _accessorNaming.findNameForRegularGetter(m, m.getName());
            }
            if (implName == null) { // if not, must skip
                implName = _accessorNaming.findNameForIsGetter(m, m.getName());
                if (implName == null) {
                    return;
                }
                visible = _visibilityChecker.isIsGetterVisible(m);
            } else {
                visible = _visibilityChecker.isGetterVisible(m);
            }
        } else { // explicit indication of inclusion, but may be empty
            // we still need implicit name to link with other pieces
            implName = _annotationIntrospector.findImplicitPropertyName(_config, m);
            if (implName == null) {
                implName = _accessorNaming.findNameForRegularGetter(m, m.getName());
                if (implName == null) {
                    implName = _accessorNaming.findNameForIsGetter(m, m.getName());
                }
            }
            // if not regular getter name, use method name as is
            if (implName == null) {
                implName = m.getName();
            }
            if (pn.isEmpty()) {
                // !!! TODO: use PropertyName for implicit names too
                pn = _propNameFromSimple(implName);
                nameExplicit = false;
            }
            visible = true;
        }
        // 27-Dec-2019, tatu: [databind#2527] may need to rename according to field
        implName = _checkRenameByField(implName);
        boolean ignore = _annotationIntrospector.hasIgnoreMarker(_config, m);
        // 03-Dec-2025, tatu: [databind#5184]: Not the cleanest fix but here goes...
        //  (why not clean? Ideally accessor reconciliation solved the issue, not
        //  special case rule like done here)
        // For Records, prevent "get"-prefix methods with @JsonIgnore from incorrectly
        // affecting Record component fields (and thereby Creator parameters).
        // For example, if getter method is "getValue()" with @JsonIgnore and there's a
        // record component "value", the method should not cause the field to be ignored since
        // the actual accessor is "value()".
        // We check: is this a Record, does the method name NOT match the derived property name
        // (indicating prefix was stripped), does the property already exist (from a record field),
        // and does this method have @JsonIgnore?
        if (_isRecordType && !nameExplicit && ignore && !implName.equals(m.getName())) {
            POJOPropertyBuilder prop = props.get(implName);
            if (prop != null && prop.hasField()) {
                // Skip adding this getter to avoid its @JsonIgnore affecting the record field
                return;
            }
        }
        _property(props, implName).addGetter(m, pn, nameExplicit, visible, ignore);
    }

    protected void _addSetterMethod(Map<String, POJOPropertyBuilder> props,
            AnnotatedMethod m)
    {
        String implName; // from naming convention
        boolean visible;
        PropertyName pn = _annotationIntrospector.findNameForDeserialization(_config, m);
        boolean nameExplicit = (pn != null);
        if (!nameExplicit) { // no explicit name; must follow naming convention
            implName = _annotationIntrospector.findImplicitPropertyName(_config, m);
            if (implName == null) {
                implName = _accessorNaming.findNameForMutator(m, m.getName());
            }
            if (implName == null) { // if not, must skip
            	return;
            }
            visible = _visibilityChecker.isSetterVisible(m);
        } else { // explicit indication of inclusion, but may be empty
            // we still need implicit name to link with other pieces
            implName = _annotationIntrospector.findImplicitPropertyName(_config, m);
            if (implName == null) {
                implName = _accessorNaming.findNameForMutator(m, m.getName());
            }
            // if not regular getter name, use method name as is
            if (implName == null) {
                implName = m.getName();
            }
            if (pn.isEmpty()) {
                // !!! TODO: use PropertyName for implicit names too
                pn = _propNameFromSimple(implName);
                nameExplicit = false;
            }
            visible = true;
        }
        // 27-Dec-2019, tatu: [databind#2527] may need to rename according to field
        implName = _checkRenameByField(implName);
        boolean ignore = _annotationIntrospector.hasIgnoreMarker(_config, m);
        _property(props, implName).addSetter(m, pn, nameExplicit, visible, ignore);
    }

    protected void _addInjectables(Map<String, POJOPropertyBuilder> props)
    {
        // first fields, then methods, to allow overriding
        for (AnnotatedField f : _classDef.fields()) {
            _doAddInjectable(_annotationIntrospector.findInjectableValue(_config, f), f);
        }

        for (AnnotatedMethod m : _classDef.memberMethods()) {
            // for now, only allow injection of a single arg (to be changed in future?)
            if (m.getParameterCount() != 1) {
                continue;
            }
            _doAddInjectable(_annotationIntrospector.findInjectableValue(_config, m), m);
        }

        // 21-Aug-2025, tatu: [databind#4218] avoid duplicate injectables
        if (_injectables != null) {
            for (POJOPropertyBuilder creatorProperty : _creatorProperties) {
                if (creatorProperty == null) {
                    continue;
                }
                final AnnotatedParameter parameter = creatorProperty.getConstructorParameter();
                JacksonInject.Value injectable = _annotationIntrospector.findInjectableValue(_config, parameter);
                if (injectable != null) {
                    _injectables.remove(injectable.getId());
                }
            }
        }
    }

    protected void _doAddInjectable(JacksonInject.Value injectable, AnnotatedMember m)
    {
        if (injectable == null) {
            return;
        }
        Object id = injectable.getId();
        if (_injectables == null) {
            _injectables = new LinkedHashMap<>();
        }
        AnnotatedMember prev = _injectables.put(id, m);
        if (prev != null) {
            // 12-Apr-2017, tatu: Let's allow masking of Field by Method
            if (prev.getClass() == m.getClass()) {
                reportProblem("Duplicate injectable value with id '%s' (of type %s)",
                        id, ClassUtil.classNameOf(id));
            }
        }
    }

    private PropertyName _propNameFromSimple(String simpleName) {
        return PropertyName.construct(simpleName, null);
    }

    private String _checkRenameByField(String implName) {
        if (_fieldRenameMappings != null) {
            PropertyName p = _fieldRenameMappings.get(_propNameFromSimple(implName));
            if (p != null) {
                implName = p.getSimpleName();
                return implName;

            }
        }
        return implName;
    }

    /*
    /**********************************************************************
    /* Internal methods; merging/fixing case-differences
    /**********************************************************************
     */

    protected void _fixLeadingFieldNameCase(Map<String, POJOPropertyBuilder> props)
    {
        // 11-Jun-2025, tatu: [databind#5152] May need to "fix" mis-matching leading case
        //    wrt Fields vs Accessors

        // First: find possible candidates where:
        //
        // 1. Property has Field and/or Constructor Parameter
        // 2. Property has no other accessors (no getters/setters)
        // 3. Field/Constructor param does NOT have explicit name (renaming)
        // 4. Implicit name has upper-case for first and/or second character

        Map<String, POJOPropertyBuilder> fieldsToCheck = null;
        for (Map.Entry<String, POJOPropertyBuilder> entry : props.entrySet()) {
            POJOPropertyBuilder  prop = entry.getValue();

            // First: (1), (2) and 3
            if (prop.isExplicitlyNamed() // (3)
                    || !(prop.hasField() || prop.hasConstructorParameter()) // (1)
                    || (prop.hasGetter() || prop.hasSetter())) { // 2
                continue;
            }
            // Second: (4)
            if (!_firstOrSecondCharUpperCase(entry.getKey())) {
                continue;
            }
            if (fieldsToCheck == null) {
                fieldsToCheck = new HashMap<>();
            }
            fieldsToCheck.put(entry.getKey(), prop);
        }
        /*// DEBUGGING
        if (fieldsToCheck == null) {
            System.err.println("_fixLeadingCase, candidates -> null; props -> "+props.keySet());
        } else {
            System.err.println("_fixLeadingCase, candidates -> "+fieldsToCheck);
        }
        */

        if (fieldsToCheck == null) {
            return;
        }

        for (Map.Entry<String, POJOPropertyBuilder> fieldEntry : fieldsToCheck.entrySet()) {
            Iterator<Map.Entry<String, POJOPropertyBuilder>> it = props.entrySet().iterator();
            final POJOPropertyBuilder fieldProp = fieldEntry.getValue();
            final String fieldName = fieldEntry.getKey();

            while (it.hasNext()) {
                Map.Entry<String, POJOPropertyBuilder> propEntry = it.next();
                final POJOPropertyBuilder prop = propEntry.getValue();

                // Skip anything that has Field (can't merge)
                if (prop == fieldProp || prop.hasField()) {
                    continue;
                }
                if (fieldName.equalsIgnoreCase(propEntry.getKey())) {
                    // Remove non-Field property; add its accessors to Field one
                    it.remove();
                    fieldProp.addAll(prop);
                    // Should we continue with possible other accessors?
                    // For now assume only one merge needed/desired
                    break;
                }
            }
        }
    }

    // @since 2.20
    private boolean _firstOrSecondCharUpperCase(String name) {
         switch (name.length()) {
         case 0:
             return false;
         default:
             if (!Character.isLowerCase(name.charAt(1))) {
                 return true;
             }
             // fall through
         case 1:
             if (!Character.isLowerCase(name.charAt(0))) {
                 return true;
             }
             return false;
         }
    }

    /*
    /**********************************************************************
    /* Internal methods; removing ignored properties
    /**********************************************************************
     */

    /**
     * Method called to get rid of candidate properties that are marked
     * as ignored.
     */
    protected void _removeUnwantedProperties(Map<String, POJOPropertyBuilder> props)
    {
        Iterator<POJOPropertyBuilder> it = props.values().iterator();
        while (it.hasNext()) {
            POJOPropertyBuilder prop = it.next();

            // First: if nothing visible, just remove altogether
            if (!prop.anyVisible()) {
                it.remove();
                continue;
            }
            // Otherwise, check ignorals
            if (prop.anyIgnorals()) {
                // Special handling for Records, as they do not have mutators so relying on constructors
                // with (mostly) implicitly-named parameters...
                // 20-Jul-2023, tatu: This can be harmful, see f.ex [databind#3992] so
                //    only use special handling for deserialization

                if (isRecordType() && !_forSerialization) {
                      // ...so can only remove ignored field and/or accessors, not constructor parameters that are needed
                      // for instantiation...
                      prop.removeIgnored();
                      // ...which will then be ignored (the incoming property value) during deserialization
                    _collectIgnorals(prop.getName());
                    continue;
                }

                // first: if one or more ignorals, and no explicit markers, remove the whole thing
                // 16-May-2022, tatu: NOTE! As per [databind#3357] need to consider
                //    only explicit inclusion by accessors OTHER than ones with ignoral marker
                if (!prop.anyExplicitsWithoutIgnoral()) {
                    it.remove();
                    _collectIgnorals(prop.getName());
                    continue;
                }
                // otherwise just remove ones marked to be ignored
                prop.removeIgnored();
                if (!prop.couldDeserialize()) {
                    _collectIgnorals(prop.getName());
                }
            }
        }
    }

    /**
     * Method called to further get rid of unwanted individual accessors,
     * based on read/write settings and rules for "pulling in" accessors
     * (or not).
     */
    protected void _removeUnwantedAccessors(Map<String, POJOPropertyBuilder> props)
    {
        // 15-Jan-2023, tatu: Avoid pulling in mutators for Records; Fields mostly
        //    since there should not be setters.
        // 22-Jul-2024, tatu: Actually do pull them to fix [databind#4630]
        final boolean inferMutators = _config.isEnabled(MapperFeature.INFER_PROPERTY_MUTATORS);
        Iterator<POJOPropertyBuilder> it = props.values().iterator();

        while (it.hasNext()) {
            POJOPropertyBuilder prop = it.next();
            // 26-Jan-2017, tatu: [databind#935]: need to denote removal of
            // 16-May-2020, tatu: [databind#2719]: need to pass `this` to allow
            //    addition of ignorals wrt explicit name
            prop.removeNonVisible(inferMutators, _forSerialization ? null : this);
        }
    }

    /**
     * Helper method called to record a per-property ignoral (from {@code @JsonIgnore}
     * or read/write-only rules) into {@link #_perPropertyIgnoredNames}.
     * Used by {@link #_renameProperties} to skip renaming ignored properties, and
     * surfaced externally via {@link #getIgnoredPropertyNames()}.
     */
    protected void _collectIgnorals(String name)
    {
        if (name != null) {
            if (_perPropertyIgnoredNames == null) {
                _perPropertyIgnoredNames = new HashSet<>();
            }
            _perPropertyIgnoredNames.add(name);
        }
    }

    /**
     * Returns {@code true} if the (already-collected) properties-based creator
     * has a parameter currently bound to the given name. Used by
     * {@link POJOPropertyBuilder#removeNonVisible} to skip {@code READ_ONLY}
     * explicit-name ignorals that would shadow a sibling creator parameter
     * ({@code [databind#5975]}).
     *
     * @since 3.2
     */
    public boolean hasCreatorBoundProperty(String name) {
        if (_creatorProperties == null) {
            return false;
        }
        for (POJOPropertyBuilder p : _creatorProperties) {
            if (p != null && name.equals(p.getName())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Helper method called to collect class-level property ignorals: stores the
     * full {@link com.fasterxml.jackson.annotation.JsonIgnoreProperties.Value}
     * (annotation + config overrides) in {@link #_propertyIgnorals} for reuse by
     * the factory layer, and copies the direction-specific property names into
     * {@link #_classLevelIgnoredNames} (held separately from
     * {@link #_perPropertyIgnoredNames} so the {@code [databind#2001]} creator-rename
     * rescue does not strip them — class-level ignorals are absolute).
     *<p>
     * Uses {@link MapperConfig#getDefaultPropertyIgnorals} rather than calling
     * {@code findPropertyIgnoralByName()} directly, so that config-level overrides
     * are included and consistent with what the factory layer sees.
     *
     * @since 3.2
     */
    protected void _collectClassLevelIgnorals()
    {
        _propertyIgnorals =
            _config.getDefaultPropertyIgnorals(_classDef.getRawType(), _classDef);
        if (_propertyIgnorals != null) {
            Set<String> ignored = _forSerialization
                    ? _propertyIgnorals.findIgnoredForSerialization()
                    : _propertyIgnorals.findIgnoredForDeserialization();
            if (_nonNullNonEmpty(ignored)) {
                if (_classLevelIgnoredNames == null) {
                    _classLevelIgnoredNames = new HashSet<>(ignored);
                } else {
                    _classLevelIgnoredNames.addAll(ignored);
                }
            }
        }
    }

    /*
    /**********************************************************************
    /* Internal methods; renaming properties
    /**********************************************************************
     */

    protected void _renameProperties(Map<String, POJOPropertyBuilder> props)
    {
        // With renaming need to do in phases: first, find properties to rename
        Iterator<Map.Entry<String,POJOPropertyBuilder>> it = props.entrySet().iterator();
        LinkedList<POJOPropertyBuilder> renamed = null;

        while (it.hasNext()) {
            Map.Entry<String, POJOPropertyBuilder> entry = it.next();
            POJOPropertyBuilder prop = entry.getValue();

            // 10-Apr-2025: [databind#4628] skip properties that are marked to be ignored
            // 19-Nov-2025: [databind#5398] BUT do not skip if property has explicit names
            //   on accessors that are NOT ignored (e.g., @JsonProperty on getter but @JsonIgnore on setter).
            //   NOTE: For Records we need to be more conservative as constructor parameters may have
            //   both annotations but generated accessors don't always inherit @JsonIgnore
            // 04-May-2026: [databind#5952] Check both per-property and class-level
            //   ignoral sets (previously a single combined set; now separated to keep
            //   class-level names immune from creator-rename rescue).
            if (_isIgnored(prop.getName())) {
                // For Records: always skip (safer due to annotation inheritance issues)
                // For regular classes: only skip if NO explicit names on non-ignored accessors
                if (isRecordType() || !prop.anyExplicitsWithoutIgnoral()) {
                    continue;
                }
                // [databind#5967]: Strip inferred non-visible field mutators to preserve @JsonIgnore
                // semantics. The ignored name was collected because couldDeserialize()==false,
                // meaning any retained fields are non-visible (kept only by INFER_PROPERTY_MUTATORS).
                // Removing them ensures the renamed property remains read-only (serialization only).
                prop.removeFields();
            }

            Collection<PropertyName> l = prop.findExplicitNames();
            // no explicit names? Implicit one is fine as is
            if (l.isEmpty()) {
                continue;
            }
            it.remove(); // need to replace with one or more renamed
            if (renamed == null) {
                renamed = new LinkedList<>();
            }
            // simple renaming? Just do it
            if (l.size() == 1) {
                PropertyName n = l.iterator().next();
                renamed.add(prop.withName(n));
                continue;
            }
            // but this may be problematic...
            renamed.addAll(prop.explode(l));

            /*
            String newName = prop.findNewName();
            if (newName != null) {
                if (renamed == null) {
                    renamed = new LinkedList<>();
                }
                prop = prop.withSimpleName(newName);
                renamed.add(prop);
                it.remove();
            }
            */
        }

        // and if any were renamed, merge back in...
        if (renamed != null) {
            for (POJOPropertyBuilder prop : renamed) {
                String name = prop.getName();
                POJOPropertyBuilder old = props.get(name);
                if (old == null) {
                    props.put(name, prop);
                } else {
                    old.addAll(prop);
                }
                // replace the creatorProperty too, if there is one
                if (_replaceCreatorProperty(_creatorProperties, prop)) {
                    // [databind#2001]: New name of property was ignored previously? Remove from ignored
                    // 01-May-2018, tatu: I have a feeling this will need to be revisited at some point,
                    //   to avoid removing some types of removals, possibly. But will do for now.

                    // 16-May-2020, tatu: ... and so the day came, [databind#2118] failed
                    //    when explicit rename added to ignorals (for READ_ONLY) was suddenly
                    //    removed from ignoral list. So, added a guard statement above so that
                    //    ignoral is ONLY removed if there was matching creator property.
                    //
                    //    Chances are this is not the last tweak we need but... that bridge then etc

                    // 04-May-2026, tatu: [databind#5952] Operates only on the
                    //   per-property set (_perPropertyIgnoredNames). Class-level names
                    //   (in _classLevelIgnoredNames) are absolute and must
                    //   never be rescued — that is what previously required the
                    //   factory layer to maintain a parallel un-stripped class-level
                    //   loop. Snapshot the un-rescued per-property view here so
                    //   getNonRescuedIgnoredPropertyNames() can still report it.
                    _rescuePerPropertyIgnoral(name);
                }
            }
        }

        // [databind#6031]: a creator property's @JsonAlias names are legitimate
        // deserialization input names, so — like the creator-rename rescue above —
        // they must not be suppressed by an unrelated per-property ignoral whose
        // implicit name happens to coincide with one (e.g. a @JsonIgnore getter
        // named "oldName" colliding with @JsonAlias("oldName") on a creator param).
        // Deser-only: aliases never apply to serialization. Class-level ignorals
        // (in _classLevelIgnoredNames) stay absolute and are deliberately untouched.
        if (!_forSerialization) {
            _rescueCreatorAliasIgnorals();
        }
    }

    /**
     * Removes from {@link #_perPropertyIgnoredNames} any name that is a
     * {@code @JsonAlias} of a (live) properties-based creator parameter.
     * Companion to the {@code [databind#2001]} creator-rename rescue in
     * {@link #_renameProperties}; see {@code [databind#6031]}.
     *
     * @since 3.2.1
     */
    protected void _rescueCreatorAliasIgnorals()
    {
        if (_perPropertyIgnoredNames == null || _creatorProperties == null) {
            return;
        }
        for (POJOPropertyBuilder creatorProp : _creatorProperties) {
            if (creatorProp == null) {
                continue;
            }
            for (PropertyName alias : creatorProp.findAliases()) {
                _rescuePerPropertyIgnoral(alias.getSimpleName());
            }
        }
    }

    /**
     * Removes a single name from {@link #_perPropertyIgnoredNames} (if present),
     * snapshotting the pre-rescue view into {@link #_nonRescuedIgnoredPropertyNames}
     * on the first removal so {@link #getNonRescuedIgnoredPropertyNames()} can still
     * report names that <em>would</em> have been ignored but for the rescue. Shared
     * by the {@code [databind#2001]} creator-rename rescue and the
     * {@code [databind#6031]} creator-alias rescue. Class-level ignorals
     * (in {@link #_classLevelIgnoredNames}) are intentionally never touched here.
     *
     * @since 3.2.1
     */
    private void _rescuePerPropertyIgnoral(String name)
    {
        if (_perPropertyIgnoredNames != null && _perPropertyIgnoredNames.contains(name)) {
            if (_nonRescuedIgnoredPropertyNames == null) {
                _nonRescuedIgnoredPropertyNames = new HashSet<>(_perPropertyIgnoredNames);
            }
            _perPropertyIgnoredNames.remove(name);
        }
    }

    protected void _renameUsing(Map<String, POJOPropertyBuilder> propMap,
            PropertyNamingStrategy naming)
    {
        // [databind#4409]: Need to skip renaming for Enums, unless Enums are handled
        //  as OBJECT format
        // 06-Mar-2024, tatu: Should move format override introspection to this
        //   class, from [Basic]BeanDescription
        if (_type.isEnumType() && (_findFormatShape() != JsonFormat.Shape.OBJECT)) {
            return;
        }

        POJOPropertyBuilder[] props = propMap.values().toArray(new POJOPropertyBuilder[0]);
        propMap.clear();
        for (POJOPropertyBuilder prop : props) {
            PropertyName fullName = prop.getFullName();
            String rename = null;
            // As per [databind#428] need to skip renaming if property has
            // explicitly defined name, unless feature  is enabled
            if (!prop.isExplicitlyNamed() || _config.isEnabled(MapperFeature.ALLOW_EXPLICIT_PROPERTY_RENAMING)) {
                if (_forSerialization) {
                    if (prop.hasGetter()) {
                        rename = naming.nameForGetterMethod(_config, prop.getGetter(), fullName.getSimpleName());
                    } else if (prop.hasField()) {
                        rename = naming.nameForField(_config, prop.getField(), fullName.getSimpleName());
                    }
                } else {
                    if (prop.hasSetter()) {
                        rename = naming.nameForSetterMethod(_config, prop.getSetterUnchecked(), fullName.getSimpleName());
                    } else if (prop.hasConstructorParameter()) {
                        rename = naming.nameForConstructorParameter(_config, prop.getConstructorParameter(), fullName.getSimpleName());
                    } else if (prop.hasField()) {
                        rename = naming.nameForField(_config, prop.getFieldUnchecked(), fullName.getSimpleName());
                    } else if (prop.hasGetter()) {
                        // Plus, when getter-as-setter is used, need to convert that too..
                        // (should we verify that's enabled? For now, assume it's ok always)
                        rename = naming.nameForGetterMethod(_config, prop.getGetterUnchecked(), fullName.getSimpleName());
                    }
                }
            }
            final String simpleName;
            if ((rename != null) && !fullName.hasSimpleName(rename)) {
                // [databind#5974]: preserve @JsonIgnore semantics through naming-strategy
                // rename so the renamed key is also recognized as ignored.
                if (_isIgnored(fullName.getSimpleName())) {
                    _collectIgnorals(rename);
                }
                prop = prop.withSimpleName(rename);
                simpleName = rename;
            } else {
                simpleName = fullName.getSimpleName();
            }
            // Need to consider case where there may already be something in there...
            POJOPropertyBuilder old = propMap.get(simpleName);
            if (old == null) {
                propMap.put(simpleName, prop);
            } else {
                old.addAll(prop);
            }

            // replace the creatorProperty too, if there is one
            _replaceCreatorProperty(_creatorProperties, prop);
        }
    }

    /**
     * Helper method called to check if given property should be renamed using {@link PropertyNamingStrategies}.
     *<p>
     * NOTE: copied+simplified version of {@code BasicBeanDescription.findExpectedFormat()}.
     *
     * @since 2.16.2
     */
    private JsonFormat.Shape _findFormatShape()
    {
        // Per-class config overrides have higher precedence so:
        JsonFormat.Value defFormat = _config.getDefaultPropertyFormat(_classDef.getRawType());
        if ((defFormat != null) && defFormat.hasShape()) {
            return defFormat.getShape();
        }
        JsonFormat.Value format = _annotationIntrospector.findFormat(_config, _classDef);
        return (format == null) ? null : format.getShape();
    }

    protected void _renameWithWrappers(Map<String, POJOPropertyBuilder> props)
    {
        // 11-Sep-2012, tatu: To support 'MapperFeature.USE_WRAPPER_NAME_AS_PROPERTY_NAME',
        //   need another round of renaming...
        Iterator<Map.Entry<String,POJOPropertyBuilder>> it = props.entrySet().iterator();
        LinkedList<POJOPropertyBuilder> renamed = null;
        while (it.hasNext()) {
            Map.Entry<String, POJOPropertyBuilder> entry = it.next();
            POJOPropertyBuilder prop = entry.getValue();
            AnnotatedMember member = prop.getPrimaryMember();
            if (member == null) {
                continue;
            }
            PropertyName wrapperName = _annotationIntrospector.findWrapperName(_config, member);
            // One trickier part (wrt [#24] of JAXB annotations: wrapper that
            // indicates use of actual property... But hopefully has been taken care
            // of previously
            if (wrapperName == null || !wrapperName.hasSimpleName()) {
                continue;
            }
            if (!wrapperName.equals(prop.getFullName())) {
                if (renamed == null) {
                    renamed = new LinkedList<>();
                }
                prop = prop.withName(wrapperName);
                renamed.add(prop);
                it.remove();
            }
        }
        // and if any were renamed, merge back in...
        if (renamed != null) {
            for (POJOPropertyBuilder prop : renamed) {
                String name = prop.getName();
                POJOPropertyBuilder old = props.get(name);
                if (old == null) {
                    props.put(name, prop);
                } else {
                    old.addAll(prop);
                }
            }
        }
    }

    /*
    /**********************************************************************
    /* Internal methods, sorting
    /**********************************************************************
     */

    // First, order by(explicit ordering and/or alphabetic),
    // then by (optional) index (if any)
    // and then implicitly order creator properties before others)

    protected void _sortProperties(Map<String, POJOPropertyBuilder> props)
    {
        // Then how about explicit ordering?
        final AnnotationIntrospector intr = _annotationIntrospector;
        Boolean alpha = intr.findSerializationSortAlphabetically(_config, _classDef);
        final boolean sortAlpha = (alpha == null)
                ? _config.shouldSortPropertiesAlphabetically()
                : alpha.booleanValue();
        final boolean useIndexOrdering = _anyIndexed(props.values())
                && _config.isEnabled(MapperFeature.SORT_PROPERTIES_BY_INDEX);
        final boolean sortCreatorsFirst = (_creatorProperties != null)
                && _config.isEnabled(MapperFeature.SORT_CREATOR_PROPERTIES_FIRST);
        final AnnotatedMember anyAccessor = _findAnyAccessor();

        String[] propertyOrder = intr.findSerializationPropertyOrder(_config, _classDef);

        // no sorting? no need to shuffle, then. But note there are lots of things
        // that do require some shuffling.
        if (!sortAlpha && !useIndexOrdering && !sortCreatorsFirst
                && (propertyOrder == null) && (anyAccessor == null)) {
            return;
        }
        int size = props.size();
        Map<String, POJOPropertyBuilder> all;
        // Need to (re)sort alphabetically?
        if (sortAlpha) {
            all = new TreeMap<>();
        } else {
            all = new LinkedHashMap<>(size+size);
        }
        // First, handle sorting caller expects:
        for (POJOPropertyBuilder prop : props.values()) {
            all.put(prop.getName(), prop);
        }
        if (anyAccessor != null) {
            all = _moveAnyAccessorToTheEnd(all, anyAccessor);
        }
        Map<String,POJOPropertyBuilder> ordered = new LinkedHashMap<>(size+size);
        // Ok: primarily by explicit order
        if (propertyOrder != null) {
            for (String name : propertyOrder) {
                POJOPropertyBuilder w = all.remove(name);
                if (w == null) { // will also allow use of "implicit" names for sorting
                    for (POJOPropertyBuilder prop : props.values()) {
                        if (name.equals(prop.getInternalName())) {
                            w = prop;
                            // plus re-map to external name, to avoid dups:
                            name = prop.getName();
                            break;
                        }
                    }
                }
                if (w != null) {
                    ordered.put(name, w);
                }
            }
        }

        // Second (starting with 2.11): index, if any:
        if (useIndexOrdering) {
            Map<Integer,POJOPropertyBuilder> byIndex = new TreeMap<>();
            Iterator<Map.Entry<String,POJOPropertyBuilder>> it = all.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<String,POJOPropertyBuilder> entry = it.next();
                POJOPropertyBuilder prop = entry.getValue();
                Integer index = prop.getMetadata().getIndex();
                if (index != null) {
                    byIndex.put(index, prop);
                    it.remove();
                }
            }
            for (POJOPropertyBuilder prop : byIndex.values()) {
                ordered.put(prop.getName(), prop);
            }
        }

        // Third by sorting Creator properties before other unordered properties
        // (unless strict ordering is requested)
        if (sortCreatorsFirst) {
            /* As per [databind#311], this is bit delicate; but if alphabetic ordering
             * is mandated, at least ensure creator properties are in alphabetic
             * order. Related question of creator vs non-creator is punted for now,
             * so creator properties still fully predate non-creator ones.
             */
            // 18-Jun-2024, tatu: Except in Jackson 3.0, we do NOT sort creator properties
            //   alphabetically if they are to be sorted before other properties.
            for (POJOPropertyBuilder prop : _creatorProperties) {
                if (prop == null) {
                    continue;
                }
                // 16-Jan-2016, tatu: Related to [databind#1317], make sure not to accidentally
                //    add back pruned creator properties!

                // [databind#5716]: @JsonUnwrapped creator params use a placeholder name to avoid
                // name conflicts during deserialization. The actual getter property name is stored
                // as internalName in _addCreatorParams(), so use it here for correct ordering.
                if (prop.isUnwrapped()) {
                    String internalName = prop.getInternalName();
                    POJOPropertyBuilder pb = all.get(internalName);
                    if (pb != null) {
                        ordered.put(internalName, pb);
                        continue;
                    }
                }
                String name = prop.getName();
                // 27-Nov-2019, tatu: Not sure why, but we should NOT remove it from `all` tho:
//                if (all.remove(name) != null) {
                if (all.containsKey(name)) {
                    ordered.put(name, prop);
                }
            }
        }
        // And finally whatever is left (trying to put again will not change ordering)
        ordered.putAll(all);
        props.clear();
        props.putAll(ordered);
    }

    private boolean _anyIndexed(Collection<POJOPropertyBuilder> props) {
        for (POJOPropertyBuilder prop : props) {
            if (prop.getMetadata().hasIndex()) {
                return true;
            }
        }
        return false;
    }

    private AnnotatedMember _findAnyAccessor() {
        if (_anyGetters != null) {
            return _anyGetters.getFirst();
        }
        if (_anyGetterField != null) {
            return _anyGetterField.getFirst();
        }
        return null;
    }

    /**
     * [databind#5215] JsonAnyGetter Serializer behavior change from 2.18.4 to 2.19.0
     * Put anyGetter in the end, before actual sorting further down {@link POJOPropertiesCollector#_sortProperties(Map)}
     */
    private Map<String, POJOPropertyBuilder> _moveAnyAccessorToTheEnd(
            Map<String, POJOPropertyBuilder> sortedProps,
            AnnotatedMember anyAccessor)
    {
        // Here we'll use insertion-order preserving map, since possible alphabetic
        // sorting already done earlier
        Map<String, POJOPropertyBuilder> newAll = new LinkedHashMap<>(sortedProps.size() * 2);
        POJOPropertyBuilder anyGetterProp = null;
        for (POJOPropertyBuilder prop : sortedProps.values()) {
            if (prop.hasFieldOrGetter(anyAccessor)) {
                anyGetterProp = prop;
            } else {
                newAll.put(prop.getName(), prop);
            }
        }
        if (anyGetterProp != null) {
            newAll.put(anyGetterProp.getName(), anyGetterProp);
        }
        return newAll;
    }
    
    /*
    /**********************************************************************
    /* Internal methods, conflict resolution
    /**********************************************************************
     */

    /**
     * Method that will be given a {@link List} with 2 or more accessors
     * that may be in conflict: it will need to remove lower-priority accessors
     * to leave just a single highest-priority accessor to use.
     * If this succeeds method returns {@code true}, otherwise {@code false}.
     *<p>
     * NOTE: method will directly modify given {@code List} directly, regardless
     * of whether it ultimately succeeds or not.
     *
     * @return True if seeming conflict was resolved and there only remains
     *    single accessor
     */
    protected boolean _resolveFieldVsGetter(List<AnnotatedMember> accessors) {
        do {
            AnnotatedMember acc1 = accessors.get(0);
            AnnotatedMember acc2 = accessors.get(1);

            if (acc1 instanceof AnnotatedField) {
                if (acc2 instanceof AnnotatedMethod) {
                    // Method has precedence, remove first entry
                    accessors.remove(0);
                    continue;
                }
            } else if (acc1 instanceof AnnotatedMethod) {
                // Method has precedence, remove second entry
                if (acc2 instanceof AnnotatedField) {
                    accessors.remove(1);
                    continue;
                }
            }
            // Not a field/method pair; fail
            return false;
        } while (accessors.size() > 1);
        return true;
    }

    /*
    /**********************************************************************
    /* Internal methods; helpers
    /**********************************************************************
     */

    protected void reportProblem(String msg, Object... args) {
        if (args.length > 0) {
            msg = msg.formatted(args);
        }
        throw new IllegalArgumentException("Problem with definition of "+_classDef+": "+msg);
    }

    protected POJOPropertyBuilder _property(Map<String, POJOPropertyBuilder> props,
            PropertyName name) {
        String simpleName = name.getSimpleName();
        POJOPropertyBuilder prop = props.get(simpleName);
        if (prop == null) {
            prop = new POJOPropertyBuilder(_config, _annotationIntrospector,
                    _forSerialization, name);
            props.put(simpleName, prop);
        }
        return prop;
    }

    // !!! TODO: deprecate, require use of PropertyName
    protected POJOPropertyBuilder _property(Map<String, POJOPropertyBuilder> props,
            String implName)
    {
        POJOPropertyBuilder prop = props.get(implName);
        if (prop == null) {
            prop = new POJOPropertyBuilder(_config, _annotationIntrospector, _forSerialization,
                    PropertyName.construct(implName));
            props.put(implName, prop);
        }
        return prop;
    }

    private PropertyNamingStrategy _findNamingStrategy()
    {
        Object namingDef = _annotationIntrospector.findNamingStrategy(_config, _classDef);
        if (namingDef == null) {
            return _config.getPropertyNamingStrategy();
        }
        if (namingDef instanceof PropertyNamingStrategy strategy) {
            return strategy;
        }
        // Alas, there's no way to force return type of "either class
        // X or Y" -- need to throw an exception after the fact
        if (!(namingDef instanceof Class)) {
            reportProblem("AnnotationIntrospector returned PropertyNamingStrategy definition of type %s"
                            + "; expected type `PropertyNamingStrategy` or `Class<PropertyNamingStrategy>` instead",
                            ClassUtil.classNameOf(namingDef));
        }
        Class<?> namingClass = (Class<?>)namingDef;
        // 09-Nov-2015, tatu: Need to consider pseudo-value of STD, which means "use default"
        if (namingClass == PropertyNamingStrategy.class) {
            return null;
        }

        if (!PropertyNamingStrategy.class.isAssignableFrom(namingClass)) {
            reportProblem("AnnotationIntrospector returned Class %s; expected `Class<PropertyNamingStrategy>`",
                    ClassUtil.classNameOf(namingClass));
        }
        HandlerInstantiator hi = _config.getHandlerInstantiator();
        if (hi != null) {
            PropertyNamingStrategy pns = hi.namingStrategyInstance(_config, _classDef, namingClass);
            if (pns != null) {
                return pns;
            }
        }
        return (PropertyNamingStrategy) ClassUtil.createInstance(namingClass,
                    _config.canOverrideAccessModifiers());
    }

    // Method called to make sure secondary _creatorProperties entries are updated
    // when main properties are recreated (for some renaming, cleaving)
    protected boolean _replaceCreatorProperty(List<POJOPropertyBuilder> creatorProperties,
            POJOPropertyBuilder prop)
    {
        final AnnotatedParameter ctorParam = prop.getConstructorParameter();
        if (creatorProperties != null && ctorParam != null) {
            for (int i = 0, len = creatorProperties.size(); i < len; ++i) {
                POJOPropertyBuilder cprop = creatorProperties.get(i);
                if (cprop != null) {
                    if (cprop.getConstructorParameter() == ctorParam) {
                        creatorProperties.set(i, prop);
                        return true;
                    }
                }
            }
        }
        return false;
    }

    // @since 3.2
    private final static boolean _nonNullNonEmpty(Collection<?> coll) {
        return (coll != null) && !coll.isEmpty();
    }

    /**
     * Returns true when {@code name} appears in either of the two ignoral sets
     * (per-property or class-level), without merging them. Used by the rename
     * gate in {@link #_renameProperties} where we only need a containment test
     * and want to avoid the merge allocation that {@link #getIgnoredPropertyNames()}
     * would do.
     *
     * @since 3.2
     */
    final boolean _isIgnored(String name) {
        return (_perPropertyIgnoredNames != null && _perPropertyIgnoredNames.contains(name))
                || (_classLevelIgnoredNames != null
                    && _classLevelIgnoredNames.contains(name));
    }

    /**
     * Returns the union of {@code base} (a per-property ignoral view) with
     * {@link #_classLevelIgnoredNames}. Avoids allocation when one or both inputs
     * are {@code null} or when only one of the two sets is present, and falls
     * back to {@link Collections#emptySet()} when both are {@code null}. Shared
     * by {@link #getIgnoredPropertyNames()} and
     * {@link #getNonRescuedIgnoredPropertyNames()}.
     *
     * @since 3.2
     */
    private Set<String> _unionWithClassLevel(Set<String> base) {
        if (_classLevelIgnoredNames == null) {
            return (base == null) ? Collections.emptySet() : base;
        }
        if (base == null) {
            return _classLevelIgnoredNames;
        }
        HashSet<String> result = new HashSet<>(base.size()
                + _classLevelIgnoredNames.size());
        result.addAll(base);
        result.addAll(_classLevelIgnoredNames);
        return result;
    }
}
