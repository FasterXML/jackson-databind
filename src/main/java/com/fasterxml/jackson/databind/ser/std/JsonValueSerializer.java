package com.fasterxml.jackson.databind.ser.std;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;

import com.fasterxml.jackson.core.*;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.annotation.JacksonStdImpl;
import com.fasterxml.jackson.databind.jsonFormatVisitors.JsonFormatVisitable;
import com.fasterxml.jackson.databind.jsonFormatVisitors.JsonFormatVisitorWrapper;
import com.fasterxml.jackson.databind.jsonschema.JsonSchema;
import com.fasterxml.jackson.databind.jsonschema.SchemaAware;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import com.fasterxml.jackson.databind.ser.BeanSerializer;
import com.fasterxml.jackson.databind.ser.ContextualSerializer;

/**
 * Serializer class that can serialize Object that have a
 * {@link com.fasterxml.jackson.annotation.JsonValue} annotation to
 * indicate that serialization should be done by calling the method
 * annotated, and serializing result it returns.
 * <p/>
 * Implementation note: we will post-process resulting serializer
 * (much like what is done with {@link BeanSerializer})
 * to figure out actual serializers for final types.
 *  This must be done from {@link #createContextual} method, and NOT from constructor;
 * otherwise we could end up with an infinite loop.
 */
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

    /**
     * @param ser Explicit serializer to use, if caller knows it (which
     *    occurs if and only if the "value method" was annotated with
     *    {@link com.fasterxml.jackson.databind.annotation.JsonSerialize#using}), otherwise
     *    null
     */
    public JsonValueSerializer(Method valueMethod, JsonSerializer<Object> ser)
    {
        super(Object.class);
        _accessorMethod = valueMethod;
        _valueSerializer = ser;
        _property = null;
        _forceTypeInformation = true; // gets reconsidered when we are contextualized
    }

    @SuppressWarnings("unchecked")
    public JsonValueSerializer(JsonValueSerializer src, BeanProperty property,
            JsonSerializer<?> ser, boolean forceTypeInfo)
    {
        super(Object.class);
        _accessorMethod = src._accessorMethod;
        _valueSerializer = (JsonSerializer<Object>) ser;
        _property = property;
        _forceTypeInformation = forceTypeInfo;
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
//  @Override
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
                /* 09-Dec-2010, tatu: Turns out we must add special handling for
                 *   cases where "native" (aka "natural") type is being serialized,
                 *   using standard serializer
                 */
                ser = provider.findTypedValueSerializer(t, false, _property);
                boolean forceTypeInformation = isNaturalTypeWithStdHandling(t.getRawClass(), ser);
                return withResolved(property, ser, forceTypeInformation);
            }
        } else if (ser instanceof ContextualSerializer) {
            ser = ((ContextualSerializer) ser).createContextual(provider, property);
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
    public void serialize(Object bean, JsonGenerator jgen, SerializerProvider prov)
        throws IOException, JsonGenerationException
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
            TypeSerializer typeSer)
        throws IOException, JsonProcessingException
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
            if (ser != null) { // already got a serializer? fabulous, that be easy...
                /* 09-Dec-2010, tatu: To work around natural type's refusal to add type info, we do
                 *    this (note: type is for the wrapper type, not enclosed value!)
                 */
                if (_forceTypeInformation) {
                    typeSer.writeTypePrefixForScalar(bean, jgen);
                } 
                ser.serializeWithType(value, jgen, provider, typeSer);
                if (_forceTypeInformation) {
                    typeSer.writeTypeSuffixForScalar(bean, jgen);
                } 
                return;
            }
            // But if not, it gets tad trickier (copied from main serialize() method)
            Class<?> c = value.getClass();
            ser = provider.findTypedValueSerializer(c, true, _property);
            // note: now we have bundled type serializer, so should NOT call with typed version
            ser.serialize(value, jgen, provider);
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
    public JsonNode getSchema(SerializerProvider provider, Type typeHint)
        throws JsonMappingException
    {
        return (_valueSerializer instanceof SchemaAware) ?
                ((SchemaAware) _valueSerializer).getSchema(provider, null) :
                JsonSchema.getDefaultSchemaNode();
    }
    
    @Override
    public void acceptJsonFormatVisitor(JsonFormatVisitorWrapper visitor, JavaType typeHint)
    {
    	if (_valueSerializer instanceof JsonFormatVisitable) {
    		((JsonFormatVisitable) _valueSerializer).acceptJsonFormatVisitor(visitor, null); 
    	} else {
    		visitor.expectAnyFormat(typeHint);
    	}
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
    public String toString()
    {
        return "(@JsonValue serializer for method " + _accessorMethod.getDeclaringClass() + "#" + _accessorMethod.getName() + ")";
    }
}
