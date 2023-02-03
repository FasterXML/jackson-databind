package com.fasterxml.jackson.databind;

import java.io.IOException;
import java.util.Collection;

import com.fasterxml.jackson.core.*;

import com.fasterxml.jackson.databind.deser.*;
import com.fasterxml.jackson.databind.deser.impl.ObjectIdReader;
import com.fasterxml.jackson.databind.jsontype.TypeDeserializer;
import com.fasterxml.jackson.databind.type.LogicalType;
import com.fasterxml.jackson.databind.util.AccessPattern;
import com.fasterxml.jackson.databind.util.NameTransformer;

/**
 * Abstract class that defines API used by {@link ObjectMapper} (and
 * other chained {@link JsonDeserializer}s too) to deserialize Objects of
 * arbitrary types from JSON, using provided {@link JsonParser}.
 *<p>
 * Custom deserializers should usually not directly extend this class,
 * but instead extend {@link com.fasterxml.jackson.databind.deser.std.StdDeserializer}
 * (or its subtypes like {@link com.fasterxml.jackson.databind.deser.std.StdScalarDeserializer}).
 *<p>
 * If deserializer is an aggregate one -- meaning it delegates handling of some
 * of its contents by using other deserializer(s) -- it typically also needs
 * to implement {@link com.fasterxml.jackson.databind.deser.ResolvableDeserializer},
 * which can locate dependant deserializers. This is important to allow dynamic
 * overrides of deserializers; separate call interface is needed to separate
 * resolution of dependant deserializers (which may have cyclic link back
 * to deserializer itself, directly or indirectly).
 *<p>
 * In addition, to support per-property annotations (to configure aspects
 * of deserialization on per-property basis), deserializers may want
 * to implement
 * {@link com.fasterxml.jackson.databind.deser.ContextualDeserializer},
 * which allows specialization of deserializers: call to
 * {@link com.fasterxml.jackson.databind.deser.ContextualDeserializer#createContextual}
 * is passed information on property, and can create a newly configured
 * deserializer for handling that particular property.
 *<p>
 * If both
 * {@link com.fasterxml.jackson.databind.deser.ResolvableDeserializer} and
 * {@link com.fasterxml.jackson.databind.deser.ContextualDeserializer}
 * are implemented, resolution of deserializers occurs before
 * contextualization.
 */
