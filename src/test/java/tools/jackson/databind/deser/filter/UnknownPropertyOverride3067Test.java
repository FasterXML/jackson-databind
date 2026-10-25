package tools.jackson.databind.deser.filter;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.OptBoolean;

import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonParser;
import tools.jackson.core.FormatSchema;
import tools.jackson.core.TokenStreamFactory;
import tools.jackson.databind.AnnotationIntrospector;
import tools.jackson.databind.BeanDescription;
import tools.jackson.databind.DeserializationConfig;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.InjectableValues;
import tools.jackson.databind.ValueDeserializer;
import tools.jackson.databind.annotation.JsonDeserialize;
import tools.jackson.databind.annotation.JsonPOJOBuilder;
import tools.jackson.databind.cfg.MapperConfig;
import tools.jackson.databind.cfg.DeserializationContexts;
import tools.jackson.databind.deser.DeserializationContextExt;
import tools.jackson.databind.deser.DeserializerCache;
import tools.jackson.databind.deser.DeserializerFactory;
import tools.jackson.databind.deser.DeserializationProblemHandler;
import tools.jackson.databind.deser.ValueDeserializerModifier;
import tools.jackson.databind.deser.bean.BeanDeserializer;
import tools.jackson.databind.deser.std.DelegatingDeserializer;
import tools.jackson.databind.exc.IgnoredPropertyException;
import tools.jackson.databind.exc.MismatchedInputException;
import tools.jackson.databind.exc.UnrecognizedPropertyException;
import tools.jackson.databind.introspect.Annotated;
import tools.jackson.databind.introspect.NopAnnotationIntrospector;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.module.SimpleModule;

import static org.junit.jupiter.api.Assertions.*;

class UnknownPropertyOverride3067Test
{
    @JsonDeserialize(failOnUnknownProperties = OptBoolean.TRUE)
    static class StrictBean { public int known; }

    @JsonDeserialize(failOnUnknownProperties = OptBoolean.FALSE)
    static class LenientBean { public int known; }

    @JsonDeserialize(failOnUnknownProperties = OptBoolean.DEFAULT)
    static class DefaultBean { public int known; }

    static class PlainBean { public int known; }

    static final class GetterCreator {
        final int known;

        @JsonCreator
        public GetterCreator(@JsonProperty("known") int known) { this.known = known; }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    static class GetterLegacyBean { public int known; }

    @JsonFormat(shape = JsonFormat.Shape.ARRAY)
    @JsonPropertyOrder("known")
    static class GetterArrayBean { public int known; }

    @JsonIgnoreProperties(ignoreUnknown = false)
    static class LegacyFalse { public int known; }

    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonDeserialize(failOnUnknownProperties = OptBoolean.DEFAULT)
    static class LegacyTrue { public int known; }

    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonDeserialize(failOnUnknownProperties = OptBoolean.TRUE)
    static class StrictOverLegacy { public int known; }

    @JsonIgnoreProperties("ignored")
    @JsonDeserialize(failOnUnknownProperties = OptBoolean.TRUE)
    static class WithNamedIgnoral { public int known; }

    @JsonDeserialize(failOnUnknownProperties = OptBoolean.TRUE)
    static class WithAnySetter {
        public int known;
        final Map<String, Object> extras = new LinkedHashMap<>();

        @JsonAnySetter
        public void put(String name, Object value) { extras.put(name, value); }
    }

    static class StrictChild extends StrictBean { }

    @JsonDeserialize(failOnUnknownProperties = OptBoolean.FALSE)
    abstract static class LenientMixin { }

    static class Nested {
        public StrictBean strict;

        @JsonDeserialize(failOnUnknownProperties = OptBoolean.FALSE)
        public StrictBean loose;
    }

    static class StrictMember {
        @JsonDeserialize(failOnUnknownProperties = OptBoolean.TRUE)
        public LenientBean value;
    }

    @JsonDeserialize(failOnUnknownProperties = OptBoolean.TRUE)
    static final class StrictCreator {
        final int known;

        @JsonCreator
        public StrictCreator(@JsonProperty("known") int known) { this.known = known; }
    }

    @JsonDeserialize(failOnUnknownProperties = OptBoolean.TRUE)
    record StrictRecord(int known) { }

    @JsonDeserialize(builder = StrictBuilder.class)
    static final class BuiltBean {
        final int known;
        BuiltBean(int known) { this.known = known; }
    }

