package com.fasterxml.jackson.databind;

import java.io.Closeable;
import java.io.IOException;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.databind.ser.DefaultSerializerProvider;

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

    public void writeValue(Object value) throws IOException
    {
        if (_cfgCloseCloseable && (value instanceof Closeable)) {
            _writeCloseableValue(value);
        } else {
            if (_rootType == null) {
                _provider.serializeValue(_generator, value);
            } else {
                _provider.serializeValue(_generator, value, _rootType, _rootSerializer);
            }
            if (_cfgFlush) {
                _generator.flush();
            }
        }
    }

    protected void _writeCloseableValue(Object value) throws IOException
    {
        Closeable toClose = (Closeable) value;
        try {
            if (_rootType == null) {
                _provider.serializeValue(_generator, value);
            } else {
                _provider.serializeValue(_generator, value, _rootType, _rootSerializer);
            }
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
}
