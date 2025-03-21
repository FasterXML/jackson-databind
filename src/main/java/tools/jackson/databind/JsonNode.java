package tools.jackson.databind;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.stream.Stream;

import tools.jackson.core.*;
import tools.jackson.databind.exc.JsonNodeException;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.JsonNodeType;
import tools.jackson.databind.node.MissingNode;
import tools.jackson.databind.node.ObjectNode;
import tools.jackson.databind.util.ClassUtil;

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
 * {@link tools.jackson.databind.node}.
 *<p>
 * Note that it is possible to "read" from nodes, using
 * method {@link TreeNode#traverse}, which will result in
 * a {@link JsonParser} being constructed. This can be used for (relatively)
 * efficient conversations between different representations; and it is what
 * core databind uses for methods like {@link ObjectMapper#treeToValue(TreeNode, Class)}
 * and {@link ObjectMapper#treeAsTokens(TreeNode)}
 */
public abstract class JsonNode
    extends JacksonSerializable.Base // i.e. implements JacksonSerializable
    implements TreeNode, Iterable<JsonNode>
{
    /**
     * Configuration setting used with {@link JsonNode#withObject(JsonPointer)}
     * method overrides, to indicate which overwrites are acceptable if the
     * path pointer indicates has incompatible nodes (for example, instead
     * of Object node a Null node is encountered).
     * Overwrite means that the existing value is replaced with compatible type,
     * potentially losing existing values or even sub-trees.
     *<p>
     * Default value if {@code NULLS} which only allows Null-value nodes
     * to be replaced but no other types.
     */
    public enum OverwriteMode {
        /**
         * Mode in which no values may be overwritten, not even {@code NullNode}s;
         * only compatible paths may be traversed.
         */
        NONE,

        /**
         * Mode in which explicit {@code NullNode}s may be replaced but no other
         * node types.
         */
        NULLS,

        /**
         * Mode in which all scalar value nodes may be replaced, but not
         * Array or Object nodes.
         */
        SCALARS,

        /**
         * Mode in which all incompatible node types may be replaced, including
         * Array and Object nodes where necessary.
         */
        ALL;
    }

    /*
    /**********************************************************************
    /* Construction, related
    /**********************************************************************
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
     * @return Node that is either a copy of this node (and all non-leaf
     *    children); or, for immutable leaf nodes, node itself.
     */
    public abstract JsonNode deepCopy();

    /*
    /**********************************************************************
    /* TreeNode implementation
    /**********************************************************************
     */

//  public abstract JsonToken asToken();
//  public abstract JsonToken traverse();
//  public abstract JsonParser.NumberType numberType();

    @Override
    public int size() { return 0; }

    /**
     * Convenience method that is functionally same as:
     *<pre>
     *    size() == 0
     *</pre>
     * for all node types.
     */
    public boolean isEmpty() { return size() == 0; }

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
    public boolean isContainer() {
        return false;
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
     * a {@link tools.jackson.databind.node.NullNode} will be returned,
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
     * a {@link tools.jackson.databind.node.NullNode} will be returned,
     * not null.
     *
     * @return Node that represent value of the specified field,
     *   if this node is an object and has value for the specified
     *   field. Null otherwise.
     */
    @Override
    public JsonNode get(String propertyName) { return null; }

    /**
     * Method for accessing value of the specified element of
     * an array node, wrapped in an {@link Optional}. For other nodes,
     * an empty Optional is always returned.
     *<p>
     * For array nodes, index specifies
     * exact location within array and allows for efficient iteration
     * over child elements (underlying storage is guaranteed to
     * be efficiently indexable, i.e. has random-access to elements).
     * If index is less than 0, or equal-or-greater than
     * <code>node.size()</code>, an empty Optional is returned; no exception is
     * thrown for any index.
     *<p>
     * NOTE: if the element value has been explicitly set as <code>null</code>
     * (which is different from removal!),
     * a {@link tools.jackson.databind.node.NullNode} will be returned
     * wrapped in an Optional, not an empty Optional.
     *
     * @return Optional containing the node that represents the value of the specified element,
     *   if this node is an array and has the specified element and otherwise, an
     *   empty Optional, never null.
     *
     * @since 2.19
     */
    public Optional<JsonNode> optional(int index) { return Optional.empty(); }

    /**
     * Method for accessing value of the specified field of
     * an object node. If this node is not an object (or it
     * does not have a value for specified field name), or
     * if there is no field with such name, empty {@link Optional}
     * is returned.
     *<p>
     * NOTE: if the property value has been explicitly set as <code>null</code>
     * (which is different from removal!), an Optional containing
     * {@link tools.jackson.databind.node.NullNode} will be returned,
     * not null.
     *
     * @return Optional that may contain value of the specified field,
     *  if this node is an object and has value for the specified
     *  field. Empty Optional otherwise never null.
     *
     * @since 2.19
     */
    public Optional<JsonNode> optional(String propertyName) { return Optional.empty(); }

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
    public abstract JsonNode path(String propertyName);

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
    public Collection<String> propertyNames() {
        return Collections.emptySet();
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
     */
    @Override
    public final JsonNode at(String jsonPtrExpr) {
        return at(JsonPointer.compile(jsonPtrExpr));
    }

    /**
     * Helper method used by other methods for traversing the next step
     * of given path expression, and returning matching value node if any:
     * if no match, {@code null} is returned.
     *
     * @param ptr Path expression to use
     *
     * @return Either matching {@link JsonNode} for the first step of path or
     *    {@code null} if no match (including case that this node is not a container)
     */
    protected abstract JsonNode _at(JsonPointer ptr);

    /*
    /**********************************************************************
    /* Public API, type introspection
    /**********************************************************************
     */

    // // First high-level division between values, containers and "missing"

    /**
     * Return the type of this node
     *
     * @return the node type as a {@link JsonNodeType} enum value
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
     * {@link #canConvertToLong()} instead.
     *
     * @return True if the value contained by this node is stored as Java <code>long</code>
     */
    public boolean isLong() { return false; }

    public boolean isFloat() { return false; }

    public boolean isDouble() { return false; }
    public boolean isBigDecimal() { return false; }
    public boolean isBigInteger() { return false; }

    /**
     * Method that checks whether this node represents JSON String
     * value.
     */
    public final boolean isString() {
        return getNodeType() == JsonNodeType.STRING;
    }

    /**
     * @deprecated Use {@link #isString} instead.
     */
    @Deprecated // since 3.0
    public boolean isTextual() {
        return isString();
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
    @Override
    public final boolean isNull() {
        return getNodeType() == JsonNodeType.NULL;
    }

    /**
     * Method that can be used to check if this node represents
     * binary data (Base64 encoded). Although this will be externally
     * written as JSON String value, {@link #isString} will
     * return false if this method returns true.
     *
     * @return True if this node represents base64 encoded binary data
     */
    public final boolean isBinary() {
        return getNodeType() == JsonNodeType.BINARY;
    }

    /**
     * Method that can be used to check whether this node is a numeric
     * node ({@link #isNumber} would return true)
     * AND can be converted without loss to it (that is, its value fits
     * within Java's 32-bit signed integer type, <code>int</code> and
     * if it is a floating-point number, it does not have fractional part).
     *<p>
     * NOTE: this method does not consider possible value type conversion
     * from non-number types like JSON String into Number; so even if this method returns false,
     * it is possible that {@link #asInt} could still succeed.
     *
     * @since 2.0
     */
    public boolean canConvertToInt() { return false; }

    /**
     * Method that can be used to check whether this node is a numeric
     * node ({@link #isNumber} would return true)
     * AND can be converted without loss to it (that is, its value fits
     * within Java's 64-bit signed integer type, <code>long</code> and
     * if it is a floating-point number, it does not have fractional part).
     *<p>
     * NOTE: this method does not consider possible value type conversion
     * from non-number types like JSON String into Number; so even if this method returns false,
     * it is possible that {@link #asLong} could still succeed.
     */
    public boolean canConvertToLong() { return false; }

    /**
     * Method that can be used to check whether contained value
     * is numeric (returns true for {@link #isNumber()}) and
     * can be losslessly converted to integral number (specifically,
     * {@link BigInteger} but potentially others, see
     * {@link #canConvertToInt} and {@link #canConvertToInt}).
     * Latter part allows floating-point numbers
     * (for which {@link #isFloatingPointNumber()} returns {@code true})
     * that do not have fractional part.
     * Note that "not-a-number" values of {@code double} and {@code float}
     * will return {@code false} as they can not be converted to matching
     * integral representations.
     *
     * @return True if the value is an actual number with no fractional
     *    part; false for non-numeric types, NaN representations of floating-point
     *    numbers, and floating-point numbers with fractional part.
     *
     * @since 2.12
     */
    public boolean canConvertToExactIntegral() {
        return isIntegralNumber();
    }

    /*
    /**********************************************************************
    /* Public API, scalar value access (exact, converting)
    /**********************************************************************
     */

    // // Scalar access: generic

    /**
     * Method that will return a {@link JsonNode} wrapped in Java's {@link Optional}.
     * All nodes except of {@link MissingNode} will return non-empty {@link Optional};
     * {@link MissingNode} will return empty {@link Optional}.
     *
     * @return {@code Optional<JsonNode>} containing this node, or {@link Optional#empty()}
     *        if this is a {@link MissingNode}.
     */
    public Optional<JsonNode> asOptional() {
        return Optional.of(this);
    }

    // // Scalar access: Strings

    /**
     * Method that will try to access value of this node as a Java {@code String}
     * which works if (and only if) node contains JSON String value:
     * if not, a {@link JsonNodeException} will be thrown.
     *<p>
     * NOTE: for more lenient conversions, use {@link #asString()}
     *<p>
     * NOTE: in Jackson 2.x, was {@code textValue()}.
     *
     * @return {@code String} value this node represents (if JSON String)
     *
     * @throws JsonNodeException if node value is not a JSON String value
     */
    public abstract String stringValue();

    /**
     * Method similar to {@link #stringValue()}, but that will return specified
     * {@code defaultValue} if this node does not contain a JSON String.
     *
     * @param defaultValue Value to return if this node does not contain a JSON String.
     *
     * @return Java {@code String} value this node represents (if JSON String);
     *   {@code defaultValue} otherwise
     */
    public abstract String stringValue(String defaultValue);

    /**
     * Method similar to {@link #stringValue()}, but that will return
     * {@code Optional.empty()} if this node does not contain a JSON String.
     *
     * @return {@code Optional<String>} value (if node represents JSON String);
     *   {@code Optional.empty()} otherwise
     */
    public abstract Optional<String> stringValueOpt();

    /**
     * @deprecated Use {@link #asString()} instead.
     */
    @Deprecated // since 3.0
    public final String textValue() {
        return stringValue();
    }

    /**
     * Method that will return a valid String representation of
     * the contained value, if the node is a value node
     * (method {@link #isValueNode} returns true),
     * otherwise empty String.
     *<p>
     * NOTE: this is NOT same as {@link #toString()} in that result is
     * <p>NOT VALID ENCODED JSON</p> for all nodes (but is for some, like
     * {@code NumberNode}s and {@code BooleanNode}s).
     */
    public abstract String asString();

    /**
     * Returns the text value of this node or the provided {@code defaultValue} if this node
     * does not have a text value. Useful for nodes that are {@link MissingNode} or
     * {@link tools.jackson.databind.node.NullNode}, ensuring a default value is returned instead of null or missing indicators.
     *
     * @param defaultValue The default value to return if this node's text value is absent.
     * @return The text value of this node, or {@code defaultValue} if the text value is absent.
     */
    public abstract String asString(String defaultValue);

    /**
     * @deprecated Use {@link #asString()} instead.
     */
    @Deprecated // since 3.0
    public final String asText() {
        return asString();
    }

    /**
     * @deprecated Use {@link #asString(String)} instead.
     */
    @Deprecated // since 3.0
    public String asText(String defaultValue) {
        return asString(defaultValue);
    }

    // // Scalar access: Binary

    /**
     * Method that will try to access value of this node as binary value (Java {@code byte[]})
     * which works if (and only if) node contains binary value (for JSON, Base64-encoded
     * String, for other formats native binary value): if not,
     * a {@link JsonNodeException} will be thrown.
     * To check if this method can be used, you may call {@link #isBinary()}.
     *<p>
     * @return Binary value this node represents (if node contains binary value)
     *
     * @throws JsonNodeException if node does not contain a Binary value (a
     */
    public abstract byte[] binaryValue();

    // // Scalar access: Boolean

    /**
     * Method that will try to access value of this node as a Java {@code boolean}
     * which works if (and only if) node contains JSON boolean value: if not,
     * a {@link JsonNodeException} will be thrown.
     *<p>
     * NOTE: for more lenient conversions, use {@link #asBoolean()}
     *
     * @return {@code boolean} value this node represents (if JSON boolean)
     *
     * @throws JsonNodeException if node does not represent a JSON boolean value
     */
    public abstract boolean booleanValue();

    /**
     * Method similar to {@link #booleanValue()}, but that will return specified
     * {@code defaultValue} if this node does not contain a JSON boolean.
     *
     * @param defaultValue Value to return if this node does not contain a JSON boolean.
     *
     * @return Java {@code boolean} value this node represents (if JSON boolean);
     *   {@code defaultValue} otherwise
     */
    public abstract boolean booleanValue(boolean defaultValue);

    /**
     * Method similar to {@link #booleanValue()}, but that will return
     * {@code Optional.empty()} if this node does not contain a JSON boolean.
     *
     * @return {@code Optional<Boolean>} value (if node represents JSON boolean);
     *   {@code Optional.empty()} otherwise
     */
    public abstract Optional<Boolean> booleanValueOpt();

    /**
     * Method that will try to convert value of this node to a Java {@code boolean}.
     * JSON Booleans map naturally; Integer numbers other than 0 map to true, and
     * 0 maps to false; {@code null} maps to false
     * and Strings 'true' and 'false' map to corresponding values.
     * Other values (including structured types like Objects and Arrays) will
     * result in a {@link JsonNodeException} being thrown.
     *
     * @return Boolean value this node represents, if coercible; exception otherwise
     *
     * @throws JsonNodeException if node cannot be coerced to a Java {@code boolean}
     */
    public abstract boolean asBoolean();

    /**
     * Similar to {@link #asBoolean()}, but instead of throwing an exception for
     * non-coercible values, will return specified default value.
     */
    public abstract boolean asBoolean(boolean defaultValue);

    /**
     * Similar to {@link #asBoolean()}, but instead of throwing an exception for
     * non-coercible values, will return {@code Optional.empty()}.
     */
    public abstract Optional<Boolean> asBooleanOpt();

    // // Scalar access: Numbers, generic

    /**
     * Method that will try to access value of this node as {@link Number}
     * that accurately represents its value, if (and only if) this is
     * a number node (returns {@code true} for {@link #isNumber}).
     * If this node is NOT a number node, a {@link JsonNodeException} will be thrown.
     *
     * @return Number value this node contains, if numeric node
     */
    public abstract Number numberValue();

    // // Scalar access: Numbers, Java short

    /**
     * Method that will try to access value of this node as 16-bit signed
     * integer value (Java {@code short}):
     * but if node value cannot be expressed <b>exactly</b> as a {@code short},
     * a {@link JsonNodeException} will be thrown.
     * Access works for following cases:
     * <ul>
     *  <li>JSON Integer values that fit in Java 16-bit signed {@code short} range
     *   </li>
     *  <li>JSON Floating-point values that fit in Java 16-bit signed {@code short} range
     *    AND do not have fractional part.
     *    </li>
     * </ul>
     *<p>
     *
     * @return {@code Short} value this node represents, if possible to accurately represent
     *
     * @throws JsonNodeException if node value cannot be converted to Java {@code short}
     */
    public abstract short shortValue();

    // // Scalar access: Numbers, Java int

    /**
     * Method that will try to access value of this node as a Java {@code int}:
     * but if node value cannot be expressed <b>exactly</b> as an {@code int},
     * a {@link JsonNodeException} will be thrown.
     * Access works for following cases:
     * <ul>
     *  <li>JSON Integer values that fit in Java 32-bit signed {@code int} range
     *   </li>
     *  <li>JSON Floating-point values that fit in Java 32-bit signed {@code int} range
     *    AND do not have fractional part.
     *    </li>
     * </ul>
     *<p>
     * NOTE: for more lenient conversions, use {@link #asInt()}
     *
     * @return {@code Int} value this node represents, if possible to accurately represent
     *
     * @throws JsonNodeException if node value cannot be converted to Java {@code int}
     */
    public abstract int intValue();

    /**
     * Method similar to {@link #intValue()}, but that will return specified
     * {@code defaultValue} if this node cannot be converted to Java {@code int}.
     *
     * @param defaultValue Value to return if this node cannot be converted to Java {@code int}
     *
     * @return Java {@code int} value this node represents, if possible to accurately represent;
     *   {@code defaultValue} otherwise
     */
    public abstract int intValue(int defaultValue);

    /**
     * Method similar to {@link #intValue()}, but that will return empty
     * {@link OptionalInt} ({@code OptionalInt.empty()}) if this node cannot
     * be converted to Java {@code int}.
     *
     * @return Java {@code int} value this node represents, as {@link OptionalInt},
     * if possible to accurately represent; {@code OptionalInt.empty()} otherwise
     */
    public abstract OptionalInt intValueOpt();

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
    public abstract int asInt();

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
    public abstract int asInt(int defaultValue);

    // // Scalar access: Numbers, Java long

    /**
     * Method that will try to access value of this node as a Java {@code long}:
     * but if node value cannot be expressed <b>exactly</b> as a {@code long},
     * a {@link JsonNodeException} will be thrown.
     * Access works for following cases:
     * <ul>
     *  <li>JSON Integer values that fit in Java 64-bit signed {@code long} range
     *   </li>
     *  <li>JSON Floating-point values that fit in Java 64-bit signed {@code long} range
     *    AND do not have fractional part.
     *    </li>
     * </ul>
     *<p>
     * NOTE: for more lenient conversions, use {@link #asLong()}
     *
     * @return {@code Long} value this node represents, if possible to accurately represent
     *
     * @throws JsonNodeException if node value cannot be converted to Java {@code long}
     */
    public abstract long longValue();

    /**
     * Method similar to {@link #longValue()}, but that will return specified
     * {@code defaultValue} if this node cannot be converted to Java {@code long}.
     *
     * @param defaultValue Value to return if this node cannot be converted to Java {@code long}
     *
     * @return Java {@code long} value this node represents, if possible to accurately represent;
     *   {@code defaultValue} otherwise
     */
    public abstract long longValue(long defaultValue);

    /**
     * Method similar to {@link #longValue()}, but that will return empty
     * {@link OptionalLong} ({@code OptionalLong.empty()}) if this node cannot
     * be converted to Java {@code long}.
     *
     * @return Java {@code long} value this node represents, as {@link OptionalLong},
     * if possible to accurately represent; {@code OptionalLong.empty()} otherwise
     */
    public abstract OptionalLong longValueOpt();

    /**
     * Method that will try to convert value of this node to a Java <b>long</b>.
     * Numbers are coerced using default Java rules; booleans convert to 0 (false)
     * and 1 (true), and Strings are parsed using default Java language integer
     * parsing rules.
     *<p>
     * If representation cannot be converted to a long (including structured types
     * like Objects and Arrays),
     * default value of <b>0</b> will be returned; no exceptions are thrown.
     */
    public abstract long asLong();

    /**
     * Method that will try to convert value of this node to a Java <b>long</b>.
     * Numbers are coerced using default Java rules; booleans convert to 0 (false)
     * and 1 (true), and Strings are parsed using default Java language integer
     * parsing rules.
     *<p>
     * If representation cannot be converted to a long (including structured types
     * like Objects and Arrays),
     * specified <b>defaultValue</b> will be returned; no exceptions are thrown.
     */
    public abstract long asLong(long defaultValue);

    // // Scalar access: Numbers, Java BigInteger

    /**
     * Method that will try to access value of this node as a {@link BigInteger},
     * but if node value cannot be expressed <b>exactly</b> as a {@link BigInteger},
     * a {@link JsonNodeException} will be thrown.
     * Access works for following cases:
     * <ul>
     *  <li>JSON Integer values
     *   </li>
     *  <li>JSON Floating-point values that do not have fractional part.
     *    </li>
     * </ul>
     *<p>
     *
     * @return {@code BigInteger} value this node represents, if possible to accurately represent
     *
     * @throws JsonNodeException if node value cannot be converted to Java {@code BigInteger}
     */
    public abstract BigInteger bigIntegerValue();

    // // Scalar access: Numbers, Java float

    /**
     * Method that will try to access value of this node as a Java {@code float}:
     * but if node value cannot be expressed <b>exactly</b> as a {@code float},
     * a {@link JsonNodeException} will be thrown.
     * Access works for following cases:
     * <ul>
     *  <li>JSON Floating-point values that fit in Java 32-bit {@code double} range
     *    </li>
     *  <li>JSON Integer values that fit in Java 32-bit {@code double} range
     *   </li>
     * </ul>
     *<p>
     *
     * @return {@code Float} value this node represents, if possible to accurately represent
     *
     * @throws JsonNodeException if node value cannot be converted to Java {@code float}
     */
    public abstract float floatValue();

    // // Scalar access: Numbers, Java double

    /**
     * Method that will try to access value of this node as a Java {@code double}:
     * but if node value cannot be expressed <b>exactly</b> as a {@code double},
     * a {@link JsonNodeException} will be thrown.
     * Access works for following cases:
     * <ul>
     *  <li>JSON Floating-point values that fit in Java 64-bit {@code double} range
     *    </li>
     *  <li>JSON Integer values that fit in Java 64-bit {@code double} range
     *   </li>
     * </ul>
     *<p>
     * NOTE: for more lenient conversions, use {@link #asDouble()}
     *
     * @return {@code Double} value this node represents, if possible to accurately represent
     *
     * @throws JsonNodeException if node value cannot be converted to Java {@code double}
     */
    public abstract double doubleValue();

    /**
     * Method similar to {@link #doubleValue()}, but that will return specified
     * {@code defaultValue} if this node cannot be converted to Java {@code double}.
     *
     * @param defaultValue Value to return if this node cannot be converted to Java {@code double}
     *
     * @return Java {@code double} value this node represents, if possible to accurately represent;
     *   {@code defaultValue} otherwise
     */
    public abstract double doubleValue(double defaultValue);

    /**
     * Method similar to {@link #doubleValue()}, but that will return empty
     * {@link OptionalLong} ({@code OptionalDouble.empty()}) if this node cannot
     * be converted to Java {@code double}.
     *
     * @return Java {@code double} value this node represents, as {@link OptionalDouble},
     * if possible to accurately represent; {@code OptionalDouble.empty()} otherwise
     */
    public abstract OptionalDouble doubleValueOpt();
    
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
    public abstract double asDouble();

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
    public abstract double asDouble(double defaultValue);

    // // Scalar access: Numbers, Java BigDecimal

    /**
     * Method that will try to access value of this node as a Java {@code BigDecimal}:
     * but if node value cannot be expressed <b>exactly</b> as a {@code BigDecimal},
     * a {@link JsonNodeException} will be thrown.
     * Access works for following cases:
     * <ul>
     *  <li>All JSON Number values
     *    </li>
     * </ul>
     *<p>
     * NOTE: for more lenient conversions, use {@link #asDecimal()}
     *
     * @return {@code BigDecimal} value this node represents, if possible to accurately represent
     *
     * @throws JsonNodeException if node value cannot be converted to Java {@code BigDecimal}
     */
    public abstract BigDecimal decimalValue();

    /**
     * Method similar to {@link #decimalValue()}, but that will return {@code defaultValue}
     * if this node cannot be coerced to Java {@code BigDecimal}.
     *
     * @return {@code BigDecimal} value this node represents,
     * if possible to accurately represent; {@code defaultValue} otherwise
     */
    public abstract BigDecimal decimalValue(BigDecimal defaultValue);

    /**
     * Method similar to {@link #decimalValue()}, but that will return empty
     * {@link Optional} ({@code Optional.empty()}) if this node cannot
     * be coerced to {@code BigDecimal}.
     *
     * @return Java {@code BigDecimal} value this node represents, as {@code Optional<BigDecimal>},
     * if possible to accurately represent; {@code Optional.empty()} otherwise
     */
    public abstract Optional<BigDecimal> decimalValueOpt();

    public abstract BigDecimal asDecimal();
    
    public abstract BigDecimal asDecimal(BigDecimal defaultValue);

    /*
    /**********************************************************************
    /* Public API, extended traversal with "required()"
    /**********************************************************************
     */

    /**
     * Method that may be called to verify that {@code this} node is NOT so-called
     * "missing node": that is, one for which {@link #isMissingNode()} returns {@code true}.
     * If not missing node, {@code this} is returned to allow chaining;
     * otherwise exception is thrown.
     *
     * @return {@code this} node to allow chaining
     *
     * @throws IllegalArgumentException if this node is "missing node"
     */
    public <T extends JsonNode> T require() {
        return _this();
    }

    /**
     * Method that may be called to verify that {@code this} node is neither so-called
     * "missing node" (that is, one for which {@link #isMissingNode()} returns {@code true})
     * nor "null node" (one for which {@link #isNull()} returns {@code true}).
     * If non-null non-missing node, {@code this} is returned to allow chaining;
     * otherwise exception is thrown.
     *
     * @return {@code this} node to allow chaining
     *
     * @throws IllegalArgumentException if this node is either "missing node" or "null node"
     */
    public <T extends JsonNode> T requireNonNull() {
        return _this();
    }

    /**
     * Method is functionally equivalent to
     *{@code
     *   path(propertyName).required()
     *}
     * and can be used to check that this node is an {@code ObjectNode} (that is, represents
     * JSON Object value) and has value for specified property with key {@code propertyName}
     * (but note that value may be explicit JSON null value).
     * If this node is Object Node and has value for specified property, matching value
     * is returned; otherwise {@link IllegalArgumentException} is thrown.
     *
     * @param propertyName Name of property to access
     *
     * @return Value of the specified property of this Object node
     *
     * @throws IllegalArgumentException if this node is not an Object node or if it does not
     *   have value for specified property
     */
    public abstract JsonNode required(String propertyName);

    /**
     * Method is functionally equivalent to
     *{@code
     *   path(index).required()
     *}
     * and can be used to check that this node is an {@code ArrayNode} (that is, represents
     * JSON Array value) and has value for specified {@code index}
     * (but note that value may be explicit JSON null value).
     * If this node is Array Node and has value for specified index, value at index
     * is returned; otherwise {@link IllegalArgumentException} is thrown.
     *
     * @param index Index of the value of this Array node to access
     *
     * @return Value at specified index of this Array node
     *
     * @throws IllegalArgumentException if this node is not an Array node or if it does not
     *   have value for specified index
     */
    public abstract JsonNode required(int index);

    /**
     * Method is functionally equivalent to
     *{@code
     *   at(pathExpr).required()
     *}
     * and can be used to check that there is an actual value node at specified {@link JsonPointer}
     * starting from {@code this} node
     * (but note that value may be explicit JSON null value).
     * If such value node exists it is returned;
     * otherwise {@link IllegalArgumentException} is thrown.
     *
     * @param pathExpr {@link JsonPointer} expression (as String) to use for finding value node
     *
     * @return Matching value node for given expression
     *
     * @throws IllegalArgumentException if no value node exists at given {@code JSON Pointer} path
     */
    public JsonNode requiredAt(String pathExpr) {
        return requiredAt(JsonPointer.compile(pathExpr));
    }

    /**
     * Method is functionally equivalent to
     *{@code
     *   at(path).required()
     *}
     * and can be used to check that there is an actual value node at specified {@link JsonPointer}
     * starting from {@code this} node
     * (but note that value may be explicit JSON null value).
     * If such value node exists it is returned;
     * otherwise {@link IllegalArgumentException} is thrown.
     *
     * @param path {@link JsonPointer} expression to use for finding value node
     *
     * @return Matching value node for given expression
     *
     * @throws IllegalArgumentException if no value node exists at given {@code JSON Pointer} path
     */
    public final JsonNode requiredAt(final JsonPointer path) {
        JsonPointer currentExpr = path;
        JsonNode curr = this;

        // Note: copied from `at()`
        while (true) {
            if (currentExpr.matches()) {
                return curr;
            }
            curr = curr._at(currentExpr); // lgtm [java/dereferenced-value-may-be-null]
            if (curr == null) {
                _reportRequiredViolation("No node at '%s' (unmatched part: '%s')",
                        path, currentExpr);
            }
            currentExpr = currentExpr.tail();
        }
    }

    /*
    /**********************************************************************
    /* Public API, value find / existence check methods
    /**********************************************************************
     */

    /**
     * Method that allows checking whether this node is JSON Object node
     * and contains value for specified property. If this is the case
     * (including properties with explicit null values), returns true;
     * otherwise returns false.
     *<p>
     * This method is equivalent to:
     *<pre>
     *   node.get(propertyName) != null
     *</pre>
     * (since return value of get() is node, not value node contains)
     *<p>
     * NOTE: when explicit <code>null</code> values are added, this
     * method will return <code>true</code> for such properties.
     *
     * @param propertyName Name of element to check
     *
     * @return True if this node is a JSON Object node, and has a property
     *   entry with specified name (with any value, including null value)
     */
    public boolean has(String propertyName) {
        return get(propertyName) != null;
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
     *   node.get(propertyName) != null &amp;&amp; !node.get(propertyName).isNull()
     *</pre>
     */
    public boolean hasNonNull(String propertyName) {
        JsonNode n = get(propertyName);
        return (n != null) && !n.isNull();
    }

    /**
     * Method that is similar to {@link #has(int)}, but that will
     * return <code>false</code> for explicitly added nulls.
     *<p>
     * This method is equivalent to:
     *<pre>
     *   node.get(index) != null &amp;&amp; !node.get(index).isNull()
     *</pre>
     */
    public boolean hasNonNull(int index) {
        JsonNode n = get(index);
        return (n != null) && !n.isNull();
    }

    /*
    /**********************************************************************
    /* Public API, container access
    /**********************************************************************
     */

    /**
     * Implemented so that convenience "for-each" loop can be used for looping over elements
     * of JSON Array constructs.
     */
    @Override
    public final Iterator<JsonNode> iterator() { return values().iterator(); }

    @Override
    public final Spliterator<JsonNode> spliterator() { return values().spliterator(); }

    /**
     * Method for accessing all value nodes of this Node, iff
     * this node is a JSON Array or Object node. In case of Object node,
     * field names (keys) are not included, only values.
     * For other types of nodes, returns empty iterator.
     */
    public Collection<JsonNode> values() {
        return Collections.emptyList();
    }

    /**
     * Accessor that will return properties of {@code ObjectNode}
     * similar to how {@link Map#entrySet()} works; 
     * for other node types will return empty {@link java.util.Set}.
     *
     * @return Set of properties, if this node is an {@code ObjectNode}
     * ({@link JsonNode#isObject()} returns {@code true}); empty
     * {@link java.util.Set} otherwise.
     */
    public Set<Map.Entry<String, JsonNode>> properties() {
        return Collections.emptySet();
    }

    /**
     * Returns a stream of all value nodes of this Node, iff
     * this node is an {@code ArrayNode} or {@code ObjectNode}.
     * In case of {@code Object} node, property names (keys) are not included, only values.
     * For other types of nodes, returns empty stream.
     *
     * @since 2.19
     */
    public Stream<JsonNode> valueStream() {
        return ClassUtil.emptyStream();
    }

    /**
     * Returns a stream of all properties (key, value pairs) of this Node,
     * iff this node is an an {@code ObjectNode}.
     * For other types of nodes, returns empty stream.
     *
     * @since 2.19
     */
    public Stream<Map.Entry<String, JsonNode>> propertyStream() {
        return ClassUtil.emptyStream();
    }

    /**
     * If this node is an {@code ObjectNode}, performs the given action for each
     * property (key, value pair)
     * until all entries have been processed or the action throws an exception.
     * Exceptions thrown by the action are relayed to the caller.     
     * For other node types, no action is performed.
     *<p>
     * Actions are performed in the order of properties, same as order returned by
     * method {@link #properties()}.
     * This is generally the document order of properties in JSON object.
     * 
     * @param action Action to perform for each entry
     */
    public void forEachEntry(BiConsumer<? super String, ? super JsonNode> action) {
        // No-op for all but ObjectNode
    }

    /*
    /**********************************************************************
    /* Public API, find methods
    /**********************************************************************
     */

    /**
     * Method for finding the first JSON Object field with specified name in this
     * node or its child nodes, and returning value it has.
     * If no matching field is found in this node or its descendants, returns null.
     *<p>
     * Note that traversal is done in document order (that is, order in which
     * nodes are iterated if using {@link JsonNode#values()})
     *
     * @param propertyName Name of field to look for
     *
     * @return Value of first matching node found, if any; null if none
     */
    public abstract JsonNode findValue(String propertyName);

    /**
     * Method for finding JSON Object fields with specified name -- both immediate
     * child values and descendants -- and returning
     * found ones as a {@link List}.
     * Note that sub-tree search ends when matching field is found,
     * so possible children of result nodes are <b>not</b> included.
     * If no matching fields are found in this node or its descendants, returns
     * an empty List.
     *
     * @param propertyName Name of field to look for
     */
    public final List<JsonNode> findValues(String propertyName)
    {
        List<JsonNode> result = findValues(propertyName, null);
        if (result == null) {
            return Collections.emptyList();
        }
        return result;
    }

    /**
     * Similar to {@link #findValues}, but will additionally convert
     * values into Strings, calling {@link #asText}.
     */
    public final List<String> findValuesAsString(String propertyName)
    {
        List<String> result = findValuesAsString(propertyName, null);
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
     * @param propertyName Name of field to look for
     *
     * @return Value of first matching node found; or if not found, a
     *    "missing node" (non-null instance that has no value)
     */
    public abstract JsonNode findPath(String propertyName);

    /**
     * Method for finding a JSON Object that contains specified field,
     * within this node or its descendants.
     * If no matching field is found in this node or its descendants, returns null.
     *
     * @param propertyName Name of field to look for
     *
     * @return Value of first matching node found, if any; null if none
     */
    public abstract JsonNode findParent(String propertyName);

    /**
     * Method for finding a JSON Object that contains specified field,
     * within this node or its descendants.
     * If no matching field is found in this node or its descendants, returns null.
     *
     * @param propertyName Name of field to look for
     *
     * @return Value of first matching node found, if any; null if none
     */
    public final List<JsonNode> findParents(String propertyName)
    {
        List<JsonNode> result = findParents(propertyName, null);
        if (result == null) {
            return Collections.emptyList();
        }
        return result;
    }

    public abstract List<JsonNode> findValues(String propertyName, List<JsonNode> foundSoFar);
    public abstract List<String> findValuesAsString(String propertyName, List<String> foundSoFar);
    public abstract List<JsonNode> findParents(String propertyName, List<JsonNode> foundSoFar);

    /*
    /**********************************************************************
    /* Public API, path handling
    /**********************************************************************
     */

    /**
     * Method that works in one of possible ways, depending on whether
     * {@code exprOrProperty} is a valid {@link JsonPointer} expression or
     * not (valid expression is either empty String {@code ""} or starts
     * with leading slash {@code /} character).
     * If it is, works as a short-cut to:
     *<pre>
     *  withObject(JsonPointer.compile(exprOrProperty));
     *</pre>
     * If it is NOT a valid {@link JsonPointer} expression, value is taken
     * as a literal Object property name and calls is alias for
     *<pre>
     *  withObjectProperty(exprOrProperty);
     *</pre>
     *
     * @param exprOrProperty {@link JsonPointer} expression to use (if valid as one),
     *    or, if not (no leading "/"), property name to match.
     *
     * @return {@link ObjectNode} found or created
     */
    public ObjectNode withObject(String exprOrProperty) {
        // To avoid abstract method, base implementation just fails
        return _reportUnsupportedOperation("`JsonNode` not of type `ObjectNode` (but `"
                +getClass().getName()+")`, cannot call `withObject()` on it");
    }

    /**
     * Short-cut equivalent to:
     *<pre>
     *  withObject(JsonPointer.compile(expr), overwriteMode, preferIndex);
     *</pre>
     */
    public final ObjectNode withObject(String expr,
            OverwriteMode overwriteMode, boolean preferIndex) {
        return withObject(JsonPointer.compile(expr), overwriteMode, preferIndex);
    }

    /**
     * Same as {@link #withObject(JsonPointer, OverwriteMode, boolean)} but
     * with defaults of {@code OvewriteMode#NULLS} (overwrite mode)
     * and {@code true} for {@code preferIndex} (that is, will try to
     * consider {@link JsonPointer} segments index if at all possible
     * and only secondarily as property name
     *
     * @param ptr {@link JsonPointer} that indicates path to use for Object value to return
     *   (potentially creating as necessary)
     *
     * @return {@link ObjectNode} found or created
     */
    public final ObjectNode withObject(JsonPointer ptr) {
        return withObject(ptr, OverwriteMode.NULLS, true);
    }

    /**
     * Method that can be called on Object or Array nodes, to access a Object-valued
     * node pointed to by given {@link JsonPointer}, if such a node exists:
     * or if not, an attempt is made to create one and return it.
     * For example, on document
     *<pre>
     *  { "a" : {
     *       "b" : {
     *          "c" : 13
     *       }
     *    }
     *  }
     *</pre>
     * calling method with {@link JsonPointer} of {@code /a/b} would return
     * {@link ObjectNode}
     *<pre>
     *  { "c" : 13 }
     *</pre>
     *<p>
     * In cases where path leads to "missing" nodes, a path is created.
     * So, for example, on above document, and
     * {@link JsonPointer} of {@code /a/x} an empty {@link ObjectNode} would
     * be returned and the document would look like:
     *<pre>
     *  { "a" : {
     *       "b" : {
     *          "c" : 13
     *       },
     *       "x" : { }
     *    }
     *  }
     *</pre>
     * Finally, if the path is incompatible with the document -- there is an existing
     * {@code JsonNode} through which expression cannot go -- a replacement is
     * attempted if (and only if) conversion is allowed as per {@code overwriteMode}
     * passed in. For example, with above document and expression of {@code /a/b/c},
     * conversion is allowed if passing {@code OverwriteMode.SCALARS} or
     * {@code OvewriteMode.ALL}, and resulting document would look like:
     *<pre>
     *  { "a" : {
     *       "b" : {
     *          "c" : { }
     *       },
     *       "x" : { }
     *    }
     *  }
     *</pre>
     * but if different modes ({@code NONE} or {@code NULLS}) is passed, an exception
     * is thrown instead.
     *
     * @param ptr Pointer that indicates path to use for {@link ObjectNode} value to return
     *   (potentially creating one as necessary)
     * @param overwriteMode Defines which node types may be converted in case of
     *    incompatible {@code JsonPointer} expression: if conversion not allowed,
     *    {@link UnsupportedOperationException} is thrown.
     * @param preferIndex When creating a path (for empty or replacement), and path
     *    contains segment that may be an array index (simple integer number like
     *    {@code 3}), whether to construct an {@link ArrayNode} ({@code true}) or
     *    {@link ObjectNode} ({@code false}). In latter case matching property with
     *    quoted number (like {@code "3"}) is used within Object.
     *
     * @return {@link ObjectNode} found or created
     *
     * @throws JsonNodeException if a conversion would be needed for given
     *    {@code JsonPointer}, document, but was not allowed for the type encountered
     */
    public ObjectNode withObject(JsonPointer ptr,
            OverwriteMode overwriteMode, boolean preferIndex) {
        // To avoid abstract method, base implementation just fails
        return _reportUnsupportedOperation("`JsonNode` not of type `ObjectNode` (but `"
                +getClass().getName()+")`, cannot call `withObject()` on it");
    }

    /**
     * Method similar to {@link #withObject(JsonPointer, OverwriteMode, boolean)} -- basically
     * short-cut to:
     *<pre>
     *   withObject(JsonPointer.compile("/"+propName), OverwriteMode.NULLS, false);
     *</pre>
     * that is, only matches immediate property on {@link ObjectNode}
     * and will either use an existing {@link ObjectNode} that is
     * value of the property, or create one if no value or value is {@code NullNode}.
     * <br>
     * Will fail with an exception if:
     * <ul>
     *  <li>Node method called on is NOT {@link ObjectNode}
     *   </li>
     *  <li>Property has an existing value that is NOT {@code NullNode} (explicit {@code null})
     *   </li>
     * </ul>
     *
     * @param propName Name of property that has or will have {@link ObjectNode} as value
     *
     * @return {@link ObjectNode} value of given property (existing or created)
     *
     * @since 2.16
     */
    public ObjectNode withObjectProperty(String propName) {
        // To avoid abstract method, base implementation just fails
        return _reportUnsupportedOperation("`JsonNode` not of type `ObjectNode` (but `"
                +getClass().getName()+")`, cannot call `withObjectProperty()` on it");
    }

    /** Short-cut equivalent to:
     *<pre>
     *  withArray(JsonPointer.compile(expr), overwriteMode, preferIndex);
     *</pre>
     */
    public ArrayNode withArray(String exprOrProperty) {
        // To avoid abstract method, base implementation just fails
        return _reportUnsupportedOperation("`JsonNode` not of type `ObjectNode` (but `"
                +getClass().getName()+")`, cannot call `withArray()` on it");
    }

    /**
     * Short-cut equivalent to:
     *<pre>
     *  withArray(JsonPointer.compile(expr), overwriteMode, preferIndex);
     *</pre>
     */
    public ArrayNode withArray(String expr,
            OverwriteMode overwriteMode, boolean preferIndex) {
        return withArray(JsonPointer.compile(expr), overwriteMode, preferIndex);
    }

    /**
     * Same as {@link #withArray(JsonPointer, OverwriteMode, boolean)} but
     * with defaults of {@code OvewriteMode#NULLS} (overwrite mode)
     * and {@code true} for {@code preferIndex}.
     *
     * @param ptr Pointer that indicates path to use for {@link ArrayNode} to return
     *   (potentially creating as necessary)
     *
     * @return {@link ArrayNode} found or created
     */
    public final ArrayNode withArray(JsonPointer ptr) {
        return withArray(ptr, OverwriteMode.NULLS, true);
    }

    /**
     * Method that can be called on Object or Array nodes, to access a Array-valued
     * node pointed to by given {@link JsonPointer}, if such a node exists:
     * or if not, an attempt is made to create one and return it.
     * For example, on document
     *<pre>
     *  { "a" : {
     *       "b" : [ 1, 2 ]
     *    }
     *  }
     *</pre>
     * calling method with {@link JsonPointer} of {@code /a/b} would return
     * {@code Array}
     *<pre>
     *  [ 1, 2 ]
     *</pre>
     *<p>
     * In cases where path leads to "missing" nodes, a path is created.
     * So, for example, on above document, and
     * {@link JsonPointer} of {@code /a/x} an empty {@code ArrayNode} would
     * be returned and the document would look like:
     *<pre>
     *  { "a" : {
     *       "b" : [ 1, 2 ],
     *       "x" : [ ]
     *    }
     *  }
     *</pre>
     * Finally, if the path is incompatible with the document -- there is an existing
     * {@code JsonNode} through which expression cannot go -- a replacement is
     * attempted if (and only if) conversion is allowed as per {@code overwriteMode}
     * passed in. For example, with above document and expression of {@code /a/b/0},
     * conversion is allowed if passing {@code OverwriteMode.SCALARS} or
     * {@code OvewriteMode.ALL}, and resulting document would look like:
     *<pre>
     *  { "a" : {
     *       "b" : [ [ ], 2 ],
     *       "x" : [ ]
     *    }
     *  }
     *</pre>
     * but if different modes ({@code NONE} or {@code NULLS}) is passed, an exception
     * is thrown instead.
     *
     * @param ptr Pointer that indicates path to use for {@link ArrayNode} value to return
     *   (potentially creating it as necessary)
     * @param overwriteMode Defines which node types may be converted in case of
     *    incompatible {@code JsonPointer} expression: if conversion not allowed,
     *    an exception is thrown.
     * @param preferIndex When creating a path (for empty or replacement), and path
     *    contains segment that may be an array index (simple integer number like
     *    {@code 3}), whether to construct an {@link ArrayNode} ({@code true}) or
     *    {@link ObjectNode} ({@code false}). In latter case matching property with
     *    quoted number (like {@code "3"}) is used within Object.
     *
     * @return {@link ArrayNode} found or created
     *
     * @throws JsonNodeException if a conversion would be needed for given
     *    {@code JsonPointer}, document, but was not allowed for the type encountered
     */
    public ArrayNode withArray(JsonPointer ptr,
            OverwriteMode overwriteMode, boolean preferIndex) {
        return _reportUnsupportedOperation(
                "`withArray(JsonPointer)` not implemented by `%s`",
                ClassUtil.nameOf(getClass()));
    }

    /**
     * Method similar to {@link #withArray(JsonPointer, OverwriteMode, boolean)} -- basically
     * short-cut to:
     *<pre>
     *   withArray(JsonPointer.compile("/"+propName), OverwriteMode.NULLS, false);
     *</pre>
     * that is, only matches immediate property on {@link ObjectNode}
     * and will either use an existing {@link ArrayNode} that is
     * value of the property, or create one if no value or value is {@code NullNode}.
     * <br>
     * Will fail with an exception if:
     * <ul>
     *  <li>Node method called on is NOT {@link ObjectNode}
     *   </li>
     *  <li>Property has an existing value that is NOT {@code NullNode} (explicit {@code null})
     *   </li>
     * </ul>
     *
     * @param propName Name of property that has or will have {@link ArrayNode} as value
     *
     * @return {@link ArrayNode} value of given property (existing or created)
     */
    public ArrayNode withArrayProperty(String propName) {
        // To avoid abstract method, base implementation just fails
        return _reportUnsupportedOperation("`JsonNode` not of type `ObjectNode` (but `"
                +getClass().getName()+")`, cannot call `withArrayProperty(String)` on it");
    }

    /*
    /**********************************************************************
    /* Public API, comparison
    /**********************************************************************
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
     */
    public boolean equals(Comparator<JsonNode> comparator, JsonNode other) {
        return comparator.compare(this, other) == 0;
    }

    /*
    /**********************************************************************
    /* Overridden standard methods
    /**********************************************************************
     */

    /**
     * Method that will produce (as of Jackson 2.10) valid JSON using
     * default settings of databind, as String.
     * If you want other kinds of JSON output (or output formatted using one of
     * other Jackson-supported data formats) make sure to use
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
     * Alternative to {@link #toString} that will serialize this node using
     * Jackson default pretty-printer.
     */
    public String toPrettyString() {
        return toString();
    }

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

    /*
    /**********************************************************************
    /* Helper methods,  for sub-classes
    /**********************************************************************
     */

    @SuppressWarnings("unchecked")
    protected <T extends JsonNode> T _this() {
        return (T) this;
    }

    /**
     * Helper method that throws {@link DatabindException} as a result of
     * violating "required-constraint" for this node (for {@link #required} or related
     * methods).
     */
    protected <T> T _reportRequiredViolation(String msgTemplate, Object...args) {
        throw JsonNodeException.from(this, String.format(msgTemplate, args));
    }

    protected <T> T _reportUnsupportedOperation(String msgTemplate, Object...args) {
        throw JsonNodeException.from(this, String.format(msgTemplate, args));
    }
}
