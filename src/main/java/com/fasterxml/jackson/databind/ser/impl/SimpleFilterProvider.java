package com.fasterxml.jackson.databind.ser.impl;

import java.util.*;

import com.fasterxml.jackson.databind.ser.*;

/**
 * Simple {@link FilterProvider} implementation that just stores
 * direct id-to-filter mapping.
 */
public class SimpleFilterProvider
    extends FilterProvider
    implements java.io.Serializable // since 2.1
{
    // generated for 2.1.0
    private static final long serialVersionUID = -2825494703774121220L;

    /**
     * Mappings from ids to filters.
     */
    protected final Map<String,BeanPropertyFilter> _filtersById;

    /**
     * This is the filter we return in case no mapping was found for
     * given id; default is 'null' (in which case caller typically
     * reports an error), but can be set to an explicit filter.
     */
    protected BeanPropertyFilter _defaultFilter;

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
        this(new HashMap<String,BeanPropertyFilter>());
    }

    /**
     * @param mapping Mapping from id to filter; used as is, no copy is made.
     */
    public SimpleFilterProvider(Map<String,BeanPropertyFilter> mapping) {
        _filtersById = mapping;
    }
    
    /**
     * Method for defining filter to return for "unknown" filters; cases
     * where there is no mapping from given id to an explicit filter.
     * 
     * @param f Filter to return when no filter is found for given id
     */
    public SimpleFilterProvider setDefaultFilter(BeanPropertyFilter f)
    {
        _defaultFilter = f;
        return this;
    }

    public BeanPropertyFilter getDefaultFilter() {
        return _defaultFilter;
    }
    
    public SimpleFilterProvider setFailOnUnknownId(boolean state) {
        _cfgFailOnUnknownId = state;
        return this;
    }

    public boolean willFailOnUnknownId() {
        return _cfgFailOnUnknownId;
    }
    
    public SimpleFilterProvider addFilter(String id, BeanPropertyFilter filter) {
        _filtersById.put(id, filter);
        return this;
    }

    public BeanPropertyFilter removeFilter(String id) {
        return _filtersById.remove(id);
    }

    /*
    /**********************************************************
    /* Public lookup API
    /**********************************************************
     */
    
    @Override
    public BeanPropertyFilter findFilter(Object filterId)
    {
        BeanPropertyFilter f = _filtersById.get(filterId);
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
