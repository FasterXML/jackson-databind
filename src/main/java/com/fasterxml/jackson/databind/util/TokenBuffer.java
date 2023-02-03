package com.fasterxml.jackson.databind.util;

import java.io.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.TreeMap;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.core.base.ParserMinimalBase;
import com.fasterxml.jackson.core.io.NumberInput;
import com.fasterxml.jackson.core.json.JsonWriteContext;
import com.fasterxml.jackson.core.util.ByteArrayBuilder;
import com.fasterxml.jackson.core.util.JacksonFeatureSet;
import com.fasterxml.jackson.databind.*;

/**
 * Utility class used for efficient storage of {@link JsonToken}
 * sequences, needed for temporary buffering.
 * Space efficient for different sequence lengths (especially so for smaller
 * ones; but not significantly less efficient for larger), highly efficient
 * for linear iteration and appending. Implemented as segmented/chunked
 * linked list of tokens; only modifications are via appends.
 */
public class TokenBuffer
// Won't use JsonGeneratorBase, to minimize overhead for validity
// checking
    extends JsonGenerator
{
    protected final static int DEFAULT_GENERATOR_FEATURES = JsonGenerator.Feature.collectDefaults();

    /*
    /**********************************************************
    /* Configuration
    /**********************************************************
     */

    /**
     * Object codec to use for stream-based object
     * conversion through parser/generator interfaces. If null,
     * such methods cannot be used.
     */
    protected ObjectCodec _objectCodec;

    /**
     * Parse context from "parent" parser (one from which content to buffer is read,
     * if specified). Used, if available, when reading content, to present full
     * context as if content was read from the original parser: this is useful
     * in error reporting and sometimes processing as well.
     */
    protected JsonStreamContext _parentContext;

    /**
     * Bit flag composed of bits that indicate which
     * {@link com.fasterxml.jackson.core.JsonGenerator.Feature}s
     * are enabled.
     *<p>
     * NOTE: most features have no effect on this class
     */
    protected int _generatorFeatures;

    /**
     * @since 2.15
     */
    protected StreamReadConstraints _streamReadConstraints = StreamReadConstraints.defaults();

    protected boolean _closed;

    /**
     * @since 2.3
     */
    protected boolean _hasNativeTypeIds;

    /**
     * @since 2.3
     */
    protected boolean _hasNativeObjectIds;

    /**
     * @since 2.3
     */
    protected boolean _mayHaveNativeIds;

    /**
     * Flag set during construction, if use of {@link BigDecimal} is to be forced
     * on all floating-point values.
     *
     * @since 2.7
     */
    protected boolean _forceBigDecimal;

    /*
    /**********************************************************
    /* Token buffering state
    /**********************************************************
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
    /**********************************************************
    /* Output state
    /**********************************************************
     */

    protected JsonWriteContext _writeContext;

    /*
    /**********************************************************
    /* Life-cycle
    /**********************************************************
     */

    /**
     * @param codec Object codec to use for stream-based object
     *   conversion through parser/generator interfaces. If null,
     *   such methods cannot be used.
     * @param hasNativeIds Whether resulting {@link JsonParser} (if created)
     *   is considered to support native type and object ids
     */
    public TokenBuffer(ObjectCodec codec, boolean hasNativeIds)
    {
        _objectCodec = codec;
        _generatorFeatures = DEFAULT_GENERATOR_FEATURES;
        _writeContext = JsonWriteContext.createRootContext(null);
        // at first we have just one segment
        _first = _last = new Segment();
        _appendAt = 0;
        _hasNativeTypeIds = hasNativeIds;
        _hasNativeObjectIds = hasNativeIds;

        _mayHaveNativeIds = _hasNativeTypeIds || _hasNativeObjectIds;
    }

    /**
     * @since 2.3
     */
    public TokenBuffer(JsonParser p) {
        this(p, null);
    }

    /**
     * @since 2.7
     */
    public TokenBuffer(JsonParser p, DeserializationContext ctxt)
    {
        _objectCodec = p.getCodec();
        _streamReadConstraints = p.streamReadConstraints();
        _parentContext = p.getParsingContext();
        _generatorFeatures = DEFAULT_GENERATOR_FEATURES;
        _writeContext = JsonWriteContext.createRootContext(null);
        // at first we have just one segment
        _first = _last = new Segment();
        _appendAt = 0;
        _hasNativeTypeIds = p.canReadTypeId();
        _hasNativeObjectIds = p.canReadObjectId();
        _mayHaveNativeIds = _hasNativeTypeIds || _hasNativeObjectIds;
        _forceBigDecimal = (ctxt == null) ? false
                : ctxt.isEnabled(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS);
    }

    /**
     * Convenience method, equivalent to:
     *<pre>
     * TokenBuffer b = new TokenBuffer(p);
     * b.copyCurrentStructure(p);
     * return b;
     *</pre>
     *
     * @since 2.9
     *
     * @deprecated Since 2.13: use {@link DeserializationContext#bufferAsCopyOfValue} instead.
     */
    @Deprecated // since 2.13
    public static TokenBuffer asCopyOfValue(JsonParser p) throws IOException {
        TokenBuffer b = new TokenBuffer(p);
        b.copyCurrentStructure(p);
        return b;
    }

    /**
     * Method that allows explicitly specifying parent parse context to associate
     * with contents of this buffer. Usually context is assigned at construction,
     * based on given parser; but it is not always available, and may not contain
     * intended context.
     *
     * @since 2.9
     */
    public TokenBuffer overrideParentContext(JsonStreamContext ctxt) {
        _parentContext = ctxt;
        return this;
    }

    /**
     * @since 2.7
     */
    public TokenBuffer forceUseOfBigDecimal(boolean b) {
        _forceBigDecimal = b;
        return this;
    }

    @Override
    public Version version() {
        return com.fasterxml.jackson.databind.cfg.PackageVersion.VERSION;
    }

    /**
     * Method used to create a {@link JsonParser} that can read contents
     * stored in this buffer. Will use default <code>_objectCodec</code> for
     * object conversions.
     *<p>
     * Note: instances are not synchronized, that is, they are not thread-safe
     * if there are concurrent appends to the underlying buffer.
     *
     * @return Parser that can be used for reading contents stored in this buffer
     */
    public JsonParser asParser() {
        return asParser(_objectCodec);
    }

    /**
     * Same as:
     *<pre>
     *  JsonParser p = asParser();
     *  p.nextToken();
     *  return p;
     *</pre>
     *
     * @since 2.9
     */
    public JsonParser asParserOnFirstToken() throws IOException {
        JsonParser p = asParser(_objectCodec);
        p.nextToken();
        return p;
    }

    /**
     * Method used to create a {@link JsonParser} that can read contents
     * stored in this buffer.
     *<p>
     * Note: instances are not synchronized, that is, they are not thread-safe
     * if there are concurrent appends to the underlying buffer.
     *
     * @param codec Object codec to use for stream-based object
     *   conversion through parser/generator interfaces. If null,
     *   such methods cannot be used.
     *
     * @return Parser that can be used for reading contents stored in this buffer
     */
    public JsonParser asParser(ObjectCodec codec)
    {
        return new Parser(_first, codec, _hasNativeTypeIds, _hasNativeObjectIds, _parentContext, _streamReadConstraints);
    }

    /**
     * @param streamReadConstraints constraints for streaming reads
     * @since v2.15
     */
    public JsonParser asParser(StreamReadConstraints streamReadConstraints)
    {
        return new Parser(_first, _objectCodec, _hasNativeTypeIds, _hasNativeObjectIds, _parentContext, streamReadConstraints);
    }

    /**
     * @param src Parser to use for accessing source information
     *    like location, configured codec, streamReadConstraints
     */
    public JsonParser asParser(JsonParser src)
    {
        Parser p = new Parser(_first, src.getCodec(), _hasNativeTypeIds, _hasNativeObjectIds,
                _parentContext, src.streamReadConstraints());
        p.setLocation(src.getTokenLocation());
        return p;
    }

    /*
    /**********************************************************
    /* Additional accessors
    /**********************************************************
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
    /**********************************************************
    /* Other custom methods not needed for implementing interfaces
    /**********************************************************
     */

    /**
     * Helper method that will append contents of given buffer into this
     * buffer.
     * Not particularly optimized; can be made faster if there is need.
     *
     * @return This buffer
     */
    @SuppressWarnings("resource")
    public TokenBuffer append(TokenBuffer other) throws IOException
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
     * <code>JsonSerializer</code>  for <code>TokenBuffer</code> type;
     * but we cannot have upwards
     * references (from core to mapper package); and as such we also
     * cannot take second argument.
     */
    public void serialize(JsonGenerator gen) throws IOException
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
            case FIELD_NAME:
            {
                // 13-Dec-2010, tatu: Maybe we should start using different type tokens to reduce casting?
                Object ob = segment.get(ptr);
                if (ob instanceof SerializableString) {
                    gen.writeFieldName((SerializableString) ob);
                } else {
                    gen.writeFieldName((String) ob);
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
                        gen.writeNumber(((Double) n).doubleValue());
                    } else if (n instanceof BigDecimal) {
                        gen.writeNumber((BigDecimal) n);
                    } else if (n instanceof Float) {
                        gen.writeNumber(((Float) n).floatValue());
                    } else if (n == null) {
                        gen.writeNull();
                    } else if (n instanceof String) {
                        gen.writeNumber((String) n);
                    } else {
                        _reportError(String.format(
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
                    } else if (value instanceof JsonSerializable) {
                        gen.writeObject(value);
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
     *
     * @since 2.3
     */
    public TokenBuffer deserialize(JsonParser p, DeserializationContext ctxt) throws IOException
    {
        if (!p.hasToken(JsonToken.FIELD_NAME)) {
            copyCurrentStructure(p);
            return this;
        }
        // 28-Oct-2014, tatu: As per [databind#592], need to support a special case of starting from
        //    FIELD_NAME, which is taken to mean that we are missing START_OBJECT, but need
        //    to assume one did exist.
        JsonToken t;
        writeStartObject();
        do {
            copyCurrentStructure(p);
        } while ((t = p.nextToken()) == JsonToken.FIELD_NAME);
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

        JsonParser jp = asParser();
        int count = 0;
        final boolean hasNativeIds = _hasNativeTypeIds || _hasNativeObjectIds;

        while (true) {
            JsonToken t;
            try {
                t = jp.nextToken();
                if (t == null) break;

                if (hasNativeIds) {
                    _appendNativeIds(sb);
                }

                if (count < MAX_COUNT) {
                    if (count > 0) {
                        sb.append(", ");
                    }
                    sb.append(t.toString());
                    if (t == JsonToken.FIELD_NAME) {
                        sb.append('(');
                        sb.append(jp.currentName());
                        sb.append(')');
                    }
                }
            } catch (IOException ioe) { // should never occur
                throw new IllegalStateException(ioe);
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
    /**********************************************************
    /* JsonGenerator implementation: configuration
    /**********************************************************
     */

    @Override
    public JsonGenerator enable(Feature f) {
        _generatorFeatures |= f.getMask();
        return this;
    }

    @Override
    public JsonGenerator disable(Feature f) {
        _generatorFeatures &= ~f.getMask();
        return this;
    }

    //public JsonGenerator configure(SerializationFeature f, boolean state) { }

    @Override
    public boolean isEnabled(Feature f) {
        return (_generatorFeatures & f.getMask()) != 0;
    }

    @Override
    public int getFeatureMask() {
        return _generatorFeatures;
    }

    // Note: cannot be removed until deprecated method removed from base class
    @Override
    @Deprecated
    public JsonGenerator setFeatureMask(int mask) {
        _generatorFeatures = mask;
        return this;
    }

    @Override
    public JsonGenerator overrideStdFeatures(int values, int mask) {
        int oldState = getFeatureMask();
        _generatorFeatures = (oldState & ~mask) | (values & mask);
        return this;
    }

    @Override
    public JsonGenerator useDefaultPrettyPrinter() {
        // No-op: we don't indent
        return this;
    }

    @Override
    public JsonGenerator setCodec(ObjectCodec oc) {
        _objectCodec = oc;
        return this;
    }

    @Override
    public ObjectCodec getCodec() { return _objectCodec; }

    @Override
    public final JsonWriteContext getOutputContext() { return _writeContext; }

    /*
    /**********************************************************
    /* JsonGenerator implementation: capability introspection
    /**********************************************************
     */

    /**
     * Since we can efficiently store <code>byte[]</code>, yes.
     */
    @Override
    public boolean canWriteBinaryNatively() {
        return true;
    }

    // 20-May-2020, tatu: This may or may not be enough -- ideally access is
    //    via `DeserializationContext`, not parser, but if latter is needed
    //    then we'll need to pass this from parser contents if which were
    //    buffered.
    @Override
    public JacksonFeatureSet<StreamWriteCapability> getWriteCapabilities() {
        return DEFAULT_WRITE_CAPABILITIES;
    }

    /*
    /**********************************************************
    /* JsonGenerator implementation: low-level output handling
    /**********************************************************
     */

    @Override
    public void flush() throws IOException { /* NOP */ }

    @Override
    public void close() throws IOException {
        _closed = true;
    }

    @Override
    public boolean isClosed() { return _closed; }

    /*
    /**********************************************************
    /* JsonGenerator implementation: write methods, structural
    /**********************************************************
     */

    @Override
    public final void writeStartArray() throws IOException
    {
        _writeContext.writeValue();
        _appendStartMarker(JsonToken.START_ARRAY);
        _writeContext = _writeContext.createChildArrayContext();
    }

    @Override // since 2.10.1
    public void writeStartArray(Object forValue) throws IOException {
        _writeContext.writeValue();
        _appendStartMarker(JsonToken.START_ARRAY);
        _writeContext = _writeContext.createChildArrayContext(forValue);
    }

    @Override // since 2.10.1
    public void writeStartArray(Object forValue, int size) throws IOException {
        _writeContext.writeValue();
        _appendStartMarker(JsonToken.START_ARRAY);
        _writeContext = _writeContext.createChildArrayContext(forValue);
    }

    @Override
    public final void writeEndArray() throws IOException
    {
        _appendEndMarker(JsonToken.END_ARRAY);
        // Let's allow unbalanced tho... i.e. not run out of root level, ever
        JsonWriteContext c = _writeContext.getParent();
        if (c != null) {
            _writeContext = c;
        }
    }

    @Override
    public final void writeStartObject() throws IOException
    {
        _writeContext.writeValue();
        _appendStartMarker(JsonToken.START_OBJECT);
        _writeContext = _writeContext.createChildObjectContext();
    }

    @Override // since 2.8
    public void writeStartObject(Object forValue) throws IOException
    {
        _writeContext.writeValue();
        _appendStartMarker(JsonToken.START_OBJECT);
        JsonWriteContext ctxt = _writeContext.createChildObjectContext(forValue);
        _writeContext = ctxt;
    }

    @Override // since 2.10.1
    public void writeStartObject(Object forValue, int size) throws IOException
    {
        _writeContext.writeValue();
        _appendStartMarker(JsonToken.START_OBJECT);
        JsonWriteContext ctxt = _writeContext.createChildObjectContext(forValue);
        _writeContext = ctxt;
    }

    @Override
    public final void writeEndObject() throws IOException
    {
        _appendEndMarker(JsonToken.END_OBJECT);
        // Let's allow unbalanced tho... i.e. not run out of root level, ever
        JsonWriteContext c = _writeContext.getParent();
        if (c != null) {
            _writeContext = c;
        }
    }

    @Override
    public final void writeFieldName(String name) throws IOException
    {
        _writeContext.writeFieldName(name);
        _appendFieldName(name);
    }

    @Override
    public void writeFieldName(SerializableString name) throws IOException
    {
        _writeContext.writeFieldName(name.getValue());
        _appendFieldName(name);
    }

    /*
    /**********************************************************
    /* JsonGenerator implementation: write methods, textual
    /**********************************************************
     */

    @Override
    public void writeString(String text) throws IOException {
        if (text == null) {
            writeNull();
        } else {
            _appendValue(JsonToken.VALUE_STRING, text);
        }
    }

    @Override
    public void writeString(char[] text, int offset, int len) throws IOException {
        writeString(new String(text, offset, len));
    }

    @Override
    public void writeString(SerializableString text) throws IOException {
        if (text == null) {
            writeNull();
        } else {
            _appendValue(JsonToken.VALUE_STRING, text);
        }
    }

    @Override
    public void writeRawUTF8String(byte[] text, int offset, int length) throws IOException
    {
        // could add support for buffering if we really want it...
        _reportUnsupportedOperation();
    }

    @Override
    public void writeUTF8String(byte[] text, int offset, int length) throws IOException
    {
        // could add support for buffering if we really want it...
        _reportUnsupportedOperation();
    }

    @Override
    public void writeRaw(String text) throws IOException {
        _reportUnsupportedOperation();
    }

    @Override
    public void writeRaw(String text, int offset, int len) throws IOException {
        _reportUnsupportedOperation();
    }

    @Override
    public void writeRaw(SerializableString text) throws IOException {
        _reportUnsupportedOperation();
    }

    @Override
    public void writeRaw(char[] text, int offset, int len) throws IOException {
        _reportUnsupportedOperation();
    }

    @Override
    public void writeRaw(char c) throws IOException {
        _reportUnsupportedOperation();
    }

    @Override
    public void writeRawValue(String text) throws IOException {
        _appendValue(JsonToken.VALUE_EMBEDDED_OBJECT, new RawValue(text));
    }

    @Override
    public void writeRawValue(String text, int offset, int len) throws IOException {
        if (offset > 0 || len != text.length()) {
            text = text.substring(offset, offset+len);
        }
        _appendValue(JsonToken.VALUE_EMBEDDED_OBJECT, new RawValue(text));
    }

    @Override
    public void writeRawValue(char[] text, int offset, int len) throws IOException {
        _appendValue(JsonToken.VALUE_EMBEDDED_OBJECT, new String(text, offset, len));
    }

    /*
    /**********************************************************
    /* JsonGenerator implementation: write methods, primitive types
    /**********************************************************
     */

    @Override
    public void writeNumber(short i) throws IOException {
        _appendValue(JsonToken.VALUE_NUMBER_INT, Short.valueOf(i));
    }

    @Override
    public void writeNumber(int i) throws IOException {
        _appendValue(JsonToken.VALUE_NUMBER_INT, Integer.valueOf(i));
    }

    @Override
    public void writeNumber(long l) throws IOException {
        _appendValue(JsonToken.VALUE_NUMBER_INT, Long.valueOf(l));
    }

    @Override
    public void writeNumber(double d) throws IOException {
        _appendValue(JsonToken.VALUE_NUMBER_FLOAT, Double.valueOf(d));
    }

    @Override
    public void writeNumber(float f) throws IOException {
        _appendValue(JsonToken.VALUE_NUMBER_FLOAT, Float.valueOf(f));
    }

    @Override
    public void writeNumber(BigDecimal dec) throws IOException {
        if (dec == null) {
            writeNull();
        } else {
            _appendValue(JsonToken.VALUE_NUMBER_FLOAT, dec);
        }
    }

    @Override
    public void writeNumber(BigInteger v) throws IOException {
        if (v == null) {
            writeNull();
        } else {
            _appendValue(JsonToken.VALUE_NUMBER_INT, v);
        }
    }

    @Override
    public void writeNumber(String encodedValue) throws IOException {
        /* 03-Dec-2010, tatu: related to [JACKSON-423], should try to keep as numeric
         *   identity as long as possible
         */
        _appendValue(JsonToken.VALUE_NUMBER_FLOAT, encodedValue);
    }

    private void writeLazyInteger(Object encodedValue) throws IOException {
        _appendValue(JsonToken.VALUE_NUMBER_INT, encodedValue);
    }

    private void writeLazyDecimal(Object encodedValue) throws IOException {
        _appendValue(JsonToken.VALUE_NUMBER_FLOAT, encodedValue);
    }

    @Override
    public void writeBoolean(boolean state) throws IOException {
        _appendValue(state ? JsonToken.VALUE_TRUE : JsonToken.VALUE_FALSE);
    }

    @Override
    public void writeNull() throws IOException {
        _appendValue(JsonToken.VALUE_NULL);
    }

    /*
    /***********************************************************
    /* JsonGenerator implementation: write methods for POJOs/trees
    /***********************************************************
     */

    @Override
    public void writeObject(Object value) throws IOException
    {
        if (value == null) {
            writeNull();
            return;
        }
        Class<?> raw = value.getClass();
        if (raw == byte[].class || (value instanceof RawValue)) {
            _appendValue(JsonToken.VALUE_EMBEDDED_OBJECT, value);
            return;
        }
        if (_objectCodec == null) {
            // 28-May-2014, tatu: Tricky choice here; if no codec, should we
            //   err out, or just embed? For now, do latter.

//          throw new XxxException("No ObjectCodec configured for TokenBuffer, writeObject() called");
            _appendValue(JsonToken.VALUE_EMBEDDED_OBJECT, value);
        } else {
            _objectCodec.writeValue(this, value);
        }
    }

    @Override
    public void writeTree(TreeNode node) throws IOException
    {
        if (node == null) {
            writeNull();
            return;
        }

        if (_objectCodec == null) {
            // as with 'writeObject()', is codec optional?
            _appendValue(JsonToken.VALUE_EMBEDDED_OBJECT, node);
        } else {
            _objectCodec.writeTree(this, node);
        }
    }

    /*
    /***********************************************************
    /* JsonGenerator implementation; binary
    /***********************************************************
     */

    @Override
    public void writeBinary(Base64Variant b64variant, byte[] data, int offset, int len) throws IOException
    {
        /* 31-Dec-2009, tatu: can do this using multiple alternatives; but for
         *   now, let's try to limit number of conversions.
         *   The only (?) tricky thing is that of whether to preserve variant,
         *   seems pointless, so let's not worry about it unless there's some
         *   compelling reason to.
         */
        byte[] copy = new byte[len];
        System.arraycopy(data, offset, copy, 0, len);
        writeObject(copy);
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
    /***********************************************************
    /* JsonGenerator implementation: native ids
    /***********************************************************
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
    public void writeTypeId(Object id) {
        _typeId = id;
        _hasNativeId = true;
    }

    @Override
    public void writeObjectId(Object id) {
        _objectId = id;
        _hasNativeId = true;
    }

    @Override // since 2.8
    public void writeEmbeddedObject(Object object) throws IOException {
        _appendValue(JsonToken.VALUE_EMBEDDED_OBJECT, object);
    }

    /*
    /**********************************************************
    /* JsonGenerator implementation; pass-through copy
    /**********************************************************
     */

    @Override
    public void copyCurrentEvent(JsonParser p) throws IOException
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
        case FIELD_NAME:
            writeFieldName(p.currentName());
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
            writeObject(p.getEmbeddedObject());
            break;
        default:
            throw new RuntimeException("Internal error: unexpected token: "+p.currentToken());
        }
    }

    @Override
    public void copyCurrentStructure(JsonParser p) throws IOException
    {
        JsonToken t = p.currentToken();

        // Let's handle field-name separately first
        if (t == JsonToken.FIELD_NAME) {
            if (_mayHaveNativeIds) {
                _checkNativeIds(p);
            }
            writeFieldName(p.currentName());
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

    protected void _copyBufferContents(JsonParser p) throws IOException
    {
        int depth = 1;
        JsonToken t;

        while ((t = p.nextToken()) != null) {
            switch (t) {
            case FIELD_NAME:
                if (_mayHaveNativeIds) {
                    _checkNativeIds(p);
                }
                writeFieldName(p.currentName());
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
    private void _copyBufferValue(JsonParser p, JsonToken t) throws IOException
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
            writeObject(p.getEmbeddedObject());
            break;
        default:
            throw new RuntimeException("Internal error: unexpected token: "+t);
        }
    }

    private final void _checkNativeIds(JsonParser p) throws IOException
    {
        if ((_typeId = p.getTypeId()) != null) {
            _hasNativeId = true;
        }
        if ((_objectId = p.getObjectId()) != null) {
            _hasNativeId = true;
        }
    }

    /*
    /**********************************************************
    /* Internal methods
    /**********************************************************
     */

    /*// Not used in / since 2.10
    protected final void _append(JsonToken type)
    {
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

    protected final void _append(JsonToken type, Object value)
    {
        Segment next;
        if (_hasNativeId) {
            next =  _last.append(_appendAt, type, value, _objectId, _typeId);
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
    */

    /**
     * Method used for appending token known to represent a "simple" scalar
     * value where token is the only information
     *
     * @since 2.6.4
     */
    protected final void _appendValue(JsonToken type)
    {
        _writeContext.writeValue();
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
     *
     * @since 2.6.4
     */
    protected final void _appendValue(JsonToken type, Object value)
    {
        _writeContext.writeValue();
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

    /**
     * Specialized method used for appending a field name, appending either
     * {@link String} or {@link SerializableString}.
     *
     * @since 2.10
     */
    protected final void _appendFieldName(Object value)
    {
        // NOTE: do NOT clear _objectId / _typeId
        Segment next;
        if (_hasNativeId) {
            next =  _last.append(_appendAt, JsonToken.FIELD_NAME, value, _objectId, _typeId);
        } else {
            next = _last.append(_appendAt, JsonToken.FIELD_NAME, value);
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
     *
     * @since 2.10
     */
    protected final void _appendStartMarker(JsonToken type)
    {
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
     *
     * @since 2.10
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
    }

    @Override
    protected void _reportUnsupportedOperation() {
        throw new UnsupportedOperationException("Called operation not supported for TokenBuffer");
    }

    /*
    /**********************************************************
    /* Supporting classes
    /**********************************************************
     */

    protected final static class Parser
        extends ParserMinimalBase
    {
        /*
        /**********************************************************
        /* Configuration
        /**********************************************************
         */

        protected ObjectCodec _codec;

        /**
         * @since 2.15
         */
        protected StreamReadConstraints _streamReadConstraints;

        /**
         * @since 2.3
         */
        protected final boolean _hasNativeTypeIds;

        /**
         * @since 2.3
         */
        protected final boolean _hasNativeObjectIds;

        protected final boolean _hasNativeIds;

        /*
        /**********************************************************
        /* Parsing state
        /**********************************************************
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
        /**********************************************************
        /* Construction, init
        /**********************************************************
         */

        @Deprecated // since 2.9
        public Parser(Segment firstSeg, ObjectCodec codec,
                boolean hasNativeTypeIds, boolean hasNativeObjectIds)
        {
            this(firstSeg, codec, hasNativeTypeIds, hasNativeObjectIds, null);
        }

        @Deprecated // since 2.15
        public Parser(Segment firstSeg, ObjectCodec codec,
                      boolean hasNativeTypeIds, boolean hasNativeObjectIds,
                      JsonStreamContext parentContext)
        {
            this(firstSeg, codec, hasNativeTypeIds, hasNativeObjectIds, parentContext, StreamReadConstraints.defaults());
        }

        public Parser(Segment firstSeg, ObjectCodec codec,
                boolean hasNativeTypeIds, boolean hasNativeObjectIds,
                JsonStreamContext parentContext, StreamReadConstraints streamReadConstraints)
        {
            // 25-Jun-2022, tatu: Ideally would pass parser flags along (as
            //    per [databund#3528]) but for now make sure not to clear the flags
            //    but let defaults be used
            super();
            _segment = firstSeg;
            _segmentPtr = -1; // not yet read
            _codec = codec;
            _streamReadConstraints = streamReadConstraints;
            _parsingContext = TokenBufferReadContext.createRootContext(parentContext);
            _hasNativeTypeIds = hasNativeTypeIds;
            _hasNativeObjectIds = hasNativeObjectIds;
            _hasNativeIds = (hasNativeTypeIds || hasNativeObjectIds);
        }

        public void setLocation(JsonLocation l) {
            _location = l;
        }

        @Override
        public ObjectCodec getCodec() { return _codec; }

        @Override
        public void setCodec(ObjectCodec c) { _codec = c; }

        /*
        /**********************************************************
        /* Public API, config access, capability introspection
        /**********************************************************
         */

        @Override
        public Version version() {
            return com.fasterxml.jackson.databind.cfg.PackageVersion.VERSION;
        }

        // 20-May-2020, tatu: This may or may not be enough -- ideally access is
        //    via `DeserializationContext`, not parser, but if latter is needed
        //    then we'll need to pass this from parser contents if which were
        //    buffered.
        @Override
        public JacksonFeatureSet<StreamReadCapability> getReadCapabilities() {
            return DEFAULT_READ_CAPABILITIES;
        }

        @Override
        public StreamReadConstraints streamReadConstraints() {
            return _streamReadConstraints;
        }

        /*
        /**********************************************************
        /* Extended API beyond JsonParser
        /**********************************************************
         */

        public JsonToken peekNextToken() throws IOException
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
        /**********************************************************
        /* Closeable implementation
        /**********************************************************
         */

        @Override
        public void close() throws IOException {
            if (!_closed) {
                _closed = true;
            }
        }

        /*
        /**********************************************************
        /* Public API, traversal
        /**********************************************************
         */

        @Override
        public JsonToken nextToken() throws IOException
        {
            // If we are closed, nothing more to do
            if (_closed || (_segment == null)) return null;

            // Ok, then: any more tokens?
            if (++_segmentPtr >= Segment.TOKENS_PER_SEGMENT) {
                _segmentPtr = 0;
                _segment = _segment.next();
                if (_segment == null) {
                    return null;
                }
            }
            _currToken = _segment.type(_segmentPtr);
            // Field name? Need to update context
            if (_currToken == JsonToken.FIELD_NAME) {
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
        public String nextFieldName() throws IOException
        {
            // inlined common case from nextToken()
            if (_closed || (_segment == null)) {
                return null;
            }

            int ptr = _segmentPtr+1;
            if ((ptr < Segment.TOKENS_PER_SEGMENT) && (_segment.type(ptr) == JsonToken.FIELD_NAME)) {
                _segmentPtr = ptr;
                _currToken = JsonToken.FIELD_NAME;
                Object ob = _segment.get(ptr); // inlined _currentObject();
                String name = (ob instanceof String) ? ((String) ob) : ob.toString();
                _parsingContext.setCurrentName(name);
                return name;
            }
            return (nextToken() == JsonToken.FIELD_NAME) ? currentName() : null;
        }

        @Override
        public boolean isClosed() { return _closed; }

        /*
        /**********************************************************
        /* Public API, token accessors
        /**********************************************************
         */

        @Override
        public JsonStreamContext getParsingContext() { return _parsingContext; }

        @Override
        public JsonLocation getTokenLocation() { return getCurrentLocation(); }

        @Override
        public JsonLocation getCurrentLocation() {
            return (_location == null) ? JsonLocation.NA : _location;
        }

        @Override
        public String currentName() {
            // 25-Jun-2015, tatu: as per [databind#838], needs to be same as ParserBase
            if (_currToken == JsonToken.START_OBJECT || _currToken == JsonToken.START_ARRAY) {
                JsonStreamContext parent = _parsingContext.getParent();
                return parent.getCurrentName();
            }
            return _parsingContext.getCurrentName();
        }

        @Override // since 2.12 delegate to the new method
        public String getCurrentName() { return currentName(); }

        @Override
        public void overrideCurrentName(String name)
        {
            // Simple, but need to look for START_OBJECT/ARRAY's "off-by-one" thing:
            JsonStreamContext ctxt = _parsingContext;
            if (_currToken == JsonToken.START_OBJECT || _currToken == JsonToken.START_ARRAY) {
                ctxt = ctxt.getParent();
            }
            if (ctxt instanceof TokenBufferReadContext) {
                try {
                    ((TokenBufferReadContext) ctxt).setCurrentName(name);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }

        /*
        /**********************************************************
        /* Public API, access to token information, text
        /**********************************************************
         */

        @Override
        public String getText()
        {
            // common cases first:
            if (_currToken == JsonToken.VALUE_STRING
                    || _currToken == JsonToken.FIELD_NAME) {
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
        /**********************************************************
        /* Public API, access to token information, numeric
        /**********************************************************
         */

        @Override
        public boolean isNaN() {
            // can only occur for floating-point numbers
            if (_currToken == JsonToken.VALUE_NUMBER_FLOAT) {
                Object value = _currentObject();
                if (value instanceof Double) {
                    Double v = (Double) value;
                    return v.isNaN() || v.isInfinite();
                }
                if (value instanceof Float) {
                    Float v = (Float) value;
                    return v.isNaN() || v.isInfinite();
                }
            }
            return false;
        }

        @Override
        public BigInteger getBigIntegerValue() throws IOException
        {
            Number n = getNumberValue(true);
            if (n instanceof BigInteger) {
                return (BigInteger) n;
            } else if (n instanceof BigDecimal) {
                return ((BigDecimal) n).toBigInteger();
            }
            // int/long is simple, but let's also just truncate float/double:
            return BigInteger.valueOf(n.longValue());
        }

        @Override
        public BigDecimal getDecimalValue() throws IOException
        {
            Number n = getNumberValue(true);
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
        public double getDoubleValue() throws IOException {
            return getNumberValue().doubleValue();
        }

        @Override
        public float getFloatValue() throws IOException {
            return getNumberValue().floatValue();
        }

        @Override
        public int getIntValue() throws IOException
        {
            Number n = (_currToken == JsonToken.VALUE_NUMBER_INT) ?
                    ((Number) _currentObject()) : getNumberValue();
            if ((n instanceof Integer) || _smallerThanInt(n)) {
                return n.intValue();
            }
            return _convertNumberToInt(n);
        }

        @Override
        public long getLongValue() throws IOException {
            Number n = (_currToken == JsonToken.VALUE_NUMBER_INT) ?
                    ((Number) _currentObject()) : getNumberValue();
            if ((n instanceof Long) || _smallerThanLong(n)) {
                return n.longValue();
            }
            return _convertNumberToLong(n);
        }

        @Override
        public NumberType getNumberType() throws IOException
        {
            Object n = getNumberValueDeferred();
            if (n instanceof Integer) return NumberType.INT;
            if (n instanceof Long) return NumberType.LONG;
            if (n instanceof Double) return NumberType.DOUBLE;
            if (n instanceof BigDecimal) return NumberType.BIG_DECIMAL;
            if (n instanceof BigInteger) return NumberType.BIG_INTEGER;
            if (n instanceof Float) return NumberType.FLOAT;
            if (n instanceof Short) return NumberType.INT;       // should be SHORT
            if (n instanceof String) {
                return (_currToken == JsonToken.VALUE_NUMBER_FLOAT)
                        ? NumberType.BIG_DECIMAL : NumberType.BIG_INTEGER;
            }
            return null;
        }

        @Override
        public final Number getNumberValue() throws IOException {
            return getNumberValue(false);
        }

        @Override
        public Object getNumberValueDeferred() throws IOException {
            _checkIsNumber();
            return _currentObject();
        }

        private Number getNumberValue(final boolean preferBigNumbers) throws IOException {
            _checkIsNumber();
            Object value = _currentObject();
            if (value instanceof Number) {
                return (Number) value;
            }
            // Difficult to really support numbers-as-Strings; but let's try.
            // NOTE: no access to DeserializationConfig, unfortunately, so cannot
            // try to determine Double/BigDecimal preference...
            if (value instanceof String) {
                String str = (String) value;
                final int len = str.length();
                if (_currToken == JsonToken.VALUE_NUMBER_INT) {
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

        protected int _convertNumberToInt(Number n) throws IOException
        {
            if (n instanceof Long) {
                long l = n.longValue();
                int result = (int) l;
                if (((long) result) != l) {
                    reportOverflowInt();
                }
                return result;
            }
            if (n instanceof BigInteger) {
                BigInteger big = (BigInteger) n;
                if (BI_MIN_INT.compareTo(big) > 0
                        || BI_MAX_INT.compareTo(big) < 0) {
                    reportOverflowInt();
                }
            } else if ((n instanceof Double) || (n instanceof Float)) {
                double d = n.doubleValue();
                // Need to check boundaries
                if (d < MIN_INT_D || d > MAX_INT_D) {
                    reportOverflowInt();
                }
                return (int) d;
            } else if (n instanceof BigDecimal) {
                BigDecimal big = (BigDecimal) n;
                if (BD_MIN_INT.compareTo(big) > 0
                    || BD_MAX_INT.compareTo(big) < 0) {
                    reportOverflowInt();
                }
            } else {
                _throwInternal();
            }
            return n.intValue();
        }

        protected long _convertNumberToLong(Number n) throws IOException
        {
            if (n instanceof BigInteger) {
                BigInteger big = (BigInteger) n;
                if (BI_MIN_LONG.compareTo(big) > 0
                        || BI_MAX_LONG.compareTo(big) < 0) {
                    reportOverflowLong();
                }
            } else if ((n instanceof Double) || (n instanceof Float)) {
                double d = n.doubleValue();
                // Need to check boundaries
                if (d < MIN_LONG_D || d > MAX_LONG_D) {
                    reportOverflowLong();
                }
                return (long) d;
            } else if (n instanceof BigDecimal) {
                BigDecimal big = (BigDecimal) n;
                if (BD_MIN_LONG.compareTo(big) > 0
                    || BD_MAX_LONG.compareTo(big) < 0) {
                    reportOverflowLong();
                }
            } else {
                _throwInternal();
            }
            return n.longValue();
        }

        /*
        /**********************************************************
        /* Public API, access to token information, other
        /**********************************************************
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
        public byte[] getBinaryValue(Base64Variant b64variant) throws IOException
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
                throw _constructError("Current token ("+_currToken+") not VALUE_STRING (or VALUE_EMBEDDED_OBJECT with byte[]), cannot access as binary");
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
        public int readBinaryValue(Base64Variant b64variant, OutputStream out) throws IOException
        {
            byte[] data = getBinaryValue(b64variant);
            if (data != null) {
                out.write(data, 0, data.length);
                return data.length;
            }
            return 0;
        }

        /*
        /**********************************************************
        /* Public API, native ids
        /**********************************************************
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
        /**********************************************************
        /* Internal methods
        /**********************************************************
         */

        protected final Object _currentObject() {
            return _segment.get(_segmentPtr);
        }

        protected final void _checkIsNumber() throws JacksonException
        {
            if (_currToken == null || !_currToken.isNumeric()) {
                throw _constructError("Current token ("+_currToken+") not numeric, cannot use numeric value accessors");
            }
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
     * use 16 distinct fields and switch statement (slightly more efficient
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
            int ix = ((int) l) & 0xF;
            return ix;
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

        /*
        public Segment appendRaw(int index, int rawTokenType, Object value)
        {
            if (index < TOKENS_PER_SEGMENT) {
                set(index, rawTokenType, value);
                return null;
            }
            _next = new Segment();
            _next.set(0, rawTokenType, value);
            return _next;
        }

        public Segment appendRaw(int index, int rawTokenType, Object value,
                Object objectId, Object typeId)
        {
            if (index < TOKENS_PER_SEGMENT) {
                set(index, rawTokenType, value, objectId, typeId);
                return null;
            }
            _next = new Segment();
            _next.set(0, rawTokenType, value, objectId, typeId);
            return _next;
        }

        private void set(int index, int rawTokenType, Object value, Object objectId, Object typeId)
        {
            _tokens[index] = value;
            long typeCode = (long) rawTokenType;
            if (index > 0) {
                typeCode <<= (index << 2);
            }
            _tokenTypes |= typeCode;
            assignNativeIds(index, objectId, typeId);
        }

        private void set(int index, int rawTokenType, Object value)
        {
            _tokens[index] = value;
            long typeCode = (long) rawTokenType;
            if (index > 0) {
                typeCode <<= (index << 2);
            }
            _tokenTypes |= typeCode;
        }
        */

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

        /**
         * @since 2.3
         */
        Object findObjectId(int index) {
            return (_nativeIds == null) ? null : _nativeIds.get(_objectIdIndex(index));
        }

        /**
         * @since 2.3
         */
        Object findTypeId(int index) {
            return (_nativeIds == null) ? null : _nativeIds.get(_typeIdIndex(index));
        }

        private final int _typeIdIndex(int i) { return i+i; }
        private final int _objectIdIndex(int i) { return i+i+1; }
    }
}
