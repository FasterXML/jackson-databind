package com.fasterxml.jackson.databind.jsontype.impl;

import java.util.Collection;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.cfg.MapperConfig;
import com.fasterxml.jackson.databind.introspect.AnnotatedClass;
import com.fasterxml.jackson.databind.introspect.AnnotatedClassResolver;
import com.fasterxml.jackson.databind.jsontype.*;
import com.fasterxml.jackson.databind.util.ClassUtil;

/**
 * Default {@link TypeResolverBuilder} implementation.
 */
public class StdTypeResolverBuilder
    implements TypeResolverBuilder<StdTypeResolverBuilder>
{
    // Configuration settings:

    protected JsonTypeInfo.Id _idType;

    protected JsonTypeInfo.As _includeAs;

    protected String _typeProperty;

    /**
     * Whether type id should be exposed to deserializers or not
     */
    protected boolean _typeIdVisible = false;
    
    /**
     * Default class to use in case type information is not available
     * or is broken.
     */
    protected Class<?> _defaultImpl;
    
    // Objects
    
    protected TypeIdResolver _customIdResolver;
    
    /*
    /**********************************************************
    /* Construction, initialization, actual building
    /**********************************************************
     */

    public StdTypeResolverBuilder() { }

    public StdTypeResolverBuilder(JsonTypeInfo.Value settings) {
        if (settings != null) {
            _idType = settings.getIdType();
            if (_idType == null) {
                throw new IllegalArgumentException("idType cannot be null");
            }
            _includeAs = settings.getInclusionType();
            _typeProperty = _propName(settings.getPropertyName(), _idType);
            _defaultImpl = settings.getDefaultImpl();
        }
    }

    /**
     * @since 2.9
     */
    public StdTypeResolverBuilder(JsonTypeInfo.Id idType,
            JsonTypeInfo.As idAs, String propName)
    {
        if (idType == null) {
            throw new IllegalArgumentException("idType cannot be null");
        }
        _idType = idType;
        _includeAs = idAs;
        _typeProperty = _propName(propName, _idType);
    }

    protected static String _propName(String propName, JsonTypeInfo.Id idType) {
        if (propName == null) {
            propName = idType.getDefaultPropertyName();
        }
        return propName;
    }

    public static StdTypeResolverBuilder noTypeInfoBuilder() {
        return new StdTypeResolverBuilder(JsonTypeInfo.Id.NONE, null, null);
    }

    @Override
    public StdTypeResolverBuilder init(JsonTypeInfo.Value settings, TypeIdResolver idRes)
    {
        _customIdResolver = idRes;

        if (settings != null) {
            _idType = settings.getIdType();
            if (_idType == null) {
                throw new IllegalArgumentException("idType cannot be null");
            }
            _includeAs = settings.getInclusionType();
    
            // Let's also initialize property name as per idType default
            _typeProperty = settings.getPropertyName();
            if (_typeProperty == null) {
                _typeProperty = _idType.getDefaultPropertyName();
            }
            _typeIdVisible = settings.getIdVisible();
            _defaultImpl = settings.getDefaultImpl();
        }
        return this;
    }

    @Override
    public TypeSerializer buildTypeSerializer(SerializationConfig config,
            JavaType baseType, Collection<NamedType> subtypes)
    {
        if (_idType == JsonTypeInfo.Id.NONE) { return null; }
        // 03-Oct-2016, tatu: As per [databind#1395] better prevent use for primitives,
        //    regardless of setting
        if (baseType.isPrimitive()) {
            return null;
        }
        TypeIdResolver idRes = idResolver(config, baseType, subtypes, true, false);
        switch (_includeAs) {
        case WRAPPER_ARRAY:
            return new AsArrayTypeSerializer(idRes, null);
        case PROPERTY:
            return new AsPropertyTypeSerializer(idRes, null, _typeProperty);
        case WRAPPER_OBJECT:
            return new AsWrapperTypeSerializer(idRes, null);
        case EXTERNAL_PROPERTY:
            return new AsExternalTypeSerializer(idRes, null, _typeProperty);
        case EXISTING_PROPERTY:
        	// as per [#528]
        	return new AsExistingPropertyTypeSerializer(idRes, null, _typeProperty);
        }
        throw new IllegalStateException("Do not know how to construct standard type serializer for inclusion type: "+_includeAs);
    }

    @Override
    public TypeDeserializer buildTypeDeserializer(DeserializationConfig config,
            JavaType baseType, Collection<NamedType> subtypes)
    {
        if (_idType == JsonTypeInfo.Id.NONE) { return null; }
        // 03-Oct-2016, tatu: As per [databind#1395] better prevent use for primitives,
        //    regardless of setting
        if (baseType.isPrimitive()) {
            return null;
        }

        TypeIdResolver idRes = idResolver(config, baseType, subtypes, false, true);
        JavaType defaultImpl;

        if (_defaultImpl == null) {
            defaultImpl = null;
        } else {
            // 20-Mar-2016, tatu: Can finally add a check for type compatibility BUT
            //   if so, need to add explicit checks for marker types. Not ideal, but
            //   seems like a reasonable compromise.
            // NOTE: `Void` actually means that for unknown type id we should get `null`
            //  value -- NOT that there is no default implementation.
            if (_defaultImpl == Void.class) {
                defaultImpl = config.getTypeFactory().constructType(_defaultImpl);
            } else if (!baseType.getRawClass().isAssignableFrom(_defaultImpl) && isBaseActuallySpecializedType(config, baseType)) {
                defaultImpl = null;
            } else {
                if (baseType.hasRawClass(_defaultImpl)) { // common enough to check
                    defaultImpl = baseType;
                } else if (baseType.isTypeOrSuperTypeOf(_defaultImpl)) {
                    // most common case with proper base type...
                    defaultImpl = config.getTypeFactory()
                            .constructSpecializedType(baseType, _defaultImpl);
                } else {
                    // 05-Apr-2018, tatu: [databind#1861] Not sure what would be the best way
                    //    to handle, but for 2.9, let's consider case of "sibling" defaultImpl...
                    // ... Ugh. Not declared to throw `JsonMappingException`, so...
                    throw new IllegalArgumentException(
                            String.format("Invalid \"defaultImpl\" (%s): not a subtype of basetype (%s)",
                                    ClassUtil.nameOf(_defaultImpl), ClassUtil.nameOf(baseType.getRawClass()))
                            );
                }
            }
        }

        // First, method for converting type info to type id:
        switch (_includeAs) {
        case WRAPPER_ARRAY:
            return new AsArrayTypeDeserializer(baseType, idRes,
                    _typeProperty, _typeIdVisible, defaultImpl);
        case PROPERTY:
        case EXISTING_PROPERTY: // as per [#528] same class as PROPERTY
            return new AsPropertyTypeDeserializer(baseType, idRes,
                    _typeProperty, _typeIdVisible, defaultImpl, _includeAs);
        case WRAPPER_OBJECT:
            return new AsWrapperTypeDeserializer(baseType, idRes,
                    _typeProperty, _typeIdVisible, defaultImpl);
        case EXTERNAL_PROPERTY:
            return new AsExternalTypeDeserializer(baseType, idRes,
                    _typeProperty, _typeIdVisible, defaultImpl);
        }
        throw new IllegalStateException("Do not know how to construct standard type serializer for inclusion type: "+_includeAs);
    }

    private boolean isBaseActuallySpecializedType(DeserializationConfig config, JavaType baseType) {
        // 12-Jun-2017, slobo: There is a chance that we're deserializing
        // a specific class part of a polymorphic hierarchy. In this case
        // baseType is actually a specific type, but the _defaultImpl comes
        // from the real base type. For example:
        //
        //     B @JsonTypeInfo(defaultImpl=S2)
        //  /--+--\
        //  S1    S2
        //  baseType = S1
        //  _defaultImpl = S2
        //
        // To detect this scenario we'll check whether _defaultImpl and
        // baseType share a common superclass or super superclass, etc.
        JavaType defaultType = config.getTypeFactory().constructType(_defaultImpl);
        for (JavaType current = defaultType; current != null; current = current.getSuperClass())
        {
            AnnotatedClass annotatedClass = AnnotatedClassResolver.resolve(config, current, config);
            if (annotatedClass.hasAnnotation(JsonTypeInfo.class)) {
                JsonTypeInfo typeInfo = annotatedClass.getAnnotation(JsonTypeInfo.class);
                if (typeInfo.defaultImpl() == null || !typeInfo.defaultImpl().equals(_defaultImpl)) {
                    break; // ignore any classes that have a different JsonTypeInfo annotation
                }
                if (annotatedClass.getRawType().isAssignableFrom(baseType.getRawClass())) {
                    return true;
                }
            }
            else {
                break; // ignore any classes above the JsonTypeInfo annotation
            }
        };
        return false;
    }

    /*
    /**********************************************************
    /* Construction, configuration
    /**********************************************************
     */

    @Override
    public StdTypeResolverBuilder defaultImpl(Class<?> defaultImpl) {
        _defaultImpl = defaultImpl;
        return this;
    }

    /*
    /**********************************************************
    /* Accessors
    /**********************************************************
     */

    @Override public Class<?> getDefaultImpl() { return _defaultImpl; }

    public String getTypeProperty() { return _typeProperty; }
    public boolean isTypeIdVisible() { return _typeIdVisible; }
    
    /*
    /**********************************************************
    /* Internal methods
    /**********************************************************
     */
    
    /**
     * Helper method that will either return configured custom
     * type id resolver, or construct a standard resolver
     * given configuration.
     */
    protected TypeIdResolver idResolver(MapperConfig<?> config,
            JavaType baseType, Collection<NamedType> subtypes, boolean forSer, boolean forDeser)
    {
        // Custom id resolver?
        if (_customIdResolver != null) { return _customIdResolver; }
        if (_idType == null) throw new IllegalStateException("Cannot build, 'init()' not yet called");
        switch (_idType) {
        case CLASS:
            return new ClassNameIdResolver(baseType, config.getTypeFactory());
        case MINIMAL_CLASS:
            return new MinimalClassNameIdResolver(baseType, config.getTypeFactory());
        case NAME:
            return TypeNameIdResolver.construct(config, baseType, subtypes, forSer, forDeser);
        case NONE: // hmmh. should never get this far with 'none'
            return null;
        case CUSTOM: // need custom resolver...
        }
        throw new IllegalStateException("Do not know how to construct standard type id resolver for idType: "+_idType);
    }
}
