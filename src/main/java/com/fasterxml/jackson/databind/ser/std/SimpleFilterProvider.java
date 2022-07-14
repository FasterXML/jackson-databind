package com.fasterxml.jackson.databind.ser.std;

import java.util.*;

import tools.jackson.core.util.Snapshottable;

import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.*;
import com.fasterxml.jackson.databind.util.ClassUtil;

/**
 * Simple {@link FilterProvider} implementation that just stores
 * direct id-to-filter mapping.
 */
public class SimpleFilterProvider
    extends FilterProvider
    implements java.io.Serializable
{
    private static final long serialVersionUID = 3L;

    /**
     * Mappings from ids to filters.
     */
    protected final Map<String,PropertyFilter> _filtersById;

    /**
     * This is the filter we return in case no mapping was found for
     * given id; default is 'null' (in which case caller typically
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
        this(new HashMap<>());
    }

    /**
     * @param mapping Mapping from id to filter; used as is if if possible
     */
    public SimpleFilterProvider(Map<String,PropertyFilter> mapping)
    {
        _filtersById = mapping;
    }

    protected SimpleFilterProvider(SimpleFilterProvider src) {
        _defaultFilter = Snapshottable.takeSnapshot(src._defaultFilter);
        _cfgFailOnUnknownId = src._cfgFailOnUnknownId;
        Map<String,PropertyFilter> f = src._filtersById;
        if (f.isEmpty()) {
            _filtersById = new HashMap<>();
        } else {
            _filtersById = new HashMap<>(f.size());
            f.forEach((k,v) -> _filtersById.put(k, v.snapshot()));
        }
    }

    @Override
    public SimpleFilterProvider snapshot() {
        return new SimpleFilterProvider(this);
    }

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
    /**********************************************************************
    /* Public lookup API
    /**********************************************************************
     */

    @Override
    public PropertyFilter findPropertyFilter(SerializerProvider ctxt,
            Object filterId, Object valueToFilter)
    {
        PropertyFilter f = _filtersById.get(filterId);
        if (f == null) {
            f = _defaultFilter;
            if (f == null && _cfgFailOnUnknownId) {
                ctxt.reportMappingProblem("No filter configured with id '%s' (type %s)",
                        filterId, ClassUtil.classNameOf(filterId));
            }
        }
        return f;
    }
}
