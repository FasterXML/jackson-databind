package com.fasterxml.jackson.databind.creators;

import com.fasterxml.jackson.annotation.*;

import com.fasterxml.jackson.core.*;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.deser.*;

// Mostly for [JACSON-774]
public class TestCreatorNullValue extends BaseMapTest
{
    protected static class Container {
        Contained<String> contained;

        @JsonCreator
        public Container(@JsonProperty("contained") Contained<String> contained) {
            this.contained = contained;
        }
    }

    private static interface Contained<T> {}

    private static class NullContained implements Contained<Object> {}

    private static final NullContained NULL_CONTAINED = new NullContained();

    private static class ContainedDeserializer extends JsonDeserializer<Contained<?>> {
        @Override
        public Contained<?> deserialize(JsonParser jp, DeserializationContext ctxt) throws JsonProcessingException {
            return null;
        }

        @Override
        public Contained<?> getNullValue() {
            return NULL_CONTAINED;
        }
    }

    private static class ContainerDeserializerResolver extends Deserializers.Base {
        @Override
        public JsonDeserializer<?> findBeanDeserializer(JavaType type,
                DeserializationConfig config, BeanDescription beanDesc)
            throws JsonMappingException
        {
            if (!Contained.class.isAssignableFrom(type.getRawClass())) {
                return null;
            } else {
                return new ContainedDeserializer();
            }
        }
    }

    private static class TestModule extends Module
    {
        @Override
        public String getModuleName() {
            return "ContainedModule";
        }

        @Override
        public Version version() {
            return Version.unknownVersion();
        }

        @Override
        public void setupModule(SetupContext setupContext) {
            setupContext.addDeserializers(new ContainerDeserializerResolver());
        }
    }

    /*
    /**********************************************************
    /* Unit tests
    /**********************************************************
     */
    
    public void testUsesDeserializersNullValue() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new TestModule());
        Container container = mapper.readValue("{}", Container.class);
        assertEquals(NULL_CONTAINED, container.contained);
    }
}
