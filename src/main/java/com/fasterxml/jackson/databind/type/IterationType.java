package com.fasterxml.jackson.databind.type;

import java.util.Objects;

import com.fasterxml.jackson.databind.JavaType;

/**
 * Specialized {@link SimpleType} for types that are allow iteration
 * over Collection(-like) types: this includes types like
 * {@link java.util.Iterator}.
 * Iterated (content) type is accessible using {@link #getContentType()}.
 *
 * @since 2.16
 */
public class IterationType extends SimpleType
{
    private static final long serialVersionUID = 1L;

    protected final JavaType _iteratedType;

    protected IterationType(Class<?> cls, TypeBindings bindings,
            JavaType superClass, JavaType[] superInts, JavaType iteratedType,
            Object valueHandler, Object typeHandler, boolean asStatic)
    {
        super(cls, bindings, superClass, superInts, Objects.hashCode(iteratedType),
                valueHandler, typeHandler, asStatic);
        _iteratedType = iteratedType;
    }

    /**
     * Constructor used when upgrading into this type (via {@link #upgradeFrom},
     * the usual way for {@link IterationType}s to come into existence.
     * Sets up what is considered the "base" iteration type
     */
    protected IterationType(TypeBase base, JavaType iteratedType)
    {
        super(base);
        _iteratedType = iteratedType;
    }

    /**
     * Factory method that can be used to "upgrade" a basic type into iteration
     * type; usually done via {@link TypeModifier}
     *
     * @param baseType Resolved non-iteration type (usually {@link SimpleType}) that is being upgraded
     * @param iteratedType Iterated type; usually the first and only type parameter, but not necessarily
     */
    public static IterationType upgradeFrom(JavaType baseType, JavaType iteratedType) {
        Objects.requireNonNull(iteratedType);
        // 19-Oct-2015, tatu: Not sure if and how other types could be used as base;
        //    will cross that bridge if and when need be
        if (baseType instanceof TypeBase) {
            return new IterationType((TypeBase) baseType, iteratedType);
        }
        throw new IllegalArgumentException("Cannot upgrade from an instance of "+baseType.getClass());
    }

    public static IterationType construct(Class<?> cls, TypeBindings bindings,
            JavaType superClass, JavaType[] superInts, JavaType iteratedType)
    {
        return new IterationType(cls, bindings, superClass, superInts,
                iteratedType, null, null, false);
    }

    @Override
    public JavaType withContentType(JavaType contentType) {
        if (_iteratedType == contentType) {
            return this;
        }
        return new IterationType(_class, _bindings, _superClass, _superInterfaces,
                contentType, _valueHandler, _typeHandler, _asStatic);
    }

    @Override
    public IterationType withTypeHandler(Object h)
    {
        if (h == _typeHandler) {
            return this;
        }
        return new IterationType(_class, _bindings, _superClass, _superInterfaces,
                _iteratedType, _valueHandler, h, _asStatic);
    }

    @Override
    public IterationType withContentTypeHandler(Object h)
    {
        if (h == _iteratedType.<Object>getTypeHandler()) {
            return this;
        }
        return new IterationType(_class, _bindings, _superClass, _superInterfaces,
                _iteratedType.withTypeHandler(h),
                _valueHandler, _typeHandler, _asStatic);
    }

    @Override
    public IterationType withValueHandler(Object h) {
        if (h == _valueHandler) {
            return this;
        }
        return new IterationType(_class, _bindings,
                _superClass, _superInterfaces, _iteratedType,
                h, _typeHandler,_asStatic);
    }

    @Override
    public IterationType withContentValueHandler(Object h) {
        if (h == _iteratedType.<Object>getValueHandler()) {
            return this;
        }
        return new IterationType(_class, _bindings,
                _superClass, _superInterfaces, _iteratedType.withValueHandler(h),
                _valueHandler, _typeHandler, _asStatic);
    }

    @Override
    public IterationType withStaticTyping() {
        if (_asStatic) {
            return this;
        }
        return new IterationType(_class, _bindings, _superClass, _superInterfaces,
                _iteratedType.withStaticTyping(),
                 _valueHandler, _typeHandler, true);
    }

    @Override
    public JavaType refine(Class<?> rawType, TypeBindings bindings,
            JavaType superClass, JavaType[] superInterfaces) {
        return new IterationType(rawType, _bindings,
                superClass, superInterfaces, _iteratedType,
                _valueHandler, _typeHandler, _asStatic);
    }

    @Override
    protected String buildCanonicalName()
    {
        StringBuilder sb = new StringBuilder();
        sb.append(_class.getName());
        if ((_iteratedType != null) && _hasNTypeParameters(1)) {
            sb.append('<');
            sb.append(_iteratedType.toCanonical());
            sb.append('>');
        }
        return sb.toString();
    }

    /*
    /**********************************************************
    /* Public API overrides
    /**********************************************************
     */

    @Override
    public JavaType getContentType() {
        return _iteratedType;
    }

    @Override
    public boolean hasContentType() {
        return true;
    }

    @Override
    public boolean isIterationType() {
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
        sb = _iteratedType.getGenericSignature(sb);
        sb.append(">;");
        return sb;
    }
}
