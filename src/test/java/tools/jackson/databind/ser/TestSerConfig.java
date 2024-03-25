package tools.jackson.databind.ser;

import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.*;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.*;

import tools.jackson.databind.*;
import tools.jackson.databind.annotation.JsonSerialize;
import tools.jackson.databind.cfg.SerializationContexts;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for checking handling of SerializationConfig.
 */
public class TestSerConfig
    extends DatabindTestUtil
{
    /*
    /**********************************************************
    /* Helper beans
    /**********************************************************
     */

    @JsonInclude(JsonInclude.Include.NON_DEFAULT)
    @JsonSerialize(typing=JsonSerialize.Typing.STATIC)
    final static class Config { }

    final static class ConfigNone { }

    static class AnnoBean {
        public int getX() { return 1; }
        @JsonProperty("y")
        private int getY() { return 2; }
    }

    static class Indentable {
        public int a = 3;
    }

    public static class SimpleBean {
        public int x = 1;
    }

    /*
    /**********************************************************
    /* Main tests
    /**********************************************************
     */

    final static ObjectMapper MAPPER = new ObjectMapper();

    /* Test to verify that we don't overflow number of features; if we
     * hit the limit, need to change implementation -- this test just
     * gives low-water mark
     */
    @Test
    public void testEnumIndexes()
    {
        int max = 0;

        for (SerializationFeature f : SerializationFeature.values()) {
            max = Math.max(max, f.ordinal());
        }
        if (max >= 31) { // 31 is actually ok; 32 not
            fail("Max number of SerializationFeature enums reached: "+max);
        }
    }

    @Test
    public void testDefaults()
    {
        SerializationConfig cfg = MAPPER.serializationConfig();

        // First, defaults:
        assertTrue(cfg.isEnabled(MapperFeature.USE_ANNOTATIONS));
        assertTrue(cfg.isEnabled(MapperFeature.CAN_OVERRIDE_ACCESS_MODIFIERS));

        assertTrue(cfg.isEnabled(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS));

        assertFalse(cfg.isEnabled(SerializationFeature.INDENT_OUTPUT));
        assertFalse(cfg.isEnabled(MapperFeature.USE_STATIC_TYPING));
        assertTrue(cfg.isEnabled(SerializationFeature.FAIL_ON_EMPTY_BEANS));
        assertTrue(cfg.isEnabled(MapperFeature.DEFAULT_VIEW_INCLUSION));
    }

    @Test
    public void testIndentation() throws Exception
    {
        Map<String,Integer> map = new HashMap<String,Integer>();
        map.put("a", Integer.valueOf(2));
        String result = MAPPER.writer().with(SerializationFeature.INDENT_OUTPUT)
                .writeValueAsString(map);
        // 02-Jun-2009, tatu: not really a clean way but...
        String lf = getLF();
        assertEquals("{"+lf+"  \"a\" : 2"+lf+"}", result);
    }

    @Test
    public void testAnnotationsDisabled() throws Exception
    {
        // first: verify that annotation introspection is enabled by default
        assertTrue(MAPPER.isEnabled(MapperFeature.USE_ANNOTATIONS));
        Map<String,Object> result = writeAndMap(MAPPER, new AnnoBean());
        assertEquals(2, result.size());

        ObjectMapper m2 = jsonMapperBuilder()
                .configure(MapperFeature.USE_ANNOTATIONS, false)
                .build();
        result = writeAndMap(m2, new AnnoBean());
        assertEquals(1, result.size());
    }

    @SuppressWarnings("serial")
    static class TestObjectMapper
        extends ObjectMapper
    {
        public SerializationContexts getSerializationContexts() { return _serializationContexts; }
    }

    /**
     * Test for verifying some aspects of serializer caching
     */
    @Test
    public void testProviderConfig() throws Exception
    {
        TestObjectMapper mapper = new TestObjectMapper();
        SerializationContexts prov = mapper.getSerializationContexts();
        assertEquals(0, prov.cachedSerializersCount());
        // and then should get one constructed for:
        Map<String,Object> result = writeAndMap(mapper, new AnnoBean());
        assertEquals(2, result.size());
        assertEquals(Integer.valueOf(1), result.get("x"));
        assertEquals(Integer.valueOf(2), result.get("y"));

        // Note: it is 2 because we'll also get serializer for basic 'int', not just AnnoBean
        // 12-Jan-2010, tatus: Actually, probably more, if and when we typing
        //   aspects are considered (depending on what is cached)
        int count = prov.cachedSerializersCount();
        if (count < 2 || count > 10) {
            fail("Should have at least 2 cached serializers, got "+count);
        }
        prov.flushCachedSerializers();
        assertEquals(0, prov.cachedSerializersCount());
    }

    @Test
    public void testIndentWithPassedGenerator() throws Exception
    {
        Indentable input = new Indentable();
        assertEquals("{\"a\":3}", MAPPER.writeValueAsString(input));
        String LF = getLF();
        String INDENTED = "{"+LF+"  \"a\" : 3"+LF+"}";
        final ObjectWriter indentWriter = MAPPER.writer().with(SerializationFeature.INDENT_OUTPUT);
        assertEquals(INDENTED, indentWriter.writeValueAsString(input));

        StringWriter sw = new StringWriter();
        indentWriter.writeValue(sw, input);
        assertEquals(INDENTED, sw.toString());

        // and also with ObjectMapper itself
        sw = new StringWriter();
        ObjectMapper m2 = jsonMapperBuilder()
                .enable(SerializationFeature.INDENT_OUTPUT)
                .build();
        m2.writeValue(sw, input);
        assertEquals(INDENTED, sw.toString());
    }

    @Test
    public void testNoAccessOverrides() throws Exception
    {
        ObjectMapper m = jsonMapperBuilder()
                .disable(MapperFeature.CAN_OVERRIDE_ACCESS_MODIFIERS)
                .build();
        assertEquals("{\"x\":1}", m.writeValueAsString(new SimpleBean()));
    }

    @Test
    public void testDateFormatConfig() throws Exception
    {
        TimeZone tz1 = TimeZone.getTimeZone("America/Los_Angeles");
        TimeZone tz2 = TimeZone.getTimeZone("US/Central");

        // sanity checks
        assertEquals(tz1, tz1);
        assertEquals(tz2, tz2);
        if (tz1.equals(tz2)) {
            fail("Should not be equal");
        }

        ObjectMapper mapper = jsonMapperBuilder()
            .defaultTimeZone(tz1)
            .build();

        assertEquals(tz1, mapper.serializationConfig().getTimeZone());
        assertEquals(tz1, mapper.deserializationConfig().getTimeZone());

        // also better stick via reader/writer as well
        assertEquals(tz1, mapper.writer().getConfig().getTimeZone());
        assertEquals(tz1, mapper.reader().getConfig().getTimeZone());

        SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        f.setTimeZone(tz2);

        mapper = jsonMapperBuilder()
                .defaultTimeZone(tz1)
                .defaultDateFormat(f)
                .build();

        // should not change the timezone tho
        assertEquals(tz1, mapper.serializationConfig().getTimeZone());
        assertEquals(tz1, mapper.deserializationConfig().getTimeZone());
        assertEquals(tz1, mapper.writer().getConfig().getTimeZone());
        assertEquals(tz1, mapper.reader().getConfig().getTimeZone());
    }

    private final static String getLF() {
        return System.getProperty("line.separator");
    }
}
