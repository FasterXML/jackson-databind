package tools.jackson.databind.deser.builder;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.MapperFeature;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.annotation.JsonDeserialize;
import tools.jackson.databind.deser.ValueInstantiator;
import tools.jackson.databind.exc.InvalidDefinitionException;
import tools.jackson.databind.exc.ValueInstantiationException;
import tools.jackson.databind.module.SimpleModule;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

import static tools.jackson.databind.testutil.DatabindTestUtil.jsonMapperBuilder;
import static tools.jackson.databind.testutil.DatabindTestUtil.verifyException;

public class BuilderWithFactoryMethod2354Test
{
    @JsonDeserialize(builder = Document.Builder.class,
            builderMethod = "builder", builderPrefix = "")
    public static class Document {
        final String _title;

        private Document(String title) {
            _title = title;
        }

        public static Builder<?, ?> builder() {
            return new BuilderImpl().title("Untitled");
        }

        // Like Lombok @SuperBuilder, expose the builder API and hide its implementation.
        public abstract static class Builder<T extends Document, B extends Builder<T, B>> {
            protected String _title;

            public B title(String title) {
                _title = title;
                return self();
            }

            protected abstract B self();

            public abstract T build();
        }

        private static final class BuilderImpl extends Builder<Document, BuilderImpl> {
            @Override
            protected BuilderImpl self() {
                return this;
            }

            @Override
            public Document build() {
                return new Document(_title);
            }
        }
    }

    static class Value {
        final int _number;

        private Value(int number) {
            _number = number;
        }

        private static Builder newBuilder() {
            return new Builder(37);
        }

        static Builder newBuilder(int number) {
            return new Builder(number);
        }

        static String wrongReturnType() {
            return "not a builder";
        }

        static Builder failingFactory() {
            throw new IllegalStateException("builder factory failed");
        }

        static class Builder {
            private int _number;

            @JsonCreator
            Builder(@JsonProperty("number") int number) {
                _number = number;
            }

            public Builder withNumber(int number) {
                _number = number;
                return this;
            }

            public Value build() {
                return new Value(_number);
            }
        }
    }

    @JsonDeserialize(builder = Value.Builder.class, builderMethod = "newBuilder")
    abstract static class FactoryMixin { }

    @JsonDeserialize(builder = Value.Builder.class, builderMethod = "missing")
    abstract static class MissingFactoryMixin { }

    @JsonDeserialize(builder = Value.Builder.class, builderMethod = "wrongReturnType")
    abstract static class WrongReturnTypeMixin { }

    @JsonDeserialize(builder = Value.Builder.class, builderMethod = "failingFactory")
    abstract static class FailingFactoryMixin { }

    @Test
    void publicFactoryCreatesPrivateBuilderImplementation() {
        ObjectMapper mapper = jsonMapperBuilder()
                .disable(MapperFeature.CAN_OVERRIDE_ACCESS_MODIFIERS)
                .build();

        Document document = mapper.readValue("""
                {"title":"Report"}
                """, Document.class);
        assertEquals("Report", document._title);
        assertEquals("Untitled", mapper.readValue("{}", Document.class)._title);
    }

    @Test
    void factoryConfiguredByMixinSuppliesBuilderBeforeBinding() {
        ObjectMapper mapper = _mapperWith(FactoryMixin.class);

        Value value = mapper.readValue("""
                {"number":42}
                """, Value.class);
        assertEquals(37, mapper.readValue("{}", Value.class)._number);
        assertEquals(42, value._number);
    }

    @Test
    void registeredValueInstantiatorStillTakesPrecedence() {
        ObjectMapper mapper = _mapperWithInstantiator(FactoryMixin.class);
        assertEquals(19, mapper.readValue("{}", Value.class)._number);
    }

    @Test
    void registeredValueInstantiatorSkipsInvalidFactory() {
        ObjectMapper missingFactory = _mapperWithInstantiator(MissingFactoryMixin.class);
        ObjectMapper wrongReturnType = _mapperWithInstantiator(WrongReturnTypeMixin.class);

        assertEquals(19, missingFactory.readValue("{}", Value.class)._number);
        assertEquals(19, wrongReturnType.readValue("{}", Value.class)._number);
    }

    @Test
    void missingFactoryFailsAsInvalidDefinition() {
        ObjectMapper mapper = _mapperWith(MissingFactoryMixin.class);

        InvalidDefinitionException e = assertThrows(InvalidDefinitionException.class,
                () -> mapper.readValue("{}", Value.class));
        verifyException(e, "static no-argument builder factory method 'missing()'");
    }

    @Test
    void incompatibleFactoryReturnTypeFailsAsInvalidDefinition() {
        ObjectMapper mapper = _mapperWith(WrongReturnTypeMixin.class);

        InvalidDefinitionException e = assertThrows(InvalidDefinitionException.class,
                () -> mapper.readValue("{}", Value.class));
        verifyException(e, "Builder factory method 'wrongReturnType()'");
        verifyException(e, "not assignable");
    }

    @Test
    void factoryExceptionUsesCreatorErrorHandling() {
        ObjectMapper mapper = _mapperWith(FailingFactoryMixin.class);

        ValueInstantiationException e = assertThrows(ValueInstantiationException.class,
                () -> mapper.readValue("{}", Value.class));
        assertInstanceOf(IllegalStateException.class, e.getCause());
        assertEquals("builder factory failed", e.getCause().getMessage());
    }

    private static ObjectMapper _mapperWith(Class<?> mixin) {
        return jsonMapperBuilder().addMixIn(Value.class, mixin).build();
    }

    private static ObjectMapper _mapperWithInstantiator(Class<?> mixin) {
        SimpleModule module = new SimpleModule().addValueInstantiator(Value.Builder.class,
                new ValueInstantiator.Base(Value.Builder.class) {
                    @Override
                    public boolean canCreateUsingDefault() {
                        return true;
                    }

                    @Override
                    public Object createUsingDefault(DeserializationContext ctxt) {
                        return new Value.Builder(19);
                    }
                });
        return jsonMapperBuilder().addMixIn(Value.class, mixin)
                .addModule(module)
                .build();
    }
}
