package com.fasterxml.jackson.databind.deser;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationConfig;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.TypeIdResolver;

/**
 * This is the class that can be registered (via
 * {@link DeserializationConfig} object owner by
 * {@link ObjectMapper}) to get called when a potentially
 * recoverable problem is encountered during deserialization
 * process. Handlers can try to resolve the problem, throw
 * an exception or just skip the content.
 *<p>
 * Default implementations for all methods implemented minimal
 * "do nothing" functionality, which is roughly equivalent to
 * not having a registered listener at all. This allows for
 * only implemented handler methods one is interested in, without
 * handling other cases.
 *<p>
 * NOTE: it is typically <b>NOT</b> acceptable to simply do nothing,
 * because this will result in unprocessed tokens being left in
 * token stream (read via {@link JsonParser}, in case a structured
 * (JSON Object or JSON Array) value is being pointed to by parser.
 */
public abstract class DeserializationProblemHandler
{
    /**
     * Marker value returned by some handler methods to indicate that
     * they could not handle problem and produce replacement value.
     *
     * @since 2.7
     */
    public final static Object NOT_HANDLED = new Object();

    /**
     * Method called when a JSON Object property with an unrecognized
     * name is encountered.
     * Content (supposedly) matching the property are accessible via
     * parser that can be obtained from passed deserialization context.
     * Handler can also choose to skip the content; if so, it MUST return
     * true to indicate it did handle property successfully.
     * Skipping is usually done like so:
     *<pre>
     *  parser.skipChildren();
     *</pre>
     *<p>
     * Note: {@link com.fasterxml.jackson.databind.DeserializationFeature#FAIL_ON_UNKNOWN_PROPERTIES})
     * takes effect only <b>after</b> handler is called, and only
     * if handler did <b>not</b> handle the problem.
     *
     * @param beanOrClass Either bean instance being deserialized (if one
     *   has been instantiated so far); or Class that indicates type that
     *   will be instantiated (if no instantiation done yet: for example
     *   when bean uses non-default constructors)
     * @param p Parser to use for handling problematic content
     *
     * @return True if the problem is resolved (and content available used or skipped);
     *  false if the handler did not anything and the problem is unresolved. Note that in
     *  latter case caller will either throw an exception or explicitly skip the content,
     *  depending on configuration.
     */
    public boolean handleUnknownProperty(DeserializationContext ctxt, JsonParser p,
            JsonDeserializer<?> deserializer, Object beanOrClass, String propertyName)
        throws IOException
    {
        return false;
    }

    /**
     * Method called when a property name from input cannot be converted to a
     * non-Java-String key type (passed as <code>rawKeyType</code>) due to format problem.
     * Handler may choose to do one of 3 things:
     *<ul>
     * <li>Indicate it does not know what to do by returning {@link #NOT_HANDLED}
     *  </li>
     * <li>Throw a {@link IOException} to indicate specific fail message (instead of
     *    standard exception caller would throw
     *  </li>
     * <li>Return actual key value to use as replacement, and continue processing.
     *  </li>
     * </ul>
     *
     * @param failureMsg Message that will be used by caller (by calling
     *    {@link DeserializationContext#weirdKeyException(Class, String, String)})
     *    to indicate type of failure unless handler produces key to use
     *
     * @return Either {@link #NOT_HANDLED} to indicate that handler does not know
     *    what to do (and exception may be thrown), or value to use as key (possibly
     *    <code>null</code>
     *
     * @since 2.8
     */
    public Object handleWeirdKey(DeserializationContext ctxt,
            Class<?> rawKeyType, String keyValue,
            String failureMsg)
        throws IOException
    {
        return NOT_HANDLED;
    }

    /**
     * Method called when a String value
     * cannot be converted to a non-String value type due to specific problem
     * (as opposed to String values never being usable).
     * Handler may choose to do one of 3 things:
     *<ul>
     * <li>Indicate it does not know what to do by returning {@link #NOT_HANDLED}
     *  </li>
     * <li>Throw a {@link IOException} to indicate specific fail message (instead of
     *    standard exception caller would throw
     *  </li>
     * <li>Return actual converted value (of type <code>targetType</code>) to use as
     *    replacement, and continue processing.
     *  </li>
     * </ul>
     *
     * @param failureMsg Message that will be used by caller (by calling
     *    {@link DeserializationContext#weirdNumberException})
     *    to indicate type of failure unless handler produces key to use
     *
     * @return Either {@link #NOT_HANDLED} to indicate that handler does not know
     *    what to do (and exception may be thrown), or value to use as (possibly
     *    <code>null</code>)
     *
     * @since 2.8
     */
    public Object handleWeirdStringValue(DeserializationContext ctxt,
            Class<?> targetType, String valueToConvert,
            String failureMsg)
        throws IOException
    {
        return NOT_HANDLED;
    }

