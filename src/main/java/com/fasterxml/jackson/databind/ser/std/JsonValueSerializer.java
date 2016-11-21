package com.fasterxml.jackson.databind.ser.std;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Type;
import java.util.LinkedHashSet;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonTypeInfo.As;
import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.annotation.JacksonStdImpl;
import com.fasterxml.jackson.databind.introspect.AnnotatedMember;
import com.fasterxml.jackson.databind.jsonFormatVisitors.JsonFormatVisitable;
import com.fasterxml.jackson.databind.jsonFormatVisitors.JsonFormatVisitorWrapper;
import com.fasterxml.jackson.databind.jsonFormatVisitors.JsonStringFormatVisitor;
import com.fasterxml.jackson.databind.jsonschema.SchemaAware;
import com.fasterxml.jackson.databind.jsontype.TypeIdResolver;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import com.fasterxml.jackson.databind.ser.BeanSerializer;
import com.fasterxml.jackson.databind.ser.ContextualSerializer;
import com.fasterxml.jackson.databind.util.ClassUtil;

/**
 * Serializer class that can serialize Object that have a
 * {@link com.fasterxml.jackson.annotation.JsonValue} annotation to
 * indicate that serialization should be done by calling the method
 * annotated, and serializing result it returns.
 *<p>
 * Implementation note: we will post-process resulting serializer
 * (much like what is done with {@link BeanSerializer})
 * to figure out actual serializers for final types.
 *  This must be done from {@link #createContextual} method, and NOT from constructor;
 * otherwise we could end up with an infinite loop.
 */
