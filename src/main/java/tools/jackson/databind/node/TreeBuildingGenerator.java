package tools.jackson.databind.node;

import java.io.InputStream;
import java.io.Reader;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;

import tools.jackson.core.*;
import tools.jackson.core.util.JacksonFeatureSet;
import tools.jackson.databind.*;
import tools.jackson.databind.util.RawValue;

/**
 * Helper class used for creating {@link JsonNode} values directly
 * as part of serialization.
 *
 * @since 3.0
 */
public class TreeBuildingGenerator
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

    protected final ObjectWriteContext _objectWriteContext;

    protected final JsonNodeFactory _nodeFactory;

    /**
     * Bit flag composed of bits that indicate which
     * {@link StreamWriteFeature}s
     * are enabled.
     *<p>
     * NOTE: most features have no effect on this class
     */
    protected final int _streamWriteFeatures;

    /*
    /**********************************************************************
    /* Output state
    /**********************************************************************
     */

    protected RootContext _rootWriteContext;

    protected TreeWriteContext _tokenWriteContext;

    /*
    /**********************************************************************
    /* Life-cycle
    /**********************************************************************
     */

    TreeBuildingGenerator(ObjectWriteContext owCtxt, JsonNodeFactory nodeFactory)
    {
        _objectWriteContext = owCtxt;
        _nodeFactory = nodeFactory;
        _streamWriteFeatures = DEFAULT_STREAM_WRITE_FEATURES;
        _rootWriteContext = new RootContext(nodeFactory);
        _tokenWriteContext = _rootWriteContext;
    }

    public static TreeBuildingGenerator forSerialization(SerializerProvider ctxt,
            JsonNodeFactory nodeFactory) {
        return new TreeBuildingGenerator(ctxt, nodeFactory);
    }

    public JsonNode treeBuilt() {
        return _rootWriteContext._node;
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
    /* JsonGenerator implementation: configuration, introspection
    /**********************************************************************
     */

    @Override
    public Version version() {
        return tools.jackson.databind.cfg.PackageVersion.VERSION;
    }

    // Should not try to reconfigure
    @Override
    public JsonGenerator configure(StreamWriteFeature f, boolean state) {
        return this;
    }

    @Override
    public boolean isEnabled(StreamWriteFeature f) {
        return (_streamWriteFeatures & f.getMask()) != 0;
    }

    @Override
    public int streamWriteFeatures() {
        return _streamWriteFeatures;
    }

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

    // 01-Mar-2021, tatu: Doesn't look like close()/state valuable but must implement

    @Override
    public void close() { }

    @Override
    public boolean isClosed() { return false; }

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
    public void writeStartArray() {
        writeStartArray(null);
    }

    @Override
    public void writeStartArray(Object forValue) {
        _tokenWriteContext = _tokenWriteContext.createChildArrayContext(forValue);
    }

    @Override
    public void writeStartArray(Object forValue, int len) {
        writeStartArray(forValue);
    }

    @Override
    public void writeEndArray() {
        if (!_tokenWriteContext.inArray()) {
            _reportError("Current context not Array but "+_tokenWriteContext.typeDesc());
        }
        _tokenWriteContext = _tokenWriteContext.getParent();
    }

    @Override
    public final void writeStartObject() {
        writeStartObject(null);
    }

    @Override
    public void writeStartObject(Object forValue)
    {
        _tokenWriteContext = _tokenWriteContext.createChildObjectContext(forValue);
    }

    @Override
    public void writeStartObject(Object forValue, int size) {
        writeStartObject(forValue);
    }

    @Override
    public final void writeEndObject() {
        if (!_tokenWriteContext.inObject()) {
            _reportError("Current context not Object but "+_tokenWriteContext.typeDesc());
        }
        _tokenWriteContext = _tokenWriteContext.getParent();
    }

    @Override
    public final void writeName(String name) {
        _tokenWriteContext.writeName(name);
    }

    @Override
    public void writeName(SerializableString name) {
        _tokenWriteContext.writeName(name.getValue());
    }

    @Override
    public void writePropertyId(long id) {
        // Cannot really preserve currently so
        writeName(Long.toString(id));
    }

    /*
    /**********************************************************************
    /* JsonGenerator implementation: write methods, textual
    /**********************************************************************
     */

    @Override
    public void writeString(String text) {
        if (text == null) {
            writeNull();
        } else {
            _tokenWriteContext.writeString(text);
        }
    }

    @Override
    public void writeString(char[] text, int offset, int len) {
        writeString(new String(text, offset, len));
    }

    @Override
    public void writeString(SerializableString text) {
        if (text == null) {
            writeNull();
        } else {
            _tokenWriteContext.writeString(text.getValue());
        }
    }

    // In 3.0 no longer implemented by `JsonGenerator, impl copied:
    @Override
    public void writeString(Reader reader, int len) {
        // Let's implement this as "unsupported" to make it easier to add new parser impls
        _reportUnsupportedOperation();
    }

    @Override
    public void writeRawUTF8String(byte[] text, int offset, int length) {
        // could add support for buffering if we really want it...
        _reportUnsupportedOperation();
    }

    @Override
    public void writeUTF8String(byte[] text, int offset, int length) {
        // could add support for buffering if we really want it...
        _reportUnsupportedOperation();
    }

    @Override
    public void writeRaw(String text) {
        _reportUnsupportedOperation();
    }

    @Override
    public void writeRaw(String text, int offset, int len) {
        _reportUnsupportedOperation();
    }

    @Override
    public void writeRaw(SerializableString text) {
        _reportUnsupportedOperation();
    }

    @Override
    public void writeRaw(char[] text, int offset, int len) {
        _reportUnsupportedOperation();
    }

    @Override
    public void writeRaw(char c) {
        _reportUnsupportedOperation();
    }

    @Override
    public void writeRawValue(String text) {
        _tokenWriteContext.writeNode(_nodeFactory.rawValueNode(new RawValue(text)));
    }

    @Override
    public void writeRawValue(String text, int offset, int len) {
        if (offset > 0 || len != text.length()) {
            text = text.substring(offset, offset+len);
        }
        writeRawValue(text);
    }

    @Override
    public void writeRawValue(char[] text, int offset, int len) {
        writeRawValue(new String(text, offset, len));
    }

    /*
    /**********************************************************************
    /* JsonGenerator implementation: write methods, primitive types
    /**********************************************************************
     */

    @Override
    public void writeNumber(short v) {
        _tokenWriteContext.writeNumber(_nodeFactory.numberNode(v));
    }

    @Override
    public void writeNumber(int v) {
        _tokenWriteContext.writeNumber(_nodeFactory.numberNode(v));
    }

    @Override
    public void writeNumber(long v) {
        _tokenWriteContext.writeNumber(_nodeFactory.numberNode(v));
    }

    @Override
    public void writeNumber(double v) {
        _tokenWriteContext.writeNumber(_nodeFactory.numberNode(v));
    }

    @Override
    public void writeNumber(float v) {
        _tokenWriteContext.writeNumber(_nodeFactory.numberNode(v));
    }

    @Override
    public void writeNumber(BigDecimal v) {
        if (v == null) {
            writeNull();
        } else {
            _tokenWriteContext.writeNumber(_nodeFactory.numberNode(v));
        }
    }

    @Override
    public void writeNumber(BigInteger v) {
        if (v == null) {
            writeNull();
        } else {
            _tokenWriteContext.writeNumber(_nodeFactory.numberNode(v));
        }
    }

    @Override
    public void writeNumber(String encodedValue) {
        writeString(encodedValue);
    }

    @Override
    public void writeBoolean(boolean state) {
        _tokenWriteContext.writeBoolean(state);
    }

    @Override
    public void writeNull() {
        _tokenWriteContext.writeNull();
    }

    /*
    /**********************************************************************
    /* JsonGenerator implementation: write methods for POJOs/trees
    /**********************************************************************
     */

    @Override
    public void writePOJO(Object value)
    {
        if (value == null) {
            writeNull();
            return;
        }
        // 02-Mar-2021, tatu: This is bit tricky; probably should be configurable,
        //    but for now let's follow what `TokenBuffer` does and by default
        //    fully serialize given Java value... unless it's one of a small
        //    number of "well-known" types to preserve.
        final Class<?> raw = value.getClass();
        if (raw == byte[].class || (value instanceof RawValue)) {
            _tokenWriteContext.writePOJO(value);
            return;
        }
        _objectWriteContext.writeValue(this, value);
    }

    @Override
    public void writeTree(TreeNode node)
    {
        if (node == null) {
            writeNull();
            return;
        }
        if (node instanceof JsonNode) {
            _tokenWriteContext.writeNode((JsonNode) node);
        } else {
            _tokenWriteContext.writePOJO(node);
        }
    }

    /*
    /**********************************************************************
    /* JsonGenerator implementation; binary
    /**********************************************************************
     */

    @Override
    public void writeBinary(Base64Variant b64variant, byte[] data, int offset, int len)
    {
        // 12-Jan-2021, tatu: Should we try to preserve the variant? Depends a
        //   lot on whether this during read (no need to retain probably) or
        //   write (probably important)
        byte[] copy = Arrays.copyOfRange(data, offset, offset + len);
        writePOJO(copy);
    }

    /**
     * Although we could support this method, it does not necessarily make
     * sense: we cannot make good use of streaming because buffer must
     * hold all the data. Because of this, currently this will simply
     * throw {@link UnsupportedOperationException}
     */
    @Override
    public int writeBinary(Base64Variant b64variant, InputStream data, int dataLength) {
        return _reportUnsupportedOperation();
    }

    /*
    /**********************************************************************
    /* JsonGenerator implementation: native ids
    /**********************************************************************
     */

    @Override
    public boolean canWriteTypeId() {
        return false;
    }

    @Override
    public boolean canWriteObjectId() {
        return false;
    }

    @Override
    public void writeTypeId(Object id) {
        _reportUnsupportedOperation();
    }

    @Override
    public void writeObjectId(Object id) {
        _reportUnsupportedOperation();
    }

    @Override
    public void writeEmbeddedObject(Object object) {
        // 02-Mar-2021, tatu: Bit tricky, this one; should we try to
        //   auto-detect types or something?
        _tokenWriteContext.writePOJO(object);
    }

    /*
    /**********************************************************************
    /* JsonGenerator implementation; pass-through copy
    /**********************************************************************
     */

// fine as-is:
//    public void copyCurrentEvent(JsonParser p)
//    public void copyCurrentStructure(JsonParser p)

    /*
    /**********************************************************************
    /* Helper classes
    /**********************************************************************
     */

    static abstract class TreeWriteContext extends TokenStreamContext
    {
        protected final TreeWriteContext _parent;
        protected final JsonNodeFactory _nodeFactory;
        protected Object _currentValue;

        protected TreeWriteContext(int type, TreeWriteContext parent,
                JsonNodeFactory nf, Object currValue)
        {
            super();
            _type = type;
            _parent = parent;
            _nodeFactory = nf;
            _currentValue = currValue;
        }

        /*
        /**********************************************************************
        /* Accessors
        /**********************************************************************
         */

        @Override
        public Object currentValue() {
            return _currentValue;
        }

        @Override
        public void assignCurrentValue(Object v) {
            _currentValue = v;
        }

        @Override
        public final TreeWriteContext getParent() { return _parent; }

        @Override
        public String currentName() { return null; }

        /*
        /**********************************************************************
        /* Write methods
        /**********************************************************************
         */

        public abstract TreeWriteContext createChildArrayContext(Object currValue);

        public abstract TreeWriteContext createChildObjectContext(Object currValue);

        public boolean writeName(String name) {
            // To be overridden by Object context
            return false;
        }

        public abstract void writeBinary(byte[] data);
        public abstract void writeBoolean(boolean v);
        public abstract void writeNull();
        public abstract void writeNumber(ValueNode v);
        public abstract void writeString(String value);

        public abstract void writeNode(JsonNode node);
        public abstract void writePOJO(Object value);
    }

    static final class RootContext extends TreeWriteContext
    {
        protected JsonNode _node;

        public RootContext(JsonNodeFactory nf)
        {
            super(TYPE_ROOT, null, nf, null);
        }

        @Override
        public final TreeWriteContext createChildArrayContext(Object currValue)
        {
            ArrayContext child = new ArrayContext(this, _nodeFactory, currValue);
            _node = child._node;
            return child;
        }

        @Override
        public TreeWriteContext createChildObjectContext(Object currValue)
        {
            ObjectContext child = new ObjectContext(this, _nodeFactory, currValue);
            _node = child._node;
            return child;
        }

        @Override
        public void writeBinary(byte[] data) { _node = _nodeFactory.binaryNode(data); }
        @Override
        public void writeBoolean(boolean v) { _node = _nodeFactory.booleanNode(v); }
        @Override
        public void writeNull() { _node = _nodeFactory.nullNode(); }
        @Override
        public void writeNumber(ValueNode v) { _node = v; }
        @Override
        public void writeString(String v) { _node = _nodeFactory.textNode(v); }

        @Override
        public void writePOJO(Object value) { _node = _nodeFactory.pojoNode(value); }
        @Override
        public void writeNode(JsonNode node) { _node = node; }
    }

    static final class ArrayContext extends TreeWriteContext
    {
        protected final ArrayNode _node;

        protected ArrayContext(TreeWriteContext parent,
                JsonNodeFactory nf, Object currValue)
        {
            super(TYPE_ARRAY, parent, nf, currValue);
            _node = nf.arrayNode();
        }

        @Override
        public final TreeWriteContext createChildArrayContext(Object currValue)
        {
            ArrayContext child = new ArrayContext(this, _nodeFactory, currValue);
            _node.add(child._node);
            return child;
        }

        @Override
        public TreeWriteContext createChildObjectContext(Object currValue)
        {
            ObjectContext child = new ObjectContext(this, _nodeFactory, currValue);
            _node.add(child._node);
            return child;
        }

        @Override
        public void writeBinary(byte[] v) { _node.add(v); }
        @Override
        public void writeBoolean(boolean v) { _node.add(v); }
        @Override
        public void writeNull() { _node.addNull(); }
        @Override
        public void writeNumber(ValueNode v) { _node.add(v); }
        @Override
        public void writeString(String v) { _node.add(v); }

        @Override
        public void writePOJO(Object value) { _node.addPOJO(value); }
        @Override
        public void writeNode(JsonNode node) { _node.add(node); }
    }

    static final class ObjectContext extends TreeWriteContext
    {
        protected final ObjectNode _node;

        protected String _currentName;

        protected boolean _expectValue = false;

        protected ObjectContext(TreeWriteContext parent,
                JsonNodeFactory nf, Object currValue)
        {
            super(TYPE_OBJECT, parent, nf, currValue);
            _node = nf.objectNode();
        }

        @Override
        public final String currentName() { return _currentName; }

        @Override
        public final TreeWriteContext createChildArrayContext(Object currValue)
        {
            _verifyValueWrite();
            ArrayContext child = new ArrayContext(this, _nodeFactory, currValue);
            _node.set(_currentName, child._node);
            return child;
        }

        @Override
        public TreeWriteContext createChildObjectContext(Object currValue)
        {
            _verifyValueWrite();
            ObjectContext child = new ObjectContext(this, _nodeFactory, currValue);
            _node.set(_currentName, child._node);
            return child;
        }

        @Override
        public boolean writeName(String name) {
            _currentName = name;
            _expectValue = true;
            return true;
        }

        @Override
        public void writeBinary(byte[] data) {
            _verifyValueWrite();
            _node.put(_currentName, data);
        }

        @Override
        public void writeBoolean(boolean v) {
            _verifyValueWrite();
            _node.put(_currentName, v);
        }

        @Override
        public void writeNull() {
            _verifyValueWrite();
            _node.putNull(_currentName);
        }

        @Override
        public void writeNumber(ValueNode v) {
            _verifyValueWrite();
            _node.set(_currentName, v);
        }

        @Override
        public void writeString(String v) {
            _verifyValueWrite();
            _node.put(_currentName, v);
        }

        @Override
        public void writePOJO(Object v) {
            _verifyValueWrite();
            _node.putPOJO(_currentName, v);
        }

        @Override
        public void writeNode(JsonNode node) {
            _verifyValueWrite();
            _node.set(_currentName, node);
        }

        protected void _verifyValueWrite() {
            if (!_expectValue) {
                throw new IllegalStateException("Expecting FIELD_NAME, not value");
            }
            _expectValue = false;
        }
    }
}
