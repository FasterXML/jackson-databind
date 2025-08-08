package tools.jackson.databind.exc;

import tools.jackson.core.*;
import tools.jackson.databind.DatabindException;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.util.ClassUtil;

/**
 * General exception type used as the base class for all {@link DatabindException}s
 * that are due to input not mapping to target definition; these are typically
 * considered "client errors" since target type definition itself is not the root cause
 * but mismatching input. This is in contrast to {@link InvalidDefinitionException} which
 * signals a problem with target type definition and not input.
 *<p>
 * This type is used as-is for some input problems, but in most cases there should be
 * more explicit subtypes to use.
 *<p>
 * NOTE: name chosen to differ from `java.util.InputMismatchException` since while that
 * would have been better name, use of same overlapping name causes nasty issues
 * with IDE auto-completion, so slightly less optimal chosen.
 */
public class MismatchedInputException
    extends DatabindException
{
    private static final long serialVersionUID = 3L;

    /**
     * Type of value that was to be deserialized
     */
    protected Class<?> _targetType;

    /**
     * Current token at the point when exception was thrown (if available).
     */
    protected JsonToken _currentToken;

    protected MismatchedInputException(JsonParser p, String msg) {
        this(p, msg, (JavaType) null);
    }

    protected MismatchedInputException(JsonParser p, String msg, TokenStreamLocation loc) {
        super(p, msg, loc);
        _currentToken = _currentToken(p);
    }

    protected MismatchedInputException(JsonParser p, String msg, Class<?> targetType) {
        super(p, msg);
        _targetType = targetType;
        _currentToken = _currentToken(p);
    }

    protected MismatchedInputException(JsonParser p, String msg, JavaType targetType) {
        super(p, msg);
        _targetType = ClassUtil.rawClass(targetType);
        _currentToken = _currentToken(p);
    }

    protected static JsonToken _currentToken(JsonParser p) {
         return (p == null) ? null : p.currentToken();
    }

    public static MismatchedInputException from(JsonParser p, JavaType targetType, String msg) {
        return new MismatchedInputException(p, msg, targetType);
    }

    public static MismatchedInputException from(JsonParser p, Class<?> targetType, String msg) {
        return new MismatchedInputException(p, msg, targetType);
    }

    public MismatchedInputException setTargetType(JavaType t) {
        _targetType = ClassUtil.rawClass(t);
        return this;
    }

    public MismatchedInputException setCurrentToken(JsonToken t) {
        _currentToken = t;
        return this;
    }

    /**
     * Accessor for getting intended target type, with which input did not match,
     * if known; `null` if not known for some reason.
     */
    public Class<?> getTargetType() {
        return _targetType;
    }

    /**
     * @return Current token at the point when exception was thrown, if available
     *   ({@code null} if not)
     */
    public JsonToken getCurrentToken() {
        return _currentToken;
    }
}
