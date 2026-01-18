package tools.jackson.databind.introspect;

import java.lang.annotation.Annotation;
import java.util.*;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.*;

import tools.jackson.core.Version;
import tools.jackson.databind.annotation.JsonSerialize;
import tools.jackson.databind.*;
import tools.jackson.databind.cfg.MapperConfig;
import tools.jackson.databind.deser.jdk.NumberDeserializers;
import tools.jackson.databind.deser.jdk.StringDeserializer;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.jsontype.NamedType;
import tools.jackson.databind.ser.jdk.StringSerializer;
import tools.jackson.databind.ser.std.ToStringSerializer;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.*;

// started with [databind#1025] in mind
@SuppressWarnings("serial")
public class IntrospectorPairTest extends DatabindTestUtil
{
    static class Introspector1 extends AnnotationIntrospector {
        @Override
        public Version version() {
            return Version.unknownVersion();
        }

        @Override
        public JsonInclude.Value findPropertyInclusion(MapperConfig<?> config, Annotated a) {
            return JsonInclude.Value.empty()
                    .withContentInclusion(JsonInclude.Include.ALWAYS)
                    .withValueInclusion(JsonInclude.Include.NON_ABSENT);
        }
    }

    static class Introspector2 extends AnnotationIntrospector {
        @Override
        public Version version() {
            return Version.unknownVersion();
        }

        @Override
        public JsonInclude.Value findPropertyInclusion(MapperConfig<?> config, Annotated a) {
            return JsonInclude.Value.empty()
                    .withContentInclusion(JsonInclude.Include.NON_EMPTY)
                    .withValueInclusion(JsonInclude.Include.USE_DEFAULTS);
        }
    }

    static class IntrospectorWithHandlers extends AnnotationIntrospector {
        final Object _deserializer;
        final Object _serializer;

        public IntrospectorWithHandlers(Object deser, Object ser) {
            _deserializer = deser;
            _serializer = ser;
        }

        @Override
        public Version version() {
            return Version.unknownVersion();
        }

        @Override
        public Object findDeserializer(MapperConfig<?> config, Annotated am) {
            return _deserializer;
        }

        @Override
        public Object findSerializer(MapperConfig<?> config, Annotated am) {
            return _serializer;
        }
    }

    static class IntrospectorWithMap extends AnnotationIntrospector
    {
        private final Map<String, Object> values = new HashMap<>();

        private Version version = Version.unknownVersion();

        public IntrospectorWithMap add(String key, Object value) {
            values.put(key, value);
            return this;
        }

        public IntrospectorWithMap version(Version v) {
            version = v;
            return this;
        }

        @Override
        public Version version() {
            return version;
        }

        @Override
        public JsonInclude.Value findPropertyInclusion(MapperConfig<?> config, Annotated a) {
            return JsonInclude.Value.empty()
                    .withContentInclusion(JsonInclude.Include.NON_EMPTY)
                    .withValueInclusion(JsonInclude.Include.USE_DEFAULTS);
        }

        @Override
        public boolean isAnnotationBundle(Annotation ann) {
            return _boolean("isAnnotationBundle");
        }

        /*
        /******************************************************
        /* General class annotations
        /******************************************************
         */

        @Override
        public PropertyName findRootName(MapperConfig<?> config, AnnotatedClass ac) {
            return (PropertyName) values.get("findRootName");
        }

        @Override
        public JsonIgnoreProperties.Value findPropertyIgnoralByName(MapperConfig<?> config, Annotated a) {
            return (JsonIgnoreProperties.Value) values.get("findPropertyIgnoralByName");
        }

        @Override
        public Boolean isIgnorableType(MapperConfig<?> config, AnnotatedClass ac) {
            return (Boolean) values.get("isIgnorableType");
        }

        @Override
        public Object findFilterId(MapperConfig<?> config, Annotated ann) {
            return (Object) values.get("findFilterId");
        }

        @Override
        public Object findNamingStrategy(MapperConfig<?> config, AnnotatedClass ac) {
            return (Object) values.get("findNamingStrategy");
        }

        @Override
        public String findClassDescription(MapperConfig<?> config, AnnotatedClass ac) {
            return (String) values.get("findClassDescription");
        }

        /*
        /******************************************************
        /* Property auto-detection
        /******************************************************
        */

        @Override
        public VisibilityChecker findAutoDetectVisibility(MapperConfig<?> config,
                AnnotatedClass ac, VisibilityChecker checker)
        {
            VisibilityChecker vc = (VisibilityChecker) values.get("findAutoDetectVisibility");
            // not really good but:
            return (vc == null) ? checker : vc;
        }

        /*
        /******************************************************
        /* Type handling
        /******************************************************
         */

        @SuppressWarnings("unchecked")
        @Override
        public List<NamedType> findSubtypes(MapperConfig<?> config, Annotated a)
        {
            return (List<NamedType>) values.get("findSubtypes");
        }

        @Override
        public String findTypeName(MapperConfig<?> config, AnnotatedClass ac) {
            return (String) values.get("findTypeName");
        }

        @Override
        public Object findTypeResolverBuilder(MapperConfig<?> config, Annotated ann) {
            return values.get("findTypeResolverBuilder");
        }

        @Override
        public Boolean isTypeId(MapperConfig<?> config, AnnotatedMember member) {
            return (Boolean) values.get("isTypeId");
        }

        /*
        /******************************************************
        /* General member (field, method/constructor) annotations
        /******************************************************
         */

        @Override
        public PropertyName findWrapperName(MapperConfig<?> config, Annotated ann) {
            return (PropertyName) values.get("findWrapperName");
        }

        /*
        /******************************************************
        /* Serialization introspection
        /******************************************************
         */

        @Override
        public Boolean hasAsKey(MapperConfig<?> config, Annotated a) {
            return (Boolean) values.get("hasAsKey");
        }

        @Override
        public Boolean hasAsValue(MapperConfig<?> config, Annotated a) {
            return (Boolean) values.get("hasAsValue");
        }

        @Override
        public Boolean hasAnyGetter(MapperConfig<?> config, Annotated ann) {
            return (Boolean) values.get("hasAnyGetter");
        }

        @Override
        public JsonSerialize.Typing findSerializationTyping(MapperConfig<?> config, Annotated a) {
            return (JsonSerialize.Typing) values.get("findSerializationTyping");
        }

