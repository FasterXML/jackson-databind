package com.fasterxml.jackson.databind.ser;

import com.fasterxml.jackson.databind.*;

/*
 * Interface used to indicate serializers that want to do post-processing
 * after construction and being added to {@link SerializerProvider},
 * but before being used. This is typically used to resolve references
 * to other contained types; for example, bean serializers use this
 * to eagerly find serializers for contained field types.
 */

/**
 * @deprecated Since 3.0: method demoted to <code>JsonSerializer</code>
 */
@Deprecated
public interface ResolvableSerializer
{
    public abstract void resolve(SerializerProvider provider)
        throws JsonMappingException;
}
