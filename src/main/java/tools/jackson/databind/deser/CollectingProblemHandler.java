package tools.jackson.databind.deser;

import java.util.ArrayList;
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

/**
 * Stateless {@link DeserializationProblemHandler} that collects recoverable
 * problems into a per-call bucket stored in {@link DeserializationContext}
 * attributes.
 *
 * <p>Designed for use with {@link tools.jackson.databind.ObjectReader#collectErrors()}.
 *
 * @since 3.1
 */
public class CollectingProblemHandler extends DeserializationProblemHandler {

    /**
     * Default maximum number of problems to collect before stopping.
     * Prevents memory exhaustion attacks.
     */
    private static final int DEFAULT_MAX_PROBLEMS = 100;

    /**
     * Unique private key object for the maximum problem limit attribute.
     * Using a dedicated object prevents collisions with user attributes.
     */
    private static final class MaxProblemsKey {
        private MaxProblemsKey() {} // Prevent instantiation
    }
    public static final Object ATTR_MAX_PROBLEMS = new MaxProblemsKey();

    /**
     * Attribute key for the problem collection bucket.
     * Using class object as key (not a string) for type safety.
     */
    private static final Object ATTR_KEY = CollectingProblemHandler.class;

    /**
     * Retrieves the problem collection bucket from context attributes.
     *
     * @return Problem bucket, or null if not in collecting mode
     */
    public static List<CollectedProblem> getBucket(DeserializationContext ctxt) {
        Object attr = ctxt.getAttribute(ATTR_KEY);
        return (attr instanceof List) ? (List<CollectedProblem>) attr : null;
    }

    /**
     * Gets the configured maximum problem limit, or the default if not configured.
     */
    private int getMaxProblems(DeserializationContext ctxt) {
        Object attr = ctxt.getAttribute(ATTR_MAX_PROBLEMS);
        if (attr instanceof Integer) {
            return (Integer) attr;
        }
        return DEFAULT_MAX_PROBLEMS;
    }

