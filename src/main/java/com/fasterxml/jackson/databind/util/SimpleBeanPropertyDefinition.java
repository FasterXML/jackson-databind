package com.fasterxml.jackson.databind.util;

import java.util.Collections;
import java.util.Iterator;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.AnnotationIntrospector;
import com.fasterxml.jackson.databind.PropertyMetadata;
import com.fasterxml.jackson.databind.PropertyName;
import com.fasterxml.jackson.databind.cfg.MapperConfig;
import com.fasterxml.jackson.databind.introspect.*;

/**
 * Simple immutable {@link BeanPropertyDefinition} implementation that can
 * be wrapped around a {@link AnnotatedMember} that is a simple
 * accessor (getter) or mutator (setter, constructor parameter)
 * (or both, for fields).
 */
public class SimpleBeanPropertyDefinition
    extends BeanPropertyDefinition
{
	protected final AnnotationIntrospector _introspector;

	/**
     * Member that defines logical property. Assumption is that it
     * should be a 'simple' accessor; meaning a zero-argument getter,
     * single-argument setter or constructor parameter.
     *<p>
     * NOTE: for "virtual" properties, this is null.
     */
    protected final AnnotatedMember _member;

    /**
     * @since 2.5
     */
    protected final PropertyMetadata _metadata;

    /**
     * @since 2.5
     */
    protected final PropertyName _fullName;

    /**
     * @since 2.5
     */
    protected final JsonInclude.Include _inclusion;

    /**
     * @deprecated Since 2.5 use <code>_fullName</code> instead.
     */
    @Deprecated
    protected final String _name;

    /*
    /**********************************************************
    /* Construction
    /**********************************************************
     */

    /**
     * @since 2.2 Use {@link #construct} instead
     */
    @Deprecated
    public SimpleBeanPropertyDefinition(AnnotatedMember member) {
        this(member, member.getName(), null);
    }

    /**
     * @since 2.2 Use {@link #construct} instead
     */
    @Deprecated
    public SimpleBeanPropertyDefinition(AnnotatedMember member, String name) {
        this(member, new PropertyName(name), null, null, null);
    }

    protected SimpleBeanPropertyDefinition(AnnotatedMember member, PropertyName fullName,
            AnnotationIntrospector intr, PropertyMetadata metadata,
            JsonInclude.Include inclusion)
    {
        _introspector = intr;
        _member = member;
        _fullName = fullName;
        _name = fullName.getSimpleName();
        _metadata = (metadata == null) ? PropertyMetadata.STD_OPTIONAL: metadata;
        _inclusion = inclusion;
    }

    /**
     * @deprecated Since 2.5 Use variant that takes PropertyName
     */
    @Deprecated
    protected SimpleBeanPropertyDefinition(AnnotatedMember member, String name,
    		AnnotationIntrospector intr) {
        this(member, new PropertyName(name), intr, null, null);
    }

    /**
     * @since 2.2
     */
    public static SimpleBeanPropertyDefinition construct(MapperConfig<?> config,
    		AnnotatedMember member) {
        return new SimpleBeanPropertyDefinition(member, PropertyName.construct(member.getName()),
                (config == null) ? null : config.getAnnotationIntrospector(),
                        null, null);
    }

    /**
     * @deprecated Since 2.5
     */
    @Deprecated
    public static SimpleBeanPropertyDefinition construct(MapperConfig<?> config,
    		AnnotatedMember member, String name) {
        return new SimpleBeanPropertyDefinition(member, PropertyName.construct(name),
                (config == null) ? null : config.getAnnotationIntrospector(),
                        null, null);
    }

    /**
     * @since 2.5
     */
    public static SimpleBeanPropertyDefinition construct(MapperConfig<?> config,
            AnnotatedMember member, PropertyName name) {
        return construct(config, member, name, null, null);
    }

    /**
     * @since 2.5
     */
    public static SimpleBeanPropertyDefinition construct(MapperConfig<?> config,
            AnnotatedMember member, PropertyName name, PropertyMetadata metadata,
            JsonInclude.Include inclusion) {
          return new SimpleBeanPropertyDefinition(member, name,
                  (config == null) ? null : config.getAnnotationIntrospector(),
                          metadata, inclusion);
    }

    /*
    /**********************************************************
    /* Fluent factories
    /**********************************************************
     */

    // Note: removed from base class in 2.6; left here until 2.7
    @Deprecated // since 2.3 (remove in 2.7)
    public BeanPropertyDefinition withName(String newName) {
        return withSimpleName(newName);
    }

    @Override
    public BeanPropertyDefinition withSimpleName(String newName) {
        if (_fullName.hasSimpleName(newName) && !_fullName.hasNamespace()) {
            return this;
        }
        return new SimpleBeanPropertyDefinition(_member, new PropertyName(newName),
                _introspector, _metadata, _inclusion);
    }

    @Override
    public BeanPropertyDefinition withName(PropertyName newName) {
        if (_fullName.equals(newName)) {
            return this;
        }
        return new SimpleBeanPropertyDefinition(_member, newName,
                _introspector, _metadata, _inclusion);
    }

    /**
     * @since 2.5
     */
    public BeanPropertyDefinition withMetadata(PropertyMetadata metadata) {
        if (metadata.equals(_metadata)) {
            return this;
        }
        return new SimpleBeanPropertyDefinition(_member, _fullName,
                _introspector, metadata, _inclusion);
    }

    /**
     * @since 2.5
     */
    public BeanPropertyDefinition withInclusion(JsonInclude.Include inclusion) {
        if (_inclusion == inclusion) {
            return this;
        }
        return new SimpleBeanPropertyDefinition(_member, _fullName,
                _introspector, _metadata, inclusion);
    }

    /*
    /**********************************************************
    /* Basic property information, name, type
    /**********************************************************
     */

    @Override
    public String getName() { return _fullName.getSimpleName(); }

    @Override
    public PropertyName getFullName() { return _fullName; }

    @Override
    public boolean hasName(PropertyName name) {
        return _fullName.equals(name);
    }

    @Override
    public String getInternalName() { return getName(); }

    @Override
    public PropertyName getWrapperName() {
        return ((_introspector == null) && (_member != null))
                ? null : _introspector.findWrapperName(_member);
    }

    // hmmh. what should we claim here?

    @Override public boolean isExplicitlyIncluded() { return false; }
    @Override public boolean isExplicitlyNamed() { return false; }

    /**
     * We will indicate that property is optional, since there is nothing
     * to indicate whether it might be required.
     */
    @Override
    public PropertyMetadata getMetadata() {
        return _metadata;
    }

    @Override
    public JsonInclude.Include findInclusion() {
        return _inclusion;
    }

    /*
    /**********************************************************
    /* Access to accessors (fields, methods etc)
    /**********************************************************
     */

    @Override
    public boolean hasGetter() { return (getGetter() != null); }

    @Override
    public boolean hasSetter() { return (getSetter() != null); }

    @Override
    public boolean hasField() { return (_member instanceof AnnotatedField); }

    @Override
    public boolean hasConstructorParameter() { return (_member instanceof AnnotatedParameter); }

    @Override
    public AnnotatedMethod getGetter() {
        if ((_member instanceof AnnotatedMethod)
                && ((AnnotatedMethod) _member).getParameterCount() == 0) {
            return (AnnotatedMethod) _member;
        }
        return null;
    }

    @Override
    public AnnotatedMethod getSetter() {
        if ((_member instanceof AnnotatedMethod)
                && ((AnnotatedMethod) _member).getParameterCount() == 1) {
            return (AnnotatedMethod) _member;
        }
        return null;
    }

    @Override
    public AnnotatedField getField() {
        return (_member instanceof AnnotatedField) ? (AnnotatedField) _member : null;
    }

    @Override
    public AnnotatedParameter getConstructorParameter() {
        return (_member instanceof AnnotatedParameter) ? (AnnotatedParameter) _member : null;
    }

    @Override
    public Iterator<AnnotatedParameter> getConstructorParameters() {
        AnnotatedParameter param = getConstructorParameter();
        if (param == null) {
            return EmptyIterator.instance();
        }
        return Collections.singleton(param).iterator();
    }

    /**
     * Method used to find accessor (getter, field to access) to use for accessing
     * value of the property.
     * Null if no such member exists.
     */
    @Override
    public AnnotatedMember getAccessor() {
        AnnotatedMember acc = getGetter();
        if (acc == null) {
            acc = getField();
        }
        return acc;
    }

    /**
     * Method used to find mutator (constructor parameter, setter, field) to use for
     * changing value of the property.
     * Null if no such member exists.
     */
    @Override
    public AnnotatedMember getMutator() {
        AnnotatedMember acc = getConstructorParameter();
        if (acc == null) {
            acc = getSetter();
            if (acc == null) {
                acc = getField();
            }
        }
        return acc;
    }

    @Override
    public AnnotatedMember getNonConstructorMutator() {
        AnnotatedMember acc = getSetter();
        if (acc == null) {
            acc = getField();
        }
        return acc;
    }

    @Override
    public AnnotatedMember getPrimaryMember() { return _member; }
}
