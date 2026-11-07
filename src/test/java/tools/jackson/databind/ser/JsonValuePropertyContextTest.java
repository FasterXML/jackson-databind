package tools.jackson.databind.ser;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonIncludeProperties;
import com.fasterxml.jackson.annotation.JsonValue;

import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.*;
import tools.jackson.databind.cfg.MapperConfig;
import tools.jackson.databind.introspect.Annotated;
import tools.jackson.databind.introspect.AnnotatedField;
import tools.jackson.databind.introspect.AnnotatedMember;
import tools.jackson.databind.introspect.AnnotatedMethod;
import tools.jackson.databind.introspect.JacksonAnnotationIntrospector;
import tools.jackson.databind.jsontype.TypeSerializer;
import tools.jackson.databind.module.SimpleModule;
import tools.jackson.databind.ser.jackson.JsonValueSerializer;
import tools.jackson.databind.testutil.DatabindTestUtil;
import tools.jackson.databind.util.ClassUtil;

import static org.junit.jupiter.api.Assertions.*;

class JsonValuePropertyContextTest extends DatabindTestUtil
{
    static final class Payload {
        public int keep = 1;
        public int drop = 2;
    }

    static class DynamicValue {
        @JsonValue
        public Object value() {
            return new Payload();
        }
    }

    static class Holder {
        public DynamicValue value = new DynamicValue();
    }

    static class AnnotatedValue extends DynamicValue {
        @Override
        @JsonValue
        @JsonIgnoreProperties("other")
        @JsonIncludeProperties({"keep", "drop", "other"})
        public Object value() {
            return super.value();
        }
    }

    static class AnnotatedHolder {
        @JsonIgnoreProperties("drop")
        @JsonIncludeProperties({"keep", "drop"})
        public AnnotatedValue value = new AnnotatedValue();
    }

    static class FilterState {
        final boolean inclusion;
        String name = "drop";

        FilterState(boolean inclusion) {
            this.inclusion = inclusion;
        }

        Set<String> ignored() {
            return inclusion ? Set.of() : Set.of(name);
        }

        Set<String> included() {
            return inclusion ? Set.of(name) : null;
        }
    }

    static class IncludedNames extends JsonIncludeProperties.Value {
        IncludedNames(Set<String> names) {
            super(names);
        }
    }

    static class ChangingIntrospector extends JacksonAnnotationIntrospector {
        private final FilterState _state;

        ChangingIntrospector(FilterState state) {
            _state = state;
        }

        @Override
        public JsonIgnoreProperties.Value findPropertyIgnoralByName(
                MapperConfig<?> config, Annotated member) {
            if (_isHolderField(member)) {
                return JsonIgnoreProperties.Value.forIgnoredProperties(_state.ignored());
            }
            return super.findPropertyIgnoralByName(config, member);
        }

        @Override
        public JsonIncludeProperties.Value findPropertyInclusionByName(
                MapperConfig<?> config, Annotated member) {
            if (_isHolderField(member)) {
                return new IncludedNames(_state.included());
            }
            return super.findPropertyInclusionByName(config, member);
        }
    }

    static class ChangingPropertyWriter extends BeanPropertyWriter {
        private final FilterState _state;

        ChangingPropertyWriter(BeanPropertyWriter base, FilterState state) {
            super(base);
            _state = state;
        }

        @Override
        public Set<String> findIgnoredForSerialization(MapperConfig<?> config) {
            return _state.ignored();
        }

        @Override
        public Set<String> findIncludedForSerialization(MapperConfig<?> config) {
            return _state.included();
        }
    }

    static class CapturedProperty {
        BeanProperty fromHook;
        BeanProperty contextual;
        SerializationConfig config;
    }

    static class CapturingSerializer extends ValueSerializer<Object> {
        private final ValueSerializer<Object> _delegate;
        private final CapturedProperty _capture;

        @SuppressWarnings("unchecked")
        CapturingSerializer(ValueSerializer<?> delegate, CapturedProperty capture) {
            _delegate = (ValueSerializer<Object>) delegate;
            _capture = capture;
        }

        @Override
        public void resolve(SerializationContext ctxt) {
            _delegate.resolve(ctxt);
        }

        @Override
        public ValueSerializer<?> createContextual(SerializationContext ctxt,
                BeanProperty property) {
            _capture.contextual = property;
            _capture.config = ctxt.getConfig();
            return new CapturingSerializer(_delegate.createContextual(ctxt, property), _capture);
        }

