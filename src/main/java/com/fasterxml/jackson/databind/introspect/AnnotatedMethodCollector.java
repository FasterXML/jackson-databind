package com.fasterxml.jackson.databind.introspect;

import java.lang.annotation.Annotation;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;

import com.fasterxml.jackson.databind.AnnotationIntrospector;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.introspect.ClassIntrospector.MixInResolver;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.fasterxml.jackson.databind.util.ClassUtil;

public class AnnotatedMethodCollector
{
    final static AnnotationMap[] NO_ANNOTATION_MAPS = new AnnotationMap[0];

    private final AnnotationIntrospector _intr;
    private final MixInResolver _mixInResolver;
    
    // // // Collected state

    AnnotatedMethodCollector(AnnotationIntrospector intr,
            MixInResolver mixins)
    {
        _intr = intr;
        _mixInResolver = mixins;
    }

    public static AnnotatedMethodMap collectMethods(TypeResolutionContext tc,
            AnnotationIntrospector intr, MixInResolver mixins, TypeFactory types,
            JavaType type, List<JavaType> superTypes, Class<?> primaryMixIn)
    {
        // Constructor also always members of resolved class, parent == resolution context
        return new AnnotatedMethodCollector(intr, mixins)
                .collect(types, tc, type, superTypes, primaryMixIn);
    }

    AnnotatedMethodMap collect(TypeFactory typeFactory, TypeResolutionContext tc,
            JavaType mainType, List<JavaType> superTypes, Class<?> primaryMixIn)
    {
        AnnotatedMethodMap memberMethods = new AnnotatedMethodMap();
        AnnotatedMethodMap mixins = new AnnotatedMethodMap();
        // first: methods from the class itself
        _addMemberMethods(tc, mainType.getRawClass(), memberMethods, primaryMixIn, mixins);

        // and then augment these with annotations from super-types:
        for (JavaType type : superTypes) {
            Class<?> mixin = (_mixInResolver == null) ? null : _mixInResolver.findMixInClassFor(type.getRawClass());
            _addMemberMethods(
                    new TypeResolutionContext.Basic(typeFactory, type.getBindings()),
                    type.getRawClass(),
                    memberMethods, mixin, mixins);
        }
        // Special case: mix-ins for Object.class? (to apply to ALL classes)
        if (_mixInResolver != null) {
            Class<?> mixin = _mixInResolver.findMixInClassFor(Object.class);
            if (mixin != null) {
                _addMethodMixIns(tc, mainType.getRawClass(), memberMethods, mixin, mixins);
            }
        }

        /* Any unmatched mix-ins? Most likely error cases (not matching
         * any method); but there is one possible real use case:
         * exposing Object#hashCode (alas, Object#getClass can NOT be
         * exposed)
         */
        // 14-Feb-2011, tatu: AnnotationIntrospector is null if annotations not enabled; if so, can skip:
        if (_intr != null) {
            if (!mixins.isEmpty()) {
                Iterator<AnnotatedMethod> it = mixins.iterator();
                while (it.hasNext()) {
                    AnnotatedMethod mixIn = it.next();
                    try {
                        Method m = Object.class.getDeclaredMethod(mixIn.getName(), mixIn.getRawParameterTypes());
                        if (m != null) {
                            // Since it's from java.lang.Object, no generics, no need for real type context:
                            AnnotatedMethod am = _constructMethod(tc, m);
                            _addMixOvers(mixIn.getAnnotated(), am, false);
                            memberMethods.add(am);
                        }
                    } catch (Exception e) { }
                }
            }
        }
        return memberMethods;
    }

