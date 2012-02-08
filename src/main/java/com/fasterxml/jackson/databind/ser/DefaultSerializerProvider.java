package com.fasterxml.jackson.databind.ser;

import java.io.IOException;
import java.util.*;

import com.fasterxml.jackson.annotation.ObjectIdGenerator;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonGenerator;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.jsonschema.JsonSchema;
import com.fasterxml.jackson.databind.jsonschema.SchemaAware;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Standard implementation used by {@link ObjectMapper}:
 * adds methods only exposed to {@link ObjectMapper},
 * as well as constructors.
 *<p>
 * Note that class is abstract just because it does not
 * define
 */
public abstract class DefaultSerializerProvider extends SerializerProvider
{
    /*
    /**********************************************************
    /* State, for non-blueprint instances: Object Id handling
    /**********************************************************
     */

    protected ArrayList<ObjectIdGenerator<?>> _objectIds;
    
    /*
    /**********************************************************
    /* Life-cycle
    /**********************************************************
     */

    protected DefaultSerializerProvider() { super(); }

    protected DefaultSerializerProvider( SerializerProvider src,
            SerializationConfig config,SerializerFactory f) {
        super(src, config, f);
    }

    /*
    /**********************************************************
    /* Extended API: methods that ObjectMapper will call
    /**********************************************************
     */

    /**
     * Overridable method, used to create a non-blueprint instances from the blueprint.
     * This is needed to retain state during serialization.
     */
    public abstract DefaultSerializerProvider createInstance(SerializationConfig config,
            SerializerFactory jsf);
    
    /**
     * The method to be called by {@link ObjectMapper} and {@link ObjectWriter}
     * for serializing given value, using serializers that
     * this provider has access to (via caching and/or creating new serializers
     * as need be).
     */
    public void serializeValue(JsonGenerator jgen, Object value)
        throws IOException, JsonGenerationException
    {
        JsonSerializer<Object> ser;
        boolean wrap;

        if (value == null) {
            // no type provided; must just use the default null serializer
            ser = getDefaultNullValueSerializer();
            wrap = false; // no name to use for wrapping; can't do!
        } else {
            Class<?> cls = value.getClass();
            // true, since we do want to cache root-level typed serializers (ditto for null property)
            ser = findTypedValueSerializer(cls, true, null);

            // Ok: should we wrap result in an additional property ("root name")?
            String rootName = _config.getRootName();
            if (rootName == null) { // not explicitly specified
                // [JACKSON-163]
                wrap = _config.isEnabled(SerializationFeature.WRAP_ROOT_VALUE);
                if (wrap) {
                    jgen.writeStartObject();
                    jgen.writeFieldName(_rootNames.findRootName(value.getClass(), _config));
                }
            } else if (rootName.length() == 0) {
                wrap = false;
            } else { // [JACKSON-764]
                // empty String means explicitly disabled; non-empty that it is enabled
                wrap = true;
                jgen.writeStartObject();
                jgen.writeFieldName(rootName);
            }
        }
        try {
            ser.serialize(value, jgen, this);
            if (wrap) {
                jgen.writeEndObject();
            }
        } catch (IOException ioe) {
            /* As per [JACKSON-99], should not wrap IOException or its
             * sub-classes (like JsonProcessingException, JsonMappingException)
             */
            throw ioe;
        } catch (Exception e) {
            // but others are wrapped
            String msg = e.getMessage();
            if (msg == null) {
                msg = "[no message for "+e.getClass().getName()+"]";
            }
            throw new JsonMappingException(msg, e);
        }
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
    public void serializeValue(JsonGenerator jgen, Object value,
            JavaType rootType)
        throws IOException, JsonGenerationException
    {
        boolean wrap;

        JsonSerializer<Object> ser;
        if (value == null) {
            ser = getDefaultNullValueSerializer();
            wrap = false;
        } else {
            // Let's ensure types are compatible at this point
            if (!rootType.getRawClass().isAssignableFrom(value.getClass())) {
                _reportIncompatibleRootType(value, rootType);
            }
            // root value, not reached via property:
            ser = findTypedValueSerializer(rootType, true, null);
            // [JACKSON-163]
            wrap = _config.isEnabled(SerializationFeature.WRAP_ROOT_VALUE);
            if (wrap) {
                jgen.writeStartObject();
                jgen.writeFieldName(_rootNames.findRootName(rootType, _config));
            }
        }
        try {
            ser.serialize(value, jgen, this);
            if (wrap) {
                jgen.writeEndObject();
            }
        } catch (IOException ioe) { // no wrapping for IO (and derived)
            throw ioe;
        } catch (Exception e) { // but others do need to be, to get path etc
            String msg = e.getMessage();
            if (msg == null) {
                msg = "[no message for "+e.getClass().getName()+"]";
            }
            throw new JsonMappingException(msg, e);
        }
    }

    /**
     * The method to be called by {@link ObjectMapper} and {@link ObjectWriter}
     * to generate <a href="http://json-schema.org/">JSON schema</a> for
     * given type.
     *
     * @param type The type for which to generate schema
     */
    public JsonSchema generateJsonSchema(Class<?> type)
        throws JsonMappingException
    {
        if (type == null) {
            throw new IllegalArgumentException("A class must be provided");
        }
        /* no need for embedded type information for JSON schema generation (all
         * type information it needs is accessible via "untyped" serializer)
         */
        JsonSerializer<Object> ser = findValueSerializer(type, null);
        JsonNode schemaNode = (ser instanceof SchemaAware) ?
                ((SchemaAware) ser).getSchema(this, null) : 
                JsonSchema.getDefaultSchemaNode();
        if (!(schemaNode instanceof ObjectNode)) {
            throw new IllegalArgumentException("Class " + type.getName() +
                    " would not be serialized as a JSON object and therefore has no schema");
        }
        return new JsonSchema((ObjectNode) schemaNode);
    }

    /**
     * Method that can be called to see if this serializer provider
     * can find a serializer for an instance of given class.
     *<p>
     * Note that no Exceptions are thrown, including unchecked ones:
     * implementations are to swallow exceptions if necessary.
     */
    public boolean hasSerializerFor(Class<?> cls) {
        return _findExplicitUntypedSerializer(cls, null) != null;
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
    /* Object Id handling
    /**********************************************************
     */

    public final ObjectIdGenerator<?> objectIdsFor(ObjectIdGenerator<?> blueprint)
    {
        if (_objectIds != null) {
            for (int i = 0, len = _objectIds.size(); i < len; ++i) {
                ObjectIdGenerator<?> gen = _objectIds.get(i);
                if (gen.canUseFor(blueprint)) {
                    return gen;
                }
            }
        }
        // not yet constructed; construct, append
        if (_objectIds == null) {
            _objectIds = new ArrayList<ObjectIdGenerator<?>>(8);
        }
        ObjectIdGenerator<?> gen = blueprint.newForSerialization();
        _objectIds.add(gen);
        return gen;
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
    public final static class Impl extends DefaultSerializerProvider
    {
        public Impl() { super(); }

        protected Impl( SerializerProvider src,
                SerializationConfig config,SerializerFactory f) {
            super(src, config, f);
        }

        @Override
        public Impl createInstance(SerializationConfig config,
                SerializerFactory jsf) {
            return new Impl(this, config, jsf);
        }
    }
}
