package com.fasterxml.jackson.databind.introspect;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.databind.AnnotationIntrospector;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.introspect.AnnotatedClass.Creators;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.fasterxml.jackson.databind.util.ClassUtil;

/**
 * Helper class used to contain details of how Creators (annotated constructors
 * and static methods) are discovered to be accessed by and via {@link AnnotatedClass}.
 *
 * @since 2.9
 */
final class AnnotatedCreatorCollector
    extends CollectorBase
{
    // // // Configuration

    private final TypeResolutionContext _typeContext;

    /**
     * @since 2.11
     */
    private final boolean _collectAnnotations;

    // // // Collected state

    private AnnotatedConstructor _defaultConstructor;

    AnnotatedCreatorCollector(AnnotationIntrospector intr,
            TypeResolutionContext tc, boolean collectAnnotations)
    {
        super(intr);
        _typeContext = tc;
        _collectAnnotations = collectAnnotations;
    }

    /**
     * @since 2.11.3
     */
    public static Creators collectCreators(AnnotationIntrospector intr,
            TypeFactory typeFactory, TypeResolutionContext tc,
            JavaType type, Class<?> primaryMixIn, boolean collectAnnotations)
    {
        // 30-Sep-2020, tatu: [databind#2795] Even if annotations not otherwise
        //  requested (for JDK collections), force change if mix-in in use
        collectAnnotations |= (primaryMixIn != null);

        // Constructor also always members of resolved class, parent == resolution context
        return new AnnotatedCreatorCollector(intr, tc, collectAnnotations)
                .collect(typeFactory, type, primaryMixIn);
    }

    Creators collect(TypeFactory typeFactory, JavaType type, Class<?> primaryMixIn)
    {
    // 30-Apr-2016, tatu: [databind#1215]: Actually, while true, this does
    //   NOT apply to context since sub-class may have type bindings
//        TypeResolutionContext typeContext = new TypeResolutionContext.Basic(_typeFactory, _type.getBindings());

        List<AnnotatedConstructor> constructors = _findPotentialConstructors(type, primaryMixIn);
        List<AnnotatedMethod> factories = _findPotentialFactories(typeFactory, type, primaryMixIn);

        /* And then... let's remove all constructors that are deemed
         * ignorable after all annotations have been properly collapsed.
         */
        // AnnotationIntrospector is null if annotations not enabled; if so, can skip:
        if (_collectAnnotations) {
            if (_defaultConstructor != null) {
                if (_intr.hasIgnoreMarker(_defaultConstructor)) {
                    _defaultConstructor = null;
                }
            }
            // count down to allow safe removal
            for (int i = constructors.size(); --i >= 0; ) {
                if (_intr.hasIgnoreMarker(constructors.get(i))) {
                    constructors.remove(i);
                }
            }
            for (int i = factories.size(); --i >= 0; ) {
                if (_intr.hasIgnoreMarker(factories.get(i))) {
                    factories.remove(i);
                }
            }
        }
        return new AnnotatedClass.Creators(_defaultConstructor, constructors, factories);
    }

    /**
     * Helper method for locating constructors (and matching mix-in overrides)
     * we might want to use; this is needed in order to mix information between
     * the two and construct resulting {@link AnnotatedConstructor}s
     */
    private List<AnnotatedConstructor> _findPotentialConstructors(JavaType type,
            Class<?> primaryMixIn)
    {
        ClassUtil.Ctor defaultCtor = null;
        List<ClassUtil.Ctor> ctors = null;

        // 18-Jun-2016, tatu: Enum constructors will never be useful (unlike
        //    possibly static factory methods); but they can be royal PITA
        //    due to some oddities by JVM; see:
        //    [https://github.com/FasterXML/jackson-module-parameter-names/issues/35]
        //    for more. So, let's just skip them.
        if (!type.isEnumType()) {
            ClassUtil.Ctor[] declaredCtors = ClassUtil.getConstructors(type.getRawClass());
            for (ClassUtil.Ctor ctor : declaredCtors) {
                if (!isIncludableConstructor(ctor.getConstructor())) {
                    continue;
                }
                if (ctor.getParamCount() == 0) {
                    defaultCtor = ctor;
                } else {
                    if (ctors == null) {
                        ctors = new ArrayList<>();
                    }
                    ctors.add(ctor);
                }
            }
        }
        List<AnnotatedConstructor> result;
        int ctorCount;
        if (ctors == null) {
            result = Collections.emptyList();
            // Nothing found? Short-circuit
            if (defaultCtor == null) {
                return result;
            }
            ctorCount = 0;
        } else {
            ctorCount = ctors.size();
            result = new ArrayList<>(ctorCount);
            for (int i = 0; i < ctorCount; ++i) {
                result.add(null);
            }
        }

        // so far so good; but do we also need to find mix-ins overrides?
        if (primaryMixIn != null) {
            MemberKey[] ctorKeys = null;
            for (ClassUtil.Ctor mixinCtor : ClassUtil.getConstructors(primaryMixIn)) {
                if (mixinCtor.getParamCount() == 0) {
                    if (defaultCtor != null) {
                        _defaultConstructor = constructDefaultConstructor(defaultCtor, mixinCtor);
                        defaultCtor = null;
                    }
                    continue;
                }
                if (ctors != null) {
                    if (ctorKeys == null) {
                        ctorKeys = new MemberKey[ctorCount];
                        for (int i = 0; i < ctorCount; ++i) {
                            ctorKeys[i] = new MemberKey(ctors.get(i).getConstructor());
                        }
                    }
                    MemberKey key = new MemberKey(mixinCtor.getConstructor());

                    for (int i = 0; i < ctorCount; ++i) {
                        if (key.equals(ctorKeys[i])) {
                            result.set(i,
                                    constructNonDefaultConstructor(ctors.get(i), mixinCtor));
                            break;
                        }
                    }
                }
            }
        }
        // Ok: anything within mix-ins has been resolved; anything remaining we must resolve
        if (defaultCtor != null) {
            _defaultConstructor = constructDefaultConstructor(defaultCtor, null);
        }
        for (int i = 0; i < ctorCount; ++i) {
            AnnotatedConstructor ctor = result.get(i);
            if (ctor == null) {
                result.set(i,
                        constructNonDefaultConstructor(ctors.get(i), null));
            }
        }
        return result;
    }

    private List<AnnotatedMethod> _findPotentialFactories(TypeFactory typeFactory,
            JavaType type, Class<?> primaryMixIn)
    {
        List<Method> candidates = null;

        // First find all potentially relevant static methods
        for (Method m : ClassUtil.getClassMethods(type.getRawClass())) {
            if (!_isIncludableFactoryMethod(m)) {
                continue;
            }
            // all factory methods are fine:
            //int argCount = m.getParameterTypes().length;
            if (candidates == null) {
                candidates = new ArrayList<>();
            }
            candidates.add(m);
        }
        // and then locate mix-ins, if any
        if (candidates == null) {
            return Collections.emptyList();
        }
        // 05-Sep-2020, tatu: Important fix wrt [databind#2821] -- static methods
        //   do NOT have type binding context of the surrounding class and although
        //   passing that should not break things, it appears to... Regardless,
        //   it should not be needed or useful as those bindings are only available
        //   to non-static members
//        final TypeResolutionContext typeResCtxt = new TypeResolutionContext.Empty(typeFactory);

        // 27-Oct-2020, tatu: SIGH. As per [databind#2894] there is widespread use of
        //   incorrect bindings in the wild -- not supported (no tests) but used
        //   nonetheless. So, for 2.11.x, put back "Bad Bindings"...
//

        // 03-Nov-2020, ckozak: Implement generic JsonCreator TypeVariable handling [databind#2895]
//        final TypeResolutionContext emptyTypeResCtxt = new TypeResolutionContext.Empty(typeFactory);

        // 23-Aug-2021, tatu: Double-d'oh! As per [databind#3220], we must revert to
        //   the Incorrect Illogical But Used By Users Bindings... for 2.x at least.
        final TypeResolutionContext initialTypeResCtxt = _typeContext;

        int factoryCount = candidates.size();
        List<AnnotatedMethod> result = new ArrayList<>(factoryCount);
        for (int i = 0; i < factoryCount; ++i) {
            result.add(null);
        }
        // so far so good; but do we also need to find mix-ins overrides?
        if (primaryMixIn != null) {
            MemberKey[] methodKeys = null;
            for (Method mixinFactory : primaryMixIn.getDeclaredMethods()) {
                if (!_isIncludableFactoryMethod(mixinFactory)) {
                    continue;
                }
                if (methodKeys == null) {
                    methodKeys = new MemberKey[factoryCount];
                    for (int i = 0; i < factoryCount; ++i) {
                        methodKeys[i] = new MemberKey(candidates.get(i));
                    }
                }
                MemberKey key = new MemberKey(mixinFactory);
                for (int i = 0; i < factoryCount; ++i) {
                    if (key.equals(methodKeys[i])) {
                        result.set(i,
                                constructFactoryCreator(candidates.get(i),
                                        initialTypeResCtxt, mixinFactory));
                        break;
                    }
                }
            }
        }
        // Ok: anything within mix-ins has been resolved; anything remaining we must resolve
        for (int i = 0; i < factoryCount; ++i) {
            AnnotatedMethod factory = result.get(i);
            if (factory == null) {
                Method candidate = candidates.get(i);
                // 06-Nov-2020, tatu: Fix from [databind#2895] will try to resolve
                //   nominal static method type bindings into expected target type
                //   (if generic types involved)
                // 23-Aug-2021, tatu: ... is this still needed, wrt un-fix in [databind#3220]?
                TypeResolutionContext typeResCtxt = MethodGenericTypeResolver.narrowMethodTypeParameters(
                        candidate, type, typeFactory, initialTypeResCtxt);
                result.set(i,
                        constructFactoryCreator(candidate, typeResCtxt, null));
            }
        }
        return result;
    }

    private static boolean _isIncludableFactoryMethod(Method m)
    {
        return Modifier.isStatic(m.getModifiers())
                // 09-Nov-2020, ckozak: Avoid considering synthetic methods such as
                // lambdas used within methods because they're not relevant.
                && !m.isSynthetic();
    }

    protected AnnotatedConstructor constructDefaultConstructor(ClassUtil.Ctor ctor,
            ClassUtil.Ctor mixin)
    {
        return new AnnotatedConstructor(_typeContext, ctor.getConstructor(),
                collectAnnotations(ctor, mixin),
                // 16-Jun-2019, tatu: default is zero-args, so can't have parameter annotations
                NO_ANNOTATION_MAPS);
    }

    protected AnnotatedConstructor constructNonDefaultConstructor(ClassUtil.Ctor ctor,
            ClassUtil.Ctor mixin)
    {
        final int paramCount = ctor.getParamCount();
        if (_intr == null) { // when annotation processing is disabled
            return new AnnotatedConstructor(_typeContext, ctor.getConstructor(),
                    _emptyAnnotationMap(), _emptyAnnotationMaps(paramCount));
        }

        /* Looks like JDK has discrepancy, whereas annotations for implicit 'this'
         * (for non-static inner classes) are NOT included, but type is?
         * Strange, sounds like a bug. Alas, we can't really fix that...
         */
        if (paramCount == 0) { // no-arg default constructors, can simplify slightly
            return new AnnotatedConstructor(_typeContext, ctor.getConstructor(),
                    collectAnnotations(ctor, mixin),
                    NO_ANNOTATION_MAPS);
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
            if (ClassUtil.isEnumType(dc) && (paramCount == paramAnns.length + 2)) {
                Annotation[][] old = paramAnns;
                paramAnns = new Annotation[old.length+2][];
                System.arraycopy(old, 0, paramAnns, 2, old.length);
                resolvedAnnotations = collectAnnotations(paramAnns, null);
            } else if (dc.isMemberClass()) {
                // (b) non-static inner classes, get implicit 'this' for parameter, not  annotation
                if (paramCount == (paramAnns.length + 1)) {
                    // hack attack: prepend a null entry to make things match
                    Annotation[][] old = paramAnns;
                    paramAnns = new Annotation[old.length+1][];
                    System.arraycopy(old, 0, paramAnns, 1, old.length);
                    paramAnns[0] = NO_ANNOTATIONS;
                    resolvedAnnotations = collectAnnotations(paramAnns, null);
                }
            }
            if (resolvedAnnotations == null) {
                throw new IllegalStateException(String.format(
"Internal error: constructor for %s has mismatch: %d parameters; %d sets of annotations",
ctor.getDeclaringClass().getName(), paramCount, paramAnns.length));
            }
        } else {
            resolvedAnnotations = collectAnnotations(paramAnns,
                    (mixin == null) ? null : mixin.getParameterAnnotations());
        }
        return new AnnotatedConstructor(_typeContext, ctor.getConstructor(),
                collectAnnotations(ctor, mixin), resolvedAnnotations);
    }

    protected AnnotatedMethod constructFactoryCreator(Method m,
            TypeResolutionContext typeResCtxt, Method mixin)
    {
        final int paramCount = m.getParameterCount();
        if (_intr == null) { // when annotation processing is disabled
            return new AnnotatedMethod(typeResCtxt, m, _emptyAnnotationMap(),
                    _emptyAnnotationMaps(paramCount));
        }
        if (paramCount == 0) { // common enough we can slightly optimize
            return new AnnotatedMethod(typeResCtxt, m, collectAnnotations(m, mixin),
                    NO_ANNOTATION_MAPS);
        }
        return new AnnotatedMethod(typeResCtxt, m, collectAnnotations(m, mixin),
                collectAnnotations(m.getParameterAnnotations(),
                        (mixin == null) ? null : mixin.getParameterAnnotations()));
    }

    private AnnotationMap[] collectAnnotations(Annotation[][] mainAnns, Annotation[][] mixinAnns) {
        if (_collectAnnotations) {
            final int count = mainAnns.length;
            AnnotationMap[] result = new AnnotationMap[count];
            for (int i = 0; i < count; ++i) {
                AnnotationCollector c = collectAnnotations(AnnotationCollector.emptyCollector(),
                        mainAnns[i]);
                if (mixinAnns != null) {
                    c = collectAnnotations(c, mixinAnns[i]);
                }
                result[i] = c.asAnnotationMap();
            }
            return result;
        }
        return NO_ANNOTATION_MAPS;
    }

    // // NOTE: these are only called when we know we have AnnotationIntrospector

    private AnnotationMap collectAnnotations(ClassUtil.Ctor main, ClassUtil.Ctor mixin) {
        if (_collectAnnotations) {
            AnnotationCollector c = collectAnnotations(main.getDeclaredAnnotations());
            if (mixin != null) {
                c = collectAnnotations(c, mixin.getDeclaredAnnotations());
            }
            return c.asAnnotationMap();
        }
        return _emptyAnnotationMap();
    }

    private final AnnotationMap collectAnnotations(AnnotatedElement main, AnnotatedElement mixin) {
        AnnotationCollector c = collectAnnotations(main.getDeclaredAnnotations());
        if (mixin != null) {
            c = collectAnnotations(c, mixin.getDeclaredAnnotations());
        }
        return c.asAnnotationMap();
    }

    // for [databind#1005]: do not use or expose synthetic constructors
    private static boolean isIncludableConstructor(Constructor<?> c) {
        return !c.isSynthetic();
    }
}
