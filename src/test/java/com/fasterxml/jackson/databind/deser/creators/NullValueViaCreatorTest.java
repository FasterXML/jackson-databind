package com.fasterxml.jackson.databind.deser.creators;

import java.io.IOException;
import java.util.UUID;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.deser.*;
import com.fasterxml.jackson.databind.exc.ValueInstantiationException;

public class NullValueViaCreatorTest extends BaseMapTest
{
    protected static class Container {
        Contained<String> contained;

        @JsonCreator
        public Container(@JsonProperty("contained") Contained<String> contained) {
            this.contained = contained;
        }
    }

    protected static interface Contained<T> {}

    protected static class NullContained implements Contained<Object> {}

    protected static final NullContained NULL_CONTAINED = new NullContained();

    protected static class ContainedDeserializer extends JsonDeserializer<Contained<?>> {
        @Override
        public Contained<?> deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException {
            return null;
        }

        @Override
        public Contained<?> getNullValue(DeserializationContext ctxt) {
            return NULL_CONTAINED;
        }
    }

    protected static class ContainerDeserializerResolver extends Deserializers.Base {
        @Override
        public JsonDeserializer<?> findBeanDeserializer(JavaType type,
                DeserializationConfig config, BeanDescription beanDesc)
        {
            if (!Contained.class.isAssignableFrom(type.getRawClass())) {
                return null;
            }
            return new ContainedDeserializer();
        }
    }

    protected static class TestModule extends com.fasterxml.jackson.databind.Module
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

    // [databind#597]
    static class JsonEntity {
        protected final String type;
        protected final UUID id;

        private JsonEntity(String type, UUID id) {
            this.type = type;
            this.id = id;
        }

        @JsonCreator
        public static JsonEntity create(@JsonProperty("type") String type, @JsonProperty("id") UUID id) {
            if (type != null && !type.contains(" ") && (id != null)) {
                return new JsonEntity(type, id);
            }

            return null;
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

    // [databind#597]: ensure that a useful exception is thrown
    public void testCreatorReturningNull() throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        String json = "{ \"type\" : \"     \", \"id\" : \"000c0ffb-a0d6-4d2e-a379-4aeaaf283599\" }";
        try {
            objectMapper.readValue(json, JsonEntity.class);
            fail("Should not have succeeded");
        } catch (ValueInstantiationException e) {
            verifyException(e, "JSON creator returned null");
        }
    }
}
