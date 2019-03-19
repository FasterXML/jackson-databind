package com.fasterxml.jackson.databind.ser;

import java.io.IOException;
import java.util.*;

import com.fasterxml.jackson.annotation.ObjectIdGenerator;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.TokenStreamFactory;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.cfg.GeneratorSettings;
import com.fasterxml.jackson.databind.cfg.HandlerInstantiator;
import com.fasterxml.jackson.databind.introspect.Annotated;
import com.fasterxml.jackson.databind.introspect.BeanPropertyDefinition;
import com.fasterxml.jackson.databind.jsonFormatVisitors.JsonFormatVisitorWrapper;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import com.fasterxml.jackson.databind.ser.impl.WritableObjectId;
import com.fasterxml.jackson.databind.util.ClassUtil;

/**
 * Extension over {@link SerializerProvider} that adds methods needed by
 * {@link ObjectMapper} (and {@link ObjectWriter}) but that are not to be exposed
 * as general context during serialization.
 *<p>
 * Also note that all custom {@link SerializerProvider}
 * implementations must sub-class this class: {@link ObjectMapper}
 * requires this type, not basic provider type.
 */
public class DefaultSerializerProvider
    extends SerializerProvider
{
    /*
    /**********************************************************************
    /* Additional state
    /**********************************************************************
     */

    /**
     * Per-serialization map Object Ids that have seen so far, iff
     * Object Id handling is enabled.
     */
    protected transient Map<Object, WritableObjectId> _seenObjectIds;
    
    protected transient ArrayList<ObjectIdGenerator<?>> _objectIdGenerators;

    /*
    /**********************************************************************
    /* Life-cycle
    /**********************************************************************
     */

    protected DefaultSerializerProvider(TokenStreamFactory streamFactory,
            SerializationConfig config, GeneratorSettings genSettings,
            SerializerFactory f, SerializerCache cache) {
        super(streamFactory, config, genSettings, f, cache);
    }

    /*
    /**********************************************************************
    /* Abstract method impls, factory methods
    /**********************************************************************
     */
    
    @Override
    public JsonSerializer<Object> serializerInstance(Annotated annotated, Object serDef)
            throws JsonMappingException
    {
        if (serDef == null) {
            return null;
        }
        JsonSerializer<?> ser;
        
        if (serDef instanceof JsonSerializer) {
            ser = (JsonSerializer<?>) serDef;
        } else {
            // Alas, there's no way to force return type of "either class
            // X or Y" -- need to throw an exception after the fact
            if (!(serDef instanceof Class)) {
                reportBadDefinition(annotated.getType(),
                        "AnnotationIntrospector returned serializer definition of type "
                        +serDef.getClass().getName()+"; expected type JsonSerializer or Class<JsonSerializer> instead");
            }
            Class<?> serClass = (Class<?>)serDef;
            // there are some known "no class" markers to consider too:
            if (serClass == JsonSerializer.None.class || ClassUtil.isBogusClass(serClass)) {
                return null;
            }
            if (!JsonSerializer.class.isAssignableFrom(serClass)) {
                reportBadDefinition(annotated.getType(),
                        "AnnotationIntrospector returned Class "
                        +serClass.getName()+"; expected Class<JsonSerializer>");
            }
            HandlerInstantiator hi = _config.getHandlerInstantiator();
            ser = (hi == null) ? null : hi.serializerInstance(_config, annotated, serClass);
            if (ser == null) {
                ser = (JsonSerializer<?>) ClassUtil.createInstance(serClass,
                        _config.canOverrideAccessModifiers());
            }
        }
        return (JsonSerializer<Object>) _handleResolvable(ser);
    }

    @Override
    public Object includeFilterInstance(BeanPropertyDefinition forProperty,
            Class<?> filterClass)
    {
        if (filterClass == null) {
            return null;
        }
        HandlerInstantiator hi = _config.getHandlerInstantiator();
        Object filter = (hi == null) ? null : hi.includeFilterInstance(_config, forProperty, filterClass);
        if (filter == null) {
            filter = ClassUtil.createInstance(filterClass,
                    _config.canOverrideAccessModifiers());
        }
        return filter;
    }

    @Override
    public boolean includeFilterSuppressNulls(Object filter) throws JsonMappingException
    {
        if (filter == null) {
            return true;
        }
        // should let filter decide what to do with nulls:
        // But just case, let's handle unexpected (from our perspective) problems explicitly
        try {
            return filter.equals(null);
        } catch (Throwable t) {
            String msg = String.format(
"Problem determining whether filter of type '%s' should filter out `null` values: (%s) %s",
filter.getClass().getName(), t.getClass().getName(), ClassUtil.exceptionMessage(t));
            reportBadDefinition(filter.getClass(), msg, t);
            return false; // never gets here
        }
    }

    /*
    /**********************************************************************
    /* Object Id handling
    /**********************************************************************
     */
    
    @Override
    public WritableObjectId findObjectId(Object forPojo, ObjectIdGenerator<?> generatorType)
    {
        if (_seenObjectIds == null) {
            _seenObjectIds = _createObjectIdMap();
        } else {
            WritableObjectId oid = _seenObjectIds.get(forPojo);
            if (oid != null) {
                return oid;
            }
        }
        // Not seen yet; must add an entry, return it. For that, we need generator
        ObjectIdGenerator<?> generator = null;
        
        if (_objectIdGenerators == null) {
            _objectIdGenerators = new ArrayList<ObjectIdGenerator<?>>(8);
        } else {
            for (int i = 0, len = _objectIdGenerators.size(); i < len; ++i) {
                ObjectIdGenerator<?> gen = _objectIdGenerators.get(i);
                if (gen.canUseFor(generatorType)) {
                    generator = gen;
                    break;
                }
            }
        }
        if (generator == null) {
            generator = generatorType.newForSerialization(this);
            _objectIdGenerators.add(generator);
        }
        WritableObjectId oid = new WritableObjectId(generator);
        _seenObjectIds.put(forPojo, oid);
        return oid;
    }

    /**
     * Overridable helper method used for creating {@link java.util.Map}
     * used for storing mappings from serializable objects to their
     * Object Ids.
     */
    protected Map<Object,WritableObjectId> _createObjectIdMap()
    {
        /* 06-Aug-2013, tatu: We may actually want to use equality,
         *   instead of identity... so:
         */
        if (isEnabled(SerializationFeature.USE_EQUALITY_FOR_OBJECT_ID)) {
            return new HashMap<Object,WritableObjectId>();
        }
        return new IdentityHashMap<Object,WritableObjectId>();
    }

    /*
    /**********************************************************************
    /* Extended API: simple accesors
    /**********************************************************************
     */

    /**
     * Accessor for the {@link JsonGenerator} currently in use for serializing
     * content. Null for blueprint instances; non-null for actual active
     * provider instances.
     */
    @Override
    public JsonGenerator getGenerator() {
        return _generator;
    }

    /*
    /**********************************************************************
    /* Extended API called by ObjectMapper: value serialization
    /**********************************************************************
     */
    
    /**
     * The method to be called by {@link ObjectMapper} and {@link ObjectWriter}
     * for serializing given value, using serializers that
     * this provider has access to (via caching and/or creating new serializers
     * as need be).
     */
    public void serializeValue(JsonGenerator gen, Object value) throws IOException
    {
        _generator = gen;
        if (value == null) {
            _serializeNull(gen);
            return;
        }
        final Class<?> cls = value.getClass();
        // true, since we do want to cache root-level typed serializers (ditto for null property)
        final JsonSerializer<Object> ser = findTypedValueSerializer(cls, true);
        PropertyName rootName = _config.getFullRootName();
        if (rootName == null) { // not explicitly specified
            if (_config.isEnabled(SerializationFeature.WRAP_ROOT_VALUE)) {
                _serialize(gen, value, ser, _config.findRootName(cls));
                return;
            }
        } else if (!rootName.isEmpty()) {
            _serialize(gen, value, ser, rootName);
            return;
        }
        _serialize(gen, value, ser);
    }

    /**
     * The method to be called by {@link ObjectMapper} and {@link ObjectWriter}
     * for serializing given value (assumed to be of specified root type,
     * instead of runtime type of value),
     * using serializers that
     * this provider has access to (via caching and/or creating new serializers
     * as need be),
     * 
     * @param rootType Type to use for locating serializer to use, instead of actual
     *    runtime type. Must be actual type, or one of its super types
     */
    public void serializeValue(JsonGenerator gen, Object value, JavaType rootType) throws IOException
    {
        _generator = gen;
        if (value == null) {
            _serializeNull(gen);
            return;
        }
        // Let's ensure types are compatible at this point
        if (!rootType.getRawClass().isAssignableFrom(value.getClass())) {
            _reportIncompatibleRootType(value, rootType);
        }
        // root value, not reached via property:
        JsonSerializer<Object> ser = findTypedValueSerializer(rootType, true);
        PropertyName rootName = _config.getFullRootName();
        if (rootName == null) { // not explicitly specified
            if (_config.isEnabled(SerializationFeature.WRAP_ROOT_VALUE)) {
                _serialize(gen, value, ser, _config.findRootName(rootType));
                return;
            }
        } else if (!rootName.isEmpty()) {
            _serialize(gen, value, ser, rootName);
            return;
        }
        _serialize(gen, value, ser);
    }

    /**
     * The method to be called by {@link ObjectWriter}
     * for serializing given value (assumed to be of specified root type,
     * instead of runtime type of value), when it may know specific
     * {@link JsonSerializer} to use.
     * 
     * @param rootType Type to use for locating serializer to use, instead of actual
     *    runtime type, if no serializer is passed
     * @param ser Root Serializer to use, if not null
     */
    public void serializeValue(JsonGenerator gen, Object value, JavaType rootType,
            JsonSerializer<Object> ser) throws IOException
    {
        _generator = gen;
        if (value == null) {
            _serializeNull(gen);
            return;
        }
        // Let's ensure types are compatible at this point
        if ((rootType != null) && !rootType.getRawClass().isAssignableFrom(value.getClass())) {
            _reportIncompatibleRootType(value, rootType);
        }
        // root value, not reached via property:
        if (ser == null) {
            ser = findTypedValueSerializer(rootType, true);
        }
        PropertyName rootName = _config.getFullRootName();
        if (rootName == null) { // not explicitly specified
            if (_config.isEnabled(SerializationFeature.WRAP_ROOT_VALUE)) {
                rootName = (rootType == null)
                        ? _config.findRootName(value.getClass())
                        : _config.findRootName(rootType);
                _serialize(gen, value, ser, rootName);
                return;
            }
        } else if (!rootName.isEmpty()) {
            _serialize(gen, value, ser, rootName);
            return;
        }
        _serialize(gen, value, ser);
    }

    /**
     * Alternate serialization call used for polymorphic types, when {@link TypeSerializer}
     * is already known, but the actual serializer may or may not be.
     */
    public void serializePolymorphic(JsonGenerator gen, Object value, JavaType rootType,
            JsonSerializer<Object> valueSer, TypeSerializer typeSer)
        throws IOException
    {
        _generator = gen;
        if (value == null) {
            _serializeNull(gen);
            return;
        }
        // Let's ensure types are compatible at this point
        if ((rootType != null) && !rootType.getRawClass().isAssignableFrom(value.getClass())) {
            _reportIncompatibleRootType(value, rootType);
        }
        /* 12-Jun-2015, tatu: nominal root type is necessary for Maps at least;
         *   possibly collections, but can cause problems for other polymorphic
         *   types. We really need to distinguish between serialization type,
         *   base type; but right we don't. Hence this check
         */
        if (valueSer == null) {
            if ((rootType != null) && rootType.isContainerType()) {
                valueSer = handleRootContextualization(findValueSerializer(rootType));
            } else {
                valueSer = handleRootContextualization(findValueSerializer(value.getClass()));
            }
        }

        final boolean wrap;
        PropertyName rootName = _config.getFullRootName();
        if (rootName == null) {
            wrap = _config.isEnabled(SerializationFeature.WRAP_ROOT_VALUE);
            if (wrap) {
                gen.writeStartObject();
                PropertyName pname = _config.findRootName(value.getClass());
                gen.writeFieldName(pname.simpleAsEncoded(_config));
            }
        } else if (rootName.isEmpty()) {
            wrap = false;
        } else {
            wrap = true;
            gen.writeStartObject();
            gen.writeFieldName(rootName.getSimpleName());
        }
        try {
            valueSer.serializeWithType(value, gen, this, typeSer);
            if (wrap) {
                gen.writeEndObject();
            }
        } catch (Exception e) {
            throw _wrapAsIOE(gen, e);
        }
    }

    private final void _serialize(JsonGenerator gen, Object value,
            JsonSerializer<Object> ser, PropertyName rootName)
        throws IOException
    {
        try {
            gen.writeStartObject();
            gen.writeFieldName(rootName.simpleAsEncoded(_config));
            ser.serialize(value, gen, this);
            gen.writeEndObject();
        } catch (Exception e) {
            throw _wrapAsIOE(gen, e);
        }
    }

    private final void _serialize(JsonGenerator gen, Object value,
            JsonSerializer<Object> ser)
        throws IOException
    {
        try {
            ser.serialize(value, gen, this);
        } catch (Exception e) {
            throw _wrapAsIOE(gen, e);
        }
    }

    /**
     * Helper method called when root value to serialize is null
     */
    protected void _serializeNull(JsonGenerator gen) throws IOException
    {
        JsonSerializer<Object> ser = getDefaultNullValueSerializer();
        try {
            ser.serialize(null, gen, this);
        } catch (Exception e) {
            throw _wrapAsIOE(gen, e);
        }
    }

    private IOException _wrapAsIOE(JsonGenerator g, Exception e) {
        if (e instanceof IOException) {
            return (IOException) e;
        }
        String msg = ClassUtil.exceptionMessage(e);
        if (msg == null) {
            msg = "[no message for "+e.getClass().getName()+"]";
        }
        return new JsonMappingException(g, msg, e);
    }

    /*
    /**********************************************************************
    /* Extended API called by ObjectMapper: other
    /**********************************************************************
     */

    /**
     * The method to be called by {@link ObjectMapper} and {@link ObjectWriter}
     * to to expose the format of the given to to the given visitor
     *
     * @param javaType The type for which to generate format
     * @param visitor the visitor to accept the format
     */
    public void acceptJsonFormatVisitor(JavaType javaType, JsonFormatVisitorWrapper visitor)
        throws JsonMappingException
    {
        if (javaType == null) {
            throw new IllegalArgumentException("A class must be provided");
        }
        // no need for embedded type information for JSON schema generation (all
        // type information it needs is accessible via "untyped" serializer)
        visitor.setProvider(this);
        findRootValueSerializer(javaType).acceptJsonFormatVisitor(visitor, javaType);
    }

    /*
    /**********************************************************************
    /* Helper classes
    /**********************************************************************
     */

    /**
     * Concrete implementation defined separately so it can be declared `final`.
     * Alternate implements should instead just extend {@link DefaultSerializerProvider}
     */
    public final static class Impl
        extends DefaultSerializerProvider
    {
        public Impl(TokenStreamFactory streamFactory,
                SerializationConfig config, GeneratorSettings genSettings,
                SerializerFactory f, SerializerCache cache) {
            super(streamFactory, config, genSettings, f, cache);
        }
    }
}
