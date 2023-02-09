package com.fasterxml.jackson.databind.ser;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.HashMap;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.SerializableString;
import com.fasterxml.jackson.core.io.SerializedString;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.annotation.JacksonStdImpl;
import com.fasterxml.jackson.databind.introspect.*;
import com.fasterxml.jackson.databind.jsonFormatVisitors.JsonObjectFormatVisitor;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.ser.impl.PropertySerializerMap;
import com.fasterxml.jackson.databind.ser.impl.UnwrappingBeanPropertyWriter;
import com.fasterxml.jackson.databind.ser.std.BeanSerializerBase;
import com.fasterxml.jackson.databind.util.Annotations;
import com.fasterxml.jackson.databind.util.ClassUtil;
import com.fasterxml.jackson.databind.util.NameTransformer;

/**
 * Base bean property handler class, which implements common parts of
 * reflection-based functionality for accessing a property value and serializing
 * it.
 * <p>
 * Note that current design tries to keep instances immutable (semi-functional
 * style); mostly because these instances are exposed to application code and
 * this is to reduce likelihood of data corruption and synchronization issues.
 */
@JacksonStdImpl
// since 2.6. NOTE: sub-classes typically are not
public class BeanPropertyWriter extends PropertyWriter // which extends
                                                       // `ConcreteBeanPropertyBase`
        implements java.io.Serializable // since 2.6
{
    // As of 2.7
    private static final long serialVersionUID = 1L;

    /**
     * Marker object used to indicate "do not serialize if empty"
     */
    public final static Object MARKER_FOR_EMPTY = JsonInclude.Include.NON_EMPTY;

    /*
    /***********************************************************
    /* Basic property metadata: name, type, other
    /***********************************************************
     */

    /**
     * Logical name of the property; will be used as the field name under which
     * value for the property is written.
     * <p>
     * NOTE: do NOT change name of this field; it is accessed by Afterburner
     * module (until 2.4; not directly from 2.5) ALSO NOTE: ... and while it
     * really ought to be `SerializableString`, changing that is also
     * binary-incompatible change. So nope.
     */
    protected final SerializedString _name;

    /**
     * Wrapper name to use for this element, if any
     *
     * @since 2.2
     */
    protected final PropertyName _wrapperName;

    /**
     * Type property is declared to have, either in class definition or
     * associated annotations.
     */
    protected final JavaType _declaredType;

    /**
     * Type to use for locating serializer; normally same as return type of the
     * accessor method, but may be overridden by annotations.
     */
    protected final JavaType _cfgSerializationType;

    /**
     * Base type of the property, if the declared type is "non-trivial"; meaning
     * it is either a structured type (collection, map, array), or
     * parameterized. Used to retain type information about contained type,
     * which is mostly necessary if type meta-data is to be included.
     */
    protected JavaType _nonTrivialBaseType;

    /**
     * Annotations from context (most often, class that declares property, or in
     * case of sub-class serializer, from that sub-class)
     * <p>
     * NOTE: transient just to support JDK serializability; Annotations do not
     * serialize. At all.
     */
    protected final transient Annotations _contextAnnotations;

    /*
    /***********************************************************
    /* Settings for accessing property value to serialize
    /***********************************************************
     */

    /**
     * Member (field, method) that represents property and allows access to
     * associated annotations.
     */
    protected final AnnotatedMember _member;

    /**
     * Accessor method used to get property value, for method-accessible
     * properties. Null if and only if {@link #_field} is null.
     * <p>
     * `transient` (and non-final) only to support JDK serializability.
     */
    protected transient Method _accessorMethod;

    /**
     * Field that contains the property value for field-accessible properties.
     * Null if and only if {@link #_accessorMethod} is null.
     * <p>
     * `transient` (and non-final) only to support JDK serializability.
     */
    protected transient Field _field;

    /*
    /***********************************************************
    /* Serializers needed
    /***********************************************************
     */

    /**
     * Serializer to use for writing out the value: null if it cannot be known
     * statically; non-null if it can.
     */
    protected JsonSerializer<Object> _serializer;

    /**
     * Serializer used for writing out null values, if any: if null, null values
     * are to be suppressed.
     */
    protected JsonSerializer<Object> _nullSerializer;

    /**
     * If property being serialized needs type information to be included this
     * is the type serializer to use. Declared type (possibly augmented with
     * annotations) of property is used for determining exact mechanism to use
     * (compared to actual runtime type used for serializing actual state).
     */
    protected TypeSerializer _typeSerializer;

    /**
     * In case serializer is not known statically (i.e. <code>_serializer</code>
     * is null), we will use a lookup structure for storing dynamically resolved
     * mapping from type(s) to serializer(s).
     */
    protected transient PropertySerializerMap _dynamicSerializers;

    /*
    /***********************************************************
    /* Filtering
    /***********************************************************
     */

    /**
     * Whether null values are to be suppressed (nothing written out if value is
     * null) or not. Note that this is a configuration value during
     * construction, and actual handling relies on setting (or not) of
     * {@link #_nullSerializer}.
     */
    protected final boolean _suppressNulls;

    /**
     * Value that is considered default value of the property; used for
     * default-value-suppression if enabled.
     */
    protected final Object _suppressableValue;

    /**
     * Alternate set of property writers used when view-based filtering is
     * available for the Bean.
     */
    protected final Class<?>[] _includeInViews;

    /*
    /**********************************************************
    /* Opaqueinternal data that bean serializer factory and
    /* bean serializers can add.
    /**********************************************************
     */

    protected transient HashMap<Object, Object> _internalSettings;

    /*
    /***********************************************************
    /* Construction, configuration
    /***********************************************************
     */

    /**
     * @since 2.9 (added `includeInViews` since 2.8)
     */
    @SuppressWarnings("unchecked")
    public BeanPropertyWriter(BeanPropertyDefinition propDef,
            AnnotatedMember member, Annotations contextAnnotations,
            JavaType declaredType,
            JsonSerializer<?> ser, TypeSerializer typeSer, JavaType serType,
            boolean suppressNulls, Object suppressableValue,
            Class<?>[] includeInViews)
    {
        super(propDef);
        _member = member;
        _contextAnnotations = contextAnnotations;

        _name = new SerializedString(propDef.getName());
        _wrapperName = propDef.getWrapperName();

        _declaredType = declaredType;
        _serializer = (JsonSerializer<Object>) ser;
        _dynamicSerializers = (ser == null) ? PropertySerializerMap
                .emptyForProperties() : null;
        _typeSerializer = typeSer;
        _cfgSerializationType = serType;

        if (member instanceof AnnotatedField) {
            _accessorMethod = null;
            _field = (Field) member.getMember();
        } else if (member instanceof AnnotatedMethod) {
            _accessorMethod = (Method) member.getMember();
            _field = null;
        } else {
            // 01-Dec-2014, tatu: Used to be illegal, but now explicitly allowed
            // for virtual props
            _accessorMethod = null;
            _field = null;
        }
        _suppressNulls = suppressNulls;
        _suppressableValue = suppressableValue;

        // this will be resolved later on, unless nulls are to be suppressed
        _nullSerializer = null;
        _includeInViews = includeInViews;
    }

    @Deprecated // Since 2.9
    public BeanPropertyWriter(BeanPropertyDefinition propDef,
            AnnotatedMember member, Annotations contextAnnotations,
            JavaType declaredType,
            JsonSerializer<?> ser, TypeSerializer typeSer, JavaType serType,
            boolean suppressNulls, Object suppressableValue)
    {
        this(propDef, member, contextAnnotations, declaredType,
                ser, typeSer, serType, suppressNulls, suppressableValue,
                null);
    }

    /**
     * Constructor that may be of use to virtual properties, when there is need
     * for the zero-arg ("default") constructor, and actual initialization is
     * done after constructor call.
     *
     * @since 2.5
     */
    protected BeanPropertyWriter() {
        super(PropertyMetadata.STD_REQUIRED_OR_OPTIONAL);
        _member = null;
        _contextAnnotations = null;

        _name = null;
        _wrapperName = null;
        _includeInViews = null;

        _declaredType = null;
        _serializer = null;
        _dynamicSerializers = null;
        _typeSerializer = null;
        _cfgSerializationType = null;

        _accessorMethod = null;
        _field = null;
        _suppressNulls = false;
        _suppressableValue = null;

        _nullSerializer = null;
    }

    /**
     * "Copy constructor" to be used by filtering sub-classes
     */
    protected BeanPropertyWriter(BeanPropertyWriter base) {
        this(base, base._name);
    }

    /**
     * @since 2.5
     */
    protected BeanPropertyWriter(BeanPropertyWriter base, PropertyName name) {
        super(base);
        /*
         * 02-Dec-2014, tatu: This is a big mess, alas, what with dependency to
         * MapperConfig to encode, and Afterburner having heartburn for
         * SerializableString (vs SerializedString). Hope it can be
         * resolved/reworked in 2.6 timeframe, if not for 2.5
         */
        _name = new SerializedString(name.getSimpleName());
        _wrapperName = base._wrapperName;

        _contextAnnotations = base._contextAnnotations;
        _declaredType = base._declaredType;

        _member = base._member;
        _accessorMethod = base._accessorMethod;
        _field = base._field;

        _serializer = base._serializer;
        _nullSerializer = base._nullSerializer;
        // one more thing: copy internal settings, if any
        if (base._internalSettings != null) {
            _internalSettings = new HashMap<Object, Object>(
                    base._internalSettings);
        }
        _cfgSerializationType = base._cfgSerializationType;
        _dynamicSerializers = base._dynamicSerializers;
        _suppressNulls = base._suppressNulls;
        _suppressableValue = base._suppressableValue;
        _includeInViews = base._includeInViews;
        _typeSerializer = base._typeSerializer;
        _nonTrivialBaseType = base._nonTrivialBaseType;
    }

    protected BeanPropertyWriter(BeanPropertyWriter base, SerializedString name) {
        super(base);
        _name = name;
        _wrapperName = base._wrapperName;

        _member = base._member;
        _contextAnnotations = base._contextAnnotations;
        _declaredType = base._declaredType;
        _accessorMethod = base._accessorMethod;
        _field = base._field;
        _serializer = base._serializer;
        _nullSerializer = base._nullSerializer;
        if (base._internalSettings != null) {
            _internalSettings = new HashMap<Object, Object>(
                    base._internalSettings);
        }
        _cfgSerializationType = base._cfgSerializationType;
        _dynamicSerializers = base._dynamicSerializers;
        _suppressNulls = base._suppressNulls;
        _suppressableValue = base._suppressableValue;
        _includeInViews = base._includeInViews;
        _typeSerializer = base._typeSerializer;
        _nonTrivialBaseType = base._nonTrivialBaseType;
    }

    public BeanPropertyWriter rename(NameTransformer transformer) {
        String newName = transformer.transform(_name.getValue());
        if (newName.equals(_name.toString())) {
            return this;
        }
        return _new(PropertyName.construct(newName));
    }

    /**
     * Overridable factory method used by sub-classes
     *
     * @since 2.6
     */
    protected BeanPropertyWriter _new(PropertyName newName) {
        return new BeanPropertyWriter(this, newName);
    }

    /**
     * Method called to set, reset or clear the configured type serializer for
     * property.
     *
     * @since 2.6
     */
    public void assignTypeSerializer(TypeSerializer typeSer) {
        _typeSerializer = typeSer;
    }

    /**
     * Method called to assign value serializer for property
     */
    public void assignSerializer(JsonSerializer<Object> ser) {
        // may need to disable check in future?
        if ((_serializer != null) && (_serializer != ser)) {
            throw new IllegalStateException(String.format(
                    "Cannot override _serializer: had a %s, trying to set to %s",
                    ClassUtil.classNameOf(_serializer), ClassUtil.classNameOf(ser)));
        }
        _serializer = ser;
    }

    /**
     * Method called to assign null value serializer for property
     */
    public void assignNullSerializer(JsonSerializer<Object> nullSer) {
        // may need to disable check in future?
        if ((_nullSerializer != null) && (_nullSerializer != nullSer)) {
            throw new IllegalStateException(String.format(
                    "Cannot override _nullSerializer: had a %s, trying to set to %s",
                    ClassUtil.classNameOf(_nullSerializer), ClassUtil.classNameOf(nullSer)));
        }
        _nullSerializer = nullSer;
    }

    /**
     * Method called create an instance that handles details of unwrapping
     * contained value.
     */
    public BeanPropertyWriter unwrappingWriter(NameTransformer unwrapper) {
        return new UnwrappingBeanPropertyWriter(this, unwrapper);
    }

    /**
     * Method called to define type to consider as "non-trivial" basetype,
     * needed for dynamic serialization resolution for complex (usually
     * container) types
     */
    public void setNonTrivialBaseType(JavaType t) {
        _nonTrivialBaseType = t;
    }

    /**
     * Method called to ensure that the mutator has proper access rights to
     * be called, as per configuration. Overridden by implementations that
     * have mutators that require access, fields and setters.
     *
     * @since 2.8.3
     */
    public void fixAccess(SerializationConfig config) {
        _member.fixAccess(config.isEnabled(MapperFeature.OVERRIDE_PUBLIC_ACCESS_MODIFIERS));
    }

    /*
    /***********************************************************
    /* JDK Serializability
    /***********************************************************
     */

    /*
     * Ideally would not require mutable state, and instead would re-create with
     * final settings. However, as things are, with sub-types and all, simplest
     * to just change Field/Method value directly.
     */
    Object readResolve() {
        if (_member instanceof AnnotatedField) {
            _accessorMethod = null;
            _field = (Field) _member.getMember();
        } else if (_member instanceof AnnotatedMethod) {
            _accessorMethod = (Method) _member.getMember();
            _field = null;
        }
        if (_serializer == null) {
            _dynamicSerializers = PropertySerializerMap.emptyForProperties();
        }
        return this;
    }

    /*
    /************************************************************
    /* BeanProperty impl
    /***********************************************************
     */

    // Note: also part of 'PropertyWriter'
    @Override
    public String getName() {
        return _name.getValue();
    }

    // Note: also part of 'PropertyWriter'
    @Override
    public PropertyName getFullName() { // !!! TODO: impl properly
        return new PropertyName(_name.getValue());
    }

    @Override
    public JavaType getType() {
        return _declaredType;
    }

    @Override
    public PropertyName getWrapperName() {
        return _wrapperName;
    }

    // Note: also part of 'PropertyWriter'
    @Override
    public <A extends Annotation> A getAnnotation(Class<A> acls) {
        return (_member == null) ? null : _member.getAnnotation(acls);
    }

    // Note: also part of 'PropertyWriter'
    @Override
    public <A extends Annotation> A getContextAnnotation(Class<A> acls) {
        return (_contextAnnotations == null) ? null : _contextAnnotations
                .get(acls);
    }

    @Override
    public AnnotatedMember getMember() {
        return _member;
    }

    // @since 2.3 -- needed so it can be overridden by unwrapping writer
    protected void _depositSchemaProperty(ObjectNode propertiesNode,
            JsonNode schemaNode) {
        propertiesNode.set(getName(), schemaNode);
    }

    /*
    /***********************************************************
    /* Managing and accessing of opaque internal settings
    /* (used by extensions)
    /***********************************************************
     */

    /**
     * Method for accessing value of specified internal setting.
     *
     * @return Value of the setting, if any; null if none.
     */
    public Object getInternalSetting(Object key) {
        return (_internalSettings == null) ? null : _internalSettings.get(key);
    }

    /**
     * Method for setting specific internal setting to given value
     *
     * @return Old value of the setting, if any (null if none)
     */
    public Object setInternalSetting(Object key, Object value) {
        if (_internalSettings == null) {
            _internalSettings = new HashMap<Object, Object>();
        }
        return _internalSettings.put(key, value);
    }

    /**
     * Method for removing entry for specified internal setting.
     *
     * @return Existing value of the setting, if any (null if none)
     */
    public Object removeInternalSetting(Object key) {
        Object removed = null;
        if (_internalSettings != null) {
            removed = _internalSettings.remove(key);
            // to reduce memory usage, let's also drop the Map itself, if empty
            if (_internalSettings.size() == 0) {
                _internalSettings = null;
            }
        }
        return removed;
    }

    /*
    /***********************************************************
    /* Accessors
    /***********************************************************
     */

    public SerializableString getSerializedName() {
        return _name;
    }

    public boolean hasSerializer() {
        return _serializer != null;
    }

    public boolean hasNullSerializer() {
        return _nullSerializer != null;
    }

    /**
     * @since 2.6
     */
    public TypeSerializer getTypeSerializer() {
        return _typeSerializer;
    }

    /**
     * Accessor that will return true if this bean property has to support
     * "unwrapping"; ability to replace POJO structural wrapping with optional
     * name prefix and/or suffix (or in some cases, just removal of wrapper
     * name).
     * <p>
     * Default implementation simply returns false.
     *
     * @since 2.3
     */
    public boolean isUnwrapping() {
        return false;
    }

    public boolean willSuppressNulls() {
        return _suppressNulls;
    }

    /**
     * Method called to check to see if this property has a name that would
     * conflict with a given name.
     *
     * @since 2.6
     */
    public boolean wouldConflictWithName(PropertyName name) {
        if (_wrapperName != null) {
            return _wrapperName.equals(name);
        }
        // Bit convoluted since our support for namespaces is spotty but:
        return name.hasSimpleName(_name.getValue()) && !name.hasNamespace();
    }

    // Needed by BeanSerializer#getSchema
    public JsonSerializer<Object> getSerializer() {
        return _serializer;
    }

    public JavaType getSerializationType() {
        return _cfgSerializationType;
    }

    @Deprecated // since 2.9
    public Class<?> getRawSerializationType() {
        return (_cfgSerializationType == null) ? null : _cfgSerializationType
                .getRawClass();
    }

    /**
     * @deprecated Since 2.7, to be removed from 2.9, use {@link #getType()} instead.
     */
    @Deprecated
    public Class<?> getPropertyType() {
        if (_accessorMethod != null) {
            return _accessorMethod.getReturnType();
        }
        if (_field != null) {
            return _field.getType();
        }
        return null;
    }

    /**
     * Get the generic property type of this property writer.
     *
     * @return The property type, or null if not found.
     *
     * @deprecated Since 2.7, to be removed from 2.9, use {@link #getType()} instead.
     */
    @Deprecated
    public Type getGenericPropertyType() {
        if (_accessorMethod != null) {
            return _accessorMethod.getGenericReturnType();
        }
        if (_field != null) {
            return _field.getGenericType();
        }
        return null;
    }

    public Class<?>[] getViews() {
        return _includeInViews;
    }

    /*
    /***********************************************************
    /* PropertyWriter methods (serialization)
    /***********************************************************
     */

    /**
     * Method called to access property that this bean stands for, from within
     * given bean, and to serialize it as a JSON Object field using appropriate
     * serializer.
     */
    @Override
    public void serializeAsField(Object bean, JsonGenerator gen,
            SerializerProvider prov) throws Exception {
        // inlined 'get()'
        final Object value = (_accessorMethod == null) ? _field.get(bean)
                : _accessorMethod.invoke(bean, (Object[]) null);

        // Null handling is bit different, check that first
        if (value == null) {
            // 20-Jun-2022, tatu: Defer checking of null, see [databind#3481]
            if((_suppressableValue != null)
                    && prov.includeFilterSuppressNulls(_suppressableValue)) {
                return;
            }
            if (_nullSerializer != null) {
                gen.writeFieldName(_name);
                _nullSerializer.serialize(null, gen, prov);
            }
            return;
        }
        // then find serializer to use
        JsonSerializer<Object> ser = _serializer;
        if (ser == null) {
            Class<?> cls = value.getClass();
            PropertySerializerMap m = _dynamicSerializers;
            ser = m.serializerFor(cls);
            if (ser == null) {
                ser = _findAndAddDynamic(m, cls, prov);
            }
        }
        // and then see if we must suppress certain values (default, empty)
        if (_suppressableValue != null) {
            if (MARKER_FOR_EMPTY == _suppressableValue) {
                if (ser.isEmpty(prov, value)) {
                    return;
                }
            } else if (_suppressableValue.equals(value)) {
                return;
            }
        }
        // For non-nulls: simple check for direct cycles
        if (value == bean) {
            // four choices: exception; handled by call; pass-through or write null
            if (_handleSelfReference(bean, gen, prov, ser)) {
                return;
            }
        }
        gen.writeFieldName(_name);
        if (_typeSerializer == null) {
            ser.serialize(value, gen, prov);
        } else {
            ser.serializeWithType(value, gen, prov, _typeSerializer);
        }
    }

    /**
     * Method called to indicate that serialization of a field was omitted due
     * to filtering, in cases where backend data format does not allow basic
     * omission.
     *
     * @since 2.3
     */
    @Override
    public void serializeAsOmittedField(Object bean, JsonGenerator gen,
            SerializerProvider prov) throws Exception {
        if (!gen.canOmitFields()) {
            gen.writeOmittedField(_name.getValue());
        }
    }

    /**
     * Alternative to {@link #serializeAsField} that is used when a POJO is
     * serialized as JSON Array; the difference is that no field names are
     * written.
     *
     * @since 2.3
     */
    @Override
    public void serializeAsElement(Object bean, JsonGenerator gen,
            SerializerProvider prov) throws Exception {
        // inlined 'get()'
        final Object value = (_accessorMethod == null) ? _field.get(bean)
                : _accessorMethod.invoke(bean, (Object[]) null);
        if (value == null) { // nulls need specialized handling
            if (_nullSerializer != null) {
                _nullSerializer.serialize(null, gen, prov);
            } else { // can NOT suppress entries in tabular output
                gen.writeNull();
            }
            return;
        }
        // otherwise find serializer to use
        JsonSerializer<Object> ser = _serializer;
        if (ser == null) {
            Class<?> cls = value.getClass();
            PropertySerializerMap map = _dynamicSerializers;
            ser = map.serializerFor(cls);
            if (ser == null) {
                ser = _findAndAddDynamic(map, cls, prov);
            }
        }
        // and then see if we must suppress certain values (default, empty)
        if (_suppressableValue != null) {
            if (MARKER_FOR_EMPTY == _suppressableValue) {
                if (ser.isEmpty(prov, value)) { // can NOT suppress entries in
                                                // tabular output
                    serializeAsPlaceholder(bean, gen, prov);
                    return;
                }
            } else if (_suppressableValue.equals(value)) {
                // can NOT suppress entries in tabular output
                serializeAsPlaceholder(bean, gen, prov);
                return;
            }
        }
        // For non-nulls: simple check for direct cycles
        if (value == bean) {
            if (_handleSelfReference(bean, gen, prov, ser)) {
                return;
            }
        }
        if (_typeSerializer == null) {
            ser.serialize(value, gen, prov);
        } else {
            ser.serializeWithType(value, gen, prov, _typeSerializer);
        }
    }

    /**
     * Method called to serialize a placeholder used in tabular output when real
     * value is not to be included (is filtered out), but when we need an entry
     * so that field indexes will not be off. Typically this should output null
     * or empty String, depending on datatype.
     *
     * @since 2.1
     */
    @Override
    public void serializeAsPlaceholder(Object bean, JsonGenerator gen,
            SerializerProvider prov) throws Exception {
        if (_nullSerializer != null) {
            _nullSerializer.serialize(null, gen, prov);
        } else {
            gen.writeNull();
        }
    }

    /*
    /***********************************************************
    /* PropertyWriter methods (schema generation)
    /***********************************************************
     */

    // Also part of BeanProperty implementation
    @Override
    public void depositSchemaProperty(JsonObjectFormatVisitor v,
            SerializerProvider provider) throws JsonMappingException {
        if (v != null) {
            if (isRequired()) {
                v.property(this);
            } else {
                v.optionalProperty(this);
            }
        }
    }

    // // // Legacy support for JsonFormatVisitable

    /**
     * Attempt to add the output of the given {@link BeanPropertyWriter} in the
     * given {@link ObjectNode}. Otherwise, add the default schema
     * {@link JsonNode} in place of the writer's output
     *
     * @param propertiesNode
     *            Node which the given property would exist within
     * @param provider
     *            Provider that can be used for accessing dynamic aspects of
     *            serialization processing
     */
    @Override
    @Deprecated
    public void depositSchemaProperty(ObjectNode propertiesNode,
            SerializerProvider provider) throws JsonMappingException {
        JavaType propType = getSerializationType();
        // 03-Dec-2010, tatu: SchemaAware REALLY should use JavaType, but alas
        // it doesn't...
        Type hint = (propType == null) ? getType() : propType.getRawClass();
        JsonNode schemaNode;
        // Maybe it already has annotated/statically configured serializer?
        JsonSerializer<Object> ser = getSerializer();
        if (ser == null) { // nope
            ser = provider.findValueSerializer(getType(), this);
        }
        boolean isOptional = !isRequired();
        if (ser instanceof com.fasterxml.jackson.databind.jsonschema.SchemaAware) {
            schemaNode = ((com.fasterxml.jackson.databind.jsonschema.SchemaAware) ser).getSchema(provider, hint,
                    isOptional);
        } else {
            schemaNode = com.fasterxml.jackson.databind.jsonschema.JsonSchema
                    .getDefaultSchemaNode();
        }
        _depositSchemaProperty(propertiesNode, schemaNode);
    }

    /*
    /**********************************************************
    /* Helper methods
    /**********************************************************
     */

    protected JsonSerializer<Object> _findAndAddDynamic(
            PropertySerializerMap map, Class<?> type,
            SerializerProvider provider) throws JsonMappingException {
        PropertySerializerMap.SerializerAndMapResult result;
        if (_nonTrivialBaseType != null) {
            JavaType t = provider.constructSpecializedType(_nonTrivialBaseType,
                    type);
            result = map.findAndAddPrimarySerializer(t, provider, this);
        } else {
            result = map.findAndAddPrimarySerializer(type, provider, this);
        }
        // did we get a new map of serializers? If so, start using it
        if (map != result.map) {
            _dynamicSerializers = result.map;
        }
        return result.serializer;
    }

    /**
     * Method that can be used to access value of the property this Object
     * describes, from given bean instance.
     * <p>
     * Note: method is final as it should not need to be overridden -- rather,
     * calling method(s) ({@link #serializeAsField}) should be overridden to
     * change the behavior
     */
    public final Object get(Object bean) throws Exception {
        return (_accessorMethod == null) ? _field.get(bean) : _accessorMethod
                .invoke(bean, (Object[]) null);
    }

    /**
     * Method called to handle a direct self-reference through this property.
     * Method can choose to indicate an error by throwing
     * {@link JsonMappingException}; fully handle serialization (and return
     * true); or indicate that it should be serialized normally (return false).
     * <p>
     * Default implementation will throw {@link JsonMappingException} if
     * {@link SerializationFeature#FAIL_ON_SELF_REFERENCES} is enabled; or
     * return <code>false</code> if it is disabled.
     *
     * @return True if method fully handled self-referential value; false if not
     *         (caller is to handle it) or {@link JsonMappingException} if there
     *         is no way handle it
     */
    protected boolean _handleSelfReference(Object bean, JsonGenerator gen,
            SerializerProvider prov, JsonSerializer<?> ser)
            throws IOException
    {
        if (!ser.usesObjectId()) {
            if (prov.isEnabled(SerializationFeature.FAIL_ON_SELF_REFERENCES)) {
                // 05-Feb-2013, tatu: Usually a problem, but NOT if we are handling
                // object id; this may be the case for BeanSerializers at least.
                // 13-Feb-2014, tatu: another possible ok case: custom serializer
                // (something OTHER than {@link BeanSerializerBase}
                if (ser instanceof BeanSerializerBase) {
                    prov.reportBadDefinition(getType(), "Direct self-reference leading to cycle");
                }
            } else if (prov.isEnabled(SerializationFeature.WRITE_SELF_REFERENCES_AS_NULL)) {
                if (_nullSerializer != null) {
                    // 23-Oct-2019, tatu: Tricky part -- caller does not specify if it's
                    //   "as property" (in JSON Object) or "as element" (JSON array, via
                    //   'POJO-as-array'). And since Afterburner calls method can not easily
                    //   start passing info either. So check generator to see...
                    //   (note: not considering ROOT context as possibility, does not seem legal)
                    if (!gen.getOutputContext().inArray()) {
                        gen.writeFieldName(_name);
                    }
                    _nullSerializer.serialize(null, gen, prov);
                }
                return true;
            }
        }
        return false;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(40);
        sb.append("property '").append(getName()).append("' (");
        if (_accessorMethod != null) {
            sb.append("via method ")
                    .append(_accessorMethod.getDeclaringClass().getName())
                    .append("#").append(_accessorMethod.getName());
        } else if (_field != null) {
            sb.append("field \"").append(_field.getDeclaringClass().getName())
                    .append("#").append(_field.getName());
        } else {
            sb.append("virtual");
        }
        if (_serializer == null) {
            sb.append(", no static serializer");
        } else {
            sb.append(", static serializer of type "
                    + _serializer.getClass().getName());
        }
        sb.append(')');
        return sb.toString();
    }
}
