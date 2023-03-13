package tools.jackson.databind.jsontype.impl;

import java.util.Collection;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

import tools.jackson.databind.*;
import tools.jackson.databind.cfg.MapperConfig;
import tools.jackson.databind.introspect.AnnotatedClass;
import tools.jackson.databind.introspect.AnnotatedClassResolver;
import tools.jackson.databind.jsontype.*;
import tools.jackson.databind.util.ClassUtil;

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
    /**********************************************************************
    /* Construction, initialization, actual building
    /**********************************************************************
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
    }

    public static StdTypeResolverBuilder noTypeInfoBuilder() {
        return new StdTypeResolverBuilder(JsonTypeInfo.Id.NONE, null, null);
    }

    @Override
    public StdTypeResolverBuilder init(JsonTypeInfo.Value settings,
            TypeIdResolver idRes)
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
    public TypeSerializer buildTypeSerializer(SerializerProvider ctxt,
            JavaType baseType, Collection<NamedType> subtypes)
    {
        if (_idType == JsonTypeInfo.Id.NONE) { return null; }
        // 03-Oct-2016, tatu: As per [databind#1395] better prevent use for primitives,
        //    regardless of setting
        if (baseType.isPrimitive()) {
            // 19-Jun-2020, tatu: But for [databind#2753], allow overriding
            if (!allowPrimitiveTypes(ctxt, baseType)) {
                return null;
            }
        }
        if (_idType == JsonTypeInfo.Id.DEDUCTION) {
            // Deduction doesn't require a type property. We use EXISTING_PROPERTY with a name of <null> to drive this.
            // 04-Jan-2023, tatu: Actually as per [databind#3711] that won't quite work so:
            return AsDeductionTypeSerializer.instance();
        }

        TypeIdResolver idRes = idResolver(ctxt, baseType, subTypeValidator(ctxt),
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

    @Override
    public TypeDeserializer buildTypeDeserializer(DeserializationContext ctxt,
            JavaType baseType, Collection<NamedType> subtypes)
    {
        if (_idType == JsonTypeInfo.Id.NONE) { return null; }
        // 03-Oct-2016, tatu: As per [databind#1395] better prevent use for primitives,
        //    regardless of setting
        if (baseType.isPrimitive()) {
            // 19-Jun-2020, tatu: But for [databind#2753], allow overriding
            if (!allowPrimitiveTypes(ctxt, baseType)) {
                return null;
            }
        }

        // 27-Apr-2019, tatu: Part of [databind#2195]; must first check whether any subtypes
        //    of basetypes might be denied or allowed
        final PolymorphicTypeValidator subTypeValidator = verifyBaseTypeValidity(ctxt, baseType);

        TypeIdResolver idRes = idResolver(ctxt, baseType, subTypeValidator, subtypes, false, true);
        JavaType defaultImpl = defineDefaultImpl(ctxt, baseType);

        if(_idType == JsonTypeInfo.Id.DEDUCTION) {
            // Deduction doesn't require an includeAs property
            return new AsDeductionTypeDeserializer(ctxt, baseType, idRes, defaultImpl, subtypes);
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
                        _hasTypeResolver(ctxt, baseType));
        case WRAPPER_OBJECT:
            return new AsWrapperTypeDeserializer(baseType, idRes,
                    _typeProperty, _typeIdVisible, defaultImpl);
        case EXTERNAL_PROPERTY:
            return new AsExternalTypeDeserializer(baseType, idRes,
                    _typeProperty, _typeIdVisible, defaultImpl);
        }
        throw new IllegalStateException("Do not know how to construct standard type serializer for inclusion type: "+_includeAs);
    }

    protected JavaType defineDefaultImpl(DatabindContext ctxt, JavaType baseType)
    {
        if (_defaultImpl != null) {
            // 20-Mar-2016, tatu: It is important to do specialization go through
            //   TypeFactory to ensure proper resolution; with 2.7 and before, direct
            //   call to JavaType was used, but that cannot work reliably with 2.7
            // NOTE: `Void` actually means that for unknown type id we should get `null`
            //  value -- NOT that there is no default implementation.
            if (_defaultImpl == Void.class) {
                // 18-Sep-2021, tatu: This has specific meaning: these two markers will
                //    be used to conjure `null` value out of invalid type ids
                return ctxt.getTypeFactory().constructType(_defaultImpl);
            }
            if (baseType.hasRawClass(_defaultImpl)) { // tiny optimization
                return baseType;
            }
            if (baseType.isTypeOrSuperTypeOf(_defaultImpl)) {
                // most common case with proper base type...
                return ctxt.getTypeFactory()
                        .constructSpecializedType(baseType, _defaultImpl);
            }
            if (baseType.hasRawClass(_defaultImpl)) {
                return baseType;
            }
        }
        // use base type as default should always be used as the last choice.
        if (ctxt.isEnabled(MapperFeature.USE_BASE_TYPE_AS_DEFAULT_IMPL)
               && !baseType.isAbstract()) {
            // still can not resolve by default impl, fall back to use base type as default impl
            return baseType;
        }
        return null;
    }

    /*
    /**********************************************************************
    /* Construction, configuration
    /**********************************************************************
     */

    @Override
    public StdTypeResolverBuilder withDefaultImpl(Class<?> defaultImpl) {
        if (_defaultImpl == defaultImpl) {
            return this;
        }
        ClassUtil.verifyMustOverride(StdTypeResolverBuilder.class, this, "withDefaultImpl");

        // NOTE: MUST create new instance, NOT modify this instance
        return new StdTypeResolverBuilder(this, defaultImpl);
    }

    /*
    /**********************************************************************
    /* Accessors
    /**********************************************************************
     */

    @Override public Class<?> getDefaultImpl() { return _defaultImpl; }

    public String getTypeProperty() { return _typeProperty; }
    public boolean isTypeIdVisible() { return _typeIdVisible; }

    /*
    /**********************************************************************
    /* Internal/subtype factory methods
    /**********************************************************************
     */

    /**
     * Helper method that will either return configured custom
     * type id resolver, or construct a standard resolver
     * given configuration.
     */
    protected TypeIdResolver idResolver(DatabindContext ctxt,
            JavaType baseType, PolymorphicTypeValidator subtypeValidator,
            Collection<NamedType> subtypes, boolean forSer, boolean forDeser)
    {
        // Custom id resolver?
        if (_customIdResolver != null) { return _customIdResolver; }
        if (_idType == null) throw new IllegalStateException("Cannot build, 'init()' not yet called");
        switch (_idType) {
        case DEDUCTION: // Deduction produces class names to be resolved
        case CLASS:
            return ClassNameIdResolver.construct(baseType, subtypeValidator);
        case MINIMAL_CLASS:
            return MinimalClassNameIdResolver.construct(baseType, subtypeValidator);
        case NAME:
            return TypeNameIdResolver.construct(ctxt.getConfig(), baseType, subtypes, forSer, forDeser);
        case NONE: // hmmh. should never get this far with 'none'
            return null;
        case CUSTOM: // need custom resolver...
        }
        throw new IllegalStateException("Do not know how to construct standard type id resolver for idType: "+_idType);
    }

    /*
    /**********************************************************************
    /* Internal/subtype factory methods
    /**********************************************************************
     */

    /**
     * Overridable helper method for determining actual validator to use when constructing
     * type serializers and type deserializers.
     *<p>
     * Default implementation simply uses one configured and accessible using
     * {@link MapperConfig#getPolymorphicTypeValidator()}.
     */
    public PolymorphicTypeValidator subTypeValidator(DatabindContext ctxt) {
        return ctxt.getConfig().getPolymorphicTypeValidator();
    }

    /**
     * Helper method called to check that base type is valid regarding possible constraints
     * on basetype/subtype combinations allowed for polymorphic type handling.
     * Currently limits are verified for class name - based methods only.
     */
    protected PolymorphicTypeValidator verifyBaseTypeValidity(DatabindContext ctxt,
            JavaType baseType)
    {
        final PolymorphicTypeValidator ptv = subTypeValidator(ctxt);
        if (_idType == JsonTypeInfo.Id.CLASS || _idType == JsonTypeInfo.Id.MINIMAL_CLASS) {
            final PolymorphicTypeValidator.Validity validity = ptv.validateBaseType(ctxt, baseType);
            // If no subtypes are legal (that is, base type itself is invalid), indicate problem
            if (validity == PolymorphicTypeValidator.Validity.DENIED) {
                return reportInvalidBaseType(ctxt, baseType, ptv);
            }
            // If there's indication that any and all subtypes are fine, replace validator itself:
            if (validity == PolymorphicTypeValidator.Validity.ALLOWED) {
                return LaissezFaireSubTypeValidator.instance;
            }
            // otherwise just return validator, is to be called for each distinct type
        }
        return ptv;
    }

    protected PolymorphicTypeValidator reportInvalidBaseType(DatabindContext ctxt,
            JavaType baseType, PolymorphicTypeValidator ptv)
    {
        return ctxt.reportBadDefinition(baseType, String.format(
"Configured `PolymorphicTypeValidator` (of type %s) denied resolution of all subtypes of base type %s",
                        ClassUtil.classNameOf(ptv), ClassUtil.classNameOf(baseType.getRawClass()))
                );
    }

    /*
    /**********************************************************************
    /* Overridable helper methods
    /**********************************************************************
     */

    /**
     * Overridable helper method that is called to determine whether type serializers
     * and type deserializers may be created even if base type is Java {@code primitive}
     * type.
     * Default implementation simply returns {@code false} (since primitive types can not
     * be sub-classed, are never polymorphic) but custom implementations
     * may change the logic for some special cases.
     *
     * @param ctxt Currently active context
     * @param baseType Primitive base type for property being handled
     *
     * @return True if type (de)serializer may be created even if base type is Java
     *    {@code primitive} type; false if not
     */
    protected boolean allowPrimitiveTypes(DatabindContext ctxt,
            JavaType baseType) {
        return false;
    }


    /**
     * Checks whether the given class has annotations indicating some type resolver
     * is applied, for example {@link com.fasterxml.jackson.annotation.JsonSubTypes}.
     * Only initializes {@link #_hasTypeResolver} once if its value is null.
     *
     * @param ctxt Currently active context
     * @param baseType the base type to check for type resolver annotations
     *
     * @return true if the class has type resolver annotations, false otherwise
     */
    protected boolean _hasTypeResolver(DatabindContext ctxt, JavaType baseType) {
        AnnotatedClass ac = 
                AnnotatedClassResolver.resolveWithoutSuperTypes(ctxt.getConfig(),
                        baseType.getRawClass());
        AnnotationIntrospector ai = ctxt.getAnnotationIntrospector();
        return ai.findPolymorphicTypeInfo(ctxt.getConfig(), ac) != null;
    }
}
