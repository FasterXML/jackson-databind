package com.fasterxml.jackson.databind.deser;

import java.io.IOException;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.deser.impl.PropertyValueBuffer;
import com.fasterxml.jackson.databind.introspect.AnnotatedParameter;
import com.fasterxml.jackson.databind.introspect.AnnotatedWithParams;

/**
 * Class that defines simple API implemented by objects that create value
 * instances.  Some or all of properties of value instances may 
 * be initialized by instantiator, rest being populated by deserializer,
 * to which value instance is passed.
 * Since different kinds of JSON values (structured and scalar)
 * may be bound to Java values, in some cases instantiator
 * fully defines resulting value; this is the case when JSON value
 * is a scalar value (String, number, boolean).
 *<p>
 * Note that this type is not parameterized (even though it would seemingly
 * make sense), because such type information cannot be use effectively
 * during runtime: access is always using either wildcard type, or just
 * basic {@link java.lang.Object}; and so adding type parameter seems
 * like unnecessary extra work.
 *<p>
 * Actual implementations are strongly recommended to be based on
 * {@link com.fasterxml.jackson.databind.deser.std.StdValueInstantiator}
 * which implements all methods, and as such will be compatible
 * across versions even if new methods were added to this interface.
 */
public abstract class ValueInstantiator
{
    /*
    /**********************************************************
    /* Metadata accessors
    /**********************************************************
     */

    /**
     * Accessor for raw (type-erased) type of instances to create.
     *<p>
     * NOTE: since this method has not existed since beginning of
     * Jackson 2.0 series, default implementation will just return
     * <code>Object.class</code>; implementations are expected
     * to override it with real value.
     *
     * @since 2.8
     */
    public Class<?> getValueClass() {
        return Object.class;
    }

    /**
     * Method that returns description of the value type this instantiator
     * handles. Used for error messages, diagnostics.
     */
    public String getValueTypeDesc() {
        Class<?> cls = getValueClass();
        if (cls == null) {
            return "UNKNOWN";
        }
        return cls.getName();
    }

    /**
     * Method that will return true if any of <code>canCreateXxx</code> method
     * returns true: that is, if there is any way that an instance could
     * be created.
     */
    public boolean canInstantiate() {
        return canCreateUsingDefault()
                || canCreateUsingDelegate() || canCreateUsingArrayDelegate()
                || canCreateFromObjectWith() || canCreateFromString()
                || canCreateFromInt() || canCreateFromLong()
                || canCreateFromDouble() || canCreateFromBoolean();
    }

    /**
     * Method that can be called to check whether a String-based creator
     * is available for this instantiator
     */
    public boolean canCreateFromString() { return false; }

    /**
     * Method that can be called to check whether an integer (int, Integer) based
     * creator is available to use (to call {@link #createFromInt}).
     */
    public boolean canCreateFromInt() { return false; }

    /**
     * Method that can be called to check whether a long (long, Long) based
     * creator is available to use (to call {@link #createFromLong}).
     */
    public boolean canCreateFromLong() { return false; }

    /**
     * Method that can be called to check whether a double (double / Double) based
     * creator is available to use (to call {@link #createFromDouble}).
     */
    public boolean canCreateFromDouble() { return false; }

    /**
     * Method that can be called to check whether a double (boolean / Boolean) based
     * creator is available to use (to call {@link #createFromDouble}).
     */
    public boolean canCreateFromBoolean() { return false; }

    /**
     * Method that can be called to check whether a default creator (constructor,
     * or no-arg static factory method)
     * is available for this instantiator
     */
    public boolean canCreateUsingDefault() {  return getDefaultCreator() != null; }

    /**
     * Method that can be called to check whether a delegate-based creator (single-arg
     * constructor or factory method)
     * is available for this instantiator
     */
    public boolean canCreateUsingDelegate() { return false; }

    /**
     * Method that can be called to check whether a array-delegate-based creator
     * (single-arg constructor or factory method)
     * is available for this instantiator
     *
     * @since 2.7
     */
    public boolean canCreateUsingArrayDelegate() { return false; }

    /**
     * Method that can be called to check whether a property-based creator
     * (argument-taking constructor or factory method)
     * is available to instantiate values from JSON Object
     */
    public boolean canCreateFromObjectWith() { return false; }

    /**
     * Method called to determine types of instantiation arguments
     * to use when creating instances with creator arguments
     * (when {@link #canCreateFromObjectWith()} returns  true).
     * These arguments are bound from JSON, using specified
     * property types to locate deserializers.
     *<p>
     * NOTE: all properties will be of type
     * {@link com.fasterxml.jackson.databind.deser.CreatorProperty}.
     */
    public SettableBeanProperty[] getFromObjectArguments(DeserializationConfig config) {
        return null;
    }

