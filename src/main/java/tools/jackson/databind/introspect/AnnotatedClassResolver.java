package tools.jackson.databind.introspect;

import java.lang.annotation.Annotation;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.util.*;

import tools.jackson.databind.AnnotationIntrospector;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.cfg.MapperConfig;
import tools.jackson.databind.type.TypeBindings;
import tools.jackson.databind.util.Annotations;
import tools.jackson.databind.util.ClassUtil;

/**
 * Helper class that contains logic for resolving annotations to construct
 * {@link AnnotatedClass} instances.
 */
public class AnnotatedClassResolver
{
    private final static Annotation[] NO_ANNOTATIONS = new Annotation[0];
    private final static Annotations EMPTY_ANNOTATIONS = AnnotationCollector.emptyAnnotations();

    private final static Class<?> CLS_OBJECT = Object.class;
    private final static Class<?> CLS_ENUM = Enum.class;

    private final static Class<?> CLS_LIST = List.class;
    private final static Class<?> CLS_MAP = Map.class;

    private final MapperConfig<?> _config;
    private final AnnotationIntrospector _intr;
    private final MixInResolver _mixInResolver;
    private final TypeBindings _bindings;

    private final JavaType _type;
    private final Class<?> _class;
    private final Class<?> _primaryMixin;

    private final boolean _collectAnnotations;

    /*
    /**********************************************************************
    /* Life-cycle
    /**********************************************************************
     */

    AnnotatedClassResolver(MapperConfig<?> config, JavaType type, MixInResolver r) {
        _config = Objects.requireNonNull(config, "Can not pass null `config`");
        _type = type;
        _class = type.getRawClass();
        _mixInResolver = r;
        _bindings = type.getBindings();
        _intr = config.isAnnotationProcessingEnabled()
                ? config.getAnnotationIntrospector() : null;
        _primaryMixin = (r == null) ? null : r.findMixInClassFor(_class);

        // Also... JDK types do not have annotations that are of interest to us
        // At least JDK container types
        _collectAnnotations = (_intr != null) &&
                (!ClassUtil.isJDKClass(_class) || !_type.isContainerType());
    }

    AnnotatedClassResolver(MapperConfig<?> config, Class<?> cls, MixInResolver r) {
        _config = Objects.requireNonNull(config, "Can not pass null `config`");
        _type = null;
        _class = cls;
        _mixInResolver = r;
        _bindings = TypeBindings.emptyBindings();
        _intr = config.isAnnotationProcessingEnabled()
                ? config.getAnnotationIntrospector() : null;
        _primaryMixin = (r == null) ? null : r.findMixInClassFor(_class);

        _collectAnnotations = (_intr != null);
    }

    /*
    /**********************************************************************
    /* Public static API
    /**********************************************************************
     */
    
    public static AnnotatedClass resolve(MapperConfig<?> config, JavaType forType,
            MixInResolver r)
    {
        if (forType.isArrayType() && skippableArray(config, forType.getRawClass())) {
            return createArrayType(config, forType.getRawClass());
        }
        return new AnnotatedClassResolver(config, forType, r).resolveFully();
    }

    public static AnnotatedClass resolveWithoutSuperTypes(MapperConfig<?> config, Class<?> forType) {
        return resolveWithoutSuperTypes(config, forType, config);
    }

    public static AnnotatedClass resolveWithoutSuperTypes(MapperConfig<?> config, JavaType forType,
            MixInResolver r)
    {
        if (forType.isArrayType() && skippableArray(config, forType.getRawClass())) {
            return createArrayType(config, forType.getRawClass());
        }
        return new AnnotatedClassResolver(config, forType, r).resolveWithoutSuperTypes();
    }

    public static AnnotatedClass resolveWithoutSuperTypes(MapperConfig<?> config, Class<?> forType,
            MixInResolver r)
    {
        if (forType.isArray() && skippableArray(config, forType)) {
            return createArrayType(config, forType);
        }
        return new AnnotatedClassResolver(config, forType, r).resolveWithoutSuperTypes();
    }

