package tools.jackson.databind;

import java.util.Iterator;

import com.fasterxml.jackson.annotation.JsonFormat;

import tools.jackson.core.*;
import tools.jackson.databind.jsonFormatVisitors.JsonFormatVisitable;
import tools.jackson.databind.jsonFormatVisitors.JsonFormatVisitorWrapper;
import tools.jackson.databind.jsontype.TypeSerializer;
import tools.jackson.databind.ser.PropertyWriter;
import tools.jackson.databind.util.ClassUtil;
import tools.jackson.databind.util.NameTransformer;

/**
 * Abstract class that defines API used by {@link ObjectMapper} (and
 * other chained {@link ValueSerializer}s too) to serialize Objects of
 * arbitrary types into JSON, using provided {@link JsonGenerator}.
 * Note that although API is defined here, custom serializer implementations
 * should almost always be based on {@link tools.jackson.databind.ser.std.StdSerializer}
 * since it will implement many of optional methods of this class.
 *<p>
 * If serializer is an aggregate one -- meaning it delegates handling of some
 * of its contents by using other serializer(s) -- it typically also needs
 * to implement {@link #resolve} which can locate secondary serializers needed.
 * This is important to allow dynamic
 * overrides of serializers; separate call interface is needed to separate
 * resolution of secondary serializers (which may have cyclic link back
 * to serializer itself, directly or indirectly).
 *<p>
 * Initialization of serializers is handled by two main methods:
 *<ol>
 *  <li>{@link #resolve}: called after instance is configured to be used for specific type,
 *     but without yet knowing property it will be used for (or, in case of root values, without property).
 *     Method needs to be implemented for serializers that may work on cyclic types, and specifically
 *     is implemented by standard POJO serializer ({@code BeanSerializer}). It is usually not needed for
 *     container types as their type definitions are not cyclic, unlike some POJO types.
 *  <li>{@link #createContextual}: called on resolved instance (whether newly created, or found via cache),
 *     when serializer is to be used for specific property, or as root value serializer (no referring property).
 *     It is used to apply annotations from property accessors (getter, field), and may also be used for resolving
 *     nested types for container serializers (such as ones for {@link java.util.Collection}s).
 * </ol>
 * Caching of serializers occurs after {@link #resolve} is called: cached instances are not contextual.
 *<p>
 * NOTE: various <code>serialize</code> methods are never (to be) called
 * with null values -- caller <b>must</b> handle null values, usually
 * by calling {@link SerializerProvider#findNullValueSerializer} to obtain
 * serializer to use.
 * This also means that custom serializers cannot be directly used to change
 * the output to produce when serializing null values.
 *<p>
 * NOTE: In Jackson 2.x was named {@code JsonSerializer}
 */