        /*
        /******************************************************
        /* Deserialization introspection
        /******************************************************
         */

        @Override
        public Boolean hasAnySetter(MapperConfig<?> config, Annotated a) {
            return (Boolean) values.get("hasAnySetter");
        }

        /*
        /******************************************************
        /* Serializer finding methods
        /******************************************************
         */

        @Override
        public Object findKeySerializer(MapperConfig<?> config, Annotated am) {
            return values.get("findKeySerializer");
        }

        @Override
        public Object findContentSerializer(MapperConfig<?> config, Annotated am) {
            return values.get("findContentSerializer");
        }

        @Override
        public Object findNullSerializer(MapperConfig<?> config, Annotated am) {
            return values.get("findNullSerializer");
        }

        /*
        /******************************************************
        /* Enum introspection
        /******************************************************
         */

        @Override
        public String[] findEnumValues(MapperConfig<?> config, AnnotatedClass annotatedClass,
                Enum<?>[] enumValues, String[] names) {
            String[] overrides = (String[]) values.get("findEnumValues");
            if (overrides != null) {
                for (int i = 0; i < overrides.length; i++) {
                    if (overrides[i] != null) {
                        names[i] = overrides[i];
                    }
                }
            }
            return names;
        }

        @Override
        public void findEnumAliases(MapperConfig<?> config, AnnotatedClass annotatedClass,
                Enum<?>[] enumValues, String[][] aliases) {
            String[][] overrides = (String[][]) values.get("findEnumAliases");
            if (overrides != null) {
                for (int i = 0; i < overrides.length && i < aliases.length; i++) {
                    if (overrides[i] != null) {
                        aliases[i] = overrides[i];
                    }
                }
            }
        }

        @Override
        public Enum<?> findDefaultEnumValue(MapperConfig<?> config,
                AnnotatedClass ac, Enum<?>[] enumValues) {
            return (Enum<?>) values.get("findDefaultEnumValue");
        }

        @Override
        public Object findEnumNamingStrategy(MapperConfig<?> config, AnnotatedClass ac) {
            return values.get("findEnumNamingStrategy");
        }

        /*
        /******************************************************
        /* Deserializer finding methods
        /******************************************************
         */

        @Override
        public Object findKeyDeserializer(MapperConfig<?> config, Annotated am) {
            return values.get("findKeyDeserializer");
        }

        @Override
        public Object findContentDeserializer(MapperConfig<?> config, Annotated am) {
            return values.get("findContentDeserializer");
        }

        /*
        /******************************************************
        /* Helper methods
        /******************************************************
         */

        private boolean _boolean(String key) {
            Object ob = values.get(key);
            return Boolean.TRUE.equals(ob);
        }
    }

    /*
    /**********************************************************
    /* Test methods, misc
    /**********************************************************
     */

    private final AnnotationIntrospector NO_ANNOTATIONS = AnnotationIntrospector.nopInstance();

    @Test
    public void testVersion() throws Exception
    {
        Version v = new Version(1, 2, 3, null,
                "tools.jackson", "IntrospectorPairTest");
        IntrospectorWithMap withVersion = new IntrospectorWithMap()
                .version(v);
        assertEquals(v,
                new AnnotationIntrospectorPair(withVersion, NO_ANNOTATIONS).version());
        IntrospectorWithMap noVersion = new IntrospectorWithMap();
        assertEquals(Version.unknownVersion(),
                new AnnotationIntrospectorPair(noVersion, withVersion).version());
    }

    @Test
    public void testCreate() throws Exception
    {
        assertNotNull(AnnotationIntrospectorPair.create(NO_ANNOTATIONS, NO_ANNOTATIONS));
        assertNotNull(AnnotationIntrospectorPair.create(NO_ANNOTATIONS, null));
        assertNotNull(AnnotationIntrospectorPair.create(null, NO_ANNOTATIONS));
    }
    
    @Test
    public void testAccess() throws Exception
    {
        IntrospectorWithMap intr1 = new IntrospectorWithMap();
        AnnotationIntrospectorPair pair = new AnnotationIntrospectorPair(intr1,
                NO_ANNOTATIONS);
        Collection<AnnotationIntrospector> intrs = pair.allIntrospectors();
        assertEquals(2, intrs.size());
        Iterator<AnnotationIntrospector> it = intrs.iterator();
        assertSame(intr1, it.next());
        assertSame(NO_ANNOTATIONS, it.next());
    }

    @Test
    public void testAnnotationBundle() throws Exception
    {
        IntrospectorWithMap isBundle = new IntrospectorWithMap()
                .add("isAnnotationBundle", true);
        assertTrue(new AnnotationIntrospectorPair(NO_ANNOTATIONS, isBundle)
                .isAnnotationBundle(null));
        assertTrue(new AnnotationIntrospectorPair(isBundle, NO_ANNOTATIONS)
                .isAnnotationBundle(null));
        assertFalse(new AnnotationIntrospectorPair(NO_ANNOTATIONS, NO_ANNOTATIONS)
                .isAnnotationBundle(null));
    }

    /*
    /**********************************************************
    /* Test methods, general class annotations
    /**********************************************************
     */

    @Test
    public void testFindRootName() throws Exception
    {
        PropertyName name = new PropertyName("test");
        IntrospectorWithMap intr = new IntrospectorWithMap()
                .add("findRootName", name);
        assertNull(new AnnotationIntrospectorPair(NO_ANNOTATIONS, NO_ANNOTATIONS).findRootName(null, null));
        assertEquals(name, new AnnotationIntrospectorPair(NO_ANNOTATIONS, intr).findRootName(null, null));
        assertEquals(name, new AnnotationIntrospectorPair(intr, NO_ANNOTATIONS).findRootName(null, null));
    }

    @Test
    public void testPropertyIgnorals() throws Exception
    {
        JsonIgnoreProperties.Value incl = JsonIgnoreProperties.Value.forIgnoredProperties("foo");
        IntrospectorWithMap intr = new IntrospectorWithMap()
                .add("findPropertyIgnoralByName", incl);
        IntrospectorWithMap intrEmpty = new IntrospectorWithMap()
                .add("findPropertyIgnoralByName", JsonIgnoreProperties.Value.empty());
        assertEquals(JsonIgnoreProperties.Value.empty(),
                new AnnotationIntrospectorPair(intrEmpty, intrEmpty).findPropertyIgnoralByName(null, null));
        // should actually verify inclusion combining, but there are separate tests for that
        assertEquals(incl, new AnnotationIntrospectorPair(intrEmpty, intr).findPropertyIgnoralByName(null, null));
        assertEquals(incl, new AnnotationIntrospectorPair(intr, intrEmpty).findPropertyIgnoralByName(null, null));
    }

