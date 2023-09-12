package com.fasterxml.jackson.databind.cfg;

import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.type.LogicalType;

import java.util.function.Consumer;

/**
 * Mutable version of {@link CoercionConfig} (or rather, extended API)
 * exposed during configuration phase of {@link com.fasterxml.jackson.databind.ObjectMapper}
 * construction (via {@link JsonMapper#builder()}).
 *
 * @since 2.12
 */
public class MutableCoercionConfig
    extends CoercionConfig
    implements java.io.Serializable
{
    private static final long serialVersionUID = 1L;

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
     *     <li>{@link MapperBuilder#withCoercionConfig(LogicalType, Consumer)} and</li>
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
