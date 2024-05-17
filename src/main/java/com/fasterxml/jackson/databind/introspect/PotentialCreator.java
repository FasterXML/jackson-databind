package com.fasterxml.jackson.databind.introspect;

import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * Information about a single Creator (constructor or factory method),
 * kept during property introspection.
 *
 * @since 2.18
 */
public class PotentialCreator
{
    public final AnnotatedWithParams creator;

    public final JsonCreator.Mode creatorMode;

    public PotentialCreator(AnnotatedWithParams cr,
            JsonCreator.Mode cm)
    {
        creator = cr;
        creatorMode = cm;
    }

    public int paramCount() {
        return creator.getParameterCount();
    }
}
