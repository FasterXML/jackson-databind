package tools.jackson.databind;

import tools.jackson.databind.cfg.MapperConfig;
import tools.jackson.databind.introspect.AnnotatedField;
import tools.jackson.databind.introspect.AnnotatedMethod;
import tools.jackson.databind.introspect.AnnotatedParameter;

/**
 * Class that defines how names of JSON properties ("external names")
 * are derived from names of POJO methods and fields ("internal names"),
 * in cases where no explicit annotations exist for naming.
 * Methods are passed information about POJO member for which name is needed,
 * as well as default name that would be used if no custom strategy was used.
 *<p>
 * Default (empty) implementation returns suggested ("implicit" or "default") name unmodified
 *<p>
 * Note that the strategy is guaranteed to be called once per logical property
 * (which may be represented by multiple members; such as pair of a getter and
 * a setter), but may be called for each: implementations should not count on
 * exact number of times, and should work for any member that represent a
 * property.
 * Also note that calls are made during construction of serializers and deserializers
 * which are typically cached, and not for every time serializer or deserializer
 * is called.
 *<p>
 * In absence of a registered custom strategy, the default Java property naming strategy
 * is used, which leaves field names as is, and removes set/get/is prefix
 * from methods (as well as lower-cases initial sequence of capitalized
 * characters).
 */
public class PropertyNamingStrategy
    implements java.io.Serializable
{
    private static final long serialVersionUID = 2L;

    /*
    /**********************************************************
    /* API
    /**********************************************************
     */

    /**
     * Method called to find external name (name used in JSON) for given logical
     * POJO property,
     * as defined by given field.
     *
     * @param config Configuration in used: either <code>SerializationConfig</code>
     *   or <code>DeserializationConfig</code>, depending on whether method is called
     *   during serialization or deserialization
     * @param field Field used to access property
     * @param defaultName Default name that would be used for property in absence of custom strategy
     *
     * @return Logical name to use for property that the field represents
     */
    public String nameForField(MapperConfig<?> config, AnnotatedField field,
            String defaultName)
    {
        return defaultName;
    }

    /**
     * Method called to find external name (name used in JSON) for given logical
     * POJO property,
     * as defined by given getter method; typically called when building a serializer.
     * (but not always -- when using "getter-as-setter", may be called during
     * deserialization)
     *
     * @param config Configuration in used: either <code>SerializationConfig</code>
     *   or <code>DeserializationConfig</code>, depending on whether method is called
     *   during serialization or deserialization
     * @param method Method used to access property.
     * @param defaultName Default name that would be used for property in absence of custom strategy
     *
     * @return Logical name to use for property that the method represents
     */
    public String nameForGetterMethod(MapperConfig<?> config, AnnotatedMethod method,
            String defaultName)
    {
        return defaultName;
    }

    /**
     * Method called to find external name (name used in JSON) for given logical
     * POJO property,
     * as defined by given setter method; typically called when building a deserializer
     * (but not necessarily only then).
     *
     * @param config Configuration in used: either <code>SerializationConfig</code>
     *   or <code>DeserializationConfig</code>, depending on whether method is called
     *   during serialization or deserialization
     * @param method Method used to access property.
     * @param defaultName Default name that would be used for property in absence of custom strategy
     *
     * @return Logical name to use for property that the method represents
     */
    public String nameForSetterMethod(MapperConfig<?> config, AnnotatedMethod method,
            String defaultName)
    {
        return defaultName;
    }

    /**
     * Method called to find external name (name used in JSON) for given logical
     * POJO property,
     * as defined by given constructor parameter; typically called when building a deserializer
     * (but not necessarily only then).
     *
     * @param config Configuration in used: either <code>SerializationConfig</code>
     *   or <code>DeserializationConfig</code>, depending on whether method is called
     *   during serialization or deserialization
     * @param ctorParam Constructor parameter used to pass property.
     * @param defaultName Default name that would be used for property in absence of custom strategy
     */
    public String nameForConstructorParameter(MapperConfig<?> config, AnnotatedParameter ctorParam,
            String defaultName)
    {
        return defaultName;
    }
}
