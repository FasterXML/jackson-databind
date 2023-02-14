package com.fasterxml.jackson.databind.ser.impl;

import java.util.*;

import com.fasterxml.jackson.databind.ser.*;

/**
 * Simple {@link FilterProvider} implementation that just stores
 * direct id-to-filter mapping. It also allows specifying a
 * "default" filter to return if no registered instance matches
 * given filter id.
 *<p>
 * Note that version 2.3 was a partial rewrite, now that
 * {@link PropertyFilter} is set to replace <code>BeanPropertyFilter</code>.
 */
public class SimpleFilterProvider
    extends FilterProvider
    implements java.io.Serializable // since 2.1
{
    // for 2.5+
    private static final long serialVersionUID = 1L;

    /**
     * Mappings from ids to filters.
     */
    protected final Map<String,PropertyFilter> _filtersById;

    /**
     * This is the filter we return in case no mapping was found for
     * given id; default is {@code null} (in which case caller typically
     * reports an error), but can be set to an explicit filter.
     */
    protected PropertyFilter _defaultFilter;

    /**
     * Flag that indicates whether request for an unknown filter id should
     * result an exception (default) or not.
     * Note that this is only relevant if no default filter has been
     * configured.
     */
    protected boolean _cfgFailOnUnknownId = true;

    /*
    /**********************************************************
    /* Life-cycle: constructing, configuring
    /**********************************************************
     */

    public SimpleFilterProvider() {
        this(new HashMap<String,Object>());
    }

    /**
     * @param mapping Mapping from id to filter; used as is if if possible
     */
    @SuppressWarnings("unchecked")
    public SimpleFilterProvider(Map<String,?> mapping)
    {
        /* 16-Oct-2013, tatu: Since we can now be getting both new and old
         *   obsolete filters (PropertyFilter vs BeanPropertyFilter), need
         *   to verify contents.
         */
        for (Object ob : mapping.values()) {
            if (!(ob instanceof PropertyFilter)) {
                _filtersById = _convert(mapping);
                return;
            }
        }
        _filtersById = (Map<String,PropertyFilter>) mapping;
    }

    @SuppressWarnings("deprecation")
    private final static Map<String,PropertyFilter> _convert(Map<String,?> filters)
    {
        HashMap<String,PropertyFilter> result = new HashMap<String,PropertyFilter>();
        for (Map.Entry<String, ?> entry : filters.entrySet()) {
            Object f = entry.getValue();
            if (f instanceof PropertyFilter) {
                result.put(entry.getKey(), (PropertyFilter) f);
            } else if (f instanceof BeanPropertyFilter) {
                result.put(entry.getKey(), _convert((BeanPropertyFilter) f));
            } else {
                throw new IllegalArgumentException("Unrecognized filter type ("+f.getClass().getName()+")");
            }
        }
        return result;
    }

    @SuppressWarnings("deprecation")
    private final static PropertyFilter _convert(BeanPropertyFilter f) {
        return SimpleBeanPropertyFilter.from((BeanPropertyFilter) f);
    }

    /**
     * Method for defining filter to return for "unknown" filters; cases
     * where there is no mapping from given id to an explicit filter.
     *
     * @param f Filter to return when no filter is found for given id
     *
     * @deprecated Since 2.3 should use {@link PropertyFilter} instead of {@link BeanPropertyFilter}
     */
    @Deprecated
    public SimpleFilterProvider setDefaultFilter(BeanPropertyFilter f)
    {
        _defaultFilter = SimpleBeanPropertyFilter.from(f);
        return this;
    }

    /**
     * Method for defining "default filter" to use, if any ({@code null} if none),
     * to return in case no registered instance matches passed filter id.
     *
     * @param f Default filter to set
     *
     * @return This provider instance, for call-chaining
     */
    public SimpleFilterProvider setDefaultFilter(PropertyFilter f)
    {
        _defaultFilter = f;
        return this;
    }

    /**
     * Overloaded variant just to resolve "ties" when using {@link SimpleBeanPropertyFilter}.
     */
    public SimpleFilterProvider setDefaultFilter(SimpleBeanPropertyFilter f)
    {
        _defaultFilter = f;
        return this;
    }

    public PropertyFilter getDefaultFilter() {
        return _defaultFilter;
    }

    public SimpleFilterProvider setFailOnUnknownId(boolean state) {
        _cfgFailOnUnknownId = state;
        return this;
    }

    public boolean willFailOnUnknownId() {
        return _cfgFailOnUnknownId;
    }

    /**
     * @deprecated since 2.3
     */
    @Deprecated
    public SimpleFilterProvider addFilter(String id, BeanPropertyFilter filter) {
        _filtersById.put(id, _convert(filter));
        return this;
    }

    public SimpleFilterProvider addFilter(String id, PropertyFilter filter) {
        _filtersById.put(id, filter);
        return this;
    }

    /**
     * Overloaded variant just to resolve "ties" when using {@link SimpleBeanPropertyFilter}.
     */
    public SimpleFilterProvider addFilter(String id, SimpleBeanPropertyFilter filter) {
        _filtersById.put(id, filter);
        return this;
    }

    public PropertyFilter removeFilter(String id) {
        return _filtersById.remove(id);
    }

    /*
    /**********************************************************
    /* Public lookup API
    /**********************************************************
     */

    @Deprecated // since 2.3
    @Override
    public BeanPropertyFilter findFilter(Object filterId)
    {
        throw new UnsupportedOperationException("Access to deprecated filters not supported");
    }

    @Override
    public PropertyFilter findPropertyFilter(Object filterId, Object valueToFilter)
    {
        PropertyFilter f = _filtersById.get(filterId);
        if (f == null) {
            f = _defaultFilter;
            if (f == null && _cfgFailOnUnknownId) {
                throw new IllegalArgumentException("No filter configured with id '"+filterId+"' (type "
                        +filterId.getClass().getName()+")");
            }
        }
        return f;
    }
}
