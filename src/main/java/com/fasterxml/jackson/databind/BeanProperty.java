package com.fasterxml.jackson.databind;

import java.lang.annotation.Annotation;

import com.fasterxml.jackson.databind.introspect.AnnotatedMember;
import com.fasterxml.jackson.databind.util.Annotations;
import com.fasterxml.jackson.databind.util.Named;

/**
 * Bean properties are logical entities that represent data
 * that Java objects (POJOs (Plain Old Java Objects), sometimes also called "beans")
 * contain; and that are accessed using accessors (methods like getters
 * and setters, fields, contstructor parametrers).
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
    /**
     * Method to get logical name of the property
     */
//  @Override
    public String getName();
    
    /**
     * Method to get declared type of the property.
     */
    public JavaType getType();

    /**
     * Method for finding annotation associated with this property;
     * meaning annotation associated with one of entities used to
     * access property.
     */
    public <A extends Annotation> A getAnnotation(Class<A> acls);

    /**
     * Method for finding annotation associated with context of
     * this property; usually class in which member is declared
     * (or its subtype if processing subtype).
     */
    public <A extends Annotation> A getContextAnnotation(Class<A> acls);

    /**
     * Method for accessing primary physical entity that represents the property;
     * annotated field, method or constructor property.
     */
    public AnnotatedMember getMember();
    
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
        protected final String _name;
        protected final JavaType _type;

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
        
        public Std(String name, JavaType type, Annotations contextAnnotations, AnnotatedMember member)
        {
            _name = name;
            _type = type;
            _member = member;
            _contextAnnotations = contextAnnotations;
        }
        
        public Std withType(JavaType type) {
            return new Std(_name, type, _contextAnnotations, _member);
        }
        
//        @Override
        public <A extends Annotation> A getAnnotation(Class<A> acls) {
            return (_member == null) ? null : _member.getAnnotation(acls);
        }

//        @Override
        public <A extends Annotation> A getContextAnnotation(Class<A> acls) {
            return (_contextAnnotations == null) ? null : _contextAnnotations.get(acls);
        }
        
//      @Override
        public String getName() {
            return _name;
        }

//      @Override
        public JavaType getType() {
            return _type;
        }

//      @Override
        public AnnotatedMember getMember() {
            return _member;
        }
    }
}
