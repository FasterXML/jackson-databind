package com.fasterxml.jackson.databind.type;

import java.util.*;

import com.fasterxml.jackson.databind.JavaType;

/**
 * Simple types are defined as anything other than one of recognized
 * container types (arrays, Collections, Maps). For our needs we
 * need not know anything further, since we have no way of dealing
 * with generic types other than Collections and Maps.
 */
public class SimpleType // note: until 2.6 was final
    extends TypeBase
{
    private static final long serialVersionUID = 1L;

    /**
     * In case there are resolved type parameters, this field stores reference
     * to that type. It must be {@link #getRawClass()} or its supertype.
     * 
     * @since 2.5
     */
    protected final Class<?> _typeParametersFor;
    
    /**
     * Generic type arguments for this type.
     */
    protected final JavaType[] _typeParameters;

    /**
     * Names of generic type arguments for this type; will
     * match values in {@link #_typeParameters}
     */
    protected final String[] _typeNames;
    
    /*
    /**********************************************************
    /* Life-cycle
    /**********************************************************
     */

    protected SimpleType(Class<?> cls) {
        this(cls, null, null, null, null, false, null);
    }

    /**
     * @deprecated Since 2.5 use variant that takes one more argument
     */
    @Deprecated
    protected SimpleType(Class<?> cls, String[] typeNames, JavaType[] typeParams,
            Object valueHandler, Object typeHandler, boolean asStatic)
    {
        this(cls, typeNames, typeParams, valueHandler, typeHandler, asStatic, null);
    }

    /**
     * 
     * @param parametersFrom Interface or abstract class implemented by this type,
     *   and for which type parameters apply. It may be <code>cls</code> itself,
     *   but more commonly it is one of its supertypes.
     */
    protected SimpleType(Class<?> cls,
            String[] typeNames, JavaType[] typeParams,
            Object valueHandler, Object typeHandler, boolean asStatic,
            Class<?> parametersFrom)
    {
        super(cls, 0, valueHandler, typeHandler, asStatic);
        if (typeNames == null || typeNames.length == 0) {
            _typeNames = null;
            _typeParameters = null;
        } else {
            _typeNames = typeNames;
            _typeParameters = typeParams;
        }
        _typeParametersFor = parametersFrom;
    }

    /**
     * Pass-through constructor used by {@link ReferenceType}.
     * 
     * @since 2.6
     */
    protected SimpleType(Class<?> cls, int extraHash,
            Object valueHandler, Object typeHandler, boolean asStatic)
    {
        super(cls, extraHash, valueHandler, typeHandler, asStatic);
        _typeNames = null;
        _typeParameters = null;
        _typeParametersFor = cls;
    }
    
    /**
     * Method used by core Jackson classes: NOT to be used by application code.
     *<p>
     * NOTE: public only because it is called by <code>ObjectMapper</code> which is
     * not in same package
     */
    public static SimpleType constructUnsafe(Class<?> raw) {
        return new SimpleType(raw, null, null, null, null, false, null);
    }

    @Override
    protected JavaType _narrow(Class<?> subclass)
    {
        // Should we check that there is a sub-class relationship?
        return new SimpleType(subclass, _typeNames, _typeParameters, _valueHandler, _typeHandler,
                _asStatic, _typeParametersFor);
    }

    @Override
    public JavaType narrowContentsBy(Class<?> subclass)
    {
        // should never get called
        throw new IllegalArgumentException("Internal error: SimpleType.narrowContentsBy() should never be called");
    }

    @Override
    public JavaType widenContentsBy(Class<?> subclass)
    {
        // should never get called
        throw new IllegalArgumentException("Internal error: SimpleType.widenContentsBy() should never be called");
    }
    
    public static SimpleType construct(Class<?> cls)
    {
        /* Let's add sanity checks, just to ensure no
         * Map/Collection entries are constructed
         */
        if (Map.class.isAssignableFrom(cls)) {
            throw new IllegalArgumentException("Can not construct SimpleType for a Map (class: "+cls.getName()+")");
        }
        if (Collection.class.isAssignableFrom(cls)) {
            throw new IllegalArgumentException("Can not construct SimpleType for a Collection (class: "+cls.getName()+")");
        }
        // ... and while we are at it, not array types either
        if (cls.isArray()) {
            throw new IllegalArgumentException("Can not construct SimpleType for an array (class: "+cls.getName()+")");
        }
        return new SimpleType(cls);
    }

    @Override
    public SimpleType withTypeHandler(Object h)
    {
        return new SimpleType(_class, _typeNames, _typeParameters, _valueHandler, h, _asStatic, _typeParametersFor);
    }

    @Override
    public JavaType withContentTypeHandler(Object h) {
        // no content type, so:
        throw new IllegalArgumentException("Simple types have no content types; can not call withContenTypeHandler()");
    }

    @Override
    public SimpleType withValueHandler(Object h) {
        if (h == _valueHandler) {
            return this;
        }
        return new SimpleType(_class, _typeNames, _typeParameters, h, _typeHandler, _asStatic, _typeParametersFor);
    }
    
    @Override
    public  SimpleType withContentValueHandler(Object h) {
        // no content type, so:
        throw new IllegalArgumentException("Simple types have no content types; can not call withContenValueHandler()");
    }

    @Override
    public SimpleType withStaticTyping() {
        return _asStatic ? this : new SimpleType(_class,
                _typeNames, _typeParameters, _valueHandler, _typeHandler, true, _typeParametersFor);
    }

    @Override
    protected String buildCanonicalName()
    {
        StringBuilder sb = new StringBuilder();
        sb.append(_class.getName());
        if (_typeParameters != null && _typeParameters.length > 0) {
            sb.append('<');
            boolean first = true;
            for (JavaType t : _typeParameters) {
                if (first) {
                    first = false;
                } else {
                    sb.append(',');
                }
                sb.append(t.toCanonical());
            }
            sb.append('>');
        }
        return sb.toString();
    }
    
    /*
    /**********************************************************
    /* Public API
    /**********************************************************
     */

    @Override
    public boolean isContainerType() { return false; }
    
    @Override
    public int containedTypeCount() {
        return (_typeParameters == null) ? 0 : _typeParameters.length;
    }

    @Override
    public JavaType containedType(int index)
    {
        if (index < 0 || _typeParameters == null || index >= _typeParameters.length) {
            return null;
        }
        return _typeParameters[index];
    }

    @Override
    public String containedTypeName(int index)
    {
        if (index < 0 || _typeNames == null || index >= _typeNames.length) {
            return null;
        }
        return _typeNames[index];
    }

    @Override
    public Class<?> getParameterSource() {
        return _typeParametersFor;
    }
    
    @Override
    public StringBuilder getErasedSignature(StringBuilder sb) {
        return _classSignature(_class, sb, true);
    }
    
    @Override
    public StringBuilder getGenericSignature(StringBuilder sb)
    {
        _classSignature(_class, sb, false);
        if (_typeParameters != null) {
            sb.append('<');
            for (JavaType param : _typeParameters) {
                sb = param.getGenericSignature(sb);
            }
            sb.append('>');
        }
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
        StringBuilder sb = new StringBuilder(40);
        sb.append("[simple type, class ").append(buildCanonicalName()).append(']');
        return sb.toString();
    }

    @Override
    public boolean equals(Object o)
    {
        if (o == this) return true;
        if (o == null) return false;
        if (o.getClass() != getClass()) return false;

        SimpleType other = (SimpleType) o;

        // Classes must be identical... 
        if (other._class != this._class) return false;

        // And finally, generic bindings, if any
        JavaType[] p1 = _typeParameters;
        JavaType[] p2 = other._typeParameters;
        if (p1 == null) {
            return (p2 == null) || p2.length == 0;
        }
        if (p2 == null) return false;

        if (p1.length != p2.length) return false;
        for (int i = 0, len = p1.length; i < len; ++i) {
            if (!p1[i].equals(p2[i])) {
                return false;
            }
        }
        return true;
    }
}
