package tools.jackson.databind.type;

import java.io.IOException;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonProperty;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.assertEquals;

// https://github.com/FasterXML/jackson-databind/issues/1647
public class TypeFactoryWithRecursiveTypesTest extends DatabindTestUtil
{
    static interface IFace<T> { }

    static class Base implements IFace<Sub> {
        @JsonProperty int base = 1;
    }

    static class Sub extends Base {
        @JsonProperty int sub = 2;
    }

    private final ObjectMapper MAPPER = newJsonMapper();

    @Test
    public void testBasePropertiesIncludedWhenSerializingSubWhenSubTypeLoadedAfterBaseType() throws IOException {
        TypeFactory tf = defaultTypeFactory();
        tf.constructType(Base.class);
        tf.constructType(Sub.class);
        Sub sub = new Sub();
        String serialized = MAPPER.writeValueAsString(sub);
        assertEquals("{\"base\":1,\"sub\":2}", serialized);
    }
}
