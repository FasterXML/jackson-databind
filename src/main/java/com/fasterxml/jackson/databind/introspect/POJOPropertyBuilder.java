package com.fasterxml.jackson.databind.introspect;

import com.fasterxml.jackson.databind.AnnotationIntrospector;
import com.fasterxml.jackson.databind.PropertyName;

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
    protected final String _name;

    /**
     * Original internal name, derived from accessor, of this
     * property. Will not be changed by renaming.
     */
    protected final String _internalName;

    protected Linked<AnnotatedField> _fields;
    
    protected Linked<AnnotatedParameter> _ctorParameters;
    
    protected Linked<AnnotatedMethod> _getters;

    protected Linked<AnnotatedMethod> _setters;
    
    public POJOPropertyBuilder(String internalName,
            AnnotationIntrospector annotationIntrospector, boolean forSerialization)
    {
        _internalName = internalName;
        _name = internalName;
        _annotationIntrospector = annotationIntrospector;
        _forSerialization = forSerialization;
    }

    public POJOPropertyBuilder(POJOPropertyBuilder src, String newName)
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
    public POJOPropertyBuilder withName(String newName) {
        return new POJOPropertyBuilder(this, newName);
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
    public String getName() { return _name; }

    @Override
    public String getInternalName() { return _internalName; }

    @Override
    public PropertyName getWrapperName() {
        /* 13-Mar-2013, tatu: Accessing via primary member SHOULD work,
         *   due to annotation merging. However, I have seen some problems
         *   with this access (for other annotations) so will leave full
         *   traversal code in place just in case.
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
    public boolean couldSerialize() {
        return (_getters != null) || (_fields != null);
    }

    @Override
    public AnnotatedMethod getGetter()
    {
        if (_getters == null) {
            return null;
        }
        // If multiple, verify that they do not conflict...
        AnnotatedMethod getter = _getters.value;
        Linked<AnnotatedMethod> next = _getters.next;
        for (; next != null; next = next.next) {
            /* [JACKSON-255] Allow masking, i.e. report exception only if
             *   declarations in same class, or there's no inheritance relationship
             *   (sibling interfaces etc)
             */
            AnnotatedMethod nextGetter = next.value;
            Class<?> getterClass = getter.getDeclaringClass();
            Class<?> nextClass = nextGetter.getDeclaringClass();
            if (getterClass != nextClass) {
                if (getterClass.isAssignableFrom(nextClass)) { // next is more specific
                    getter = nextGetter;
                    continue;
                }
                if (nextClass.isAssignableFrom(getterClass)) { // getter more specific
                    continue;
                }
            }
            throw new IllegalArgumentException("Conflicting getter definitions for property \""+getName()+"\": "
                    +getter.getFullName()+" vs "+nextGetter.getFullName());
        }
        return getter;
    }

    @Override
    public AnnotatedMethod getSetter()
    {
        if (_setters == null) {
            return null;
        }
        // If multiple, verify that they do not conflict...
        AnnotatedMethod setter = _setters.value;
        Linked<AnnotatedMethod> next = _setters.next;
        for (; next != null; next = next.next) {
            /* [JACKSON-255] Allow masking, i.e. report exception only if
             *   declarations in same class, or there's no inheritance relationship
             *   (sibling interfaces etc)
             */
            AnnotatedMethod nextSetter = next.value;
            Class<?> setterClass = setter.getDeclaringClass();
            Class<?> nextClass = nextSetter.getDeclaringClass();
            if (setterClass != nextClass) {
                if (setterClass.isAssignableFrom(nextClass)) { // next is more specific
                    setter = nextSetter;
                    continue;
                }
                if (nextClass.isAssignableFrom(setterClass)) { // getter more specific
                    continue;
                }
            }
            throw new IllegalArgumentException("Conflicting setter definitions for property \""+getName()+"\": "
                    +setter.getFullName()+" vs "+nextSetter.getFullName());
        }
        return setter;
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
    public AnnotatedMember getPrimaryMember() {
        if (_forSerialization) {
            return getAccessor();
        }
        return getMutator();
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
    public boolean isRequired() {
        Boolean b = fromMemberAnnotations(new WithMember<Boolean>() {
            @Override
            public Boolean withMember(AnnotatedMember member) {
                return _annotationIntrospector.hasRequiredMarker(member);
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
    
    /*
    /**********************************************************
    /* Data aggregation
    /**********************************************************
     */
    
    public void addField(AnnotatedField a, String ename, boolean visible, boolean ignored) {
        _fields = new Linked<AnnotatedField>(a, _fields, ename, visible, ignored);
    }

    public void addCtor(AnnotatedParameter a, String ename, boolean visible, boolean ignored) {
        _ctorParameters = new Linked<AnnotatedParameter>(a, _ctorParameters, ename, visible, ignored);
    }

    public void addGetter(AnnotatedMethod a, String ename, boolean visible, boolean ignored) {
        _getters = new Linked<AnnotatedMethod>(a, _getters, ename, visible, ignored);
    }

    public void addSetter(AnnotatedMethod a, String ename, boolean visible, boolean ignored) {
        _setters = new Linked<AnnotatedMethod>(a, _setters, ename, visible, ignored);
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

    /**
     * @deprecated Since 2.2, use variant that takes boolean argument
     */
    @Deprecated
    public void removeNonVisible() {
        removeNonVisible(false);
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
        } else {
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

    private <T> boolean _anyExplicitNames(Linked<T> n)
    {
        for (; n != null; n = n.next) {
            if (n.explicitName != null && n.explicitName.length() > 0) {
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
     * Method called to check whether property represented by this collector
     * should be renamed from the implicit name; and also verify that there
     * are no conflicting rename definitions.
     */
    public String findNewName()
    {
        Linked<? extends AnnotatedMember> renamed = null;
        renamed = findRenamed(_fields, renamed);
        renamed = findRenamed(_getters, renamed);
        renamed = findRenamed(_setters, renamed);
        renamed = findRenamed(_ctorParameters, renamed);
        return (renamed == null) ? null : renamed.explicitName;
    }

    private Linked<? extends AnnotatedMember> findRenamed(Linked<? extends AnnotatedMember> node,
            Linked<? extends AnnotatedMember> renamed)
    {
        for (; node != null; node = node.next) {
            String explName = node.explicitName;
            if (explName == null) {
                continue;
            }
            // different from default name?
            if (explName.equals(_name)) { // nope, skip
                continue;
            }
            if (renamed == null) {
                renamed = node;
            } else {
                // different from an earlier renaming? problem
                if (!explName.equals(renamed.explicitName)) {
                    throw new IllegalStateException("Conflicting property name definitions: '"
                            +renamed.explicitName+"' (for "+renamed.value+") vs '"
                            +node.explicitName+"' (for "+node.value+")");
                }
            }
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

    private interface WithMember<T>
    {
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

        public final String explicitName;
        public final boolean isVisible;
        public final boolean isMarkedIgnored;
        
        public Linked(T v, Linked<T> n,
                String explName, boolean visible, boolean ignored)
        {
            value = v;
            next = n;
            // ensure that we'll never have missing names
            if (explName == null) {
                explicitName = null;
            } else {
                explicitName = (explName.length() == 0) ? null : explName;
            }
            isVisible = visible;
            isMarkedIgnored = ignored;
        }

        public Linked<T> withValue(T newValue)
        {
            if (newValue == value) {
                return this;
            }
            return new Linked<T>(newValue, next, explicitName, isVisible, isMarkedIgnored);
        }
        
        public Linked<T> withNext(Linked<T> newNext) {
            if (newNext == next) {
                return this;
            }
            return new Linked<T>(value, newNext, explicitName, isVisible, isMarkedIgnored);
        }
        
        public Linked<T> withoutIgnored()
        {
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
        
        public Linked<T> withoutNonVisible()
        {
            Linked<T> newNext = (next == null) ? null : next.withoutNonVisible();
            return isVisible ? withNext(newNext) : newNext;
        }

        /**
         * Method called to append given node(s) at the end of this
         * node chain.
         */
        private Linked<T> append(Linked<T> appendable) 
        {
            if (next == null) {
                return withNext(appendable);
            }
            return withNext(next.append(appendable));
        }
        
        public Linked<T> trimByVisibility()
        {
            if (next == null) {
                return this;
            }
            Linked<T> newNext = next.trimByVisibility();
            if (explicitName != null) { // this already has highest; how about next one?
                if (newNext.explicitName == null) { // next one not, drop it
                    return withNext(null);
                }
                //  both have it, keep
                return withNext(newNext);
            }
            if (newNext.explicitName != null) { // next one has higher, return it...
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
            String msg = value.toString()+"[visible="+isVisible+"]";
            if (next != null) {
                msg = msg + ", "+next.toString();
            }
            return msg;
        }
    }
}
