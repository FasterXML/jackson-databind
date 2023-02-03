package com.fasterxml.jackson.databind.ser.std;

import java.io.IOException;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.introspect.Annotated;
import com.fasterxml.jackson.databind.jsonFormatVisitors.JsonFormatVisitorWrapper;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import com.fasterxml.jackson.databind.ser.ContextualSerializer;
import com.fasterxml.jackson.databind.ser.impl.PropertySerializerMap;
import com.fasterxml.jackson.databind.type.ReferenceType;
import com.fasterxml.jackson.databind.util.ArrayBuilders;
import com.fasterxml.jackson.databind.util.BeanUtil;
import com.fasterxml.jackson.databind.util.NameTransformer;

/**
 * Base implementation for values of {@link ReferenceType}.
 * Implements most of functionality, only leaving couple of abstract
 * methods for sub-classes to implement.
 *
 * @since 2.8
 */
public abstract class ReferenceTypeSerializer<T>
    extends StdSerializer<T>
    implements ContextualSerializer
{
    private static final long serialVersionUID = 1L;

    /**
     * @since 2.9
     */
    public final static Object MARKER_FOR_EMPTY = JsonInclude.Include.NON_EMPTY;

    /**
     * Value type
     */
    protected final JavaType _referredType;

    protected final BeanProperty _property;

    /**
     * Type serializer used for values, if any.
     */
    protected final TypeSerializer _valueTypeSerializer;

    /**
     * Serializer for content values, if statically known.
     */
    protected final JsonSerializer<Object> _valueSerializer;

    /**
     * In case of unwrapping, need name transformer.
     */
    protected final NameTransformer _unwrapper;

    /**
     * If element type cannot be statically determined, mapping from
     * runtime type to serializer is handled using this object
     */
    protected transient PropertySerializerMap _dynamicSerializers;

    /*
    /**********************************************************
    /* Config settings, filtering
    /**********************************************************
     */

    /**
     * Value that indicates suppression mechanism to use for <b>values contained</b>;
     * either "filter" (of which <code>equals()</code> is called), or marker
     * value of {@link #MARKER_FOR_EMPTY}, or null to indicate no filtering for
     * non-null values.
     * Note that inclusion value for Map instance itself is handled by caller (POJO
     * property that refers to the Map value).
     *
     * @since 2.9
     */
    protected final Object _suppressableValue;

    /**
     * Flag that indicates what to do with `null` values, distinct from
     * handling of {@link #_suppressableValue}
     *
     * @since 2.9
     */
    protected final boolean _suppressNulls;

    /*
    /**********************************************************
    /* Constructors, factory methods
    /**********************************************************
     */

    public ReferenceTypeSerializer(ReferenceType fullType, boolean staticTyping,
            TypeSerializer vts, JsonSerializer<Object> ser)
    {
        super(fullType);
        _referredType = fullType.getReferencedType();
        _property = null;
        _valueTypeSerializer = vts;
        _valueSerializer = ser;
        _unwrapper = null;
        _suppressableValue = null;
        _suppressNulls = false;
        _dynamicSerializers = PropertySerializerMap.emptyForProperties();
    }

    @SuppressWarnings("unchecked")
    protected ReferenceTypeSerializer(ReferenceTypeSerializer<?> base, BeanProperty property,
            TypeSerializer vts, JsonSerializer<?> valueSer,
            NameTransformer unwrapper,
            Object suppressableValue, boolean suppressNulls)
    {
        super(base);
        _referredType = base._referredType;
        // [databind#2181]: may not be safe to reuse, start from empty
        _dynamicSerializers = PropertySerializerMap.emptyForProperties();
        _property = property;
        _valueTypeSerializer = vts;
        _valueSerializer = (JsonSerializer<Object>) valueSer;
        _unwrapper = unwrapper;
        _suppressableValue = suppressableValue;
        _suppressNulls = suppressNulls;
    }

    @Override
    public JsonSerializer<T> unwrappingSerializer(NameTransformer transformer) {
        JsonSerializer<Object> valueSer = _valueSerializer;
        if (valueSer != null) {
            // 09-Dec-2019, tatu: [databind#2565] Can not assume that serializer in
            //    question actually can unwrap
            valueSer = valueSer.unwrappingSerializer(transformer);
            if (valueSer == _valueSerializer) {
                return this;
            }
        }
        NameTransformer unwrapper = (_unwrapper == null) ? transformer
                : NameTransformer.chainedTransformer(transformer, _unwrapper);
        if ((_valueSerializer == valueSer) && (_unwrapper == unwrapper)) {
            return this;
        }
        return withResolved(_property, _valueTypeSerializer, valueSer, unwrapper);
    }

    /*
    /**********************************************************
    /* Abstract methods to implement
    /**********************************************************
     */

    /**
     * Mutant factory method called when changes are needed; should construct
     * newly configured instance with new values as indicated.
     *<p>
     * NOTE: caller has verified that there are changes, so implementations
     * need NOT check if a new instance is needed.
     *
     * @since 2.9
     */
    protected abstract ReferenceTypeSerializer<T> withResolved(BeanProperty prop,
            TypeSerializer vts, JsonSerializer<?> valueSer,
            NameTransformer unwrapper);

    /**
     * Mutant factory method called to create a differently constructed instance,
     * specifically with different exclusion rules for contained value.
     *<p>
     * NOTE: caller has verified that there are changes, so implementations
     * need NOT check if a new instance is needed.
     *
     * @since 2.9
     */
    public abstract ReferenceTypeSerializer<T> withContentInclusion(Object suppressableValue,
            boolean suppressNulls);

    /**
     * Method called to see if there is a value present or not.
     * Note that value itself may still be `null`, even if present,
     * if referential type allows three states (absent, present-null,
     * present-non-null); some only allow two (absent, present-non-null).
     */
    protected abstract boolean _isValuePresent(T value);

    protected abstract Object _getReferenced(T value);

    protected abstract Object _getReferencedIfPresent(T value);

    /*
    /**********************************************************
    /* Contextualization (support for property annotations)
    /**********************************************************
     */

    @Override
    public JsonSerializer<?> createContextual(SerializerProvider provider,
            BeanProperty property) throws JsonMappingException
    {
        TypeSerializer typeSer = _valueTypeSerializer;
        if (typeSer != null) {
            typeSer = typeSer.forProperty(property);
        }
        // First: do we have an annotation override from property?
        JsonSerializer<?> ser = findAnnotatedContentSerializer(provider, property);
        if (ser == null) {
            // If not, use whatever was configured by type
            ser = _valueSerializer;
            if (ser == null) {
                // A few conditions needed to be able to fetch serializer here:
                if (_useStatic(provider, property, _referredType)) {
                    ser = _findSerializer(provider, _referredType, property);
                }
            } else {
                ser = provider.handlePrimaryContextualization(ser, property);
            }
        }
        // First, resolve wrt property, resolved serializers
        ReferenceTypeSerializer<?> refSer;
        if ((_property == property)
                && (_valueTypeSerializer == typeSer) && (_valueSerializer == ser)) {
            refSer = this;
        } else {
            refSer = withResolved(property, typeSer, ser, _unwrapper);
        }

        // and then see if we have property-inclusion overrides
        if (property != null) {
            JsonInclude.Value inclV = property.findPropertyInclusion(provider.getConfig(), handledType());
            if (inclV != null) {
                JsonInclude.Include incl = inclV.getContentInclusion();

                if (incl != JsonInclude.Include.USE_DEFAULTS) {
                    Object valueToSuppress;
                    boolean suppressNulls;
                    switch (incl) {
                    case NON_DEFAULT:
                        valueToSuppress = BeanUtil.getDefaultValue(_referredType);
                        suppressNulls = true;
                        if (valueToSuppress != null) {
                            if (valueToSuppress.getClass().isArray()) {
                                valueToSuppress = ArrayBuilders.getArrayComparator(valueToSuppress);
                            }
                        }
                        break;
                    case NON_ABSENT:
                        suppressNulls = true;
                        valueToSuppress = _referredType.isReferenceType() ? MARKER_FOR_EMPTY : null;
                        break;
                    case NON_EMPTY:
                        suppressNulls = true;
                        valueToSuppress = MARKER_FOR_EMPTY;
                        break;
                    case CUSTOM:
                        valueToSuppress = provider.includeFilterInstance(null, inclV.getContentFilter());
                        if (valueToSuppress == null) { // is this legal?
                            suppressNulls = true;
                        } else {
                            suppressNulls = provider.includeFilterSuppressNulls(valueToSuppress);
                        }
                        break;
                    case NON_NULL:
                        valueToSuppress = null;
                        suppressNulls = true;
                        break;
                    case ALWAYS: // default
                    default:
                        valueToSuppress = null;
                        suppressNulls = false;
                        break;
                    }
                    if ((_suppressableValue != valueToSuppress)
                            || (_suppressNulls != suppressNulls)) {
                        refSer = refSer.withContentInclusion(valueToSuppress, suppressNulls);
                    }
                }
            }
        }
        return refSer;
    }

    protected boolean _useStatic(SerializerProvider provider, BeanProperty property,
            JavaType referredType)
    {
        // First: no serializer for `Object.class`, must be dynamic
        if (referredType.isJavaLangObject()) {
            return false;
        }
        // but if type is final, might as well fetch
        if (referredType.isFinal()) { // or should we allow annotation override? (only if requested...)
            return true;
        }
        // also: if indicated by typing, should be considered static
        if (referredType.useStaticType()) {
            return true;
        }
        // if neither, maybe explicit annotation?
        AnnotationIntrospector intr = provider.getAnnotationIntrospector();
        if ((intr != null) && (property != null)) {
            Annotated ann = property.getMember();
            if (ann != null) {
                JsonSerialize.Typing t = intr.findSerializationTyping(property.getMember());
                if (t == JsonSerialize.Typing.STATIC) {
                    return true;
                }
                if (t == JsonSerialize.Typing.DYNAMIC) {
                    return false;
                }
            }
        }
        // and finally, may be forced by global static typing (unlikely...)
        return provider.isEnabled(MapperFeature.USE_STATIC_TYPING);
    }

    /*
    /**********************************************************
    /* Accessors
    /**********************************************************
     */

    @Override
    public boolean isEmpty(SerializerProvider provider, T value)
    {
        // First, absent value (note: null check is just sanity check here)
        if (!_isValuePresent(value)) {
            return true;
        }
        Object contents = _getReferenced(value);
        if (contents == null) { // possible for explicitly contained `null`
            return _suppressNulls;
        }
        if (_suppressableValue == null) {
            return false;
        }
        JsonSerializer<Object> ser = _valueSerializer;
        if (ser == null) {
            try {
                ser = _findCachedSerializer(provider, contents.getClass());
            } catch (JsonMappingException e) { // nasty but necessary
                throw new RuntimeJsonMappingException(e);
            }
        }
        if (_suppressableValue == MARKER_FOR_EMPTY) {
            return ser.isEmpty(provider, contents);
        }
        return _suppressableValue.equals(contents);
    }

    @Override
    public boolean isUnwrappingSerializer() {
        return (_unwrapper != null);
    }

    /**
     * @since 2.9
     */
    public JavaType getReferredType() {
        return _referredType;
    }

    /*
    /**********************************************************
    /* Serialization methods
    /**********************************************************
     */

    @Override
    public void serialize(T ref, JsonGenerator g, SerializerProvider provider)
        throws IOException
    {
        Object value = _getReferencedIfPresent(ref);
        if (value == null) {
            if (_unwrapper == null) {
                provider.defaultSerializeNull(g);
            }
            return;
        }
        JsonSerializer<Object> ser = _valueSerializer;
        if (ser == null) {
            ser = _findCachedSerializer(provider, value.getClass());
        }
        if (_valueTypeSerializer != null) {
            ser.serializeWithType(value, g, provider, _valueTypeSerializer);
        } else {
            ser.serialize(value, g, provider);
        }
    }

    @Override
    public void serializeWithType(T ref,
            JsonGenerator g, SerializerProvider provider,
            TypeSerializer typeSer) throws IOException
    {
        Object value = _getReferencedIfPresent(ref);
        if (value == null) {
            if (_unwrapper == null) {
                provider.defaultSerializeNull(g);
            }
            return;
        }

        // 19-Apr-2016, tatu: In order to basically "skip" the whole wrapper level
        //    (which is what non-polymorphic serialization does too), we will need
        //    to simply delegate call, I think, and NOT try to use it here.

        // Otherwise apply type-prefix/suffix, then std serialize:
        /*
        typeSer.writeTypePrefixForScalar(ref, g);
        serialize(ref, g, provider);
        typeSer.writeTypeSuffixForScalar(ref, g);
        */
        JsonSerializer<Object> ser = _valueSerializer;
        if (ser == null) {
            ser = _findCachedSerializer(provider, value.getClass());
        }
        ser.serializeWithType(value, g, provider, typeSer);
    }

    /*
    /**********************************************************
    /* Introspection support
    /**********************************************************
     */

    @Override
    public void acceptJsonFormatVisitor(JsonFormatVisitorWrapper visitor, JavaType typeHint)
        throws JsonMappingException
    {
        JsonSerializer<?> ser = _valueSerializer;
        if (ser == null) {
            ser = _findSerializer(visitor.getProvider(), _referredType, _property);
            if (_unwrapper != null) {
                ser = ser.unwrappingSerializer(_unwrapper);
            }
        }
        ser.acceptJsonFormatVisitor(visitor, _referredType);
    }

    /*
    /**********************************************************
    /* Helper methods
    /**********************************************************
     */

    /**
     * Helper method that encapsulates logic of retrieving and caching required
     * serializer.
     */
    private final JsonSerializer<Object> _findCachedSerializer(SerializerProvider provider,
            Class<?> rawType) throws JsonMappingException
    {
        JsonSerializer<Object> ser = _dynamicSerializers.serializerFor(rawType);
        if (ser == null) {
            // NOTE: call this instead of `map._findAndAddDynamic(...)` (which in turn calls
            // `findAndAddSecondarySerializer`) since we may need to apply unwrapper
            // too, before caching. But calls made are the same
            if (_referredType.hasGenericTypes()) {
                // [databind#1673] Must ensure we will resolve all available type information
                //  so as not to miss generic declaration of, say, `List<GenericPojo>`...
                JavaType fullType = provider.constructSpecializedType(_referredType, rawType);
                // 23-Oct-2019, tatu: I _think_ we actually need to consider referenced
                //    type as "primary" to allow applying various handlers -- done since 2.11

                ser = provider.findPrimaryPropertySerializer(fullType, _property);
            } else {
                ser = provider.findPrimaryPropertySerializer(rawType, _property);
            }
            if (_unwrapper != null) {
                ser = ser.unwrappingSerializer(_unwrapper);
            }
            _dynamicSerializers = _dynamicSerializers.newWith(rawType, ser);
        }
        return ser;
    }

    private final JsonSerializer<Object> _findSerializer(SerializerProvider provider,
        JavaType type, BeanProperty prop) throws JsonMappingException
    {
        // 13-Mar-2017, tatu: Used to call `findTypeValueSerializer()`, but contextualization
        //   not working for that case for some reason
        // 15-Jan-2017, tatu: ... possibly because we need to access "secondary" serializer,
        //   not primary (primary being one for Reference type itself, not value)
//        return provider.findTypedValueSerializer(type, true, prop);
        return provider.findPrimaryPropertySerializer(type, prop);
    }
}
