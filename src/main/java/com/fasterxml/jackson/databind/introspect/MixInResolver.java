package com.fasterxml.jackson.databind.introspect;

import com.fasterxml.jackson.core.util.Snapshottable;

/**
 * Interface used for decoupling details of how mix-in annotation
 * definitions are accessed (via this interface), and how
 * they are stored (defined by classes that implement the interface)
 *
 * @since 3.0 (in 2.x was nested type of {@link ClassIntrospector})
 */
public interface MixInResolver
    extends Snapshottable<MixInResolver>
{
    /**
     * Method that will check if there are "mix-in" classes (with mix-in
     * annotations) for given class
     */
    public Class<?> findMixInClassFor(Class<?> cls);

    /**
     * Method called to create a new, non-shared copy, to be used by different
     * <code>ObjectMapper</code> instance, and one that should not be connected
     * to this instance, if resolver has mutable state.
     * If resolver is immutable may simply return `this`.
     */
    @Override
    public MixInResolver snapshot();
}