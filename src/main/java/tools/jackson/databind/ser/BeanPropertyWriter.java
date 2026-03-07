package tools.jackson.databind.ser;

import java.lang.annotation.Annotation;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.util.HashMap;

import com.fasterxml.jackson.annotation.JsonInclude;

import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonGenerator;
import tools.jackson.core.SerializableString;
import tools.jackson.core.io.SerializedString;
import tools.jackson.databind.*;
import tools.jackson.databind.annotation.JacksonStdImpl;
import tools.jackson.databind.cfg.MapperConfig;
import tools.jackson.databind.introspect.*;
import tools.jackson.databind.jsonFormatVisitors.JsonObjectFormatVisitor;
import tools.jackson.databind.jsontype.TypeSerializer;
import tools.jackson.databind.node.ObjectNode;
import tools.jackson.databind.ser.bean.BeanSerializerBase;
import tools.jackson.databind.ser.bean.UnwrappingBeanPropertyWriter;
import tools.jackson.databind.ser.impl.PropertySerializerMap;
import tools.jackson.databind.util.Annotations;
import tools.jackson.databind.util.ClassUtil;
import tools.jackson.databind.util.NameTransformer;
import tools.jackson.databind.util.internal.UnreflectHandleSupplier;

import static java.lang.invoke.MethodType.methodType;

