package com.fasterxml.jackson.databind.ser.std;

import java.io.IOException;
import java.lang.reflect.Type;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.introspect.AnnotatedMember;
import com.fasterxml.jackson.databind.jsonFormatVisitors.JsonArrayFormatVisitor;
import com.fasterxml.jackson.databind.jsonFormatVisitors.JsonFormatVisitorWrapper;
import com.fasterxml.jackson.databind.jsonschema.SchemaAware;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.ser.ContainerSerializer;
import com.fasterxml.jackson.databind.ser.ContextualSerializer;
import com.fasterxml.jackson.databind.ser.impl.PropertySerializerMap;

/**
 * Base class for serializers that will output contents as JSON
 * arrays; typically serializers used for {@link java.util.Collection}
 * and array types.
 */
public abstract class AsArraySerializerBase<T>
    extends ContainerSerializer<T>
    implements ContextualSerializer
{
    protected final boolean _staticTyping;

    protected final JavaType _elementType;

    /**
     * Type serializer used for values, if any.
     */
    protected final TypeSerializer _valueTypeSerializer;

    /**
     * Value serializer to use, if it can be statically determined
     */
    protected final JsonSerializer<Object> _elementSerializer;

    /**
     * Collection-valued property being serialized with this instance
     */
    protected final BeanProperty _property;

    /**
     * If element type can not be statically determined, mapping from
     * runtime type to serializer is handled using this object
     */
    protected PropertySerializerMap _dynamicSerializers;
    
    /*
    /**********************************************************
    /* Life-cycle
    /**********************************************************
     */
    
    protected AsArraySerializerBase(Class<?> cls, JavaType et, boolean staticTyping,
            TypeSerializer vts, BeanProperty property, JsonSerializer<Object> elementSerializer)
    {
        // typing with generics is messy... have to resort to this:
        super(cls, false);
        _elementType = et;
        // static if explicitly requested, or if element type is final
        _staticTyping = staticTyping || (et != null && et.isFinal());
        _valueTypeSerializer = vts;
        _property = property;
        _elementSerializer = elementSerializer;
        _dynamicSerializers = PropertySerializerMap.emptyMap();
    }

    @SuppressWarnings("unchecked")
    protected AsArraySerializerBase(AsArraySerializerBase<?> src,
            BeanProperty property, TypeSerializer vts, JsonSerializer<?> elementSerializer)
    {
        super(src);
        _elementType = src._elementType;
        _staticTyping = src._staticTyping;
        _valueTypeSerializer = vts;
        _property = property;
        _elementSerializer = (JsonSerializer<Object>) elementSerializer;
        _dynamicSerializers = src._dynamicSerializers;
    }
    
    public abstract AsArraySerializerBase<T> withResolved(BeanProperty property,
            TypeSerializer vts, JsonSerializer<?> elementSerializer);

    /*
    /**********************************************************
    /* Post-processing
    /**********************************************************
     */
    
    /**
     * This method is needed to resolve contextual annotations like
     * per-property overrides, as well as do recursive call
     * to <code>createContextual</code> of content serializer, if
     * known statically.
     */
    @Override
    public JsonSerializer<?> createContextual(SerializerProvider provider,
            BeanProperty property)
        throws JsonMappingException
    {
        TypeSerializer typeSer = _valueTypeSerializer;
        if (typeSer != null) {
            typeSer = typeSer.forProperty(property);
        }
        /* 29-Sep-2012, tatu: Actually, we need to do much more contextual
         *    checking here since we finally know for sure the property,
         *    and it may have overrides
         */
        JsonSerializer<?> ser = null;
        // First: if we have a property, may have property-annotation overrides
        if (property != null) {
            AnnotatedMember m = property.getMember();
            if (m != null) {
                Object serDef = provider.getAnnotationIntrospector().findContentSerializer(m);
                if (serDef != null) {
                    ser = provider.serializerInstance(m, serDef);
                }
            }
        }
        if (ser == null) {
            ser = _elementSerializer;
        }
        // 18-Feb-2013, tatu: May have a content converter:
        ser = findConvertingContentSerializer(provider, property, ser);
        if (ser == null) {
            // 30-Sep-2012, tatu: One more thing -- if explicit content type is annotated,
            //   we can consider it a static case as well.
            if (_elementType != null) {
                // 20-Aug-2013, tatu: Need to avoid trying to access serializer for java.lang.Object tho
                if ((_staticTyping && _elementType.getRawClass() != Object.class)
                        || hasContentTypeAnnotation(provider, property)) {
                    ser = provider.findValueSerializer(_elementType, property);
                }
            }
        } else {
            ser = provider.handleSecondaryContextualization(ser, property);
        }
        if ((ser != _elementSerializer) || (property != _property) || _valueTypeSerializer != typeSer) {
            return withResolved(property, typeSer, ser);
        }
        return this;
    }
    
    /*
    /**********************************************************
    /* Accessors
    /**********************************************************
     */
    
    @Override
    public JavaType getContentType() {
        return _elementType;
    }

    @Override
    public JsonSerializer<?> getContentSerializer() {
        return _elementSerializer;
    }

    /*
    /**********************************************************
    /* Serialization
    /**********************************************************
     */
    
    @Override
    public final void serialize(T value, JsonGenerator jgen, SerializerProvider provider)
        throws IOException, JsonGenerationException
    {
        // [JACKSON-805]
        if (provider.isEnabled(SerializationFeature.WRITE_SINGLE_ELEM_ARRAYS_UNWRAPPED)
                && hasSingleElement(value)) {
            serializeContents(value, jgen, provider);
            return;
        }
        jgen.writeStartArray();
        serializeContents(value, jgen, provider);
        jgen.writeEndArray();
    }

    // Note: was 'final' modifier in 2.2 and before; no real need to be, removed
    @Override
    public void serializeWithType(T value, JsonGenerator jgen, SerializerProvider provider,
            TypeSerializer typeSer)
        throws IOException, JsonGenerationException
    {
        // note: let's NOT consider [JACKSON-805] here; gets too complicated, and probably just won't work
        typeSer.writeTypePrefixForArray(value, jgen);
        serializeContents(value, jgen, provider);
        typeSer.writeTypeSuffixForArray(value, jgen);
    }

    protected abstract void serializeContents(T value, JsonGenerator jgen, SerializerProvider provider)
        throws IOException, JsonGenerationException;

    @SuppressWarnings("deprecation")
    @Override
    public JsonNode getSchema(SerializerProvider provider, Type typeHint)
        throws JsonMappingException
    {
        ObjectNode o = createSchemaNode("array", true);
        JavaType contentType = _elementType;
        if (contentType != null) {
            JsonNode schemaNode = null;
            // 15-Oct-2010, tatu: We can't serialize plain Object.class; but what should it produce here? Untyped?
            if (contentType.getRawClass() != Object.class) {
                JsonSerializer<Object> ser = provider.findValueSerializer(contentType, _property);
                if (ser instanceof SchemaAware) {
                    schemaNode = ((SchemaAware) ser).getSchema(provider, null);
                }
            }
            if (schemaNode == null) {
                schemaNode = com.fasterxml.jackson.databind.jsonschema.JsonSchema.getDefaultSchemaNode();
            }
            o.put("items", schemaNode);
        }
        return o;
    }
    
    @Override
    public void acceptJsonFormatVisitor(JsonFormatVisitorWrapper visitor, JavaType typeHint)
        throws JsonMappingException
    {
        JsonArrayFormatVisitor arrayVisitor = (visitor == null) ? null : visitor.expectArrayFormat(typeHint);
        if (arrayVisitor != null) {
            /* 01-Sep-2014, tatu: Earlier was trying to make use of 'typeHint' for some
             *   reason, causing NPE (as per https://github.com/FasterXML/jackson-module-jsonSchema/issues/34)
             *   if coupled with `@JsonValue`. But I can't see much benefit of trying to rely
             *   on TypeHint here so code is simplified like so:
             */
            JsonSerializer<?> valueSer = _elementSerializer;
            if (valueSer == null) {
                valueSer = visitor.getProvider().findValueSerializer(_elementType, _property);
            }
            arrayVisitor.itemsFormat(valueSer, _elementType);
        }
    }

    protected final JsonSerializer<Object> _findAndAddDynamic(PropertySerializerMap map,
            Class<?> type, SerializerProvider provider) throws JsonMappingException
    {
        PropertySerializerMap.SerializerAndMapResult result = map.findAndAddSecondarySerializer(type, provider, _property);
        // did we get a new map of serializers? If so, start using it
        if (map != result.map) {
            _dynamicSerializers = result.map;
        }
        return result.serializer;
    }

    protected final JsonSerializer<Object> _findAndAddDynamic(PropertySerializerMap map,
            JavaType type, SerializerProvider provider) throws JsonMappingException
    {
        PropertySerializerMap.SerializerAndMapResult result = map.findAndAddSecondarySerializer(type, provider, _property);
        if (map != result.map) {
            _dynamicSerializers = result.map;
        }
        return result.serializer;
    }
}
