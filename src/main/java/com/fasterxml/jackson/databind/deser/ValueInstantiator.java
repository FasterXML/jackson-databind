package com.fasterxml.jackson.databind.deser;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationConfig;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonMappingException;
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
 * make sense), because such type information can not be use effectively
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
     * Method that returns description of the value type this instantiator
     * handles. Used for error messages, diagnostics.
     */
    public abstract String getValueTypeDesc();

    /**
     * Method that will return true if any of <code>canCreateXxx</code> method
     * returns true: that is, if there is any way that an instance could
     * be created.
     */
    public boolean canInstantiate() {
        return
             canCreateUsingDefault()
             || canCreateUsingDelegate()
             || canCreateFromObjectWith()
             || canCreateFromString()
             || canCreateFromInt()
             || canCreateFromLong()
             || canCreateFromDouble()
             || canCreateFromBoolean()
             ;
    }    
    
    /**
     * Method that can be called to check whether a String-based creator
     * is available for this instantiator
     */
    public boolean canCreateFromString() {
        return false;
    }

    /**
     * Method that can be called to check whether an integer (int, Integer) based
     * creator is available to use (to call {@link #createFromInt}).
     */
    public boolean canCreateFromInt() {
        return false;
    }

    /**
     * Method that can be called to check whether a long (long, Long) based
     * creator is available to use (to call {@link #createFromLong}).
     */
    public boolean canCreateFromLong() {
        return false;
    }

    /**
     * Method that can be called to check whether a double (double / Double) based
     * creator is available to use (to call {@link #createFromDouble}).
     */
    public boolean canCreateFromDouble() {
        return false;
    }

    /**
     * Method that can be called to check whether a double (boolean / Boolean) based
     * creator is available to use (to call {@link #createFromDouble}).
     */
    public boolean canCreateFromBoolean() {
        return false;
    }
    
    /**
     * Method that can be called to check whether a default creator (constructor,
     * or no-arg static factory method)
     * is available for this instantiator
     */
    public boolean canCreateUsingDefault() {
        return getDefaultCreator() != null;
    }

    /**
     * Method that can be called to check whether a delegate-based creator (single-arg
     * constructor or factory method)
     * is available for this instantiator
     */
    public boolean canCreateUsingDelegate() {
        return false;
    }

    /**
     * Method that can be called to check whether a property-based creator
     * (argument-taking constructor or factory method)
     * is available to instantiate values from JSON Object
     */
    public boolean canCreateFromObjectWith() {
        return false;
    }

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
    public JavaType getDelegateType(DeserializationConfig config) {
        return null;
    }
    
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
    public Object createUsingDefault(DeserializationContext ctxt)
        throws IOException, JsonProcessingException {
        throw new JsonMappingException("Can not instantiate value of type "
                +getValueTypeDesc()+"; no default creator found");
    }

    /**
     * Method called to create value instance from JSON Object when
     * instantiation arguments are passed; this is done, for example when passing information
     * specified with "Creator" annotations.
     *<p>
     * This method is called if {@link #getFromObjectArguments} returns
     * a non-empty List of arguments.
     */
    public Object createFromObjectWith(DeserializationContext ctxt, Object[] args)
        throws IOException, JsonProcessingException {
        throw new JsonMappingException("Can not instantiate value of type "
                +getValueTypeDesc()+" with arguments");
    }

    /**
     * Method to called to create value instance from JSON Object using
     * an intermediate "delegate" value to pass to createor method
     */
    public Object createUsingDelegate(DeserializationContext ctxt, Object delegate)
        throws IOException, JsonProcessingException
    {
        throw new JsonMappingException("Can not instantiate value of type "
                +getValueTypeDesc()+" using delegate");
    }
    
    /*
    /**********************************************************
    /* Instantiation methods for JSON scalar types
    /* (String, Number, Boolean)
    /**********************************************************
     */
    
    public Object createFromString(DeserializationContext ctxt, String value)
            throws IOException, JsonProcessingException {
        throw new JsonMappingException("Can not instantiate value of type "
                +getValueTypeDesc()+" from String value");
    }
    
    public Object createFromInt(DeserializationContext ctxt, int value)
            throws IOException, JsonProcessingException {
        throw new JsonMappingException("Can not instantiate value of type "
                +getValueTypeDesc()+" from Integer number (int)");
    }

    public Object createFromLong(DeserializationContext ctxt, long value)
            throws IOException, JsonProcessingException {
        throw new JsonMappingException("Can not instantiate value of type "
                +getValueTypeDesc()+" from Integer number (long)");
    }

    public Object createFromDouble(DeserializationContext ctxt, double value)
            throws IOException, JsonProcessingException {
        throw new JsonMappingException("Can not instantiate value of type "
                +getValueTypeDesc()+" from Floating-point number (double)");
    }
    
    public Object createFromBoolean(DeserializationContext ctxt, boolean value)
            throws IOException, JsonProcessingException {
        throw new JsonMappingException("Can not instantiate value of type "
                +getValueTypeDesc()+" from Boolean value");
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
    public AnnotatedWithParams getDefaultCreator() {
        return null;
    }
    
    /**
     * Method that can be called to try to access member (constructor,
     * static factory method) that is used as the "delegate creator".
     * Note that implementations not required to return actual object
     * they use (or, they may use some other instantiation) method.
     * That is, even if {@link #canCreateUsingDelegate()} returns true,
     * this method may return null .
     */
    public AnnotatedWithParams getDelegateCreator() {
        return null;
    }

    /**
     * Method that can be called to try to access member (constructor,
     * static factory method) that is used as the "non-default creator"
     * (constructor or factory method that takes one or more arguments).
     * Note that implementations not required to return actual object
     * they use (or, they may use some other instantiation) method.
     * That is, even if {@link #canCreateFromObjectWith()} returns true,
     * this method may return null .
     */
    public AnnotatedWithParams getWithArgsCreator() {
        return null;
    }

    /**
     * If an incomplete creator was found, this is the first parameter that
     * needs further annotation to help make the creator complete.
     */
    public AnnotatedParameter getIncompleteParameter() {
        return null;
    }
}
