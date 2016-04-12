package com.fasterxml.jackson.databind.type;

import com.fasterxml.jackson.databind.JavaType;

/**
 * Specialized {@link SimpleType} for types that are referential types,
 * that is, values that can be dereferenced to another value (or null),
 * of different type.
 * Referenced type is accessible using {@link #getContentType()}.
 * 
 * @since 2.6
 */
public class ReferenceType extends SimpleType
{
    private static final long serialVersionUID = 1L;

    protected final JavaType _referencedType;

    protected ReferenceType(Class<?> cls, TypeBindings bindings,
            JavaType superClass, JavaType[] superInts, JavaType refType,
            Object valueHandler, Object typeHandler, boolean asStatic)
    {
        super(cls, bindings, superClass, superInts, refType.hashCode(),
                valueHandler, typeHandler, asStatic);
        _referencedType = refType;
    }

    /**
     * @since 2.7
     */
    protected ReferenceType(TypeBase base, JavaType refType)
    {
        super(base);
        _referencedType = refType;
    }

    /**
     * Factory method that can be used to "upgrade" a basic type into collection-like
     * one; usually done via {@link TypeModifier}
     *
     * @since 2.7
     */
    public static ReferenceType upgradeFrom(JavaType baseType, JavaType refType) {
        if (refType == null) {
            throw new IllegalArgumentException("Missing referencedType");
        }
        // 19-Oct-2015, tatu: Not sure if and how other types could be used as base;
        //    will cross that bridge if and when need be
        if (baseType instanceof TypeBase) {
            return new ReferenceType((TypeBase) baseType, refType);
        }
        throw new IllegalArgumentException("Can not upgrade from an instance of "+baseType.getClass());
    }

    /**
     * @since 2.7
     */
    public static ReferenceType construct(Class<?> cls, TypeBindings bindings,
            JavaType superClass, JavaType[] superInts, JavaType refType)
    {
        return new ReferenceType(cls, bindings, superClass, superInts,
                refType, null, null, false);
    }

    @Deprecated // since 2.7
    public static ReferenceType construct(Class<?> cls, JavaType refType) {
        return new ReferenceType(cls, TypeBindings.emptyBindings(),
                // !!! TODO: missing supertypes
                null, null, refType, null, null, false);
    }

    @Override
    public JavaType withContentType(JavaType contentType) {
        if (_referencedType == contentType) {
            return this;
        }
        return new ReferenceType(_class, _bindings, _superClass, _superInterfaces,
                contentType, _valueHandler, _typeHandler, _asStatic);
    }

    @Override
    public ReferenceType withTypeHandler(Object h)
    {
        if (h == _typeHandler) {
            return this;
        }
        return new ReferenceType(_class, _bindings,
                _superClass, _superInterfaces, _referencedType, _valueHandler, h, _asStatic);
    }

    @Override
    public ReferenceType withContentTypeHandler(Object h)
    {
        if (h == _referencedType.<Object>getTypeHandler()) {
            return this;
        }
        return new ReferenceType(_class, _bindings,
                _superClass, _superInterfaces, _referencedType.withTypeHandler(h),
                _valueHandler, _typeHandler, _asStatic);
    }

    @Override
    public ReferenceType withValueHandler(Object h) {
        if (h == _valueHandler) {
            return this;
        }
        return new ReferenceType(_class, _bindings,
                _superClass, _superInterfaces, _referencedType, h, _typeHandler,_asStatic);
    }

    @Override
    public ReferenceType withContentValueHandler(Object h) {
        if (h == _referencedType.<Object>getValueHandler()) {
            return this;
        }
        JavaType refdType = _referencedType.withValueHandler(h);
        return new ReferenceType(_class, _bindings,
                _superClass, _superInterfaces, refdType,
                _valueHandler, _typeHandler, _asStatic);
    }

    @Override
    public ReferenceType withStaticTyping() {
        if (_asStatic) {
            return this;
        }
        return new ReferenceType(_class, _bindings,
                _superClass, _superInterfaces, _referencedType.withStaticTyping(),
                 _valueHandler, _typeHandler, true);
    }

    @Override
    public JavaType refine(Class<?> rawType, TypeBindings bindings,
            JavaType superClass, JavaType[] superInterfaces) {
        return new ReferenceType(rawType, _bindings,
                superClass, superInterfaces, _referencedType,
                _valueHandler, _typeHandler, _asStatic);
    }

    @Override
    protected String buildCanonicalName()
    {
        StringBuilder sb = new StringBuilder();
        sb.append(_class.getName());
        sb.append('<');
        sb.append(_referencedType.toCanonical());
        return sb.toString();
    }

    /*
    /**********************************************************
    /* Narrow/widen
    /**********************************************************
     */

    @Override
    @Deprecated // since 2.7
    protected JavaType _narrow(Class<?> subclass)
    {
        // Should we check that there is a sub-class relationship?
        return new ReferenceType(subclass, _bindings,
                _superClass, _superInterfaces, _referencedType,
                _valueHandler, _typeHandler, _asStatic);
    }

    /*
    /**********************************************************
    /* Public API overrides
    /**********************************************************
     */

    @Override
    public JavaType getContentType() {
        return _referencedType;
    }
    
    @Override
    public JavaType getReferencedType() {
        return _referencedType;
    }

    @Override
    public boolean isReferenceType() {
        return true;
    }

    @Override
    public StringBuilder getErasedSignature(StringBuilder sb) {
        return _classSignature(_class, sb, true);
    }
    
    @Override
    public StringBuilder getGenericSignature(StringBuilder sb)
    {
        _classSignature(_class, sb, false);
        sb.append('<');
        sb = _referencedType.getGenericSignature(sb);
        sb.append(">;");
        return sb;
    }
    
    /*
    /**********************************************************
    /* Standard methods
    /**********************************************************
     */

    @Override
    public String toString()
    {
        return new StringBuilder(40)
            .append("[reference type, class ")
            .append(buildCanonicalName())
            .append('<')
            .append(_referencedType)
            .append('>')
            .append(']')
            .toString();
    }

    @Override
    public boolean equals(Object o)
    {
        if (o == this) return true;
        if (o == null) return false;
        if (o.getClass() != getClass()) return false;

        ReferenceType other = (ReferenceType) o;

        if (other._class != _class) return false;
        
        // Otherwise actually mostly worry about referenced type
        return _referencedType.equals(other._referencedType);
    }
}