    private static boolean skippableArray(MapperConfig<?> config, Class<?> type) {
        return (config == null) || (config.findMixInClassFor(type) == null);
    }

    /**
     * Internal helper method used for resolving array types, unless they happen
     * to have associated mix-in to apply.
     */
    static AnnotatedClass createArrayType(MapperConfig<?> config, Class<?> raw) {
        return new AnnotatedClass(raw);
    }

    /*
    /**********************************************************************
    /* Main resolution methods
    /**********************************************************************
     */
    
    AnnotatedClass resolveFully() {
        final List<JavaType> superTypes;

        // 15-Oct-2019, tatu: Can avoid hierarchy traversal in some cases; but best
        //    caught considering starting point and not the middle
        if (!_collectAnnotations && // that is, JDK types
            ((_class == CLS_LIST) || (_class == CLS_MAP))) {
            superTypes = Collections.emptyList();
        } else {
            superTypes = new ArrayList<>(8);
            if (_type.isInterface()) {
                _addSuperInterfaces(_type, superTypes, false);
            } else {
                if (!_type.hasRawClass(Object.class)) {
                    _addSuperTypes(_type, superTypes, false);
                }
            }
        }
//System.err.println(" resolveFully("+_type.getRawClass().getSimpleName()+") -> "+superTypes);        
        return new AnnotatedClass(_config, _type, _class, superTypes, _primaryMixin,
                resolveClassAnnotations(superTypes),
                _bindings, _mixInResolver,
                _collectAnnotations);

    }

    AnnotatedClass resolveWithoutSuperTypes() {
        List<JavaType> superTypes = Collections.emptyList();
        return new AnnotatedClass(_config, null, _class, superTypes, _primaryMixin,
                resolveClassAnnotations(superTypes),
                _bindings, _mixInResolver,
                _collectAnnotations);
    }

    private static void _addSuperTypes(JavaType type, List<JavaType> result,
            boolean addClassItself)
    {
        if (type == null) {
            return;
        }
        final Class<?> cls = type.getRawClass();

        // 15-Oct-2019, tatu: certain paths do not lead to useful information, so prune
        //    as optimization
        if ((cls == CLS_OBJECT) || (cls == CLS_ENUM)) {
            return;
        }
        
        if (addClassItself) {
            if (_contains(result, cls)) { // already added, no need to check supers
                return;
            }
            result.add(type);
        }
        for (JavaType intCls : type.getInterfaces()) {
            _addSuperInterfaces(intCls, result, true);
        }
        _addSuperTypes(type.getSuperClass(), result, true);
    }

    private static void _addSuperInterfaces(JavaType type, List<JavaType> result,
            boolean addClassItself)
    {
        final Class<?> cls = type.getRawClass();
        if (addClassItself) {
            if (_contains(result, cls)) { // already added, no need to check supers
                return;
            }
            result.add(type);
            // 30-Oct-2019, tatu: Further, no point going beyond some containers
            if ((cls == CLS_LIST) || (cls == CLS_MAP)) {
                return;
            }
        }
        for (JavaType intCls : type.getInterfaces()) {
            _addSuperInterfaces(intCls, result, true);
        }
    }

    private static boolean _contains(List<JavaType> found, Class<?> raw) {
        for (int i = 0, end = found.size(); i < end; ++i) {
            if (found.get(i).getRawClass() == raw) {
                return true;
            }
        }
        return false;
    }

    /*
    /**********************************************************************
    /* Class annotation resolution
    /**********************************************************************
     */

