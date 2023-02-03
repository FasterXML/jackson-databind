package com.fasterxml.jackson.databind.introspect;

import java.util.*;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.cfg.ConfigOverride;
import com.fasterxml.jackson.databind.cfg.MapperConfig;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.fasterxml.jackson.databind.util.ClassUtil;

/**
 * Helper class used for aggregating information about a single
 * potential POJO property.
 */
public class POJOPropertyBuilder
    extends BeanPropertyDefinition
    implements Comparable<POJOPropertyBuilder>
{
    /**
     * Marker value used to denote that no reference-property information found for
     * this property
     *
     * @since 2.9
     */
    private final static AnnotationIntrospector.ReferenceProperty NOT_REFEFERENCE_PROP =
            AnnotationIntrospector.ReferenceProperty.managed("");

    /**
     * Whether property is being composed for serialization
     * (true) or deserialization (false)
     */
    protected final boolean _forSerialization;

    protected final MapperConfig<?> _config;

    protected final AnnotationIntrospector _annotationIntrospector;

    /**
     * External name of logical property; may change with
     * renaming (by new instance being constructed using
     * a new name)
     */
    protected final PropertyName _name;

    /**
     * Original internal name, derived from accessor, of this
     * property. Will not be changed by renaming.
     */
    protected final PropertyName _internalName;

    protected Linked<AnnotatedField> _fields;

    protected Linked<AnnotatedParameter> _ctorParameters;

    protected Linked<AnnotatedMethod> _getters;

    protected Linked<AnnotatedMethod> _setters;

    protected transient PropertyMetadata _metadata;

    /**
     * Lazily accessed information about this property iff it is a forward or
     * back reference.
     *
     * @since 2.9
     */
    protected transient AnnotationIntrospector.ReferenceProperty _referenceInfo;

    public POJOPropertyBuilder(MapperConfig<?> config, AnnotationIntrospector ai,
            boolean forSerialization, PropertyName internalName) {
        this(config, ai, forSerialization, internalName, internalName);
    }

    protected POJOPropertyBuilder(MapperConfig<?> config, AnnotationIntrospector ai,
            boolean forSerialization, PropertyName internalName, PropertyName name)
    {
        _config = config;
        _annotationIntrospector = ai;
        _internalName = internalName;
        _name = name;
        _forSerialization = forSerialization;
    }

    // protected since 2.9 (was public before)
    protected POJOPropertyBuilder(POJOPropertyBuilder src, PropertyName newName)
    {
        _config = src._config;
        _annotationIntrospector = src._annotationIntrospector;
        _internalName = src._internalName;
        _name = newName;
        _fields = src._fields;
        _ctorParameters = src._ctorParameters;
        _getters = src._getters;
        _setters = src._setters;
        _forSerialization = src._forSerialization;
    }

    /*
    /**********************************************************
    /* Mutant factory methods
    /**********************************************************
     */

    @Override
    public POJOPropertyBuilder withName(PropertyName newName) {
        return new POJOPropertyBuilder(this, newName);
    }

    @Override
    public POJOPropertyBuilder withSimpleName(String newSimpleName)
    {
        PropertyName newName = _name.withSimpleName(newSimpleName);
        return (newName == _name) ? this : new POJOPropertyBuilder(this, newName);
    }

    /*
    /**********************************************************
    /* Comparable implementation: sort alphabetically, except
    /* that properties with constructor parameters sorted
    /* before other properties
    /**********************************************************
     */

    @Override
    public int compareTo(POJOPropertyBuilder other)
    {
        // first, if one has ctor params, that should come first:
        if (_ctorParameters != null) {
            if (other._ctorParameters == null) {
                return -1;
            }
        } else if (other._ctorParameters != null) {
            return 1;
        }
        /* otherwise sort by external name (including sorting of
         * ctor parameters)
         */
        return getName().compareTo(other.getName());
    }

    /*
    /**********************************************************
    /* BeanPropertyDefinition implementation, name/type
    /**********************************************************
     */

    @Override
    public String getName() {
        return (_name == null) ? null : _name.getSimpleName();
    }

    @Override
    public PropertyName getFullName() {
        return _name;
    }

    @Override
    public boolean hasName(PropertyName name) {
        return _name.equals(name);
    }

    @Override
    public String getInternalName() { return _internalName.getSimpleName(); }

    @Override
    public PropertyName getWrapperName() {
        /* 13-Mar-2013, tatu: Accessing via primary member SHOULD work,
         *   due to annotation merging. However, I have seen some problems
         *   with this access (for other annotations)... so if this should
         *   occur, try commenting out full traversal code
         */
        AnnotatedMember member = getPrimaryMember();
        return (member == null || _annotationIntrospector == null) ? null
                : _annotationIntrospector.findWrapperName(member);
    	/*
        return fromMemberAnnotations(new WithMember<PropertyName>() {
            @Override
            public PropertyName withMember(AnnotatedMember member) {
                return _annotationIntrospector.findWrapperName(member);
            }
        });
        */
    }

    @Override
    public boolean isExplicitlyIncluded() {
        return _anyExplicits(_fields)
                || _anyExplicits(_getters)
                || _anyExplicits(_setters)
                // 16-Jan-2016, tatu: Creator names are special, in that name should exist too;
                //   reason for this is [databind#1317]. Let's hope this works well, may need
                //   to tweak further if this lowers visibility
//                || _anyExplicits(_ctorParameters)
                || _anyExplicitNames(_ctorParameters)
                ;
    }

    @Override
    public boolean isExplicitlyNamed() {
        return _anyExplicitNames(_fields)
                || _anyExplicitNames(_getters)
                || _anyExplicitNames(_setters)
                || _anyExplicitNames(_ctorParameters)
                ;
    }

    /*
    /**********************************************************
    /* Simple metadata
    /**********************************************************
     */

    @Override
    public PropertyMetadata getMetadata()
    {
        if (_metadata == null) {
            // 20-Jun-2020, tatu: Unfortunately strict checks lead to [databind#2757]
            //   so we will need to try to avoid them at this point
            final AnnotatedMember prim = getPrimaryMemberUnchecked();

            if (prim == null) {
                _metadata = PropertyMetadata.STD_REQUIRED_OR_OPTIONAL;
            } else {
                final Boolean b = _annotationIntrospector.hasRequiredMarker(prim);
                final String desc = _annotationIntrospector.findPropertyDescription(prim);
                final Integer idx = _annotationIntrospector.findPropertyIndex(prim);
                final String def = _annotationIntrospector.findPropertyDefaultValue(prim);

                if (b == null && idx == null && def == null) {
                    _metadata = (desc == null) ? PropertyMetadata.STD_REQUIRED_OR_OPTIONAL
                            : PropertyMetadata.STD_REQUIRED_OR_OPTIONAL.withDescription(desc);
                } else {
                    _metadata = PropertyMetadata.construct(b, desc, idx, def);
                }
                if (!_forSerialization) {
                    _metadata = _getSetterInfo(_metadata, prim);
                }
            }
        }
        return _metadata;
    }

    /**
     * Helper method that contains logic for accessing and merging all setter
     * information that we needed, regarding things like possible merging
     * of property value, and handling of incoming nulls.
     * Only called for deserialization purposes.
     */
    protected PropertyMetadata _getSetterInfo(PropertyMetadata metadata,
            AnnotatedMember primary)
    {
        boolean needMerge = true;
        Nulls valueNulls = null;
        Nulls contentNulls = null;

        // Slightly confusing: first, annotations should be accessed via primary member
        // (mutator); but accessor is needed for actual merge operation. So

        AnnotatedMember acc = getAccessor();

        if (primary != null) {
            // Ok, first: does property itself have something to say?
            if (_annotationIntrospector != null) {
                if (acc != null) {
                    Boolean b = _annotationIntrospector.findMergeInfo(primary);
                    if (b != null) {
                        needMerge = false;
                        if (b.booleanValue()) {
                            metadata = metadata.withMergeInfo(PropertyMetadata.MergeInfo.createForPropertyOverride(acc));
                        }
                    }
                }
                JsonSetter.Value setterInfo = _annotationIntrospector.findSetterInfo(primary);
                if (setterInfo != null) {
                    valueNulls = setterInfo.nonDefaultValueNulls();
                    contentNulls = setterInfo.nonDefaultContentNulls();
                }
            }
            // If not, config override?
            // 25-Oct-2016, tatu: Either this, or type of accessor...
            if (needMerge || (valueNulls == null) || (contentNulls == null)) {
                // 20-Jun-2020, tatu: Related to [databind#2757], need to find type
                //   but keeping mind that type for setters is trickier; and that
                //   generic typing gets tricky as well.
                Class<?> rawType = _rawTypeOf(primary);
                ConfigOverride co = _config.getConfigOverride(rawType);
                JsonSetter.Value setterInfo = co.getSetterInfo();
                if (setterInfo != null) {
                    if (valueNulls == null) {
                        valueNulls = setterInfo.nonDefaultValueNulls();
                    }
                    if (contentNulls == null) {
                        contentNulls = setterInfo.nonDefaultContentNulls();
                    }
                }
                if (needMerge && (acc != null)) {
                    Boolean b = co.getMergeable();
                    if (b != null) {
                        needMerge = false;
                        if (b.booleanValue()) {
                            metadata = metadata.withMergeInfo(PropertyMetadata.MergeInfo.createForTypeOverride(acc));
                        }
                    }
                }
            }
        }
        if (needMerge || (valueNulls == null) || (contentNulls == null)) {
            JsonSetter.Value setterInfo = _config.getDefaultSetterInfo();
            if (valueNulls == null) {
                valueNulls = setterInfo.nonDefaultValueNulls();
            }
            if (contentNulls == null) {
                contentNulls = setterInfo.nonDefaultContentNulls();
            }
            if (needMerge) {
                Boolean b = _config.getDefaultMergeable();
                if (Boolean.TRUE.equals(b) && (acc != null)) {
                    metadata = metadata.withMergeInfo(PropertyMetadata.MergeInfo.createForDefaults(acc));
                }
            }
        }
        if ((valueNulls != null) || (contentNulls != null)) {
            metadata = metadata.withNulls(valueNulls, contentNulls);
        }
        return metadata;
    }

    /**
     * Type determined from the primary member for the property being built,
     * considering precedence according to whether we are processing serialization
     * or deserialization.
     */
    @Override
    public JavaType getPrimaryType() {
        if (_forSerialization) {
            AnnotatedMember m = getGetter();
            if (m == null) {
                m = getField();
                if (m == null) {
                    // 09-Feb-2017, tatu: Not sure if this or `null` but...
                    return TypeFactory.unknownType();
                }
            }
            return m.getType();
        }
        AnnotatedMember m = getConstructorParameter();
        if (m == null) {
            m = getSetter();
            // Important: can't try direct type access for setter; what we need is
            // type of the first parameter
            if (m != null) {
                return ((AnnotatedMethod) m).getParameterType(0);
            }
            m = getField();
        }
        // for setterless properties, however, can further try getter
        if (m == null) {
            m = getGetter();
            if (m == null) {
                return TypeFactory.unknownType();
            }
        }
        return m.getType();
    }

    @Override
    public Class<?> getRawPrimaryType() {
        return getPrimaryType().getRawClass();
    }

    /*
    /**********************************************************
    /* BeanPropertyDefinition implementation, accessor access
    /**********************************************************
     */

    @Override
    public boolean hasGetter() { return _getters != null; }

    @Override
    public boolean hasSetter() { return _setters != null; }

    @Override
    public boolean hasField() { return _fields != null; }

    @Override
    public boolean hasConstructorParameter() { return _ctorParameters != null; }

    @Override
    public boolean couldDeserialize() {
        return (_ctorParameters != null) || (_setters != null) || (_fields != null);
    }

    @Override
    public boolean couldSerialize() {
        return (_getters != null) || (_fields != null);
    }

    @Override
    public AnnotatedMethod getGetter()
    {
        // Easy with zero or one getters...
        Linked<AnnotatedMethod> curr = _getters;
        if (curr == null) {
            return null;
        }
        Linked<AnnotatedMethod> next = curr.next;
        if (next == null) {
            return curr.value;
        }
        // But if multiple, verify that they do not conflict...
        for (; next != null; next = next.next) {
            /* [JACKSON-255] Allow masking, i.e. do not report exception if one
             *   is in super-class from the other
             */
            Class<?> currClass = curr.value.getDeclaringClass();
            Class<?> nextClass = next.value.getDeclaringClass();
            if (currClass != nextClass) {
                if (currClass.isAssignableFrom(nextClass)) { // next is more specific
                    curr = next;
                    continue;
                }
                if (nextClass.isAssignableFrom(currClass)) { // current more specific
                    continue;
                }
            }
            /* 30-May-2014, tatu: Three levels of precedence:
             *
             * 1. Regular getters ("getX")
             * 2. Is-getters ("isX")
             * 3. Implicit, possible getters ("x")
             */
            int priNext = _getterPriority(next.value);
            int priCurr = _getterPriority(curr.value);

            if (priNext != priCurr) {
                if (priNext < priCurr) {
                    curr = next;
                }
                continue;
            }
            throw new IllegalArgumentException("Conflicting getter definitions for property \""+getName()+"\": "
                    +curr.value.getFullName()+" vs "+next.value.getFullName());
        }
        // One more thing; to avoid having to do it again...
        _getters = curr.withoutNext();
        return curr.value;
    }

    /**
     * Variant of {@link #getGetter} that does NOT trigger pruning of
     * getter candidates.
     */
    protected AnnotatedMethod getGetterUnchecked()
    {
        Linked<AnnotatedMethod> curr = _getters;
        if (curr == null) {
            return null;
        }
        return curr.value;
    }

    @Override
    public AnnotatedMethod getSetter()
    {
        // Easy with zero or one setters...
        Linked<AnnotatedMethod> curr = _setters;
        if (curr == null) {
            return null;
        }
        Linked<AnnotatedMethod> next = curr.next;
        if (next == null) {
            return curr.value;
        }
        // But if multiple, verify that they do not conflict...
        for (; next != null; next = next.next) {
            AnnotatedMethod selected = _selectSetter(curr.value, next.value);
            if (selected == curr.value) {
                continue;
            }
            if (selected == next.value) {
                curr = next;
                continue;
            }
            // 10-May-2021, tatu: unbreakable tie, for now; offline handling
            return _selectSetterFromMultiple(curr, next);
        }

        // One more thing; to avoid having to do it again...
        _setters = curr.withoutNext();
        return curr.value;
    }

    /**
     * Variant of {@link #getSetter} that does NOT trigger pruning of
     * setter candidates.
     */
    protected AnnotatedMethod getSetterUnchecked()
    {
        Linked<AnnotatedMethod> curr = _setters;
        if (curr == null) {
            return null;
        }
        return curr.value;
    }

    /**
     * Helper method called in cases where we have encountered two setter methods
     * that have same precedence and cannot be resolved. This does not yet necessarily
     * mean a failure since it is possible something with a higher precedence could
     * still be found; handling is just separated into separate method for convenience.
     *
     * @param curr
     * @param next
     *
     * @return Chosen setter method, if any
     *
     * @throws IllegalArgumentException If conflict could not be resolved
     *
     * @since 2.13
     */
    protected AnnotatedMethod _selectSetterFromMultiple(Linked<AnnotatedMethod> curr,
            Linked<AnnotatedMethod> next)
    {
        // First: store reference to the initial possible conflict
        List<AnnotatedMethod> conflicts = new ArrayList<>();
        conflicts.add(curr.value);
        conflicts.add(next.value);

        next = next.next;
        for (; next != null; next = next.next) {
            AnnotatedMethod selected = _selectSetter(curr.value, next.value);
            if (selected == curr.value) {
                // No change, next was lower-precedence
                continue;
            }
            if (selected == next.value) {
                // Hooray! Found a higher-priority one; clear conflict list
                conflicts.clear();
                curr = next;
                continue;
            }
            // Tie means one more non-resolved, add
            conflicts.add(next.value);
        }

        // It is possible we resolved it; if so:
        if (conflicts.isEmpty()) {
            _setters = curr.withoutNext();
            return curr.value;
        }
        // Otherwise
        String desc = conflicts.stream().map(AnnotatedMethod::getFullName)
                .collect(Collectors.joining(" vs "));
        throw new IllegalArgumentException(String.format(
                "Conflicting setter definitions for property \"%s\": %s",
                getName(), desc));
    }

    // @since 2.13
    protected AnnotatedMethod _selectSetter(AnnotatedMethod currM, AnnotatedMethod nextM)
    {
        // Allow masking, i.e. do not fail if one is in super-class from the other
        final Class<?> currClass = currM.getDeclaringClass();
        final Class<?> nextClass = nextM.getDeclaringClass();
        if (currClass != nextClass) {
            if (currClass.isAssignableFrom(nextClass)) { // next is more specific
                return nextM;
            }
            if (nextClass.isAssignableFrom(currClass)) { // current more specific
                return currM;
            }
        }

        /* 30-May-2014, tatu: Two levels of precedence:
         *
         * 1. Regular setters ("setX(...)")
         * 2. Implicit, possible setters ("x(...)")
         */
        // 25-Apr-2021, tatu: This is probably wrong, should not rely on
        //    hard-coded "set" prefix here.
        int priNext = _setterPriority(nextM);
        int priCurr = _setterPriority(currM);

        if (priNext != priCurr) {
            // Smaller value, higher; so, if next has higher precedence:
            if (priNext < priCurr) {
                return nextM;
            }
            // otherwise current one has, proceed
            return currM;
        }
        // 11-Dec-2015, tatu: As per [databind#1033] allow pluggable conflict resolution
        return (_annotationIntrospector == null) ? null
                : _annotationIntrospector.resolveSetterConflict(_config, currM, nextM);
    }

    @Override
    public AnnotatedField getField()
    {
        if (_fields == null) {
            return null;
        }
        // If multiple, verify that they do not conflict...
        AnnotatedField field = _fields.value;
        Linked<AnnotatedField> next = _fields.next;
        for (; next != null; next = next.next) {
            AnnotatedField nextField = next.value;
            Class<?> fieldClass = field.getDeclaringClass();
            Class<?> nextClass = nextField.getDeclaringClass();
            if (fieldClass != nextClass) {
                if (fieldClass.isAssignableFrom(nextClass)) { // next is more specific
                    field = nextField;
                    continue;
                }
                if (nextClass.isAssignableFrom(fieldClass)) { // getter more specific
                    continue;
                }
            }
            throw new IllegalArgumentException("Multiple fields representing property \""+getName()+"\": "
                    +field.getFullName()+" vs "+nextField.getFullName());
        }
        return field;
    }

    /**
     * Variant of {@link #getField} that does NOT trigger pruning of
     * Field candidates.
     */
    protected AnnotatedField getFieldUnchecked()
    {
        Linked<AnnotatedField> curr = _fields;
        if (curr == null) {
            return null;
        }
        return curr.value;
    }

    @Override
    public AnnotatedParameter getConstructorParameter()
    {
        if (_ctorParameters == null) {
            return null;
        }
        /* Hmmh. Checking for constructor parameters is trickier; for one,
         * we must allow creator and factory method annotations.
         * If this is the case, constructor parameter has the precedence.
         *
         * So, for now, just try finding the first constructor parameter;
         * if none, first factory method. And don't check for dups, if we must,
         * can start checking for them later on.
         */
        Linked<AnnotatedParameter> curr = _ctorParameters;
        do {
            if (curr.value.getOwner() instanceof AnnotatedConstructor) {
                return curr.value;
            }
            curr = curr.next;
        } while (curr != null);
        return _ctorParameters.value;
    }

    @Override
    public Iterator<AnnotatedParameter> getConstructorParameters() {
        if (_ctorParameters == null) {
            return ClassUtil.emptyIterator();
        }
        return new MemberIterator<AnnotatedParameter>(_ctorParameters);
    }

    @Override
    public AnnotatedMember getPrimaryMember() {
        if (_forSerialization) {
            return getAccessor();
        }
        AnnotatedMember m = getMutator();
        // for setterless properties, however...
        if (m == null) {
            m = getAccessor();
        }
        return m;
    }

    // Sometimes we need to actually by-pass failures related to conflicting
    // getters or setters (see [databind#2757] for specific example); if so,
    // this method is to be used instead of `getPrimaryMember()`
    // @since 2.11.1
    protected AnnotatedMember getPrimaryMemberUnchecked() {
        if (_forSerialization) { // Inlined `getAccessor()` logic:
            // Inlined `getGetter()`:
            if (_getters != null) {
                return _getters.value;
            }
            // Inlined `getField()`:
            if (_fields != null) {
                return _fields.value;
            }
            return null;
        }

        // Otherwise, inlined `getMutator()` logic:

        // Inlined `getConstructorParameter()`:
        if (_ctorParameters != null) {
            return _ctorParameters.value;
        }
        // Inlined `getSetter()`:
        if (_setters != null) {
            return _setters.value;
        }
        // Inlined `getField()`:
        if (_fields != null) {
            return _fields.value;
        }
        // but to support setterless-properties, also include part of
        // `getAccessor()` not yet covered, `getGetter()`:
        if (_getters != null) {
            return _getters.value;
        }
        return null;
    }

    protected int _getterPriority(AnnotatedMethod m)
    {
        final String name = m.getName();
        // [databind#238]: Also, regular getters have precedence over "is-getters"
        if (name.startsWith("get") && name.length() > 3) {
            // should we check capitalization?
            return 1;
        }
        if (name.startsWith("is") && name.length() > 2) {
            return 2;
        }
        return 3;
    }

    protected int _setterPriority(AnnotatedMethod m)
    {
        final String name = m.getName();
        if (name.startsWith("set") && name.length() > 3) {
            // should we check capitalization?
            return 1;
        }
        return 2;
    }

    /*
    /**********************************************************
    /* Implementations of refinement accessors
    /**********************************************************
     */

    @Override
    public Class<?>[] findViews() {
        return fromMemberAnnotations(new WithMember<Class<?>[]>() {
            @Override
            public Class<?>[] withMember(AnnotatedMember member) {
                return _annotationIntrospector.findViews(member);
            }
        });
    }

    @Override
    public AnnotationIntrospector.ReferenceProperty findReferenceType() {
        // 30-Mar-2017, tatu: Access lazily but retain information since it needs
        //   to be accessed multiple times during processing.
        AnnotationIntrospector.ReferenceProperty result = _referenceInfo;
        if (result != null) {
            if (result == NOT_REFEFERENCE_PROP) {
                return null;
            }
            return result;
        }
        result = fromMemberAnnotations(new WithMember<AnnotationIntrospector.ReferenceProperty>() {
            @Override
            public AnnotationIntrospector.ReferenceProperty withMember(AnnotatedMember member) {
                return _annotationIntrospector.findReferenceType(member);
            }
        });
        _referenceInfo = (result == null) ? NOT_REFEFERENCE_PROP : result;
        return result;
    }

    @Override
    public boolean isTypeId() {
        Boolean b = fromMemberAnnotations(new WithMember<Boolean>() {
            @Override
            public Boolean withMember(AnnotatedMember member) {
                return _annotationIntrospector.isTypeId(member);
            }
        });
        return (b != null) && b.booleanValue();
    }

    @Override
    public ObjectIdInfo findObjectIdInfo() {
        return fromMemberAnnotations(new WithMember<ObjectIdInfo>() {
            @Override
            public ObjectIdInfo withMember(AnnotatedMember member) {
                ObjectIdInfo info = _annotationIntrospector.findObjectIdInfo(member);
                if (info != null) {
                    info = _annotationIntrospector.findObjectReferenceInfo(member, info);
                }
                return info;
            }
        });
    }

    @Override
    public JsonInclude.Value findInclusion() {
        AnnotatedMember a = getAccessor();
        // 16-Apr-2106, tatu: Let's include per-type default inclusion too
        // 17-Aug-2016, tatu: Do NOT include global, or per-type defaults, because
        //    not all of this information (specifically, enclosing type's settings)
        //    is available here
        JsonInclude.Value v = (_annotationIntrospector == null) ?
                null : _annotationIntrospector.findPropertyInclusion(a);
        return (v == null) ? JsonInclude.Value.empty() : v;
    }

    public JsonProperty.Access findAccess() {
        return fromMemberAnnotationsExcept(new WithMember<JsonProperty.Access>() {
            @Override
            public JsonProperty.Access withMember(AnnotatedMember member) {
                return _annotationIntrospector.findPropertyAccess(member);
            }
        }, JsonProperty.Access.AUTO);
    }

    /*
    /**********************************************************
    /* Data aggregation
    /**********************************************************
     */

    public void addField(AnnotatedField a, PropertyName name, boolean explName, boolean visible, boolean ignored) {
        _fields = new Linked<AnnotatedField>(a, _fields, name, explName, visible, ignored);
    }

    public void addCtor(AnnotatedParameter a, PropertyName name, boolean explName, boolean visible, boolean ignored) {
        _ctorParameters = new Linked<AnnotatedParameter>(a, _ctorParameters, name, explName, visible, ignored);
    }

    public void addGetter(AnnotatedMethod a, PropertyName name, boolean explName, boolean visible, boolean ignored) {
        _getters = new Linked<AnnotatedMethod>(a, _getters, name, explName, visible, ignored);
    }

    public void addSetter(AnnotatedMethod a, PropertyName name, boolean explName, boolean visible, boolean ignored) {
        _setters = new Linked<AnnotatedMethod>(a, _setters, name, explName, visible, ignored);
    }

    /**
     * Method for adding all property members from specified collector into
     * this collector.
     */
    public void addAll(POJOPropertyBuilder src)
    {
        _fields = merge(_fields, src._fields);
        _ctorParameters = merge(_ctorParameters, src._ctorParameters);
        _getters= merge(_getters, src._getters);
        _setters = merge(_setters, src._setters);
    }

    private static <T> Linked<T> merge(Linked<T> chain1, Linked<T> chain2)
    {
        if (chain1 == null) {
            return chain2;
        }
        if (chain2 == null) {
            return chain1;
        }
        return chain1.append(chain2);
    }

    /*
    /**********************************************************
    /* Modifications
    /**********************************************************
     */

    /**
     * Method called to remove all entries that are marked as
     * ignored.
     */
    public void removeIgnored()
    {
        _fields = _removeIgnored(_fields);
        _getters = _removeIgnored(_getters);
        _setters = _removeIgnored(_setters);
        _ctorParameters = _removeIgnored(_ctorParameters);
    }

    @Deprecated // since 2.12
    public JsonProperty.Access removeNonVisible(boolean inferMutators) {
        return removeNonVisible(inferMutators, null);
    }

    /**
     * @param inferMutators Whether mutators can be "pulled in" by visible
     *    accessors or not.
     *
     * @since 2.12 (earlier had different signature)
     */
    public JsonProperty.Access removeNonVisible(boolean inferMutators,
            POJOPropertiesCollector parent)
    {
        /* 07-Jun-2015, tatu: With 2.6, we will allow optional definition
         *  of explicit access type for property; if not "AUTO", it will
         *  dictate how visibility checks are applied.
         */
        JsonProperty.Access acc = findAccess();
        if (acc == null) {
            acc = JsonProperty.Access.AUTO;
        }
        switch (acc) {
        case READ_ONLY:
            // [databind#2719]: Need to add ignorals, first, keeping in mind
            // we have not yet resolved explicit names, so include implicit
            // and possible explicit names
            if (parent != null) {
                parent._collectIgnorals(getName());
                for (PropertyName pn : findExplicitNames()) {
                    parent._collectIgnorals(pn.getSimpleName());
                }
            }
            // Remove setters, creators for sure, but fields too if deserializing
            _setters = null;
            _ctorParameters = null;
            if (!_forSerialization) {
                _fields = null;
            }
            break;
        case READ_WRITE:
            // no trimming whatsoever?
            break;
        case WRITE_ONLY:
            // remove getters, definitely, but also fields if serializing
            _getters = null;
            if (_forSerialization) {
                _fields = null;
            }
            break;
        default:
        case AUTO: // the default case: base it on visibility
            _getters = _removeNonVisible(_getters);
            _ctorParameters = _removeNonVisible(_ctorParameters);

            if (!inferMutators || (_getters == null)) {
                _fields = _removeNonVisible(_fields);
                _setters = _removeNonVisible(_setters);
            }
        }
        return acc;
    }

    /**
     * Mutator that will simply drop any constructor parameters property may have.
     *
     * @since 2.5
     */
    public void removeConstructors() {
        _ctorParameters = null;
    }

    /**
     * Method called to trim unnecessary entries, such as implicit
     * getter if there is an explict one available. This is important
     * for later stages, to avoid unnecessary conflicts.
     */
    public void trimByVisibility()
    {
        _fields = _trimByVisibility(_fields);
        _getters = _trimByVisibility(_getters);
        _setters = _trimByVisibility(_setters);
        _ctorParameters = _trimByVisibility(_ctorParameters);
    }

    @SuppressWarnings("unchecked")
    public void mergeAnnotations(boolean forSerialization)
    {
        if (forSerialization) {
            if (_getters != null) {
                AnnotationMap ann = _mergeAnnotations(0, _getters, _fields, _ctorParameters, _setters);
                _getters = _applyAnnotations(_getters, ann);
            } else if (_fields != null) {
                AnnotationMap ann = _mergeAnnotations(0, _fields, _ctorParameters, _setters);
                _fields = _applyAnnotations(_fields, ann);
            }
        } else { // for deserialization
            if (_ctorParameters != null) {
                AnnotationMap ann = _mergeAnnotations(0, _ctorParameters, _setters, _fields, _getters);
                _ctorParameters = _applyAnnotations(_ctorParameters, ann);
            } else if (_setters != null) {
                AnnotationMap ann = _mergeAnnotations(0, _setters, _fields, _getters);
                _setters = _applyAnnotations(_setters, ann);
            } else if (_fields != null) {
                AnnotationMap ann = _mergeAnnotations(0, _fields, _getters);
                _fields = _applyAnnotations(_fields, ann);
            }
        }
    }

    private AnnotationMap _mergeAnnotations(int index,
            Linked<? extends AnnotatedMember>... nodes)
    {
        AnnotationMap ann = _getAllAnnotations(nodes[index]);
        while (++index < nodes.length) {
            if (nodes[index] != null) {
                return AnnotationMap.merge(ann, _mergeAnnotations(index, nodes));
            }
        }
        return ann;
    }

    /**
     * Replacement, as per [databind#868], of simple access to annotations, which
     * does "deep merge" if an as necessary.
     *<pre>
     * nodes[index].value.getAllAnnotations()
     *</pre>
     *
     * @since 2.6
     */
    private <T extends AnnotatedMember> AnnotationMap _getAllAnnotations(Linked<T> node) {
        AnnotationMap ann = node.value.getAllAnnotations();
        if (node.next != null) {
            ann = AnnotationMap.merge(ann, _getAllAnnotations(node.next));
        }
        return ann;
    }

    /**
     * Helper method to handle recursive merging of annotations within accessor class,
     * to ensure no annotations are accidentally dropped within chain when non-visible
     * and secondary accessors are pruned later on.
     *<p>
     * See [databind#868] for more information.
     *
     * @since 2.6
     */
    private <T extends AnnotatedMember> Linked<T> _applyAnnotations(Linked<T> node, AnnotationMap ann) {
        @SuppressWarnings("unchecked")
        T value = (T) node.value.withAnnotations(ann);
        if (node.next != null) {
            node = node.withNext(_applyAnnotations(node.next, ann));
        }
        return node.withValue(value);
    }

    private <T> Linked<T> _removeIgnored(Linked<T> node)
    {
        if (node == null) {
            return node;
        }
        return node.withoutIgnored();
    }

    private <T> Linked<T> _removeNonVisible(Linked<T> node)
    {
        if (node == null) {
            return node;
        }
        return node.withoutNonVisible();
    }

    private <T> Linked<T> _trimByVisibility(Linked<T> node)
    {
        if (node == null) {
            return node;
        }
        return node.trimByVisibility();
    }

    /*
    /**********************************************************
    /* Accessors for aggregate information
    /**********************************************************
     */

    private <T> boolean _anyExplicits(Linked<T> n)
    {
        for (; n != null; n = n.next) {
            if (n.name != null && n.name.hasSimpleName()) {
                return true;
            }
        }
        return false;
    }

    private <T> boolean _anyExplicitNames(Linked<T> n)
    {
        for (; n != null; n = n.next) {
            if (n.name != null && n.isNameExplicit) {
                return true;
            }
        }
        return false;
    }

    public boolean anyVisible() {
        return _anyVisible(_fields)
            || _anyVisible(_getters)
            || _anyVisible(_setters)
            || _anyVisible(_ctorParameters)
        ;
    }

    private <T> boolean _anyVisible(Linked<T> n)
    {
        for (; n != null; n = n.next) {
            if (n.isVisible) {
                return true;
            }
        }
        return false;
    }

    public boolean anyIgnorals() {
        return _anyIgnorals(_fields)
            || _anyIgnorals(_getters)
            || _anyIgnorals(_setters)
            || _anyIgnorals(_ctorParameters)
        ;
    }

    private <T> boolean _anyIgnorals(Linked<T> n)
    {
        for (; n != null; n = n.next) {
            if (n.isMarkedIgnored) {
                return true;
            }
        }
        return false;
    }

    // @since 2.14
    public boolean anyExplicitsWithoutIgnoral() {
        return _anyExplicitsWithoutIgnoral(_fields)
                || _anyExplicitsWithoutIgnoral(_getters)
                || _anyExplicitsWithoutIgnoral(_setters)
                // as per [databind#1317], constructor names are special...
                || _anyExplicitNamesWithoutIgnoral(_ctorParameters);
    }

    // For accessors other than constructor parameters
    private <T> boolean _anyExplicitsWithoutIgnoral(Linked<T> n) {
        for (; n != null; n = n.next) {
            if (!n.isMarkedIgnored
                && (n.name != null && n.name.hasSimpleName())) {
                    return true;
            }
        }
        return false;
    }

    // For constructor parameters
    private <T> boolean _anyExplicitNamesWithoutIgnoral(Linked<T> n) {
        for (; n != null; n = n.next) {
            if (!n.isMarkedIgnored
                && (n.name != null && n.isNameExplicit)) {
                    return true;
            }
        }
        return false;
    }

    /**
     * Method called to find out set of explicit names for accessors
     * bound together due to implicit name.
     *
     * @since 2.4
     */
    public Set<PropertyName> findExplicitNames()
    {
        Set<PropertyName> renamed = null;
        renamed = _findExplicitNames(_fields, renamed);
        renamed = _findExplicitNames(_getters, renamed);
        renamed = _findExplicitNames(_setters, renamed);
        renamed = _findExplicitNames(_ctorParameters, renamed);
        if (renamed == null) {
            return Collections.emptySet();
        }
        return renamed;
    }

    /**
     * Method called when a previous call to {@link #findExplicitNames} found
     * multiple distinct explicit names, and the property this builder represents
     * basically needs to be broken apart and replaced by a set of more than
     * one properties.
     *
     * @since 2.4
     */
    public Collection<POJOPropertyBuilder> explode(Collection<PropertyName> newNames)
    {
        HashMap<PropertyName,POJOPropertyBuilder> props = new HashMap<PropertyName,POJOPropertyBuilder>();
        _explode(newNames, props, _fields);
        _explode(newNames, props, _getters);
        _explode(newNames, props, _setters);
        _explode(newNames, props, _ctorParameters);
        return props.values();
    }

    @SuppressWarnings("unchecked")
    private void _explode(Collection<PropertyName> newNames,
            Map<PropertyName,POJOPropertyBuilder> props,
            Linked<?> accessors)
    {
        final Linked<?> firstAcc = accessors; // clumsy, part 1
        for (Linked<?> node = accessors; node != null; node = node.next) {
            PropertyName name = node.name;
            if (!node.isNameExplicit || name == null) { // no explicit name -- problem!
                // [databind#541] ... but only as long as it's visible
                if (!node.isVisible) {
                    continue;
                }

                throw new IllegalStateException("Conflicting/ambiguous property name definitions (implicit name "
                        +ClassUtil.name(_name)+"): found multiple explicit names: "
                        +newNames+", but also implicit accessor: "+node);
            }
            POJOPropertyBuilder prop = props.get(name);
            if (prop == null) {
                prop = new POJOPropertyBuilder(_config, _annotationIntrospector, _forSerialization,
                        _internalName, name);
                props.put(name, prop);
            }
            // ultra-clumsy, part 2 -- lambdas would be nice here
            if (firstAcc == _fields) {
                Linked<AnnotatedField> n2 = (Linked<AnnotatedField>) node;
                prop._fields = n2.withNext(prop._fields);
            } else if (firstAcc == _getters) {
                Linked<AnnotatedMethod> n2 = (Linked<AnnotatedMethod>) node;
                prop._getters = n2.withNext(prop._getters);
            } else if (firstAcc == _setters) {
                Linked<AnnotatedMethod> n2 = (Linked<AnnotatedMethod>) node;
                prop._setters = n2.withNext(prop._setters);
            } else if (firstAcc == _ctorParameters) {
                Linked<AnnotatedParameter> n2 = (Linked<AnnotatedParameter>) node;
                prop._ctorParameters = n2.withNext(prop._ctorParameters);
            } else {
                throw new IllegalStateException("Internal error: mismatched accessors, property: "+this);
            }
        }
    }

    private Set<PropertyName> _findExplicitNames(Linked<? extends AnnotatedMember> node,
            Set<PropertyName> renamed)
    {
        for (; node != null; node = node.next) {
            /* 30-Mar-2014, tatu: Second check should not be needed, but seems like
             *   removing it can cause nasty exceptions with certain version
             *   combinations (2.4 databind, an older module).
             *   So leaving it in for now until this is resolved
             *   (or version beyond 2.4)
             */
            if (!node.isNameExplicit || node.name == null) {
                continue;
            }
            if (renamed == null) {
                renamed = new HashSet<PropertyName>();
            }
            renamed.add(node.name);
        }
        return renamed;
    }

    // For trouble-shooting
    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("[Property '").append(_name)
          .append("'; ctors: ").append(_ctorParameters)
          .append(", field(s): ").append(_fields)
          .append(", getter(s): ").append(_getters)
          .append(", setter(s): ").append(_setters)
          ;
        sb.append("]");
        return sb.toString();
    }

    /*
    /**********************************************************
    /* Helper methods
    /**********************************************************
     */

    /**
     * Helper method used for finding annotation values, from accessors
     * relevant to current usage (deserialization, serialization)
     */
    protected <T> T fromMemberAnnotations(WithMember<T> func)
    {
        T result = null;
        if (_annotationIntrospector != null) {
            if (_forSerialization) {
                if (_getters != null) {
                    result = func.withMember(_getters.value);
                }
            } else {
                if (_ctorParameters != null) {
                    result = func.withMember(_ctorParameters.value);
                }
                if (result == null && _setters != null) {
                    result = func.withMember(_setters.value);
                }
            }
            if (result == null && _fields != null) {
                result = func.withMember(_fields.value);
            }
        }
        return result;
    }

    protected <T> T fromMemberAnnotationsExcept(WithMember<T> func, T defaultValue)
    {
        if (_annotationIntrospector == null) {
            return null;
        }

        // NOTE: here we must ask ALL accessors, but the order varies between
        // serialization, deserialization
        if (_forSerialization) {
            if (_getters != null) {
                T result = func.withMember(_getters.value);
                if ((result != null) && (result != defaultValue)) {
                    return result;
                }
            }
            if (_fields != null) {
                T result = func.withMember(_fields.value);
                if ((result != null) && (result != defaultValue)) {
                    return result;
                }
            }
            if (_ctorParameters != null) {
                T result = func.withMember(_ctorParameters.value);
                if ((result != null) && (result != defaultValue)) {
                    return result;
                }
            }
            if (_setters != null) {
                T result = func.withMember(_setters.value);
                if ((result != null) && (result != defaultValue)) {
                    return result;
                }
            }
            return null;
        }
        if (_ctorParameters != null) {
            T result = func.withMember(_ctorParameters.value);
            if ((result != null) && (result != defaultValue)) {
                return result;
            }
        }
        if (_setters != null) {
            T result = func.withMember(_setters.value);
            if ((result != null) && (result != defaultValue)) {
                return result;
            }
        }
        if (_fields != null) {
            T result = func.withMember(_fields.value);
            if ((result != null) && (result != defaultValue)) {
                return result;
            }
        }
        if (_getters != null) {
            T result = func.withMember(_getters.value);
            if ((result != null) && (result != defaultValue)) {
                return result;
            }
        }
        return null;
    }

    // Helper method needed to work around oddity in type access for
    // `AnnotatedMethod`.
    //
    // @since 2.11.1
    protected Class<?> _rawTypeOf(AnnotatedMember m) {
        // AnnotatedMethod always returns return type, but for setters we
        // actually need argument type
        if (m instanceof AnnotatedMethod) {
            AnnotatedMethod meh = (AnnotatedMethod) m;
            if (meh.getParameterCount() > 0) {
                // note: get raw type FROM full type since only that resolves
                // generic types
                return meh.getParameterType(0).getRawClass();
            }
        }
        // same as above, must get fully resolved type to handled generic typing
        // of fields etc.
        return m.getType().getRawClass();
    }

    /*
    /**********************************************************
    /* Helper classes
    /**********************************************************
     */

    private interface WithMember<T> {
        public T withMember(AnnotatedMember member);
    }

    /**
     * @since 2.5
     */
    protected static class MemberIterator<T extends AnnotatedMember>
        implements Iterator<T>
    {
        private Linked<T> next;

        public MemberIterator(Linked<T> first) {
            next = first;
        }

        @Override
        public boolean hasNext() {
            return (next != null);
        }

        @Override
        public T next() {
            if (next == null) throw new NoSuchElementException();
            T result = next.value;
            next = next.next;
            return result;
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }

    }

    /**
     * Node used for creating simple linked lists to efficiently store small sets
     * of things.
     */
    protected final static class Linked<T>
    {
        public final T value;
        public final Linked<T> next;

        public final PropertyName name;
        public final boolean isNameExplicit;
        public final boolean isVisible;
        public final boolean isMarkedIgnored;

        public Linked(T v, Linked<T> n,
                PropertyName name, boolean explName, boolean visible, boolean ignored)
        {
            value = v;
            next = n;
            // ensure that we'll never have missing names
            this.name = (name == null || name.isEmpty()) ? null : name;

            if (explName) {
                if (this.name == null) { // sanity check to catch internal problems
                    throw new IllegalArgumentException("Cannot pass true for 'explName' if name is null/empty");
                }
                // 03-Apr-2014, tatu: But how about name-space only override?
                //   Probably should not be explicit? Or, need to merge somehow?
                if (!name.hasSimpleName()) {
                    explName = false;
                }
            }

            isNameExplicit = explName;
            isVisible = visible;
            isMarkedIgnored = ignored;
        }

        public Linked<T> withoutNext() {
            if (next == null) {
                return this;
            }
            return new Linked<T>(value, null, name, isNameExplicit, isVisible, isMarkedIgnored);
        }

        public Linked<T> withValue(T newValue) {
            if (newValue == value) {
                return this;
            }
            return new Linked<T>(newValue, next, name, isNameExplicit, isVisible, isMarkedIgnored);
        }

        public Linked<T> withNext(Linked<T> newNext) {
            if (newNext == next) {
                return this;
            }
            return new Linked<T>(value, newNext, name, isNameExplicit, isVisible, isMarkedIgnored);
        }

        public Linked<T> withoutIgnored() {
            if (isMarkedIgnored) {
                return (next == null) ? null : next.withoutIgnored();
            }
            if (next != null) {
                Linked<T> newNext = next.withoutIgnored();
                if (newNext != next) {
                    return withNext(newNext);
                }
            }
            return this;
        }

        public Linked<T> withoutNonVisible() {
            Linked<T> newNext = (next == null) ? null : next.withoutNonVisible();
            return isVisible ? withNext(newNext) : newNext;
        }

        /**
         * Method called to append given node(s) at the end of this
         * node chain.
         */
        protected Linked<T> append(Linked<T> appendable) {
            if (next == null) {
                return withNext(appendable);
            }
            return withNext(next.append(appendable));
        }

        public Linked<T> trimByVisibility() {
            if (next == null) {
                return this;
            }
            Linked<T> newNext = next.trimByVisibility();
            if (name != null) { // this already has highest; how about next one?
                if (newNext.name == null) { // next one not, drop it
                    return withNext(null);
                }
                //  both have it, keep
                return withNext(newNext);
            }
            if (newNext.name != null) { // next one has higher, return it...
                return newNext;
            }
            // neither has explicit name; how about visibility?
            if (isVisible == newNext.isVisible) { // same; keep both in current order
                return withNext(newNext);
            }
            return isVisible ? withNext(null) : newNext;
        }

        @Override
        public String toString() {
            String msg = String.format("%s[visible=%b,ignore=%b,explicitName=%b]",
                    value.toString(), isVisible, isMarkedIgnored, isNameExplicit);
            if (next != null) {
                msg = msg + ", "+next.toString();
            }
            return msg;
        }
    }
}
