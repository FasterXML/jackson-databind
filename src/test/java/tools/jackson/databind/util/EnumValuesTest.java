package tools.jackson.databind.util;

import java.util.List;
import java.util.Locale;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.junit.jupiter.api.parallel.Resources;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.SerializationConfig;
import tools.jackson.databind.cfg.EnumFeature;
import tools.jackson.databind.introspect.AnnotatedClass;
import tools.jackson.databind.introspect.AnnotatedClassResolver;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

@Deprecated // @since 3.1 along with EnumValues type itself
public class EnumValuesTest extends DatabindTestUtil
{
    enum ABC {
        A("A"),
        B("b"),
        C("C");

        private final String desc;

        private ABC(String d) { desc = d; }

        @Override
        public String toString() { return desc; }
    }

    enum LocaleSensitiveABC {
        IS_ADMIN
    }

    private final ObjectMapper MAPPER = newJsonMapper();

    @Test
    public void testConstructFromName() {
        SerializationConfig cfg = MAPPER.serializationConfig()
                .without(EnumFeature.WRITE_ENUMS_USING_TO_STRING);
        AnnotatedClass enumClass = resolve(MAPPER, ABC.class);
        EnumValues values = EnumValues.constructFromName(cfg, enumClass);
        assertEquals("A", values.serializedValueFor(ABC.A).toString());
        assertEquals("B", values.serializedValueFor(ABC.B).toString());
        assertEquals("C", values.serializedValueFor(ABC.C).toString());
        assertEquals(3, values.values().size());
        assertEquals(3, values.internalMap().size());
    }

    @Test
    public void testConstructWithToString() {
        SerializationConfig cfg = MAPPER.serializationConfig()
                .with(EnumFeature.WRITE_ENUMS_USING_TO_STRING);
        AnnotatedClass enumClass = resolve(MAPPER, ABC.class);

        EnumValues values = EnumValues.constructFromToString(cfg, enumClass);
        assertEquals("A", values.serializedValueFor(ABC.A).toString());
        assertEquals("b", values.serializedValueFor(ABC.B).toString());
        assertEquals("C", values.serializedValueFor(ABC.C).toString());
        assertEquals(3, values.values().size());
        assertEquals(3, values.internalMap().size());
    }

    @Test
    public void testEnumResolverNew()
    {
        AnnotatedClass annotatedClass = resolve(MAPPER, ABC.class);
        EnumResolver enumRes = EnumResolver.constructUsingToString(MAPPER.deserializationConfig(),
               annotatedClass);
        assertEquals(ABC.B, enumRes.getEnum(1));
        assertNull(enumRes.getEnum(-1));
        assertNull(enumRes.getEnum(3));
        assertEquals(2, enumRes.lastValidIndex());
        List<Enum<?>> enums = enumRes.getEnums();
        assertEquals(3, enums.size());
        assertEquals(ABC.A, enums.get(0));
        assertEquals(ABC.B, enums.get(1));
        assertEquals(ABC.C, enums.get(2));
    }

    // [databind#3053]
    @Test
    public void testConstructFromNameLowerCased() {
        SerializationConfig cfg = MAPPER.serializationConfig()
            .with(EnumFeature.WRITE_ENUMS_TO_LOWERCASE);
        AnnotatedClass enumClass = resolve(MAPPER, ABC.class);

        EnumValues values = EnumValues.constructFromName(cfg, enumClass);
        assertEquals("a", values.serializedValueFor(ABC.A).toString());
        assertEquals("b", values.serializedValueFor(ABC.B).toString());
        assertEquals("c", values.serializedValueFor(ABC.C).toString());
        assertEquals(3, values.values().size());
        assertEquals(3, values.internalMap().size());
    }

    // [databind#5993]: Follow-up to #5994; deprecated EnumValues lower-case path
    // should not use the default Locale
    @ResourceLock(Resources.LOCALE)
    @Test
    public void testConstructFromNameLowerCasedWithRootLocale() {
        Locale old = Locale.getDefault();
        try {
            Locale.setDefault(Locale.forLanguageTag("tr-TR"));
            assertEquals("\u0131", "I".toLowerCase(),
                    "Test requires a default locale where \"I\".toLowerCase() yields U+0131");

            SerializationConfig cfg = MAPPER.serializationConfig()
                    .with(EnumFeature.WRITE_ENUMS_TO_LOWERCASE);
            AnnotatedClass enumClass = resolve(MAPPER, LocaleSensitiveABC.class);

            EnumValues values = EnumValues.constructFromName(cfg, enumClass);
            assertEquals("is_admin", values.serializedValueFor(LocaleSensitiveABC.IS_ADMIN).toString());
        } finally {
            Locale.setDefault(old);
        }
    }

    private AnnotatedClass resolve(ObjectMapper mapper, Class<?> enumClass) {
        return AnnotatedClassResolver.resolve(mapper.serializationConfig(),
                mapper.constructType(enumClass), null);
    }
}
