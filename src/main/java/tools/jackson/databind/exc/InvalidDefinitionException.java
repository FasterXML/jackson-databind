package tools.jackson.databind.exc;

import tools.jackson.core.JsonGenerator;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.BeanDescription;
import tools.jackson.databind.DatabindException;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.introspect.BeanPropertyDefinition;

/**
 * Intermediate exception type used as the base class for all {@link DatabindException}s
 * that are due to problems with target type definition; usually a problem with
 * annotations used on a class or its properties.
 * This is in contrast to {@link MismatchedInputException} which
 * signals a problem with input to map.
 */
@SuppressWarnings("serial")
public class InvalidDefinitionException
    extends DatabindException
{
    protected final JavaType _type;

    protected transient BeanDescription _beanDesc;
    protected transient BeanPropertyDefinition _property;

    protected InvalidDefinitionException(JsonParser p, String msg,
            JavaType type) {
        super(p, msg);
        _type = type;
        _beanDesc = null;
        _property = null;
    }

    protected InvalidDefinitionException(JsonGenerator g, String msg,
            JavaType type) {
        super(g, msg);
        _type = type;
        _beanDesc = null;
        _property = null;
    }

    protected InvalidDefinitionException(JsonParser p, String msg,
            BeanDescription bean, BeanPropertyDefinition prop) {
        super(p, msg);
        _type = (bean == null) ? null : bean.getType();
        _beanDesc = bean;
        _property = prop;
    }

    protected InvalidDefinitionException(JsonGenerator g, String msg,
            BeanDescription bean, BeanPropertyDefinition prop) {
        super(g, msg);
        _type = (bean == null) ? null : bean.getType();
        _beanDesc = bean;
        _property = prop;
    }

    public static InvalidDefinitionException from(JsonParser p, String msg,
            BeanDescription bean, BeanPropertyDefinition prop) {
        return new InvalidDefinitionException(p, msg, bean, prop);
    }

    public static InvalidDefinitionException from(JsonParser p, String msg,
            JavaType type) {
        return new InvalidDefinitionException(p, msg, type);
    }

    public static InvalidDefinitionException from(JsonGenerator g, String msg,
            BeanDescription bean, BeanPropertyDefinition prop) {
        return new InvalidDefinitionException(g, msg, bean, prop);
    }

    public static InvalidDefinitionException from(JsonGenerator g, String msg,
            JavaType type) {
        return new InvalidDefinitionException(g, msg, type);
    }

    /**
     * Accessor for type fully resolved type that had the problem; this should always
     * known and available, never <code>null</code>
     */
    public JavaType getType() {
        return _type;
    }

    /**
     * Accessor for type definition (class) that had the definition problem, if any; may sometimes
     * be undefined or unknown; if so, returns <code>null</code>.
     */
    public BeanDescription getBeanDescription() {
        return _beanDesc;
    }

    /**
     * Accessor for property that had the definition problem if any
     * (none, for example if the problem relates to type in general),
     * if known. If not known (or relevant), returns <code>null</code>.
     */
    public BeanPropertyDefinition getProperty() {
        return _property;
    }
}
