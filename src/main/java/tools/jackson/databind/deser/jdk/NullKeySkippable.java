package tools.jackson.databind.deser.jdk;

import tools.jackson.databind.DeserializationContext;

/**
 * Internal capability interface for key deserializers that can signal
 * null keys should be skipped during Map deserialization.
 *<p>
 * This abstraction exists so that
 * {@link tools.jackson.databind.deser.jdk.MapDeserializer} does not need to know
 * about specific key types (like enums) or their configuration options.
 * The key deserializer itself decides whether null keys warrant skipping the entry.
 *<p>
 * Package-private: not part of public API.
 *
 * @since 3.1
 */
interface NullKeySkippable {
    /**
     * @param ctxt Deserialization context
     * @return {@code true} if a null key (from an unrecognized value) should cause
     *         the entire map entry to be skipped
     */
    boolean skipNullKeys(DeserializationContext ctxt);
}