    @Test
    public void testIsIgnorableType() throws Exception
    {
        IntrospectorWithMap intr1 = new IntrospectorWithMap()
                .add("isIgnorableType", Boolean.TRUE);
        IntrospectorWithMap intr2 = new IntrospectorWithMap()
                .add("isIgnorableType", Boolean.FALSE);
        assertNull(new AnnotationIntrospectorPair(NO_ANNOTATIONS, NO_ANNOTATIONS).isIgnorableType(null, null));
        assertEquals(Boolean.TRUE, new AnnotationIntrospectorPair(intr1, intr2).isIgnorableType(null, null));
        assertEquals(Boolean.FALSE, new AnnotationIntrospectorPair(intr2, intr1).isIgnorableType(null, null));
    }

    @Test
    public void testFindFilterId() throws Exception
    {
        IntrospectorWithMap intr1 = new IntrospectorWithMap()
                .add("findFilterId", "a");
        IntrospectorWithMap intr2 = new IntrospectorWithMap()
                .add("findFilterId", "b");
        assertNull(new AnnotationIntrospectorPair(NO_ANNOTATIONS, NO_ANNOTATIONS).findFilterId(null, null));
        assertEquals("a", new AnnotationIntrospectorPair(intr1, intr2).findFilterId(null, null));
        assertEquals("b", new AnnotationIntrospectorPair(intr2, intr1).findFilterId(null, null));
    }

    @Test
    public void testFindNamingStrategy() throws Exception
    {
        // shouldn't be bogus Classes for real use, but works here
        IntrospectorWithMap intr1 = new IntrospectorWithMap()
                .add("findNamingStrategy", Integer.class);
        IntrospectorWithMap intr2 = new IntrospectorWithMap()
                .add("findNamingStrategy", String.class);
        assertNull(new AnnotationIntrospectorPair(NO_ANNOTATIONS, NO_ANNOTATIONS).findNamingStrategy(null, null));
        assertEquals(Integer.class,
                new AnnotationIntrospectorPair(intr1, intr2).findNamingStrategy(null, null));
        assertEquals(String.class,
                new AnnotationIntrospectorPair(intr2, intr1).findNamingStrategy(null, null));
    }

    @Test
    public void testFindClassDescription() throws Exception
    {
        IntrospectorWithMap intr1 = new IntrospectorWithMap()
                .add("findClassDescription", "Desc1");
        IntrospectorWithMap intr2 = new IntrospectorWithMap()
                .add("findClassDescription", "Desc2");
        assertNull(new AnnotationIntrospectorPair(NO_ANNOTATIONS, NO_ANNOTATIONS).findClassDescription(null, null));
        assertEquals("Desc1",
                new AnnotationIntrospectorPair(intr1, intr2).findClassDescription(null, null));
        assertEquals("Desc2",
                new AnnotationIntrospectorPair(intr2, intr1).findClassDescription(null, null));
    }

    // // // 3 deprecated methods, skip

    /*
    /**********************************************************
    /* Test methods, general member annotations
    /**********************************************************
     */

    @Test
    public void testFindWrapperName() throws Exception
    {
        final PropertyName NAME_WITH_NS = PropertyName.construct("simple", "ns");
        final PropertyName NAME_NO_NS = PropertyName.construct("other", null);

        assertNull(new AnnotationIntrospectorPair(NO_ANNOTATIONS, NO_ANNOTATIONS)
                .findClassDescription(null, null));

        // First: basic merging of namespace/localname info
        IntrospectorWithMap intr1 = new IntrospectorWithMap()
                .add("findWrapperName", NAME_WITH_NS);
        IntrospectorWithMap intr2 = new IntrospectorWithMap()
                .add("findWrapperName", NAME_NO_NS);
        assertSame(NAME_WITH_NS,
                new AnnotationIntrospectorPair(intr1, intr2).findWrapperName(null, null));
        assertEquals(PropertyName.construct("other", "ns"),
                new AnnotationIntrospectorPair(intr2, intr1).findWrapperName(null, null));

        // [databind#4595]: NO_NAME should be retained, not merged
        intr1 = new IntrospectorWithMap()
                .add("findWrapperName", PropertyName.NO_NAME);
        intr2 = new IntrospectorWithMap()
                .add("findWrapperName", NAME_WITH_NS);
        assertSame(PropertyName.NO_NAME,
                new AnnotationIntrospectorPair(intr1, intr2).findWrapperName(null, null));
        assertSame(NAME_WITH_NS,
                new AnnotationIntrospectorPair(intr2, intr1).findWrapperName(null, null));
    }

    /*
    /**********************************************************
    /* Test methods, ser/deser
    /**********************************************************
     */

    @Test
    public void testFindSerializer() throws Exception
    {
        final ValueSerializer<?> serString = new StringSerializer();
        final ValueSerializer<?> serToString = ToStringSerializer.instance;

        AnnotationIntrospector intr1 = new IntrospectorWithHandlers(null, serString);
        AnnotationIntrospector intr2 = new IntrospectorWithHandlers(null, serToString);
        AnnotationIntrospector nop = AnnotationIntrospector.nopInstance();
        AnnotationIntrospector nop2 = new IntrospectorWithHandlers(null, ValueSerializer.None.class);

        assertSame(serString,
                new AnnotationIntrospectorPair(intr1, intr2).findSerializer(null, null));
        assertSame(serToString,
                new AnnotationIntrospectorPair(intr2, intr1).findSerializer(null, null));

        // also: no-op instance should not block real one, regardless
        assertSame(serString,
                new AnnotationIntrospectorPair(nop, intr1).findSerializer(null, null));
        assertSame(serString,
                new AnnotationIntrospectorPair(nop2, intr1).findSerializer(null, null));

        // nor should no-op result in non-null result
        assertNull(new AnnotationIntrospectorPair(nop, nop2).findSerializer(null, null));
        assertNull(new AnnotationIntrospectorPair(nop2, nop).findSerializer(null, null));
    }

