package com.fasterxml.jackson.databind.type;

public final class ClassWithTypeBindingsKey
        implements java.io.Serializable
{
    private static final long serialVersionUID = 1L;

    private final ClassKey _classKey;

    private final TypeBindings _typeBindings;

    private final int _hashCode;

    public ClassWithTypeBindingsKey(Class<?> clz, TypeBindings typeBindings)
    {
        _classKey = new ClassKey(clz);
        _typeBindings = typeBindings == null ? TypeBindings.emptyBindings() : typeBindings;
        _hashCode = _classKey.hashCode() * 41 + _typeBindings.hashCode();
    }

    /*
    /**********************************************************
    /* Standard methods
    /**********************************************************
     */

    @Override
        public boolean equals(Object o)
    {
        if (o == this) return true;
        if (o == null) return false;
        if (o.getClass() != getClass()) return false;
        ClassWithTypeBindingsKey other = (ClassWithTypeBindingsKey) o;
        if (!other._classKey.equals(_classKey)) return false;
        try {
            return other._typeBindings.equals(_typeBindings); // TODO: Fix cause of NPE
        } catch (RuntimeException ex) {
            return false;
        }
    }

    @Override public int hashCode() { return _hashCode; }

    @Override public String toString() { return _classKey.toString() + _typeBindings.toString(); }
}