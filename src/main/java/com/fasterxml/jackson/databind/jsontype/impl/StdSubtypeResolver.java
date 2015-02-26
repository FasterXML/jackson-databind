package com.fasterxml.jackson.databind.jsontype.impl;

import java.util.*;

import com.fasterxml.jackson.databind.AnnotationIntrospector;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.cfg.MapperConfig;
import com.fasterxml.jackson.databind.introspect.*;
import com.fasterxml.jackson.databind.jsontype.NamedType;
import com.fasterxml.jackson.databind.jsontype.SubtypeResolver;

/**
 * Standard {@link SubtypeResolver} implementation.
 */
public class StdSubtypeResolver
    extends SubtypeResolver
    implements java.io.Serializable
{
    private static final long serialVersionUID = 1L;

    protected LinkedHashSet<NamedType> _registeredSubtypes;

    public StdSubtypeResolver() { }
    
    /*
    /**********************************************************
    /* Subtype registration
    /**********************************************************
     */

    @Override    
    public void registerSubtypes(NamedType... types) {
        if (_registeredSubtypes == null) {
            _registeredSubtypes = new LinkedHashSet<NamedType>();
        }
        for (NamedType type : types) {
            _registeredSubtypes.add(type);
        }
    }

    @Override
    public void registerSubtypes(Class<?>... classes) {
        NamedType[] types = new NamedType[classes.length];
        for (int i = 0, len = classes.length; i < len; ++i) {
            types[i] = new NamedType(classes[i]);
        }
        registerSubtypes(types);
    }

    /*
    /**********************************************************
    /* Resolution by class (serialization)
    /**********************************************************
     */

    @Override
    public Collection<NamedType> collectAndResolveSubtypesByClass(MapperConfig<?> config, 
            AnnotatedMember property, JavaType baseType)
    {
        final AnnotationIntrospector ai = config.getAnnotationIntrospector();
        // for backwards compatibility, must allow null here:
        Class<?> rawBase = (baseType == null) ? property.getRawType() : baseType.getRawClass();
        
        HashMap<NamedType, NamedType> collected = new HashMap<NamedType, NamedType>();
        // start with registered subtypes (which have precedence)
        if (_registeredSubtypes != null) {
            for (NamedType subtype : _registeredSubtypes) {
                // is it a subtype of root type?
                if (rawBase.isAssignableFrom(subtype.getType())) { // yes
                    AnnotatedClass curr = AnnotatedClass.constructWithoutSuperTypes(subtype.getType(), ai, config);
                    _collectAndResolve(curr, subtype, config, ai, collected);
                }
            }
        }
        
        // then annotated types for property itself
        Collection<NamedType> st = ai.findSubtypes(property);
        if (st != null) {
            for (NamedType nt : st) {
                AnnotatedClass ac = AnnotatedClass.constructWithoutSuperTypes(nt.getType(), ai, config);
                _collectAndResolve(ac, nt, config, ai, collected);
            }            
        }
        
        NamedType rootType = new NamedType(rawBase, null);
        AnnotatedClass ac = AnnotatedClass.constructWithoutSuperTypes(rawBase, ai, config);
            
        // and finally subtypes via annotations from base type (recursively)
        _collectAndResolve(ac, rootType, config, ai, collected);

        return new ArrayList<NamedType>(collected.values());
    }

    @Override
    public Collection<NamedType> collectAndResolveSubtypesByClass(MapperConfig<?> config,
            AnnotatedClass type)
    {
        final AnnotationIntrospector ai = config.getAnnotationIntrospector();
        HashMap<NamedType, NamedType> subtypes = new HashMap<NamedType, NamedType>();
        // [JACKSON-257] then consider registered subtypes (which have precedence over annotations)
        if (_registeredSubtypes != null) {
            Class<?> rawBase = type.getRawType();
            for (NamedType subtype : _registeredSubtypes) {
                // is it a subtype of root type?
                if (rawBase.isAssignableFrom(subtype.getType())) { // yes
                    AnnotatedClass curr = AnnotatedClass.constructWithoutSuperTypes(subtype.getType(), ai, config);
                    _collectAndResolve(curr, subtype, config, ai, subtypes);
                }
            }
        }
        // and then check subtypes via annotations from base type (recursively)
        NamedType rootType = new NamedType(type.getRawType(), null);
        _collectAndResolve(type, rootType, config, ai, subtypes);
        return new ArrayList<NamedType>(subtypes.values());
    }

    /*
    /**********************************************************
    /* Resolution by class (deserialization)
    /**********************************************************
     */

    @Override
    public Collection<NamedType> collectAndResolveSubtypesByName(MapperConfig<?> config, 
            AnnotatedMember property, JavaType baseType)
    {
        // !!! TODO: implement properly
        return collectAndResolveSubtypesByClass(config, property, baseType);
    }

    @Override
    public Collection<NamedType> collectAndResolveSubtypesByName(MapperConfig<?> config,
            AnnotatedClass type)
    {
        // !!! TODO: implement properly
        return collectAndResolveSubtypesByClass(config, type);
    }

    /*
    /**********************************************************
    /* Deprecated method overrides
    /**********************************************************
     */

    @Override
    public Collection<NamedType> collectAndResolveSubtypes(AnnotatedMember property,
        MapperConfig<?> config, AnnotationIntrospector ai, JavaType baseType)
    {
        return collectAndResolveSubtypesByClass(config, property, baseType);
    }

    @Override
    public Collection<NamedType> collectAndResolveSubtypes(AnnotatedClass type,
            MapperConfig<?> config, AnnotationIntrospector ai)
    {
        return collectAndResolveSubtypesByClass(config, type);
    }

    /*
    /**********************************************************
    /* Internal methods
    /**********************************************************
     */

    /**
     * Method called to find subtypes for a specific type (class)
     */
    protected void _collectAndResolve(AnnotatedClass annotatedType, NamedType namedType,
            MapperConfig<?> config, AnnotationIntrospector ai,
            HashMap<NamedType, NamedType> collectedSubtypes)
    {
        if (!namedType.hasName()) {
            String name = ai.findTypeName(annotatedType);
            if (name != null) {
                namedType = new NamedType(namedType.getType(), name);
            }
        }

        // First things first: is base type itself included?
        if (collectedSubtypes.containsKey(namedType)) {
            // if so, no recursion; however, may need to update name?
            if (namedType.hasName()) {
                NamedType prev = collectedSubtypes.get(namedType);
                if (!prev.hasName()) {
                    collectedSubtypes.put(namedType, namedType);
                }
            }
            return;
        }
        // if it wasn't, add and check subtypes recursively
        collectedSubtypes.put(namedType, namedType);
        Collection<NamedType> st = ai.findSubtypes(annotatedType);
        if (st != null && !st.isEmpty()) {
            for (NamedType subtype : st) {
                AnnotatedClass subtypeClass = AnnotatedClass.constructWithoutSuperTypes(subtype.getType(), ai, config);
                // One more thing: name may be either in reference, or in subtype:
                if (!subtype.hasName()) {
                    subtype = new NamedType(subtype.getType(), ai.findTypeName(subtypeClass));
                }
                _collectAndResolve(subtypeClass, subtype, config, ai, collectedSubtypes);
            }
        }
    }
}