    @Test
    public void testFindKeySerializer() throws Exception
    {
        final ValueSerializer<?> serString = new StringSerializer();
        final ValueSerializer<?> serToString = ToStringSerializer.instance;

        IntrospectorWithMap intr1 = new IntrospectorWithMap()
                .add("findKeySerializer", serString);
        IntrospectorWithMap intr2 = new IntrospectorWithMap()
                .add("findKeySerializer", serToString);
        AnnotationIntrospector nop = AnnotationIntrospector.nopInstance();
        IntrospectorWithMap nop2 = new IntrospectorWithMap()
                .add("findKeySerializer", ValueSerializer.None.class);

        // Primary takes precedence when both have values
        assertSame(serString,
                new AnnotationIntrospectorPair(intr1, intr2).findKeySerializer(null, null));
        assertSame(serToString,
                new AnnotationIntrospectorPair(intr2, intr1).findKeySerializer(null, null));

        // No-op instance should not block real one
        assertSame(serString,
                new AnnotationIntrospectorPair(nop, intr1).findKeySerializer(null, null));
        assertSame(serString,
                new AnnotationIntrospectorPair(nop2, intr1).findKeySerializer(null, null));

        // No-ops should result in null
        assertNull(new AnnotationIntrospectorPair(nop, nop2).findKeySerializer(null, null));
        assertNull(new AnnotationIntrospectorPair(nop2, nop).findKeySerializer(null, null));
    }

    @Test
    public void testFindContentSerializer() throws Exception
    {
        final ValueSerializer<?> serString = new StringSerializer();
        final ValueSerializer<?> serToString = ToStringSerializer.instance;

        IntrospectorWithMap intr1 = new IntrospectorWithMap()
                .add("findContentSerializer", serString);
        IntrospectorWithMap intr2 = new IntrospectorWithMap()
                .add("findContentSerializer", serToString);
        AnnotationIntrospector nop = AnnotationIntrospector.nopInstance();
        IntrospectorWithMap nop2 = new IntrospectorWithMap()
                .add("findContentSerializer", ValueSerializer.None.class);

        // Primary takes precedence when both have values
        assertSame(serString,
                new AnnotationIntrospectorPair(intr1, intr2).findContentSerializer(null, null));
        assertSame(serToString,
                new AnnotationIntrospectorPair(intr2, intr1).findContentSerializer(null, null));

        // No-op instance should not block real one
        assertSame(serString,
                new AnnotationIntrospectorPair(nop, intr1).findContentSerializer(null, null));
        assertSame(serString,
                new AnnotationIntrospectorPair(nop2, intr1).findContentSerializer(null, null));

        // No-ops should result in null
        assertNull(new AnnotationIntrospectorPair(nop, nop2).findContentSerializer(null, null));
        assertNull(new AnnotationIntrospectorPair(nop2, nop).findContentSerializer(null, null));
    }

    @Test
    public void testFindNullSerializer() throws Exception
    {
        final ValueSerializer<?> serString = new StringSerializer();
        final ValueSerializer<?> serToString = ToStringSerializer.instance;

        IntrospectorWithMap intr1 = new IntrospectorWithMap()
                .add("findNullSerializer", serString);
        IntrospectorWithMap intr2 = new IntrospectorWithMap()
                .add("findNullSerializer", serToString);
        AnnotationIntrospector nop = AnnotationIntrospector.nopInstance();
        IntrospectorWithMap nop2 = new IntrospectorWithMap()
                .add("findNullSerializer", ValueSerializer.None.class);

        // Primary takes precedence when both have values
        assertSame(serString,
                new AnnotationIntrospectorPair(intr1, intr2).findNullSerializer(null, null));
        assertSame(serToString,
                new AnnotationIntrospectorPair(intr2, intr1).findNullSerializer(null, null));

        // No-op instance should not block real one
        assertSame(serString,
                new AnnotationIntrospectorPair(nop, intr1).findNullSerializer(null, null));
        assertSame(serString,
                new AnnotationIntrospectorPair(nop2, intr1).findNullSerializer(null, null));

        // No-ops should result in null
        assertNull(new AnnotationIntrospectorPair(nop, nop2).findNullSerializer(null, null));
        assertNull(new AnnotationIntrospectorPair(nop2, nop).findNullSerializer(null, null));
    }

    @Test
    public void testFindSerializationTyping() throws Exception
    {
        IntrospectorWithMap intr1 = new IntrospectorWithMap()
                .add("findSerializationTyping", JsonSerialize.Typing.STATIC);
        IntrospectorWithMap intr2 = new IntrospectorWithMap()
                .add("findSerializationTyping", JsonSerialize.Typing.DYNAMIC);

        // Both null -> null
        assertNull(new AnnotationIntrospectorPair(NO_ANNOTATIONS, NO_ANNOTATIONS)
                .findSerializationTyping(null, null));

        // Primary takes precedence
        assertEquals(JsonSerialize.Typing.STATIC,
                new AnnotationIntrospectorPair(intr1, intr2).findSerializationTyping(null, null));
        assertEquals(JsonSerialize.Typing.DYNAMIC,
                new AnnotationIntrospectorPair(intr2, intr1).findSerializationTyping(null, null));

        // If primary returns null, secondary is used
        assertEquals(JsonSerialize.Typing.STATIC,
                new AnnotationIntrospectorPair(NO_ANNOTATIONS, intr1).findSerializationTyping(null, null));
        assertEquals(JsonSerialize.Typing.DYNAMIC,
                new AnnotationIntrospectorPair(NO_ANNOTATIONS, intr2).findSerializationTyping(null, null));
    }

