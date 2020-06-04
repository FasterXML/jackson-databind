package com.fasterxml.jackson.databind.cfg;

/**
 * Mutable version of {@link CoercionConfig} (or rather, extended API)
 * exposed during configuration phase of {@link com.fasterxml.jackson.databind.ObjectMapper}
 * construction (via Builder).
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
