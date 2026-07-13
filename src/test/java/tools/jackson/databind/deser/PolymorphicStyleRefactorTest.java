package tools.jackson.databind.deser;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

/**
 * The aim of this test is to show that it is possible to refactor code that
 * uses the dangerous `@JsonTypeInfo(use = Id.CLASS)` to the safer alternative
 * of `@JsonTypeInfo(use = Id.NAME)` without breaking message content compatibility.
 */
public class PolymorphicStyleRefactorTest
{

    static class ClassRoot {
        public ClassChild child;
    }

    @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS)
    static abstract class ClassChild {
    }

    static class ClassChildA extends ClassChild {
        public String name;
    }

    static class ClassChildB extends ClassChild {
        public String code;
    }

    static class NameRoot {
        public NameChild child;
    }

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME)
    @JsonSubTypes({
        @JsonSubTypes.Type(value = NameChildA.class, name = "ChildA"),
        @JsonSubTypes.Type(value = NameChildB.class, name = "ChildB")
    })
    static abstract class NameChild {
    }

    static class NameChildA extends NameChild {
        public String name;
    }

    static class NameChildB extends NameChild {
        public String code;
    }

    private final ObjectMapper MAPPER = JsonMapper.builder()
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .build();

    @Test
    public void testPolymorphicNewObject() throws Exception {
        NameRoot root = MAPPER.readValue("{\"child\": { \"@type\": \"ChildA\", \"name\": \"I'm child A\" }}", NameRoot.class);
        NameChildA childA = assertInstanceOf(NameChildA.class, root.child);
        assertEquals("I'm child A", childA.name);
    }

}
