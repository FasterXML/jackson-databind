package com.fasterxml.jackson.databind.type;

import java.io.IOException;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;

import static com.fasterxml.jackson.databind.testutil.DatabindTestUtil.newJsonMapper;


// https://github.com/FasterXML/jackson-databind/issues/1647
public class TypeFactoryWithRecursiveTypesTest
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
        TypeFactory tf = TypeFactory.defaultInstance();
        tf.constructType(Base.class);
        tf.constructType(Sub.class);
        Sub sub = new Sub();
        String serialized = MAPPER.writeValueAsString(sub);
        assertEquals("{\"base\":1,\"sub\":2}", serialized);
    }
}