    @Test
    public void testHasAsValue() throws Exception
    {
        IntrospectorWithMap intr1 = new IntrospectorWithMap()
                .add("hasAsValue", Boolean.TRUE);
        IntrospectorWithMap intr2 = new IntrospectorWithMap()
                .add("hasAsValue", Boolean.FALSE);
        assertNull(new AnnotationIntrospectorPair(NO_ANNOTATIONS, NO_ANNOTATIONS)
                .hasAsValue(null, null));
        assertEquals(Boolean.TRUE, new AnnotationIntrospectorPair(intr1, NO_ANNOTATIONS)
                .hasAsValue(null, null));
        assertEquals(Boolean.TRUE, new AnnotationIntrospectorPair(NO_ANNOTATIONS, intr1)
                .hasAsValue(null, null));
        assertEquals(Boolean.FALSE, new AnnotationIntrospectorPair(intr2, NO_ANNOTATIONS)
                .hasAsValue(null, null));
        assertEquals(Boolean.FALSE, new AnnotationIntrospectorPair(NO_ANNOTATIONS, intr2)
                .hasAsValue(null, null));

        assertEquals(Boolean.TRUE, new AnnotationIntrospectorPair(intr1, intr2)
                .hasAsValue(null, null));
        assertEquals(Boolean.FALSE, new AnnotationIntrospectorPair(intr2, intr1)
                .hasAsValue(null, null));
    }

    @Test
    public void testHasAsKey() throws Exception
    {
        IntrospectorWithMap intr1 = new IntrospectorWithMap()
                .add("hasAsKey", Boolean.TRUE);
        IntrospectorWithMap intr2 = new IntrospectorWithMap()
                .add("hasAsKey", Boolean.FALSE);
        assertNull(new AnnotationIntrospectorPair(NO_ANNOTATIONS, NO_ANNOTATIONS)
                .hasAsKey(null, null));
        assertEquals(Boolean.TRUE, new AnnotationIntrospectorPair(intr1, NO_ANNOTATIONS)
                .hasAsKey(null, null));
        assertEquals(Boolean.TRUE, new AnnotationIntrospectorPair(NO_ANNOTATIONS, intr1)
                .hasAsKey(null, null));
        assertEquals(Boolean.FALSE, new AnnotationIntrospectorPair(intr2, NO_ANNOTATIONS)
                .hasAsKey(null, null));
        assertEquals(Boolean.FALSE, new AnnotationIntrospectorPair(NO_ANNOTATIONS, intr2)
                .hasAsKey(null, null));

        assertEquals(Boolean.TRUE, new AnnotationIntrospectorPair(intr1, intr2)
                .hasAsKey(null, null));
        assertEquals(Boolean.FALSE, new AnnotationIntrospectorPair(intr2, intr1)
                .hasAsKey(null, null));
    }

    @Test
    public void testHasAnyGetter() throws Exception
    {
        IntrospectorWithMap intr1 = new IntrospectorWithMap()
                .add("hasAnyGetter", Boolean.TRUE);
        IntrospectorWithMap intr2 = new IntrospectorWithMap()
                .add("hasAnyGetter", Boolean.FALSE);
        assertNull(new AnnotationIntrospectorPair(NO_ANNOTATIONS, NO_ANNOTATIONS)
                .hasAnyGetter(null, null));
        assertEquals(Boolean.TRUE, new AnnotationIntrospectorPair(intr1, NO_ANNOTATIONS)
                .hasAnyGetter(null, null));
        assertEquals(Boolean.TRUE, new AnnotationIntrospectorPair(NO_ANNOTATIONS, intr1)
                .hasAnyGetter(null, null));
        assertEquals(Boolean.FALSE, new AnnotationIntrospectorPair(intr2, NO_ANNOTATIONS)
                .hasAnyGetter(null, null));
        assertEquals(Boolean.FALSE, new AnnotationIntrospectorPair(NO_ANNOTATIONS, intr2)
                .hasAnyGetter(null, null));

        assertEquals(Boolean.TRUE, new AnnotationIntrospectorPair(intr1, intr2)
                .hasAnyGetter(null, null));
        assertEquals(Boolean.FALSE, new AnnotationIntrospectorPair(intr2, intr1)
                .hasAnyGetter(null, null));
    }

    /*
    /**********************************************************
    /* Test methods, deser
    /**********************************************************
     */

    @Test
    public void testFindDeserializer() throws Exception
    {
        final ValueDeserializer<?> deserString = StringDeserializer.instance;
        final ValueDeserializer<?> deserBoolean = NumberDeserializers.find(Boolean.TYPE);

        AnnotationIntrospector intr1 = new IntrospectorWithHandlers(deserString, null);
        AnnotationIntrospector intr2 = new IntrospectorWithHandlers(deserBoolean, null);
        AnnotationIntrospector nop = AnnotationIntrospector.nopInstance();
        AnnotationIntrospector nop2 = new IntrospectorWithHandlers(ValueDeserializer.None.class, null);

        assertSame(deserString,
                new AnnotationIntrospectorPair(intr1, intr2).findDeserializer(null, null));
        assertSame(deserBoolean,
                new AnnotationIntrospectorPair(intr2, intr1).findDeserializer(null, null));
        // also: no-op instance should not block real one, regardless
        assertSame(deserString,
                new AnnotationIntrospectorPair(nop, intr1).findDeserializer(null, null));
        assertSame(deserString,
                new AnnotationIntrospectorPair(nop2, intr1).findDeserializer(null, null));

        // nor should no-op result in non-null result
        assertNull(new AnnotationIntrospectorPair(nop, nop2).findDeserializer(null, null));
        assertNull(new AnnotationIntrospectorPair(nop2, nop).findDeserializer(null, null));
    }

    @Test
    public void testFindKeyDeserializer() throws Exception
    {
        final ValueDeserializer<?> deserString = StringDeserializer.instance;
        final ValueDeserializer<?> deserBoolean = NumberDeserializers.find(Boolean.TYPE);

        IntrospectorWithMap intr1 = new IntrospectorWithMap()
                .add("findKeyDeserializer", deserString);
        IntrospectorWithMap intr2 = new IntrospectorWithMap()
                .add("findKeyDeserializer", deserBoolean);
        AnnotationIntrospector nop = AnnotationIntrospector.nopInstance();
        IntrospectorWithMap nop2 = new IntrospectorWithMap()
                .add("findKeyDeserializer", KeyDeserializer.None.class);

        // Primary takes precedence when both have values
        assertSame(deserString,
                new AnnotationIntrospectorPair(intr1, intr2).findKeyDeserializer(null, null));
        assertSame(deserBoolean,
                new AnnotationIntrospectorPair(intr2, intr1).findKeyDeserializer(null, null));

        // No-op instance should not block real one
        assertSame(deserString,
                new AnnotationIntrospectorPair(nop, intr1).findKeyDeserializer(null, null));
        assertSame(deserString,
                new AnnotationIntrospectorPair(nop2, intr1).findKeyDeserializer(null, null));

        // No-ops should result in null
        assertNull(new AnnotationIntrospectorPair(nop, nop2).findKeyDeserializer(null, null));
        assertNull(new AnnotationIntrospectorPair(nop2, nop).findKeyDeserializer(null, null));
    }