    /**
     * Method called when a numeric value (integral or floating-point from input
     * cannot be converted to a non-numeric value type due to specific problem
     * (as opposed to numeric values never being usable).
     * Handler may choose to do one of 3 things:
     *<ul>
     * <li>Indicate it does not know what to do by returning {@link #NOT_HANDLED}
     *  </li>
     * <li>Throw a {@link IOException} to indicate specific fail message (instead of
     *    standard exception caller would throw
     *  </li>
     * <li>Return actual converted value (of type <code>targetType</code>) to use as
     *    replacement, and continue processing.
     *  </li>
     * </ul>
     *
     * @param failureMsg Message that will be used by caller (by calling
     *    {@link DeserializationContext#weirdNumberException})
     *    to indicate type of failure unless handler produces key to use
     *
     * @return Either {@link #NOT_HANDLED} to indicate that handler does not know
     *    what to do (and exception may be thrown), or value to use as (possibly
     *    <code>null</code>)
     *
     * @since 2.8
     */
    public Object handleWeirdNumberValue(DeserializationContext ctxt,
            Class<?> targetType, Number valueToConvert, String failureMsg)
        throws IOException
    {
        return NOT_HANDLED;
    }

    /**
     * Method called when an embedded (native) value ({@link JsonToken#VALUE_EMBEDDED_OBJECT})
     * cannot be converted directly into expected value type (usually POJO).
     * Handler may choose to do one of 3 things:
     *<ul>
     * <li>Indicate it does not know what to do by returning {@link #NOT_HANDLED}
     *  </li>
     * <li>Throw a {@link IOException} to indicate specific fail message (instead of
     *    standard exception caller would throw
     *  </li>
     * <li>Return actual converted value (of type <code>targetType</code>) to use as
     *    replacement, and continue processing.
     *  </li>
     * </ul>
     *
     * @return Either {@link #NOT_HANDLED} to indicate that handler does not know
     *    what to do (and exception may be thrown), or value to use (possibly
     *    <code>null</code>)
     *
     * @since 2.9
     */
    public Object handleWeirdNativeValue(DeserializationContext ctxt,
            JavaType targetType, Object valueToConvert, JsonParser p)
        throws IOException
    {
        return NOT_HANDLED;
    }

    /**
     * Deprecated variant of
     * {@link #handleUnexpectedToken(DeserializationContext, JavaType, JsonToken, JsonParser, String)}
     *
     * @since 2.8
     *
     * @deprecated Since 2.10
     */
    @Deprecated
    public Object handleUnexpectedToken(DeserializationContext ctxt,
            Class<?> targetType, JsonToken t, JsonParser p,
            String failureMsg)
        throws IOException
    {
        return NOT_HANDLED;
    }

    /**
     * Method that deserializers should call if the first token of the value to
     * deserialize is of unexpected type (that is, type of token that deserializer
     * cannot handle). This could occur, for example, if a Number deserializer
     * encounter {@link JsonToken#START_ARRAY} instead of
     * {@link JsonToken#VALUE_NUMBER_INT} or {@link JsonToken#VALUE_NUMBER_FLOAT}.
     *<ul>
     * <li>Indicate it does not know what to do by returning {@link #NOT_HANDLED}
     *  </li>
     * <li>Throw a {@link IOException} to indicate specific fail message (instead of
     *    standard exception caller would throw
     *  </li>
     * <li>Handle content to match (by consuming or skipping it), and return actual
     *    instantiated value (of type <code>targetType</code>) to use as replacement;
     *    value may be `null` as well as expected target type.
     *  </li>
     * </ul>
     *
     * @param failureMsg Message that will be used by caller
     *    to indicate type of failure unless handler produces value to use
     *
     * @return Either {@link #NOT_HANDLED} to indicate that handler does not know
     *    what to do (and exception may be thrown), or value to use (possibly
     *    <code>null</code>
     *
     * @since 2.10
     */
    public Object handleUnexpectedToken(DeserializationContext ctxt,
            JavaType targetType, JsonToken t, JsonParser p,
            String failureMsg)
        throws IOException
    {
        // Calling class-version handler for backward compatibility, as of 2.10
        return handleUnexpectedToken(ctxt, targetType.getRawClass(), t, p, failureMsg);
    }

    /**
     * Method called when instance creation for a type fails due to an exception.
     * Handler may choose to do one of following things:
     *<ul>
     * <li>Indicate it does not know what to do by returning {@link #NOT_HANDLED}
     *  </li>
     * <li>Throw a {@link IOException} to indicate specific fail message (instead of
     *    standard exception caller would throw
     *  </li>
     * <li>Return actual instantiated value (of type <code>targetType</code>) to use as
     *    replacement, and continue processing.
     *  </li>
     * <li>Return <code>null</code> to use null as value but not to try further
     *   processing (in cases where properties would otherwise be bound)
     *  </li>
     * </ul>
     *
     * @param instClass Type that was to be instantiated
     * @param argument (optional) Additional argument that was passed to creator, if any
     * @param t Exception that caused instantiation failure
     *
     * @return Either {@link #NOT_HANDLED} to indicate that handler does not know
     *    what to do (and exception may be thrown), or value to use (possibly
     *    <code>null</code>
     *
     * @since 2.8
     */
    public Object handleInstantiationProblem(DeserializationContext ctxt,
            Class<?> instClass, Object argument, Throwable t)
        throws IOException
    {
        return NOT_HANDLED;
    }

