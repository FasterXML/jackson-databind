package tools.jackson.databind.deser.filter;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.*;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Tests for per-property {@code @JsonIgnoreProperties} / {@code @JsonIncludeProperties}
 * excluding a property with an external type id ({@code As.EXTERNAL_PROPERTY}):
 * such properties are only known to the {@code ExternalTypeHandler}, which must not
 * bind them either.
 */
public class IgnorePropertiesExternalTypeIdTest extends DatabindTestUtil
{
    static abstract class Animal {
        public String name;
    }

    static class Dog extends Animal { }

    // Property-based Creator, external-typed property NOT a Creator property
    static class CreatorBean {
        public final String id;

        @JsonTypeInfo(use = JsonTypeInfo.Id.NAME,
                include = JsonTypeInfo.As.EXTERNAL_PROPERTY, property = "type")
        @JsonSubTypes({ @JsonSubTypes.Type(value = Dog.class, name = "dog") })
        public Animal value;

        @JsonCreator
        public CreatorBean(@JsonProperty("id") String id) {
            this.id = id;
        }
    }

    // Same but with default constructor
    static class PlainBean {
        public String id;

        @JsonTypeInfo(use = JsonTypeInfo.Id.NAME,
                include = JsonTypeInfo.As.EXTERNAL_PROPERTY, property = "type")
        @JsonSubTypes({ @JsonSubTypes.Type(value = Dog.class, name = "dog") })
        public Animal value;
    }

    static class CreatorIgnoringWrapper {
        @JsonIgnoreProperties("value")
        public CreatorBean bean;
    }

    static class CreatorIncludingWrapper {
        @JsonIncludeProperties("id")
        public CreatorBean bean;
    }

    static class PlainIgnoringWrapper {
        @JsonIgnoreProperties("value")
        public PlainBean bean;
    }

    static class PlainIncludingWrapper {
        @JsonIncludeProperties("id")
        public PlainBean bean;
    }

    static class CreatorWrapper {
        public CreatorBean bean;
    }

    static class PlainWrapper {
        public PlainBean bean;
    }

    private final ObjectMapper MAPPER = newJsonMapper();

    // Type id and value in either order; also external type id before Creator property
    private final static String[] DOCS = new String[] {
            """
            {"bean":{"id":"x","type":"dog","value":{"name":"Rex"}}}
            """,
            """
            {"bean":{"id":"x","value":{"name":"Rex"},"type":"dog"}}
            """,
            """
            {"bean":{"type":"dog","value":{"name":"Rex"},"id":"x"}}
            """
    };

    @Test
    public void ignoredExternalTypedPropertyWithCreator() throws Exception
    {
        for (String doc : DOCS) {
            CreatorIgnoringWrapper ign = MAPPER.readValue(doc, CreatorIgnoringWrapper.class);
            assertEquals("x", ign.bean.id);
            assertNull(ign.bean.value);

            CreatorIncludingWrapper incl = MAPPER.readValue(doc, CreatorIncludingWrapper.class);
            assertEquals("x", incl.bean.id);
            assertNull(incl.bean.value);
        }
    }

    @Test
    public void ignoredExternalTypedPropertyWithoutCreator() throws Exception
    {
        for (String doc : DOCS) {
            PlainIgnoringWrapper ign = MAPPER.readValue(doc, PlainIgnoringWrapper.class);
            assertEquals("x", ign.bean.id);
            assertNull(ign.bean.value);

            PlainIncludingWrapper incl = MAPPER.readValue(doc, PlainIncludingWrapper.class);
            assertEquals("x", incl.bean.id);
            assertNull(incl.bean.value);
        }
    }

    // Without exclusion, same documents must still bind the value
    @Test
    public void externalTypedPropertyNotIgnored() throws Exception
    {
        for (String doc : DOCS) {
            CreatorWrapper c = MAPPER.readValue(doc, CreatorWrapper.class);
            assertEquals("Rex", c.bean.value.name);
            PlainWrapper p = MAPPER.readValue(doc, PlainWrapper.class);
            assertEquals("Rex", p.bean.value.name);
        }
    }
}