@SuppressWarnings("serial")
@JacksonStdImpl
public class JsonValueSerializer
    extends StdSerializer<Object>
    implements ContextualSerializer, JsonFormatVisitable, SchemaAware
{
    /**
     * @since 2.9
     */
    protected final AnnotatedMember _accessor;

    protected final JsonSerializer<Object> _valueSerializer;

    protected final BeanProperty _property;

    /**
     * This is a flag that is set in rare (?) cases where this serializer
     * is used for "natural" types (boolean, int, String, double); and where
     * we actually must force type information wrapping, even though
     * one would not normally be added.
     */
    protected final boolean _forceTypeInformation;
    
    /*
    /**********************************************************
    /* Life-cycle
    /**********************************************************
     */

    /**
     * @param ser Explicit serializer to use, if caller knows it (which
     *    occurs if and only if the "value method" was annotated with
     *    {@link com.fasterxml.jackson.databind.annotation.JsonSerialize#using}), otherwise
     *    null
     *    
     * @since 2.8 Earlier method took "raw" Method, but that does not work with access
     *    to information we need
     */
    @SuppressWarnings("unchecked")
    public JsonValueSerializer(AnnotatedMember accessor, JsonSerializer<?> ser)
    {
        super(accessor.getType());
        _accessor = accessor;
        _valueSerializer = (JsonSerializer<Object>) ser;
        _property = null;
        _forceTypeInformation = true; // gets reconsidered when we are contextualized
    }

    @SuppressWarnings("unchecked")
    public JsonValueSerializer(JsonValueSerializer src, BeanProperty property,
            JsonSerializer<?> ser, boolean forceTypeInfo)
    {
        super(_notNullClass(src.handledType()));
        _accessor = src._accessor;
        _valueSerializer = (JsonSerializer<Object>) ser;
        _property = property;
        _forceTypeInformation = forceTypeInfo;
    }

    @SuppressWarnings("unchecked")
    private final static Class<Object> _notNullClass(Class<?> cls) {
        return (cls == null) ? Object.class : (Class<Object>) cls;
    }
    
    public JsonValueSerializer withResolved(BeanProperty property,
            JsonSerializer<?> ser, boolean forceTypeInfo)
    {
        if (_property == property && _valueSerializer == ser
                && forceTypeInfo == _forceTypeInformation) {
            return this;
        }
        return new JsonValueSerializer(this, property, ser, forceTypeInfo);
    }
    
    /*
    /**********************************************************
    /* Post-processing
    /**********************************************************
     */

    /**
     * We can try to find the actual serializer for value, if we can
     * statically figure out what the result type must be.
     */
    @Override
    public JsonSerializer<?> createContextual(SerializerProvider provider,
            BeanProperty property)
        throws JsonMappingException
    {
        JsonSerializer<?> ser = _valueSerializer;
        if (ser == null) {
            /* Can only assign serializer statically if the declared type is final:
             * if not, we don't really know the actual type until we get the instance.
             */
            // 10-Mar-2010, tatu: Except if static typing is to be used
            JavaType t = _accessor.getType();
            if (provider.isEnabled(MapperFeature.USE_STATIC_TYPING) || t.isFinal()) {
                // false -> no need to cache
                /* 10-Mar-2010, tatu: Ideally we would actually separate out type
                 *   serializer from value serializer; but, alas, there's no access
                 *   to serializer factory at this point... 
                 */
                // 05-Sep-2013, tatu: I _think_ this can be considered a primary property...
                ser = provider.findPrimaryPropertySerializer(t, property);
                /* 09-Dec-2010, tatu: Turns out we must add special handling for
                 *   cases where "native" (aka "natural") type is being serialized,
                 *   using standard serializer
                 */
                boolean forceTypeInformation = isNaturalTypeWithStdHandling(t.getRawClass(), ser);
                return withResolved(property, ser, forceTypeInformation);
            }
        } else {
            // 05-Sep-2013, tatu: I _think_ this can be considered a primary property...
            ser = provider.handlePrimaryContextualization(ser, property);
            return withResolved(property, ser, _forceTypeInformation);
        }
        return this;
    }
    
    /*
    /**********************************************************
    /* Actual serialization
    /**********************************************************
     */
    
    @Override
    public void serialize(Object bean, JsonGenerator gen, SerializerProvider prov) throws IOException
    {
        try {
            Object value = _accessor.getValue(bean);
            if (value == null) {
                prov.defaultSerializeNull(gen);
                return;
            }
            JsonSerializer<Object> ser = _valueSerializer;
            if (ser == null) {
                Class<?> c = value.getClass();
                /* 10-Mar-2010, tatu: Ideally we would actually separate out type
                 *   serializer from value serializer; but, alas, there's no access
                 *   to serializer factory at this point... 
                 */
                // let's cache it, may be needed soon again
                ser = prov.findTypedValueSerializer(c, true, _property);
            }
            ser.serialize(value, gen, prov);
        } catch (Exception e) {
            wrapAndThrow(prov, e, bean, _accessor.getName() + "()");
        }
    }

    @Override
    public void serializeWithType(Object bean, JsonGenerator gen, SerializerProvider provider,
            TypeSerializer typeSer0) throws IOException
    {
        // Regardless of other parts, first need to find value to serialize:
        Object value = null;
        try {
            value = _accessor.getValue(bean);
            // and if we got null, can also just write it directly
            if (value == null) {
                provider.defaultSerializeNull(gen);
                return;
            }
            JsonSerializer<Object> ser = _valueSerializer;
            if (ser == null) { // no serializer yet? Need to fetch
//                ser = provider.findTypedValueSerializer(value.getClass(), true, _property);
                ser = provider.findValueSerializer(value.getClass(), _property);
            } else {
                /* 09-Dec-2010, tatu: To work around natural type's refusal to add type info, we do
                 *    this (note: type is for the wrapper type, not enclosed value!)
                 */
                if (_forceTypeInformation) {
                    typeSer0.writeTypePrefixForScalar(bean, gen);
                    ser.serialize(value, gen, provider);
                    typeSer0.writeTypeSuffixForScalar(bean, gen);
                    return;
                }
            }
            // 28-Sep-2016, tatu: As per [databind#1385], we do need to do some juggling
            //    to use different Object for type id (logical type) and actual serialization
            //    (delegat type).
            TypeSerializerRerouter rr = new TypeSerializerRerouter(typeSer0, bean);
            ser.serializeWithType(value, gen, provider, rr);
        } catch (Exception e) {
            wrapAndThrow(provider, e, bean, _accessor.getName() + "()");
        }
    }
    
    @SuppressWarnings("deprecation")
    @Override
    public JsonNode getSchema(SerializerProvider provider, Type typeHint)
        throws JsonMappingException
    {
        if (_valueSerializer instanceof SchemaAware) {
            return ((SchemaAware)_valueSerializer).getSchema(provider, null);
        }
        return com.fasterxml.jackson.databind.jsonschema.JsonSchema.getDefaultSchemaNode();
    }

    @Override
    public void acceptJsonFormatVisitor(JsonFormatVisitorWrapper visitor, JavaType typeHint)
        throws JsonMappingException
    {
        /* 27-Apr-2015, tatu: First things first; for JSON Schema introspection,
         *    Enum types that use `@JsonValue` are special (but NOT necessarily
         *    anything else that RETURNS an enum!)
         *    So we will need to add special
         *    handling here (see https://github.com/FasterXML/jackson-module-jsonSchema/issues/57
         *    for details).
         *    
         *    Note that meaning of JsonValue, then, is very different for Enums. Sigh.
         */
        final JavaType type = _accessor.getType();
        Class<?> declaring = _accessor.getDeclaringClass();
        if ((declaring != null) && declaring.isEnum()) {
            if (_acceptJsonFormatVisitorForEnum(visitor, typeHint, declaring)) {
                return;
            }
        }
        JsonSerializer<Object> ser = _valueSerializer;
        if (ser == null) {
            ser = visitor.getProvider().findTypedValueSerializer(type, false, _property);
            if (ser == null) { // can this ever occur?
                visitor.expectAnyFormat(typeHint);
                return;
            }
        }
        ser.acceptJsonFormatVisitor(visitor, null); 
    }

    /**
     * Overridable helper method used for special case handling of schema information for
     * Enums.
     * 
     * @return True if method handled callbacks; false if not; in latter case caller will
     *   send default callbacks
     *
     * @since 2.6
     */
    protected boolean _acceptJsonFormatVisitorForEnum(JsonFormatVisitorWrapper visitor,
            JavaType typeHint, Class<?> enumType)
        throws JsonMappingException
    {
        // Copied from EnumSerializer#acceptJsonFormatVisitor
        JsonStringFormatVisitor stringVisitor = visitor.expectStringFormat(typeHint);
        if (stringVisitor != null) {
            Set<String> enums = new LinkedHashSet<String>();
            for (Object en : enumType.getEnumConstants()) {
                try {
                    // 21-Apr-2016, tatu: This is convoluted to the max, but essentially we
                    //   call `@JsonValue`-annotated accessor method on all Enum members,
                    //   so it all "works out". To some degree.
                    enums.add(String.valueOf(_accessor.getValue(en)));
                } catch (Exception e) {
                    Throwable t = e;
                    while (t instanceof InvocationTargetException && t.getCause() != null) {
                        t = t.getCause();
                    }
                    ClassUtil.throwIfError(t);
                    throw JsonMappingException.wrapWithPath(t, en, _accessor.getName() + "()");
                }
            }
            stringVisitor.enumTypes(enums);
        }
        return true;
    }

    protected boolean isNaturalTypeWithStdHandling(Class<?> rawType, JsonSerializer<?> ser)
    {
        // First: do we have a natural type being handled?
        if (rawType.isPrimitive()) {
            if (rawType != Integer.TYPE && rawType != Boolean.TYPE && rawType != Double.TYPE) {
                return false;
            }
        } else {
            if (rawType != String.class &&
                    rawType != Integer.class && rawType != Boolean.class && rawType != Double.class) {
                return false;
            }
        }
        return isDefaultSerializer(ser);
    }

    /*
    /**********************************************************
    /* Other methods
    /**********************************************************
     */

    @Override
    public String toString() {
        return "(@JsonValue serializer for method " + _accessor.getDeclaringClass() + "#" + _accessor.getName() + ")";
    }

    /*
    /**********************************************************
    /* Helper class
    /**********************************************************
     */

    /**
     * Silly little wrapper class we need to re-route type serialization so that we can
     * override Object to use for type id (logical type) even when asking serialization
     * of something else (delegate type)
     */
    static class TypeSerializerRerouter
        extends TypeSerializer
    {
        protected final TypeSerializer _typeSerializer;
        protected final Object _forObject;

        public TypeSerializerRerouter(TypeSerializer ts, Object ob) {
            _typeSerializer = ts;
            _forObject = ob;
        }

        @Override
        public TypeSerializer forProperty(BeanProperty prop) { // should never get called
            throw new UnsupportedOperationException();
        }

        @Override
        public As getTypeInclusion() {
            return _typeSerializer.getTypeInclusion();
        }

        @Override
        public String getPropertyName() {
            return _typeSerializer.getPropertyName();
        }

        @Override
        public TypeIdResolver getTypeIdResolver() {
            return _typeSerializer.getTypeIdResolver();
        }

        @Override
        public void writeTypePrefixForScalar(Object value, JsonGenerator gen) throws IOException {
            _typeSerializer.writeTypePrefixForScalar(_forObject, gen);
        }

        @Override
        public void writeTypePrefixForObject(Object value, JsonGenerator gen) throws IOException {
            _typeSerializer.writeTypePrefixForObject(_forObject, gen);
        }

        @Override
        public void writeTypePrefixForArray(Object value, JsonGenerator gen) throws IOException {
            _typeSerializer.writeTypePrefixForArray(_forObject, gen);
        }

        @Override
        public void writeTypeSuffixForScalar(Object value, JsonGenerator gen) throws IOException {
            _typeSerializer.writeTypeSuffixForScalar(_forObject, gen);
        }

        @Override
        public void writeTypeSuffixForObject(Object value, JsonGenerator gen) throws IOException {
            _typeSerializer.writeTypeSuffixForObject(_forObject, gen);
        }

        @Override
        public void writeTypeSuffixForArray(Object value, JsonGenerator gen) throws IOException {
            _typeSerializer.writeTypeSuffixForArray(_forObject, gen);
        }

        @Override
        public void writeTypePrefixForScalar(Object value, JsonGenerator gen, Class<?> type) throws IOException {
            _typeSerializer.writeTypePrefixForScalar(_forObject, gen, type);
        }

        @Override
        public void writeTypePrefixForObject(Object value, JsonGenerator gen, Class<?> type) throws IOException {
            _typeSerializer.writeTypePrefixForObject(_forObject, gen, type);
        }

        @Override
        public void writeTypePrefixForArray(Object value, JsonGenerator gen, Class<?> type) throws IOException {
            _typeSerializer.writeTypePrefixForArray(_forObject, gen, type);
        }

        @Override
        public void writeCustomTypePrefixForScalar(Object value, JsonGenerator gen, String typeId)
                throws IOException {
            _typeSerializer.writeCustomTypePrefixForScalar(_forObject, gen, typeId);
        }

        @Override
        public void writeCustomTypePrefixForObject(Object value, JsonGenerator gen, String typeId) throws IOException {
            _typeSerializer.writeCustomTypePrefixForObject(_forObject, gen, typeId);
        }

        @Override
        public void writeCustomTypePrefixForArray(Object value, JsonGenerator gen, String typeId) throws IOException {
            _typeSerializer.writeCustomTypePrefixForArray(_forObject, gen, typeId);
        }

        @Override
        public void writeCustomTypeSuffixForScalar(Object value, JsonGenerator gen, String typeId) throws IOException {
            _typeSerializer.writeCustomTypeSuffixForScalar(_forObject, gen, typeId);
        }

        @Override
        public void writeCustomTypeSuffixForObject(Object value, JsonGenerator gen, String typeId) throws IOException {
            _typeSerializer.writeCustomTypeSuffixForObject(_forObject, gen, typeId);
        }

        @Override
        public void writeCustomTypeSuffixForArray(Object value, JsonGenerator gen, String typeId) throws IOException {
            _typeSerializer.writeCustomTypeSuffixForArray(_forObject, gen, typeId);
        }
    }
}
