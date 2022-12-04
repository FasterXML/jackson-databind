package tools.jackson.databind.node;

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.math.BigInteger;

import tools.jackson.core.*;
import tools.jackson.core.base.ParserMinimalBase;
import tools.jackson.core.exc.InputCoercionException;
import tools.jackson.core.util.JacksonFeatureSet;
import tools.jackson.databind.JsonNode;

/**
 * Facade over {@link JsonNode} that implements {@link JsonParser} to allow
 * accessing contents of JSON tree in alternate form (stream of tokens).
 * Useful when a streaming source is expected by code, such as data binding
 * functionality.
 */
public class TreeTraversingParser
    extends ParserMinimalBase
{
    /*
    /**********************************************************************
    /* Configuration
    /**********************************************************************
     */

    /**
     * @since 3.0
     */
    protected final JsonNode _source;

    /**
     * Traversal context within tree
     */
    protected NodeCursor _nodeCursor;

    /*
    /**********************************************************************
    /* State
    /**********************************************************************
     */

    /**
     * Flag that indicates whether parser is closed or not. Gets
     * set when parser is either closed by explicit call
     * ({@link #close}) or when end-of-input is reached.
     */
    protected boolean _closed;

    /*
    /**********************************************************************
    /* Life-cycle
    /**********************************************************************
     */

    public TreeTraversingParser(JsonNode n) { this(n, ObjectReadContext.empty()); }

    public TreeTraversingParser(JsonNode n, ObjectReadContext readContext)
    {
        super(readContext);
        _source = n;
        _nodeCursor = new NodeCursor.RootCursor(n, null);
    }

    @Override
    public Version version() {
        return tools.jackson.databind.cfg.PackageVersion.VERSION;
    }

    @Override
    public JacksonFeatureSet<StreamReadCapability> streamReadCapabilities() {
        // Defaults are fine
        return DEFAULT_READ_CAPABILITIES;
    }

    @Override
    public JsonNode streamReadInputSource() {
        return _source;
    }

    // Default from base class should be fine:
    //public StreamReadConstraints streamReadConstraints() {

    /*
    /**********************************************************************
    /* Closeable implementation
    /**********************************************************************
     */

    @Override
    public void close()
    {
        if (!_closed) {
            _closed = true;
            _nodeCursor = null;
            _currToken = null;
        }
    }

    /*
    /**********************************************************************
    /* Public API, traversal
    /**********************************************************************
     */

    @Override
    public JsonToken nextToken()
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
    //public JsonToken nextValue()

    @Override
    public JsonParser skipChildren()
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
    /**********************************************************************
    /* Public API, token accessors
    /**********************************************************************
     */

    @Override public String currentName() {
        NodeCursor crsr = _nodeCursor;
        if (_currToken == JsonToken.START_OBJECT || _currToken == JsonToken.START_ARRAY) {
            crsr = crsr.getParent();
        }
        return (crsr == null) ? null : crsr.currentName();
    }

    @Override
    public TokenStreamContext streamReadContext() {
        return _nodeCursor;
    }

    @Override public void assignCurrentValue(Object v) { _nodeCursor.assignCurrentValue(v); }
    @Override public Object currentValue() { return _nodeCursor.currentValue(); }

    @Override
    public JsonLocation currentTokenLocation() {
        return JsonLocation.NA;
    }

    @Override
    public JsonLocation currentLocation() {
        return JsonLocation.NA;
    }

    /*
    /**********************************************************************
    /* Public API, access to textual content
    /**********************************************************************
     */

    @Override
    public String getText()
    {
        if (_currToken == null) {
            return null;
        }
        // need to separate handling a bit...
        switch (_currToken) {
        case PROPERTY_NAME:
            return _nodeCursor.currentName();
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
    public char[] getTextCharacters() {
        return getText().toCharArray();
    }

    @Override
    public int getTextLength() {
        return getText().length();
    }

    @Override
    public int getTextOffset() {
        return 0;
    }

    @Override
    public boolean hasTextCharacters() {
        // generally we do not have efficient access as char[], hence:
        return false;
    }

    /*
    /**********************************************************************
    /* Public API, typed non-text access
    /**********************************************************************
     */

    //public byte getByteValue();

    @Override
    public NumberType getNumberType() {
        // NOTE: do not call "currentNumericNode()" as that would throw exception
        // on non-numeric node
        JsonNode n = currentNode();
        if (n instanceof NumericNode) {
            return n.numberType();
        }
        return null;
    }

    @Override
    public BigInteger getBigIntegerValue() throws InputCoercionException {
        return currentNumericNode(NR_BIGINT).bigIntegerValue();
    }

    @Override
    public BigDecimal getDecimalValue() throws InputCoercionException {
        return currentNumericNode(NR_BIGDECIMAL).decimalValue();
    }

    @Override
    public double getDoubleValue() throws InputCoercionException {
        return currentNumericNode(NR_DOUBLE).doubleValue();
    }

    @Override
    public float getFloatValue() throws InputCoercionException {
        return (float) currentNumericNode(NR_FLOAT).doubleValue();
    }

    @Override
    public int getIntValue() throws InputCoercionException {
        final NumericNode node = (NumericNode) currentNumericNode(NR_INT);
        if (!node.canConvertToInt()) {
            _reportOverflowInt();
        }
        return node.intValue();
    }

    @Override
    public long getLongValue() throws InputCoercionException {
        final NumericNode node = (NumericNode) currentNumericNode(NR_LONG);
        if (!node.canConvertToLong()) {
            _reportOverflowLong();
        }
        return node.longValue();
    }

    @Override
    public Number getNumberValue() throws InputCoercionException {
        return currentNumericNode(-1).numberValue();
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
    /**********************************************************************
    /* Public API, typed binary (base64) access
    /**********************************************************************
     */

    @Override
    public byte[] getBinaryValue(Base64Variant b64variant)
        throws JacksonException
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
    /**********************************************************************
    /* Internal methods
    /**********************************************************************
     */

    protected JsonNode currentNode() {
        if (_closed || _nodeCursor == null) {
            return null;
        }
        return _nodeCursor.currentNode();
    }

    protected JsonNode currentNumericNode(int targetNumType)
    {
        JsonNode n = currentNode();
        if (n == null || !n.isNumber()) {
            JsonToken t = (n == null) ? null : n.asToken();
            throw _constructNotNumericType(t, -1);
        }
        return n;
    }

    @Override
    protected void _handleEOF() {
        _throwInternal(); // should never get called
    }
}
