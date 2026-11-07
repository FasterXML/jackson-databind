package tools.jackson.databind.ser.filter;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.MapperFeature;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueSerializer;
import tools.jackson.databind.annotation.JsonSerialize;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

// [databind#1757]: Global NON_DEFAULT should use real bean defaults
//  for round-trip consistency when USE_REAL_INCLUDE_NON_DEFAULT is enabled
public class JsonInclude1757Test extends DatabindTestUtil
{
    static class Entity {
        private String someFieldWithDefault = "a default";

        public void setSomeFieldWithDefault(String someFieldWithDefault) {
            this.someFieldWithDefault = someFieldWithDefault;
        }

        public String getSomeFieldWithDefault() {
            return someFieldWithDefault;
        }
    }

    static class IntDefaultEntity {
        public int value = 42;
    }

    private ObjectMapper globalRealNonDefaultMapper() {
        return jsonMapperBuilder()
                .enable(MapperFeature.USE_REAL_INCLUDE_NON_DEFAULT)
                .changeDefaultPropertyInclusion(incl ->
                        incl.withValueInclusion(JsonInclude.Include.NON_DEFAULT))
                .build();
    }

    // [databind#1757]: default values should round-trip with global NON_DEFAULT
    @Test
    public void testGlobalNonDefaultRoundTrip() throws Exception {
        ObjectMapper mapper = globalRealNonDefaultMapper();

        // Serializing a default instance should produce {}
        Entity entity = new Entity();
        String json = mapper.writeValueAsString(entity);
        assertEquals("{}", json);

        // Deserializing {} should restore the default value
        Entity deserialized = mapper.readValue(json, Entity.class);
        assertEquals("a default", deserialized.getSomeFieldWithDefault());
    }

    // [databind#1757]: non-default values should be included
    @Test
    public void testGlobalNonDefaultIncludesNonDefault() throws Exception {
        ObjectMapper mapper = globalRealNonDefaultMapper();

        Entity entity = new Entity();
        entity.setSomeFieldWithDefault("custom value");
        String json = mapper.writeValueAsString(entity);
        assertEquals(a2q("{'someFieldWithDefault':'custom value'}"), json);
    }

    // [databind#1757]: empty string should not be suppressed if it's not the default
    @Test
    public void testGlobalNonDefaultEmptyStringNonDefault() throws Exception {
        ObjectMapper mapper = globalRealNonDefaultMapper();

        Entity entity = new Entity();
        entity.setSomeFieldWithDefault("");
        String json = mapper.writeValueAsString(entity);
        // Empty string is NOT the default ("a default"), so it should be included
        assertEquals(a2q("{'someFieldWithDefault':''}"), json);
    }

    // [databind#6317]: null over non-null default written as explicit null (like per-type),
    //  so that it round-trips
    @Test
    public void globalNonDefaultWritesExplicitNull() throws Exception {
        ObjectMapper mapper = globalRealNonDefaultMapper();

        Entity entity = new Entity();
        entity.setSomeFieldWithDefault(null);
        String json = mapper.writeValueAsString(entity);
        assertEquals("""
                {"someFieldWithDefault":null}""", json);
        assertNull(mapper.readValue(json, Entity.class).getSomeFieldWithDefault());
    }

    // [databind#1757]: global NON_DEFAULT with primitive field having non-zero default
    @Test
    public void testGlobalNonDefaultPrimitiveField() throws Exception {
        ObjectMapper mapper = globalRealNonDefaultMapper();

        // Default value (42) should be suppressed
        IntDefaultEntity entity = new IntDefaultEntity();
        assertEquals("{}", mapper.writeValueAsString(entity));

        // Non-default value should be included
        entity.value = 0;
        assertEquals(a2q("{'value':0}"), mapper.writeValueAsString(entity));

        // Another non-default value
        entity.value = 99;
        assertEquals(a2q("{'value':99}"), mapper.writeValueAsString(entity));
    }

    // [databind#1757]: global NON_DEFAULT should match per-class NON_DEFAULT behavior
    @Test
    public void testGlobalMatchesPerClassBehavior() throws Exception {
        ObjectMapper globalMapper = globalRealNonDefaultMapper();

        ObjectMapper defaultMapper = newJsonMapper();

        // Per-class annotated bean
        AnnotatedEntity annotated = new AnnotatedEntity();
        String perClassJson = defaultMapper.writeValueAsString(annotated);

        // Global NON_DEFAULT bean (same structure, no annotation)
        Entity global = new Entity();
        String globalJson = globalMapper.writeValueAsString(global);

        // Both should produce {}
        assertEquals("{}", perClassJson);
        assertEquals("{}", globalJson);
    }