    @JsonPOJOBuilder(withPrefix = "")
    @JsonDeserialize(failOnUnknownProperties = OptBoolean.TRUE)
    static class StrictBuilder {
        int known;
        public StrictBuilder known(int value) { known = value; return this; }
        public BuiltBean build() { return new BuiltBean(known); }
    }

    @JsonDeserialize(builder = StrictBuilderForLenientTarget.class,
            failOnUnknownProperties = OptBoolean.FALSE)
    static final class LenientBuiltBean {
        final int known;
        LenientBuiltBean(int known) { this.known = known; }
    }

    @JsonPOJOBuilder(withPrefix = "")
    @JsonDeserialize(failOnUnknownProperties = OptBoolean.TRUE)
    static class StrictBuilderForLenientTarget {
        int known;
        public StrictBuilderForLenientTarget known(int value) { known = value; return this; }
        public LenientBuiltBean build() { return new LenientBuiltBean(known); }
    }

    @JsonFormat(shape = JsonFormat.Shape.ARRAY)
    @JsonPropertyOrder("known")
    @JsonDeserialize(failOnUnknownProperties = OptBoolean.TRUE)
    static class StrictArrayBean { public int known; }

    @JsonFormat(shape = JsonFormat.Shape.ARRAY)
    @JsonPropertyOrder("known")
    @JsonDeserialize(failOnUnknownProperties = OptBoolean.FALSE)
    static class LenientArrayBean { public int known; }

    @JsonFormat(shape = JsonFormat.Shape.ARRAY)
    @JsonDeserialize(failOnUnknownProperties = OptBoolean.TRUE)
    record StrictArrayRecord(int known) { }

    @JsonDeserialize(failOnUnknownProperties = OptBoolean.TRUE)
    static class StrictException extends RuntimeException {
        private static final long serialVersionUID = 1L;
        public StrictException(String message) { super(message); }
    }

    static class ThrowableMember {
        @JsonDeserialize(failOnUnknownProperties = OptBoolean.FALSE)
        public StrictException value;
    }

    @JsonDeserialize(failOnUnknownProperties = OptBoolean.TRUE)
    static class PolymorphicBase {
        public final int known;

        PolymorphicBase(int known) { this.known = known; }

        @JsonCreator
        public static PolymorphicBase create(@JsonProperty("known") int known) {
            return known == 2 ? new WrappedPolymorphicChild(known) : new PolymorphicChild(known);
        }
    }

    @JsonDeserialize(failOnUnknownProperties = OptBoolean.FALSE)
    static class PolymorphicChild extends PolymorphicBase {
        public String child;
        PolymorphicChild(int known) { super(known); }
    }

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
    @JsonTypeName("wrapped")
    @JsonDeserialize(failOnUnknownProperties = OptBoolean.FALSE)
    static class WrappedPolymorphicChild extends PolymorphicChild {
        WrappedPolymorphicChild(int known) { super(known); }
    }

    static class PolymorphicMembers {
        @JsonDeserialize(failOnUnknownProperties = OptBoolean.TRUE)
        public PolymorphicBase strict;

        @JsonDeserialize(failOnUnknownProperties = OptBoolean.FALSE)
        public PolymorphicBase loose;
    }

    @JsonDeserialize(failOnUnknownProperties = OptBoolean.TRUE)
    static class UnwrappedPair {
        @JsonUnwrapped(prefix = "left_")
        public StrictBean left;

        @JsonUnwrapped(prefix = "right_")
        public StrictBean right;
    }

    static class FixedIntrospector extends NopAnnotationIntrospector {
        private static final long serialVersionUID = 1L;
        private final Boolean answer;

        FixedIntrospector(Boolean answer) { this.answer = answer; }

        @Override
        public Boolean findFailOnUnknownProperties(MapperConfig<?> config, Annotated annotated) {
            return answer;
        }
    }

    static class ReusingDeserializer extends BeanDeserializer {
        private static final long serialVersionUID = 1L;

        ReusingDeserializer(BeanDeserializer source) { super(source); }

        @Override
        public ReusingDeserializer withByNameInclusion(Set<String> ignored, Set<String> included) {
            return this;
        }
    }

    static class PassthroughDeserializer extends DelegatingDeserializer {
        PassthroughDeserializer(ValueDeserializer<?> delegate) { super(delegate); }

