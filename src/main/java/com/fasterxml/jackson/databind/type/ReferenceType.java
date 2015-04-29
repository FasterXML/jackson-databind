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

    protected ReferenceType(Class<?> cls, JavaType refType,
            Object valueHandler, Object typeHandler, boolean asStatic)
    {
        super(cls, refType.hashCode(),
                valueHandler, typeHandler, asStatic);
        _referencedType = refType;
    }

    public static ReferenceType construct(Class<?> cls, JavaType refType,
            Object valueHandler, Object typeHandler)
    {
        return new ReferenceType(cls, refType, null, null, false);
    }                                   
    
    @Override
    public ReferenceType withTypeHandler(Object h)
    {
        if (h == _typeHandler) {
            return this;
        }
        return new ReferenceType(_class, _referencedType, _valueHandler, h, _asStatic);
    }

    @Override
    public ReferenceType withContentTypeHandler(Object h)
    {
        if (h == _referencedType.<Object>getTypeHandler()) {
            return this;
        }
        return new ReferenceType(_class, _referencedType.withTypeHandler(h),
                _valueHandler, _typeHandler, _asStatic);
    }

    @Override
    public ReferenceType withValueHandler(Object h) {
        if (h == _valueHandler) {
            return this;
        }
        return new ReferenceType(_class, _referencedType, h, _typeHandler,_asStatic);
    }

    @Override
    public ReferenceType withContentValueHandler(Object h) {
        if (h == _referencedType.<Object>getValueHandler()) {
            return this;
        }
        return new ReferenceType(_class, _referencedType.withValueHandler(h),
                _valueHandler, _typeHandler, _asStatic);
    }

    @Override
    public ReferenceType withStaticTyping() {
        if (_asStatic) {
            return this;
        }
        return new ReferenceType(_class, _referencedType.withStaticTyping(),
                 _valueHandler, _typeHandler, true);
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
    protected JavaType _narrow(Class<?> subclass)
    {
        // Should we check that there is a sub-class relationship?
        return new ReferenceType(subclass, _referencedType,
                _valueHandler, _typeHandler, _asStatic);
    }

    /*
    /**********************************************************
    /* Extended API
    /**********************************************************
     */
    
    @Override
    public JavaType getReferencedType() {
        return _referencedType;
    }

    @Override
    public boolean isReferenceType() {
        return true;
    }
    
    /*
    /**********************************************************
    /* Public API overrides
    /**********************************************************
     */

    @Override
    public int containedTypeCount() {
        return 1;
    }

    @Override
    public JavaType containedType(int index) {
        return (index == 0) ? _referencedType : null;
    }

    @Override
    public String containedTypeName(int index) {
        return (index == 0) ? "T" : null;
    }

    @Override
    public Class<?> getParameterSource() {
        // Hmmh. For now, assume it's the raw type
        return _class;
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
        sb.append(';');
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
