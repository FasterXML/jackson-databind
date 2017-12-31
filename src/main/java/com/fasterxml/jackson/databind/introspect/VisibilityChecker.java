package com.fasterxml.jackson.databind.introspect;

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
     * Method that can be used for merging default values from `this`
     * instance with specified overrides; and either return `this`
     * if overrides had no effect (that is, result would be equal),
     * or a new instance with merged visibility settings.
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
     * specified minimum visibility level for fields.
     */
    public T withFieldVisibility(Visibility v);
    
    /**
     * Builder method that will return a checker instance that has
     * specified minimum visibility level for creator methods
     * (constructors, factory methods)
     */
    public T withCreatorVisibility(Visibility v);

    /**
     * @since 3.0
     */
    public T withScalarConstructorVisibility(Visibility v);
	
    // // Accessors
	
    /**
     * Method for checking whether given method is auto-detectable
     * as regular getter, with respect to its visibility (not considering
     * method signature or name, just visibility)
     */
    public boolean isGetterVisible(AnnotatedMethod m);

    /**
     * Method for checking whether given method is auto-detectable
     * as is-getter, with respect to its visibility (not considering
     * method signature or name, just visibility)
     */
    public boolean isIsGetterVisible(AnnotatedMethod m);
    
    /**
     * Method for checking whether given method is auto-detectable
     * as setter, with respect to its visibility (not considering
     * method signature or name, just visibility)
     */
    public boolean isSetterVisible(AnnotatedMethod m);

    /**
     * Method for checking whether given field is auto-detectable
     * as property, with respect to its visibility (not considering
     * method signature or name, just visibility)
     */
    public boolean isFieldVisible(AnnotatedField f);

    /**
     * Method for checking whether given creator (other than "scalar constructor",
     * see {@link #isScalarConstructorVisible}) is auto-detectable
     * as Creator, with respect to its visibility
     * (not considering signature, just visibility)
     */
    public boolean isCreatorVisible(AnnotatedMember m);

    /**
     * Method for checking whether given single-scalar-argument
     * constructor is auto-detectable
     * as delegating Creator, with respect to its visibility
     * (not considering signature, just visibility)
     * 
     * @since 3.0
     */
    public boolean isScalarConstructorVisible(AnnotatedMember m);

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
                Visibility.PUBLIC_ONLY, // field
                Visibility.PUBLIC_ONLY, // getter
                Visibility.PUBLIC_ONLY, // is-getter
                Visibility.ANY, // setter
                Visibility.PUBLIC_ONLY, // creator -- NOTE: was `ANY` for 2.x
                Visibility.NON_PRIVATE // scalar-constructor (new in 3.x)
                );

        protected final Visibility _fieldMinLevel;
        protected final Visibility _getterMinLevel;
        protected final Visibility _isGetterMinLevel;
        protected final Visibility _setterMinLevel;
        protected final Visibility _creatorMinLevel;
        protected final Visibility _scalarConstructorMinLevel;

        public static Std defaultInstance() { return DEFAULT; }
        
        /**
         * Constructor used for building instance that has minumum visibility
         * levels as indicated by given annotation instance
         * 
         * @param ann Annotations to use for determining minimum visibility levels
         */
        public Std(JsonAutoDetect ann)
        {
            // let's combine checks for enabled/disabled, with minimum level checks:
            _fieldMinLevel = ann.fieldVisibility();
            _getterMinLevel = ann.getterVisibility();
            _isGetterMinLevel = ann.isGetterVisibility();
            _setterMinLevel = ann.setterVisibility();
            _creatorMinLevel = ann.creatorVisibility();
            _scalarConstructorMinLevel = ann.scalarConstructorVisibility();
        }

        /**
         * Constructor that allows directly specifying minimum visibility levels to use
         */
        public Std(Visibility field,
                Visibility getter, Visibility isGetter, Visibility setter,
                Visibility creator, Visibility scalarConstructor)
        {
            
            _getterMinLevel = getter;
            _isGetterMinLevel = isGetter;
            _setterMinLevel = setter;
            _creatorMinLevel = creator;
            _fieldMinLevel = field;
            _scalarConstructorMinLevel = scalarConstructor;
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
                _scalarConstructorMinLevel = DEFAULT._scalarConstructorMinLevel;
            } else {
                _getterMinLevel = v;
                _isGetterMinLevel = v;
                _setterMinLevel = v;
                _creatorMinLevel = v;
                _fieldMinLevel = v;
                _scalarConstructorMinLevel = v;
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

        protected Std _with(Visibility f, Visibility g, Visibility isG, Visibility s,
                Visibility cr, Visibility scalarCr) {
            if ((f == _fieldMinLevel)
                    && (g == _getterMinLevel)
                    && (isG == _isGetterMinLevel)
                    && (s == _setterMinLevel)
                    && (cr == _creatorMinLevel)
                    && (scalarCr == _scalarConstructorMinLevel)) {
                return this;
            }
            return new Std(f, g, isG, s, cr, scalarCr);
        }

        @Override
        public Std withOverrides(JsonAutoDetect.Value vis)
        {
            if (vis == null) {
                return this;
            }
            return _with(
                    _defaultOrOverride(_fieldMinLevel, vis.getFieldVisibility()),
                    _defaultOrOverride(_getterMinLevel, vis.getGetterVisibility()),
                    _defaultOrOverride(_isGetterMinLevel, vis.getIsGetterVisibility()),
                    _defaultOrOverride(_setterMinLevel, vis.getSetterVisibility()),
                    _defaultOrOverride(_creatorMinLevel, vis.getCreatorVisibility()),
                    _defaultOrOverride(_scalarConstructorMinLevel, vis.getScalarConstructorVisibility())
                    );
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
        public Std withFieldVisibility(Visibility v) {
            if (v == Visibility.DEFAULT)  v = DEFAULT._fieldMinLevel;
            if (_fieldMinLevel == v) return this;
            return new Std(v, _getterMinLevel, _isGetterMinLevel, _setterMinLevel,
                    _creatorMinLevel, _scalarConstructorMinLevel);
        }

        @Override
        public Std withGetterVisibility(Visibility v) {
            if (v == Visibility.DEFAULT) v = DEFAULT._getterMinLevel;
            if (_getterMinLevel == v) return this;
            return new Std(_fieldMinLevel, v, _isGetterMinLevel, _setterMinLevel,
                    _creatorMinLevel, _scalarConstructorMinLevel);
        }

        @Override
        public Std withIsGetterVisibility(Visibility v) {
            if (v == Visibility.DEFAULT) v = DEFAULT._isGetterMinLevel;
            if (_isGetterMinLevel == v) return this;
            return new Std(_fieldMinLevel, _getterMinLevel, v, _setterMinLevel,
                    _creatorMinLevel, _scalarConstructorMinLevel);
        }

        @Override
        public Std withSetterVisibility(Visibility v) {
            if (v == Visibility.DEFAULT) v = DEFAULT._setterMinLevel;
            if (_setterMinLevel == v) return this;
            return new Std(_fieldMinLevel, _getterMinLevel, _isGetterMinLevel, v,
                    _creatorMinLevel, _scalarConstructorMinLevel);
        }

        @Override
        public Std withCreatorVisibility(Visibility v) {
            if (v == Visibility.DEFAULT) v = DEFAULT._creatorMinLevel;
            if (_creatorMinLevel == v) return this;
            return new Std(_fieldMinLevel, _getterMinLevel, _isGetterMinLevel, _setterMinLevel,
                    v, _scalarConstructorMinLevel);
        }

        @Override
        public Std withScalarConstructorVisibility(Visibility v) {
            if (v == Visibility.DEFAULT)  v = DEFAULT._scalarConstructorMinLevel;
            if (_scalarConstructorMinLevel == v) return this;
            return new Std(_fieldMinLevel, _getterMinLevel, _isGetterMinLevel, _setterMinLevel,
                    _creatorMinLevel, v);
        }

        /*
        /********************************************************
        /* Public API impl
        /********************************************************
         */

        @Override
        public boolean isFieldVisible(AnnotatedField f) {
            return _fieldMinLevel.isVisible(f.getAnnotated());
        }

        @Override
        public boolean isGetterVisible(AnnotatedMethod m) {
             return _getterMinLevel.isVisible(m.getAnnotated());
        }

        @Override
        public boolean isIsGetterVisible(AnnotatedMethod m) {
            return _isGetterMinLevel.isVisible(m.getAnnotated());
        }

        @Override
        public boolean isSetterVisible(AnnotatedMethod m) {
            return _setterMinLevel.isVisible(m.getAnnotated());
        }

        @Override
        public boolean isCreatorVisible(AnnotatedMember m) {
            return _creatorMinLevel.isVisible(m.getMember());
        }

        @Override
        public boolean isScalarConstructorVisible(AnnotatedMember m) {
            return _scalarConstructorMinLevel.isVisible(m.getMember());
        }

        /*
        /********************************************************
        /* Standard methods
        /********************************************************
         */
    
        @Override
        public String toString() {
            return String.format("[Visibility: field=%s,getter=%s,isGetter=%s,setter=%s,creator=%s,scalarConstructor=%s]",
                    _fieldMinLevel, _getterMinLevel, _isGetterMinLevel, _setterMinLevel,
                    _creatorMinLevel, _scalarConstructorMinLevel);
        }
    }
}
