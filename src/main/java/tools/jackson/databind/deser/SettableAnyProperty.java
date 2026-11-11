package tools.jackson.databind.deser;

import java.lang.reflect.InvocationTargetException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import tools.jackson.core.*;
import tools.jackson.databind.*;
import tools.jackson.databind.deser.ReadableObjectId.Referring;
import tools.jackson.databind.deser.impl.NullsConstantProvider;
import tools.jackson.databind.deser.impl.NullsFailProvider;
import tools.jackson.databind.deser.jdk.JDKValueInstantiators;
import tools.jackson.databind.exc.InvalidNullException;
import tools.jackson.databind.introspect.AnnotatedField;
import tools.jackson.databind.introspect.AnnotatedMember;
import tools.jackson.databind.introspect.AnnotatedMethod;
import tools.jackson.databind.jsontype.TypeDeserializer;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;
import tools.jackson.databind.util.ClassUtil;
import tools.jackson.databind.util.IgnorePropertiesUtil;

/**
 * Class that represents a "wildcard" set method which can be used
 * to generically set values of otherwise unmapped (aka "unknown")
 * properties read from JSON content.
 */
public abstract class SettableAnyProperty
{
    /**
     * Method used for setting "any" properties, along with annotation
     * information. Retained to allow contextualization of any properties.
     */
    protected final BeanProperty _property;

    /**
     * Annotated variant is needed for JDK serialization only
     */
    protected final AnnotatedMember _setter;

    protected final boolean _setterIsField;

    protected final JavaType _type;

    protected ValueDeserializer<Object> _valueDeserializer;

    protected final TypeDeserializer _valueTypeDeserializer;
    protected final KeyDeserializer _keyDeserializer;

    /**
     * Entity used for possible translation from explicit JSON {@code null}
     * into non-null value, or for skipping the any-setter call.
     * If {@code null}, {@link #_valueDeserializer} is used.
     *
     * @since 3.3
     */
    protected final NullValueProvider _nullProvider;

    /**
     * @since 3.3
     */
    protected final boolean _skipNulls;

    /**
     * Whether explicit JSON {@code null} should fail; handled here (instead of
     * via {@link #_nullProvider}) to report the actual property name (JSON key)
     * instead of the name of any-setter method or field.
     *
     * @since 3.3
     */
    protected final boolean _failOnNulls;

    /**
     * Property-level {@code @JsonIgnoreProperties} / {@code @JsonIncludeProperties}
     * rules declared on the any-setter itself, to apply to the JSON property names it
     * accepts; {@code null} when no rules apply. A {@code Map}-valued regular property
     * gets the same treatment from its {@code MapDeserializer} during contextualization,
     * but an any-setter is fed entry by entry and never goes through one, so the check
     * is captured here instead. Assigned by {@code BeanDeserializerFactory}
     * (via {@link #withInclusionChecker}) when constructing the any-setter.
     *
     * @since 3.4
     */
    protected final IgnorePropertiesUtil.Checker _inclusionChecker;

    /*
    /**********************************************************************
    /* Life-cycle
    /**********************************************************************
     */

    public SettableAnyProperty(BeanProperty property, AnnotatedMember setter, JavaType type,
            KeyDeserializer keyDeser,
            ValueDeserializer<Object> valueDeser, TypeDeserializer typeDeser)
    {
        this(property, setter, type, keyDeser, valueDeser, typeDeser, null);
    }

    /**
     * @since 3.3
     */
    protected SettableAnyProperty(BeanProperty property, AnnotatedMember setter, JavaType type,
            KeyDeserializer keyDeser,
            ValueDeserializer<Object> valueDeser, TypeDeserializer typeDeser,
            NullValueProvider nullProvider)
    {
        _property = property;
        _setter = setter;
        _type = type;
        _valueDeserializer = valueDeser;
        _valueTypeDeserializer = typeDeser;
        _keyDeserializer = keyDeser;
        _setterIsField = setter instanceof AnnotatedField;
        _nullProvider = nullProvider;
        _skipNulls = NullsConstantProvider.isSkipper(nullProvider);
        _failOnNulls = (nullProvider instanceof NullsFailProvider);
        _inclusionChecker = null;
    }

