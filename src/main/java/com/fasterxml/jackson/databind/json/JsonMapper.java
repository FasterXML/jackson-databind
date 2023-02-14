package com.fasterxml.jackson.databind.json;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.core.json.JsonReadFeature;
import com.fasterxml.jackson.core.json.JsonWriteFeature;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.cfg.MapperBuilder;
import com.fasterxml.jackson.databind.cfg.PackageVersion;

/**
 * JSON-format specific {@link ObjectMapper} implementation.
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

        public Builder enable(JsonReadFeature... features)  {
            for (JsonReadFeature f : features) {
                _mapper.enable(f.mappedFeature());
            }
            return this;
        }

        public Builder disable(JsonReadFeature... features) {
            for (JsonReadFeature f : features) {
                _mapper.disable(f.mappedFeature());
            }
            return this;
        }

        public Builder configure(JsonReadFeature f, boolean state)
        {
            if (state) {
                _mapper.enable(f.mappedFeature());
            } else {
                _mapper.disable(f.mappedFeature());
            }
            return this;
        }

        public Builder enable(JsonWriteFeature... features)  {
            for (JsonWriteFeature f : features) {
                _mapper.enable(f.mappedFeature());
            }
            return this;
        }

        public Builder disable(JsonWriteFeature... features) {
            for (JsonWriteFeature f : features) {
                _mapper.disable(f.mappedFeature());
            }
            return this;
        }

        public Builder configure(JsonWriteFeature f, boolean state)
        {
            if (state) {
                _mapper.enable(f.mappedFeature());
            } else {
                _mapper.disable(f.mappedFeature());
            }
            return this;
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

    public JsonMapper.Builder  rebuild() {
        // 09-Dec-2018, tatu: Not as good as what 3.0 has wrt immutability, but best approximation
        //     we have for 2.x
        return new Builder(this.copy());
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

    /*
    /**********************************************************
    /* JSON-specific accessors, mutators
    /**********************************************************
     */

    // // // 25-Oct-2018, tatu: Since for 2.x these will simply map to legacy settings,
    // // //   we will fake them

    public boolean isEnabled(JsonReadFeature f) {
        return isEnabled(f.mappedFeature());
    }

    public boolean isEnabled(JsonWriteFeature f) {
        return isEnabled(f.mappedFeature());
    }
}
