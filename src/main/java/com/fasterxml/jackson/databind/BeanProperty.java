package com.fasterxml.jackson.databind;

import java.lang.annotation.Annotation;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.cfg.MapperConfig;
import com.fasterxml.jackson.databind.introspect.AnnotatedMember;
import com.fasterxml.jackson.databind.jsonFormatVisitors.JsonObjectFormatVisitor;
import com.fasterxml.jackson.databind.util.Annotations;
import com.fasterxml.jackson.databind.util.Named;

/**
 * Bean properties are logical entities that represent data
 * that Java objects (POJOs (Plain Old Java Objects), sometimes also called "beans")
 * contain; and that are accessed using accessors (methods like getters
 * and setters, fields, constructor parametrers).
 * Instances allow access to annotations directly associated
 * to property (via field or method), as well as contextual
 * annotations (annotations for class that contains properties).
 *<p>
 * Instances are not typically passed when constructing serializers
 * and deserializers, but rather only passed when context
 * is known when
 * {@link com.fasterxml.jackson.databind.ser.ContextualSerializer} and
 * {@link com.fasterxml.jackson.databind.deser.ContextualDeserializer}
 * resolution occurs (<code>createContextual(...)</code> method is called).
 * References may (need to) be retained by serializers and deserializers,
 * especially when further resolving dependant handlers like value
 * serializers/deserializers or structured types.
 */
public interface BeanProperty extends Named
{
    public final static JsonFormat.Value EMPTY_FORMAT = new JsonFormat.Value();
    public final static JsonInclude.Value EMPTY_INCLUDE = JsonInclude.Value.empty();

    /**
     * Method to get logical name of the property
     */
    @Override
    public String getName();

    /**
     * Method for getting full name definition, including possible
     * format-specific additional properties (such as namespace when
     * using XML backend).
     * 
     * @since 2.3
     */
    public PropertyName getFullName();

    /**
     * Method to get declared type of the property.
     */
    public JavaType getType();

    /**
     * If property is indicated to be wrapped, name of
     * wrapper element to use.
     * 
     * @since 2.2
     */
    public PropertyName getWrapperName();

    /**
     * Accessor for additional optional information about property.
     * 
     * @since 2.3
     * 
     * @return Metadata about property; never null.
     */
    public PropertyMetadata getMetadata();
    
    /**
     * Whether value for property is marked as required using
     * annotations or associated schema.
     * Equivalent to:
     *<code>
     *  getMetadata().isRequired()
     *</code>
     * 
     * @since 2.2
     */
    public boolean isRequired();

    /**
     * Accessor for checking whether there is an actual physical property
     * behind this property abstraction or not.
     * 
     * @since 2.7
     */
    public boolean isVirtual();

    /*
    /**********************************************************
    /* Access to annotation information
    /**********************************************************
     */
    
    /**
     * Method for finding annotation associated with this property;
     * meaning annotation associated with one of entities used to
     * access property.
     *<p>
     * Note that this method should only be called for custom annotations;
     * access to standard Jackson annotations (or ones supported by
     * alternate {@link AnnotationIntrospector}s) should be accessed
     * through {@link AnnotationIntrospector}.
     */
    public <A extends Annotation> A getAnnotation(Class<A> acls);

    /**
     * Method for finding annotation associated with context of
     * this property; usually class in which member is declared
     * (or its subtype if processing subtype).
     *<p>
     * Note that this method should only be called for custom annotations;
     * access to standard Jackson annotations (or ones supported by
     * alternate {@link AnnotationIntrospector}s) should be accessed
     * through {@link AnnotationIntrospector}.
     */
    public <A extends Annotation> A getContextAnnotation(Class<A> acls);

    /**
     * Method for accessing primary physical entity that represents the property;
     * annotated field, method or constructor property.
     */
    public AnnotatedMember getMember();

    /**
     * Convenience method that is roughly equivalent to
     *<pre>
     *   return intr.findFormat(getMember());
     *</pre>
     * and specifically does NOT try to find per-type format defaults to merge;
     * use {@link #findPropertyFormat} if such defaults would be useful.
     *
     * @since 2.6
     * 
     * @deprecated since 2.8 use {@link #findPropertyFormat} instead.
     */
    @Deprecated
    public JsonFormat.Value findFormatOverrides(AnnotationIntrospector intr);

    /**
     * Helper method used to look up format settings applicable to this property,
     * considering both possible per-type configuration settings
     *
     * @since 2.7
     */
    public JsonFormat.Value findPropertyFormat(MapperConfig<?> config, Class<?> baseType);
    
    /**
     * Convenience method that is roughly equivalent to
     *<pre>
     *   return config.getAnnotationIntrospector().findPropertyInclusion(getMember());
     *</pre>
     * but also considers global default settings for inclusion
     *
     * @since 2.7
     */
    public JsonInclude.Value findPropertyInclusion(MapperConfig<?> config, Class<?> baseType);

