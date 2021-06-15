package com.fasterxml.jackson.databind.introspect;

/**
 * Silly little "Pair" class needed for 2-element tuples (without
 * adding dependency to one of 3rd party packages that has one).
 *
 * @since 2.13
 */
public class AnnotatedAndMetadata<A extends Annotated, M extends Object>
{
    public final A annotated;
    public final M metadata;

    public AnnotatedAndMetadata(A ann, M md) {
        annotated = ann;
        metadata = md;
    }

    public static <A extends Annotated, M> AnnotatedAndMetadata<A, M> of(A ann, M md) {
        return new AnnotatedAndMetadata<>(ann, md);
    }
}
