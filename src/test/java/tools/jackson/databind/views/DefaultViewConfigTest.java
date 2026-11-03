package tools.jackson.databind.views;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.*;

import tools.jackson.databind.*;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests for configuring default serialization and deserialization views
 * at the mapper level (via builder).
 * Addresses [databind#5575].
 *
 * @since 3.1
 */
public class DefaultViewConfigTest extends DatabindTestUtil
{
    // Classes that represent views
    static class ViewPublic { }
    static class ViewInternal { }

    @JsonPropertyOrder({ "id", "name", "internalData" })
    static class Bean {
        @JsonView(ViewPublic.class)
        public int id = 1;

        @JsonView(ViewPublic.class)
        public String name = "Bob";

        @JsonView(ViewInternal.class)
        public String internalData = "secret";
    }

    /*
    /**********************************************************
    /* Tests: default serialization view
    /**********************************************************
     */

    @Test
    public void testDefaultSerializationView() throws Exception
    {
        // Configure mapper with default serialization view
        ObjectMapper mapper = jsonMapperBuilder()
                .defaultSerializationView(ViewPublic.class)
                .build();

        Bean bean = new Bean();

        // Should use ViewPublic by default
        String json = mapper.writeValueAsString(bean);
        assertEquals(a2q("{'id':1,'name':'Bob'}"), json);
    }

    @Test
    public void testDefaultSerializationViewOverride() throws Exception
    {
        // Configure mapper with default serialization view
        ObjectMapper mapper = jsonMapperBuilder()
                .defaultSerializationView(ViewPublic.class)
                .build();

        Bean bean = new Bean();

        // Override default view with writer
        String json = mapper.writerWithView(ViewInternal.class)
                .writeValueAsString(bean);
        assertEquals(a2q("{'internalData':'secret'}"), json);

        // No view on writer should use default
        json = mapper.writer().writeValueAsString(bean);
        assertEquals(a2q("{'id':1,'name':'Bob'}"), json);
    }

    /*
    /**********************************************************
    /* Tests: default deserialization view
    /**********************************************************
     */

    @Test
    public void testDefaultDeserializationView() throws Exception
    {
        // Configure mapper with default deserialization view
        ObjectMapper mapper = jsonMapperBuilder()
                .defaultDeserializationView(ViewPublic.class)
                .disable(DeserializationFeature.FAIL_ON_UNEXPECTED_VIEW_PROPERTIES)
                .build();

        String json = a2q("{'id':99,'name':'Alice','internalData':'hacked'}");

        // Should use ViewPublic by default (internal data not set)
        Bean bean = mapper.readerFor(Bean.class).readValue(json);
        assertEquals(99, bean.id);
        assertEquals("Alice", bean.name);
        assertEquals("secret", bean.internalData); // default value, not from JSON
    }

    @Test
    public void testDefaultDeserializationViewOverride() throws Exception
    {
        // Configure mapper with default deserialization view
        ObjectMapper mapper = jsonMapperBuilder()
                .defaultDeserializationView(ViewPublic.class)
                .disable(DeserializationFeature.FAIL_ON_UNEXPECTED_VIEW_PROPERTIES)
                .build();

        String json = a2q("{'id':99,'name':'Alice','internalData':'hacked'}");

        // Override default view with reader
        Bean bean = mapper.readerFor(Bean.class)
                .withView(ViewInternal.class)
                .readValue(json);
        assertEquals(1, bean.id); // default value, not from JSON
        assertEquals("Bob", bean.name); // default value, not from JSON
        assertEquals("hacked", bean.internalData);

        // No view on reader should use default
        bean = mapper.readerFor(Bean.class).readValue(json);
        assertEquals(99, bean.id);
        assertEquals("Alice", bean.name);
        assertEquals("secret", bean.internalData); // default value
    }

    /*
    /**********************************************************
    /* Tests: convenience method for both ser and deser
    /**********************************************************
     */

    @Test
    public void testDefaultViewBothSerAndDeser() throws Exception
    {
        // Configure mapper with default view for both serialization and deserialization
        ObjectMapper mapper = jsonMapperBuilder()
                .defaultView(ViewPublic.class)
                .disable(DeserializationFeature.FAIL_ON_UNEXPECTED_VIEW_PROPERTIES)
                .build();

        Bean bean = new Bean();

        // Serialization should use ViewPublic
        String json = mapper.writeValueAsString(bean);
        assertEquals(a2q("{'id':1,'name':'Bob'}"), json);

        // Deserialization should use ViewPublic
        String inputJson = a2q("{'id':99,'name':'Alice','internalData':'hacked'}");
        Bean result = mapper.readerFor(Bean.class).readValue(inputJson);
        assertEquals(99, result.id);
        assertEquals("Alice", result.name);
        assertEquals("secret", result.internalData); // default value
    }

    /*
    /**********************************************************
    /* Tests: separate ser/deser views
    /**********************************************************
     */

    @Test
    public void testSeparateSerializationAndDeserializationViews() throws Exception
    {
        // Configure different default views for serialization and deserialization
        ObjectMapper mapper = jsonMapperBuilder()
                .defaultSerializationView(ViewPublic.class)
                .defaultDeserializationView(ViewInternal.class)
                .disable(DeserializationFeature.FAIL_ON_UNEXPECTED_VIEW_PROPERTIES)
                .build();

        Bean bean = new Bean();

        // Serialization should use ViewPublic
        String json = mapper.writeValueAsString(bean);
        assertEquals(a2q("{'id':1,'name':'Bob'}"), json);

        // Deserialization should use ViewInternal
        String inputJson = a2q("{'id':99,'name':'Alice','internalData':'hacked'}");
        Bean result = mapper.readerFor(Bean.class).readValue(inputJson);
        assertEquals(1, result.id); // default value
        assertEquals("Bob", result.name); // default value
        assertEquals("hacked", result.internalData);
    }

    /*
    /**********************************************************
    /* Tests: no default view (null)
    /**********************************************************
     */

    @Test
    public void testNoDefaultView() throws Exception
    {
        // Mapper with no default view
        ObjectMapper mapper = jsonMapperBuilder().build();

        Bean bean = new Bean();

        // Without view, all properties should be serialized
        String json = mapper.writeValueAsString(bean);
        assertEquals(a2q("{'id':1,'name':'Bob','internalData':'secret'}"), json);

        // Without view, all properties should be deserialized
        String inputJson = a2q("{'id':99,'name':'Alice','internalData':'hacked'}");
        Bean result = mapper.readerFor(Bean.class).readValue(inputJson);
        assertEquals(99, result.id);
        assertEquals("Alice", result.name);
        assertEquals("hacked", result.internalData);
    }

    /*
    /**********************************************************
    /* Tests: rebuild preserves default views
    /**********************************************************
     */

    @Test
    public void testRebuildPreservesDefaultViews() throws Exception
    {
        // Configure mapper with default views
        ObjectMapper mapper1 = jsonMapperBuilder()
                .defaultSerializationView(ViewPublic.class)
                .defaultDeserializationView(ViewInternal.class)
                .disable(DeserializationFeature.FAIL_ON_UNEXPECTED_VIEW_PROPERTIES)
                .build();

        // Rebuild mapper
        ObjectMapper mapper2 = mapper1.rebuild().build();

        Bean bean = new Bean();

        // Rebuilt mapper should preserve default serialization view
        String json = mapper2.writeValueAsString(bean);
        assertEquals(a2q("{'id':1,'name':'Bob'}"), json);

        // Rebuilt mapper should preserve default deserialization view
        String inputJson = a2q("{'id':99,'name':'Alice','internalData':'hacked'}");
        Bean result = mapper2.readerFor(Bean.class).readValue(inputJson);
        assertEquals(1, result.id); // default value
        assertEquals("Bob", result.name); // default value
        assertEquals("hacked", result.internalData);
    }

    /*
    /**********************************************************
    /* Tests: setting null view clears previous value
    /**********************************************************
     */

    @Test
    public void testNullViewClearsPrevious() throws Exception
    {
        // Configure mapper with default views, then clear them
        ObjectMapper mapper = jsonMapperBuilder()
                .defaultView(ViewPublic.class)
                .defaultView(null) // clear the default view
                .build();

        Bean bean = new Bean();

        // Without view, all properties should be serialized
        String json = mapper.writeValueAsString(bean);
        assertEquals(a2q("{'id':1,'name':'Bob','internalData':'secret'}"), json);
    }
}