    /**
     * Method called when instance creation for a type fails due to lack of an
     * instantiator. Method is called before actual deserialization from input
     * is attempted, so handler may do one of following things:
     *<ul>
     * <li>Indicate it does not know what to do by returning {@link #NOT_HANDLED}
     *  </li>
     * <li>Throw a {@link IOException} to indicate specific fail message (instead of
     *    standard exception caller would throw
     *  </li>
     * <li>Handle content to match (by consuming or skipping it), and return actual
     *    instantiated value (of type <code>targetType</code>) to use as replacement;
     *    value may be `null` as well as expected target type.
     *  </li>
     * </ul>
     *
     * @param instClass Type that was to be instantiated
     * @param p Parser to use for accessing content that needs handling, to either
     *   use it or skip it (latter with {@link JsonParser#skipChildren()}.
     *
     * @return Either {@link #NOT_HANDLED} to indicate that handler does not know
     *    what to do (and exception may be thrown), or value to use (possibly
     *    <code>null</code>
     *
     * @since 2.9
     */
    public Object handleMissingInstantiator(DeserializationContext ctxt,
            Class<?> instClass, ValueInstantiator valueInsta, JsonParser p,
            String msg)
        throws IOException
    {
        // 16-Oct-2016, tatu: Need to delegate to deprecated method from 2.8;
        //   remove redirect from later versions (post-2.9)
        return handleMissingInstantiator(ctxt, instClass, p, msg);
    }

    /**
     * Handler method called if resolution of type id from given String failed
     * to produce a subtype; usually because logical id is not mapped to actual
     * implementation class.
     * Handler may choose to do one of following things:
     *<ul>
     * <li>Indicate it does not know what to do by returning `null`
     *  </li>
     * <li>Indicate that nothing should be deserialized, by return `Void.class`
     *  </li>
     * <li>Throw a {@link IOException} to indicate specific fail message (instead of
     *    standard exception caller would throw
     *  </li>
     * <li>Return actual resolved type to use for type id.
     *  </li>
     * </ul>
     *
     * @param ctxt Deserialization context to use for accessing information or
     *    constructing exception to throw
     * @param baseType Base type to use for resolving subtype id
     * @param subTypeId Subtype id that failed to resolve
     * @param failureMsg Informational message that would be thrown as part of
     *    exception, if resolution still fails
     *
     * @return Actual type to use, if resolved; `null` if handler does not know what
     *     to do; or `Void.class` to indicate that nothing should be deserialized for
     *     type with the id (which caller may choose to do... or not)
     *
     * @since 2.8
     */
    public JavaType handleUnknownTypeId(DeserializationContext ctxt,
            JavaType baseType, String subTypeId, TypeIdResolver idResolver,
            String failureMsg)
        throws IOException
    {
        return null;
    }

    /**
     * Handler method called if an expected type id for a polymorphic value is
     * not found and no "default type" is specified or allowed.
     * Handler may choose to do one of following things:
     *<ul>
     * <li>Indicate it does not know what to do by returning `null`
     *  </li>
     * <li>Indicate that nothing should be deserialized, by return `Void.class`
     *  </li>
     * <li>Throw a {@link IOException} to indicate specific fail message (instead of
     *    standard exception caller would throw
     *  </li>
     * <li>Return actual resolved type to use for this particular case.
     *  </li>
     * </ul>
     *
     * @param ctxt Deserialization context to use for accessing information or
     *    constructing exception to throw
     * @param baseType Base type to use for resolving subtype id
     * @param failureMsg Informational message that would be thrown as part of
     *    exception, if resolution still fails
     *
     * @return Actual type to use, if resolved; `null` if handler does not know what
     *     to do; or `Void.class` to indicate that nothing should be deserialized for
     *     type with the id (which caller may choose to do... or not)
     *
     * @since 2.9
     */
    public JavaType handleMissingTypeId(DeserializationContext ctxt,
            JavaType baseType, TypeIdResolver idResolver,
            String failureMsg)
        throws IOException
    {
        return null;
    }

    /*
    /**********************************************************
    /* Deprecated
    /**********************************************************
     */

    /**
     * @since 2.8
     * @deprecated Since 2.9: use variant that takes {@link ValueInstantiator}
     */
    @Deprecated
    public Object handleMissingInstantiator(DeserializationContext ctxt,
            Class<?> instClass, JsonParser p, String msg)
        throws IOException
    {
        return NOT_HANDLED;
    }
}