        @Override
        protected ValueDeserializer<?> newDelegatingInstance(ValueDeserializer<?> delegate) {
            return new PassthroughDeserializer(delegate);
        }
    }

    static class StrictPolicyGetterDeserializer extends BeanDeserializer {
        private static final long serialVersionUID = 1L;

        StrictPolicyGetterDeserializer(BeanDeserializer source) { super(source); }

        @Override
        public Boolean getFailOnUnknownProperties() { return Boolean.TRUE; }
    }

    static class LegacyHandlingContexts extends DeserializationContexts.DefaultImpl {
        private static final long serialVersionUID = 1L;
        private final AtomicInteger calls;
        private final boolean consume;

        LegacyHandlingContexts(AtomicInteger calls, boolean consume) {
            this.calls = calls;
            this.consume = consume;
        }

        LegacyHandlingContexts(TokenStreamFactory factory, DeserializerFactory deserializers,
                DeserializerCache cache, AtomicInteger calls, boolean consume) {
            super(factory, deserializers, cache);
            this.calls = calls;
            this.consume = consume;
        }

        @Override
        public DeserializationContexts forMapper(Object mapper, TokenStreamFactory factory,
                DeserializerFactory deserializers, DeserializerCache cache) {
            return new LegacyHandlingContexts(factory, deserializers, cache, calls, consume);
        }

        @Override
        public DeserializationContextExt createContext(DeserializationConfig config,
                FormatSchema schema, InjectableValues values) {
            return new LegacyHandlingContext(_streamFactory, _deserializerFactory, _cache,
                    config, schema, values, calls, consume);
        }
    }

    static class LegacyHandlingContext extends DeserializationContextExt {
        private final AtomicInteger calls;
        private final boolean consume;

        LegacyHandlingContext(TokenStreamFactory factory, DeserializerFactory deserializers,
                DeserializerCache cache, DeserializationConfig config, FormatSchema schema,
                InjectableValues values, AtomicInteger calls, boolean consume) {
            super(factory, deserializers, cache, config, schema, values);
            this.calls = calls;
            this.consume = consume;
        }

        @Override
        public boolean handleUnknownProperty(JsonParser parser, ValueDeserializer<?> deserializer,
                Object value, String name) throws JacksonException {
            calls.incrementAndGet();
            if (consume) {
                parser.skipChildren();
                return true;
            }
            return super.handleUnknownProperty(parser, deserializer, value, name);
        }
    }

