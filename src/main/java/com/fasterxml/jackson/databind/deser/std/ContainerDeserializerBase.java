package com.fasterxml.jackson.databind.deser.std;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.deser.NullValueProvider;
import com.fasterxml.jackson.databind.deser.SettableBeanProperty;
import com.fasterxml.jackson.databind.deser.ValueInstantiator;
import com.fasterxml.jackson.databind.deser.impl.NullsConstantProvider;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.fasterxml.jackson.databind.util.AccessPattern;
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
     * Marker flag set if the <code>_nullProvider</code> indicates that all null
     * content values should be skipped (instead of being possibly converted).
     *
     * @since 2.9
     */
    protected final boolean _skipNullValues;

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
        _skipNullValues = NullsConstantProvider.isSkipper(nuller);
    }

    protected ContainerDeserializerBase(JavaType selfType) {
        this(selfType, null, null);
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
        _skipNullValues = NullsConstantProvider.isSkipper(nuller);
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
                    "Cannot handle managed/back reference '%s': type: container deserializer of type %s returned null for 'getContentDeserializer()'",
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

    @Override // since 2.9
    public AccessPattern getEmptyAccessPattern() {
        // 02-Feb-2017, tatu: Empty containers are usually constructed as needed
        //   and may not be shared; for some deserializers this may be further refined.
        return AccessPattern.DYNAMIC;
    }

    @Override // since 2.9
    public Object getEmptyValue(DeserializationContext ctxt) throws JsonMappingException {
        ValueInstantiator vi = getValueInstantiator();
        if (vi == null || !vi.canCreateUsingDefault()) {
            JavaType type = getValueType();
            ctxt.reportBadDefinition(type,
                    String.format("Cannot create empty instance of %s, no default Creator", type));
        }
        try {
            return vi.createUsingDefault(ctxt); // lgtm [java/dereferenced-value-may-be-null]
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
     * @deprecated Since 2.12.2 (since it does not get context for accessing config)
     */
    @Deprecated
    protected <BOGUS> BOGUS wrapAndThrow(Throwable t, Object ref, String key) throws IOException
    {
        return wrapAndThrow(null, t, ref, key);
    }

    /**
     * Helper method called by various Map(-like) deserializers when encountering
     * a processing problem (whether from underlying parser, i/o, or something else).
     *
     * @since 2.12.2
     */
    protected <BOGUS> BOGUS wrapAndThrow(DeserializationContext ctxt,
            Throwable t, Object ref, String key) throws IOException
    {
        // to handle StackOverflow:
        while (t instanceof InvocationTargetException && t.getCause() != null) {
            t = t.getCause();
        }
        // Errors and "plain" IOExceptions to be passed as-is
        ClassUtil.throwIfError(t);
        // 25-Feb-2021, tatu: as per [databind#3068] need to obey WRAP_EXCEPTIONS setting
        if ((ctxt != null) && !ctxt.isEnabled(DeserializationFeature.WRAP_EXCEPTIONS)) {
            ClassUtil.throwIfRTE(t);
        }
        // ... except for mapping exceptions
        if (t instanceof IOException && !(t instanceof JsonMappingException)) {
            throw (IOException) t;
        }
        // for [databind#1141]
        throw JsonMappingException.wrapWithPath(t, ref,
                ClassUtil.nonNull(key, "N/A"));
    }
}
