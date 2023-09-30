package tools.jackson.databind.node;

import java.math.BigDecimal;
import java.math.BigInteger;

import tools.jackson.core.*;

import tools.jackson.databind.JacksonSerializable;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.SerializerProvider;
import tools.jackson.databind.exc.JsonNodeException;
import tools.jackson.databind.jsontype.TypeSerializer;
import tools.jackson.databind.util.ClassUtil;

/**
 * Abstract base class common to all standard {@link JsonNode}
 * implementations.
 * The main addition here is that we declare that sub-classes must
 * implement {@link JacksonSerializable}.
 * This simplifies object mapping aspects a bit, as no external serializers are needed.
 *<p>
 * Note that support for {@link java.io.Serializable} is added here and so all subtypes
 * are fully JDK serializable: but also note that serialization is as JSON and should
 * only be used for interoperability purposes where other approaches are not available.
 */
public abstract class BaseJsonNode
    extends JsonNode
    implements java.io.Serializable
{
    private static final long serialVersionUID = 3L;

    // Simplest way is by using a helper
    Object writeReplace() {
        return NodeSerialization.from(this);
    }

    protected BaseJsonNode() { }

    /*
    /**********************************************************************
    /* Defaulting for introspection
    /**********************************************************************
     */

    @Override
    public boolean isMissingNode() { return false; }

    @Override
    public boolean isEmbeddedValue() { return false; }

    /*
    /**********************************************************************
    /* Basic definitions for non-container types
    /**********************************************************************
     */

    @Override
    public final JsonNode findPath(String fieldName)
    {
        JsonNode value = findValue(fieldName);
        if (value == null) {
            return MissingNode.getInstance();
        }
        return value;
    }

    // Also, force (re)definition
    @Override public abstract int hashCode();

    /*
    /**********************************************************************
    /* Improved required-ness checks for standard JsonNode implementations
    /**********************************************************************
     */

    @Override
    public JsonNode required(String fieldName) {
        return _reportRequiredViolation("Node of type %s has no fields",
                ClassUtil.nameOf(getClass()));
    }

    @Override
    public JsonNode required(int index) {
        return _reportRequiredViolation("Node of type %s has no indexed values",
                ClassUtil.nameOf(getClass()));
    }

    /*
    /**********************************************************************
    /* Support for traversal-as-stream
    /**********************************************************************
     */

    @Override
    public JsonParser traverse(ObjectReadContext readCtxt) {
        return new TreeTraversingParser(this, readCtxt);
    }

    /**
     * Method that can be used for efficient type detection
     * when using stream abstraction for traversing nodes.
     * Will return the first {@link JsonToken} that equivalent
     * stream event would produce (for most nodes there is just
     * one token but for structured/container types multiple)
     */
    @Override
    public abstract JsonToken asToken();

    /**
     * Returns code that identifies type of underlying numeric
     * value, if (and only if) node is a number node.
     */
    @Override
    public JsonParser.NumberType numberType() {
        // most types non-numeric, so:
        return null;
    }

    /*
    /**********************************************************************
    /* With-Traversal
    /**********************************************************************
     */

    @Override
    public ObjectNode withObject(JsonPointer ptr,
            OverwriteMode overwriteMode, boolean preferIndex)
    {
        // Degenerate case of using with "empty" path; ok if ObjectNode
        if (ptr.matches()) {
            if (this instanceof ObjectNode) {
                return (ObjectNode) this;
            }
            _reportWrongNodeType("Can only call `withObject()` with empty JSON Pointer on `ObjectNode`, not %s",
                    ClassUtil.nameOf(getClass()));
        }
        // Otherwise check recursively
        ObjectNode n = _withObject(ptr, ptr, overwriteMode, preferIndex);
        if (n == null) {
            _reportWrongNodeType("Cannot replace context node (of type %s) using `withObject()` with  JSON Pointer '%s'",
                    ClassUtil.nameOf(getClass()), ptr);
        }
        return n;
    }

    protected ObjectNode _withObject(JsonPointer origPtr,
            JsonPointer currentPtr,
            OverwriteMode overwriteMode, boolean preferIndex)
    {
        // Three-part logic:
        //
        // 1) If we are at the end of JSON Pointer; if so, return
        //    `this` if Object node, `null` if not (for caller to handle)
        // 2) If not at the end, if we can follow next segment, call recursively
        //    handle non-null (existing Object node, return)
        //    vs `null` (must replace; may not be allowed to)
        // 3) Can not follow the segment? Try constructing, adding path
        //
        // But the default implementation assumes non-container behavior so
        // it'll simply return `null`
        return null;
    }

    protected void _withXxxVerifyReplace(JsonPointer origPtr,
            JsonPointer currentPtr,
            OverwriteMode overwriteMode, boolean preferIndex,
            JsonNode toReplace)
    {
        if (!_withXxxMayReplace(toReplace, overwriteMode)) {
            _reportWrongNodeType(
"Cannot replace `JsonNode` of type %s for property \"%s\" in JSON Pointer \"%s\" (mode `OverwriteMode.%s`)",
                ClassUtil.nameOf(toReplace.getClass()), currentPtr.getMatchingProperty(),
                origPtr, overwriteMode);
        }
    }

    protected boolean _withXxxMayReplace(JsonNode node, OverwriteMode overwriteMode) {
        switch (overwriteMode) {
        case NONE:
            return false;
        case NULLS:
            return node.isNull();
        case SCALARS:
            return !node.isContainerNode();
        default:
        case ALL:
            return true;
        }
    }

    @Override
    public ArrayNode withArray(JsonPointer ptr,
            OverwriteMode overwriteMode, boolean preferIndex)
    {
        // Degenerate case of using with "empty" path; ok if ArrayNode
        if (ptr.matches()) {
            if (this instanceof ArrayNode) {
                return (ArrayNode) this;
            }
            _reportWrongNodeType("Can only call `withArray()` with empty JSON Pointer on `ArrayNode`, not %s",
                    ClassUtil.nameOf(getClass()));
        }
        // Otherwise check recursively
        ArrayNode n = _withArray(ptr, ptr, overwriteMode, preferIndex);
        if (n == null) {
            _reportWrongNodeType("Cannot replace context node (of type %s) using `withArray()` with  JSON Pointer '%s'",
                    ClassUtil.nameOf(getClass()), ptr);
        }
        return n;
    }

    protected ArrayNode _withArray(JsonPointer origPtr,
            JsonPointer currentPtr,
            OverwriteMode overwriteMode, boolean preferIndex)
    {
        // Similar logic to "_withObject()" but the default implementation
        // used for non-container behavior so it'll simply return `null`
        return null;
    }

    /*
    /**********************************************************************
    /* JacksonSerializable
    /**********************************************************************
     */

    /**
     * Method called to serialize node instances using given generator.
     */
    @Override
    public abstract void serialize(JsonGenerator jgen, SerializerProvider ctxt)
        throws JacksonException;

    /**
     * Type information is needed, even if JsonNode instances are "plain" JSON,
     * since they may be mixed with other types.
     */
    @Override
    public abstract void serializeWithType(JsonGenerator jgen, SerializerProvider ctxt,
            TypeSerializer typeSer)
        throws JacksonException;

    /*
    /**********************************************************************
    /* Standard method overrides
    /**********************************************************************
     */

    @Override
    public String toString() {
        return InternalNodeSerializer.toString(this);
    }

    @Override
    public String toPrettyString() {
        return InternalNodeSerializer.toPrettyString(this);
    }

    /*
    /**********************************************************************
    /* Other helper methods for subtypes
    /**********************************************************************
     */

    /**
     * Helper method that throws {@link JsonNodeException} as a result of
     * this node being of wrong type
     */
    protected <T> T _reportWrongNodeType(String msgTemplate, Object...args) {
        throw JsonNodeException.from(this, String.format(msgTemplate, args));
    }

    protected BigInteger _bigIntFromBigDec(BigDecimal value) {
        StreamReadConstraints.defaults().validateBigIntegerScale(value.scale());
        return value.toBigInteger();
    }
}