    // [databind#1757]: Without USE_REAL_INCLUDE_NON_DEFAULT, old behavior is preserved
    @Test
    public void testOldBehaviorWithoutFeature() throws Exception {
        ObjectMapper mapper = jsonMapperBuilder()
                // USE_REAL_INCLUDE_NON_DEFAULT is disabled by default
                .changeDefaultPropertyInclusion(incl ->
                        incl.withValueInclusion(JsonInclude.Include.NON_DEFAULT))
                .build();

        // With old behavior (Mode 2), String type default is "".
        // "a default" != "" so the field IS included.
        Entity entity = new Entity();
        String json = mapper.writeValueAsString(entity);
        assertEquals(a2q("{'someFieldWithDefault':'a default'}"), json);
    }

    @JsonInclude(JsonInclude.Include.NON_DEFAULT)
    static class AnnotatedEntity {
        private String someFieldWithDefault = "a default";

        public void setSomeFieldWithDefault(String v) {
            this.someFieldWithDefault = v;
        }

        public String getSomeFieldWithDefault() {
            return someFieldWithDefault;
        }
    }

    // [databind#6275]: per-type NON_DEFAULT, non-null constructor default
    @JsonInclude(JsonInclude.Include.NON_DEFAULT)
    static class PerTypeMode {
        public String mode = "default";
    }

    // [databind#6275]: per-type NON_DEFAULT whose real default is null
    @JsonInclude(JsonInclude.Include.NON_DEFAULT)
    static class PerTypeNullDefault {
        public String mode;
    }

    // [databind#6275]: explicit null over a non-null per-type default must round-trip
    @Test
    public void testPerTypeNonDefaultExplicitNullRoundTrip() throws Exception {
        ObjectMapper mapper = newJsonMapper();

        PerTypeMode bean = new PerTypeMode();
        bean.mode = null;
        String json = mapper.writeValueAsString(bean);
        assertEquals(a2q("{'mode':null}"), json);

        PerTypeMode deserialized = mapper.readValue(json, PerTypeMode.class);
        assertNull(deserialized.mode);

        // Unchanged constructor default is omitted
        assertEquals("{}", mapper.writeValueAsString(new PerTypeMode()));

        // Non-default non-null value is included
        bean.mode = "custom";
        assertEquals(a2q("{'mode':'custom'}"), mapper.writeValueAsString(bean));
    }

    // [databind#6275]: a null real default still suppresses nulls
    @Test
    public void testPerTypeNonDefaultNullDefaultSuppressesNull() throws Exception {
        assertEquals("{}", newJsonMapper().writeValueAsString(new PerTypeNullDefault()));
    }

    static class NullDefaultEntity {
        public String mode;
    }

    static class PropertyOverrideEntity {
        @JsonInclude(JsonInclude.Include.NON_NULL)
        public String mode = "default";
    }

    // [databind#6317]: a null real default still suppresses nulls in global mode
    @Test
    public void globalNonDefaultNullDefaultSuppressesNull() throws Exception {
        ObjectMapper mapper = globalRealNonDefaultMapper();
        assertEquals("{}", mapper.writeValueAsString(new NullDefaultEntity()));
        NullDefaultEntity bean = new NullDefaultEntity();
        bean.mode = "x";
        assertEquals("""
                {"mode":"x"}""", mapper.writeValueAsString(bean));
    }

