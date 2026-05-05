package tools.jackson.databind.ser;

import java.io.*;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import tools.jackson.core.JsonEncoding;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.ObjectWriter;
import tools.jackson.databind.SequenceWriter;
import tools.jackson.databind.cfg.GeneratorInitializer;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link GeneratorInitializer} support (issue #5860).
 * Verifies that the initializer is called exactly once per internally-created
 * generator and never for externally-provided generators.
 */
public class GeneratorInitializerTest extends DatabindTestUtil
{
    // Helper to build a mapper with a counting initializer
    private ObjectMapper _mapperWith(AtomicInteger count) {
        return JsonMapper.builder()
                .generatorInitializer((config, gen) -> count.incrementAndGet())
                .build();
    }

    // Helper to build an ObjectWriter with a counting initializer
    private ObjectWriter _writerWith(AtomicInteger count) {
        return _mapperWith(count).writer();
    }

    /*
    /**********************************************************************
    /* Basic configuration tests
    /**********************************************************************
     */

    @Test
    public void testNoInitializerByDefault() throws Exception
    {
        ObjectMapper mapper = new JsonMapper();
        assertNull(mapper.serializationConfig().getGeneratorInitializer());
        assertEquals("42", mapper.writeValueAsString(42));
    }

    @Test
    public void testInitializerOverrideOnObjectWriter() throws Exception
    {
        final AtomicInteger count1 = new AtomicInteger();
        final AtomicInteger count2 = new AtomicInteger();
        ObjectWriter writer = _mapperWith(count1).writer()
                .with((config, gen) -> count2.incrementAndGet());

        writer.writeValueAsString(42);
        assertEquals(0, count1.get(), "Original initializer should NOT be called");
        assertEquals(1, count2.get(), "Override initializer should be called once");
    }

    @Test
    public void testInitializerClearedWithNull() throws Exception
    {
        final AtomicInteger count = new AtomicInteger();
        ObjectWriter writer = _mapperWith(count).writer()
                .with((GeneratorInitializer) null);

        writer.writeValueAsString(42);
        assertEquals(0, count.get(), "No initializer should be called after clearing");
    }

    /*
    /**********************************************************************
    /* ObjectMapper.createGenerator(): exactly once
    /**********************************************************************
     */

    @Test
    public void testMapperCreateGeneratorOutputStream() throws Exception
    {
        final AtomicInteger count = new AtomicInteger();
        ObjectMapper mapper = _mapperWith(count);
        try (JsonGenerator g = mapper.createGenerator(new ByteArrayOutputStream())) {
            assertEquals(1, count.get());
            g.writeNumber(1);
        }
        assertEquals(1, count.get());
    }

    @Test
    public void testMapperCreateGeneratorOutputStreamEncoding() throws Exception
    {
        final AtomicInteger count = new AtomicInteger();
        ObjectMapper mapper = _mapperWith(count);
        try (JsonGenerator g = mapper.createGenerator(new ByteArrayOutputStream(), JsonEncoding.UTF8)) {
            assertEquals(1, count.get());
            g.writeNumber(1);
        }
        assertEquals(1, count.get());
    }

    @Test
    public void testMapperCreateGeneratorWriter() throws Exception
    {
        final AtomicInteger count = new AtomicInteger();
        ObjectMapper mapper = _mapperWith(count);
        try (JsonGenerator g = mapper.createGenerator(new StringWriter())) {
            assertEquals(1, count.get());
            g.writeNumber(1);
        }
        assertEquals(1, count.get());
    }

    @Test
    public void testMapperCreateGeneratorFile(@TempDir Path tempDir) throws Exception
    {
        final AtomicInteger count = new AtomicInteger();
        ObjectMapper mapper = _mapperWith(count);
        File f = tempDir.resolve("test.json").toFile();
        try (JsonGenerator g = mapper.createGenerator(f, JsonEncoding.UTF8)) {
            assertEquals(1, count.get());
            g.writeNumber(1);
        }
        assertEquals(1, count.get());
    }

    @Test
    public void testMapperCreateGeneratorPath(@TempDir Path tempDir) throws Exception
    {
        final AtomicInteger count = new AtomicInteger();
        ObjectMapper mapper = _mapperWith(count);
        Path p = tempDir.resolve("test.json");
        try (JsonGenerator g = mapper.createGenerator(p, JsonEncoding.UTF8)) {
            assertEquals(1, count.get());
            g.writeNumber(1);
        }
        assertEquals(1, count.get());
    }

    @Test
    public void testMapperCreateGeneratorDataOutput() throws Exception
    {
        final AtomicInteger count = new AtomicInteger();
        ObjectMapper mapper = _mapperWith(count);
        try (JsonGenerator g = mapper.createGenerator((DataOutput) new DataOutputStream(new ByteArrayOutputStream()))) {
            assertEquals(1, count.get());
            g.writeNumber(1);
        }
        assertEquals(1, count.get());
    }

    /*
    /**********************************************************************
    /* ObjectMapper.writeValue*(): exactly once
    /**********************************************************************
     */

    @Test
    public void testMapperWriteValueAsString() throws Exception
    {
        final AtomicInteger count = new AtomicInteger();
        ObjectMapper mapper = _mapperWith(count);
        assertEquals("42", mapper.writeValueAsString(42));
        assertEquals(1, count.get());
    }

    @Test
    public void testMapperWriteValueAsBytes() throws Exception
    {
        final AtomicInteger count = new AtomicInteger();
        ObjectMapper mapper = _mapperWith(count);
        byte[] bytes = mapper.writeValueAsBytes(42);
        assertEquals("42", new String(bytes, "UTF-8"));
        assertEquals(1, count.get());
    }

    @Test
    public void testMapperWriteValueOutputStream() throws Exception
    {
        final AtomicInteger count = new AtomicInteger();
        ObjectMapper mapper = _mapperWith(count);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        mapper.writeValue(out, "test");
        assertEquals(1, count.get());
    }

    @Test
    public void testMapperWriteValueWriter() throws Exception
    {
        final AtomicInteger count = new AtomicInteger();
        ObjectMapper mapper = _mapperWith(count);
        mapper.writeValue(new StringWriter(), "test");
        assertEquals(1, count.get());
    }

    @Test
    public void testMapperWriteValueFile(@TempDir Path tempDir) throws Exception
    {
        final AtomicInteger count = new AtomicInteger();
        ObjectMapper mapper = _mapperWith(count);
        mapper.writeValue(tempDir.resolve("test.json").toFile(), "test");
        assertEquals(1, count.get());
    }

    @Test
    public void testMapperWriteValuePath(@TempDir Path tempDir) throws Exception
    {
        final AtomicInteger count = new AtomicInteger();
        ObjectMapper mapper = _mapperWith(count);
        mapper.writeValue(tempDir.resolve("test.json"), "test");
        assertEquals(1, count.get());
    }

    @Test
    public void testMapperWriteValueDataOutput() throws Exception
    {
        final AtomicInteger count = new AtomicInteger();
        ObjectMapper mapper = _mapperWith(count);
        mapper.writeValue((DataOutput) new DataOutputStream(new ByteArrayOutputStream()), "test");
        assertEquals(1, count.get());
    }

    /*
    /**********************************************************************
    /* ObjectMapper: NOT called for user-provided generators
    /**********************************************************************
     */

    @Test
    public void testMapperWriteValueWithProvidedGenerator() throws Exception
    {
        final AtomicInteger count = new AtomicInteger();
        ObjectMapper mapper = _mapperWith(count);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        // createGenerator fires once
        try (JsonGenerator g = mapper.createGenerator(out)) {
            assertEquals(1, count.get());
            // writeValue(gen, ...) must NOT fire again
            mapper.writeValue(g, 42);
            assertEquals(1, count.get());
        }
    }

    /*
    /**********************************************************************
    /* ObjectWriter.createGenerator(): exactly once
    /**********************************************************************
     */

    @Test
    public void testWriterCreateGeneratorOutputStream() throws Exception
    {
        final AtomicInteger count = new AtomicInteger();
        ObjectWriter w = _writerWith(count);
        try (JsonGenerator g = w.createGenerator(new ByteArrayOutputStream())) {
            assertEquals(1, count.get());
            g.writeNumber(1);
        }
        assertEquals(1, count.get());
    }

    @Test
    public void testWriterCreateGeneratorOutputStreamEncoding() throws Exception
    {
        final AtomicInteger count = new AtomicInteger();
        ObjectWriter w = _writerWith(count);
        try (JsonGenerator g = w.createGenerator(new ByteArrayOutputStream(), JsonEncoding.UTF8)) {
            assertEquals(1, count.get());
            g.writeNumber(1);
        }
        assertEquals(1, count.get());
    }

    @Test
    public void testWriterCreateGeneratorWriter() throws Exception
    {
        final AtomicInteger count = new AtomicInteger();
        ObjectWriter w = _writerWith(count);
        try (JsonGenerator g = w.createGenerator(new StringWriter())) {
            assertEquals(1, count.get());
            g.writeNumber(1);
        }
        assertEquals(1, count.get());
    }

    @Test
    public void testWriterCreateGeneratorFile(@TempDir Path tempDir) throws Exception
    {
        final AtomicInteger count = new AtomicInteger();
        ObjectWriter w = _writerWith(count);
        File f = tempDir.resolve("test.json").toFile();
        try (JsonGenerator g = w.createGenerator(f, JsonEncoding.UTF8)) {
            assertEquals(1, count.get());
            g.writeNumber(1);
        }
        assertEquals(1, count.get());
    }

    @Test
    public void testWriterCreateGeneratorPath(@TempDir Path tempDir) throws Exception
    {
        final AtomicInteger count = new AtomicInteger();
        ObjectWriter w = _writerWith(count);
        Path p = tempDir.resolve("test.json");
        try (JsonGenerator g = w.createGenerator(p, JsonEncoding.UTF8)) {
            assertEquals(1, count.get());
            g.writeNumber(1);
        }
        assertEquals(1, count.get());
    }

    @Test
    public void testWriterCreateGeneratorDataOutput() throws Exception
    {
        final AtomicInteger count = new AtomicInteger();
        ObjectWriter w = _writerWith(count);
        try (JsonGenerator g = w.createGenerator((DataOutput) new DataOutputStream(new ByteArrayOutputStream()))) {
            assertEquals(1, count.get());
            g.writeNumber(1);
        }
        assertEquals(1, count.get());
    }

    /*
    /**********************************************************************
    /* ObjectWriter.writeValue*(): exactly once
    /**********************************************************************
     */

    @Test
    public void testWriterWriteValueAsString() throws Exception
    {
        final AtomicInteger count = new AtomicInteger();
        ObjectWriter w = _writerWith(count);
        assertEquals("42", w.writeValueAsString(42));
        assertEquals(1, count.get());
    }

    @Test
    public void testWriterWriteValueAsBytes() throws Exception
    {
        final AtomicInteger count = new AtomicInteger();
        ObjectWriter w = _writerWith(count);
        byte[] bytes = w.writeValueAsBytes(42);
        assertEquals("42", new String(bytes, "UTF-8"));
        assertEquals(1, count.get());
    }

    @Test
    public void testWriterWriteValueOutputStream() throws Exception
    {
        final AtomicInteger count = new AtomicInteger();
        ObjectWriter w = _writerWith(count);
        w.writeValue(new ByteArrayOutputStream(), "test");
        assertEquals(1, count.get());
    }

    @Test
    public void testWriterWriteValueWriter() throws Exception
    {
        final AtomicInteger count = new AtomicInteger();
        ObjectWriter w = _writerWith(count);
        w.writeValue(new StringWriter(), "test");
        assertEquals(1, count.get());
    }

    @Test
    public void testWriterWriteValueFile(@TempDir Path tempDir) throws Exception
    {
        final AtomicInteger count = new AtomicInteger();
        ObjectWriter w = _writerWith(count);
        w.writeValue(tempDir.resolve("test.json").toFile(), "test");
        assertEquals(1, count.get());
    }

    @Test
    public void testWriterWriteValuePath(@TempDir Path tempDir) throws Exception
    {
        final AtomicInteger count = new AtomicInteger();
        ObjectWriter w = _writerWith(count);
        w.writeValue(tempDir.resolve("test.json"), "test");
        assertEquals(1, count.get());
    }

    @Test
    public void testWriterWriteValueDataOutput() throws Exception
    {
        final AtomicInteger count = new AtomicInteger();
        ObjectWriter w = _writerWith(count);
        w.writeValue((DataOutput) new DataOutputStream(new ByteArrayOutputStream()), "test");
        assertEquals(1, count.get());
    }

    /*
    /**********************************************************************
    /* ObjectWriter: NOT called for user-provided generators
    /**********************************************************************
     */

    @Test
    public void testWriterWriteValueWithProvidedGenerator() throws Exception
    {
        final AtomicInteger count = new AtomicInteger();
        ObjectMapper mapper = _mapperWith(count);
        ObjectWriter w = mapper.writer();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (JsonGenerator g = mapper.createGenerator(out)) {
            assertEquals(1, count.get()); // once from createGenerator
            w.writeValue(g, 42);
            assertEquals(1, count.get()); // NOT again for writeValue
        }
    }

    /*
    /**********************************************************************
    /* ObjectWriter.writeValues(): exactly once per sequence writer
    /**********************************************************************
     */

    @Test
    public void testWriterWriteValuesOutputStream() throws Exception
    {
        final AtomicInteger count = new AtomicInteger();
        ObjectWriter w = _writerWith(count);
        try (SequenceWriter seq = w.writeValues(new ByteArrayOutputStream())) {
            assertEquals(1, count.get());
            seq.write(1);
            seq.write(2);
            assertEquals(1, count.get());
        }
    }

    @Test
    public void testWriterWriteValuesWriter() throws Exception
    {
        final AtomicInteger count = new AtomicInteger();
        ObjectWriter w = _writerWith(count);
        try (SequenceWriter seq = w.writeValues(new StringWriter())) {
            assertEquals(1, count.get());
            seq.write(1);
            assertEquals(1, count.get());
        }
    }

    @Test
    public void testWriterWriteValuesFile(@TempDir Path tempDir) throws Exception
    {
        final AtomicInteger count = new AtomicInteger();
        ObjectWriter w = _writerWith(count);
        try (SequenceWriter seq = w.writeValues(tempDir.resolve("test.json").toFile())) {
            assertEquals(1, count.get());
            seq.write(1);
            assertEquals(1, count.get());
        }
    }

    @Test
    public void testWriterWriteValuesPath(@TempDir Path tempDir) throws Exception
    {
        final AtomicInteger count = new AtomicInteger();
        ObjectWriter w = _writerWith(count);
        try (SequenceWriter seq = w.writeValues(tempDir.resolve("test.json"))) {
            assertEquals(1, count.get());
            seq.write(1);
            assertEquals(1, count.get());
        }
    }

    @Test
    public void testWriterWriteValuesDataOutput() throws Exception
    {
        final AtomicInteger count = new AtomicInteger();
        ObjectWriter w = _writerWith(count);
        try (SequenceWriter seq = w.writeValues((DataOutput) new DataOutputStream(new ByteArrayOutputStream()))) {
            assertEquals(1, count.get());
            seq.write(1);
            assertEquals(1, count.get());
        }
    }

    @Test
    public void testWriterWriteValuesWithProvidedGenerator() throws Exception
    {
        final AtomicInteger count = new AtomicInteger();
        ObjectMapper mapper = _mapperWith(count);
        ObjectWriter w = mapper.writer();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (JsonGenerator g = mapper.createGenerator(out)) {
            assertEquals(1, count.get());
            try (SequenceWriter seq = w.writeValues(g)) {
                seq.write(1);
            }
            assertEquals(1, count.get()); // NOT again
        }
    }

    /*
    /**********************************************************************
    /* ObjectWriter.writeValuesAsArray(): exactly once per sequence writer
    /**********************************************************************
     */

    @Test
    public void testWriterWriteValuesAsArrayOutputStream() throws Exception
    {
        final AtomicInteger count = new AtomicInteger();
        ObjectWriter w = _writerWith(count);
        try (SequenceWriter seq = w.writeValuesAsArray(new ByteArrayOutputStream())) {
            assertEquals(1, count.get());
            seq.write(1);
            seq.write(2);
            assertEquals(1, count.get());
        }
    }

    @Test
    public void testWriterWriteValuesAsArrayWriter() throws Exception
    {
        final AtomicInteger count = new AtomicInteger();
        ObjectWriter w = _writerWith(count);
        try (SequenceWriter seq = w.writeValuesAsArray(new StringWriter())) {
            assertEquals(1, count.get());
            seq.write(1);
            assertEquals(1, count.get());
        }
    }

    @Test
    public void testWriterWriteValuesAsArrayFile(@TempDir Path tempDir) throws Exception
    {
        final AtomicInteger count = new AtomicInteger();
        ObjectWriter w = _writerWith(count);
        try (SequenceWriter seq = w.writeValuesAsArray(tempDir.resolve("test.json").toFile())) {
            assertEquals(1, count.get());
            seq.write(1);
            assertEquals(1, count.get());
        }
    }

    @Test
    public void testWriterWriteValuesAsArrayPath(@TempDir Path tempDir) throws Exception
    {
        final AtomicInteger count = new AtomicInteger();
        ObjectWriter w = _writerWith(count);
        try (SequenceWriter seq = w.writeValuesAsArray(tempDir.resolve("test.json"))) {
            assertEquals(1, count.get());
            seq.write(1);
            assertEquals(1, count.get());
        }
    }

    @Test
    public void testWriterWriteValuesAsArrayDataOutput() throws Exception
    {
        final AtomicInteger count = new AtomicInteger();
        ObjectWriter w = _writerWith(count);
        try (SequenceWriter seq = w.writeValuesAsArray((DataOutput) new DataOutputStream(new ByteArrayOutputStream()))) {
            assertEquals(1, count.get());
            seq.write(1);
            assertEquals(1, count.get());
        }
    }

    @Test
    public void testWriterWriteValuesAsArrayWithProvidedGenerator() throws Exception
    {
        final AtomicInteger count = new AtomicInteger();
        ObjectMapper mapper = _mapperWith(count);
        ObjectWriter w = mapper.writer();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (JsonGenerator g = mapper.createGenerator(out)) {
            assertEquals(1, count.get());
            try (SequenceWriter seq = w.writeValuesAsArray(g)) {
                seq.write(1);
            }
            assertEquals(1, count.get()); // NOT again
        }
    }

    /*
    /**********************************************************************
    /* ObjectWriter.valueToTree(): exactly once (uses internal generator)
    /**********************************************************************
     */

    @Test
    public void testWriterValueToTree() throws Exception
    {
        final AtomicInteger count = new AtomicInteger();
        ObjectWriter w = _writerWith(count);
        JsonNode node = w.valueToTree(Map.of("a", 1));
        assertNotNull(node);
        assertTrue(node.isObject());
        assertEquals(1, count.get());
    }

    /*
    /**********************************************************************
    /* Multiple independent writes: count increments once per call
    /**********************************************************************
     */

    @Test
    public void testMultipleWritesIncrementCount() throws Exception
    {
        final AtomicInteger count = new AtomicInteger();
        ObjectMapper mapper = _mapperWith(count);

        mapper.writeValueAsString(1);
        assertEquals(1, count.get());
        mapper.writeValueAsString(2);
        assertEquals(2, count.get());
        mapper.writeValueAsString(3);
        assertEquals(3, count.get());
    }

    @Test
    public void testMultipleWriterWritesIncrementCount() throws Exception
    {
        final AtomicInteger count = new AtomicInteger();
        ObjectWriter w = _writerWith(count);

        w.writeValueAsString(1);
        assertEquals(1, count.get());
        w.writeValueAsString(2);
        assertEquals(2, count.get());
        w.writeValueAsString(3);
        assertEquals(3, count.get());
    }
}
