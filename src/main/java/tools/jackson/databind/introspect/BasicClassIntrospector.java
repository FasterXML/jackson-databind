package tools.jackson.databind.introspect;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import tools.jackson.databind.*;
import tools.jackson.databind.cfg.MapperConfig;
import tools.jackson.databind.util.ClassUtil;

public class BasicClassIntrospector
    extends ClassIntrospector
    implements java.io.Serializable
{
    private static final long serialVersionUID = 3L;

    private final static Class<?> CLS_OBJECT = Object.class;
    private final static Class<?> CLS_STRING = String.class;
    private final static Class<?> CLS_NUMBER = Number.class;
    private final static Class<?> CLS_JSON_NODE = JsonNode.class;

    /* We keep a small set of pre-constructed descriptions to use for
     * common non-structured values, such as Numbers and Strings.
     * This is strictly performance optimization to reduce what is
     * usually one-time cost, but seems useful for some cases considering
     * simplicity.
     */
    private final static AnnotatedClass OBJECT_AC = new AnnotatedClass(CLS_OBJECT);
    private final static AnnotatedClass STRING_AC = new AnnotatedClass(CLS_STRING);

    private final static AnnotatedClass BOOLEAN_AC = new AnnotatedClass(Boolean.TYPE);
    private final static AnnotatedClass INT_AC = new AnnotatedClass(Integer.TYPE);
    private final static AnnotatedClass LONG_AC = new AnnotatedClass(Long.TYPE);

    private final static AnnotatedClass NUMBER_AC = new AnnotatedClass(CLS_NUMBER);

    /*
    /**********************************************************************
    /* Configuration
    /**********************************************************************
     */

    protected final MixInResolver _mixInResolver;

    protected final MapperConfig<?> _config;

    /*
    /**********************************************************************
    /* State
    /**********************************************************************
     */

    /**
     * Reuse fully-resolved annotations during a single operation
     */
    protected HashMap<JavaType, AnnotatedClass> _resolvedFullAnnotations;

    // 15-Oct-2019, tatu: No measurable benefit from trying to reuse direct
    //    annotation access.
//    protected HashMap<JavaType, AnnotatedClass> _resolvedDirectAnnotations;

    /**
     * Reuse full bean descriptions for serialization during a single operation
     */
    protected HashMap<JavaType, BasicBeanDescription> _resolvedSerBeanDescs;

    /**
     * Reuse full bean descriptions for serialization during a single operation
     */
    protected HashMap<JavaType, BasicBeanDescription> _resolvedDeserBeanDescs;

    /*
    /**********************************************************************
    /* Life cycle
    /**********************************************************************
     */

    public BasicClassIntrospector() {
        _config = null;
        _mixInResolver = null;
    }

    protected BasicClassIntrospector(MapperConfig<?> config) {
        _config = Objects.requireNonNull(config, "Can not pass null `config`");
        _mixInResolver = config;
    }

    @Override
    public BasicClassIntrospector forMapper() {
        // 14-Oct-2019, tatu: no per-mapper caching used, so just return as-is
        return this;
    }

    @Override
    public BasicClassIntrospector forOperation(MapperConfig<?> config) {
        return new BasicClassIntrospector(config);
    }
    /*
    /**********************************************************************
    /* Factory method impls: annotation resolution
    /**********************************************************************
     */

    @Override
    public AnnotatedClass introspectClassAnnotations(JavaType type)
    {
        AnnotatedClass ac = _findStdTypeDef(type);
        if (ac != null) {
//System.err.println(" AC.introspectClassAnnotations "+type.getRawClass().getSimpleName()+" -> std-def");
            return ac;
        }
        if (_resolvedFullAnnotations == null) {
            _resolvedFullAnnotations = new HashMap<>();
        } else {
            ac = _resolvedFullAnnotations.get(type);
            if (ac != null) {
//System.err.println(" AC.introspectClassAnnotations "+type.getRawClass().getSimpleName()+" -> CACHED");
                return ac;
            }
        }
//System.err.println(" AC.introspectClassAnnotations "+type.getRawClass().getSimpleName()+" -> resolve");
        ac = _resolveAnnotatedClass(type);
        _resolvedFullAnnotations.put(type, ac);
        return ac;
    }

    @Override
    public AnnotatedClass introspectDirectClassAnnotations(JavaType type)
    {
        AnnotatedClass ac = _findStdTypeDef(type);
        return (ac != null) ? ac : _resolveAnnotatedWithoutSuperTypes(type);
    }

    protected AnnotatedClass _resolveAnnotatedClass(JavaType type) {
        return AnnotatedClassResolver.resolve(_config, type, _mixInResolver);
    }

    protected AnnotatedClass _resolveAnnotatedWithoutSuperTypes(JavaType type) {
        return AnnotatedClassResolver.resolveWithoutSuperTypes(_config, type, _mixInResolver);
    }

    /*
    /**********************************************************************
    /* Factory method impls: bean introspection
    /**********************************************************************
     */

    @Override
    public BasicBeanDescription introspectForSerialization(JavaType type)
    {
        // minor optimization: for some JDK types do minimal introspection
        BasicBeanDescription desc = _findStdTypeDesc(type);
        if (desc == null) {
            // As per [databind#550], skip full introspection for some of standard
            // structured types as well
            desc = _findStdJdkCollectionDesc(type);
            if (desc == null) {
                if (_resolvedSerBeanDescs == null) {
                    _resolvedSerBeanDescs = new HashMap<>();
                } else {
                    desc = _resolvedSerBeanDescs.get(type);
                    if (desc != null) {
                        return desc;
                    }
                }
                desc = BasicBeanDescription.forSerialization(collectProperties(type,
                        introspectClassAnnotations(type),
                        true, "set"));
                _resolvedSerBeanDescs.put(type, desc);
            }
        }
        return desc;
    }

    @Override
    public BasicBeanDescription introspectForDeserialization(JavaType type)
    {
        // minor optimization: for some JDK types do minimal introspection
        BasicBeanDescription desc = _findStdTypeDesc(type);
        if (desc == null) {
            // As per [databind#550], skip full introspection for some of standard
            // structured types as well
            desc = _findStdJdkCollectionDesc(type);
            if (desc == null) {
                if (_resolvedDeserBeanDescs == null) {
                    _resolvedDeserBeanDescs = new HashMap<>();
                } else {
                    desc = _resolvedDeserBeanDescs.get(type);
                    if (desc != null) {
                        return desc;
                    }
                }
                desc = BasicBeanDescription.forDeserialization(collectProperties(type,
                        introspectClassAnnotations(type),
                        false, "set"));
                _resolvedDeserBeanDescs.put(type, desc);
            }
        }
        return desc;
    }

    @Override
    public BasicBeanDescription introspectForDeserializationWithBuilder(JavaType type,
            BeanDescription valueTypeDesc)
    {
        // no std JDK types with Builders, so:
        return BasicBeanDescription.forDeserialization(collectPropertiesWithBuilder(type,
                introspectClassAnnotations(type), valueTypeDesc,
                false));
    }

    @Override
    public BasicBeanDescription introspectForCreation(JavaType type)
    {
        BasicBeanDescription desc = _findStdTypeDesc(type);
        if (desc == null) {
            // As per [databind#550], skip full introspection for some of standard
            // structured types as well
            desc = _findStdJdkCollectionDesc(type);
            if (desc == null) {
                desc = BasicBeanDescription.forDeserialization(collectProperties(type,
                        introspectClassAnnotations(type),
                        false, "set"));
            }
        }
        return desc;
    }

    /*
    /**********************************************************************
    /* Overridable helper methods
    /**********************************************************************
     */

    protected POJOPropertiesCollector collectProperties(JavaType type, AnnotatedClass classDef,
            boolean forSerialization, String mutatorPrefix)
    {
        final AccessorNamingStrategy accNaming = type.isRecordType()
                ? _config.getAccessorNaming().forRecord(_config, classDef)
                : _config.getAccessorNaming().forPOJO(_config, classDef);
        return constructPropertyCollector(type, classDef, forSerialization, accNaming);
    }

    protected POJOPropertiesCollector collectPropertiesWithBuilder(JavaType type,
            AnnotatedClass builderClassDef, BeanDescription valueTypeDesc,
            boolean forSerialization)
    {
        final AccessorNamingStrategy accNaming = _config.getAccessorNaming().forBuilder(_config,
                builderClassDef, valueTypeDesc);
        return constructPropertyCollector(type, builderClassDef, forSerialization, accNaming);
    }

    /**
     * Overridable method called for creating {@link POJOPropertiesCollector} instance
     * to use; override is needed if a custom sub-class is to be used.
     */
    protected POJOPropertiesCollector constructPropertyCollector(JavaType type, AnnotatedClass classDef,
            boolean forSerialization, AccessorNamingStrategy accNaming)
    {
        return new POJOPropertiesCollector(_config, forSerialization, type, classDef, accNaming);
    }

    protected BasicBeanDescription _findStdTypeDesc(JavaType type) {
        AnnotatedClass ac = _findStdTypeDef(type);
        return (ac == null) ? null : BasicBeanDescription.forOtherUse(_config, type, ac);
    }

    /**
     * Method called to see if type is one of core JDK types
     * that we have cached for efficiency.
     */
    protected AnnotatedClass _findStdTypeDef(JavaType type)
    {
        final Class<?> rawType = type.getRawClass();
        if (rawType.isPrimitive()) {
            if (rawType == Integer.TYPE) {
                return INT_AC;
            }
            if (rawType == Long.TYPE) {
                return LONG_AC;
            }
            if (rawType == Boolean.TYPE) {
                return BOOLEAN_AC;
            }
        } else if (ClassUtil.isJDKClass(rawType)) {
            if (rawType == CLS_STRING) {
                return STRING_AC;
            }
            // Should be ok to just pass "primitive" info
            if (rawType == Integer.class) {
                return INT_AC;
            }
            if (rawType == Long.class) {
                return LONG_AC;
            }
            if (rawType == Boolean.class) {
                return BOOLEAN_AC;
            }

            if (rawType == CLS_OBJECT) {
                return OBJECT_AC;
            }
            // This mostly matters for "untyped" deserialization
            if (rawType == CLS_NUMBER) {
                return NUMBER_AC;
            }
        } else if (CLS_JSON_NODE.isAssignableFrom(rawType)) {
            return new AnnotatedClass(rawType);
        }
        return null;
    }

    protected BasicBeanDescription _findStdJdkCollectionDesc(JavaType type)
    {
        if (_isStdJDKCollection(type)) {
            return BasicBeanDescription.forOtherUse(_config, type,
                    introspectClassAnnotations(type));
        }
        return null;
    }

    /**
     * Helper method used to decide whether we can omit introspection
     * for members (methods, fields, constructors); we may do so for
     * a limited number of container types JDK provides.
     */
    private boolean _isStdJDKCollection(JavaType type)
    {
        if (!type.isContainerType() || type.isArrayType()) {
            return false;
        }
        Class<?> raw = type.getRawClass();
        if (ClassUtil.isJDKClass(raw)) {
            // 23-Sep-2014, tatu: Should we be conservative here (minimal number
            //    of matches), or ambitious? Let's do latter for now.
            if (Collection.class.isAssignableFrom(raw)
                    || Map.class.isAssignableFrom(raw)) {
                // 28-May-2024, tatu: Complications wrt [databind#4515] / [databind#2795]
                //    mean that partial introspection NOT for inner classes
                if (raw.toString().indexOf('$') > 0) {
                    return false;
                }
                return true;
            }
        }
        return false;
    }
}
