package tools.jackson.databind.ser;

import java.io.*;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.ObjectWriter;
import tools.jackson.databind.cfg.GeneratorInitializer;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link GeneratorInitializer} support (issue #5860).
 */
public class GeneratorInitializerTest extends DatabindTestUtil
{
    @Test
    public void testInitializerCalledViaMapper() throws Exception
    {
        final AtomicInteger count = new AtomicInteger();
        ObjectMapper mapper = JsonMapper.builder()
                .generatorInitializer((config, gen) -> count.incrementAndGet())
                .build();

        // writeValueAsString creates a generator internally
        String json = mapper.writeValueAsString(42);
        assertEquals("42", json);
        assertEquals(1, count.get());
    }

    @Test
    public void testInitializerCalledViaObjectWriter() throws Exception
    {
        final AtomicInteger count = new AtomicInteger();
        ObjectMapper mapper = JsonMapper.builder()
                .generatorInitializer((config, gen) -> count.incrementAndGet())
                .build();
        ObjectWriter writer = mapper.writer();

        String json = writer.writeValueAsString("hello");
        assertEquals("\"hello\"", json);
        assertEquals(1, count.get());

        // Second call should invoke again
        writer.writeValueAsString("world");
        assertEquals(2, count.get());
    }

    @Test
    public void testInitializerOnObjectWriterWithOverride() throws Exception
    {
        final AtomicInteger count1 = new AtomicInteger();
        final AtomicInteger count2 = new AtomicInteger();
        ObjectMapper mapper = JsonMapper.builder()
                .generatorInitializer((config, gen) -> count1.incrementAndGet())
                .build();

        ObjectWriter writer = mapper.writer()
                .withGeneratorInitializer((config, gen) -> count2.incrementAndGet());

        writer.writeValueAsString(42);
        assertEquals(0, count1.get());
        assertEquals(1, count2.get());
    }

    @Test
    public void testInitializerCalledOnCreateGenerator() throws Exception
    {
        final AtomicInteger count = new AtomicInteger();
        ObjectMapper mapper = JsonMapper.builder()
                .generatorInitializer((config, gen) -> count.incrementAndGet())
                .build();

        // createGenerator should also invoke the initializer
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (JsonGenerator gen = mapper.createGenerator(out)) {
            gen.writeNumber(1);
        }
        assertEquals(1, count.get());
    }

    @Test
    public void testInitializerNotCalledForProvidedGenerator() throws Exception
    {
        final AtomicInteger count = new AtomicInteger();
        ObjectMapper mapper = JsonMapper.builder()
                .generatorInitializer((config, gen) -> count.incrementAndGet())
                .build();
        ObjectWriter writer = mapper.writer();

        // Use externally-created generator: initializer should be called once
        // (for createGenerator), but NOT again for writeValue(gen, ...)
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (JsonGenerator gen = mapper.createGenerator(out)) {
            assertEquals(1, count.get()); // from createGenerator
            writer.writeValue(gen, 42);
            assertEquals(1, count.get()); // NOT called again
        }
    }

    @Test
    public void testNoInitializerByDefault() throws Exception
    {
        ObjectMapper mapper = new JsonMapper();
        assertNull(mapper.serializationConfig().getGeneratorInitializer());
        // Should still work fine
        assertEquals("42", mapper.writeValueAsString(42));
    }

    @Test
    public void testInitializerCalledOnWriteValueOutputStream() throws Exception
    {
        final AtomicInteger count = new AtomicInteger();
        ObjectMapper mapper = JsonMapper.builder()
                .generatorInitializer((config, gen) -> count.incrementAndGet())
                .build();

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        mapper.writeValue(out, "test");
        assertEquals(1, count.get());
        assertEquals("\"test\"", out.toString("UTF-8"));
    }

    @Test
    public void testInitializerCalledOnWriteValueAsBytes() throws Exception
    {
        final AtomicInteger count = new AtomicInteger();
        ObjectMapper mapper = JsonMapper.builder()
                .generatorInitializer((config, gen) -> count.incrementAndGet())
                .build();

        byte[] bytes = mapper.writeValueAsBytes(42);
        assertEquals("42", new String(bytes, "UTF-8"));
        assertEquals(1, count.get());
    }
}
