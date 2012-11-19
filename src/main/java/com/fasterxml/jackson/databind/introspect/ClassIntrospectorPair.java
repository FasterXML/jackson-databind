package com.fasterxml.jackson.databind.introspect;

import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.DeserializationConfig;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.cfg.MapperConfig;

import java.io.Serializable;

/**
 * Helper class that allows using 2 introspectors such that one
 * introspector acts as the primary one to use; and second one
 * as a fallback used if the primary does not provide conclusive
 * or useful result for a method.
 *
 * @since 2.2
 */
public class ClassIntrospectorPair
    extends ClassIntrospector
    implements Serializable
{
    private static final long serialVersionUID = 1L;

    protected final ClassIntrospector _primary, _secondary;

    public ClassIntrospectorPair(ClassIntrospector p, ClassIntrospector s)
    {
        _primary = p;
        _secondary = s;
    }

    /**
     * Helper method for constructing a Pair from two given introspectors (if
     * neither is null); or returning non-null introspector if one is null
     * (and return just null if both are null)
     */
    public static ClassIntrospector create(ClassIntrospector primary,
            ClassIntrospector secondary)
    {
        if (primary == null) {
            return secondary;
        }
        if (secondary == null) {
            return primary;
        }
        return new ClassIntrospectorPair(primary, secondary);
    }

    @Override
    public BeanDescription forSerialization(SerializationConfig cfg, JavaType type, MixInResolver r) {
        BeanDescription result = _primary.forSerialization(cfg, type, r);
        if (result == null) {
            result = _secondary.forSerialization(cfg, type, r);
        }
        return result;
    }

    @Override
    public BeanDescription forDeserialization(DeserializationConfig cfg, JavaType type, MixInResolver r) {
        BeanDescription result = _primary.forDeserialization(cfg, type, r);
        if (result == null) {
            result = _secondary.forDeserialization(cfg, type, r);
        }
        return result;
    }

    @Override
    public BeanDescription forDeserializationWithBuilder(DeserializationConfig cfg, JavaType type, MixInResolver r) {
        BeanDescription result = _primary.forDeserializationWithBuilder(cfg, type, r);
        if (result == null) {
            result = _secondary.forDeserializationWithBuilder(cfg, type, r);
        }
        return result;
    }

    @Override
    public BeanDescription forCreation(DeserializationConfig cfg, JavaType type, MixInResolver r) {
        BeanDescription result = _primary.forCreation(cfg, type, r);
        if (result == null) {
            result = _secondary.forCreation(cfg, type, r);
        }
        return result;
    }

    @Override
    public BeanDescription forClassAnnotations(MapperConfig<?> cfg, JavaType type, MixInResolver r) {
        BeanDescription result = _primary.forClassAnnotations(cfg, type, r);
        if (result == null) {
            result = _secondary.forClassAnnotations(cfg, type, r);
        }
        return result;
    }

    @Override
    public BeanDescription forDirectClassAnnotations(MapperConfig<?> cfg, JavaType type, MixInResolver r) {
        BeanDescription result = _primary.forDirectClassAnnotations(cfg, type, r);
        if (result == null) {
            result = _secondary.forDirectClassAnnotations(cfg, type, r);
        }
        return result;
    }
}
