package tools.jackson.databind.deser;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.*;

import tools.jackson.databind.*;
import tools.jackson.databind.exc.InvalidDefinitionException;

import static org.junit.jupiter.api.Assertions.*;

import static tools.jackson.databind.testutil.DatabindTestUtil.*;

/**
 * Tests for deserialization of structural/complex object patterns:
 * inner classes, object-or-array delegation, and cyclic references.
 */
public class StructuralTypeDeserTest
{
    /*
    /**********************************************************************
    /* Helper classes for inner class tests
    /**********************************************************************
     */

    static class Dog
    {
      public String name;
      public Brain brain;

      public Dog() { }
      protected Dog(String n, boolean thinking) {
          name = n;
          brain = new Brain();
          brain.isThinking = thinking;
      }

      // note: non-static
      public class Brain {
          @JsonProperty("brainiac")
          public boolean isThinking;

          public String parentName() { return name; }
      }
    }

    /*
    /**********************************************************************
    /* Helper classes for object-or-array delegation tests
    /**********************************************************************
     */

    public static class SomeObject {
        public String someField;
    }

    public static class ArrayOrObject {
        final List<SomeObject> objects;
        final SomeObject object;

        @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
        public ArrayOrObject(List<SomeObject> objects) {
            this.objects = objects;
            this.object = null;
        }

        @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
        public ArrayOrObject(SomeObject object) {
            this.objects = null;
            this.object = object;
        }
    }

    /*
    /**********************************************************************
    /* Helper classes for cyclic type tests
    /**********************************************************************
     */

    static class CyclicBean
    {
        CyclicBean _next;
        String _name;

        public CyclicBean() { }

        public void setNext(CyclicBean b) { _next = b; }
        public void setName(String n) { _name = n; }

    }

    static class LinkA {
        public LinkB next;
    }

    static class LinkB {
        protected LinkA a;

        public void setA(LinkA a) { this.a = a; }
        public LinkA getA() { return a; }
    }

    static class GenericLink<T> {
        public GenericLink<T> next;
    }

    static class StringLink extends GenericLink<String> {
    }

    @JsonPropertyOrder({ "id", "parent" })
    static class Selfie405 {
        public int id;

        @JsonIgnoreProperties({ "parent" })
        public Selfie405 parent;

        public Selfie405(int id) { this.id = id; }
    }

    private final ObjectMapper MAPPER = newJsonMapper();

    /*
    /**********************************************************
    /* Test methods, object-or-array delegation
    /**********************************************************
     */

    @Test
    public void testObjectCase() throws Exception {
        ArrayOrObject arrayOrObject = MAPPER.readValue("{}", ArrayOrObject.class);
        assertNull(arrayOrObject.objects, "expected objects field to be null");
        assertNotNull(arrayOrObject.object, "expected object field not to be null");
    }

    @Test
    public void testEmptyArrayCase() throws Exception {
        ArrayOrObject arrayOrObject = MAPPER.readValue("[]", ArrayOrObject.class);
        assertNotNull(arrayOrObject.objects, "expected objects field not to be null");
        assertTrue(arrayOrObject.objects.isEmpty(), "expected objects field to be an empty list");
        assertNull(arrayOrObject.object, "expected object field to be null");
    }

    @Test
    public void testNotEmptyArrayCase() throws Exception {
        ArrayOrObject arrayOrObject = MAPPER.readValue("[{}, {}]", ArrayOrObject.class);
        assertNotNull(arrayOrObject.objects, "expected objects field not to be null");
        assertEquals(2, arrayOrObject.objects.size(), "expected objects field to have size 2");
        assertNull(arrayOrObject.object, "expected object field to be null");
    }

    /*
    /**********************************************************
    /* Test methods, cyclic types
    /**********************************************************
     */

    @Test
    public void testLinked() throws Exception
    {
        CyclicBean first = MAPPER.readValue
            ("{\"name\":\"first\", \"next\": { "
             +" \"name\":\"last\", \"next\" : null }}",
             CyclicBean.class);

        assertNotNull(first);
        assertEquals("first", first._name);
        CyclicBean last = first._next;
        assertNotNull(last);
        assertEquals("last", last._name);
        assertNull(last._next);
    }

    @Test
    public void testLinkedGeneric() throws Exception
    {
        StringLink link = MAPPER.readValue("{\"next\":null}", StringLink.class);
        assertNotNull(link);
        assertNull(link.next);
    }

    @Test
    public void testCycleWith2Classes() throws Exception
    {
        LinkA a = MAPPER.readValue("{\"next\":{\"a\":null}}", LinkA.class);
        assertNotNull(a.next);
        LinkB b = a.next;
        assertNull(b.a);
    }

    // [Issue#405]: Should be possible to ignore cyclic ref
    @Test
    public void testIgnoredCycle() throws Exception
    {
        Selfie405 self1 = new Selfie405(1);
        self1.parent = self1;

        // First: exception with default settings:
        assertTrue(MAPPER.isEnabled(SerializationFeature.FAIL_ON_SELF_REFERENCES));
        try {
            MAPPER.writeValueAsString(self1);
            fail("Should fail with direct self-ref");
        } catch (InvalidDefinitionException e) {
            verifyException(e, "Direct self-reference");
        }

        ObjectWriter w = MAPPER.writer()
                .without(SerializationFeature.FAIL_ON_SELF_REFERENCES);
        String json = w.writeValueAsString(self1);
        assertNotNull(json);
        assertEquals(a2q("{'id':1,'parent':{'id':1}}"), json);
    }

    /*
    /**********************************************************************
    /* Test methods, inner classes
    /**********************************************************************
     */

    @Test
    public void testSimpleNonStaticInner() throws Exception
    {
        // Let's actually verify by first serializing, then deserializing back
        Dog input = new Dog("Smurf", true);
        String json = MAPPER.writeValueAsString(input);
        Dog output = MAPPER.readValue(json, Dog.class);
        assertEquals("Smurf", output.name);
        assertNotNull(output.brain);
        assertTrue(output.brain.isThinking);
        // and verify correct binding...
        assertEquals("Smurf", output.brain.parentName());
        output.name = "Foo";
        assertEquals("Foo", output.brain.parentName());

        // also, null handling
        input.brain = null;

        output = MAPPER.readValue(MAPPER.writeValueAsString(input), Dog.class);
        assertNull(output.brain);
    }
}