    // [databind#6317]: per-property NON_NULL overrides global NON_DEFAULT
    @Test
    public void globalNonDefaultPerPropertyOverride() throws Exception {
        ObjectMapper mapper = globalRealNonDefaultMapper();
        PropertyOverrideEntity bean = new PropertyOverrideEntity();
        assertEquals("""
                {"mode":"default"}""", mapper.writeValueAsString(bean));
        bean.mode = null;
        assertEquals("{}", mapper.writeValueAsString(bean));
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    static class ClassOverridePropertyNonDefaultEntity {
        @JsonInclude(JsonInclude.Include.NON_DEFAULT)
        public String mode = "default";
    }

    // [databind#6317]: if class-level inclusion overrides global NON_DEFAULT,
    //  per-property NON_DEFAULT uses static defaults, and nulls are suppressed
    @Test
    public void classOverridePerPropertyNonDefaultSuppressesNull() throws Exception {
        ObjectMapper mapper = globalRealNonDefaultMapper();
        ClassOverridePropertyNonDefaultEntity bean = new ClassOverridePropertyNonDefaultEntity();
        // static default for String is "", so real default is not suppressed
        assertEquals("""
                {"mode":"default"}""", mapper.writeValueAsString(bean));
        bean.mode = null;
        assertEquals("{}", mapper.writeValueAsString(bean));
    }

    // [databind#6319]: custom "empty" check must apply when default bean cannot be created
    static class Money {
        final long cents;
        Money(long c) { cents = c; }
    }

    static class MoneySer extends ValueSerializer<Money> {
        @Override
        public void serialize(Money v, JsonGenerator g, SerializationContext ctxt) {
            g.writeNumber(v.cents);
        }

        @Override
        public boolean isEmpty(SerializationContext ctxt, Money v) {
            return v.cents == 0;
        }
    }

    // No default constructor: default bean cannot be created
    static class CreatorOnly {
        @JsonSerialize(using = MoneySer.class)
        public Money amount;

        @JsonCreator
        CreatorOnly(@JsonProperty("amount") Money amount) { this.amount = amount; }
    }

    @JsonInclude(JsonInclude.Include.NON_DEFAULT)
    static class PerTypeCreatorOnly {
        @JsonSerialize(using = MoneySer.class)
        public Money amount;

        @JsonCreator
        PerTypeCreatorOnly(@JsonProperty("amount") Money amount) { this.amount = amount; }
    }

    record MoneyRecord(@JsonSerialize(using = MoneySer.class) Money amount) { }

    private ObjectMapper globalNonDefaultMapper(boolean real) {
        return jsonMapperBuilder()
                .configure(MapperFeature.USE_REAL_INCLUDE_NON_DEFAULT, real)
                .changeDefaultPropertyInclusion(incl ->
                        incl.withValueInclusion(JsonInclude.Include.NON_DEFAULT))
                .build();
    }

    @Test
    public void testNoDefaultBeanGlobalNonDefault() throws Exception {
        for (boolean real : new boolean[] { false, true }) {
            ObjectMapper mapper = globalNonDefaultMapper(real);
            assertEquals("{}", mapper.writeValueAsString(new CreatorOnly(new Money(0))),
                    "real=" + real);
            assertEquals("{\"amount\":5}", mapper.writeValueAsString(new CreatorOnly(new Money(5))),
                    "real=" + real);
        }
    }

    @JsonInclude(JsonInclude.Include.NON_DEFAULT)
    static class PerTypeBuiltInsCreatorOnly {
        public String text;
        public List<String> list;

        @JsonCreator
        PerTypeBuiltInsCreatorOnly(@JsonProperty("text") String text,
                @JsonProperty("list") List<String> list) {
            this.text = text;
            this.list = list;
        }
    }

    @Test
    public void testNoDefaultBeanPerTypeNonDefault() throws Exception {
        for (boolean real : new boolean[] { false, true }) {
            ObjectMapper mapper = jsonMapperBuilder()
                    .configure(MapperFeature.USE_REAL_INCLUDE_NON_DEFAULT, real)
                    .build();
            assertEquals("{}", mapper.writeValueAsString(new PerTypeCreatorOnly(new Money(0))),
                    "real=" + real);
            assertEquals("{\"amount\":5}",
                    mapper.writeValueAsString(new PerTypeCreatorOnly(new Money(5))),
                    "real=" + real);
        }
    }

    // Built-in types: static defaults ("", empty List) apply, as no real default available
    @Test
    public void testNoDefaultBeanPerTypeBuiltIns() throws Exception {
        for (boolean real : new boolean[] { false, true }) {
            ObjectMapper mapper = jsonMapperBuilder()
                    .configure(MapperFeature.USE_REAL_INCLUDE_NON_DEFAULT, real)
                    .build();
            assertEquals("{}",
                    mapper.writeValueAsString(new PerTypeBuiltInsCreatorOnly("", List.of())),
                    "real=" + real);
            assertEquals("{\"text\":\"a\",\"list\":[\"b\"]}",
                    mapper.writeValueAsString(new PerTypeBuiltInsCreatorOnly("a", List.of("b"))),
                    "real=" + real);
        }
    }

    // Records are excluded from "empty" fallback ([databind#5312]), regardless of feature
    @Test
    public void testRecordGlobalNonDefault() throws Exception {
        for (boolean real : new boolean[] { false, true }) {
            ObjectMapper mapper = globalNonDefaultMapper(real);
            assertEquals("{\"amount\":0}", mapper.writeValueAsString(new MoneyRecord(new Money(0))),
                    "real=" + real);
            assertEquals("{\"amount\":5}", mapper.writeValueAsString(new MoneyRecord(new Money(5))),
                    "real=" + real);
        }
    }
}
