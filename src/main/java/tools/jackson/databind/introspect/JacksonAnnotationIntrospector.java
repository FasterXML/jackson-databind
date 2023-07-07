package tools.jackson.databind.introspect;

import java.lang.annotation.Annotation;
import java.lang.reflect.MalformedParametersException;
import java.lang.reflect.Parameter;
import java.util.*;

import com.fasterxml.jackson.annotation.*;
import tools.jackson.core.JsonParser;
import tools.jackson.core.Version;
import tools.jackson.databind.*;
import tools.jackson.databind.annotation.*;
import tools.jackson.databind.cfg.HandlerInstantiator;
import tools.jackson.databind.cfg.MapperConfig;
import tools.jackson.databind.ext.beans.JavaBeansAnnotations;
import tools.jackson.databind.jsontype.NamedType;
import tools.jackson.databind.ser.BeanPropertyWriter;
import tools.jackson.databind.ser.VirtualBeanPropertyWriter;
import tools.jackson.databind.ser.impl.AttributePropertyWriter;
import tools.jackson.databind.ser.jackson.RawSerializer;
import tools.jackson.databind.type.MapLikeType;
import tools.jackson.databind.type.TypeFactory;
import tools.jackson.databind.util.*;

/**
 * {@link AnnotationIntrospector} implementation that handles standard
 * Jackson annotations.
 */
