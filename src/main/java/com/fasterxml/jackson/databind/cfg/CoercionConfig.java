package com.fasterxml.jackson.databind.cfg;

import java.util.Arrays;

/**
 * @since 2.12
 */
public class CoercionConfig
    implements java.io.Serializable
{
    private static final long serialVersionUID = 1L;

    private final static int INPUT_SHAPE_COUNT = CoercionInputShape.values().length;

    protected Boolean _acceptBlankAsEmpty;

    /**
     * Mapping from {@link CoercionInputShape} into corresponding
     * {@link CoercionAction}.
     */
    protected final CoercionAction[] _coercionsByShape;

    public CoercionConfig() {
        _coercionsByShape = new CoercionAction[INPUT_SHAPE_COUNT];
        // 23-Sep-2021, tatu: In 2.12 was `false` but should really be `null`
        //    to mean "not specified" (use defaults)
        _acceptBlankAsEmpty = null;
    }

    protected CoercionConfig(CoercionConfig src) {
        _acceptBlankAsEmpty = src._acceptBlankAsEmpty;
        _coercionsByShape = Arrays.copyOf(src._coercionsByShape,
                src._coercionsByShape.length);
    }

    public CoercionAction findAction(CoercionInputShape shape) {
        return _coercionsByShape[shape.ordinal()];
    }

    public Boolean getAcceptBlankAsEmpty() {
        return _acceptBlankAsEmpty;
    }
}
