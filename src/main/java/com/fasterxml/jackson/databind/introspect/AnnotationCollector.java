package com.fasterxml.jackson.databind.introspect;

import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import com.fasterxml.jackson.databind.util.Annotations;

/**
 * Helper class used to collect annotations to be stored as
 * {@link com.fasterxml.jackson.databind.util.Annotations} (like {@link AnnotationMap}).
 *
 * @since 2.9
 */
public class AnnotationCollector
{
    protected final static Annotations NO_ANNOTATIONS = new NoAnnotations();

    // // // For now, add support for small number of super compact impls
    
    protected Class<?> _firstType;
    protected Annotation _firstValue;

    protected HashMap<Class<?>,Annotation> _annotations;

    public AnnotationCollector() { }

    public static Annotations emptyAnnotations() { return NO_ANNOTATIONS; }
    
    public Annotations asAnnotations() {
        if (_annotations == null) { // 0 or 1
            if (_firstType == null) {
                return NO_ANNOTATIONS;
            }
            return new OneAnnotation(_firstType, _firstValue);
        }
        if (_annotations.size() == 2) {
            Iterator<Map.Entry<Class<?>,Annotation>> it = _annotations.entrySet().iterator();
            Map.Entry<Class<?>,Annotation> en1 = it.next(), en2 = it.next();
            return new TwoAnnotations(en1.getKey(), en1.getValue(),
                    en2.getKey(), en2.getValue());
        }
        return new AnnotationMap(_annotations);
    }

    /*
    /**********************************************************
    /* API
    /**********************************************************
     */

    public boolean addIfNotPresent(Annotation ann) {
        final Class<?> type = ann.annotationType();
        if (_annotations == null) {
            if (_firstType == null) {
                _firstType = type;
                _firstValue = ann;
                return true;
            }
            if (_firstType == type) {
                return false;
            }
            // Otherwise, "upgrade" to a Map
            _annotations = new HashMap<>();
            _annotations.put(_firstType, _firstValue);
        } else if (_annotations.containsKey(type)) {
            return false;
        }
        _annotations.put(type, ann);
        return true;
    }

    /*
    /**********************************************************
    /* Annotations implementations
    /**********************************************************
     */

    /**
     * Immutable implementation for case where no annotations are associated with
     * an annotatable entity.
     *
     * @since 2.9
     */
    public static class NoAnnotations
        implements Annotations, java.io.Serializable
    {
        private static final long serialVersionUID = 1L;

        NoAnnotations() { }

        @Override
        public <A extends Annotation> A get(Class<A> cls) {
            return null;
        }

        @Override
        public boolean has(Class<?> cls) {
            return false;
        }

        @Override
        public boolean hasOneOf(Class<? extends Annotation>[] annoClasses) {
            return false;
        }

        @Override
        public int size() {
            return 0;
        }
    }

    public static class OneAnnotation
        implements Annotations, java.io.Serializable
    {
        private static final long serialVersionUID = 1L;

        private final Class<?> _type;
        private final Annotation _value;

        public OneAnnotation(Class<?> type, Annotation value) {
            _type = type;
            _value = value;
        }

        @SuppressWarnings("unchecked")
        @Override
        public <A extends Annotation> A get(Class<A> cls) {
            if (_type == cls) {
                return (A) _value;
            }
            return null;
        }

        @Override
        public boolean has(Class<?> cls) {
            return (_type == cls);
        }

        @Override
        public boolean hasOneOf(Class<? extends Annotation>[] annoClasses) {
            for (Class<?> cls : annoClasses) {
                if (cls == _type) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public int size() {
            return 1;
        }
    }

    public static class TwoAnnotations
        implements Annotations, java.io.Serializable
    {
        private static final long serialVersionUID = 1L;
    
        private final Class<?> _type1, _type2;
        private final Annotation _value1, _value2;
    
        public TwoAnnotations(Class<?> type1, Annotation value1,
                Class<?> type2, Annotation value2) {
            _type1 = type1;
            _value1 = value1;
            _type2 = type2;
            _value2 = value2;
        }

        @SuppressWarnings("unchecked")
        @Override
        public <A extends Annotation> A get(Class<A> cls) {
            if (_type1 == cls) {
                return (A) _value1;
            }
            if (_type2 == cls) {
                return (A) _value2;
            }
            return null;
        }

        @Override
        public boolean has(Class<?> cls) {
            return (_type1 == cls) || (_type2 == cls);
        }

        @Override
        public boolean hasOneOf(Class<? extends Annotation>[] annoClasses) {
            for (Class<?> cls : annoClasses) {
                if ((cls == _type1) || (cls == _type2)) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public int size() {
            return 2;
        }
    }
}
