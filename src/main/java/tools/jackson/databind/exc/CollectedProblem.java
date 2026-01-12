package tools.jackson.databind.exc;

import java.util.Objects;

import tools.jackson.core.JsonPointer;
import tools.jackson.core.JsonToken;
import tools.jackson.core.TokenStreamLocation;
import tools.jackson.databind.JavaType;

/**
 * Immutable value object capturing details about a single deserialization
 * problem encountered during error-collecting mode.
 *
 * <p><b>Contents</b>: Each problem records:
 * <ul>
 * <li>{@link #getPath() path} - RFC 6901 JSON Pointer to the problematic field
 *     (e.g., {@code "/items/2/price"})</li>
 * <li>{@link #getMessage() message} - Human-readable error description</li>
 * <li>{@link #getTargetType() targetType} - Expected Java type (may be null)</li>
 * <li>{@link #getLocation() location} - Source location in JSON (line/column)</li>
 * <li>{@link #getRawValue() rawValue} - Original value from JSON that caused the error
 *     (truncated if > 200 chars)</li>
 * <li>{@link #getToken() token} - JSON token type at error location</li>
 * </ul>
 *
 * <p><b>Truncation</b>: String values longer than {@code #MAX_RAW_VALUE_LENGTH}
 * (currently: 200)
 * characters are truncated with "..." suffix to prevent memory issues.
 *
 * <p><b>Unknown properties</b>: For unknown property errors, {@code rawValue}
 * is {@code null} since the property name is already in the path.
 *
 * <p><b>Immutability</b>: All instances are immutable and thread-safe.
 *
 * @since 3.1
 * @see DeferredBindingException#getProblems()
 */
public final class CollectedProblem {
    /**
     * Maximum length for raw value strings before truncation.
     */
    private static final int MAX_RAW_VALUE_LENGTH = 200;

    private final JsonPointer path;
    private final String message;
    private final JavaType targetType;
    private final TokenStreamLocation location;
    private final Object rawValue; // @Nullable
    private final JsonToken token; // @Nullable

    public CollectedProblem(JsonPointer path, String message,
            JavaType targetType, TokenStreamLocation location,
            Object rawValue, JsonToken token) {
        this.path = Objects.requireNonNull(path, "path");
        this.message = Objects.requireNonNull(message, "message");
        this.targetType = targetType;
        this.location = location;
        this.rawValue = truncateIfNeeded(rawValue);
        this.token = token;
    }

    /**
     * @return JSON Pointer path to the problematic field (e.g., "/items/1/date").
     *         Empty string ("") for root-level problems.
     */
    public JsonPointer getPath() { return path; }

    /**
     * @return Human-readable error message
     */
    public String getMessage() { return message; }

    /**
     * @return Expected Java type for the field (may be null)
     */
    public JavaType getTargetType() { return targetType; }

    /**
     * @return Location in source JSON where problem occurred (may be null)
     */
    public TokenStreamLocation getLocation() { return location; }

    /**
     * @return Raw value from JSON that caused the problem (may be null or truncated).
     *         For unknown properties, this is null; use the path to identify the property name.
     */
    public Object getRawValue() { return rawValue; }

    /**
     * @return JSON token type at the error location (may be null)
     */
    public JsonToken getToken() { return token; }

    private static Object truncateIfNeeded(Object value) {
        if (value instanceof String) {
            String s = (String) value;
            if (s.length() > MAX_RAW_VALUE_LENGTH) {
                return s.substring(0, MAX_RAW_VALUE_LENGTH - 3) + "...";
            }
        }
        return value;
    }

    @Override
    public String toString() {
        return String.format("CollectedProblem[path=%s, message=%s, targetType=%s]",
            path, message, targetType);
    }
}
