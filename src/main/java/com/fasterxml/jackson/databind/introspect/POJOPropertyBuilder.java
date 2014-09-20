package com.fasterxml.jackson.databind.introspect;

import java.util.*;

import com.fasterxml.jackson.databind.*;

/**
 * Helper class used for aggregating information about a single
 * potential POJO property.
 */
public class POJOPropertyBuilder
    extends BeanPropertyDefinition
    implements Comparable<POJOPropertyBuilder>
{
    /**
     * Whether property is being composed for serialization
     * (true) or deserialization (false)
     */
    protected final boolean _forSerialization;

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

    public POJOPropertyBuilder(PropertyName internalName, AnnotationIntrospector ai, boolean forSerialization) {
        this(internalName, internalName, ai, forSerialization);
    }

    protected POJOPropertyBuilder(PropertyName internalName, PropertyName name,
            AnnotationIntrospector annotationIntrospector, boolean forSerialization)
    {
        _internalName = internalName;
        _name = name;
        _annotationIntrospector = annotationIntrospector;
        _forSerialization = forSerialization;
    }

    public POJOPropertyBuilder(POJOPropertyBuilder src, PropertyName newName)
    {
        _internalName = src._internalName;
        _name = newName;
        _annotationIntrospector = src._annotationIntrospector;
        _fields = src._fields;
        _ctorParameters = src._ctorParameters;
        _getters = src._getters;
        _setters = src._setters;
        _forSerialization = src._forSerialization;
    }
    
    /*
    /**********************************************************
    /* Fluent factory methods
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
                || _anyExplicits(_ctorParameters)
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
    
    @Override
    public AnnotatedMethod getSetter()
    {
        // Easy with zero or one getters...
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
            /* 30-May-2014, tatu: Two levels of precedence:
             * 
             * 1. Regular setters ("setX(...)")
             * 2. Implicit, possible setters ("x(...)")
             */
            int priNext = _setterPriority(next.value);
            int priCurr = _setterPriority(curr.value);

            if (priNext != priCurr) {
                if (priNext < priCurr) {
                    curr = next;
                }
                continue;
            }
            throw new IllegalArgumentException("Conflicting setter definitions for property \""+getName()+"\": "
                    +curr.value.getFullName()+" vs "+next.value.getFullName());
        }
        // One more thing; to avoid having to do it again...
        _setters = curr.withoutNext();
        return curr.value;
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
    public AnnotatedMember getAccessor()
    {
        AnnotatedMember m = getGetter();
        if (m == null) {
            m = getField();
        }
        return m;
    }

    @Override
    public AnnotatedMember getMutator()
    {
        AnnotatedMember m = getConstructorParameter();
        if (m == null) {
            m = getSetter();
            if (m == null) {
                m = getField();
            }
        }
        return m;
    }

    @Override
    public AnnotatedMember getNonConstructorMutator() {
        AnnotatedMember m = getSetter();
        if (m == null) {
            m = getField();
        }
        return m;
    }

    @Override
    public AnnotatedMember getPrimaryMember() {
        if (_forSerialization) {
            return getAccessor();
        }
        return getMutator();
    }

    protected int _getterPriority(AnnotatedMethod m)
    {
        final String name = m.getName();
        // [#238]: Also, regular getters have precedence over "is-getters"
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
        return fromMemberAnnotations(new WithMember<AnnotationIntrospector.ReferenceProperty>() {
            @Override
            public AnnotationIntrospector.ReferenceProperty withMember(AnnotatedMember member) {
                return _annotationIntrospector.findReferenceType(member);
            }
        });
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
    public PropertyMetadata getMetadata() {
        final Boolean b = _findRequired();
        final String desc = _findDescription();
        final Integer idx = _findIndex();
        if (b == null && idx == null) {
            return (desc == null) ? PropertyMetadata.STD_REQUIRED_OR_OPTIONAL
                    : PropertyMetadata.STD_REQUIRED_OR_OPTIONAL.withDescription(desc);
        }
        return PropertyMetadata.construct(b.booleanValue(), desc, idx);
    }

    protected Boolean _findRequired() {
        Boolean b = fromMemberAnnotations(new WithMember<Boolean>() {
            @Override
            public Boolean withMember(AnnotatedMember member) {
                return _annotationIntrospector.hasRequiredMarker(member);
            }
        });
        return b;
    }
    
    protected String _findDescription() {
        return fromMemberAnnotations(new WithMember<String>() {
            @Override
            public String withMember(AnnotatedMember member) {
                return _annotationIntrospector.findPropertyDescription(member);
            }
        });
    }

    protected Integer _findIndex() {
        return fromMemberAnnotations(new WithMember<Integer>() {
            @Override
            public Integer withMember(AnnotatedMember member) {
                return _annotationIntrospector.findPropertyIndex(member);
            }
        });
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

    // // Deprecated variants that do not take 'explName': to be removed in a later version
    // // (but are used at least by 2.3 and earlier versions of Scala module at least so
    // // need to be careful with phasing out if before 3.0)
    
    /**
     * @deprecated Since 2.4 call method that takes additional 'explName' argument, to indicate
     *   whether name of property was provided by annotation (and not derived from accessor name);
     *   this method assumes the name is explicit if it is non-null.
     */
    @Deprecated
    public void addField(AnnotatedField a, String name, boolean visible, boolean ignored) {
        addField(a, _propName(name), name != null, visible, ignored);
    }

    @Deprecated
    public void addField(AnnotatedField a, String name, boolean explName, boolean visible, boolean ignored) {
        addField(a, _propName(name), explName, visible, ignored);
    }
    
    /**
     * @deprecated Since 2.4 call method that takes additional 'explName' argument, to indicate
     *   whether name of property was provided by annotation (and not derived from accessor name);
     *   this method assumes the name is explicit if it is non-null.
     */
    @Deprecated
    public void addCtor(AnnotatedParameter a, String name, boolean visible, boolean ignored) {
        addCtor(a, _propName(name), name != null, visible, ignored);
    }
    @Deprecated
    public void addCtor(AnnotatedParameter a, String name, boolean explName, boolean visible, boolean ignored) {
        addCtor(a, _propName(name), explName, visible, ignored);
    }
    
    /**
     * @deprecated Since 2.4 call method that takes additional 'explName' argument, to indicate
     *   whether name of property was provided by annotation (and not derived from accessor name);
     *   this method assumes the name is explicit if it is non-null.
     */
    @Deprecated
    public void addGetter(AnnotatedMethod a, String name, boolean visible, boolean ignored) {
        addGetter(a, _propName(name), name != null, visible, ignored);
    }
    @Deprecated
    public void addGetter(AnnotatedMethod a, String name, boolean explName, boolean visible, boolean ignored) {
        addGetter(a, _propName(name), explName, visible, ignored);
    }
    
    /**
     * @deprecated Since 2.4 call method that takes additional 'explName' argument, to indicate
     *   whether name of property was provided by annotation (and not derived from accessor name);
     *   this method assumes the name is explicit if it is non-null.
     */
    @Deprecated
    public void addSetter(AnnotatedMethod a, String name, boolean visible, boolean ignored) {
        addSetter(a, _propName(name), name != null, visible, ignored);
    }
    @Deprecated
    public void addSetter(AnnotatedMethod a, String name, boolean explName, boolean visible, boolean ignored) {
        addSetter(a, _propName(name), explName, visible, ignored);
    }

    private PropertyName _propName(String simple) {
        return PropertyName.construct(simple, null);
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

    public void removeNonVisible(boolean force)
    {
        /* 21-Aug-2011, tatu: This is tricky part -- if and when allow
         *   non-visible property elements to be "pulled in" by visible
         *   counterparts?
         *   For now, we will only do this to pull in setter or field used
         *   as setter, if an explicit getter is found.
         */
        /*
         * 28-Mar-2013, tatu: Also, as per [Issue#195], may force removal
         *   if inferred properties are NOT supported.
         */
        _getters = _removeNonVisible(_getters);
        _ctorParameters = _removeNonVisible(_ctorParameters);

        if (force || (_getters == null)) {
            _fields = _removeNonVisible(_fields);
            _setters = _removeNonVisible(_setters);
        }
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
                _getters = _getters.withValue(_getters.value.withAnnotations(ann));
            } else if (_fields != null) {
                AnnotationMap ann = _mergeAnnotations(0, _fields, _ctorParameters, _setters);
                _fields = _fields.withValue(_fields.value.withAnnotations(ann));
            }
        } else { // for deserialization
            if (_ctorParameters != null) {
                AnnotationMap ann = _mergeAnnotations(0, _ctorParameters, _setters, _fields, _getters);
                _ctorParameters = _ctorParameters.withValue(_ctorParameters.value.withAnnotations(ann));
            } else if (_setters != null) {
                AnnotationMap ann = _mergeAnnotations(0, _setters, _fields, _getters);
                _setters = _setters.withValue(_setters.value.withAnnotations(ann));
            } else if (_fields != null) {
                AnnotationMap ann = _mergeAnnotations(0, _fields, _getters);
                _fields = _fields.withValue(_fields.value.withAnnotations(ann));
            }
        }
    }

    private AnnotationMap _mergeAnnotations(int index, Linked<? extends AnnotatedMember>... nodes)
    {
        AnnotationMap ann = nodes[index].value.getAllAnnotations();
        ++index;
        for (; index < nodes.length; ++index) {
            if (nodes[index] != null) {
              return AnnotationMap.merge(ann, _mergeAnnotations(index, nodes));
            }
        }
        return ann;
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

    /**
     * @since 2.4 Use {@link #findExplicitNames} instead
     */
    @Deprecated
    public String findNewName()
    {
        Collection<PropertyName> l = findExplicitNames();
        if (l == null) {
            return null;
        }
        
        // 13-Apr-2014, tatu: Start with code similar to existing conflict checks
        if (l.size() > 1) {
            throw new IllegalStateException("Conflicting/ambiguous property name definitions (implicit name '"
                    +_name+"'): found more than one explicit name: "
                    +l);
        }
        PropertyName first = l.iterator().next();
        if (first.equals(_name)) {
            return null;
        }
        return first.getSimpleName();
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
                // [Issue#541] ... but only as long as it's visible
                if (!node.isVisible) {
                    continue;
                }
                
                throw new IllegalStateException("Conflicting/ambiguous property name definitions (implicit name '"
                        +_name+"'): found multiple explicit names: "
                        +newNames+", but also implicit accessor: "+node);
            }
            POJOPropertyBuilder prop = props.get(name);
            if (prop == null) {
                prop = new POJOPropertyBuilder(_internalName, name, _annotationIntrospector, _forSerialization);
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
    
    /*
    /**********************************************************
    /* Helper classes
    /**********************************************************
     */

    private interface WithMember<T> {
        public T withMember(AnnotatedMember member);
    }
    
    /**
     * Node used for creating simple linked lists to efficiently store small sets
     * of things.
     */
    private final static class Linked<T>
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
                    throw new IllegalArgumentException("Can not pass true for 'explName' if name is null/empty");
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
        private Linked<T> append(Linked<T> appendable) {
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
            String msg = value.toString()+"[visible="+isVisible+",ignore="+isMarkedIgnored
                    +",explicitName="+isNameExplicit+"]";
            if (next != null) {
                msg = msg + ", "+next.toString();
            }
            return msg;
        }
    }
}