    /**
     * Copy-constructor used by fluent factories.
     *
     * @since 3.4
     */
    protected SettableAnyProperty(SettableAnyProperty src,
            ValueDeserializer<Object> valueDeser, NullValueProvider nullProvider,
            IgnorePropertiesUtil.Checker inclusionChecker)
    {
        _property = src._property;
        _setter = src._setter;
        _type = src._type;
        _valueDeserializer = valueDeser;
        _valueTypeDeserializer = src._valueTypeDeserializer;
        _keyDeserializer = src._keyDeserializer;
        _setterIsField = src._setterIsField;
        _nullProvider = nullProvider;
        _skipNulls = NullsConstantProvider.isSkipper(nullProvider);
        _failOnNulls = (nullProvider instanceof NullsFailProvider);
        _inclusionChecker = inclusionChecker;
    }

    public static SettableAnyProperty constructForMethod(DeserializationContext ctxt,
            BeanProperty property,
            AnnotatedMember field, JavaType valueType,
            KeyDeserializer keyDeser,
            ValueDeserializer<Object> valueDeser, TypeDeserializer typeDeser) {
        return new MethodAnyProperty(property, field, valueType,
                keyDeser, valueDeser, typeDeser);
    }

    public static SettableAnyProperty constructForMapField(DeserializationContext ctxt,
            BeanProperty property,
            AnnotatedMember field, JavaType valueType,
            KeyDeserializer keyDeser,
            ValueDeserializer<Object> valueDeser, TypeDeserializer typeDeser)
    {
        Class<?> mapType = field.getRawType();
        // 02-Aug-2022, tatu: Ideally would be resolved to a concrete type by caller but
        //    alas doesn't appear to happen. Nor does `BasicDeserializerFactory` expose method
        //    for finding default or explicit mappings.
        if (mapType == Map.class) {
            mapType = LinkedHashMap.class;
        }
        ValueInstantiator vi = JDKValueInstantiators.findStdValueInstantiator(ctxt.getConfig(), mapType);
        return new MapFieldAnyProperty(property, field, valueType,
                keyDeser, valueDeser, typeDeser,
                vi);
    }

    public static SettableAnyProperty constructForJsonNodeField(DeserializationContext ctxt,
            BeanProperty property,
            AnnotatedMember field, JavaType valueType, ValueDeserializer<Object> valueDeser) {
        return new JsonNodeFieldAnyProperty(property, field, valueType,
                valueDeser,
                ctxt.getNodeFactory());
    }

    /**
     * @since 2.18
     */
    public static SettableAnyProperty constructForMapParameter(DeserializationContext ctxt,
            BeanProperty property, AnnotatedMember field, JavaType valueType, KeyDeserializer keyDeser,
            ValueDeserializer<Object> valueDeser, TypeDeserializer typeDeser, int parameterIndex
    ) {
        Class<?> mapType = field.getRawType();
        // 02-Aug-2022, tatu: Ideally would be resolved to a concrete type by caller but
        //    alas doesn't appear to happen. Nor does `BasicDeserializerFactory` expose method
        //    for finding default or explicit mappings.
        if (mapType == Map.class) {
            mapType = LinkedHashMap.class;
        }
        ValueInstantiator vi = JDKValueInstantiators.findStdValueInstantiator(ctxt.getConfig(), mapType);
        return new MapParameterAnyProperty(property, field, valueType, keyDeser, valueDeser, typeDeser, vi, parameterIndex);
    }

    public static SettableAnyProperty constructForJsonNodeParameter(DeserializationContext ctxt, BeanProperty prop,
            AnnotatedMember mutator, JavaType valueType, ValueDeserializer<Object> valueDeser, int parameterIndex
    ) {
        return new JsonNodeParameterAnyProperty(prop, mutator, valueType, valueDeser, ctxt.getNodeFactory(), parameterIndex);
    }


    // Abstract @since 2.14
    public abstract SettableAnyProperty withValueDeserializer(ValueDeserializer<Object> deser);

    /**
     * Fluent factory for creating instance that uses specified {@link NullValueProvider}
     * for handling explicit JSON {@code null} values.
     *<p>
     * Default implementation returns {@code this} (that is, ignores null-handling
     * configuration) for backwards-compatibility with custom sub-classes; all
     * standard implementations override it.
     *
     * @since 3.3
     */
    public SettableAnyProperty withNullProvider(NullValueProvider nullProvider) {
        return this;
    }