public class JacksonAnnotationIntrospector
    extends AnnotationIntrospector
    implements java.io.Serializable
{
    private static final long serialVersionUID = 1L;

    @SuppressWarnings("unchecked")
    private final static Class<? extends Annotation>[] ANNOTATIONS_TO_INFER_SER = (Class<? extends Annotation>[])
            new Class<?>[] {
        JsonSerialize.class,
        JsonView.class,
        JsonFormat.class,
        JsonTypeInfo.class,
        JsonRawValue.class,
        JsonUnwrapped.class,
        JsonBackReference.class,
        JsonManagedReference.class
    };

    @SuppressWarnings("unchecked")
    private final static Class<? extends Annotation>[] ANNOTATIONS_TO_INFER_DESER = (Class<? extends Annotation>[])
            new Class<?>[] {
        JsonDeserialize.class,
        JsonView.class,
        JsonFormat.class,
        JsonTypeInfo.class,
        JsonUnwrapped.class,
        JsonBackReference.class,
        JsonManagedReference.class,
        JsonMerge.class // since 2.9
    };

    // NOTE: To avoid mandatory Module dependency to "java.beans", support for 2
    // annotations is done dynamically.
    private static final JavaBeansAnnotations _javaBeansHelper;
    static {
        JavaBeansAnnotations x = null;
        try {
            x = JavaBeansAnnotations.instance();
        } catch (Throwable t) {
            ExceptionUtil.rethrowIfFatal(t);
        }
        _javaBeansHelper = x;
    }

    /**
     * Since introspection of annotation types is a performance issue in some
     * use cases (rare, but do exist), let's try a simple cache to reduce
     * need for actual meta-annotation introspection.
     *<p>
     * Non-final only because it needs to be re-created after deserialization.
     */
    protected transient LookupCache<String, Boolean> _annotationsInside = new SimpleLookupCache<>(48, 96);

    /*
    /**********************************************************************
    /* Local configuration settings
    /**********************************************************************
     */

    /**
     * See {@link #setConstructorPropertiesImpliesCreator(boolean)} for
     * explanation.
     *<p>
     * Defaults to true.
     */
    protected boolean _cfgConstructorPropertiesImpliesCreator = true;

    /*
    /**********************************************************************
    /* Life-cycle
    /**********************************************************************
     */

    public JacksonAnnotationIntrospector() { }

    @Override
    public Version version() {
        return tools.jackson.databind.cfg.PackageVersion.VERSION;
    }

    protected Object readResolve() {
        if (_annotationsInside == null) {
            _annotationsInside = new SimpleLookupCache<>(48, 96);
        }
        return this;
    }

    /*
    /**********************************************************************
    /* Configuration
    /**********************************************************************
     */

    /**
     * Method for changing behavior of {@link java.beans.ConstructorProperties}:
     * if set to `true`, existence DOES indicate that the given constructor should
     * be considered a creator; `false` that it should NOT be considered a creator
     * without explicit use of <code>JsonCreator</code> annotation.
     *<p>
     * Default setting is `true`
     */
    public JacksonAnnotationIntrospector setConstructorPropertiesImpliesCreator(boolean b)
    {
        _cfgConstructorPropertiesImpliesCreator = b;
        return this;
    }

    /*
    /**********************************************************************
    /* General annotation properties
    /**********************************************************************
     */

    /**
     * Annotations with meta-annotation {@link JacksonAnnotationsInside}
     * are considered bundles.
     */
    @Override
    public boolean isAnnotationBundle(Annotation ann) {
        // 22-Sep-2015, tatu: Caching here has modest effect on JavaSE, and only
        //   mostly in degenerate cases where introspection used more often than
        //   it should (like recreating ObjectMapper once per read/write).
        //   But it may be more beneficial on platforms like Android (should verify)
        final Class<?> type = ann.annotationType();
        final String typeName = type.getName();
        Boolean b = _annotationsInside.get(typeName);
        if (b == null) {
            b = type.getAnnotation(JacksonAnnotationsInside.class) != null;
            _annotationsInside.putIfAbsent(typeName, b);
        }
        return b.booleanValue();
    }

    /*
    /**********************************************************************
    /* General annotations
    /**********************************************************************
     */

    @Override // since 2.16
    public String[] findEnumValues(MapperConfig<?> config, AnnotatedClass annotatedClass,
            Enum<?>[] enumValues, String[] names)
    {
        Map<String, String> enumToPropertyMap = new LinkedHashMap<String, String>();
        for (AnnotatedField field : annotatedClass.fields()) {
            JsonProperty property = field.getAnnotation(JsonProperty.class);
            if (property != null) {
                String propValue = property.value();
                if (propValue != null && !propValue.isEmpty()) {
                    enumToPropertyMap.put(field.getName(), propValue);
                }
            }
        }

        // and then stitch them together if and as necessary
        for (int i = 0, end = enumValues.length; i < end; ++i) {
            String defName = enumValues[i].name();
            String explValue = enumToPropertyMap.get(defName);
            if (explValue != null) {
                names[i] = explValue;
            }
        }
        return names;
    }

    @Override
    public void findEnumAliases(MapperConfig<?> config, AnnotatedClass annotatedClass,
            Enum<?>[] enumValues, String[][] aliasList)
    {
        HashMap<String, String[]> enumToAliasMap = new HashMap<>();
        for (AnnotatedField field : annotatedClass.fields()) {
            JsonAlias alias = field.getAnnotation(JsonAlias.class);
            if (alias != null) {
                enumToAliasMap.putIfAbsent(field.getName(), alias.value());
            }
        }

        for (int i = 0, end = enumValues.length; i < end; ++i) {
            Enum<?> enumValue = enumValues[i];
            aliasList[i] = enumToAliasMap.getOrDefault(enumValue.name(), new String[]{});
        }
    }

    /**
     * Finds the Enum value that should be considered the default value, if possible.
     * <p>
     * This implementation relies on {@link JsonEnumDefaultValue} annotation to determine the default value if present.
     *
     * @param enumCls The Enum class to scan for the default value.
     * @return null if none found or it's not possible to determine one.
     */
    @Override
    public Enum<?> findDefaultEnumValue(MapperConfig<?> config, Class<?> enumCls) {
        return ClassUtil.findFirstAnnotatedEnumValue(enumCls, JsonEnumDefaultValue.class);
    }

    /*
    /**********************************************************************
    /* General class annotations
    /**********************************************************************
     */

    @Override
    public PropertyName findRootName(MapperConfig<?> config, AnnotatedClass ac)
    {
        JsonRootName ann = _findAnnotation(ac, JsonRootName.class);
        if (ann == null) {
            return null;
        }
        String ns = ann.namespace();
        if (ns != null && ns.isEmpty()) {
            ns = null;
        }
        return PropertyName.construct(ann.value(), ns);
    }

    @Override
    public Boolean isIgnorableType(MapperConfig<?> config, AnnotatedClass ac) {
        JsonIgnoreType ignore = _findAnnotation(ac, JsonIgnoreType.class);
        return (ignore == null) ? null : ignore.value();
    }

    @Override
    public JsonIgnoreProperties.Value findPropertyIgnoralByName(MapperConfig<?> config, Annotated a)
    {
        JsonIgnoreProperties v = _findAnnotation(a, JsonIgnoreProperties.class);
        if (v == null) {
            return JsonIgnoreProperties.Value.empty();
        }
        return JsonIgnoreProperties.Value.from(v);
    }

    @Override
    public JsonIncludeProperties.Value findPropertyInclusionByName(MapperConfig<?> config, Annotated a)
    {
        JsonIncludeProperties v = _findAnnotation(a, JsonIncludeProperties.class);
        if (v == null) {
            return JsonIncludeProperties.Value.all();
        }
        return JsonIncludeProperties.Value.from(v);
    }

    @Override
    public Object findFilterId(MapperConfig<?> config, Annotated a) {
        JsonFilter ann = _findAnnotation(a, JsonFilter.class);
        if (ann != null) {
            String id = ann.value();
            // Empty String is same as not having annotation, to allow overrides
            if (!id.isEmpty()) {
                return id;
            }
        }
        return null;
    }

    @Override
    public Object findNamingStrategy(MapperConfig<?> config, AnnotatedClass ac)
    {
        JsonNaming ann = _findAnnotation(ac, JsonNaming.class);
        return (ann == null) ? null : ann.value();
    }

    @Override
    public Object findEnumNamingStrategy(MapperConfig<?> config, AnnotatedClass ac) {
        EnumNaming ann = _findAnnotation(ac, EnumNaming.class);
        return (ann == null) ? null : ann.value();
    }

    @Override
    public String findClassDescription(MapperConfig<?> config, AnnotatedClass ac) {
        JsonClassDescription ann = _findAnnotation(ac, JsonClassDescription.class);
        return (ann == null) ? null : ann.value();
    }

    /*
    /**********************************************************************
    /* Property auto-detection
    /**********************************************************************
     */

    @Override
    public VisibilityChecker findAutoDetectVisibility(MapperConfig<?> config,
            AnnotatedClass ac, VisibilityChecker checker)
    {
        JsonAutoDetect ann = _findAnnotation(ac, JsonAutoDetect.class);
        if (ann == null) {
            return checker;
        }
        return checker.withOverrides(JsonAutoDetect.Value.from(ann));
    }

    /*
    /**********************************************************************
    /* General member (field, method/constructor) annotations
    /**********************************************************************
     */

    @Override
    public String findImplicitPropertyName(MapperConfig<?> config, AnnotatedMember m)
    {
        // Always get name for fields so why not
        if (m instanceof AnnotatedField) {
            return m.getName();
        }
        if (m instanceof AnnotatedParameter) {
            AnnotatedParameter p = (AnnotatedParameter) m;
            AnnotatedWithParams owner = p.getOwner();
            if (owner instanceof AnnotatedConstructor) {
                if (_javaBeansHelper != null) {
                    PropertyName name = _javaBeansHelper.findConstructorName(p);
                    if (name != null) {
                        return name.getSimpleName();
                    }
                }
                // ... or parameter names from bytecode (JDK8)
                return _findImplicitName(owner, p.getIndex());
            }
            if (owner instanceof AnnotatedMethod) {
                // For now let's only bother discovering names for static methods as they
                // (only) may be creators
                if (owner.isStatic()) {
                    return _findImplicitName(owner, p.getIndex());
                }
            }
        }
        return null;
    }

    protected String _findImplicitName(AnnotatedWithParams m, int index)
    {
        try {
            Parameter[] params = m.getNativeParameters();
            Parameter p = params[index];
            if (p.isNamePresent()) {
                return p.getName();
            }
        } catch (MalformedParametersException e) {
            // 17-Sep-2017, tatu: I don't usually add defensive handling like this without
            //    having clear examples of problems, but this seems like something that
            //    can still crop up unexpectedly and be a PITA so...
        }
        return null;
    }

    @Override
    public List<PropertyName> findPropertyAliases(MapperConfig<?> config, Annotated m) {
        JsonAlias ann = _findAnnotation(m, JsonAlias.class);
        if (ann == null) {
            return null;
        }
        String[] strs = ann.value();
        final int len = strs.length;
        if (len == 0) {
            return Collections.emptyList();
        }
        List<PropertyName> result = new ArrayList<>(len);
        for (int i = 0; i < len; ++i) {
            result.add(PropertyName.construct(strs[i]));
        }
        return result;
    }

    @Override
    public boolean hasIgnoreMarker(MapperConfig<?> config, AnnotatedMember m) {
        return _isIgnorable(m);
    }

    @Override
    public Boolean hasRequiredMarker(MapperConfig<?> config, AnnotatedMember m)
    {
        JsonProperty ann = _findAnnotation(m, JsonProperty.class);
        if (ann != null) {
            return ann.required();
        }
        return null;
    }

    @Override
    public JsonProperty.Access findPropertyAccess(MapperConfig<?> config, Annotated m) {
        JsonProperty ann = _findAnnotation(m, JsonProperty.class);
        if (ann != null) {
            return ann.access();
        }
        return null;
    }

    @Override
    public String findPropertyDescription(MapperConfig<?> config, Annotated ann) {
        JsonPropertyDescription desc = _findAnnotation(ann, JsonPropertyDescription.class);
        return (desc == null) ? null : desc.value();
    }

    @Override
    public Integer findPropertyIndex(MapperConfig<?> config, Annotated ann) {
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
    public String findPropertyDefaultValue(MapperConfig<?> config, Annotated ann) {
        JsonProperty prop = _findAnnotation(ann, JsonProperty.class);
        if (prop == null) {
            return null;
        }
        String str = prop.defaultValue();
        // Since annotations do not allow nulls, need to assume empty means "none"
        return str.isEmpty() ? null : str;
    }

    @Override
    public JsonFormat.Value findFormat(MapperConfig<?> config, Annotated ann) {
        JsonFormat f = _findAnnotation(ann, JsonFormat.class);
        // NOTE: could also just call `JsonFormat.Value.from()` with `null`
        // too, but that returns "empty" instance
        return (f == null)  ? null : JsonFormat.Value.from(f);
    }

    @Override
    public ReferenceProperty findReferenceType(MapperConfig<?> config, AnnotatedMember member)
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
    public NameTransformer findUnwrappingNameTransformer(MapperConfig<?> config, AnnotatedMember member)
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

    @Override // since 2.9
    public JacksonInject.Value findInjectableValue(MapperConfig<?> config, AnnotatedMember m) {
        JacksonInject ann = _findAnnotation(m, JacksonInject.class);
        if (ann == null) {
            return null;
        }
        // Empty String means that we should use name of declared value class.
        JacksonInject.Value v = JacksonInject.Value.from(ann);
        if (!v.hasId()) {
            Object id;
            // slight complication; for setters, type
            if (!(m instanceof AnnotatedMethod)) {
                id = m.getRawType().getName();
            } else {
                AnnotatedMethod am = (AnnotatedMethod) m;
                if (am.getParameterCount() == 0) { // getter
                    id = m.getRawType().getName();
                } else { // setter
                    id = am.getRawParameterType(0).getName();
                }
            }
            v = v.withId(id);
        }
        return v;
    }

    @Override
    public Class<?>[] findViews(MapperConfig<?> config, Annotated a)
    {
        JsonView ann = _findAnnotation(a, JsonView.class);
        return (ann == null) ? null : ann.value();
    }

    @Override
    /**
     * Specific implementation that will use following tie-breaker on
     * given setter parameter types:
     *<ol>
     * <li>If either one is primitive type then either return {@code null}
     *   (both primitives) or one that is primitive (when only primitive)
     *  </li>
     * <li>If only one is of type {@code String}, return that setter
     *  </li>
     * <li>Otherwise return {@code null}
     *  </li>
     * </ol>
     * Returning {@code null} will indicate that resolution could not be done.
     */
    public AnnotatedMethod resolveSetterConflict(MapperConfig<?> config,
            AnnotatedMethod setter1, AnnotatedMethod setter2)
    {
        Class<?> cls1 = setter1.getRawParameterType(0);
        Class<?> cls2 = setter2.getRawParameterType(0);

        // First: prefer primitives over non-primitives
        // 11-Dec-2015, tatu: TODO, perhaps consider wrappers for primitives too?
        if (cls1.isPrimitive()) {
            if (!cls2.isPrimitive()) {
                return setter1;
            }
            // 10-May-2021, tatu: if both primitives cannot decide
            return null;
        } else if (cls2.isPrimitive()) {
            return setter2;
        }

        if (cls1 == String.class) {
            if (cls2 != String.class) {
                return setter1;
            }
        } else if (cls2 == String.class) {
            return setter2;
        }

        return null;
    }

    @Override // since 2.11
    public PropertyName findRenameByField(MapperConfig<?> config,
            AnnotatedField f, PropertyName implName) {
        // Nothing to report, only used by modules. But define just as documentation
        return null;
    }

    /*
    /**********************************************************************
    /* Annotations for Polymorphic Type handling
    /**********************************************************************
     */

    @Override
    public JsonTypeInfo.Value findPolymorphicTypeInfo(MapperConfig<?> config,
            Annotated ann)
    {
        JsonTypeInfo t = _findAnnotation(ann, JsonTypeInfo.class);
        return (t == null) ? null : JsonTypeInfo.Value.from(t);
    }

    @Override
    public Object findTypeResolverBuilder(MapperConfig<?> config,
            Annotated ann) {
        JsonTypeResolver a = _findAnnotation(ann, JsonTypeResolver.class);
        return (a == null) ? a : a.value();
    }

    @Override
    public Object findTypeIdResolver(MapperConfig<?> config, Annotated ann) {
        JsonTypeIdResolver a = _findAnnotation(ann, JsonTypeIdResolver.class);
        return (a == null) ? a : a.value();
    }

    /*
    @Override
    public TypeResolverBuilder<?> findPropertyTypeResolver(MapperConfig<?> config,
            Annotated ann, JavaType baseType, JsonTypeInfo.Value typeInfo)
    {
        // As per definition of @JsonTypeInfo, should only apply to contents of container
        // (collection, map) types, not container types themselves:
        if (baseType.isContainerType() || baseType.isReferenceType()) {
            return null;
        }
        // No per-member type overrides (yet)
        return _findTypeResolver(config, ann, baseType, typeInfo);
    }
    */

    /*
    @Override
    public TypeResolverBuilder<?> findPropertyContentTypeResolver(MapperConfig<?> config,
            Annotated ann, JavaType containerType, JsonTypeInfo.Value typeInfo)
    {
        // First: let's ensure property is a container type: caller should have
        // verified but just to be sure
        if (containerType.getContentType() == null) {
            throw new IllegalArgumentException("Must call method with a container or reference type (got "+containerType+")");
        }
        return _findTypeResolver(config, ann, containerType, typeInfo);
    }
    */

    @Override
    public List<NamedType> findSubtypes(MapperConfig<?> config, Annotated a)
    {
        JsonSubTypes t = _findAnnotation(a, JsonSubTypes.class);
        if (t == null) return null;
        JsonSubTypes.Type[] types = t.value();

        // 02-Aug-2022, tatu: As per [databind#3500], may need to check uniqueness
        //     of names
        if (t.failOnRepeatedNames()) {
            return findSubtypesCheckRepeatedNames(a.getName(), types);
        } else {
            ArrayList<NamedType> result = new ArrayList<NamedType>(types.length);
            for (JsonSubTypes.Type type : types) {
                result.add(new NamedType(type.value(), type.name()));
                // [databind#2761]: alternative set of names to use
                for (String name : type.names()) {
                    result.add(new NamedType(type.value(), name));
                }
            }
            return result;
        }
    }

    // @since 2.14
    private List<NamedType> findSubtypesCheckRepeatedNames(String annotatedTypeName, JsonSubTypes.Type[] types)
    {
        ArrayList<NamedType> result = new ArrayList<NamedType>(types.length);
        Set<String> seenNames = new HashSet<>();
        for (JsonSubTypes.Type type : types) {
            final String typeName = type.name();
            if (!typeName.isEmpty() && seenNames.contains(typeName)) {
                throw new IllegalArgumentException("Annotated type [" + annotatedTypeName + "] got repeated subtype name [" + typeName + "]");
            } else {
                seenNames.add(typeName);
            }
            result.add(new NamedType(type.value(), typeName));

            // [databind#2761]: alternative set of names to use
            for (String altName : type.names()) {
                if (!altName.isEmpty() && seenNames.contains(altName)) {
                    throw new IllegalArgumentException("Annotated type [" + annotatedTypeName + "] got repeated subtype name [" + altName + "]");
                } else {
                    seenNames.add(altName);
                }
                result.add(new NamedType(type.value(), altName));
            }
        }

        return result;
    }

    @Override
    public String findTypeName(MapperConfig<?> config, AnnotatedClass ac)
    {
        JsonTypeName tn = _findAnnotation(ac, JsonTypeName.class);
        return (tn == null) ? null : tn.value();
    }

    @Override
    public Boolean isTypeId(MapperConfig<?> config, AnnotatedMember member) {
        return _hasAnnotation(member, JsonTypeId.class);
    }

    /*
    /**********************************************************
    /* Annotations for Object Id handling
    /**********************************************************
     */

    @Override
    public ObjectIdInfo findObjectIdInfo(MapperConfig<?> config, Annotated ann) {
        JsonIdentityInfo info = _findAnnotation(ann, JsonIdentityInfo.class);
        if (info == null || info.generator() == ObjectIdGenerators.None.class) {
            return null;
        }
        // In future may need to allow passing namespace?
        PropertyName name = PropertyName.construct(info.property());
        return new ObjectIdInfo(name, info.scope(), info.generator(), info.resolver());
    }

    @Override
    public ObjectIdInfo findObjectReferenceInfo(MapperConfig<?> config,
            Annotated ann, ObjectIdInfo objectIdInfo) {
        JsonIdentityReference ref = _findAnnotation(ann, JsonIdentityReference.class);
        if (ref == null) {
            return objectIdInfo;
        }
        if (objectIdInfo == null) {
            objectIdInfo = ObjectIdInfo.empty();
        }
        return objectIdInfo.withAlwaysAsId(ref.alwaysAsId());
    }

    /*
    /**********************************************************************
    /* Serialization: general annotations
    /**********************************************************************
     */

    @Override
    public Object findSerializer(MapperConfig<?> config, Annotated a)
    {
        JsonSerialize ann = _findAnnotation(a, JsonSerialize.class);
        if (ann != null) {
            @SuppressWarnings("rawtypes")
            Class<? extends ValueSerializer> serClass = ann.using();
            if (serClass != ValueSerializer.None.class) {
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
    public Object findKeySerializer(MapperConfig<?> config, Annotated a)
    {
        JsonSerialize ann = _findAnnotation(a, JsonSerialize.class);
        if (ann != null) {
            @SuppressWarnings("rawtypes")
            Class<? extends ValueSerializer> serClass = ann.keyUsing();
            if (serClass != ValueSerializer.None.class) {
                return serClass;
            }
        }
        return null;
    }

    @Override
    public Object findContentSerializer(MapperConfig<?> config, Annotated a)
    {
        JsonSerialize ann = _findAnnotation(a, JsonSerialize.class);
        if (ann != null) {
            @SuppressWarnings("rawtypes")
            Class<? extends ValueSerializer> serClass = ann.contentUsing();
            if (serClass != ValueSerializer.None.class) {
                return serClass;
            }
        }
        return null;
    }

    @Override
    public Object findNullSerializer(MapperConfig<?> config, Annotated a)
    {
        JsonSerialize ann = _findAnnotation(a, JsonSerialize.class);
        if (ann != null) {
            @SuppressWarnings("rawtypes")
            Class<? extends ValueSerializer> serClass = ann.nullsUsing();
            if (serClass != ValueSerializer.None.class) {
                return serClass;
            }
        }
        return null;
    }

    @Override
    public JsonInclude.Value findPropertyInclusion(MapperConfig<?> config, Annotated a)
    {
        JsonInclude inc = _findAnnotation(a, JsonInclude.class);
        JsonInclude.Value value = (inc == null) ? JsonInclude.Value.empty() : JsonInclude.Value.from(inc);
        return value;
    }

    @Override
    public JsonSerialize.Typing findSerializationTyping(MapperConfig<?> config, Annotated a)
    {
        JsonSerialize ann = _findAnnotation(a, JsonSerialize.class);
        return (ann == null) ? null : ann.typing();
    }

    @Override
    public Object findSerializationConverter(MapperConfig<?> config, Annotated a) {
        JsonSerialize ann = _findAnnotation(a, JsonSerialize.class);
        return (ann == null) ? null : _classIfExplicit(ann.converter(), Converter.None.class);
    }

    @Override
    public Object findSerializationContentConverter(MapperConfig<?> config, AnnotatedMember a) {
        JsonSerialize ann = _findAnnotation(a, JsonSerialize.class);
        return (ann == null) ? null : _classIfExplicit(ann.contentConverter(), Converter.None.class);
    }

    /*
    /**********************************************************************
    /* Serialization: type refinements
    /**********************************************************************
     */

    @Override
    public JavaType refineSerializationType(final MapperConfig<?> config,
            final Annotated a, final JavaType baseType)
    {
        JavaType type = baseType;
        final TypeFactory tf = config.getTypeFactory();

        final JsonSerialize jsonSer = _findAnnotation(a, JsonSerialize.class);

        // Ok: start by refining the main type itself; common to all types

        final Class<?> serClass = (jsonSer == null) ? null : _classIfExplicit(jsonSer.as());
        if (serClass != null) {
            if (type.hasRawClass(serClass)) {
                // 30-Nov-2015, tatu: As per [databind#1023], need to allow forcing of
                //    static typing this way
                type = type.withStaticTyping();
            } else {
                Class<?> currRaw = type.getRawClass();
                try {
                    // 11-Oct-2015, tatu: For deser, we call `TypeFactory.constructSpecializedType()`,
                    //   may be needed here too in future?
                    if (serClass.isAssignableFrom(currRaw)) { // common case
                        type = tf.constructGeneralizedType(type, serClass);
                    } else if (currRaw.isAssignableFrom(serClass)) { // specialization, ok as well
                        type = tf.constructSpecializedType(type, serClass);
                    } else if (_primitiveAndWrapper(currRaw, serClass)) {
                        // 27-Apr-2017, tatu: [databind#1592] ignore primitive<->wrapper refinements
                        type = type.withStaticTyping();
                    } else {
                        throw _databindException(
                                String.format("Cannot refine serialization type %s into %s; types not related",
                                        type, serClass.getName()));
                    }
                } catch (IllegalArgumentException iae) {
                    throw _databindException(iae,
                            String.format("Failed to widen type %s with annotation (value %s), from '%s': %s",
                                    type, serClass.getName(), a.getName(), iae.getMessage()));
                }
            }
        }
        // Then further processing for container types

        // First, key type (for Maps, Map-like types):
        if (type.isMapLikeType()) {
            JavaType keyType = type.getKeyType();
            final Class<?> keyClass = (jsonSer == null) ? null : _classIfExplicit(jsonSer.keyAs());
            if (keyClass != null) {
                if (keyType.hasRawClass(keyClass)) {
                    keyType = keyType.withStaticTyping();
                } else {
                    Class<?> currRaw = keyType.getRawClass();
                    try {
                        // 19-May-2016, tatu: As per [databind#1231], [databind#1178] may need to actually
                        //   specialize (narrow) type sometimes, even if more commonly opposite
                        //   is needed.
                        if (keyClass.isAssignableFrom(currRaw)) { // common case
                            keyType = tf.constructGeneralizedType(keyType, keyClass);
                        } else if (currRaw.isAssignableFrom(keyClass)) { // specialization, ok as well
                            keyType = tf.constructSpecializedType(keyType, keyClass);
                        } else if (_primitiveAndWrapper(currRaw, keyClass)) {
                            // 27-Apr-2017, tatu: [databind#1592] ignore primitive<->wrapper refinements
                            keyType = keyType.withStaticTyping();
                        } else {
                            throw _databindException(
                                    String.format("Cannot refine serialization key type %s into %s; types not related",
                                            keyType, keyClass.getName()));
                        }
                    } catch (IllegalArgumentException iae) {
                        throw _databindException(iae,
                                String.format("Failed to widen key type of %s with concrete-type annotation (value %s), from '%s': %s",
                                        type, keyClass.getName(), a.getName(), iae.getMessage()));
                    }
                }
                type = ((MapLikeType) type).withKeyType(keyType);
            }
        }

        JavaType contentType = type.getContentType();
        if (contentType != null) { // collection[like], map[like], array, reference
            // And then value types for all containers:
           final Class<?> contentClass = (jsonSer == null) ? null : _classIfExplicit(jsonSer.contentAs());
           if (contentClass != null) {
               if (contentType.hasRawClass(contentClass)) {
                   contentType = contentType.withStaticTyping();
               } else {
                   // 03-Apr-2016, tatu: As per [databind#1178], may need to actually
                   //   specialize (narrow) type sometimes, even if more commonly opposite
                   //   is needed.
                   Class<?> currRaw = contentType.getRawClass();
                   try {
                       if (contentClass.isAssignableFrom(currRaw)) { // common case
                           contentType = tf.constructGeneralizedType(contentType, contentClass);
                       } else if (currRaw.isAssignableFrom(contentClass)) { // specialization, ok as well
                           contentType = tf.constructSpecializedType(contentType, contentClass);
                       } else if (_primitiveAndWrapper(currRaw, contentClass)) {
                           // 27-Apr-2017, tatu: [databind#1592] ignore primitive<->wrapper refinements
                           contentType = contentType.withStaticTyping();
                       } else {
                           throw _databindException(
                                   String.format("Cannot refine serialization content type %s into %s; types not related",
                                           contentType, contentClass.getName()));
                       }
                   } catch (IllegalArgumentException iae) { // shouldn't really happen
                       throw _databindException(iae,
                               String.format("Internal error: failed to refine value type of %s with concrete-type annotation (value %s), from '%s': %s",
                                       type, contentClass.getName(), a.getName(), iae.getMessage()));
                   }
               }
               type = type.withContentType(contentType);
           }
        }
        return type;
    }

    /*
    /**********************************************************************
    /* Serialization: class annotations
    /**********************************************************************
     */

    @Override
    public String[] findSerializationPropertyOrder(MapperConfig<?> config, AnnotatedClass ac) {
        JsonPropertyOrder order = _findAnnotation(ac, JsonPropertyOrder.class);
        return (order == null) ? null : order.value();
    }

    @Override
    public Boolean findSerializationSortAlphabetically(MapperConfig<?> config, Annotated ann) {
        return _findSortAlpha(ann);
    }

    private final Boolean _findSortAlpha(Annotated ann) {
        JsonPropertyOrder order = _findAnnotation(ann, JsonPropertyOrder.class);
        // 23-Jun-2015, tatu: as per [databind#840], let's only consider
        //  `true` to have any significance.
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
                attrName, type);
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
                propName.getSimpleName(), type);
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
    /**********************************************************************
    /* Serialization: property annotations
    /**********************************************************************
     */

    @Override
    public PropertyName findNameForSerialization(MapperConfig<?> config, Annotated a)
    {
        boolean useDefault = false;
        JsonGetter jg = _findAnnotation(a, JsonGetter.class);
        if (jg != null) {
            String s = jg.value();
            // 04-May-2018, tatu: Should allow for "nameless" `@JsonGetter` too
            if (!s.isEmpty()) {
                return PropertyName.construct(s);
            }
            useDefault = true;
        }
        JsonProperty pann = _findAnnotation(a, JsonProperty.class);
        if (pann != null) {
            // 14-Nov-2020, tatu: "namespace" added in 2.12
            String ns = pann.namespace();
            if (ns != null && ns.isEmpty()) {
                ns = null;
            }
            return PropertyName.construct(pann.value(), ns);
        }
        if (useDefault || _hasOneOf(a, ANNOTATIONS_TO_INFER_SER)) {
            return PropertyName.USE_DEFAULT;
        }
        return null;
    }

    @Override // since 2.12
    public Boolean hasAsKey(MapperConfig<?> config, Annotated a) {
        JsonKey ann = _findAnnotation(a, JsonKey.class);
        if (ann == null) {
            return null;
        }
        return ann.value();
    }

    @Override
    public Boolean hasAsValue(MapperConfig<?> config, Annotated a) {
        JsonValue ann = _findAnnotation(a, JsonValue.class);
        if (ann == null) {
            return null;
        }
        return ann.value();
    }

    @Override
    public Boolean hasAnyGetter(MapperConfig<?> config, Annotated a) {
        JsonAnyGetter ann = _findAnnotation(a, JsonAnyGetter.class);
        if (ann == null) {
            return null;
        }
        return ann.enabled();
    }

    /*
    /**********************************************************************
    /* Deserialization: general annotations
    /**********************************************************************
     */

    @Override
    public Object findDeserializer(MapperConfig<?> config, Annotated a)
    {
        JsonDeserialize ann = _findAnnotation(a, JsonDeserialize.class);
        if (ann != null) {
            @SuppressWarnings("rawtypes")
            Class<? extends ValueDeserializer> deserClass = ann.using();
            if (deserClass != ValueDeserializer.None.class) {
                return deserClass;
            }
        }
        return null;
    }

    @Override
    public Object findKeyDeserializer(MapperConfig<?> config, Annotated a)
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
    public Object findContentDeserializer(MapperConfig<?> config, Annotated a)
    {
        JsonDeserialize ann = _findAnnotation(a, JsonDeserialize.class);
        if (ann != null) {
            @SuppressWarnings("rawtypes")
            Class<? extends ValueDeserializer> deserClass = ann.contentUsing();
            if (deserClass != ValueDeserializer.None.class) {
                return deserClass;
            }
        }
        return null;
    }

    @Override
    public Object findDeserializationConverter(MapperConfig<?> config, Annotated a)
    {
        JsonDeserialize ann = _findAnnotation(a, JsonDeserialize.class);
        return (ann == null) ? null : _classIfExplicit(ann.converter(), Converter.None.class);
    }

    @Override
    public Object findDeserializationContentConverter(MapperConfig<?> config, AnnotatedMember a)
    {
        JsonDeserialize ann = _findAnnotation(a, JsonDeserialize.class);
        return (ann == null) ? null : _classIfExplicit(ann.contentConverter(), Converter.None.class);
    }

    /*
    /**********************************************************************
    /* Deserialization: type modifications
    /**********************************************************************
     */

    @Override
    public JavaType refineDeserializationType(final MapperConfig<?> config,
            final Annotated a, final JavaType baseType)
    {
        JavaType type = baseType;
        final TypeFactory tf = config.getTypeFactory();

        final JsonDeserialize jsonDeser = _findAnnotation(a, JsonDeserialize.class);

        // Ok: start by refining the main type itself; common to all types
        final Class<?> valueClass = (jsonDeser == null) ? null : _classIfExplicit(jsonDeser.as());
        if ((valueClass != null) && !type.hasRawClass(valueClass)
                && !_primitiveAndWrapper(type, valueClass)) {
            try {
                type = tf.constructSpecializedType(type, valueClass);
            } catch (IllegalArgumentException iae) {
                throw _databindException(iae,
                        String.format("Failed to narrow type %s with annotation (value %s), from '%s': %s",
                                type, valueClass.getName(), a.getName(), iae.getMessage()));
            }
        }
        // Then further processing for container types

        // First, key type (for Maps, Map-like types):
        if (type.isMapLikeType()) {
            JavaType keyType = type.getKeyType();
            final Class<?> keyClass = (jsonDeser == null) ? null : _classIfExplicit(jsonDeser.keyAs());
            if ((keyClass != null)
                    && !_primitiveAndWrapper(keyType, keyClass)) {
                try {
                    keyType = tf.constructSpecializedType(keyType, keyClass);
                    type = ((MapLikeType) type).withKeyType(keyType);
                } catch (IllegalArgumentException iae) {
                    throw _databindException(iae,
                            String.format("Failed to narrow key type of %s with concrete-type annotation (value %s), from '%s': %s",
                                    type, keyClass.getName(), a.getName(), iae.getMessage()));
                }
            }
        }
        JavaType contentType = type.getContentType();
        if (contentType != null) { // collection[like], map[like], array, reference
            // And then value types for all containers:
            final Class<?> contentClass = (jsonDeser == null) ? null : _classIfExplicit(jsonDeser.contentAs());
            if ((contentClass != null)
                    && !_primitiveAndWrapper(contentType, contentClass)) {
                try {
                    contentType = tf.constructSpecializedType(contentType, contentClass);
                    type = type.withContentType(contentType);
                } catch (IllegalArgumentException iae) {
                    throw _databindException(iae,
                            String.format("Failed to narrow value type of %s with concrete-type annotation (value %s), from '%s': %s",
                                    type, contentClass.getName(), a.getName(), iae.getMessage()));
                }
            }
        }
        return type;
    }

    /*
    /**********************************************************************
    /* Deserialization: Class annotations
    /**********************************************************************
     */

    @Override
    public Object findValueInstantiator(MapperConfig<?> config, AnnotatedClass ac)
    {
        JsonValueInstantiator ann = _findAnnotation(ac, JsonValueInstantiator.class);
        // no 'null' marker yet, so:
        return (ann == null) ? null : ann.value();
    }

    @Override
    public Class<?> findPOJOBuilder(MapperConfig<?> config, AnnotatedClass ac)
    {
        JsonDeserialize ann = _findAnnotation(ac, JsonDeserialize.class);
        return (ann == null) ? null : _classIfExplicit(ann.builder());
    }

    @Override
    public JsonPOJOBuilder.Value findPOJOBuilderConfig(MapperConfig<?> config, AnnotatedClass ac)
    {
        JsonPOJOBuilder ann = _findAnnotation(ac, JsonPOJOBuilder.class);
        return (ann == null) ? null : new JsonPOJOBuilder.Value(ann);
    }

    /*
    /**********************************************************************
    /* Deserialization: property annotations
    /**********************************************************************
     */

    @Override
    public PropertyName findNameForDeserialization(MapperConfig<?> config, Annotated a)
    {
        // @JsonSetter has precedence over @JsonProperty, being more specific

        boolean useDefault = false;
        JsonSetter js = _findAnnotation(a, JsonSetter.class);
        if (js != null) {
            String s = js.value();
            // 04-May-2018, tatu: Need to allow for "nameless" `@JsonSetter` too
            if (s.isEmpty()) {
                useDefault = true;
            } else {
                return PropertyName.construct(s);
            }
        }
        JsonProperty pann = _findAnnotation(a, JsonProperty.class);
        if (pann != null) {
            // 14-Nov-2020, tatu: "namespace" added in 2.12
            String ns = pann.namespace();
            if (ns != null && ns.isEmpty()) {
                ns = null;
            }
            return PropertyName.construct(pann.value(), ns);
        }
        if (useDefault || _hasOneOf(a, ANNOTATIONS_TO_INFER_DESER)) {
            return PropertyName.USE_DEFAULT;
        }
        return null;
    }

    @Override
    public Boolean hasAnySetter(MapperConfig<?> config, Annotated a) {
        JsonAnySetter ann = _findAnnotation(a, JsonAnySetter.class);
        return (ann == null) ? null : ann.enabled();
    }

    @Override
    public JsonSetter.Value findSetterInfo(MapperConfig<?> config, Annotated a) {
        return JsonSetter.Value.from(_findAnnotation(a, JsonSetter.class));
    }

    @Override
    public Boolean findMergeInfo(MapperConfig<?> config, Annotated a) {
        JsonMerge ann = _findAnnotation(a, JsonMerge.class);
        return (ann == null) ? null : ann.value().asBoolean();
    }

    @Override
    public JsonCreator.Mode findCreatorAnnotation(MapperConfig<?> config, Annotated a) {
        JsonCreator ann = _findAnnotation(a, JsonCreator.class);
        if (ann != null) {
            return ann.mode();
        }
        if (_cfgConstructorPropertiesImpliesCreator
                && config.isEnabled(MapperFeature.INFER_CREATOR_FROM_CONSTRUCTOR_PROPERTIES)
            ) {
            if (_javaBeansHelper != null) {
                Boolean b = _javaBeansHelper.hasCreatorAnnotation(a);
                if ((b != null) && b.booleanValue()) {
                    // 13-Sep-2016, tatu: Judgment call, but I don't think JDK ever implies
                    //    use of delegate; assumes as-properties implicitly
                    return JsonCreator.Mode.PROPERTIES;
                }
            }
        }
        return null;
    }

    /*
    /**********************************************************************
    /* Helper methods
    /**********************************************************************
     */

    protected boolean _isIgnorable(Annotated a)
    {
        JsonIgnore ann = _findAnnotation(a, JsonIgnore.class);
        if (ann != null) {
            return ann.value();
        }
        // From JDK 7/java.beans
        if (_javaBeansHelper != null) {
            Boolean b = _javaBeansHelper.findTransient(a);
            if (b != null) {
                return b.booleanValue();
            }
        }
        return false;
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

    private boolean _primitiveAndWrapper(Class<?> baseType, Class<?> refinement)
    {
        if (baseType.isPrimitive()) {
            return baseType == ClassUtil.primitiveType(refinement);
        }
        if (refinement.isPrimitive()) {
            return refinement == ClassUtil.primitiveType(baseType);
        }
        return false;
    }

    private boolean _primitiveAndWrapper(JavaType baseType, Class<?> refinement)
    {
        if (baseType.isPrimitive()) {
            return baseType.hasRawClass(ClassUtil.primitiveType(refinement));
        }
        if (refinement.isPrimitive()) {
            return refinement == ClassUtil.primitiveType(baseType.getRawClass());
        }
        return false;
    }

    // @since 2.12
    private DatabindException _databindException(String msg) {
        // not optimal as we have no parser/generator/context to pass
        return DatabindException.from((JsonParser) null, msg);
    }

    // @since 2.12
    private DatabindException _databindException(Throwable t, String msg) {
        // not optimal as we have no parser/generator/context to pass
        return DatabindException.from((JsonParser) null, msg, t);
    }
}
