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
     * Method that may be called for optimization purposes, to see if calls to
     * mix-in resolver may be avoided. Return value of {@code true} means that
     * it is possible that a mix-in class will be found; {@code false} that no
     * mix-in will ever be found. In latter case caller can avoid calls altogether.
     *<p>
     * Note that the reason for "empty" resolvers is to use "null object"
     * for simplifying calling code.
     *
     * @return True, if this resolver MAY have mix-ins to apply; false if not (it is "empty")
     */
    public boolean hasMixIns();

    /**
     * Method called to create a new, non-shared copy, to be used by different
     * <code>ObjectMapper</code> instance, and one that should not be connected
     * to this instance, if resolver has mutable state.
     * If resolver is immutable may simply return `this`.
     */
    @Override
    public MixInResolver snapshot();
}