package com.fasterxml.jackson.databind.introspect;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.AnnotationIntrospector;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.PropertyMetadata;

/**
 * Intermediate {@link BeanProperty} class shared by concrete readable- and
 * writable property implementations for sharing common functionality.
 *
 * @since 2.7
 */
public abstract class ConcreteBeanPropertyBase
    implements BeanProperty, java.io.Serializable
{
    private static final long serialVersionUID = 1;

    /**
     * Additional information about property
     *
     * @since 2.3
     */
    protected final PropertyMetadata _metadata;
    
    /**
     * Lazily accessed value for per-property format override definition.
     * 
     * @since 2.6
     */
    protected transient JsonFormat.Value _format;

    protected ConcreteBeanPropertyBase(PropertyMetadata md) {
        _metadata = (md == null) ? PropertyMetadata.STD_REQUIRED_OR_OPTIONAL : md;
    }

    protected ConcreteBeanPropertyBase(ConcreteBeanPropertyBase src) {
        _metadata = src._metadata;
        _format = src._format;
    }

    @Override
    public boolean isRequired() { return _metadata.isRequired(); }

    @Override
    public PropertyMetadata getMetadata() { return _metadata; }
    
    @Override
    public boolean isVirtual() { return false; }

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
                f = EMPTY_FORMAT;
            }
        }
        return f;
    }

    @Override
    public JsonInclude.Value findPropertyInclusion(AnnotationIntrospector intr) {
        // 08-Oct-2015, tatu: Unlike with Format, let's not cache locally here, for now
        JsonInclude.Value v = null;
        if (intr != null) {
            AnnotatedMember member = getMember();
            if (member != null) {
                v = intr.findPropertyInclusion(member);
            }
        }
        return (v == null) ? JsonInclude.Value.empty() : v;
    }
}
