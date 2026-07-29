package tools.jackson.databind.ser;

import tools.jackson.core.*;
import tools.jackson.core.util.JsonGeneratorDelegate;
import tools.jackson.databind.util.NameTransformer;

/**
 * A {@link JsonGenerator} wrapper that applies a {@link NameTransformer} to
 * field names written via {@link #writeName(String)} and
 * {@link #writeName(SerializableString)}. This allows existing serialization
 * code (e.g. {@link MapSerializer}) to produce transformed key names without
 * any special-case logic: the generator intercepts every field-name write and
 * applies the prefix/suffix transformation transparently.
 *
 * @since 3.3
 */
class NameTransformingGenerator extends JsonGeneratorDelegate
{
    private final NameTransformer _transformer;

    NameTransformingGenerator(JsonGenerator gen, NameTransformer transformer) {
        super(gen, false);
        _transformer = transformer;
    }

    @Override
    public JsonGenerator writeName(String name) throws JacksonException {
        delegate.writeName(_transformer.transform(name));
        return this;
    }

    @Override
    public JsonGenerator writeName(SerializableString name) throws JacksonException {
        delegate.writeName(_transformer.transform(name.getValue()));
        return this;
    }
}
