package tools.jackson.databind.cfg;

import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationConfig;

/**
 * Extension point for configuring a callback that is invoked immediately after
 * a {@link JsonGenerator} is constructed by {@link tools.jackson.databind.ObjectMapper}
 * or {@link tools.jackson.databind.ObjectWriter}, before the generator is used for
 * serialization or returned to the caller.
 *<p>
 * The main use case is for format backends (such as XML) that need to initialize
 * generator state (like writing an XML declaration) before content is written,
 * but it can be useful for any format backend that needs generator-level initialization.
 *<p>
 * Instances can be configured via:
 * <ul>
 *  <li>{@link MapperBuilder#generatorInitializer(GeneratorInitializer)} during mapper construction</li>
 *  <li>{@link tools.jackson.databind.ObjectWriter#with(GeneratorInitializer)} for per-writer configuration</li>
 * </ul>
 *
 * @since 3.2
 */
@FunctionalInterface
public interface GeneratorInitializer
{
    /**
     * Method called immediately after a {@link JsonGenerator} is constructed,
     * before it is used for serialization or returned to the caller.
     *
     * @param config Active serialization configuration
     * @param g Newly constructed generator to initialize
     *
     * @throws JacksonException if initialization fails
     */
    public void initialize(SerializationConfig config, JsonGenerator g)
        throws JacksonException;
}
