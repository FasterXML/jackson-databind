package com.fasterxml.jackson.databind.deser;

import java.io.IOException;
import java.lang.annotation.Annotation;

import com.fasterxml.jackson.core.*;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.deser.impl.FailingDeserializer;
import com.fasterxml.jackson.databind.deser.impl.NullsConstantProvider;
import com.fasterxml.jackson.databind.introspect.*;
import com.fasterxml.jackson.databind.jsonFormatVisitors.JsonObjectFormatVisitor;
import com.fasterxml.jackson.databind.jsontype.TypeDeserializer;
import com.fasterxml.jackson.databind.util.Annotations;
import com.fasterxml.jackson.databind.util.ClassUtil;
import com.fasterxml.jackson.databind.util.ViewMatcher;

/**
 * Base class for deserializable properties of a bean: contains
 * both type and name definitions, and reflection-based set functionality.
 * Concrete sub-classes implement details, so that field- and
 * setter-backed properties, as well as a few more esoteric variations,
 * can be handled.
 */
@SuppressWarnings("serial")
public abstract class SettableBeanProperty
    extends ConcreteBeanPropertyBase
    implements java.io.Serializable
{
    /**
     * To avoid nasty NPEs, let's use a placeholder for _valueDeserializer,
     * if real deserializer is not (yet) available.
     * 
     * @since 2.2
     */
    protected static final JsonDeserializer<Object> MISSING_VALUE_DESERIALIZER = new FailingDeserializer(
            "No _valueDeserializer assigned");

    /**
     * Logical name of the property (often but not always derived
     * from the setter method name)
     */
    protected final PropertyName _propName;

    /**
     * Base type for property; may be a supertype of actual value.
     */
    protected final JavaType _type;

    /**
     * @since 2.2
     */
    protected final PropertyName _wrapperName;

    /**
     * Class that contains this property (either class that declares
     * the property or one of its subclasses), class that is
     * deserialized using deserializer that contains this property.
     */
    protected final transient Annotations _contextAnnotations;

    /**
     * Deserializer used for handling property value.
     *<p>
     * NOTE: has been immutable since 2.3
     */
    protected final JsonDeserializer<Object> _valueDeserializer;

    /**
     * If value will contain type information (to support
     * polymorphic handling), this is the type deserializer
     * used to handle type resolution.
     */
    protected final TypeDeserializer _valueTypeDeserializer;

    /**
     * Entity used for possible translation from `null` into non-null
     * value of type of this property.
     * Often same as <code>_valueDeserializer</code>, but not always.
     *
     * @since 2.9
     */
    protected final NullValueProvider _nullProvider;

    /*
    /**********************************************************
    /* Configuration that is not yet immutable; generally assigned
    /* during initialization process but cannot be passed to
    /* constructor.
    /**********************************************************
     */

    /**
     * If property represents a managed (forward) reference, we will need
     * the name of reference for later linking.
     *<p>
     * TODO: should try to make immutable.
     */
    protected String _managedReferenceName;

    /**
     * This is the information for object identity associated with the property.
     * <p>
     * TODO: should try to make immutable.
     */
    protected ObjectIdInfo _objectIdInfo;

    /**
     * Helper object used for checking whether this property is to
     * be included in the active view, if property is view-specific;
     * null otherwise.
     *<p>
     * TODO: should try to make immutable.
     */
    protected ViewMatcher _viewMatcher;

    /**
     * Index of property (within all property of a bean); assigned
     * when all properties have been collected. Order of entries
     * is arbitrary, but once indexes are assigned they are not
     * changed.
     *<p>
     * TODO: should try to make immutable if at all possible
     */
    protected int _propertyIndex = -1;

    /*
    /**********************************************************
    /* Life-cycle (construct & configure)
    /**********************************************************
     */

    protected SettableBeanProperty(BeanPropertyDefinition propDef,
            JavaType type, TypeDeserializer typeDeser, Annotations contextAnnotations)
    {
        this(propDef.getFullName(), type, propDef.getWrapperName(), typeDeser,
                contextAnnotations, propDef.getMetadata());
    }

    protected SettableBeanProperty(PropertyName propName, JavaType type, PropertyName wrapper,
            TypeDeserializer typeDeser, Annotations contextAnnotations,
            PropertyMetadata metadata)
    {
        super(metadata);
        // 09-Jan-2009, tatu: Intern()ing makes sense since Jackson parsed
        //  field names are (usually) interned too, hence lookups will be faster.
        // 23-Oct-2009, tatu: should this be disabled wrt [JACKSON-180]?
        //   Probably need not, given that namespace of field/method names
        //   is not unbounded, unlike potential JSON names.
        if (propName == null) {
            _propName = PropertyName.NO_NAME;
        } else {
            _propName = propName.internSimpleName();
        }
        _type = type;
        _wrapperName = wrapper;
        _contextAnnotations = contextAnnotations;
        _viewMatcher = null;

        // 30-Jan-2012, tatu: Important: contextualize TypeDeserializer now...
        if (typeDeser != null) {
            typeDeser = typeDeser.forProperty(this);
        }
        _valueTypeDeserializer = typeDeser;
        _valueDeserializer = MISSING_VALUE_DESERIALIZER;
        _nullProvider = MISSING_VALUE_DESERIALIZER;
    }

    /**
     * Constructor only used by {@link com.fasterxml.jackson.databind.deser.impl.ObjectIdValueProperty}.
     * 
     * @since 2.3
     */
    protected SettableBeanProperty(PropertyName propName, JavaType type, 
            PropertyMetadata metadata, JsonDeserializer<Object> valueDeser)
    {
        super(metadata);
        // as with above ctor, intern()ing probably fine
        if (propName == null) {
            _propName = PropertyName.NO_NAME;
        } else {
            _propName = propName.internSimpleName();
        }
        _type = type;
        _wrapperName = null;
        _contextAnnotations = null;
        _viewMatcher = null;
        _valueTypeDeserializer = null;
        _valueDeserializer = valueDeser;
        // 29-Jan-2017, tatu: Presumed to be irrelevant for ObjectId values...
        _nullProvider = valueDeser;
    }

    /**
     * Basic copy-constructor for sub-classes to use.
     */
    protected SettableBeanProperty(SettableBeanProperty src)
    {
        super(src);
        _propName = src._propName;
        _type = src._type;
        _wrapperName = src._wrapperName;
        _contextAnnotations = src._contextAnnotations;
        _valueDeserializer = src._valueDeserializer;
        _valueTypeDeserializer = src._valueTypeDeserializer;
        _managedReferenceName = src._managedReferenceName;
        _propertyIndex = src._propertyIndex;
        _viewMatcher = src._viewMatcher;
        _nullProvider = src._nullProvider;
    }

    /**
     * Copy-with-deserializer-change constructor for sub-classes to use.
     */
    @SuppressWarnings("unchecked")
    protected SettableBeanProperty(SettableBeanProperty src,
            JsonDeserializer<?> deser, NullValueProvider nuller)
    {
        super(src);
        _propName = src._propName;
        _type = src._type;
        _wrapperName = src._wrapperName;
        _contextAnnotations = src._contextAnnotations;
        _valueTypeDeserializer = src._valueTypeDeserializer;
        _managedReferenceName = src._managedReferenceName;
        _propertyIndex = src._propertyIndex;

        if (deser == null) {
            _valueDeserializer = MISSING_VALUE_DESERIALIZER;
        } else {
            _valueDeserializer = (JsonDeserializer<Object>) deser;
        }
        _viewMatcher = src._viewMatcher;
        // 29-Jan-2017, tatu: Bit messy, but for now has to do...
        if (nuller == MISSING_VALUE_DESERIALIZER) {
            nuller = _valueDeserializer;
        }
        _nullProvider = nuller;
    }

    /**
     * Copy-with-deserializer-change constructor for sub-classes to use.
     */
    protected SettableBeanProperty(SettableBeanProperty src, PropertyName newName)
    {
        super(src);
        _propName = newName;
        _type = src._type;
        _wrapperName = src._wrapperName;
        _contextAnnotations = src._contextAnnotations;
        _valueDeserializer = src._valueDeserializer;
        _valueTypeDeserializer = src._valueTypeDeserializer;
        _managedReferenceName = src._managedReferenceName;
        _propertyIndex = src._propertyIndex;
        _viewMatcher = src._viewMatcher;
        _nullProvider = src._nullProvider;
    }

    /**
     * Fluent factory method for constructing and returning a new instance
     * with specified value deserializer.
     * Note that this method should NOT change configuration of this instance.
     * 
     * @param deser Deserializer to assign to the new property instance
     * 
     * @return Newly constructed instance, if value deserializer differs from the
     *   one used for this instance; or 'this' if not.
     */
    public abstract SettableBeanProperty withValueDeserializer(JsonDeserializer<?> deser);

    /**
     * Fluent factory method for constructing and returning a new instance
     * with specified property name.
     * Note that this method should NOT change configuration of this instance.
     * 
     * @param newName Name to use for the new instance.
     * 
     * @return Newly constructed instance, if property name differs from the
     *   one used for this instance; or 'this' if not.
     */
    public abstract SettableBeanProperty withName(PropertyName newName);

    /**
     * @since 2.3
     */
    public SettableBeanProperty withSimpleName(String simpleName) {
        PropertyName n = (_propName == null)
                ? new PropertyName(simpleName) : _propName.withSimpleName(simpleName);
        return (n == _propName) ? this : withName(n);
    }

    /**
     * @since 2.9
     */
    public abstract SettableBeanProperty withNullProvider(NullValueProvider nva);

    public void setManagedReferenceName(String n) {
        _managedReferenceName = n;
    }

    public void setObjectIdInfo(ObjectIdInfo objectIdInfo) {
        _objectIdInfo = objectIdInfo;
    }

    public void setViews(Class<?>[] views) {
        if (views == null) {
            _viewMatcher = null;
        } else {
            _viewMatcher = ViewMatcher.construct(views);
        }
    }

    /**
     * Method used to assign index for property.
     */
    public void assignIndex(int index) {
        if (_propertyIndex != -1) {
            throw new IllegalStateException("Property '"+getName()+"' already had index ("+_propertyIndex+"), trying to assign "+index);
        }
        _propertyIndex = index;
    }

    /**
     * Method called to ensure that the mutator has proper access rights to
     * be called, as per configuration. Overridden by implementations that
     * have mutators that require access, fields and setters.
     *
     * @since 2.8.3
     */
    public void fixAccess(DeserializationConfig config) {
        ;
    }

    /**
     * @since 2.9.4
     */
    public void markAsIgnorable() { }

    /**
     * @since 2.9.4
     */
    public boolean isIgnorable() { return false; }

    /*
    /**********************************************************
    /* BeanProperty impl
    /**********************************************************
     */
    
    @Override
    public final String getName() {
        return _propName.getSimpleName();
    }

    @Override
    public PropertyName getFullName() {
        return _propName;
    }

    @Override
    public JavaType getType() { return _type; }

    @Override
    public PropertyName getWrapperName() {
        return _wrapperName;
    }
    
    @Override
    public abstract AnnotatedMember getMember();

    @Override
    public abstract <A extends Annotation> A getAnnotation(Class<A> acls);

    @Override
    public <A extends Annotation> A getContextAnnotation(Class<A> acls) {
        return _contextAnnotations.get(acls);
    }

    @Override
    public void depositSchemaProperty(JsonObjectFormatVisitor objectVisitor,
            SerializerProvider provider)
        throws JsonMappingException
    {
        if (isRequired()) {
            objectVisitor.property(this); 
        } else {
            objectVisitor.optionalProperty(this);
        }
    }

    /*
    /**********************************************************
    /* Accessors
    /**********************************************************
     */

    protected Class<?> getDeclaringClass() {
        return getMember().getDeclaringClass();
    }

    public String getManagedReferenceName() { return _managedReferenceName; }

    public ObjectIdInfo getObjectIdInfo() { return _objectIdInfo; }

    public boolean hasValueDeserializer() {
        return (_valueDeserializer != null) && (_valueDeserializer != MISSING_VALUE_DESERIALIZER);
    }

    public boolean hasValueTypeDeserializer() { return (_valueTypeDeserializer != null); }

    public JsonDeserializer<Object> getValueDeserializer() {
        JsonDeserializer<Object> deser = _valueDeserializer;
        if (deser == MISSING_VALUE_DESERIALIZER) {
            return null;
        }
        return deser;
    }

    public TypeDeserializer getValueTypeDeserializer() { return _valueTypeDeserializer; }

    /**
     * @since 2.9
     */
    public NullValueProvider getNullValueProvider() { return _nullProvider; }

    public boolean visibleInView(Class<?> activeView) {
        return (_viewMatcher == null) || _viewMatcher.isVisibleForView(activeView);
    }

    public boolean hasViews() { return _viewMatcher != null; }
    
    /**
     * Method for accessing unique index of this property; indexes are
     * assigned once all properties of a {@link BeanDeserializer} have
     * been collected.
     * 
     * @return Index of this property
     */
    public int getPropertyIndex() { return _propertyIndex; }

    /**
     * Method for accessing index of the creator property: for other
     * types of properties will simply return -1.
     * 
     * @since 2.1
     */
    public int getCreatorIndex() {
        // changed from 'return -1' in 2.7.9 / 2.8.7
        throw new IllegalStateException(String.format(
                "Internal error: no creator index for property '%s' (of type %s)",
                this.getName(), getClass().getName()));
    }

    /**
     * Accessor for id of injectable value, if this bean property supports
     * value injection.
     */
    public Object getInjectableValueId() { return null; }
    
    /*
    /**********************************************************
    /* Public API
    /**********************************************************
     */

    /**
     * Method called to deserialize appropriate value, given parser (and
     * context), and set it using appropriate mechanism.
     * Pre-condition is that passed parser must point to the first token
     * that should be consumed to produce the value (the only value for
     * scalars, multiple for Objects and Arrays).
     */
    public abstract void deserializeAndSet(JsonParser p,
    		DeserializationContext ctxt, Object instance) throws IOException;

	/**
	 * Alternative to {@link #deserializeAndSet} that returns
	 * either return value of setter method called (if one is),
	 * or null to indicate that no return value is available.
	 * Mostly used to support Builder style deserialization.
	 *
	 * @since 2.0
	 */
    public abstract Object deserializeSetAndReturn(JsonParser p,
    		DeserializationContext ctxt, Object instance) throws IOException;

    /**
     * Method called to assign given value to this property, on
     * specified Object.
     *<p>
     * Note: this is an optional operation, not supported by all
     * implementations, creator-backed properties for example do not
     * support this method.
     */
    public abstract void set(Object instance, Object value) throws IOException;

    /**
     * Method called to assign given value to this property, on
     * specified Object, and return whatever delegating accessor
     * returned (if anything)
     *<p>
     * Note: this is an optional operation, not supported by all
     * implementations, creator-backed properties for example do not
     * support this method.
     */
    public abstract Object setAndReturn(Object instance, Object value) throws IOException;
    
    /**
     * This method is needed by some specialized bean deserializers,
     * and also called by some {@link #deserializeAndSet} implementations.
     *<p>
     * Pre-condition is that passed parser must point to the first token
     * that should be consumed to produce the value (the only value for
     * scalars, multiple for Objects and Arrays).
     *<p> 
     * Note that this method is final for performance reasons: to override
     * functionality you must override other methods that call this method;
     * this method should also not be called directly unless you really know
     * what you are doing (and probably not even then).
     */
    public final Object deserialize(JsonParser p, DeserializationContext ctxt) throws IOException
    {
        if (p.hasToken(JsonToken.VALUE_NULL)) {
            return _nullProvider.getNullValue(ctxt);
        }
        if (_valueTypeDeserializer != null) {
            return _valueDeserializer.deserializeWithType(p, ctxt, _valueTypeDeserializer);
        }
        // 04-May-2018, tatu: [databind#2023] Coercion from String (mostly) can give null
        Object value =  _valueDeserializer.deserialize(p, ctxt);
        if (value == null) {
            value = _nullProvider.getNullValue(ctxt);
        }
        return value;
    }

    /**
     * @since 2.9
     */
    public final Object deserializeWith(JsonParser p, DeserializationContext ctxt,
            Object toUpdate) throws IOException
    {
        // 20-Oct-2016, tatu: Not 100% sure what to do; probably best to simply return
        //   null value and let caller decide what to do.
        if (p.hasToken(JsonToken.VALUE_NULL)) {
            // ... except for "skip nulls" case which should just do that:
            if (NullsConstantProvider.isSkipper(_nullProvider)) {
                return toUpdate;
            }
            return _nullProvider.getNullValue(ctxt);
        }
        // 20-Oct-2016, tatu: Also tricky -- for now, report an error
        if (_valueTypeDeserializer != null) {
            ctxt.reportBadDefinition(getType(),
                    String.format("Cannot merge polymorphic property '%s'",
                            getName()));
//            return _valueDeserializer.deserializeWithType(p, ctxt, _valueTypeDeserializer);
        }
        // 04-May-2018, tatu: [databind#2023] Coercion from String (mostly) can give null
        Object value = _valueDeserializer.deserialize(p, ctxt, toUpdate);
        if (value == null) {
            if (NullsConstantProvider.isSkipper(_nullProvider)) {
                return toUpdate;
            }
            value = _nullProvider.getNullValue(ctxt);
        }
        return value;
    }

    /*
    /**********************************************************
    /* Helper methods
    /**********************************************************
     */

    /**
     * Method that takes in exception of any type, and casts or wraps it
     * to an IOException or its subclass.
     */
    protected void _throwAsIOE(JsonParser p, Exception e, Object value) throws IOException
    {
        if (e instanceof IllegalArgumentException) {
            String actType = ClassUtil.classNameOf(value);
            StringBuilder msg = new StringBuilder("Problem deserializing property '")
                    .append(getName())
                    .append("' (expected type: ")
                    .append(getType())
                    .append("; actual type: ")
                    .append(actType).append(")");
            String origMsg = e.getMessage();
            if (origMsg != null) {
                msg.append(", problem: ")
                    .append(origMsg);
            } else {
                msg.append(" (no error message provided)");
            }
            throw JsonMappingException.from(p, msg.toString(), e);
        }
        _throwAsIOE(p, e);
    }
    
    /**
     * @since 2.7
     */
    protected IOException _throwAsIOE(JsonParser p, Exception e) throws IOException
    {
        ClassUtil.throwIfIOE(e);
        ClassUtil.throwIfRTE(e);
        // let's wrap the innermost problem
        Throwable th = ClassUtil.getRootCause(e);
        throw JsonMappingException.from(p, th.getMessage(), th);
    }

    @Deprecated // since 2.7
    protected IOException _throwAsIOE(Exception e) throws IOException {
        return _throwAsIOE((JsonParser) null, e);
    }

    // 10-Oct-2015, tatu: _Should_ be deprecated, too, but its remaining
    //   callers cannot actually provide a JsonParser
    protected void _throwAsIOE(Exception e, Object value) throws IOException {
        _throwAsIOE((JsonParser) null, e, value);
    }

    @Override public String toString() { return "[property '"+getName()+"']"; }

    /*
    /**********************************************************
    /* Helper classes
    /**********************************************************
     */

    /**
     * Helper class that is designed to both make it easier to sub-class
     * delegating subtypes and to reduce likelihood of breakage when
     * new methods are added.
     *<p>
     * Class was specifically added to help with {@code Afterburner}
     * module, but its use is not limited to only support it.
     *
     * @since 2.9
     */
    public static abstract class Delegating
        extends SettableBeanProperty
    {
        protected final SettableBeanProperty delegate;

        protected Delegating(SettableBeanProperty d) {
            super(d);
            delegate = d;
        }

        /**
         * Method sub-classes must implement, to construct a new instance
         * with given delegate.
         */
        protected abstract SettableBeanProperty withDelegate(SettableBeanProperty d);

        protected SettableBeanProperty _with(SettableBeanProperty newDelegate) {
            if (newDelegate == delegate) {
                return this;
            }
            return withDelegate(newDelegate);
        }
        
        @Override
        public SettableBeanProperty withValueDeserializer(JsonDeserializer<?> deser) {
            return _with(delegate.withValueDeserializer(deser));
        }

        @Override
        public SettableBeanProperty withName(PropertyName newName) {
            return _with(delegate.withName(newName));
        }

        @Override
        public SettableBeanProperty withNullProvider(NullValueProvider nva) {
            return _with(delegate.withNullProvider(nva));
        }

        @Override
        public void assignIndex(int index) {
            delegate.assignIndex(index);
        }

        @Override
        public void fixAccess(DeserializationConfig config) {
            delegate.fixAccess(config);
        }

        /*
        /**********************************************************
        /* Accessors
        /**********************************************************
         */

        @Override
        protected Class<?> getDeclaringClass() { return delegate.getDeclaringClass(); }

        @Override
        public String getManagedReferenceName() { return delegate.getManagedReferenceName(); }

        @Override
        public ObjectIdInfo getObjectIdInfo() { return delegate.getObjectIdInfo(); }

        @Override
        public boolean hasValueDeserializer() { return delegate.hasValueDeserializer(); }

        @Override
        public boolean hasValueTypeDeserializer() { return delegate.hasValueTypeDeserializer(); }
        
        @Override
        public JsonDeserializer<Object> getValueDeserializer() { return delegate.getValueDeserializer(); }

        @Override
        public TypeDeserializer getValueTypeDeserializer() { return delegate.getValueTypeDeserializer(); }

        @Override
        public boolean visibleInView(Class<?> activeView) { return delegate.visibleInView(activeView); }

        @Override
        public boolean hasViews() { return delegate.hasViews(); }

        @Override
        public int getPropertyIndex() { return delegate.getPropertyIndex(); }

        @Override
        public int getCreatorIndex() { return delegate.getCreatorIndex(); }

        @Override
        public Object getInjectableValueId() { return delegate.getInjectableValueId(); }

        @Override
        public AnnotatedMember getMember() {
            return delegate.getMember();
        }

        @Override
        public <A extends Annotation> A getAnnotation(Class<A> acls) {
            return delegate.getAnnotation(acls);
        }

        /*
        /**********************************************************
        /* Extended API
        /**********************************************************
         */

        public SettableBeanProperty getDelegate() {
            return delegate;
        }

        /*
        /**********************************************************
        /* Actual mutators
        /**********************************************************
         */

        @Override
        public void deserializeAndSet(JsonParser p, DeserializationContext ctxt,
                Object instance) throws IOException {
            delegate.deserializeAndSet(p, ctxt, instance);
        }

        @Override
        public Object deserializeSetAndReturn(JsonParser p,
                DeserializationContext ctxt, Object instance) throws IOException
        {
            return delegate.deserializeSetAndReturn(p, ctxt, instance);
        }

        @Override
        public void set(Object instance, Object value) throws IOException {
            delegate.set(instance, value);
        }

        @Override
        public Object setAndReturn(Object instance, Object value) throws IOException {
            return delegate.setAndReturn(instance, value);
        }
    }
}
