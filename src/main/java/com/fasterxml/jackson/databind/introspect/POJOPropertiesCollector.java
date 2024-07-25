package com.fasterxml.jackson.databind.introspect;

import java.lang.reflect.Modifier;
import java.util.*;

import com.fasterxml.jackson.annotation.JacksonInject;
import com.fasterxml.jackson.annotation.JsonCreator;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.cfg.ConstructorDetector;
import com.fasterxml.jackson.databind.cfg.HandlerInstantiator;
import com.fasterxml.jackson.databind.cfg.MapperConfig;
import com.fasterxml.jackson.databind.jdk14.JDK14Util;
import com.fasterxml.jackson.databind.util.ClassUtil;

/**
 * Helper class used for aggregating information about all possible
 * properties of a POJO.
 */
public class POJOPropertiesCollector
{
    /*
    /**********************************************************
    /* Configuration
    /**********************************************************
     */

    /**
     * Configuration settings
     */
    protected final MapperConfig<?> _config;

    /**
     * Handler used for name-mangling of getter, mutator (setter/with) methods
     *
     * @since 2.12
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

    protected final VisibilityChecker<?> _visibilityChecker;

    protected final AnnotationIntrospector _annotationIntrospector;

    /**
     * @since 2.9
     */
    protected final boolean _useAnnotations;

    /**
     * @since 2.15
     */
    protected final boolean _isRecordType;

    /*
    /**********************************************************
    /* Collected property information
    /**********************************************************
     */

    /**
     * State flag we keep to indicate whether actual property information
     * has been collected or not.
     */
    protected boolean _collected;

    /**
     * Set of logical property information collected so far.
     *<p>
     * Since 2.6, this has been constructed (more) lazily, to defer
     * throwing of exceptions for potential conflicts in cases where
     * this may not be an actual problem.
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
     *
     * @since 2.11
     */
    protected Map<PropertyName, PropertyName> _fieldRenameMappings;

    protected LinkedList<AnnotatedMember> _anyGetters;

    /**
     * @since 2.12
     */
    protected LinkedList<AnnotatedMember> _anyGetterField;

    protected LinkedList<AnnotatedMethod> _anySetters;

    protected LinkedList<AnnotatedMember> _anySetterField;

    /**
     * Accessors (field or "getter" method annotated with
     * {@link com.fasterxml.jackson.annotation.JsonKey}
     *
     * @since 2.12
     */
    protected LinkedList<AnnotatedMember> _jsonKeyAccessors;

    /**
     * Accessors (field or "getter" method) annotated with
     * {@link com.fasterxml.jackson.annotation.JsonValue}
     */
    protected LinkedList<AnnotatedMember> _jsonValueAccessors;

    /**
     * Lazily collected list of properties that can be implicitly
     * ignored during serialization; only updated when collecting
     * information for deserialization purposes
     */
    protected HashSet<String> _ignoredPropertyNames;

    /**
     * Lazily collected list of members that were annotated to
     * indicate that they represent mutators for deserializer
     * value injection.
     */
    protected LinkedHashMap<Object, AnnotatedMember> _injectables;

    /**
     * Lazily accessed information about POJO format overrides
     *
     * @since 2.17
     */
    protected JsonFormat.Value _formatOverrides;

    /*
    /**********************************************************
    /* Life-cycle
    /**********************************************************
     */

    /**
     * @since 2.12
     */
    protected POJOPropertiesCollector(MapperConfig<?> config, boolean forSerialization,
            JavaType type, AnnotatedClass classDef,
            AccessorNamingStrategy accessorNaming)
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
    /**********************************************************
    /* Public API
    /**********************************************************
     */

    public MapperConfig<?> getConfig() {
        return _config;
    }

    public JavaType getType() {
        return _type;
    }

    /**
     * @since 2.15
     */
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

    /**
     * @since 2.12
     */
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

    /**
     * @since 2.9
     */
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

    /**
     * Alias for {@link #getAnyGetterMethod()}.
     *
     * @deprecated Since 2.12 use separate {@link #getAnyGetterMethod()} and
     *     {@link #getAnyGetterField()}.
     */
    @Deprecated // since 2.12
    public AnnotatedMember getAnyGetter() {
        return getAnyGetterMethod();
    }

