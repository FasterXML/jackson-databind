package com.fasterxml.jackson.databind.jsontype.impl;

import java.io.IOException;
import java.util.HashMap;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.core.*;

import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.annotation.NoClass;
import com.fasterxml.jackson.databind.deser.std.NullifyingDeserializer;
import com.fasterxml.jackson.databind.jsontype.TypeDeserializer;
import com.fasterxml.jackson.databind.jsontype.TypeIdResolver;

/**
 * Base class for all standard Jackson {@link TypeDeserializer}s.
 */
public abstract class TypeDeserializerBase
    extends TypeDeserializer
    implements java.io.Serializable
{
    private static final long serialVersionUID = 278445030337366675L;
    
    protected final TypeIdResolver _idResolver;
    
    protected final JavaType _baseType;

    /**
     * Property that contains value for which type information
     * is included; null if value is a root value.
     * Note that this value is not assigned during construction
     * but only when {@link #forProperty} is called to create
     * a copy.
     */
    protected final BeanProperty _property;

    /**
     * Type to use as the default implementation, if type id is
     * missing or can not be resolved.
     */
    protected final JavaType _defaultImpl;

    /**
     * Name of type property used; needed for non-property versions too,
     * in cases where type id is to be exposed as part of JSON.
     */
    protected final String _typePropertyName;
    
    protected final boolean _typeIdVisible;
    
    /**
     * For efficient operation we will lazily build mappings from type ids
     * to actual deserializers, once needed.
     */
    protected final HashMap<String,JsonDeserializer<Object>> _deserializers;

    protected JsonDeserializer<Object> _defaultImplDeserializer;

    /*
    /**********************************************************
    /* Life-cycle
    /**********************************************************
     */
    
    protected TypeDeserializerBase(JavaType baseType, TypeIdResolver idRes,
            String typePropertyName, boolean typeIdVisible, Class<?> defaultImpl)
    {
        _baseType = baseType;
        _idResolver = idRes;
        _typePropertyName = typePropertyName;
        _typeIdVisible = typeIdVisible;
        _deserializers = new HashMap<String,JsonDeserializer<Object>>();
        if (defaultImpl == null) {
            _defaultImpl = null;
        } else {
            /* 16-Oct-2011, tatu: should call this via TypeFactory; this is
             *    not entirely safe... however, since Collections/Maps are
             *    seldom (if ever) base types, may be ok.
             */
            _defaultImpl = baseType.forcedNarrowBy(defaultImpl);
        }

        _property = null;
    }

    protected TypeDeserializerBase(TypeDeserializerBase src, BeanProperty property)
    {
        _baseType = src._baseType;
        _idResolver = src._idResolver;
        _typePropertyName = src._typePropertyName;
        _typeIdVisible = src._typeIdVisible;
        _deserializers = src._deserializers;
        _defaultImpl = src._defaultImpl;
        _defaultImplDeserializer = src._defaultImplDeserializer;

        _property = property;
    }

    @Override
    public abstract TypeDeserializer forProperty(BeanProperty prop);
    
    /*
    /**********************************************************
    /* Accessors
    /**********************************************************
     */
    
    @Override
    public abstract JsonTypeInfo.As getTypeInclusion();

    public String baseTypeName() { return _baseType.getRawClass().getName(); }

    @Override
    public final String getPropertyName() { return _typePropertyName; }
    
    @Override    
    public TypeIdResolver getTypeIdResolver() { return _idResolver; }

    @Override    
    public Class<?> getDefaultImpl() {
        return (_defaultImpl == null) ? null : _defaultImpl.getRawClass();
    }
    
    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append('[').append(getClass().getName());
        sb.append("; base-type:").append(_baseType);
        sb.append("; id-resolver: ").append(_idResolver);
    	    sb.append(']');
    	    return sb.toString();
    }
    
    /*
    /**********************************************************
    /* Helper methods for sub-classes
    /**********************************************************
     */

    protected final JsonDeserializer<Object> _findDeserializer(DeserializationContext ctxt,
            String typeId)
        throws IOException, JsonProcessingException
    {
        JsonDeserializer<Object> deser;

        synchronized (_deserializers) {
            deser = _deserializers.get(typeId);
            if (deser == null) {
                JavaType type = _idResolver.typeFromId(typeId);
                if (type == null) {
                    // As per [JACKSON-614], use the default impl if no type id available:
                    if (_defaultImpl == null) {
                        throw ctxt.unknownTypeException(_baseType, typeId);
                    }
                    deser = _findDefaultImplDeserializer(ctxt);
                } else {
                    /* 16-Dec-2010, tatu: Since nominal type we get here has no (generic) type parameters,
                     *   we actually now need to explicitly narrow from base type (which may have parameterization)
                     *   using raw type.
                     *   
                     *   One complication, though; can not change 'type class' (simple type to container); otherwise
                     *   we may try to narrow a SimpleType (Object.class) into MapType (Map.class), losing actual
                     *   type in process (getting SimpleType of Map.class which will not work as expected)
                     */
                    if (_baseType != null && _baseType.getClass() == type.getClass()) {
                        type = _baseType.narrowBy(type.getRawClass());
                    }
                    deser = ctxt.findContextualValueDeserializer(type, _property);
                }
                _deserializers.put(typeId, deser);
            }
        }
        return deser;
    }

    protected final JsonDeserializer<Object> _findDefaultImplDeserializer(DeserializationContext ctxt)
        throws IOException, JsonProcessingException
    {
        /* 06-Feb-2013, tatu: As per [Issue#148], consider default implementation value of
         *   {@link NoClass} to mean "serialize as null"; as well as DeserializationFeature
         *   to do swift mapping to null
         */
        if (_defaultImpl == null) {
            if (!ctxt.isEnabled(DeserializationFeature.FAIL_ON_INVALID_SUBTYPE)) {
                return NullifyingDeserializer.instance;
            }
            return null;
        }
        if (_defaultImpl.getRawClass() == NoClass.class) {
            return NullifyingDeserializer.instance;
        }
        
        synchronized (_defaultImpl) {
            if (_defaultImplDeserializer == null) {
                _defaultImplDeserializer = ctxt.findContextualValueDeserializer(
                        _defaultImpl, _property);
            }
            return _defaultImplDeserializer;
        }
    }
}
