package com.fasterxml.jackson.databind.introspect;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;

import com.fasterxml.jackson.databind.AnnotationIntrospector;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.introspect.ClassIntrospector.MixInResolver;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.fasterxml.jackson.databind.util.ClassUtil;

public class AnnotatedMethodCollector
    extends CollectorBase
{
    private final MixInResolver _mixInResolver;

    AnnotatedMethodCollector(AnnotationIntrospector intr,
            MixInResolver mixins)
    {
        super(intr);
        _mixInResolver = (intr == null) ? null : mixins;
    }

    public static AnnotatedMethodMap collectMethods(AnnotationIntrospector intr,
            TypeResolutionContext tc,
            MixInResolver mixins, TypeFactory types,
            JavaType type, List<JavaType> superTypes, Class<?> primaryMixIn)
    {
        // Constructor also always members of resolved class, parent == resolution context
        return new AnnotatedMethodCollector(intr, mixins)
                .collect(types, tc, type, superTypes, primaryMixIn);
    }

    AnnotatedMethodMap collect(TypeFactory typeFactory, TypeResolutionContext tc,
            JavaType mainType, List<JavaType> superTypes, Class<?> primaryMixIn)
    {
        Map<MemberKey,MethodBuilder> methods = new LinkedHashMap<>();

        // first: methods from the class itself
        _addMemberMethods(tc, mainType.getRawClass(), methods, primaryMixIn);

        // and then augment these with annotations from super-types:
        for (JavaType type : superTypes) {
            Class<?> mixin = (_mixInResolver == null) ? null : _mixInResolver.findMixInClassFor(type.getRawClass());
            _addMemberMethods(
                    new TypeResolutionContext.Basic(typeFactory, type.getBindings()),
                    type.getRawClass(), methods, mixin);
        }
        // Special case: mix-ins for Object.class? (to apply to ALL classes)
        /*
        if (_mixInResolver != null) {
            Class<?> mixin = _mixInResolver.findMixInClassFor(Object.class);
            if (mixin != null) {
                _addMethodMixIns(tc, mainType.getRawClass(), memberMethods, mixin, mixins);
            }
        }

        // Any unmatched mix-ins? Most likely error cases (not matching any method);
        // but there is one possible real use case: exposing Object#hashCode
        // (alas, Object#getClass can NOT be exposed)
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
        */

        // And then let's 
        if (methods.isEmpty()) {
            return new AnnotatedMethodMap();
        }
        Map<MemberKey,AnnotatedMethod> actual = new LinkedHashMap<>(methods.size());
        for (Map.Entry<MemberKey,MethodBuilder> entry : methods.entrySet()) {
            AnnotatedMethod am = entry.getValue().build();
            if (am != null) {
                actual.put(entry.getKey(), am);
            }
        }
        return new AnnotatedMethodMap(actual);
    }

    private void _addMemberMethods(TypeResolutionContext tc,
            Class<?> cls, Map<MemberKey,MethodBuilder> methods, Class<?> mixInCls)
    {
        // first, mixIns, since they have higher priority then class methods
        if (mixInCls != null) {
            _addMethodMixIns(tc, cls, methods, mixInCls);
        }
        if (cls == null) { // just so caller need not check when passing super-class
            return;
        }
        // then methods from the class itself
        for (Method m : ClassUtil.getClassMethods(cls)) {
            if (!_isIncludableMemberMethod(m)) {
                continue;
            }
            final MemberKey key = new MemberKey(m);
            MethodBuilder b = methods.get(key);
            if (b == null) {
                AnnotationCollector c = (_intr == null) ? AnnotationCollector.emptyCollector()
                        : collectAnnotations(m.getDeclaredAnnotations());
                methods.put(key, new MethodBuilder(tc, m, c));
            } else {
                if (_intr != null) {
                    b.annotations = collectDefaultAnnotations(b.annotations, m.getDeclaredAnnotations());
                }
                Method old = b.method;
                if (old == null) { // had "mix-over", replace
                    b.method = m;
//                } else if (old.getDeclaringClass().isInterface() && !m.getDeclaringClass().isInterface()) {
                } else if (Modifier.isAbstract(old.getModifiers())
                        && !Modifier.isAbstract(m.getModifiers())) {
                    // 06-Jan-2010, tatu: Except that if method we saw first is
                    // from an interface, and we now find a non-interface definition, we should
                    //   use this method, but with combination of annotations.
                    //   This helps (or rather, is essential) with JAXB annotations and
                    //   may also result in faster method calls (interface calls are slightly
                    //   costlier than regular method calls)
                    b.method = m;
                }
            }
        }
    }

    protected void _addMethodMixIns(TypeResolutionContext tc, Class<?> targetClass,
            Map<MemberKey,MethodBuilder> methods, Class<?> mixInCls)
    {
        if (_intr == null) {
            return;
        }
        for (Class<?> mixin : ClassUtil.findRawSuperTypes(mixInCls, targetClass, true)) {
            for (Method m : ClassUtil.getDeclaredMethods(mixin)) {
                if (!_isIncludableMemberMethod(m)) {
                    continue;
                }
                final MemberKey key = new MemberKey(m);
                MethodBuilder b = methods.get(key);
                Annotation[] anns = m.getDeclaredAnnotations();
                if (b == null) {
                    // nothing yet; add but do NOT specify method -- this marks it
                    // as "mix-over", floating mix-in
                    methods.put(key, new MethodBuilder(tc, null, collectAnnotations(anns)));
                } else {
                    b.annotations = collectDefaultAnnotations(b.annotations, anns);
                }
            }
        }
    }

    private boolean _isIncludableMemberMethod(Method m)
    {
        if (Modifier.isStatic(m.getModifiers())
                // Looks like generics can introduce hidden bridge and/or synthetic methods.
                // I don't think we want to consider those...
                || m.isSynthetic() || m.isBridge()) {
            return false;
        }
        // also, for now we have no use for methods with more than 2 arguments:
        // (2 argument methods for "any setter", fwtw)
        int pcount = m.getParameterTypes().length;
        return (pcount <= 2);
    }

    private final static class MethodBuilder {
        public final TypeResolutionContext typeContext;

        // Method left empty for "floating" mix-in, filled in as need be
        public Method method;
        public AnnotationCollector annotations;

        public MethodBuilder(TypeResolutionContext tc, Method m,
                AnnotationCollector ann) {
            typeContext = tc;
            method = m;
            annotations = ann;
        }

        public AnnotatedMethod build() {
            if (method == null) {
                return null;
            }
            // 12-Apr-2017, tatu: Note that parameter annotations are NOT collected -- we could
            //   collect them if that'd make sense but...
            return new AnnotatedMethod(typeContext, method, annotations.asAnnotationMap(), null);
        }
    }
}