    /**
     * Records a problem in the collection bucket.
     *
     * @return true if problem was recorded, false if limit reached
     */
    private boolean recordProblem(DeserializationContext ctxt,
            String message, JavaType targetType, Object rawValue) {
        List<CollectedProblem> bucket = getBucket(ctxt);
        if (bucket == null) {
            return false; // Not in collecting mode
        }

        int maxProblems = getMaxProblems(ctxt);
        if (bucket.size() >= maxProblems) {
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
        try {
            return (p != null) ? p.currentTokenLocation() : null;
        } catch (Exception e) {
            return null; // Defensively handle any errors
        }
    }

    /**
     * Safely retrieves the current token, handling null parser.
     */
    private JsonToken safeGetToken(JsonParser p) {
        try {
            return (p != null) ? p.currentToken() : null;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Builds a JsonPointer from the parser's current context.
     * Handles buffered content scenarios where getCurrentName() may return null.
     * Returns empty pointer ("") for root-level problems.
     *
     * <p>Implements RFC 6901 escaping:
     * <ul>
     * <li>'~' becomes '~0'</li>
     * <li>'/' becomes '~1'</li>
     * </ul>
     */
    private JsonPointer buildJsonPointer(JsonParser p) {
        if (p == null) {
            return JsonPointer.compile("");
        }

        // Use parsing context to build robust path
        TokenStreamContext ctx = p.streamReadContext();
        List<String> segments = new ArrayList<>();

        while (ctx != null) {
            if (ctx.inObject() && ctx.currentName() != null) {
                // Escape property name per RFC 6901
                segments.add(0, escapeJsonPointerSegment(ctx.currentName()));
            } else if (ctx.inArray()) {
                // getCurrentIndex() may be -1 before consuming first element
                int index = ctx.getCurrentIndex();
                if (index >= 0) {
                    segments.add(0, String.valueOf(index));
                }
            }
            ctx = ctx.getParent();
        }

        // Return empty pointer for root, not "/"
        if (segments.isEmpty()) {
            return JsonPointer.compile("");
        }

        return JsonPointer.compile("/" + String.join("/", segments));
    }

    /**
     * Escapes a JSON Pointer segment per RFC 6901.
     * Must escape '~' before '/' to avoid double-escaping.
     *
     * @param segment The raw segment (property name or array index)
     * @return Escaped segment safe for JSON Pointer
     */
    private String escapeJsonPointerSegment(String segment) {
        if (segment == null) {
            return null;
        }
        // Order matters: escape ~ first, then /
        // Otherwise "~" -> "~0" -> "~01" (wrong!)
        return segment.replace("~", "~0").replace("/", "~1");
    }

    @Override
    public boolean handleUnknownProperty(DeserializationContext ctxt,
            JsonParser p, ValueDeserializer<?> deserializer,
            Object beanOrClass, String propertyName) throws JacksonException {

        String message = String.format(
            "Unknown property '%s' for type %s",
            propertyName,
            beanOrClass instanceof Class ?
                ((Class<?>) beanOrClass).getName() :
                beanOrClass.getClass().getName()
        );

        // Store null as rawValue for unknown properties
        // (property name is in the path, no need to duplicate)
        if (recordProblem(ctxt, message, null, null)) {
            p.skipChildren(); // Skip the unknown property value
            return true; // Problem handled
        }

        return false; // Limit reached, let default handling throw
    }

    @Override
    public Object handleWeirdKey(DeserializationContext ctxt,
            Class<?> rawKeyType, String keyValue, String failureMsg)
            throws JacksonException {

        String message = String.format(
            "Cannot deserialize Map key '%s' to %s: %s",
            keyValue,
            rawKeyType.getSimpleName(),
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

        return NOT_HANDLED; // Limit reached
    }

    @Override
    public Object handleWeirdStringValue(DeserializationContext ctxt,
            Class<?> targetType, String valueToConvert, String failureMsg)
            throws JacksonException {

        String message = String.format(
            "Cannot deserialize value '%s' to %s: %s",
            valueToConvert,
            targetType.getSimpleName(),
            failureMsg
        );

        if (recordProblem(ctxt, message,
                ctxt.constructType(targetType), valueToConvert)) {
            // Return sensible default based on target type
            return getDefaultValue(targetType);
        }

        return NOT_HANDLED; // Limit reached
    }

    @Override
    public Object handleWeirdNumberValue(DeserializationContext ctxt,
            Class<?> targetType, Number valueToConvert, String failureMsg)
            throws JacksonException {

        String message = String.format(
            "Cannot deserialize number %s to %s: %s",
            valueToConvert,
            targetType.getSimpleName(),
            failureMsg
        );

        if (recordProblem(ctxt, message,
                ctxt.constructType(targetType), valueToConvert)) {
            return getDefaultValue(targetType);
        }

        return NOT_HANDLED;
    }

    @Override
    public Object handleInstantiationProblem(DeserializationContext ctxt,
            Class<?> instClass, Object argument, Throwable t)
            throws JacksonException {

        String message = String.format(
            "Cannot instantiate %s: %s",
            instClass.getSimpleName(),
            t.getMessage()
        );

        if (recordProblem(ctxt, message,
                ctxt.constructType(instClass), argument)) {
            // Only return null if we can safely continue
            // For some types, instantiation failure is fatal
            if (canReturnNullFor(instClass)) {
                return null;
            }
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
        if (type == int.class) return 0;
        if (type == long.class) return 0L;
        if (type == double.class) return 0.0;
        if (type == float.class) return 0.0f;
        if (type == boolean.class) return false;
        if (type == byte.class) return (byte) 0;
        if (type == short.class) return (short) 0;
        if (type == char.class) return '\0';

        // Reference types (including Integer, Long, etc.) get null
        // This avoids masking nullability issues in the domain model
        return null;
    }

    /**
     * Checks if it's safe to return null for a given type after
     * instantiation failure.
     */
    private boolean canReturnNullFor(Class<?> type) {
        // Cannot return null for primitives or arrays
        if (type.isPrimitive() || type.isArray()) {
            return false;
        }
        // Safe for most reference types
        return true;
    }
}
