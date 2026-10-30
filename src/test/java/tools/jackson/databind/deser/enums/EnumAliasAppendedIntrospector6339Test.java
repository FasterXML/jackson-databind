package tools.jackson.databind.deser.enums;

import java.util.Collections;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonAlias;
import tools.jackson.core.Version;
import tools.jackson.core.type.TypeReference;

import tools.jackson.databind.*;
import tools.jackson.databind.annotation.EnumNaming;
import tools.jackson.databind.cfg.EnumFeature;
import tools.jackson.databind.cfg.MapperConfig;
import tools.jackson.databind.exc.InvalidFormatException;
import tools.jackson.databind.introspect.AnnotatedClass;
import tools.jackson.databind.introspect.NopAnnotationIntrospector;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.module.SimpleModule;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import static tools.jackson.databind.testutil.DatabindTestUtil.a2q;
import static tools.jackson.databind.testutil.DatabindTestUtil.q;

// [databind#6339]: Enum aliases from an appended AnnotationIntrospector
// must not be discarded by JacksonAnnotationIntrospector
public class EnumAliasAppendedIntrospector6339Test
{
    enum Color6339 {
        RED,
        @JsonAlias("vert")
        GREEN
    }

    enum EmptyAlias6339 {
        @JsonAlias({})
        RED,
        GREEN
    }

    @EnumNaming(EnumNamingStrategies.LowerCamelCaseStrategy.class)
    enum Named6339 {
        RED,
        GREEN
    }

    // Adds "rouge" for RED and "verde" for GREEN
    static class AliasIntrospector extends NopAnnotationIntrospector {
        private static final long serialVersionUID = 1L;

        @Override
        public Version version() {
            return Version.unknownVersion();
        }

        @Override
        public void findEnumAliases(MapperConfig<?> config, AnnotatedClass ac,
                Enum<?>[] enumValues, String[][] aliases) {
            aliases[0] = new String[] { "rouge" };
            aliases[1] = new String[] { "verde" };
        }
    }

    static class AliasModule extends SimpleModule {
        private static final long serialVersionUID = 1L;

        private final boolean _append;

        AliasModule(boolean append) {
            _append = append;
        }

        @Override
        public void setupModule(SetupContext context) {
            super.setupModule(context);
            if (_append) {
                context.appendAnnotationIntrospector(new AliasIntrospector());
            } else {
                context.insertAnnotationIntrospector(new AliasIntrospector());
            }
        }
    }

    @Test
    public void testAliasFromAppendedIntrospector() throws Exception
    {
        ObjectMapper mapper = JsonMapper.builder()
                .addModule(new AliasModule(true))
                .build();
        assertEquals(Color6339.RED, mapper.readValue(q("rouge"), Color6339.class));
        assertEquals(Color6339.RED, mapper.readValue(q("RED"), Color6339.class));
        // `@JsonAlias` of the primary introspector still has precedence
        assertEquals(Color6339.GREEN, mapper.readValue(q("vert"), Color6339.class));
        assertThrows(InvalidFormatException.class,
                () -> mapper.readValue(q("verde"), Color6339.class));
    }

    @Test
    public void testAliasFromInsertedIntrospector() throws Exception
    {
        ObjectMapper mapper = JsonMapper.builder()
                .addModule(new AliasModule(false))
                .build();
        assertEquals(Color6339.RED, mapper.readValue(q("rouge"), Color6339.class));
        // the inserted introspector is the primary, so its alias wins
        assertEquals(Color6339.GREEN, mapper.readValue(q("verde"), Color6339.class));
    }

    // Empty `@JsonAlias` must not discard aliases of the secondary introspector either
    @Test
    public void testEmptyAliasKeepsAppendedAlias() throws Exception
    {
        ObjectMapper mapper = JsonMapper.builder()
                .addModule(new AliasModule(true))
                .build();
        assertEquals(EmptyAlias6339.RED, mapper.readValue(q("rouge"), EmptyAlias6339.class));
    }

    @Test
    public void testAliasFromAppendedIntrospectorUsingToString() throws Exception
    {
        ObjectMapper mapper = JsonMapper.builder()
                .addModule(new AliasModule(true))
                .enable(EnumFeature.READ_ENUMS_USING_TO_STRING)
                .build();
        assertEquals(Color6339.RED, mapper.readValue(q("rouge"), Color6339.class));
    }

    @Test
    public void testAliasFromAppendedIntrospectorWithNamingStrategy() throws Exception
    {
        ObjectMapper mapper = JsonMapper.builder()
                .addModule(new AliasModule(true))
                .build();
        assertEquals(Named6339.RED, mapper.readValue(q("rouge"), Named6339.class));
        assertEquals(Named6339.RED, mapper.readValue(q("red"), Named6339.class));
    }

    @Test
    public void testAliasFromAppendedIntrospectorAsMapKey() throws Exception
    {
        ObjectMapper mapper = JsonMapper.builder()
                .addModule(new AliasModule(true))
                .build();
        Map<Color6339, Integer> map = mapper.readValue(a2q("{'rouge':1}"),
                new TypeReference<Map<Color6339, Integer>>() { });
        assertEquals(Collections.singletonMap(Color6339.RED, 1), map);

        Map<Color6339, Integer> map2 = mapper.readerFor(new TypeReference<Map<Color6339, Integer>>() { })
                .with(EnumFeature.READ_ENUMS_USING_TO_STRING)
                .readValue(a2q("{'rouge':1}"));
        assertEquals(Collections.singletonMap(Color6339.RED, 1), map2);
    }
}
