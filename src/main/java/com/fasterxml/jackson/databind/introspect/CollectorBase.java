package com.fasterxml.jackson.databind.introspect;

import java.lang.annotation.Annotation;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import com.fasterxml.jackson.databind.AnnotationIntrospector;
import com.fasterxml.jackson.databind.util.ClassUtil;

// @since 2.9
class CollectorBase
{
    protected final static AnnotationMap[] NO_ANNOTATION_MAPS = new AnnotationMap[0];
    protected final static Annotation[] NO_ANNOTATIONS = new Annotation[0];

    protected final AnnotationIntrospector _intr;

    protected CollectorBase(AnnotationIntrospector intr) {
        _intr = intr;
    }

    // // // Annotation overrides ("mix over")

    protected final AnnotationCollector collectAnnotations(Annotation[] anns) {
        AnnotationCollector c = AnnotationCollector.emptyCollector();
        for (int i = 0, end = anns.length; i < end; ++i) {
            Annotation ann = anns[i];
            c = c.addOrOverride(ann);
            if (_intr.isAnnotationBundle(ann)) {
                c = collectFromBundle(c, ann);
            }
        }
        return c;
    }

    protected final AnnotationCollector collectAnnotations(AnnotationCollector c, Annotation[] anns) {
        for (int i = 0, end = anns.length; i < end; ++i) {
            Annotation ann = anns[i];
            c = c.addOrOverride(ann);
            if (_intr.isAnnotationBundle(ann)) {
                c = collectFromBundle(c, ann);
            }
        }
        return c;
    }

    protected final AnnotationCollector collectFromBundle(AnnotationCollector c, Annotation bundle) {
        Annotation[] anns = ClassUtil.findClassAnnotations(bundle.annotationType());
        for (int i = 0, end = anns.length; i < end; ++i) {
            Annotation ann = anns[i];
            // minor optimization: by-pass 2 common JDK meta-annotations
            if (_ignorableAnnotation(ann)) {
                continue;
            }
            if (_intr.isAnnotationBundle(ann)) {
                // 11-Apr-2017, tatu: Also must guard against recursive definitions...
                if (!c.isPresent(ann)) {
                    c = c.addOrOverride(ann);
                    c = collectFromBundle(c, ann);
                }
            } else {
                c = c.addOrOverride(ann);
            }
        }
        return c;
    }

    // // // Defaulting ("mix under")

    // Variant that only adds annotations that are missing
    protected final AnnotationCollector collectDefaultAnnotations(AnnotationCollector c,
            Annotation[] anns) {
        for (int i = 0, end = anns.length; i < end; ++i) {
            Annotation ann = anns[i];
            if (!c.isPresent(ann)) {
                c = c.addOrOverride(ann);
                if (_intr.isAnnotationBundle(ann)) {
                    c = collectDefaultFromBundle(c, ann);
                }
            }
        }
        return c;
    }

    protected final AnnotationCollector collectDefaultFromBundle(AnnotationCollector c,
            Annotation bundle) {
        Annotation[] anns = ClassUtil.findClassAnnotations(bundle.annotationType());
        for (int i = 0, end = anns.length; i < end; ++i) {
            Annotation ann = anns[i];
            // minor optimization: by-pass 2 common JDK meta-annotations
            if (_ignorableAnnotation(ann)) {
                continue;
            }
            // also only defaulting, not overrides:
            if (!c.isPresent(ann)) {
                c = c.addOrOverride(ann);
                if (_intr.isAnnotationBundle(ann)) {
                    c = collectFromBundle(c, ann);
                }
            }
        }
        return c;
    }

    protected final static boolean _ignorableAnnotation(Annotation a) {
        return (a instanceof Target) || (a instanceof Retention);
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
}