    @Test
    public void testFindContentDeserializer() throws Exception
    {
        final ValueDeserializer<?> deserString = StringDeserializer.instance;
        final ValueDeserializer<?> deserBoolean = NumberDeserializers.find(Boolean.TYPE);

        IntrospectorWithMap intr1 = new IntrospectorWithMap()
                .add("findContentDeserializer", deserString);
        IntrospectorWithMap intr2 = new IntrospectorWithMap()
                .add("findContentDeserializer", deserBoolean);
        AnnotationIntrospector nop = AnnotationIntrospector.nopInstance();
        IntrospectorWithMap nop2 = new IntrospectorWithMap()
                .add("findContentDeserializer", ValueDeserializer.None.class);

        // Primary takes precedence when both have values
        assertSame(deserString,
                new AnnotationIntrospectorPair(intr1, intr2).findContentDeserializer(null, null));
        assertSame(deserBoolean,
                new AnnotationIntrospectorPair(intr2, intr1).findContentDeserializer(null, null));

        // No-op instance should not block real one
        assertSame(deserString,
                new AnnotationIntrospectorPair(nop, intr1).findContentDeserializer(null, null));
        assertSame(deserString,
                new AnnotationIntrospectorPair(nop2, intr1).findContentDeserializer(null, null));

        // No-ops should result in null
        assertNull(new AnnotationIntrospectorPair(nop, nop2).findContentDeserializer(null, null));
        assertNull(new AnnotationIntrospectorPair(nop2, nop).findContentDeserializer(null, null));
    }

    /*
    /******************************************************
    /* Property auto-detection
    /******************************************************
     */

    @Test
    public void testFindAutoDetectVisibility() throws Exception
    {
        VisibilityChecker vc = VisibilityChecker.defaultInstance();
        IntrospectorWithMap intr1 = new IntrospectorWithMap()
                .add("findAutoDetectVisibility", vc);
        SerializationConfig config = null;
        assertNull(new AnnotationIntrospectorPair(NO_ANNOTATIONS, NO_ANNOTATIONS)
                .findAutoDetectVisibility(config, null, null));
        assertSame(vc, new AnnotationIntrospectorPair(intr1, NO_ANNOTATIONS)
                .findAutoDetectVisibility(config, null, null));
        assertSame(vc, new AnnotationIntrospectorPair(NO_ANNOTATIONS, intr1)
                .findAutoDetectVisibility(config, null, null));
    }

    /*
    /******************************************************
    /* Type handling
    /******************************************************
     */

    @Test
    public void testFindTypeResolver() throws Exception
    {
        // Test findTypeResolverBuilder (the actual method name in AnnotationIntrospectorPair)
        IntrospectorWithMap intr1 = new IntrospectorWithMap()
                .add("findTypeResolverBuilder", "resolver1");
        IntrospectorWithMap intr2 = new IntrospectorWithMap()
                .add("findTypeResolverBuilder", "resolver2");
        assertNull(new AnnotationIntrospectorPair(NO_ANNOTATIONS, NO_ANNOTATIONS)
                .findTypeResolverBuilder(null, null));
        assertEquals("resolver1",
                new AnnotationIntrospectorPair(intr1, intr2).findTypeResolverBuilder(null, null));
        assertEquals("resolver2",
                new AnnotationIntrospectorPair(intr2, intr1).findTypeResolverBuilder(null, null));
        // When primary returns null, secondary should be used
        assertEquals("resolver2",
                new AnnotationIntrospectorPair(NO_ANNOTATIONS, intr2).findTypeResolverBuilder(null, null));
    }

    @Test
    public void testFindSubtypes() {
        NamedType type1 = new NamedType(String.class, "string");
        NamedType type2 = new NamedType(Integer.class, "integer");
        List<NamedType> list1 = Arrays.asList(type1);
        List<NamedType> list2 = Arrays.asList(type2);
        IntrospectorWithMap intr1 = new IntrospectorWithMap()
                .add("findSubtypes", list1);
        IntrospectorWithMap intr2 = new IntrospectorWithMap()
                .add("findSubtypes", list2);

        // Both null -> null
        assertNull(new AnnotationIntrospectorPair(NO_ANNOTATIONS, NO_ANNOTATIONS)
                .findSubtypes(null, null));

        // If only one returns non-null, that one is returned
        assertEquals(list1, new AnnotationIntrospectorPair(intr1, NO_ANNOTATIONS)
                .findSubtypes(null, null));
        assertEquals(list2, new AnnotationIntrospectorPair(NO_ANNOTATIONS, intr2)
                .findSubtypes(null, null));

        // If both return non-null, results are merged (primary first, then secondary)
        List<NamedType> merged = new AnnotationIntrospectorPair(intr1, intr2)
                .findSubtypes(null, null);
        assertEquals(2, merged.size());
        assertEquals(type1, merged.get(0));
        assertEquals(type2, merged.get(1));

        // Order should matter (primary first, secondary second)
        List<NamedType> mergedReverse = new AnnotationIntrospectorPair(intr2, intr1)
                .findSubtypes(null, null);
        assertEquals(2, mergedReverse.size());
        assertEquals(type2, mergedReverse.get(0));
        assertEquals(type1, mergedReverse.get(1));
    }

    @Test
    public void testFindTypeName() {
        IntrospectorWithMap intr1 = new IntrospectorWithMap()
                .add("findTypeName", "type1");
        IntrospectorWithMap intr2 = new IntrospectorWithMap()
                .add("findTypeName", "type2");
        assertNull(new AnnotationIntrospectorPair(NO_ANNOTATIONS, NO_ANNOTATIONS).findTypeName(null, null));
        assertEquals("type1",
                new AnnotationIntrospectorPair(intr1, intr2).findTypeName(null, null));
        assertEquals("type2",
                new AnnotationIntrospectorPair(intr2, intr1).findTypeName(null, null));
    }

