package tools.jackson.databind.introspect;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;

import tools.jackson.databind.AnnotationIntrospector;
import tools.jackson.databind.BeanProperty;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.PropertyMetadata;
import tools.jackson.databind.PropertyName;
import tools.jackson.databind.cfg.MapperConfig;
import tools.jackson.databind.util.Annotations;

/**
 * Intermediate {@link BeanProperty} class shared by concrete readable- and
 * writable property implementations for sharing common functionality.
 */
public abstract class ConcreteBeanPropertyBase
    implements BeanProperty
{
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
        JsonFormat.Value format = config.getDefaultPropertyFormat(baseType);
        JsonFormat.Value overrides = findFormatOverrides(config);

        return (overrides == null) ? format : format.withOverrides(overrides);
    }

    @Override
    public JsonInclude.Value findPropertyInclusion(MapperConfig<?> config, Class<?> baseType)
    {
        final AnnotationIntrospector intr = config.getAnnotationIntrospector();
        final AnnotatedMember member = getMember();
        if (member == null) {
            return config.getDefaultPropertyInclusion(baseType);
        }
        // Start with type-based defaults (including ConfigOverrides for property type)
        JsonInclude.Value v0 = config.getDefaultInclusion(baseType, member.getRawType());
        if (intr == null) {
            return v0;
        }

        // [databind#1649]: Check for class-level @JsonInclude annotation via context annotations
        // to properly resolve class-level JsonInclude settings (esp. "content" for Maps/Collections)
        AnnotationsAsAnnotated classAnns = AnnotationsAsAnnotated.of(contextAnnotations(), getName());
        if (classAnns != null) {
            JsonInclude.Value classIncl = intr.findPropertyInclusion(config, classAnns);
            if (classIncl != null) {
                v0 = (v0 == null) ? classIncl : v0.withOverrides(classIncl);
            }
        }

        // and finally per-property overrides
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

    // @since 3.1
    protected abstract Annotations contextAnnotations();

    /**
     * @since 3.1
     */
    private static class AnnotationsAsAnnotated 
        extends Annotated
    {
        private final Annotations _annotations;
        private final String _name;

        private AnnotationsAsAnnotated(Annotations ann, String name) {
            _annotations = Objects.requireNonNull(ann);
            _name = name;
        }

        public static AnnotationsAsAnnotated of(Annotations ann, String name) {
            if (ann == null) {
                return null;
            }
            return new AnnotationsAsAnnotated(ann, name);
        }

        @Override
        public <A extends Annotation> A getAnnotation(Class<A> acls) {
            return _annotations.get(acls);
        }

        @Override
        public Stream<Annotation> annotations() {
            return _annotations.values();
        }

        @Override
        public boolean hasAnnotation(Class<? extends Annotation> acls) {
            return _annotations.has(acls);
        }

        @Override
        public boolean hasOneOf(Class<? extends Annotation>[] annoClasses) {
            return _annotations.hasOneOf(annoClasses);
        }

        @Override
        public AnnotatedElement getAnnotated() {
            return null;
        }

        @Override
        protected int getModifiers() {
            return 0;
        }

        @Override
        public String getName() {
            return _name;
        }

        @Override
        public JavaType getType() {
            return null;
        }

        @Override
        public Class<?> getRawType() {
            return null;
        }

        @Override
        public boolean equals(Object o) {
            return false;
        }

        @Override
        public int hashCode() {
            return -1;
        }

        @Override
        public String toString() {
            return getClass().getSimpleName()+"{'"+ _name+"'}";
        }
    }
}
