package com.fasterxml.jackson.databind.jsontype.impl;

import java.util.Collection;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.annotation.NoClass;
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
     * 
     * Boolean value configured through {@link JsonTypeInfo#requireTypeIdForSubtypes}.
     * If this value is not {@code null}, this value should override the global configuration of
     * {@link com.fasterxml.jackson.databind.MapperFeature#REQUIRE_TYPE_ID_FOR_SUBTYPES}. 
     * 
     * @since 2.16 (backported from Jackson 3.0)
     */
    protected Boolean _requireTypeIdForSubtypes;

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
        _typeProperty = _propName(propName, idType);
    }

    /**
     * Copy-constructor
     *
     * @since 2.13
     */
    protected StdTypeResolverBuilder(StdTypeResolverBuilder base,
            Class<?> defaultImpl)
    {
        _idType = base._idType;
        _includeAs = base._includeAs;
        _typeProperty = base._typeProperty;
        _typeIdVisible = base._typeIdVisible;
        _customIdResolver = base._customIdResolver;

        _defaultImpl = defaultImpl;
        _requireTypeIdForSubtypes = base._requireTypeIdForSubtypes;
    }

    /**
     * @since 2.16
     */
    public StdTypeResolverBuilder(JsonTypeInfo.Value settings) {
        if (settings != null) {
            withSettings(settings);
        }
    }

    public static StdTypeResolverBuilder noTypeInfoBuilder() {
        JsonTypeInfo.Value typeInfo = JsonTypeInfo.Value.construct(JsonTypeInfo.Id.NONE, null,
                null, null, false, null);
        return new StdTypeResolverBuilder().withSettings(typeInfo);
    }

    @Override
    public TypeSerializer buildTypeSerializer(SerializationConfig config,
            JavaType baseType, Collection<NamedType> subtypes)
    {
        if (_idType == JsonTypeInfo.Id.NONE) { return null; }
        // 03-Oct-2016, tatu: As per [databind#1395] better prevent use for primitives,
        //    regardless of setting
        if (baseType.isPrimitive()) {
            // 19-Jun-2020, tatu: But for [databind#2753], allow overriding
            if (!allowPrimitiveTypes(config, baseType)) {
                return null;
            }
        }
        if(_idType == JsonTypeInfo.Id.DEDUCTION) {
            // Deduction doesn't require a type property. We use EXISTING_PROPERTY with a name of <null> to drive this.
            // 04-Jan-2023, tatu: Actually as per [databind#3711] that won't quite work so:
            return AsDeductionTypeSerializer.instance();
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
            // 19-Jun-2020, tatu: But for [databind#2753], allow overriding
            if (!allowPrimitiveTypes(config, baseType)) {
                return null;
            }
        }

        // 27-Apr-2019, tatu: Part of [databind#2195]; must first check whether any subtypes
        //    of basetypes might be denied or allowed
        final PolymorphicTypeValidator subTypeValidator = verifyBaseTypeValidity(config, baseType);

        TypeIdResolver idRes = idResolver(config, baseType, subTypeValidator, subtypes, false, true);

        JavaType defaultImpl = defineDefaultImpl(config, baseType);

        if(_idType == JsonTypeInfo.Id.DEDUCTION) {
            // Deduction doesn't require an includeAs property
            return new AsDeductionTypeDeserializer(baseType, idRes, defaultImpl, config, subtypes);
        }

        // First, method for converting type info to type id:
        switch (_includeAs) {
        case WRAPPER_ARRAY:
            return new AsArrayTypeDeserializer(baseType, idRes,
                    _typeProperty, _typeIdVisible, defaultImpl);
        case PROPERTY:
        case EXISTING_PROPERTY: // as per [#528] same class as PROPERTY
            return new AsPropertyTypeDeserializer(baseType, idRes,
                    _typeProperty, _typeIdVisible, defaultImpl, _includeAs,
                    _strictTypeIdHandling(config, baseType));
        case WRAPPER_OBJECT:
            return new AsWrapperTypeDeserializer(baseType, idRes,
                    _typeProperty, _typeIdVisible, defaultImpl);
        case EXTERNAL_PROPERTY:
            return new AsExternalTypeDeserializer(baseType, idRes,
                    _typeProperty, _typeIdVisible, defaultImpl);
        }
        throw new IllegalStateException("Do not know how to construct standard type serializer for inclusion type: "+_includeAs);
    }

    protected JavaType defineDefaultImpl(DeserializationConfig config, JavaType baseType)
    {
        if (_defaultImpl != null) {
            // 20-Mar-2016, tatu: It is important to do specialization go through
            //   TypeFactory to ensure proper resolution; with 2.7 and before, direct
            //   call to JavaType was used, but that cannot work reliably with 2.7
            // 20-Mar-2016, tatu: Can finally add a check for type compatibility BUT
            //   if so, need to add explicit checks for marker types. Not ideal, but
            //   seems like a reasonable compromise.
            if ((_defaultImpl == Void.class) || (_defaultImpl == NoClass.class)) {
                // 18-Sep-2021, tatu: This has specific meaning: these two markers will
                //    be used to conjure `null` value out of invalid type ids
                return config.getTypeFactory().constructType(_defaultImpl);
            }
            if (baseType.hasRawClass(_defaultImpl)) { // tiny optimization
                return baseType;
            }
            if (baseType.isTypeOrSuperTypeOf(_defaultImpl)) {
                // most common case with proper base type...
                return config.getTypeFactory()
                        .constructSpecializedType(baseType, _defaultImpl);
            }
            if (baseType.hasRawClass(_defaultImpl)) {
                return baseType;
            }
        }
        // use base type as default should always be used as the last choice.
        if (config.isEnabled(MapperFeature.USE_BASE_TYPE_AS_DEFAULT_IMPL)
               && !baseType.isAbstract()) {
            // still can not resolve by default impl, fall back to use base type as default impl
            return baseType;
        }
        return null;
    }

    /*
    /**********************************************************
    /* Construction, configuration
    /**********************************************************
     */

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
        _typeProperty = _propName(null, idType);
        return this;
    }

    @Override
    public StdTypeResolverBuilder init(JsonTypeInfo.Value settings,
            TypeIdResolver idRes)
    {
        _customIdResolver = idRes;

        if (settings != null) {
            withSettings(settings);
        }
        return this;
    }

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
        _typeProperty = _propName(typeIdPropName, _idType);
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

    @Override
    public StdTypeResolverBuilder withDefaultImpl(Class<?> defaultImpl) {
        if (_defaultImpl == defaultImpl) {
            return this;
        }
        ClassUtil.verifyMustOverride(StdTypeResolverBuilder.class, this, "withDefaultImpl");

        // NOTE: MUST create new instance, NOT modify this instance
        return new StdTypeResolverBuilder(this, defaultImpl);
    }

    @Override
    public StdTypeResolverBuilder withSettings(JsonTypeInfo.Value settings) {
        _idType = Objects.requireNonNull(settings.getIdType());
        _includeAs = settings.getInclusionType();
        _typeProperty = _propName(settings.getPropertyName(), _idType);
        _defaultImpl = settings.getDefaultImpl();
        _typeIdVisible = settings.getIdVisible();
        _requireTypeIdForSubtypes = settings.getRequireTypeIdForSubtypes();
        return this;
    }

    /**
     * @since 2.16; non-static since 2.17
     */
    protected String _propName(String propName, JsonTypeInfo.Id idType) {
        if (propName == null || propName.isEmpty()) {
            propName = idType.getDefaultPropertyName();
        }
        return propName;
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
        case DEDUCTION: // Deduction produces class names to be resolved
        case CLASS:
            return ClassNameIdResolver.construct(baseType, config, subtypeValidator);
        case MINIMAL_CLASS:
            return MinimalClassNameIdResolver.construct(baseType, config, subtypeValidator);
        case SIMPLE_NAME:
            return SimpleNameIdResolver.construct(config, baseType, subtypes, forSer, forDeser);
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
            final PolymorphicTypeValidator.Validity validity = ptv.validateBaseType(config, baseType);
            // If no subtypes are legal (that is, base type itself is invalid), indicate problem
            if (validity == PolymorphicTypeValidator.Validity.DENIED) {
                return reportInvalidBaseType(config, baseType, ptv);
            }
            // If there's indication that any and all subtypes are fine, replace validator itself:
            if (validity == PolymorphicTypeValidator.Validity.ALLOWED) {
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

    /*
    /**********************************************************
    /* Overridable helper methods
    /**********************************************************
     */

    /**
     * Overridable helper method that is called to determine whether type serializers
     * and type deserializers may be created even if base type is Java {@code primitive}
     * type.
     * Default implementation simply returns {@code false} (since primitive types can not
     * be sub-classed, are never polymorphic) but custom implementations
     * may change the logic for some special cases.
     *
     * @param config Currently active configuration
     * @param baseType Primitive base type for property being handled
     *
     * @return True if type (de)serializer may be created even if base type is Java
     *    {@code primitive} type; false if not
     *
     * @since 2.11.1
     */
    protected boolean allowPrimitiveTypes(MapperConfig<?> config,
            JavaType baseType) {
        return false;
    }

    /**
     * Determines whether strict type ID handling should be used for this type or not.
     * This will be enabld as configured by {@link JsonTypeInfo#requireTypeIdForSubtypes()}
     * unless its value is {@link com.fasterxml.jackson.annotation.OptBoolean#DEFAULT}. 
     * In case the value of {@link JsonTypeInfo#requireTypeIdForSubtypes()} is {@code OptBoolean.DEFAULT},
     * this will be enabled when either the type has type resolver annotations or if
     * {@link com.fasterxml.jackson.databind.MapperFeature#REQUIRE_TYPE_ID_FOR_SUBTYPES}
     * is enabled.
     *
     * @param config the deserialization configuration to use
     * @param baseType the base type to check for type resolver annotations
     *
     * @return {@code true} if the class has type resolver annotations, or the strict
     * handling feature is enabled, {@code false} otherwise.
     *
     * @since 2.15
     */
    protected boolean _strictTypeIdHandling(DeserializationConfig config, JavaType baseType) {
        // [databind#3877]: per-type strict type handling, since 2.16
        if (_requireTypeIdForSubtypes != null && baseType.isConcrete()) {
            return _requireTypeIdForSubtypes;
        }
        if (config.isEnabled(MapperFeature.REQUIRE_TYPE_ID_FOR_SUBTYPES)) {
            return true;
        }
        // Otherwise we will be strict if there's a type resolver: presumably
        // target type is a (likely abstract) base type and cannot be used as target
        return _hasTypeResolver(config, baseType);
    }

    /**
     * Checks whether the given class has annotations indicating some type resolver
     * is applied, for example {@link com.fasterxml.jackson.annotation.JsonTypeInfo}.
     * Only initializes {@link #_hasTypeResolver} once if its value is null.
     *
     * @param config the deserialization configuration to use
     * @param baseType the base type to check for type resolver annotations
     *
     * @return true if the class has type resolver annotations, false otherwise
     *
     * @since 2.15, using {@code ai.findPolymorphicTypeInfo(config, ac)} since 2.16.
     */
    protected boolean _hasTypeResolver(DeserializationConfig config, JavaType baseType) {
        AnnotatedClass ac = AnnotatedClassResolver.resolveWithoutSuperTypes(config,  baseType.getRawClass());
        AnnotationIntrospector ai = config.getAnnotationIntrospector();
        return ai.findPolymorphicTypeInfo(config, ac) != null;
    }
}
