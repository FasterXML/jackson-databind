package com.fasterxml.jackson.databind.ser.std;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.LinkedHashSet;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonTypeInfo.As;
import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.core.type.WritableTypeId;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.annotation.JacksonStdImpl;
import com.fasterxml.jackson.databind.introspect.AnnotatedMember;
import com.fasterxml.jackson.databind.jsonFormatVisitors.JsonFormatVisitorWrapper;
import com.fasterxml.jackson.databind.jsonFormatVisitors.JsonStringFormatVisitor;
import com.fasterxml.jackson.databind.jsontype.TypeIdResolver;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import com.fasterxml.jackson.databind.ser.BeanSerializer;
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
    extends StdDynamicSerializer<Object>
{
    /**
     * Accessor (field, getter) used to access value to serialize.
     */
    protected final AnnotatedMember _accessor;

    /**
     * Value for annotated accessor.
     */
    protected final JavaType _valueType;

    protected final boolean _staticTyping;

    /**
     * This is a flag that is set in rare (?) cases where this serializer
     * is used for "natural" types (boolean, int, String, double); and where
     * we actually must force type information wrapping, even though
     * one would not normally be added.
     */
    protected final boolean _forceTypeInformation;
    
    /*
    /**********************************************************************
    /* Life-cycle
    /**********************************************************************
     */

    /**
     * @param ser Explicit serializer to use, if caller knows it (which
     *    occurs if and only if the "value method" was annotated with
     *    {@link com.fasterxml.jackson.databind.annotation.JsonSerialize#using}), otherwise
     *    null
     */
    public JsonValueSerializer(JavaType nominalType,
            JavaType valueType, boolean staticTyping,
            TypeSerializer vts, JsonSerializer<?> ser,
            AnnotatedMember accessor)
    {
        super(nominalType, null, vts, ser);
        _valueType = valueType;
        _staticTyping = staticTyping;
        _accessor = accessor;
        _forceTypeInformation = true; // gets reconsidered when we are contextualized
    }

    protected JsonValueSerializer(JsonValueSerializer src, BeanProperty property,
            JsonSerializer<?> ser, boolean forceTypeInfo)
    {
        super(src, property, src._valueTypeSerializer, ser);
        _valueType = src._valueType;
        _accessor = src._accessor;
        _staticTyping = src._staticTyping;
        _forceTypeInformation = forceTypeInfo;
    }

    public JsonValueSerializer withResolved(BeanProperty property,
            JsonSerializer<?> ser, boolean forceTypeInfo)
    {
        if ((_property == property) && (_valueSerializer == ser)
                && (forceTypeInfo == _forceTypeInformation)) {
            return this;
        }
        return new JsonValueSerializer(this, property, ser, forceTypeInfo);
    }
    
    /*
    /**********************************************************************
    /* Post-processing
    /**********************************************************************
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
            // Can only assign serializer statically if the declared type is final:
            // if not, we don't really know the actual type until we get the instance.

            // 10-Mar-2010, tatu: Except if static typing is to be used
            if (_staticTyping || provider.isEnabled(MapperFeature.USE_STATIC_TYPING)
                    || _valueType.isFinal()) {
                // false -> no need to cache
                /* 10-Mar-2010, tatu: Ideally we would actually separate out type
                 *   serializer from value serializer; but, alas, there's no access
                 *   to serializer factory at this point... 
                 */
                // I _think_ this can be considered a primary property...
                ser = provider.findPrimaryPropertySerializer(_valueType, property);
                /* 09-Dec-2010, tatu: Turns out we must add special handling for
                 *   cases where "native" (aka "natural") type is being serialized,
                 *   using standard serializer
                 */
                boolean forceTypeInformation = isNaturalTypeWithStdHandling(_valueType.getRawClass(), ser);
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
    /**********************************************************************
    /* Actual serialization
    /**********************************************************************
     */

    @Override
    public void serialize(Object bean, JsonGenerator gen, SerializerProvider ctxt) throws IOException
    {
        Object value;
        try {
            value = _accessor.getValue(bean);
        } catch (Exception e) {
            wrapAndThrow(ctxt, e, bean, _accessor.getName() + "()");
            return; // never gets here
        }
        if (value == null) {
            ctxt.defaultSerializeNullValue(gen);
            return;
        }
        JsonSerializer<Object> ser = _valueSerializer;
        if (ser == null) {
            Class<?> cc = value.getClass();
            if (_valueType.hasGenericTypes()) {
                ser = _findAndAddDynamic(ctxt,
                        ctxt.constructSpecializedType(_valueType, cc));
            } else {
                ser = _findAndAddDynamic(ctxt, cc);
            }
        }
        if (_valueTypeSerializer != null) {
            ser.serializeWithType(value, gen, ctxt, _valueTypeSerializer);
        } else {
            ser.serialize(value, gen, ctxt);
        }
    }

    @Override
    public void serializeWithType(Object bean, JsonGenerator gen, SerializerProvider ctxt,
            TypeSerializer typeSer0) throws IOException
    {
        // Regardless of other parts, first need to find value to serialize:
        Object value;
        try {
            value = _accessor.getValue(bean);
        } catch (Exception e) {
            wrapAndThrow(ctxt, e, bean, _accessor.getName() + "()");
            return; // never gets here
        }
        // and if we got null, can also just write it directly
        if (value == null) {
            ctxt.defaultSerializeNullValue(gen);
            return;
        }
        JsonSerializer<Object> ser = _valueSerializer;
        if (ser == null) {
            Class<?> cc = value.getClass();
            if (_valueType.hasGenericTypes()) {
                ser = _findAndAddDynamic(ctxt, ctxt.constructSpecializedType(_valueType, cc));
            } else {
                ser = _findAndAddDynamic(ctxt, cc);
            }
        }

        // 16-Apr-2018, tatu: This is interesting piece of vestigal code but...
        //    I guess it is still needed, too.

        // 09-Dec-2010, tatu: To work around natural type's refusal to add type info, we do
        //    this (note: type is for the wrapper type, not enclosed value!)
        if (_forceTypeInformation) {
            // Confusing? Type id is for POJO and NOT for value returned by JsonValue accessor...
            WritableTypeId typeIdDef = typeSer0.writeTypePrefix(gen,
                    typeSer0.typeId(bean, JsonToken.VALUE_STRING));
            ser.serialize(value, gen, ctxt);
            typeSer0.writeTypeSuffix(gen, typeIdDef);
            return;
        }

        // 28-Sep-2016, tatu: As per [databind#1385], we do need to do some juggling
        //    to use different Object for type id (logical type) and actual serialization
        //    (delegate type).

        // 16-Apr-2018, tatu: What seems suspicious is that we do not use `_valueTypeSerializer`
        //    for anything but... it appears to work wrt existing tests, and alternative
        //    is not very clear. So most likely it'll fail at some point and require
        //    full investigation. But not today.
        TypeSerializerRerouter rr = new TypeSerializerRerouter(typeSer0, bean);
        ser.serializeWithType(value, gen, ctxt, rr);
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
            ser = visitor.getProvider().findPrimaryPropertySerializer(type, _property);
            if (ser == null) { // can this ever occur?
                visitor.expectAnyFormat(typeHint);
                return;
            }
        }
        ser.acceptJsonFormatVisitor(visitor, type);
    }

    /**
     * Overridable helper method used for special case handling of schema information for
     * Enums.
     * 
     * @return True if method handled callbacks; false if not; in latter case caller will
     *   send default callbacks
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
    /**********************************************************************
    /* Helper class(es)
    /**********************************************************************
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
        public TypeSerializer forProperty(SerializerProvider ctxt,
                BeanProperty prop) { // should never get called
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
        public WritableTypeId writeTypePrefix(JsonGenerator g,
                WritableTypeId typeId) throws IOException {
            // 28-Jun-2017, tatu: Important! Need to "override" value
            typeId.forValue = _forObject;
            return _typeSerializer.writeTypePrefix(g, typeId);
        }

        @Override
        public WritableTypeId writeTypeSuffix(JsonGenerator g,
                WritableTypeId typeId) throws IOException {
            // NOTE: already overwrote value object so:
            return _typeSerializer.writeTypeSuffix(g, typeId);
        }
    }
}