    /**
     * Fluent factory for creating instance that uses specified inclusion checker
     * (see {@link #_inclusionChecker}).
     *<p>
     * Default implementation returns {@code this} (that is, ignores inclusion
     * rules) for backwards-compatibility with custom sub-classes; all
     * standard implementations override it.
     *
     * @since 3.4
     */
    protected SettableAnyProperty withInclusionChecker(IgnorePropertiesUtil.Checker inclusionChecker) {
        return this;
    }

    public void fixAccess(DeserializationConfig config) {
        _setter.fixAccess(
                config.isEnabled(MapperFeature.OVERRIDE_PUBLIC_ACCESS_MODIFIERS));
    }

    /*
    /**********************************************************************
    /* Public API, accessors
    /**********************************************************************
     */

    public BeanProperty getProperty() { return _property; }

    public boolean hasValueDeserializer() { return (_valueDeserializer != null); }

    /**
     * @since 3.3
     */
    public ValueDeserializer<Object> getValueDeserializer() { return _valueDeserializer; }

    public JavaType getType() { return _type; }

    public String getPropertyName() { return _property.getName(); }

    /**
     * Method called before deserializing value of property {@code propName}
     * to handle explicit JSON {@code null} for "skip" and "fail" null-handling
     * settings; other settings are handled by
     * {@link #deserialize(JsonParser, DeserializationContext)}.
     *
     * @return {@code true} if the value is an explicit {@code null} that is to
     *    be skipped (not passed to any-setter); {@code false} otherwise
     *
     * @throws InvalidNullException if the value is an explicit {@code null}
     *    and nulls are not allowed; reported using {@code propName} (JSON key)
     *    instead of the name of the any-setter method or field
     *
     * @since 3.3
     */
    public boolean skipOrFailOnNull(JsonParser p, DeserializationContext ctxt,
            String propName)
        throws JacksonException
    {
        if (p.hasToken(JsonToken.VALUE_NULL)) {
            if (_skipNulls) {
                return true;
            }
            if (_failOnNulls) {
                throw InvalidNullException.from(ctxt, PropertyName.construct(propName), _type);
            }
        }
        return false;
    }

    /**
     * Method called before deserializing value of property {@code propName} to check
     * whether the value is to be skipped instead of being passed in: either because
     * the name is excluded by {@code @JsonIgnoreProperties} / {@code @JsonIncludeProperties}
     * declared on the any-setter, in which case the value is skipped here, or because
     * it is an explicit {@code null} to skip (see {@link #skipOrFailOnNull}).
     *
     * @return {@code true} if the value is to be skipped (and has been consumed);
     *    {@code false} if it is to be deserialized and passed in
     *
     * @since 3.4
     */
    public boolean skipValue(JsonParser p, DeserializationContext ctxt, String propName)
        throws JacksonException
    {
        if ((_inclusionChecker != null) && _inclusionChecker.shouldIgnore(propName)) {
            p.skipChildren();
            return true;
        }
        return skipOrFailOnNull(p, ctxt, propName);
    }

    /**
     * Accessor for parameterIndex.
     * @return -1 if not a parameterized setter, otherwise index of parameter
     *
     * @since 2.18
     */
    public int getParameterIndex() { return -1; }

    /**
     * Method called to check whether this property is field
     *
     * @since 2.18.1
     */
    public boolean isFieldType() { return _setterIsField; }

    /**
     * Method called to check whether this property is method
     *
     * @return 2.18.2
     */
    public boolean isSetterType() { return _setter instanceof AnnotatedMethod; }

    /**
     * Create an instance of value to pass through Creator parameter.
     *
     * @since 2.18
     */
    public Object createParameterObject() {
        throw new UnsupportedOperationException("Cannot call createParameterObject() on " + getClass().getName());
    }

    /*
    /**********************************************************************
    /* Public API, deserialization
    /**********************************************************************
     */

    /**
     * Method called to deserialize appropriate value, given parser (and
     * context), and set it using appropriate method (a setter method).
     */
    public void deserializeAndSet(JsonParser p, DeserializationContext ctxt,
            Object instance, String propName)
        throws JacksonException
    {
        try {
            if (skipValue(p, ctxt, propName)) {
                return;
            }
            Object key = (_keyDeserializer == null) ? propName
                    : _keyDeserializer.deserializeKey(propName, ctxt);
            set(ctxt, instance, key, deserialize(p, ctxt));
        } catch (UnresolvedForwardReference reference) {
            if (_valueDeserializer.getObjectIdReader(ctxt) == null) {
                throw DatabindException.from(p, "Unresolved forward reference but no identity info.", reference);
            }
            AnySetterReferring referring = new AnySetterReferring(this, reference,
                    _type.getRawClass(), instance, propName);
            reference.getRoid().appendReferring(referring);
        }
    }

