package com.fasterxml.jackson.databind.ser;

import java.util.*;

import com.fasterxml.jackson.annotation.ObjectIdGenerator;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.TokenStreamFactory;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.cfg.GeneratorSettings;
import com.fasterxml.jackson.databind.cfg.HandlerInstantiator;
import com.fasterxml.jackson.databind.introspect.Annotated;
import com.fasterxml.jackson.databind.introspect.BeanPropertyDefinition;
import com.fasterxml.jackson.databind.jsonFormatVisitors.JsonFormatVisitorWrapper;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.TreeBuildingGenerator;
import com.fasterxml.jackson.databind.util.ClassUtil;
import com.fasterxml.jackson.databind.util.TokenBuffer;

/**
 * Extension over {@link SerializerProvider} that adds methods needed by
 * {@link ObjectMapper} (and {@link ObjectWriter}) but that are not to be exposed
 * as general context during serialization.
 *<p>
 * Also note that all custom {@link SerializerProvider}
 * implementations must sub-class this class: {@link ObjectMapper}
 * requires this type, not basic provider type.
 */
public class SerializationContextExt
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

    protected SerializationContextExt(TokenStreamFactory streamFactory,
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
    public ValueSerializer<Object> serializerInstance(Annotated annotated, Object serDef)
    {
        if (serDef == null) {
            return null;
        }
        ValueSerializer<?> ser;
        
        if (serDef instanceof ValueSerializer) {
            ser = (ValueSerializer<?>) serDef;
        } else {
            // Alas, there's no way to force return type of "either class
            // X or Y" -- need to throw an exception after the fact
            if (!(serDef instanceof Class)) {
                reportBadDefinition(annotated.getType(),
                        "AnnotationIntrospector returned serializer definition of type "
                        +serDef.getClass().getName()+"; expected type `ValueSerializer` or `Class<ValueSerializer>` instead");
            }
            Class<?> serClass = (Class<?>)serDef;
            // there are some known "no class" markers to consider too:
            if (serClass == ValueSerializer.None.class || ClassUtil.isBogusClass(serClass)) {
                return null;
            }
            if (!ValueSerializer.class.isAssignableFrom(serClass)) {
                reportBadDefinition(annotated.getType(),
                        "AnnotationIntrospector returned Class `"
                        +serClass.getName()+"`; expected `Class<ValueSerializer>`");
            }
            HandlerInstantiator hi = _config.getHandlerInstantiator();
            ser = (hi == null) ? null : hi.serializerInstance(_config, annotated, serClass);
            if (ser == null) {
                ser = (ValueSerializer<?>) ClassUtil.createInstance(serClass,
                        _config.canOverrideAccessModifiers());
            }
        }
        return (ValueSerializer<Object>) _handleResolvable(ser);
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
    public boolean includeFilterSuppressNulls(Object filter)
    {
        if (filter == null) {
            return true;
        }
        // should let filter decide what to do with nulls:
        // But just in case, let's handle unexpected (from our perspective) problems explicitly
        try {
            return filter.equals(null);
        } catch (Exception e) {
            String msg = String.format(
"Problem determining whether filter of type '%s' should filter out `null` values: (%s) %s",
filter.getClass().getName(), e.getClass().getName(), ClassUtil.exceptionMessage(e));
            reportBadDefinition(filter.getClass(), msg, e);
            return false; // never gets here
        }
    }

    /*
    /**********************************************************************
    /* Abstract method impls, serialization-like methods
    /**********************************************************************
     */

    /**
     * Method that
     * will convert given Java value (usually bean) into its
     * equivalent Tree model {@link JsonNode} representation.
     * Functionally similar to serializing value into token stream and parsing that
     * stream back as tree model node,
     * but more efficient as {@link TokenBuffer} is used to contain the intermediate
     * representation instead of fully serialized contents.
     *<p>
     * NOTE: while results are usually identical to that of serialization followed
     * by deserialization, this is not always the case. In some cases serialization
     * into intermediate representation will retain encapsulation of things like
     * raw value ({@link com.fasterxml.jackson.databind.util.RawValue}) or basic
     * node identity ({@link JsonNode}). If so, result is a valid tree, but values
     * are not re-constructed through actual format representation. So if transformation
     * requires actual materialization of encoded content,
     * it will be necessary to do actual serialization.
     * 
     * @param <T> Actual node type; usually either basic {@link JsonNode} or
     *  {@link com.fasterxml.jackson.databind.node.ObjectNode}
     * @param fromValue Java value to convert
     *
     * @return (non-null) Root node of the resulting content tree: in case of
     *   {@code null} value node for which {@link JsonNode#isNull()} returns {@code true}.
     *
     * @since 3.0
     */
    @Override
    @SuppressWarnings({ "unchecked" })
    public <T extends JsonNode> T valueToTree(Object fromValue)
        throws JacksonException
    {
        final JsonNodeFactory nodeF = _config.getNodeFactory();
        if (fromValue == null) {
            return (T) nodeF.nullNode();
        }

        try (TreeBuildingGenerator gen = TreeBuildingGenerator.forSerialization(this, nodeF)) {
            // NOTE: inlined "writeValue(generator, value)" to streamline, force no root wrap:
            final Class<?> rawType = fromValue.getClass();
            final ValueSerializer<Object> ser = findTypedValueSerializer(rawType, true);
            _serialize(gen, fromValue, ser);
            return (T) gen.treeBuilt();
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
    public void serializeValue(JsonGenerator gen, Object value) throws JacksonException
    {
        _assignGenerator(gen);
        if (value == null) {
            _serializeNull(gen);
            return;
        }
        final Class<?> cls = value.getClass();
        // true, since we do want to cache root-level typed serializers (ditto for null property)
        final ValueSerializer<Object> ser = findTypedValueSerializer(cls, true);
        PropertyName rootName = _config.getFullRootName();
        if (rootName == null) { // not explicitly specified
            if (_config.isEnabled(SerializationFeature.WRAP_ROOT_VALUE)) {
                _serialize(gen, value, ser, findRootName(cls));
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
    public void serializeValue(JsonGenerator gen, Object value, JavaType rootType) throws JacksonException
    {
        _assignGenerator(gen);
        if (value == null) {
            _serializeNull(gen);
            return;
        }
        // Let's ensure types are compatible at this point
        if (!rootType.getRawClass().isAssignableFrom(value.getClass())) {
            _reportIncompatibleRootType(value, rootType);
        }
        // root value, not reached via property:
        ValueSerializer<Object> ser = findTypedValueSerializer(rootType, true);
        PropertyName rootName = _config.getFullRootName();
        if (rootName == null) { // not explicitly specified
            if (_config.isEnabled(SerializationFeature.WRAP_ROOT_VALUE)) {
                _serialize(gen, value, ser, findRootName(rootType));
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
     * {@link ValueSerializer} to use.
     * 
     * @param rootType Type to use for locating serializer to use, instead of actual
     *    runtime type, if no serializer is passed
     * @param ser Root Serializer to use, if not null
     */
    public void serializeValue(JsonGenerator gen, Object value, JavaType rootType,
            ValueSerializer<Object> ser) throws JacksonException
    {
        _assignGenerator(gen);
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
                        ? findRootName(value.getClass())
                        : findRootName(rootType);
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
            ValueSerializer<Object> valueSer, TypeSerializer typeSer)
        throws JacksonException
    {
        _assignGenerator(gen);
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
                PropertyName pname = findRootName(value.getClass());
                gen.writeName(pname.simpleAsEncoded(_config));
            }
        } else if (rootName.isEmpty()) {
            wrap = false;
        } else {
            wrap = true;
            gen.writeStartObject();
            gen.writeName(rootName.getSimpleName());
        }
        valueSer.serializeWithType(value, gen, this, typeSer);
        if (wrap) {
            gen.writeEndObject();
        }
    }

    private final void _serialize(JsonGenerator gen, Object value,
            ValueSerializer<Object> ser, PropertyName rootName)
        throws JacksonException
    {
        gen.writeStartObject();
        gen.writeName(rootName.simpleAsEncoded(_config));
        ser.serialize(value, gen, this);
        gen.writeEndObject();
    }

    private final void _serialize(JsonGenerator gen, Object value,
            ValueSerializer<Object> ser)
        throws JacksonException
    {
        ser.serialize(value, gen, this);
    }

    /**
     * Helper method called when root value to serialize is null
     */
    protected void _serializeNull(JsonGenerator gen) throws JacksonException
    {
        ValueSerializer<Object> ser = getDefaultNullValueSerializer();
        ser.serialize(null, gen, this);
    }

    /*
    /**********************************************************************
    /* Extended API called by ObjectMapper: other
    /**********************************************************************
     */

    /**
     * The method to be called by {@link ObjectMapper} and {@link ObjectWriter}
     * to expose the format of the given type to the given visitor
     *
     * @param javaType The type for which to generate format
     * @param visitor the visitor to accept the format
     */
    public void acceptJsonFormatVisitor(JavaType javaType, JsonFormatVisitorWrapper visitor)
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
    /* Other helper methods
    /**********************************************************************
     */

    private void _assignGenerator(JsonGenerator g) {
        _generator = g;
        _writeCapabilities = g.streamWriteCapabilities();
    }

    /*
    /**********************************************************************
    /* Helper classes
    /**********************************************************************
     */

    /**
     * Concrete implementation defined separately so it can be declared `final`.
     * Alternate implements should instead just extend {@link SerializationContextExt}
     */
    public final static class Impl
        extends SerializationContextExt
    {
        public Impl(TokenStreamFactory streamFactory,
                SerializationConfig config, GeneratorSettings genSettings,
                SerializerFactory f, SerializerCache cache) {
            super(streamFactory, config, genSettings, f, cache);
        }
    }
}
