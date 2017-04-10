package com.fasterxml.jackson.databind.introspect;

import java.lang.annotation.Annotation;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
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
    final static AnnotationMap[] NO_ANNOTATION_MAPS = new AnnotationMap[0];

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
     */
    AnnotatedClass(JavaType type, Class<?> rawType, List<JavaType> superTypes,
            Class<?> primaryMixIn, Annotations classAnnotations, TypeBindings bindings, 
            AnnotationIntrospector aintr, MixInResolver mir, TypeFactory tf)
    {
        _type = type;
        _class = rawType;
        _classAnnotations = classAnnotations;
        _bindings = bindings;
        _superTypes = superTypes;
        _annotationIntrospector = aintr;
        _typeFactory = tf;
        _mixInResolver = mir;
        _primaryMixIn = primaryMixIn;
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
        return _typeFactory.constructType(type, _bindings);
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

    public AnnotatedConstructor getDefaultConstructor()
    {
        if (_creators == null) {
            _creators = AnnotatedCreatorResolver.resolve(this, _type);
        }
        return _creators.defaultConstructor;
    }

    public List<AnnotatedConstructor> getConstructors()
    {
        if (_creators == null) {
            _creators = AnnotatedCreatorResolver.resolve(this, _type);
        }
        return _creators.constructors;
    }

    public List<AnnotatedMethod> getStaticMethods()
    {
        if (_creators == null) {
            _creators = AnnotatedCreatorResolver.resolve(this, _type);
        }
        return _creators.creatorMethods;
    }

    public Iterable<AnnotatedMethod> memberMethods()
    {
        if (_memberMethods == null) {
            resolveMemberMethods();
        }
        return _memberMethods;
    }

    public int getMemberMethodCount()
    {
        if (_memberMethods == null) {
            resolveMemberMethods();
        }
        return _memberMethods.size();
    }

    public AnnotatedMethod findMethod(String name, Class<?>[] paramTypes)
    {
        if (_memberMethods == null) {
            resolveMemberMethods();
        }
        return _memberMethods.find(name, paramTypes);
    }

    public int getFieldCount() {
        if (_fields == null) {
            resolveFields();
        }
        return _fields.size();
    }

    public Iterable<AnnotatedField> fields()
    {
        if (_fields == null) {
            resolveFields();
        }
        return _fields;
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
    /* Public API, main-level resolution methods
    /**********************************************************
     */

    /**
     * Method for resolving member method information: aggregating all non-static methods
     * and combining annotations (to implement method-annotation inheritance)
     * 
     * @param methodFilter Filter used to determine which methods to include
     */
    private void resolveMemberMethods() {
        _memberMethods = _resolveMemberMethods();
    }

    private AnnotatedMethodMap _resolveMemberMethods()
    {
        AnnotatedMethodMap memberMethods = new AnnotatedMethodMap();
        AnnotatedMethodMap mixins = new AnnotatedMethodMap();
        // first: methods from the class itself
        _addMemberMethods(_class, this, memberMethods, _primaryMixIn, mixins);

        // and then augment these with annotations from super-types:
        for (JavaType type : _superTypes) {
            Class<?> mixin = (_mixInResolver == null) ? null : _mixInResolver.findMixInClassFor(type.getRawClass());
            _addMemberMethods(type.getRawClass(),
                    new TypeResolutionContext.Basic(_typeFactory, type.getBindings()),
                    memberMethods, mixin, mixins);
        }
        // Special case: mix-ins for Object.class? (to apply to ALL classes)
        if (_mixInResolver != null) {
            Class<?> mixin = _mixInResolver.findMixInClassFor(Object.class);
            if (mixin != null) {
                _addMethodMixIns(_class, memberMethods, mixin, mixins);
            }
        }

        /* Any unmatched mix-ins? Most likely error cases (not matching
         * any method); but there is one possible real use case:
         * exposing Object#hashCode (alas, Object#getClass can NOT be
         * exposed)
         */
        // 14-Feb-2011, tatu: AnnotationIntrospector is null if annotations not enabled; if so, can skip:
        if (_annotationIntrospector != null) {
            if (!mixins.isEmpty()) {
                Iterator<AnnotatedMethod> it = mixins.iterator();
                while (it.hasNext()) {
                    AnnotatedMethod mixIn = it.next();
                    try {
                        Method m = Object.class.getDeclaredMethod(mixIn.getName(), mixIn.getRawParameterTypes());
                        if (m != null) {
                            // Since it's from java.lang.Object, no generics, no need for real type context:
                            AnnotatedMethod am = _constructMethod(m, this);
                            _addMixOvers(mixIn.getAnnotated(), am, false);
                            memberMethods.add(am);
                        }
                    } catch (Exception e) { }
                }
            }
        }
        return memberMethods;
    }

    /**
     * Method that will collect all member (non-static) fields
     * that are either public, or have at least a single annotation
     * associated with them.
     */
    private void resolveFields()
    {
        Map<String,AnnotatedField> foundFields = _findFields(_type, this, null);
        List<AnnotatedField> f;
        if (foundFields == null || foundFields.size() == 0) {
            f = Collections.emptyList();
        } else {
            f = new ArrayList<AnnotatedField>(foundFields.size());
            f.addAll(foundFields.values());
        }
        _fields = f;
    }

    /*
    /**********************************************************
    /* Helper methods for populating method information
    /**********************************************************
     */

    protected void _addMemberMethods(Class<?> cls, TypeResolutionContext typeContext,
            AnnotatedMethodMap methods,
            Class<?> mixInCls, AnnotatedMethodMap mixIns)
    {
        // first, mixIns, since they have higher priority then class methods
        if (mixInCls != null) {
            _addMethodMixIns(cls, methods, mixInCls, mixIns);
        }
        if (cls == null) { // just so caller need not check when passing super-class
            return;
        }
        // then methods from the class itself
        for (Method m : _findClassMethods(cls)) {
            if (!_isIncludableMemberMethod(m)) {
                continue;
            }
            AnnotatedMethod old = methods.find(m);
            if (old == null) {
                AnnotatedMethod newM = _constructMethod(m, typeContext);
                methods.add(newM);
                // Ok, but is there a mix-in to connect now?
                old = mixIns.remove(m);
                if (old != null) {
                    _addMixOvers(old.getAnnotated(), newM, false);
                }
            } else {
                /* If sub-class already has the method, we only want to augment
                 * annotations with entries that are not masked by sub-class.
                 */
                _addMixUnders(m, old);

                /* 06-Jan-2010, tatu: [JACKSON-450] Except that if method we saw first is
                 *   from an interface, and we now find a non-interface definition, we should
                 *   use this method, but with combination of annotations.
                 *   This helps (or rather, is essential) with JAXB annotations and
                 *   may also result in faster method calls (interface calls are slightly
                 *   costlier than regular method calls)
                 */
                if (old.getDeclaringClass().isInterface() && !m.getDeclaringClass().isInterface()) {
                    methods.add(old.withMethod(m));
                }
            }
        }
    }

    protected void _addMethodMixIns(Class<?> targetClass, AnnotatedMethodMap methods,
            Class<?> mixInCls, AnnotatedMethodMap mixIns)
    {
//        List<Class<?>> parents = ClassUtil.findSuperClasses(mixInCls, targetClass, true);

        List<Class<?>> parents = ClassUtil.findRawSuperTypes(mixInCls, targetClass, true);
        for (Class<?> mixin : parents) {
            for (Method m : ClassUtil.getDeclaredMethods(mixin)) {
                if (!_isIncludableMemberMethod(m)) {
                    continue;
                }
                AnnotatedMethod am = methods.find(m);
                /* Do we already have a method to augment (from sub-class
                 * that will mask this mixIn)? If so, add if visible
                 * without masking (no such annotation)
                 */
                if (am != null) {
                    _addMixUnders(m, am);
                    /* Otherwise will have precedence, but must wait
                     * until we find the real method (mixIn methods are
                     * just placeholder, can't be called)
                     */
                } else {
                    // Well, or, as per [databind#515], multi-level merge within mixins...
                    am = mixIns.find(m);
                    if (am != null) {
                        _addMixUnders(m, am);
                    } else {
                        // 03-Nov-2015, tatu: Mix-in method never called, should not need
                        //    to resolve generic types, so this class is fine as context
                        mixIns.add(_constructMethod(m, this));
                    }
                }
            }
        }
    }

    /*
    /**********************************************************
    /* Helper methods for populating field information
    /**********************************************************
     */

    protected Map<String,AnnotatedField> _findFields(JavaType type,
            TypeResolutionContext typeContext, Map<String,AnnotatedField> fields)
    {
        /* First, a quick test: we only care for regular classes (not
         * interfaces, primitive types etc), except for Object.class.
         * A simple check to rule out other cases is to see if there
         * is a super class or not.
         */
        JavaType parent = type.getSuperClass();
        if (parent != null) {
            final Class<?> cls = type.getRawClass();
            // Let's add super-class' fields first, then ours.
            fields = _findFields(parent,
                    new TypeResolutionContext.Basic(_typeFactory, parent.getBindings()),
                    fields);
            for (Field f : ClassUtil.getDeclaredFields(cls)) {
                // static fields not included (transients are at this point, filtered out later)
                if (!_isIncludableField(f)) {
                    continue;
                }
                /* Ok now: we can (and need) not filter out ignorable fields
                 * at this point; partly because mix-ins haven't been
                 * added, and partly because logic can be done when
                 * determining get/settability of the field.
                 */
                if (fields == null) {
                    fields = new LinkedHashMap<String,AnnotatedField>();
                }
                fields.put(f.getName(), _constructField(f, typeContext));
            }
            // And then... any mix-in overrides?
            if (_mixInResolver != null) {
                Class<?> mixin = _mixInResolver.findMixInClassFor(cls);
                if (mixin != null) {
                    _addFieldMixIns(mixin, cls, fields);
                }
            }
        }
        return fields;
    }

    /**
     * Method called to add field mix-ins from given mix-in class (and its fields)
     * into already collected actual fields (from introspected classes and their
     * super-classes)
     */
    protected void _addFieldMixIns(Class<?> mixInCls, Class<?> targetClass,
            Map<String,AnnotatedField> fields)
    {
        List<Class<?>> parents = ClassUtil.findSuperClasses(mixInCls, targetClass, true);
        for (Class<?> mixin : parents) {
            for (Field mixinField : ClassUtil.getDeclaredFields(mixin)) {
                // there are some dummy things (static, synthetic); better ignore
                if (!_isIncludableField(mixinField)) {
                    continue;
                }
                String name = mixinField.getName();
                // anything to mask? (if not, quietly ignore)
                AnnotatedField maskedField = fields.get(name);
                if (maskedField != null) {
                    _addOrOverrideAnnotations(maskedField, mixinField.getDeclaredAnnotations());
                }
            }
        }
    }

    /*
    /**********************************************************
    /* Helper methods, constructing value types
    /**********************************************************
     */

    protected AnnotatedMethod _constructMethod(Method m, TypeResolutionContext typeContext)
    {
        /* note: parameter annotations not used for regular (getter, setter)
         * methods; only for creator methods (static factory methods)
         * -- at least not yet!
         */
        if (_annotationIntrospector == null) { // when annotation processing is disabled
            return new AnnotatedMethod(typeContext, m, _emptyAnnotationMap(), null);
        }
        return new AnnotatedMethod(typeContext, m, _collectRelevantAnnotations(m.getDeclaredAnnotations()), null);
    }

    protected AnnotatedField _constructField(Field f, TypeResolutionContext typeContext)
    {
        if (_annotationIntrospector == null) { // when annotation processing is disabled
            return new AnnotatedField(typeContext, f, _emptyAnnotationMap());
        }
        return new AnnotatedField(typeContext, f, _collectRelevantAnnotations(f.getDeclaredAnnotations()));
    }
 
    private AnnotationMap _collectRelevantAnnotations(Annotation[] anns) {
        return _addAnnotationsIfNotPresent(_annotationIntrospector, new AnnotationMap(), anns);
    }

    static AnnotationMap _emptyAnnotationMap() {
        return new AnnotationMap();
    }

    static AnnotationMap[] _emptyAnnotationMaps(int count) {
        if (count == 0) {
            return NO_ANNOTATION_MAPS;
        }
        AnnotationMap[] maps = new AnnotationMap[count];
        for (int i = 0; i < count; ++i) {
            maps[i] = _emptyAnnotationMap();
        }
        return maps;
    }
    
    /*
    /**********************************************************
    /* Helper methods, inclusion filtering
    /**********************************************************
     */

    protected boolean _isIncludableMemberMethod(Method m)
    {
        if (Modifier.isStatic(m.getModifiers())) {
            return false;
        }
        /* 07-Apr-2009, tatu: Looks like generics can introduce hidden
         *   bridge and/or synthetic methods. I don't think we want to
         *   consider those...
         */
        if (m.isSynthetic() || m.isBridge()) {
            return false;
        }
        // also, for now we have no use for methods with 2 or more arguments:
        int pcount = m.getParameterTypes().length;
        return (pcount <= 2);
    }

    private boolean _isIncludableField(Field f)
    {
        // Most likely synthetic fields, if any, are to be skipped similar to methods
        if (f.isSynthetic()) {
            return false;
        }
        // Static fields are never included. Transient are (since 2.6), for
        // purpose of propagating removal
        int mods = f.getModifiers();
        if (Modifier.isStatic(mods)) {
            return false;
        }
        return true;
    }

    /*
    /**********************************************************
    /* Helper methods, attaching annotations
    /**********************************************************
     */

    /**
     * @param addParamAnnotations Whether parameter annotations are to be
     *   added as well
     */
    private void _addMixOvers(Method mixin, AnnotatedMethod target,
            boolean addParamAnnotations)
    {
        _addOrOverrideAnnotations(target, mixin.getDeclaredAnnotations());
        if (addParamAnnotations) {
            Annotation[][] pa = mixin.getParameterAnnotations();
            for (int i = 0, len = pa.length; i < len; ++i) {
                for (Annotation a : pa[i]) {
                    target.addOrOverrideParam(i, a);
                }
            }
        }
    }

    /**
     * Method that will add annotations from specified source method to target method,
     * but only if target does not yet have them.
     */
    private void _addMixUnders(Method src, AnnotatedMethod target) {
        _addAnnotationsIfNotPresent(_annotationIntrospector, target, src.getDeclaredAnnotations());
    }

    private void _addOrOverrideAnnotations(AnnotatedMember target, Annotation[] anns)
    {
        if (anns == null) {
            return;
        }
        List<Annotation> fromBundles = null;
        for (Annotation ann : anns) { // first: direct annotations
            boolean wasModified = target.addOrOverride(ann);
            if (wasModified && _annotationIntrospector.isAnnotationBundle(ann)) {
                fromBundles = _addFromBundle(ann, fromBundles);
            }
        }
        if (fromBundles != null) { // and then bundles, if any: important for precedence
            _addOrOverrideAnnotations(target, fromBundles.toArray(new Annotation[fromBundles.size()]));
        }
    }

    /*
    /**********************************************************
    /* Static helper methods, attaching annotations
    /**********************************************************
     */

    // Helper method used to add all applicable annotations from given set.
    // Takes into account possible "annotation bundles" (meta-annotations to
    // include instead of main-level annotation)
    private static AnnotationMap _addAnnotationsIfNotPresent(AnnotationIntrospector intr,
            AnnotationMap result, Annotation[] anns)
    {
        if (anns != null) {
            List<Annotation> fromBundles = null;
            for (Annotation ann : anns) { // first: direct annotations
                // note: we will NOT filter out non-Jackson anns any more
                boolean wasNotPresent = result.addIfNotPresent(ann);
                if (wasNotPresent && intr.isAnnotationBundle(ann)) {
                    fromBundles = _addFromBundle(ann, fromBundles);
                }
            }
            if (fromBundles != null) { // and secondarily handle bundles, if any found: precedence important
                _addAnnotationsIfNotPresent(intr,
                        result, fromBundles.toArray(new Annotation[fromBundles.size()]));
            }
        }
        return result;
    }

    private static List<Annotation> _addFromBundle(Annotation bundle, List<Annotation> result)
    {
        for (Annotation a : ClassUtil.findClassAnnotations(bundle.annotationType())) {
            // minor optimization: by-pass 2 common JDK meta-annotations
            if ((a instanceof Target) || (a instanceof Retention)) {
                continue;
            }
            if (result == null) {
                result = new ArrayList<Annotation>();
            }
            result.add(a);
        }
        return result;
    }
    
    private static void _addAnnotationsIfNotPresent(AnnotationIntrospector intr,
            AnnotatedMethod target, Annotation[] anns)
    {
        if (anns == null) {
            return;
        }
        List<Annotation> fromBundles = null;
        for (Annotation ann : anns) { // first: direct annotations
            boolean wasNotPresent = target.addIfNotPresent(ann);
            if (wasNotPresent && intr.isAnnotationBundle(ann)) {
                fromBundles = _addFromBundle(ann, fromBundles);
            }
        }
        if (fromBundles != null) { // and secondarily handle bundles, if any found: precedence important
            _addAnnotationsIfNotPresent(intr,
                    target, fromBundles.toArray(new Annotation[fromBundles.size()]));
        }
    }

    /**
     * Helper method that gets methods declared in given class; usually a simple thing,
     * but sometimes (as per [databind#785]) more complicated, depending on classloader
     * setup.
     *
     * @since 2.5
     */
    protected static Method[] _findClassMethods(Class<?> cls)
    {
        try {
            return ClassUtil.getDeclaredMethods(cls);
        } catch (final NoClassDefFoundError ex) {
            // One of the methods had a class that was not found in the cls.getClassLoader.
            // Maybe the developer was nice and has a different class loader for this context.
            final ClassLoader loader = Thread.currentThread().getContextClassLoader();
            if (loader == null){
                // Nope... this is going to end poorly
                throw ex;
            }
            final Class<?> contextClass;
            try {
                contextClass = loader.loadClass(cls.getName());
            } catch (ClassNotFoundException e) {
                ex.addSuppressed(e);
                throw ex;
            }
            return contextClass.getDeclaredMethods(); // Cross fingers
        }
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