public abstract class JsonDeserializer<T>
    implements NullValueProvider // since 2.9
{
    /*
    /**********************************************************
    /* Main deserialization methods
    /**********************************************************
     */

    /**
     * Method that can be called to ask implementation to deserialize
     * JSON content into the value type this serializer handles.
     * Returned instance is to be constructed by method itself.
     *<p>
     * Pre-condition for this method is that the parser points to the
     * first event that is part of value to deserializer (and which
     * is never JSON 'null' literal, more on this below): for simple
     * types it may be the only value; and for structured types the
     * Object start marker or a FIELD_NAME.
     * </p>
     * <p>
     * The two possible input conditions for structured types result
     * from polymorphism via fields. In the ordinary case, Jackson
     * calls this method when it has encountered an OBJECT_START,
     * and the method implementation must advance to the next token to
     * see the first field name. If the application configures
     * polymorphism via a field, then the object looks like the following.
     *  <pre>
     *      {
     *          "@class": "class name",
     *          ...
     *      }
     *  </pre>
     *  Jackson consumes the two tokens (the <tt>@class</tt> field name
     *  and its value) in order to learn the class and select the deserializer.
     *  Thus, the stream is pointing to the FIELD_NAME for the first field
     *  after the @class. Thus, if you want your method to work correctly
     *  both with and without polymorphism, you must begin your method with:
     *  <pre>
     *       if (p.currentToken() == JsonToken.START_OBJECT) {
     *         p.nextToken();
     *       }
     *  </pre>
     * This results in the stream pointing to the field name, so that
     * the two conditions align.
     * <p>
     * Post-condition is that the parser will point to the last
     * event that is part of deserialized value (or in case deserialization
     * fails, event that was not recognized or usable, which may be
     * the same event as the one it pointed to upon call).
     *<p>
     * Note that this method is never called for JSON null literal,
     * and thus deserializers need (and should) not check for it.
     *
     * @param p Parsed used for reading JSON content
     * @param ctxt Context that can be used to access information about
     *   this deserialization activity.
     *
     * @return Deserialized value
     */
    public abstract T deserialize(JsonParser p, DeserializationContext ctxt)
        throws IOException, JacksonException;

    /**
     * Alternate deserialization method (compared to the most commonly
     * used, {@link #deserialize(JsonParser, DeserializationContext)}),
     * which takes in initialized value instance, to be
     * configured and/or populated by deserializer.
     * Method is not necessarily used (or supported) by all types
     * (it will not work for immutable types, for obvious reasons):
     * most commonly it is used for Collections and Maps.
     * It may be used both with "updating readers" (for POJOs) and
     * when Collections and Maps use "getter as setter".
     *<p>
     * Default implementation just throws
     * {@link UnsupportedOperationException}, to indicate that types
     * that do not explicitly add support do not necessarily support
     * update-existing-value operation (esp. immutable types)
     */
    public T deserialize(JsonParser p, DeserializationContext ctxt, T intoValue)
        throws IOException, JacksonException
    {
        ctxt.handleBadMerge(this);
        return deserialize(p, ctxt);
    }

    /**
     * Deserialization called when type being deserialized is defined to
     * contain additional type identifier, to allow for correctly
     * instantiating correct subtype. This can be due to annotation on
     * type (or its supertype), or due to global settings without
     * annotations.
     *<p>
     * Default implementation may work for some types, but ideally subclasses
     * should not rely on current default implementation.
     * Implementation is mostly provided to avoid compilation errors with older
     * code.
     *
     * @param typeDeserializer Deserializer to use for handling type information
     */
    public Object deserializeWithType(JsonParser p, DeserializationContext ctxt,
            TypeDeserializer typeDeserializer)
        throws IOException, JacksonException
    {
        // We could try calling
        return typeDeserializer.deserializeTypedFromAny(p, ctxt);
    }

    /**
     * Method similar to {@link #deserializeWithType(JsonParser,DeserializationContext,TypeDeserializer)}
     * but called when merging value. Considered "bad merge" by default implementation,
     * but if {@link MapperFeature#IGNORE_MERGE_FOR_UNMERGEABLE} is enabled will simple delegate to
     * {@link #deserializeWithType(JsonParser, DeserializationContext, TypeDeserializer)}.
     *
     * @since 2.10
     */
    public Object deserializeWithType(JsonParser p, DeserializationContext ctxt,
            TypeDeserializer typeDeserializer, T intoValue)
        throws IOException, JacksonException
    {
        ctxt.handleBadMerge(this);
        return deserializeWithType(p, ctxt, typeDeserializer);
    }

    /*
    /**********************************************************
    /* Fluent factory methods for constructing decorated versions
    /**********************************************************
     */

    /**
     * Method that will return deserializer instance that is able
     * to handle "unwrapped" value instances
     * If no unwrapped instance can be constructed, will simply
     * return this object as-is.
     *<p>
     * Default implementation just returns 'this'
     * indicating that no unwrapped variant exists
     */
    public JsonDeserializer<T> unwrappingDeserializer(NameTransformer unwrapper) {
        return this;
    }

    /**
     * Method that can be called to try to replace deserializer this deserializer
     * delegates calls to. If not supported (either this deserializer does not
     * delegate anything; or it does not want any changes), should either
     * throw {@link UnsupportedOperationException} (if operation does not
     * make sense or is not allowed); or return this deserializer as is.
     *
     * @since 2.1
     */
    public JsonDeserializer<?> replaceDelegatee(JsonDeserializer<?> delegatee) {
        throw new UnsupportedOperationException();
    }

    /*
    /**********************************************************
    /* Introspection methods for figuring out configuration/setup
    /* of this deserializer instance and/or type it handles
    /**********************************************************
     */

    /**
     * Method for accessing concrete physical type of values this deserializer produces.
     * Note that this information is not guaranteed to be exact -- it
     * may be a more generic (super-type) -- but it should not be
     * incorrect (return a non-related type).
     *<p>
     * Default implementation will return null, which means almost same
     * same as returning <code>Object.class</code> would; that is, that
     * nothing is known about handled type.
     *
     * @return Physical type of values this deserializer produces, if known;
     *    {@code null} if not
     *
     * @since 2.3
     */
    public Class<?> handledType() { return null; }

    /**
     * Method for accessing logical type of values this deserializer produces.
     * Typically used for further configuring handling of values, for example,
     * to find which coercions are legal.
     *
     * @return Logical type of values this deserializer produces, if known;
     *    {@code null} if not
     *
     * @since 2.12
     */
    public LogicalType logicalType() { return null; }

    /**
     * Method called to see if deserializer instance is cachable and
     * usable for other properties of same type (type for which instance
     * was created).
     *<p>
     * Note that cached instances are still resolved on per-property basis,
     * if instance implements {@link com.fasterxml.jackson.databind.deser.ResolvableDeserializer}:
     * cached instance is just as the base. This means that in most cases it is safe to
     * cache instances; however, it only makes sense to cache instances
     * if instantiation is expensive, or if instances are heavy-weight.
     *<p>
     * Default implementation returns false, to indicate that no caching
     * is done.
     */
    public boolean isCachable() { return false; }

    /**
     * Accessor that can be used to determine if this deserializer uses
     * another deserializer for actual deserialization, by delegating
     * calls. If so, will return immediate delegate (which itself may
     * delegate to further deserializers); otherwise will return null.
     *
     * @return Deserializer this deserializer delegates calls to, if null;
     *   null otherwise.
     *
     * @since 2.1
     */
    public JsonDeserializer<?> getDelegatee() {
        return null;
    }

    /**
     * Method that will
     * either return null to indicate that type being deserializers
     * has no concept of properties; or a collection of identifiers
     * for which <code>toString</code> will give external property
     * name.
     * This is only to be used for error reporting and diagnostics
     * purposes (most commonly, to accompany "unknown property"
     * exception).
     *
     * @since 2.0
     */
    public Collection<Object> getKnownPropertyNames() {
        return null;
    }

    /*
    /**********************************************************
    /* Default NullValueProvider implementation
    /**********************************************************
     */

    /**
     * Method that can be called to determine value to be used for
     * representing null values (values deserialized when JSON token
     * is {@link JsonToken#VALUE_NULL}). Usually this is simply
     * Java null, but for some types (especially primitives) it may be
     * necessary to use non-null values.
     *<p>
     * This method may be called once, or multiple times, depending on what
     * {@link #getNullAccessPattern()} returns.
     *<p>
     * Default implementation simply returns null.
     *
     * @since 2.6 Added to replace earlier no-arguments variant
     */
    @Override
    public T getNullValue(DeserializationContext ctxt) throws JsonMappingException {
        // Change the direction in 2.7
        return getNullValue();
    }

    /**
     *  This method may be called in conjunction with calls to
     * {@link #getNullValue(DeserializationContext)}, to check whether it needs
     * to be called just once (static values), or each time empty value is
     * needed.
     *<p>
     * Default implementation indicates that the "null value" to use for input null
     * does not vary across uses so that {@link #getNullValue(DeserializationContext)}
     * need not be called more than once per deserializer instance.
     * This information may be used as optimization.
     */
    @Override
    public AccessPattern getNullAccessPattern() {
        // Default implementation assumes that the null value does not vary, which
        // is usually the case for most implementations. But it is not necessarily
        // `null`; so sub-classes may want to refine further.
        return AccessPattern.CONSTANT;
    }

    /**
     * Method called to determine placeholder value to be used for cases
     * where no value was obtained from input but we must pass a value
     * nonetheless: the common case is that of Creator methods requiring
     * passing a value for every parameter.
     * Usually this is same as {@link #getNullValue} (which in turn
     * is usually simply Java {@code null}), but it can be overridden
     * for specific types: most notable scalar types must use "default"
     * values.
     *<p>
     * This method needs to be called every time a determination is made.
     *<p>
     * Default implementation simply calls {@link #getNullValue} and
     * returns value.
     *
     * @since 2.13
     */
    @Override
    public Object getAbsentValue(DeserializationContext ctxt) throws JsonMappingException {
        return getNullValue(ctxt);
    }

    /*
    /**********************************************************
    /* Accessors for other replacement/placeholder values
    /**********************************************************
     */

    /**
     * Method called to determine value to be used for "empty" values
     * (most commonly when deserializing from empty JSON Strings).
     * Usually this is same as {@link #getNullValue} (which in turn
     * is usually simply Java null), but it can be overridden
     * for specific types. Or, if type should never be converted from empty
     * String, method can also throw an exception.
     *<p>
     * This method may be called once, or multiple times, depending on what
     * {@link #getEmptyAccessPattern()} returns.
     *<p>
     * Default implementation simply calls {@link #getNullValue} and
     * returns value.
     *
     * @since 2.6 Added to replace earlier no-arguments variant
     */
    public Object getEmptyValue(DeserializationContext ctxt) throws JsonMappingException {
        return getNullValue(ctxt);
    }

    /**
     * This method may be called in conjunction with calls to
     * {@link #getEmptyValue(DeserializationContext)}, to check whether it needs
     * to be called just once (static values), or each time empty value is
     * needed.
     *
     * @since 2.9
     */
    public AccessPattern getEmptyAccessPattern() {
        return AccessPattern.DYNAMIC;
    }

    /*
    /**********************************************************
    /* Other accessors
    /**********************************************************
     */

    /**
     * Accessor that can be used to check whether this deserializer
     * is expecting to possibly get an Object Identifier value instead of full value
     * serialization, and if so, should be able to resolve it to actual
     * Object instance to return as deserialized value.
     *<p>
     * Default implementation returns null, as support cannot be implemented
     * generically. Some standard deserializers (most notably
     * {@link com.fasterxml.jackson.databind.deser.BeanDeserializer})
     * do implement this feature, and may return reader instance, depending on exact
     * configuration of instance (which is based on type, and referring property).
     *
     * @return ObjectIdReader used for resolving possible Object Identifier
     *    value, instead of full value serialization, if deserializer can do that;
     *    null if no Object Id is expected.
     *
     * @since 2.0
     */
    public ObjectIdReader getObjectIdReader() { return null; }

    /**
     * Method needed by {@link BeanDeserializerFactory} to properly link
     * managed- and back-reference pairs.
     *
     * @since 2.2 (was moved out of <code>BeanDeserializerBase</code>)
     */
    public SettableBeanProperty findBackReference(String refName)
    {
        throw new IllegalArgumentException("Cannot handle managed/back reference '"+refName
                +"': type: value deserializer of type "+getClass().getName()+" does not support them");
    }

    /**
     * Introspection method that may be called to see whether deserializer supports
     * update of an existing value (aka "merging") or not. Return value should either
     * be {@link Boolean#FALSE} if update is not supported at all (immutable values);
     * {@link Boolean#TRUE} if update should usually work (regular POJOs, for example),
     * or <code>null</code> if this is either not known, or may sometimes work.
     *<p>
     * Information gathered is typically used to either prevent merging update for
     * property (either by skipping, if based on global defaults; or by exception during
     * deserializer construction if explicit attempt made) if {@link Boolean#FALSE}
     * returned, or inclusion if {@link Boolean#TRUE} is specified. If "unknown" case
     * (<code>null</code> returned) behavior is to exclude property if global defaults
     * used; or to allow if explicit per-type or property merging is defined.
     *<p>
     * Default implementation returns <code>null</code> to allow explicit per-type
     * or per-property attempts.
     *
     * @since 2.9
     */
    public Boolean supportsUpdate(DeserializationConfig config) {
        return null;
    }

    /*
    /**********************************************************
    /* Deprecated methods
    /**********************************************************
     */

    /**
     * @deprecated Since 2.6 Use overloaded variant that takes context argument
     */
    @Deprecated
    public T getNullValue() { return null; }

    /**
     * @deprecated Since 2.6 Use overloaded variant that takes context argument
     */
    @Deprecated
    public Object getEmptyValue() { return getNullValue(); }

    /*
    /**********************************************************
    /* Helper classes
    /**********************************************************
     */

    /**
     * This marker class is only to be used with annotations, to
     * indicate that <b>no deserializer is configured</b>.
     *<p>
     * Specifically, this class is to be used as the marker for
     * annotation {@link com.fasterxml.jackson.databind.annotation.JsonDeserialize}
     */
    public abstract static class None extends JsonDeserializer<Object> {
        private None() { } // not to be instantiated
    }
}
