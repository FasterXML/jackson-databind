package com.fasterxml.jackson.databind.introspect;

import java.lang.annotation.Annotation;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;

import com.fasterxml.jackson.databind.AnnotationIntrospector;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.introspect.ClassIntrospector.MixInResolver;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.fasterxml.jackson.databind.util.ClassUtil;

public class AnnotatedFieldCollector
{
//    private final static Annotation[] NO_ANNOTATIONS = new Annotation[0];
    
    // // // Configuration

    private final AnnotationIntrospector _intr;
    private final MixInResolver _mixInResolver;
    
    // // // Collected state

    AnnotatedFieldCollector(AnnotationIntrospector intr,
            MixInResolver mixins)
    {
        _intr = intr;
        _mixInResolver = mixins;
    }

    public static List<AnnotatedField> collectFields(TypeResolutionContext tc,
            AnnotationIntrospector intr, MixInResolver mixins, TypeFactory types,
            JavaType type)
    {
        // Constructor also always members of resolved class, parent == resolution context
        return new AnnotatedFieldCollector(intr, mixins).collect(types, tc, type);
    }

    List<AnnotatedField> collect(TypeFactory types, TypeResolutionContext tc,
            JavaType type)
    {
        Map<String,AnnotatedField> foundFields = _findFields(types, tc, type, null);
        List<AnnotatedField> f;
        if (foundFields == null || foundFields.size() == 0) {
            f = Collections.emptyList();
        } else {
            f = new ArrayList<AnnotatedField>(foundFields.size());
            f.addAll(foundFields.values());
        }
        return f;
    }

    private Map<String,AnnotatedField> _findFields(TypeFactory typeFactory,
            TypeResolutionContext tc,
            JavaType type, Map<String,AnnotatedField> fields)
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
            fields = _findFields(typeFactory,
                    new TypeResolutionContext.Basic(typeFactory, parent.getBindings()),
                    parent, fields);
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
                fields.put(f.getName(), _constructField(tc, f));
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
    private void _addFieldMixIns(Class<?> mixInCls, Class<?> targetClass,
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
    
    private AnnotatedField _constructField(TypeResolutionContext tc, Field f)
    {
        if (_intr == null) { // when annotation processing is disabled
            return new AnnotatedField(tc, f, _emptyAnnotationMap());
        }
        return new AnnotatedField(tc, f, _collectRelevantAnnotations(f.getDeclaredAnnotations()));
    }

    private AnnotationMap _collectRelevantAnnotations(Annotation[] anns) {
        return _addAnnotationsIfNotPresent(new AnnotationMap(), anns);
    }

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
                _addAnnotationsIfNotPresent(result, fromBundles.toArray(new Annotation[fromBundles.size()]));
            }
        }
        return result;
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

    private static AnnotationMap _emptyAnnotationMap() {
        return new AnnotationMap();
    }
}
