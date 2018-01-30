package com.fasterxml.jackson.databind;

import com.fasterxml.jackson.core.*;

/**
 * Since {@link ObjectMapper} instances are immutable in  Jackson 3.x for full thread-safety,
 * we need means to construct configured instances. This is the shared base API for
 * builders for all types of mappers.
 *
 * @since 3.0
 */
public abstract class MapperBuilder<M extends ObjectMapper,
    T extends MapperBuilder<M,T>>
{
    /*
    /**********************************************************
    /* Simple feature bitmasks
    /**********************************************************
     */

    /**
     * Set of {@link MapperFeature}s enabled, as bitmask.
     */
    protected int _mapperFeatures;

    /*
    /**********************************************************
    /* Various factories
    /**********************************************************
     */

    protected final TokenStreamFactory _streamFactory;

    /*
    /**********************************************************
    /* Configuration settings, shared
    /**********************************************************
     */
    
    /*
    /**********************************************************
    /* Life-cycle
    /**********************************************************
     */

    protected MapperBuilder(TokenStreamFactory streamFactory) {
        _streamFactory = streamFactory;
//        _mapperFeatures = MapperFeature;
    }

    /*
    protected ObjectMapperBuilder(TokenStreamFactory base)
    {
        this(base._factoryFeatures,
                base.getParserFeatures(), base.getGeneratorFeatures());
    }
*/

    /**
     * Method to call to create an initialize actual mapper instance
     */
    public abstract M build();

    /*
    /**********************************************************
    /* Accessors
    /**********************************************************
     */

    public TokenStreamFactory streamFactory() {
        return _streamFactory;
    }

    /*
    /**********************************************************
    /* Changing simple features
    /**********************************************************
     */

}
