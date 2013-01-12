package com.fasterxml.jackson.databind.deser.std;

import java.io.IOException;
import java.util.List;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationConfig;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.deser.CreatorProperty;
import com.fasterxml.jackson.databind.deser.SettableBeanProperty;
import com.fasterxml.jackson.databind.deser.ValueInstantiator;
import com.fasterxml.jackson.databind.introspect.AnnotatedParameter;
import com.fasterxml.jackson.databind.introspect.AnnotatedWithParams;


/**
 * Default {@link ValueInstantiator} implementation, which supports
 * Creator methods that can be indicated by standard Jackson
 * annotations.
 */
public class StdValueInstantiator
    extends ValueInstantiator
    implements java.io.Serializable
{
    private static final long serialVersionUID = 1L;

    /**
     * Type of values that are instantiated; used
     * for error reporting purposes.
     */
    protected final String _valueTypeDesc;

    /**
     * Are we allowed to convert empty Strings to null objects?
     */
    protected final boolean _cfgEmptyStringsAsObjects;
    
    // // // Default (no-args) construction

    /**
     * Default (no-argument) constructor to use for instantiation
     * (with {@link #createUsingDefault})
     */
    protected AnnotatedWithParams _defaultCreator;

    // // // With-args (property-based) construction

    protected AnnotatedWithParams _withArgsCreator;
    protected CreatorProperty[] _constructorArguments;

    // // // Delegate construction
    
    protected JavaType _delegateType;
    protected AnnotatedWithParams _delegateCreator;
    protected CreatorProperty[] _delegateArguments;
    
    // // // Scalar construction

    protected AnnotatedWithParams _fromStringCreator;
    protected AnnotatedWithParams _fromIntCreator;
    protected AnnotatedWithParams _fromLongCreator;
    protected AnnotatedWithParams _fromDoubleCreator;
    protected AnnotatedWithParams _fromBooleanCreator;

    // // // Incomplete creator
    protected AnnotatedParameter  _incompleteParameter;
    
    /*
    /**********************************************************
    /* Life-cycle
    /**********************************************************
     */

    public StdValueInstantiator(DeserializationConfig config, Class<?> valueType)
    {
        _cfgEmptyStringsAsObjects = (config == null) ? false
                : config.isEnabled(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT);
        _valueTypeDesc = (valueType == null) ? "UNKNOWN TYPE" : valueType.getName();
    }
    
    public StdValueInstantiator(DeserializationConfig config, JavaType valueType)
    {
        _cfgEmptyStringsAsObjects = (config == null) ? false
                : config.isEnabled(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT);
        _valueTypeDesc = (valueType == null) ? "UNKNOWN TYPE" : valueType.toString();
    }
    
    /**
     * Copy-constructor that sub-classes can use when creating new instances
     * by fluent-style construction
     */
    protected StdValueInstantiator(StdValueInstantiator src)
    {
        _cfgEmptyStringsAsObjects = src._cfgEmptyStringsAsObjects;
        _valueTypeDesc = src._valueTypeDesc;

        _defaultCreator = src._defaultCreator;

        _constructorArguments = src._constructorArguments;
        _withArgsCreator = src._withArgsCreator;

        _delegateType = src._delegateType;
        _delegateCreator = src._delegateCreator;
        _delegateArguments = src._delegateArguments;
        
        _fromStringCreator = src._fromStringCreator;
        _fromIntCreator = src._fromIntCreator;
        _fromLongCreator = src._fromLongCreator;
        _fromDoubleCreator = src._fromDoubleCreator;
        _fromBooleanCreator = src._fromBooleanCreator;
    }

    /**
     * Method for setting properties related to instantiating values
     * from JSON Object. We will choose basically only one approach (out of possible
     * three), and clear other properties
     */
    public void configureFromObjectSettings(AnnotatedWithParams defaultCreator,
            AnnotatedWithParams delegateCreator, JavaType delegateType, CreatorProperty[] delegateArgs,
            AnnotatedWithParams withArgsCreator, CreatorProperty[] constructorArgs)
    {
        _defaultCreator = defaultCreator;
        _delegateCreator = delegateCreator;
        _delegateType = delegateType;
        _delegateArguments = delegateArgs;
        _withArgsCreator = withArgsCreator;
        _constructorArguments = constructorArgs;
    }

    public void configureFromStringCreator(AnnotatedWithParams creator) {
        _fromStringCreator = creator;
    }

    public void configureFromIntCreator(AnnotatedWithParams creator) {
        _fromIntCreator = creator;
    }

    public void configureFromLongCreator(AnnotatedWithParams creator) {
        _fromLongCreator = creator;
    }

    public void configureFromDoubleCreator(AnnotatedWithParams creator) {
        _fromDoubleCreator = creator;
    }

    public void configureFromBooleanCreator(AnnotatedWithParams creator) {
        _fromBooleanCreator = creator;
    }

    public void configureIncompleteParameter(AnnotatedParameter parameter) {
        _incompleteParameter = parameter;
    }
    
    /*
    /**********************************************************
    /* Public API implementation; metadata
    /**********************************************************
     */

    @Override
    public String getValueTypeDesc() {
        return _valueTypeDesc;
    }
    
    @Override
    public boolean canCreateFromString() {
        return (_fromStringCreator != null);
    }

    @Override
    public boolean canCreateFromInt() {
        return (_fromIntCreator != null);
    }

    @Override
    public boolean canCreateFromLong() {
        return (_fromLongCreator != null);
    }

    @Override
    public boolean canCreateFromDouble() {
        return (_fromDoubleCreator != null);
    }

    @Override
    public boolean canCreateFromBoolean() {
        return (_fromBooleanCreator != null);
    }
    
    @Override
    public boolean canCreateUsingDefault() {
        return (_defaultCreator != null);
    }

    @Override
    public boolean canCreateUsingDelegate() {
        return _delegateType != null;
    }
    
    @Override
    public boolean canCreateFromObjectWith() {
        return (_withArgsCreator != null);
    }

    @Override
    public JavaType getDelegateType(DeserializationConfig config) {
        return _delegateType;
    }

    @Override
    public SettableBeanProperty[] getFromObjectArguments(DeserializationConfig config) {
        return _constructorArguments;
    }
    
    /*
    /**********************************************************
    /* Public API implementation; instantiation from JSON Object
    /**********************************************************
     */
    
    @Override
    public Object createUsingDefault(DeserializationContext ctxt)
        throws IOException, JsonProcessingException
    {
        if (_defaultCreator == null) { // sanity-check; caller should check
            throw new IllegalStateException("No default constructor for "+getValueTypeDesc());
        }
        try {
            return _defaultCreator.call();
        } catch (ExceptionInInitializerError e) {
            throw wrapException(e);
        } catch (Exception e) {
            throw wrapException(e);
        }
    }
    
    @Override
    public Object createFromObjectWith(DeserializationContext ctxt, Object[] args)
        throws IOException, JsonProcessingException
    {
        if (_withArgsCreator == null) { // sanity-check; caller should check
            throw new IllegalStateException("No with-args constructor for "+getValueTypeDesc());
        }
        try {
            return _withArgsCreator.call(args);
        } catch (ExceptionInInitializerError e) {
            throw wrapException(e);
        } catch (Exception e) {
            throw wrapException(e);
        }
    }

    @Override
    public Object createUsingDelegate(DeserializationContext ctxt, Object delegate)
        throws IOException, JsonProcessingException
    {
        if (_delegateCreator == null) { // sanity-check; caller should check
            throw new IllegalStateException("No delegate constructor for "+getValueTypeDesc());
        }
        try {
            // First simple case: just delegate, no injectables
            if (_delegateArguments == null) {
                return _delegateCreator.call1(delegate);
            }
            // And then the case with at least one injectable...
            final int len = _delegateArguments.length;
            Object[] args = new Object[len];
            for (int i = 0; i < len; ++i) {
                CreatorProperty prop = _delegateArguments[i];
                if (prop == null) { // delegate
                    args[i] = delegate;
                } else { // nope, injectable:
                    args[i] = ctxt.findInjectableValue(prop.getInjectableValueId(), prop, null);
                }
            }
            // and then try calling with full set of arguments
            return _delegateCreator.call(args);
        } catch (ExceptionInInitializerError e) {
            throw wrapException(e);
        } catch (Exception e) {
            throw wrapException(e);
        }
    }
    
    /*
    /**********************************************************
    /* Public API implementation; instantiation from JSON scalars
    /**********************************************************
     */
    
    @Override
    public Object createFromString(DeserializationContext ctxt, String value)
            throws IOException, JsonProcessingException
    {
        if (_fromStringCreator != null) {
            try {
                return _fromStringCreator.call1(value);
            } catch (Exception e) {
                throw wrapException(e);
            } catch (ExceptionInInitializerError e) {
                throw wrapException(e);
            }
        }
        return _createFromStringFallbacks(ctxt, value);
    }
    
    @Override
    public Object createFromInt(DeserializationContext ctxt, int value)
            throws IOException, JsonProcessingException
    {
        try {
            // First: "native" int methods work best:
            if (_fromIntCreator != null) {
                return _fromIntCreator.call1(Integer.valueOf(value));
            }
            // but if not, can do widening conversion
            if (_fromLongCreator != null) {
                return _fromLongCreator.call1(Long.valueOf(value));
            }
        } catch (Exception e) {
            throw wrapException(e);
        } catch (ExceptionInInitializerError e) {
            throw wrapException(e);
        }
        throw new JsonMappingException("Can not instantiate value of type "+getValueTypeDesc()
                +" from Integral number; no single-int-arg constructor/factory method");
    }

    @Override
    public Object createFromLong(DeserializationContext ctxt, long value)
            throws IOException, JsonProcessingException
    {
        try {
            if (_fromLongCreator != null) {
                return _fromLongCreator.call1(Long.valueOf(value));
            }
        } catch (Exception e) {
            throw wrapException(e);
        } catch (ExceptionInInitializerError e) {
            throw wrapException(e);
        }
        throw new JsonMappingException("Can not instantiate value of type "+getValueTypeDesc()
                +" from Long integral number; no single-long-arg constructor/factory method");
    }

    @Override
    public Object createFromDouble(DeserializationContext ctxt, double value)
            throws IOException, JsonProcessingException
    {
        try {
            if (_fromDoubleCreator != null) {
                return _fromDoubleCreator.call1(Double.valueOf(value));
            }
        } catch (Exception e) {
            throw wrapException(e);
        } catch (ExceptionInInitializerError e) {
            throw wrapException(e);
        }
        throw new JsonMappingException("Can not instantiate value of type "+getValueTypeDesc()
                +" from Floating-point number; no one-double/Double-arg constructor/factory method");
    }

    @Override
    public Object createFromBoolean(DeserializationContext ctxt, boolean value)
            throws IOException, JsonProcessingException
    {
        try {
            if (_fromBooleanCreator != null) {
                return _fromBooleanCreator.call1(Boolean.valueOf(value));
            }
        } catch (Exception e) {
            throw wrapException(e);
        } catch (ExceptionInInitializerError e) {
            throw wrapException(e);
        }
        throw new JsonMappingException("Can not instantiate value of type "+getValueTypeDesc()
                +" from Boolean value; no single-boolean/Boolean-arg constructor/factory method");
    }
    
    /*
    /**********************************************************
    /* Extended API: configuration mutators, accessors
    /**********************************************************
     */

    @Override
    public AnnotatedWithParams getDelegateCreator() {
        return _delegateCreator;
    }

    @Override
    public AnnotatedWithParams getDefaultCreator() {
        return _defaultCreator;
    }

    @Override
    public AnnotatedWithParams getWithArgsCreator() {
        return _withArgsCreator;
    }

    @Override
    public AnnotatedParameter getIncompleteParameter() {
        return _incompleteParameter;
    }
    
    /*
    /**********************************************************
    /* Internal methods
    /**********************************************************
     */

    protected Object _createFromStringFallbacks(DeserializationContext ctxt, String value)
            throws IOException, JsonProcessingException
    {
        /* 28-Sep-2011, tatu: Ok this is not clean at all; but since there are legacy
         *   systems that expect conversions in some cases, let's just add a minimal
         *   patch (note: same could conceivably be used for numbers too).
         */
        if (_fromBooleanCreator != null) {
            String str = value.trim();
            if ("true".equals(str)) {
                return createFromBoolean(ctxt, true);
            }
            if ("false".equals(str)) {
                return createFromBoolean(ctxt, false);
            }
        }
        
        // and finally, empty Strings might be accepted as null Object...
        if (_cfgEmptyStringsAsObjects && value.length() == 0) {
            return null;
        }
        throw new JsonMappingException("Can not instantiate value of type "+getValueTypeDesc()
                +" from String value; no single-String constructor/factory method");
    }
    
    protected JsonMappingException wrapException(Throwable t)
    {
        while (t.getCause() != null) {
            t = t.getCause();
        }
        if (t instanceof JsonMappingException) {
            return (JsonMappingException) t;
        }
        return new JsonMappingException("Instantiation of "+getValueTypeDesc()+" value failed: "+t.getMessage(), t);
    }
}