    public Object deserialize(JsonParser p, DeserializationContext ctxt) throws JacksonException
    {
        JsonToken t = p.currentToken();
        if (t == JsonToken.VALUE_NULL) {
            return (_nullProvider == null) ? _valueDeserializer.getNullValue(ctxt)
                    : _nullProvider.getNullValue(ctxt);
        }
        if (_valueTypeDeserializer != null) {
            return _valueDeserializer.deserializeWithType(p, ctxt, _valueTypeDeserializer);
        }
        return _valueDeserializer.deserialize(p, ctxt);
    }

    public void set(DeserializationContext ctxt, Object instance, Object propName, Object value) throws JacksonException
    {
        try {
            _set(ctxt, instance, propName, value);
        } catch (JacksonException e) {
            throw e;
        } catch (Exception e) {
            _throwAsIOE(ctxt, e, propName, value);
        }
    }

    protected abstract void _set(DeserializationContext ctxt, Object instance, Object propName, Object value)
        throws Exception;

    /*
    /**********************************************************************
    /* Helper methods
    /**********************************************************************
     */

    /**
     * @param t Exception to re-throw or wrap
     * @param propName Name of property (from Json input) to set
     * @param value Value of the property
     */
    protected void _throwAsIOE(DeserializationContext ctxt, Throwable t, Object propName, Object value)
        throws JacksonException
    {
        // [databind#6303]: wrap type mismatch, `IllegalArgumentException` from setter
        SettableBeanProperty._wrapSetterProblem(ctxt.getParser(), t, value, _type,
                "\"any-property\" '"+propName+"' of class "+getClassName());
        ClassUtil.throwIfJacksonE(t);
        ClassUtil.throwIfRTE(t);
        // let's wrap the innermost problem
        // 11-Apr-2025: [databind#4603] no more general unwrapping
        // Throwable t = ClassUtil.getRootCause(e);
        // ... just one:
        if (t instanceof InvocationTargetException ite) {
            t = ite.getTargetException();
            ClassUtil.throwIfRTE(t);
            ClassUtil.throwIfJacksonE(t);
        }
        throw DatabindException.from(ctxt, ClassUtil.exceptionMessage(t), t);
    }

    private String getClassName() { return ClassUtil.nameOf(_setter.getDeclaringClass()); }

    @Override public String toString() { return "[any property on class "+getClassName()+"]"; }

    private static class AnySetterReferring extends Referring {
        private final SettableAnyProperty _parent;
        private final Object _pojo;
        private final String _propName;

        public AnySetterReferring(SettableAnyProperty parent,
                UnresolvedForwardReference reference, Class<?> type, Object instance, String propName)
        {
            super(reference, type);
            _parent = parent;
            _pojo = instance;
            _propName = propName;
        }

        @Override
        public void handleResolvedForwardReference(DeserializationContext ctxt, Object id, Object value)
            throws JacksonException
        {
            if (!hasId(id)) {
                throw new IllegalArgumentException("Trying to resolve a forward reference with id [" + id.toString()
                        + "] that wasn't previously registered.");
            }
            _parent.set(ctxt, _pojo, _propName, value);
        }
    }

    /*
    /**********************************************************************
    /* Concrete implementations
    /**********************************************************************
     */

    protected static class MethodAnyProperty extends SettableAnyProperty
    {
        public MethodAnyProperty(BeanProperty property,
                AnnotatedMember field, JavaType valueType,
                KeyDeserializer keyDeser,
                ValueDeserializer<Object> valueDeser, TypeDeserializer typeDeser) {
            this(property, field, valueType, keyDeser, valueDeser, typeDeser, null);
        }

        /**
         * @since 3.3
         */
        protected MethodAnyProperty(BeanProperty property,
                AnnotatedMember field, JavaType valueType,
                KeyDeserializer keyDeser,
                ValueDeserializer<Object> valueDeser, TypeDeserializer typeDeser,
                NullValueProvider nullProvider) {
            super(property, field, valueType,
                    keyDeser, valueDeser, typeDeser, nullProvider);
        }

