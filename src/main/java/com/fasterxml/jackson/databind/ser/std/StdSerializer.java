package com.fasterxml.jackson.databind.ser.std;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.core.JsonParser.NumberType;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.annotation.JacksonStdImpl;
import com.fasterxml.jackson.databind.introspect.AnnotatedMember;
import com.fasterxml.jackson.databind.jsonFormatVisitors.*;
import com.fasterxml.jackson.databind.jsonschema.SchemaAware;
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
 * Provides convenience methods for implementing {@link SchemaAware}
 */
public abstract class StdSerializer<T>
    extends JsonSerializer<T>
    implements JsonFormatVisitable, SchemaAware, java.io.Serializable
{
    private static final long serialVersionUID = 1L;

    /**
     * Key used for storing a lock object to prevent infinite recursion when
     * constructing converting serializers.
     *
     * @since 2.9
     */
    private final static Object KEY_CONTENT_CONVERTER_LOCK = new Object();
    
    /**
     * Nominal type supported, usually declared type of
     * property for which serializer is used.
     */
    protected final Class<T> _handledType;

    /*
    /**********************************************************
    /* Life-cycle
    /**********************************************************
     */

    protected StdSerializer(Class<T> t) {
        _handledType = t;
    }

    @SuppressWarnings("unchecked")
    protected StdSerializer(JavaType type) {
        _handledType = (Class<T>) type.getRawClass();
    }

    /**
     * Alternate constructor that is (alas!) needed to work
     * around kinks of generic type handling
     */
    @SuppressWarnings("unchecked")
    protected StdSerializer(Class<?> t, boolean dummy) {
        _handledType = (Class<T>) t;
    }

    /**
     * @since 2.6
     */
    @SuppressWarnings("unchecked")
    protected StdSerializer(StdSerializer<?> src) {
        _handledType = (Class<T>) src._handledType;
    }

    /*
    /**********************************************************
    /* Accessors
    /**********************************************************
     */

    @Override
    public Class<T> handledType() { return _handledType; }

    /*
    /**********************************************************
    /* Serialization
    /**********************************************************
     */

    @Override
    public abstract void serialize(T value, JsonGenerator gen, SerializerProvider provider)
        throws IOException;

    /*
    /**********************************************************
    /* Type introspection API, partial/default implementation
    /**********************************************************
     */

    /**
     * Default implementation specifies no format. This behavior is usually
     * overriden by custom serializers.
     */
    @Override
    public void acceptJsonFormatVisitor(JsonFormatVisitorWrapper visitor, JavaType typeHint)
        throws JsonMappingException
    {
        visitor.expectAnyFormat(typeHint);
    }

    /**
     * Default implementation simply claims type is "string"; usually
     * overriden by custom serializers.
     */
    @Override
    public JsonNode getSchema(SerializerProvider provider, Type typeHint) throws JsonMappingException
    {
        return createSchemaNode("string");
    }
    
    /**
     * Default implementation simply claims type is "string"; usually
     * overriden by custom serializers.
     */
    @Override
    public JsonNode getSchema(SerializerProvider provider, Type typeHint, boolean isOptional)
        throws JsonMappingException
    {
        ObjectNode schema = (ObjectNode) getSchema(provider, typeHint);
        if (!isOptional) {
            schema.put("required", !isOptional);
        }
        return schema;
    }

    /*
    /**********************************************************
    /* Helper methods for JSON Schema generation
    /**********************************************************
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
     *
     * @since 2.7
     */
    protected void visitStringFormat(JsonFormatVisitorWrapper visitor, JavaType typeHint)
            throws JsonMappingException {
        /*JsonStringFormatVisitor v2 =*/ visitor.expectStringFormat(typeHint);
    }

    /**
     * Helper method that calls necessary visit method(s) to indicate that the
     * underlying JSON type is JSON String, but that there is a more refined
     * logical type
     *
     * @since 2.7
     */
    protected void visitStringFormat(JsonFormatVisitorWrapper visitor, JavaType typeHint,
            JsonValueFormat format)
        throws JsonMappingException
    {
        JsonStringFormatVisitor v2 = visitor.expectStringFormat(typeHint);
        if (v2 != null) {
            v2.format(format);
        }
    }

    /**
     * Helper method that calls necessary visit method(s) to indicate that the
     * underlying JSON type is JSON Integer number.
     *
     * @since 2.7
     */
    protected void visitIntFormat(JsonFormatVisitorWrapper visitor, JavaType typeHint,
            NumberType numberType)
        throws JsonMappingException
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
     *
     * @since 2.7
     */
    protected void visitIntFormat(JsonFormatVisitorWrapper visitor, JavaType typeHint,
            NumberType numberType, JsonValueFormat format)
        throws JsonMappingException
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
     *
     * @since 2.7
     */
    protected void visitFloatFormat(JsonFormatVisitorWrapper visitor, JavaType typeHint,
            NumberType numberType)
        throws JsonMappingException
    {
        JsonNumberFormatVisitor v2 = visitor.expectNumberFormat(typeHint);
        if (v2 != null) {
            v2.numberType(numberType);
        }
    }

    /**
     * @since 2.7
     */
    protected void visitArrayFormat(JsonFormatVisitorWrapper visitor, JavaType typeHint,
            JsonSerializer<?> itemSerializer, JavaType itemType)
        throws JsonMappingException
    {
        JsonArrayFormatVisitor v2 = visitor.expectArrayFormat(typeHint);
        if (_neitherNull(v2, itemSerializer)) {
            v2.itemsFormat(itemSerializer, itemType);
        }
    }

    /**
     * @since 2.7
     */
    protected void visitArrayFormat(JsonFormatVisitorWrapper visitor, JavaType typeHint,
            JsonFormatTypes itemType)
        throws JsonMappingException
    {
        JsonArrayFormatVisitor v2 = visitor.expectArrayFormat(typeHint);
        if (v2 != null) {
            v2.itemsFormat(itemType);
        }
    }

    /*
    /**********************************************************
    /* Helper methods for exception handling
    /**********************************************************
     */
    
    /**
     * Method that will modify caught exception (passed in as argument)
     * as necessary to include reference information, and to ensure it
     * is a subtype of {@link IOException}, or an unchecked exception.
     *<p>
     * Rules for wrapping and unwrapping are bit complicated; essentially:
     *<ul>
     * <li>Errors are to be passed as is (if uncovered via unwrapping)
     * <li>"Plain" IOExceptions (ones that are not of type
     *   {@link JsonMappingException} are to be passed as is
     *</ul>
     */
    public void wrapAndThrow(SerializerProvider provider,
            Throwable t, Object bean, String fieldName)
        throws IOException
    {
        /* 05-Mar-2009, tatu: But one nasty edge is when we get
         *   StackOverflow: usually due to infinite loop. But that
         *   usually gets hidden within an InvocationTargetException...
         */
        while (t instanceof InvocationTargetException && t.getCause() != null) {
            t = t.getCause();
        }
        // Errors and "plain" to be passed as is
        ClassUtil.throwIfError(t);
        // Ditto for IOExceptions... except for mapping exceptions!
        boolean wrap = (provider == null) || provider.isEnabled(SerializationFeature.WRAP_EXCEPTIONS);
        if (t instanceof IOException) {
            if (!wrap || !(t instanceof JsonMappingException)) {
                throw (IOException) t;
            }
        } else if (!wrap) {
            ClassUtil.throwIfRTE(t);
        }
        // Need to add reference information
        throw JsonMappingException.wrapWithPath(t, bean, fieldName);
    }

    public void wrapAndThrow(SerializerProvider provider,
            Throwable t, Object bean, int index)
        throws IOException
    {
        while (t instanceof InvocationTargetException && t.getCause() != null) {
            t = t.getCause();
        }
        // Errors are to be passed as is
        ClassUtil.throwIfError(t);
        // Ditto for IOExceptions... except for mapping exceptions!
        boolean wrap = (provider == null) || provider.isEnabled(SerializationFeature.WRAP_EXCEPTIONS);
        if (t instanceof IOException) {
            if (!wrap || !(t instanceof JsonMappingException)) {
                throw (IOException) t;
            }
        } else if (!wrap) {
            ClassUtil.throwIfRTE(t);
        }
        // Need to add reference information
        throw JsonMappingException.wrapWithPath(t, bean, index);
    }

    /*
    /**********************************************************
    /* Helper methods, accessing annotation-based configuration
    /**********************************************************
     */

    /**
     * Helper method that can be used to see if specified property has annotation
     * indicating that a converter is to be used for contained values (contents
     * of structured types; array/List/Map values)
     * 
     * @param existingSerializer (optional) configured content
     *    serializer if one already exists.
     * 
     * @since 2.9
     */
    protected JsonSerializer<?> findContextualConvertingSerializer(SerializerProvider provider,
            BeanProperty property, JsonSerializer<?> existingSerializer)
        throws JsonMappingException
    {
        // 08-Dec-2016, tatu: to fix [databind#357], need to prevent recursive calls for
        //     same property
        @SuppressWarnings("unchecked")
        Map<Object,Object> conversions = (Map<Object,Object>) provider.getAttribute(KEY_CONTENT_CONVERTER_LOCK);
        if (conversions != null) {
            Object lock = conversions.get(property);
            if (lock != null) {
                return existingSerializer;
            }
        } else {
            conversions = new IdentityHashMap<>();
            provider.setAttribute(KEY_CONTENT_CONVERTER_LOCK, conversions);
        }
        conversions.put(property, Boolean.TRUE);
        try {
            JsonSerializer<?> ser = findConvertingContentSerializer(provider, property, existingSerializer);
            if (ser != null) {
                return provider.handleSecondaryContextualization(ser, property);
            }
        } finally {
            conversions.remove(property);
        }
        return existingSerializer;
    }

    /**
     * @deprecated Since 2.9 use {link {@link #findContextualConvertingSerializer} instead
     */
    @Deprecated
    protected JsonSerializer<?> findConvertingContentSerializer(SerializerProvider provider,
            BeanProperty prop, JsonSerializer<?> existingSerializer)
        throws JsonMappingException
    {
        final AnnotationIntrospector intr = provider.getAnnotationIntrospector();
        if (_neitherNull(intr, prop)) {
            AnnotatedMember m = prop.getMember();
            if (m != null) {
                Object convDef = intr.findSerializationContentConverter(m);
                if (convDef != null) {
                    Converter<Object,Object> conv = provider.converterInstance(prop.getMember(), convDef);
                    JavaType delegateType = conv.getOutputType(provider.getTypeFactory());
                    // [databind#731]: Should skip if nominally java.lang.Object
                    if ((existingSerializer == null) && !delegateType.isJavaLangObject()) {
                        existingSerializer = provider.findValueSerializer(delegateType);
                    }
                    return new StdDelegatingSerializer(conv, delegateType, existingSerializer);
                }
            }
        }
        return existingSerializer;
    }

    /**
     * Helper method used to locate filter that is needed, based on filter id
     * this serializer was constructed with.
     * 
     * @since 2.3
     */
    protected PropertyFilter findPropertyFilter(SerializerProvider provider,
            Object filterId, Object valueToFilter)
        throws JsonMappingException
    {
        FilterProvider filters = provider.getFilterProvider();
        // Not ok to miss the provider, if a filter is declared to be needed.
        if (filters == null) {
            provider.reportBadDefinition(handledType(),
                    "Cannot resolve PropertyFilter with id '"+filterId+"'; no FilterProvider configured");
        }
        // But whether unknown ids are ok just depends on filter provider; if we get null that's fine
        return filters.findPropertyFilter(filterId, valueToFilter);
    }

    /**
     * Helper method that may be used to find if this deserializer has specific
     * {@link JsonFormat} settings, either via property, or through type-specific
     * defaulting.
     *
     * @param typeForDefaults Type (erased) used for finding default format settings, if any
     *
     * @since 2.7
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
     *
     * @since 2.7
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

    /**
     * @since 2.8
     */
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
     *
     * @since 2.7.4
     */
    protected JsonSerializer<?> findAnnotatedContentSerializer(SerializerProvider serializers,
            BeanProperty property)
        throws JsonMappingException
    {
        if (property != null) {
            // First: if we have a property, may have property-annotation overrides
            AnnotatedMember m = property.getMember();
            final AnnotationIntrospector intr = serializers.getAnnotationIntrospector();
            if (m != null) {
                Object serDef = intr.findContentSerializer(m);
                if (serDef != null) {
                    return serializers.serializerInstance(m, serDef);
                }
            }
        }
        return null;
    }

    /*
    /**********************************************************
    /* Helper methods, other
    /**********************************************************
     */
    
    /**
     * Method that can be called to determine if given serializer is the default
     * serializer Jackson uses; as opposed to a custom serializer installed by
     * a module or calling application. Determination is done using
     * {@link JacksonStdImpl} annotation on serializer class.
     */
    protected boolean isDefaultSerializer(JsonSerializer<?> serializer) {
        return ClassUtil.isJacksonStdImpl(serializer);
    }

    /**
     * @since 2.9
     */
    protected final static boolean _neitherNull(Object a, Object b) {
        return (a != null) && (b != null);
    }

    /**
     * @since 2.9
     */
    protected final static boolean _nonEmpty(Collection<?> c) {
        return (c != null) && !c.isEmpty();
    }
}
