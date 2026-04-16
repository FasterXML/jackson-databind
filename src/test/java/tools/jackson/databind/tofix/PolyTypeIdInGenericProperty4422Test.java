package tools.jackson.databind.tofix;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import tools.jackson.databind.*;
import tools.jackson.databind.testutil.DatabindTestUtil;
import tools.jackson.databind.testutil.failure.JacksonTestFailureExpected;

import static org.junit.jupiter.api.Assertions.assertEquals;

// [databind#4422]: Type id not written when polymorphic value is assigned
// to a generic (unresolved type variable) property, even though the runtime
// class serializer is otherwise used to write the value's fields.
class PolyTypeIdInGenericProperty4422Test extends DatabindTestUtil {

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "valueType")
    @JsonSubTypes({
            @JsonSubTypes.Type(value = PolyChild.class, name = "CHILD")
    })
    interface PolyParent { }

    static class PolyChild implements PolyParent {
        public String polyChildField;
        public PolyChild() { }
        public PolyChild(String v) { polyChildField = v; }
    }

    static class Generic<T> {
        public T genericField;
        public Generic() { }
        public Generic(T v) { genericField = v; }
    }

    private final ObjectMapper MAPPER = newJsonMapper();

    @JacksonTestFailureExpected
    @Test
    void typeIdWrittenForGenericProperty() throws Exception {
        String json = MAPPER.writeValueAsString(new Generic<>(new PolyChild("S")));
        assertEquals(a2q(
                "{'genericField':{'valueType':'CHILD','polyChildField':'S'}}"),
                json);
    }
}
