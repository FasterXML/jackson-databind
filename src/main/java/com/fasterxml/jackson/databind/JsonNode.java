package com.fasterxml.jackson.databind;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import com.fasterxml.jackson.databind.node.MissingNode;
import com.fasterxml.jackson.databind.util.ClassUtil;

/**
 * Base class for all JSON nodes, which form the basis of JSON
 * Tree Model that Jackson implements.
 * One way to think of these nodes is to consider them
 * similar to DOM nodes in XML DOM trees.
 *<p>
 * As a general design rule, most accessors ("getters") are included
 * in this base class, to allow for traversing structure without
 * type casts. Most mutators, however, need to be accessed through
 * specific sub-classes (such as <code>ObjectNode</code>
 * and <code>ArrayNode</code>).
 * This seems sensible because proper type
 * information is generally available when building or modifying
 * trees, but less often when reading a tree (newly built from
 * parsed JSON content).
 *<p>
 * Actual concrete sub-classes can be found from package
 * {@link com.fasterxml.jackson.databind.node}.
 *<p>
 * Note that it is possible to "read" from nodes, using
 * method {@link TreeNode#traverse(ObjectCodec)}, which will result in
 * a {@link JsonParser} being constructed. This can be used for (relatively)
 * efficient conversations between different representations; and it is what
 * core databind uses for methods like {@link ObjectMapper#treeToValue(TreeNode, Class)}
 * and {@link ObjectMapper#treeAsTokens(TreeNode)}
 */
