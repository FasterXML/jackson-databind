package com.fasterxml.jackson.databind.introspect;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.AnnotationIntrospector;
import com.fasterxml.jackson.databind.BeanProperty;

/**
 * Intermediate {@link BeanProperty} class shared by concrete readable- and
 * writable property implementations for sharing common functionality.
 *
 * @since 2.7
 */
public abstract class ConcreteBeanPropertyBase
    implements BeanProperty
{
    /**
     * Marker we use to indicate case where we have done format lookup,
     * but found nothing; marker used to avoid having to repeat such lookups.
     *
     * @since 2.6
     */
    protected final static JsonFormat.Value NO_FORMAT = new JsonFormat.Value();

    /**
     * Lazily accessed value for per-property format override definition.
     * 
     * @since 2.6
     */
    protected transient JsonFormat.Value _format;
    
    @Override
    public final JsonFormat.Value findFormatOverrides(AnnotationIntrospector intr) {
        JsonFormat.Value f = _format;
        if (f == null) { // not yet looked up, do that
            if (intr != null) {
                AnnotatedMember member = getMember();
                if (member != null) {
                    f = intr.findFormat(member);
                }
            }
            if (f == null) {
                f = NO_FORMAT;
            }
        }
        return (f == NO_FORMAT) ? null : f;
    }
}
