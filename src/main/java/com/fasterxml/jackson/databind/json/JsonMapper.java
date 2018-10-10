package com.fasterxml.jackson.databind.json;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.cfg.MapperBuilder;
import com.fasterxml.jackson.databind.cfg.PackageVersion;

/**
 *
 * @since 2.10
 */
public class JsonMapper extends ObjectMapper
{
    private static final long serialVersionUID = 1L;

    /**
     * Base implementation for "Vanilla" {@link ObjectMapper}, used with
     * JSON dataformat backend.
     *
     * @since 2.10
     */
    public static class Builder extends MapperBuilder<JsonMapper, Builder>
    {
        public Builder(JsonMapper m) {
            super(m);
        }
    }

    /*
    /**********************************************************
    /* Life-cycle, constructors
    /**********************************************************
     */

    public JsonMapper() {
        this(new JsonFactory());
    }

    public JsonMapper(JsonFactory f) {
        super(f);
    }

    protected JsonMapper(JsonMapper src) {
        super(src);
    }

    @Override
    public JsonMapper copy()
    {
        _checkInvalidCopy(JsonMapper.class);
        return new JsonMapper(this);
    }

    /*
    /**********************************************************
    /* Life-cycle, builders
    /**********************************************************
     */

    public static JsonMapper.Builder builder() {
        return new Builder(new JsonMapper());
    }

    public static Builder builder(JsonFactory streamFactory) {
        return new Builder(new JsonMapper(streamFactory));
    }

    /*
    /**********************************************************
    /* Standard method overrides
    /**********************************************************
     */

    @Override
    public Version version() {
        return PackageVersion.VERSION;
    }

    @Override
    public JsonFactory getFactory() {
        return _jsonFactory;
    }
}