    /**
     * Initialization method that will recursively collect Jackson
     * annotations for this class and all super classes and
     * interfaces.
     */
    private Annotations resolveClassAnnotations(List<JavaType> superTypes)
    {
        // Should skip processing if annotation processing disabled
        if (_intr == null) {
            return EMPTY_ANNOTATIONS;
        }
        // Plus we may or may not have mix-ins to consider
        final boolean checkMixIns = (_mixInResolver != null) && _mixInResolver.hasMixIns();

        // also skip if there's nothing to do
        if (!checkMixIns && !_collectAnnotations) {
            return EMPTY_ANNOTATIONS;
        }

        AnnotationCollector resolvedCA = AnnotationCollector.emptyCollector();
        // add mix-in annotations first (overrides)
        if (_primaryMixin != null) {
            resolvedCA = _addClassMixIns(resolvedCA, _class, _primaryMixin);
        }
        // then annotations from the class itself:
        // 06-Oct-2019, tatu: [databind#2464] Skip class annotations for JDK classes
        if (_collectAnnotations) {
            resolvedCA = _addAnnotationsIfNotPresent(resolvedCA, _findClassAnnotations(_class));
        }

        // and then from super types
        for (JavaType type : superTypes) {
            // and mix mix-in annotations in-between
            if (checkMixIns) {
                Class<?> cls = type.getRawClass();
                resolvedCA = _addClassMixIns(resolvedCA, cls,
                        _mixInResolver.findMixInClassFor(cls));
            }
            if (_collectAnnotations) {
                resolvedCA = _addAnnotationsIfNotPresent(resolvedCA,
                        _findClassAnnotations(type.getRawClass()));
            }
        }

        // and finally... any annotations there might be for plain old Object.class:
        // separate because otherwise it is just ignored (not included in super types)

        // 12-Jul-2009, tatu: Should this be done for interfaces too?
        //  For now, yes, seems useful for some cases, and not harmful for any?
        if (checkMixIns) {
            resolvedCA = _addClassMixIns(resolvedCA, Object.class,
                    _mixInResolver.findMixInClassFor(Object.class));
        }
        return resolvedCA.asAnnotations();
    }

    private AnnotationCollector _addClassMixIns(AnnotationCollector annotations,
            Class<?> target, Class<?> mixin)
    {
        if (mixin != null) {
            // Ok, first: annotations from mix-in class itself:
            annotations = _addAnnotationsIfNotPresent(annotations, _findClassAnnotations(mixin));
    
            // And then from its supertypes, if any. But note that we will only consider
            // super-types up until reaching the masked class (if found); this because
            // often mix-in class is a sub-class (for convenience reasons).
            // And if so, we absolutely must NOT include super types of masked class,
            // as that would inverse precedence of annotations.
            for (Class<?> parent : ClassUtil.findSuperClasses(mixin, target, false)) {
                annotations = _addAnnotationsIfNotPresent(annotations, _findClassAnnotations(parent));
            }
        }
        return annotations;
    }

    private AnnotationCollector _addAnnotationsIfNotPresent(AnnotationCollector c,
            Annotation[] anns)
    {
        if (anns != null) {
            for (Annotation ann : anns) { // first: direct annotations
                // note: we will NOT filter out non-Jackson annotations any more
                if (!c.isPresent(ann)) {
                    c = c.addOrOverride(ann);
                    if (_intr.isAnnotationBundle(ann)) {
                        c = _addFromBundleIfNotPresent(c, ann);
                    }
                }
            }
        }
        return c;
    }

    private AnnotationCollector _addFromBundleIfNotPresent(AnnotationCollector c,
            Annotation bundle)
    {
        for (Annotation ann : _findClassAnnotations(bundle.annotationType())) {
            // minor optimization: by-pass 2 common JDK meta-annotations
            if ((ann instanceof Target) || (ann instanceof Retention)) {
                continue;
            }
            if (!c.isPresent(ann)) {
                c = c.addOrOverride(ann);
                if (_intr.isAnnotationBundle(ann)) {
                    c = _addFromBundleIfNotPresent(c, ann);
                }
            }
        }
        return c;
    }

    /*
    /**********************************************************************
    /* Methods copied from `ClassUtil`
    /**********************************************************************
     */

    private static Annotation[] _findClassAnnotations(Class<?> cls) {
        if (cls == CLS_OBJECT) { // never called with primitive types but might get Object.class
            return NO_ANNOTATIONS;
        }
        return cls.getDeclaredAnnotations();
    }
}
