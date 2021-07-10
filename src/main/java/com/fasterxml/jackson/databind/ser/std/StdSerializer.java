package com.fasterxml.jackson.databind.ser.std;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.core.JsonParser.NumberType;
import com.fasterxml.jackson.core.exc.WrappedIOException;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.annotation.JacksonStdImpl;
import com.fasterxml.jackson.databind.introspect.AnnotatedMember;
import com.fasterxml.jackson.databind.jsonFormatVisitors.*;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.ser.FilterProvider;
import com.fasterxml.jackson.databind.ser.PropertyFilter;
import com.fasterxml.jackson.databind.util.ClassUtil;
import com.fasterxml.jackson.databind.util.Converter;

/**
 * Base class used by all standard serializers, and can also
 * be used for custom serializers (in fact, this is the recommended
 * base class to use).
 */
public abstract class StdSerializer<T>
    extends ValueSerializer<T>
    implements JsonFormatVisitable
{
    /**
     * Key used for storing a lock object to prevent infinite recursion when
     * constructing converting serializers.
     */
    private final static Object KEY_CONTENT_CONVERTER_LOCK = new Object();
    
    /**
     * Nominal type supported, usually declared type of
     * property for which serializer is used.
     */
    protected final Class<?> _handledType;

    /*
    /**********************************************************************
    /* Life-cycle
    /**********************************************************************
     */

    protected StdSerializer(Class<?> t) {
        _handledType = t;
    }

    protected StdSerializer(JavaType type) {
        _handledType = type.getRawClass();
    }

    /**
     * Alternate constructor that is (alas!) needed to work
     * around kinks of generic type handling
     */
    @Deprecated // since 3.0
    protected StdSerializer(Class<?> t, boolean dummy) {
        _handledType = t;
    }

    protected StdSerializer(StdSerializer<?> src) {
        _handledType = src._handledType;
    }

    /*
    /**********************************************************************
    /* Accessors
    /**********************************************************************
     */

    @Override
    public Class<?> handledType() { return _handledType; }

    /*
    /**********************************************************************
    /* Serialization
    /**********************************************************************
     */

    @Override
    public abstract void serialize(T value, JsonGenerator gen, SerializerProvider provider)
        throws JacksonException;

    /*
    /**********************************************************************
    /* Type introspection API, partial/default implementation
    /**********************************************************************
     */

    /**
     * Default implementation specifies no format. This behavior is usually
     * overriden by custom serializers.
     */
    @Override
    public void acceptJsonFormatVisitor(JsonFormatVisitorWrapper visitor, JavaType typeHint)
    {
        visitor.expectAnyFormat(typeHint);
    }

    /*
    /**********************************************************************
    /* Helper methods for JSON Schema generation
    /**********************************************************************
     */

    protected ObjectNode createSchemaNode(String type)
    {
        ObjectNode schema = JsonNodeFactory.instance.objectNode();
        schema.put("type", type);
        return schema;
    }
    
    protected ObjectNode createSchemaNode(String type, boolean isOptional)
    {
        ObjectNode schema = createSchemaNode(type);
        if (!isOptional) {
            schema.put("required", !isOptional);
        }
        return schema;
    }

    /**
     * Helper method that calls necessary visit method(s) to indicate that the
     * underlying JSON type is JSON String.
     */
    protected void visitStringFormat(JsonFormatVisitorWrapper visitor, JavaType typeHint) {
        /*JsonStringFormatVisitor v2 =*/ visitor.expectStringFormat(typeHint);
    }

    /**
     * Helper method that calls necessary visit method(s) to indicate that the
     * underlying JSON type is JSON String, but that there is a more refined
     * logical type
     */
    protected void visitStringFormat(JsonFormatVisitorWrapper visitor, JavaType typeHint,
            JsonValueFormat format)
    {
        JsonStringFormatVisitor v2 = visitor.expectStringFormat(typeHint);
        if (v2 != null) {
            v2.format(format);
        }
    }

    /**
     * Helper method that calls necessary visit method(s) to indicate that the
     * underlying JSON type is JSON Integer number.
     */
    protected void visitIntFormat(JsonFormatVisitorWrapper visitor, JavaType typeHint,
            NumberType numberType)
    {
        JsonIntegerFormatVisitor v2 = visitor.expectIntegerFormat(typeHint);
        if (_neitherNull(v2, numberType)) {
            v2.numberType(numberType);
        }
    }

    /**
     * Helper method that calls necessary visit method(s) to indicate that the
     * underlying JSON type is JSON Integer number, but that there is also a further
     * format restriction involved.
     */
    protected void visitIntFormat(JsonFormatVisitorWrapper visitor, JavaType typeHint,
            NumberType numberType, JsonValueFormat format)
    {
        JsonIntegerFormatVisitor v2 = visitor.expectIntegerFormat(typeHint);
        if (v2 != null) {
            if (numberType != null) {
                v2.numberType(numberType);
            }
            if (format != null) {
                v2.format(format);
            }
        }
    }
    
    /**
     * Helper method that calls necessary visit method(s) to indicate that the
     * underlying JSON type is a floating-point JSON number.
     */
    protected void visitFloatFormat(JsonFormatVisitorWrapper visitor, JavaType typeHint,
            NumberType numberType)
    {
        JsonNumberFormatVisitor v2 = visitor.expectNumberFormat(typeHint);
        if (v2 != null) {
            v2.numberType(numberType);
        }
    }

    protected void visitArrayFormat(JsonFormatVisitorWrapper visitor, JavaType typeHint,
            ValueSerializer<?> itemSerializer, JavaType itemType)
    {
        JsonArrayFormatVisitor v2 = visitor.expectArrayFormat(typeHint);
        if (_neitherNull(v2, itemSerializer)) {
            v2.itemsFormat(itemSerializer, itemType);
        }
    }

    protected void visitArrayFormat(JsonFormatVisitorWrapper visitor, JavaType typeHint,
            JsonFormatTypes itemType)
    {
        JsonArrayFormatVisitor v2 = visitor.expectArrayFormat(typeHint);
        if (v2 != null) {
            v2.itemsFormat(itemType);
        }
    }

    /*
    /**********************************************************************
    /* Helper methods for exception handling
    /**********************************************************************
     */
    
    /**
     * Method that will modify caught exception (passed in as argument)
     * as necessary to include reference information, and to ensure it
     * is a subtype of {@link JacksonException}, or an unchecked exception.
     *<p>
     * Rules for wrapping and unwrapping are bit complicated; essentially:
     *<ul>
     * <li>Errors are to be passed as is (if uncovered via unwrapping)
     * <li>Wrapped {@code IOException}s are unpeeled
     *</ul>
     */
    public void wrapAndThrow(SerializerProvider provider,
            Throwable t, Object bean, String fieldName)
        throws JacksonException
    {
        // 05-Mar-2009, tatu: But one nasty edge is when we get
        //   StackOverflow: usually due to infinite loop. But that
        //   usually gets hidden within an InvocationTargetException...
        while (t instanceof InvocationTargetException && t.getCause() != null) {
            t = t.getCause();
        }
        // 16-Jan-2021, tatu: Let's peel off useless wrapper as well
        while (t instanceof WrappedIOException && t.getCause() != null) {
            t = t.getCause();
        }
        // Errors to be passed as is, most others not
        ClassUtil.throwIfError(t);
        if (!(t instanceof JacksonException)) {
            boolean wrap = (provider == null) || provider.isEnabled(SerializationFeature.WRAP_EXCEPTIONS);
            if (!wrap) {
                ClassUtil.throwIfRTE(t);
            }
        }
        // Need to add reference information
        throw DatabindException.wrapWithPath(t, bean, fieldName);
    }

    public void wrapAndThrow(SerializerProvider provider,
            Throwable t, Object bean, int index)
        throws JacksonException
    {
        while (t instanceof InvocationTargetException && t.getCause() != null) {
            t = t.getCause();
        }
        while (t instanceof WrappedIOException && t.getCause() != null) {
            t = t.getCause();
        }
        // Errors to be passed as is, most others not
        ClassUtil.throwIfError(t);
        if (!(t instanceof JacksonException)) {
            boolean wrap = (provider == null) || provider.isEnabled(SerializationFeature.WRAP_EXCEPTIONS);
            if (!wrap) {
                ClassUtil.throwIfRTE(t);
            }
        }
        // Need to add reference information
        throw DatabindException.wrapWithPath(t, bean, index);
    }

    /*
    /**********************************************************************
    /* Helper methods, accessing annotation-based configuration
    /**********************************************************************
     */

    /**
     * Helper method that can be used to see if specified property has annotation
     * indicating that a converter is to be used for contained values (contents
     * of structured types; array/List/Map values)
     * 
     * @param existingSerializer (optional) configured content
     *    serializer if one already exists.
     */
    protected ValueSerializer<?> findContextualConvertingSerializer(SerializerProvider provider,
            BeanProperty prop, ValueSerializer<?> existingSerializer)
    {
        // 08-Dec-2016, tatu: to fix [databind#357], need to prevent recursive calls for
        //     same property
        @SuppressWarnings("unchecked")
        Map<Object,Object> conversions = (Map<Object,Object>) provider.getAttribute(KEY_CONTENT_CONVERTER_LOCK);
        if (conversions != null) {
            Object lock = conversions.get(prop);
            if (lock != null) {
                return existingSerializer;
            }
        } else {
            conversions = new IdentityHashMap<>();
            provider.setAttribute(KEY_CONTENT_CONVERTER_LOCK, conversions);
        }
        final AnnotationIntrospector intr = provider.getAnnotationIntrospector();
        if (_neitherNull(intr, prop)) {
            conversions.put(prop, Boolean.TRUE);
            try {
                ValueSerializer<?> ser = _findConvertingContentSerializer(provider, intr,
                        prop, existingSerializer);
                if (ser != null) {
                    return provider.handleSecondaryContextualization(ser, prop);
                }
            } finally {
                conversions.remove(prop);
            }
        }
        return existingSerializer;
    }

    private ValueSerializer<?> _findConvertingContentSerializer(SerializerProvider provider,
            AnnotationIntrospector intr, BeanProperty prop, ValueSerializer<?> existingSerializer)
    {
        AnnotatedMember m = prop.getMember();
        if (m != null) {
            Object convDef = intr.findSerializationContentConverter(provider.getConfig(), m);
            if (convDef != null) {
                Converter<Object,Object> conv = provider.converterInstance(prop.getMember(), convDef);
                JavaType delegateType = conv.getOutputType(provider.getTypeFactory());
                // [databind#731]: Should skip if nominally java.lang.Object
                if ((existingSerializer == null) && !delegateType.isJavaLangObject()) {
                    existingSerializer = provider.findValueSerializer(delegateType);
                }
                return new StdDelegatingSerializer(conv, delegateType, existingSerializer, prop);
            }
        }
        return existingSerializer;
    }

    /**
     * Helper method used to locate filter that is needed, based on filter id
     * this serializer was constructed with.
     */
    protected PropertyFilter findPropertyFilter(SerializerProvider provider,
            Object filterId, Object valueToFilter)
    {
        FilterProvider filters = provider.getFilterProvider();
        // Not ok to miss the provider, if a filter is declared to be needed.
        if (filters == null) {
            return provider.reportBadDefinition(handledType(),
                    "Cannot resolve PropertyFilter with id '"+filterId+"'; no FilterProvider configured");
        }
        // But whether unknown ids are ok just depends on filter provider; if we get null that's fine
        return filters.findPropertyFilter(provider, filterId, valueToFilter);
    }

    /**
     * Helper method that may be used to find if this deserializer has specific
     * {@link JsonFormat} settings, either via property, or through type-specific
     * defaulting.
     *
     * @param typeForDefaults Type (erased) used for finding default format settings, if any
     */
    protected JsonFormat.Value findFormatOverrides(SerializerProvider provider,
            BeanProperty prop, Class<?> typeForDefaults)
    {
        if (prop != null) {
            return prop.findPropertyFormat(provider.getConfig(), typeForDefaults);
        }
        // even without property or AnnotationIntrospector, may have type-specific defaults
        return provider.getDefaultPropertyFormat(typeForDefaults);
    }

    /**
     * Convenience method that uses {@link #findFormatOverrides} to find possible
     * defaults and/of overrides, and then calls <code>JsonFormat.Value.getFeature(...)</code>
     * to find whether that feature has been specifically marked as enabled or disabled.
     * 
     * @param typeForDefaults Type (erased) used for finding default format settings, if any
     */
    protected Boolean findFormatFeature(SerializerProvider provider,
            BeanProperty prop, Class<?> typeForDefaults, JsonFormat.Feature feat)
    {
        JsonFormat.Value format = findFormatOverrides(provider, prop, typeForDefaults);
        if (format != null) {
            return format.getFeature(feat);
        }
        return null;
    }

    protected JsonInclude.Value findIncludeOverrides(SerializerProvider provider,
            BeanProperty prop, Class<?> typeForDefaults)
    {
        if (prop != null) {
            return prop.findPropertyInclusion(provider.getConfig(), typeForDefaults);
        }
        // even without property or AnnotationIntrospector, may have type-specific defaults
        return provider.getDefaultPropertyInclusion(typeForDefaults);
    }
    
    /**
     * Convenience method for finding out possibly configured content value serializer.
     */
    protected ValueSerializer<?> findAnnotatedContentSerializer(SerializerProvider serializers,
            BeanProperty property)
    {
        if (property != null) {
            // First: if we have a property, may have property-annotation overrides
            AnnotatedMember m = property.getMember();
            final AnnotationIntrospector intr = serializers.getAnnotationIntrospector();
            if (m != null) {
                return serializers.serializerInstance(m,
                        intr.findContentSerializer(serializers.getConfig(), m));
            }
        }
        return null;
    }

    /*
    /**********************************************************************
    /* Helper methods, other
    /**********************************************************************
     */
    
    /**
     * Method that can be called to determine if given serializer is the default
     * serializer Jackson uses; as opposed to a custom serializer installed by
     * a module or calling application. Determination is done using
     * {@link JacksonStdImpl} annotation on serializer class.
     */
    protected boolean isDefaultSerializer(ValueSerializer<?> serializer) {
        return ClassUtil.isJacksonStdImpl(serializer);
    }

    protected final static boolean _neitherNull(Object a, Object b) {
        return (a != null) && (b != null);
    }

    protected final static boolean _nonEmpty(Collection<?> c) {
        return (c != null) && !c.isEmpty();
    }

    // @since 3.0
    protected JacksonException _wrapIOFailure(SerializerProvider ctxt, IOException e) {
        return WrappedIOException.construct(e, ctxt);
    }
}
