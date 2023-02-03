package com.fasterxml.jackson.databind.jsontype.impl;

import java.lang.reflect.Modifier;
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

    // @since 2.12
    protected StdSubtypeResolver(StdSubtypeResolver src) {
        LinkedHashSet<NamedType> reg = src._registeredSubtypes;
        _registeredSubtypes = (reg == null) ? null
                : new LinkedHashSet<>(reg);
    }

    // @since 2.12
    @Override
    public SubtypeResolver copy() {
        return new StdSubtypeResolver(this);
    }

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

    @Override // since 2.9
    public void registerSubtypes(Collection<Class<?>> subtypes) {
        int len = subtypes.size();
        NamedType[] types = new NamedType[len];
        int i = 0;
        for (Class<?> subtype : subtypes) {
            types[i++] = new NamedType(subtype);
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
        final Class<?> rawBase;
        if (baseType != null) {
            rawBase = baseType.getRawClass();
        } else if (property != null) {
            rawBase = property.getRawType();
        } else {
            throw new IllegalArgumentException("Both property and base type are nulls");
        }

        HashMap<NamedType, NamedType> collected = new HashMap<NamedType, NamedType>();
        // start with registered subtypes (which have precedence)
        if (_registeredSubtypes != null) {
            for (NamedType subtype : _registeredSubtypes) {
                // is it a subtype of root type?
                if (rawBase.isAssignableFrom(subtype.getType())) { // yes
                    AnnotatedClass curr = AnnotatedClassResolver.resolveWithoutSuperTypes(config,
                            subtype.getType());
                    _collectAndResolve(curr, subtype, config, ai, collected);
                }
            }
        }

        // then annotated types for property itself
        if (property != null) {
            Collection<NamedType> st = ai.findSubtypes(property);
            if (st != null) {
                for (NamedType nt : st) {
                    AnnotatedClass ac = AnnotatedClassResolver.resolveWithoutSuperTypes(config,
                            nt.getType());
                    _collectAndResolve(ac, nt, config, ai, collected);
                }
            }
        }

        NamedType rootType = new NamedType(rawBase, null);
        AnnotatedClass ac = AnnotatedClassResolver.resolveWithoutSuperTypes(config, rawBase);

        // and finally subtypes via annotations from base type (recursively)
        _collectAndResolve(ac, rootType, config, ai, collected);

        return new ArrayList<NamedType>(collected.values());
    }

    @Override
    public Collection<NamedType> collectAndResolveSubtypesByClass(MapperConfig<?> config,
            AnnotatedClass type)
    {
        final AnnotationIntrospector ai = config.getAnnotationIntrospector();
        HashMap<NamedType, NamedType> subtypes = new HashMap<>();

        // then consider registered subtypes (which have precedence over annotations)
        if (_registeredSubtypes != null) {
            Class<?> rawBase = type.getRawType();
            for (NamedType subtype : _registeredSubtypes) {
                // is it a subtype of root type?
                if (rawBase.isAssignableFrom(subtype.getType())) { // yes
                    AnnotatedClass curr = AnnotatedClassResolver.resolveWithoutSuperTypes(config,
                            subtype.getType());
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
    public Collection<NamedType> collectAndResolveSubtypesByTypeId(MapperConfig<?> config,
            AnnotatedMember property, JavaType baseType)
    {
        final AnnotationIntrospector ai = config.getAnnotationIntrospector();
        Class<?> rawBase = baseType.getRawClass();

        // Need to keep track of classes that have been handled already
        Set<Class<?>> typesHandled = new HashSet<Class<?>>();
        Map<String,NamedType> byName = new LinkedHashMap<String,NamedType>();

        // start with lowest-precedence, which is from type hierarchy
        NamedType rootType = new NamedType(rawBase, null);
        AnnotatedClass ac = AnnotatedClassResolver.resolveWithoutSuperTypes(config,
                rawBase);
        _collectAndResolveByTypeId(ac, rootType, config, typesHandled, byName);

        // then with definitions from property
        if (property != null) {
            Collection<NamedType> st = ai.findSubtypes(property);
            if (st != null) {
                for (NamedType nt : st) {
                    ac = AnnotatedClassResolver.resolveWithoutSuperTypes(config, nt.getType());
                    _collectAndResolveByTypeId(ac, nt, config, typesHandled, byName);
                }
            }
        }
        // and finally explicit type registrations (highest precedence)
        if (_registeredSubtypes != null) {
            for (NamedType subtype : _registeredSubtypes) {
                // is it a subtype of root type?
                if (rawBase.isAssignableFrom(subtype.getType())) { // yes
                    AnnotatedClass curr = AnnotatedClassResolver.resolveWithoutSuperTypes(config,
                            subtype.getType());
                    _collectAndResolveByTypeId(curr, subtype, config, typesHandled, byName);
                }
            }
        }
        return _combineNamedAndUnnamed(rawBase, typesHandled, byName);
    }

    @Override
    public Collection<NamedType> collectAndResolveSubtypesByTypeId(MapperConfig<?> config,
            AnnotatedClass baseType)
    {
        final Class<?> rawBase = baseType.getRawType();
        Set<Class<?>> typesHandled = new HashSet<Class<?>>();
        Map<String,NamedType> byName = new LinkedHashMap<String,NamedType>();

        NamedType rootType = new NamedType(rawBase, null);
        _collectAndResolveByTypeId(baseType, rootType, config, typesHandled, byName);

        if (_registeredSubtypes != null) {
            for (NamedType subtype : _registeredSubtypes) {
                // is it a subtype of root type?
                if (rawBase.isAssignableFrom(subtype.getType())) { // yes
                    AnnotatedClass curr = AnnotatedClassResolver.resolveWithoutSuperTypes(config,
                            subtype.getType());
                    _collectAndResolveByTypeId(curr, subtype, config, typesHandled, byName);
                }
            }
        }
        return _combineNamedAndUnnamed(rawBase, typesHandled, byName);
    }

    /*
    /**********************************************************
    /* Internal methods
    /**********************************************************
     */

    /**
     * Method called to find subtypes for a specific type (class), using
     * type (class) as the unique key (in case of conflicts).
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

        //For Serialization we only want to return a single NamedType per class so it's
        //unambiguous what name we use.
        NamedType typeOnlyNamedType = new NamedType(namedType.getType());

        // First things first: is base type itself included?
        if (collectedSubtypes.containsKey(typeOnlyNamedType)) {
            // if so, no recursion; however, may need to update name?
            if (namedType.hasName()) {
                NamedType prev = collectedSubtypes.get(typeOnlyNamedType);
                if (!prev.hasName()) {
                    collectedSubtypes.put(typeOnlyNamedType, namedType);
                }
            }
            return;
        }
        // if it wasn't, add and check subtypes recursively
        collectedSubtypes.put(typeOnlyNamedType, namedType);
        Collection<NamedType> st = ai.findSubtypes(annotatedType);
        if (st != null && !st.isEmpty()) {
            for (NamedType subtype : st) {
                AnnotatedClass subtypeClass = AnnotatedClassResolver.resolveWithoutSuperTypes(config,
                        subtype.getType());
                _collectAndResolve(subtypeClass, subtype, config, ai, collectedSubtypes);
            }
        }
    }

    /**
     * Method called to find subtypes for a specific type (class), using
     * type id as the unique key (in case of conflicts).
     */
    protected void _collectAndResolveByTypeId(AnnotatedClass annotatedType, NamedType namedType,
            MapperConfig<?> config,
            Set<Class<?>> typesHandled, Map<String,NamedType> byName)
    {
        final AnnotationIntrospector ai = config.getAnnotationIntrospector();
        if (!namedType.hasName()) {
            String name = ai.findTypeName(annotatedType);
            if (name != null) {
                namedType = new NamedType(namedType.getType(), name);
            }
        }
        if (namedType.hasName()) {
            byName.put(namedType.getName(), namedType);
        }

        // only check subtypes if this type hadn't yet been handled
        if (typesHandled.add(namedType.getType())) {
            Collection<NamedType> st = ai.findSubtypes(annotatedType);
            if (st != null && !st.isEmpty()) {
                for (NamedType subtype : st) {
                    AnnotatedClass subtypeClass = AnnotatedClassResolver.resolveWithoutSuperTypes(config,
                            subtype.getType());
                    _collectAndResolveByTypeId(subtypeClass, subtype, config, typesHandled, byName);
                }
            }
        }
    }

    /**
     * Helper method used for merging explicitly named types and handled classes
     * without explicit names.
     */
    protected Collection<NamedType> _combineNamedAndUnnamed(Class<?> rawBase,
            Set<Class<?>> typesHandled, Map<String,NamedType> byName)
    {
        ArrayList<NamedType> result = new ArrayList<NamedType>(byName.values());

        // Ok, so... we will figure out which classes have no explicitly assigned name,
        // by removing Classes from Set. And for remaining classes, add an anonymous
        // marker
        for (NamedType t : byName.values()) {
            typesHandled.remove(t.getType());
        }
        for (Class<?> cls : typesHandled) {
            // 27-Apr-2017, tatu: [databind#1616] Do not add base type itself unless
            //     it is concrete (or has explicit type name)
            if ((cls == rawBase) && Modifier.isAbstract(cls.getModifiers())) {
                continue;
            }
            result.add(new NamedType(cls));
        }
        return result;
    }
}
