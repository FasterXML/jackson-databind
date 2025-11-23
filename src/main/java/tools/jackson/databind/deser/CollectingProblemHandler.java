package tools.jackson.databind.deser;

import java.util.List;

import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonPointer;
import tools.jackson.core.JsonToken;
import tools.jackson.core.TokenStreamContext;
import tools.jackson.core.TokenStreamLocation;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.ValueDeserializer;
import tools.jackson.databind.exc.CollectedProblem;
import tools.jackson.databind.util.ClassUtil;

/**
 * Stateless {@link DeserializationProblemHandler} that collects recoverable
 * problems into a per-call bucket stored in {@link DeserializationContext}
 * attributes.
 *
 * <p><b>Design</b>: This handler is completely stateless. The problem collection
 * bucket is allocated per-call by {@code ObjectReader.readValueCollectingProblems(...)}
 * and stored in per-call context attributes, ensuring thread-safety and call isolation.
 *
 * <p><b>Usage</b>: This class is internal infrastructure, registered automatically by
 * {@code ObjectReader.problemCollectingReader()}. Users should not instantiate or
 * register this handler manually.
 *
 * <p><b>Design rationale - Context Attributes vs Handler State</b>:
 *
 * <p>Problem collection state is stored in {@link DeserializationContext} attributes
 * rather than within this handler for several reasons:
 *
 * <ol>
 * <li><b>Thread-safety</b>: The handler instance is shared across all calls to the
 *     same ObjectReader. Storing mutable state in the handler would require
 *     synchronization and complicate the implementation.</li>
 *
 * <li><b>Call isolation</b>: Each call to {@code readValueCollectingProblems()} needs
 *     its own problem bucket. Context attributes are perfect for this - they're created
 *     per-call and automatically cleaned up after deserialization.</li>
 *
 * <li><b>Immutability</b>: Jackson's config objects (including handlers) are designed
 *     to be immutable and reusable. Storing per-call state violates this principle.</li>
 *
 * <li><b>Configuration vs State</b>: The handler stores configuration (max problems
 *     limit) while attributes store runtime state (the actual problem list). This
 *     separation follows Jackson's design patterns.</li>
 * </ol>
 *
 * <p>The handler itself is stateless - it's just a strategy for handling problems.
 * The actual collection happens in a bucket passed through context attributes.
 *
 * <p><b>Recoverable errors handled</b>:
 * <ul>
 * <li>Unknown properties ({@link #handleUnknownProperty handleUnknownProperty}) - skips children</li>
 * <li>Type coercion failures ({@link #handleWeirdStringValue handleWeirdStringValue},
 *     {@link #handleWeirdNumberValue handleWeirdNumberValue}) - returns defaults</li>
 * <li>Map key coercion ({@link #handleWeirdKey handleWeirdKey}) - returns {@code NOT_HANDLED}</li>
 * <li>Instantiation failures ({@link #handleInstantiationProblem handleInstantiationProblem}) -
 *     returns null when safe</li>
 * </ul>
 *
 * <p><b>Default values</b>: Primitives receive zero/false defaults; reference types
 * (including boxed primitives) receive {@code null} to avoid masking nullability issues.
 *
 * <p><b>DoS protection</b>: Collection stops when the configured limit (default 100)
 * is reached, preventing memory/CPU exhaustion attacks.
 *
 * <p><b>JSON Pointer</b>: Paths are built from parser context following RFC 6901,
 * with proper escaping of {@code ~} and {@code /} characters via jackson-core's
 * {@link JsonPointer} class.
 *
 * @since 3.1
 */
public class CollectingProblemHandler extends DeserializationProblemHandler
{
    /**
     * Default maximum number of problems to collect before stopping.
     * Prevents memory exhaustion attacks.
     */
    public static final int DEFAULT_MAX_PROBLEMS = 100;

    /**
     * Attribute key for the problem collection bucket.
     * Using class object as key (not a string) for type safety.
     */
    private static final Object ATTR_KEY = CollectingProblemHandler.class;

    /**
     * Maximum number of problems to collect before stopping.
     */
    private final int _maxProblems;

    /**
     * Constructs a handler with the default maximum problem limit.
     */
    public CollectingProblemHandler() {
        this(DEFAULT_MAX_PROBLEMS);
    }

    /**
     * Constructs a handler with a specific maximum problem limit.
     *
     * @param maxProblems Maximum number of problems to collect (must be positive)
     */
    public CollectingProblemHandler(int maxProblems) {
        if (maxProblems <= 0) {
            throw new IllegalArgumentException("maxProblems must be positive, was: " + maxProblems);
        }
        _maxProblems = maxProblems;
    }

    /**
     * Gets the maximum number of problems this handler will collect.
     */
    public int getMaxProblems() {
        return _maxProblems;
    }

    /**
     * Retrieves the problem collection bucket from context attributes.
     *
     * @return Problem bucket, or null if not in collecting mode
     */
    @SuppressWarnings("unchecked")
    public static List<CollectedProblem> getBucket(DeserializationContext ctxt) {
        Object attr = ctxt.getAttribute(ATTR_KEY);
        return (attr instanceof List) ? (List<CollectedProblem>) attr : null;
    }

