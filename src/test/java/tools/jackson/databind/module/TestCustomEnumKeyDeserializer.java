package tools.jackson.databind.module;

import java.io.File;
import java.io.IOException;
import java.util.*;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

import tools.jackson.core.*;
import tools.jackson.core.type.TypeReference;

import tools.jackson.databind.*;
import tools.jackson.databind.annotation.JsonDeserialize;
import tools.jackson.databind.annotation.JsonSerialize;
import tools.jackson.databind.deser.ValueDeserializerModifier;
import tools.jackson.databind.deser.std.StdDeserializer;
import tools.jackson.databind.exc.InvalidFormatException;
import tools.jackson.databind.node.ObjectNode;

@SuppressWarnings("serial")
public class TestCustomEnumKeyDeserializer extends BaseMapTest
{
    @JsonSerialize(using = TestEnumSerializer.class, keyUsing = TestEnumKeySerializer.class)
    @JsonDeserialize(using = TestEnumDeserializer.class, keyUsing = TestEnumKeyDeserializer.class)
    public enum TestEnumMixin { }

    enum KeyEnum {
        replacements,
        rootDirectory,
        licenseString
    }

    enum TestEnum {
        RED("red"),
        GREEN("green");

        private final String code;

        TestEnum(String code) {
            this.code = code;
        }

        public static TestEnum lookup(String lower) {
            for (TestEnum item : values()) {
                if (item.code().equals(lower)) {
                    return item;
                }
            }
            throw new IllegalArgumentException("Invalid code " + lower);
        }

        public String code() {
            return code;
        }
    }

    static class TestEnumSerializer extends ValueSerializer<TestEnum> {
        @Override
        public void serialize(TestEnum languageCode, JsonGenerator g, SerializerProvider serializerProvider) {
            g.writeString(languageCode.code());
        }

        @Override
        public Class<TestEnum> handledType() {
            return TestEnum.class;
        }
    }

    static class TestEnumKeyDeserializer extends KeyDeserializer {
        @Override
        public Object deserializeKey(String key, DeserializationContext ctxt) {
            try {
                return TestEnum.lookup(key);
            } catch (IllegalArgumentException e) {
                return ctxt.handleWeirdKey(TestEnum.class, key, "Unknown code");
            }
        }
    }

    static class TestEnumDeserializer extends StdDeserializer<TestEnum> {
        public TestEnumDeserializer() {
            super(TestEnum.class);
        }

        @Override
        public TestEnum deserialize(JsonParser p, DeserializationContext ctxt) {
            String code = p.getText();
            try {
                return TestEnum.lookup(code);
            } catch (IllegalArgumentException e) {
                throw InvalidFormatException.from(p, "Undefined ISO-639 language code",
                        code, TestEnum.class);
            }
        }
    }

    static class TestEnumKeySerializer extends ValueSerializer<TestEnum> {
        @Override
        public void serialize(TestEnum test, JsonGenerator g, SerializerProvider serializerProvider) {
            g.writeName(test.code());
        }

        @Override
        public Class<TestEnum> handledType() {
            return TestEnum.class;
        }
    }

    static class Bean {
        private File rootDirectory;
        private String licenseString;
        private Map<TestEnum, Map<String, String>> replacements;

        public File getRootDirectory() {
            return rootDirectory;
        }

        public void setRootDirectory(File rootDirectory) {
            this.rootDirectory = rootDirectory;
        }

        public String getLicenseString() {
            return licenseString;
        }

        public void setLicenseString(String licenseString) {
            this.licenseString = licenseString;
        }

        public Map<TestEnum, Map<String, String>> getReplacements() {
            return replacements;
        }

        public void setReplacements(Map<TestEnum, Map<String, String>> replacements) {
            this.replacements = replacements;
        }
    }

    static class TestEnumModule extends SimpleModule {
        public TestEnumModule() {
            super(Version.unknownVersion());
        }

        @Override
        public void setupModule(SetupContext context) {
            context.setMixIn(TestEnum.class, TestEnumMixin.class);
            SimpleSerializers keySerializers = new SimpleSerializers();
            keySerializers.addSerializer(new TestEnumKeySerializer());
            context.addKeySerializers(keySerializers);
        }
    }

    // for [databind#1441]