    /**
     * Method that can be used to determine what is the type of delegate
     * type to use, if any; if no delegates are used, will return null.
     * If non-null type is returned, deserializer will bind JSON into
     * specified type (using standard deserializer for that type), and
     * pass that to instantiator.
     */
    public JavaType getDelegateType(DeserializationConfig config) { return null; }

    /**
     * Method that can be used to determine what is the type of array delegate
     * type to use, if any; if no delegates are used, will return null. If
     * non-null type is returned, deserializer will bind JSON into specified
     * type (using standard deserializer for that type), and pass that to
     * instantiator.
     *
     * @since 2.7
     */
    public JavaType getArrayDelegateType(DeserializationConfig config) { return null; }

    /*
    /**********************************************************
    /* Instantiation methods for JSON Object
    /**********************************************************
     */

    /**
     * Method called to create value instance from a JSON value when
     * no data needs to passed to creator (constructor, factory method);
     * typically this will call the default constructor of the value object.
     * It will only be used if more specific creator methods are not
     * applicable; hence "default".
     *<p>
     * This method is called if {@link #getFromObjectArguments} returns
     * null or empty List.
     */
    public Object createUsingDefault(DeserializationContext ctxt) throws IOException {
        return ctxt.handleMissingInstantiator(getValueClass(), this, null,
                "no default no-arguments constructor found");
    }

    /**
     * Method called to create value instance from JSON Object when
     * instantiation arguments are passed; this is done, for example when passing information
     * specified with "Creator" annotations.
     *<p>
     * This method is called if {@link #getFromObjectArguments} returns
     * a non-empty List of arguments.
     */
    public Object createFromObjectWith(DeserializationContext ctxt, Object[] args) throws IOException {
        // sanity check; shouldn't really get called if no Creator specified
        return ctxt.handleMissingInstantiator(getValueClass(), this, null,
                "no creator with arguments specified");
    }

    /**
     * Method that delegates to
     * {@link #createFromObjectWith(DeserializationContext, Object[])} by
     * default, but can be overridden if the application should have customized
     * behavior with respect to missing properties.
     *<p>
     * The default implementation of this method uses
     * {@link PropertyValueBuffer#getParameters(SettableBeanProperty[])} to read
     * and validate all properties in bulk, possibly substituting defaults for
     * missing properties or throwing exceptions for missing properties.  An
     * overridden implementation of this method could, for example, use
     * {@link PropertyValueBuffer#hasParameter(SettableBeanProperty)} and
     * {@link PropertyValueBuffer#getParameter(SettableBeanProperty)} to safely
     * read the present properties only, and to have some other behavior for the
     * missing properties.
     * 
     * @since 2.8
     */
    public Object createFromObjectWith(DeserializationContext ctxt,
            SettableBeanProperty[] props, PropertyValueBuffer buffer)
        throws IOException
    {
        return createFromObjectWith(ctxt, buffer.getParameters(props));
    }

    /**
     * Method to called to create value instance from JSON Object using
     * an intermediate "delegate" value to pass to createor method
     */
    public Object createUsingDelegate(DeserializationContext ctxt, Object delegate) throws IOException {
        return ctxt.handleMissingInstantiator(getValueClass(), this, null,
                "no delegate creator specified");
    }

    /**
     * Method to called to create value instance from JSON Array using
     * an intermediate "delegate" value to pass to createor method
     */
    public Object createUsingArrayDelegate(DeserializationContext ctxt, Object delegate) throws IOException {
        return ctxt.handleMissingInstantiator(getValueClass(), this, null,
                "no array delegate creator specified");
    }

    /*
    /**********************************************************
    /* Instantiation methods for JSON scalar types
    /* (String, Number, Boolean)
    /**********************************************************
     */
    
    public Object createFromString(DeserializationContext ctxt, String value) throws IOException {
        return _createFromStringFallbacks(ctxt, value);
    }

    public Object createFromInt(DeserializationContext ctxt, int value) throws IOException {
        return ctxt.handleMissingInstantiator(getValueClass(), this, null,
                "no int/Int-argument constructor/factory method to deserialize from Number value (%s)",
                value);
    }

    public Object createFromLong(DeserializationContext ctxt, long value) throws IOException {
        return ctxt.handleMissingInstantiator(getValueClass(), this, null,
                "no long/Long-argument constructor/factory method to deserialize from Number value (%s)",
                value);
    }

