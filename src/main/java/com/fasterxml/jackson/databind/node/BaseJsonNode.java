package com.fasterxml.jackson.databind.node;

import java.io.IOException;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.JsonSerializable;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;

/**
 * Abstract base class common to all standard {@link JsonNode}
 * implementations.
 * The main addition here is that we declare that sub-classes must
 * implement {@link JsonSerializable}.
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
    private static final long serialVersionUID = 1L;

    // Simplest way is by using a helper
    Object writeReplace() {
        return NodeSerialization.from(this);
    }

    protected BaseJsonNode() { }

    /*
    /**********************************************************
    /* Defaulting for introspection
    /**********************************************************
     */
    
    @Override
    public boolean isMissingNode() { return false; }

    @Override
    public boolean isEmbeddedValue() { return false; }

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

    // Also, force (re)definition
    @Override public abstract int hashCode();

    /*
    /**********************************************************
    /* Support for traversal-as-stream
    /**********************************************************
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
    /**********************************************************
    /* JsonSerializable
    /**********************************************************
     */

    /**
     * Method called to serialize node instances using given generator.
     */
    @Override
    public abstract void serialize(JsonGenerator jgen, SerializerProvider provider) throws IOException;

    /**
     * Type information is needed, even if JsonNode instances are "plain" JSON,
     * since they may be mixed with other types.
     */
    @Override
    public abstract void serializeWithType(JsonGenerator jgen, SerializerProvider provider,
            TypeSerializer typeSer)  throws IOException;

    /*
   /**********************************************************
   /* Std method overrides
   /**********************************************************
    */

   @Override
   public String toString() {
       try {
           return JsonMapper.shared().writeValueAsString(this);
       } catch (IOException e) { // should never occur
           throw new RuntimeException(e);
       }
   }

   @Override
   public String toPrettyString() {
       try {
           return JsonMapper.shared()
                   .writerWithDefaultPrettyPrinter()
                   .writeValueAsString(this);
       } catch (IOException e) { // should never occur
           throw new RuntimeException(e);
       }
   }
}
