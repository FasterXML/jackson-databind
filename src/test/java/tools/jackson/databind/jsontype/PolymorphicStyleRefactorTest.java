package tools.jackson.databind.jsontype;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

/**
 * The aim of this test is to show that it is possible to refactor code that
 * uses the dangerous `@JsonTypeInfo(use = Id.CLASS)` to the safer alternative
 * of `@JsonTypeInfo(use = Id.NAME)` without breaking message content compatibility.
 */
public class PolymorphicStyleRefactorTest extends DatabindTestUtil
{
    static class ClassWrap {
        static class Root {
            public Child child;

            public Root(Child child) {
                this.child = child;
            }
        }

        // property defaults to `@class` for use = Id.CLASS but making it explicit for this example
        @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, property = "@class")
        static abstract class Child {
        }

        static class ChildA extends Child {
            public String name;

            ChildA(String name) {
                this.name = name;
            }
        }

        static class ChildB extends Child {

            public String code;

            ChildB(String code) {
                this.code = code;
            }
        }
    }

    static class NameWrap {
        static class Root {
            public Child child;

            public Root(Child child) {
                this.child = child;
            }

        }

        // property defaults to `@name` for use = Id.NAME but making using `@class` to match RootChild above
        @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "@class")
        @JsonSubTypes({
                @JsonSubTypes.Type(value = ChildA.class, names = {"tools.jackson.databind.jsontype.PolymorphicStyleRefactorTest$ClassWrap$ChildA", "ChildA"}),
                @JsonSubTypes.Type(value = ChildB.class, names = {"tools.jackson.databind.jsontype.PolymorphicStyleRefactorTest$ClassWrap$ChildB", "ChildB"})
        })
        static abstract class Child {
        }

        static class ChildA extends Child {
            public String name;

            ChildA(String name) {
                this.name = name;
            }
        }

        static class ChildB extends Child {
            public String code;

            ChildB(String code) {
                this.code = code;
            }
        }
    }

    private final ObjectMapper MAPPER = JsonMapper.builder()
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .build();

    String expectedJson = a2q("{'child':{'@class':'tools.jackson.databind.jsontype.PolymorphicStyleRefactorTest$ClassWrap$ChildA','name':'Child A'}}");

    @Test
    public void testSerializationForIdClass() {
        String name = "Child A";
        ClassWrap.Root classRoot = new ClassWrap.Root(new ClassWrap.ChildA(name));
        String serialized = MAPPER.writeValueAsString(classRoot);
        assertEquals(expectedJson, serialized);
        ClassWrap.Root deserialized = MAPPER.readValue(serialized, ClassWrap.Root.class);
        ClassWrap.ChildA childA = assertInstanceOf(ClassWrap.ChildA.class, deserialized.child);
        assertEquals(name, childA.name);
    }

    @Test
    public void testSerializationForIdName() {
        String name = "Child A";
        NameWrap.Root nameRoot = new NameWrap.Root(new NameWrap.ChildA(name));
        String serialized = MAPPER.writeValueAsString(nameRoot);
        assertEquals(expectedJson, serialized);
        NameWrap.Root deserialized = MAPPER.readValue(serialized, NameWrap.Root.class);
        NameWrap.ChildA childA = assertInstanceOf(NameWrap.ChildA.class, deserialized.child);
        assertEquals(name, childA.name);
    }

    @Test
    public void testIdClassDeserializationWithIdRoot() {
        String name = "Child A";
        ClassWrap.Root classRoot = new ClassWrap.Root(new ClassWrap.ChildA(name));
        String serialized = MAPPER.writeValueAsString(classRoot);
        NameWrap.Root deserialized = MAPPER.readValue(serialized, NameWrap.Root.class);
        NameWrap.ChildA childA = assertInstanceOf(NameWrap.ChildA.class, deserialized.child);
        assertEquals(name, childA.name);
    }

}
