package com.fasterxml.jackson.databind.type;

import com.fasterxml.jackson.databind.JavaType;

/**
 * Type that represents Java Collection types (Lists, Sets).
 */
public final class CollectionType
    extends CollectionLikeType
{
    private static final long serialVersionUID = 1L;

    /*
    /**********************************************************
    /* Life-cycle
    /**********************************************************
     */

    private CollectionType(Class<?> collT, JavaType elemT,
            Object valueHandler, Object typeHandler, boolean asStatic)
    {
        super(collT,  elemT, valueHandler, typeHandler, asStatic);
    }

    @Override
    protected JavaType _narrow(Class<?> subclass) {
        return new CollectionType(subclass, _elementType, null, null, _asStatic);
    }

    @Override
    public JavaType narrowContentsBy(Class<?> contentClass)
    {
        // Can do a quick check first:
        if (contentClass == _elementType.getRawClass()) {
            return this;
        }
        return new CollectionType(_class, _elementType.narrowBy(contentClass),
                _valueHandler, _typeHandler, _asStatic);
    }

    @Override
    public JavaType widenContentsBy(Class<?> contentClass)
    {
        // Can do a quick check first:
        if (contentClass == _elementType.getRawClass()) {
            return this;
        }
        return new CollectionType(_class, _elementType.widenBy(contentClass),
                _valueHandler, _typeHandler, _asStatic);
    }
    
    public static CollectionType construct(Class<?> rawType, JavaType elemT)
    {
        // nominally component types will be just Object.class
        return new CollectionType(rawType, elemT, null, null, false);
    }

    // Since 1.7:
    @Override
    public CollectionType withTypeHandler(Object h) {
        return new CollectionType(_class, _elementType, _valueHandler, h, _asStatic);
    }

    // Since 1.7:
    @Override
    public CollectionType withContentTypeHandler(Object h)
    {
        return new CollectionType(_class, _elementType.withTypeHandler(h),
                _valueHandler, _typeHandler, _asStatic);
    }

    @Override
    public CollectionType withValueHandler(Object h) {
        return new CollectionType(_class, _elementType, h, _typeHandler, _asStatic);
    }

    @Override
    public  CollectionType withContentValueHandler(Object h) {
        return new CollectionType(_class, _elementType.withValueHandler(h),
                _valueHandler, _typeHandler, _asStatic);
    }

    @Override
    public CollectionType withStaticTyping() {
        if (_asStatic) {
            return this;
        }
        return new CollectionType(_class, _elementType.withStaticTyping(),
                _valueHandler, _typeHandler, true);
    }

    /*
    /**********************************************************
    /* Overridden accessors
    /**********************************************************
     */
    
    @Override
    public Class<?> getParameterSource() {
        return java.util.Collection.class;
    }
    
    /*
    /**********************************************************
    /* Standard methods
    /**********************************************************
     */

    @Override
    public String toString()
    {
        return "[collection type; class "+_class.getName()+", contains "+_elementType+"]";
    }
}
