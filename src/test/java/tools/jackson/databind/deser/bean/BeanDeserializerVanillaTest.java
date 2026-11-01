package tools.jackson.databind.deser.bean;

import java.util.*;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonView;

import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.core.ObjectReadContext;
import tools.jackson.core.TokenStreamLocation;
import tools.jackson.core.sym.PropertyNameMatcher;
import tools.jackson.core.util.JsonParserDelegate;
import tools.jackson.databind.*;
import tools.jackson.databind.deser.DeserializationContextExt;
import tools.jackson.databind.exc.InvalidFormatException;
import tools.jackson.databind.exc.MismatchedInputException;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.testutil.DatabindTestUtil;
import tools.jackson.databind.testutil.MockDataInput;
import tools.jackson.databind.util.TokenBuffer;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the "vanilla" fast path of {@link BeanDeserializer} (which uses
 * {@code JsonParser.nextNameMatchAndToken()}).
 * <p>
 * The 3.x default disables {@link MapperFeature#DEFAULT_VIEW_INCLUSION}, which
 * still requires view processing when a view is active. The fast path is used
 * when no view is active (see [databind#6219]).
 */
public class BeanDeserializerVanillaTest extends DatabindTestUtil
{
    @SuppressWarnings("serial")
    static class AccessibleMapper extends JsonMapper {
        AccessibleMapper(JsonMapper.Builder b) {
            super(b);
        }

        DeserializationContextExt deserializationContext() {
            return _deserializationContext();
        }

        DeserializationContextExt deserializationContext(Class<?> activeView) {
            return _deserializationContexts.createContext(
                    deserializationConfig().withView(activeView),
                    null, _injectableValues);
        }
    }

    static class Point {
        public int x, y;
    }

    // More than 4 properties, to also cover wrap-around of unrolled loop
    static class Wide {
        public String a;
        public int b;
        public boolean c;
        public Double d = -1.0;
        public Point e;
        public List<Point> f;
        public int[] g;
        public Map<String, Object> h;
        public String i;
    }

    static class RefView { }

    // Mix-in that puts all properties in `RefView`: reading with that view
    // active forces non-vanilla (but otherwise standard) processing
    @JsonView(RefView.class)
    static abstract class RefViewMixIn {
    }

    // Parser delegate that overrides `nextNameMatch()` (and `currentNameMatch()`)
    // but not `nextNameMatchAndToken()`: overrides must still be honored
    static class RenamingParser extends JsonParserDelegate {
        RenamingParser(JsonParser p) {
            super(p);
        }

        @Override
        public String currentName() {
            String name = delegate.currentName();
            return "legacyA".equals(name) ? "a" : name;
        }

        @Override
        public int currentNameMatch(PropertyNameMatcher matcher) {
            return matcher.matchName(currentName());
        }

        @Override
        public int nextNameMatch(PropertyNameMatcher matcher) {
            JsonToken t = nextToken();
            if (t == JsonToken.PROPERTY_NAME) {
                return matcher.matchName(currentName());
            }
            return (t == JsonToken.END_OBJECT) ? PropertyNameMatcher.MATCH_END_OBJECT
                    : PropertyNameMatcher.MATCH_ODD_TOKEN;
        }
    }

    // All properties of `Wide`, one per line
    private static final String KNOWN_PROPS = """
            "a": "text"
            "b": 42
            "c": true
            "d": null
            "e": { "x": 1, "y": 2 }
            "f": [ { "x": 3, "y": 4 }, { "y": 6, "x": 5 } ]
            "g": [ 7, 8 ]
            "h": { "k": [ 1, { "n": null } ] }
            "i": "last"
            """;

    // Unknown property with structured value, containing names of known properties
    private static final String UNKNOWN_PROP = """
            "unknown": { "a": [ 1, { "e": [ ] } ], "b": "not for Wide.b" }
            """.strip();

    private final AccessibleMapper MAPPER = new AccessibleMapper(jsonMapperBuilder()
            .enable(MapperFeature.DEFAULT_VIEW_INCLUSION));

    // 3.x default: DEFAULT_VIEW_INCLUSION disabled. Vanilla still applies when
    // no view is active ([databind#6219]).
    private final AccessibleMapper DEFAULT_MAPPER = new AccessibleMapper(jsonMapperBuilder());

    // Sanity check: verify tests actually exercise vanilla processing
    @Test
    void vanillaProcessingUsed() throws Exception
    {
        for (Class<?> type : List.of(Wide.class, Point.class)) {
            ValueDeserializer<Object> deser = MAPPER.deserializationContext()
                    .findRootValueDeserializer(MAPPER.constructType(type));
            assertInstanceOf(BeanDeserializer.class, deser);
            assertTrue(((BeanDeserializer) deser)._vanillaProcessing,
                    "Should use vanilla processing for "+type.getSimpleName());
        }
    }

    @Test
    void vanillaProcessingUsedWithDefaultSettings() throws Exception
    {
        for (Class<?> type : List.of(Wide.class, Point.class)) {
            ValueDeserializer<Object> deser = DEFAULT_MAPPER.deserializationContext()
                    .findRootValueDeserializer(DEFAULT_MAPPER.constructType(type));
            assertInstanceOf(BeanDeserializer.class, deser);
            BeanDeserializer beanDeser = (BeanDeserializer) deser;
            assertTrue(beanDeser.hasViews(),
                    "View processing must remain available when DEFAULT_VIEW_INCLUSION is off");
            // Not "fully vanilla" (external subclasses rely on this), but
            // vanilla when no view is active
            assertFalse(beanDeser._vanillaProcessing);
            assertTrue(beanDeser._useVanillaProcessing(DEFAULT_MAPPER.deserializationContext()),
                    "Should use vanilla processing with 3.x defaults for "+type.getSimpleName());
            assertFalse(beanDeser._useVanillaProcessing(DEFAULT_MAPPER.deserializationContext(Object.class)),
                    "Should NOT use vanilla processing with active view for "+type.getSimpleName());
        }
        Point p = DEFAULT_MAPPER.readValue("""
                {"x":1,"y":2}
                """, Point.class);
        assertEquals(1, p.x);
        assertEquals(2, p.y);
    }

    @Test
    void unannotatedPropertiesSkippedWhenViewActive() throws Exception
    {
        Point p = DEFAULT_MAPPER.readerWithView(Object.class)
                .forType(Point.class)
                .readValue("""
                {"x":1,"y":2}
                """);
        assertEquals(0, p.x);
        assertEquals(0, p.y);
    }

    @Test
    void allKnownProperties() throws Exception
    {
        for (Wide w : _readAllWays(_object(_knownProps()))) {
            _verifyWide(w);
        }
    }

    @Test
    void emptyObject() throws Exception
    {
        for (Wide w : _readAllWays("{ }")) {
            assertNull(w.a);
            assertEquals(Double.valueOf(-1.0), w.d);
            assertNull(w.i);
        }
    }

    // Unknown property at every position, to hit unknown-name handling from
    // all points of unrolled loop (and its continuation)
    @Test
    void unknownPropertyAtEveryPosition() throws Exception
    {
        final int count = _knownProps().size();
        for (int pos = 0; pos <= count; ++pos) {
            List<String> props = _knownProps();
            props.add(pos, UNKNOWN_PROP);
            final String doc = _object(props);
            for (Wide w : _readAllWays(doc)) {
                try {
                    _verifyWide(w);
                } catch (AssertionError e) {
                    fail("Failed for unknown property at #"+pos+": "+e.getMessage()+"; doc: "+doc);
                }
            }
        }
    }

    // Unknown property before every known one
    @Test
    void unknownPropertiesInterleaved() throws Exception
    {
        List<String> props = new ArrayList<>();
        for (String prop : _knownProps()) {
            props.add(UNKNOWN_PROP);
            props.add(prop);
        }
        props.add(UNKNOWN_PROP);
        for (Wide w : _readAllWays(_object(props))) {
            _verifyWide(w);
        }
    }

    // Overrides of `nextNameMatch()` in a `JsonParserDelegate` sub-class must be
    // honored (was not the case before [core#1710])
    @Test
    void parserDelegateOverridesHonored() throws Exception
    {
        final String doc = """
                {
                  "b": 42,
                  "legacyA": "text",
                  "e": { "x": 1, "y": 2 },
                  "unknown": [ 1 ],
                  "legacyA": "again"
                }
                """;
        try (JsonParser p = new RenamingParser(MAPPER.createParser(doc))) {
            Wide w = MAPPER.readValue(p, Wide.class);
            assertEquals("again", w.a);
            assertEquals(42, w.b);
            assertNotNull(w.e);
            assertEquals(1, w.e.x);
            assertEquals(2, w.e.y);
        }
    }

    /*
    /**********************************************************************
    /* Error reporting: must be same as with non-vanilla processing
    /**********************************************************************
     */

    // Replaces "b": type mismatch for a top-level property
    private static final String BAD_SCALAR_PROP = """
            "b": "notAnInt"
            """.strip();

    // Replaces "e": type mismatch for a property of nested POJO
    private static final String BAD_NESTED_PROP = """
            "e": { "x": 1, "y": "notAnInt" }
            """.strip();

    // Default settings use vanilla processing when no view is active (see
    // [databind#6219]); force standard non-vanilla processing by reading with
    // an active view that includes all properties.
    private final AccessibleMapper REFERENCE_MAPPER = new AccessibleMapper(jsonMapperBuilder()
            .addMixIn(Wide.class, RefViewMixIn.class)
            .addMixIn(Point.class, RefViewMixIn.class));

    // Errors within vanilla processing (at every position of unrolled loop, and
    // within continuation after unknown property) must be reported same as with
    // non-vanilla processing: same exception type, message, path and location
    @Test
    void errorsReportedSameAsNonVanilla() throws Exception
    {
        for (Class<?> type : List.of(Wide.class, Point.class)) {
            DeserializationContextExt ctxt = REFERENCE_MAPPER.deserializationContext(RefView.class);
            ValueDeserializer<Object> deser = ctxt
                    .findRootValueDeserializer(REFERENCE_MAPPER.constructType(type));
            assertFalse(((BeanDeserializer) deser)._useVanillaProcessing(ctxt),
                    "Reference mapper should NOT use vanilla processing for "+type.getSimpleName());
        }

        final int count = _knownProps().size();
        for (String badProp : List.of(BAD_SCALAR_PROP, BAD_NESTED_PROP)) {
            final String badName = badProp.substring(0, badProp.indexOf(':'));
            final List<String> expPath = (badProp == BAD_SCALAR_PROP)
                    ? List.of("b") : List.of("e", "y");
            for (boolean unknownFirst : new boolean[] { false, true }) {
                for (int pos = 0; pos < count; ++pos) {
                    List<String> props = _knownProps();
                    props.removeIf(prop -> prop.startsWith(badName));
                    props.add(pos, badProp);
                    if (unknownFirst) {
                        props.add(0, UNKNOWN_PROP);
                    }
                    // One property per line: bad one is on line (index + 2)
                    final String doc = "{\n" + String.join(",\n", props) + "\n}";
                    final int expLine = props.indexOf(badProp) + 2;

                    for (int mode = 0; mode < 3; ++mode) {
                        final String desc = "input mode #"+mode+", doc:\n"+doc;
                        InvalidFormatException vanilla = _readFail(MAPPER, doc, mode);
                        InvalidFormatException reference = _readFail(
                                REFERENCE_MAPPER.readerWithView(RefView.class), doc, mode);

                        assertEquals("notAnInt", vanilla.getValue(), "value: "+desc);
                        assertEquals(int.class, vanilla.getTargetType(), "target type: "+desc);

                        assertEquals(expPath, vanilla.getPath().stream()
                                .map(JacksonException.Reference::getPropertyName).toList(),
                                "path: "+desc);
                        // DataInput-backed parser does not track token location accurately,
                        // so only compare it with reference (below)
                        if (mode != 2) {
                            assertEquals(expLine, vanilla.getLocation().getLineNr(), "line: "+desc);
                        }

                        assertEquals(reference.getClass(), vanilla.getClass(), desc);
                        assertEquals(reference.getOriginalMessage(), vanilla.getOriginalMessage(), desc);
                        assertEquals(reference.getPathReference(), vanilla.getPathReference(), desc);
                        _assertSameLocation(reference.getLocation(), vanilla.getLocation(), desc);
                    }
                }
            }
        }
    }

    /*
    /**********************************************************************
    /* Helper methods
    /**********************************************************************
     */

    // Read using one of JSON parser implementations, expecting failure:
    // 0 = byte-backed, 1 = char-backed, 2 = DataInput-backed
    private InvalidFormatException _readFail(ObjectMapper mapper, String doc, int mode) {
        return _readFail(mapper.reader(), doc, mode);
    }

    private InvalidFormatException _readFail(ObjectReader reader, String doc, int mode) {
        final ObjectReader r = reader.forType(Wide.class);
        return assertThrows(InvalidFormatException.class, () -> {
            switch (mode) {
            case 0 -> r.readValue(utf8Bytes(doc));
            case 1 -> r.readValue(doc);
            default -> r.readValue(new MockDataInput(doc));
            }
        });
    }

    private void _assertSameLocation(TokenStreamLocation exp, TokenStreamLocation act, String desc) {
        assertEquals(exp.getLineNr(), act.getLineNr(), "line: "+desc);
        assertEquals(exp.getColumnNr(), act.getColumnNr(), "column: "+desc);
        assertEquals(exp.getByteOffset(), act.getByteOffset(), "byte offset: "+desc);
        assertEquals(exp.getCharOffset(), act.getCharOffset(), "char offset: "+desc);
    }

    private List<String> _knownProps() {
        return new ArrayList<>(KNOWN_PROPS.lines().toList());
    }

    private String _object(List<String> props) {
        return "{" + String.join(",", props) + "}";
    }

    // [databind#6357]: token that is neither name nor END_OBJECT where a name is
    // expected (not possible with JSON, but with other sources) must be reported
    // same as with non-vanilla processing, also after an unknown property
    @Test
    void unexpectedTokenWithinObjectReported() throws Exception
    {
        for (boolean withUnknown : new boolean[] { false, true }) {
            MismatchedInputException vanilla = assertThrows(MismatchedInputException.class,
                    () -> _readWithUnexpectedToken(MAPPER, withUnknown));
            MismatchedInputException reference = assertThrows(MismatchedInputException.class,
                    () -> _readWithUnexpectedToken(REFERENCE_MAPPER, withUnknown));
            assertEquals(reference.getOriginalMessage(), vanilla.getOriginalMessage(),
                    "withUnknown: "+withUnknown);
        }
    }

    // {"x":1, ["z":2,] 3, "y":5}: a bare number where a name belongs
    private Point _readWithUnexpectedToken(ObjectMapper mapper, boolean withUnknown)
    {
        TokenBuffer tb = TokenBuffer.forGeneration();
        tb.writeStartObject();
        tb.writeName("x");
        tb.writeNumber(1);
        if (withUnknown) {
            tb.writeName("z");
            tb.writeNumber(2);
        }
        tb.writeNumber(3);
        tb.writeName("y");
        tb.writeNumber(5);
        tb.writeEndObject();
        try (JsonParser p = tb.asParser(ObjectReadContext.empty())) {
            return mapper.readerFor(Point.class)
                    .without(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                    .readValue(p);
        }
    }

    // Read using all 3 JSON parser implementations: byte-, char- and DataInput-backed
    private List<Wide> _readAllWays(String doc) throws Exception {
        return List.of(MAPPER.readValue(utf8Bytes(doc), Wide.class),
                MAPPER.readValue(doc, Wide.class),
                MAPPER.readValue(new MockDataInput(doc), Wide.class));
    }

    private void _verifyWide(Wide w) {
        assertEquals("text", w.a);
        assertEquals(42, w.b);
        assertTrue(w.c);
        assertNull(w.d);
        assertNotNull(w.e);
        assertEquals(1, w.e.x);
        assertEquals(2, w.e.y);
        assertNotNull(w.f);
        assertEquals(2, w.f.size());
        assertEquals(3, w.f.get(0).x);
        assertEquals(4, w.f.get(0).y);
        assertEquals(5, w.f.get(1).x);
        assertEquals(6, w.f.get(1).y);
        assertArrayEquals(new int[] { 7, 8 }, w.g);
        assertEquals(Map.of("k", List.of(1, Collections.singletonMap("n", null))), w.h);
        assertEquals("last", w.i);
    }
}
