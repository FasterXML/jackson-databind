package com.fasterxml.jackson.databind.cfg;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.core.util.Snapshottable;

/**
 * @since 2.12
 */
public class CoercionConfigs
    implements java.io.Serializable,
        Snapshottable<CoercionConfigs>
{
    private static final long serialVersionUID = 3L;

    private final static int TARGET_TYPE_COUNT = CoercionTargetType.values().length;

    /**
     * Global default setting for whether blank (all-white space) String is
     * accepted as "empty" (zero-length) for purposes of coercions.
     *<p>
     * Default value is {@code false}, meaning blank Strings are NOT considered
     * "empty" for coercion purposes.
     */
    protected boolean _acceptBlankAsEmpty;

    /**
     * Coercion definitions by logical type ({@link CoercionTargetType})
     */
    protected MutableCoercionConfig[] _perTypeCoercions;

    /**
     * Coercion definitions by physical type (Class).
     */
    protected Map<Class<?>, MutableCoercionConfig> _perClassCoercions;

    /*
    /**********************************************************************
    /* Life cycle
    /**********************************************************************
     */

    public CoercionConfigs() {
        this(false, null, null);
    }

    protected CoercionConfigs(boolean acceptBlankAsEmpty,
            MutableCoercionConfig[] perTypeCoercions,
            Map<Class<?>, MutableCoercionConfig> perClassCoercions) {
        _acceptBlankAsEmpty = acceptBlankAsEmpty;
        _perTypeCoercions = perTypeCoercions;
        _perClassCoercions = perClassCoercions;
    }

    /**
     * Method called to create a non-shared copy of configuration settings,
     * to be used by another {@link com.fasterxml.jackson.databind.ObjectMapper}
     * instance.
     *
     * @return A non-shared copy of configuration settings
     */
    @Override
    public CoercionConfigs snapshot()
    {
        MutableCoercionConfig[] newPerType;
        if (_perTypeCoercions == null) {
            newPerType = null;
        } else {
            final int size = _perTypeCoercions.length;
            newPerType = new MutableCoercionConfig[size];
            for (int i = 0; i < size; ++i) {
                newPerType[i] = _copy(_perTypeCoercions[i]);
            }
        }
        Map<Class<?>, MutableCoercionConfig> newPerClass;
        if (_perClassCoercions == null) {
            newPerClass = null;
        } else {
            newPerClass = new HashMap<>();
            for (Map.Entry<Class<?>, MutableCoercionConfig> entry : _perClassCoercions.entrySet()) {
                newPerClass.put(entry.getKey(), entry.getValue().copy());
            }
        }
        return new CoercionConfigs(_acceptBlankAsEmpty, newPerType, newPerClass);
    }

    private static MutableCoercionConfig _copy(MutableCoercionConfig src) {
        if (src == null) {
            return null;
        }
        return src.copy();
    }
}
