package tools.jackson.databind.deser;

import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.util.AccessPattern;

/**
 * Helper interface implemented by classes that are to be used as
 * null providers during deserialization. Most importantly implemented by
 * {@link tools.jackson.databind.ValueDeserializer} (as a mix-in
 * interface), but also by converters used to support more configurable
 * null replacement.
 */
public interface NullValueProvider
{
    /**
     * Method called to possibly convert incoming `null` token (read via
     * underlying streaming input source) into other value of type accessor
     * supports. May return `null`, or value compatible with type binding.
     *<p>
     * NOTE: if {@link #getNullAccessPattern()} returns `ALWAYS_NULL` or
     * `CONSTANT`, this method WILL NOT use provided `ctxt` and it may thus
     * be passed as `null`.
     */
    public Object getNullValue(DeserializationContext ctxt);

    /**
     * Accessor that may be used to determine if and when provider must be called to
     * access null replacement value.
     */
    public AccessPattern getNullAccessPattern();

    /**
     * Method called to determine placeholder value to be used for cases
     * where no value was obtained from input but we must pass a value
     * nonetheless: the common case is that of Creator methods requiring
     * passing a value for every parameter.
     * Usually this is same as {@link #getNullValue} (which in turn
     * is usually simply Java {@code null}), but it can be overridden
     * for specific types: most notable scalar types must use "default"
     * values.
     *<p>
     * This method needs to be called every time a determination is made.
     *<p>
     * Default implementation simply calls and returns {@link #getNullValue}.
     *
     * @since 2.13
     */
    default Object getAbsentValue(DeserializationContext ctxt) {
        return getNullValue(ctxt);
    }
}
