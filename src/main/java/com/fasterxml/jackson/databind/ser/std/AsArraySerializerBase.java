package com.fasterxml.jackson.databind.ser.std;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonFormat;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.core.type.WritableTypeId;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.introspect.AnnotatedMember;
import com.fasterxml.jackson.databind.jsonFormatVisitors.JsonFormatVisitorWrapper;
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
@SuppressWarnings("serial")
public abstract class AsArraySerializerBase<T>
    extends ContainerSerializer<T>
    implements ContextualSerializer
{
    protected final JavaType _elementType;

    /**
     * Collection-valued property being serialized with this instance
     */
    protected final BeanProperty _property;

    protected final boolean _staticTyping;

    /**
     * Setting for specific local override for "unwrap single element arrays":
     * true for enable unwrapping, false for preventing it, `null` for using
     * global configuration.
     *
     * @since 2.6
     */
    protected final Boolean _unwrapSingle;

    /**
     * Type serializer used for values, if any.
     */
    protected final TypeSerializer _valueTypeSerializer;

    /**
     * Value serializer to use, if it can be statically determined
     */
    protected final JsonSerializer<Object> _elementSerializer;

    /**
     * If element type cannot be statically determined, mapping from
     * runtime type to serializer is handled using this object
     */
    protected PropertySerializerMap _dynamicSerializers;

    /*
    /**********************************************************
    /* Life-cycle
    /**********************************************************
     */

    /**
     * Non-contextual, "blueprint" constructor typically called when the first
     * instance is created, without knowledge of property it was used via.
     *
     * @since 2.6
     */
    protected AsArraySerializerBase(Class<?> cls, JavaType et, boolean staticTyping,
            TypeSerializer vts, JsonSerializer<Object> elementSerializer)
    {
        this(cls, et, staticTyping, vts, null, elementSerializer, null);
    }

    /**
     * @deprecated Since 2.6 Use variants that either take 'src', or do NOT pass
     *    BeanProperty
     */
    @Deprecated
    protected AsArraySerializerBase(Class<?> cls, JavaType et, boolean staticTyping,
            TypeSerializer vts, BeanProperty property, JsonSerializer<Object> elementSerializer)
    {
        this(cls, et, staticTyping, vts, property, elementSerializer, null);
    }

    /**
     * General purpose constructor. Use contextual constructors, if possible.
     *
     * @since 2.12
     */
    @SuppressWarnings("unchecked")
    protected AsArraySerializerBase(Class<?> cls, JavaType elementType, boolean staticTyping,
            TypeSerializer vts, BeanProperty property, JsonSerializer<?> elementSerializer,
            Boolean unwrapSingle)
    {
        // typing with generics is messy... have to resort to this:
        super(cls, false);
        _elementType = elementType;
        // static if explicitly requested, or if element type is final
        _staticTyping = staticTyping || (elementType != null && elementType.isFinal());
        _valueTypeSerializer = vts;
        _property = property;
        _elementSerializer = (JsonSerializer<Object>) elementSerializer;
        _dynamicSerializers = PropertySerializerMap.emptyForProperties();
        _unwrapSingle = unwrapSingle;
    }

    @SuppressWarnings("unchecked")
    protected AsArraySerializerBase(AsArraySerializerBase<?> src,
            BeanProperty property, TypeSerializer vts, JsonSerializer<?> elementSerializer,
            Boolean unwrapSingle)
    {
        super(src);
        _elementType = src._elementType;
        _staticTyping = src._staticTyping;
        _valueTypeSerializer = vts;
        _property = property;
        _elementSerializer = (JsonSerializer<Object>) elementSerializer;
        // [databind#2181]: may not be safe to reuse, start from empty
        _dynamicSerializers = PropertySerializerMap.emptyForProperties();
        _unwrapSingle = unwrapSingle;
    }

    /**
     * @deprecated since 2.6: use the overloaded method that takes 'unwrapSingle'
     */
    @Deprecated
    protected AsArraySerializerBase(AsArraySerializerBase<?> src,
            BeanProperty property, TypeSerializer vts, JsonSerializer<?> elementSerializer)
    {
        this(src, property, vts, elementSerializer, src._unwrapSingle);
    }

    /**
     * @deprecated since 2.6: use the overloaded method that takes 'unwrapSingle'
     */
    @Deprecated
    public final AsArraySerializerBase<T> withResolved(BeanProperty property,
            TypeSerializer vts, JsonSerializer<?> elementSerializer) {
        return withResolved(property, vts, elementSerializer, _unwrapSingle);
    }

    /**
     * @since 2.6
     */
    public abstract AsArraySerializerBase<T> withResolved(BeanProperty property,
            TypeSerializer vts, JsonSerializer<?> elementSerializer,
            Boolean unwrapSingle);

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
    public JsonSerializer<?> createContextual(SerializerProvider serializers,
            BeanProperty property)
        throws JsonMappingException
    {
        TypeSerializer typeSer = _valueTypeSerializer;
        if (typeSer != null) {
            typeSer = typeSer.forProperty(property);
        }
        JsonSerializer<?> ser = null;
        Boolean unwrapSingle = null;
        // First: if we have a property, may have property-annotation overrides

        if (property != null) {
            final AnnotationIntrospector intr = serializers.getAnnotationIntrospector();
            AnnotatedMember m = property.getMember();
            if (m != null) {
                Object serDef = intr.findContentSerializer(m);
                if (serDef != null) {
                    ser = serializers.serializerInstance(m, serDef);
                }
            }
        }
        JsonFormat.Value format = findFormatOverrides(serializers, property, handledType());
        if (format != null) {
            unwrapSingle = format.getFeature(JsonFormat.Feature.WRITE_SINGLE_ELEM_ARRAYS_UNWRAPPED);
        }
        if (ser == null) {
            ser = _elementSerializer;
        }
        // 18-Feb-2013, tatu: May have a content converter:
        ser = findContextualConvertingSerializer(serializers, property, ser);
        if (ser == null) {
            // 30-Sep-2012, tatu: One more thing -- if explicit content type is annotated,
            //   we can consider it a static case as well.
            if (_elementType != null) {
                if (_staticTyping && !_elementType.isJavaLangObject()) {
                    ser = serializers.findContentValueSerializer(_elementType, property);
                }
            }
        }
        if ((ser != _elementSerializer)
                || (property != _property)
                || (_valueTypeSerializer != typeSer)
                || (!Objects.equals(_unwrapSingle, unwrapSingle))) {
            return withResolved(property, typeSer, ser, unwrapSingle);
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

    // NOTE: as of 2.5, sub-classes SHOULD override (in 2.4 and before, was final),
    // at least if they can provide access to actual size of value and use `writeStartArray()`
    // variant that passes size of array to output, which is helpful with some data formats
    @Override
    public void serialize(T value, JsonGenerator gen, SerializerProvider provider) throws IOException
    {
        if (provider.isEnabled(SerializationFeature.WRITE_SINGLE_ELEM_ARRAYS_UNWRAPPED)
                && hasSingleElement(value)) {
            serializeContents(value, gen, provider);
            return;
        }
        gen.writeStartArray(value);
        serializeContents(value, gen, provider);
        gen.writeEndArray();
    }

    @Override
    public void serializeWithType(T value, JsonGenerator g, SerializerProvider provider,
            TypeSerializer typeSer) throws IOException
    {
        WritableTypeId typeIdDef = typeSer.writeTypePrefix(g,
                typeSer.typeId(value, JsonToken.START_ARRAY));
        // [databind#631]: Assign current value, to be accessible by custom serializers
        g.setCurrentValue(value);
        serializeContents(value, g, provider);
        typeSer.writeTypeSuffix(g, typeIdDef);
    }

    protected abstract void serializeContents(T value, JsonGenerator gen, SerializerProvider provider)
        throws IOException;

    /**
     * @deprecated Since 2.15
     */
    @Deprecated
    @Override
    public JsonNode getSchema(SerializerProvider provider, Type typeHint)
        throws JsonMappingException
    {
        ObjectNode o = createSchemaNode("array", true);
        if (_elementSerializer != null) {
            JsonNode schemaNode = null;
            if (_elementSerializer instanceof com.fasterxml.jackson.databind.jsonschema.SchemaAware) {
                schemaNode = ((com.fasterxml.jackson.databind.jsonschema.SchemaAware) _elementSerializer)
                    .getSchema(provider, null);
            }
            if (schemaNode == null) {
                schemaNode = com.fasterxml.jackson.databind.jsonschema.JsonSchema.getDefaultSchemaNode();
            }
            o.set("items", schemaNode);
        }
        return o;
    }

    @Override
    public void acceptJsonFormatVisitor(JsonFormatVisitorWrapper visitor, JavaType typeHint)
        throws JsonMappingException
    {
        JsonSerializer<?> valueSer = _elementSerializer;
        if (valueSer == null) {
            // 19-Oct-2016, tatu: Apparently we get null for untyped/raw `EnumSet`s... not 100%
            //   sure what'd be the clean way but let's try this for now:
            if (_elementType != null) {
                valueSer = visitor.getProvider().findContentValueSerializer(_elementType, _property);
            }
        }
        visitArrayFormat(visitor, typeHint, valueSer, _elementType);
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