        @Override
        public void serialize(Object value, JsonGenerator gen, SerializationContext ctxt) {
            _delegate.serialize(value, gen, ctxt);
        }
    }

    static class HookSerializer extends JsonValueSerializer {
        private final CapturedProperty _capture;
        private final boolean _subclass;

        HookSerializer(JsonValueSerializer base, CapturedProperty capture, boolean subclass) {
            super(base, null, null, null, true);
            _capture = capture;
            _subclass = subclass;
        }

        HookSerializer(HookSerializer base, BeanProperty property, TypeSerializer vts,
                ValueSerializer<?> serializer, boolean forceTypeInfo, boolean emptyCheckDisabled) {
            super(base, property, vts, serializer, forceTypeInfo, emptyCheckDisabled);
            _capture = base._capture;
            _subclass = base._subclass;
        }

        @Override
        public JsonValueSerializer withResolved(BeanProperty property, TypeSerializer vts,
                ValueSerializer<?> serializer, boolean forceTypeInfo) {
            return new HookSerializer(this, property, vts, serializer, forceTypeInfo,
                    _emptyCheckDisabled);
        }

        @Override
        public JsonValueSerializer withEmptyCheckDisabled(boolean state) {
            return new HookSerializer(this, _property, _valueTypeSerializer, _valueSerializer,
                    _forceTypeInformation, state);
        }

