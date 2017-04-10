package com.fasterxml.jackson.databind.introspect;

import java.lang.annotation.Annotation;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.databind.AnnotationIntrospector;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.introspect.AnnotatedClass.Creators;
import com.fasterxml.jackson.databind.util.ClassUtil;

/**
 * Helper class used to contain details of how Creators (annotated constructors
 * and static methods) are discovered to be accessed by and via {@link AnnotatedClass}.
 *
 * @since 2.9
 */
final class AnnotatedCreatorResolver
    {
        private final AnnotationIntrospector _intr;
        private final Class<?> _primaryMixIn;

        public AnnotatedCreatorResolver(AnnotatedClass parent) {
            _intr = parent._annotationIntrospector;
            _primaryMixIn = parent._primaryMixIn;
        }

        public static Creators resolve(AnnotatedClass parent, JavaType type) {
            // Constructor also always members of resolved class, parent == resolution context
            return new AnnotatedCreatorResolver(parent).resolve(type, parent);
        }

        public Creators resolve(JavaType type, final TypeResolutionContext typeContext)
        {
        // 30-Apr-2016, tatu: [databind#1215]: Actually, while true, this does
        //   NOT apply to context since sub-class may have type bindings
//        TypeResolutionContext typeContext = new TypeResolutionContext.Basic(_typeFactory, _type.getBindings());

            // Then see which constructors we have
            AnnotatedConstructor defaultCtor = null;
            List<AnnotatedConstructor> constructors = null;
            List<AnnotatedMethod> creatorMethods = null;

            // 18-Jun-2016, tatu: Enum constructors will never be useful (unlike
            //    possibly static factory methods); but they can be royal PITA
            //    due to some oddities by JVM; see:
            //    [https://github.com/FasterXML/jackson-module-parameter-names/issues/35]
            //    for more. So, let's just skip them.
            if (!type.isEnumType()) {
                ClassUtil.Ctor[] declaredCtors = ClassUtil.getConstructors(type.getRawClass());
                for (ClassUtil.Ctor ctor : declaredCtors) {
                    if (isIncludableConstructor(ctor.getConstructor())) {
                        if (ctor.getParamCount() == 0) {
                            defaultCtor = constructDefaultConstructor(ctor, typeContext);
                        } else {
                            if (constructors == null) {
                                constructors = new ArrayList<AnnotatedConstructor>(Math.max(10, declaredCtors.length));
                            }
                            constructors.add(constructNonDefaultConstructor(ctor, typeContext));
                        }
                    }
                }
            }
            if (constructors == null) {
                constructors = Collections.emptyList();
            }
            // and if need be, augment with mix-ins
            if (_primaryMixIn != null) {
                if (defaultCtor != null || !constructors.isEmpty()) {
                    addConstructorMixIns(_primaryMixIn, defaultCtor, constructors);
                }
            }

            /* And then... let's remove all constructors that are deemed
             * ignorable after all annotations have been properly collapsed.
             */
            // AnnotationIntrospector is null if annotations not enabled; if so, can skip:
            if (_intr != null) {
                if (defaultCtor != null) {
                    if (_intr.hasIgnoreMarker(defaultCtor)) {
                        defaultCtor = null;
                    }
                }
                if (constructors != null) {
                    for (int i = constructors.size(); --i >= 0; ) {
                        if (_intr.hasIgnoreMarker(constructors.get(i))) {
                            constructors.remove(i);
                        }
                    }
                }
            }

            // Then static methods which are potential factory methods
            for (Method m : AnnotatedClass._findClassMethods(type.getRawClass())) {
                if (!Modifier.isStatic(m.getModifiers())) {
                    continue;
                }
                // all factory methods are fine:
                //int argCount = m.getParameterTypes().length;
                if (creatorMethods == null) {
                    creatorMethods = new ArrayList<AnnotatedMethod>(8);
                }
                creatorMethods.add(constructCreatorMethod(m, typeContext));
            }
            if (creatorMethods == null) {
                creatorMethods = Collections.emptyList();
            } else {
                // mix-ins to mix in?
                if (_primaryMixIn != null) {
                    addFactoryMixIns(_primaryMixIn, creatorMethods);
                }
                // anything to ignore at this point?
                if (_intr != null) {
                    // count down to allow safe removal
                    for (int i = creatorMethods.size(); --i >= 0; ) {
                        if (_intr.hasIgnoreMarker(creatorMethods.get(i))) {
                            creatorMethods.remove(i);
                        }
                    }
                }
            }
            return new AnnotatedClass.Creators(defaultCtor, constructors, creatorMethods);
        }

        // for [databind#1005]: do not use or expose synthetic constructors
        private static boolean isIncludableConstructor(Constructor<?> c)
        {
            return !c.isSynthetic();
        }

        protected void addConstructorMixIns(Class<?> mixin,
                AnnotatedConstructor defaultCtor,
                List<AnnotatedConstructor> constructors)
        {
            MemberKey[] ctorKeys = null;
            int ctorCount = constructors.size();
            for (ClassUtil.Ctor ctor0 : ClassUtil.getConstructors(mixin)) {
                Constructor<?> ctor = ctor0.getConstructor();
                if (ctor.getParameterTypes().length == 0) {
                    if (defaultCtor != null) {
                        addMixOvers(ctor, defaultCtor, false);
                    }
                } else {
                    if (ctorKeys == null) {
                        ctorKeys = new MemberKey[ctorCount];
                        for (int i = 0; i < ctorCount; ++i) {
                            ctorKeys[i] = new MemberKey(constructors.get(i).getAnnotated());
                        }
                    }
                    MemberKey key = new MemberKey(ctor);

                    for (int i = 0; i < ctorCount; ++i) {
                        if (!key.equals(ctorKeys[i])) {
                            continue;
                        }
                        addMixOvers(ctor, constructors.get(i), true);
                        break;
                    }
                }
            }
        }

        protected void addFactoryMixIns(Class<?> mixin, List<AnnotatedMethod> creatorMethods)
        {
            MemberKey[] methodKeys = null;
            int methodCount = creatorMethods.size();

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
                        methodKeys[i] = new MemberKey(creatorMethods.get(i).getAnnotated());
                    }
                }
                MemberKey key = new MemberKey(m);
                for (int i = 0; i < methodCount; ++i) {
                    if (!key.equals(methodKeys[i])) {
                        continue;
                    }
                    addMixOvers(m, creatorMethods.get(i));
                    break;
                }
            }
        }

        protected AnnotatedConstructor constructDefaultConstructor(ClassUtil.Ctor ctor,
                TypeResolutionContext typeContext)
        {
            if (_intr == null) { // when annotation processing is disabled
                return new AnnotatedConstructor(typeContext, ctor.getConstructor(), AnnotatedClass._emptyAnnotationMap(), AnnotatedClass.NO_ANNOTATION_MAPS);
            }
            return new AnnotatedConstructor(typeContext, ctor.getConstructor(),
                    collectRelevantAnnotations(ctor.getDeclaredAnnotations()), AnnotatedClass.NO_ANNOTATION_MAPS);
        }

        protected AnnotatedConstructor constructNonDefaultConstructor(ClassUtil.Ctor ctor,
                TypeResolutionContext typeContext)
        {
            final int paramCount = ctor.getParamCount();
            if (_intr == null) { // when annotation processing is disabled
                return new AnnotatedConstructor(typeContext, ctor.getConstructor(),
                        AnnotatedClass._emptyAnnotationMap(), AnnotatedClass._emptyAnnotationMaps(paramCount));
            }

            /* Looks like JDK has discrepancy, whereas annotations for implicit 'this'
             * (for non-static inner classes) are NOT included, but type is?
             * Strange, sounds like a bug. Alas, we can't really fix that...
             */
            if (paramCount == 0) { // no-arg default constructors, can simplify slightly
                return new AnnotatedConstructor(typeContext, ctor.getConstructor(),
                        collectRelevantAnnotations(ctor.getDeclaredAnnotations()), AnnotatedClass.NO_ANNOTATION_MAPS);
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
                    resolvedAnnotations = collectRelevantAnnotations(paramAnns);
                } else if (dc.isMemberClass()) {
                    // (b) non-static inner classes, get implicit 'this' for parameter, not  annotation
                    if (paramCount == (paramAnns.length + 1)) {
                        // hack attack: prepend a null entry to make things match
                        Annotation[][] old = paramAnns;
                        paramAnns = new Annotation[old.length+1][];
                        System.arraycopy(old, 0, paramAnns, 1, old.length);
                        resolvedAnnotations = collectRelevantAnnotations(paramAnns);
                    }
                }
                if (resolvedAnnotations == null) {
                    throw new IllegalStateException(String.format(
"Internal error: constructor for %s has mismatch: %d parameters; %d sets of annotations",
ctor.getDeclaringClass().getName(), paramCount, paramAnns.length));
                }
            } else {
                resolvedAnnotations = collectRelevantAnnotations(paramAnns);
            }
            return new AnnotatedConstructor(typeContext, ctor.getConstructor(),
                    collectRelevantAnnotations(ctor.getDeclaredAnnotations()), resolvedAnnotations);
        }

        protected AnnotatedMethod constructCreatorMethod(Method m, TypeResolutionContext typeContext)
        {
            final int paramCount = m.getParameterTypes().length;
            if (_intr == null) { // when annotation processing is disabled
                return new AnnotatedMethod(typeContext, m, AnnotatedClass._emptyAnnotationMap(), AnnotatedClass._emptyAnnotationMaps(paramCount));
            }
            if (paramCount == 0) { // common enough we can slightly optimize
                return new AnnotatedMethod(typeContext, m, collectRelevantAnnotations(m.getDeclaredAnnotations()),
                        AnnotatedClass.NO_ANNOTATION_MAPS);
            }
            return new AnnotatedMethod(typeContext, m, collectRelevantAnnotations(m.getDeclaredAnnotations()),
                    collectRelevantAnnotations(m.getParameterAnnotations()));
        }

        private AnnotationMap[] collectRelevantAnnotations(Annotation[][] anns)
        {
            int len = anns.length;
            AnnotationMap[] result = new AnnotationMap[len];
            for (int i = 0; i < len; ++i) {
                result[i] = collectRelevantAnnotations(anns[i]);
            }
            return result;
        }
        
        private AnnotationMap collectRelevantAnnotations(Annotation[] anns) {
            return addAnnotationsIfNotPresent(new AnnotationMap(), anns);
        }

        private AnnotationMap addAnnotationsIfNotPresent(AnnotationMap result, Annotation[] anns)
        {
            if (anns != null) {
                List<Annotation> fromBundles = null;
                for (Annotation ann : anns) { // first: direct annotations
                    // note: we will NOT filter out non-Jackson anns any more
                    boolean wasNotPresent = result.addIfNotPresent(ann);
                    if (wasNotPresent && _intr.isAnnotationBundle(ann)) {
                        fromBundles = addFromBundle(ann, fromBundles);
                    }
                }
                if (fromBundles != null) { // and secondarily handle bundles, if any found: precedence important
                    addAnnotationsIfNotPresent(result,
                            fromBundles.toArray(new Annotation[fromBundles.size()]));
                }
            }
            return result;
        }

        private List<Annotation> addFromBundle(Annotation bundle, List<Annotation> result)
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

        /**
         * @param addParamAnnotations Whether parameter annotations are to be
         *   added as well
         */
        private void addMixOvers(Constructor<?> mixin, AnnotatedConstructor target,
                boolean addParamAnnotations)
        {
            addOrOverrideAnnotations(target, mixin.getDeclaredAnnotations());
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
        private void addMixOvers(Method mixin, AnnotatedMethod target)
        {
            addOrOverrideAnnotations(target, mixin.getDeclaredAnnotations());
            Annotation[][] pa = mixin.getParameterAnnotations();
            for (int i = 0, len = pa.length; i < len; ++i) {
                for (Annotation a : pa[i]) {
                    target.addOrOverrideParam(i, a);
                }
            }
        }

        private void addOrOverrideAnnotations(AnnotatedMember target, Annotation[] anns)
        {
            if (anns == null) {
                return;
            }
            List<Annotation> fromBundles = null;
            for (Annotation ann : anns) { // first: direct annotations
                boolean wasModified = target.addOrOverride(ann);
                if (wasModified && _intr.isAnnotationBundle(ann)) {
                    fromBundles = addFromBundle(ann, fromBundles);
                }
            }
            if (fromBundles != null) { // and then bundles, if any: important for precedence
                addOrOverrideAnnotations(target, fromBundles.toArray(new Annotation[fromBundles.size()]));
            }
        }
    }