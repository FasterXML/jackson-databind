package tools.jackson.databind.ser.std;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonFormat;

import com.fasterxml.jackson.annotation.JsonInclude;
import tools.jackson.core.*;
import tools.jackson.core.type.WritableTypeId;
import tools.jackson.databind.*;
import tools.jackson.databind.introspect.AnnotatedMember;
import tools.jackson.databind.jsonFormatVisitors.JsonFormatVisitorWrapper;
import tools.jackson.databind.jsontype.TypeSerializer;
import tools.jackson.databind.util.ArrayBuilders;
import tools.jackson.databind.util.BeanUtil;

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

    protected final static Object MARKER_FOR_EMPTY = JsonInclude.Include.NON_EMPTY;

    /**
     * Value that indicates suppression mechanism to use for
     * content values (elements of container), if any; null
     * for no filtering.
     *
     * @since 3.1
     */
    protected final Object _suppressableValue;

    /**
     * Flag that indicates whether nulls should be suppressed.
     *
     * @since 3.1
     */
    protected final boolean _suppressNulls;

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
        this(cls, elementType, staticTyping, vts, elementSerializer, null, null, null, false);
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
        _suppressableValue = null;
        _suppressNulls = false;
    }

    @Deprecated // since 3.1
    protected AsArraySerializerBase(Class<?> cls, JavaType elementType, boolean staticTyping,
            TypeSerializer vts, ValueSerializer<?> elementSerializer,
            Boolean unwrapSingle, BeanProperty property)
    {
        this(cls, elementType, staticTyping, vts, elementSerializer, unwrapSingle, property, null, false);
    }

    /**
     * General purpose constructor. Use contextual constructors, if possible.
     *
     * @since 3.1
     */
    @SuppressWarnings("unchecked")
    protected AsArraySerializerBase(Class<?> cls, JavaType elementType, boolean staticTyping,
            TypeSerializer vts, ValueSerializer<?> elementSerializer,
            Boolean unwrapSingle, BeanProperty property,
            Object suppressableValue, boolean suppressNulls)
    {
        super(cls, property);
        _elementType = elementType;
        // static if explicitly requested, or if element type is final
        _staticTyping = staticTyping || (elementType != null && elementType.isFinal());
        _valueTypeSerializer = vts;
        _elementSerializer = (ValueSerializer<Object>) elementSerializer;
        _unwrapSingle = unwrapSingle;
        _suppressableValue = suppressableValue;
        _suppressNulls = suppressNulls;
    }

    @Deprecated // since 3.1
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
        _suppressableValue = src._suppressableValue;
        _suppressNulls = src._suppressNulls;
    }

    /**
     * @since 3.1
     */
    @SuppressWarnings("unchecked")
    protected AsArraySerializerBase(AsArraySerializerBase<?> src,
             TypeSerializer vts, ValueSerializer<?> elementSerializer,
             Boolean unwrapSingle, BeanProperty property,
             Object suppressableValue, boolean suppressNulls)
    {
        super(src, property);
        _elementType = src._elementType;
        _staticTyping = src._staticTyping;
        _valueTypeSerializer = vts;
        _elementSerializer = (ValueSerializer<Object>) elementSerializer;
        _unwrapSingle = unwrapSingle;
        _suppressableValue = suppressableValue;
        _suppressNulls = suppressNulls;
    }

    @Deprecated // since 3.1
    protected abstract AsArraySerializerBase<T> withResolved(BeanProperty property,
            TypeSerializer vts, ValueSerializer<?> elementSerializer,
            Boolean unwrapSingle);


    protected abstract AsArraySerializerBase<T> withResolved(BeanProperty property,
            TypeSerializer vts, ValueSerializer<?> elementSerializer,
            Boolean unwrapSingle, Object suppressableValue, boolean suppressNulls);

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
    public ValueSerializer<?> createContextual(SerializationContext ctxt,
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


        // Handle content inclusion (similar to MapSerializer lines 560-609)
        JsonInclude.Value inclV = findIncludeOverrides(ctxt, property, handledType());
        Object valueToSuppress = _suppressableValue;
        boolean suppressNulls = _suppressNulls;

        if (inclV != null) {
            JsonInclude.Include incl = inclV.getContentInclusion();
            if (incl != JsonInclude.Include.USE_DEFAULTS) {
                switch (incl) {
                    case NON_DEFAULT:
                        valueToSuppress = BeanUtil.getDefaultValue(_elementType);
                        suppressNulls = true;
                        if (valueToSuppress != null) {
                            if (valueToSuppress.getClass().isArray()) {
                                valueToSuppress = ArrayBuilders.getArrayComparator(valueToSuppress);
                            }
                        }
                        break;
                    case NON_ABSENT:
                        suppressNulls = true;
                        valueToSuppress = MARKER_FOR_EMPTY;
                        break;
                    case NON_EMPTY:
                        suppressNulls = true;
                        valueToSuppress = MARKER_FOR_EMPTY;
                        break;
                    case CUSTOM:
                        valueToSuppress = ctxt.includeFilterInstance(null, inclV.getContentFilter());
                        if (valueToSuppress == null) {
                            suppressNulls = true;
                        } else {
                            suppressNulls = ctxt.includeFilterSuppressNulls(valueToSuppress);
                        }
                        break;
                    case NON_NULL:
                        valueToSuppress = null;
                        suppressNulls = true;
                        break;
                    case ALWAYS:
                    default:
                        valueToSuppress = null;
                        suppressNulls = false;
                        break;
                }
            }
        }

        if ((ser != _elementSerializer)
                || (property != _property)
                || (_valueTypeSerializer != typeSer)
                || (!Objects.equals(_unwrapSingle, unwrapSingle))
                || (!Objects.equals(valueToSuppress, _suppressableValue))
                || (suppressNulls != _suppressNulls)) {
            return withResolved(property, typeSer, ser, unwrapSingle, valueToSuppress, suppressNulls);
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
    public void serialize(T value, JsonGenerator ggen, SerializationContext ctxt) throws JacksonException
    {
        if (provider.isEnabled(SerializationFeature.WRITE_SINGLE_ELEM_ARRAYS_UNWRAPPED)
                && hasSingleElement(value)) {
            serializeContents(value, g, ctxt);
            return;
        }
        gen.writeStartArray(value);
        serializeContents(value, g, ctxt);
        gen.writeEndArray();
    }
    */

    @Override
    public void serializeWithType(T value, JsonGenerator g, SerializationContext ctxt,
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

    protected abstract void serializeContents(T value, JsonGenerator g,
            SerializationContext ctxt)
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
                valueSer = visitor.getContext().findContentValueSerializer(_elementType, _property);
            }
        }
        visitArrayFormat(visitor, typeHint, valueSer, _elementType);
    }

    /**
     * Common utility method for checking if this serializer needs to consider
     * filtering of its elements.
     * Returns {@code true} if filtering needs to be checked,
     * {@code false} if not.
     *
     * @since 3.1
     */
    protected boolean _needToCheckFiltering(SerializationContext ctxt) {
        return ((_suppressableValue != null) || _suppressNulls)
                && ctxt.isEnabled(SerializationFeature.APPLY_JSON_INCLUDE_FOR_COLLECTIONS);
    }
    
    /**
     * Common utility method for checking if an element should be filtered/suppressed
     * based on @JsonInclude settings. Returns {@code true} if element should be serialized,
     * {@code false} if it should be skipped.
     *
     * @param ctxt Serialization context
     * @param elem Element to check for suppression
     * @param serializer Serializer for the element (may be null for strings)
     * @return true if element should be serialized, false if suppressed
     *
     * @since 3.1
     */
    protected boolean _shouldSerializeElement(SerializationContext ctxt,
            Object elem, ValueSerializer<Object> serializer)
    {
        if (_suppressableValue == null) {
            return true;
        }
        if (_suppressableValue == MARKER_FOR_EMPTY) {
            if (serializer != null) {
                return !serializer.isEmpty(ctxt, elem);
            }
            // For strings and primitives, check emptiness directly
            if (elem instanceof String str) {
                return !str.isEmpty();
            }
            return true;
        }
        return !_suppressableValue.equals(elem);
    }
}