        /**
         * @since 3.4
         */
        protected MethodAnyProperty(MethodAnyProperty src,
                ValueDeserializer<Object> valueDeser, NullValueProvider nullProvider,
                IgnorePropertiesUtil.Checker inclusionChecker) {
            super(src, valueDeser, nullProvider, inclusionChecker);
        }

        @Override
        protected void _set(DeserializationContext ctxt, Object instance, Object propName, Object value)
            throws Exception
        {
            // note: cannot use 'setValue()' due to taking 2 args
            ((AnnotatedMethod) _setter).callOnWith(instance, propName, value);
        }

        @Override
        public SettableAnyProperty withValueDeserializer(ValueDeserializer<Object> deser) {
            return new MethodAnyProperty(this, deser, _nullProvider, _inclusionChecker);
        }

        @Override
        public SettableAnyProperty withNullProvider(NullValueProvider nullProvider) {
            return new MethodAnyProperty(this, _valueDeserializer, nullProvider, _inclusionChecker);
        }

        @Override
        protected SettableAnyProperty withInclusionChecker(IgnorePropertiesUtil.Checker inclusionChecker) {
            return new MethodAnyProperty(this, _valueDeserializer, _nullProvider, inclusionChecker);
        }
    }

    /**
     * @since 2.14
     */
    protected static class MapFieldAnyProperty extends SettableAnyProperty
    {
        protected final ValueInstantiator _valueInstantiator;

        public MapFieldAnyProperty(BeanProperty property,
                AnnotatedMember field, JavaType valueType,
                KeyDeserializer keyDeser,
                ValueDeserializer<Object> valueDeser, TypeDeserializer typeDeser,
                ValueInstantiator inst) {
            this(property, field, valueType, keyDeser, valueDeser, typeDeser, inst, null);
        }

        /**
         * @since 3.3
         */
        protected MapFieldAnyProperty(BeanProperty property,
                AnnotatedMember field, JavaType valueType,
                KeyDeserializer keyDeser,
                ValueDeserializer<Object> valueDeser, TypeDeserializer typeDeser,
                ValueInstantiator inst, NullValueProvider nullProvider) {
            super(property, field, valueType,
                    keyDeser, valueDeser, typeDeser, nullProvider);
            _valueInstantiator = inst;
        }

        /**
         * @since 3.4
         */
        protected MapFieldAnyProperty(MapFieldAnyProperty src,
                ValueDeserializer<Object> valueDeser, NullValueProvider nullProvider,
                IgnorePropertiesUtil.Checker inclusionChecker) {
            super(src, valueDeser, nullProvider, inclusionChecker);
            _valueInstantiator = src._valueInstantiator;
        }

        @Override
        public SettableAnyProperty withValueDeserializer(ValueDeserializer<Object> deser) {
            return new MapFieldAnyProperty(this, deser, _nullProvider, _inclusionChecker);
        }

        @Override
        public SettableAnyProperty withNullProvider(NullValueProvider nullProvider) {
            return new MapFieldAnyProperty(this, _valueDeserializer, nullProvider, _inclusionChecker);
        }

        @Override
        protected SettableAnyProperty withInclusionChecker(IgnorePropertiesUtil.Checker inclusionChecker) {
            return new MapFieldAnyProperty(this, _valueDeserializer, _nullProvider, inclusionChecker);
        }

        @SuppressWarnings("unchecked")
        @Override
        protected void _set(DeserializationContext ctxt, Object instance, Object propName, Object value)
            throws Exception
        {
            AnnotatedField field = (AnnotatedField) _setter;
            Map<Object,Object> val = (Map<Object,Object>) field.getValue(instance);
            // 01-Aug-2022, tatu: [databind#3559] Will try to create and assign an
            //    instance.
            if (val == null) {
                val = _createAndSetMap(null, field, instance, propName);
            }
            // add the property key and value
            val.put(propName, value);
        }

        @SuppressWarnings("unchecked")
        protected Map<Object, Object> _createAndSetMap(DeserializationContext ctxt, AnnotatedField field,
                Object instance, Object propName)
            throws JacksonException
        {
            if (_valueInstantiator == null) {
                throw DatabindException.from(ctxt, "Cannot create an instance of %s for use as \"any-setter\" '%s'".formatted(
                        ClassUtil.nameOf(_type.getRawClass()), _property.getName()));
            }
            Map<Object,Object> map = (Map<Object,Object>) _valueInstantiator.createUsingDefault(ctxt);
            field.setValue(instance, map);
            return map;
        }
    }