public abstract class ValueSerializer<T>
    implements JsonFormatVisitable
{
    /*
    /**********************************************************************
    /* Initialization, with former `ResolvableSerializer`,
    /* `ContextualSerializer`.
    /**********************************************************************
     */

    /**
     * Method called after {@link SerializerProvider} has registered
     * the serializer, but before it has returned it to the caller.
     * Called object can then resolve its dependencies to other types,
     * including self-references (direct or indirect).
     *<p>
     * Note that this method does NOT return serializer, since resolution
     * is not allowed to change actual serializer to use.
     *
     * @param provider Provider that has constructed serializer this method
     *   is called on.
     */
    public void resolve(SerializerProvider provider) {
        // Default implementation does nothing
    }

    /**
     * Method called to see if a different (or differently configured) serializer
     * is needed to serialize values of specified property (or, for root values, in which
     * case `null` is passed).
     * Note that instance that this method is called on is typically shared one and
     * as a result method should <b>NOT</b> modify this instance but rather construct
     * and return a new instance. This instance should only be returned as-is, in case
     * it is already suitable for use.
     *<p>
     * Note that method is only called once per POJO property, and for the first usage as root
     * value serializer; it is not called for every serialization, as doing that would have
     * significant performance impact; most serializers cache contextual instances for future
     * use.
     *
     * @param prov Serializer provider to use for accessing config, other serializers
     * @param property Property (defined by one or more accessors - field or method - used
     *     for accessing logical property value) for which serializer is used to be used;
     *     or, `null` for root value (or in cases where caller does not have this information,
     *     which is handled as root value case).
     *
     * @return Serializer to use for serializing values of specified property;
     *   may be this instance or a new instance.
     */
    public ValueSerializer<?> createContextual(SerializerProvider prov,
            BeanProperty property) {
        // default implementation returns instance unmodified
        return this;
    }

    /*
    /**********************************************************************
    /* Fluent factory methods for constructing decorated versions
    /**********************************************************************
     */

    /**
     * Method that will return serializer instance that produces
     * "unwrapped" serialization, if applicable for type being
     * serialized (which is the case for some serializers
     * that produce JSON Objects as output).
     * If no unwrapped serializer can be constructed, will simply
     * return serializer as-is.
     *<p>
     * Default implementation just returns serializer as-is,
     * indicating that no unwrapped variant exists
     *
     * @param unwrapper Name transformation to use to convert between names
     *   of unwrapper properties
     */
    public ValueSerializer<T> unwrappingSerializer(NameTransformer unwrapper) {
        return this;
    }

    /**
     * Method that can be called to try to replace serializer this serializer
     * delegates calls to. If not supported (either this serializer does not
     * delegate anything; or it does not want any changes), should either
     * throw {@link UnsupportedOperationException} (if operation does not
     * make sense or is not allowed); or return this serializer as is.
     */
    public ValueSerializer<T> replaceDelegatee(ValueSerializer<?> delegatee) {
        throw new UnsupportedOperationException();
    }

    /**
     * Mutant factory method that is called if contextual configuration indicates that
     * a specific filter (as specified by <code>filterId</code>) is to be used for
     * serialization.
     *<p>
     * Default implementation simply returns <code>this</code>; sub-classes that do support
     * filtering will need to create and return new instance if filter changes.
     */
    public ValueSerializer<?> withFilterId(Object filterId) {
        return this;
    }

    /**
     * Mutant factory called if there is need to create a serializer with specified
     * format overrides (typically from property on which this serializer would be used,
     * based on type declaration). Method is called before {@link #createContextual}
     * but right after serializer is either constructed or fetched from cache.
     *<p>
     * Method can do one of three things:
     *<ul>
     * <li>Return {@code this} instance as is: this means that none of overrides has any effect
     *  </li>
     * <li>Return an alternate {@link ValueSerializer}, suitable for use with specified format
     *  </li>
     * <li>Return {@code null} to indicate that this serializer instance is not suitable for
     *    handling format variation, but does not know how to construct new serializer: caller
     *    will typically then call {@link tools.jackson.databind.ser.SerializerFactory} with overrides to construct new serializer
     *  </li>
     *</ul>
     * One example of second approach is the case where {@link com.fasterxml.jackson.annotation.JsonFormat.Shape#STRING} indicates String
     * representation and code can just construct simple "string-like serializer", or variant of itself
     * (similar to how {@link #createContextual} is often implemented).
     * And third case (returning {@code null}) is applicable for cases like format defines
     * {@link com.fasterxml.jackson.annotation.JsonFormat.Shape#POJO}, requesting "introspect serializer for POJO regardless of type":
     * {@link tools.jackson.databind.ser.SerializerFactory} is needed for full re-introspection, typically.
     *
     * @param formatOverrides (not null) Override settings, NOT including original format settings (which
     *    serializer needs to explicitly retain if needed)
     *
     * @since 3.0
     */
    public ValueSerializer<?> withFormatOverrides(SerializationConfig config,
            JsonFormat.Value formatOverrides)
    {
        // First: if no override, safe to use as is:
        if (formatOverrides.getShape() == JsonFormat.Shape.ANY) {
            return this;
        }
        return null;
    }

    /*
    /**********************************************************************
    /* Serialization methods
    /**********************************************************************
     */

    /**
     * Method that can be called to ask implementation to serialize
     * values of type this serializer handles.
     *
     * @param value Value to serialize; can <b>not</b> be null.
     * @param gen Generator used to output resulting Json content
     * @param serializers Provider that can be used to get serializers for
     *   serializing Objects value contains, if any.
     */
    public abstract void serialize(T value, JsonGenerator gen, SerializerProvider serializers)
        throws JacksonException;

    /**
     * Method that can be called to ask implementation to serialize
     * values of type this serializer handles, using specified type serializer
     * for embedding necessary type information.
     *<p>
     * Default implementation will throw {@link UnsupportedOperationException}
     * to indicate that proper type handling needs to be implemented.
     *<p>
     * For simple datatypes written as a single scalar value (JSON String, Number, Boolean),
     * implementation would look like:
     *<pre>
     *  // note: method to call depends on whether this type is serialized as JSON scalar, object or Array!
     *  typeSer.writeTypePrefixForScalar(value, gen);
     *  serialize(value, gen, provider);
     *  typeSer.writeTypeSuffixForScalar(value, gen);
     *</pre>
     * and implementations for type serialized as JSON Arrays or Objects would differ slightly,
     * as <code>START-ARRAY</code>/<code>END-ARRAY</code> and
     * <code>START-OBJECT</code>/<code>END-OBJECT</code> pairs
     * need to be properly handled with respect to serializing of contents.
     *
     * @param value Value to serialize; can <b>not</b> be null.
     * @param gen Generator used to output resulting Json content
     * @param serializers Provider that can be used to get serializers for
     *   serializing Objects value contains, if any.
     * @param typeSer Type serializer to use for including type information
     */
    public void serializeWithType(T value, JsonGenerator gen, SerializerProvider serializers,
            TypeSerializer typeSer)
        throws JacksonException
    {
        Class<?> clz = handledType();
        if (clz == null) {
            clz = value.getClass();
        }
        serializers.reportBadDefinition(clz, String.format(
                "Type id handling not implemented for type %s (by serializer of type %s)",
                clz.getName(), getClass().getName()));
    }

    /*
    /**********************************************************************
    /* Accessors for serializer metadata
    /**********************************************************************
     */

    /**
     * Method for accessing type of Objects this serializer can handle.
     * Note that this information is not guaranteed to be exact -- it
     * may be a more generic (super-type) -- but it should not be
     * incorrect (return a non-related type).
     *<p>
     * NOTE: starting with 3.0, left {@code abstract}.
     */
    public Class<?> handledType() { return Object.class; }

    /**
     * Method that can be called to see whether this serializer instance
     * will use Object Id to handle cyclic references.
     */
    public boolean usesObjectId() {
        return false;
    }

    /**
     * Accessor for checking whether this serializer is an
     * "unwrapping" serializer; this is necessary to know since
     * it may also require caller to suppress writing of the
     * leading property name.
     */
    public boolean isUnwrappingSerializer() {
        return false;
    }

    /**
     * Accessor that can be used to determine if this serializer uses
     * another serializer for actual serialization, by delegating
     * calls. If so, will return immediate delegate (which itself may
     * delegate to further serializers); otherwise will return null.
     *
     * @return Serializer this serializer delegates calls to, if null;
     *   null otherwise.
     */
    public ValueSerializer<?> getDelegatee() {
        return null;
    }

    /**
     * Accessor for iterating over logical properties that the type
     * handled by this serializer has, from serialization perspective.
     * Actual type of properties, if any, will be
     * {@link tools.jackson.databind.ser.BeanPropertyWriter}.
     * Of standard Jackson serializers, only {@link tools.jackson.databind.ser.BeanSerializer}
     * exposes properties.
     */
    public Iterator<PropertyWriter> properties() {
        return ClassUtil.emptyIterator();
    }

    /*
    /**********************************************************************
    /* Accessors for introspecting handling of values
    /**********************************************************************
     */

    /**
     * Method called to check whether given serializable value is
     * considered "empty" value (for purposes of suppressing serialization
     * of empty values).
     *<p>
     * Default implementation will consider only null values to be empty.
     */
    public boolean isEmpty(SerializerProvider provider, T value) {
        return (value == null);
    }

    /*
    /**********************************************************************
    /* Default JsonFormatVisitable implementation
    /**********************************************************************
     */

    /**
     * Default implementation simply calls {@link JsonFormatVisitorWrapper#expectAnyFormat(JavaType)}.
     */
    @Override
    public void acceptJsonFormatVisitor(JsonFormatVisitorWrapper visitor, JavaType type)
    {
        visitor.expectAnyFormat(type);
    }

    /*
    /**********************************************************************
    /* Helper class(es)
    /**********************************************************************
     */

    /**
     * This marker class is only to be used with annotations, to
     * indicate that <b>no serializer is configured</b>.
     *<p>
     * Specifically, this class is to be used as the marker for
     * annotation {@link tools.jackson.databind.annotation.JsonSerialize}.
     */
    public abstract static class None
        extends ValueSerializer<Object> { }
}
