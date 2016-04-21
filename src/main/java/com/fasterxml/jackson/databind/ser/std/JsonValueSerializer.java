package com.fasterxml.jackson.databind.ser.std;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.LinkedHashSet;
import java.util.Set;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.annotation.JacksonStdImpl;
import com.fasterxml.jackson.databind.introspect.AnnotatedMethod;
import com.fasterxml.jackson.databind.jsonFormatVisitors.JsonFormatVisitable;
import com.fasterxml.jackson.databind.jsonFormatVisitors.JsonFormatVisitorWrapper;
import com.fasterxml.jackson.databind.jsonFormatVisitors.JsonStringFormatVisitor;
import com.fasterxml.jackson.databind.jsonschema.SchemaAware;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import com.fasterxml.jackson.databind.ser.BeanSerializer;
import com.fasterxml.jackson.databind.ser.ContextualSerializer;

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
    protected final Method _accessorMethod;

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

    // Added in 2.7.4 for forward-compatibility reasons; will be used by default in 2.8.0
    public JsonValueSerializer(AnnotatedMethod valueMethod, JsonSerializer<?> ser) {
        this(valueMethod.getAnnotated(), ser);
    }
    
    /**
     * @param ser Explicit serializer to use, if caller knows it (which
     *    occurs if and only if the "value method" was annotated with
     *    {@link com.fasterxml.jackson.databind.annotation.JsonSerialize#using}), otherwise
     *    null
     */
    @SuppressWarnings("unchecked")
    public JsonValueSerializer(Method valueMethod, JsonSerializer<?> ser)
    {
        super(valueMethod.getReturnType(), false);
        _accessorMethod = valueMethod;
        _valueSerializer = (JsonSerializer<Object>) ser;
        _property = null;
        _forceTypeInformation = true; // gets reconsidered when we are contextualized
    }

    @SuppressWarnings("unchecked")
    public JsonValueSerializer(JsonValueSerializer src, BeanProperty property,
            JsonSerializer<?> ser, boolean forceTypeInfo)
    {
        super(_notNullClass(src.handledType()));
        _accessorMethod = src._accessorMethod;
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
            if (provider.isEnabled(MapperFeature.USE_STATIC_TYPING)
                    || Modifier.isFinal(_accessorMethod.getReturnType().getModifiers())) {
                JavaType t = provider.constructType(_accessorMethod.getGenericReturnType());
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
    public void serialize(Object bean, JsonGenerator jgen, SerializerProvider prov) throws IOException
    {
        try {
            Object value = _accessorMethod.invoke(bean);
            if (value == null) {
                prov.defaultSerializeNull(jgen);
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
            ser.serialize(value, jgen, prov);
        } catch (IOException ioe) {
            throw ioe;
        } catch (Exception e) {
            Throwable t = e;
            // Need to unwrap this specific type, to see infinite recursion...
            while (t instanceof InvocationTargetException && t.getCause() != null) {
                t = t.getCause();
            }
            // Errors shouldn't be wrapped (and often can't, as well)
            if (t instanceof Error) {
                throw (Error) t;
            }
            // let's try to indicate the path best we can...
            throw JsonMappingException.wrapWithPath(t, bean, _accessorMethod.getName() + "()");
        }
    }

    @Override
    public void serializeWithType(Object bean, JsonGenerator jgen, SerializerProvider provider,
            TypeSerializer typeSer0) throws IOException
    {
        // Regardless of other parts, first need to find value to serialize:
        Object value = null;
        try {
            value = _accessorMethod.invoke(bean);
            // and if we got null, can also just write it directly
            if (value == null) {
                provider.defaultSerializeNull(jgen);
                return;
            }
            JsonSerializer<Object> ser = _valueSerializer;
            if (ser == null) { // already got a serializer? fabulous, that be easy...
//                ser = provider.findTypedValueSerializer(value.getClass(), true, _property);
                ser = provider.findValueSerializer(value.getClass(), _property);
            } else {
                /* 09-Dec-2010, tatu: To work around natural type's refusal to add type info, we do
                 *    this (note: type is for the wrapper type, not enclosed value!)
                 */
                if (_forceTypeInformation) {
                    typeSer0.writeTypePrefixForScalar(bean, jgen);
                    ser.serialize(value, jgen, provider);
                    typeSer0.writeTypeSuffixForScalar(bean, jgen);
                    return;
                }
            }
            /* 13-Feb-2013, tatu: Turns out that work-around should NOT be required
             *   at all; it would not lead to correct behavior (as per #167).
             */
            // and then redirect type id lookups
//            TypeSerializer typeSer = new TypeSerializerWrapper(typeSer0, bean);
            ser.serializeWithType(value, jgen, provider, typeSer0);
        } catch (IOException ioe) {
            throw ioe;
        } catch (Exception e) {
            Throwable t = e;
            // Need to unwrap this specific type, to see infinite recursion...
            while (t instanceof InvocationTargetException && t.getCause() != null) {
                t = t.getCause();
            }
            // Errors shouldn't be wrapped (and often can't, as well)
            if (t instanceof Error) {
                throw (Error) t;
            }
            // let's try to indicate the path best we can...
            throw JsonMappingException.wrapWithPath(t, bean, _accessorMethod.getName() + "()");
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
         *    Enums are special, and unfortunately we will need to add special
         *    handling here (see https://github.com/FasterXML/jackson-module-jsonSchema/issues/57
         *    for details).
         */
        Class<?> decl = (typeHint == null) ? null : typeHint.getRawClass();
        if (decl == null) {
            decl = _accessorMethod.getDeclaringClass();
        }
        if ((decl != null) && (decl.isEnum())) {
            if (_acceptJsonFormatVisitorForEnum(visitor, typeHint, decl)) {
                return;
            }
        }
        
        JsonSerializer<Object> ser = _valueSerializer;
        if (ser == null) {
            if (typeHint == null) {
                if (_property != null) {
                    typeHint = _property.getType();
                }
                if (typeHint == null) {
                    typeHint = visitor.getProvider().constructType(_handledType);
                }
            }
            ser = visitor.getProvider().findTypedValueSerializer(typeHint, false, _property);
            if (ser == null) {
                visitor.expectAnyFormat(typeHint);
                return;
            }
        }
        ser.acceptJsonFormatVisitor(visitor, null); 
    }

    /**
     * Overridable helper method used for special case handling of schema information for
     * Enums
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
                    enums.add(String.valueOf(_accessorMethod.invoke(en)));
                } catch (Exception e) {
                    Throwable t = e;
                    while (t instanceof InvocationTargetException && t.getCause() != null) {
                        t = t.getCause();
                    }
                    if (t instanceof Error) {
                        throw (Error) t;
                    }
                    throw JsonMappingException.wrapWithPath(t, en, _accessorMethod.getName() + "()");
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
        return "(@JsonValue serializer for method " + _accessorMethod.getDeclaringClass() + "#" + _accessorMethod.getName() + ")";
    }
}
