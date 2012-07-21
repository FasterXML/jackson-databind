package com.fasterxml.jackson.databind.deser;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationConfig;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * This is the class that can be registered (via
 * {@link DeserializationConfig} object owner by
 * {@link ObjectMapper}) to get calledn when a potentially
 * recoverable problem is encountered during deserialization
 * process. Handlers can try to resolve the problem, throw
 * an exception or do nothing.
 *<p>
 * Default implementations for all methods implemented minimal
 * "do nothing" functionality, which is roughly equivalent to
 * not having a registered listener at all. This allows for
 * only implemented handler methods one is interested in, without
 * handling other cases.
 */
public abstract class DeserializationProblemHandler
{
    /**
     * Method called when a JSON Map ("Object") entry with an unrecognized
     * name is encountered.
     * Content (supposedly) matching the property are accessible via
     * parser that can be obtained from passed deserialization context.
     * Handler can also choose to skip the content; if so, it MUST return
     * true to indicate it did handle property succesfully.
     * Skipping is usually done like so:
     *<pre>
     *  ctxt.getParser().skipChildren();
     *</pre>
     *<p>
     * Note: version 1.2 added new deserialization feature
     * (<code>DeserializationConfig.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES</code>).
     * It will only have effect <b>after</b> handler is called, and only
     * if handler did <b>not</b> handle the problem.
     *
     * @param beanOrClass Either bean instance being deserialized (if one
     *   has been instantiated so far); or Class that indicates type that
     *   will be instantiated (if no instantiation done yet: for example
     *   when bean uses non-default constructors)
     * @param jp Parser to use for handling problematic content
     * 
     * @return True if the problem is resolved (and content available used or skipped);
     *  false if the handler did not anything and the problem is unresolved. Note that in
     *  latter case caller will either throw an exception or explicitly skip the content,
     *  depending on configuration.
     */
    public boolean handleUnknownProperty(DeserializationContext ctxt, JsonParser jp,
            JsonDeserializer<?> deserializer, Object beanOrClass, String propertyName)
        throws IOException, JsonProcessingException
    {
        return false;
    }
}
