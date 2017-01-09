package com.fasterxml.jackson.databind.deser.impl;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Member;
import java.lang.reflect.Type;
import java.util.*;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.cfg.MapperConfig;
import com.fasterxml.jackson.databind.deser.SettableBeanProperty;
import com.fasterxml.jackson.databind.deser.ValueInstantiator;
import com.fasterxml.jackson.databind.deser.std.StdValueInstantiator;
import com.fasterxml.jackson.databind.introspect.*;
import com.fasterxml.jackson.databind.util.ClassUtil;

/**
 * Container class for storing information on creators (based on annotations,
 * visibility), to be able to build actual instantiator later on.
 */
public class CreatorCollector {
    // Since 2.5
    protected final static int C_DEFAULT = 0;
    protected final static int C_STRING = 1;
    protected final static int C_INT = 2;
    protected final static int C_LONG = 3;
    protected final static int C_DOUBLE = 4;
    protected final static int C_BOOLEAN = 5;
    protected final static int C_DELEGATE = 6;
    protected final static int C_PROPS = 7;
    protected final static int C_ARRAY_DELEGATE = 8;

    protected final static String[] TYPE_DESCS = new String[] { "default",
            "from-String", "from-int", "from-long", "from-double",
            "from-boolean", "delegate", "property-based" };

    /// Type of bean being created
    final protected BeanDescription _beanDesc;

    final protected boolean _canFixAccess;

    /**
     * @since 2.7
     */
    final protected boolean _forceAccess;

    /**
     * Set of creators we have collected so far
     * 
     * @since 2.5
     */
    protected final AnnotatedWithParams[] _creators = new AnnotatedWithParams[9];

    /**
     * Bitmask of creators that were explicitly marked as creators; false for
     * auto-detected (ones included base on naming and/or visibility, not
     * annotation)
     * 
     * @since 2.5
     */
    protected int _explicitCreators = 0;

    protected boolean _hasNonDefaultCreator = false;

    // when there are injectable values along with delegate:
    protected SettableBeanProperty[] _delegateArgs;

    protected SettableBeanProperty[] _arrayDelegateArgs;

    protected SettableBeanProperty[] _propertyBasedArgs;

    protected AnnotatedParameter _incompleteParameter;

    /*
    /**********************************************************
    /* Life-cycle
    /**********************************************************
     */

    public CreatorCollector(BeanDescription beanDesc, MapperConfig<?> config) {
        _beanDesc = beanDesc;
        _canFixAccess = config.canOverrideAccessModifiers();
        _forceAccess = config
                .isEnabled(MapperFeature.OVERRIDE_PUBLIC_ACCESS_MODIFIERS);
    }

    public ValueInstantiator constructValueInstantiator(
            DeserializationConfig config) {
        final JavaType delegateType = _computeDelegateType(
                _creators[C_DELEGATE], _delegateArgs);
        final JavaType arrayDelegateType = _computeDelegateType(
                _creators[C_ARRAY_DELEGATE], _arrayDelegateArgs);
        final JavaType type = _beanDesc.getType();

        // 11-Jul-2016, tatu: Earlier optimization by replacing the whole
        // instantiator did not
        // work well, so let's replace by lower-level check:
        AnnotatedWithParams defaultCtor = StdTypeConstructor
                .tryToOptimize(_creators[C_DEFAULT]);

        StdValueInstantiator inst = new StdValueInstantiator(config, type);
        inst.configureFromObjectSettings(defaultCtor, _creators[C_DELEGATE],
                delegateType, _delegateArgs, _creators[C_PROPS],
                _propertyBasedArgs);
        inst.configureFromArraySettings(_creators[C_ARRAY_DELEGATE],
                arrayDelegateType, _arrayDelegateArgs);
        inst.configureFromStringCreator(_creators[C_STRING]);
        inst.configureFromIntCreator(_creators[C_INT]);
        inst.configureFromLongCreator(_creators[C_LONG]);
        inst.configureFromDoubleCreator(_creators[C_DOUBLE]);
        inst.configureFromBooleanCreator(_creators[C_BOOLEAN]);
        inst.configureIncompleteParameter(_incompleteParameter);
        return inst;
    }

    /*
    /**********************************************************
    /* Setters
    /**********************************************************
     */

    /**
     * Method called to indicate the default creator: no-arguments constructor
     * or factory method that is called to instantiate a value before populating
     * it with data. Default creator is only used if no other creators are
     * indicated.
     * 
     * @param creator
     *            Creator method; no-arguments constructor or static factory
     *            method.
     */
    public void setDefaultCreator(AnnotatedWithParams creator) {
        _creators[C_DEFAULT] = _fixAccess(creator);
    }

    public void addStringCreator(AnnotatedWithParams creator,
            boolean explicit) {
        verifyNonDup(creator, C_STRING, explicit);
    }