import static tools.jackson.databind.util.ClassUtil.sneakyThrow;

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
public class BeanPropertyWriter
    extends PropertyWriter // which extends `ConcreteBeanPropertyBase`
{
    /**
     * Marker object used to indicate "do not serialize if empty"
     */
    public final static Object MARKER_FOR_EMPTY = JsonInclude.Include.NON_EMPTY;

    /*
    /**********************************************************************
    /* Basic property metadata: name, type, other
    /**********************************************************************
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
    /**********************************************************************
    /* Settings for accessing property value to serialize
    /**********************************************************************
     */

    /**
     * Member (field, method) that represents property and allows access to
     * associated annotations.
     */
    protected final AnnotatedMember _member;

    /**
     * Accessor method used to get property value.
     * Wrapped in a holder since MethodHandle is not serializable.
     */
    protected final GetterHolder _accessor = new GetterHolder();

    /*
    /**********************************************************************
    /* Serializers needed
    /**********************************************************************
     */

    /**
     * Serializer to use for writing out the value: null if it cannot be known
     * statically; non-null if it can.
     */
    protected ValueSerializer<Object> _serializer;

    /**
     * Serializer used for writing out null values, if any: if null, null values
     * are to be suppressed.
     */
    protected ValueSerializer<Object> _nullSerializer;

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
    /**********************************************************************
    /* Filtering
    /**********************************************************************
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

    /**
     * Inclusion settings for this property, pre-computed in {@code PropertyBuilder}
     * by merging global defaults, type defaults, and property-level annotations,
     * including contextual annotations from the enclosing class.
     *
     * @since 3.1
     */
    protected final JsonInclude.Value _inclusion;

    /*
    /**********************************************************************
    /* Opaqueinternal data that bean serializer factory and
    /* bean serializers can add.
    /**********************************************************************
     */

    protected transient HashMap<Object, Object> _internalSettings;

    /*
    /**********************************************************************
    /* Construction, configuration
    /**********************************************************************
     */

    /**
     * @deprecated Since 3.1
     */
    @Deprecated // @since 3.1
    public BeanPropertyWriter(BeanPropertyDefinition propDef,
            AnnotatedMember member, Annotations contextAnnotations,
            JavaType declaredType,
            ValueSerializer<?> ser, TypeSerializer typeSer, JavaType serType,
            boolean suppressNulls, Object suppressableValue,
            Class<?>[] includeInViews)
    {
        this(propDef, member, contextAnnotations, declaredType,
                ser, typeSer, serType, suppressNulls, suppressableValue,
                includeInViews, null);
    }

    /**
     * Constructor with additional inclusion parameter.
     *
     * @since 3.1
     */
    @SuppressWarnings("unchecked")
    public BeanPropertyWriter(BeanPropertyDefinition propDef,
            AnnotatedMember member, Annotations contextAnnotations,
            JavaType declaredType,
            ValueSerializer<?> ser, TypeSerializer typeSer, JavaType serType,
            boolean suppressNulls, Object suppressableValue,
            Class<?>[] includeInViews, JsonInclude.Value inclusion)
    {
        super(propDef);
        _member = member;
        _contextAnnotations = contextAnnotations;

        _name = new SerializedString(propDef.getName());
        _wrapperName = propDef.getWrapperName();

        _declaredType = declaredType;
        _serializer = (ValueSerializer<Object>) ser;
        _dynamicSerializers = (ser == null) ? PropertySerializerMap.emptyForProperties() : null;
        _typeSerializer = typeSer;
        _cfgSerializationType = serType;

        _suppressNulls = suppressNulls;
        _suppressableValue = suppressableValue;

        // this will be resolved later on, unless nulls are to be suppressed
        _nullSerializer = null;
        _includeInViews = includeInViews;
        _inclusion = (inclusion == null) ? JsonInclude.Value.empty() : inclusion;
    }

    /**
     * Constructor that may be of use to virtual properties, when there is need
     * for the zero-arg ("default") constructor, and actual initialization is
     * done after constructor call.
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

        _suppressNulls = false;
        _suppressableValue = null;

        _nullSerializer = null;
        _inclusion = null;
    }

    /**
     * "Copy constructor" to be used by filtering sub-classes
     */
    protected BeanPropertyWriter(BeanPropertyWriter base) {
        this(base, base._name);
    }

    protected BeanPropertyWriter(BeanPropertyWriter base, PropertyName name) {
        super(base);
        /* 02-Dec-2014, tatu: This is a big mess, alas, what with dependency to
         * MapperConfig to encode, and Afterburner having heart-burn for
         * SerializableString (vs SerializedString).
         */
        _name = new SerializedString(name.getSimpleName());
        _wrapperName = base._wrapperName;

        _contextAnnotations = base._contextAnnotations;
        _declaredType = base._declaredType;

        _member = base._member;

        _serializer = base._serializer;
        _nullSerializer = base._nullSerializer;
        // one more thing: copy internal settings, if any
        if (base._internalSettings != null) {
            _internalSettings = new HashMap<Object, Object>(
                    base._internalSettings);
        }
        _cfgSerializationType = base._cfgSerializationType;
        _dynamicSerializers = PropertySerializerMap.emptyForProperties();
        _suppressNulls = base._suppressNulls;
        _suppressableValue = base._suppressableValue;
        _includeInViews = base._includeInViews;
        _typeSerializer = base._typeSerializer;
        _nonTrivialBaseType = base._nonTrivialBaseType;
        _inclusion = base._inclusion;
    }

    protected BeanPropertyWriter(BeanPropertyWriter base, SerializedString name) {
        super(base);
        _name = name;
        _wrapperName = base._wrapperName;

        _member = base._member;
        _contextAnnotations = base._contextAnnotations;
        _declaredType = base._declaredType;
        _serializer = base._serializer;
        _nullSerializer = base._nullSerializer;
        if (base._internalSettings != null) {
            _internalSettings = new HashMap<Object, Object>(
                    base._internalSettings);
        }
        _cfgSerializationType = base._cfgSerializationType;
        _dynamicSerializers = PropertySerializerMap.emptyForProperties();
        _suppressNulls = base._suppressNulls;
        _suppressableValue = base._suppressableValue;
        _includeInViews = base._includeInViews;
        _typeSerializer = base._typeSerializer;
        _nonTrivialBaseType = base._nonTrivialBaseType;
        _inclusion = base._inclusion;
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
     */
    protected BeanPropertyWriter _new(PropertyName newName) {
        if (getClass() != BeanPropertyWriter.class) {
            throw new IllegalStateException("Method must be overridden by "+getClass());
        }
        return new BeanPropertyWriter(this, newName);
    }

    /**
     * Method called to set, reset or clear the configured type serializer for
     * property.
     */
    public void assignTypeSerializer(TypeSerializer typeSer) {
        _typeSerializer = typeSer;
    }

    /**
     * Method called to assign value serializer for property
     */
    public void assignSerializer(ValueSerializer<Object> ser) {
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
    public void assignNullSerializer(ValueSerializer<Object> nullSer) {
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
     */
    public void fixAccess(SerializationConfig config) {
        _member.fixAccess(config.isEnabled(MapperFeature.OVERRIDE_PUBLIC_ACCESS_MODIFIERS));
    }

    /*
    /**********************************************************************
    /* BeanProperty impl
    /**********************************************************************
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

    @Override
    public JsonInclude.Value findPropertyInclusion(MapperConfig<?> config, Class<?> baseType)
    {
        return _inclusion;
    }

    protected void _depositSchemaProperty(ObjectNode propertiesNode,
            JsonNode schemaNode) {
        propertiesNode.set(getName(), schemaNode);
    }

    /*
    /**********************************************************************
    /* Managing and accessing of opaque internal settings
    /* (used by extensions)
    /**********************************************************************
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
            _internalSettings = new HashMap<>();
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
    public ValueSerializer<Object> getSerializer() {
        return _serializer;
    }

    public JavaType getSerializationType() {
        return _cfgSerializationType;
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
    public void serializeAsProperty(Object bean, JsonGenerator g, SerializationContext ctxt)
        throws Exception
    {
        final Object value = get(bean);

        // Null handling is bit different, check that first
        if (value == null) {
            // 20-Jun-2022, tatu: Defer checking of null, see [databind#3481]
            if((_suppressableValue != null)
                    && ctxt.includeFilterSuppressNulls(_suppressableValue)) {
                return;
            }
            if (_nullSerializer != null) {
                g.writeName(_name);
                _nullSerializer.serialize(null, g, ctxt);
            }
            return;
        }
        // then find serializer to use
        ValueSerializer<Object> ser = _serializer;
        if (ser == null) {
            Class<?> cls = value.getClass();
            PropertySerializerMap m = _dynamicSerializers;
            ser = m.serializerFor(cls);
            if (ser == null) {
                ser = _findAndAddDynamic(m, cls, ctxt);
            }
        }
        // and then see if we must suppress certain values (default, empty)
        if (_suppressableValue != null) {
            if (MARKER_FOR_EMPTY == _suppressableValue) {
                if (ser.isEmpty(ctxt, value)) {
                    return;
                }
            } else if (_suppressableValue.equals(value)) {
                return;
            }
        }
        // For non-nulls: simple check for direct cycles
        if (value == bean) {
            // four choices: exception; handled by call; pass-through or write null
            if (_handleSelfReference(bean, g, ctxt, ser)) {
                return;
            }
        }
        g.writeName(_name);
        if (_typeSerializer == null) {
            ser.serialize(value, g, ctxt);
        } else {
            ser.serializeWithType(value, g, ctxt, _typeSerializer);
        }
    }

    /**
     * Method called to indicate that serialization of a field was omitted due
     * to filtering, in cases where backend data format does not allow basic
     * omission.
     */
    @Override
    public void serializeAsOmittedProperty(Object bean, JsonGenerator g, SerializationContext ctxt)
        throws Exception
    {
        if (!g.canOmitProperties()) {
            g.writeOmittedProperty(_name.getValue());
        }
    }

    /**
     * Alternative to {@link #serializeAsProperty} that is used when a POJO is
     * serialized as JSON Array (usually when "Shape" is forced as 'Array'):
     * the difference is that no property names are written.
     */
    @Override
    public void serializeAsElement(Object bean, JsonGenerator g, SerializationContext ctxt)
        throws Exception
    {
        final Object value = get(bean);
        if (value == null) { // nulls need specialized handling
            if (_nullSerializer != null) {
                _nullSerializer.serialize(null, g, ctxt);
            } else { // CANNOT suppress entries in tabular output
                g.writeNull();
            }
            return;
        }
        // otherwise find serializer to use
        ValueSerializer<Object> ser = _serializer;
        if (ser == null) {
            Class<?> cls = value.getClass();
            PropertySerializerMap map = _dynamicSerializers;
            ser = map.serializerFor(cls);
            if (ser == null) {
                ser = _findAndAddDynamic(map, cls, ctxt);
            }
        }
        // and then see if we must suppress certain values (default, empty)
        if (_suppressableValue != null) {
            if (MARKER_FOR_EMPTY == _suppressableValue) {
                if (ser.isEmpty(ctxt, value)) { // CANNOT suppress entries in tabular output
                    serializeAsOmittedElement(bean, g, ctxt);
                    return;
                }
            } else if (_suppressableValue.equals(value)) {
                // CANNOT suppress entries in tabular output
                serializeAsOmittedElement(bean, g, ctxt);
                return;
            }
        }
        // For non-nulls: simple check for direct cycles
        if (value == bean) {
            if (_handleSelfReference(bean, g, ctxt, ser)) {
                return;
            }
        }
        if (_typeSerializer == null) {
            ser.serialize(value, g, ctxt);
        } else {
            ser.serializeWithType(value, g, ctxt, _typeSerializer);
        }
    }

    /**
     * Method called to serialize a placeholder used in tabular output when real
     * value is not to be included (is filtered out), but when we need an entry
     * so that field indexes will not be off. Typically this should output null
     * or empty String, depending on datatype.
     */
    @Override
    public void serializeAsOmittedElement(Object bean, JsonGenerator g,
            SerializationContext prov)
        throws Exception
    {
        if (_nullSerializer != null) {
            _nullSerializer.serialize(null, g, prov);
        } else {
            g.writeNull();
        }
    }

    /*
    /**********************************************************************
    /* PropertyWriter methods (schema generation)
    /**********************************************************************
     */

    // Also part of BeanProperty implementation
    @Override
    public void depositSchemaProperty(JsonObjectFormatVisitor v,
            SerializationContext ctxt)
    {
        if (v != null) {
            if (isRequired()) {
                v.property(this);
            } else {
                v.optionalProperty(this);
            }
        }
    }

    /*
    /**********************************************************************
    /* Helper methods
    /**********************************************************************
     */

    protected ValueSerializer<Object> _findAndAddDynamic(PropertySerializerMap map,
            Class<?> rawType, SerializationContext ctxt)
    {
        JavaType t;
        if (_nonTrivialBaseType != null) {
            t = ctxt.constructSpecializedType(_nonTrivialBaseType,
                    rawType);
        } else {
            t = ctxt.constructType(rawType);
        }
        PropertySerializerMap.SerializerAndMapResult result = map.findAndAddPrimarySerializer(t, ctxt, this);
        // did we get a new map of serializers? If so, start using it
        if (map != result.map) {
            _dynamicSerializers = result.map;
        }
        return result.serializer;
    }

    /**
     * Method that can be used to access value of the property this Object
     * describes, from given bean instance.
     */
    public final Object get(Object bean) throws Exception {
        try {
            return _accessor.get().invokeExact(bean);
        } catch (Throwable e) {
            throw sneakyThrow(e);
        }
    }

    /**
     * Method called to handle a direct self-reference through this property.
     * Method can choose to indicate an error by throwing
     * {@link DatabindException}; fully handle serialization (and return
     * true); or indicate that it should be serialized normally (return false).
     * <p>
     * Default implementation will throw {@link DatabindException} if
     * {@link SerializationFeature#FAIL_ON_SELF_REFERENCES} is enabled; or
     * return <code>false</code> if it is disabled.
     *
     * @return True if method fully handled self-referential value; false if not
     *         (caller is to handle it) or {@link DatabindException} if there
     *         is no way handle it
     */
    protected boolean _handleSelfReference(Object bean, JsonGenerator g,
            SerializationContext ctxt, ValueSerializer<?> ser)
        throws JacksonException
    {
        if (!ser.usesObjectId()) {
            boolean writeAsNull = false;

            if (ctxt.isEnabled(SerializationFeature.FAIL_ON_SELF_REFERENCES)) {
                // 05-Feb-2013, tatu: Usually a problem, but NOT if we are handling
                // object id; this may be the case for BeanSerializers at least.
                if (ser instanceof BeanSerializerBase) {
                    // 09-Jul-2025, tatu: [databind#5194] Let's suppress specific case
                    //   of "cause" for Throwables
                    if (_isThrowableFieldCause(ctxt, bean)) {
                        writeAsNull = true;
                    } else {
                        ctxt.reportBadDefinition(getType(), "Direct self-reference leading to cycle");
                    }
                }
            } else {
                writeAsNull = ctxt.isEnabled(SerializationFeature.WRITE_SELF_REFERENCES_AS_NULL);
            }

            if (writeAsNull) {
                if (_nullSerializer != null) {
                    // 23-Oct-2019, tatu: Tricky part -- caller does not specify if it's
                    //   "as property" (in JSON Object) or "as element" (JSON array, via
                    //   'POJO-as-array'). And since Afterburner calls method cannot easily
                    //   start passing info either. So check generator to see...
                    //   (note: not considering ROOT context as possibility, does not seem legal)
                    if (!g.streamWriteContext().inArray()) {
                        g.writeName(_name);
                    }
                    _nullSerializer.serialize(null, g, ctxt);
                }
                return true;
            }
        }
        return false;
    }

    // Helper method to recognize `Throwable.cause` Field, which has "this" as initialized
    // value to mean "not set" (`Throwable.getCause()` translates this to `null`).
    //
    // @since 2.20
    private boolean _isThrowableFieldCause(SerializationContext ctxt, Object bean) {
        return bean instanceof Throwable
                && _member instanceof AnnotatedField
                && "cause".equals(_name.getValue())
                ;
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(40);
        sb.append("property '").append(getName()).append("' (");
        if (_accessor != null) {
            sb.append("via methodhandle ")
                    .append(_accessor);
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

    class GetterHolder extends UnreflectHandleSupplier {
        public GetterHolder() {
            super(methodType(Object.class, Object.class));
        }

        @Override
        protected MethodHandle unreflect() throws IllegalAccessException {
            if (_member instanceof AnnotatedField) {
                return MethodHandles.lookup().unreflectGetter((Field) _member.getMember());
            } else if (_member instanceof AnnotatedMethod method) {
                return MethodHandles.lookup().unreflect(method.getMember());
            } else {
                // 01-Dec-2014, tatu: Used to be illegal, but now explicitly allowed
                // for virtual props
                return null;
            }
        }
    }
}
