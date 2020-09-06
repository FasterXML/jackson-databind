package com.fasterxml.jackson.databind.type;

import java.io.IOException;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.BaseMapTest;


// https://github.com/FasterXML/jackson-databind/issues/1647
public class TestTypeFactoryWithRecursiveTypes extends BaseMapTest
{
    static interface IFace<T> { }

    static class Base implements IFace<Sub> {
        @JsonProperty int base = 1;
    }

    static class Sub extends Base {
        @JsonProperty int sub = 2;
    }

    public void testBasePropertiesIncludedWhenSerializingSubWhenSubTypeLoadedAfterBaseType() throws IOException {
        TypeFactory tf = TypeFactory.defaultInstance();
        tf.constructType(Base.class);
        tf.constructType(Sub.class);
        Sub sub = new Sub();
        String serialized = objectMapper().writeValueAsString(sub);
        assertEquals("{\"base\":1,\"sub\":2}", serialized);
    }
}
