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
    private final static AnnotationMap[] NO_ANNOTATION_MAPS = new AnnotationMap[0];

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
     *<p>
     * NOTE: changed in 2.7 from List of <code>Class</code>es to List of {@link JavaType}s.
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
    protected AnnotationMap _classAnnotations;

    /**
     * Flag to indicate whether creator information has been resolved
     * or not.
     */
    protected boolean _creatorsResolved = false;
    
    /**
     * Default constructor of the annotated class, if it has one.
     */
    protected AnnotatedConstructor _defaultConstructor;

    /**
     * Single argument constructors the class has, if any.
     */
    protected List<AnnotatedConstructor> _constructors;

    /**
     * Single argument static methods that might be usable
     * as factory methods
     */
    protected List<AnnotatedMethod> _creatorMethods;

    /**
     * Member methods of interest; for now ones with 0 or 1 arguments
     * (just optimization, since others won't be used now)
     */
    protected AnnotatedMethodMap  _memberMethods;

    /**
     * Member fields of interest: ones that are either public,
     * or have at least one annotation.
     */
    protected List<AnnotatedField> _fields;

    /*
    /**********************************************************
    /* Life-cycle
    /**********************************************************
     */

    /**
     * Constructor will not do any initializations, to allow for
     * configuring instances differently depending on use cases
     */
    private AnnotatedClass(JavaType type, Class<?> rawType, TypeBindings bindings,
            List<JavaType> superTypes,
            AnnotationIntrospector aintr, MixInResolver mir, TypeFactory tf,
            AnnotationMap classAnnotations)
    {
        _type = type;
        _class = rawType;
        _bindings = bindings;
        _superTypes = superTypes;
        _annotationIntrospector = aintr;
        _typeFactory = tf;
        _mixInResolver = mir;
        _primaryMixIn = (_mixInResolver == null) ? null
            : _mixInResolver.findMixInClassFor(_class);
        _classAnnotations = classAnnotations;
    }

    @Override
    public AnnotatedClass withAnnotations(AnnotationMap ann) {
        return new AnnotatedClass(_type, _class, _bindings, _superTypes,
                _annotationIntrospector, _mixInResolver, _typeFactory, ann);
    }

    /**
     * Factory method that instantiates an instance. Returned instance
     * will only be initialized with class annotations, but not with
     * any method information.
     * 
     * @since 2.7
     */
    public static AnnotatedClass construct(JavaType type, MapperConfig<?> config) {
        AnnotationIntrospector intr = config.isAnnotationProcessingEnabled()
                ? config.getAnnotationIntrospector() : null;
        Class<?> raw = type.getRawClass();
        return new AnnotatedClass(type, raw, type.getBindings(),
                ClassUtil.findSuperTypes(type, null, false), intr,
                (MixInResolver) config, config.getTypeFactory(), null);
    }

    /**
     * @since 2.7
     */
    public static AnnotatedClass construct(JavaType type, MapperConfig<?> config,
            MixInResolver mir)
    {
        AnnotationIntrospector intr = config.isAnnotationProcessingEnabled()
                ? config.getAnnotationIntrospector() : null;
        Class<?> raw = type.getRawClass();
        return new AnnotatedClass(type, raw, type.getBindings(),
                ClassUtil.findSuperTypes(type, null, false),
                intr, mir, config.getTypeFactory(), null);
    }
    
    /**
     * Method similar to {@link #construct}, but that will NOT include
     * information from supertypes; only class itself and any direct
     * mix-ins it may have.
     */
    public static AnnotatedClass constructWithoutSuperTypes(Class<?> cls, MapperConfig<?> config)
    {
        if (config == null) {
            return new AnnotatedClass(null, cls, TypeBindings.emptyBindings(),
                    Collections.<JavaType>emptyList(), null, null, null, null);
        }
        AnnotationIntrospector intr = config.isAnnotationProcessingEnabled()
                ? config.getAnnotationIntrospector() : null;
        return new AnnotatedClass(null, cls, TypeBindings.emptyBindings(),
                Collections.<JavaType>emptyList(), intr, (MixInResolver) config, config.getTypeFactory(), null);
    }

    public static AnnotatedClass constructWithoutSuperTypes(Class<?> cls, MapperConfig<?> config,
            MixInResolver mir)
    {
        if (config == null) {
            return new AnnotatedClass(null, cls, TypeBindings.emptyBindings(),
                    Collections.<JavaType>emptyList(), null, null, null, null);
        }
        AnnotationIntrospector intr = config.isAnnotationProcessingEnabled()
                ? config.getAnnotationIntrospector() : null;
        return new AnnotatedClass(null, cls, TypeBindings.emptyBindings(),
                Collections.<JavaType>emptyList(), intr, mir, config.getTypeFactory(), null);
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
        return _classAnnotations().get(acls);
    }

    @Override
    public boolean hasAnnotation(Class<?> acls) {
        return _classAnnotations().has(acls);
    }

    @Override
    public boolean hasOneOf(Class<? extends Annotation>[] annoClasses) {
        return _classAnnotations().hasOneOf(annoClasses);
    }

    @Override
    public Class<?> getRawType() {
        return _class;
    }

    @Override
    public Iterable<Annotation> annotations() {
        return _classAnnotations().annotations();
    }
    
    @Override
    protected AnnotationMap getAllAnnotations() {
        return _classAnnotations();
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
        return _classAnnotations();
    }

    public boolean hasAnnotations() {
        return _classAnnotations().size() > 0;
    }

    public AnnotatedConstructor getDefaultConstructor()
    {
        if (!_creatorsResolved) {
            resolveCreators();
        }
        return _defaultConstructor;
    }

    public List<AnnotatedConstructor> getConstructors()
    {
        if (!_creatorsResolved) {
            resolveCreators();
        }
        return _constructors;
    }

    public List<AnnotatedMethod> getStaticMethods()
    {
        if (!_creatorsResolved) {
            resolveCreators();
        }
        return _creatorMethods;
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

    /*
    /**********************************************************
    /* Public API, main-level resolution methods
    /**********************************************************
     */

    private AnnotationMap _classAnnotations() {
        AnnotationMap anns = _classAnnotations;
        if (anns == null) {
            // 06-Dec-2015, tatu: yes, double-locking, typically not a good choice.
            //  But for typical usage pattern here (and with JVM 7 and above) is
            //  a reasonable choice to avoid non-common but existing race condition
            //  from root name lookup style usage
            // Also note that race condition stems from caching only used for loading
            // where just class annotations are needed
            synchronized (this) {
                anns = _classAnnotations;
                if (anns == null) {
                    anns = _resolveClassAnnotations();
                    _classAnnotations = anns;
                }
            }
        }
        return anns;
    }

    /**
     * Initialization method that will recursively collect Jackson
     * annotations for this class and all super classes and
     * interfaces.
     */
    private AnnotationMap _resolveClassAnnotations()
    {
        AnnotationMap ca = new AnnotationMap();
        // Should skip processing if annotation processing disabled
        if (_annotationIntrospector != null) {
            // add mix-in annotations first (overrides)
            if (_primaryMixIn != null) {
                _addClassMixIns(ca, _class, _primaryMixIn);
            }
            // first, annotations from the class itself:
            _addAnnotationsIfNotPresent(ca,
                    ClassUtil.findClassAnnotations(_class));
    
            // and then from super types
            for (JavaType type : _superTypes) {
                // and mix mix-in annotations in-between
                _addClassMixIns(ca, type);
                _addAnnotationsIfNotPresent(ca,
                        ClassUtil.findClassAnnotations(type.getRawClass()));
            }
            /* and finally... any annotations there might be for plain
             * old Object.class: separate because for all other purposes
             * it is just ignored (not included in super types)
             */
            /* 12-Jul-2009, tatu: Should this be done for interfaces too?
             *   For now, yes, seems useful for some cases, and not harmful for any?
             */
            _addClassMixIns(ca, Object.class);
        }
        return ca;
    }

    /**
     * Initialization method that will find out all constructors
     * and potential static factory methods the class has.
     */
    private void resolveCreators()
    {
        // Constructor also always members of this class
        TypeResolutionContext typeContext = this; 

    // 30-Apr-2016, tatu: [databind#1215]: Actually, while true, this does
    //   NOT apply to context since sub-class may have type bindings
//    TypeResolutionContext typeContext = new TypeResolutionContext.Basic(_typeFactory, _type.getBindings());

        // Then see which constructors we have
        List<AnnotatedConstructor> constructors = null;

        // 18-Jun-2016, tatu: Enum constructors will never be useful (unlike
        //    possibly static factory methods); but they can be royal PITA
        //    due to some oddities by JVM; see:
        //    [https://github.com/FasterXML/jackson-module-parameter-names/issues/35]
        //    for more. So, let's just skip them.
        if (!_type.isEnumType()) {
            ClassUtil.Ctor[] declaredCtors = ClassUtil.getConstructors(_class);
            for (ClassUtil.Ctor ctor : declaredCtors) {
                if (_isIncludableConstructor(ctor.getConstructor())) {
                    if (ctor.getParamCount() == 0) {
                        _defaultConstructor = _constructDefaultConstructor(ctor, typeContext);
                    } else {
                        if (constructors == null) {
                            constructors = new ArrayList<AnnotatedConstructor>(Math.max(10, declaredCtors.length));
                        }
                        constructors.add(_constructNonDefaultConstructor(ctor, typeContext));
                    }
                }
            }
        }
        if (constructors == null) {
            _constructors = Collections.emptyList();
        } else {
            _constructors = constructors;
        }
        // and if need be, augment with mix-ins
        if (_primaryMixIn != null) {
            if (_defaultConstructor != null || !_constructors.isEmpty()) {
                _addConstructorMixIns(_primaryMixIn);
            }
        }

        /* And then... let's remove all constructors that are deemed
         * ignorable after all annotations have been properly collapsed.
         */
        // AnnotationIntrospector is null if annotations not enabled; if so, can skip:
        if (_annotationIntrospector != null) {
            if (_defaultConstructor != null) {
                if (_annotationIntrospector.hasIgnoreMarker(_defaultConstructor)) {
                    _defaultConstructor = null;
                }
            }
            if (_constructors != null) {
                // count down to allow safe removal
                for (int i = _constructors.size(); --i >= 0; ) {
                    if (_annotationIntrospector.hasIgnoreMarker(_constructors.get(i))) {
                        _constructors.remove(i);
                    }
                }
            }
        }
        List<AnnotatedMethod> creatorMethods = null;
        
        // Then static methods which are potential factory methods
        for (Method m : _findClassMethods(_class)) {
            if (!Modifier.isStatic(m.getModifiers())) {
                continue;
            }
            // all factory methods are fine:
            //int argCount = m.getParameterTypes().length;
            if (creatorMethods == null) {
                creatorMethods = new ArrayList<AnnotatedMethod>(8);
            }
            creatorMethods.add(_constructCreatorMethod(m, typeContext));
        }
        if (creatorMethods == null) {
            _creatorMethods = Collections.emptyList();
        } else {
            _creatorMethods = creatorMethods;
            // mix-ins to mix in?
            if (_primaryMixIn != null) {
                _addFactoryMixIns(_primaryMixIn);
            }
            // anything to ignore at this point?
            if (_annotationIntrospector != null) {
                // count down to allow safe removal
                for (int i = _creatorMethods.size(); --i >= 0; ) {
                    if (_annotationIntrospector.hasIgnoreMarker(_creatorMethods.get(i))) {
                        _creatorMethods.remove(i);
                    }
                }
            }
        }
        _creatorsResolved = true;
    }

    /**
     * Method for resolving member method information: aggregating all non-static methods
     * and combining annotations (to implement method-annotation inheritance)
     * 
     * @param methodFilter Filter used to determine which methods to include
     */
    private void resolveMemberMethods()
    {
        _memberMethods = new AnnotatedMethodMap();
        AnnotatedMethodMap mixins = new AnnotatedMethodMap();
        // first: methods from the class itself
        _addMemberMethods(_class, this, _memberMethods, _primaryMixIn, mixins);

        // and then augment these with annotations from super-types:
        for (JavaType type : _superTypes) {
            Class<?> mixin = (_mixInResolver == null) ? null : _mixInResolver.findMixInClassFor(type.getRawClass());
            _addMemberMethods(type.getRawClass(),
                    new TypeResolutionContext.Basic(_typeFactory, type.getBindings()),
                    _memberMethods, mixin, mixins);
        }
        // Special case: mix-ins for Object.class? (to apply to ALL classes)
        if (_mixInResolver != null) {
            Class<?> mixin = _mixInResolver.findMixInClassFor(Object.class);
            if (mixin != null) {
                _addMethodMixIns(_class, _memberMethods, mixin, mixins);
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
                            _memberMethods.add(am);
                        }
                    } catch (Exception e) { }
                }
            }
        }
    }
    
    /**
     * Method that will collect all member (non-static) fields
     * that are either public, or have at least a single annotation
     * associated with them.
     */
    private void resolveFields()
    {
        Map<String,AnnotatedField> foundFields = _findFields(_type, this, null);
        if (foundFields == null || foundFields.size() == 0) {
            _fields = Collections.emptyList();
        } else {
            _fields = new ArrayList<AnnotatedField>(foundFields.size());
            _fields.addAll(foundFields.values());
        }
    }
    
    /*
    /**********************************************************
    /* Helper methods for resolving class annotations
    /* (resolution consisting of inheritance, overrides,
    /* and injection of mix-ins as necessary)
    /**********************************************************
     */
    
    /**
     * Helper method for adding any mix-in annotations specified
     * class might have.
     */
    protected void _addClassMixIns(AnnotationMap annotations, JavaType target)
    {
        if (_mixInResolver != null) {
            final Class<?> toMask = target.getRawClass();
            _addClassMixIns(annotations, toMask, _mixInResolver.findMixInClassFor(toMask));
        }
    }

    protected void _addClassMixIns(AnnotationMap annotations, Class<?> target)
    {
        if (_mixInResolver != null) {
            _addClassMixIns(annotations, target, _mixInResolver.findMixInClassFor(target));
        }
    }

    protected void _addClassMixIns(AnnotationMap annotations, Class<?> toMask,
            Class<?> mixin)
    {
        if (mixin == null) {
            return;
        }
        // Ok, first: annotations from mix-in class itself:
        _addAnnotationsIfNotPresent(annotations, ClassUtil.findClassAnnotations(mixin));

        /* And then from its supertypes, if any. But note that we will
         * only consider super-types up until reaching the masked
         * class (if found); this because often mix-in class
         * is a sub-class (for convenience reasons). And if so, we
         * absolutely must NOT include super types of masked class,
         * as that would inverse precedence of annotations.
         */
        for (Class<?> parent : ClassUtil.findSuperClasses(mixin, toMask, false)) {
            _addAnnotationsIfNotPresent(annotations, ClassUtil.findClassAnnotations(parent));
        }
    }

    /*
    /**********************************************************
    /* Helper methods for populating creator (ctor, factory) information
    /**********************************************************
     */

    protected void _addConstructorMixIns(Class<?> mixin)
    {
        MemberKey[] ctorKeys = null;
        int ctorCount = (_constructors == null) ? 0 : _constructors.size();
        for (ClassUtil.Ctor ctor0 : ClassUtil.getConstructors(mixin)) {
            Constructor<?> ctor = ctor0.getConstructor();
            if (ctor.getParameterTypes().length == 0) {
                if (_defaultConstructor != null) {
                    _addMixOvers(ctor, _defaultConstructor, false);
                }
            } else {
                if (ctorKeys == null) {
                    ctorKeys = new MemberKey[ctorCount];
                    for (int i = 0; i < ctorCount; ++i) {
                        ctorKeys[i] = new MemberKey(_constructors.get(i).getAnnotated());
                    }
                }
                MemberKey key = new MemberKey(ctor);

                for (int i = 0; i < ctorCount; ++i) {
                    if (!key.equals(ctorKeys[i])) {
                        continue;
                    }
                    _addMixOvers(ctor, _constructors.get(i), true);
                    break;
                }
            }
        }
    }

    protected void _addFactoryMixIns(Class<?> mixin)
    {
        MemberKey[] methodKeys = null;
        int methodCount = _creatorMethods.size();

        for (Method m : ClassUtil.getDeclaredMethods(mixin)) {
            if (!Modifier.isStatic(m.getModifiers())) {
                continue;
            }
            if (m.getParameterTypes().length == 0) {
                continue;
            }
            if (methodKeys == null) {
                methodKeys = new MemberKey[methodCount];
                for (int i = 0; i < methodCount; ++i) {
                    methodKeys[i] = new MemberKey(_creatorMethods.get(i).getAnnotated());
                }
            }
            MemberKey key = new MemberKey(m);
            for (int i = 0; i < methodCount; ++i) {
                if (!key.equals(methodKeys[i])) {
                    continue;
                }
                _addMixOvers(m, _creatorMethods.get(i), true);
                break;
            }
        }
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
            /* 21-Feb-2010, tatu: Need to handle masking: as per [JACKSON-226]
             *    we otherwise get into trouble...
             */
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

    protected AnnotatedConstructor _constructDefaultConstructor(ClassUtil.Ctor ctor,
            TypeResolutionContext typeContext)
    {
        if (_annotationIntrospector == null) { // when annotation processing is disabled
            return new AnnotatedConstructor(typeContext, ctor.getConstructor(), _emptyAnnotationMap(), NO_ANNOTATION_MAPS);
        }
        return new AnnotatedConstructor(typeContext, ctor.getConstructor(),
                _collectRelevantAnnotations(ctor.getDeclaredAnnotations()), NO_ANNOTATION_MAPS);
    }

    protected AnnotatedConstructor _constructNonDefaultConstructor(ClassUtil.Ctor ctor,
            TypeResolutionContext typeContext)
    {
        final int paramCount = ctor.getParamCount();
        if (_annotationIntrospector == null) { // when annotation processing is disabled
            return new AnnotatedConstructor(typeContext, ctor.getConstructor(),
                    _emptyAnnotationMap(), _emptyAnnotationMaps(paramCount));
        }

        /* Looks like JDK has discrepancy, whereas annotations for implicit 'this'
         * (for non-static inner classes) are NOT included, but type is?
         * Strange, sounds like a bug. Alas, we can't really fix that...
         */
        if (paramCount == 0) { // no-arg default constructors, can simplify slightly
            return new AnnotatedConstructor(typeContext, ctor.getConstructor(),
                    _collectRelevantAnnotations(ctor.getDeclaredAnnotations()), NO_ANNOTATION_MAPS);
        }
        // Also: enum value constructors
        AnnotationMap[] resolvedAnnotations;
        Annotation[][] paramAnns = ctor.getParameterAnnotations();
        if (paramCount != paramAnns.length) {
            // Limits of the work-around (to avoid hiding real errors):
            // first, only applicable for member classes and then either:

            resolvedAnnotations = null;
            Class<?> dc = ctor.getDeclaringClass();
            // (a) is enum, which have two extra hidden params (name, index)
            if (dc.isEnum() && (paramCount == paramAnns.length + 2)) {
                Annotation[][] old = paramAnns;
                paramAnns = new Annotation[old.length+2][];
                System.arraycopy(old, 0, paramAnns, 2, old.length);
                resolvedAnnotations = _collectRelevantAnnotations(paramAnns);
            } else if (dc.isMemberClass()) {
                // (b) non-static inner classes, get implicit 'this' for parameter, not  annotation
                if (paramCount == (paramAnns.length + 1)) {
                    // hack attack: prepend a null entry to make things match
                    Annotation[][] old = paramAnns;
                    paramAnns = new Annotation[old.length+1][];
                    System.arraycopy(old, 0, paramAnns, 1, old.length);
                    resolvedAnnotations = _collectRelevantAnnotations(paramAnns);
                }
            }
            if (resolvedAnnotations == null) {
                throw new IllegalStateException("Internal error: constructor for "+ctor.getDeclaringClass().getName()
                        +" has mismatch: "+paramCount+" parameters; "+paramAnns.length+" sets of annotations");
            }
        } else {
            resolvedAnnotations = _collectRelevantAnnotations(paramAnns);
        }
        return new AnnotatedConstructor(typeContext, ctor.getConstructor(),
                _collectRelevantAnnotations(ctor.getDeclaredAnnotations()), resolvedAnnotations);
    }

    protected AnnotatedMethod _constructCreatorMethod(Method m, TypeResolutionContext typeContext)
    {
        final int paramCount = m.getParameterTypes().length;
        if (_annotationIntrospector == null) { // when annotation processing is disabled
            return new AnnotatedMethod(typeContext, m, _emptyAnnotationMap(), _emptyAnnotationMaps(paramCount));
        }
        if (paramCount == 0) { // common enough we can slightly optimize
            return new AnnotatedMethod(typeContext, m, _collectRelevantAnnotations(m.getDeclaredAnnotations()),
                    NO_ANNOTATION_MAPS);
        }
        return new AnnotatedMethod(typeContext, m, _collectRelevantAnnotations(m.getDeclaredAnnotations()),
                                   _collectRelevantAnnotations(m.getParameterAnnotations()));
    }

    protected AnnotatedField _constructField(Field f, TypeResolutionContext typeContext)
    {
        if (_annotationIntrospector == null) { // when annotation processing is disabled
            return new AnnotatedField(typeContext, f, _emptyAnnotationMap());
        }
        return new AnnotatedField(typeContext, f, _collectRelevantAnnotations(f.getDeclaredAnnotations()));
    }
 
    private AnnotationMap _emptyAnnotationMap() {
        return new AnnotationMap();
    }

    private AnnotationMap[] _emptyAnnotationMaps(int count) {
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

    // for [databind#1005]: do not use or expose synthetic constructors
    private boolean _isIncludableConstructor(Constructor<?> c)
    {
        return !c.isSynthetic();
    }

    /*
    /**********************************************************
    /* Helper methods, attaching annotations
    /**********************************************************
     */

    protected AnnotationMap[] _collectRelevantAnnotations(Annotation[][] anns)
    {
        int len = anns.length;
        AnnotationMap[] result = new AnnotationMap[len];
        for (int i = 0; i < len; ++i) {
            result[i] = _collectRelevantAnnotations(anns[i]);
        }
        return result;
    }

    protected AnnotationMap _collectRelevantAnnotations(Annotation[] anns)
    {
        return _addAnnotationsIfNotPresent(new AnnotationMap(), anns);
    }
    
    /* Helper method used to add all applicable annotations from given set.
     * Takes into account possible "annotation bundles" (meta-annotations to
     * include instead of main-level annotation)
     */
    private AnnotationMap _addAnnotationsIfNotPresent(AnnotationMap result, Annotation[] anns)
    {
        if (anns != null) {
            List<Annotation> fromBundles = null;
            for (Annotation ann : anns) { // first: direct annotations
                // note: we will NOT filter out non-Jackson anns any more
                boolean wasNotPresent = result.addIfNotPresent(ann);
                if (wasNotPresent && _isAnnotationBundle(ann)) {
                    fromBundles = _addFromBundle(ann, fromBundles);
                }
            }
            if (fromBundles != null) { // and secondarily handle bundles, if any found: precedence important
                _addAnnotationsIfNotPresent(result, fromBundles.toArray(new Annotation[fromBundles.size()]));
            }
        }
        return result;
    }

    private List<Annotation> _addFromBundle(Annotation bundle, List<Annotation> result)
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
    
    private void _addAnnotationsIfNotPresent(AnnotatedMember target, Annotation[] anns)
    {
        if (anns != null) {
            List<Annotation> fromBundles = null;
            for (Annotation ann : anns) { // first: direct annotations
                boolean wasNotPresent = target.addIfNotPresent(ann);
                if (wasNotPresent && _isAnnotationBundle(ann)) {
                    fromBundles = _addFromBundle(ann, fromBundles);
                }
            }
            if (fromBundles != null) { // and secondarily handle bundles, if any found: precedence important
                _addAnnotationsIfNotPresent(target, fromBundles.toArray(new Annotation[fromBundles.size()]));
            }
        }
    }
    
    private void _addOrOverrideAnnotations(AnnotatedMember target, Annotation[] anns)
    {
        if (anns != null) {
            List<Annotation> fromBundles = null;
            for (Annotation ann : anns) { // first: direct annotations
                boolean wasModified = target.addOrOverride(ann);
                if (wasModified && _isAnnotationBundle(ann)) {
                    fromBundles = _addFromBundle(ann, fromBundles);
                }
            }
            if (fromBundles != null) { // and then bundles, if any: important for precedence
                _addOrOverrideAnnotations(target, fromBundles.toArray(new Annotation[fromBundles.size()]));
            }
        }
    }
    
    /**
     * @param addParamAnnotations Whether parameter annotations are to be
     *   added as well
     */
    protected void _addMixOvers(Constructor<?> mixin, AnnotatedConstructor target,
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
     * @param addParamAnnotations Whether parameter annotations are to be
     *   added as well
     */
    protected void _addMixOvers(Method mixin, AnnotatedMethod target,
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
    protected void _addMixUnders(Method src, AnnotatedMethod target) {
        _addAnnotationsIfNotPresent(target, src.getDeclaredAnnotations());
    }

    private final boolean _isAnnotationBundle(Annotation ann) {
        return (_annotationIntrospector != null) && _annotationIntrospector.isAnnotationBundle(ann);
    }

    /**
     * Helper method that gets methods declared in given class; usually a simple thing,
     * but sometimes (as per [databind#785]) more complicated, depending on classloader
     * setup.
     *
     * @since 2.4.7
     */
    protected Method[] _findClassMethods(Class<?> cls)
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
                // !!! TODO: 08-May-2015, tatu: Chain appropriately once we have JDK 1.7/Java7 as baseline
                //ex.addSuppressed(e); Not until Jackson 2.7
               throw ex;
            }
            return contextClass.getDeclaredMethods(); // Cross fingers
        }
    }

    /*
    /**********************************************************
    /* Other methods
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
        if (o == null || o.getClass() != getClass()) return false;
        return ((AnnotatedClass) o)._class == _class;
    }
}
