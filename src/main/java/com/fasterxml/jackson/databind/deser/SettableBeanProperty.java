package com.fasterxml.jackson.databind.deser;

import java.io.IOException;
import java.lang.annotation.Annotation;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.deser.impl.FailingDeserializer;
import com.fasterxml.jackson.databind.deser.impl.NullProvider;
import com.fasterxml.jackson.databind.introspect.AnnotatedMember;
import com.fasterxml.jackson.databind.introspect.BeanPropertyDefinition;
import com.fasterxml.jackson.databind.introspect.ObjectIdInfo;
import com.fasterxml.jackson.databind.jsonFormatVisitors.JsonObjectFormatVisitor;
import com.fasterxml.jackson.databind.jsontype.TypeDeserializer;
import com.fasterxml.jackson.databind.util.Annotations;
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
    implements BeanProperty,
        java.io.Serializable
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
     * Object used to figure out value to be used when 'null' literal is encountered in JSON.
     * For most types simply Java null, but for primitive types must
     * be a non-null value (like Integer.valueOf(0) for int).
     */
    protected final NullProvider _nullProvider;

    /**
     * Additional optional property metadata, such as whether
     * property is required, and whether there is additional
     * human-readable description
     * 
     * @since 2.3
     */
    protected final PropertyMetadata _metadata;

    /*
    /**********************************************************
    /* Configuration that is not yet immutable; generally assigned
    /* during initialization process but can not be passed to
    /* constructor.
    /**********************************************************
     */

    /**
     * If property represents a managed (forward) reference
     * (see [JACKSON-235]), we will need name of reference for
     * later linking.
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

    @Deprecated // since 2.3
    protected SettableBeanProperty(String propName, JavaType type, PropertyName wrapper,
            TypeDeserializer typeDeser, Annotations contextAnnotations,
            boolean isRequired)
    {
        this(new PropertyName(propName), type, wrapper, typeDeser, contextAnnotations,
                PropertyMetadata.construct(isRequired, null, null, null));
    }
    
    protected SettableBeanProperty(PropertyName propName, JavaType type, PropertyName wrapper,
            TypeDeserializer typeDeser, Annotations contextAnnotations,
            PropertyMetadata metadata)
    {
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
        _metadata = metadata;
        _contextAnnotations = contextAnnotations;
        _viewMatcher = null;
        _nullProvider = null;

        // 30-Jan-2012, tatu: Important: contextualize TypeDeserializer now...
        if (typeDeser != null) {
            typeDeser = typeDeser.forProperty(this);
        }
        _valueTypeDeserializer = typeDeser;
        _valueDeserializer = MISSING_VALUE_DESERIALIZER;
    }

    /**
     * Constructor only used by {@link com.fasterxml.jackson.databind.deser.impl.ObjectIdValueProperty}.
     * 
     * @since 2.3
     */
    protected SettableBeanProperty(PropertyName propName, JavaType type, 
            PropertyMetadata metadata, JsonDeserializer<Object> valueDeser)
    {
        // as with above ctor, intern()ing probably fine
        if (propName == null) {
            _propName = PropertyName.NO_NAME;
        } else {
            _propName = propName.internSimpleName();
        }
        _type = type;
        _wrapperName = null;
        _metadata = metadata;
        _contextAnnotations = null;
        _viewMatcher = null;
        _nullProvider = null;
        _valueTypeDeserializer = null;
        _valueDeserializer = valueDeser;
    }
    
    /**
     * Basic copy-constructor for sub-classes to use.
     */
    protected SettableBeanProperty(SettableBeanProperty src)
    {
        _propName = src._propName;
        _type = src._type;
        _wrapperName = src._wrapperName;
        _metadata = src._metadata;
        _contextAnnotations = src._contextAnnotations;
        _valueDeserializer = src._valueDeserializer;
        _valueTypeDeserializer = src._valueTypeDeserializer;
        _nullProvider = src._nullProvider;
        _managedReferenceName = src._managedReferenceName;
        _propertyIndex = src._propertyIndex;
        _viewMatcher = src._viewMatcher;
    }

    /**
     * Copy-with-deserializer-change constructor for sub-classes to use.
     */
    @SuppressWarnings("unchecked")
    protected SettableBeanProperty(SettableBeanProperty src, JsonDeserializer<?> deser)
    {
        _propName = src._propName;
        _type = src._type;
        _wrapperName = src._wrapperName;
        _metadata = src._metadata;
        _contextAnnotations = src._contextAnnotations;
        _valueTypeDeserializer = src._valueTypeDeserializer;
        _managedReferenceName = src._managedReferenceName;
        _propertyIndex = src._propertyIndex;

        if (deser == null) {
            _nullProvider = null;
            _valueDeserializer = MISSING_VALUE_DESERIALIZER;
        } else {
            Object nvl = deser.getNullValue();
            _nullProvider = (nvl == null) ? null : new NullProvider(_type, nvl);
            _valueDeserializer = (JsonDeserializer<Object>) deser;
        }
        _viewMatcher = src._viewMatcher;
    }

    @Deprecated // since 2.3
    protected SettableBeanProperty(SettableBeanProperty src, String newName) {
        this(src, new PropertyName(newName));
    }
    
    /**
     * Copy-with-deserializer-change constructor for sub-classes to use.
     */
    protected SettableBeanProperty(SettableBeanProperty src, PropertyName newName)
    {
        _propName = newName;
        _type = src._type;
        _wrapperName = src._wrapperName;
        _metadata = src._metadata;
        _contextAnnotations = src._contextAnnotations;
        _valueDeserializer = src._valueDeserializer;
        _valueTypeDeserializer = src._valueTypeDeserializer;
        _nullProvider = src._nullProvider;
        _managedReferenceName = src._managedReferenceName;
        _propertyIndex = src._propertyIndex;
        _viewMatcher = src._viewMatcher;
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
    
    @Deprecated // since 2.3 -- use 'withSimpleName' instead if need be
    public SettableBeanProperty withName(String simpleName) {
        return withName(new PropertyName(simpleName));
    }

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
    public boolean isRequired() { return _metadata.isRequired(); }

    @Override
    public PropertyMetadata getMetadata() { return _metadata; }
    
    @Override
    public JavaType getType() { return _type; }

    @Override
    public PropertyName getWrapperName() {
        return _wrapperName;
    }
    
    @Override
    public abstract <A extends Annotation> A getAnnotation(Class<A> acls);

    @Override
    public abstract AnnotatedMember getMember();

    @Override
    public <A extends Annotation> A getContextAnnotation(Class<A> acls) {
        return _contextAnnotations.get(acls);
    }

    @Override
    public void depositSchemaProperty(JsonObjectFormatVisitor objectVisitor)
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

    protected final Class<?> getDeclaringClass() {
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
    public int getCreatorIndex() { return -1; }
    
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
        JsonToken t = p.getCurrentToken();
        
        if (t == JsonToken.VALUE_NULL) {
            return (_nullProvider == null) ? null : _nullProvider.nullValue(ctxt);
        }
        if (_valueTypeDeserializer != null) {
            return _valueDeserializer.deserializeWithType(p, ctxt, _valueTypeDeserializer);
        }
        return _valueDeserializer.deserialize(p, ctxt);
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
    protected void _throwAsIOE(Exception e, Object value) throws IOException
    {
        if (e instanceof IllegalArgumentException) {
            String actType = (value == null) ? "[NULL]" : value.getClass().getName();
            StringBuilder msg = new StringBuilder("Problem deserializing property '").append(getName());
            msg.append("' (expected type: ").append(getType());
            msg.append("; actual type: ").append(actType).append(")");
            String origMsg = e.getMessage();
            if (origMsg != null) {
                msg.append(", problem: ").append(origMsg);
            } else {
                msg.append(" (no error message provided)");
            }
            throw new JsonMappingException(msg.toString(), null, e);
        }
        _throwAsIOE(e);
    }

    protected IOException _throwAsIOE(Exception e) throws IOException
    {
        if (e instanceof IOException) {
            throw (IOException) e;
        }
        if (e instanceof RuntimeException) {
            throw (RuntimeException) e;
        }
        // let's wrap the innermost problem
        Throwable th = e;
        while (th.getCause() != null) {
            th = th.getCause();
        }
        throw new JsonMappingException(th.getMessage(), null, th);
    }

    @Override public String toString() { return "[property '"+getName()+"']"; }
}
