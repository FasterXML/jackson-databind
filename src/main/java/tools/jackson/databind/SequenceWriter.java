package tools.jackson.databind;

import java.io.Closeable;
import java.io.IOException;
import java.util.Collection;

import tools.jackson.core.*;
import tools.jackson.core.exc.JacksonIOException;
import tools.jackson.databind.jsontype.TypeSerializer;
import tools.jackson.databind.ser.SerializationContextExt;
import tools.jackson.databind.ser.impl.PropertySerializerMap;
import tools.jackson.databind.ser.impl.TypeWrappedSerializer;

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
 */
public class SequenceWriter
    implements Versioned, java.io.Closeable, java.io.Flushable
{
    /*
    /**********************************************************************
    /* Configuration
    /**********************************************************************
     */

    protected final SerializationContextExt _provider;
    protected final SerializationConfig _config;
    protected final JsonGenerator _generator;

    protected final ValueSerializer<Object> _rootSerializer;
    protected final TypeSerializer _typeSerializer;

    protected final boolean _closeGenerator;
    protected final boolean _cfgFlush;
    protected final boolean _cfgCloseCloseable;

    /*
    /**********************************************************************
    /* State
    /**********************************************************************
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
    /**********************************************************************
    /* Life-cycle
    /**********************************************************************
     */

    public SequenceWriter(SerializationContextExt prov, JsonGenerator gen,
            boolean closeGenerator, ObjectWriter.Prefetch prefetch)
    {
        _provider = prov;
        _generator = gen;
        _closeGenerator = closeGenerator;
        _rootSerializer = prefetch.getValueSerializer();
        _typeSerializer = prefetch.getTypeSerializer();

        _config = prov.getConfig();
        _cfgFlush = _config.isEnabled(SerializationFeature.FLUSH_AFTER_WRITE_VALUE);
        _cfgCloseCloseable = _config.isEnabled(SerializationFeature.CLOSE_CLOSEABLE);
        // important: need to cache "root value" serializers, to handle polymorphic
        // types properly
        _dynamicSerializers = PropertySerializerMap.emptyForRootValues();
    }

    /**
     * Internal method called by {@link ObjectWriter}: should not be called by code
     * outside {@code jackson-databind} classes.
     */
    public SequenceWriter init(boolean wrapInArray) throws JacksonException
    {
        if (wrapInArray) {
            _generator.writeStartArray();
            _openArray = true;
        }
        return this;
    }

    /*
    /**********************************************************************
    /* Public API, basic accessors
    /**********************************************************************
     */

    /**
     * Method that will return version information stored in and read from jar
     * that contains this class.
     */
    @Override
    public Version version() {
        return tools.jackson.databind.cfg.PackageVersion.VERSION;
    }

    /*
    /**********************************************************************
    /* Public API, write operations, related
    /**********************************************************************
     */

    /**
     * Method for writing given value into output, as part of sequence
     * to write. If root type was specified for {@link ObjectWriter},
     * value must be of compatible type (same or subtype).
     */
    public SequenceWriter write(Object value) throws JacksonException
    {
        if (value == null) {
            _provider.serializeValue(_generator, null);
            return this;
        }

        if (_cfgCloseCloseable && (value instanceof Closeable)) {
            return _writeCloseableValue(value);
        }
        ValueSerializer<Object> ser = _rootSerializer;
        if (ser == null) {
            Class<?> type = value.getClass();
            ser = _dynamicSerializers.serializerFor(type);
            if (ser == null) {
                ser = _findAndAddDynamic(type);
            }
        }
        _provider.serializeValue(_generator, value, null, ser);
        if (_cfgFlush) {
            _generator.flush();
        }
        return this;
    }

    /**
     * Method for writing given value into output, as part of sequence
     * to write; further, full type (often generic, like {@link java.util.Map}
     * is passed in case a new
     * {@link ValueSerializer} needs to be fetched to handle type
     *
     * If root type was specified for {@link ObjectWriter},
     * value must be of compatible type (same or subtype).
     */
    public SequenceWriter write(Object value, JavaType type) throws JacksonException
    {
        if (value == null) {
            _provider.serializeValue(_generator, null);
            return this;
        }

        if (_cfgCloseCloseable && (value instanceof Closeable)) {
            return _writeCloseableValue(value, type);
        }
        /* 15-Dec-2014, tatu: I wonder if this could become problematic. It shouldn't
         *   really, since trying to use differently paramterized types in a sequence
         *   is likely to run into other issues. But who knows; if it does become an
         *   issue, may need to implement alternative, JavaType-based map.
         */
        ValueSerializer<Object> ser = _dynamicSerializers.serializerFor(type.getRawClass());
        if (ser == null) {
            ser = _findAndAddDynamic(type);
        }
        _provider.serializeValue(_generator, value, type, ser);
        if (_cfgFlush) {
            _generator.flush();
        }
        return this;
    }

    public SequenceWriter writeAll(Object[] value) throws JacksonException
    {
        for (int i = 0, len = value.length; i < len; ++i) {
            write(value[i]);
        }
        return this;
    }

    // NOTE: redundant wrt variant that takes Iterable, but cannot remove or even
    // deprecate due to backwards-compatibility needs
    public <C extends Collection<?>> SequenceWriter writeAll(C container) throws JacksonException {
        for (Object value : container) {
            write(value);
        }
        return this;
    }

    public SequenceWriter writeAll(Iterable<?> iterable) throws JacksonException
    {
        for (Object value : iterable) {
            write(value);
        }
        return this;
    }

    @Override
    public void flush() {
        if (!_closed) {
            _generator.flush();
        }
    }

    @Override
    public void close()
    {
        if (!_closed) {
            _closed = true;
            if (_openArray) {
                _openArray = false;
                _generator.writeEndArray();
            }
            if (_closeGenerator) {
                _generator.close();
            }
        }
    }

    /*
    /**********************************************************************
    /* Internal helper methods, serializer lookups
    /**********************************************************************
     */

    protected SequenceWriter _writeCloseableValue(Object value) throws JacksonException
    {
        Closeable toClose = (Closeable) value;
        try {
            ValueSerializer<Object> ser = _rootSerializer;
            if (ser == null) {
                Class<?> type = value.getClass();
                ser = _dynamicSerializers.serializerFor(type);
                if (ser == null) {
                    ser = _findAndAddDynamic(type);
                }
            }
            _provider.serializeValue(_generator, value, null, ser);
            if (_cfgFlush) {
                _generator.flush();
            }
            Closeable tmpToClose = toClose;
            toClose = null;
            try {
                tmpToClose.close();
            } catch (IOException e) {
                throw JacksonIOException.construct(e, _generator);
            }
        } finally {
            if (toClose != null) { // only if there was other throwable
                try {
                    toClose.close();
                } catch (IOException ioe) { }
            }
        }
        return this;
    }

    protected SequenceWriter _writeCloseableValue(Object value, JavaType type)
        throws JacksonException
    {
        Closeable toClose = (Closeable) value;
        try {
            // 15-Dec-2014, tatu: As per above, could be problem that we do not pass generic type
            ValueSerializer<Object> ser = _dynamicSerializers.serializerFor(type.getRawClass());
            if (ser == null) {
                ser = _findAndAddDynamic(type);
            }
            _provider.serializeValue(_generator, value, type, ser);
            if (_cfgFlush) {
                _generator.flush();
            }
            Closeable tmpToClose = toClose;
            toClose = null;
            try {
                tmpToClose.close();
            } catch (IOException e) {
                throw JacksonIOException.construct(e);
            }
        } finally {
            if (toClose != null) { // only if there was another throwable
                try {
                    toClose.close();
                } catch (IOException ioe) { }
            }
        }
        return this;
    }

    private final ValueSerializer<Object> _findAndAddDynamic(Class<?> type)
    {
        PropertySerializerMap.SerializerAndMapResult result;
        if (_typeSerializer == null) {
            result = _dynamicSerializers.findAndAddRootValueSerializer(type, _provider);
        } else {
            result = _dynamicSerializers.addSerializer(type,
                    new TypeWrappedSerializer(_typeSerializer,
                            _provider.findRootValueSerializer(type)));
        }
        _dynamicSerializers = result.map;
        return result.serializer;
    }

    private final ValueSerializer<Object> _findAndAddDynamic(JavaType type)
    {
        PropertySerializerMap.SerializerAndMapResult result;
        if (_typeSerializer == null) {
            result = _dynamicSerializers.findAndAddRootValueSerializer(type, _provider);
        } else {
            result = _dynamicSerializers.addSerializer(type,
                    new TypeWrappedSerializer(_typeSerializer,
                            _provider.findRootValueSerializer(type)));
        }
        _dynamicSerializers = result.map;
        return result.serializer;
    }
}
