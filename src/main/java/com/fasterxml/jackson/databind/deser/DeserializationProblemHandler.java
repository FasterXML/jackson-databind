package com.fasterxml.jackson.databind.deser;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationConfig;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.ObjectMapper;

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
     * Method called when a property name from input can not be converted to a
     * non-Java-String key type (passed as <code>rawKeyType</code>) due to format
     * problem. Handler may choose to do one of 3 things:
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
     * @since 2.8
     *
     * @param failureMsg Message that will be used by caller (by calling
     *    {@link DeserializationContext#weirdKeyException(Class, String, String)})
     *    to indicate type of failure unless handler produces key to use
     *
     * @return Either {@link #NOT_HANDLED} to indicate that handler does not know
     *    what to do (and exception may be thrown), or value to use as key (possibly
     *    <code>null</code>
     */
    public Object handleWeirdKey(DeserializationContext ctxt,
            Class<?> rawKeyType, String keyValue,
            String failureMsg)
        throws IOException
    {
        return NOT_HANDLED;
    }
}
