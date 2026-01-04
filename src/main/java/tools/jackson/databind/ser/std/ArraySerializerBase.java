package tools.jackson.databind.ser.std;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;

import tools.jackson.core.*;
import tools.jackson.core.type.WritableTypeId;
import tools.jackson.databind.*;
import tools.jackson.databind.jsontype.TypeSerializer;
import tools.jackson.databind.util.ArrayBuilders;
import tools.jackson.databind.util.BeanUtil;

/**
 * Intermediate base class for serializers used for various Java arrays.
 *
 * @param <T> Type of arrays serializer handles
 */
public abstract class ArraySerializerBase<T>
    extends StdContainerSerializer<T>
{
    protected final static Object MARKER_FOR_EMPTY = JsonInclude.Include.NON_EMPTY;

    /**
     * Setting for specific local override for "unwrap single element arrays":
     * true for enable unwrapping, false for preventing it, `null` for using
     * global configuration.
     */
    protected final Boolean _unwrapSingle;

    /**
     * Value that indicates suppression mechanism to use for
     * content values (elements of array), if any; null
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

    protected ArraySerializerBase(Class<T> cls)
    {
        super(cls);
        _unwrapSingle = null;
        _suppressableValue = null;
        _suppressNulls = false;
    }

    protected ArraySerializerBase(ArraySerializerBase<?> src) {
        super(src);
        _unwrapSingle = src._unwrapSingle;
        _suppressableValue = src._suppressableValue;
        _suppressNulls = src._suppressNulls;
    }

    /**
     * @since 3.1
     */
    protected ArraySerializerBase(ArraySerializerBase<?> src, BeanProperty property,
            Boolean unwrapSingle, Object suppressableValue, boolean suppressNulls)
    {
        super(src, property);
        _unwrapSingle = unwrapSingle;
        _suppressableValue = suppressableValue;
        _suppressNulls = suppressNulls;
    }

    /**
     * Factory method to use for creating differently configured instances with
     * content inclusion settings, called by this class from {@link #createContextual}.
     *
     * @since 3.1
     */
    protected abstract ArraySerializerBase<T> _withResolved(BeanProperty prop,
            Boolean unwrapSingle, Object suppressableValue, boolean suppressNulls);

    @Override
    public ValueSerializer<?> createContextual(SerializationContext serializers,
            BeanProperty property)
    {
        Boolean unwrapSingle = null;

        // First: if we have a property, may have property-annotation overrides
        if (property != null) {
            JsonFormat.Value format = findFormatOverrides(serializers, property, handledType());
            if (format != null) {
                unwrapSingle = format.getFeature(JsonFormat.Feature.WRITE_SINGLE_ELEM_ARRAYS_UNWRAPPED);
            }
        }

        // [databind#5515]: Handle content inclusion for arrays
        JsonInclude.Value inclV = findIncludeOverrides(serializers, property, handledType());
        Object valueToSuppress = _suppressableValue;
        boolean suppressNulls = _suppressNulls;

        if (inclV != null) {
            JsonInclude.Include incl = inclV.getContentInclusion();
            if (incl != JsonInclude.Include.USE_DEFAULTS) {
                switch (incl) {
                    case NON_DEFAULT:
                        valueToSuppress = BeanUtil.getDefaultValue(getContentType());
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
                        valueToSuppress = serializers.includeFilterInstance(null, inclV.getContentFilter());
                        if (valueToSuppress == null) {
                            suppressNulls = true;
                        } else {
                            suppressNulls = serializers.includeFilterSuppressNulls(valueToSuppress);
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

        if (!Objects.equals(unwrapSingle, _unwrapSingle)
                || !Objects.equals(valueToSuppress, _suppressableValue)
                || (suppressNulls != _suppressNulls)) {
            return _withResolved(property, unwrapSingle, valueToSuppress, suppressNulls);
        }
        return this;
    }

    @Override
    public final void serializeWithType(T value, JsonGenerator g, SerializationContext ctxt,
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

    protected abstract void serializeContents(T value, JsonGenerator jgen, SerializationContext provider)
        throws JacksonException;

    protected final boolean _shouldUnwrapSingle(SerializationContext provider) {
        if (_unwrapSingle == null) {
            return provider.isEnabled(SerializationFeature.WRITE_SINGLE_ELEM_ARRAYS_UNWRAPPED);
        }
        return _unwrapSingle.booleanValue();
    }

    /*
    /**********************************************************************
    /* Helper methods for content filtering
    /**********************************************************************
     */

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
                && ctxt.isEnabled(SerializationFeature.APPLY_JSON_INCLUDE_FOR_CONTAINERS);
    }

    /**
     * Common utility method for checking if an element should be filtered/suppressed
     * based on @JsonInclude settings. Returns {@code true} if element should be serialized,
     * {@code false} if it should be skipped.
     *
     * @param ctxt Serialization context
     * @param elem Element to check for suppression (boxed primitive)
     * @return true if element should be serialized, false if suppressed
     *
     * @since 3.1
     */
    protected boolean _shouldSerializeElement(SerializationContext ctxt, Object elem)
    {
        if (_suppressableValue == null) {
            return true;
        }
        if (_suppressableValue == MARKER_FOR_EMPTY) {
            // For primitives, no concept of "empty"
            return true;
        }
        return !_suppressableValue.equals(elem);
    }
}