    enum SuperTypeEnum {
        FOO;
    }

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "@type", defaultImpl = SuperType.class)
    static class SuperType {
        public Map<SuperTypeEnum, String> someMap;
    }

    /*
    /**********************************************************
    /* Test methods
    /**********************************************************
     */
    
    // Test passing with the fix
    public void testWithEnumKeys() throws Exception {
        ObjectMapper plainObjectMapper = new ObjectMapper();
        JsonNode tree = plainObjectMapper.readTree(a2q("{'red' : [ 'a', 'b']}"));

        ObjectMapper fancyObjectMapper = jsonMapperBuilder()
                .addModule(new TestEnumModule())
                .build();
        Map<TestEnum, Set<String>> map = fancyObjectMapper.convertValue(tree,
                new TypeReference<Map<TestEnum, Set<String>>>() { } );
        assertNotNull(map);
    }

    // and another still failing
    // NOTE: temporarily named as non-test to ignore it; JsonIgnore doesn't work for some reason
//    public void testWithTree749() throws Exception
    public void withTree749() throws Exception
    {
        ObjectMapper mapper = jsonMapperBuilder()
                .addModule(new TestEnumModule())
                .build();
        Map<KeyEnum, Object> inputMap = new LinkedHashMap<KeyEnum, Object>();
        Map<TestEnum, Map<String, String>> replacements = new LinkedHashMap<TestEnum, Map<String, String>>();
        Map<String, String> reps = new LinkedHashMap<String, String>();
        reps.put("1", "one");
        replacements.put(TestEnum.GREEN, reps);
        inputMap.put(KeyEnum.replacements, replacements);

        JsonNode tree = mapper.valueToTree(inputMap);
        ObjectNode ob = (ObjectNode) tree;

        JsonNode inner = ob.get("replacements");
        String firstFieldName = inner.propertyNames().next();
        assertEquals("green", firstFieldName);
    }

    // [databind#1441]
    public void testCustomEnumKeySerializerWithPolymorphic() throws IOException
    {
        SimpleModule simpleModule = new SimpleModule();
        simpleModule.addDeserializer(SuperTypeEnum.class, new ValueDeserializer<SuperTypeEnum>() {
            @Override
            public SuperTypeEnum deserialize(JsonParser p, DeserializationContext deserializationContext)
            {
                return SuperTypeEnum.valueOf(p.getText());
            }
        });
        ObjectMapper mapper = jsonMapperBuilder()
                .addModule(simpleModule)
                .build();

        SuperType superType = mapper.readValue("{\"someMap\": {\"FOO\": \"bar\"}}",
                SuperType.class);
        assertEquals("Deserialized someMap.FOO should equal bar", "bar",
                superType.someMap.get(SuperTypeEnum.FOO));
    }

    // [databind#1445]
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public void testCustomEnumValueAndKeyViaModifier() throws IOException
    {
        SimpleModule module = new SimpleModule();
        module.setDeserializerModifier(new ValueDeserializerModifier() {        
            @Override
            public ValueDeserializer<Enum> modifyEnumDeserializer(DeserializationConfig config,
                    final JavaType type, BeanDescription beanDesc,
                    final ValueDeserializer<?> deserializer) {
                return new ValueDeserializer<Enum>() {
                    @Override
                    public Enum deserialize(JsonParser p, DeserializationContext ctxt) {
                        Class<? extends Enum> rawClass = (Class<Enum<?>>) type.getRawClass();
                        final String str = p.getValueAsString().toLowerCase();
                        return KeyEnum.valueOf(rawClass, str);
                    }
                };
            }

            @Override
            public KeyDeserializer modifyKeyDeserializer(DeserializationConfig config,
                    final JavaType type, KeyDeserializer deserializer)
            {
                if (!type.isEnumType()) {
                    return deserializer;
                }
                return new KeyDeserializer() {
                    @Override
                    public Object deserializeKey(String key, DeserializationContext ctxt)
                    {
                        Class<? extends Enum> rawClass = (Class<Enum<?>>) type.getRawClass();
                        return Enum.valueOf(rawClass, key.toLowerCase());
                    }
                };
            }
        });
        ObjectMapper mapper = jsonMapperBuilder()
                .addModule(module)
                .build();

        // First, enum value as is
        KeyEnum key = mapper.readValue(q(KeyEnum.replacements.name().toUpperCase()),
                KeyEnum.class);
        assertSame(KeyEnum.replacements, key);

        // and then as key
        EnumMap<KeyEnum,String> map = mapper.readValue(
                a2q("{'REPlaceMENTS':'foobar'}"),
                new TypeReference<EnumMap<KeyEnum,String>>() { });
        assertEquals(1, map.size());
        assertSame(KeyEnum.replacements, map.keySet().iterator().next());
    }
}
