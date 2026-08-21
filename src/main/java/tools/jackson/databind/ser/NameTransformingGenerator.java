package tools.jackson.databind.ser;

import tools.jackson.core.*;
import tools.jackson.core.util.JsonGeneratorDelegate;
import tools.jackson.databind.util.NameTransformer;

/**
 * A {@link JsonGenerator} wrapper that applies a {@link NameTransformer} to
 * field names written via {@link #writeName(String)} and
 * {@link #writeName(SerializableString)}.
 * This allows existing serialization code (e.g. {@link MapSerializer}) to produce
 * transformed key names without any special-case logic: the generator intercepts
 * every field-name write and applies the prefix/suffix transformation transparently.
 *<p>
 * Only names written at the nesting level at which this wrapper was installed are
 * transformed: names written inside nested Objects (that is, the properties of an
 * entry's value, including any type ids written for it) are passed through as-is.
 *
 * @since 3.3
 */
class NameTransformingGenerator extends JsonGeneratorDelegate
{
    private final NameTransformer _transformer;

    /**
     * Nesting depth relative to the point at which this wrapper was installed;
     * names are only transformed while this is {@code 0}.
     */
    private int _depth;

    NameTransformingGenerator(JsonGenerator gen, NameTransformer transformer) {
        super(gen, false);
        _transformer = transformer;
    }

    @Override
    public JsonGenerator writeName(String name) throws JacksonException {
        delegate.writeName((_depth == 0) ? _transformer.transform(name) : name);
        return this;
    }

    @Override
    public JsonGenerator writeName(SerializableString name) throws JacksonException {
        if (_depth == 0) {
            delegate.writeName(_transformer.transform(name.getValue()));
        } else {
            delegate.writeName(name);
        }
        return this;
    }

    /*
    /**********************************************************************
    /* Nesting depth tracking
    /**********************************************************************
     */

    @Override
    public JsonGenerator writeStartObject() throws JacksonException {
        ++_depth;
        delegate.writeStartObject();
        return this;
    }

    @Override
    public JsonGenerator writeStartObject(Object forValue) throws JacksonException {
        ++_depth;
        delegate.writeStartObject(forValue);
        return this;
    }

    @Override
    public JsonGenerator writeStartObject(Object forValue, int size) throws JacksonException {
        ++_depth;
        delegate.writeStartObject(forValue, size);
        return this;
    }

    @Override
    public JsonGenerator writeEndObject() throws JacksonException {
        --_depth;
        delegate.writeEndObject();
        return this;
    }

    @Override
    public JsonGenerator writeStartArray() throws JacksonException {
        ++_depth;
        delegate.writeStartArray();
        return this;
    }

    @Override
    public JsonGenerator writeStartArray(Object forValue) throws JacksonException {
        ++_depth;
        delegate.writeStartArray(forValue);
        return this;
    }

    @Override
    public JsonGenerator writeStartArray(Object forValue, int size) throws JacksonException {
        ++_depth;
        delegate.writeStartArray(forValue, size);
        return this;
    }

    @Override
    public JsonGenerator writeEndArray() throws JacksonException {
        --_depth;
        delegate.writeEndArray();
        return this;
    }
}
