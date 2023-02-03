package com.fasterxml.jackson.databind.util;

import com.fasterxml.jackson.databind.JavaType;

/**
 * Key that offers two "modes"; one with raw class, as used for
 * cases were raw class type is available (for example, when using
 * runtime type); and one with full generics-including.
 */
public class TypeKey
{
    protected int _hashCode;

    protected Class<?> _class;

    protected JavaType _type;

    /**
     * Indicator of whether serializer stored has a type serializer
     * wrapper around it or not; if not, it is "untyped" serializer;
     * if it has, it is "typed"
     */
    protected boolean _isTyped;

    public TypeKey() { }

    public TypeKey(TypeKey src) {
        _hashCode = src._hashCode;
        _class = src._class;
        _type = src._type;
        _isTyped = src._isTyped;
    }

    public TypeKey(Class<?> key, boolean typed) {
        _class = key;
        _type = null;
        _isTyped = typed;
        _hashCode = typed ? typedHash(key) : untypedHash(key);
    }

    public TypeKey(JavaType key, boolean typed) {
        _type = key;
        _class = null;
        _isTyped = typed;
        _hashCode = typed ? typedHash(key) : untypedHash(key);
    }

    public final static int untypedHash(Class<?> cls) {
        return cls.getName().hashCode();
    }

    public final static int typedHash(Class<?> cls) {
        return cls.getName().hashCode()+1;
    }

    public final static int untypedHash(JavaType type) {
        return type.hashCode() - 1;
    }

    public final static int typedHash(JavaType type) {
        return type.hashCode() - 2;
    }

    public final void resetTyped(Class<?> cls) {
        _type = null;
        _class = cls;
        _isTyped = true;
        _hashCode = typedHash(cls);
    }

    public final void resetUntyped(Class<?> cls) {
        _type = null;
        _class = cls;
        _isTyped = false;
        _hashCode = untypedHash(cls);
    }

    public final void resetTyped(JavaType type) {
        _type = type;
        _class = null;
        _isTyped = true;
        _hashCode = typedHash(type);
    }

    public final void resetUntyped(JavaType type) {
        _type = type;
        _class = null;
        _isTyped = false;
        _hashCode = untypedHash(type);
    }

    public boolean isTyped() {
        return _isTyped;
    }

    public Class<?> getRawType() {
        return _class;
    }

    public JavaType getType() {
        return _type;
    }

    @Override public final int hashCode() { return _hashCode; }

    @Override public final String toString() {
        if (_class != null) {
            return "{class: "+_class.getName()+", typed? "+_isTyped+"}";
        }
        return "{type: "+_type+", typed? "+_isTyped+"}";
    }

    // note: we assume key is never used for anything other than as map key, so:
    @Override public final boolean equals(Object o)
    {
        if (o == null) return false;
        if (o == this) return true;
        if (o.getClass() != getClass()) {
            return false;
        }
        TypeKey other = (TypeKey) o;
        if (other._isTyped == _isTyped) {
            if (_class != null) {
                return other._class == _class;
            }
            return _type.equals(other._type);
        }
        return false;
    }
}