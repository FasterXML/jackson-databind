package tools.jackson.databind.util;

import tools.jackson.core.util.InternCache;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.SerializationContext;

/**
 * Converter that canonicalizes {@link String} values using databind's default
 * String canonicalization method.
 *<p>
 * Can be used with {@link tools.jackson.databind.annotation.JsonDeserialize#converter()}
 * or {@link tools.jackson.databind.annotation.JsonDeserialize#contentConverter()}
 * to canonicalize low-cardinality String values during deserialization.
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

    @Override
    public String convert(SerializationContext ctxt, String value) {
        return ctxt.canonicalizeString(value);
    }

    @Override
    public String convert(String value) {
        return (value == null) ? null : InternCache.instance.intern(value);
    }
}
