package tools.jackson.databind.tofix;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import com.fasterxml.jackson.annotation.JsonProperty;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.testutil.DatabindTestUtil;
import tools.jackson.databind.testutil.failure.JacksonTestFailureExpected;

import static org.junit.jupiter.api.Assertions.assertNotNull;

// [databind#3355] Deserialization fails depending on the order of deserialized
// objects with "Cannot construct instance (although at least one Creator exists)"
public class CreatorWithIgnoreProperties3355Test
    extends DatabindTestUtil
{
    static class Common3355 {
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

    static class ContainerFail3355 {
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

    private final ObjectMapper MAPPER = newJsonMapper();

    @JacksonTestFailureExpected
    @Test
    public void testDeserFailing() throws Exception
    {
        final String objectJson = "{ \"property\": \"valueOne\" }";
        final String containersJson = "{ \"common\": { \"property\": \"valueTwo\" } }";

        // If we deserialize inner object first, outer object FAILS
        Common3355 object = MAPPER.readValue(objectJson, Common3355.class);
        ContainerFail3355 container = MAPPER.readValue(containersJson, ContainerFail3355.class);

        assertNotNull(object);
        assertNotNull(container);
    }

    @Test
    public void testDeserPassing() throws Exception
    {
        final String objectJson = "{ \"property\": \"valueOne\" }";
        final String containersJson = "{ \"common\": { \"property\": \"valueTwo\" } }";

        // If we deserialize outer object first, it WORKS
        final ContainerFail3355 container = MAPPER.readValue(containersJson, ContainerFail3355.class);
        final Common3355 object = MAPPER.readValue(objectJson, Common3355.class);

        assertNotNull(object);
        assertNotNull(container);
    }
}
