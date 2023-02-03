package tools.jackson.databind.json;

import tools.jackson.core.Version;
import tools.jackson.core.json.JsonFactory;
import tools.jackson.core.json.JsonReadFeature;
import tools.jackson.core.json.JsonWriteFeature;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.cfg.MapperBuilder;
import tools.jackson.databind.cfg.MapperBuilderState;

import tools.jackson.databind.cfg.PackageVersion;

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

        /*
        /******************************************************************
        /* Format features
        /******************************************************************
         */

        public Builder enable(JsonReadFeature... features) {
            for (JsonReadFeature f : features) {
                _formatReadFeatures |= f.getMask();
            }
            return this;
        }

        public Builder disable(JsonReadFeature... features) {
            for (JsonReadFeature f : features) {
                _formatReadFeatures &= ~f.getMask();
            }
            return this;
        }

        public Builder configure(JsonReadFeature feature, boolean state)
        {
            if (state) {
                _formatReadFeatures |= feature.getMask();
            } else {
                _formatReadFeatures &= ~feature.getMask();
            }
            return this;
        }

        public Builder enable(JsonWriteFeature... features) {
            for (JsonWriteFeature f : features) {
                _formatWriteFeatures |= f.getMask();
            }
            return this;
        }

        public Builder disable(JsonWriteFeature... features) {
            for (JsonWriteFeature f : features) {
                _formatWriteFeatures &= ~f.getMask();
            }
            return this;
        }

        public Builder configure(JsonWriteFeature feature, boolean state)
        {
            if (state) {
                _formatWriteFeatures |= feature.getMask();
            } else {
                _formatWriteFeatures &= ~feature.getMask();
            }
            return this;
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
    /**********************************************************************
    /* Life-cycle, constructors
    /**********************************************************************
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
    /**********************************************************************
    /* Life-cycle, builders
    /**********************************************************************
     */

    public static JsonMapper.Builder builder() {
        return new Builder(new JsonFactory());
    }

    public static Builder builder(JsonFactory streamFactory) {
        return new Builder(streamFactory);
    }

    @SuppressWarnings("unchecked")
    @Override
    public JsonMapper.Builder rebuild() {
        return new Builder((Builder.StateImpl)_savedBuilderState);
    }

    /*
    /**********************************************************************
    /* Life-cycle, shared "vanilla" (default configuration) instance
    /**********************************************************************
     */

    /**
     * Accessor method for getting globally shared "default" {@link JsonMapper}
     * instance: one that has default configuration, no modules registered, no
     * config overrides. Usable mostly when dealing "untyped" or Tree-style
     * content reading and writing.
     */
    public static JsonMapper shared() {
        return SharedWrapper.wrapped();
    }

    /*
    /**********************************************************************
    /* Standard method overrides
    /**********************************************************************
     */

    @Override
    public Version version() {
        return PackageVersion.VERSION;
    }

    @Override
    public JsonFactory tokenStreamFactory() {
        return (JsonFactory) _streamFactory;
    }

    /*
    /**********************************************************
    /* Format-specific
    /**********************************************************
     */

    public boolean isEnabled(JsonReadFeature f) {
        return _deserializationConfig.hasFormatFeature(f);
    }

    public boolean isEnabled(JsonWriteFeature f) {
        return _serializationConfig.hasFormatFeature(f);
    }

    /*
    /**********************************************************
    /* Helper class(es)
    /**********************************************************
     */

    /**
     * Helper class to contain dynamically constructed "shared" instance of
     * mapper, should one be needed via {@link #shared}.
     */
    private final static class SharedWrapper {
        private final static JsonMapper MAPPER = JsonMapper.builder().build();

        public static JsonMapper wrapped() { return MAPPER; }
    }
}