    @Test
    public void testIsTypeId() {
        IntrospectorWithMap intr1 = new IntrospectorWithMap()
                .add("isTypeId", Boolean.TRUE);
        IntrospectorWithMap intr2 = new IntrospectorWithMap()
                .add("isTypeId", Boolean.FALSE);

        // Both null -> null
        assertNull(new AnnotationIntrospectorPair(NO_ANNOTATIONS, NO_ANNOTATIONS)
                .isTypeId(null, null));

        // Primary takes precedence
        assertEquals(Boolean.TRUE,
                new AnnotationIntrospectorPair(intr1, intr2).isTypeId(null, null));
        assertEquals(Boolean.FALSE,
                new AnnotationIntrospectorPair(intr2, intr1).isTypeId(null, null));

        // If primary returns null, secondary is used
        assertEquals(Boolean.TRUE,
                new AnnotationIntrospectorPair(NO_ANNOTATIONS, intr1).isTypeId(null, null));
        assertEquals(Boolean.FALSE,
                new AnnotationIntrospectorPair(NO_ANNOTATIONS, intr2).isTypeId(null, null));
    }

    /*
    /******************************************************
    /* Enum introspection
    /******************************************************
     */

    @Test
    public void testFindEnumValues() {
        // Secondary sets names for indices 0 and 1
        IntrospectorWithMap intr1 = new IntrospectorWithMap()
                .add("findEnumValues", new String[] { "PRIMARY_A", null, "PRIMARY_C" });
        // Primary sets names for indices 0 and 2
        IntrospectorWithMap intr2 = new IntrospectorWithMap()
                .add("findEnumValues", new String[] { "SECONDARY_A", "SECONDARY_B", null });

        String[] defaultNames = new String[] { "A", "B", "C" };

        // With no introspectors returning values, names should be unchanged
        String[] result = new AnnotationIntrospectorPair(NO_ANNOTATIONS, NO_ANNOTATIONS)
                .findEnumValues(null, null, null, defaultNames.clone());
        assertArrayEquals(new String[] { "A", "B", "C" }, result);

        // Primary takes precedence: secondary runs first, then primary overwrites
        // So index 0 -> PRIMARY_A (primary wins), index 1 -> SECONDARY_B, index 2 -> PRIMARY_C
        result = new AnnotationIntrospectorPair(intr1, intr2)
                .findEnumValues(null, null, null, defaultNames.clone());
        assertArrayEquals(new String[] { "PRIMARY_A", "SECONDARY_B", "PRIMARY_C" }, result);

        // Reversed order: intr2 is primary now
        // So index 0 -> SECONDARY_A (primary wins), index 1 -> SECONDARY_B, index 2 -> PRIMARY_C
        result = new AnnotationIntrospectorPair(intr2, intr1)
                .findEnumValues(null, null, null, defaultNames.clone());
        assertArrayEquals(new String[] { "SECONDARY_A", "SECONDARY_B", "PRIMARY_C" }, result);
    }

    @Test
    public void testFindEnumAliases() {
        // Primary sets aliases for index 0
        IntrospectorWithMap intr1 = new IntrospectorWithMap()
                .add("findEnumAliases", new String[][] { new String[] { "p_alias1" }, null, null });
        // Secondary sets aliases for indices 0 and 1
        IntrospectorWithMap intr2 = new IntrospectorWithMap()
                .add("findEnumAliases", new String[][] { new String[] { "s_alias1" }, new String[] { "s_alias2" }, null });

        String[][] aliases = new String[3][];

        // With no introspectors, aliases should remain null
        new AnnotationIntrospectorPair(NO_ANNOTATIONS, NO_ANNOTATIONS)
                .findEnumAliases(null, null, null, aliases);
        assertNull(aliases[0]);
        assertNull(aliases[1]);
        assertNull(aliases[2]);

        // Primary takes precedence: secondary runs first, then primary overwrites
        aliases = new String[3][];
        new AnnotationIntrospectorPair(intr1, intr2)
                .findEnumAliases(null, null, null, aliases);
        // Index 0: primary overwrites secondary
        assertArrayEquals(new String[] { "p_alias1" }, aliases[0]);
        // Index 1: only secondary sets it
        assertArrayEquals(new String[] { "s_alias2" }, aliases[1]);
        // Index 2: neither sets it
        assertNull(aliases[2]);

        // Reversed order
        aliases = new String[3][];
        new AnnotationIntrospectorPair(intr2, intr1)
                .findEnumAliases(null, null, null, aliases);
        // Index 0: intr2 (now primary) overwrites intr1 (now secondary)
        assertArrayEquals(new String[] { "s_alias1" }, aliases[0]);
        // Index 1: only intr2 sets it
        assertArrayEquals(new String[] { "s_alias2" }, aliases[1]);
        assertNull(aliases[2]);
    }

    @Test
    public void testFindDefaultEnumValue() {
        IntrospectorWithMap intr1 = new IntrospectorWithMap()
                .add("findDefaultEnumValue", SimpleEnum.ONE);
        IntrospectorWithMap intr2 = new IntrospectorWithMap()
                .add("findDefaultEnumValue", SimpleEnum.TWO);

        // Both null -> null
        assertNull(new AnnotationIntrospectorPair(NO_ANNOTATIONS, NO_ANNOTATIONS)
                .findDefaultEnumValue(null, null, null));

        // Primary takes precedence
        assertEquals(SimpleEnum.ONE,
                new AnnotationIntrospectorPair(intr1, intr2).findDefaultEnumValue(null, null, null));
        assertEquals(SimpleEnum.TWO,
                new AnnotationIntrospectorPair(intr2, intr1).findDefaultEnumValue(null, null, null));

        // If primary returns null, secondary is used
        assertEquals(SimpleEnum.ONE,
                new AnnotationIntrospectorPair(NO_ANNOTATIONS, intr1).findDefaultEnumValue(null, null, null));
        assertEquals(SimpleEnum.TWO,
                new AnnotationIntrospectorPair(NO_ANNOTATIONS, intr2).findDefaultEnumValue(null, null, null));
    }

