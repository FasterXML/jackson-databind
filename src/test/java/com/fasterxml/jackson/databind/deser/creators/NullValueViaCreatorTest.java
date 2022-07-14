package com.fasterxml.jackson.databind.deser.creators;

import java.util.UUID;

import com.fasterxml.jackson.annotation.*;
import tools.jackson.core.*;
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

    protected static class ContainedDeserializer extends ValueDeserializer<Contained<?>> {
        @Override
        public Contained<?> deserialize(JsonParser jp, DeserializationContext ctxt) {
            return null;
        }

        @Override
        public Contained<?> getNullValue(DeserializationContext ctxt) {
            return NULL_CONTAINED;
        }
    }

    protected static class ContainerDeserializerResolver extends Deserializers.Base {
        @Override
        public ValueDeserializer<?> findBeanDeserializer(JavaType type,
                DeserializationConfig config, BeanDescription beanDesc)
        {
            if (!Contained.class.isAssignableFrom(type.getRawClass())) {
                return null;
            }
            return new ContainedDeserializer();
        }

        @Override
        public boolean hasDeserializerFor(DeserializationConfig config,
                Class<?> valueType) {
            return false;
        }
    }

    protected static class TestModule extends com.fasterxml.jackson.databind.JacksonModule
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
        ObjectMapper mapper = jsonMapperBuilder()
                .addModule(new TestModule())
                .build();
        Container container = mapper.readValue("{}", Container.class);
        assertEquals(NULL_CONTAINED, container.contained);
    }

    // [databind#597]: ensure that a useful exception is thrown
    public void testCreatorReturningNull()
    {
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
