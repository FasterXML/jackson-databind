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

    public TypeKey(Class<?> key, boolean typed) {
        _class = key;
        _type = null;
        _isTyped = typed;
        _hashCode = hash(key, typed);
    }

    public TypeKey(JavaType key, boolean typed) {
        _type = key;
        _class = null;
        _isTyped = typed;
        _hashCode = hash(key, typed);
    }

    private final static int hash(Class<?> cls, boolean typed) {
        int hash = cls.getName().hashCode();
        if (typed) {
            ++hash;
        }
        return hash;
    }

    private final static int hash(JavaType type, boolean typed) {
        int hash = type.hashCode() - 1;
        if (typed) {
            --hash;
        }
        return hash;
    }
    
    public final void resetTyped(Class<?> cls) {
        _type = null;
        _class = cls;
        _isTyped = true;
        _hashCode = hash(cls, true);
    }

    public final void resetUntyped(Class<?> cls) {
        _type = null;
        _class = cls;
        _isTyped = false;
        _hashCode = hash(cls, false);
    }
    
    public final void resetTyped(JavaType type) {
        _type = type;
        _class = null;
        _isTyped = true;
        _hashCode = hash(type, true);
    }

    public final void resetUntyped(JavaType type) {
        _type = type;
        _class = null;
        _isTyped = false;
        _hashCode = hash(type, false);
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