    /*
    /**********************************************************
    /* Schema/introspection support
    /**********************************************************
     */
    
    /**
     * Method that can be called to visit the type structure that this
     * property is part of.
     * Note that not all implementations support traversal with this
     * method; those that do not should throw
     * {@link UnsupportedOperationException}.
     *<p>
     * NOTE: Starting with 2.7, takes explicit {@link SerializerProvider}
     * argument to reduce the need to rely on provider visitor may or may not
     * have assigned.
     * 
     * @param objectVisitor Visitor to used as the callback handler
     * 
     * @since 2.2 (although signature did change in 2.7)
     */
    public void depositSchemaProperty(JsonObjectFormatVisitor objectVisitor,
            SerializerProvider provider)
        throws JsonMappingException;

    /*
    /**********************************************************
    /* Helper classes
    /**********************************************************
     */

    /**
     * Simple stand-alone implementation, useful as a placeholder
     * or base class for more complex implementations.
     */
    public static class Std implements BeanProperty
    {
        protected final PropertyName _name;
        protected final JavaType _type;
        protected final PropertyName _wrapperName;

        protected final PropertyMetadata _metadata;

        /**
         * Physical entity (field, method or constructor argument) that
         * is used to access value of property (or in case of constructor
         * property, just placeholder)
         */
        protected final AnnotatedMember _member;

        /**
         * Annotations defined in the context class (if any); may be null
         * if no annotations were found
         */
        protected final Annotations _contextAnnotations;

        public Std(PropertyName name, JavaType type, PropertyName wrapperName,
                Annotations contextAnnotations, AnnotatedMember member,
                PropertyMetadata metadata)
        {
            _name = name;
            _type = type;
            _wrapperName = wrapperName;
            _metadata = metadata;
            _member = member;
            _contextAnnotations = contextAnnotations;
        }

        /**
         * @since 2.6
         */
        public Std(Std base, JavaType newType) {
            this(base._name, newType, base._wrapperName, base._contextAnnotations, base._member, base._metadata);
        }

        public Std withType(JavaType type) {
            return new Std(this, type);
        }

        @Override
        public <A extends Annotation> A getAnnotation(Class<A> acls) {
            return (_member == null) ? null : _member.getAnnotation(acls);
        }

        @Override
        public <A extends Annotation> A getContextAnnotation(Class<A> acls) {
            return (_contextAnnotations == null) ? null : _contextAnnotations.get(acls);
        }

        @Override
        @Deprecated
        public JsonFormat.Value findFormatOverrides(AnnotationIntrospector intr) {
            if ((_member != null) && (intr != null)) {
                JsonFormat.Value v = intr.findFormat(_member);
                if (v != null) {
                    return v;
                }
            }
            return EMPTY_FORMAT;
        }

        @Override
        public JsonFormat.Value findPropertyFormat(MapperConfig<?> config, Class<?> baseType) {
            JsonFormat.Value v0 = config.getDefaultPropertyFormat(baseType);
            AnnotationIntrospector intr = config.getAnnotationIntrospector();
            if ((intr == null) || (_member == null)) {
                return v0;
            }
            JsonFormat.Value v = intr.findFormat(_member);
            if (v == null) {
                return v0;
            }
            return v0.withOverrides(v);
        }
        
        @Override
        public JsonInclude.Value findPropertyInclusion(MapperConfig<?> config, Class<?> baseType)
        {
            JsonInclude.Value v0 = config.getDefaultPropertyInclusion(baseType);
            AnnotationIntrospector intr = config.getAnnotationIntrospector();
            if ((intr == null) || (_member == null)) {
                return v0;
            }
            JsonInclude.Value v = intr.findPropertyInclusion(_member);
            if (v == null) {
                return v0;
            }
            return v0.withOverrides(v);
        }

        @Override public String getName() { return _name.getSimpleName(); }
        @Override public PropertyName getFullName() { return _name; }
        @Override public JavaType getType() { return _type; }
        @Override public PropertyName getWrapperName() { return _wrapperName; }
        @Override public boolean isRequired() { return _metadata.isRequired(); }
        @Override public PropertyMetadata getMetadata() { return _metadata; }
        @Override public AnnotatedMember getMember() { return _member; }

        @Override
        public boolean isVirtual() { return false; }

        /**
         * Implementation of this method throws
         * {@link UnsupportedOperationException}, since instances of this
         * implementation should not be used as part of actual structure
         * visited. Rather, other implementations should handle it.
         */
        @Override
        public void depositSchemaProperty(JsonObjectFormatVisitor objectVisitor,
                SerializerProvider provider) {
            throw new UnsupportedOperationException("Instances of "+getClass().getName()+" should not get visited");
        }
    }
}