    /**
     * Records a problem in the collection bucket.
     *
     * @return true if problem was recorded, false if limit reached
     */
    private boolean recordProblem(DeserializationContext ctxt,
            String message, JavaType targetType, Object rawValue)
    {
        List<CollectedProblem> bucket = getBucket(ctxt);
        if (bucket == null) {
            return false; // Not in collecting mode
        }

        if (bucket.size() >= _maxProblems) {
            return false; // Limit reached
        }

        JsonParser p = ctxt.getParser();
        JsonPointer path = buildJsonPointer(p);
        TokenStreamLocation location = safeGetLocation(p);
        JsonToken token = safeGetToken(p);

        bucket.add(new CollectedProblem(
            path, message, targetType, location, rawValue, token
        ));

        return true;
    }

    /**
     * Safely retrieves the current token location, handling null parser.
     */
    private TokenStreamLocation safeGetLocation(JsonParser p) {
        return (p != null) ? p.currentTokenLocation() : null;
    }

    /**
     * Safely retrieves the current token, handling null parser.
     */
    private JsonToken safeGetToken(JsonParser p) {
        return (p != null) ? p.currentToken() : null;
    }

    /**
     * Builds a JsonPointer from the parser's current context.
     * Uses the built-in {@link TokenStreamContext#pathAsPointer()} method
     * which handles RFC 6901 escaping ('~' becomes '~0', '/' becomes '~1').
     */
    private JsonPointer buildJsonPointer(JsonParser p) {
        if (p == null) {
            return JsonPointer.empty();
        }
        return p.streamReadContext().pathAsPointer();
    }

    @Override
    public boolean handleUnknownProperty(DeserializationContext ctxt,
            JsonParser p, ValueDeserializer<?> deserializer,
            Object beanOrClass, String propertyName)
        throws JacksonException
    {
        String message = String.format(
            "Unknown property '%s' for type %s",
            propertyName,
            ClassUtil.getClassDescription(beanOrClass)
        );

        // Store null as rawValue for unknown properties
        // (property name is in the path, no need to duplicate)
        if (recordProblem(ctxt, message, null, null)) {
            p.skipChildren(); // Skip the unknown property value
            return true; // Problem handled
        }

        return false; // Limit reached or not collecting, let default handling throw
    }

    @Override
    public Object handleWeirdKey(DeserializationContext ctxt,
            Class<?> rawKeyType, String keyValue, String failureMsg)
        throws JacksonException
    {
        String message = String.format(
            "Cannot deserialize Map key '%s' to %s: %s",
            keyValue,
            ClassUtil.getClassDescription(rawKeyType),
            failureMsg
        );

        if (recordProblem(ctxt, message,
                ctxt.constructType(rawKeyType), keyValue)) {
            // Return NOT_HANDLED instead of null
            // Rationale: Some Map implementations (Hashtable, ConcurrentHashMap)
            // reject null keys. Safer to let Jackson handle it than risk NPE.
            // If null keys are needed, users can provide custom handler.
            return NOT_HANDLED;
        }

        return NOT_HANDLED; // Limit reached or not collecting
    }

    @Override
    public Object handleWeirdStringValue(DeserializationContext ctxt,
            Class<?> targetType, String valueToConvert, String failureMsg)
        throws JacksonException
    {
        String message = String.format(
            "Cannot deserialize value '%s' to %s: %s",
            valueToConvert,
            ClassUtil.getClassDescription(targetType),
            failureMsg
        );

        if (recordProblem(ctxt, message,
                ctxt.constructType(targetType), valueToConvert)) {
            // Return sensible default based on target type
            return getDefaultValue(targetType);
        }

        return NOT_HANDLED; // Limit reached or not collecting
    }

    @Override
    public Object handleWeirdNumberValue(DeserializationContext ctxt,
            Class<?> targetType, Number valueToConvert, String failureMsg)
        throws JacksonException
    {
        String message = String.format(
            "Cannot deserialize number %s to %s: %s",
            valueToConvert,
            ClassUtil.getClassDescription(targetType),
            failureMsg
        );

        if (recordProblem(ctxt, message,
                ctxt.constructType(targetType), valueToConvert)) {
            return getDefaultValue(targetType);
        }

        return NOT_HANDLED; // Limit reached or not collecting
    }

    @Override
    public Object handleInstantiationProblem(DeserializationContext ctxt,
            Class<?> instClass, Object argument, Throwable t)
        throws JacksonException
    {
        String message = String.format(
            "Cannot instantiate %s: %s",
            ClassUtil.getClassDescription(instClass),
            t.getMessage()
        );

        if (recordProblem(ctxt, message,
                ctxt.constructType(instClass), argument)) {
            // Cannot return null for primitives; safe for all reference types
            if (!instClass.isPrimitive()) {
                return null;
            }
            // fall through
        }

        return NOT_HANDLED; // Cannot recover
    }

    /**
     * Returns a sensible default value for the given type to allow
     * deserialization to continue.
     *
     * <p>IMPORTANT: Only primitives get non-null defaults. Reference types
     * (including boxed primitives) get null to avoid masking nullability issues.
     */
    private Object getDefaultValue(Class<?> type) {
        // Primitives MUST have non-null defaults (cannot be null)
        // Use ClassUtil for consistent primitive default handling
        if (type.isPrimitive()) {
            return ClassUtil.defaultValue(type);
        }

        // Reference types (including Integer, Long, etc.) get null
        // This avoids masking nullability issues in the domain model
        return null;
    }
}