    public void addIntCreator(AnnotatedWithParams creator, boolean explicit) {
        verifyNonDup(creator, C_INT, explicit);
    }

    public void addLongCreator(AnnotatedWithParams creator, boolean explicit) {
        verifyNonDup(creator, C_LONG, explicit);
    }

    public void addDoubleCreator(AnnotatedWithParams creator,
            boolean explicit) {
        verifyNonDup(creator, C_DOUBLE, explicit);
    }

    public void addBooleanCreator(AnnotatedWithParams creator,
            boolean explicit) {
        verifyNonDup(creator, C_BOOLEAN, explicit);
    }

    public void addDelegatingCreator(AnnotatedWithParams creator,
            boolean explicit, SettableBeanProperty[] injectables) {
        if (creator.getParameterType(0).isCollectionLikeType()) {
            if (verifyNonDup(creator, C_ARRAY_DELEGATE, explicit)) {
                _arrayDelegateArgs = injectables;
            }
        } else {
            if (verifyNonDup(creator, C_DELEGATE, explicit)) {
                _delegateArgs = injectables;
            }
        }
    }

    public void addPropertyCreator(AnnotatedWithParams creator,
            boolean explicit, SettableBeanProperty[] properties) {
        if (verifyNonDup(creator, C_PROPS, explicit)) {
            // Better ensure we have no duplicate names either...
            if (properties.length > 1) {
                HashMap<String, Integer> names = new HashMap<String, Integer>();
                for (int i = 0, len = properties.length; i < len; ++i) {
                    String name = properties[i].getName();
                    // Need to consider Injectables, which may not have
                    // a name at all, and need to be skipped
                    if (name.length() == 0
                            && properties[i].getInjectableValueId() != null) {
                        continue;
                    }
                    Integer old = names.put(name, Integer.valueOf(i));
                    if (old != null) {
                        throw new IllegalArgumentException(String.format(
                                "Duplicate creator property \"%s\" (index %s vs %d)",
                                name, old, i));
                    }
                }
            }
            _propertyBasedArgs = properties;
        }
    }

    public void addIncompeteParameter(AnnotatedParameter parameter) {
        if (_incompleteParameter == null) {
            _incompleteParameter = parameter;
        }
    }

    /*
    /**********************************************************
    /* Accessors
    /**********************************************************
     */

    /**
     * @since 2.1
     */
    public boolean hasDefaultCreator() {
        return _creators[C_DEFAULT] != null;
    }

    /**
     * @since 2.6
     */
    public boolean hasDelegatingCreator() {
        return _creators[C_DELEGATE] != null;
    }

    /**
     * @since 2.6
     */
    public boolean hasPropertyBasedCreator() {
        return _creators[C_PROPS] != null;
    }

    /*
    /**********************************************************
    /* Helper methods
    /**********************************************************
     */

    private JavaType _computeDelegateType(AnnotatedWithParams creator,
            SettableBeanProperty[] delegateArgs) {
        if (!_hasNonDefaultCreator || (creator == null)) {
            return null;
        }
        // need to find type...
        int ix = 0;
        if (delegateArgs != null) {
            for (int i = 0, len = delegateArgs.length; i < len; ++i) {
                if (delegateArgs[i] == null) { // marker for delegate itself
                    ix = i;
                    break;
                }
            }
        }
        return creator.getParameterType(ix);
    }

    private <T extends AnnotatedMember> T _fixAccess(T member) {
        if (member != null && _canFixAccess) {
            ClassUtil.checkAndFixAccess((Member) member.getAnnotated(),
                    _forceAccess);
        }
        return member;
    }

    /**
     * @return True if specified Creator is to be used
     */
    protected boolean verifyNonDup(AnnotatedWithParams newOne, int typeIndex, boolean explicit)
    {
        final int mask = (1 << typeIndex);
        _hasNonDefaultCreator = true;
        AnnotatedWithParams oldOne = _creators[typeIndex];
        // already had an explicitly marked one?
        if (oldOne != null) {
            boolean verify;
            if ((_explicitCreators & mask) != 0) { // already had explicitly annotated, leave as-is
                // but skip, if new one not annotated
                if (!explicit) {
                    return false;
                }
                // both explicit: verify
                verify = true;
            } else {
                // otherwise only verify if neither explicitly annotated.
                verify = !explicit;
            }

            // one more thing: ok to override in sub-class
            if (verify && (oldOne.getClass() == newOne.getClass())) {
                // [databind#667]: avoid one particular class of bogus problems
                Class<?> oldType = oldOne.getRawParameterType(0);
                Class<?> newType = newOne.getRawParameterType(0);

                if (oldType == newType) {
                    // 13-Jul-2016, tatu: One more thing to check; since Enum
                    // classes always have
                    // implicitly created `valueOf()`, let's resolve in favor of
                    // other implicit
                    // creator (`fromString()`)
                    if (_isEnumValueOf(newOne)) {
                        return false; // ignore
                    }
                    if (_isEnumValueOf(oldOne)) {
                        ;
                    } else {
                        throw new IllegalArgumentException(String.format(
                                "Conflicting %s creators: already had %s creator %s, encountered another: %s",
                                TYPE_DESCS[typeIndex],
                                explicit ? "explicitly marked"
                                        : "implicitly discovered",
                                oldOne, newOne));
                    }
                }
                // otherwise, which one to choose?
                else if (newType.isAssignableFrom(oldType)) {
                    // new type more generic, use old
                    return false;
                }
                // new type more specific, use it
            }
        }
        if (explicit) {
            _explicitCreators |= mask;
        }
        _creators[typeIndex] = _fixAccess(newOne);
        return true;
    }

