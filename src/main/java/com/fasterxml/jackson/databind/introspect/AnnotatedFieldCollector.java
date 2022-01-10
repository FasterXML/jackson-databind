package com.fasterxml.jackson.databind.introspect;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.cfg.MapperConfig;
import com.fasterxml.jackson.databind.util.ClassUtil;

public class AnnotatedFieldCollector
    extends CollectorBase
{
    private final MixInResolver _mixInResolver;

    private final boolean _collectAnnotations;

    // // // Collected state

    AnnotatedFieldCollector(MapperConfig<?> config, MixInResolver mixins,
            boolean collectAnnotations)
    {
        super(config);
        _mixInResolver = mixins;
        _collectAnnotations = collectAnnotations;
    }

    public static List<AnnotatedField> collectFields(MapperConfig<?> config,
            TypeResolutionContext tc, MixInResolver mixins,
            JavaType type, Class<?> primaryMixIn, boolean collectAnnotations)
    {
        return new AnnotatedFieldCollector(config, mixins, collectAnnotations)
                .collect(tc, type, primaryMixIn);
    }

    List<AnnotatedField> collect(TypeResolutionContext tc,
            JavaType type, Class<?> primaryMixIn)
    {
        Map<String,FieldBuilder> foundFields = _findFields(tc, type, primaryMixIn, null);
        if (foundFields == null) {
            return Collections.emptyList();
        }
        List<AnnotatedField> result = new ArrayList<>(foundFields.size());
        for (FieldBuilder b : foundFields.values()) {
            result.add(b.build());
        }
        return result;
    }

    private Map<String,FieldBuilder> _findFields(TypeResolutionContext tc,
            JavaType type, Class<?> mixin,
            Map<String,FieldBuilder> fields)
    {
        // First, a quick test: we only care for regular classes (not interfaces,
        //primitive types etc), except for Object.class. A simple check to rule out
        // other cases is to see if there is a super class or not.
        final JavaType parentType = type.getSuperClass();
        if (parentType == null) {
            return fields;
        }
        // Let's add super-class' fields first, then ours.
        {
            Class<?> parentMixin = (_mixInResolver == null) ? null
                    : _mixInResolver.findMixInClassFor(parentType.getRawClass());
            fields = _findFields(new TypeResolutionContext.Basic(_config.getTypeFactory(),
                    parentType.getBindings()),
                    parentType, parentMixin, fields);
        }
        final Class<?> rawType = type.getRawClass();
        for (Field f : rawType.getDeclaredFields()) {
            // static fields not included (transients are at this point, filtered out later)
            if (!_isIncludableField(f)) {
                continue;
            }
            // Ok now: we can (and need) not filter out ignorable fields at this point; partly
            // because mix-ins haven't been added, and partly because logic can be done
            // when determining get/settability of the field.
            if (fields == null) {
                fields = new LinkedHashMap<>();
            }
            FieldBuilder b = new FieldBuilder(tc, f);
            if (_collectAnnotations) {
                b.annotations = collectAnnotations(b.annotations, f.getDeclaredAnnotations());
            }
            fields.put(f.getName(), b);
        }
        // And then... any mix-in overrides?
        if ((fields != null) && (mixin != null)) {
            _addFieldMixIns(mixin, rawType, fields);
        }
        return fields;
    }

    /**
     * Method called to add field mix-ins from given mix-in class (and its fields)
     * into already collected actual fields (from introspected classes and their
     * super-classes)
     */
    private void _addFieldMixIns(Class<?> mixInCls, Class<?> targetClass,
            Map<String,FieldBuilder> fields)
    {
        List<Class<?>> parents = ClassUtil.findSuperClasses(mixInCls, targetClass, true);
        for (Class<?> mixin : parents) {
            for (Field mixinField : mixin.getDeclaredFields()) {
                // there are some dummy things (static, synthetic); better ignore
                if (!_isIncludableField(mixinField)) {
                    continue;
                }
                String name = mixinField.getName();
                // anything to mask? (if not, quietly ignore)
                FieldBuilder b = fields.get(name);
                if (b != null) {
                    b.annotations = collectAnnotations(b.annotations, mixinField.getDeclaredAnnotations());
                }
            }
        }
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

    private final static class FieldBuilder {
        public final TypeResolutionContext typeContext;
        public final Field field;

        public AnnotationCollector annotations;

        public FieldBuilder(TypeResolutionContext tc, Field f) {
            typeContext = tc;
            field = f;
            annotations = AnnotationCollector.emptyCollector();
        }

        public AnnotatedField build() {
            return new AnnotatedField(typeContext, field, annotations.asAnnotationMap());
        }
    }
}
