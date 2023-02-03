package com.fasterxml.jackson.databind.introspect;

import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;


import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;

/**
 * Interface for object used for determine which property elements
 * (methods, fields, constructors) can be auto-detected, with respect
 * to their visibility modifiers.
 *<p>
 * Note on type declaration: funky recursive type is necessary to
 * support builder/fluent pattern.
 */
public interface VisibilityChecker<T extends VisibilityChecker<T>>
{
    // // Builder methods

    /**
     * Builder method that will return an instance that has same
     * settings as this instance has, except for values that
     * given annotation overrides.
     */
    public T with(JsonAutoDetect ann);

    /**
     * Method that can be used for merging default values from `this`
     * instance with specified overrides; and either return `this`
     * if overrides had no effect (that is, result would be equal),
     * or a new instance with merged visibility settings.
     *
     * @since 2.9
     */
    public T withOverrides(JsonAutoDetect.Value vis);

    /**
     * Builder method that will create and return an instance that has specified
     * {@link Visibility} value to use for all property elements.
     * Typical usage would be something like:
     *<pre>
     *  mapper.setVisibilityChecker(
     *     mapper.getVisibilityChecker().with(Visibility.NONE));
     *</pre>
     * (which would basically disable all auto-detection)
     */
    public T with(Visibility v);

    /**
     * Builder method that will create and return an instance that has specified
     * {@link Visibility} value to use for specified property.
     * Typical usage would be:
     *<pre>
     *  mapper.setVisibilityChecker(
     *     mapper.getVisibilityChecker().withVisibility(JsonMethod.FIELD, Visibility.ANY));
     *</pre>
     * (which would basically enable auto-detection for all member fields)
     */
    public T withVisibility(PropertyAccessor method, Visibility v);

    /**
     * Builder method that will return a checker instance that has
     * specified minimum visibility level for regular ("getXxx") getters.
     */
    public T withGetterVisibility(Visibility v);

    /**
     * Builder method that will return a checker instance that has
     * specified minimum visibility level for "is-getters" ("isXxx").
     */
    public T withIsGetterVisibility(Visibility v);

    /**
     * Builder method that will return a checker instance that has
     * specified minimum visibility level for setters.
     */
    public T withSetterVisibility(Visibility v);

    /**
     * Builder method that will return a checker instance that has
     * specified minimum visibility level for creator methods
     * (constructors, factory methods)
     */
    public T withCreatorVisibility(Visibility v);

    /**
     * Builder method that will return a checker instance that has
     * specified minimum visibility level for fields.
     */
    public T withFieldVisibility(Visibility v);

    // // Accessors

    /**
     * Method for checking whether given method is auto-detectable
     * as regular getter, with respect to its visibility (not considering
     * method signature or name, just visibility)
     */
    public boolean isGetterVisible(Method m);
    public boolean isGetterVisible(AnnotatedMethod m);

    /**
     * Method for checking whether given method is auto-detectable
     * as is-getter, with respect to its visibility (not considering
     * method signature or name, just visibility)
     */
    public boolean isIsGetterVisible(Method m);
    public boolean isIsGetterVisible(AnnotatedMethod m);

    /**
     * Method for checking whether given method is auto-detectable
     * as setter, with respect to its visibility (not considering
     * method signature or name, just visibility)
     */
    public boolean isSetterVisible(Method m);
    public boolean isSetterVisible(AnnotatedMethod m);

    /**
     * Method for checking whether given method is auto-detectable
     * as Creator, with respect to its visibility (not considering
     * method signature or name, just visibility)
     */
    public boolean isCreatorVisible(Member m);
    public boolean isCreatorVisible(AnnotatedMember m);

    /**
     * Method for checking whether given field is auto-detectable
     * as property, with respect to its visibility (not considering
     * method signature or name, just visibility)
     */
    public boolean isFieldVisible(Field f);
    public boolean isFieldVisible(AnnotatedField f);

    /*
    /********************************************************
    /* Standard implementation suitable for basic use
    /********************************************************
    */

