package com.fasterxml.jackson.databind.ser;

import com.fasterxml.jackson.databind.*;

/**
 * Interface used to indicate serializers that want to do post-processing
 * after construction and being added to {@link SerializerProvider},
 * but before being used. This is typically used to resolve references
 * to other contained types; for example, bean serializers use this
 * to eagerly find serializers for contained field types.
 *<p>
 * Note that in cases where serializer needs both contextualization and
 * resolution -- that is, implements both this interface and {@link ContextualSerializer}
 * -- resolution via this interface occurs first, and contextual
 * resolution (using {@link ContextualSerializer}) later on.
 */
public interface ResolvableSerializer
{
    /**
     * Method called after {@link SerializerProvider} has registered
     * the serializer, but before it has returned it to the caller.
     * Called object can then resolve its dependencies to other types,
     * including self-references (direct or indirect).
     *<p>
     * Note that this method does NOT return serializer, since resolution
     * is not allowed to change actual serializer to use.
     *
     * @param provider Provider that has constructed serializer this method
     *   is called on.
     */
    public abstract void resolve(SerializerProvider provider)
        throws JsonMappingException;
}