    protected static class JsonNodeFieldAnyProperty extends SettableAnyProperty
    {
        protected final JsonNodeFactory _nodeFactory;

        public JsonNodeFieldAnyProperty(BeanProperty property,
                AnnotatedMember field, JavaType valueType,
                ValueDeserializer<Object> valueDeser,
                JsonNodeFactory nodeFactory) {
            this(property, field, valueType, valueDeser, nodeFactory, null);
        }

        /**
         * @since 3.3
         */
        protected JsonNodeFieldAnyProperty(BeanProperty property,
                AnnotatedMember field, JavaType valueType,
                ValueDeserializer<Object> valueDeser,
                JsonNodeFactory nodeFactory, NullValueProvider nullProvider) {
            super(property, field, valueType, null, valueDeser, null, nullProvider);
            _nodeFactory = nodeFactory;
        }

        /**
         * @since 3.4
         */
        protected JsonNodeFieldAnyProperty(JsonNodeFieldAnyProperty src,
                NullValueProvider nullProvider, IgnorePropertiesUtil.Checker inclusionChecker) {
            super(src, src._valueDeserializer, nullProvider, inclusionChecker);
            _nodeFactory = src._nodeFactory;
        }

        // Let's override since this is much simpler with JsonNodes
        @Override
        public void deserializeAndSet(JsonParser p, DeserializationContext ctxt,
                Object instance, String propName)
            throws JacksonException
        {
            if (skipValue(p, ctxt, propName)) {
                return;
            }
            setProperty(instance, propName, (JsonNode) deserialize(p, ctxt));
        }

        @Override
        protected void _set(DeserializationContext ctxt, Object instance, Object propName, Object value) throws Exception {
            setProperty(instance, (String) propName, (JsonNode) value);
        }

        protected void setProperty(Object instance, String propName, JsonNode value)
            throws JacksonException
        {
            AnnotatedField field = (AnnotatedField) _setter;
            Object val0 = field.getValue(instance);
            ObjectNode objectNode;

            if (val0 == null) {
                objectNode = _nodeFactory.objectNode();
                field.setValue(instance, objectNode);
            } else if (!(val0 instanceof ObjectNode)) {
                throw DatabindException.from((DeserializationContext) null, "Value \"any-setter\" '%s' not `ObjectNode` but %s".formatted(
                        getPropertyName(),
                        ClassUtil.nameOf(val0.getClass())));
            } else {
                objectNode = (ObjectNode) val0;
            }
            // add the property key and value
            objectNode.set(propName, value);
        }

        // Should not get called but...
        @Override
        public SettableAnyProperty withValueDeserializer(ValueDeserializer<Object> deser) {
            return this;
        }

        @Override
        public SettableAnyProperty withNullProvider(NullValueProvider nullProvider) {
            return new JsonNodeFieldAnyProperty(this, nullProvider, _inclusionChecker);
        }

        @Override
        protected SettableAnyProperty withInclusionChecker(IgnorePropertiesUtil.Checker inclusionChecker) {
            return new JsonNodeFieldAnyProperty(this, _nullProvider, inclusionChecker);
        }
    }

    /**
     * [databind#562] Allow @JsonAnySetter on Creator constructor
     */
    protected static class MapParameterAnyProperty extends SettableAnyProperty
    {
        protected final ValueInstantiator _valueInstantiator;

        protected final int _parameterIndex;

        public MapParameterAnyProperty(BeanProperty property, AnnotatedMember field, JavaType valueType,
                KeyDeserializer keyDeser, ValueDeserializer<Object> valueDeser, TypeDeserializer typeDeser,
                ValueInstantiator inst, int parameterIndex)
        {
            this(property, field, valueType, keyDeser, valueDeser, typeDeser, inst, parameterIndex, null);
        }

