package com.fasterxml.jackson.databind.node;

import java.io.IOException;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.JsonSerializable;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;

/**
 * Abstract base class common to all standard {@link JsonNode}
 * implementations.
 * The main addition here is that we declare that sub-classes must
 * implement {@link JsonSerializable}.
 * This simplifies object mapping aspects a bit, as no external serializers are needed.
 *<p>
 * Since 2.10, all implements have been {@link java.io.Serializable}.
 */
public abstract class BaseJsonNode
    extends JsonNode
    implements java.io.Serializable
{
    private static final long serialVersionUID = 1L;

    // Simplest way is by using a helper
    Object writeReplace() {
        return NodeSerialization.from(this);
    }

    protected BaseJsonNode() { }

    /*
    /**********************************************************
    /* Basic definitions for non-container types
    /**********************************************************
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

    // Also, force (re)definition (2.7)
    @Override public abstract int hashCode();

    /*
    /**********************************************************************
    /* Improved required-ness checks for standard JsonNode implementations
    /**********************************************************************
     */

    @Override
    public JsonNode required(String fieldName) {
        return _reportRequiredViolation("Node of type `%s` has no fields",
                getClass().getSimpleName());
    }

    @Override
    public JsonNode required(int index) {
        return _reportRequiredViolation("Node of type `%s` has no indexed values",
                getClass().getSimpleName());
    }

    /*
    /**********************************************************
    /* Support for traversal-as-stream
    /**********************************************************
     */

    @Override
    public JsonParser traverse() {
        return new TreeTraversingParser(this);
    }

    @Override
    public JsonParser traverse(ObjectCodec codec) {
        return new TreeTraversingParser(this, codec);
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
    /**********************************************************
    /* Other traversal
    /**********************************************************
     */

    @Override
    public ObjectNode withObject(JsonPointer ptr,
            OverwriteMode overwriteMode, boolean preferIndex) {
        if (!isObject()) {
            // To avoid abstract method, base implementation just fails
            _reportWrongNodeType("Can only call `withObject()` on `ObjectNode`, not `%s`",
                getClass().getName());
        }
        return _withObject(ptr, ptr, overwriteMode, preferIndex);
    }

    protected ObjectNode _withObject(JsonPointer origPtr,
            JsonPointer currentPtr,
            OverwriteMode overwriteMode, boolean preferIndex)
    {
        if (currentPtr.matches()) {
            if (this instanceof ObjectNode) {
                return (ObjectNode) this;
            }
            return _reportWrongNodeType(
                    "`JsonNode` matching `JsonPointer` \"%s\" must be `ObjectNode`, not `%s`",
                    origPtr.toString(),
                    getClass().getName());
        }
        JsonNode n = _at(currentPtr);
        // If there's a path, follow it
        if ((n != null) && (n instanceof BaseJsonNode)) {
            return ((BaseJsonNode) n)._withObject(origPtr, currentPtr.tail(),
                    overwriteMode, preferIndex);
        }
        return _withObjectCreatePath(origPtr, currentPtr, overwriteMode, preferIndex);
    }

    /**
     * Helper method for constructing specified path under this node, if possible;
     * or throwing an exception if not. If construction successful, needs to return
     * the innermost {@code ObjectNode} constructed.
     */
    protected ObjectNode _withObjectCreatePath(JsonPointer origPtr,
            JsonPointer currentPtr,
            OverwriteMode overwriteMode, boolean preferIndex)
    {
        // Cannot traverse non-container nodes:
        return _reportWrongNodeType(
                "`JsonPointer` path \"%s\" cannot traverse non-container node of type `%s`",
                origPtr.toString(),
                getClass().getName());
    }

    /*
    /**********************************************************
    /* JsonSerializable
    /**********************************************************
     */

    /**
     * Method called to serialize node instances using given generator.
     */
    @Override
    public abstract void serialize(JsonGenerator g, SerializerProvider ctxt)
        throws IOException;

    /**
     * Type information is needed, even if JsonNode instances are "plain" JSON,
     * since they may be mixed with other types.
     */
    @Override
    public abstract void serializeWithType(JsonGenerator g, SerializerProvider ctxt,
            TypeSerializer typeSer)
        throws IOException;

   /*
   /**********************************************************
   /* Standard method overrides
   /**********************************************************
    */

   @Override
   public String toString() {
       return InternalNodeMapper.nodeToString(this);
   }

   @Override
   public String toPrettyString() {
       return InternalNodeMapper.nodeToPrettyString(this);
   }

   /*
   /**********************************************************
   /* Other helper methods for subtypes
   /**********************************************************
    */

   /**
    * Helper method that throws {@link UnsupportedOperationException} as a result of
    * this node being of wrong type
    */
   protected <T> T _reportWrongNodeType(String msgTemplate, Object...args) {
       throw new UnsupportedOperationException(String.format(msgTemplate, args));
   }
}