    public Object createFromDouble(DeserializationContext ctxt, double value) throws IOException {
        return ctxt.handleMissingInstantiator(getValueClass(), this, null,
                "no double/Double-argument constructor/factory method to deserialize from Number value (%s)",
                value);
    }

    public Object createFromBoolean(DeserializationContext ctxt, boolean value) throws IOException {
        return ctxt.handleMissingInstantiator(getValueClass(), this, null,
                "no boolean/Boolean-argument constructor/factory method to deserialize from boolean value (%s)",
                value);
    }

    /*
    /**********************************************************
    /* Accessors for underlying creator objects (optional)
    /**********************************************************
     */

    /**
     * Method that can be called to try to access member (constructor,
     * static factory method) that is used as the "default creator"
     * (creator that is called without arguments; typically default
     * [zero-argument] constructor of the type).
     * Note that implementations not required to return actual object
     * they use (or, they may use some other instantiation) method.
     * That is, even if {@link #canCreateUsingDefault()} returns true,
     * this method may return null .
     */
    public AnnotatedWithParams getDefaultCreator() { return null; }

    /**
     * Method that can be called to try to access member (constructor,
     * static factory method) that is used as the "delegate creator".
     * Note that implementations not required to return actual object
     * they use (or, they may use some other instantiation) method.
     * That is, even if {@link #canCreateUsingDelegate()} returns true,
     * this method may return null .
     */
    public AnnotatedWithParams getDelegateCreator() { return null; }

    /**
     * Method that can be called to try to access member (constructor,
     * static factory method) that is used as the "array delegate creator".
     * Note that implementations not required to return actual object
     * they use (or, they may use some other instantiation) method.
     * That is, even if {@link #canCreateUsingArrayDelegate()} returns true,
     * this method may return null .
     */
    public AnnotatedWithParams getArrayDelegateCreator() { return null; }

    /**
     * Method that can be called to try to access member (constructor,
     * static factory method) that is used as the "non-default creator"
     * (constructor or factory method that takes one or more arguments).
     * Note that implementations not required to return actual object
     * they use (or, they may use some other instantiation) method.
     * That is, even if {@link #canCreateFromObjectWith()} returns true,
     * this method may return null .
     */
    public AnnotatedWithParams getWithArgsCreator() { return null; }

    /**
     * If an incomplete creator was found, this is the first parameter that
     * needs further annotation to help make the creator complete.
     */
    public AnnotatedParameter getIncompleteParameter() { return null; }

    /*
    /**********************************************************
    /* Helper methods
    /**********************************************************
     */

    /**
     * @since 2.4 (demoted from <code>StdValueInstantiator</code>)
     */
    protected Object _createFromStringFallbacks(DeserializationContext ctxt, String value)
            throws IOException
    {
        /* 28-Sep-2011, tatu: Ok this is not clean at all; but since there are legacy
         *   systems that expect conversions in some cases, let's just add a minimal
         *   patch (note: same could conceivably be used for numbers too).
         */
        if (canCreateFromBoolean()) {
            String str = value.trim();
            if ("true".equals(str)) {
                return createFromBoolean(ctxt, true);
            }
            if ("false".equals(str)) {
                return createFromBoolean(ctxt, false);
            }
        }
        // also, empty Strings might be accepted as null Object...
        if (value.length() == 0) {
            if (ctxt.isEnabled(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT)) {
                return null;
            }
        }
        return ctxt.handleMissingInstantiator(getValueClass(), this, ctxt.getParser(),
                "no String-argument constructor/factory method to deserialize from String value ('%s')",
                value);
    }

    /*
    /**********************************************************
    /* Introspection
    /**********************************************************
     */

    /**
     * @since 2.9
     */
    public interface Gettable {
        public ValueInstantiator getValueInstantiator();
    }

    /*
    /**********************************************************
    /* Standard Base implementation (since 2.8)
    /**********************************************************
     */

    /**
     * Partial {@link ValueInstantiator} implementation that is strongly recommended
     * to be used instead of directly extending {@link ValueInstantiator} itself.
     */
    public static class Base extends ValueInstantiator
    {
        protected final Class<?> _valueType;

        public Base(Class<?> type) {
            _valueType = type;
        }

        public Base(JavaType type) {
            _valueType = type.getRawClass();
        }
        
        @Override
        public String getValueTypeDesc() {
            return _valueType.getName();
        }

        @Override
        public Class<?> getValueClass() {
            return _valueType;
        }
    }
}
