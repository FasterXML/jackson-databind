package tools.jackson.databind.cfg;

import java.util.function.Consumer;

/**
 * Mutable version of {@link CoercionConfig} (or rather, extended API)
 * exposed during configuration phase of {@link tools.jackson.databind.ObjectMapper}
 * (via {@link tools.jackson.databind.json.JsonMapper#builder()}).
 */
public class MutableCoercionConfig
    extends CoercionConfig
    implements java.io.Serializable
{
    private static final long serialVersionUID = 3L;

    public MutableCoercionConfig() { }

    protected MutableCoercionConfig(MutableCoercionConfig src) {
        super(src);
    }

    public MutableCoercionConfig copy() {
        return new MutableCoercionConfig(this);
    }

    /**
     * Method to set coercions to target type or class during builder-style mapper construction with
     * <ul>
     *     <li>{@link MapperBuilder#withCoercionConfig(Class, Consumer)},</li>
     *     <li>{@link MapperBuilder#withCoercionConfig(tools.jackson.databind.type.LogicalType, Consumer)} and</li>
     *     <li>{@link MapperBuilder#withCoercionConfigDefaults(Consumer)}</li>
     * </ul>
     * ... these builder methods. Refrain from using this method outside of builder phase.
     */
    public MutableCoercionConfig setCoercion(CoercionInputShape shape,
            CoercionAction action) {
        _coercionsByShape[shape.ordinal()] = action;
        return this;
    }

    public MutableCoercionConfig setAcceptBlankAsEmpty(Boolean state) {
        _acceptBlankAsEmpty = state;
        return this;
    }
}
