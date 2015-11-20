package com.fasterxml.jackson.databind.introspect;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.*;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.annotation.*;
import com.fasterxml.jackson.databind.cfg.HandlerInstantiator;
import com.fasterxml.jackson.databind.cfg.MapperConfig;
import com.fasterxml.jackson.databind.jsontype.NamedType;
import com.fasterxml.jackson.databind.jsontype.TypeIdResolver;
import com.fasterxml.jackson.databind.jsontype.TypeResolverBuilder;
import com.fasterxml.jackson.databind.jsontype.impl.StdTypeResolverBuilder;
import com.fasterxml.jackson.databind.ser.BeanPropertyWriter;
import com.fasterxml.jackson.databind.ser.VirtualBeanPropertyWriter;
import com.fasterxml.jackson.databind.ser.impl.AttributePropertyWriter;
import com.fasterxml.jackson.databind.ser.std.RawSerializer;
import com.fasterxml.jackson.databind.util.*;

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

    /**
     * Annotations with meta-annotation {@link JacksonAnnotationsInside}
     * are considered bundles.
     */
    @Override
    public boolean isAnnotationBundle(Annotation ann) {
        return ann.annotationType().getAnnotation(JacksonAnnotationsInside.class) != null;
    }

    /*
    /**********************************************************
    /* General annotations
    /**********************************************************
     */

    /**
     * Since 2.6, we have supported use of {@link JsonProperty} for specifying
     * explicit serialized name
     */
    @Override
    public String findEnumValue(Enum<?> value)
    {
        // 11-Jun-2015, tatu: As per [databind#677], need to allow explicit naming.
        //   Unfortunately can not quite use standard AnnotatedClass here (due to various
        //   reasons, including odd representation JVM uses); has to do for now
        try {
            // We know that values are actually static fields with matching name so:
            Field f = value.getClass().getField(value.name());
            if (f != null) {
                JsonProperty prop = f.getAnnotation(JsonProperty.class);
                if (prop != null) {
                    String n = prop.value();
                    if (n != null && !n.isEmpty()) {
                        return n;
                    }
                }
            }
        } catch (SecurityException e) {
            // 17-Sep-2015, tatu: Anything we could/should do here?
        } catch (NoSuchFieldException e) {
            // 17-Sep-2015, tatu: should not really happen. But... can we do anything?
        }
        return value.name();
    }

    /*
    /**********************************************************
    /* General class annotations
    /**********************************************************
     */

    @Override
    public PropertyName findRootName(AnnotatedClass ac)
    {
        JsonRootName ann = _findAnnotation(ac, JsonRootName.class);
        if (ann == null) {
            return null;
        }
        String ns = ann.namespace();
        if (ns != null && ns.length() == 0) {
            ns = null;
        }
        return PropertyName.construct(ann.value(), ns);
    }

    @Override
    @Deprecated // since 2.6, remove from 2.7 or later
    public String[] findPropertiesToIgnore(Annotated ac) {
        JsonIgnoreProperties ignore = _findAnnotation(ac, JsonIgnoreProperties.class);
        return (ignore == null) ? null : ignore.value();
    }

    @Override // since 2.6
    public String[] findPropertiesToIgnore(Annotated ac, boolean forSerialization) {
        JsonIgnoreProperties ignore = _findAnnotation(ac, JsonIgnoreProperties.class);
        if (ignore == null) {
            return null;
        }
        // 13-May-2015, tatu: As per [databind#95], allow read-only/write-only props
        if (forSerialization) {
            if (ignore.allowGetters()) {
                return null;
            }
        } else {
            if (ignore.allowSetters()) {
                return null;
            }
        }
        return ignore.value();
    }
    
    @Override
    public Boolean findIgnoreUnknownProperties(AnnotatedClass ac) {
        JsonIgnoreProperties ignore = _findAnnotation(ac, JsonIgnoreProperties.class);
        return (ignore == null) ? null : ignore.ignoreUnknown();
    }

    @Override
    public Boolean isIgnorableType(AnnotatedClass ac) {
        JsonIgnoreType ignore = _findAnnotation(ac, JsonIgnoreType.class);
        return (ignore == null) ? null : ignore.value();
    }

    /**
     * @deprecated (since 2.3) Use {@link #findFilterId(Annotated)} instead
     */
    @Deprecated
    @Override
    public Object findFilterId(AnnotatedClass ac) {
        return _findFilterId(ac);
    }
    
    @Override
    public Object findFilterId(Annotated a) {
        return _findFilterId(a);
    }

    protected final Object _findFilterId(Annotated a)
    {
        JsonFilter ann = _findAnnotation(a, JsonFilter.class);
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
        JsonNaming ann = _findAnnotation(ac, JsonNaming.class);
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
        JsonAutoDetect ann = _findAnnotation(ac, JsonAutoDetect.class);
        return (ann == null) ? checker : checker.with(ann);
    }

    /*
    /**********************************************************
    /* General member (field, method/constructor) annotations
    /**********************************************************
     */

    @Override
    public String findImplicitPropertyName(AnnotatedMember param) {
        // not known by default (until JDK8) for creators; default 
        //
        return null;
    }
    
    @Override
    public boolean hasIgnoreMarker(AnnotatedMember m) {
        return _isIgnorable(m);
    }

    @Override
    public Boolean hasRequiredMarker(AnnotatedMember m)
    {
        JsonProperty ann = _findAnnotation(m, JsonProperty.class);
        if (ann != null) {
            return ann.required();
        }
        return null;
    }

    @Override
    public JsonProperty.Access findPropertyAccess(Annotated m) {
        JsonProperty ann = _findAnnotation(m, JsonProperty.class);
        if (ann != null) {
            return ann.access();
        }
        return null;
    }

    @Override
    public String findPropertyDescription(Annotated ann) {
        JsonPropertyDescription desc = _findAnnotation(ann, JsonPropertyDescription.class);
        return (desc == null) ? null : desc.value();
    }

    @Override
    public Integer findPropertyIndex(Annotated ann) {
        JsonProperty prop = _findAnnotation(ann, JsonProperty.class);
        if (prop != null) {
          int ix = prop.index();
          if (ix != JsonProperty.INDEX_UNKNOWN) {
               return Integer.valueOf(ix);
          }
        }
        return null;
    }
    
    @Override
    public String findPropertyDefaultValue(Annotated ann) {
        JsonProperty prop = _findAnnotation(ann, JsonProperty.class);
        if (prop == null) {
            return null;
        }
        String str = prop.defaultValue();
        // Since annotations do not allow nulls, need to assume empty means "none"
        return str.isEmpty() ? null : str;
    }
    
    @Override
    public JsonFormat.Value findFormat(Annotated ann) {
        JsonFormat f = _findAnnotation(ann, JsonFormat.class);
        return (f == null)  ? null : new JsonFormat.Value(f);
    }

    @Override        
    public ReferenceProperty findReferenceType(AnnotatedMember member)
    {
        JsonManagedReference ref1 = _findAnnotation(member, JsonManagedReference.class);
        if (ref1 != null) {
            return AnnotationIntrospector.ReferenceProperty.managed(ref1.value());
        }
        JsonBackReference ref2 = _findAnnotation(member, JsonBackReference.class);
        if (ref2 != null) {
            return AnnotationIntrospector.ReferenceProperty.back(ref2.value());
        }
        return null;
    }

    @Override
    public NameTransformer findUnwrappingNameTransformer(AnnotatedMember member)
    {
        JsonUnwrapped ann = _findAnnotation(member, JsonUnwrapped.class);
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
    public Object findInjectableValueId(AnnotatedMember m)
    {
        JacksonInject ann = _findAnnotation(m, JacksonInject.class);
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

    @Override
    public Class<?>[] findViews(Annotated a)
    {
        JsonView ann = _findAnnotation(a, JsonView.class);
        return (ann == null) ? null : ann.value();
    }

    /*
    /**********************************************************
    /* Annotations for Polymorphic Type handling
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
        JsonSubTypes t = _findAnnotation(a, JsonSubTypes.class);
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
        JsonTypeName tn = _findAnnotation(ac, JsonTypeName.class);
        return (tn == null) ? null : tn.value();
    }

    @Override
    public Boolean isTypeId(AnnotatedMember member) {
        return _hasAnnotation(member, JsonTypeId.class);
    }

    /*
    /**********************************************************
    /* Annotations for Object Id handling
    /**********************************************************
     */

    @Override
    public ObjectIdInfo findObjectIdInfo(Annotated ann) {
        JsonIdentityInfo info = _findAnnotation(ann, JsonIdentityInfo.class);
        if (info == null || info.generator() == ObjectIdGenerators.None.class) {
            return null;
        }
        // In future may need to allow passing namespace?
        PropertyName name = PropertyName.construct(info.property());
        return new ObjectIdInfo(name, info.scope(), info.generator(), info.resolver());
    }

    @Override
    public ObjectIdInfo findObjectReferenceInfo(Annotated ann, ObjectIdInfo objectIdInfo) {
        JsonIdentityReference ref = _findAnnotation(ann, JsonIdentityReference.class);
        if (ref != null) {
            objectIdInfo = objectIdInfo.withAlwaysAsId(ref.alwaysAsId());
        }
        return objectIdInfo;
    }

    /*
    /**********************************************************
    /* Serialization: general annotations
    /**********************************************************
    */

    @Override
    public Object findSerializer(Annotated a)
    {
        JsonSerialize ann = _findAnnotation(a, JsonSerialize.class);
        if (ann != null) {
            @SuppressWarnings("rawtypes")
            Class<? extends JsonSerializer> serClass = ann.using();
            if (serClass != JsonSerializer.None.class) {
                return serClass;
            }
        }
        
        /* 18-Oct-2010, tatu: [JACKSON-351] @JsonRawValue handled just here, for now;
         *  if we need to get raw indicator from other sources need to add
         *  separate accessor within {@link AnnotationIntrospector} interface.
         */
        JsonRawValue annRaw =  _findAnnotation(a, JsonRawValue.class);
        if ((annRaw != null) && annRaw.value()) {
            // let's construct instance with nominal type:
            Class<?> cls = a.getRawType();
            return new RawSerializer<Object>(cls);
        }       
        return null;
    }

    @Override
    public Object findKeySerializer(Annotated a)
    {
        JsonSerialize ann = _findAnnotation(a, JsonSerialize.class);
        if (ann != null) {
            @SuppressWarnings("rawtypes")
            Class<? extends JsonSerializer> serClass = ann.keyUsing();
            if (serClass != JsonSerializer.None.class) {
                return serClass;
            }
        }
        return null;
    }

    @Override
    public Object findContentSerializer(Annotated a)
    {
        JsonSerialize ann = _findAnnotation(a, JsonSerialize.class);
        if (ann != null) {
            @SuppressWarnings("rawtypes")
            Class<? extends JsonSerializer> serClass = ann.contentUsing();
            if (serClass != JsonSerializer.None.class) {
                return serClass;
            }
        }
        return null;
    }

    @Override
    public Object findNullSerializer(Annotated a)
    {
        JsonSerialize ann = _findAnnotation(a, JsonSerialize.class);
        if (ann != null) {
            @SuppressWarnings("rawtypes")
            Class<? extends JsonSerializer> serClass = ann.nullsUsing();
            if (serClass != JsonSerializer.None.class) {
                return serClass;
            }
        }
        return null;
    }

    @Override
    @SuppressWarnings("deprecation")
    public JsonInclude.Include findSerializationInclusion(Annotated a, JsonInclude.Include defValue)
    {
        JsonInclude inc = _findAnnotation(a, JsonInclude.class);
        if (inc != null) {
            JsonInclude.Include v = inc.value();
            if (v != JsonInclude.Include.USE_DEFAULTS) {
                return v;
            }
        }
        JsonSerialize ann = _findAnnotation(a, JsonSerialize.class);
        if (ann != null) {
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
            case DEFAULT_INCLUSION: // since 2.3 -- fall through, use default
                break;
            }
        }
        return defValue;
    }

    @Override
    public JsonInclude.Include findSerializationInclusionForContent(Annotated a, JsonInclude.Include defValue)
    {
        JsonInclude inc = _findAnnotation(a, JsonInclude.class);
        if (inc != null) {
            JsonInclude.Include incl = inc.content();
            if (incl != JsonInclude.Include.USE_DEFAULTS) {
                return incl;
            }
        }
        return defValue;
    }

    @Override
    @SuppressWarnings("deprecation")
    public JsonInclude.Value findPropertyInclusion(Annotated a)
    {
        JsonInclude inc = _findAnnotation(a, JsonInclude.class);
        JsonInclude.Include valueIncl = (inc == null) ? JsonInclude.Include.USE_DEFAULTS : inc.value();
        if (valueIncl == JsonInclude.Include.USE_DEFAULTS) {
            JsonSerialize ann = _findAnnotation(a, JsonSerialize.class);
            if (ann != null) {
                JsonSerialize.Inclusion i2 = ann.include();
                switch (i2) {
                case ALWAYS:
                    valueIncl = JsonInclude.Include.ALWAYS;
                    break;
                case NON_NULL:
                    valueIncl = JsonInclude.Include.NON_NULL;
                    break;
                case NON_DEFAULT:
                    valueIncl = JsonInclude.Include.NON_DEFAULT;
                    break;
                case NON_EMPTY:
                    valueIncl = JsonInclude.Include.NON_EMPTY;
                    break;
                case DEFAULT_INCLUSION:
                default:
                }
            }
        }
        JsonInclude.Include contentIncl = (inc == null) ? JsonInclude.Include.USE_DEFAULTS : inc.content();
        return JsonInclude.Value.construct(valueIncl, contentIncl);
    }

    @Override
    public Class<?> findSerializationType(Annotated am)
    {
        JsonSerialize ann = _findAnnotation(am, JsonSerialize.class);
        return (ann == null) ? null : _classIfExplicit(ann.as());
    }

    @Override
    public Class<?> findSerializationKeyType(Annotated am, JavaType baseType)
    {
        JsonSerialize ann = _findAnnotation(am, JsonSerialize.class);
        return (ann == null) ? null : _classIfExplicit(ann.keyAs());
    }

    @Override
    public Class<?> findSerializationContentType(Annotated am, JavaType baseType)
    {
        JsonSerialize ann = _findAnnotation(am, JsonSerialize.class);
        return (ann == null) ? null : _classIfExplicit(ann.contentAs());
    }
    
    @Override
    public JsonSerialize.Typing findSerializationTyping(Annotated a)
    {
        JsonSerialize ann = _findAnnotation(a, JsonSerialize.class);
        return (ann == null) ? null : ann.typing();
    }

    @Override
    public Object findSerializationConverter(Annotated a) {
        JsonSerialize ann = _findAnnotation(a, JsonSerialize.class);
        return (ann == null) ? null : _classIfExplicit(ann.converter(), Converter.None.class);
    }

    @Override
    public Object findSerializationContentConverter(AnnotatedMember a) {
        JsonSerialize ann = _findAnnotation(a, JsonSerialize.class);
        return (ann == null) ? null : _classIfExplicit(ann.contentConverter(), Converter.None.class);
    }

    /*
    /**********************************************************
    /* Serialization: class annotations
    /**********************************************************
     */

    @Override
    public String[] findSerializationPropertyOrder(AnnotatedClass ac) {
        JsonPropertyOrder order = _findAnnotation(ac, JsonPropertyOrder.class);
        return (order == null) ? null : order.value();
    }

    @Override
    public Boolean findSerializationSortAlphabetically(Annotated ann) {
        return _findSortAlpha(ann);
    }

    private final Boolean _findSortAlpha(Annotated ann) {
        JsonPropertyOrder order = _findAnnotation(ann, JsonPropertyOrder.class);
        /* 23-Jun-2015, tatu: as per [databind#840], let's only consider
         *  `true` to have any significance.
         */
        if ((order != null) && order.alphabetic()) {
            return Boolean.TRUE;
        }
        return null;
    }

    @Override
    public void findAndAddVirtualProperties(MapperConfig<?> config, AnnotatedClass ac,
            List<BeanPropertyWriter> properties) {
        JsonAppend ann = _findAnnotation(ac, JsonAppend.class);
        if (ann == null) {
            return;
        }
        final boolean prepend = ann.prepend();
        JavaType propType = null;

        // First: any attribute-backed properties?
        JsonAppend.Attr[] attrs = ann.attrs();
        for (int i = 0, len = attrs.length; i < len; ++i) {
            if (propType == null) {
                propType = config.constructType(Object.class);
            }
            BeanPropertyWriter bpw = _constructVirtualProperty(attrs[i],
                    config, ac, propType);
            if (prepend) {
                properties.add(i, bpw);
            } else {
                properties.add(bpw);
            }
        }

        // Then: general-purpose virtual properties?
        JsonAppend.Prop[] props = ann.props();
        for (int i = 0, len = props.length; i < len; ++i) {
            BeanPropertyWriter bpw = _constructVirtualProperty(props[i],
                    config, ac);
            if (prepend) {
                properties.add(i, bpw);
            } else {
                properties.add(bpw);
            }
        }
    }

    protected BeanPropertyWriter _constructVirtualProperty(JsonAppend.Attr attr,
            MapperConfig<?> config, AnnotatedClass ac, JavaType type)
    {
        PropertyMetadata metadata = attr.required() ?
                    PropertyMetadata.STD_REQUIRED : PropertyMetadata.STD_OPTIONAL;
        // could add Index, Description in future, if those matter
        String attrName = attr.value();

        // allow explicit renaming; if none, default to attribute name
        PropertyName propName = _propertyName(attr.propName(), attr.propNamespace());
        if (!propName.hasSimpleName()) {
            propName = PropertyName.construct(attrName);
        }
        // now, then, we need a placeholder for member (no real Field/Method):
        AnnotatedMember member = new VirtualAnnotatedMember(ac, ac.getRawType(),
                attrName, type.getRawClass());
        // and with that and property definition
        SimpleBeanPropertyDefinition propDef = SimpleBeanPropertyDefinition.construct(config,
                member, propName, metadata, attr.include());
        // can construct the property writer
        return AttributePropertyWriter.construct(attrName, propDef,
                ac.getAnnotations(), type);
    }

    protected BeanPropertyWriter _constructVirtualProperty(JsonAppend.Prop prop,
            MapperConfig<?> config, AnnotatedClass ac)
    {
        PropertyMetadata metadata = prop.required() ?
                    PropertyMetadata.STD_REQUIRED : PropertyMetadata.STD_OPTIONAL;
        PropertyName propName = _propertyName(prop.name(), prop.namespace());
        JavaType type = config.constructType(prop.type());
        // now, then, we need a placeholder for member (no real Field/Method):
        AnnotatedMember member = new VirtualAnnotatedMember(ac, ac.getRawType(),
                propName.getSimpleName(), type.getRawClass());
        // and with that and property definition
        SimpleBeanPropertyDefinition propDef = SimpleBeanPropertyDefinition.construct(config,
                member, propName, metadata, prop.include());

        Class<?> implClass = prop.value();

        HandlerInstantiator hi = config.getHandlerInstantiator();
        VirtualBeanPropertyWriter bpw = (hi == null) ? null
                : hi.virtualPropertyWriterInstance(config, implClass);
        if (bpw == null) {
            bpw = (VirtualBeanPropertyWriter) ClassUtil.createInstance(implClass,
                    config.canOverrideAccessModifiers());
        }

        // one more thing: give it necessary contextual information
        return bpw.withConfig(config, ac, propDef, type);
    }

    /*
    /**********************************************************
    /* Serialization: property annotations
    /**********************************************************
     */

    @Override
    public PropertyName findNameForSerialization(Annotated a)
    {
        String name = null;

        JsonGetter jg = _findAnnotation(a, JsonGetter.class);
        if (jg != null) {
            name = jg.value();
        } else {
            JsonProperty pann = _findAnnotation(a, JsonProperty.class);
            if (pann != null) {
                name = pann.value();
                /* 22-Apr-2014, tatu: Should figure out a better way to do this, but
                 *   it's actually bit tricky to do it more efficiently (meta-annotations
                 *   add more lookups; AnnotationMap costs etc)
                 */
            } else if (_hasAnnotation(a, JsonSerialize.class)
                    || _hasAnnotation(a, JsonView.class)
                    || _hasAnnotation(a, JsonRawValue.class)
                    || _hasAnnotation(a, JsonUnwrapped.class)
                    || _hasAnnotation(a, JsonBackReference.class)
                    || _hasAnnotation(a, JsonManagedReference.class)) {
                name = "";
            } else {
                return null;
            }
        }
        return PropertyName.construct(name);
    }

    @Override
    public boolean hasAsValueAnnotation(AnnotatedMethod am) {
        JsonValue ann = _findAnnotation(am, JsonValue.class);
        // value of 'false' means disabled...
        return (ann != null && ann.value());
    }

    /*
    /**********************************************************
    /* Deserialization: general annotations
    /**********************************************************
     */

    @Override
    public Object findDeserializer(Annotated a)
    {
        JsonDeserialize ann = _findAnnotation(a, JsonDeserialize.class);
        if (ann != null) {
            @SuppressWarnings("rawtypes")
            Class<? extends JsonDeserializer> deserClass = ann.using();
            if (deserClass != JsonDeserializer.None.class) {
                return deserClass;
            }
        }
        return null;
    }

    @Override
    public Object findKeyDeserializer(Annotated a)
    {
        JsonDeserialize ann = _findAnnotation(a, JsonDeserialize.class);
        if (ann != null) {
            Class<? extends KeyDeserializer> deserClass = ann.keyUsing();
            if (deserClass != KeyDeserializer.None.class) {
                return deserClass;
            }
        }
        return null;
    }

    @Override
    public Object findContentDeserializer(Annotated a)
    {
        JsonDeserialize ann = _findAnnotation(a, JsonDeserialize.class);
        if (ann != null) {
            @SuppressWarnings("rawtypes")
            Class<? extends JsonDeserializer> deserClass = ann.contentUsing();
            if (deserClass != JsonDeserializer.None.class) {
                return deserClass;
            }
        }
        return null;
    }

    @Override
    public Class<?> findDeserializationType(Annotated am, JavaType baseType) {
        JsonDeserialize ann = _findAnnotation(am, JsonDeserialize.class);
        return (ann == null) ? null : _classIfExplicit(ann.as());
    }

    @Override
    public Class<?> findDeserializationKeyType(Annotated am, JavaType baseKeyType) {
        JsonDeserialize ann = _findAnnotation(am, JsonDeserialize.class);
        return (ann == null) ? null : _classIfExplicit(ann.keyAs());
    }

    @Override
    public Class<?> findDeserializationContentType(Annotated am, JavaType baseContentType)
    {
        JsonDeserialize ann = _findAnnotation(am, JsonDeserialize.class);
        return (ann == null) ? null : _classIfExplicit(ann.contentAs());
    }

    @Override
    public Object findDeserializationConverter(Annotated a)
    {
        JsonDeserialize ann = _findAnnotation(a, JsonDeserialize.class);
        return (ann == null) ? null : _classIfExplicit(ann.converter(), Converter.None.class);
    }

    @Override
    public Object findDeserializationContentConverter(AnnotatedMember a)
    {
        JsonDeserialize ann = _findAnnotation(a, JsonDeserialize.class);
        return (ann == null) ? null : _classIfExplicit(ann.contentConverter(), Converter.None.class);
    }

    /*
    /**********************************************************
    /* Deserialization: Class annotations
    /**********************************************************
     */
    
    @Override
    public Object findValueInstantiator(AnnotatedClass ac)
    {
        JsonValueInstantiator ann = _findAnnotation(ac, JsonValueInstantiator.class);
        // no 'null' marker yet, so:
        return (ann == null) ? null : ann.value();
    }

    @Override
    public Class<?> findPOJOBuilder(AnnotatedClass ac)
    {
        JsonDeserialize ann = _findAnnotation(ac, JsonDeserialize.class);
        return (ann == null) ? null : _classIfExplicit(ann.builder());
    }

    @Override
    public JsonPOJOBuilder.Value findPOJOBuilderConfig(AnnotatedClass ac)
    {
        JsonPOJOBuilder ann = _findAnnotation(ac, JsonPOJOBuilder.class);
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
        String name;

        // @JsonSetter has precedence over @JsonProperty, being more specific
        // @JsonDeserialize implies that there is a property, but no name
        JsonSetter js = _findAnnotation(a, JsonSetter.class);
        if (js != null) {
            name = js.value();
        } else {
            JsonProperty pann = _findAnnotation(a, JsonProperty.class);
            if (pann != null) {
                name = pann.value();
                /* 22-Apr-2014, tatu: Should figure out a better way to do this, but
                 *   it's actually bit tricky to do it more efficiently (meta-annotations
                 *   add more lookups; AnnotationMap costs etc)
                 */
            } else if (_hasAnnotation(a, JsonDeserialize.class)
                    || _hasAnnotation(a, JsonView.class)
                    || _hasAnnotation(a, JsonUnwrapped.class) // [#442]
                    || _hasAnnotation(a, JsonBackReference.class)
                    || _hasAnnotation(a, JsonManagedReference.class)) {
                    name = "";
            } else {
                return null;
            }
        }
        return PropertyName.construct(name);
    }
    
    @Override
    public boolean hasAnySetterAnnotation(AnnotatedMethod am)
    {
        /* No dedicated disabling; regular @JsonIgnore used
         * if needs to be ignored (and if so, is handled prior
         * to this method getting called)
         */
        return _hasAnnotation(am, JsonAnySetter.class);
    }

    @Override
    public boolean hasAnyGetterAnnotation(AnnotatedMethod am)
    {
        /* No dedicated disabling; regular @JsonIgnore used
         * if needs to be ignored (handled separately
         */
        return _hasAnnotation(am, JsonAnyGetter.class);
    }

    @Override
    public boolean hasCreatorAnnotation(Annotated a)
    {
        /* No dedicated disabling; regular @JsonIgnore used
         * if needs to be ignored (and if so, is handled prior
         * to this method getting called)
         */
         JsonCreator ann = _findAnnotation(a, JsonCreator.class);
         return (ann != null && ann.mode() != JsonCreator.Mode.DISABLED);
    }

    @Override
    public JsonCreator.Mode findCreatorBinding(Annotated a) {
        JsonCreator ann = _findAnnotation(a, JsonCreator.class);
        return (ann == null) ? null : ann.mode();
    }

    /*
    /**********************************************************
    /* Helper methods
    /**********************************************************
     */

    protected boolean _isIgnorable(Annotated a)
    {
        JsonIgnore ann = _findAnnotation(a, JsonIgnore.class);
        return (ann != null && ann.value());
    }

    protected Class<?> _classIfExplicit(Class<?> cls) {
        if (cls == null || ClassUtil.isBogusClass(cls)) {
            return null;
        }
        return cls;
    }

    protected Class<?> _classIfExplicit(Class<?> cls, Class<?> implicit) {
        cls = _classIfExplicit(cls);
        return (cls == null || cls == implicit) ? null : cls;
    }

    protected PropertyName _propertyName(String localName, String namespace) {
        if (localName.isEmpty()) {
            return PropertyName.USE_DEFAULT;
        }
        if (namespace == null || namespace.isEmpty()) {
            return PropertyName.construct(localName);
        }
        return PropertyName.construct(localName, namespace);
    }

    /**
     * Helper method called to construct and initialize instance of {@link TypeResolverBuilder}
     * if given annotated element indicates one is needed.
     */
    @SuppressWarnings("deprecation")
    protected TypeResolverBuilder<?> _findTypeResolver(MapperConfig<?> config,
            Annotated ann, JavaType baseType)
    {
        // First: maybe we have explicit type resolver?
        TypeResolverBuilder<?> b;
        JsonTypeInfo info = _findAnnotation(ann, JsonTypeInfo.class);
        JsonTypeResolver resAnn = _findAnnotation(ann, JsonTypeResolver.class);
        
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
        JsonTypeIdResolver idResInfo = _findAnnotation(ann, JsonTypeIdResolver.class);
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

        // 08-Dec-2014, tatu: To deprecated `JsonTypeInfo.None` we need to use other placeholder(s);
        //   and since `java.util.Void` has other purpose (to indicate "deser as null"), we'll instead
        //   use `JsonTypeInfo.class` itself. But any annotation type will actually do, as they have no
        //   valid use (can not instantiate as default)
        if (defaultImpl != JsonTypeInfo.None.class && !defaultImpl.isAnnotation()) {
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
