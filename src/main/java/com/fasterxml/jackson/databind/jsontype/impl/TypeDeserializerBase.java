package com.fasterxml.jackson.databind.jsontype.impl;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.deser.std.NullifyingDeserializer;
import com.fasterxml.jackson.databind.jsontype.TypeDeserializer;
import com.fasterxml.jackson.databind.jsontype.TypeIdResolver;
import com.fasterxml.jackson.databind.util.ClassUtil;

/**
 * Base class for all standard Jackson {@link TypeDeserializer}s.
 */
public abstract class TypeDeserializerBase
    extends TypeDeserializer
    implements java.io.Serializable
{
    private static final long serialVersionUID = 1;
    
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
    protected final Map<String,JsonDeserializer<Object>> _deserializers;

    protected JsonDeserializer<Object> _defaultImplDeserializer;

    /*
    /**********************************************************
    /* Life-cycle
    /**********************************************************
     */

    /**
     * @since 2.8
     */
    protected TypeDeserializerBase(JavaType baseType, TypeIdResolver idRes,
            String typePropertyName, boolean typeIdVisible, JavaType defaultImpl)
    {
        _baseType = baseType;
        _idResolver = idRes;
        // 22-Dec-2015, tatu: as per [databind#1055], avoid NPE
        _typePropertyName = (typePropertyName == null) ? "" : typePropertyName;
        _typeIdVisible = typeIdVisible;
        // defaults are fine, although shouldn't need much concurrency
        _deserializers = new ConcurrentHashMap<String, JsonDeserializer<Object>>(16, 0.75f, 2);
        _defaultImpl = defaultImpl;
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
            String typeId) throws IOException
    {
        JsonDeserializer<Object> deser = _deserializers.get(typeId);
        if (deser == null) {
            /* As per [Databind#305], need to provide contextual info. But for
             * backwards compatibility, let's start by only supporting this
             * for base class, not via interface. Later on we can add this
             * to the interface, assuming deprecation at base class helps.
             */
            JavaType type = _idResolver.typeFromId(ctxt, typeId);
            if (type == null) {
                // As per [JACKSON-614], use the default impl if no type id available:
                deser = _findDefaultImplDeserializer(ctxt);
                if (deser == null) {
                    // 10-May-2016, tatu: We may get some help...
                    JavaType actual = _handleUnknownTypeId(ctxt, typeId, _idResolver, _baseType);
                    if (actual == null) { // what should this be taken to mean?
                        // TODO: try to figure out something better
                        return null;
                    }
                    // ... would this actually work?
                    deser = ctxt.findContextualValueDeserializer(actual, _property);
                }
            } else {
                /* 16-Dec-2010, tatu: Since nominal type we get here has no (generic) type parameters,
                 *   we actually now need to explicitly narrow from base type (which may have parameterization)
                 *   using raw type.
                 *
                 *   One complication, though; can not change 'type class' (simple type to container); otherwise
                 *   we may try to narrow a SimpleType (Object.class) into MapType (Map.class), losing actual
                 *   type in process (getting SimpleType of Map.class which will not work as expected)
                 */
                if ((_baseType != null)
                        && _baseType.getClass() == type.getClass()) {
                    /* 09-Aug-2015, tatu: Not sure if the second part of the check makes sense;
                     *   but it appears to check that JavaType impl class is the same which is
                     *   important for some reason?
                     *   Disabling the check will break 2 Enum-related tests.
                     */
                    // 19-Jun-2016, tatu: As per [databind#1270] we may actually get full
                    //   generic type with custom type resolvers. If so, should try to retain them.
                    //  Whether this is sufficient to avoid problems remains to be seen, but for
                    //  now it should improve things.
                    if (!type.hasGenericTypes()) {
                        type = ctxt.getTypeFactory().constructSpecializedType(_baseType, type.getRawClass());
                    }
                }
                deser = ctxt.findContextualValueDeserializer(type, _property);
            }
            _deserializers.put(typeId, deser);
        }
        return deser;
    }

    protected final JsonDeserializer<Object> _findDefaultImplDeserializer(DeserializationContext ctxt) throws IOException
    {
        /* 06-Feb-2013, tatu: As per [databind#148], consider default implementation value of
         *   {@link java.lang.Void} to mean "serialize as null"; as well as DeserializationFeature
         *   to do swift mapping to null
         */
        if (_defaultImpl == null) {
            if (!ctxt.isEnabled(DeserializationFeature.FAIL_ON_INVALID_SUBTYPE)) {
                return NullifyingDeserializer.instance;
            }
            return null;
        }
        Class<?> raw = _defaultImpl.getRawClass();
        if (ClassUtil.isBogusClass(raw)) {
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

    /**
     * Helper method called when {@link JsonParser} indicates that it can use
     * so-called native type ids. Assumption from there is that only native
     * type ids are to be used.
     * 
     * @since 2.3
     */
    @Deprecated
    protected Object _deserializeWithNativeTypeId(JsonParser jp, DeserializationContext ctxt) throws IOException {
        return _deserializeWithNativeTypeId(jp, ctxt, jp.getTypeId());
    }

    /**
     * Helper method called when {@link JsonParser} indicates that it can use
     * so-called native type ids, and such type id has been found.
     * 
     * @since 2.4
     */
    protected Object _deserializeWithNativeTypeId(JsonParser jp, DeserializationContext ctxt, Object typeId)
        throws IOException
    {
        JsonDeserializer<Object> deser;
        if (typeId == null) {
            /* 04-May-2014, tatu: Should error be obligatory, or should there be another method
             *   for "try to deserialize with native tpye id"?
             */
            deser = _findDefaultImplDeserializer(ctxt);
            if (deser == null) {
                ctxt.reportMappingException("No (native) type id found when one was expected for polymorphic type handling");
                return null;
            }
        } else {
            String typeIdStr = (typeId instanceof String) ? (String) typeId : String.valueOf(typeId);
            deser = _findDeserializer(ctxt, typeIdStr);
        }
        return deser.deserialize(jp, ctxt);
    }

    /**
     * Helper method called when given type id can not be resolved into 
     * concrete deserializer either directly (using given {@link  TypeIdResolver}),
     * or using default type.
     * Default implementation simply throws a {@link com.fasterxml.jackson.databind.JsonMappingException} to
     * indicate the problem; sub-classes may choose
     *
     * @return If it is possible to resolve type id into a {@link JsonDeserializer}
     *   should return that deserializer; otherwise throw an exception to indicate
     *   the problem.
     *
     * @since 2.8
     */
    protected JavaType _handleUnknownTypeId(DeserializationContext ctxt, String typeId,
            TypeIdResolver idResolver, JavaType baseType)
        throws IOException
    {
        String extraDesc = idResolver.getDescForKnownTypeIds();
        if (extraDesc == null) {
            extraDesc = "known type ids are not statically known";
        } else {
            extraDesc = "known type ids = " + extraDesc;
        }
        return ctxt.handleUnknownTypeId(_baseType, typeId, idResolver, extraDesc);
    }
}
