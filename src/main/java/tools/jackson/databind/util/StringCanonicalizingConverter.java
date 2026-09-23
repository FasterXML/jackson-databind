package tools.jackson.databind.util;

import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.SerializationContext;

/**
 * Converter that canonicalizes {@link String} values using databind's default
 * String canonicalization method.
 *<p>
 * Can be used with {@link tools.jackson.databind.annotation.JsonDeserialize#converter()}
 * or {@link tools.jackson.databind.annotation.JsonDeserialize#contentConverter()}
 * to canonicalize low-cardinality String values during deserialization.
 * Canonicalization only makes sense on deserialization, so if used on the
 * serialization side (via {@link tools.jackson.databind.annotation.JsonSerialize#converter()})
 * values are passed through as-is.
 *<p>
 * WARNING: the default canonicalization is implemented using {@link String#intern()},
 * the effects of which are JVM-wide and not limited to the on-going read.
 * Because of this, this Converter should only be applied to properties known to
 * have low cardinality (enumeration-like values, identifiers from a fixed set):
 * applying it to unbounded, caller-provided content (free-form text) allows a
 * malicious payload to degrade performance of the whole JVM.
 *
 * @since 3.3
 */
public class StringCanonicalizingConverter
    extends StdConverter<String, String>
{
    @Override
    public String convert(DeserializationContext ctxt, String value) {
        return ctxt.canonicalizeString(value);
    }

    /**
     * Serialization-side canonicalization is a no-op: the value returned is written
     * out and immediately discarded, so canonicalizing it would only add the cost
     * of {@link String#intern()} without any of the memory savings this Converter
     * exists for. Value is hence returned as-is.
     */
    @Override
    public String convert(SerializationContext ctxt, String value) {
        return value;
    }

    /**
     * Context-less conversion is not supported: canonicalization is always done
     * via {@link tools.jackson.databind.DatabindContext#canonicalizeString(String)}
     * so that the context-specific canonicalization method is used.
     * Both context-taking variants are overridden, so databind itself never calls
     * this method.
     *
     * @throws IllegalStateException Always
     */
    @Override
    public String convert(String value) {
        throw new IllegalStateException(String.format(
                "Should never be called (%s.convert(String)): must use context-taking variant",
                getClass().getName()));
    }
}
