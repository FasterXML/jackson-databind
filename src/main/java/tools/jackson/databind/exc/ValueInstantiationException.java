package tools.jackson.databind.exc;

import tools.jackson.core.JsonParser;
import tools.jackson.databind.DatabindException;
import tools.jackson.databind.JavaType;

/**
 * Exception type used for generic failures during processing by
 * {@link tools.jackson.databind.deser.ValueInstantiator}:
 * commonly used to wrap exceptions thrown by constructor or factory
 * method.
 *<p>
 * Note that this type is sibling of {@link MismatchedInputException} and
 * {@link InvalidDefinitionException} since it is not clear if problem is
 * with input, or type definition (or possibly neither).
 * It is recommended that if either specific input, or type definition problem
 * is known, a more accurate exception is used instead.
 */
@SuppressWarnings("serial")
public class ValueInstantiationException
    extends DatabindException
{
    protected final JavaType _type;

    protected ValueInstantiationException(JsonParser p, String msg,
            JavaType type, Throwable cause) {
        super(p, msg, cause);
        _type = type;
    }

    protected ValueInstantiationException(JsonParser p, String msg,
            JavaType type) {
        super(p, msg);
        _type = type;
    }

    public static ValueInstantiationException from(JsonParser p, String msg,
            JavaType type) {
        return new ValueInstantiationException(p, msg, type);
    }

    public static ValueInstantiationException from(JsonParser p, String msg,
            JavaType type, Throwable cause) {
        return new ValueInstantiationException(p, msg, type, cause);
    }

    /**
     * Accessor for type fully resolved type that had the problem; this should always
     * known and available, never <code>null</code>
     */
    public JavaType getType() {
        return _type;
    }
}
