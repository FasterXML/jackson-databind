package com.fasterxml.jackson.databind.type;

import com.fasterxml.jackson.databind.JavaType;

/**
 * Type that represents "true" Java Map types.
 */
public final class MapType extends MapLikeType
{
    private static final long serialVersionUID = -811146779148281500L;

    /*
    /**********************************************************
    /* Life-cycle
    /**********************************************************
     */

    private MapType(Class<?> mapType, JavaType keyT, JavaType valueT,
            Object valueHandler, Object typeHandler, boolean asStatic) {
        super(mapType, keyT, valueT, valueHandler, typeHandler, asStatic);
    }
    
    public static MapType construct(Class<?> rawType, JavaType keyT, JavaType valueT) {
        // nominally component types will be just Object.class
        return new MapType(rawType, keyT, valueT, null, null, false);
    }

    @Override
    protected JavaType _narrow(Class<?> subclass) {
        return new MapType(subclass, _keyType, _valueType,
                _valueHandler, _typeHandler, _asStatic);
    }

    @Override
    public JavaType narrowContentsBy(Class<?> contentClass)
    {
        // Can do a quick check first:
        if (contentClass == _valueType.getRawClass()) {
            return this;
        }
        return new MapType(_class, _keyType, _valueType.narrowBy(contentClass),
                _valueHandler, _typeHandler, _asStatic);
    }

    @Override
    public JavaType widenContentsBy(Class<?> contentClass)
    {
        if (contentClass == _valueType.getRawClass()) {
            return this;
        }
        return new MapType(_class, _keyType, _valueType.widenBy(contentClass),
                _valueHandler, _typeHandler, _asStatic);
    }
    
    @Override
    public JavaType narrowKey(Class<?> keySubclass)
    {
        // Can do a quick check first:
        if (keySubclass == _keyType.getRawClass()) {
            return this;
        }
        return new MapType(_class, _keyType.narrowBy(keySubclass), _valueType,
                _valueHandler, _typeHandler, _asStatic);
    }

    @Override
    public JavaType widenKey(Class<?> keySubclass)
    {
        // Can do a quick check first:
        if (keySubclass == _keyType.getRawClass()) {
            return this;
        }
        return new MapType(_class, _keyType.widenBy(keySubclass), _valueType,
                _valueHandler, _typeHandler, _asStatic);
    }
    
    @Override
    public MapType withTypeHandler(Object h) {
        return new MapType(_class, _keyType, _valueType, _valueHandler, h, _asStatic);
    }

    @Override
    public MapType withContentTypeHandler(Object h)
    {
        return new MapType(_class, _keyType, _valueType.withTypeHandler(h),
                _valueHandler, _typeHandler, _asStatic);
    }
    
    @Override
    public MapType withValueHandler(Object h) {
        return new MapType(_class, _keyType, _valueType, h, _typeHandler, _asStatic);
    }

    @Override
    public MapType withContentValueHandler(Object h) {
        return new MapType(_class, _keyType, _valueType.withValueHandler(h),
                _valueHandler, _typeHandler, _asStatic);
    }

    @Override
    public MapType withStaticTyping() {
        if (_asStatic) {
            return this;
        }
        return new MapType(_class, _keyType.withStaticTyping(), _valueType.withStaticTyping(),
                _valueHandler, _typeHandler, true);
    }

    /*
    /**********************************************************
    /* Extended API
    /**********************************************************
     */
    
    @Override
    public MapType withKeyTypeHandler(Object h)
    {
        return new MapType(_class, _keyType.withTypeHandler(h), _valueType,
                _valueHandler, _typeHandler, _asStatic);
    }

    @Override
    public MapType withKeyValueHandler(Object h) {
        return new MapType(_class, _keyType.withValueHandler(h), _valueType,
                _valueHandler, _typeHandler, _asStatic);
    }
    
    /*
    /**********************************************************
    /* Standard methods
    /**********************************************************
     */

    @Override
    public String toString()
    {
        return "[map type; class "+_class.getName()+", "+_keyType+" -> "+_valueType+"]";
    }
}
