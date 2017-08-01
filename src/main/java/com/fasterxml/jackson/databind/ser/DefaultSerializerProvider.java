package com.fasterxml.jackson.databind.ser;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

import com.fasterxml.jackson.annotation.ObjectIdGenerator;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.cfg.HandlerInstantiator;
import com.fasterxml.jackson.databind.introspect.Annotated;
import com.fasterxml.jackson.databind.introspect.BeanPropertyDefinition;
import com.fasterxml.jackson.databind.jsonFormatVisitors.JsonFormatVisitorWrapper;
import com.fasterxml.jackson.databind.jsonschema.SchemaAware;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.ser.impl.WritableObjectId;
import com.fasterxml.jackson.databind.util.ClassUtil;

/**
 * Standard implementation used by {@link ObjectMapper}:
 * adds methods only exposed to {@link ObjectMapper},
 * as well as constructors.
 *<p>
 * Note that class is abstract just because it does not
 * define {@link #createInstance} method.
 *<p>
 * Also note that all custom {@link SerializerProvider}
 * implementations must sub-class this class: {@link ObjectMapper}
 * requires this type, not basic provider type.
 */
public abstract class DefaultSerializerProvider
    extends SerializerProvider
    implements java.io.Serializable // since 2.1; only because ObjectWriter needs it
{
    private static final long serialVersionUID = 1L;

    /*
    /**********************************************************
    /* State, for non-blueprint instances
    /**********************************************************
     */

    /**
     * Per-serialization map Object Ids that have seen so far, iff
     * Object Id handling is enabled.
     */
    protected transient Map<Object, WritableObjectId> _seenObjectIds;
    
    protected transient ArrayList<ObjectIdGenerator<?>> _objectIdGenerators;

    /**
     * Generator used for serialization. Needed mostly for error reporting
     * purposes.
     *
     * @since 2.8
     */
    protected transient JsonGenerator _generator;

    /*
    /**********************************************************
    /* Life-cycle
    /**********************************************************
     */

    protected DefaultSerializerProvider() { super(); }

    protected DefaultSerializerProvider(SerializerProvider src,
            SerializationConfig config,SerializerFactory f) {
        super(src, config, f);
    }

    protected DefaultSerializerProvider(DefaultSerializerProvider src) {
        super(src);
    }

    /**
     * Method that sub-classes need to implement: used to create a non-blueprint instances
     * from the blueprint.
     * This is needed to retain state during serialization.
     */
    public abstract DefaultSerializerProvider createInstance(SerializationConfig config,
            SerializerFactory jsf);

    /**
     * Method needed to ensure that {@link ObjectMapper#copy} will work
     * properly; specifically, that caches are cleared, but settings
     * will otherwise remain identical; and that no sharing of state
     * occurs.
     *
     * @since 2.5
     */
    public DefaultSerializerProvider copy() {
        throw new IllegalStateException("DefaultSerializerProvider sub-class not overriding copy()");
    }

    /*
    /**********************************************************
    /* Abstract method impls, factory methods
    /**********************************************************
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
filter.getClass().getName(), t.getClass().getName(), t.getMessage());
            reportBadDefinition(filter.getClass(), msg, t);
            return false; // never gets here
        }
    }

    /*
    /**********************************************************
    /* Object Id handling
    /**********************************************************
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
     * 
     * @since 2.3
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
    /**********************************************************
    /* Extended API: simple accesors
    /**********************************************************
     */

    /**
     * Method that can be called to see if this serializer provider
     * can find a serializer for an instance of given class.
     *<p>
     * Note that no Exceptions are thrown, including unchecked ones:
     * implementations are to swallow exceptions if necessary.
     */
    public boolean hasSerializerFor(Class<?> cls, AtomicReference<Throwable> cause)
    {
        // 07-Nov-2015, tatu: One special case, Object.class; will work only if
        //   empty beans are allowed or custom serializer registered. Easiest to
        //   check here.
        if (cls == Object.class) {
            if (!_config.isEnabled(SerializationFeature.FAIL_ON_EMPTY_BEANS)) {
                return true;
            }
        }
        
        try {
            JsonSerializer<?> ser = _findExplicitUntypedSerializer(cls);
            return (ser != null);
        } catch (JsonMappingException e) {
            if (cause != null) {
                cause.set(e);
            }
        } catch (RuntimeException e) {
            if (cause == null) { // earlier behavior
                throw e;
            }
            cause.set(e);
        }
        return false;
    }

    /**
     * Accessor for the {@link JsonGenerator} currently in use for serializing
     * content. Null for blueprint instances; non-null for actual active
     * provider instances.
     *
     * @since 2.8
     */
    @Override
    public JsonGenerator getGenerator() {
        return _generator;
    }

    /*
    /**********************************************************
    /* Extended API called by ObjectMapper: value serialization
    /**********************************************************
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
        final JsonSerializer<Object> ser = findTypedValueSerializer(cls, true, null);
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
        JsonSerializer<Object> ser = findTypedValueSerializer(rootType, true, null);
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
     * 
     * @since 2.1
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
            ser = findTypedValueSerializer(rootType, true, null);
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
     *
     * @since 2.6
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
                valueSer = findValueSerializer(rootType, null);
            } else {
                valueSer = findValueSerializer(value.getClass(), null);
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
     * 
     * @since 2.3
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
        String msg = e.getMessage();
        if (msg == null) {
            msg = "[no message for "+e.getClass().getName()+"]";
        }
        return new JsonMappingException(g, msg, e);
    }

    /*
    /********************************************************
    /* Access to caching details
    /********************************************************
     */

    /**
     * Method that can be used to determine how many serializers this
     * provider is caching currently
     * (if it does caching: default implementation does)
     * Exact count depends on what kind of serializers get cached;
     * default implementation caches all serializers, including ones that
     * are eagerly constructed (for optimal access speed)
     *<p> 
     * The main use case for this method is to allow conditional flushing of
     * serializer cache, if certain number of entries is reached.
     */
    public int cachedSerializersCount() {
        return _serializerCache.size();
    }

    /**
     * Method that will drop all serializers currently cached by this provider.
     * This can be used to remove memory usage (in case some serializers are
     * only used once or so), or to force re-construction of serializers after
     * configuration changes for mapper than owns the provider.
     */
    public void flushCachedSerializers() {
        _serializerCache.flush();
    }

    /*
    /**********************************************************
    /* Extended API called by ObjectMapper: other
    /**********************************************************
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
        /* no need for embedded type information for JSON schema generation (all
         * type information it needs is accessible via "untyped" serializer)
         */
        visitor.setProvider(this);
        findValueSerializer(javaType, null).acceptJsonFormatVisitor(visitor, javaType);
    }

    /**
     * The method to be called by {@link ObjectMapper}
     * to generate <a href="http://json-schema.org/">JSON schema</a> for
     * given type.
     *
     * @param type The type for which to generate schema
     * 
     * @deprecated Should not be used any more
     */
    @Deprecated // since 2.6
    public com.fasterxml.jackson.databind.jsonschema.JsonSchema generateJsonSchema(Class<?> type)
        throws JsonMappingException
    {
        /* no need for embedded type information for JSON schema generation (all
         * type information it needs is accessible via "untyped" serializer)
         */
        JsonSerializer<Object> ser = findValueSerializer(type, null);
        JsonNode schemaNode = (ser instanceof SchemaAware) ?
                ((SchemaAware) ser).getSchema(this, null) : com.fasterxml.jackson.databind.jsonschema.JsonSchema.getDefaultSchemaNode();
        if (!(schemaNode instanceof ObjectNode)) {
            throw new IllegalArgumentException("Class " + type.getName()
                    +" would not be serialized as a JSON object and therefore has no schema");
        }
        return new com.fasterxml.jackson.databind.jsonschema.JsonSchema((ObjectNode) schemaNode);
    }
    
    
    /*
    /**********************************************************
    /* Helper classes
    /**********************************************************
     */

    /**
     * Concrete implementation that defines factory method(s),
     * defined as final.
     */
    public final static class Impl extends DefaultSerializerProvider {
        private static final long serialVersionUID = 1L;

        public Impl() { super(); }
        public Impl(Impl src) { super(src); }

        protected Impl(SerializerProvider src, SerializationConfig config,
                SerializerFactory f) {
            super(src, config, f);
        }

        @Override
        public DefaultSerializerProvider copy()
        {
            if (getClass() != Impl.class) {
                return super.copy();
            }
            return new Impl(this);
        }
        
        @Override
        public Impl createInstance(SerializationConfig config, SerializerFactory jsf) {
            return new Impl(this, config, jsf);
        }
    }
}
