package com.fasterxml.jackson.databind;

import java.io.Closeable;
import java.io.IOException;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.databind.ser.DefaultSerializerProvider;
import com.fasterxml.jackson.databind.ser.impl.PropertySerializerMap;

/**
 * Writer class similar to {@link ObjectWriter}, except that it can be used
 * for writing sequences of values, not just a single value.
 * The main use case is in writing very long sequences, or sequences where
 * values are incrementally produced; cases where it would be impractical
 * or at least inconvenient to construct a wrapper container around values
 * (or where no JSON array is desired around values).
 *<p>
 * Differences from {@link ObjectWriter} include:
 *<ul>
 *  <li>Instances of {@link SequenceWriter} are stateful, and not thread-safe:
 *    if sharing, external synchronization must be used.
 *  <li>Explicit {@link #close} is needed after all values have been written
 *     ({@link ObjectWriter} can auto-close after individual value writes)
 *</ul>
 * 
 * @since 2.5
 */
public class SequenceWriter
    implements Versioned, java.io.Closeable
{
    /*
    /**********************************************************
    /* Configuration
    /**********************************************************
     */

    protected final DefaultSerializerProvider _provider;
    protected final SerializationConfig _config;
    protected final JsonGenerator _generator;

    protected final JavaType _rootType;
    protected final JsonSerializer<Object> _rootSerializer;
    
    protected final boolean _closeGenerator;
    protected final boolean _cfgFlush;
    protected final boolean _cfgCloseCloseable;

    /*
    /**********************************************************
    /* State
    /**********************************************************
     */

    /**
     * If {@link #_rootSerializer} is not defined (no root type
     * was used for constructing {@link ObjectWriter}), we will
     * use simple scheme for keeping track of serializers needed.
     * Assumption is that
     */
    protected PropertySerializerMap _dynamicSerializers;
    
    /**
     * State flag for keeping track of need to write matching END_ARRAY,
     * if a START_ARRAY was written during initialization
     */
    protected boolean _openArray;
    protected boolean _closed;

    /*
    /**********************************************************
    /* Life-cycle
    /**********************************************************
     */

    public SequenceWriter(DefaultSerializerProvider prov, JsonGenerator gen,
            boolean closeGenerator, JavaType rootType, JsonSerializer<Object> rootSerializer)
                    throws IOException
    {
        _provider = prov;
        _generator = gen;
        _closeGenerator = closeGenerator;
        _rootType = rootType;
        _rootSerializer = rootSerializer;
        _config = prov.getConfig();
        _cfgFlush = _config.isEnabled(SerializationFeature.FLUSH_AFTER_WRITE_VALUE);
        _cfgCloseCloseable = _config.isEnabled(SerializationFeature.CLOSE_CLOSEABLE);
        // important: need to cache "root value" serializers, to handle polymorphic
        // types properly
        _dynamicSerializers = PropertySerializerMap.emptyForRootValues();
    }

    public SequenceWriter init(boolean wrapInArray) throws IOException
    {
        if (wrapInArray) {
            _generator.writeStartArray();
            _openArray = true;
        }
        return this;
    }

    /*
    /**********************************************************
    /* Public API, basic accessors
    /**********************************************************
     */

    /**
     * Method that will return version information stored in and read from jar
     * that contains this class.
     */
    @Override
    public Version version() {
        return com.fasterxml.jackson.databind.cfg.PackageVersion.VERSION;
    }

    /*
    /**********************************************************
    /* Public API, write operations, related
    /**********************************************************
     */

    /**
     * Method for writing given value into output, as part of sequence
     * to write. If root type was specified for {@link ObjectWriter},
     * value must be of compatible type (same or subtype).
     */
    public void writeValue(Object value) throws IOException
    {
        if (value == null) {
            _provider.serializeValue(_generator, null);
            return;
        }
        
        if (_cfgCloseCloseable && (value instanceof Closeable)) {
            _writeCloseableValue(value);
            return;
        }
        JsonSerializer<Object> ser = _rootSerializer;
        if (ser == null) {
            Class<?> type = value.getClass();
            ser = _dynamicSerializers.serializerFor(type);
            if (ser == null) {
                ser = _findAndAddDynamic(type);
            }
        }
        _provider.serializeValue(_generator, value, _rootType, ser);
        if (_cfgFlush) {
            _generator.flush();
        }
    }

    /**
     * Method for writing given value into output, as part of sequence
     * to write; further, full type (often generic, like {@link java.util.Map}
     * is passed in case a new
     * {@link JsonSerializer} needs to be fetched to handle type
     * 
     * If root type was specified for {@link ObjectWriter},
     * value must be of compatible type (same or subtype).
     */
    public void writeValue(Object value, JavaType type) throws IOException
    {
        if (value == null) {
            _provider.serializeValue(_generator, null);
            return;
        }
        
        if (_cfgCloseCloseable && (value instanceof Closeable)) {
            _writeCloseableValue(value, type);
            return;
        }
        /* 15-Dec-2014, tatu: I wonder if this could be come problematic. It shouldn't
         *   really, since trying to use differently paramterized types in a sequence
         *   is likely to run into other issues. But who knows; if it does become an
         *   issue, may need to implement alternative, JavaType-based map.
         */
        JsonSerializer<Object> ser = _dynamicSerializers.serializerFor(type.getRawClass());
        if (ser == null) {
            ser = _findAndAddDynamic(type);
        }
        _provider.serializeValue(_generator, value, type, ser);
        if (_cfgFlush) {
            _generator.flush();
        }
    }
    
    @Override
    public void close() throws IOException
    {
        if (!_closed) {
            _closed = true;
            if (_openArray) {
                _openArray = false;
                _generator.writeEndArray();
            }
        }
    }

    /*
    /**********************************************************
    /* Internal helper methods, serializer lookups
    /**********************************************************
     */

    protected void _writeCloseableValue(Object value) throws IOException
    {
        Closeable toClose = (Closeable) value;
        try {
            JsonSerializer<Object> ser = _rootSerializer;
            if (ser == null) {
                Class<?> type = value.getClass();
                ser = _dynamicSerializers.serializerFor(type);
                if (ser == null) {
                    ser = _findAndAddDynamic(type);
                }
                _provider.serializeValue(_generator, value, null, ser);
            }
            _provider.serializeValue(_generator, value, _rootType, ser);
            if (_cfgFlush) {
                _generator.flush();
            }
            Closeable tmpToClose = toClose;
            toClose = null;
            tmpToClose.close();
        } finally {
            if (toClose != null) {
                try {
                    toClose.close();
                } catch (IOException ioe) { }
            }
        }
    }

    protected void _writeCloseableValue(Object value, JavaType type) throws IOException
    {
        Closeable toClose = (Closeable) value;
        try {
            // 15-Dec-2014, tatu: As per above, could be problem that we do not pass generic type
            JsonSerializer<Object >ser = _dynamicSerializers.serializerFor(type.getRawClass());
            if (ser == null) {
                ser = _findAndAddDynamic(type);
            }
            _provider.serializeValue(_generator, value, type, ser);
            if (_cfgFlush) {
                _generator.flush();
            }
            Closeable tmpToClose = toClose;
            toClose = null;
            tmpToClose.close();
        } finally {
            if (toClose != null) {
                try {
                    toClose.close();
                } catch (IOException ioe) { }
            }
        }
    }

    protected final JsonSerializer<Object> _findAndAddDynamic(Class<?> type) throws JsonMappingException
    {
        PropertySerializerMap.SerializerAndMapResult result
            = _dynamicSerializers.findAndAddPrimarySerializer(type, _provider, null);
        _dynamicSerializers = result.map;
        return result.serializer;
    }

    protected final JsonSerializer<Object> _findAndAddDynamic(JavaType type) throws JsonMappingException
    {
        PropertySerializerMap.SerializerAndMapResult result
            = _dynamicSerializers.findAndAddPrimarySerializer(type, _provider, null);
        _dynamicSerializers = result.map;
        return result.serializer;
    }
}