public abstract class JsonNode
    extends JsonSerializable.Base // i.e. implements JsonSerializable
    implements TreeNode, Iterable<JsonNode>
{
    /*
    /**********************************************************
    /* Construction, related
    /**********************************************************
     */
    
    protected JsonNode() { }

    /**
     * Method that can be called to get a node that is guaranteed
     * not to allow changing of this node through mutators on
     * this node or any of its children.
     * This means it can either make a copy of this node (and all
     * mutable children and grand children nodes), or node itself
     * if it is immutable.
     *<p>
     * Note: return type is guaranteed to have same type as the
     * node method is called on; which is why method is declared
     * with local generic type.
     * 
     * @since 2.0
     * 
     * @return Node that is either a copy of this node (and all non-leaf
     *    children); or, for immutable leaf nodes, node itself.
     */
    public abstract <T extends JsonNode> T deepCopy();

    /*
    /**********************************************************
    /* TreeNode implementation
    /**********************************************************
     */

//  public abstract JsonToken asToken();
//  public abstract JsonToken traverse();
//  public abstract JsonToken traverse(ObjectCodec codec);
//  public abstract JsonParser.NumberType numberType();

    @Override
    public int size() { return 0; }

    @Override
    public final boolean isValueNode()
    {
        switch (getNodeType()) {
            case ARRAY: case OBJECT: case MISSING:
                return false;
            default:
                return true;
        }
    }

    @Override
    public final boolean isContainerNode() {
        final JsonNodeType type = getNodeType();
        return type == JsonNodeType.OBJECT || type == JsonNodeType.ARRAY;
    }

    @Override
    public boolean isMissingNode() {
        return false;
    }

    @Override
    public boolean isArray() {
        return false;
    }

    @Override
    public boolean isObject() {
        return false;
    }

    /**
     * Method for accessing value of the specified element of
     * an array node. For other nodes, null is always returned.
     *<p>
     * For array nodes, index specifies
     * exact location within array and allows for efficient iteration
     * over child elements (underlying storage is guaranteed to
     * be efficiently indexable, i.e. has random-access to elements).
     * If index is less than 0, or equal-or-greater than
     * <code>node.size()</code>, null is returned; no exception is
     * thrown for any index.
     *<p>
     * NOTE: if the element value has been explicitly set as <code>null</code>
     * (which is different from removal!),
     * a {@link com.fasterxml.jackson.databind.node.NullNode} will be returned,
     * not null.
     *
     * @return Node that represent value of the specified element,
     *   if this node is an array and has specified element.
     *   Null otherwise.
     */
    @Override
    public abstract JsonNode get(int index);

    /**
     * Method for accessing value of the specified field of
     * an object node. If this node is not an object (or it
     * does not have a value for specified field name), or
     * if there is no field with such name, null is returned.
     *<p>
     * NOTE: if the property value has been explicitly set as <code>null</code>
     * (which is different from removal!),
     * a {@link com.fasterxml.jackson.databind.node.NullNode} will be returned,
     * not null.
     *
     * @return Node that represent value of the specified field,
     *   if this node is an object and has value for the specified
     *   field. Null otherwise.
     */
    @Override
    public JsonNode get(String fieldName) { return null; }
    /**
     * This method is similar to {@link #get(String)}, except
     * that instead of returning null if no such value exists (due
     * to this node not being an object, or object not having value
     * for the specified field),
     * a "missing node" (node that returns true for
     * {@link #isMissingNode}) will be returned. This allows for
     * convenient and safe chained access via path calls.
     */

    @Override
    public abstract JsonNode path(String fieldName);

    /**
     * This method is similar to {@link #get(int)}, except
     * that instead of returning null if no such element exists (due
     * to index being out of range, or this node not being an array),
     * a "missing node" (node that returns true for
     * {@link #isMissingNode}) will be returned. This allows for
     * convenient and safe chained access via path calls.
     */
    @Override
    public abstract JsonNode path(int index);

    @Override
    public Iterator<String> fieldNames() {
        return ClassUtil.emptyIterator();
    }

    /**
     * Method for locating node specified by given JSON pointer instances.
     * Method will never return null; if no matching node exists, 
     *   will return a node for which {@link #isMissingNode()} returns true.
     * 
     * @return Node that matches given JSON Pointer: if no match exists,
     *   will return a node for which {@link #isMissingNode()} returns true.
     * 
     * @since 2.3
     */
    @Override
    public final JsonNode at(JsonPointer ptr)
    {
        // Basically: value nodes only match if we have "empty" path left
        if (ptr.matches()) {
            return this;
        }
        JsonNode n = _at(ptr);
        if (n == null) {
            return MissingNode.getInstance();
        }
        return n.at(ptr.tail());
    }

    /**
     * Convenience method that is functionally equivalent to:
     *<pre>
     *   return at(JsonPointer.valueOf(jsonPointerExpression));
     *</pre>
     *<p>
     * Note that if the same expression is used often, it is preferable to construct
     * {@link JsonPointer} instance once and reuse it: this method will not perform
     * any caching of compiled expressions.
     * 
     * @param jsonPtrExpr Expression to compile as a {@link JsonPointer}
     *   instance
     * 
     * @return Node that matches given JSON Pointer: if no match exists,
     *   will return a node for which {@link TreeNode#isMissingNode()} returns true.
     * 
     * @since 2.3
     */
    @Override
    public final JsonNode at(String jsonPtrExpr) {
        return at(JsonPointer.compile(jsonPtrExpr));
    }

    protected abstract JsonNode _at(JsonPointer ptr);

    /*
    /**********************************************************
    /* Public API, type introspection
    /**********************************************************
     */

    // // First high-level division between values, containers and "missing"

    /**
     * Return the type of this node
     *
     * @return the node type as a {@link JsonNodeType} enum value
     *
     * @since 2.2
     */
    public abstract JsonNodeType getNodeType();

    /**
     * Method that can be used to check if the node is a wrapper
     * for a POJO ("Plain Old Java Object" aka "bean".
     * Returns true only for
     * instances of <code>POJONode</code>.
     *
     * @return True if this node wraps a POJO
     */
    public final boolean isPojo() {
        return getNodeType() == JsonNodeType.POJO;
    }

    /**
     * @return True if this node represents a numeric JSON value
     */
    public final boolean isNumber() {
        return getNodeType() == JsonNodeType.NUMBER;
    }

    /**
     * 
     * @return True if this node represents an integral (integer)
     *   numeric JSON value
     */
    public boolean isIntegralNumber() { return false; }

    /**
     * @return True if this node represents a non-integral
     *   numeric JSON value
     */
    public boolean isFloatingPointNumber() { return false; }

    /**
     * Method that can be used to check whether contained value
     * is a number represented as Java <code>short</code>.
     * Note, however, that even if this method returns false, it
     * is possible that conversion would be possible from other numeric
     * types -- to check if this is possible, use
     * {@link #canConvertToInt()} instead.
     * 
     * @return True if the value contained by this node is stored as Java short
     */
    public boolean isShort() { return false; }

    /**
     * Method that can be used to check whether contained value
     * is a number represented as Java <code>int</code>.
     * Note, however, that even if this method returns false, it
     * is possible that conversion would be possible from other numeric
     * types -- to check if this is possible, use
     * {@link #canConvertToInt()} instead.
     * 
     * @return True if the value contained by this node is stored as Java int
     */
    public boolean isInt() { return false; }

    /**
     * Method that can be used to check whether contained value
     * is a number represented as Java <code>long</code>.
     * Note, however, that even if this method returns false, it
     * is possible that conversion would be possible from other numeric
     * types -- to check if this is possible, use
     * {@link #canConvertToInt()} instead.
     * 
     * @return True if the value contained by this node is stored as Java <code>long</code>
     */
    public boolean isLong() { return false; }

    /**
     * @since 2.2
     */
    public boolean isFloat() { return false; }

    public boolean isDouble() { return false; }
    public boolean isBigDecimal() { return false; }
    public boolean isBigInteger() { return false; }

    /**
     * Method that checks whether this node represents basic JSON String
     * value.
     */
    public final boolean isTextual() {
        return getNodeType() == JsonNodeType.STRING;
    }

    /**
     * Method that can be used to check if this node was created from
     * JSON boolean value (literals "true" and "false").
     */
    public final boolean isBoolean() {
        return getNodeType() == JsonNodeType.BOOLEAN;
    }

    /**
     * Method that can be used to check if this node was created from
     * JSON literal null value.
     */
    public final boolean isNull() {
        return getNodeType() == JsonNodeType.NULL;
    }

    /**
     * Method that can be used to check if this node represents
     * binary data (Base64 encoded). Although this will be externally
     * written as JSON String value, {@link #isTextual} will
     * return false if this method returns true.
     *
     * @return True if this node represents base64 encoded binary data
     */
    public final boolean isBinary() {
        return getNodeType() == JsonNodeType.BINARY;
    }

    /**
     * Method that can be used to check whether this node is a numeric
     * node ({@link #isNumber} would return true) AND its value fits
     * within Java's 32-bit signed integer type, <code>int</code>.
     * Note that floating-point numbers are convertible if the integral
     * part fits without overflow (as per standard Java coercion rules)
     *<p>
     * NOTE: this method does not consider possible value type conversion
     * from JSON String into Number; so even if this method returns false,
     * it is possible that {@link #asInt} could still succeed
     * if node is a JSON String representing integral number, or boolean.
     * 
     * @since 2.0
     */
    public boolean canConvertToInt() { return false; }

    /**
     * Method that can be used to check whether this node is a numeric
     * node ({@link #isNumber} would return true) AND its value fits
     * within Java's 64-bit signed integer type, <code>long</code>.
     * Note that floating-point numbers are convertible if the integral
     * part fits without overflow (as per standard Java coercion rules)
     *<p>
     * NOTE: this method does not consider possible value type conversion
     * from JSON String into Number; so even if this method returns false,
     * it is possible that {@link #asLong} could still succeed
     * if node is a JSON String representing integral number, or boolean.
     * 
     * @since 2.0
     */
    public boolean canConvertToLong() { return false; }
    
    /*
    /**********************************************************
    /* Public API, straight value access
    /**********************************************************
     */

    /**
     * Method to use for accessing String values.
     * Does <b>NOT</b> do any conversions for non-String value nodes;
     * for non-String values (ones for which {@link #isTextual} returns
     * false) null will be returned.
     * For String values, null is never returned (but empty Strings may be)
     *
     * @return Textual value this node contains, iff it is a textual
     *   JSON node (comes from JSON String value entry)
     */
    public String textValue() { return null; }

    /**
     * Method to use for accessing binary content of binary nodes (nodes
     * for which {@link #isBinary} returns true); or for Text Nodes
     * (ones for which {@link #textValue} returns non-null value),
     * to read decoded base64 data.
     * For other types of nodes, returns null.
     *
     * @return Binary data this node contains, iff it is a binary
     *   node; null otherwise
     */
    public byte[] binaryValue() throws IOException {
        return null;
    }

    /**
     * Method to use for accessing JSON boolean values (value
     * literals 'true' and 'false').
     * For other types, always returns false.
     *
     * @return Textual value this node contains, iff it is a textual
     *   json node (comes from JSON String value entry)
     */
    public boolean booleanValue() { return false; }

    /**
     * Returns numeric value for this node, <b>if and only if</b>
     * this node is numeric ({@link #isNumber} returns true); otherwise
     * returns null
     *
     * @return Number value this node contains, if any (null for non-number
     *   nodes).
     */
    public Number numberValue() { return null; }

    /**
     * Returns 16-bit short value for this node, <b>if and only if</b>
     * this node is numeric ({@link #isNumber} returns true). For other
     * types returns 0.
     * For floating-point numbers, value is truncated using default
     * Java coercion, similar to how cast from double to short operates.
     *
     * @return Short value this node contains, if any; 0 for non-number
     *   nodes.
     */
    public short shortValue() { return 0; }

    /**
     * Returns integer value for this node, <b>if and only if</b>
     * this node is numeric ({@link #isNumber} returns true). For other
     * types returns 0.
     * For floating-point numbers, value is truncated using default
     * Java coercion, similar to how cast from double to int operates.
     *
     * @return Integer value this node contains, if any; 0 for non-number
     *   nodes.
     */
    public int intValue() { return 0; }

    /**
     * Returns 64-bit long value for this node, <b>if and only if</b>
     * this node is numeric ({@link #isNumber} returns true). For other
     * types returns 0.
     * For floating-point numbers, value is truncated using default
     * Java coercion, similar to how cast from double to long operates.
     *
     * @return Long value this node contains, if any; 0 for non-number
     *   nodes.
     */
    public long longValue() { return 0L; }
    
    /**
     * Returns 32-bit floating value for this node, <b>if and only if</b>
     * this node is numeric ({@link #isNumber} returns true). For other
     * types returns 0.0.
     * For integer values, conversion is done using coercion; this means
     * that an overflow is possible for `long` values
     *
     * @return 32-bit float value this node contains, if any; 0.0 for non-number nodes.
     *
     * @since 2.2
     */
    public float floatValue() { return 0.0f; }

    /**
     * Returns 64-bit floating point (double) value for this node, <b>if and only if</b>
     * this node is numeric ({@link #isNumber} returns true). For other
     * types returns 0.0.
     * For integer values, conversion is done using coercion; this may result
     * in overflows with {@link BigInteger} values.
     *
     * @return 64-bit double value this node contains, if any; 0.0 for non-number nodes.
     *
     * @since 2.2
     */
    public double doubleValue() { return 0.0; }

    /**
     * Returns floating point value for this node (as {@link BigDecimal}), <b>if and only if</b>
     * this node is numeric ({@link #isNumber} returns true). For other
     * types returns <code>BigDecimal.ZERO</code>.
     *
     * @return {@link BigDecimal} value this node contains, if numeric node; <code>BigDecimal.ZERO</code> for non-number nodes.
     */
    public BigDecimal decimalValue() { return BigDecimal.ZERO; }

    /**
     * Returns integer value for this node (as {@link BigDecimal}), <b>if and only if</b>
     * this node is numeric ({@link #isNumber} returns true). For other
     * types returns <code>BigInteger.ZERO</code>.
     *
     * @return {@link BigInteger} value this node contains, if numeric node; <code>BigInteger.ZERO</code> for non-number nodes.
     */
    public BigInteger bigIntegerValue() { return BigInteger.ZERO; }

    /*
    /**********************************************************
    /* Public API, value access with conversion(s)/coercion(s)
    /**********************************************************
     */

    /**
     * Method that will return a valid String representation of
     * the container value, if the node is a value node
     * (method {@link #isValueNode} returns true),
     * otherwise empty String.
     */
    public abstract String asText();

    /**
     * Method similar to {@link #asText()}, except that it will return
     * <code>defaultValue</code> in cases where null value would be returned;
     * either for missing nodes (trying to access missing property, or element
     * at invalid item for array) or explicit nulls.
     * 
     * @since 2.4
     */
    public String asText(String defaultValue) {
        String str = asText();
        return (str == null) ? defaultValue : str;
    }
    
    /**
     * Method that will try to convert value of this node to a Java <b>int</b>.
     * Numbers are coerced using default Java rules; booleans convert to 0 (false)
     * and 1 (true), and Strings are parsed using default Java language integer
     * parsing rules.
     *<p>
     * If representation cannot be converted to an int (including structured types
     * like Objects and Arrays),
     * default value of <b>0</b> will be returned; no exceptions are thrown.
     */
    public int asInt() {
        return asInt(0);
    }

    /**
     * Method that will try to convert value of this node to a Java <b>int</b>.
     * Numbers are coerced using default Java rules; booleans convert to 0 (false)
     * and 1 (true), and Strings are parsed using default Java language integer
     * parsing rules.
     *<p>
     * If representation cannot be converted to an int (including structured types
     * like Objects and Arrays),
     * specified <b>defaultValue</b> will be returned; no exceptions are thrown.
     */
    public int asInt(int defaultValue) {
        return defaultValue;
    }

    /**
     * Method that will try to convert value of this node to a Java <b>long</b>.
     * Numbers are coerced using default Java rules; booleans convert to 0 (false)
     * and 1 (true), and Strings are parsed using default Java language integer
     * parsing rules.
     *<p>
     * If representation cannot be converted to an long (including structured types
     * like Objects and Arrays),
     * default value of <b>0</b> will be returned; no exceptions are thrown.
     */
    public long asLong() {
        return asLong(0L);
    }
    
    /**
     * Method that will try to convert value of this node to a Java <b>long</b>.
     * Numbers are coerced using default Java rules; booleans convert to 0 (false)
     * and 1 (true), and Strings are parsed using default Java language integer
     * parsing rules.
     *<p>
     * If representation cannot be converted to an long (including structured types
     * like Objects and Arrays),
     * specified <b>defaultValue</b> will be returned; no exceptions are thrown.
     */
    public long asLong(long defaultValue) {
        return defaultValue;
    }
    
    /**
     * Method that will try to convert value of this node to a Java <b>double</b>.
     * Numbers are coerced using default Java rules; booleans convert to 0.0 (false)
     * and 1.0 (true), and Strings are parsed using default Java language integer
     * parsing rules.
     *<p>
     * If representation cannot be converted to an int (including structured types
     * like Objects and Arrays),
     * default value of <b>0.0</b> will be returned; no exceptions are thrown.
     */
    public double asDouble() {
        return asDouble(0.0);
    }
    
    /**
     * Method that will try to convert value of this node to a Java <b>double</b>.
     * Numbers are coerced using default Java rules; booleans convert to 0.0 (false)
     * and 1.0 (true), and Strings are parsed using default Java language integer
     * parsing rules.
     *<p>
     * If representation cannot be converted to an int (including structured types
     * like Objects and Arrays),
     * specified <b>defaultValue</b> will be returned; no exceptions are thrown.
     */
    public double asDouble(double defaultValue) {
        return defaultValue;
    }

    /**
     * Method that will try to convert value of this node to a Java <b>boolean</b>.
     * JSON booleans map naturally; integer numbers other than 0 map to true, and
     * 0 maps to false
     * and Strings 'true' and 'false' map to corresponding values.
     *<p>
     * If representation cannot be converted to a boolean value (including structured types
     * like Objects and Arrays),
     * default value of <b>false</b> will be returned; no exceptions are thrown.
     */
    public boolean asBoolean() {
        return asBoolean(false);
    }
    
    /**
     * Method that will try to convert value of this node to a Java <b>boolean</b>.
     * JSON booleans map naturally; integer numbers other than 0 map to true, and
     * 0 maps to false
     * and Strings 'true' and 'false' map to corresponding values.
     *<p>
     * If representation cannot be converted to a boolean value (including structured types
     * like Objects and Arrays),
     * specified <b>defaultValue</b> will be returned; no exceptions are thrown.
     */
    public boolean asBoolean(boolean defaultValue) {
        return defaultValue;
    }

    /*
    /**********************************************************
    /* Public API, value find / existence check methods
    /**********************************************************
     */

    /**
     * Method that allows checking whether this node is JSON Object node
     * and contains value for specified property. If this is the case
     * (including properties with explicit null values), returns true;
     * otherwise returns false.
     *<p>
     * This method is equivalent to:
     *<pre>
     *   node.get(fieldName) != null
     *</pre>
     * (since return value of get() is node, not value node contains)
     *<p>
     * NOTE: when explicit <code>null</code> values are added, this
     * method will return <code>true</code> for such properties.
     *
     * @param fieldName Name of element to check
     * 
     * @return True if this node is a JSON Object node, and has a property
     *   entry with specified name (with any value, including null value)
     */
    public boolean has(String fieldName) {
        return get(fieldName) != null;
    }

    /**
     * Method that allows checking whether this node is JSON Array node
     * and contains a value for specified index
     * If this is the case
     * (including case of specified indexing having null as value), returns true;
     * otherwise returns false.
     *<p>
     * Note: array element indexes are 0-based.
     *<p>
     * This method is equivalent to:
     *<pre>
     *   node.get(index) != null
     *</pre>
     *<p>
     * NOTE: this method will return <code>true</code> for explicitly added
     * null values.
     *
     * @param index Index to check
     * 
     * @return True if this node is a JSON Object node, and has a property
     *   entry with specified name (with any value, including null value)
     */
    public boolean has(int index) {
        return get(index) != null;
    }

    /**
     * Method that is similar to {@link #has(String)}, but that will
     * return <code>false</code> for explicitly added nulls.
     *<p>
     * This method is functionally equivalent to:
     *<pre>
     *   node.get(fieldName) != null &lt;&lt; !node.get(fieldName).isNull()
     *</pre>
     * 
     * @since 2.1
     */
    public boolean hasNonNull(String fieldName) {
        JsonNode n = get(fieldName);
        return (n != null) && !n.isNull();
    }

    /**
     * Method that is similar to {@link #has(int)}, but that will
     * return <code>false</code> for explicitly added nulls.
     *<p>
     * This method is equivalent to:
     *<pre>
     *   node.get(index) != null &lt;&lt; !node.get(index).isNull()
     *</pre>
     * 
     * @since 2.1
     */
    public boolean hasNonNull(int index) {
        JsonNode n = get(index);
        return (n != null) && !n.isNull();
    }

    /*
    /**********************************************************
    /* Public API, container access
    /**********************************************************
     */

    /**
     * Same as calling {@link #elements}; implemented so that
     * convenience "for-each" loop can be used for looping over elements
     * of JSON Array constructs.
     */
    @Override
    public final Iterator<JsonNode> iterator() { return elements(); }

    /**
     * Method for accessing all value nodes of this Node, iff
     * this node is a JSON Array or Object node. In case of Object node,
     * field names (keys) are not included, only values.
     * For other types of nodes, returns empty iterator.
     */
    public Iterator<JsonNode> elements() {
        return ClassUtil.emptyIterator();
    }

    /**
     * @return Iterator that can be used to traverse all key/value pairs for
     *   object nodes; empty iterator (no contents) for other types
     */
    public Iterator<Map.Entry<String, JsonNode>> fields() {
        return ClassUtil.emptyIterator();
    }

    /*
    /**********************************************************
    /* Public API, find methods
    /**********************************************************
     */

    /**
     * Method for finding a JSON Object field with specified name in this
     * node or its child nodes, and returning value it has.
     * If no matching field is found in this node or its descendants, returns null.
     * 
     * @param fieldName Name of field to look for
     * 
     * @return Value of first matching node found, if any; null if none
     */
    public abstract JsonNode findValue(String fieldName);

    /**
     * Method for finding JSON Object fields with specified name, and returning
     * found ones as a List. Note that sub-tree search ends if a field is found,
     * so possible children of result nodes are <b>not</b> included.
     * If no matching fields are found in this node or its descendants, returns
     * an empty List.
     * 
     * @param fieldName Name of field to look for
     */
    public final List<JsonNode> findValues(String fieldName)
    {
        List<JsonNode> result = findValues(fieldName, null);
        if (result == null) {
            return Collections.emptyList();
        }
        return result;
    }

    /**
     * Similar to {@link #findValues}, but will additionally convert
     * values into Strings, calling {@link #asText}.
     */
    public final List<String> findValuesAsText(String fieldName)
    {
        List<String> result = findValuesAsText(fieldName, null);
        if (result == null) {
            return Collections.emptyList();
        }
        return result;
    }
    
    /**
     * Method similar to {@link #findValue}, but that will return a
     * "missing node" instead of null if no field is found. Missing node
     * is a specific kind of node for which {@link #isMissingNode}
     * returns true; and all value access methods return empty or
     * missing value.
     * 
     * @param fieldName Name of field to look for
     * 
     * @return Value of first matching node found; or if not found, a
     *    "missing node" (non-null instance that has no value)
     */
    public abstract JsonNode findPath(String fieldName);
    
    /**
     * Method for finding a JSON Object that contains specified field,
     * within this node or its descendants.
     * If no matching field is found in this node or its descendants, returns null.
     * 
     * @param fieldName Name of field to look for
     * 
     * @return Value of first matching node found, if any; null if none
     */
    public abstract JsonNode findParent(String fieldName);

    /**
     * Method for finding a JSON Object that contains specified field,
     * within this node or its descendants.
     * If no matching field is found in this node or its descendants, returns null.
     * 
     * @param fieldName Name of field to look for
     * 
     * @return Value of first matching node found, if any; null if none
     */
    public final List<JsonNode> findParents(String fieldName)
    {
        List<JsonNode> result = findParents(fieldName, null);
        if (result == null) {
            return Collections.emptyList();
        }
        return result;
    }

    public abstract List<JsonNode> findValues(String fieldName, List<JsonNode> foundSoFar);
    public abstract List<String> findValuesAsText(String fieldName, List<String> foundSoFar);
    public abstract List<JsonNode> findParents(String fieldName, List<JsonNode> foundSoFar);

    /*
    /**********************************************************
    /* Public API, path handling
    /**********************************************************
     */

    /**
     * Method that can be called on Object nodes, to access a property
     * that has Object value; or if no such property exists, to create,
     * add and return such Object node.
     * If the node method is called on is not Object node,
     * or if property exists and has value that is not Object node,
     * {@link UnsupportedOperationException} is thrown
     */
    public JsonNode with(String propertyName) {
        throw new UnsupportedOperationException("JsonNode not of type ObjectNode (but "
                +getClass().getName()+"), cannot call with() on it");
    }

    /**
     * Method that can be called on Object nodes, to access a property
     * that has <code>Array</code> value; or if no such property exists, to create,
     * add and return such Array node.
     * If the node method is called on is not Object node,
     * or if property exists and has value that is not Array node,
     * {@link UnsupportedOperationException} is thrown
     */
    public JsonNode withArray(String propertyName) {
        throw new UnsupportedOperationException("JsonNode not of type ObjectNode (but "
                +getClass().getName()+"), cannot call withArray() on it");
    }

    /*
    /**********************************************************
    /* Public API, comparison
    /**********************************************************
     */

    /**
     * Entry method for invoking customizable comparison, using passed-in
     * {@link Comparator} object. Nodes will handle traversal of structured
     * types (arrays, objects), but defer to comparator for scalar value
     * comparisons. If a "natural" {@link Comparator} is passed -- one that
     * simply calls <code>equals()</code> on one of arguments, passing the other
     * -- implementation is the same as directly calling <code>equals()</code>
     * on node.
     *<p>
     * Default implementation simply delegates to passed in <code>comparator</code>,
     * with <code>this</code> as the first argument, and <code>other</code> as
     * the second argument.
     * 
     * @param comparator Object called to compare two scalar {@link JsonNode} 
     *   instances, and return either 0 (are equals) or non-zero (not equal)
     *
     * @since 2.6
     */
    public boolean equals(Comparator<JsonNode> comparator, JsonNode other) {
        return comparator.compare(this, other) == 0;
    }
    
    /*
    /**********************************************************
    /* Overridden standard methods
    /**********************************************************
     */
    
    /**
     * Method that will produce developer-readable representation of the
     * node; which may <b>or may not</b> be as valid JSON.
     * If you want valid JSON output (or output formatted using one of
     * other Jackson supported data formats) make sure to use
     * {@link ObjectMapper} or {@link ObjectWriter} to serialize an
     * instance, for example:
     *<pre>
     *   String json = objectMapper.writeValueAsString(rootNode);
     *</pre>
     *<p>
     * Note: method defined as abstract to ensure all implementation
     * classes explicitly implement method, instead of relying
     * on {@link Object#toString()} definition.
     */
    @Override
    public abstract String toString();

    /**
     * Equality for node objects is defined as full (deep) value
     * equality. This means that it is possible to compare complete
     * JSON trees for equality by comparing equality of root nodes.
     *<p>
     * Note: marked as abstract to ensure all implementation
     * classes define it properly and not rely on definition
     * from {@link java.lang.Object}.
     */
    @Override
    public abstract boolean equals(Object o);
}
