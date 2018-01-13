package com.fasterxml.jackson.databind.deser;

import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonMappingException;

/*
 * Interface used to indicate deserializers that want to do post-processing
 * after construction but before being returned to caller (and possibly cached)
 * and used.
 * This is typically used to resolve references
 * to other contained types; for example, bean deserializers use this callback
 * to locate deserializers for contained field types.
 * Main reason for using a callback (instead of trying to resolve dependencies
 * immediately) is to make it possible to cleanly handle self-references;
 * otherwise it would be easy to get into infinite recursion.
 *<p>
 * Note that {@link #resolve} method does NOT allow returning anything 
 * (specifically, a new deserializer instance): reason for this is that
 * allowing this would not work with proper handling of cyclic dependencies,
 * which are resolved by two-phase processing, where initially constructed
 * deserializer is added as known deserializer, and only after this
 * resolution is done. Resolution is the part that results in lookups for
 * dependant deserializers, which may include handling references to
 * deserializer itself.
 *<p>
 * Note that in cases where deserializer needs both contextualization and
 * resolution -- that is, implements both this interface and {@link ContextualDeserializer}
 * -- resolution via this interface occurs first, and contextual
 * resolution (using {@link ContextualDeserializer}) later on.
 *
 * @deprecated Since 3.0: method demoted to <code>JsonSerializer</code>
 */
/**
 * Leftover interface from 2.x: method now merged in <code>JsonSerializer</code>
 *
 * @deprecated Since 3.0: method demoted to <code>JsonSerializer</code>
*/
@Deprecated
public interface ResolvableDeserializer
{
    public abstract void resolve(DeserializationContext ctxt)
        throws JsonMappingException;
}