    protected void _addMemberMethods(TypeResolutionContext tc,
            Class<?> cls,  AnnotatedMethodMap methods,
            Class<?> mixInCls, AnnotatedMethodMap mixIns)
    {
        // first, mixIns, since they have higher priority then class methods
        if (mixInCls != null) {
            _addMethodMixIns(tc, cls, methods, mixInCls, mixIns);
        }
        if (cls == null) { // just so caller need not check when passing super-class
            return;
        }
        // then methods from the class itself
        for (Method m : ClassUtil.getClassMethods(cls)) {
            if (!_isIncludableMemberMethod(m)) {
                continue;
            }
            AnnotatedMethod old = methods.find(m);
            if (old == null) {
                AnnotatedMethod newM = _constructMethod(tc, m);
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

    protected void _addMethodMixIns(TypeResolutionContext tc, Class<?> targetClass,
            AnnotatedMethodMap methods,
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
                    // Otherwise will have precedence, but must wait until we find
                    // the real method (mixIn methods are just placeholder, can't be called)
                } else {
                    // Well, or, as per [databind#515], multi-level merge within mixins...
                    am = mixIns.find(m);
                    if (am != null) {
                        _addMixUnders(m, am);
                    } else {
                        // 03-Nov-2015, tatu: Mix-in method never called, should not need
                        //    to resolve generic types, so this class is fine as context
                        mixIns.add(_constructMethod(tc, m));
                    }
                }
            }
        }
    }

    /*
    /**********************************************************
    /* Helper methods, constructing value types
    /**********************************************************
     */

    private AnnotatedMethod _constructMethod(TypeResolutionContext typeContext, Method m)
    {
        // note: parameter annotations not used for regular (getter, setter) methods;
        // only for creator methods (static factory methods)-- at least not yet!
        // (would change if multi-argument setters allowed)
        if (_intr == null) { // when annotation processing is disabled
            return new AnnotatedMethod(typeContext, m, _emptyAnnotationMap(), null);
        }
        return new AnnotatedMethod(typeContext, m, _collectRelevantAnnotations(m.getDeclaredAnnotations()), null);
    }
 
    private AnnotationMap _collectRelevantAnnotations(Annotation[] anns) {
        return _addAnnotationsIfNotPresent(new AnnotationMap(), anns);
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

    private boolean _isIncludableMemberMethod(Method m)
    {
        if (Modifier.isStatic(m.getModifiers())) {
            return false;
        }
        // Looks like generics can introduce hidden bridge and/or synthetic methods.
        // I don't think we want to consider those...
        if (m.isSynthetic() || m.isBridge()) {
            return false;
        }
        // also, for now we have no use for methods with 2 or more arguments:
        int pcount = m.getParameterTypes().length;
        return (pcount <= 2);
    }

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

    private void _addMixUnders(Method src, AnnotatedMethod target) {
        _addAnnotationsIfNotPresent(target, src.getDeclaredAnnotations());
    }

    private void _addOrOverrideAnnotations(AnnotatedMember target, Annotation[] anns)
    {
        if (anns == null) {
            return;
        }
        List<Annotation> fromBundles = null;
        for (Annotation ann : anns) { // first: direct annotations
            boolean wasModified = target.addOrOverride(ann);
            if (wasModified && _intr.isAnnotationBundle(ann)) {
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
    private AnnotationMap _addAnnotationsIfNotPresent(AnnotationMap result, Annotation[] anns)
    {
        if (anns != null) {
            List<Annotation> fromBundles = null;
            for (Annotation ann : anns) { // first: direct annotations
                // note: we will NOT filter out non-Jackson anns any more
                boolean wasNotPresent = result.addIfNotPresent(ann);
                if (wasNotPresent && _intr.isAnnotationBundle(ann)) {
                    fromBundles = _addFromBundle(ann, fromBundles);
                }
            }
            if (fromBundles != null) { // and secondarily handle bundles, if any found: precedence important
                _addAnnotationsIfNotPresent(result,
                        fromBundles.toArray(new Annotation[fromBundles.size()]));
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
    
    private void _addAnnotationsIfNotPresent(AnnotatedMethod target, Annotation[] anns)
    {
        if (anns == null) {
            return;
        }
        List<Annotation> fromBundles = null;
        for (Annotation ann : anns) { // first: direct annotations
            boolean wasNotPresent = target.addIfNotPresent(ann);
            if (wasNotPresent && _intr.isAnnotationBundle(ann)) {
                fromBundles = _addFromBundle(ann, fromBundles);
            }
        }
        if (fromBundles != null) { // and secondarily handle bundles, if any found: precedence important
            _addAnnotationsIfNotPresent(target,
                    fromBundles.toArray(new Annotation[fromBundles.size()]));
        }
    }
}
