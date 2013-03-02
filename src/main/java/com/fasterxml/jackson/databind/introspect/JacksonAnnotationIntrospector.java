package com.fasterxml.jackson.databind.introspect;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.*;

import com.fasterxml.jackson.core.Version;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.annotation.*;
import com.fasterxml.jackson.databind.cfg.MapperConfig;
import com.fasterxml.jackson.databind.jsontype.NamedType;
import com.fasterxml.jackson.databind.jsontype.TypeIdResolver;
import com.fasterxml.jackson.databind.jsontype.TypeResolverBuilder;
import com.fasterxml.jackson.databind.jsontype.impl.StdTypeResolverBuilder;
import com.fasterxml.jackson.databind.ser.std.RawSerializer;
import com.fasterxml.jackson.databind.util.Converter;
import com.fasterxml.jackson.databind.util.NameTransformer;

/**
 * {@link AnnotationIntrospector} implementation that handles standard
 * Jackson annotations.
 */
public class JacksonAnnotationIntrospector
    extends AnnotationIntrospector
    implements java.io.Serializable
{
    private static final long serialVersionUID = 1L;

    public JacksonAnnotationIntrospector() { }

    @Override
    public Version version() {
        return com.fasterxml.jackson.databind.cfg.PackageVersion.VERSION;
    }
    
    /*
    /**********************************************************
    /* General annotation properties
    /**********************************************************
     */

    // TODO: remove in 2.2
    @Override
    @Deprecated
    public boolean isHandled(Annotation ann)
    {
        Class<? extends Annotation> acls = ann.annotationType();
        return acls.getAnnotation(JacksonAnnotation.class) != null;
    }

    /**
     * Annotations with meta-annotation {@link JacksonAnnotationsInside}
     * are considered bundles.
     */
    @Override
    public boolean isAnnotationBundle(Annotation ann)
    {
        return ann.annotationType().getAnnotation(JacksonAnnotationsInside.class) != null;
    }
    
    /*
    /**********************************************************
    /* General annotations
    /**********************************************************
     */

    // default impl is fine:
    //public String findEnumValue(Enum<?> value) { return value.name(); }
    
    /*
    /**********************************************************
    /* General class annotations
    /**********************************************************
     */

    @Override
    public PropertyName findRootName(AnnotatedClass ac)
    {
        JsonRootName ann = ac.getAnnotation(JsonRootName.class);
        if (ann == null) {
            return null;
        }
        return new PropertyName(ann.value());
    }

    @Override
    public String[] findPropertiesToIgnore(Annotated ac) {
        JsonIgnoreProperties ignore = ac.getAnnotation(JsonIgnoreProperties.class);
        return (ignore == null) ? null : ignore.value();
    }

    @Override
    public Boolean findIgnoreUnknownProperties(AnnotatedClass ac) {
        JsonIgnoreProperties ignore = ac.getAnnotation(JsonIgnoreProperties.class);
        return (ignore == null) ? null : ignore.ignoreUnknown();
    }

    @Override
    public Boolean isIgnorableType(AnnotatedClass ac) {
        JsonIgnoreType ignore = ac.getAnnotation(JsonIgnoreType.class);
        return (ignore == null) ? null : ignore.value();
    }

    @Override
    public Object findFilterId(AnnotatedClass ac)
    {
        JsonFilter ann = ac.getAnnotation(JsonFilter.class);
        if (ann != null) {
            String id = ann.value();
            // Empty String is same as not having annotation, to allow overrides
            if (id.length() > 0) {
                return id;
            }
        }
        return null;
    }

    @Override
    public Object findNamingStrategy(AnnotatedClass ac)
    {
        JsonNaming ann = ac.getAnnotation(JsonNaming.class);
        return (ann == null) ? null : ann.value();
    } 

    /*
    /**********************************************************
    /* Property auto-detection
    /**********************************************************
     */
    
    @Override
    public VisibilityChecker<?> findAutoDetectVisibility(AnnotatedClass ac,
        VisibilityChecker<?> checker)
    {
        JsonAutoDetect ann = ac.getAnnotation(JsonAutoDetect.class);
        return (ann == null) ? checker : checker.with(ann);
    }

    /*
    /**********************************************************
    /* General member (field, method/constructor) annotations
    /**********************************************************
     */

    @Override        
    public ReferenceProperty findReferenceType(AnnotatedMember member)
    {
        JsonManagedReference ref1 = member.getAnnotation(JsonManagedReference.class);
        if (ref1 != null) {
            return AnnotationIntrospector.ReferenceProperty.managed(ref1.value());
        }
        JsonBackReference ref2 = member.getAnnotation(JsonBackReference.class);
        if (ref2 != null) {
            return AnnotationIntrospector.ReferenceProperty.back(ref2.value());
        }
        return null;
    }

    @Override
    public NameTransformer findUnwrappingNameTransformer(AnnotatedMember member)
    {
        JsonUnwrapped ann = member.getAnnotation(JsonUnwrapped.class);
        // if not enabled, just means annotation is not enabled; not necessarily
        // that unwrapping should not be done (relevant when using chained introspectors)
        if (ann == null || !ann.enabled()) {
            return null;
        }
        String prefix = ann.prefix();
        String suffix = ann.suffix();
        return NameTransformer.simpleTransformer(prefix, suffix);
    }

    @Override
    public boolean hasIgnoreMarker(AnnotatedMember m) {
        return _isIgnorable(m);
    }

    @Override
    public Boolean hasRequiredMarker(AnnotatedMember m)
    {
        JsonProperty ann = m.getAnnotation(JsonProperty.class);
        if (ann != null) {
            return ann.required();
        }
        return null;
    }
    
    @Override
    public Object findInjectableValueId(AnnotatedMember m)
    {
        JacksonInject ann = m.getAnnotation(JacksonInject.class);
        if (ann == null) {
            return null;
        }
        /* Empty String means that we should use name of declared
         * value class.
         */
        String id = ann.value();
        if (id.length() == 0) {
            // slight complication; for setters, type 
            if (!(m instanceof AnnotatedMethod)) {
                return m.getRawType().getName();
            }
            AnnotatedMethod am = (AnnotatedMethod) m;
            if (am.getParameterCount() == 0) {
                return m.getRawType().getName();
            }
            return am.getRawParameterType(0).getName();
        }
        return id;
    }
    
    /*
    /**********************************************************
    /* Class annotations for PM type handling (1.5+)
    /**********************************************************
     */

    @Override
    public TypeResolverBuilder<?> findTypeResolver(MapperConfig<?> config,
            AnnotatedClass ac, JavaType baseType)
    {
        return _findTypeResolver(config, ac, baseType);
    }

    @Override
    public TypeResolverBuilder<?> findPropertyTypeResolver(MapperConfig<?> config,
            AnnotatedMember am, JavaType baseType)
    {
        /* As per definition of @JsonTypeInfo, should only apply to contents of container
         * (collection, map) types, not container types themselves:
         */
        if (baseType.isContainerType()) return null;
        // No per-member type overrides (yet)
        return _findTypeResolver(config, am, baseType);
    }

    @Override
    public TypeResolverBuilder<?> findPropertyContentTypeResolver(MapperConfig<?> config,
            AnnotatedMember am, JavaType containerType)
    {
        /* First: let's ensure property is a container type: caller should have
         * verified but just to be sure
         */
        if (!containerType.isContainerType()) {
            throw new IllegalArgumentException("Must call method with a container type (got "+containerType+")");
        }
        return _findTypeResolver(config, am, containerType);
    }
    
    @Override
    public List<NamedType> findSubtypes(Annotated a)
    {
        JsonSubTypes t = a.getAnnotation(JsonSubTypes.class);
        if (t == null) return null;
        JsonSubTypes.Type[] types = t.value();
        ArrayList<NamedType> result = new ArrayList<NamedType>(types.length);
        for (JsonSubTypes.Type type : types) {
            result.add(new NamedType(type.value(), type.name()));
        }
        return result;
    }

    @Override        
    public String findTypeName(AnnotatedClass ac)
    {
        JsonTypeName tn = ac.getAnnotation(JsonTypeName.class);
        return (tn == null) ? null : tn.value();
    }

    /*
    /**********************************************************
    /* Serialization: general annotations
    /**********************************************************
    */

    @Override
    public Object findSerializer(Annotated a)
    {
        JsonSerialize ann = a.getAnnotation(JsonSerialize.class);
        if (ann != null) {
            Class<? extends JsonSerializer<?>> serClass = ann.using();
            if (serClass != JsonSerializer.None.class) {
                return serClass;
            }
        }
        
        /* 18-Oct-2010, tatu: [JACKSON-351] @JsonRawValue handled just here, for now;
         *  if we need to get raw indicator from other sources need to add
         *  separate accessor within {@link AnnotationIntrospector} interface.
         */
        JsonRawValue annRaw =  a.getAnnotation(JsonRawValue.class);
        if ((annRaw != null) && annRaw.value()) {
            // let's construct instance with nominal type:
            Class<?> cls = a.getRawType();
            return new RawSerializer<Object>(cls);
        }       
        return null;
    }

    @Override
    public Class<? extends JsonSerializer<?>> findKeySerializer(Annotated a)
    {
        JsonSerialize ann = a.getAnnotation(JsonSerialize.class);
        if (ann != null) {
            Class<? extends JsonSerializer<?>> serClass = ann.keyUsing();
            if (serClass != JsonSerializer.None.class) {
                return serClass;
            }
        }
        return null;
    }

    @Override
    public Class<? extends JsonSerializer<?>> findContentSerializer(Annotated a)
    {
        JsonSerialize ann = a.getAnnotation(JsonSerialize.class);
        if (ann != null) {
            Class<? extends JsonSerializer<?>> serClass = ann.contentUsing();
            if (serClass != JsonSerializer.None.class) {
                return serClass;
            }
        }
        return null;
    }
    
    @Override
    public JsonInclude.Include findSerializationInclusion(Annotated a, JsonInclude.Include defValue)
    {
        JsonInclude inc = a.getAnnotation(JsonInclude.class);
        if (inc != null) {
            return inc.value();
        }
        JsonSerialize ann = a.getAnnotation(JsonSerialize.class);
        if (ann != null) {
            @SuppressWarnings("deprecation")
            JsonSerialize.Inclusion i2 = ann.include();
            switch (i2) {
            case ALWAYS:
                return JsonInclude.Include.ALWAYS;
            case NON_NULL:
                return JsonInclude.Include.NON_NULL;
            case NON_DEFAULT:
                return JsonInclude.Include.NON_DEFAULT;
            case NON_EMPTY:
                return JsonInclude.Include.NON_EMPTY;
            }
        }
        return defValue;
    }

    @Override
    public Class<?> findSerializationType(Annotated am)
    {
        JsonSerialize ann = am.getAnnotation(JsonSerialize.class);
        if (ann != null) {
            Class<?> cls = ann.as();
            if (cls != NoClass.class) {
                return cls;
            }
        }
        return null;
    }

    @Override
    public Class<?> findSerializationKeyType(Annotated am, JavaType baseType)
    {
        JsonSerialize ann = am.getAnnotation(JsonSerialize.class);
        if (ann != null) {
            Class<?> cls = ann.keyAs();
            if (cls != NoClass.class) {
                return cls;
            }
        }
        return null;
    }

    @Override
    public Class<?> findSerializationContentType(Annotated am, JavaType baseType)
    {
        JsonSerialize ann = am.getAnnotation(JsonSerialize.class);
        if (ann != null) {
            Class<?> cls = ann.contentAs();
            if (cls != NoClass.class) {
                return cls;
            }
        }
        return null;
    }
    
    @Override
    public JsonSerialize.Typing findSerializationTyping(Annotated a)
    {
        JsonSerialize ann = a.getAnnotation(JsonSerialize.class);
        return (ann == null) ? null : ann.typing();
    }

    @Override
    public Object findSerializationConverter(Annotated a) {
        JsonSerialize ann = a.getAnnotation(JsonSerialize.class);
        if (ann != null) {
            Class<?> def = ann.converter();
            if (def != Converter.None.class) {
                return def;
            }
        }
        return null;
    }

    @Override
    public Object findSerializationContentConverter(AnnotatedMember a) {
        JsonSerialize ann = a.getAnnotation(JsonSerialize.class);
        if (ann != null) {
            Class<?> def = ann.contentConverter();
            if (def != Converter.None.class) {
                return def;
            }
        }
        return null;
    }
    
    @Override
    public Class<?>[] findViews(Annotated a)
    {
        JsonView ann = a.getAnnotation(JsonView.class);
        return (ann == null) ? null : ann.value();
    }

    @Override
    public Boolean isTypeId(AnnotatedMember member) {
        return member.hasAnnotation(JsonTypeId.class);
    }

    @Override
    public ObjectIdInfo findObjectIdInfo(Annotated ann) {
        JsonIdentityInfo info = ann.getAnnotation(JsonIdentityInfo.class);
        if (info == null || info.generator() == ObjectIdGenerators.None.class) {
            return null;
        }
        return new ObjectIdInfo(info.property(), info.scope(), info.generator());
    }

    @Override
    public ObjectIdInfo findObjectReferenceInfo(Annotated ann, ObjectIdInfo objectIdInfo) {
        JsonIdentityReference ref = ann.getAnnotation(JsonIdentityReference.class);
        if (ref != null) {
            objectIdInfo = objectIdInfo.withAlwaysAsId(ref.alwaysAsId());
        }
        return objectIdInfo;
    }
    
    @Override
    public JsonFormat.Value findFormat(AnnotatedMember member) {
        return findFormat(member);
    }
    
    @Override
    public JsonFormat.Value findFormat(Annotated annotated) {
        JsonFormat ann = annotated.getAnnotation(JsonFormat.class);
        return (ann == null)  ? null : new JsonFormat.Value(ann);
    }

    /*
    /**********************************************************
    /* Serialization: class annotations
    /**********************************************************
     */

    @Override
    public String[] findSerializationPropertyOrder(AnnotatedClass ac) {
        JsonPropertyOrder order = ac.getAnnotation(JsonPropertyOrder.class);
        return (order == null) ? null : order.value();
    }

    @Override
    public Boolean findSerializationSortAlphabetically(AnnotatedClass ac) {
        JsonPropertyOrder order = ac.getAnnotation(JsonPropertyOrder.class);
        return (order == null) ? null : order.alphabetic();
    }

    /*
    /**********************************************************
    /* Serialization: property annotations
    /**********************************************************
     */

    @Override
    public PropertyName findNameForSerialization(Annotated a)
    {
        // [Issue#69], need bit of delegation 
        // !!! TODO: in 2.2, remove old methods?
        String name;
        if (a instanceof AnnotatedField) {
            name = findSerializationName((AnnotatedField) a);
        } else if (a instanceof AnnotatedMethod) {
            name = findSerializationName((AnnotatedMethod) a);
        } else {
            name = null;
        }
        if (name != null) {
            if (name.length() == 0) { // empty String means 'default'
                return PropertyName.USE_DEFAULT;
            }
            return new PropertyName(name);
        }
        return null;
    }
    
    @Override
    public String findSerializationName(AnnotatedField af)
    {
        JsonProperty pann = af.getAnnotation(JsonProperty.class);
        if (pann != null) {
            return pann.value();
        }
        // Also: having JsonSerialize implies it is such a property
        // 09-Apr-2010, tatu: Ditto for JsonView
        if (af.hasAnnotation(JsonSerialize.class) || af.hasAnnotation(JsonView.class)) {
            return "";
        }
        return null;
    }
    
    @Override
    public String findSerializationName(AnnotatedMethod am)
    {
        // @JsonGetter is most specific, has precedence
        JsonGetter ann = am.getAnnotation(JsonGetter.class);
        if (ann != null) {
            return ann.value();
        }
        JsonProperty pann = am.getAnnotation(JsonProperty.class);
        if (pann != null) {
            return pann.value();
        }
        /* 22-May-2009, tatu: And finally, JsonSerialize implies
         *   that there is a property, although doesn't define name
         */
        // 09-Apr-2010, tatu: Ditto for JsonView
        if (am.hasAnnotation(JsonSerialize.class) || am.hasAnnotation(JsonView.class)) {
            return "";
        }
        return null;
    }

    @Override
    public boolean hasAsValueAnnotation(AnnotatedMethod am)
    {
        JsonValue ann = am.getAnnotation(JsonValue.class);
        // value of 'false' means disabled...
        return (ann != null && ann.value());
    }

    /*
    /**********************************************************
    /* Deserialization: general annotations
    /**********************************************************
     */

    @Override
    public Class<? extends JsonDeserializer<?>> findDeserializer(Annotated a)
    {
        JsonDeserialize ann = a.getAnnotation(JsonDeserialize.class);
        if (ann != null) {
            Class<? extends JsonDeserializer<?>> deserClass = ann.using();
            if (deserClass != JsonDeserializer.None.class) {
                return deserClass;
            }
        }
        return null;
    }

    @Override
    public Class<? extends KeyDeserializer> findKeyDeserializer(Annotated a)
    {
        JsonDeserialize ann = a.getAnnotation(JsonDeserialize.class);
        if (ann != null) {
            Class<? extends KeyDeserializer> deserClass = ann.keyUsing();
            if (deserClass != KeyDeserializer.None.class) {
                return deserClass;
            }
        }
        return null;
    }

    @Override
    public Class<? extends JsonDeserializer<?>> findContentDeserializer(Annotated a)
    {
        JsonDeserialize ann = a.getAnnotation(JsonDeserialize.class);
        if (ann != null) {
            Class<? extends JsonDeserializer<?>> deserClass = ann.contentUsing();
            if (deserClass != JsonDeserializer.None.class) {
                return deserClass;
            }
        }
        return null;
    }

    @Override
    public Class<?> findDeserializationType(Annotated am, JavaType baseType)
    {
        // Primary annotation, JsonDeserialize
        JsonDeserialize ann = am.getAnnotation(JsonDeserialize.class);
        if (ann != null) {
            Class<?> cls = ann.as();
            if (cls != NoClass.class) {
                return cls;
            }
        }
        return null;
    }

    @Override
    public Class<?> findDeserializationKeyType(Annotated am, JavaType baseKeyType)
    {
        // Primary annotation, JsonDeserialize
        JsonDeserialize ann = am.getAnnotation(JsonDeserialize.class);
        if (ann != null) {
            Class<?> cls = ann.keyAs();
            if (cls != NoClass.class) {
                return cls;
            }
        }
        return null;
    }

    @Override
    public Class<?> findDeserializationContentType(Annotated am, JavaType baseContentType)
    {
        // Primary annotation, JsonDeserialize
        JsonDeserialize ann = am.getAnnotation(JsonDeserialize.class);
        if (ann != null) {
            Class<?> cls = ann.contentAs();
            if (cls != NoClass.class) {
                return cls;
            }
        }
        return null;
    }

    @Override
    public Object findDeserializationConverter(Annotated a)
    {
        JsonDeserialize ann = a.getAnnotation(JsonDeserialize.class);
        if (ann != null) {
            Class<?> def = ann.converter();
            if (def != Converter.None.class) {
                return def;
            }
        }
        return null;
    }

    @Override
    public Object findDeserializationContentConverter(AnnotatedMember a)
    {
        JsonDeserialize ann = a.getAnnotation(JsonDeserialize.class);
        if (ann != null) {
            Class<?> def = ann.contentConverter();
            if (def != Converter.None.class) {
                return def;
            }
        }
        return null;    }
    
    /*
    /**********************************************************
    /* Deserialization: Class annotations
    /**********************************************************
     */
    
    @Override
    public Object findValueInstantiator(AnnotatedClass ac)
    {
        JsonValueInstantiator ann = ac.getAnnotation(JsonValueInstantiator.class);
        // no 'null' marker yet, so:
        return (ann == null) ? null : ann.value();
    }

    @Override
    public Class<?> findPOJOBuilder(AnnotatedClass ac)
    {
        JsonDeserialize ann = ac.getAnnotation(JsonDeserialize.class);
        return ((ann == null) || (ann.builder() == NoClass.class)) ?
                null : ann.builder();
    }

    @Override
    public JsonPOJOBuilder.Value findPOJOBuilderConfig(AnnotatedClass ac)
    {
        JsonPOJOBuilder ann = ac.getAnnotation(JsonPOJOBuilder.class);
        return (ann == null) ? null : new JsonPOJOBuilder.Value(ann);
    }
    
    /*
    /**********************************************************
    /* Deserialization: property annotations
    /**********************************************************
     */

    @Override
    public PropertyName findNameForDeserialization(Annotated a)
    {
        // [Issue#69], need bit of delegation 
        // !!! TODO: in 2.2, remove old methods?
        String name;
        if (a instanceof AnnotatedField) {
            name = findDeserializationName((AnnotatedField) a);
        } else if (a instanceof AnnotatedMethod) {
            name = findDeserializationName((AnnotatedMethod) a);
        } else if (a instanceof AnnotatedParameter) {
            name = findDeserializationName((AnnotatedParameter) a);
        } else {
            name = null;
        }
        if (name != null) {
            if (name.length() == 0) { // empty String means 'default'
                return PropertyName.USE_DEFAULT;
            }
            return new PropertyName(name);
        }
        return null;
    }
    
    @Override
    public String findDeserializationName(AnnotatedMethod am)
    {
        // @JsonSetter has precedence over @JsonProperty, being more specific
        JsonSetter ann = am.getAnnotation(JsonSetter.class);
        if (ann != null) {
            return ann.value();
        }
        JsonProperty pann = am.getAnnotation(JsonProperty.class);
        if (pann != null) {
            return pann.value();
        }
        // @JsonSerialize implies that there is a property, but no name
        // 09-Apr-2010, tatu: Ditto for JsonView
        // 19-Oct-2011, tatu: And JsonBackReference/JsonManagedReference
    	if (am.hasAnnotation(JsonDeserialize.class)
    	        || am.hasAnnotation(JsonView.class)
                || am.hasAnnotation(JsonBackReference.class)
                || am.hasAnnotation(JsonManagedReference.class)
    	        ) {
            return "";
        }
        return null;
    }

    @Override
    public String findDeserializationName(AnnotatedField af)
    {
        JsonProperty pann = af.getAnnotation(JsonProperty.class);
        if (pann != null) {
            return pann.value();
        }
        // Also: having JsonDeserialize implies it is such a property
        // 09-Apr-2010, tatu: Ditto for JsonView
        if (af.hasAnnotation(JsonDeserialize.class)
                || af.hasAnnotation(JsonView.class)
                || af.hasAnnotation(JsonBackReference.class)
                || af.hasAnnotation(JsonManagedReference.class)
                ) {
            return "";
        }
        return null;
    }
    @Override
    public String findDeserializationName(AnnotatedParameter param)
    {
        if (param != null) {
            JsonProperty pann = param.getAnnotation(JsonProperty.class);
            if (pann != null) {
                return pann.value();
            }
            /* And can not use JsonDeserialize as we can not use
             * name auto-detection (names of local variables including
             * parameters are not necessarily preserved in bytecode)
             */
        }
        return null;
    }
    
    @Override
    public boolean hasAnySetterAnnotation(AnnotatedMethod am)
    {
        /* No dedicated disabling; regular @JsonIgnore used
         * if needs to be ignored (and if so, is handled prior
         * to this method getting called)
         */
        return am.hasAnnotation(JsonAnySetter.class);
    }

    @Override
    public boolean hasAnyGetterAnnotation(AnnotatedMethod am)
    {
        /* No dedicated disabling; regular @JsonIgnore used
         * if needs to be ignored (handled separately
         */
        return am.hasAnnotation(JsonAnyGetter.class);
    }
    
    @Override
    public boolean hasCreatorAnnotation(Annotated a)
    {
        /* No dedicated disabling; regular @JsonIgnore used
         * if needs to be ignored (and if so, is handled prior
         * to this method getting called)
         */
        return a.hasAnnotation(JsonCreator.class);
    }

    /*
    /**********************************************************
    /* Helper methods
    /**********************************************************
     */

    protected boolean _isIgnorable(Annotated a)
    {
        JsonIgnore ann = a.getAnnotation(JsonIgnore.class);
        return (ann != null && ann.value());
    }

    /**
     * Helper method called to construct and initialize instance of {@link TypeResolverBuilder}
     * if given annotated element indicates one is needed.
     */
    protected TypeResolverBuilder<?> _findTypeResolver(MapperConfig<?> config,
            Annotated ann, JavaType baseType)
    {
        // First: maybe we have explicit type resolver?
        TypeResolverBuilder<?> b;
        JsonTypeInfo info = ann.getAnnotation(JsonTypeInfo.class);
        JsonTypeResolver resAnn = ann.getAnnotation(JsonTypeResolver.class);
        
        if (resAnn != null) {
            if (info == null) {
                return null;
            }
            /* let's not try to force access override (would need to pass
             * settings through if we did, since that's not doable on some
             * platforms)
             */
            b = config.typeResolverBuilderInstance(ann, resAnn.value());
        } else { // if not, use standard one, if indicated by annotations
            if (info == null) {
                return null;
            }
            // bit special; must return 'marker' to block use of default typing:
            if (info.use() == JsonTypeInfo.Id.NONE) {
                return _constructNoTypeResolverBuilder();
            }
            b = _constructStdTypeResolverBuilder();
        }
        // Does it define a custom type id resolver?
        JsonTypeIdResolver idResInfo = ann.getAnnotation(JsonTypeIdResolver.class);
        TypeIdResolver idRes = (idResInfo == null) ? null
                : config.typeIdResolverInstance(ann, idResInfo.value());
        if (idRes != null) { // [JACKSON-359]
            idRes.init(baseType);
        }
        b = b.init(info.use(), idRes);
        /* 13-Aug-2011, tatu: One complication wrt [JACKSON-453]; external id
         *   only works for properties; so if declared for a Class, we will need
         *   to map it to "PROPERTY" instead of "EXTERNAL_PROPERTY"
         */
        JsonTypeInfo.As inclusion = info.include();
        if (inclusion == JsonTypeInfo.As.EXTERNAL_PROPERTY && (ann instanceof AnnotatedClass)) {
            inclusion = JsonTypeInfo.As.PROPERTY;
        }
        b = b.inclusion(inclusion);
        b = b.typeProperty(info.property());
        Class<?> defaultImpl = info.defaultImpl();
        if (defaultImpl != JsonTypeInfo.None.class) {
            b = b.defaultImpl(defaultImpl);
        }
        b = b.typeIdVisibility(info.visible());
        return b;
    }

    /**
     * Helper method for constructing standard {@link TypeResolverBuilder}
     * implementation.
     */
    protected StdTypeResolverBuilder _constructStdTypeResolverBuilder() {
        return new StdTypeResolverBuilder();
    }

    /**
     * Helper method for dealing with "no type info" marker; can't be null
     * (as it'd be replaced by default typing)
     */
    protected StdTypeResolverBuilder _constructNoTypeResolverBuilder() {
        return StdTypeResolverBuilder.noTypeInfoBuilder();
    }
}
