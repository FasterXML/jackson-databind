package com.fasterxml.jackson.databind.node;

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.math.BigInteger;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.core.base.ParserMinimalBase;
import com.fasterxml.jackson.core.util.JacksonFeatureSet;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * Facade over {@link JsonNode} that implements {@link JsonParser} to allow
 * accessing contents of JSON tree in alternate form (stream of tokens).
 * Useful when a streaming source is expected by code, such as data binding
 * functionality.
 */
public class TreeTraversingParser extends ParserMinimalBase
{
    /*
    /**********************************************************
    /* Configuration
    /**********************************************************
     */

    protected ObjectCodec _objectCodec;

    /**
     * Traversal context within tree
     */
    protected NodeCursor _nodeCursor;

    /*
    /**********************************************************
    /* State
    /**********************************************************
     */

    /**
     * Flag that indicates whether parser is closed or not. Gets
     * set when parser is either closed by explicit call
     * ({@link #close}) or when end-of-input is reached.
     */
    protected boolean _closed;

    /*
    /**********************************************************
    /* Life-cycle
    /**********************************************************
     */

    public TreeTraversingParser(JsonNode n) { this(n, null); }

    public TreeTraversingParser(JsonNode n, ObjectCodec codec)
    {
        super(0);
        _objectCodec = codec;
        _nodeCursor = new NodeCursor.RootCursor(n, null);
    }

    @Override
    public void setCodec(ObjectCodec c) {
        _objectCodec = c;
    }

    @Override
    public ObjectCodec getCodec() {
        return _objectCodec;
    }

    @Override
    public Version version() {
        return com.fasterxml.jackson.databind.cfg.PackageVersion.VERSION;
    }

    @Override
    public JacksonFeatureSet<StreamReadCapability> getReadCapabilities() {
        // Defaults are fine
        return DEFAULT_READ_CAPABILITIES;
    }

    /*
    /**********************************************************
    /* Closeable implementation
    /**********************************************************
     */

