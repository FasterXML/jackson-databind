package com.fasterxml.jackson.databind.util;

import java.util.Collections;
import java.util.Iterator;

import com.fasterxml.jackson.annotation.JsonInclude;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.cfg.MapperConfig;
import com.fasterxml.jackson.databind.introspect.*;
import com.fasterxml.jackson.databind.type.TypeFactory;

/**
 * Simple immutable {@link BeanPropertyDefinition} implementation that can
 * be wrapped around a {@link AnnotatedMember} that is a simple
 * accessor (getter) or mutator (setter, constructor parameter)
 * (or both, for fields).
 */
public class SimpleBeanPropertyDefinition
    extends BeanPropertyDefinition
{
    protected final AnnotationIntrospector _annotationIntrospector;

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
    protected final JsonInclude.Value _inclusion;

    /*
    /**********************************************************
    /* Construction
    /**********************************************************
     */

    /**
     * @since 2.9
     */
    protected SimpleBeanPropertyDefinition(AnnotationIntrospector intr,
            AnnotatedMember member, PropertyName fullName, PropertyMetadata metadata,
            JsonInclude.Value inclusion)
    {
        _annotationIntrospector = intr;
        _member = member;
        _fullName = fullName;
        _metadata = (metadata == null) ? PropertyMetadata.STD_OPTIONAL: metadata;
        _inclusion = inclusion;
    }

    /**
     * @since 2.2
     */
    public static SimpleBeanPropertyDefinition construct(MapperConfig<?> config,
    		AnnotatedMember member)
    {
        return new SimpleBeanPropertyDefinition(config.getAnnotationIntrospector(),
                member, PropertyName.construct(member.getName()), null, EMPTY_INCLUDE);
    }

    /**
     * @since 2.5
     */
    public static SimpleBeanPropertyDefinition construct(MapperConfig<?> config,
            AnnotatedMember member, PropertyName name) {
        return construct(config, member, name, null, EMPTY_INCLUDE);
    }

    /**
     * Method called to create instance for virtual properties.
     *
     * @since 2.5
     */
    public static SimpleBeanPropertyDefinition construct(MapperConfig<?> config,
            AnnotatedMember member, PropertyName name, PropertyMetadata metadata,
            JsonInclude.Include inclusion)
    {
        JsonInclude.Value inclValue
             = ((inclusion == null) || (inclusion == JsonInclude.Include.USE_DEFAULTS))
             ? EMPTY_INCLUDE : JsonInclude.Value.construct(inclusion, null);
        return new SimpleBeanPropertyDefinition(config.getAnnotationIntrospector(),
                member, name, metadata, inclValue);
    }

    /**
     * @since 2.7
     */
    public static SimpleBeanPropertyDefinition construct(MapperConfig<?> config,
            AnnotatedMember member, PropertyName name, PropertyMetadata metadata,
            JsonInclude.Value inclusion) {
          return new SimpleBeanPropertyDefinition(config.getAnnotationIntrospector(),
                  member, name, metadata, inclusion);
    }

    /*
    /**********************************************************
    /* Fluent factories
    /**********************************************************
     */

    @Override
    public BeanPropertyDefinition withSimpleName(String newName) {
        if (_fullName.hasSimpleName(newName) && !_fullName.hasNamespace()) {
            return this;
        }
        return new SimpleBeanPropertyDefinition(_annotationIntrospector,
                _member, new PropertyName(newName), _metadata, _inclusion);
    }

    @Override
    public BeanPropertyDefinition withName(PropertyName newName) {
        if (_fullName.equals(newName)) {
            return this;
        }
        return new SimpleBeanPropertyDefinition(_annotationIntrospector,
                _member, newName, _metadata, _inclusion);
    }

    /**
     * @since 2.5
     */
    public BeanPropertyDefinition withMetadata(PropertyMetadata metadata) {
        if (metadata.equals(_metadata)) {
            return this;
        }
        return new SimpleBeanPropertyDefinition(_annotationIntrospector,
                _member, _fullName, metadata, _inclusion);
    }

    /**
     * @since 2.5
     */
    public BeanPropertyDefinition withInclusion(JsonInclude.Value inclusion) {
        if (_inclusion == inclusion) {
            return this;
        }
        return new SimpleBeanPropertyDefinition(_annotationIntrospector,
                _member, _fullName, _metadata, inclusion);
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
        if ((_annotationIntrospector == null) || (_member == null)) {
            return null;
        }
        return _annotationIntrospector.findWrapperName(_member);
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
    public JavaType getPrimaryType() {
        if (_member == null) {
            return TypeFactory.unknownType();
        }
        return _member.getType();
    }

    @Override
    public Class<?> getRawPrimaryType() {
        if (_member == null) {
            return Object.class;
        }
        return _member.getRawType();
    }

    @Override
    public JsonInclude.Value findInclusion() {
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
            return ClassUtil.emptyIterator();
        }
        return Collections.singleton(param).iterator();
    }

    @Override
    public AnnotatedMember getPrimaryMember() { return _member; }
}
