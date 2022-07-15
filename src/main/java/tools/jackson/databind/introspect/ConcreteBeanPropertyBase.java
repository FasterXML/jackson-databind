package tools.jackson.databind.introspect;

import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;

import tools.jackson.databind.AnnotationIntrospector;
import tools.jackson.databind.BeanProperty;
import tools.jackson.databind.PropertyMetadata;
import tools.jackson.databind.PropertyName;
import tools.jackson.databind.cfg.MapperConfig;

/**
 * Intermediate {@link BeanProperty} class shared by concrete readable- and
 * writable property implementations for sharing common functionality.
 */
public abstract class ConcreteBeanPropertyBase
    implements BeanProperty, java.io.Serializable
{
    private static final long serialVersionUID = 1;

    /**
     * Additional information about property
     */
    protected final PropertyMetadata _metadata;

    protected transient List<PropertyName> _aliases;

    protected ConcreteBeanPropertyBase(PropertyMetadata md) {
        _metadata = (md == null) ? PropertyMetadata.STD_REQUIRED_OR_OPTIONAL : md;
    }

    protected ConcreteBeanPropertyBase(ConcreteBeanPropertyBase src) {
        _metadata = src._metadata;
    }

    @Override
    public boolean isRequired() { return _metadata.isRequired(); }

    @Override
    public PropertyMetadata getMetadata() { return _metadata; }
    
    @Override
    public boolean isVirtual() { return false; }

    @Override
    public JsonFormat.Value findFormatOverrides(MapperConfig<?> config) {
        AnnotationIntrospector intr = config.getAnnotationIntrospector();
        if (intr != null) {
            AnnotatedMember member = getMember();
            if (member != null) {
                return intr.findFormat(config, member);
            }
        }
        return null;
    }

    @Override
    public JsonFormat.Value findPropertyFormat(MapperConfig<?> config, Class<?> baseType)
    {
        JsonFormat.Value v1 = config.getDefaultPropertyFormat(baseType);
        JsonFormat.Value v2 = findFormatOverrides(config);
        if (v1 == null) {
            return (v2 == null) ? EMPTY_FORMAT : v2;
        }
        return (v2 == null) ? v1 : v1.withOverrides(v2);
    }

    @Override
    public JsonInclude.Value findPropertyInclusion(MapperConfig<?> config, Class<?> baseType)
    {
        AnnotationIntrospector intr = config.getAnnotationIntrospector();
        AnnotatedMember member = getMember();
        if (member == null) {
            JsonInclude.Value def = config.getDefaultPropertyInclusion(baseType);
            return def;
        }
        JsonInclude.Value v0 = config.getDefaultInclusion(baseType, member.getRawType());
        if (intr == null) {
            return v0;
        }
        JsonInclude.Value v = intr.findPropertyInclusion(config, member);
        if (v0 == null) {
            return v;
        }
        return v0.withOverrides(v);
    }

    @Override
    public List<PropertyName> findAliases(MapperConfig<?> config)
    {
        List<PropertyName> aliases = _aliases;
        if (aliases == null) {
            AnnotationIntrospector intr = config.getAnnotationIntrospector();
            if (intr != null) {
                final AnnotatedMember member = getMember();
                if (member != null) {
                    aliases = intr.findPropertyAliases(config, member);
                }
            }
            if (aliases == null) {
                aliases = Collections.emptyList();
            }
            _aliases = aliases;
        }
        return aliases;
    }
}