        @Override
        protected BeanProperty _accessorProperty(BeanProperty property) {
            BeanProperty base = super._accessorProperty(property);
            BeanProperty result;
            if (_subclass) {
                result = new JsonValueProperty(property, base.getFullName(), base.getType(),
                        base.getWrapperName(), base.getMember(), base.getMetadata()) {
                    @Override
                    public Set<String> findIgnoredForSerialization(MapperConfig<?> config) {
                        return Set.of("drop");
                    }

                    @Override
                    public Set<String> findIncludedForSerialization(MapperConfig<?> config) {
                        return Set.of("keep");
                    }
                };
            } else {
                result = new BeanProperty.Std(base.getFullName(), base.getType(),
                        base.getWrapperName(), base.getMember(), base.getMetadata()) {
                    @Override
                    public Set<String> findIgnoredForSerialization(MapperConfig<?> config) {
                        return Set.of("drop");
                    }

                    @Override
                    public Set<String> findIncludedForSerialization(MapperConfig<?> config) {
                        return Set.of("keep");
                    }
                };
            }
            _capture.fromHook = result;
            return result;
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void honorsChangingIntrospector(boolean inclusion) {
        FilterState state = new FilterState(inclusion);
        ObjectMapper mapper = jsonMapperBuilder()
                .annotationIntrospector(new ChangingIntrospector(state))
                .build();
        _assertChangingFilter(mapper, state);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void honorsChangingPropertyWriter(boolean inclusion) {
        FilterState state = new FilterState(inclusion);
        SimpleModule module = new SimpleModule();
        module.setSerializerModifier(new ValueSerializerModifier() {
            @Override
            public List<BeanPropertyWriter> changeProperties(SerializationConfig config,
                    BeanDescription.Supplier beanDesc, List<BeanPropertyWriter> properties) {
                properties.replaceAll(property -> _isHolderField(property.getMember())
                        ? new ChangingPropertyWriter(property, state) : property);
                return properties;
            }
        });
        _assertChangingFilter(jsonMapperBuilder().addModule(module).build(), state);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void preservesAccessorPropertyHook(boolean subclass) throws Exception {
        CapturedProperty capture = new CapturedProperty();
        ObjectMapper mapper = jsonMapperBuilder()
                .addModule(_capturingModule(capture, subclass))
                .build();
        assertEquals(mapper.readTree("{\"value\":{\"keep\":1}}"),
                mapper.readTree(mapper.writeValueAsString(new Holder())));
        assertNotNull(capture.fromHook);
        assertSame(capture.fromHook, capture.contextual);
        assertEquals(DynamicValue.class.getDeclaredMethod("value"),
                capture.contextual.getMember().getMember());
        assertEquals(Set.of("drop"),
                capture.contextual.findIgnoredForSerialization(capture.config));
        assertEquals(Set.of("keep"),
                capture.contextual.findIncludedForSerialization(capture.config));

        capture.fromHook = null;
        capture.contextual = null;
        ObjectWriter writer = mapper.writerFor(DynamicValue.class);
        assertEquals(mapper.readTree("{\"keep\":1}"),
                mapper.readTree(writer.writeValueAsString(new DynamicValue())));
        assertNotNull(capture.fromHook);
        assertSame(capture.fromHook, capture.contextual);
        assertEquals(DynamicValue.class.getDeclaredMethod("value"),
                capture.contextual.getMember().getMember());
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void usesCurrentConfigurationForCapturedProperty(boolean root) throws Exception {
        CapturedProperty capture = new CapturedProperty();
        ObjectMapper mapper = jsonMapperBuilder()
                .addModule(_capturingModule(capture, null)).build();
        Object input = root ? new AnnotatedValue() : new AnnotatedHolder();
        String expected = root ? "{\"keep\":1,\"drop\":2}" : "{\"value\":{\"keep\":1}}";
        Set<String> ignored = root ? Set.of("other") : Set.of("drop", "other");
        Set<String> included = root ? Set.of("keep", "drop", "other") : Set.of("keep", "drop");
        assertEquals(mapper.readTree(expected), mapper.readTree(mapper.writeValueAsString(input)));
        BeanProperty property = capture.contextual;
        assertNotNull(property);
        assertEquals(AnnotatedValue.class.getDeclaredMethod("value"),
                property.getMember().getMember());
        assertEquals(ignored, property.findIgnoredForSerialization(capture.config));
        assertEquals(included, property.findIncludedForSerialization(capture.config));

        SerializationConfig standard = capture.config.withAttribute("context", "standard");
        assertEquals(ignored, property.findIgnoredForSerialization(standard));
        assertEquals(included, property.findIncludedForSerialization(standard));
        SerializationConfig alternateStandard = jsonMapperBuilder()
                .annotationIntrospector(new JacksonAnnotationIntrospector())
                .build().serializationConfig();
        assertNotSame(standard.getAnnotationIntrospector(),
                alternateStandard.getAnnotationIntrospector());
        assertEquals(ignored, property.findIgnoredForSerialization(alternateStandard));
        assertEquals(included, property.findIncludedForSerialization(alternateStandard));
        SerializationConfig withoutAnnotations = jsonMapperBuilder()
                .disable(MapperFeature.USE_ANNOTATIONS)
                .build().serializationConfig();
        assertEquals(Set.of(), property.findIgnoredForSerialization(withoutAnnotations));
        assertNull(property.findIncludedForSerialization(withoutAnnotations));

        ObjectMapper alternateMapper = jsonMapperBuilder()
                .annotationIntrospector(new JacksonAnnotationIntrospector() {
                    @Override
                    public JsonIgnoreProperties.Value findPropertyIgnoralByName(
                            MapperConfig<?> config, Annotated member) {
                        assertEquals("alternate", config.getAttributes().getAttribute("context"));
                        if (member instanceof AnnotatedMethod method
                                && method.getDeclaringClass() == AnnotatedValue.class) {
                            return JsonIgnoreProperties.Value.forIgnoredProperties("keep");
                        }
                        if (member instanceof AnnotatedField field
                                && field.getDeclaringClass() == AnnotatedHolder.class) {
                            return JsonIgnoreProperties.Value.forIgnoredProperties("extra");
                        }
                        return super.findPropertyIgnoralByName(config, member);
                    }

                    @Override
                    public JsonIncludeProperties.Value findPropertyInclusionByName(
                            MapperConfig<?> config, Annotated member) {
                        assertEquals("alternate", config.getAttributes().getAttribute("context"));
                        if (member instanceof AnnotatedMethod method
                                && method.getDeclaringClass() == AnnotatedValue.class) {
                            return new IncludedNames(Set.of("drop", "other"));
                        }
                        if (member instanceof AnnotatedField field
                                && field.getDeclaringClass() == AnnotatedHolder.class) {
                            return new IncludedNames(Set.of("drop"));
                        }
                        return super.findPropertyInclusionByName(config, member);
                    }
                }).build();
        SerializationConfig alternate = alternateMapper.serializationConfig()
                .withAttribute("context", "alternate");
        assertEquals(root ? Set.of("keep") : Set.of("keep", "extra"),
                property.findIgnoredForSerialization(alternate));
        assertEquals(root ? Set.of("drop", "other") : Set.of("drop"),
                property.findIncludedForSerialization(alternate));
        assertEquals(ignored, property.findIgnoredForSerialization(capture.config));
        assertEquals(included, property.findIncludedForSerialization(capture.config));
        assertEquals(AnnotatedValue.class.getDeclaredMethod("value"),
                property.getMember().getMember());

        CapturedProperty customCapture = new CapturedProperty();
        ObjectMapper customMapper = jsonMapperBuilder()
                .annotationIntrospector(alternate.getAnnotationIntrospector())
                .addModule(_capturingModule(customCapture, null)).build();
        ObjectWriter customWriter = customMapper.writer().withAttribute("context", "alternate")
                .forType(input.getClass());
        String customExpected = root ? "{\"drop\":2}" : "{\"value\":{\"drop\":2}}";
        assertEquals(customMapper.readTree(customExpected),
                customMapper.readTree(customWriter.writeValueAsString(input)));
        assertNotNull(customCapture.contextual);
        assertEquals(ignored, customCapture.contextual.findIgnoredForSerialization(standard));
        assertEquals(included, customCapture.contextual.findIncludedForSerialization(standard));
    }

    @Test
    void usesCurrentIntrospectionAfterCustomSerializerConstruction() throws Exception {
        CapturedProperty capture = new CapturedProperty();
        SerializationConfig customConfig = jsonMapperBuilder()
                .annotationIntrospector(new JacksonAnnotationIntrospector() {
                    @Override
                    public JsonIgnoreProperties.Value findPropertyIgnoralByName(
                            MapperConfig<?> config, Annotated member) {
                        if (member instanceof AnnotatedMethod method
                                && method.getDeclaringClass() == AnnotatedValue.class) {
                            return JsonIgnoreProperties.Value.forIgnoredProperties(Set.of());
                        }
                        return super.findPropertyIgnoralByName(config, member);
                    }
                }).build().serializationConfig();
        SimpleModule module = new SimpleModule();
        module.setSerializerModifier(new ValueSerializerModifier() {
            @Override
            public ValueSerializer<?> modifySerializer(SerializationConfig config,
                    BeanDescription.Supplier beanDesc, ValueSerializer<?> serializer) {
                if (beanDesc.getBeanClass() == AnnotatedValue.class) {
                    AnnotatedMember accessor = beanDesc.get().findJsonValueAccessor();
                    return JsonValueSerializer.construct(customConfig, beanDesc.getType(),
                            accessor.getType(), false, null, null, accessor);
                }
                if (beanDesc.getBeanClass() == Payload.class) {
                    return new CapturingSerializer(serializer, capture);
                }
                return serializer;
            }
        });
        ObjectMapper mapper = jsonMapperBuilder().addModule(module).build();
        assertEquals(mapper.readTree("{\"keep\":1,\"drop\":2}"),
                mapper.readTree(mapper.writerFor(AnnotatedValue.class)
                        .writeValueAsString(new AnnotatedValue())));
        assertNotNull(capture.contextual);
        assertEquals(JacksonAnnotationIntrospector.class,
                capture.config.getAnnotationIntrospector().getClass());
        assertEquals(AnnotatedValue.class.getDeclaredMethod("value"),
                capture.contextual.getMember().getMember());
        assertEquals(Set.of("other"),
                capture.contextual.findIgnoredForSerialization(capture.config));
    }

    private static boolean _isHolderField(Annotated member) {
        return member instanceof AnnotatedField field
                && field.getDeclaringClass() == Holder.class
                && field.getName().equals("value");
    }

    private void _assertChangingFilter(ObjectMapper mapper, FilterState state) {
        ObjectWriter writer = mapper.writerFor(Holder.class);
        Holder input = new Holder();
        JsonNode keep = mapper.readTree("{\"value\":{\"keep\":1}}");
        JsonNode drop = mapper.readTree("{\"value\":{\"drop\":2}}");
        assertEquals(state.inclusion ? drop : keep,
                mapper.readTree(writer.writeValueAsString(input)));
        state.name = "keep";
        assertEquals(state.inclusion ? keep : drop,
                mapper.readTree(writer.writeValueAsString(input)));
        state.name = "drop";
        assertEquals(state.inclusion ? drop : keep,
                mapper.readTree(writer.writeValueAsString(input)));
    }

    private SimpleModule _capturingModule(CapturedProperty capture, Boolean hookSubclass) {
        return new SimpleModule().setSerializerModifier(new ValueSerializerModifier() {
            @Override
            public ValueSerializer<?> modifySerializer(SerializationConfig config,
                    BeanDescription.Supplier beanDesc, ValueSerializer<?> serializer) {
                if (serializer instanceof JsonValueSerializer jsonValue) {
                    assertTrue(ClassUtil.isJacksonStdImpl(serializer));
                    if (hookSubclass != null) {
                        return new HookSerializer(jsonValue, capture, hookSubclass);
                    }
                }
                if (beanDesc.getBeanClass() == Payload.class) {
                    return new CapturingSerializer(serializer, capture);
                }
                return serializer;
            }
        });
    }
}
