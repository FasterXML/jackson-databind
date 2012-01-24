package com.fasterxml.jackson.databind.deser;

import com.fasterxml.jackson.databind.DeserializationConfig;
import com.fasterxml.jackson.databind.JsonMappingException;

/**
 * Interface used to indicate deserializers that want to do post-processing
 * after construction and being added to {@link DeserializerProvider},
 * but before being used. This is typically used to resolve references
 * to other contained types; for example, bean deserializers use this
 * to eagerly find deserializers for contained field types.
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
 */
public interface ResolvableDeserializer
{
    /**
     * Method called after {@link DeserializerProvider} has registered
     * the deserializer, but before it has returned it to the caller.
     * Called object can then resolve its dependencies to other types,
     * including self-references (direct or indirect).
     *
     * @param provider Provider that has constructed deserializer this method
     *   is called on.
     */
    public abstract void resolve(DeserializationConfig config, DeserializerProvider provider)
        throws JsonMappingException;
}