    private final ObjectMapper lenient = JsonMapper.builder()
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES).build();
    private final ObjectMapper strict = JsonMapper.builder()
            .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES).build();

    private static final String EXTRA = """
            {"known":1,"extra":2}
            """;

    private ObjectMapper policyGetterMapper() {
        SimpleModule module = new SimpleModule();
        module.setDeserializerModifier(new ValueDeserializerModifier() {
            @Override
            public ValueDeserializer<?> modifyDeserializer(DeserializationConfig config,
                    BeanDescription.Supplier description, ValueDeserializer<?> deserializer) {
                return Set.of(PlainBean.class, GetterCreator.class, GetterLegacyBean.class,
                        GetterArrayBean.class).contains(description.getBeanClass())
                        ? new StrictPolicyGetterDeserializer((BeanDeserializer) deserializer) : deserializer;
            }
        });
        return JsonMapper.builder().disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .addModule(module).build();
    }

    @Test
    void explicitStrictOverridesGlobalLenient() {
        assertThrows(UnrecognizedPropertyException.class,
                () -> lenient.readValue(EXTRA, StrictBean.class));
    }

    @Test
    void explicitLenientOverridesGlobalStrict() throws Exception {
        assertEquals(1, strict.readValue(EXTRA, LenientBean.class).known);
    }

    @Test
    void unspecifiedAndDefaultPreserveGlobalBehavior() throws Exception {
        assertEquals(1, lenient.readValue(EXTRA, PlainBean.class).known);
        assertEquals(1, lenient.readValue(EXTRA, DefaultBean.class).known);
        assertThrows(UnrecognizedPropertyException.class,
                () -> strict.readValue(EXTRA, PlainBean.class));
        assertThrows(UnrecognizedPropertyException.class,
                () -> strict.readValue(EXTRA, DefaultBean.class));
    }

    @Test
    void legacyAnnotationBehaviorIsUnchangedWithoutOverride() throws Exception {
        assertEquals(1, lenient.readValue(EXTRA, LegacyFalse.class).known);
        assertEquals(1, strict.readValue(EXTRA, LegacyTrue.class).known);
        assertThrows(UnrecognizedPropertyException.class,
                () -> strict.readValue(EXTRA, LegacyFalse.class));
    }

    @Test
    void explicitSettingTakesPrecedenceOverLegacyIgnoreUnknown() {
        assertThrows(UnrecognizedPropertyException.class,
                () -> lenient.readValue(EXTRA, StrictOverLegacy.class));
    }

    @Test
    void namedIgnoralsKeepTheirSeparatePolicy() throws Exception {
        String input = """
                {"known":1,"ignored":2}
                """;
        assertEquals(1, lenient.readValue(input, WithNamedIgnoral.class).known);
        assertThrows(UnrecognizedPropertyException.class,
                () -> lenient.readValue(EXTRA, WithNamedIgnoral.class));
        assertThrows(IgnoredPropertyException.class,
                () -> lenient.readerFor(WithNamedIgnoral.class)
                        .with(DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES).readValue(input));
    }

    @Test
    void anySetterStillHandlesUnknownProperties() throws Exception {
        WithAnySetter bean = lenient.readValue(EXTRA, WithAnySetter.class);
        assertEquals(1, bean.known);
        assertEquals(Map.of("extra", 2), bean.extras);
    }

    @Test
    void problemHandlerCanConsumeUnknownBeforeStrictFailure() throws Exception {
        AtomicInteger calls = new AtomicInteger();
        ObjectMapper mapper = JsonMapper.builder()
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .addHandler(new DeserializationProblemHandler() {
                    @Override
                    public boolean handleUnknownProperty(DeserializationContext ctxt, JsonParser parser,
                            ValueDeserializer<?> deser, Object bean, String name) throws JacksonException {
                        calls.incrementAndGet();
                        parser.skipChildren();
                        return true;
                    }
                }).build();
        assertEquals(1, mapper.readValue(EXTRA, StrictBean.class).known);
        assertEquals(1, calls.get());
    }

    @Test
    void decliningHandlerStillRunsForExplicitLenientSetting() throws Exception {
        AtomicInteger calls = new AtomicInteger();
        ObjectMapper mapper = JsonMapper.builder()
                .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .addHandler(new DeserializationProblemHandler() {
                    @Override
                    public boolean handleUnknownProperty(DeserializationContext ctxt, JsonParser parser,
                            ValueDeserializer<?> deser, Object bean, String name) {
                        calls.incrementAndGet();
                        return false;
                    }
                }).build();
        LenientBean bean = mapper.readValue("""
                {"extra":{"nested":2},"known":7}
                """, LenientBean.class);
        assertEquals(7, bean.known);
        assertEquals(1, calls.get());
    }

    @Test
    void nestedAndMemberOverridesDoNotLeakThroughCaches() throws Exception {
        assertEquals(1, lenient.readValue("""
                {"loose":{"known":1,"extra":2},"outerExtra":3}
                """, Nested.class).loose.known);
        assertThrows(UnrecognizedPropertyException.class, () -> lenient.readValue("""
                {"strict":{"known":1,"extra":2}}
                """, Nested.class));
        assertThrows(UnrecognizedPropertyException.class,
                () -> lenient.readValue(EXTRA, StrictBean.class));
        assertThrows(UnrecognizedPropertyException.class, () -> lenient.readValue("""
                {"value":{"known":1,"extra":2}}
                """, StrictMember.class));
        assertEquals(1, strict.readValue(EXTRA, LenientBean.class).known);
    }

    @Test
    void inheritedAndMixinSettingsAreRespected() throws Exception {
        assertThrows(UnrecognizedPropertyException.class,
                () -> lenient.readValue(EXTRA, StrictChild.class));
        ObjectMapper mapper = JsonMapper.builder()
                .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .addMixIn(StrictBean.class, LenientMixin.class).build();
        assertEquals(1, mapper.readValue(EXTRA, StrictBean.class).known);
    }

    @Test
    void creatorAndRecordCannotDropUnknownBeforeConstruction() {
        for (String input : new String[] {EXTRA, """
                {"extra":2,"known":1}
                """}) {
            assertThrows(UnrecognizedPropertyException.class,
                    () -> lenient.readValue(input, StrictCreator.class));
            assertThrows(UnrecognizedPropertyException.class,
                    () -> lenient.readValue(input, StrictRecord.class));
        }
    }

    @Test
    void builderUsesItsOwnUnknownPropertySetting() throws Exception {
        assertEquals(1, lenient.readValue("""
                {"known":1}
                """, BuiltBean.class).known);
        for (String input : new String[] {EXTRA, """
                {"extra":2,"known":1}
                """}) {
            assertThrows(UnrecognizedPropertyException.class,
                    () -> lenient.readValue(input, BuiltBean.class));
        }
    }

    @Test
    void arrayShapeUsesTheExplicitSetting() throws Exception {
        assertEquals(1, lenient.readValue("[1]", StrictArrayBean.class).known);
        assertThrows(MismatchedInputException.class,
                () -> lenient.readValue("[1,2]", StrictArrayBean.class));
        assertThrows(MismatchedInputException.class,
                () -> lenient.readValue("[1,2]", StrictArrayRecord.class));
        assertEquals(1, strict.readValue("[1,2]", LenientArrayBean.class).known);
    }

    @Test
    void targetSettingTakesPrecedenceOverBuilderSetting() throws Exception {
        assertEquals(1, strict.readValue(EXTRA, LenientBuiltBean.class).known);
    }

    @Test
    void throwableContextualCopyKeepsThrowableDeserialization() throws Exception {
        assertThrows(UnrecognizedPropertyException.class, () -> lenient.readValue("""
                {"message":"retained","extra":2}
                """, StrictException.class));
        ThrowableMember value = strict.readValue("""
                {"value":{"message":"retained","extra":2}}
                """, ThrowableMember.class);
        assertEquals("retained", value.value.getMessage());
    }

    @Test
    void creatorReturningSubtypeStillReplaysSubtypeProperties() throws Exception {
        PolymorphicBase value = lenient.readValue("""
                {"child":"retained","known":1}
                """, PolymorphicBase.class);
        assertInstanceOf(PolymorphicChild.class, value);
        assertEquals("retained", ((PolymorphicChild) value).child);
        assertEquals(1, value.known);
    }

    @Test
    void memberOverrideSurvivesSubtypeReplayWithoutChangingRootCache() throws Exception {
        String input = """
                {"child":"retained","extra":2,"known":1}
                """;
        // The runtime subtype has its own lenient type annotation.
        assertInstanceOf(PolymorphicChild.class, lenient.readValue(input, PolymorphicBase.class));
        assertEquals("retained", ((PolymorphicChild) lenient.readValue("""
                {"strict":{"child":"retained","known":1}}
                """, PolymorphicMembers.class).strict).child);
        assertThrows(UnrecognizedPropertyException.class, () -> lenient.readValue("""
                {"strict":{"child":"retained","extra":2,"known":1}}
                """, PolymorphicMembers.class));
        assertInstanceOf(PolymorphicChild.class, lenient.readValue(input, PolymorphicBase.class));
        assertInstanceOf(PolymorphicChild.class, lenient.readValue("""
                {"loose":{"child":"retained","extra":2,"known":1}}
                """, PolymorphicMembers.class).loose);
    }

    @Test
    void unwrappedChildrenDoNotRejectEachOthersFields() throws Exception {
        UnwrappedPair value = lenient.readValue("""
                {"left_known":1,"right_known":2}
                """, UnwrappedPair.class);
        assertEquals(1, value.left.known);
        assertEquals(2, value.right.known);
    }

    @Test
    void introspectorPairTreatsFalseAsAnExplicitAnswer() throws Exception {
        ObjectMapper mapper = JsonMapper.builder()
                .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .annotationIntrospector(AnnotationIntrospector.pair(
                        new FixedIntrospector(false), new FixedIntrospector(true))).build();
        assertEquals(1, mapper.readValue(EXTRA, PlainBean.class).known);
        ObjectMapper fallback = JsonMapper.builder()
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .annotationIntrospector(AnnotationIntrospector.pair(
                        new FixedIntrospector(null), new FixedIntrospector(true))).build();
        assertThrows(UnrecognizedPropertyException.class,
                () -> fallback.readValue(EXTRA, PlainBean.class));
    }

    @Test
    void customNoOpCopyCannotMutateTheCachedDeserializer() throws Exception {
        SimpleModule module = new SimpleModule();
        module.setDeserializerModifier(new ValueDeserializerModifier() {
            @Override
            public ValueDeserializer<?> modifyDeserializer(DeserializationConfig config,
                    BeanDescription.Supplier description, ValueDeserializer<?> deserializer) {
                return description.getBeanClass() == StrictBean.class
                        ? new ReusingDeserializer((BeanDeserializer) deserializer) : deserializer;
            }
        });
        ObjectMapper mapper = JsonMapper.builder()
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .addModule(module).build();
        assertEquals(1, mapper.readValue("""
                {"known":1}
                """, StrictBean.class).known);
        IllegalStateException problem = assertThrows(IllegalStateException.class,
                () -> mapper.readValue("""
                        {"loose":{"known":1,"extra":2}}
                        """, Nested.class));
        assertTrue(problem.getMessage().contains("contextual copy"));
        assertThrows(UnrecognizedPropertyException.class,
                () -> mapper.readValue(EXTRA, StrictBean.class));
    }

    @Test
    void memberOverrideSurvivesATypeWrappedSubtype() throws Exception {
        assertInstanceOf(WrappedPolymorphicChild.class, lenient.readValue("""
                {"child":"retained","known":2}
                """, PolymorphicBase.class));
        assertThrows(UnrecognizedPropertyException.class, () -> lenient.readValue("""
                {"strict":{"child":"retained","extra":3,"known":2}}
                """, PolymorphicMembers.class));
    }

    @Test
    void memberOverrideSurvivesTypeAndModuleWrappers() throws Exception {
        SimpleModule module = new SimpleModule();
        module.setDeserializerModifier(new ValueDeserializerModifier() {
            @Override
            public ValueDeserializer<?> modifyDeserializer(DeserializationConfig config,
                    BeanDescription.Supplier description, ValueDeserializer<?> deserializer) {
                return description.getBeanClass() == WrappedPolymorphicChild.class
                        ? new PassthroughDeserializer(deserializer) : deserializer;
            }
        });
        ObjectMapper mapper = JsonMapper.builder()
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .addModule(module).build();
        assertInstanceOf(WrappedPolymorphicChild.class, mapper.readValue("""
                {"child":"retained","known":2}
                """, PolymorphicBase.class));
        assertThrows(UnrecognizedPropertyException.class, () -> mapper.readValue("""
                {"strict":{"child":"retained","extra":3,"known":2}}
                """, PolymorphicMembers.class));
    }

    @Test
    void legacyContextOverrideCanStillHandleUnknownProperties() throws Exception {
        AtomicInteger calls = new AtomicInteger();
        ObjectMapper mapper = JsonMapper.builder()
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .deserializationContexts(new LegacyHandlingContexts(calls, true)).build();
        assertEquals(1, mapper.readValue(EXTRA, StrictBean.class).known);
        assertEquals(1, calls.get());
    }

    @Test
    void legacyContextOverrideCanForwardToTheNewPolicy() {
        AtomicInteger calls = new AtomicInteger();
        ObjectMapper mapper = JsonMapper.builder()
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .deserializationContexts(new LegacyHandlingContexts(calls, false)).build();
        assertThrows(UnrecognizedPropertyException.class,
                () -> mapper.readValue(EXTRA, StrictBean.class));
        assertEquals(1, calls.get());
    }

    @Test
    void policyGetterControlsCreatorBuffering() {
        ObjectMapper mapper = policyGetterMapper();
        assertThrows(UnrecognizedPropertyException.class,
                () -> mapper.readValue(EXTRA, PlainBean.class));
        assertThrows(UnrecognizedPropertyException.class, () -> mapper.readValue("""
                {"extra":2,"known":1}
                """, GetterCreator.class));
    }

    @Test
    void policyGetterTakesPrecedenceOverLegacyIgnoreUnknown() {
        ObjectMapper mapper = policyGetterMapper();
        assertThrows(UnrecognizedPropertyException.class,
                () -> mapper.readValue(EXTRA, GetterLegacyBean.class));
    }

    @Test
    void arrayCopyPreservesPolicyReportedByDelegate() throws Exception {
        ObjectMapper mapper = policyGetterMapper();
        assertEquals(1, mapper.readValue("[1]", GetterArrayBean.class).known);
        assertThrows(MismatchedInputException.class,
                () -> mapper.readValue("[1,2]", GetterArrayBean.class));
    }
}
