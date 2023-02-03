package com.fasterxml.jackson.databind.deser;

import java.io.IOException;
import java.lang.annotation.Annotation;

import com.fasterxml.jackson.annotation.JacksonInject;
import com.fasterxml.jackson.core.JsonParser;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.exc.InvalidDefinitionException;
import com.fasterxml.jackson.databind.introspect.AnnotatedMember;
import com.fasterxml.jackson.databind.introspect.AnnotatedParameter;
import com.fasterxml.jackson.databind.jsontype.TypeDeserializer;
import com.fasterxml.jackson.databind.util.Annotations;
import com.fasterxml.jackson.databind.util.ClassUtil;

/**
 * This concrete sub-class implements property that is passed
 * via Creator (constructor or static factory method).
 * It is not a full-featured implementation in that its set method
 * should usually not be called for primary mutation -- instead, value must separately passed --
 * but some aspects are still needed (specifically, injection).
 *<p>
 * Note on injectable values: unlike with other mutators, where
 * deserializer and injecting are separate, here we treat the two as related
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
     *
     * @since 2.11
     */
    protected final JacksonInject.Value _injectableValue;

    /**
     * In special cases, when implementing "updateValue", we cannot use
     * constructors or factory methods, but have to fall back on using a
     * setter (or mutable field property). If so, this refers to that fallback
     * accessor.
     *<p>
     * Mutable only to allow setting after construction, but must be strictly
     * set before any use.
     *
     * @since 2.3
     */
    protected SettableBeanProperty _fallbackSetter;

    /**
     * @since 2.1
     */
    protected final int _creatorIndex;

    /**
     * Marker flag that may have to be set during construction, to indicate that
     * although property may have been constructed and added as a placeholder,
     * it represents something that should be ignored during deserialization.
     * This mostly concerns Creator properties which may not be easily deleted
     * during processing.
     *
     * @since 2.9.4
     */
    protected boolean _ignorable;

    /**
     * @since 2.11
     */
    protected CreatorProperty(PropertyName name, JavaType type, PropertyName wrapperName,
            TypeDeserializer typeDeser,
            Annotations contextAnnotations, AnnotatedParameter param,
            int index, JacksonInject.Value injectable,
            PropertyMetadata metadata)
    {
        super(name, type, wrapperName, typeDeser, contextAnnotations, metadata);
        _annotated = param;
        _creatorIndex = index;
        _injectableValue = injectable;
        _fallbackSetter = null;
    }

    /**
     * @deprecated Since 2.11 use factory method instead
     */
    @Deprecated // since 2.11
    public CreatorProperty(PropertyName name, JavaType type, PropertyName wrapperName,
            TypeDeserializer typeDeser,
            Annotations contextAnnotations, AnnotatedParameter param,
            int index, Object injectableValueId,
            PropertyMetadata metadata)
    {
        this(name, type, wrapperName, typeDeser, contextAnnotations, param, index,
                (injectableValueId == null) ? null
                        : JacksonInject.Value.construct(injectableValueId, null),
                metadata);
    }

    /**
     * Factory method for creating {@link CreatorProperty} instances
     *
     * @param name Name of the logical property
     * @param type Type of the property, used to find deserializer
     * @param wrapperName Possible wrapper to use for logical property, if any
     * @param typeDeser Type deserializer to use for handling polymorphic type
     *    information, if one is needed
     * @param contextAnnotations Contextual annotations (usually by class that
     *    declares creator [constructor, factory method] that includes
     *    this property)
     * @param param Representation of property, constructor or factory
     *    method parameter; used for accessing annotations of the property
     * @param injectable Information about injectable value, if any
     * @param index Index of this property within creator invocation
     *
     * @since 2.11
     */
    public static CreatorProperty construct(PropertyName name, JavaType type, PropertyName wrapperName,
            TypeDeserializer typeDeser,
            Annotations contextAnnotations, AnnotatedParameter param,
            int index, JacksonInject.Value injectable,
            PropertyMetadata metadata)
    {
        return new CreatorProperty(name, type, wrapperName, typeDeser, contextAnnotations,
                param, index, injectable, metadata);
    }

    /**
     * @since 2.3
     */
    protected CreatorProperty(CreatorProperty src, PropertyName newName) {
        super(src, newName);
        _annotated = src._annotated;
        _injectableValue = src._injectableValue;
        _fallbackSetter = src._fallbackSetter;
        _creatorIndex = src._creatorIndex;
        _ignorable = src._ignorable;
    }

    protected CreatorProperty(CreatorProperty src, JsonDeserializer<?> deser,
            NullValueProvider nva) {
        super(src, deser, nva);
        _annotated = src._annotated;
        _injectableValue = src._injectableValue;
        _fallbackSetter = src._fallbackSetter;
        _creatorIndex = src._creatorIndex;
        _ignorable = src._ignorable;
    }

    @Override
    public SettableBeanProperty withName(PropertyName newName) {
        return new CreatorProperty(this, newName);
    }

    @Override
    public SettableBeanProperty withValueDeserializer(JsonDeserializer<?> deser) {
        if (_valueDeserializer == deser) {
            return this;
        }
        // 07-May-2019, tatu: As per [databind#2303], must keep VD/NVP in-sync if they were
        NullValueProvider nvp = (_valueDeserializer == _nullProvider) ? deser : _nullProvider;
        return new CreatorProperty(this, deser, nvp);
    }

    @Override
    public SettableBeanProperty withNullProvider(NullValueProvider nva) {
        return new CreatorProperty(this, _valueDeserializer, nva);
    }

    @Override
    public void fixAccess(DeserializationConfig config) {
        if (_fallbackSetter != null) {
            _fallbackSetter.fixAccess(config);
        }
    }

    /**
     * NOTE: one exception to immutability, due to problems with CreatorProperty instances
     * being shared between Bean, separate PropertyBasedCreator
     *
     * @since 2.6
     */
    public void setFallbackSetter(SettableBeanProperty fallbackSetter) {
        _fallbackSetter = fallbackSetter;
    }

    @Override
    public void markAsIgnorable() {
        _ignorable = true;
    }

    @Override
    public boolean isIgnorable() {
        return _ignorable;
    }

    /*
    /**********************************************************
    /* Injection support
    /**********************************************************
     */

    // 14-Apr-2020, tatu: Does not appear to be used so deprecated in 2.11.0,
    //    to be removed from 2.12.0

    // Method that can be called to locate value to be injected for this
    // property, if it is configured for this.
    @Deprecated // remove from 2.12
    public Object findInjectableValue(DeserializationContext context, Object beanInstance)
        throws JsonMappingException
    {
        if (_injectableValue == null) {
            context.reportBadDefinition(ClassUtil.classOf(beanInstance),
                    String.format("Property %s (type %s) has no injectable value id configured",
                    ClassUtil.name(getName()), ClassUtil.classNameOf(this)));
        }
        return context.findInjectableValue(_injectableValue.getId(), this, beanInstance); // lgtm [java/dereferenced-value-may-be-null]
    }

    // 14-Apr-2020, tatu: Does not appear to be used so deprecated in 2.11.0,
    //    to be removed from 2.12.0

    // Method to find value to inject, and inject it to this property.
    @Deprecated // remove from 2.12
    public void inject(DeserializationContext context, Object beanInstance) throws IOException
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
    /* Overridden methods, SettableBeanProperty
    /**********************************************************
     */

    @Override
    public void deserializeAndSet(JsonParser p, DeserializationContext ctxt,
            Object instance) throws IOException
    {
        _verifySetter();
        _fallbackSetter.set(instance, deserialize(p, ctxt));
    }

    @Override
    public Object deserializeSetAndReturn(JsonParser p,
            DeserializationContext ctxt, Object instance) throws IOException
    {
        _verifySetter();
        return _fallbackSetter.setAndReturn(instance, deserialize(p, ctxt));
    }

    @Override
    public void set(Object instance, Object value) throws IOException
    {
        _verifySetter();
        _fallbackSetter.set(instance, value);
    }

    @Override
    public Object setAndReturn(Object instance, Object value) throws IOException
    {
        _verifySetter();
        return _fallbackSetter.setAndReturn(instance, value);
    }

    @Override
    public PropertyMetadata getMetadata() {
        // 03-Jun-2019, tatu: Added as per [databind#2280] to support merge.
        //   Not 100% sure why it would be needed (or fixes things) but... appears to.
        //   Need to understand better in future as it seems like it should probably be
        //   linked earlier during construction or something.
        // 22-Sep-2019, tatu: Was hoping [databind#2458] fixed this, too, but no such luck
        PropertyMetadata md = super.getMetadata();
        if (_fallbackSetter != null) {
            return md.withMergeInfo(_fallbackSetter.getMetadata().getMergeInfo());
        }
        return md;
    }

    // Perhaps counter-intuitively, ONLY creator properties return non-null id
    @Override
    public Object getInjectableValueId() {
        return (_injectableValue == null) ? null : _injectableValue.getId();
    }

    @Override
    public boolean isInjectionOnly() {
        return (_injectableValue != null) && !_injectableValue.willUseInput(true);
    }

    //  public boolean isInjectionOnly() { return false; }

    /*
    /**********************************************************
    /* Overridden methods, other
    /**********************************************************
     */

    @Override
    public String toString() { return "[creator property, name "+ClassUtil.name(getName())+"; inject id '"+getInjectableValueId()+"']"; }

    /*
    /**********************************************************
    /* Internal helper methods
    /**********************************************************
     */

    // since 2.9
    private final void _verifySetter() throws IOException {
        if (_fallbackSetter == null) {
            _reportMissingSetter(null, null);
        }
    }

    // since 2.9
    private void _reportMissingSetter(JsonParser p, DeserializationContext ctxt) throws IOException
    {
        final String msg = "No fallback setter/field defined for creator property "+ClassUtil.name(getName());
        // Hmmmh. Should we return quietly (NOP), or error?
        // Perhaps better to throw an exception, since it's generally an error.
        if (ctxt != null ) {
            ctxt.reportBadDefinition(getType(), msg);
        } else {
            throw InvalidDefinitionException.from(p, msg, getType());
        }
    }
}
