package tools.jackson.databind.ext.javatime;

import java.io.Serial;
import java.time.DateTimeException;

import tools.jackson.core.JsonParser;
import tools.jackson.databind.exc.MismatchedInputException;

/**
 * Specialized exception to be thrown when deserialization of {@code java.time}
 * instances fails due to {@link java.time.DateTimeException} or related exceptions.
 * <p>
 * This exception is used to wrap {@link java.time.DateTimeException}s and
 * {@link ArithmeticException}s that occur during parsing or construction of
 * {@code java.time} objects (like {@code LocalDateTime}, {@code LocalDate},
 * {@code LocalTime}, etc.), providing better error context for Jackson
 * deserialization failures.
 *
 * @since 3.1
 */
public class DateTimeParseException extends MismatchedInputException
{
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * The value that failed to be parsed into a {@code java.time} instance.
     */
    protected final String _value;

    public DateTimeParseException(JsonParser p, String msg, String value,
            Class<?> targetType, Throwable cause)
    {
        super(p, msg, targetType);
        _value = value;
        if (cause != null) {
            initCause(cause);
        }
    }

    /**
     * Factory method for constructing an instance with given arguments.
     *
     * @param p Parser in use when exception occurred
     * @param msg Error message
     * @param value The value that could not be parsed
     * @param targetType Type we attempted to deserialize into
     * @param cause The underlying exception (typically {@link java.time.DateTimeException}
     *              or {@link ArithmeticException})
     *
     * @return New {@link DateTimeParseException} instance
     */
    public static DateTimeParseException from(JsonParser p, String msg,
            String value, Class<?> targetType, Throwable cause)
    {
        return new DateTimeParseException(p, msg, value, targetType, cause);
    }

    /**
     * Accessor for the value that could not be parsed.
     *
     * @return The string value that failed to parse, if available
     */
    public String getValue() {
        return _value;
    }
}
