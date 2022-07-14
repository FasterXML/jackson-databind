package com.fasterxml.jackson.databind.ser.jdk;

import java.util.*;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;

import tools.jackson.core.*;
import tools.jackson.core.type.WritableTypeId;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.annotation.JacksonStdImpl;
import com.fasterxml.jackson.databind.introspect.AnnotatedMember;
import com.fasterxml.jackson.databind.jsonFormatVisitors.JsonFormatVisitorWrapper;
import com.fasterxml.jackson.databind.jsonFormatVisitors.JsonMapFormatVisitor;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import com.fasterxml.jackson.databind.ser.PropertyFilter;
import com.fasterxml.jackson.databind.ser.std.StdContainerSerializer;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.fasterxml.jackson.databind.util.ArrayBuilders;
import com.fasterxml.jackson.databind.util.BeanUtil;
import com.fasterxml.jackson.databind.util.ClassUtil;
import com.fasterxml.jackson.databind.util.IgnorePropertiesUtil;

/**
 * Standard serializer implementation for serializing {link java.util.Map} types.
 *<p>
 * Note: about the only configurable setting currently is ability to filter out
 * entries with specified names.
 */
@JacksonStdImpl
public class MapSerializer
    extends StdContainerSerializer<Map<?,?>>
{
    protected final static JavaType UNSPECIFIED_TYPE = TypeFactory.unknownType();

    public final static Object MARKER_FOR_EMPTY = JsonInclude.Include.NON_EMPTY;

    /*
    /**********************************************************************
    /* Basic information about referring property, type
    /**********************************************************************
     */

    /**
     * Whether static types should be used for serialization of values
     * or not (if not, dynamic runtime type is used)
     */
    protected final boolean _valueTypeIsStatic;

    /**
     * Declared type of keys
     */
    protected final JavaType _keyType;

    /**
     * Declared type of contained values
     */
    protected final JavaType _valueType;

    /*
    /**********************************************************************
    /* Serializers used
    /**********************************************************************
     */
    
    /**
     * Key serializer to use, if it can be statically determined
     */
    protected ValueSerializer<Object> _keySerializer;

    /**
     * Value serializer to use, if it can be statically determined
     */
    protected ValueSerializer<Object> _valueSerializer;

    /**
     * Type identifier serializer used for values, if any.
     */
    protected final TypeSerializer _valueTypeSerializer;

    /*
    /**********************************************************************
    /* Config settings, filtering
    /**********************************************************************
     */

    /**
     * Set of entries to omit during serialization, if any
     */
    protected final Set<String> _ignoredEntries;

    /**
     * Set of entries to include during serialization, if null, it is ignored, empty will include nothing.
     *
     * @since 2.12
     */
    protected final Set<String> _includedEntries;

    /**
     * Id of the property filter to use, if any; null if none.
     */
    protected final Object _filterId;

    /**
     * Value that indicates suppression mechanism to use for <b>values contained</b>;
     * either "filter" (of which <code>equals()</code> is called), or marker
     * value of {@link #MARKER_FOR_EMPTY}, or null to indicate no filtering for
     * non-null values.
     * Note that inclusion value for Map instance itself is handled by caller (POJO
     * property that refers to the Map value).
     */
    protected final Object _suppressableValue;

    /**
     * Flag that indicates what to do with `null` values, distinct from
     * handling of {@link #_suppressableValue}
     */
    protected final boolean _suppressNulls;

    /**
     * Helper object used for name-based filtering
     *
     * @since 2.12
     */
    protected final IgnorePropertiesUtil.Checker _inclusionChecker;

    /*
    /**********************************************************************
    /* Config settings, other
    /**********************************************************************
     */

    /**
     * Flag set if output is forced to be sorted by keys (usually due
     * to annotation).
     */
    protected final boolean _sortKeys;

    /*
    /**********************************************************************
    /* Life-cycle
    /**********************************************************************
     */

    @SuppressWarnings("unchecked")
    protected MapSerializer(Set<String> ignoredEntries, Set<String> includedEntries,
            JavaType keyType, JavaType valueType, boolean valueTypeIsStatic,
            TypeSerializer vts,
            ValueSerializer<?> keySerializer, ValueSerializer<?> valueSerializer)
    {
        super(Map.class);
        _ignoredEntries = ((ignoredEntries == null) || ignoredEntries.isEmpty())
                ? null : ignoredEntries;
        _includedEntries = includedEntries;
        _keyType = keyType;
        _valueType = valueType;
        _valueTypeIsStatic = valueTypeIsStatic;
        _valueTypeSerializer = vts;
        _keySerializer = (ValueSerializer<Object>) keySerializer;
        _valueSerializer = (ValueSerializer<Object>) valueSerializer;
        _filterId = null;
        _sortKeys = false;
        _suppressableValue = null;
        _suppressNulls = false;

        _inclusionChecker = IgnorePropertiesUtil.buildCheckerIfNeeded(_ignoredEntries, _includedEntries);
    }

    @SuppressWarnings("unchecked")
    protected MapSerializer(MapSerializer src, BeanProperty property,
            ValueSerializer<?> keySerializer, ValueSerializer<?> valueSerializer,
            Set<String> ignoredEntries, Set<String> includedEntries)
    {
        super(src, property);
        _ignoredEntries = ((ignoredEntries == null) || ignoredEntries.isEmpty())
                ? null : ignoredEntries;
        _includedEntries = includedEntries;
        _keyType = src._keyType;
        _valueType = src._valueType;
        _valueTypeIsStatic = src._valueTypeIsStatic;
        _valueTypeSerializer = src._valueTypeSerializer;
        _keySerializer = (ValueSerializer<Object>) keySerializer;
        _valueSerializer = (ValueSerializer<Object>) valueSerializer;
        _filterId = src._filterId;
        _sortKeys = src._sortKeys;
        _suppressableValue = src._suppressableValue;
        _suppressNulls = src._suppressNulls;

        _inclusionChecker = IgnorePropertiesUtil.buildCheckerIfNeeded(_ignoredEntries, _includedEntries);
    }

    protected MapSerializer(MapSerializer src,
            TypeSerializer vts, Object suppressableValue, boolean suppressNulls)
    {
        super(src);
        _ignoredEntries = src._ignoredEntries;
        _includedEntries = src._includedEntries;
        _keyType = src._keyType;
        _valueType = src._valueType;
        _valueTypeIsStatic = src._valueTypeIsStatic;
        _valueTypeSerializer = vts;
        _keySerializer = src._keySerializer;
        _valueSerializer = src._valueSerializer;
        _filterId = src._filterId;
        _sortKeys = src._sortKeys;
        _suppressableValue = suppressableValue;
        _suppressNulls = suppressNulls;

        _inclusionChecker = src._inclusionChecker;
    }

    protected MapSerializer(MapSerializer src, Object filterId, boolean sortKeys)
    {
        super(src);
        _ignoredEntries = src._ignoredEntries;
        _includedEntries = src._includedEntries;
        _keyType = src._keyType;
        _valueType = src._valueType;
        _valueTypeIsStatic = src._valueTypeIsStatic;
        _valueTypeSerializer = src._valueTypeSerializer;
        _keySerializer = src._keySerializer;
        _valueSerializer = src._valueSerializer;
        _filterId = filterId;
        _sortKeys = sortKeys;
        _suppressableValue = src._suppressableValue;
        _suppressNulls = src._suppressNulls;

        _inclusionChecker = src._inclusionChecker;
    }

    @Override
    protected MapSerializer _withValueTypeSerializer(TypeSerializer vts) {
        if (_valueTypeSerializer == vts) {
            return this;
        }
        _ensureOverride("_withValueTypeSerializer");
        return new MapSerializer(this, vts, _suppressableValue, _suppressNulls);
    }

    protected MapSerializer withResolved(BeanProperty property,
             ValueSerializer<?> keySerializer, ValueSerializer<?> valueSerializer,
             Set<String> ignored, Set<String> included, boolean sortKeys)
    {
        _ensureOverride("withResolved");
        MapSerializer ser = new MapSerializer(this, property, keySerializer, valueSerializer, ignored, included);
        if (sortKeys != ser._sortKeys) {
            ser = new MapSerializer(ser, _filterId, sortKeys);
        }
        return ser;
    }

    @Override
    public MapSerializer withFilterId(Object filterId) {
        if (_filterId == filterId) {
            return this;
        }
        _ensureOverride("withFilterId");
        return new MapSerializer(this, filterId, _sortKeys);
    }

    /**
     * Mutant factory for constructing an instance with different inclusion strategy
     * for content (Map values).
     *<p>
     * NOTE: only {@code public} because it is called by {@code BasicSerializerFactory}.
     */
    public MapSerializer withContentInclusion(Object suppressableValue, boolean suppressNulls) {
        if ((suppressableValue == _suppressableValue) && (suppressNulls == _suppressNulls)) {
            return this;
        }
        _ensureOverride("withContentInclusion");
        return new MapSerializer(this, _valueTypeSerializer, suppressableValue, suppressNulls);
    }

    // @since 3.0 (different argument order in 2.x)
    public static MapSerializer construct(JavaType mapType,
            boolean staticValueType, TypeSerializer vts,
            ValueSerializer<Object> keySerializer, ValueSerializer<Object> valueSerializer,
            Object filterId,
            Set<String> ignoredEntries, Set<String> includedEntries)
    {
        JavaType keyType, valueType;

        if (mapType == null) { // for tests?
            keyType = valueType = UNSPECIFIED_TYPE;
        } else { 
            keyType = mapType.getKeyType();
            if (mapType.hasRawClass(java.util.Properties.class)) {
                // 25-Mar-2020, tatu: [databind#2657] Since non-standard Properties may actually
                //     contain non-Strings, demote value type to raw `Object`
                valueType = TypeFactory.unknownType();
            } else {
                valueType = mapType.getContentType();
            }
        }
        // If value type is final, it's same as forcing static value typing:
        if (!staticValueType) {
            staticValueType = (valueType != null && valueType.isFinal());
        } else {
            // also: Object.class cannot be handled as static, ever
            if (valueType.getRawClass() == Object.class) {
                staticValueType = false;
            }
        }
        MapSerializer ser = new MapSerializer(ignoredEntries, includedEntries, keyType, valueType, staticValueType, vts,
                keySerializer, valueSerializer);
        if (filterId != null) {
            ser = ser.withFilterId(filterId);
        }
        return ser;
    }

    protected void _ensureOverride(String method) {
        ClassUtil.verifyMustOverride(MapSerializer.class, this, method);
    }

    /*
    /**********************************************************************
    /* Post-processing (contextualization)
    /**********************************************************************
     */

    @Override
    public ValueSerializer<?> createContextual(SerializerProvider ctxt,
            BeanProperty property)
    {
        ValueSerializer<?> ser = null;
        ValueSerializer<?> keySer = null;
        final AnnotationIntrospector intr = ctxt.getAnnotationIntrospector();
        final SerializationConfig config = ctxt.getConfig();
        final AnnotatedMember propertyAcc = (property == null) ? null : property.getMember();

        // First: if we have a property, may have property-annotation overrides
        if (_neitherNull(propertyAcc, intr)) {
            keySer = ctxt.serializerInstance(propertyAcc,
                    intr.findKeySerializer(config, propertyAcc));
            ser = ctxt.serializerInstance(propertyAcc,
                    intr.findContentSerializer(config, propertyAcc));
        }
        if (ser == null) {
            ser = _valueSerializer;
        }
        // [databind#124]: May have a content converter
        ser = findContextualConvertingSerializer(ctxt, property, ser);
        if (ser == null) {
            // 30-Sep-2012, tatu: One more thing -- if explicit content type is annotated,
            //   we can consider it a static case as well.
            // 20-Aug-2013, tatu: Need to avoid trying to access serializer for java.lang.Object tho
            if (_valueTypeIsStatic && !_valueType.isJavaLangObject()) {
                ser = ctxt.findContentValueSerializer(_valueType, property);
            }
        }
        if (keySer == null) {
            keySer = _keySerializer;
        }
        if (keySer == null) {
            keySer = ctxt.findKeySerializer(_keyType, property);
        } else {
            keySer = ctxt.handleSecondaryContextualization(keySer, property);
        }
        Set<String> ignored = _ignoredEntries;
        Set<String> included = _includedEntries;
        boolean sortKeys = false;
        if (_neitherNull(propertyAcc, intr)) {
            Set<String> newIgnored = intr.findPropertyIgnoralByName(config, propertyAcc).findIgnoredForSerialization();
            if (_nonEmpty(newIgnored)) {
                ignored = (ignored == null) ? new HashSet<String>() : new HashSet<String>(ignored);
                for (String str : newIgnored) {
                    ignored.add(str);
                }
            }

            // inclusions
            Set<String> newIncluded = intr.findPropertyInclusionByName(config, propertyAcc).getIncluded();
            if (newIncluded != null) {
                included = (included == null) ? new HashSet<String>() : new HashSet<String>(included);
                for (String str : newIncluded) {
                    included.add(str);
                }
            }
            // sort key
            Boolean b = intr.findSerializationSortAlphabetically(config, propertyAcc);
            sortKeys = Boolean.TRUE.equals(b);
        }
        JsonFormat.Value format = findFormatOverrides(ctxt, property, Map.class);
        if (format != null) {
            Boolean B = format.getFeature(JsonFormat.Feature.WRITE_SORTED_MAP_ENTRIES);
            if (B != null) { // do NOT override if not defined
                sortKeys = B.booleanValue();
            }
        }
        MapSerializer mser = withResolved(property, keySer, ser, ignored, included, sortKeys);

        // [databind#307]: allow filtering
        if (propertyAcc != null) {
            Object filterId = intr.findFilterId(config, propertyAcc);
            if (filterId != null) {
                mser = mser.withFilterId(filterId);
            }
        }

        JsonInclude.Value inclV = findIncludeOverrides(ctxt, property, Map.class);
        if (inclV != null) {
            JsonInclude.Include incl = inclV.getContentInclusion();

            if (incl != JsonInclude.Include.USE_DEFAULTS) {
                Object valueToSuppress;
                boolean suppressNulls;
                switch (incl) {
                case NON_DEFAULT:
                    valueToSuppress = BeanUtil.getDefaultValue(_valueType);
                    suppressNulls = true;
                    if (valueToSuppress != null) {
                        if (valueToSuppress.getClass().isArray()) {
                            valueToSuppress = ArrayBuilders.getArrayComparator(valueToSuppress);
                        }
                    }
                    break;
                case NON_ABSENT:
                    suppressNulls = true;
                    valueToSuppress = _valueType.isReferenceType() ? MARKER_FOR_EMPTY : null;
                    break;
                case NON_EMPTY:
                    suppressNulls = true;
                    valueToSuppress = MARKER_FOR_EMPTY;
                    break;
                case CUSTOM:
                    valueToSuppress = ctxt.includeFilterInstance(null, inclV.getContentFilter());
                    if (valueToSuppress == null) { // is this legal?
                        suppressNulls = true;
                    } else {
                        suppressNulls = ctxt.includeFilterSuppressNulls(valueToSuppress);
                    }
                    break;
                case NON_NULL:
                    valueToSuppress = null;
                    suppressNulls = true;
                    break;
                case ALWAYS: // default
                default:
                    valueToSuppress = null;
                    // 30-Sep-2016, tatu: Should not need to check global flags here,
                    //   if inclusion forced to be ALWAYS
                    suppressNulls = false;
                    break;
                }
                mser = mser.withContentInclusion(valueToSuppress, suppressNulls);
            }
        }
        return mser;
    }

    /*
    /**********************************************************************
    /* Accessors
    /**********************************************************************
     */

    @Override
    public JavaType getContentType() {
        return _valueType;
    }

    @Override
    public ValueSerializer<?> getContentSerializer() {
        return _valueSerializer;
    }

    @Override
    public boolean isEmpty(SerializerProvider prov, Map<?,?> value)
    {
        if (value.isEmpty()) {
            return true;
        }
        
        // 05-Nove-2015, tatu: Simple cases are cheap, but for recursive
        //   emptiness checking we actually need to see if values are empty as well.
        Object supp = _suppressableValue;
        if ((supp == null) && !_suppressNulls) {
            return false;
        }
        ValueSerializer<Object> valueSer = _valueSerializer;
        final boolean checkEmpty = (MARKER_FOR_EMPTY == supp);
        if (valueSer != null) {
            for (Object elemValue : value.values()) {
                if (elemValue == null) {
                    if (_suppressNulls) {
                        continue;
                    }
                    return false;
                }
                if (checkEmpty) {
                    if (!valueSer.isEmpty(prov, elemValue)) {
                        return false;
                    }
                } else if ((supp == null) || !supp.equals(value)) {
                    return false;
                }
            }
            return true;
        }
        // But if not statically known, try this:
        for (Object elemValue : value.values()) {
            if (elemValue == null) {
                if (_suppressNulls) {
                    continue;
                }
                return false;
            }
            valueSer = _findSerializer(prov, elemValue);
            if (checkEmpty) {
                if (!valueSer.isEmpty(prov, elemValue)) {
                    return false;
                }
            } else if ((supp == null) || !supp.equals(value)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean hasSingleElement(Map<?,?> value) {
        return (value.size() == 1);
    }

    /*
    /**********************************************************************
    /* Extended API
    /**********************************************************************
     */

    /**
     * Accessor for currently assigned key serializer. Note that
     * this may return null during construction of <code>MapSerializer</code>:
     * depedencies are resolved during {@link #createContextual} method
     * (which can be overridden by custom implementations), but for some
     * dynamic types, it is possible that serializer is only resolved
     * during actual serialization.
     */
    public ValueSerializer<?> getKeySerializer() {
        return _keySerializer;
    }

    /*
    /**********************************************************************
    /* ValueSerializer implementation
    /**********************************************************************
     */

    @Override
    public void serialize(Map<?,?> value, JsonGenerator gen, SerializerProvider provider)
        throws JacksonException
    {
        gen.writeStartObject(value);
        serializeWithoutTypeInfo(value, gen, provider);
        gen.writeEndObject();
    }

    @Override
    public void serializeWithType(Map<?,?> value, JsonGenerator gen, SerializerProvider ctxt,
            TypeSerializer typeSer)
        throws JacksonException
    {
        // [databind#631]: Assign current value, to be accessible by custom serializers
        gen.assignCurrentValue(value);
        WritableTypeId typeIdDef = typeSer.writeTypePrefix(gen, ctxt,
                typeSer.typeId(value, JsonToken.START_OBJECT));
        serializeWithoutTypeInfo(value, gen, ctxt);
        typeSer.writeTypeSuffix(gen, ctxt, typeIdDef);
    }

    /*
    /**********************************************************************
    /* Secondary serialization methods
    /**********************************************************************
     */

    /**
     * General-purpose serialization for contents without writing object type. Will suppress, filter and
     * use custom serializers.
     *<p>
     * NOTE: Public only since it also is called by {@code AnyGetterWriter}.
     */
    public void serializeWithoutTypeInfo(Map<?, ?> value, JsonGenerator gen, SerializerProvider ctxt)
        throws JacksonException
    {
        if (!value.isEmpty()) {
            if (_sortKeys || ctxt.isEnabled(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS)) {
                value = _orderEntries(value, gen, ctxt);
            }
            PropertyFilter pf;
            if ((_filterId != null) && (pf = findPropertyFilter(ctxt, _filterId, value)) != null) {
                serializeFilteredEntries(value, gen, ctxt, pf, _suppressableValue);
            } else if ((_suppressableValue != null) || _suppressNulls) {
                serializeOptionalFields(value, gen, ctxt, _suppressableValue);
            } else if (_valueSerializer != null) {
                serializeEntriesUsing(value, gen, ctxt, _valueSerializer);
            } else {
                serializeEntries(value, gen, ctxt);
            }
        }
    }

    /*
    /**********************************************************************
    /* Secondary serialization methods: note -- most public due to legacy
    /* reasons (access from different packages)
    /**********************************************************************
     */

    /**
     * General-purpose serialization for contents, where we do not necessarily know
     * the value serialization, but 
     * we do know that no value suppression is needed (which simplifies processing a bit)
     *<p>
     * NOTE: {@code public} only because it is called by code from {@code Guava}
     *  {@code TableSerializer}
     */
    public void serializeEntries(Map<?,?> value, JsonGenerator gen, SerializerProvider provider)
        throws JacksonException
    {
        // If value type needs polymorphic type handling, some more work needed:
        if (_valueTypeSerializer != null) {
            serializeTypedEntries(value, gen, provider, null);
            return;
        }
        final ValueSerializer<Object> keySerializer = _keySerializer;
        Object keyElem = null;

        try {
            for (Map.Entry<?,?> entry : value.entrySet()) {
                Object valueElem = entry.getValue();
                // First, serialize key
                keyElem = entry.getKey();
                if (keyElem == null) {
                    provider.findNullKeySerializer(_keyType, _property).serialize(null, gen, provider);
                } else {
                    // One twist: is entry ignorable? If so, skip
                    if ((_inclusionChecker != null) && _inclusionChecker.shouldIgnore(keyElem)) {
                        continue;
                    }
                    keySerializer.serialize(keyElem, gen, provider);
                }
                // And then value
                if (valueElem == null) {
                    provider.defaultSerializeNullValue(gen);
                    continue;
                }
                ValueSerializer<Object> serializer = _valueSerializer;
                if (serializer == null) {
                    serializer = _findSerializer(provider, valueElem);
                }
                serializer.serialize(valueElem, gen, provider);
            }
        } catch (Exception e) { // Add reference information
            wrapAndThrow(provider, e, value, String.valueOf(keyElem));
        }
    }

    /**
     * Serialization method called when exclusion filtering needs to be applied.
     *<p>
     * NOTE: {@code public} because other similar methods are (no current known
     * external usage).
     */
    public void serializeOptionalFields(Map<?,?> value, JsonGenerator gen, SerializerProvider provider,
            Object suppressableValue)
        throws JacksonException
    {
        // If value type needs polymorphic type handling, some more work needed:
        if (_valueTypeSerializer != null) {
            serializeTypedEntries(value, gen, provider, suppressableValue);
            return;
        }
        final boolean checkEmpty = (MARKER_FOR_EMPTY == suppressableValue);

        for (Map.Entry<?,?> entry : value.entrySet()) {
            // First find key serializer
            final Object keyElem = entry.getKey();
            ValueSerializer<Object> keySerializer;
            if (keyElem == null) {
                keySerializer = provider.findNullKeySerializer(_keyType, _property);
            } else {
                if ((_inclusionChecker != null) && _inclusionChecker.shouldIgnore(keyElem)) {
                    continue;
                }
                keySerializer = _keySerializer;
            }

            // Then value serializer
            final Object valueElem = entry.getValue();
            ValueSerializer<Object> valueSer;
            if (valueElem == null) {
                if (_suppressNulls) { // all suppressions include null-suppression
                    continue;
                }
                valueSer = provider.getDefaultNullValueSerializer();
            } else {
                valueSer = _valueSerializer;
                if (valueSer == null) {
                    valueSer = _findSerializer(provider, valueElem);
                }
                // also may need to skip non-empty values:
                if (checkEmpty) {
                    if (valueSer.isEmpty(provider, valueElem)) {
                        continue;
                    }
                } else if (suppressableValue != null) {
                    if (suppressableValue.equals(valueElem)) {
                        continue;
                    }
                }
            }
            // and then serialize, if all went well
            try {
                keySerializer.serialize(keyElem, gen, provider);
                valueSer.serialize(valueElem, gen, provider);
            } catch (Exception e) {
                wrapAndThrow(provider, e, value, String.valueOf(keyElem));
            }
        }
    }
    
    /**
     * Method called to serialize fields, when the value type is statically known,
     * so that value serializer is passed and does not need to be fetched from
     * provider.
     *<p>
     * NOTE: {@code public} because other similar methods are (no current known
     * external usage).
     */
    public void serializeEntriesUsing(Map<?,?> value, JsonGenerator gen, SerializerProvider provider,
            ValueSerializer<Object> ser)
        throws JacksonException
    {
        final ValueSerializer<Object> keySerializer = _keySerializer;
        final TypeSerializer typeSer = _valueTypeSerializer;

        for (Map.Entry<?,?> entry : value.entrySet()) {
            Object keyElem = entry.getKey();
            if ((_inclusionChecker != null) && _inclusionChecker.shouldIgnore(keyElem)) {
                continue;
            }

            if (keyElem == null) {
                provider.findNullKeySerializer(_keyType, _property).serialize(null, gen, provider);
            } else {
                keySerializer.serialize(keyElem, gen, provider);
            }
            final Object valueElem = entry.getValue();
            if (valueElem == null) {
                provider.defaultSerializeNullValue(gen);
            } else {
                try {
                    if (typeSer == null) {
                        ser.serialize(valueElem, gen, provider);
                    } else {
                        ser.serializeWithType(valueElem, gen, provider, typeSer);
                    }
                } catch (Exception e) {
                    wrapAndThrow(provider, e, value, String.valueOf(keyElem));
                }
            }
        }
    }

    /**
     * Helper method used when we have a JSON Filter to use for potentially
     * filtering out Map entries.
     *<p>
     * NOTE: {@code public} because other similar methods are (no current known
     * external usage).
     */
    public void serializeFilteredEntries(Map<?,?> value, JsonGenerator gen, SerializerProvider provider,
            PropertyFilter filter,
            Object suppressableValue)
        throws JacksonException
    {
        final MapProperty prop = new MapProperty(_valueTypeSerializer, _property);
        final boolean checkEmpty = (MARKER_FOR_EMPTY == suppressableValue);

        for (Map.Entry<?,?> entry : value.entrySet()) {
            // First, serialize key; unless ignorable by key
            final Object keyElem = entry.getKey();
            if ((_inclusionChecker != null) && _inclusionChecker.shouldIgnore(keyElem)) {
                continue;
            }

            ValueSerializer<Object> keySerializer;
            if (keyElem == null) {
                keySerializer = provider.findNullKeySerializer(_keyType, _property);
            } else {
                keySerializer = _keySerializer;
            }
            // or by value; nulls often suppressed
            final Object valueElem = entry.getValue();

            ValueSerializer<Object> valueSer;
            // And then value
            if (valueElem == null) {
                if (_suppressNulls) {
                    continue;
                }
                valueSer = provider.getDefaultNullValueSerializer();
            } else {
                valueSer = _valueSerializer;
                if (valueSer == null) {
                    valueSer = _findSerializer(provider, valueElem);
                }
                // also may need to skip non-empty values:
                if (checkEmpty) {
                    if (valueSer.isEmpty(provider, valueElem)) {
                        continue;
                    }
                } else if (suppressableValue != null) {
                    if (suppressableValue.equals(valueElem)) {
                        continue;
                    }
                }
            }
            // and with that, ask filter to handle it
            prop.reset(keyElem, valueElem, keySerializer, valueSer);
            try {
                filter.serializeAsProperty(value, gen, provider, prop);
            } catch (Exception e) {
                wrapAndThrow(provider, e, value, String.valueOf(keyElem));
            }
        }
    }

    /**
     *<p>
     * NOTE: {@code public} because other similar methods are (no current known
     * external usage).
     */
    public void serializeTypedEntries(Map<?,?> value, JsonGenerator gen, SerializerProvider provider,
            Object suppressableValue)
        throws JacksonException
    {
        final boolean checkEmpty = (MARKER_FOR_EMPTY == suppressableValue);

        for (Map.Entry<?,?> entry : value.entrySet()) {
            Object keyElem = entry.getKey();
            ValueSerializer<Object> keySerializer;
            if (keyElem == null) {
                keySerializer = provider.findNullKeySerializer(_keyType, _property);
            } else {
                // One twist: is entry ignorable? If so, skip
                if ((_inclusionChecker != null) && _inclusionChecker.shouldIgnore(keyElem)) {
                    continue;
                }
                keySerializer = _keySerializer;
            }
            final Object valueElem = entry.getValue();
    
            // And then value
            ValueSerializer<Object> valueSer;
            if (valueElem == null) {
                if (_suppressNulls) { // all suppression include null suppression
                    continue;
                }
                valueSer = provider.getDefaultNullValueSerializer();
            } else {
                valueSer = _valueSerializer;
                if (valueSer == null) {
                    valueSer = _findSerializer(provider, valueElem);
                }
                // also may need to skip non-empty values:
                if (checkEmpty) {
                    if (valueSer.isEmpty(provider, valueElem)) {
                        continue;
                    }
                } else if (suppressableValue != null) {
                    if (suppressableValue.equals(valueElem)) {
                        continue;
                    }
                }
            }
            keySerializer.serialize(keyElem, gen, provider);
            try {
                valueSer.serializeWithType(valueElem, gen, provider, _valueTypeSerializer);
            } catch (Exception e) {
                wrapAndThrow(provider, e, value, String.valueOf(keyElem));
            }
        }
    }

    /**
     * Helper method used when we have a JSON Filter to use AND contents are
     * "any properties" of a POJO.
     *<p>
     * NOTE: {@code public} only because it is called by {@code AnyGetterWriter}
     *
     * @param bean Enclosing POJO that has any-getter used to obtain "any properties"
     */
    public void serializeFilteredAnyProperties(SerializerProvider provider, JsonGenerator gen,
            Object bean, Map<?,?> value, PropertyFilter filter,
            Object suppressableValue)
        throws JacksonException
    {
        final MapProperty prop = new MapProperty(_valueTypeSerializer, _property);
        final boolean checkEmpty = (MARKER_FOR_EMPTY == suppressableValue);

        for (Map.Entry<?,?> entry : value.entrySet()) {
            // First, serialize key; unless ignorable by key
            final Object keyElem = entry.getKey();
            if ((_inclusionChecker != null) && _inclusionChecker.shouldIgnore(keyElem)) {
                continue;
            }

            ValueSerializer<Object> keySerializer;
            if (keyElem == null) {
                keySerializer = provider.findNullKeySerializer(_keyType, _property);
            } else {
                keySerializer = _keySerializer;
            }
            // or by value; nulls often suppressed
            final Object valueElem = entry.getValue();

            ValueSerializer<Object> valueSer;
            // And then value
            if (valueElem == null) {
                if (_suppressNulls) {
                    continue;
                }
                valueSer = provider.getDefaultNullValueSerializer();
            } else {
                valueSer = _valueSerializer;
                if (valueSer == null) {
                    valueSer = _findSerializer(provider, valueElem);
                }
                // also may need to skip non-empty values:
                if (checkEmpty) {
                    if (valueSer.isEmpty(provider, valueElem)) {
                        continue;
                    }
                } else if (suppressableValue != null) {
                    if (suppressableValue.equals(valueElem)) {
                        continue;
                    }
                }
            }
            // and with that, ask filter to handle it
            prop.reset(keyElem, valueElem, keySerializer, valueSer);
            try {
                filter.serializeAsProperty(bean, gen, provider, prop);
            } catch (Exception e) {
                wrapAndThrow(provider, e, value, String.valueOf(keyElem));
            }
        }
    }

    /*
    /**********************************************************************
    /* Schema related functionality
    /**********************************************************************
     */

    @Override
    public void acceptJsonFormatVisitor(JsonFormatVisitorWrapper visitor, JavaType typeHint)
    {
        JsonMapFormatVisitor v2 = visitor.expectMapFormat(typeHint);        
        if (v2 != null) {
            v2.keyFormat(_keySerializer, _keyType);
            ValueSerializer<?> valueSer = _valueSerializer;
            if (valueSer == null) {
                valueSer = _findAndAddDynamic(visitor.getProvider(), _valueType);
            }
            v2.valueFormat(valueSer, _valueType);
        }
    }

    /*
    /**********************************************************************
    /* Internal helper methods
    /**********************************************************************
     */

    protected Map<?,?> _orderEntries(Map<?,?> input, JsonGenerator gen,
            SerializerProvider provider) throws JacksonException
    {
        // minor optimization: may already be sorted?
        if (input instanceof SortedMap<?,?>) {
            return input;
        }
        // [databind#1411]: TreeMap does not like null key... (although note that
        //   check above should prevent this code from being called in that case)
        // [databind#153]: but, apparently, some custom Maps do manage hit this
        //   problem.
        if (_hasNullKey(input)) {
            TreeMap<Object,Object> result = new TreeMap<Object,Object>();
            for (Map.Entry<?,?> entry : input.entrySet()) {
                Object key = entry.getKey();
                if (key == null) {
                    _writeNullKeyedEntry(gen, provider, entry.getValue());
                    continue;
                } 
                result.put(key, entry.getValue());
            }
            return result;
        }
        return new TreeMap<Object,Object>(input);
    }

    protected boolean _hasNullKey(Map<?,?> input) {
        // 19-Feb-2017, tatu: As per [databind#1513] there are many cases where `null`
        //   keys are not allowed, and even attempt to check for presence can cause
        //   problems. Without resorting to external sorting (and internal API change),
        //   or custom sortable Map implementation (more code) we can try black- or
        //   white-listing (that is; either skip known problem cases; or only apply for
        //   known good cases).
        //   While my first instinct was to do black-listing (remove Hashtable and ConcurrentHashMap),
        //   all in all it is probably better to just white list `HashMap` (and its sub-classes).
        
        return (input instanceof HashMap) && input.containsKey(null);
    }
    
    protected void _writeNullKeyedEntry(JsonGenerator g, SerializerProvider ctxt,
            Object value) throws JacksonException
    {
        ValueSerializer<Object> keySerializer = ctxt.findNullKeySerializer(_keyType, _property);
        ValueSerializer<Object> valueSer;
        if (value == null) {
            if (_suppressNulls) {
                return;
            }
            valueSer = ctxt.getDefaultNullValueSerializer();
        } else {
            valueSer = _valueSerializer;
            if (valueSer == null) {
                valueSer = _findSerializer(ctxt, value);
            }
            if (_suppressableValue == MARKER_FOR_EMPTY) {
                if (valueSer.isEmpty(ctxt, value)) {
                    return;
                }
            } else if ((_suppressableValue != null)
                && (_suppressableValue.equals(value))) {
                return;
            }
        }

        try {
            keySerializer.serialize(null, g, ctxt);
            valueSer.serialize(value, g, ctxt);
        } catch (Exception e) {
            wrapAndThrow(ctxt, e, value, "");
        }
    }

    private final ValueSerializer<Object> _findSerializer(SerializerProvider ctxt,
            Object value)
    {
        final Class<?> cc = value.getClass();
        ValueSerializer<Object> valueSer = _dynamicValueSerializers.serializerFor(cc);
        if (valueSer != null) {
            return valueSer;
        }
        if (_valueType.hasGenericTypes()) {
            return _findAndAddDynamic(ctxt,
                    ctxt.constructSpecializedType(_valueType, cc));
        }
        return _findAndAddDynamic(ctxt, cc);
    }
}
