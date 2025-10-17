package tools.jackson.databind.tofix;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.*;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.testutil.DatabindTestUtil;
import tools.jackson.databind.testutil.failure.JacksonTestFailureExpected;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

// [databind#5342] JsonAnyGetter method serialization can override JsonProperty serialization on serialized name conflict
public class AnyGetterNameConflictSerialization5342Test
    extends DatabindTestUtil
{
    public static class Pojo5342 {
        @JsonIgnore
        private Map<String, Object> additionalProperties;
        @JsonProperty(value = "additionalProperties")
        private Map<String, Object> hidden;

        @JsonAnySetter
        private void additionalProperties(String key, Object value) {
            if (additionalProperties == null) {
                additionalProperties = new HashMap<>();
            }
            additionalProperties.put(key.replace("\\.", "."), value);
        }

        @JsonAnyGetter
        public Map<String, Object> additionalProperties() {
            return additionalProperties;
        }

        public Map<String, Object> hidden() {
            return hidden;
        }

        public void hidden(Map<String, Object> additionalPropertiesProperty) {
            this.hidden = additionalPropertiesProperty;
        }
    }

    private final ObjectMapper MAPPER = newJsonMapper();

    @JacksonTestFailureExpected
    @Test
    public void anyGetter5342()
        throws Exception
    {
        Pojo5342 pojo = new Pojo5342();
        pojo.additionalProperties("foo", "bar");

        Map<String, Object> hidden = new HashMap<>();
        hidden.put("fizz", "buzz");
        pojo.hidden(hidden);


        String JSON = MAPPER.writeValueAsString(pojo);
        // was in 2.18 : {"foo":"bar","additionalProperties": {"fizz":"buzz"}}
        // now in 2.19 : {"foo":"bar"}... need FIX!
        // any-getter
        assertThat(JSON).contains("\"foo\":\"bar\"");
        // hidden field
        assertThat(JSON).contains("\"additionalProperties\":{\"fizz\":\"buzz\"}");

        // Try deserializing back
        Pojo5342 actual = MAPPER.readValue(JSON, Pojo5342.class);
        assertNotNull(actual.additionalProperties());
        assertEquals(1, actual.additionalProperties.size());
        assertNotNull(actual.hidden());
        assertEquals(1, actual.hidden().size());
    }

}