    /**
     * Helper method for recognizing `Enum.valueOf()` factory method
     *
     * @since 2.8.1
     */
    protected boolean _isEnumValueOf(AnnotatedWithParams creator) {
        return creator.getDeclaringClass().isEnum()
                && "valueOf".equals(creator.getName());
    }

    /*
    /**********************************************************
    /* Helper class(es)
    /**********************************************************
     */

    /**
     * Replacement for default constructor to use for a small set of
     * "well-known" types.
     * <p>
     * Note: replaces earlier <code>Vanilla</code>
     * <code>ValueInstantiator</code> implementation
     *
     * @since 2.8.1 (replacing earlier <code>Vanilla</code> instantiator
     */
    protected final static class StdTypeConstructor extends AnnotatedWithParams
            implements java.io.Serializable {
        private static final long serialVersionUID = 1L;

        public final static int TYPE_ARRAY_LIST = 1;
        public final static int TYPE_HASH_MAP = 2;
        public final static int TYPE_LINKED_HASH_MAP = 3;

        private final AnnotatedWithParams _base;

        private final int _type;

        public StdTypeConstructor(AnnotatedWithParams base, int t) {
            super(base, null);
            _base = base;
            _type = t;
        }

        public static AnnotatedWithParams tryToOptimize(
                AnnotatedWithParams src) {
            if (src != null) {
                final Class<?> rawType = src.getDeclaringClass();
                if (rawType == List.class || rawType == ArrayList.class) {
                    return new StdTypeConstructor(src, TYPE_ARRAY_LIST);
                }
                if (rawType == LinkedHashMap.class) {
                    return new StdTypeConstructor(src, TYPE_LINKED_HASH_MAP);
                }
                if (rawType == HashMap.class) {
                    return new StdTypeConstructor(src, TYPE_HASH_MAP);
                }
            }
            return src;
        }

        protected final Object _construct() {
            switch (_type) {
            case TYPE_ARRAY_LIST:
                return new ArrayList<Object>();
            case TYPE_LINKED_HASH_MAP:
                return new LinkedHashMap<String, Object>();
            case TYPE_HASH_MAP:
                return new HashMap<String, Object>();
            }
            throw new IllegalStateException("Unknown type " + _type);
        }

        @Override
        public int getParameterCount() {
            return _base.getParameterCount();
        }

        @Override
        public Class<?> getRawParameterType(int index) {
            return _base.getRawParameterType(index);
        }

        @Override
        public JavaType getParameterType(int index) {
            return _base.getParameterType(index);
        }

        @Override
        @Deprecated
        public Type getGenericParameterType(int index) {
            return _base.getGenericParameterType(index);
        }

        @Override
        public Object call() throws Exception {
            return _construct();
        }

        @Override
        public Object call(Object[] args) throws Exception {
            return _construct();
        }

        @Override
        public Object call1(Object arg) throws Exception {
            return _construct();
        }

        @Override
        public Class<?> getDeclaringClass() {
            return _base.getDeclaringClass();
        }

        @Override
        public Member getMember() {
            return _base.getMember();
        }

        @Override
        public void setValue(Object pojo, Object value)
                throws UnsupportedOperationException, IllegalArgumentException {
            throw new UnsupportedOperationException();
        }

        @Override
        public Object getValue(Object pojo)
                throws UnsupportedOperationException, IllegalArgumentException {
            throw new UnsupportedOperationException();
        }

        @Override
        public Annotated withAnnotations(AnnotationMap fallback) {
            throw new UnsupportedOperationException();
        }

        @Override
        public AnnotatedElement getAnnotated() {
            return _base.getAnnotated();
        }

        @Override
        protected int getModifiers() {
            return _base.getMember().getModifiers();
        }

        @Override
        public String getName() {
            return _base.getName();
        }

        @Override
        public JavaType getType() {
            return _base.getType();
        }

        @Override
        public Class<?> getRawType() {
            return _base.getRawType();
        }

        @Override
        public boolean equals(Object o) {
            return (o == this);
        }

        @Override
        public int hashCode() {
            return _base.hashCode();
        }

        @Override
        public String toString() {
            return _base.toString();
        }
    }
}
