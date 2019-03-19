package com.fasterxml.jackson.databind;

import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonFormat.Value;
import com.fasterxml.jackson.annotation.JsonInclude;

import com.fasterxml.jackson.databind.cfg.MapperConfig;
import com.fasterxml.jackson.databind.introspect.AnnotatedMember;
import com.fasterxml.jackson.databind.jsonFormatVisitors.JsonObjectFormatVisitor;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.fasterxml.jackson.databind.util.FullyNamed;

/**
 * Bean properties are logical entities that represent data
 * that Java objects (POJOs (Plain Old Java Objects), sometimes also called "beans")
 * contain; and that are accessed using accessors (methods like getters
 * and setters, fields, constructor parameters).
 * Instances allow access to annotations directly associated
 * to property (via field or method), as well as contextual
 * annotations (annotations for class that contains properties).
 *<p>
 * Instances are not typically passed when constructing serializers
 * and deserializers, but rather only passed when context
 * is known and
 * {@link JsonSerializer#createContextual} and
 * {@link JsonDeserializer#createContextual} are called.
 * References may (need to) be retained by serializers and deserializers,
 * especially when further resolving dependent handlers like value
 * serializers/deserializers or structured types.
 */
public interface BeanProperty extends FullyNamed
{
    public final static JsonFormat.Value EMPTY_FORMAT = new JsonFormat.Value();
    public final static JsonInclude.Value EMPTY_INCLUDE = JsonInclude.Value.empty();

    // // // From FullyNamed
    
    //public String getName();
    //public PropertyName getFullName();

    /**
     * Method to get declared type of the property.
     */
    public JavaType getType();

    /**
     * If property is indicated to be wrapped, name of
     * wrapper element to use.
     */
    public PropertyName getWrapperName();

    /**
     * Accessor for additional optional information about property.
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
     */
    public boolean isRequired();

    /**
     * Accessor for checking whether there is an actual physical property
     * behind this property abstraction or not.
     */
    public boolean isVirtual();

    /*
    /**********************************************************************
    /* Access to annotation information
    /**********************************************************************
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
     * Helper method used to look up format settings applicable to this property,
     * considering both possible per-type configuration settings
     */
    public JsonFormat.Value findPropertyFormat(MapperConfig<?> config, Class<?> baseType);

    /**
     * Helper method used to only access property-specified format overrides, if any,
     * not considering type or global default format settings.
     *
     * @return Format override settings if any; `null` if no overrides
     *
     * @since 3.0
     */
    public JsonFormat.Value findFormatOverrides(MapperConfig<?> config);
    
    /**
     * Convenience method that is roughly equivalent to
     *<pre>
     *   return config.getAnnotationIntrospector().findPropertyInclusion(getMember());
     *</pre>
     * but also considers global default settings for inclusion
     */
    public JsonInclude.Value findPropertyInclusion(MapperConfig<?> config, Class<?> baseType);

    /**
     * Method for accessing set of possible alternate names that are accepted
     * during deserialization.
     *
     * @return List (possibly empty) of alternate names; never null
     */
    public List<PropertyName> findAliases(MapperConfig<?> config);

    /*
    /**********************************************************************
    /* Schema/introspection support
    /**********************************************************************
     */
    
    /**
     * Method that can be called to visit the type structure that this
     * property is part of.
     * Note that not all implementations support traversal with this
     * method; those that do not should throw
     * {@link UnsupportedOperationException}.
     * 
     * @param objectVisitor Visitor to used as the callback handler
     */
    public void depositSchemaProperty(JsonObjectFormatVisitor objectVisitor,
            SerializerProvider provider)
        throws JsonMappingException;

    /*
    /**********************************************************************
    /* Helper classes
    /**********************************************************************
     */

    /**
     * Simple stand-alone implementation, useful as a placeholder
     * or base class for more complex implementations.
     */
    public static class Std implements BeanProperty,
        java.io.Serializable
    {
        private static final long serialVersionUID = 3L;

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

        public Std(PropertyName name, JavaType type, PropertyName wrapperName,
                AnnotatedMember member, PropertyMetadata metadata)
        {
            _name = name;
            _type = type;
            _wrapperName = wrapperName;
            _metadata = metadata;
            _member = member;
        }

        public Std(Std base, JavaType newType) {
            this(base._name, newType, base._wrapperName, base._member, base._metadata);
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
            return null;
        }

        @Override
        public JsonFormat.Value findFormatOverrides(MapperConfig<?> config) {
            return null;
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
            JsonInclude.Value v0 = config.getDefaultInclusion(baseType, _type.getRawClass());
            AnnotationIntrospector intr = config.getAnnotationIntrospector();
            if ((intr == null) || (_member == null)) {
                return v0;
            }
            JsonInclude.Value v = intr.findPropertyInclusion(config, _member);
            if (v == null) {
                return v0;
            }
            return v0.withOverrides(v);
        }

        @Override
        public List<PropertyName> findAliases(MapperConfig<?> config) {
            // 26-Feb-2017, tatu: Do we really need to allow actual definition?
            //    For now, let's not.
            return Collections.emptyList();
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

    /**
     * Alternative "Null" implementation that can be used in cases where a non-null
     * {@link BeanProperty} is needed
     */
    public static class Bogus implements BeanProperty
    {
        @Override
        public String getName() {
            return "";
        }

        @Override
        public PropertyName getFullName() {
            return PropertyName.NO_NAME;
        }

        @Override
        public JavaType getType() {
            return TypeFactory.unknownType();
        }

        @Override
        public PropertyName getWrapperName() {
            return null;
        }

        @Override
        public PropertyMetadata getMetadata() {
            return PropertyMetadata.STD_REQUIRED_OR_OPTIONAL;
        }

        @Override
        public boolean isRequired() {
            return false;
        }

        @Override
        public boolean isVirtual() {
            return false;
        }

        @Override
        public <A extends Annotation> A getAnnotation(Class<A> acls) {
            return null;
        }

        @Override
        public <A extends Annotation> A getContextAnnotation(Class<A> acls) {
            return null;
        }

        @Override
        public AnnotatedMember getMember() {
            return null;
        }

        @Override
        public JsonFormat.Value findFormatOverrides(MapperConfig<?> config) {
            return null;
        }
        
        @Override
        public Value findPropertyFormat(MapperConfig<?> config, Class<?> baseType) {
            return Value.empty();
        }

        @Override
        public com.fasterxml.jackson.annotation.JsonInclude.Value findPropertyInclusion(
                MapperConfig<?> config, Class<?> baseType)
        {
            return null;
        }

        @Override
        public List<PropertyName> findAliases(MapperConfig<?> config) {
            return Collections.emptyList();
        }

        @Override
        public void depositSchemaProperty(JsonObjectFormatVisitor objectVisitor,
                SerializerProvider provider) throws JsonMappingException {
        }
    }
}
