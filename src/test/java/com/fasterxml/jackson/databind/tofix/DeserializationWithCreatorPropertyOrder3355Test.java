package com.fasterxml.jackson.databind.tofix;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.testutil.DatabindTestUtil;
import com.fasterxml.jackson.databind.testutil.failure.JacksonTestFailureExpected;
import org.junit.jupiter.api.Test;

// [databind#3355] Deserialization fails depending on the order of deserialized
// objects with "Cannot construct instance (although at least one Creator exists)"
public class DeserializationWithCreatorPropertyOrder3355Test
        extends DatabindTestUtil
{

    public static class Common3355 {
        private final String property;
        private final ContainerFail3355 container;

        @JsonCreator
        public Common3355(@JsonProperty("property") final String property,
                          @JsonProperty("container") final ContainerFail3355 container) {
            this.property = property;
            this.container = container;
        }

        public String getProperty() {
            return property;
        }

        public ContainerFail3355 getContainer() {
            return container;
        }
    }

    public static class ContainerFail3355 {
        private final Common3355 common;

        @JsonCreator
        public ContainerFail3355(@JsonProperty("common") final Common3355 common) {
            this.common = common;
        }

        @JsonIgnoreProperties("container")
        public Common3355 getCommon() {
            return common;
        }
    }

    @JacksonTestFailureExpected
    @Test
    public void testDeserFailing()
            throws Exception
    {
        final String objectJson = "{ \"property\": \"valueOne\" }";
        final String containersJson = "{ \"common\": { \"property\": \"valueTwo\" } }";

        final ObjectMapper objectMapper = newJsonMapper();

        // If we deserialize inner object first, outer object FAILS
        final Common3355 object = objectMapper.readValue(objectJson, Common3355.class);
        final ContainerFail3355 container = objectMapper.readValue(containersJson, ContainerFail3355.class);
    }

    @Test
    public void testDeserPassing()
            throws Exception
    {
        final String objectJson = "{ \"property\": \"valueOne\" }";
        final String containersJson = "{ \"common\": { \"property\": \"valueTwo\" } }";

        final ObjectMapper objectMapper = newJsonMapper();

        // If we deserialize outer object first, it WORKS
        final ContainerFail3355 container = objectMapper.readValue(containersJson, ContainerFail3355.class);
        final Common3355 object = objectMapper.readValue(objectJson, Common3355.class);
    }

}