    @Test
    public void testFindEnumNamingStrategy() {
        // Using Class objects as stand-ins for enum naming strategies
        IntrospectorWithMap intr1 = new IntrospectorWithMap()
                .add("findEnumNamingStrategy", Integer.class);
        IntrospectorWithMap intr2 = new IntrospectorWithMap()
                .add("findEnumNamingStrategy", String.class);

        // Both null -> null
        assertNull(new AnnotationIntrospectorPair(NO_ANNOTATIONS, NO_ANNOTATIONS)
                .findEnumNamingStrategy(null, null));

        // Primary takes precedence
        assertEquals(Integer.class,
                new AnnotationIntrospectorPair(intr1, intr2).findEnumNamingStrategy(null, null));
        assertEquals(String.class,
                new AnnotationIntrospectorPair(intr2, intr1).findEnumNamingStrategy(null, null));

        // If primary returns null, secondary is used
        assertEquals(Integer.class,
                new AnnotationIntrospectorPair(NO_ANNOTATIONS, intr1).findEnumNamingStrategy(null, null));
        assertEquals(String.class,
                new AnnotationIntrospectorPair(NO_ANNOTATIONS, intr2).findEnumNamingStrategy(null, null));
    }

    /*
    /******************************************************
    /* Deserialization introspection
    /******************************************************
     */

    // for [databind#1672]
    @Test
    public void testHasAnySetter() {
        IntrospectorWithMap intr1 = new IntrospectorWithMap()
                .add("hasAnySetter", Boolean.TRUE);
        IntrospectorWithMap intr2 = new IntrospectorWithMap()
                .add("hasAnySetter", Boolean.FALSE);
        assertNull(new AnnotationIntrospectorPair(NO_ANNOTATIONS, NO_ANNOTATIONS).hasAnySetter(null, null));
        assertEquals(Boolean.TRUE,
                new AnnotationIntrospectorPair(intr1, intr2).hasAnySetter(null, null));
        assertEquals(Boolean.TRUE,
                new AnnotationIntrospectorPair(NO_ANNOTATIONS, intr1).hasAnySetter(null, null));
        assertEquals(Boolean.FALSE,
                new AnnotationIntrospectorPair(intr2, intr1).hasAnySetter(null, null));
        assertEquals(Boolean.FALSE,
                new AnnotationIntrospectorPair(NO_ANNOTATIONS, intr2).hasAnySetter(null, null));
    }

    /*
    /**********************************************************
    /* Test methods, others
    /**********************************************************
     */

    private final AnnotationIntrospectorPair introPair12
        = new AnnotationIntrospectorPair(new Introspector1(), new Introspector2());

    private final AnnotationIntrospectorPair introPair21
        = new AnnotationIntrospectorPair(new Introspector2(), new Introspector1());

    // for [databind#1025]
    @Test
    public void testInclusionMerging() throws Exception
    {
        // argument is ignored by test introspectors, may be null
        JsonInclude.Value v12 = introPair12.findPropertyInclusion(null, null);
        JsonInclude.Value v21 = introPair21.findPropertyInclusion(null, null);

        assertEquals(JsonInclude.Include.ALWAYS, v12.getContentInclusion());
        assertEquals(JsonInclude.Include.NON_ABSENT, v12.getValueInclusion());

        assertEquals(JsonInclude.Include.NON_EMPTY, v21.getContentInclusion());
        assertEquals(JsonInclude.Include.NON_ABSENT, v21.getValueInclusion());
    }

    /*
    /**********************************************************
    /* Introspectors and test for [jackson-modules-base#134]/[databind#962]
    /**********************************************************
     */
    static class TestIntrospector extends NopAnnotationIntrospector {
        @Override
        public JacksonInject.Value findInjectableValue(MapperConfig<?> config,
                AnnotatedMember m) {
            if (m.getRawType() == UnreadableBean.class) {
                return JacksonInject.Value.forId("jjj");
            }
            return null;
        }
    }

    static class TestInjector extends InjectableValues {
        @Override
        public Object findInjectableValue(DeserializationContext ctxt,
                Object valueId, 
                BeanProperty forProperty, Object beanInstance,
                Boolean optional, Boolean useInput) {
            if (valueId == "jjj") {
                UnreadableBean bean = new UnreadableBean();
                bean.setValue(1);
                return bean;
            }
            return null;
        }

        @Deprecated // since 2.20
        @Override
        public InjectableValues snapshot() {
            return this;
        }
    }

    enum SimpleEnum { ONE, TWO }

    static class UnreadableBean {
        public SimpleEnum value;

        public void setValue(SimpleEnum value) {
            this.value = value;
        }

        public void setValue(Integer intValue) {
            this.value = SimpleEnum.values()[intValue];
        }

        public SimpleEnum getValue() {
            return value;
        }
    }

    static class ReadableInjectedBean {
        public ReadableInjectedBean(@JacksonInject(useInput = OptBoolean.FALSE) UnreadableBean injectBean) {
            this.injectBean = injectBean;
        }
        @JsonProperty
        String foo;
        @JsonIgnore
        UnreadableBean injectBean;
    }

    static class UnreadableInjectedBean {
        public UnreadableInjectedBean(@JacksonInject UnreadableBean injectBean) {
            this.injectBean = injectBean;
        }
        @JsonProperty
        private String foo;
        @JsonIgnore
        private UnreadableBean injectBean;
    }

    @Test
    public void testMergingIntrospectorsForInjection() throws Exception {
        AnnotationIntrospector testIntrospector = new TestIntrospector();
        ObjectMapper mapper = JsonMapper.builder()
                .injectableValues(new TestInjector())
                .annotationIntrospector(new AnnotationIntrospectorPair(testIntrospector,
                        new JacksonAnnotationIntrospector()))
                .build();
        ReadableInjectedBean bean = mapper.readValue("{\"foo\": \"bob\"}", ReadableInjectedBean.class);
        assertEquals("bob", bean.foo);
        assertEquals(SimpleEnum.TWO, bean.injectBean.value);

        boolean successReadingUnreadableInjectedBean;
        try {
            /*UnreadableInjectedBean noBean =*/ mapper.readValue("{\"foo\": \"bob\"}", UnreadableInjectedBean.class);
            successReadingUnreadableInjectedBean = true;
        } catch (DatabindException e) {
            successReadingUnreadableInjectedBean = false;
            assertTrue(e.getMessage().contains("Conflicting setter definitions"));
        }
        assertFalse(successReadingUnreadableInjectedBean);
    }
}
