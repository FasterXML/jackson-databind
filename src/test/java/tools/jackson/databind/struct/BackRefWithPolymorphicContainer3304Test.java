package tools.jackson.databind.struct;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.*;

import tools.jackson.databind.*;
import tools.jackson.databind.exc.InvalidDefinitionException;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.*;

// [databind#3304]: Better error message when @JsonBackReference is only on
// subtypes, not on the declared (abstract/interface) element type of a container.
public class BackRefWithPolymorphicContainer3304Test extends DatabindTestUtil
{
    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME,
            include = JsonTypeInfo.As.PROPERTY,
            property = "type")
    @JsonSubTypes({
        @JsonSubTypes.Type(value = SimpleChild.class, name = "SIMPLE"),
        @JsonSubTypes.Type(value = RefChild.class, name = "REF")
    })
    interface IChild { }

    static class SimpleChild implements IChild {
        public String value;

        public SimpleChild() { }
        public SimpleChild(String v) { value = v; }
    }

    // Back reference is ONLY on this subtype, NOT on the interface
    static class RefChild implements IChild {
        public String data;

        @JsonBackReference
        public Container parent;

        public RefChild() { }
        public RefChild(String d) { data = d; }
    }

    static class Container {
        @JsonManagedReference
        public IChild[] children;
    }

    // Variant with List instead of array
    static class ContainerWithList {
        @JsonManagedReference
        public java.util.List<IChild> children;
    }

    private final ObjectMapper MAPPER = newJsonMapper();

    // Verify error message mentions the abstract type issue
    @Test
    public void testBackRefOnSubtypeOnly_array() {
        InvalidDefinitionException ex = assertThrows(
                InvalidDefinitionException.class,
                () -> MAPPER.readValue(
                        "{\"children\":[{\"type\":\"SIMPLE\",\"value\":\"x\"}]}",
                        Container.class));
        String msg = ex.getMessage();
        assertNotNull(msg);
        // Verify improved error message mentions the abstract/interface issue
        assertTrue(msg.contains("abstract"),
                "Error message should mention abstract type issue, got: " + msg);
        assertTrue(msg.contains("@JsonBackReference"),
                "Error message should mention @JsonBackReference, got: " + msg);
    }

    @Test
    public void testBackRefOnSubtypeOnly_list() {
        InvalidDefinitionException ex = assertThrows(
                InvalidDefinitionException.class,
                () -> MAPPER.readValue(
                        "{\"children\":[{\"type\":\"SIMPLE\",\"value\":\"x\"}]}",
                        ContainerWithList.class));
        String msg = ex.getMessage();
        assertNotNull(msg);
        assertTrue(msg.contains("abstract"),
                "Error message should mention abstract type issue, got: " + msg);
        assertTrue(msg.contains("@JsonBackReference"),
                "Error message should mention @JsonBackReference, got: " + msg);
    }
}
