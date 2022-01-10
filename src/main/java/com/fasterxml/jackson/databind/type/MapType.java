package com.fasterxml.jackson.databind.type;

import com.fasterxml.jackson.databind.JavaType;

/**
 * Type that represents "true" Java Map types.
 */
public final class MapType extends MapLikeType
{
    private static final long serialVersionUID = 1L;

    /*
    /**********************************************************
    /* Life-cycle
    /**********************************************************
     */

    private MapType(Class<?> mapType, TypeBindings bindings,
            JavaType superClass, JavaType[] superInts, JavaType keyT, JavaType valueT,
            Object valueHandler, Object typeHandler, boolean asStatic) {
        super(mapType, bindings, superClass, superInts,
                keyT, valueT, valueHandler, typeHandler, asStatic);
    }

    protected MapType(TypeBase base, JavaType keyT, JavaType valueT) {
        super(base, keyT, valueT);
    }

    public static MapType construct(Class<?> rawType, TypeBindings bindings,
            JavaType superClass, JavaType[] superInts,
            JavaType keyT, JavaType valueT) {
        return new MapType(rawType, bindings, superClass, superInts, keyT, valueT, null, null, false);
    }

    @Override
    public MapType withTypeHandler(Object h) {
        return new MapType(_class, _bindings,
                _superClass, _superInterfaces, _keyType, _valueType, _valueHandler, h, _asStatic);
    }

    @Override
    public MapType withContentTypeHandler(Object h)
    {
        return new MapType(_class, _bindings,
                _superClass, _superInterfaces, _keyType, _valueType.withTypeHandler(h),
                _valueHandler, _typeHandler, _asStatic);
    }
    
    @Override
    public MapType withValueHandler(Object h) {
        return new MapType(_class, _bindings,
                _superClass, _superInterfaces, _keyType, _valueType, h, _typeHandler, _asStatic);
    }

    @Override
    public MapType withContentValueHandler(Object h) {
        return new MapType(_class, _bindings,
                _superClass, _superInterfaces, _keyType, _valueType.withValueHandler(h),
                _valueHandler, _typeHandler, _asStatic);
    }

    @Override
    public MapType withStaticTyping() {
        if (_asStatic) {
            return this;
        }
        return new MapType(_class, _bindings,
                _superClass, _superInterfaces, _keyType.withStaticTyping(), _valueType.withStaticTyping(),
                _valueHandler, _typeHandler, true);
    }

    @Override
    public JavaType withContentType(JavaType contentType) {
        if (_valueType == contentType) {
            return this;
        }
        return new MapType(_class, _bindings, _superClass, _superInterfaces,
                _keyType, contentType, _valueHandler, _typeHandler, _asStatic);
    }
    
    @Override
    public MapType withKeyType(JavaType keyType) {
        if (keyType == _keyType) {
            return this;
        }
        return new MapType(_class, _bindings, _superClass, _superInterfaces,
                keyType, _valueType, _valueHandler, _typeHandler, _asStatic);
    }

    @Override
    public JavaType refine(Class<?> rawType, TypeBindings bindings,
            JavaType superClass, JavaType[] superInterfaces) {
        return new MapType(rawType, bindings,
                superClass, superInterfaces, _keyType, _valueType,
                _valueHandler, _typeHandler, _asStatic);
    }

    /*
    /**********************************************************
    /* Extended API
    /**********************************************************
     */
    
    @Override
    public MapType withKeyTypeHandler(Object h)
    {
        return new MapType(_class, _bindings,
                _superClass, _superInterfaces, _keyType.withTypeHandler(h), _valueType,
                _valueHandler, _typeHandler, _asStatic);
    }

    @Override
    public MapType withKeyValueHandler(Object h) {
        return new MapType(_class, _bindings,
                _superClass, _superInterfaces, _keyType.withValueHandler(h), _valueType,
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
