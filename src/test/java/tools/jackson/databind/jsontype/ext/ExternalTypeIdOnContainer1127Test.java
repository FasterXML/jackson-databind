package tools.jackson.databind.jsontype.ext;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.exc.InvalidDefinitionException;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.assertThrows;

// [databind#1127]: `@JsonTypeInfo(As.EXTERNAL_PROPERTY)` cannot work on a container-typed
// (Collection/array/Map) or reference-typed (Optional) property: there is no place to attach
// the external type-id sibling property. Verify we fail eagerly with a clear definition error
// instead of a confusing "expected START_ARRAY / As.WRAPPER_ARRAY" error at read time.
public class ExternalTypeIdOnContainer1127Test extends DatabindTestUtil
{
    static class Foo { public String msg; }
    static class FooA extends Foo { public String hey; }
    static class FooB extends Foo { public String whoa; }

    static class ListHolder {
        public String footype;
        @JsonTypeInfo(use = JsonTypeInfo.Id.NAME,
                include = JsonTypeInfo.As.EXTERNAL_PROPERTY, property = "footype")
        @JsonSubTypes({ @JsonSubTypes.Type(value = FooA.class, name = "a"),
                @JsonSubTypes.Type(value = FooB.class, name = "b") })
        public List<Foo> foo;
    }

    static class ArrayHolder {
        public String footype;
        @JsonTypeInfo(use = JsonTypeInfo.Id.NAME,
                include = JsonTypeInfo.As.EXTERNAL_PROPERTY, property = "footype")
        @JsonSubTypes({ @JsonSubTypes.Type(value = FooA.class, name = "a") })
        public Foo[] foo;
    }

    static class MapHolder {
        public String footype;
        @JsonTypeInfo(use = JsonTypeInfo.Id.NAME,
                include = JsonTypeInfo.As.EXTERNAL_PROPERTY, property = "footype")
        @JsonSubTypes({ @JsonSubTypes.Type(value = FooA.class, name = "a") })
        public Map<String, Foo> foo;
    }

    static class OptionalHolder {
        public String footype;
        @JsonTypeInfo(use = JsonTypeInfo.Id.NAME,
                include = JsonTypeInfo.As.EXTERNAL_PROPERTY, property = "footype")
        @JsonSubTypes({ @JsonSubTypes.Type(value = FooA.class, name = "a") })
        public Optional<Foo> foo;
    }

    private final ObjectMapper MAPPER = newJsonMapper();

    @Test
    public void testExternalPropertyOnListFailsEagerly() throws Exception {
        _verifyFailure(ListHolder.class,
                "{'footype':'a','foo':[{'msg':'Hello','hey':'there'}]}");
    }

    @Test
    public void testExternalPropertyOnArrayFailsEagerly() throws Exception {
        _verifyFailure(ArrayHolder.class,
                "{'footype':'a','foo':[{'msg':'Hello','hey':'there'}]}");
    }

    @Test
    public void testExternalPropertyOnMapFailsEagerly() throws Exception {
        _verifyFailure(MapHolder.class,
                "{'footype':'a','foo':{'x':{'msg':'Hello','hey':'there'}}}");
    }

    @Test
    public void testExternalPropertyOnReferenceFailsEagerly() throws Exception {
        _verifyFailure(OptionalHolder.class,
                "{'footype':'a','foo':{'msg':'Hello','hey':'there'}}");
    }

    private void _verifyFailure(Class<?> type, String json) {
        InvalidDefinitionException e = assertThrows(InvalidDefinitionException.class,
                () -> MAPPER.readValue(a2q(json), type));
        verifyException(e, "EXTERNAL_PROPERTY");
        verifyException(e, "container-typed property");
    }
}
