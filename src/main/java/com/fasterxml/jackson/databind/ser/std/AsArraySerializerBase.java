package com.fasterxml.jackson.databind.ser.std;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonFormat;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.core.type.WritableTypeId;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.introspect.AnnotatedMember;
import com.fasterxml.jackson.databind.jsonFormatVisitors.JsonFormatVisitorWrapper;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;

/**
 * Base class for serializers that will output contents as JSON
 * arrays; typically serializers used for {@link java.util.Collection}
 * and array types.
 */
public abstract class AsArraySerializerBase<T>
    extends StdContainerSerializer<T>
{
    protected final JavaType _elementType;

    protected final boolean _staticTyping;

    /**
     * Setting for specific local override for "unwrap single element arrays":
     * true for enable unwrapping, false for preventing it, `null` for using
     * global configuration.
     */
    protected final Boolean _unwrapSingle;

    /**
     * Type serializer used for values, if any.
     */
    protected final TypeSerializer _valueTypeSerializer;

    /**
     * Value serializer to use, if it can be statically determined
     */
    protected final ValueSerializer<Object> _elementSerializer;

    /*
    /**********************************************************************
    /* Life-cycle
    /**********************************************************************
     */

    /**
     * Non-contextual, "blueprint" constructor typically called when the first
     * instance is created, without knowledge of property it was used via.
     */
    protected AsArraySerializerBase(Class<?> cls, JavaType elementType, boolean staticTyping,
            TypeSerializer vts, ValueSerializer<?> elementSerializer)
    {
        this(cls, elementType, staticTyping, vts, elementSerializer, null);
    }

    /**
     * General purpose constructor. Use contextual constructors, if possible.
     */
    @SuppressWarnings("unchecked")
    protected AsArraySerializerBase(Class<?> cls, JavaType elementType, boolean staticTyping,
            TypeSerializer vts, ValueSerializer<?> elementSerializer,
            Boolean unwrapSingle)
    {
        super(cls);
        _elementType = elementType;
        // static if explicitly requested, or if element type is final
        _staticTyping = staticTyping || (elementType != null && elementType.isFinal());
        _valueTypeSerializer = vts;
        _elementSerializer = (ValueSerializer<Object>) elementSerializer;
        _unwrapSingle = unwrapSingle;
    }

    /**
     * General purpose constructor. Use contextual constructors, if possible.
     */
    @SuppressWarnings("unchecked")
    protected AsArraySerializerBase(Class<?> cls, JavaType elementType, boolean staticTyping,
            TypeSerializer vts, ValueSerializer<?> elementSerializer,
            Boolean unwrapSingle, BeanProperty property)
    {
        super(cls, property);
        _elementType = elementType;
        // static if explicitly requested, or if element type is final
        _staticTyping = staticTyping || (elementType != null && elementType.isFinal());
        _valueTypeSerializer = vts;
        _elementSerializer = (ValueSerializer<Object>) elementSerializer;
        _unwrapSingle = unwrapSingle;
    }

    @SuppressWarnings("unchecked")
    protected AsArraySerializerBase(AsArraySerializerBase<?> src,
            TypeSerializer vts, ValueSerializer<?> elementSerializer,
            Boolean unwrapSingle, BeanProperty property)
    {
        super(src, property);
        _elementType = src._elementType;
        _staticTyping = src._staticTyping;
        _valueTypeSerializer = vts;
        _elementSerializer = (ValueSerializer<Object>) elementSerializer;
        _unwrapSingle = unwrapSingle;
    }

    public abstract AsArraySerializerBase<T> withResolved(BeanProperty property,
            TypeSerializer vts, ValueSerializer<?> elementSerializer,
            Boolean unwrapSingle);

    /*
    /**********************************************************************
    /* Post-processing
    /**********************************************************************
     */
    
    /**
     * This method is needed to resolve contextual annotations like
     * per-property overrides, as well as do recursive call
     * to <code>createContextual</code> of content serializer, if
     * known statically.
     */
    @Override
    public ValueSerializer<?> createContextual(SerializerProvider ctxt,
            BeanProperty property)
    {
        TypeSerializer typeSer = _valueTypeSerializer;
        if (typeSer != null) {
            typeSer = typeSer.forProperty(ctxt, property);
        }
        ValueSerializer<?> ser = null;
        Boolean unwrapSingle = null;
        // First: if we have a property, may have property-annotation overrides
        
        if (property != null) {
            final AnnotationIntrospector intr = ctxt.getAnnotationIntrospector();
            AnnotatedMember m = property.getMember();
            if (m != null) {
                ser = ctxt.serializerInstance(m,
                        intr.findContentSerializer(ctxt.getConfig(), m));
            }
        }
        JsonFormat.Value format = findFormatOverrides(ctxt, property, handledType());
        if (format != null) {
            unwrapSingle = format.getFeature(JsonFormat.Feature.WRITE_SINGLE_ELEM_ARRAYS_UNWRAPPED);
        }
        if (ser == null) {
            ser = _elementSerializer;
        }
        // 18-Feb-2013, tatu: May have a content converter:
        ser = findContextualConvertingSerializer(ctxt, property, ser);
        if (ser == null) {
            // 30-Sep-2012, tatu: One more thing -- if explicit content type is annotated,
            //   we can consider it a static case as well.
            if (_elementType != null) {
                if (_staticTyping && !_elementType.isJavaLangObject()) {
                    ser = ctxt.findContentValueSerializer(_elementType, property);
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
    /**********************************************************************
    /* Accessors
    /**********************************************************************
     */
    
    @Override
    public JavaType getContentType() {
        return _elementType;
    }

    @Override
    public ValueSerializer<?> getContentSerializer() {
        return _elementSerializer;
    }

    /*
    /**********************************************************************
    /* Serialization
    /**********************************************************************
     */

    // 16-Apr-2018, tatu: Sample code, but sub-classes need to implement (for more
    //    efficient "is-single-unwrapped" check)
    
    // at least if they can provide access to actual size of value and use `writeStartArray()`
    // variant that passes size of array to output, which is helpful with some data formats
    /*
    @Override
    public void serialize(T value, JsonGenerator gen, SerializerProvider ctxt) throws JacksonException
    {
        if (provider.isEnabled(SerializationFeature.WRITE_SINGLE_ELEM_ARRAYS_UNWRAPPED)
                && hasSingleElement(value)) {
            serializeContents(value, gen, ctxt);
            return;
        }
        gen.writeStartArray(value);
        serializeContents(value, gen, ctxt);
        gen.writeEndArray();
    }
    */

    @Override
    public void serializeWithType(T value, JsonGenerator g, SerializerProvider ctxt,
            TypeSerializer typeSer)
        throws JacksonException
    {
        WritableTypeId typeIdDef = typeSer.writeTypePrefix(g, ctxt,
                typeSer.typeId(value, JsonToken.START_ARRAY));
        // [databind#631]: Assign current value, to be accessible by custom serializers
        g.assignCurrentValue(value);
        serializeContents(value, g, ctxt);
        typeSer.writeTypeSuffix(g, ctxt, typeIdDef);
    }

    protected abstract void serializeContents(T value, JsonGenerator gen, SerializerProvider provider)
        throws JacksonException;

    @Override
    public void acceptJsonFormatVisitor(JsonFormatVisitorWrapper visitor, JavaType typeHint)
        throws JacksonException
    {
        ValueSerializer<?> valueSer = _elementSerializer;
        if (valueSer == null) {
            // 19-Oct-2016, tatu: Apparently we get null for untyped/raw `EnumSet`s... not 100%
            //   sure what'd be the clean way but let's try this for now:
            if (_elementType != null) {
                valueSer = visitor.getProvider().findContentValueSerializer(_elementType, _property);
            }
        }
        visitArrayFormat(visitor, typeHint, valueSer, _elementType);
    }
}