    @Override
    public void close() throws IOException
    {
        if (!_closed) {
            _closed = true;
            _nodeCursor = null;
            _currToken = null;
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
        _currToken = _nodeCursor.nextToken();
        if (_currToken == null) {
            _closed = true; // if not already set
            return null;
        }
        switch (_currToken) {
        case START_OBJECT:
            _nodeCursor = _nodeCursor.startObject();
            break;
        case START_ARRAY:
            _nodeCursor = _nodeCursor.startArray();
            break;
        case END_OBJECT:
        case END_ARRAY:
            _nodeCursor = _nodeCursor.getParent();
        default:
        }
        return _currToken;
    }

    // default works well here:
    //public JsonToken nextValue() throws IOException

    @Override
    public JsonParser skipChildren() throws IOException
    {
        if (_currToken == JsonToken.START_OBJECT) {
            _nodeCursor = _nodeCursor.getParent();
            _currToken = JsonToken.END_OBJECT;
        } else if (_currToken == JsonToken.START_ARRAY) {
            _nodeCursor = _nodeCursor.getParent();
            _currToken = JsonToken.END_ARRAY;
        }
        return this;
    }

    @Override
    public boolean isClosed() {
        return _closed;
    }

    /*
    /**********************************************************
    /* Public API, token accessors
    /**********************************************************
     */

    @Override public String getCurrentName() {
        NodeCursor crsr = _nodeCursor;
        if (_currToken == JsonToken.START_OBJECT || _currToken == JsonToken.START_ARRAY) {
            crsr = crsr.getParent();
        }
        return (crsr == null) ? null : crsr.getCurrentName();
    }

    @Override public void overrideCurrentName(String name) {
        NodeCursor crsr = _nodeCursor;
        if (_currToken == JsonToken.START_OBJECT || _currToken == JsonToken.START_ARRAY) {
            crsr = crsr.getParent();
        }
        if (crsr != null) {
            crsr.overrideCurrentName(name);
        }
    }

    @Override
    public JsonStreamContext getParsingContext() {
        return _nodeCursor;
    }

    @Override
    public JsonLocation getTokenLocation() {
        return JsonLocation.NA;
    }

    @Override
    public JsonLocation getCurrentLocation() {
        return JsonLocation.NA;
    }

    /*
    /**********************************************************
    /* Public API, access to textual content
    /**********************************************************
     */

    @Override
    public String getText()
    {
        if (_currToken == null) {
            return null;
        }
        // need to separate handling a bit...
        switch (_currToken) {
        case FIELD_NAME:
            return _nodeCursor.getCurrentName();
        case VALUE_STRING:
            return currentNode().textValue();
        case VALUE_NUMBER_INT:
        case VALUE_NUMBER_FLOAT:
            return String.valueOf(currentNode().numberValue());
        case VALUE_EMBEDDED_OBJECT:
            JsonNode n = currentNode();
            if (n != null && n.isBinary()) {
                // this will convert it to base64
                return n.asText();
            }
        default:
            return _currToken.asString();
        }
    }

    @Override
    public char[] getTextCharacters() throws IOException {
        return getText().toCharArray();
    }

    @Override
    public int getTextLength() throws IOException {
        return getText().length();
    }

    @Override
    public int getTextOffset() throws IOException {
        return 0;
    }

    @Override
    public boolean hasTextCharacters() {
        // generally we do not have efficient access as char[], hence:
        return false;
    }

    /*
    /**********************************************************
    /* Public API, typed non-text access
    /**********************************************************
     */

    //public byte getByteValue() throws IOException

    @Override
    public NumberType getNumberType() throws IOException {
        JsonNode n = currentNumericNode();
        return (n == null) ? null : n.numberType();
    }

    @Override
    public BigInteger getBigIntegerValue() throws IOException
    {
        return currentNumericNode().bigIntegerValue();
    }

    @Override
    public BigDecimal getDecimalValue() throws IOException {
        return currentNumericNode().decimalValue();
    }

    @Override
    public double getDoubleValue() throws IOException {
        return currentNumericNode().doubleValue();
    }

    @Override
    public float getFloatValue() throws IOException {
        return (float) currentNumericNode().doubleValue();
    }

    @Override
    public int getIntValue() throws IOException {
        final NumericNode node = (NumericNode) currentNumericNode();
        if (!node.canConvertToInt()) {
            reportOverflowInt();
        }
        return node.intValue();
    }

    @Override
    public long getLongValue() throws IOException {
        final NumericNode node = (NumericNode) currentNumericNode();
        if (!node.canConvertToLong()) {
            reportOverflowLong();
        }
        return node.longValue();
    }

    @Override
    public Number getNumberValue() throws IOException {
        return currentNumericNode().numberValue();
    }

    @Override
    public Object getEmbeddedObject()
    {
        if (!_closed) {
            JsonNode n = currentNode();
            if (n != null) {
                if (n.isPojo()) {
                    return ((POJONode) n).getPojo();
                }
                if (n.isBinary()) {
                    return ((BinaryNode) n).binaryValue();
                }
            }
        }
        return null;
    }

    @Override
    public boolean isNaN() {
        if (!_closed) {
            JsonNode n = currentNode();
            if (n instanceof NumericNode) {
                return ((NumericNode) n).isNaN();
            }
        }
        return false;
    }

    /*
    /**********************************************************
    /* Public API, typed binary (base64) access
    /**********************************************************
     */

    @Override
    public byte[] getBinaryValue(Base64Variant b64variant)
        throws IOException
    {
        // Multiple possibilities...
        JsonNode n = currentNode();
        if (n != null) {
            // [databind#2096]: although `binaryValue()` works for real binary node
            // and embedded "POJO" node, coercion from TextNode may require variant, so:
            if (n instanceof TextNode) {
                return ((TextNode) n).getBinaryValue(b64variant);
            }
            return n.binaryValue();
        }
        // otherwise return null to mark we have no binary content
        return null;
    }


    @Override
    public int readBinaryValue(Base64Variant b64variant, OutputStream out)
        throws IOException
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
    /* Internal methods
    /**********************************************************
     */

    protected JsonNode currentNode() {
        if (_closed || _nodeCursor == null) {
            return null;
        }
        return _nodeCursor.currentNode();
    }

    protected JsonNode currentNumericNode()
        throws JacksonException
    {
        JsonNode n = currentNode();
        if (n == null || !n.isNumber()) {
            JsonToken t = (n == null) ? null : n.asToken();
            throw _constructError("Current token ("+t+") not numeric, cannot use numeric value accessors");
        }
        return n;
    }

    @Override
    protected void _handleEOF() {
        _throwInternal(); // should never get called
    }
}
