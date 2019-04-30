package com.fasterxml.jackson.databind.jsontype.impl;

import java.util.Collection;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.annotation.NoClass;
import com.fasterxml.jackson.databind.cfg.MapperConfig;
import com.fasterxml.jackson.databind.jsontype.*;
import com.fasterxml.jackson.databind.jsontype.PolymorphicTypeValidator.Validity;
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

    /**
     * @since 2.9
     */
    protected StdTypeResolverBuilder(JsonTypeInfo.Id idType,
            JsonTypeInfo.As idAs, String propName) {
        _idType = idType;
        _includeAs = idAs;
        _typeProperty = propName;
    }

    public static StdTypeResolverBuilder noTypeInfoBuilder() {
        return new StdTypeResolverBuilder().init(JsonTypeInfo.Id.NONE, null);
    }

    @Override
    public StdTypeResolverBuilder init(JsonTypeInfo.Id idType, TypeIdResolver idRes)
    {
        // sanity checks
        if (idType == null) {
            throw new IllegalArgumentException("idType cannot be null");
        }
        _idType = idType;
        _customIdResolver = idRes;
        // Let's also initialize property name as per idType default
        _typeProperty = idType.getDefaultPropertyName();
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
        TypeIdResolver idRes = idResolver(config, baseType, subTypeValidator(config),
                subtypes, true, false);
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

    // as per [#368]
    // removed when fix [#528]
    //private IllegalArgumentException _noExisting() {
    //    return new IllegalArgumentException("Inclusion type "+_includeAs+" not yet supported");
    //}

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

        // 27-Apr-2019, tatu: Part of [databind#2195]; must first check whether any subtypes
        //    of basetypes might be denied or allowed
        final PolymorphicTypeValidator subTypeValidator = verifyBaseTypeValidity(config, baseType);

        TypeIdResolver idRes = idResolver(config, baseType, subTypeValidator, subtypes, false, true);

        JavaType defaultImpl = defineDefaultImpl(config, baseType);

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

    protected JavaType defineDefaultImpl(DeserializationConfig config, JavaType baseType) {
        JavaType defaultImpl;
        if (_defaultImpl == null) {
            //Fis of issue #955
            if (config.isEnabled(MapperFeature.USE_BASE_TYPE_AS_DEFAULT_IMPL) && !baseType.isAbstract()) {
                defaultImpl = baseType;
            } else {
                defaultImpl = null;
            }
        } else {
            // 20-Mar-2016, tatu: It is important to do specialization go through
            //   TypeFactory to ensure proper resolution; with 2.7 and before, direct
            //   call to JavaType was used, but that cannot work reliably with 2.7
            // 20-Mar-2016, tatu: Can finally add a check for type compatibility BUT
            //   if so, need to add explicit checks for marker types. Not ideal, but
            //   seems like a reasonable compromise.
            if ((_defaultImpl == Void.class)
                    || (_defaultImpl == NoClass.class)) {
                defaultImpl = config.getTypeFactory().constructType(_defaultImpl);
            } else {
                if (baseType.hasRawClass(_defaultImpl)) { // common enough to check
                    defaultImpl = baseType;
                } else if (baseType.isTypeOrSuperTypeOf(_defaultImpl)) {
                    // most common case with proper base type...
                    defaultImpl = config.getTypeFactory()
                            .constructSpecializedType(baseType, _defaultImpl);
                } else {
                    // 05-Apr-2018, tatu: As [databind#1565] and [databind#1861] need to allow
                    //    some cases of seemingly incompatible `defaultImpl`. Easiest to just clear
                    //    the setting.

                    /*
                    throw new IllegalArgumentException(
                            String.format("Invalid \"defaultImpl\" (%s): not a subtype of basetype (%s)",
                                    ClassUtil.nameOf(_defaultImpl), ClassUtil.nameOf(baseType.getRawClass()))
                            );
                            */
                    defaultImpl = null;
                }
            }
        }
        return defaultImpl;
    }

    /*
    /**********************************************************
    /* Construction, configuration
    /**********************************************************
     */

    @Override
    public StdTypeResolverBuilder inclusion(JsonTypeInfo.As includeAs) {
        if (includeAs == null) {
            throw new IllegalArgumentException("includeAs cannot be null");
        }
        _includeAs = includeAs;
        return this;
    }

    /**
     * Method for constructing an instance with specified type property name
     * (property name to use for type id when using "as-property" inclusion).
     */
    @Override
    public StdTypeResolverBuilder typeProperty(String typeIdPropName) {
        // ok to have null/empty; will restore to use defaults
        if (typeIdPropName == null || typeIdPropName.length() == 0) {
            typeIdPropName = _idType.getDefaultPropertyName();
        }
        _typeProperty = typeIdPropName;
        return this;
    }

    @Override
    public StdTypeResolverBuilder defaultImpl(Class<?> defaultImpl) {
        _defaultImpl = defaultImpl;
        return this;
    }

    @Override
    public StdTypeResolverBuilder typeIdVisibility(boolean isVisible) {
        _typeIdVisible = isVisible;
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
    /* Internal/subtype factory methods
    /**********************************************************
     */

    /**
     * Helper method that will either return configured custom
     * type id resolver, or construct a standard resolver
     * given configuration.
     */
    protected TypeIdResolver idResolver(MapperConfig<?> config,
            JavaType baseType, PolymorphicTypeValidator subtypeValidator,
            Collection<NamedType> subtypes, boolean forSer, boolean forDeser)
    {
        // Custom id resolver?
        if (_customIdResolver != null) { return _customIdResolver; }
        if (_idType == null) throw new IllegalStateException("Cannot build, 'init()' not yet called");
        switch (_idType) {
        case CLASS:
            return ClassNameIdResolver.construct(baseType, config, subtypeValidator);
        case MINIMAL_CLASS:
            return MinimalClassNameIdResolver.construct(baseType, config, subtypeValidator);
        case NAME:
            return TypeNameIdResolver.construct(config, baseType, subtypes, forSer, forDeser);
        case NONE: // hmmh. should never get this far with 'none'
            return null;
        case CUSTOM: // need custom resolver...
        }
        throw new IllegalStateException("Do not know how to construct standard type id resolver for idType: "+_idType);
    }

    /*
    /**********************************************************
    /* Internal/subtype factory methods
    /**********************************************************
     */

    /**
     * Overridable helper method for determining actual validator to use when constructing
     * type serializers and type deserializers.
     *<p>
     * Default implementation simply uses one configured and accessible using
     * {@link MapperConfig#getPolymorphicTypeValidator()}.
     *
     * @since 2.10
     */
    public PolymorphicTypeValidator subTypeValidator(MapperConfig<?> config) {
        return config.getPolymorphicTypeValidator();
    }
    
    /**
     * Helper method called to check that base type is valid regarding possible constraints
     * on basetype/subtype combinations allowed for polymorphic type handling.
     * Currently limits are verified for class name - based methods only.
     *
     * @since 2.10
     */
    protected PolymorphicTypeValidator verifyBaseTypeValidity(MapperConfig<?> config,
            JavaType baseType)
    {
        final PolymorphicTypeValidator ptv = subTypeValidator(config);
        if (_idType == JsonTypeInfo.Id.CLASS || _idType == JsonTypeInfo.Id.MINIMAL_CLASS) {
            final Validity validity = ptv.validateBaseType(config, baseType);
            // If no subtypes are legal (that is, base type itself is invalid), indicate problem
            if (validity == Validity.DENIED) {
                return reportInvalidBaseType(config, baseType, ptv);
            }
            // If there's indication that any and all subtypes are fine, replace validator itself:
            if (validity == Validity.ALLOWED) {
                return LaissezFaireSubTypeValidator.instance;
            }
            // otherwise just return validator, is to be called for each distinct type
        }
        return ptv;
    }

    /**
     * @since 2.10
     */
    protected PolymorphicTypeValidator reportInvalidBaseType(MapperConfig<?> config,
            JavaType baseType, PolymorphicTypeValidator ptv)
    {
        throw new IllegalArgumentException(String.format(
"Configured `PolymorphicTypeValidator` (of type %s) denied resolution of all subtypes of base type %s",
                        ClassUtil.classNameOf(ptv), ClassUtil.classNameOf(baseType.getRawClass()))
        );
    }
}
