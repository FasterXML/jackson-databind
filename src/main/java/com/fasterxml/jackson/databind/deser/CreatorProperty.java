package com.fasterxml.jackson.databind.deser;

import java.io.IOException;
import java.lang.annotation.Annotation;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;

import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.PropertyName;
import com.fasterxml.jackson.databind.introspect.AnnotatedMember;
import com.fasterxml.jackson.databind.introspect.AnnotatedParameter;
import com.fasterxml.jackson.databind.jsontype.TypeDeserializer;
import com.fasterxml.jackson.databind.util.Annotations;

/**
 * This concrete sub-class implements property that is passed
 * via Creator (constructor or static factory method).
 * It is not a full-featured implementation in that its set method
 * should never be called -- instead, value must separately passed.
 *<p>
 * Note on injectable values: unlike with other mutators, where
 * deserializer and injecting are separate, here we deal the two as related
 * things. This is necessary to add proper priority, as well as to simplify
 * coordination.
 */
public class CreatorProperty
    extends SettableBeanProperty
{
    private static final long serialVersionUID = 1L;

    /**
     * Placeholder that represents constructor parameter, when it is created
     * from actual constructor.
     * May be null when a synthetic instance is created.
     */
    protected final AnnotatedParameter _annotated;

    /**
     * Id of value to inject, if value injection should be used for this parameter
     * (in addition to, or instead of, regular deserialization).
     */
    protected final Object _injectableValueId;

    /**
     * @since 2.1
     */
    protected final int _creatorIndex;

    /**
     * @deprecated Since 2.2: use the method that takes <code>isRequired</code> property
     */
    @Deprecated
    public CreatorProperty(String name, JavaType type, TypeDeserializer typeDeser,
            Annotations contextAnnotations, AnnotatedParameter param,
            int index, Object injectableValueId)
    {
        this(name, type, null, typeDeser, contextAnnotations, param, index,
                injectableValueId, true);
    }
    
    /**
     * @param name Name of the logical property
     * @param type Type of the property, used to find deserializer
     * @param typeDeser Type deserializer to use for handling polymorphic type
     *    information, if one is needed
     * @param contextAnnotations Contextual annotations (usually by class that
     *    declares creator [constructor, factory method] that includes
     *    this property)
     * @param param Representation of property, constructor or factory
     *    method parameter; used for accessing annotations of the property
     * @param index Index of this property within creator invocatino
     */
    public CreatorProperty(String name, JavaType type, PropertyName wrapperName,
            TypeDeserializer typeDeser,
            Annotations contextAnnotations, AnnotatedParameter param,
            int index, Object injectableValueId,
            boolean isRequired)
    {
        super(name, type, wrapperName, typeDeser, contextAnnotations, isRequired);
        _annotated = param;
        _creatorIndex = index;
        _injectableValueId = injectableValueId;
    }

    protected CreatorProperty(CreatorProperty src, String newName) {
        super(src, newName);
        _annotated = src._annotated;
        _creatorIndex = src._creatorIndex;
        _injectableValueId = src._injectableValueId;
    }
    
    protected CreatorProperty(CreatorProperty src, JsonDeserializer<?> deser) {
        super(src, deser);
        _annotated = src._annotated;
        _creatorIndex = src._creatorIndex;
        _injectableValueId = src._injectableValueId;
    }

    @Override
    public CreatorProperty withName(String newName) {
        return new CreatorProperty(this, newName);
    }
    
    @Override
    public CreatorProperty withValueDeserializer(JsonDeserializer<?> deser) {
        return new CreatorProperty(this, deser);
    }

    /**
     * Method that can be called to locate value to be injected for this
     * property, if it is configured for this.
     */
    public Object findInjectableValue(DeserializationContext context, Object beanInstance)
    {
        if (_injectableValueId == null) {
            throw new IllegalStateException("Property '"+getName()
                    +"' (type "+getClass().getName()+") has no injectable value id configured");
        }
        return context.findInjectableValue(_injectableValueId, this, beanInstance);
    }
    
    /**
     * Method to find value to inject, and inject it to this property.
     */
    public void inject(DeserializationContext context, Object beanInstance)
        throws IOException
    {
        set(beanInstance, findInjectableValue(context, beanInstance));
    }
    
    /*
    /**********************************************************
    /* BeanProperty impl
    /**********************************************************
     */
    
    @Override
    public <A extends Annotation> A getAnnotation(Class<A> acls) {
        if (_annotated == null) {
            return null;
        }
        return _annotated.getAnnotation(acls);
    }

    @Override public AnnotatedMember getMember() {  return _annotated; }

    @Override public int getCreatorIndex() {
        return _creatorIndex;
    }
    
    /*
    /**********************************************************
    /* Overridden methods
    /**********************************************************
     */

    @Override
    public void deserializeAndSet(JsonParser jp, DeserializationContext ctxt,
                                  Object instance)
        throws IOException, JsonProcessingException
    {
        set(instance, deserialize(jp, ctxt));
    }

    @Override
    public Object deserializeSetAndReturn(JsonParser jp,
    		DeserializationContext ctxt, Object instance)
        throws IOException, JsonProcessingException
    {
        return setAndReturn(instance, deserialize(jp, ctxt));
    }
    
    @Override
    public void set(Object instance, Object value) throws IOException
    {
        /* Hmmmh. Should we return quietly (NOP), or error?
         * Perhaps better to throw an exception, since it's generally an error.
         */
        throw new IllegalStateException("Method should never be called on a "+getClass().getName());
    }

    @Override
    public Object setAndReturn(Object instance, Object value) throws IOException
    {
        return instance;
    }
    
    @Override
    public Object getInjectableValueId() {
        return _injectableValueId;
    }

    @Override
    public String toString() { return "[creator property, name '"+getName()+"'; inject id '"+_injectableValueId+"']"; }
}
