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
public abstract class AnnotationCollector
{
    protected final static Annotations NO_ANNOTATIONS = new NoAnnotations();

    /**
     * Optional data to carry along
     */
    protected final Object _data;

    protected AnnotationCollector(Object d) {
        _data = d;
    }

    public static Annotations emptyAnnotations() { return NO_ANNOTATIONS; }

    public static AnnotationCollector emptyCollector() {
        return EmptyCollector.instance;
    }

    public static AnnotationCollector emptyCollector(Object data) {
        return new EmptyCollector(data);
    }

    public abstract Annotations asAnnotations();
    public abstract AnnotationMap asAnnotationMap();

    public Object getData() {
        return _data;
    }

    /*
    /**********************************************************
    /* API
    /**********************************************************
     */

    public abstract boolean isPresent(Annotation ann);

    public abstract AnnotationCollector addOrOverride(Annotation ann);

    /*
    /**********************************************************
    /* Collector implementations
    /**********************************************************
     */

    static class EmptyCollector extends AnnotationCollector
    {
        public final static EmptyCollector instance = new EmptyCollector(null);

        EmptyCollector(Object data) { super(data); }

        @Override
        public Annotations asAnnotations() {
            return NO_ANNOTATIONS;
        }

        @Override
        public AnnotationMap asAnnotationMap() {
            return new AnnotationMap();
        }

        @Override
        public boolean isPresent(Annotation ann) { return false; }

        @Override
        public AnnotationCollector addOrOverride(Annotation ann) {
            return new OneCollector(_data, ann.annotationType(), ann);
        }
    }

    static class OneCollector extends AnnotationCollector
    {
        private Class<?> _type;
        private Annotation _value;

        public OneCollector(Object data,
                Class<?> type, Annotation value) {
            super(data);
            _type = type;
            _value = value;
        }

        @Override
        public Annotations asAnnotations() {
            return new OneAnnotation(_type, _value);
        }

        @Override
        public AnnotationMap asAnnotationMap() {
            return AnnotationMap.of(_type, _value);
        }

        @Override
        public boolean isPresent(Annotation ann) {
            return ann.annotationType() == _type;
        }

        @Override
        public AnnotationCollector addOrOverride(Annotation ann) {
            final Class<?> type = ann.annotationType();
            // true override? Just replace in-place, return
            if (_type == type) {
                _value = ann;
                return this;
            }
            return new NCollector(_data, _type, _value, type, ann);
        }
    }

    static class NCollector extends AnnotationCollector
    {
        protected final HashMap<Class<?>,Annotation> _annotations;

        public NCollector(Object data,
                Class<?> type1, Annotation value1,
                Class<?> type2, Annotation value2) {
            super(data);
            _annotations = new HashMap<>();
            _annotations.put(type1, value1);
            _annotations.put(type2, value2);
        }

        @Override
        public Annotations asAnnotations() {
            if (_annotations.size() == 2) {
                Iterator<Map.Entry<Class<?>,Annotation>> it = _annotations.entrySet().iterator();
                Map.Entry<Class<?>,Annotation> en1 = it.next(), en2 = it.next();
                return new TwoAnnotations(en1.getKey(), en1.getValue(),
                        en2.getKey(), en2.getValue());
            }
            return new AnnotationMap(_annotations);
        }

        @Override
        public AnnotationMap asAnnotationMap() {
            AnnotationMap result = new AnnotationMap();
            for (Annotation ann : _annotations.values()) {
                result.add(ann);
            }
            return result;
        }

        @Override
        public boolean isPresent(Annotation ann) {
            return _annotations.containsKey(ann.annotationType());
        }

        @Override
        public AnnotationCollector addOrOverride(Annotation ann) {
            _annotations.put(ann.annotationType(), ann);
            return this;
        }
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