        /**
         * @since 3.3
         */
        protected MapParameterAnyProperty(BeanProperty property, AnnotatedMember field, JavaType valueType,
                KeyDeserializer keyDeser, ValueDeserializer<Object> valueDeser, TypeDeserializer typeDeser,
                ValueInstantiator inst, int parameterIndex, NullValueProvider nullProvider)
        {
            super(property, field, valueType, keyDeser, valueDeser, typeDeser, nullProvider);
            _valueInstantiator = Objects.requireNonNull(inst, "ValueInstantiator for MapParameterAnyProperty cannot be `null`");
            _parameterIndex = parameterIndex;
        }

        /**
         * @since 3.4
         */
        protected MapParameterAnyProperty(MapParameterAnyProperty src,
                ValueDeserializer<Object> valueDeser, NullValueProvider nullProvider,
                IgnorePropertiesUtil.Checker inclusionChecker)
        {
            super(src, valueDeser, nullProvider, inclusionChecker);
            _valueInstantiator = src._valueInstantiator;
            _parameterIndex = src._parameterIndex;
        }

        @Override
        public SettableAnyProperty withValueDeserializer(ValueDeserializer<Object> deser)
        {
            return new MapParameterAnyProperty(this, deser, _nullProvider, _inclusionChecker);
        }

        @Override
        public SettableAnyProperty withNullProvider(NullValueProvider nullProvider)
        {
            return new MapParameterAnyProperty(this, _valueDeserializer, nullProvider, _inclusionChecker);
        }

        @Override
        protected SettableAnyProperty withInclusionChecker(IgnorePropertiesUtil.Checker inclusionChecker)
        {
            return new MapParameterAnyProperty(this, _valueDeserializer, _nullProvider, inclusionChecker);
        }

        @SuppressWarnings("unchecked")
        @Override
        protected void _set(DeserializationContext ctxt, Object instance, Object propName, Object value)
        {
            ((Map<Object, Object>) instance).put(propName, value);
        }

        @Override
        public int getParameterIndex() { return _parameterIndex; }

        @Override
        public Object createParameterObject() { return new LinkedHashMap<>(); }
    }

    /**
     * [databind#562] Allow @JsonAnySetter on Creator constructor
     */
    protected static class JsonNodeParameterAnyProperty extends SettableAnyProperty
    {
        protected final JsonNodeFactory _nodeFactory;

        protected final int _parameterIndex;

        public JsonNodeParameterAnyProperty(BeanProperty property, AnnotatedMember field, JavaType valueType,
                ValueDeserializer<Object> valueDeser, JsonNodeFactory nodeFactory, int parameterIndex)
        {
            this(property, field, valueType, valueDeser, nodeFactory, parameterIndex, null);
        }

        /**
         * @since 3.3
         */
        protected JsonNodeParameterAnyProperty(BeanProperty property, AnnotatedMember field, JavaType valueType,
                ValueDeserializer<Object> valueDeser, JsonNodeFactory nodeFactory, int parameterIndex,
                NullValueProvider nullProvider)
        {
            super(property, field, valueType, null, valueDeser, null, nullProvider);
            _nodeFactory = nodeFactory;
            _parameterIndex = parameterIndex;
        }

        /**
         * @since 3.4
         */
        protected JsonNodeParameterAnyProperty(JsonNodeParameterAnyProperty src,
                NullValueProvider nullProvider, IgnorePropertiesUtil.Checker inclusionChecker)
        {
            super(src, src._valueDeserializer, nullProvider, inclusionChecker);
            _nodeFactory = src._nodeFactory;
            _parameterIndex = src._parameterIndex;
        }

        @Override
        protected void _set(DeserializationContext ctxt, Object instance, Object propName, Object value)
            throws Exception
        {
            ((ObjectNode) instance).set((String) propName, (JsonNode) value);
        }

        // Should not get called but...
        @Override
        public SettableAnyProperty withValueDeserializer(ValueDeserializer<Object> deser) {
            throw new UnsupportedOperationException("Cannot call withValueDeserializer() on " + getClass().getName());
        }

        @Override
        public SettableAnyProperty withNullProvider(NullValueProvider nullProvider) {
            return new JsonNodeParameterAnyProperty(this, nullProvider, _inclusionChecker);
        }

        @Override
        protected SettableAnyProperty withInclusionChecker(IgnorePropertiesUtil.Checker inclusionChecker) {
            return new JsonNodeParameterAnyProperty(this, _nullProvider, inclusionChecker);
        }

        @Override
        public int getParameterIndex() { return _parameterIndex; }

        @Override
        public Object createParameterObject() { return _nodeFactory.objectNode(); }
    }
}
