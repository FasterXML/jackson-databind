package com.fasterxml.jackson.databind.introspect;

import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.util.*;

import com.fasterxml.jackson.databind.AnnotationIntrospector;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.cfg.MapperConfig;
import com.fasterxml.jackson.databind.introspect.ClassIntrospector.MixInResolver;
import com.fasterxml.jackson.databind.type.TypeBindings;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.fasterxml.jackson.databind.util.Annotations;
import com.fasterxml.jackson.databind.util.ClassUtil;

public final class AnnotatedClass
    extends Annotated
    implements TypeResolutionContext
{
    private final static Creators NO_CREATORS = new Creators(null,
            Collections.<AnnotatedConstructor>emptyList(),
            Collections.<AnnotatedMethod>emptyList());

    /*
    /**********************************************************
    /* Configuration
    /**********************************************************
     */

    /**
     * @since 2.7
     */
    final protected JavaType _type;

    /**
     * Class for which annotations apply, and that owns other
     * components (constructors, methods)
     */
    final protected Class<?> _class;

    /**
     * Type bindings to use for members of {@link #_class}.
     *
     * @since 2.7
     */
    final protected TypeBindings _bindings;

    /**
     * Ordered set of super classes and interfaces of the
     * class itself: included in order of precedence
     */
    final protected List<JavaType> _superTypes;

    /**
     * Filter used to determine which annotations to gather; used
     * to optimize things so that unnecessary annotations are
     * ignored.
     */
    final protected AnnotationIntrospector _annotationIntrospector;

    /**
     * @since 2.7
     */
    final protected TypeFactory _typeFactory;

    /**
     * Object that knows mapping of mix-in classes (ones that contain
     * annotations to add) with their target classes (ones that
     * get these additional annotations "mixed in").
     */
    final protected MixInResolver _mixInResolver;

    /**
     * Primary mix-in class; one to use for the annotated class
     * itself. Can be null.
     */
    final protected Class<?> _primaryMixIn;

    /**
     * Flag that indicates whether (fulll) annotation resolution should
     * occur: starting with 2.11 is disabled for JDK container types.
     *
     * @since 2.11
     */
    final protected boolean _collectAnnotations;

    /*
    /**********************************************************
    /* Gathered information
    /**********************************************************
     */

    /**
     * Combined list of Jackson annotations that the class has,
     * including inheritable ones from super classes and interfaces
     */
    final protected Annotations _classAnnotations;

    /**
     * @since 2.9
     */
    protected Creators _creators;

    /**
     * Member methods of interest; for now ones with 0 or 1 arguments
     * (just optimization, since others won't be used now)
     */
    protected AnnotatedMethodMap _memberMethods;

    /**
     * Member fields of interest: ones that are either public,
     * or have at least one annotation.
     */
    protected List<AnnotatedField> _fields;

    /**
     * Lazily determined property to see if this is a non-static inner
     * class.
     *
     * @since 2.8.7
     */
    protected transient Boolean _nonStaticInnerClass;

    /*
    /**********************************************************
    /* Life-cycle
    /**********************************************************
     */

    /**
     * Constructor will not do any initializations, to allow for
     * configuring instances differently depending on use cases
     *
     * @param type Fully resolved type; may be `null`, but ONLY if no member fields or
     *    methods are to be accessed
     * @param rawType Type-erased class; pass if no `type` needed or available
     */
    AnnotatedClass(JavaType type, Class<?> rawType, List<JavaType> superTypes,
            Class<?> primaryMixIn, Annotations classAnnotations, TypeBindings bindings,
            AnnotationIntrospector aintr, MixInResolver mir, TypeFactory tf,
            boolean collectAnnotations)
    {
        _type = type;
        _class = rawType;
        _superTypes = superTypes;
        _primaryMixIn = primaryMixIn;
        _classAnnotations = classAnnotations;
        _bindings = bindings;
        _annotationIntrospector = aintr;
        _mixInResolver = mir;
        _typeFactory = tf;
        _collectAnnotations = collectAnnotations;
    }

    @Deprecated // since 2.10
    AnnotatedClass(JavaType type, Class<?> rawType, List<JavaType> superTypes,
            Class<?> primaryMixIn, Annotations classAnnotations, TypeBindings bindings,
            AnnotationIntrospector aintr, MixInResolver mir, TypeFactory tf)
    {
        this(type, rawType, superTypes, primaryMixIn, classAnnotations, bindings,
                aintr, mir, tf, true);
    }

    /**
     * Constructor (only) used for creating primordial simple types (during bootstrapping)
     * and array type placeholders where no fields or methods are needed.
     *
     * @since 2.9
     */
    AnnotatedClass(Class<?> rawType) {
        _type = null;
        _class = rawType;
        _superTypes = Collections.emptyList();
        _primaryMixIn = null;
        _classAnnotations = AnnotationCollector.emptyAnnotations();
        _bindings = TypeBindings.emptyBindings();
        _annotationIntrospector = null;
        _mixInResolver = null;
        _typeFactory = null;
        _collectAnnotations = false;
    }

    /**
     * @deprecated Since 2.9, use methods in {@link AnnotatedClassResolver} instead.
     */
    @Deprecated
    public static AnnotatedClass construct(JavaType type, MapperConfig<?> config) {
        return construct(type, config, (MixInResolver) config);
    }

    /**
     * @deprecated Since 2.9, use methods in {@link AnnotatedClassResolver} instead.
     */
    @Deprecated
    public static AnnotatedClass construct(JavaType type, MapperConfig<?> config,
            MixInResolver mir)
    {
        return AnnotatedClassResolver.resolve(config, type, mir);
    }

    /**
     * Method similar to {@link #construct}, but that will NOT include
     * information from supertypes; only class itself and any direct
     * mix-ins it may have.
     */
    /**
     * @deprecated Since 2.9, use methods in {@link AnnotatedClassResolver} instead.
     */
    @Deprecated
    public static AnnotatedClass constructWithoutSuperTypes(Class<?> raw, MapperConfig<?> config) {
        return constructWithoutSuperTypes(raw, config, config);
    }

    /**
     * @deprecated Since 2.9, use methods in {@link AnnotatedClassResolver} instead.
     */
    @Deprecated
    public static AnnotatedClass constructWithoutSuperTypes(Class<?> raw, MapperConfig<?> config,
            MixInResolver mir)
    {
        return AnnotatedClassResolver.resolveWithoutSuperTypes(config, raw, mir);
    }

    /*
    /**********************************************************
    /* TypeResolutionContext implementation
    /**********************************************************
     */

    @Override
    public JavaType resolveType(Type type) {
        // 06-Sep-2020, tatu: Careful wrt [databind#2846][databind#2821],
        //     call new method added in 2.12
        return _typeFactory.resolveMemberType(type, _bindings);
    }

    /*
    /**********************************************************
    /* Annotated impl
    /**********************************************************
     */

    @Override
    public Class<?> getAnnotated() { return _class; }

    @Override
    public int getModifiers() { return _class.getModifiers(); }

    @Override
    public String getName() { return _class.getName(); }

    @Override
    public <A extends Annotation> A getAnnotation(Class<A> acls) {
        return _classAnnotations.get(acls);
    }

    @Override
    public boolean hasAnnotation(Class<?> acls) {
        return _classAnnotations.has(acls);
    }

    @Override
    public boolean hasOneOf(Class<? extends Annotation>[] annoClasses) {
        return _classAnnotations.hasOneOf(annoClasses);
    }

    @Override
    public Class<?> getRawType() {
        return _class;
    }

    @Override
    @Deprecated
    public Iterable<Annotation> annotations() {
        if (_classAnnotations instanceof AnnotationMap) {
            return ((AnnotationMap) _classAnnotations).annotations();
        } else if (_classAnnotations instanceof AnnotationCollector.OneAnnotation ||
           _classAnnotations instanceof AnnotationCollector.TwoAnnotations) {
            throw new UnsupportedOperationException("please use getAnnotations/ hasAnnotation to check for Annotations");
        }
        return Collections.emptyList();
    }

    @Override
    public JavaType getType() {
        return _type;
    }

    /*
    /**********************************************************
    /* Public API, generic accessors
    /**********************************************************
     */

    public Annotations getAnnotations() {
        return _classAnnotations;
    }

    public boolean hasAnnotations() {
        return _classAnnotations.size() > 0;
    }

    public AnnotatedConstructor getDefaultConstructor() {
        return _creators().defaultConstructor;
    }

    public List<AnnotatedConstructor> getConstructors() {
        return _creators().constructors;
    }

    /**
     * @since 2.9
     */
    public List<AnnotatedMethod> getFactoryMethods() {
        return _creators().creatorMethods;
    }

    /**
     * @deprecated Since 2.9; use {@link #getFactoryMethods} instead.
     */
    @Deprecated
    public List<AnnotatedMethod> getStaticMethods() {
        return getFactoryMethods();
    }

    public Iterable<AnnotatedMethod> memberMethods() {
        return _methods();
    }

    public int getMemberMethodCount() {
        return _methods().size();
    }

    public AnnotatedMethod findMethod(String name, Class<?>[] paramTypes) {
        return _methods().find(name, paramTypes);
    }

    public int getFieldCount() {
        return _fields().size();
    }

    public Iterable<AnnotatedField> fields() {
        return _fields();
    }

    /**
     * @since 2.9
     */
    public boolean isNonStaticInnerClass()
    {
        Boolean B = _nonStaticInnerClass;
        if (B == null) {
            _nonStaticInnerClass = B = ClassUtil.isNonStaticInnerClass(_class);
        }
        return B.booleanValue();
    }

    /*
    /**********************************************************
    /* Lazily-operating accessors
    /**********************************************************
     */

    private final List<AnnotatedField> _fields() {
        List<AnnotatedField> f = _fields;
        if (f == null) {
            // 09-Jun-2017, tatu: _type only null for primordial, placeholder array types.
            if (_type == null) {
                f = Collections.emptyList();
            } else {
                f = AnnotatedFieldCollector.collectFields(_annotationIntrospector,
                        this, _mixInResolver, _typeFactory, _type, _collectAnnotations);
            }
            _fields = f;
        }
        return f;
    }

    private final AnnotatedMethodMap _methods() {
        AnnotatedMethodMap m = _memberMethods;
        if (m == null) {
            // 09-Jun-2017, tatu: _type only null for primordial, placeholder array types.
            //    NOTE: would be great to have light-weight shareable maps; no such impl exists for now
            if (_type == null) {
                m = new AnnotatedMethodMap();
            } else {
                m = AnnotatedMethodCollector.collectMethods(_annotationIntrospector,
                        this,
                        _mixInResolver, _typeFactory,
                        _type, _superTypes, _primaryMixIn, _collectAnnotations);
            }
            _memberMethods = m;
        }
        return m;
    }

    private final Creators _creators() {
        Creators c = _creators;
        if (c == null) {
            if (_type == null) {
                c = NO_CREATORS;
            } else {
                c = AnnotatedCreatorCollector.collectCreators(_annotationIntrospector,
                        _typeFactory,
                        this, _type, _primaryMixIn, _collectAnnotations);
            }
            _creators = c;
        }
        return c;
    }

    /*
    /**********************************************************
    /* Standard method overrides
    /**********************************************************
     */

    @Override
    public String toString() {
        return "[AnnotedClass "+_class.getName()+"]";
    }

    @Override
    public int hashCode() {
        return _class.getName().hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) return true;
        if (!ClassUtil.hasClass(o, getClass())) {
            return false;
        }
        return ((AnnotatedClass) o)._class == _class;
    }

    /*
    /**********************************************************
    /* Helper classes
    /**********************************************************
     */

    public static final class Creators
    {
        /**
         * Default constructor of the annotated class, if it has one.
         */
        public final AnnotatedConstructor defaultConstructor;

        /**
         * Single argument constructors the class has, if any.
         */
        public final List<AnnotatedConstructor> constructors;

        /**
         * Single argument static methods that might be usable
         * as factory methods
         */
        public final List<AnnotatedMethod> creatorMethods;

        public Creators(AnnotatedConstructor defCtor,
                List<AnnotatedConstructor> ctors,
                List<AnnotatedMethod> ctorMethods)
        {
            defaultConstructor = defCtor;
            constructors = ctors;
            creatorMethods = ctorMethods;
        }
    }
}