   /**
    * Default standard implementation is purely based on visibility
    * modifier of given class members, and its configured minimum
    * levels.
    * Implemented using "builder" (or "Fluent") pattern, whereas instances
    * are immutable, and configuration is achieved by chainable factory
    * methods. As a result, type is declared is funky recursive generic
    * type, to allow for sub-classing of build methods with property type
    * co-variance.
    */
    public static class Std
        implements VisibilityChecker<Std>,
            java.io.Serializable
    {
        private static final long serialVersionUID = 1;

        /**
         * This is the canonical base instance, configured with default
         * visibility values
         */
        protected final static Std DEFAULT = new Std(
                Visibility.PUBLIC_ONLY, // getter
                Visibility.PUBLIC_ONLY, // is-getter
                Visibility.ANY, // setter
                Visibility.ANY, // creator -- legacy, to support single-arg ctors
                Visibility.PUBLIC_ONLY // field
                );

        /**
         * Alternate base settings used for JDK types: public visibility
         * required for everything
         *
         * @since 2.13
         */
        protected final static Std ALL_PUBLIC = new Std(
                Visibility.PUBLIC_ONLY, // getter
                Visibility.PUBLIC_ONLY, // is-getter
                Visibility.PUBLIC_ONLY, // setter
                Visibility.PUBLIC_ONLY, // creator (single-arg ctors)
                Visibility.PUBLIC_ONLY // field
        );

        protected final Visibility _getterMinLevel;
        protected final Visibility _isGetterMinLevel;
        protected final Visibility _setterMinLevel;
        protected final Visibility _creatorMinLevel;
        protected final Visibility _fieldMinLevel;

        /**
         * @return Instance with default settings: "public" for getters/is-getters,
         *    fields; "any" for setters and legacy 1-arg constructors
         */
        public static Std defaultInstance() { return DEFAULT; }

        /**
         * @return Instance with all visibility levels set to "public"
         *
         * @since 2.13
         */
        public static Std allPublicInstance() { return ALL_PUBLIC; }

        /**
         * Constructor used for building instance that has minumum visibility
         * levels as indicated by given annotation instance
         *
         * @param ann Annotations to use for determining minimum visibility levels
         */
        public Std(JsonAutoDetect ann)
        {
            // let's combine checks for enabled/disabled, with minimum level checks:
            _getterMinLevel = ann.getterVisibility();
            _isGetterMinLevel = ann.isGetterVisibility();
            _setterMinLevel = ann.setterVisibility();
            _creatorMinLevel = ann.creatorVisibility();
            _fieldMinLevel = ann.fieldVisibility();
        }

        /**
         * Constructor that allows directly specifying minimum visibility levels to use
         */
        public Std(Visibility getter, Visibility isGetter, Visibility setter,
                Visibility creator, Visibility field)
        {
            _getterMinLevel = getter;
            _isGetterMinLevel = isGetter;
            _setterMinLevel = setter;
            _creatorMinLevel = creator;
            _fieldMinLevel = field;
        }

        /**
         * Constructor that will assign given visibility value for all
         * properties.
         *
         * @param v level to use for all property types
         */
        public Std(Visibility v)
        {
            // typically we shouldn't get this value; but let's handle it if we do:
            if (v == Visibility.DEFAULT) {
                _getterMinLevel = DEFAULT._getterMinLevel;
                _isGetterMinLevel = DEFAULT._isGetterMinLevel;
                _setterMinLevel = DEFAULT._setterMinLevel;
                _creatorMinLevel = DEFAULT._creatorMinLevel;
                _fieldMinLevel = DEFAULT._fieldMinLevel;
            } else {
                _getterMinLevel = v;
                _isGetterMinLevel = v;
                _setterMinLevel = v;
                _creatorMinLevel = v;
                _fieldMinLevel = v;
            }
        }

        /**
         * @since 2.9
         */
        public static Std construct(JsonAutoDetect.Value vis) {
            return DEFAULT.withOverrides(vis);
        }

        /*
        /********************************************************
        /* Builder/fluent methods for instantiating configured
        /* instances
        /********************************************************
         */

        protected Std _with(Visibility g, Visibility isG, Visibility s,
                Visibility cr, Visibility f) {
            if ((g == _getterMinLevel)
                    && (isG == _isGetterMinLevel)
                    && (s == _setterMinLevel)
                    && (cr == _creatorMinLevel)
                    && (f == _fieldMinLevel)
                    ) {
                return this;
            }
            return new Std(g, isG, s, cr, f);
        }

        @Override
        public Std with(JsonAutoDetect ann)
        {
            Std curr = this;
            if (ann != null) {
                return _with(
                        _defaultOrOverride(_getterMinLevel, ann.getterVisibility()),
                        _defaultOrOverride(_isGetterMinLevel, ann.isGetterVisibility()),
                        _defaultOrOverride(_setterMinLevel, ann.setterVisibility()),
                        _defaultOrOverride(_creatorMinLevel, ann.creatorVisibility()),
                        _defaultOrOverride(_fieldMinLevel, ann.fieldVisibility())
                        );
            }
            return curr;
        }

        @Override // since 2.9
        public Std withOverrides(JsonAutoDetect.Value vis)
        {
            Std curr = this;
            if (vis != null) {
                return _with(
                        _defaultOrOverride(_getterMinLevel, vis.getGetterVisibility()),
                        _defaultOrOverride(_isGetterMinLevel, vis.getIsGetterVisibility()),
                        _defaultOrOverride(_setterMinLevel, vis.getSetterVisibility()),
                        _defaultOrOverride(_creatorMinLevel, vis.getCreatorVisibility()),
                        _defaultOrOverride(_fieldMinLevel, vis.getFieldVisibility())
                        );
            }
            return curr;
        }

        private Visibility _defaultOrOverride(Visibility defaults, Visibility override) {
            if (override == Visibility.DEFAULT) {
                return defaults;
            }
            return override;
        }

        @Override
        public Std with(Visibility v)
        {
            if (v == Visibility.DEFAULT) {
                return DEFAULT;
            }
            return new Std(v);
        }

        @Override
        public Std withVisibility(PropertyAccessor method, Visibility v)
        {
            switch (method) {
            case GETTER:
                return withGetterVisibility(v);
            case SETTER:
                return withSetterVisibility(v);
            case CREATOR:
                return withCreatorVisibility(v);
            case FIELD:
                return withFieldVisibility(v);
            case IS_GETTER:
                return withIsGetterVisibility(v);
            case ALL:
                return with(v);
            //case NONE:
            default:
                // break;
                return this;
            }
        }

        @Override
        public Std withGetterVisibility(Visibility v) {
            if (v == Visibility.DEFAULT) v = DEFAULT._getterMinLevel;
            if (_getterMinLevel == v) return this;
            return new Std(v, _isGetterMinLevel, _setterMinLevel, _creatorMinLevel, _fieldMinLevel);
        }

        @Override
        public Std withIsGetterVisibility(Visibility v) {
            if (v == Visibility.DEFAULT) v = DEFAULT._isGetterMinLevel;
            if (_isGetterMinLevel == v) return this;
            return new Std(_getterMinLevel, v, _setterMinLevel, _creatorMinLevel, _fieldMinLevel);
        }

        @Override
        public Std withSetterVisibility(Visibility v) {
            if (v == Visibility.DEFAULT) v = DEFAULT._setterMinLevel;
            if (_setterMinLevel == v) return this;
            return new Std(_getterMinLevel, _isGetterMinLevel, v, _creatorMinLevel, _fieldMinLevel);
        }

        @Override
        public Std withCreatorVisibility(Visibility v) {
            if (v == Visibility.DEFAULT) v = DEFAULT._creatorMinLevel;
            if (_creatorMinLevel == v) return this;
            return new Std(_getterMinLevel, _isGetterMinLevel, _setterMinLevel, v, _fieldMinLevel);
        }

        @Override
        public Std withFieldVisibility(Visibility v) {
            if (v == Visibility.DEFAULT)  v = DEFAULT._fieldMinLevel;
            if (_fieldMinLevel == v) return this;
            return new Std(_getterMinLevel, _isGetterMinLevel, _setterMinLevel, _creatorMinLevel, v);
        }

        /*
        /********************************************************
        /* Public API impl
        /********************************************************
         */

        @Override
        public boolean isCreatorVisible(Member m) {
            return _creatorMinLevel.isVisible(m);
        }

        @Override
        public boolean isCreatorVisible(AnnotatedMember m) {
            return isCreatorVisible(m.getMember());
        }

        @Override
        public boolean isFieldVisible(Field f) {
            return _fieldMinLevel.isVisible(f);
        }

        @Override
        public boolean isFieldVisible(AnnotatedField f) {
            return isFieldVisible(f.getAnnotated());
        }

        @Override
        public boolean isGetterVisible(Method m) {
            return _getterMinLevel.isVisible(m);
        }

        @Override
        public boolean isGetterVisible(AnnotatedMethod m) {
             return isGetterVisible(m.getAnnotated());
        }

        @Override
        public boolean isIsGetterVisible(Method m) {
            return _isGetterMinLevel.isVisible(m);
        }

        @Override
        public boolean isIsGetterVisible(AnnotatedMethod m) {
            return isIsGetterVisible(m.getAnnotated());
        }

        @Override
        public boolean isSetterVisible(Method m) {
            return _setterMinLevel.isVisible(m);
        }

        @Override
        public boolean isSetterVisible(AnnotatedMethod m) {
            return isSetterVisible(m.getAnnotated());
        }

        /*
        /********************************************************
        /* Standard methods
        /********************************************************
         */

        @Override
        public String toString() {
            return String.format("[Visibility: getter=%s,isGetter=%s,setter=%s,creator=%s,field=%s]",
                    _getterMinLevel, _isGetterMinLevel, _setterMinLevel, _creatorMinLevel, _fieldMinLevel);
        }
    }
}
