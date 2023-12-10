package tools.jackson.databind.util;

import java.io.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.TreeMap;

import tools.jackson.core.*;
import tools.jackson.core.base.ParserMinimalBase;
import tools.jackson.core.exc.InputCoercionException;
import tools.jackson.core.exc.StreamWriteException;
import tools.jackson.core.io.NumberInput;
import tools.jackson.core.io.NumberOutput;
import tools.jackson.core.sym.PropertyNameMatcher;
import tools.jackson.core.util.ByteArrayBuilder;
import tools.jackson.core.util.JacksonFeatureSet;
import tools.jackson.core.util.SimpleStreamWriteContext;
import tools.jackson.databind.*;

/**
 * Utility class used for efficient storage of {@link JsonToken}
 * sequences, needed for temporary buffering.
 * Space efficient for different sequence lengths (especially so for smaller
 * ones; but not significantly less efficient for larger), highly efficient
 * for linear iteration and appending. Implemented as segmented/chunked
 * linked list of tokens; only modifications are via appends.
 */
public class TokenBuffer
// Won't use JsonGeneratorBase, to minimize overhead for validity checking
    extends JsonGenerator
{
    protected final static int DEFAULT_STREAM_WRITE_FEATURES = StreamWriteFeature.collectDefaults();

    // Should work for now
    protected final static JacksonFeatureSet<StreamWriteCapability> BOGUS_WRITE_CAPABILITIES
        = JacksonFeatureSet.fromDefaults(StreamWriteCapability.values());

    /*
    /**********************************************************************
    /* Configuration
    /**********************************************************************
     */

    /**
     * Parse context from "parent" parser (one from which content to buffer is read,
     * if specified). Used, if available, when reading content, to present full
     * context as if content was read from the original parser: this is useful
     * in error reporting and sometimes processing as well.
     */
    protected TokenStreamContext _parentContext;

    /**
     * Bit flag composed of bits that indicate which
     * {@link StreamWriteFeature}s
     * are enabled.
     *<p>
     * NOTE: most features have no effect on this class
     */
    protected int _streamWriteFeatures;

    protected final StreamReadConstraints _streamReadConstraints;

    protected boolean _closed;

    protected boolean _hasNativeTypeIds;

    protected boolean _hasNativeObjectIds;

    protected boolean _mayHaveNativeIds;

    /**
     * Flag set during construction, if use of {@link BigDecimal} is to be forced
     * on all floating-point values.
     */
    protected boolean _forceBigDecimal;

    /*
    /**********************************************************************
    /* Token buffering state
    /**********************************************************************
     */

    /**
     * First segment, for contents this buffer has
     */
    protected Segment _first;

    /**
     * Last segment of this buffer, one that is used
     * for appending more tokens
     */
    protected Segment _last;

    /**
     * Offset within last segment,
     */
    protected int _appendAt;

    /**
     * If native type ids supported, this is the id for following
     * value (or first token of one) to be written.
     */
    protected Object _typeId;

    /**
     * If native object ids supported, this is the id for following
     * value (or first token of one) to be written.
     */
    protected Object _objectId;

    /**
     * Do we currently have a native type or object id buffered?
     */
    protected boolean _hasNativeId = false;

    /*
    /**********************************************************************
    /* Output state
    /**********************************************************************
     */

    protected SimpleStreamWriteContext _tokenWriteContext;

    // 05-Oct-2017, tatu: need to consider if this needs to  be properly linked...
    //   especially for "convertValue()" use case
    /**
     * @since 3.0
     */
    protected ObjectWriteContext _objectWriteContext; // = ObjectWriteContext.empty();

    /*
    /**********************************************************************
    /* Life-cycle: constructors
    /**********************************************************************
     */

    /**
     * @param hasNativeIds Whether resulting {@link JsonParser} (if created)
     *   is considered to support native type and object ids
     */
    @Deprecated
    public TokenBuffer(boolean hasNativeIds)
    {
        _streamWriteFeatures = DEFAULT_STREAM_WRITE_FEATURES;
        _tokenWriteContext = SimpleStreamWriteContext.createRootContext(null);
        // at first we have just one segment
        _first = _last = new Segment();
        _appendAt = 0;
        _hasNativeTypeIds = hasNativeIds;
        _hasNativeObjectIds = hasNativeIds;

        // Nothing to base constraints on so:
        _streamReadConstraints = StreamReadConstraints.defaults();

        _mayHaveNativeIds = _hasNativeTypeIds || _hasNativeObjectIds;
    }

    /**
     * @since 3.0
     */
    public TokenBuffer(ObjectWriteContext writeContext, boolean hasNativeIds)
    {
        _objectWriteContext = writeContext;
        _streamWriteFeatures = DEFAULT_STREAM_WRITE_FEATURES;
        _tokenWriteContext = SimpleStreamWriteContext.createRootContext(null);

        // Nothing to base constraints on so:
        _streamReadConstraints = StreamReadConstraints.defaults();

        // at first we have just one segment
        _first = _last = new Segment();
        _appendAt = 0;
        _hasNativeTypeIds = hasNativeIds;
        _hasNativeObjectIds = hasNativeIds;

        _mayHaveNativeIds = _hasNativeTypeIds || _hasNativeObjectIds;
    }

    protected TokenBuffer(JsonParser p, ObjectReadContext ctxt)
    {
        _parentContext = p.streamReadContext();
        // Work-around mostly for unit tests; either should be able to provide
        // proper values but context preferable.
        _streamReadConstraints = (ctxt == null) ? p.streamReadConstraints() : ctxt.streamReadConstraints();
        _streamWriteFeatures = DEFAULT_STREAM_WRITE_FEATURES;
        _tokenWriteContext = SimpleStreamWriteContext.createRootContext(null);
        // at first we have just one segment
        _first = _last = new Segment();
        _appendAt = 0;
        _hasNativeTypeIds = p.canReadTypeId();
        _hasNativeObjectIds = p.canReadObjectId();
        _mayHaveNativeIds = _hasNativeTypeIds || _hasNativeObjectIds;
        if (ctxt instanceof DeserializationContext) {
            _forceBigDecimal = ((DeserializationContext) ctxt).isEnabled(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS);
        } else {
            _forceBigDecimal = false;
        }
    }

    /*
    /**********************************************************************
    /* Life-cycle: helper factory methods
    /**********************************************************************
     */

    /**
     * Specialized factory method used when we are generating token stream for further processing
     * without tokens coming from specific input token stream.
     *
     * @since 3.0
     */
    public static TokenBuffer forGeneration()
    {
        return new TokenBuffer(false);
    }

    /**
     * Specialized factory method used when we are specifically buffering contents of
     * a token stream for further processing.
     *
     * @since 3.0
     */
    public static TokenBuffer forBuffering(JsonParser p, ObjectReadContext ctxt) {
        return new TokenBuffer(p, ctxt);
    }

    /*
    /**********************************************************************
    /* Life-cycle: initialization
    /**********************************************************************
     */

    /**
     * Method that allows explicitly specifying parent parse context to associate
     * with contents of this buffer. Usually context is assigned at construction,
     * based on given parser; but it is not always available, and may not contain
     * intended context.
     */
    public TokenBuffer overrideParentContext(TokenStreamContext ctxt) {
        _parentContext = ctxt;
        return this;
    }

    public TokenBuffer forceUseOfBigDecimal(boolean b) {
        _forceBigDecimal = b;
        return this;
    }

    /*
    /**********************************************************************
    /* Parser construction
    /**********************************************************************
     */

    /**
     * Method used to create a {@link JsonParser} that can read contents
     * stored in this buffer. Will create an "empty" read context
     * (see {@link ObjectReadContext#empty()} which often is not what you want.
     *<p>
     * Note: instances are not synchronized, that is, they are not thread-safe
     * if there are concurrent appends to the underlying buffer.
     *
     * @return Parser that can be used for reading contents stored in this buffer
     */
    public JsonParser asParser() {
        return new Parser(ObjectReadContext.empty(), this,
                _first, _hasNativeTypeIds, _hasNativeObjectIds,
                _parentContext, _streamReadConstraints);
    }

    /**
     * Method used to create a {@link JsonParser} that can read contents
     * stored in this buffer.
     *<p>
     * Note: instances are not synchronized, that is, they are not thread-safe
     * if there are concurrent appends to the underlying buffer.
     *
     * @param readCtxt Active read context to use.
     *
     * @return Parser that can be used for reading contents stored in this buffer
     */
    public JsonParser asParser(ObjectReadContext readCtxt)
    {
        return new Parser(readCtxt, this,
                _first, _hasNativeTypeIds, _hasNativeObjectIds,
                _parentContext, _streamReadConstraints);
    }

    /**
     * @param p0 Parser to use for accessing source information
     *    like location, configured codec, streamReadConstraints
     */
    public JsonParser asParser(ObjectReadContext readCtxt, JsonParser p0)
    {
        StreamReadConstraints src = (p0 == null)
                ? _streamReadConstraints : p0.streamReadConstraints();
        Parser p = new Parser(readCtxt, this,
                _first, _hasNativeTypeIds, _hasNativeObjectIds,
                _parentContext, src);
        if (p0 != null) {
            p.setLocation(p0.currentTokenLocation());
        }
        return p;
    }

    /**
     * Same as:
     *<pre>
     *  JsonParser p = asParser(readCtxt);
     *  p.nextToken();
     *  return p;
     *</pre>
     */
    public JsonParser asParserOnFirstToken(ObjectReadContext readCtxt)
        throws JacksonException
    {
        JsonParser p = asParser(readCtxt);
        p.nextToken();
        return p;
    }

    public JsonParser asParserOnFirstToken(ObjectReadContext readCtxt,
            JsonParser src) throws JacksonException
    {
        JsonParser p = asParser(readCtxt, src);
        p.nextToken();
        return p;
    }

    /*
    /**********************************************************************
    /* Versioned (mostly since buffer is `JsonGenerator`
    /**********************************************************************
     */

    @Override
    public Version version() {
        return tools.jackson.databind.cfg.PackageVersion.VERSION;
    }

    /*
    /**********************************************************************
    /* Additional accessors
    /**********************************************************************
     */

    public JsonToken firstToken() {
        // no need to null check; never create without `_first`
        return _first.type(0);
    }

    /**
     * Accessor for checking whether this buffer has one or more tokens
     * or not.
     *
     * @return True if this buffer instance has no tokens
     *
     * @since 2.13
     */
    public boolean isEmpty() {
        return (_appendAt == 0) && (_first == _last);
    }
    /*
    /**********************************************************************
    /* Other custom methods not needed for implementing interfaces
    /**********************************************************************
     */

    /**
     * Helper method that will append contents of given buffer into this
     * buffer.
     * Not particularly optimized; can be made faster if there is need.
     *
     * @return This buffer
     */
    @SuppressWarnings("resource")
    public TokenBuffer append(TokenBuffer other)
    {
        // Important? If source has native ids, need to store
        if (!_hasNativeTypeIds) {
            _hasNativeTypeIds = other.canWriteTypeId();
        }
        if (!_hasNativeObjectIds) {
            _hasNativeObjectIds = other.canWriteObjectId();
        }
        _mayHaveNativeIds = _hasNativeTypeIds || _hasNativeObjectIds;

        JsonParser p = other.asParser();
        while (p.nextToken() != null) {
            copyCurrentStructure(p);
        }
        return this;
    }

    /**
     * Helper method that will write all contents of this buffer
     * using given {@link JsonGenerator}.
     *<p>
     * Note: this method would be enough to implement
     * <code>ValueSerializer</code>  for <code>TokenBuffer</code> type;
     * but we cannot have upwards
     * references (from core to mapper package); and as such we also
     * cannot take second argument.
     */
    public void serialize(JsonGenerator gen) throws JacksonException
    {
        Segment segment = _first;
        int ptr = -1;

        final boolean checkIds = _mayHaveNativeIds;
        boolean hasIds = checkIds && (segment.hasIds());

        while (true) {
            if (++ptr >= Segment.TOKENS_PER_SEGMENT) {
                ptr = 0;
                segment = segment.next();
                if (segment == null) break;
                hasIds = checkIds && (segment.hasIds());
            }
            JsonToken t = segment.type(ptr);
            if (t == null) break;

            if (hasIds) {
                Object id = segment.findObjectId(ptr);
                if (id != null) {
                    gen.writeObjectId(id);
                }
                id = segment.findTypeId(ptr);
                if (id != null) {
                    gen.writeTypeId(id);
                }
            }

            // Note: copied from 'copyCurrentEvent'...
            switch (t) {
            case START_OBJECT:
                gen.writeStartObject();
                break;
            case END_OBJECT:
                gen.writeEndObject();
                break;
            case START_ARRAY:
                gen.writeStartArray();
                break;
            case END_ARRAY:
                gen.writeEndArray();
                break;
            case PROPERTY_NAME:
            {
                // 13-Dec-2010, tatu: Maybe we should start using different type tokens to reduce casting?
                Object ob = segment.get(ptr);
                if (ob instanceof SerializableString) {
                    gen.writeName((SerializableString) ob);
                } else {
                    gen.writeName((String) ob);
                }
            }
                break;
            case VALUE_STRING:
                {
                    Object ob = segment.get(ptr);
                    if (ob instanceof SerializableString) {
                        gen.writeString((SerializableString) ob);
                    } else {
                        gen.writeString((String) ob);
                    }
                }
                break;
            case VALUE_NUMBER_INT:
                {
                    Object n = segment.get(ptr);
                    if (n instanceof Integer) {
                        gen.writeNumber((Integer) n);
                    } else if (n instanceof BigInteger) {
                        gen.writeNumber((BigInteger) n);
                    } else if (n instanceof Long) {
                        gen.writeNumber((Long) n);
                    } else if (n instanceof Short) {
                        gen.writeNumber((Short) n);
                    } else {
                        gen.writeNumber(((Number) n).intValue());
                    }
                }
                break;
            case VALUE_NUMBER_FLOAT:
                {
                    Object n = segment.get(ptr);
                    if (n instanceof Double) {
                        gen.writeNumber((Double) n);
                    } else if (n instanceof BigDecimal) {
                        gen.writeNumber((BigDecimal) n);
                    } else if (n instanceof Float) {
                        gen.writeNumber((Float) n);
                    } else if (n == null) {
                        gen.writeNull();
                    } else if (n instanceof String) {
                        gen.writeNumber((String) n);
                    } else {
                        throw new StreamWriteException(gen, String.format(
                                "Unrecognized value type for VALUE_NUMBER_FLOAT: %s, cannot serialize",
                                n.getClass().getName()));
                    }
                }
                break;
            case VALUE_TRUE:
                gen.writeBoolean(true);
                break;
            case VALUE_FALSE:
                gen.writeBoolean(false);
                break;
            case VALUE_NULL:
                gen.writeNull();
                break;
            case VALUE_EMBEDDED_OBJECT:
                {
                    Object value = segment.get(ptr);
                    // 01-Sep-2016, tatu: as per [databind#1361], should use `writeEmbeddedObject()`;
                    //    however, may need to consider alternatives for some well-known types
                    //    first
                    if (value instanceof RawValue) {
                        ((RawValue) value).serialize(gen);
                    } else if (value instanceof JacksonSerializable) {
                        gen.writePOJO(value);
                    } else {
                        gen.writeEmbeddedObject(value);
                    }
                }
                break;
            default:
                throw new RuntimeException("Internal error: should never end up through this code path");
            }
        }
    }

    /**
     * Helper method used by standard deserializer.
     */
    public TokenBuffer deserialize(JsonParser p, DeserializationContext ctxt) throws JacksonException
    {
        if (!p.hasToken(JsonToken.PROPERTY_NAME)) {
            copyCurrentStructure(p);
            return this;
        }
        // 28-Oct-2014, tatu: As per [databind#592], need to support a special case of starting from
        //    PROPERTY_NAME, which is taken to mean that we are missing START_OBJECT, but need
        //    to assume one did exist.
        JsonToken t;
        writeStartObject();
        do {
            copyCurrentStructure(p);
        } while ((t = p.nextToken()) == JsonToken.PROPERTY_NAME);
        if (t != JsonToken.END_OBJECT) {
            ctxt.reportWrongTokenException(TokenBuffer.class, JsonToken.END_OBJECT,
                    "Expected END_OBJECT after copying contents of a JsonParser into TokenBuffer, got "+t);
            // never gets here
        }
        writeEndObject();
        return this;
    }

    @Override
    @SuppressWarnings("resource")
    public String toString()
    {
        // Let's print up to 100 first tokens...
        final int MAX_COUNT = 100;

        StringBuilder sb = new StringBuilder();
        sb.append("[TokenBuffer: ");

        /*
sb.append("NativeTypeIds=").append(_hasNativeTypeIds).append(",");
sb.append("NativeObjectIds=").append(_hasNativeObjectIds).append(",");
*/

        JsonParser p = asParser();
        int count = 0;
        final boolean hasNativeIds = _hasNativeTypeIds || _hasNativeObjectIds;

        while (true) {
            JsonToken t = p.nextToken();
            if (t == null) break;

            if (count < MAX_COUNT) {
                if (count > 0) {
                    sb.append(", ");
                }
                if (hasNativeIds) {
                    _appendNativeIds(sb);
                }
                sb.append(t.toString());
                if (t == JsonToken.PROPERTY_NAME) {
                    sb.append('(');
                    sb.append(p.currentName());
                    sb.append(')');
                }
            }
            ++count;
        }
        if (count >= MAX_COUNT) {
            sb.append(" ... (truncated ").append(count-MAX_COUNT).append(" entries)");
        }
        sb.append(']');
        return sb.toString();
    }

    private final void _appendNativeIds(StringBuilder sb)
    {
        Object objectId = _last.findObjectId(_appendAt-1);
        if (objectId != null) {
            sb.append("[objectId=").append(String.valueOf(objectId)).append(']');
        }
        Object typeId = _last.findTypeId(_appendAt-1);
        if (typeId != null) {
            sb.append("[typeId=").append(String.valueOf(typeId)).append(']');
        }
    }

    /*
    /**********************************************************************
    /* JsonGenerator implementation: context
    /**********************************************************************
     */

    @Override
    public TokenStreamContext streamWriteContext() { return _tokenWriteContext; }

    @Override
    public Object currentValue() {
        return _tokenWriteContext.currentValue();
    }

    @Override
    public void assignCurrentValue(Object v) {
        _tokenWriteContext.assignCurrentValue(v);
    }

    @Override
    public ObjectWriteContext objectWriteContext() { return _objectWriteContext; }

    /*
    /**********************************************************************
    /* JsonGenerator implementation: configuration
    /**********************************************************************
     */

    @Override
    public JsonGenerator configure(StreamWriteFeature f, boolean state) {
        if (state) {
            _streamWriteFeatures |= f.getMask();
        } else {
            _streamWriteFeatures &= ~f.getMask();
        }
        return this;
    }

    //public JsonGenerator configure(SerializationFeature f, boolean state) { }

    @Override
    public boolean isEnabled(StreamWriteFeature f) {
        return (_streamWriteFeatures & f.getMask()) != 0;
    }

    @Override
    public int streamWriteFeatures() {
        return _streamWriteFeatures;
    }

    /*
    /**********************************************************************
    /* JsonGenerator implementation: capability introspection
    /**********************************************************************
     */

    // 20-May-2020, tatu: This may or may not be enough -- ideally access is
    //    via `DeserializationContext`, not parser, but if latter is needed
    //    then we'll need to pass this from parser contents if which were
    //    buffered.
    @Override
    public JacksonFeatureSet<StreamWriteCapability> streamWriteCapabilities() {
        return BOGUS_WRITE_CAPABILITIES;
    }

    /*
    /**********************************************************************
    /* JsonGenerator implementation: low-level output handling
    /**********************************************************************
     */

    @Override
    public void flush() { /* NOP */ }

    @Override
    public void close() {
        _closed = true;
    }

    @Override
    public boolean isClosed() { return _closed; }

    @Override
    public Object streamWriteOutputTarget() { return null; }

    @Override
    public int streamWriteOutputBuffered() { return -1; }

    /*
    /**********************************************************************
    /* JsonGenerator implementation: write methods, structural
    /**********************************************************************
     */

    @Override
    public final JsonGenerator writeStartArray()
    {
        _appendStartMarker(JsonToken.START_ARRAY);
        _tokenWriteContext = _tokenWriteContext.createChildArrayContext(null);
        return this;
    }

    @Override
    public final JsonGenerator writeStartArray(Object forValue)
    {
        _appendStartMarker(JsonToken.START_ARRAY);
        _tokenWriteContext = _tokenWriteContext.createChildArrayContext(forValue);
        return this;
    }

    @Override
    public final JsonGenerator writeStartArray(Object forValue, int len)
    {
        _appendStartMarker(JsonToken.START_ARRAY);
        _tokenWriteContext = _tokenWriteContext.createChildArrayContext(forValue);
        return this;
    }

    @Override
    public final JsonGenerator writeEndArray()
    {
        _appendEndMarker(JsonToken.END_ARRAY);
        return this;
    }

    @Override
    public final JsonGenerator writeStartObject()
    {
        _appendStartMarker(JsonToken.START_OBJECT);
        _tokenWriteContext = _tokenWriteContext.createChildObjectContext(null);
        return this;
    }

    @Override
    public JsonGenerator writeStartObject(Object forValue)
    {
        _appendStartMarker(JsonToken.START_OBJECT);
        _tokenWriteContext = _tokenWriteContext.createChildObjectContext(forValue);
        return this;
    }

    @Override
    public JsonGenerator writeStartObject(Object forValue, int size)
    {
        _appendStartMarker(JsonToken.START_OBJECT);
        _tokenWriteContext = _tokenWriteContext.createChildObjectContext(forValue);
        return this;
    }

    @Override
    public final JsonGenerator writeEndObject() {
        _appendEndMarker(JsonToken.END_OBJECT);
        return this;
    }

    @Override
    public final JsonGenerator writeName(String name) {
        _tokenWriteContext.writeName(name);
        _appendName(name);
        return this;
    }

    @Override
    public JsonGenerator writeName(SerializableString name) {
        _tokenWriteContext.writeName(name.getValue());
        _appendName(name);
        return this;
    }

    @Override
    public JsonGenerator writePropertyId(long id) {
        // 15-Aug-2019, tatu: could and probably should be improved to support
        //    buffering but...
        final String name = Long.toString(id);
        _tokenWriteContext.writeName(name);
        _appendName(name);
        return this;
    }

    /*
    /**********************************************************************
    /* JsonGenerator implementation: write methods, textual
    /**********************************************************************
     */

    @Override
    public JsonGenerator writeString(String text) {
        if (text == null) {
            writeNull();
        } else {
            _appendValue(JsonToken.VALUE_STRING, text);
        }
        return this;
    }

    @Override
    public JsonGenerator writeString(char[] text, int offset, int len) {
        return writeString(new String(text, offset, len));
    }

    @Override
    public JsonGenerator writeString(SerializableString text) {
        if (text == null) {
            writeNull();
        } else {
            _appendValue(JsonToken.VALUE_STRING, text);
        }
        return this;
    }

    @Override
    public JsonGenerator writeString(Reader reader, final int len)
    {
        if (reader == null) {
            _reportError("null reader");
        }
        int toRead = (len >= 0) ? len : Integer.MAX_VALUE;

        // 11-Mar-2023, tatu: Really crude implementation, but it is not
        //    expected this method gets often used. Feel free to send a PR
        //    for more optimal handling if you got an itch. :)
        final char[] buf = new char[1000];
        StringBuilder sb = new StringBuilder(1000);
        while (toRead > 0) {
            int toReadNow = Math.min(toRead, buf.length);
            int numRead;

            try {
                numRead = reader.read(buf, 0, toReadNow);
            } catch (IOException e) {
                throw _wrapIOFailure(e);
            }
            if (numRead <= 0) {
                break;
            }
            sb.append(buf, 0, numRead);
            toRead -= numRead;
        }
        if (toRead > 0 && len >= 0) {
            _reportError("Was not able to read enough from reader");
        }
        _appendValue(JsonToken.VALUE_STRING, sb.toString());
        return this;
    }

    @Override
    public JsonGenerator writeRawUTF8String(byte[] text, int offset, int length) {
        // could add support for buffering if we really want it...
        return _reportUnsupportedOperation();
    }

    @Override
    public JsonGenerator writeUTF8String(byte[] text, int offset, int length) {
        // could add support for buffering if we really want it...
        return _reportUnsupportedOperation();
    }

    @Override
    public JsonGenerator writeRaw(String text) {
        return _reportUnsupportedOperation();
    }

    @Override
    public JsonGenerator writeRaw(String text, int offset, int len) {
        return _reportUnsupportedOperation();
    }

    @Override
    public JsonGenerator writeRaw(SerializableString text) {
        return _reportUnsupportedOperation();
    }

    @Override
    public JsonGenerator writeRaw(char[] text, int offset, int len) {
        return _reportUnsupportedOperation();
    }

    @Override
    public JsonGenerator writeRaw(char c) {
        return _reportUnsupportedOperation();
    }

    @Override
    public JsonGenerator writeRawValue(String text) {
        _appendValue(JsonToken.VALUE_EMBEDDED_OBJECT, new RawValue(text));
        return this;
    }

    @Override
    public JsonGenerator writeRawValue(String text, int offset, int len) {
        if (offset > 0 || len != text.length()) {
            text = text.substring(offset, offset+len);
        }
        _appendValue(JsonToken.VALUE_EMBEDDED_OBJECT, new RawValue(text));
        return this;
    }

    @Override
    public JsonGenerator writeRawValue(char[] text, int offset, int len) {
        _appendValue(JsonToken.VALUE_EMBEDDED_OBJECT, new String(text, offset, len));
        return this;
    }

    /*
    /**********************************************************************
    /* JsonGenerator implementation: write methods, primitive types
    /**********************************************************************
     */

    @Override
    public JsonGenerator writeNumber(short i) {
        _appendValue(JsonToken.VALUE_NUMBER_INT, Short.valueOf(i));
        return this;
    }

    @Override
    public JsonGenerator writeNumber(int i) {
        _appendValue(JsonToken.VALUE_NUMBER_INT, Integer.valueOf(i));
        return this;
    }

    @Override
    public JsonGenerator writeNumber(long l) {
        _appendValue(JsonToken.VALUE_NUMBER_INT, Long.valueOf(l));
        return this;
    }

    @Override
    public JsonGenerator writeNumber(double d) {
        _appendValue(JsonToken.VALUE_NUMBER_FLOAT, Double.valueOf(d));
        return this;
    }

    @Override
    public JsonGenerator writeNumber(float f) {
        _appendValue(JsonToken.VALUE_NUMBER_FLOAT, Float.valueOf(f));
        return this;
    }

    @Override
    public JsonGenerator writeNumber(BigDecimal dec) {
        if (dec == null) {
            writeNull();
        } else {
            _appendValue(JsonToken.VALUE_NUMBER_FLOAT, dec);
        }
        return this;
    }

    @Override
    public JsonGenerator writeNumber(BigInteger v) {
        if (v == null) {
            writeNull();
        } else {
            _appendValue(JsonToken.VALUE_NUMBER_INT, v);
        }
        return this;
    }

    @Override
    public JsonGenerator writeNumber(String encodedValue) {
        /* 03-Dec-2010, tatu: related to [JACKSON-423], should try to keep as numeric
         *   identity as long as possible
         */
        _appendValue(JsonToken.VALUE_NUMBER_FLOAT, encodedValue);
        return this;
    }

    private void writeLazyInteger(Object encodedValue) {
        _appendValue(JsonToken.VALUE_NUMBER_INT, encodedValue);
    }

    private void writeLazyDecimal(Object encodedValue) {
        _appendValue(JsonToken.VALUE_NUMBER_FLOAT, encodedValue);
    }

    @Override
    public JsonGenerator writeBoolean(boolean state) {
        _appendValue(state ? JsonToken.VALUE_TRUE : JsonToken.VALUE_FALSE);
        return this;
    }

    @Override
    public JsonGenerator writeNull() {
        _appendValue(JsonToken.VALUE_NULL);
        return this;
    }

    /*
    /**********************************************************************
    /* JsonGenerator implementation: write methods for POJOs/trees
    /**********************************************************************
     */

    @Override
    public JsonGenerator writePOJO(Object value)
    {
        if (value == null) {
            return writeNull();
        }
        final Class<?> raw = value.getClass();
        if (raw == byte[].class || (value instanceof RawValue)
                || (_objectWriteContext == null)) {
            _appendValue(JsonToken.VALUE_EMBEDDED_OBJECT, value);
            return this;
        }
        _objectWriteContext.writeValue(this, value);
        return this;
    }

    @Override
    public JsonGenerator writeTree(TreeNode node)
    {
        if (node == null) {
            return writeNull();
        }
        if (_objectWriteContext == null) {
            _appendValue(JsonToken.VALUE_EMBEDDED_OBJECT, node);
            return this;
        }
        _objectWriteContext.writeTree(this, node);
        return this;
    }

    /*
    /**********************************************************************
    /* JsonGenerator implementation; binary
    /**********************************************************************
     */

    @Override
    public JsonGenerator writeBinary(Base64Variant b64variant, byte[] data, int offset, int len)
    {
        // 12-Jan-2021, tatu: Should we try to preserve the variant? Depends a
        //   lot on whether this during read (no need to retain probably) or
        //   write (probably important)
        byte[] copy = Arrays.copyOfRange(data, offset, offset + len);
        return writePOJO(copy);
    }

    /**
     * Although we could support this method, it does not necessarily make
     * sense: we cannot make good use of streaming because buffer must
     * hold all the data. Because of this, currently this will simply
     * throw {@link UnsupportedOperationException}
     */
    @Override
    public int writeBinary(Base64Variant b64variant, InputStream data, int dataLength) {
        throw new UnsupportedOperationException();
    }

    /*
    /**********************************************************************
    /* JsonGenerator implementation: native ids
    /**********************************************************************
     */

    @Override
    public boolean canWriteTypeId() {
        return _hasNativeTypeIds;
    }

    @Override
    public boolean canWriteObjectId() {
        return _hasNativeObjectIds;
    }

    @Override
    public JsonGenerator writeTypeId(Object id) {
        _typeId = id;
        _hasNativeId = true;
        return this;
    }

    @Override
    public JsonGenerator writeObjectId(Object id) {
        _objectId = id;
        _hasNativeId = true;
        return this;
    }

    @Override
    public JsonGenerator writeEmbeddedObject(Object object) {
        _appendValue(JsonToken.VALUE_EMBEDDED_OBJECT, object);
        return this;
    }

    /*
    /**********************************************************************
    /* JsonGenerator implementation; pass-through copy
    /**********************************************************************
     */

    @Override
    public void copyCurrentEvent(JsonParser p)
    {
        if (_mayHaveNativeIds) {
            _checkNativeIds(p);
        }
        switch (p.currentToken()) {
        case START_OBJECT:
            writeStartObject();
            break;
        case END_OBJECT:
            writeEndObject();
            break;
        case START_ARRAY:
            writeStartArray();
            break;
        case END_ARRAY:
            writeEndArray();
            break;
        case PROPERTY_NAME:
            writeName(p.currentName());
            break;
        case VALUE_STRING:
            if (p.hasTextCharacters()) {
                writeString(p.getTextCharacters(), p.getTextOffset(), p.getTextLength());
            } else {
                writeString(p.getText());
            }
            break;
        case VALUE_NUMBER_INT:
            switch (p.getNumberType()) {
            case INT:
                writeNumber(p.getIntValue());
                break;
            case BIG_INTEGER:
                writeLazyInteger(p.getNumberValueDeferred());
                break;
            default:
                writeNumber(p.getLongValue());
            }
            break;
        case VALUE_NUMBER_FLOAT:
            writeLazyDecimal(p.getNumberValueDeferred());
            break;
        case VALUE_TRUE:
            writeBoolean(true);
            break;
        case VALUE_FALSE:
            writeBoolean(false);
            break;
        case VALUE_NULL:
            writeNull();
            break;
        case VALUE_EMBEDDED_OBJECT:
            writePOJO(p.getEmbeddedObject());
            break;
        default:
            throw new RuntimeException("Internal error: unexpected token: "+p.currentToken());
        }
    }

    @Override
    public void copyCurrentStructure(JsonParser p)
    {
        JsonToken t = p.currentToken();

        // Let's handle property name separately first
        if (t == JsonToken.PROPERTY_NAME) {
            if (_mayHaveNativeIds) {
                _checkNativeIds(p);
            }
            writeName(p.currentName());
            t = p.nextToken();
            // fall-through to copy the associated value
        } else if (t == null) {
            throw new IllegalStateException("No token available from argument `JsonParser`");
        }

        // We'll do minor handling here to separate structured, scalar values,
        // then delegate appropriately.
        // Plus also deal with oddity of "dangling" END_OBJECT/END_ARRAY
        switch (t) {
        case START_ARRAY:
            if (_mayHaveNativeIds) {
                _checkNativeIds(p);
            }
            writeStartArray();
            _copyBufferContents(p);
            break;
        case START_OBJECT:
            if (_mayHaveNativeIds) {
                _checkNativeIds(p);
            }
            writeStartObject();
            _copyBufferContents(p);
            break;
        case END_ARRAY:
            writeEndArray();
            break;
        case END_OBJECT:
            writeEndObject();
            break;
        default: // others are simple:
            _copyBufferValue(p, t);
        }
    }

    protected void _copyBufferContents(JsonParser p)
    {
        int depth = 1;
        JsonToken t;

        while ((t = p.nextToken()) != null) {
            switch (t) {
            case PROPERTY_NAME:
                if (_mayHaveNativeIds) {
                    _checkNativeIds(p);
                }
                writeName(p.currentName());
                break;

            case START_ARRAY:
                if (_mayHaveNativeIds) {
                    _checkNativeIds(p);
                }
                writeStartArray();
                ++depth;
                break;

            case START_OBJECT:
                if (_mayHaveNativeIds) {
                    _checkNativeIds(p);
                }
                writeStartObject();
                ++depth;
                break;

            case END_ARRAY:
                writeEndArray();
                if (--depth == 0) {
                    return;
                }
                break;
            case END_OBJECT:
                writeEndObject();
                if (--depth == 0) {
                    return;
                }
                break;

            default:
                _copyBufferValue(p, t);
            }
        }
    }

    // NOTE: Copied from earlier `copyCurrentEvent()`
    private void _copyBufferValue(JsonParser p, JsonToken t)
    {
        if (_mayHaveNativeIds) {
            _checkNativeIds(p);
        }
        switch (t) {
        case VALUE_STRING:
            if (p.hasTextCharacters()) {
                writeString(p.getTextCharacters(), p.getTextOffset(), p.getTextLength());
            } else {
                writeString(p.getText());
            }
            break;
        case VALUE_NUMBER_INT:
            switch (p.getNumberType()) {
            case INT:
                writeNumber(p.getIntValue());
                break;
            case BIG_INTEGER:
                writeLazyInteger(p.getNumberValueDeferred());
                break;
            default:
                writeNumber(p.getLongValue());
            }
            break;
        case VALUE_NUMBER_FLOAT:
            writeLazyDecimal(p.getNumberValueDeferred());
            break;
        case VALUE_TRUE:
            writeBoolean(true);
            break;
        case VALUE_FALSE:
            writeBoolean(false);
            break;
        case VALUE_NULL:
            writeNull();
            break;
        case VALUE_EMBEDDED_OBJECT:
            writePOJO(p.getEmbeddedObject());
            break;
        default:
            throw new RuntimeException("Internal error: unexpected token: "+t);
        }
    }

    private final void _checkNativeIds(JsonParser p)
    {
        if ((_typeId = p.getTypeId()) != null) {
            _hasNativeId = true;
        }
        if ((_objectId = p.getObjectId()) != null) {
            _hasNativeId = true;
        }
    }

    /*
    /**********************************************************************
    /* Internal methods
    /**********************************************************************
     */

    /**
     * Method used for appending token known to represent a "simple" scalar
     * value where token is the only information
     */
    protected final void _appendValue(JsonToken type)
    {
         _tokenWriteContext.writeValue();
        Segment next;
        if (_hasNativeId) {
            next = _last.append(_appendAt, type, _objectId, _typeId);
        } else {
            next = _last.append(_appendAt, type);
        }
        if (next == null) {
            ++_appendAt;
        } else {
            _last = next;
            _appendAt = 1; // since we added first at 0
        }
    }

    /**
     * Method used for appending token known to represent a scalar value
     * where there is additional content (text, number) beyond type token
     */
    protected final void _appendValue(JsonToken type, Object value)
    {
         _tokenWriteContext.writeValue();
        Segment next;
        if (_hasNativeId) {
            next = _last.append(_appendAt, type, value, _objectId, _typeId);
        } else {
            next = _last.append(_appendAt, type, value);
        }
        if (next == null) {
            ++_appendAt;
        } else {
            _last = next;
            _appendAt = 1;
        }
    }

    /*
     * Specialized method used for appending an Object property name, appending either
     * {@link String} or {@link SerializableString}.
     */
    protected final void _appendName(Object value)
    {
        // NOTE: do NOT clear _objectId / _typeId
        Segment next;
        if (_hasNativeId) {
            next =  _last.append(_appendAt, JsonToken.PROPERTY_NAME, value, _objectId, _typeId);
        } else {
            next = _last.append(_appendAt, JsonToken.PROPERTY_NAME, value);
        }
        if (next == null) {
            ++_appendAt;
        } else {
            _last = next;
            _appendAt = 1;
        }
    }

    /**
     * Specialized method used for appending a structural start Object/Array marker
     */
    protected final void _appendStartMarker(JsonToken type)
    {
        _tokenWriteContext.writeValue();

        Segment next;
        if (_hasNativeId) {
            next =_last.append(_appendAt, type, _objectId, _typeId);
        } else {
            next =  _last.append(_appendAt, type);
        }
        if (next == null) {
            ++_appendAt;
        } else {
            _last = next;
            _appendAt = 1; // since we added first at 0
        }
    }

    /**
     * Specialized method used for appending a structural end Object/Array marker
     */
    protected final void _appendEndMarker(JsonToken type)
    {
        // NOTE: type/object id not relevant
        Segment next = _last.append(_appendAt, type);
        if (next == null) {
            ++_appendAt;
        } else {
            _last = next;
            _appendAt = 1;
        }

        // but then we need to update context. One twist: do allow unbalanced content;
        // for that need to check that we will retain "root context"
        SimpleStreamWriteContext c = _tokenWriteContext.getParent();
        if (c != null) {
            _tokenWriteContext = c;
        }
    }

    @Override
    protected <T> T _reportUnsupportedOperation() {
        throw new UnsupportedOperationException("Called operation not supported for TokenBuffer");
    }

    /*
    /**********************************************************************
    /* Supporting classes
    /**********************************************************************
     */

    protected final static class Parser
        extends ParserMinimalBase
    {
        /*
        /******************************************************************
        /* Configuration
        /******************************************************************
         */

        protected StreamReadConstraints _streamReadConstraints;

        protected final TokenBuffer _source;

        protected final boolean _hasNativeTypeIds;

        protected final boolean _hasNativeObjectIds;

        protected final boolean _hasNativeIds;

        /*
        /******************************************************************
        /* Parsing state
        /******************************************************************
         */

        /**
         * Currently active segment
         */
        protected Segment _segment;

        /**
         * Pointer to current token within current segment
         */
        protected int _segmentPtr;

        /**
         * Information about parser context, context in which
         * the next token is to be parsed (root, array, object).
         */
        protected TokenBufferReadContext _parsingContext;

        protected boolean _closed;

        protected transient ByteArrayBuilder _byteBuilder;

        protected JsonLocation _location = null;

        /*
        /******************************************************************
        /* Construction, init
        /******************************************************************
         */

        public Parser(ObjectReadContext readCtxt, TokenBuffer source,
                Segment firstSeg, boolean hasNativeTypeIds, boolean hasNativeObjectIds,
                TokenStreamContext parentContext,
                StreamReadConstraints streamReadConstraints)
        {
            // 25-Jun-2022, tatu: This should pass stream read features as
            //    per [databind#3528]) but for now at very least should get
            //    sane defaults
            super(readCtxt);
            _source = source;
            _segment = firstSeg;
            _segmentPtr = -1; // not yet read
            _streamReadConstraints = streamReadConstraints;
            _parsingContext = TokenBufferReadContext.createRootContext(parentContext);
            _hasNativeTypeIds = hasNativeTypeIds;
            _hasNativeObjectIds = hasNativeObjectIds;
            _hasNativeIds = (hasNativeTypeIds || hasNativeObjectIds);
        }

        public void setLocation(JsonLocation l) {
            _location = l;
        }

        /*
        /**********************************************************
        /* Public API, config access, capability introspection
        /**********************************************************
         */

        @Override
        public Version version() {
            return tools.jackson.databind.cfg.PackageVersion.VERSION;
        }

        // 20-May-2020, tatu: This may or may not be enough -- ideally access is
        //    via `DeserializationContext`, not parser, but if latter is needed
        //    then we'll need to pass this from parser contents if which were
        //    buffered.
        @Override
        public JacksonFeatureSet<StreamReadCapability> streamReadCapabilities() {
            return DEFAULT_READ_CAPABILITIES;
        }

        @Override
        public TokenBuffer streamReadInputSource() {
            return _source;
        }

        @Override
        public StreamReadConstraints streamReadConstraints() {
            return _streamReadConstraints;
        }

        /*
        /******************************************************************
        /* Extended API beyond JsonParser
        /******************************************************************
         */

        public JsonToken peekNextToken()
        {
            // closed? nothing more to peek, either
            if (_closed) return null;
            Segment seg = _segment;
            int ptr = _segmentPtr+1;
            if (ptr >= Segment.TOKENS_PER_SEGMENT) {
                ptr = 0;
                seg = (seg == null) ? null : seg.next();
            }
            return (seg == null) ? null : seg.type(ptr);
        }

        /*
        /******************************************************************
        /* Closing, related
        /******************************************************************
         */

        @Override
        public void close() {
            _closed = true;
        }

        @Override
        protected void _closeInput() throws IOException { }

        @Override
        protected void _releaseBuffers() { }

        /*
        /******************************************************************
        /* Public API, traversal
        /******************************************************************
         */

        @Override
        public JsonToken nextToken()
        {
            // If we are closed, nothing more to do
            if (_closed || (_segment == null)) {
                _currToken = null;
                return null;
            }

            // Ok, then: any more tokens?
            if (++_segmentPtr >= Segment.TOKENS_PER_SEGMENT) {
                _segmentPtr = 0;
                _segment = _segment.next();
                if (_segment == null) {
                    _currToken = null;
                    return null;
                }
            }
            _currToken = _segment.type(_segmentPtr);
            // Property name? Need to update context
            if (_currToken == JsonToken.PROPERTY_NAME) {
                Object ob = _currentObject();
                String name = (ob instanceof String) ? ((String) ob) : ob.toString();
                _parsingContext.setCurrentName(name);
            } else if (_currToken == JsonToken.START_OBJECT) {
                _parsingContext = _parsingContext.createChildObjectContext();
            } else if (_currToken == JsonToken.START_ARRAY) {
                _parsingContext = _parsingContext.createChildArrayContext();
            } else if (_currToken == JsonToken.END_OBJECT
                    || _currToken == JsonToken.END_ARRAY) {
                // Closing JSON Object/Array? Close matching context
                _parsingContext = _parsingContext.parentOrCopy();
            } else {
                _parsingContext.updateForValue();
            }
            return _currToken;
        }

        @Override
        public String nextName()
        {
            // inlined common case from nextToken()
            if (_closed || (_segment == null)) {
                return null;
            }

            int ptr = _segmentPtr+1;
            if ((ptr < Segment.TOKENS_PER_SEGMENT) && (_segment.type(ptr) == JsonToken.PROPERTY_NAME)) {
                _segmentPtr = ptr;
                _currToken = JsonToken.PROPERTY_NAME;
                Object ob = _segment.get(ptr); // inlined _currentObject();
                String name = (ob instanceof String) ? ((String) ob) : ob.toString();
                _parsingContext.setCurrentName(name);
                return name;
            }
            return (nextToken() == JsonToken.PROPERTY_NAME) ? currentName() : null;
        }

        // NOTE: since we know there's no native matching just use simpler way:
        @Override // since 3.0
        public int nextNameMatch(PropertyNameMatcher matcher) {
            String str = nextName();
            if (str != null) {
                // 15-Nov-2017, tatu: Can not assume name given is intern()ed
                return matcher.matchName(str);
            }
            if (hasToken(JsonToken.END_OBJECT)) {
                return PropertyNameMatcher.MATCH_END_OBJECT;
            }
            return PropertyNameMatcher.MATCH_ODD_TOKEN;
        }

        @Override
        public boolean isClosed() { return _closed; }

        /*
        /******************************************************************
        /* Public API, token accessors
        /******************************************************************
         */

        @Override public TokenStreamContext streamReadContext() { return _parsingContext; }
        @Override public void assignCurrentValue(Object v) { _parsingContext.assignCurrentValue(v); }
        @Override public Object currentValue() { return _parsingContext.currentValue(); }

        @Override
        public JsonLocation currentTokenLocation() { return currentLocation(); }

        @Override
        public JsonLocation currentLocation() {
            return (_location == null) ? JsonLocation.NA : _location;
        }

        @Override
        public String currentName() {
            // 25-Jun-2015, tatu: as per [databind#838], needs to be same as ParserBase
            if (_currToken == JsonToken.START_OBJECT || _currToken == JsonToken.START_ARRAY) {
                TokenStreamContext parent = _parsingContext.getParent();
                return parent.currentName();
            }
            return _parsingContext.currentName();
        }

        /*
        /******************************************************************
        /* Public API, access to token information, text
        /******************************************************************
         */

        @Override
        public String getText()
        {
            // common cases first:
            if (_currToken == JsonToken.VALUE_STRING
                    || _currToken == JsonToken.PROPERTY_NAME) {
                Object ob = _currentObject();
                if (ob instanceof String) {
                    return (String) ob;
                }
                return ClassUtil.nullOrToString(ob);
            }
            if (_currToken == null) {
                return null;
            }
            switch (_currToken) {
            case VALUE_NUMBER_INT:
            case VALUE_NUMBER_FLOAT:
                return ClassUtil.nullOrToString(_currentObject());
            default:
            	return _currToken.asString();
            }
        }

        @Override
        public char[] getTextCharacters() {
            String str = getText();
            return (str == null) ? null : str.toCharArray();
        }

        @Override
        public int getTextLength() {
            String str = getText();
            return (str == null) ? 0 : str.length();
        }

        @Override
        public int getTextOffset() { return 0; }

        @Override
        public boolean hasTextCharacters() {
            // We never have raw buffer available, so:
            return false;
        }

        /*
        /******************************************************************
        /* Public API, access to token information, numeric
        /******************************************************************
         */

        @Override
        public boolean isNaN() {
            // can only occur for floating-point numbers
            if (_currToken == JsonToken.VALUE_NUMBER_FLOAT) {
                Object value = _currentObject();
                if (value instanceof Double) {
                    return NumberOutput.notFinite((Double) value);
                }
                if (value instanceof Float) {
                    return NumberOutput.notFinite((Float) value);
                }
            }
            return false;
        }

        @Override
        public BigInteger getBigIntegerValue()
        {
            Number n = _numberValue(NR_BIGINT, true);
            if (n instanceof BigInteger) {
                return (BigInteger) n;
            } else if (n instanceof BigDecimal) {
                final BigDecimal bd = (BigDecimal) n;
                streamReadConstraints().validateBigIntegerScale(bd.scale());
                return bd.toBigInteger();
            }
            // int/long is simple, but let's also just truncate float/double:
            return BigInteger.valueOf(n.longValue());
        }

        @Override
        public BigDecimal getDecimalValue()
        {
            Number n = _numberValue(NR_BIGDECIMAL, true);
            if (n instanceof BigDecimal) {
                return (BigDecimal) n;
            } else if (n instanceof Integer) {
                return BigDecimal.valueOf(n.intValue());
            } else if (n instanceof Long) {
                return BigDecimal.valueOf(n.longValue());
            } else if (n instanceof BigInteger) {
                return new BigDecimal((BigInteger) n);
            }
            // float or double
            return BigDecimal.valueOf(n.doubleValue());
        }

        @Override
        public double getDoubleValue() {
            return _numberValue(NR_DOUBLE, false).doubleValue();
        }

        @Override
        public float getFloatValue() {
            return _numberValue(NR_FLOAT, false).floatValue();
        }

        @Override
        public int getIntValue()
        {
            Number n = (_currToken == JsonToken.VALUE_NUMBER_INT) ?
                    ((Number) _currentObject()) : _numberValue(NR_INT, false);
            if ((n instanceof Integer) || _smallerThanInt(n)) {
                return n.intValue();
            }
            return _convertNumberToInt(n);
        }

        @Override
        public long getLongValue() {
            Number n = (_currToken == JsonToken.VALUE_NUMBER_INT) ?
                    ((Number) _currentObject()) : _numberValue(NR_LONG, false);
            if ((n instanceof Long) || _smallerThanLong(n)) {
                return n.longValue();
            }
            return _convertNumberToLong(n);
        }

        @Override
        public NumberType getNumberType()
        {
            // 2021-01-12, tatu: Avoid throwing exception by not calling accessor
            if (_currToken == null) {
                return null;
            }
            final Object value = _currentObject();
            if (value instanceof Number) {
                Number n = (Number) value;
                if (n instanceof Integer) return NumberType.INT;
                if (n instanceof Long) return NumberType.LONG;
                if (n instanceof Double) return NumberType.DOUBLE;
                if (n instanceof BigDecimal) return NumberType.BIG_DECIMAL;
                if (n instanceof BigInteger) return NumberType.BIG_INTEGER;
                if (n instanceof Float) return NumberType.FLOAT;
                if (n instanceof Short) return NumberType.INT;       // should be SHORT
            } else if (value instanceof String) {
                return (_currToken == JsonToken.VALUE_NUMBER_FLOAT)
                        ? NumberType.BIG_DECIMAL : NumberType.BIG_INTEGER;
            }
            return null;
        }

        @Override
        public final Number getNumberValue() {
            return _numberValue(-1, false);
        }

        @Override
        public Object getNumberValueDeferred() {
            // Former "_checkIsNumber()"
            if (_currToken == null || !_currToken.isNumeric()) {
                throw _constructNotNumericType(_currToken, 0);
            }
            return _currentObject();
        }

        private Number _numberValue(final int targetNumType, final boolean preferBigNumbers) {
            // Former "_checkIsNumber()"
            if (_currToken == null || !_currToken.isNumeric()) {
                throw _constructNotNumericType(_currToken, targetNumType);
            }
            Object value = _currentObject();
            if (value instanceof Number) {
                return (Number) value;
            }
            // Difficult to really support numbers-as-Strings; but let's try.
            // NOTE: no access to DeserializationConfig, unfortunately, so cannot
            // try to determine Double/BigDecimal preference...

            // 12-Jan-2021, tatu: Is this really needed, and for what? CSV, XML?
            if (value instanceof String) {
                String str = (String) value;
                final int len = str.length();
                if (_currToken == JsonToken.VALUE_NUMBER_INT) {
                    // 08-Dec-2024, tatu: Note -- deferred numbers' validity (wrt input token)
                    //    has been verified by underlying `JsonParser`: no need to check again
                    if (preferBigNumbers
                            // 01-Feb-2023, tatu: Not really accurate but we'll err on side
                            //   of not losing accuracy (should really check 19-char case,
                            //   or, with minus sign, 20-char)
                            || (len >= 19)) {
                        return NumberInput.parseBigInteger(str, isEnabled(StreamReadFeature.USE_FAST_BIG_NUMBER_PARSER));
                    }
                    // Otherwise things get trickier; here, too, we should use more accurate
                    // boundary checks
                    if (len >= 10) {
                        return NumberInput.parseLong(str);
                    }
                    return NumberInput.parseInt(str);
                }
                if (preferBigNumbers) {
                    BigDecimal dec = NumberInput.parseBigDecimal(str,
                            isEnabled(StreamReadFeature.USE_FAST_BIG_NUMBER_PARSER));
                    // 01-Feb-2023, tatu: This is... weird. Seen during tests, only
                    if (dec == null) {
                        throw new IllegalStateException("Internal error: failed to parse number '"+str+"'");
                    }
                    return dec;
                }
                return NumberInput.parseDouble(str, isEnabled(StreamReadFeature.USE_FAST_DOUBLE_PARSER));
            }
            throw new IllegalStateException("Internal error: entry should be a Number, but is of type "
                    +ClassUtil.classNameOf(value));
        }

        private final boolean _smallerThanInt(Number n) {
            return (n instanceof Short) || (n instanceof Byte);
        }

        private final boolean _smallerThanLong(Number n) {
            return (n instanceof Integer) || (n instanceof Short) || (n instanceof Byte);
        }

        // 02-Jan-2017, tatu: Modified from method(s) in `ParserBase`

        protected int _convertNumberToInt(Number n) throws InputCoercionException
        {
            if (n instanceof Long) {
                long l = n.longValue();
                int result = (int) l;
                if (((long) result) != l) {
                    _reportOverflowInt();
                }
                return result;
            }
            if (n instanceof BigInteger) {
                BigInteger big = (BigInteger) n;
                if (BI_MIN_INT.compareTo(big) > 0
                        || BI_MAX_INT.compareTo(big) < 0) {
                    _reportOverflowInt();
                }
            } else if ((n instanceof Double) || (n instanceof Float)) {
                double d = n.doubleValue();
                // Need to check boundaries
                if (d < MIN_INT_D || d > MAX_INT_D) {
                    _reportOverflowInt();
                }
                return (int) d;
            } else if (n instanceof BigDecimal) {
                BigDecimal big = (BigDecimal) n;
                if (BD_MIN_INT.compareTo(big) > 0
                    || BD_MAX_INT.compareTo(big) < 0) {
                    _reportOverflowInt();
                }
            } else {
                _throwInternal();
            }
            return n.intValue();
        }

        protected long _convertNumberToLong(Number n) throws InputCoercionException
        {
            if (n instanceof BigInteger) {
                BigInteger big = (BigInteger) n;
                if (BI_MIN_LONG.compareTo(big) > 0
                        || BI_MAX_LONG.compareTo(big) < 0) {
                    _reportOverflowLong();
                }
            } else if ((n instanceof Double) || (n instanceof Float)) {
                double d = n.doubleValue();
                // Need to check boundaries
                if (d < MIN_LONG_D || d > MAX_LONG_D) {
                    _reportOverflowLong();
                }
                return (long) d;
            } else if (n instanceof BigDecimal) {
                BigDecimal big = (BigDecimal) n;
                if (BD_MIN_LONG.compareTo(big) > 0
                    || BD_MAX_LONG.compareTo(big) < 0) {
                    _reportOverflowLong();
                }
            } else {
                _throwInternal();
            }
            return n.longValue();
        }

        /*
        /******************************************************************
        /* Public API, access to token information, other
        /******************************************************************
         */

        @Override
        public Object getEmbeddedObject()
        {
            if (_currToken == JsonToken.VALUE_EMBEDDED_OBJECT) {
                return _currentObject();
            }
            return null;
        }

        @Override
        @SuppressWarnings("resource")
        public byte[] getBinaryValue(Base64Variant b64variant) throws JacksonException
        {
            // First: maybe we some special types?
            if (_currToken == JsonToken.VALUE_EMBEDDED_OBJECT) {
                // Embedded byte array would work nicely...
                Object ob = _currentObject();
                if (ob instanceof byte[]) {
                    return (byte[]) ob;
                }
                // fall through to error case
            }
            if (_currToken != JsonToken.VALUE_STRING) {
                throw _constructReadException("Current token ("+_currToken+") not VALUE_STRING (or VALUE_EMBEDDED_OBJECT with byte[]), cannot access as binary");
            }
            final String str = getText();
            if (str == null) {
                return null;
            }
            ByteArrayBuilder builder = _byteBuilder;
            if (builder == null) {
                _byteBuilder = builder = new ByteArrayBuilder(100);
            } else {
                _byteBuilder.reset();
            }
            _decodeBase64(str, builder, b64variant);
            return builder.toByteArray();
        }

        @Override
        public int readBinaryValue(Base64Variant b64variant, OutputStream out)
            throws JacksonException
        {
            byte[] data = getBinaryValue(b64variant);
            if (data != null) {
                try {
                    out.write(data, 0, data.length);
                } catch (IOException e) {
                    throw _wrapIOFailure(e);
                }
                return data.length;
            }
            return 0;
        }

        /*
        /******************************************************************
        /* Public API, native ids
        /******************************************************************
         */

        @Override
        public boolean canReadObjectId() {
            return _hasNativeObjectIds;
        }

        @Override
        public boolean canReadTypeId() {
            return _hasNativeTypeIds;
        }

        @Override
        public Object getTypeId() {
            return _segment.findTypeId(_segmentPtr);
        }

        @Override
        public Object getObjectId() {
            return _segment.findObjectId(_segmentPtr);
        }

        /*
        /******************************************************************
        /* Internal methods
        /******************************************************************
         */

        protected final Object _currentObject() {
            return _segment.get(_segmentPtr);
        }

        @Override
        protected void _handleEOF() {
            _throwInternal();
        }
    }

    /**
     * Individual segment of TokenBuffer that can store up to 16 tokens
     * (limited by 4 bits per token type marker requirement).
     * Current implementation uses fixed length array; could alternatively
     * use 16 distinct elements and switch statement (slightly more efficient
     * storage, slightly slower access)
     */
    protected final static class Segment
    {
        public final static int TOKENS_PER_SEGMENT = 16;

        /**
         * Static array used for fast conversion between token markers and
         * matching {@link JsonToken} instances
         */
        private final static JsonToken[] TOKEN_TYPES_BY_INDEX;
        static {
            // ... here we know that there are <= 15 values in JsonToken enum
            TOKEN_TYPES_BY_INDEX = new JsonToken[16];
            JsonToken[] t = JsonToken.values();
            // and reserve entry 0 for "not available"
            System.arraycopy(t, 1, TOKEN_TYPES_BY_INDEX, 1, Math.min(15, t.length - 1));
        }

        // // // Linking

        protected Segment _next;

        // // // State

        /**
         * Bit field used to store types of buffered tokens; 4 bits per token.
         * Value 0 is reserved for "not in use"
         */
        protected long _tokenTypes;


        // Actual tokens

        protected final Object[] _tokens = new Object[TOKENS_PER_SEGMENT];

        /**
         * Lazily constructed Map for storing native type and object ids, if any
         */
        protected TreeMap<Integer,Object> _nativeIds;

        public Segment() { }

        // // // Accessors

        public JsonToken type(int index)
        {
            long l = _tokenTypes;
            if (index > 0) {
                l >>= (index << 2);
            }
            int ix = ((int) l) & 0xF;
            return TOKEN_TYPES_BY_INDEX[ix];
        }

        public int rawType(int index)
        {
            long l = _tokenTypes;
            if (index > 0) {
                l >>= (index << 2);
            }
            return ((int) l) & 0xF;
        }

        public Object get(int index) {
            return _tokens[index];
        }

        public Segment next() { return _next; }

        /**
         * Accessor for checking whether this segment may have native
         * type or object ids.
         */
        public boolean hasIds() {
            return _nativeIds != null;
        }

        // // // Mutators

        public Segment append(int index, JsonToken tokenType)
        {
            if (index < TOKENS_PER_SEGMENT) {
                set(index, tokenType);
                return null;
            }
            _next = new Segment();
            _next.set(0, tokenType);
            return _next;
        }

        public Segment append(int index, JsonToken tokenType,
                Object objectId, Object typeId)
        {
            if (index < TOKENS_PER_SEGMENT) {
                set(index, tokenType, objectId, typeId);
                return null;
            }
            _next = new Segment();
            _next.set(0, tokenType, objectId, typeId);
            return _next;
        }

        public Segment append(int index, JsonToken tokenType, Object value)
        {
            if (index < TOKENS_PER_SEGMENT) {
                set(index, tokenType, value);
                return null;
            }
            _next = new Segment();
            _next.set(0, tokenType, value);
            return _next;
        }

        public Segment append(int index, JsonToken tokenType, Object value,
                Object objectId, Object typeId)
        {
            if (index < TOKENS_PER_SEGMENT) {
                set(index, tokenType, value, objectId, typeId);
                return null;
            }
            _next = new Segment();
            _next.set(0, tokenType, value, objectId, typeId);
            return _next;
        }

        private void set(int index, JsonToken tokenType)
        {
            /* Assumption here is that there are no overwrites, just appends;
             * and so no masking is needed (nor explicit setting of null)
             */
            long typeCode = tokenType.ordinal();
            if (index > 0) {
                typeCode <<= (index << 2);
            }
            _tokenTypes |= typeCode;
        }

        private void set(int index, JsonToken tokenType,
                Object objectId, Object typeId)
        {
            long typeCode = tokenType.ordinal();
            if (index > 0) {
                typeCode <<= (index << 2);
            }
            _tokenTypes |= typeCode;
            assignNativeIds(index, objectId, typeId);
        }

        private void set(int index, JsonToken tokenType, Object value)
        {
            _tokens[index] = value;
            long typeCode = tokenType.ordinal();
            if (index > 0) {
                typeCode <<= (index << 2);
            }
            _tokenTypes |= typeCode;
        }

        private void set(int index, JsonToken tokenType, Object value,
                Object objectId, Object typeId)
        {
            _tokens[index] = value;
            long typeCode = tokenType.ordinal();
            if (index > 0) {
                typeCode <<= (index << 2);
            }
            _tokenTypes |= typeCode;
            assignNativeIds(index, objectId, typeId);
        }

        private final void assignNativeIds(int index, Object objectId, Object typeId)
        {
            if (_nativeIds == null) {
                _nativeIds = new TreeMap<Integer,Object>();
            }
            if (objectId != null) {
                _nativeIds.put(_objectIdIndex(index), objectId);
            }
            if (typeId != null) {
                _nativeIds.put(_typeIdIndex(index), typeId);
            }
        }

        public Object findObjectId(int index) {
            return (_nativeIds == null) ? null : _nativeIds.get(_objectIdIndex(index));
        }

        public Object findTypeId(int index) {
            return (_nativeIds == null) ? null : _nativeIds.get(_typeIdIndex(index));
        }

        private final int _typeIdIndex(int i) { return i+i; }
        private final int _objectIdIndex(int i) { return i+i+1; }
    }
}
