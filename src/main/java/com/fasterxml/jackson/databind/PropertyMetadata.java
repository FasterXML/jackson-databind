package com.fasterxml.jackson.databind;

import com.fasterxml.jackson.annotation.Nulls;
import com.fasterxml.jackson.databind.introspect.AnnotatedMember;

/**
 * Simple container class used for storing "additional" metadata about
 * properties. Carved out to reduce number of distinct properties that
 * actual property implementations and place holders need to store;
 * since instances are immutable, they can be freely shared.
 *
 * @since 2.3
 */
public class PropertyMetadata
    implements java.io.Serializable
{
    private static final long serialVersionUID = -1;

    public final static PropertyMetadata STD_REQUIRED = new PropertyMetadata(Boolean.TRUE,
            null, null, null, null, null, null);

    public final static PropertyMetadata STD_OPTIONAL = new PropertyMetadata(Boolean.FALSE,
            null, null, null, null, null, null);

    public final static PropertyMetadata STD_REQUIRED_OR_OPTIONAL = new PropertyMetadata(null,
            null, null, null, null, null, null);

    /**
     * Helper class used for containing information about expected merge
     * information for this property, if merging is expected.
     *
     * @since 2.9
     */
    public final static class MergeInfo
    // NOTE: need not be Serializable, not persisted
    {
        public final AnnotatedMember getter;

        /**
         * Flag that is set if the information came from global defaults,
         * and not from explicit per-property annotations or per-type
         * config overrides.
         */
        public final boolean fromDefaults;

        protected MergeInfo(AnnotatedMember getter, boolean fromDefaults) {
            this.getter = getter;
            this.fromDefaults = fromDefaults;
        }

        public static MergeInfo createForDefaults(AnnotatedMember getter) {
            return new MergeInfo(getter, true);
        }

        public static MergeInfo createForTypeOverride(AnnotatedMember getter) {
            return new MergeInfo(getter, false);
        }

        public static MergeInfo createForPropertyOverride(AnnotatedMember getter) {
            return new MergeInfo(getter, false);
        }
    }

    /**
     * Three states: required, not required and unknown; unknown represented
     * as null.
     */
    protected final Boolean _required;

    /**
     * Optional human-readable description associated with the property.
     */
    protected final String _description;

    /**
     * Optional index of the property within containing Object.
     *
     * @since 2.4
     */
    protected final Integer _index;

    /**
     * Optional default value, as String, for property; not used for
     * any functionality by core databind, offered as metadata for
     * extensions.
     */
    protected final String _defaultValue;

    /**
     * Settings regarding merging, if property is determined to possibly
     * be mergeable (possibly since global settings may be omitted for
     * non-mergeable types).
     *<p>
     * NOTE: transient since it is assumed that this information is only
     * relevant during initial setup and not needed after full initialization.
     * May be changed if this proves necessary.
     *
     * @since 2.9
     */
    protected final transient MergeInfo _mergeInfo;

    /**
     * Settings regarding handling of incoming `null`s, both for value itself
     * and, for structured types, content values (array/Collection elements,
     * Map values).
     *
     * @since 2.9
     */
    protected Nulls _valueNulls, _contentNulls;

    /*
    /**********************************************************
    /* Construction, configuration
    /**********************************************************
     */

    /**
     * @since 2.9
     */
    protected PropertyMetadata(Boolean req, String desc, Integer index, String def,
            MergeInfo mergeInfo, Nulls valueNulls, Nulls contentNulls)
    {
        _required = req;
        _description = desc;
        _index = index;
        _defaultValue = (def == null || def.isEmpty()) ? null : def;
        _mergeInfo = mergeInfo;
        _valueNulls = valueNulls;
        _contentNulls = contentNulls;
    }

    /**
     * @since 2.8.8
     */
    public static PropertyMetadata construct(Boolean req, String desc, Integer index,
            String defaultValue) {
        if ((desc != null) || (index != null) || (defaultValue != null)) {
            return new PropertyMetadata(req, desc, index, defaultValue,
                    null, null, null);
        }
        if (req == null) {
            return STD_REQUIRED_OR_OPTIONAL;
        }
        return req ? STD_REQUIRED : STD_OPTIONAL;
    }

    @Deprecated // since 2.8.8
    public static PropertyMetadata construct(boolean req, String desc, Integer index,
            String defaultValue) {
        if (desc != null || index != null || defaultValue != null) {
            return new PropertyMetadata(req, desc, index, defaultValue,
                    null, null, null);
        }
        return req ? STD_REQUIRED : STD_OPTIONAL;
    }

    /**
     * Minor optimization: let's canonicalize back to placeholders in cases
     * where there is no real data to consider
     */
    protected Object readResolve()
    {
        if ((_description == null) && (_index == null) && (_defaultValue == null)
                && (_mergeInfo == null)
                && (_valueNulls == null) && (_contentNulls == null)) {
            if (_required == null) {
                return STD_REQUIRED_OR_OPTIONAL;
            }
            return _required.booleanValue() ? STD_REQUIRED : STD_OPTIONAL;
        }
        return this;
    }

    public PropertyMetadata withDescription(String desc) {
        return new PropertyMetadata(_required, desc, _index, _defaultValue,
                _mergeInfo, _valueNulls, _contentNulls);
    }

    /**
     * @since 2.9
     */
    public PropertyMetadata withMergeInfo(MergeInfo mergeInfo) {
        return new PropertyMetadata(_required, _description, _index, _defaultValue,
                mergeInfo, _valueNulls, _contentNulls);
    }

    /**
     * @since 2.9
     */
    public PropertyMetadata withNulls(Nulls valueNulls,
            Nulls contentNulls) {
        return new PropertyMetadata(_required, _description, _index, _defaultValue,
                _mergeInfo, valueNulls, contentNulls);
    }

    public PropertyMetadata withDefaultValue(String def) {
        if ((def == null) || def.isEmpty()) {
            if (_defaultValue == null) {
                return this;
            }
            def = null;
        } else if (def.equals(_defaultValue)) {
            return this;
        }
        return new PropertyMetadata(_required, _description, _index, def,
                _mergeInfo, _valueNulls, _contentNulls);
    }

    public PropertyMetadata withIndex(Integer index) {
        return new PropertyMetadata(_required, _description, index, _defaultValue,
                _mergeInfo, _valueNulls, _contentNulls);
    }

    public PropertyMetadata withRequired(Boolean b) {
        if (b == null) {
            if (_required == null) {
                return this;
            }
        } else if (b.equals(_required)) {
            return this;
        }
        return new PropertyMetadata(b, _description, _index, _defaultValue,
                _mergeInfo, _valueNulls, _contentNulls);
    }

    /*
    /**********************************************************
    /* Accessors
    /**********************************************************
     */

    public String getDescription() { return _description; }

    /**
     * @since 2.5
     */
    public String getDefaultValue() { return _defaultValue; }

    /**
     * Accessor for determining whether property has declared "default value",
     * which may be used by extension modules.
     *
     * @since 2.6
     */
    public boolean hasDefaultValue() { return (_defaultValue != null); }

    public boolean isRequired() { return (_required != null) && _required.booleanValue(); }

    public Boolean getRequired() { return _required; }

    /**
     * @since 2.4
     */
    public Integer getIndex() { return _index; }

    /**
     * @since 2.4
     */
    public boolean hasIndex() { return _index != null; }

    /**
     * @since 2.9
     */
    public MergeInfo getMergeInfo() { return _mergeInfo; }

    /**
     * @since 2.9
     */
    public Nulls getValueNulls() { return _valueNulls; }

    /**
     * @since 2.9
     */
    public Nulls getContentNulls() { return _contentNulls; }
}
