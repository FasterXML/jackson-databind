package tools.jackson.databind.ser.filter;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.*;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.exc.InvalidDefinitionException;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.assertTrue;

// Test case for https://github.com/FasterXML/jackson-databind/issues/1298:
// @JsonUnwrapped cannot be combined with @JsonIdentityInfo on property type
class ObjectIdWithUnwrapping1298Test extends DatabindTestUtil {
    static Long nextId = 1L;

    public static final class ListOfParents {
        public List<Parent> parents = new ArrayList<>();

        public void addParent(Parent parent) {
            parents.add(parent);
        }
    }

    @JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id", scope = Parent.class)
    public static final class Parent {
        public Long id;

        @JsonUnwrapped
        public Child child;

        public Parent() {
            this.id = nextId++;
        }
    }

    @JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id", scope = Child.class)
    public static final class Child {
        public Long id;

        public final String name;

        public Child(@JsonProperty("name") String name) {
            this.name = name;
            this.id = ObjectIdWithUnwrapping1298Test.nextId++;
        }
    }

    private final ObjectMapper MAPPER = newJsonMapper();

    // Should fail with clear error when trying to serialize type that uses
    // @JsonUnwrapped on property whose type has @JsonIdentityInfo
    @Test
    void objectIdWithUnwrappedNotSupported() throws Exception
    {
        ListOfParents parents = new ListOfParents();

        Parent parent1 = new Parent();
        Child child1 = new Child("Child1");
        parent1.child = child1;
        parents.addParent(parent1);

        try {
            MAPPER.writeValueAsString(parents);
        } catch (InvalidDefinitionException e) {
            verifyException(e, "cannot use `@JsonUnwrapped`");
            verifyException(e, "@JsonIdentityInfo");
            return;
        }
        // 14-Apr-2025: Should not get here; if it does, something is wrong
        assertTrue(false, "Should have thrown InvalidDefinitionException");
    }
}