    /**
     * @since 2.12 (before only had "getAnyGetter()")
     */
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

    /**
     * @since 2.12 (before only had "getAnyGetter()")
     */
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
     * Accessor for set of properties that are explicitly marked to be ignored
     * via per-property markers (but NOT class annotations).
     */
    public Set<String> getIgnoredPropertyNames() {
        return _ignoredPropertyNames;
    }

    /**
     * Accessor to find out whether type specified requires inclusion
     * of Object Identifier.
     */
    public ObjectIdInfo getObjectIdInfo()
    {
        ObjectIdInfo info = _annotationIntrospector.findObjectIdInfo(_classDef);
        if (info != null) { // 2.1: may also have different defaults for refs:
            info = _annotationIntrospector.findObjectReferenceInfo(_classDef, info);
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

    /**
     * @since 2.17
     */
    public JsonFormat.Value getFormatOverrides() {
        if (_formatOverrides == null) {
            JsonFormat.Value format = null;

            // Let's check both per-type defaults and annotations;
            // per-type defaults having higher precedence, so start with annotations
            if (_annotationIntrospector != null) {
                format = _annotationIntrospector.findFormat(_classDef);
            }
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
     *
     * @since 2.6
     */
    protected void collectAll()
    {
        _potentialCreators = new PotentialCreators();

        // First: gather basic accessors
        LinkedHashMap<String, POJOPropertyBuilder> props = new LinkedHashMap<String, POJOPropertyBuilder>();

        // 15-Jan-2023, tatu: [databind#3736] Let's avoid detecting fields of Records
        //   altogether (unless we find a good reason to detect them)
        // 17-Apr-2023: Need Records' fields for serialization for cases
        //   like [databind#3628], [databind#3895] and [databind#3992]
        // 22-Jul-2024, tatu: ... and for deserialization sometimes too [databind#4626]
        _addFields(props); // note: populates _fieldRenameMappings

        _addMethods(props);
        // 25-Jan-2016, tatu: Avoid introspecting (constructor-)creators for non-static
        //    inner classes, see [databind#1502]
        // 13-May-2023, PJ: Need to avoid adding creators for Records when serializing [databind#3925]
        // 18-May-2024, tatu: Serialization side does, however, require access to renaming
        //    etc (see f.ex [databind#4452]) so let's not skip
        if (!_classDef.isNonStaticInnerClass()) { // && !(_forSerialization && isRecord)) {
            _addCreators(props);
        }

        // Remove ignored properties, first; this MUST precede annotation merging
        // since logic relies on knowing exactly which accessor has which annotation
        _removeUnwantedProperties(props);
        // and then remove unneeded accessors (wrt read-only, read-write)
        _removeUnwantedAccessors(props);

        // Rename remaining properties
        _renameProperties(props);

        // and now add injectables, but taking care to avoid overlapping ones
        // via creator and regular properties
        _addInjectables(props);

        // then merge annotations, to simplify further processing
        // 26-Sep-2017, tatu: Before 2.9.2 was done earlier but that prevented some of
        //   annotations from getting properly merged
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
        /* 28-Mar-2013, tatu: For deserialization we may also want to remove
         *   final fields, as often they won't make very good mutators...
         *   (although, maybe surprisingly, JVM _can_ force setting of such fields!)
         */
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
            if (Boolean.TRUE.equals(ai.hasAsValue(f))) {
                if (_jsonValueAccessors == null) {
                    _jsonValueAccessors = new LinkedList<>();
                }
                _jsonValueAccessors.add(f);
                continue;
            }
            // 12-October-2020, dominikrebhan: [databind#1458] Support @JsonAnyGetter on
            //   fields and allow @JsonAnySetter to be declared as well.
            boolean anyGetter = Boolean.TRUE.equals(ai.hasAnyGetter(f));
            boolean anySetter = Boolean.TRUE.equals(ai.hasAnySetter(f));
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
                }
                continue;
            }
            String implName = ai.findImplicitPropertyName(f);
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
                pn = ai.findNameForSerialization(f);
            } else {
                pn = ai.findNameForDeserialization(f);
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
            boolean ignored = ai.hasIgnoreMarker(f);

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

        // Then find what is the Primary Constructor (if one exists for type):
        // for Java Records and potentially other types too ("data classes"):
        // Needs to be done early to get implicit names populated
        final PotentialCreator primary;
        if (_isRecordType) {
            primary = JDK14Util.findCanonicalRecordConstructor(_config, _classDef, constructors);
        } else {
            primary = _annotationIntrospector.findDefaultCreator(_config, _classDef,
                    constructors, factories);
        }
        // Next: remove creators marked as explicitly disabled
        _removeDisabledCreators(constructors);
        _removeDisabledCreators(factories);
        
        // And then remove non-annotated static methods that do not look like factories
        _removeNonFactoryStaticMethods(factories, primary);

        // and use annotations to find explicitly chosen Creators
        if (_useAnnotations) { // can't have explicit ones without Annotation introspection
            // Start with Constructors as they have higher precedence:
            _addExplicitlyAnnotatedCreators(creators, constructors, props, false);
            // followed by Factory methods (lower precedence)
            _addExplicitlyAnnotatedCreators(creators, factories, props,
                    creators.hasPropertiesBased());
        }

        // If no Explicitly annotated creators (or Default one) found, look
        // for ones with explicitly-named ({@code @JsonProperty}) parameters
        if (!creators.hasPropertiesBased()) {
            // only discover constructor Creators?
            _addCreatorsWithAnnotatedNames(creators, constructors, primary);
        }

        // But if no annotation-based Creators found, find/use Primary Creator
        // detected earlier, if any
        if (primary != null) {
            if (!creators.hasPropertiesBased()) {
                // ... but only process if still included as a candidate
                if (constructors.remove(primary)
                        || factories.remove(primary)) {
                    // But wait! Could be delegating
                    if (_isDelegatingConstructor(primary)) {
                        creators.addExplicitDelegating(primary);
                    } else {
                        creators.setPropertiesBased(_config, primary, "Primary");
                    }
                }
            }
        }

        // One more thing: if neither explicit (constructor or factory) nor
        // canonical (constructor?), consider implicit Constructor with
        // all named.
        final ConstructorDetector ctorDetector = _config.getConstructorDetector();
        if (!creators.hasPropertiesBasedOrDelegating()
                && !ctorDetector.requireCtorAnnotation()) {
            // But only if no default constructor available OR if we are configured
            // to prefer properties-based Creators
            if ((_classDef.getDefaultConstructor() == null)
                    || ctorDetector.singleArgCreatorDefaultsToProperties()) {
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
        default: // case DEFAULT:
        }

        // Only consider single-arg case, for now
        if (ctor.paramCount() == 1) {
            // Main thing: @JsonValue makes it delegating:
            if ((_jsonValueAccessors != null) && !_jsonValueAccessors.isEmpty()) {
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
            JsonCreator.Mode creatorMode = _useAnnotations
                    ? _annotationIntrospector.findCreatorAnnotation(_config, ctor) : null;
            // 06-Jul-2024, tatu: Can't yet drop DISABLED ones; add all (for now)
            result.add(new PotentialCreator(ctor, creatorMode));
        }
        return (result == null) ? Collections.emptyList() : result;
    }

    private void _removeDisabledCreators(List<PotentialCreator> ctors)
    {
        Iterator<PotentialCreator> it = ctors.iterator();
        while (it.hasNext()) {
            // explicitly prevented? Remove
            if (it.next().creatorMode() == JsonCreator.Mode.DISABLED) {
                it.remove();
            }
        }
    }

    private void _removeNonVisibleCreators(List<PotentialCreator> ctors)
    {
        Iterator<PotentialCreator> it = ctors.iterator();
        while (it.hasNext()) {
            PotentialCreator ctor = it.next();
            if (!_visibilityChecker.isCreatorVisible(ctor.creator())) {
                it.remove();
            }
        }
    }

    private void _removeNonFactoryStaticMethods(List<PotentialCreator> ctors,
            PotentialCreator canonical)
    {
        final Class<?> rawType = _type.getRawClass();
        Iterator<PotentialCreator> it = ctors.iterator();
        while (it.hasNext()) {
            // explicit mode? Retain (for now)
            PotentialCreator ctor = it.next();
            if (ctor.isAnnotated()) {
                continue;
            }
            // Do not trim canonical either
            if (canonical == ctor) {
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
                    collector.setPropertiesBased(_config, ctor, "explicit");
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
        if ((_jsonValueAccessors != null) && !_jsonValueAccessors.isEmpty()) {
            return false;
        }
        if (ctor.paramCount() == 1) {
            // One more possibility: implicit name that maps to implied
            // property with at least one visible accessor
            String implName = ctor.implicitNameSimple(0);
            if (implName != null) {
                POJOPropertyBuilder prop = props.get(implName);
                if ((prop != null) && prop.anyVisible() && !prop.anyIgnorals()) {
                    return true;
                }
            }
            // Second: injectable also suffices
            if ((_annotationIntrospector != null)
                    && _annotationIntrospector.findInjectableValue(ctor.param(0)) != null) {
                return true;
            }
            return false;
        }
        // Trickiest case: rely on existence of implicit names and/or injectables
        return ctor.hasNameOrInjectForAllParams(_config);
    }

    private void _addCreatorsWithAnnotatedNames(PotentialCreators collector,
            List<PotentialCreator> ctors, PotentialCreator defaultCtor)
    {
        final List<PotentialCreator> found = _findCreatorsWithAnnotatedNames(ctors);
        // 16-Jul-2024, tatu: [databind#4620] If Default Creator found, it
        //    will be used to resolve candidate to use, if any
        if (defaultCtor != null) {
            if (found.contains(defaultCtor)) {
                collector.setPropertiesBased(_config, defaultCtor, "implicit");
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
        if (!_visibilityChecker.isCreatorVisible(ctor.creator())) {
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
            if ((_annotationIntrospector != null)
                    && _annotationIntrospector.findInjectableValue(ctor.param(0)) != null) {
                // props-based, continue
            } else {
                // may have explicit preference
                final ConstructorDetector ctorDetector = _config.getConstructorDetector();
                if (ctorDetector.singleArgCreatorDefaultsToDelegating()) {
                    return false;
                }
                // if not, prefer Properties-based if explicit preference OR
                // property with same name with at least one visible accessor
                if (!ctorDetector.singleArgCreatorDefaultsToProperties()) {
                    POJOPropertyBuilder prop = props.get(ctor.implicitNameSimple(0));
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
            final POJOPropertyBuilder prop;

            if (!hasExplicit && (implName == null)) {
                // Important: if neither implicit nor explicit name, cannot make use of
                // this creator parameter -- may or may not be a problem, verified at a later point.
                prop = null;
            } else {
                // 27-Dec-2019, tatu: [databind#2527] may need to rename according to field
                if (implName != null) {
                    String n = _checkRenameByField(implName.getSimpleName());
                    implName = PropertyName.construct(n);
                }
                prop = (implName == null)
                        ? _property(props, explName) : _property(props, implName);
                prop.addCtor(param, hasExplicit ? explName : implName, hasExplicit, true, false);
            }
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
                _addGetterMethod(props, m, _annotationIntrospector);
            } else if (argCount == 1) { // setters
                _addSetterMethod(props, m, _annotationIntrospector);
            } else if (argCount == 2) { // any setter?
                if (Boolean.TRUE.equals(_annotationIntrospector.hasAnySetter(m))) {
                    if (_anySetters == null) {
                        _anySetters = new LinkedList<>();
                    }
                    _anySetters.add(m);
                }
            }
        }
    }

    protected void _addGetterMethod(Map<String, POJOPropertyBuilder> props,
            AnnotatedMethod m, AnnotationIntrospector ai)
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
        if (Boolean.TRUE.equals(ai.hasAnyGetter(m))) {
            if (_anyGetters == null) {
                _anyGetters = new LinkedList<>();
            }
            _anyGetters.add(m);
            return;
        }
        // @JsonKey?
        if (Boolean.TRUE.equals(ai.hasAsKey(_config, m))) {
            if (_jsonKeyAccessors == null) {
                _jsonKeyAccessors = new LinkedList<>();
            }
            _jsonKeyAccessors.add(m);
            return;
        }
        // @JsonValue?
        if (Boolean.TRUE.equals(ai.hasAsValue(m))) {
            if (_jsonValueAccessors == null) {
                _jsonValueAccessors = new LinkedList<>();
            }
            _jsonValueAccessors.add(m);
            return;
        }
        String implName; // from naming convention
        boolean visible;

        PropertyName pn = ai.findNameForSerialization(m);
        boolean nameExplicit = (pn != null);

        if (!nameExplicit) { // no explicit name; must consider implicit
            implName = ai.findImplicitPropertyName(m);
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
            implName = ai.findImplicitPropertyName(m);
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
        boolean ignore = ai.hasIgnoreMarker(m);
        _property(props, implName).addGetter(m, pn, nameExplicit, visible, ignore);
    }

    protected void _addSetterMethod(Map<String, POJOPropertyBuilder> props,
            AnnotatedMethod m, AnnotationIntrospector ai)
    {
        String implName; // from naming convention
        boolean visible;
        PropertyName pn = ai.findNameForDeserialization(m);
        boolean nameExplicit = (pn != null);
        if (!nameExplicit) { // no explicit name; must follow naming convention
            implName = ai.findImplicitPropertyName(m);
            if (implName == null) {
                implName = _accessorNaming.findNameForMutator(m, m.getName());
            }
            if (implName == null) { // if not, must skip
            	return;
            }
            visible = _visibilityChecker.isSetterVisible(m);
        } else { // explicit indication of inclusion, but may be empty
            // we still need implicit name to link with other pieces
            implName = ai.findImplicitPropertyName(m);
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
        final boolean ignore = ai.hasIgnoreMarker(m);
        _property(props, implName)
            .addSetter(m, pn, nameExplicit, visible, ignore);
    }

    protected void _addInjectables(Map<String, POJOPropertyBuilder> props)
    {
        // first fields, then methods, to allow overriding
        for (AnnotatedField f : _classDef.fields()) {
            _doAddInjectable(_annotationIntrospector.findInjectableValue(f), f);
        }

        for (AnnotatedMethod m : _classDef.memberMethods()) {
            // for now, only allow injection of a single arg (to be changed in future?)
            if (m.getParameterCount() != 1) {
                continue;
            }
            _doAddInjectable(_annotationIntrospector.findInjectableValue(m), m);
        }
    }

    protected void _doAddInjectable(JacksonInject.Value injectable, AnnotatedMember m)
    {
        if (injectable == null) {
            return;
        }
        Object id = injectable.getId();
        if (_injectables == null) {
            _injectables = new LinkedHashMap<Object, AnnotatedMember>();
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

    // @since 2.11
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
    /**********************************************************
    /* Internal methods; removing ignored properties
    /**********************************************************
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
     * Helper method called to add explicitly ignored properties to a list
     * of known ignored properties; this helps in proper reporting of
     * errors.
     */
    protected void _collectIgnorals(String name)
    {
        if (!_forSerialization && (name != null)) {
            if (_ignoredPropertyNames == null) {
                _ignoredPropertyNames = new HashSet<>();
            }
            _ignoredPropertyNames.add(name);
        }
    }

    /*
    /**********************************************************
    /* Internal methods; renaming properties
    /**********************************************************
     */

    protected void _renameProperties(Map<String, POJOPropertyBuilder> props)
    {
        // With renaming need to do in phases: first, find properties to rename
        Iterator<Map.Entry<String,POJOPropertyBuilder>> it = props.entrySet().iterator();
        LinkedList<POJOPropertyBuilder> renamed = null;
        while (it.hasNext()) {
            Map.Entry<String, POJOPropertyBuilder> entry = it.next();
            POJOPropertyBuilder prop = entry.getValue();

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
                    if (_ignoredPropertyNames != null) {
                        _ignoredPropertyNames.remove(name);
                    }
                }
            }
        }
    }

    protected void _renameUsing(Map<String, POJOPropertyBuilder> propMap,
            PropertyNamingStrategy naming)
    {
        // [databind#4409]: Need to skip renaming for Enums, unless Enums are handled as OBJECT format
        if (_type.isEnumType()) {
            if (getFormatOverrides().getShape() != JsonFormat.Shape.OBJECT) {
                return;
            }
        }
        POJOPropertyBuilder[] props = propMap.values().toArray(new POJOPropertyBuilder[propMap.size()]);
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
            PropertyName wrapperName = _annotationIntrospector.findWrapperName(member);
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
    /**********************************************************
    /* Internal methods, sorting
    /**********************************************************
     */

    // First, order by(explicit ordering and/or alphabetic),
    // then by (optional) index (if any)
    // and then implicitly order creator properties before others)

    protected void _sortProperties(Map<String, POJOPropertyBuilder> props)
    {
        // Then how about explicit ordering?
        final AnnotationIntrospector intr = _annotationIntrospector;
        Boolean alpha = intr.findSerializationSortAlphabetically(_classDef);
        final boolean sortAlpha = (alpha == null)
                ? _config.shouldSortPropertiesAlphabetically()
                : alpha.booleanValue();
        final boolean indexed = _anyIndexed(props.values());

        String[] propertyOrder = intr.findSerializationPropertyOrder(_classDef);

        // no sorting? no need to shuffle, then
        if (!sortAlpha && !indexed && (_creatorProperties == null) && (propertyOrder == null)) {
            return;
        }
        int size = props.size();
        Map<String, POJOPropertyBuilder> all;
        // Need to (re)sort alphabetically?
        if (sortAlpha) {
            all = new TreeMap<String,POJOPropertyBuilder>();
        } else {
            all = new LinkedHashMap<String,POJOPropertyBuilder>(size+size);
        }

        for (POJOPropertyBuilder prop : props.values()) {
            all.put(prop.getName(), prop);
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
        if (indexed) {
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
        if ((_creatorProperties != null)
                && (!sortAlpha || _config.isEnabled(MapperFeature.SORT_CREATOR_PROPERTIES_FIRST))) {
            /* As per [databind#311], this is bit delicate; but if alphabetic ordering
             * is mandated, at least ensure creator properties are in alphabetic
             * order. Related question of creator vs non-creator is punted for now,
             * so creator properties still fully predate non-creator ones.
             */
            Collection<POJOPropertyBuilder> cr;
            // 18-Jun-2024, tatu: [databind#4580] We may want to retain declaration
            //    order regardless
            boolean sortCreatorPropsByAlpha = sortAlpha
                    && !_config.isEnabled(MapperFeature.SORT_CREATOR_PROPERTIES_BY_DECLARATION_ORDER);
            if (sortCreatorPropsByAlpha) {
                TreeMap<String, POJOPropertyBuilder> sorted =
                        new TreeMap<String,POJOPropertyBuilder>();
                for (POJOPropertyBuilder prop : _creatorProperties) {
                    if (prop != null) {
                        sorted.put(prop.getName(), prop);
                    }
                }
                cr = sorted.values();
            } else {
                cr = _creatorProperties;
            }
            for (POJOPropertyBuilder prop : cr) {
                if (prop == null) {
                    continue;
                }
                // 16-Jan-2016, tatu: Related to [databind#1317], make sure not to accidentally
                //    add back pruned creator properties!

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

    /*
    /**********************************************************
    /* Internal methods, conflict resolution
    /**********************************************************
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
    /**********************************************************
    /* Internal methods; helpers
    /**********************************************************
     */

    protected void reportProblem(String msg, Object... args) {
        if (args.length > 0) {
            msg = String.format(msg, args);
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
        Object namingDef = _annotationIntrospector.findNamingStrategy(_classDef);
        if (namingDef == null) {
            return _config.getPropertyNamingStrategy();
        }
        if (namingDef instanceof PropertyNamingStrategy) {
            return (PropertyNamingStrategy) namingDef;
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
        if (creatorProperties != null) {
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
}
