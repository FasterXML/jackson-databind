package com.fasterxml.jackson.databind.json;

import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.core.json.JsonFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.cfg.MapperBuilder;
import com.fasterxml.jackson.databind.cfg.MapperBuilderState;
import com.fasterxml.jackson.databind.cfg.PackageVersion;

/**
 * JSON-specific {@link ObjectMapper} implementation.
 */
public class JsonMapper extends ObjectMapper
{
    private static final long serialVersionUID = 1L;

    /**
     * Base implementation for "Vanilla" {@link ObjectMapper}, used with
     * JSON dataformat backend.
     */
    public static class Builder extends MapperBuilder<JsonMapper, Builder>
    {
        public Builder(JsonFactory f) {
            super(f);
        }

        public Builder(StateImpl state) {
            super(state);
        }

        @Override
        public JsonMapper build() {
            return new JsonMapper(this);
        }

        @Override
        protected MapperBuilderState _saveState() {
            return new StateImpl(this);
        }

        protected static class StateImpl extends MapperBuilderState
            implements java.io.Serializable // important!
        {
            private static final long serialVersionUID = 3L;
    
            public StateImpl(Builder src) {
                super(src);
            }
    
            // We also need actual instance of state as base class can not implement logic
             // for reinstating mapper (via mapper builder) from state.
            @Override
            protected Object readResolve() {
                return new Builder(this).build();
            }
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
        this(new Builder(f));
    }

    public JsonMapper(Builder b) {
        super(b);
    }

    /*
    /**********************************************************
    /* Life-cycle, builders
    /**********************************************************
     */

    public static JsonMapper.Builder builder() {
        return new Builder(new JsonFactory());
    }

    public static Builder builder(JsonFactory streamFactory) {
        return new Builder(streamFactory);
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
    public JsonFactory tokenStreamFactory() {
        return (JsonFactory) _streamFactory;
    }
}
