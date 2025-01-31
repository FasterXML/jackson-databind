package tools.jackson.databind.tofix;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.*;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.testutil.DatabindTestUtil;
import tools.jackson.databind.testutil.failure.JacksonTestFailureExpected;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import static com.fasterxml.jackson.annotation.JsonProperty.Access.READ_ONLY;

// [databind#4792] JsonUnwrapped throwing "Unrecognized field" after upgrade to 2.18
public class PolymorphicDeserWithJsonUnwrapped4792Test
    extends DatabindTestUtil
{

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "name")
    @JsonSubTypes({
            @JsonSubTypes.Type(value = SubA.class, name = "A"),
    })
    interface Parent { }

    static class SubA implements Parent {
        @JsonUnwrapped
        @JsonProperty(access = READ_ONLY)
        Model model;

        @JsonCreator
        public SubA(@JsonProperty("model") Model model) {
            this.model = model;
        }
    }

    static class Model {
        public String name;
    }

    public static class Wrapper {
        public Parent model;
    }

    private final ObjectMapper objectMapper = newJsonMapper();

    @JacksonTestFailureExpected
    @Test
    public void testMainTest()
            throws Exception
    {
        Wrapper w = objectMapper.readValue(a2q("{'model':{'name':'A','name': 'Rick'}}"), Wrapper.class);

        assertInstanceOf(SubA.class, w.model);
        assertInstanceOf(Model.class, ((SubA) w.model).model);
        assertEquals("Rick", ((SubA) w.model).model.name);
    }

}
