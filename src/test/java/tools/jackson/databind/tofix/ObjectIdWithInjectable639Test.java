package tools.jackson.databind.tofix;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.*;

import tools.jackson.databind.*;
import tools.jackson.databind.testutil.DatabindTestUtil;
import tools.jackson.databind.testutil.failure.JacksonTestFailureExpected;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class ObjectIdWithInjectable639Test extends DatabindTestUtil {
    // for [databind#639]
    @JsonIdentityInfo(generator = ObjectIdGenerators.IntSequenceGenerator.class)
    public static final class Parent2 {
        @JsonProperty
        public Child2 child;

        @JsonCreator
        public Parent2(@JacksonInject("context") String context) {
        }
    }

    @JsonIdentityInfo(generator = ObjectIdGenerators.IntSequenceGenerator.class)
    public static final class Child2 {
        @JsonProperty
        private final Parent2 parent;

        @JsonCreator
        public Child2(@JsonProperty("parent") Parent2 parent) {
            this.parent = parent;
        }
    }

    // for [databind#639]
    @JacksonTestFailureExpected
    @Test
    void objectIdWithInjectable() throws Exception
    {
        ObjectMapper mapper = jsonMapperBuilder()
                .injectableValues(new InjectableValues.Std().
                        addValue("context", "Stuff"))
                .build();
        Parent2 parent2 = new Parent2("foo");
        Child2 child2 = new Child2(parent2);
        parent2.child = child2;

        String json2 = mapper.writeValueAsString(parent2);
        parent2 = mapper.readValue(json2, Parent2.class);
        assertNotNull(parent2);
        assertNotNull(parent2.child);
    }
}
