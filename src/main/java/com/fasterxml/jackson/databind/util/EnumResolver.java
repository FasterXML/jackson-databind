package com.fasterxml.jackson.databind.util;

import com.fasterxml.jackson.databind.AnnotationIntrospector;

import java.lang.reflect.Method;
import java.util.*;

/**
 * Helper class used to resolve String values (either JSON Object field
 * names or regular String values) into Java Enum instances.
 */
public class EnumResolver implements java.io.Serializable
{
    private static final long serialVersionUID = 1L;

    protected final Class<Enum<?>> _enumClass;

    protected final Enum<?>[] _enums;

    protected final HashMap<String, Enum<?>> _enumsById;

    protected EnumResolver(Class<Enum<?>> enumClass, Enum<?>[] enums, HashMap<String, Enum<?>> map)
    {
        _enumClass = enumClass;
        _enums = enums;
        _enumsById = map;
    }

    /**
     * Factory method for constructing resolver that maps from Enum.name() into
     * Enum value
     */
    public static EnumResolver constructFor(Class<Enum<?>> enumCls, AnnotationIntrospector ai)
    {
        Enum<?>[] enumValues = enumCls.getEnumConstants();
        if (enumValues == null) {
            throw new IllegalArgumentException("No enum constants for class "+enumCls.getName());
        }
        HashMap<String, Enum<?>> map = new HashMap<String, Enum<?>>();
        for (Enum<?> e : enumValues) {
            map.put(ai.findEnumValue(e), e);
        }
        return new EnumResolver(enumCls, enumValues, map);
    }

    /**
     * Factory method for constructing resolver that maps from Enum.toString() into
     * Enum value
     */
    public static EnumResolver constructUsingToString(Class<Enum<?>> enumCls)
    {
        Enum<?>[] enumValues = enumCls.getEnumConstants();
        HashMap<String, Enum<?>> map = new HashMap<String, Enum<?>>();
        // from last to first, so that in case of duplicate values, first wins
        for (int i = enumValues.length; --i >= 0; ) {
            Enum<?> e = enumValues[i];
            map.put(e.toString(), e);
        }
        return new EnumResolver(enumCls, enumValues, map);
    }    

    public static EnumResolver constructUsingMethod(Class<Enum<?>> enumCls,
            Method accessor)
    {
        Enum<?>[] enumValues = enumCls.getEnumConstants();
        HashMap<String, Enum<?>> map = new HashMap<String, Enum<?>>();
        // from last to first, so that in case of duplicate values, first wins
        for (int i = enumValues.length; --i >= 0; ) {
            Enum<?> en = enumValues[i];
            try {
                Object o = accessor.invoke(en);
                if (o != null) {
                    map.put(o.toString(), en);
                }
            } catch (Exception e) {
                throw new IllegalArgumentException("Failed to access @JsonValue of Enum value "+en+": "+e.getMessage());
            }
        }
        return new EnumResolver(enumCls, enumValues, map);
    }    

    /**
     * This method is needed because of the dynamic nature of constructing Enum
     * resolvers.
     */
    @SuppressWarnings({ "unchecked" })
    public static EnumResolver constructUnsafe(Class<?> rawEnumCls, AnnotationIntrospector ai)
    {            
        /* This is oh so wrong... but at least ugliness is mostly hidden in just
         * this one place.
         */
        Class<Enum<?>> enumCls = (Class<Enum<?>>) rawEnumCls;
        return constructFor(enumCls, ai);
    }

    /**
     * Method that needs to be used instead of {@link #constructUsingToString}
     * if static type of enum is not known.
     */
    @SuppressWarnings({ "unchecked" })
    public static EnumResolver constructUnsafeUsingToString(Class<?> rawEnumCls)
    {            
        // oh so wrong... not much that can be done tho
        Class<Enum<?>> enumCls = (Class<Enum<?>>) rawEnumCls;
        return constructUsingToString(enumCls);
    }

    /**
     * Method used when actual String serialization is indicated using @JsonValue
     * on a method.
     */
    @SuppressWarnings({ "unchecked" })
    public static EnumResolver constructUnsafeUsingMethod(Class<?> rawEnumCls, Method accessor)
    {            
        // wrong as ever but:
        Class<Enum<?>> enumCls = (Class<Enum<?>>) rawEnumCls;
        return constructUsingMethod(enumCls, accessor);
    }

    public CompactStringObjectMap constructLookup() {
        return CompactStringObjectMap.construct(_enumsById);
    }
    
    public Enum<?> findEnum(String key) { return _enumsById.get(key); }

    public Enum<?> getEnum(int index) {
        if (index < 0 || index >= _enums.length) {
            return null;
        }
        return _enums[index];
    }

    public Enum<?>[] getRawEnums() {
        return _enums;
    }
    
    public List<Enum<?>> getEnums() {
        ArrayList<Enum<?>> enums = new ArrayList<Enum<?>>(_enums.length);
        for (Enum<?> e : _enums) {
            enums.add(e);
        }
        return enums;
    }
    
    public Class<Enum<?>> getEnumClass() { return _enumClass; }

    public int lastValidIndex() { return _enums.length-1; }
}

