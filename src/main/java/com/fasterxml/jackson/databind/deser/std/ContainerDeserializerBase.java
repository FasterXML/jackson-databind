package com.fasterxml.jackson.databind.deser.std;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.deser.NullValueProvider;
import com.fasterxml.jackson.databind.deser.SettableBeanProperty;
import com.fasterxml.jackson.databind.deser.ValueInstantiator;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.fasterxml.jackson.databind.util.ClassUtil;

/**
 * Intermediate base deserializer class that adds more shared accessor
 * so that other classes can access information about contained (value) types
 */
@SuppressWarnings("serial")
public abstract class ContainerDeserializerBase<T>
    extends StdDeserializer<T>
    implements ValueInstantiator.Gettable // since 2.9
{
    protected final JavaType _containerType;

    /**
     * Handler we need for dealing with nulls.
     *
     * @since 2.9
     */
    protected final NullValueProvider _nullProvider;

    /**
     * Specific override for this instance (from proper, or global per-type overrides)
     * to indicate whether single value may be taken to mean an unwrapped one-element array
     * or not. If null, left to global defaults.
     *
     * @since 2.9 (demoted from sub-classes where added in 2.7)
     */
    protected final Boolean _unwrapSingle;
    
    protected ContainerDeserializerBase(JavaType selfType,
            NullValueProvider nuller, Boolean unwrapSingle) {
        super(selfType);
        _containerType = selfType;
        _unwrapSingle = unwrapSingle;
        _nullProvider = nuller;
    }

    /**
     * @since 2.9
     */
    protected ContainerDeserializerBase(ContainerDeserializerBase<?> base) {
        this(base, base._nullProvider, base._unwrapSingle);
    }

    /**
     * @since 2.9
     */
    protected ContainerDeserializerBase(ContainerDeserializerBase<?> base,
            NullValueProvider nuller, Boolean unwrapSingle) {
        super(base._containerType);
        _containerType = base._containerType;
        _nullProvider = nuller;
        _unwrapSingle = unwrapSingle;
    }
    
    @Deprecated // since 2.9
    protected ContainerDeserializerBase(JavaType selfType) {
        this(selfType, null, null);
    }

    /*
    /**********************************************************
    /* Overrides
    /**********************************************************
     */

    @Override // since 2.9
    public JavaType getValueType() { return _containerType; }
    
    @Override // since 2.9
    public Boolean supportsUpdate(DeserializationConfig config) {
        return Boolean.TRUE;
    }

    @Override
    public SettableBeanProperty findBackReference(String refName) {
        JsonDeserializer<Object> valueDeser = getContentDeserializer();
        if (valueDeser == null) {
            throw new IllegalArgumentException(String.format(
                    "Can not handle managed/back reference '%s': type: container deserializer of type %s returned null for 'getContentDeserializer()'",
                    refName, getClass().getName()));
        }
        return valueDeser.findBackReference(refName);
    }

    /*
    /**********************************************************
    /* Extended API
    /**********************************************************
     */

    /**
     * Accessor for declared type of contained value elements; either exact
     * type, or one of its supertypes.
     */
    public JavaType getContentType() {
        if (_containerType == null) {
            return TypeFactory.unknownType(); // should never occur but...
        }
        return _containerType.getContentType();
    }

    /**
     * Accesor for deserializer use for deserializing content values.
     */
    public abstract JsonDeserializer<Object> getContentDeserializer();

    /**
     * @since 2.9
     */
    @Override
    public ValueInstantiator getValueInstantiator() {
        return null;
    }

    @Override // since 2.9
    public Object getEmptyValue(DeserializationContext ctxt) throws JsonMappingException {
        ValueInstantiator vi = getValueInstantiator();
        if (vi == null || !vi.canCreateUsingDefault()) {
            JavaType type = getValueType();
            ctxt.reportBadDefinition(type,
                    String.format("Can not create empty instance of %s, no default Creator", type));
        }
        try {
            return vi.createUsingDefault(ctxt);
        } catch (IOException e) {
            return ClassUtil.throwAsMappingException(ctxt, e);
        }
    }

    /*
    /**********************************************************
    /* Shared methods for sub-classes
    /**********************************************************
     */

    /**
     * Helper method called by various Map(-like) deserializers.
     */
    protected <BOGUS> BOGUS wrapAndThrow(Throwable t, Object ref, String key) throws IOException
    {
        // to handle StackOverflow:
        while (t instanceof InvocationTargetException && t.getCause() != null) {
            t = t.getCause();
        }
        // Errors and "plain" IOExceptions to be passed as is
        ClassUtil.throwIfError(t);
        // ... except for mapping exceptions
        if (t instanceof IOException && !(t instanceof JsonMappingException)) {
            throw (IOException) t;
        }
        // for [databind#1141]
        throw JsonMappingException.wrapWithPath(t, ref,
                ClassUtil.nonNull(key, "N/A"));
    }
}
